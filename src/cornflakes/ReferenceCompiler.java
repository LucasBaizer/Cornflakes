package cornflakes;

import java.util.regex.Pattern;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class ReferenceCompiler implements GenericCompiler {
	private MethodData data;
	private Label label;
	private String referenceType;

	public ReferenceCompiler(Label label, MethodData data) {
		this.label = label;
		this.data = data;
	}

	@Override
	public int compile(ClassData data, MethodVisitor m, int num, String body, String[] lines) {
		String[] operandSplit = body.split(Pattern.quote("."));

		String part = operandSplit[0].trim();

		if (part.equals("this")) {
			if ((this.data.getModifiers() & ACC_STATIC) == ACC_STATIC) {
				throw new CompileError("Cannot access this from a static context");
			}

			if (operandSplit.length > 1) {
				part = operandSplit[1];
			}
		}

		if (Strings.hasMatching(part, '(', ')')) {
			String before = part.substring(0, part.indexOf('(')).trim();
			String pars = part.substring(part.indexOf('(') + 1, part.indexOf(')')).trim();
			String[] split = pars.isEmpty() ? new String[0] : pars.split(",");

			if (data.hasMethod(before)) {
				MethodData method = data.getMethodData(before);

				if (method.getParameters().size() != split.length) {
					throw new CompileError("Method " + method.getName() + " expects " + method.getParameters().size()
							+ " parameter" + (method.getParameters().size() == 1 ? "" : "s") + ", but " + split.length
							+ " " + (split.length == 1 ? "was" : "were") + " given");
				}

				for (String par : split) {
					String type = Types.getType(par, this.data.getReturnType().getSimpleName().toLowerCase());
					if (type != null) {
						m.visitLdcInsn(Types.parseLiteral(type, par));
					} else {
						//TODO
						/*
						m.visitLineNumber(num++, ret);
						ReferenceCompiler compiler = new ReferenceCompiler(this.data);
						num = compiler.compile(data, m, num, par, new String[] { par });

						String ref = Types.getTypeFromSignature(compiler.getReferenceType()).getSimpleName()
								.toLowerCase();

						int op = ARETURN;
						if (ref.equals("int")) {
							op = IRETURN;
						} else if (ref.equals("float")) {
							op = FRETURN;
						} else if (ref.equals("double")) {
							op = DRETURN;
						} else if (ref.equals("long")) {
							op = LRETURN;
						}

						m.visitInsn(op);*/
					}
				}

				if ((method.getModifiers() & ACC_STATIC) == ACC_STATIC) {
					m.visitMethodInsn(INVOKESTATIC, data.getClassName(), before, method.getSignature(), false);
				} else {
					if ((this.data.getModifiers() & ACC_STATIC) == ACC_STATIC) {
						throw new CompileError("Cannot access instance method from a static context");
					}
					m.visitMethodInsn(INVOKEVIRTUAL, data.getClassName(), before, method.getSignature(), false);
				}

				referenceType = method.getReturnTypeSignature();
			}
		}

		return num;
	}

	public String getReferenceType() {
		return referenceType;
	}

	public void setReferenceType(String referenceType) {
		this.referenceType = referenceType;
	}
}
