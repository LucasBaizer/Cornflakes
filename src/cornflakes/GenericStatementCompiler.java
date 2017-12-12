package cornflakes;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class GenericStatementCompiler implements GenericCompiler {
	private MethodData data;

	public GenericStatementCompiler(MethodData data) {
		this.data = data;
	}

	@Override
	public int compile(ClassData data, MethodVisitor m, int num, String body, String[] lines) {
		if (body.startsWith("return")) {
			body = Strings.normalizeSpaces(body);

			Label ret = new Label();

			m.visitLabel(ret);

			String[] split = body.split(" ", 2);
			if (split.length == 2) {
				String par = split[1].trim();

				String type = Types.getType(par, this.data.getReturnType().getSimpleName().toLowerCase());
				if (type != null) {
					if (type.equals("string")) {
						if (!this.data.getReturnTypeSignature().equals("Ljava/lang/String;")) {
							throw new CompileError("A return value of type " + this.data.getReturnType().getSimpleName()
									+ " is expected, but one of type string was given");
						}

						Object val = Types.parseLiteral(type, par);
						m.visitLineNumber(num++, ret);
						m.visitLdcInsn(val);
						m.visitInsn(ARETURN);
					} else {
						if (!this.data.getReturnTypeSignature().equals(Types.getTypeSignature(type))) {
							throw new CompileError("A return value of type " + this.data.getReturnType().getSimpleName()
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
					new ReferenceCompiler(this.data).compile(data, m, num, body, lines);
					m.visitInsn(ARETURN);
				}
			} else {
				if (this.data.getReturnTypeSignature().equals("V")) {
					m.visitLineNumber(num++, ret);
					m.visitInsn(RETURN);
				} else {
					throw new CompileError("A return value of type " + this.data.getReturnType() + " is expected");
				}
			}
		}
		return num;
	}
}
