package cornflakes;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class GenericStatementCompiler implements GenericCompiler {
	public static final int RETURN = 1;

	private MethodData data;
	private int type;

	public GenericStatementCompiler(MethodData data) {
		this.data = data;
	}

	@Override
	public int compile(ClassData data, MethodVisitor m, int num, String body, String[] lines) {
		if (body.startsWith("return")) {
			type = RETURN;

			body = Strings.normalizeSpaces(body);

			Label ret = new Label();

			m.visitLabel(ret);

			String[] split = body.split(" ", 2);
			if (split.length == 2) {
				if (this.data.getReturnTypeSignature().equals("V")) {
					throw new CompileError("Cannot return a value to a void function");
				}

				String par = split[1].trim();

				String type = Types.getType(par, this.data.getReturnType().getSimpleClassName().toLowerCase());
				if (type != null) {
					if (type.equals("string")) {
						if (!this.data.getReturnTypeSignature().equals("Ljava/lang/String;")) {
							throw new CompileError(
									"A return value of type " + this.data.getReturnType().getSimpleClassName()
											+ " is expected, but one of type string was given");
						}

						Object val = Types.parseLiteral(type, par);
						m.visitLineNumber(num++, ret);
						m.visitLdcInsn(val);
						m.visitInsn(ARETURN);
					} else {
						if (!Types.isSuitable(this.data.getReturnTypeSignature(), Types.getTypeSignature(type))) {
							throw new CompileError(
									"A return value of type " + this.data.getReturnType().getSimpleClassName()
											+ " is expected, but one of type " + type + " was given");
						}

						Object val = Types.parseLiteral(type, par);
						m.visitLineNumber(num++, ret);
						m.visitLdcInsn(val);

						int op = 0;
						if (type.equals("int")) {
							op = IRETURN;
						} else if (type.equals("float")) {
							op = FRETURN;
						} else if (type.equals("double")) {
							op = DRETURN;
						} else if (type.equals("long")) {
							op = LRETURN;
						}

						m.visitInsn(op);
						num++;
					}
				} else {
					m.visitLineNumber(num++, ret);
					ReferenceCompiler compiler = new ReferenceCompiler(ret, this.data);
					num = compiler.compile(data, m, num, par, new String[] { par });

					String ref = Types.getTypeFromSignature(Types.unpadSignature(compiler.getReferenceType()))
							.getSimpleClassName().toLowerCase();

					if (!Types.isSuitable(this.data.getReturnTypeSignature(), compiler.getReferenceType())) {
						throw new CompileError("A return value of type "
								+ this.data.getReturnType().getSimpleClassName() + " is expected, but one of type "
								+ Types.getTypeFromSignature(compiler.getReferenceType()).getSimpleClassName()
								+ " was given");
					}

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

					// TODO check return type here

					m.visitInsn(op);
				}
			} else {
				if (this.data.getReturnTypeSignature().equals("V")) {
					m.visitLineNumber(num++, ret);
					m.visitInsn(RETURN);
				} else {
					throw new CompileError("A return value of type " + this.data.getReturnType() + " is expected");
				}
			}
		} else {
			Label label = new Label();
			m.visitLabel(label);
			m.visitLineNumber(num++, label);

			new ReferenceCompiler(label, this.data).compile(data, m, num, body, lines);
		}
		return num;
	}

	public int getType() {
		return type;
	}
}
