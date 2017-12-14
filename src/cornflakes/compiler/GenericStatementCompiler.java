package cornflakes.compiler;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class GenericStatementCompiler implements GenericCompiler {
	public static final int RETURN = 1;
	public static final int LET = 2;

	private MethodData data;
	private int type;

	public GenericStatementCompiler(MethodData data) {
		this.data = data;
	}

	@Override
	public int compile(ClassData data, MethodVisitor m, Label start, Label end, int num, String body, String[] lines) {
		if (body.startsWith("return")) {
			type = RETURN;

			body = Strings.normalizeSpaces(body);

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
						m.visitLdcInsn(val);
						m.visitInsn(ARETURN);
					} else {
						if (!Types.isSuitable(this.data.getReturnTypeSignature(), Types.getTypeSignature(type))) {
							throw new CompileError(
									"A return value of type " + this.data.getReturnType().getSimpleClassName()
											+ " is expected, but one of type " + type + " was given");
						}

						Object val = Types.parseLiteral(type, par);
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
					ReferenceCompiler compiler = new ReferenceCompiler(true, this.data);
					num = compiler.compile(data, m, start, end, num, par, new String[] { par });

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
					m.visitInsn(RETURN);
				} else {
					throw new CompileError("A return value of type " + this.data.getReturnType() + " is expected");
				}
			}
		} else if (body.startsWith("let")) {
			type = LET;

			body = Strings.normalizeSpaces(body);

			String[] split = body.split(":");
			String[] look = split.length == 1 ? body.split(" ") : split[0].split(" ");
			if (look.length == 1) {
				throw new CompileError("Expecting variable name after let");
			}

			String variableName = look[1].trim();
			String type = null;
			Object value = null;
			if (split.length == 1) {
				String[] set = body.split("=");
				if (set.length == 1) {
					throw new CompileError("A variable with an unspecified type must have an initial value");
				}

				String givenValue = set[1].trim();
				String valType = Types.getType(givenValue, "");
				if (valType == null) {
					throw new CompileError("A type for the variable could not be assumed; one must be assigned");
				}
				value = Types.parseLiteral(valType, givenValue);
			} else {
				String[] spaces = split[1].trim().split(" ");
				type = spaces[0];

				String[] set = body.split("=");
				if (set.length > 1) {
					String givenValue = set[1].trim();

					String inputType = Types.getType(givenValue, type);
					if (!Types.isSuitable(type, inputType)) {
						throw new CompileError(inputType + " is not assignable to " + type);
					}

					value = Types.parseLiteral(type, givenValue);
				}

				type = Types.getTypeSignature(type);
			}

			int idx = this.data.getLocalVariables();
			m.visitLocalVariable(variableName, type, null, start, end, idx);
			if (value != null) {
				m.visitLdcInsn(value);
				m.visitVarInsn(ASTORE, idx);
			}

			this.data.addLocal(new LocalData(variableName, type, start, end, idx, 0));
			this.data.addLocalVariable();
		} else {
			num = new ReferenceCompiler(true, this.data).compile(data, m, start, end, num, body, lines);
		}

		return num + 1;
	}

	public int getType() {
		return type;
	}
}
