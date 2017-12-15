package cornflakes.compiler;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class GenericStatementCompiler implements GenericCompiler {
	public static final int RETURN = 1;
	public static final int VAR = 2;

	private MethodData data;
	private int type;

	public GenericStatementCompiler(MethodData data) {
		this.data = data;
	}

	@Override
	public void compile(ClassData data, MethodVisitor m, Label start, Label end, String body, String[] lines) {
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

						this.data.increaseStackSize();
					} else {
						if (!Types.isSuitable(this.data.getReturnTypeSignature(), Types.getTypeSignature(type))) {
							throw new CompileError(
									"A return value of type " + this.data.getReturnType().getSimpleClassName()
											+ " is expected, but one of type " + type + " was given");
						}

						Object val = Types.parseLiteral(type, par);
						int push = Types.getOpcode(Types.PUSH, type);
						if (push == LDC) {
							m.visitLdcInsn(val);
						} else {
							m.visitVarInsn(push, Integer.parseInt(val.toString()));
						}
						this.data.increaseStackSize();

						int op = Types.getOpcode(Types.RETURN, type);
						m.visitInsn(op);
					}
				} else {
					ReferenceCompiler compiler = new ReferenceCompiler(true, this.data);
					compiler.compile(data, m, start, end, par, new String[] { par });

					String ref = Types.getTypeFromSignature(Types.unpadSignature(compiler.getReferenceSignature()))
							.getSimpleClassName().toLowerCase();

					if (!Types.isSuitable(this.data.getReturnTypeSignature(), compiler.getReferenceSignature())) {
						throw new CompileError("A return value of type "
								+ this.data.getReturnType().getSimpleClassName() + " is expected, but one of type "
								+ Types.getTypeFromSignature(compiler.getReferenceSignature()).getSimpleClassName()
								+ " was given");
					}

					int op = Types.getOpcode(RETURN, ref);
					m.visitInsn(op);
				}
			} else {
				if (this.data.getReturnTypeSignature().equals("V")) {
					m.visitInsn(RETURN);
				} else {
					throw new CompileError("A return value of type " + this.data.getReturnType() + " is expected");
				}
			}
		} else if (body.startsWith("var") || body.startsWith("const")) {
			type = VAR;

			body = Strings.normalizeSpaces(body);

			String[] split = body.split(":");
			String[] look = split.length == 1 ? body.split(" ") : split[0].split(" ");
			if (look.length == 1) {
				throw new CompileError("Expecting variable name");
			}

			String variableName = look[1].trim();

			if (this.data.hasLocal(variableName, start, end)) {
				throw new CompileError("Duplicate variable: " + variableName);
			}

			String variableType = null;
			Object value = null;
			String valueType = null;
			boolean isRef = false;

			if (split.length == 1) {
				String[] set = body.split("=");
				if (set.length == 1) {
					throw new CompileError("A variable with an unspecified type must have an initial value");
				}

				String givenValue = set[1].trim();
				valueType = Types.getType(givenValue, "");
				if (valueType == null) {
					ReferenceCompiler ref = new ReferenceCompiler(true, this.data);
					ref.compile(data, m, start, end, givenValue, new String[] { givenValue });

					if ((valueType = ref.getReferenceSignature()) == null) {
						throw new CompileError("A type for the variable could not be assumed; one must be assigned");
					}

					isRef = true;
				} else {
					value = Types.parseLiteral(valueType, givenValue);
				}

				variableType = Types.getTypeSignature(valueType);
			} else {
				String[] spaces = split[1].trim().split(" ");
				variableType = spaces[0];

				if (!Types.isPrimitive(variableType)) {
					variableType = data.resolveClass(variableType);
				}

				String[] set = body.split("=");
				if (set.length > 1) {
					String givenValue = set[1].trim();

					valueType = Types.getType(givenValue, variableType);

					if (valueType == null) {
						ReferenceCompiler compiler = new ReferenceCompiler(true, this.data);
						compiler.compile(data, m, start, end, givenValue, new String[] { givenValue });
						valueType = compiler.getReferenceSignature();
						isRef = true;
					}

					if (!Types.isSuitable(variableType, valueType)) {
						throw new CompileError(valueType + " is not assignable to " + variableType);
					}

					if (!isRef) {
						value = Types.parseLiteral(variableType, givenValue);
					}
				}

				variableType = Types.getTypeSignature(variableType);
			}

			int idx = this.data.getLocalVariables();
			m.visitLocalVariable(variableName, variableType, null, start, end, idx);
			if (value != null) {
				int push = Types.getOpcode(Types.PUSH, valueType);
				int store = Types.getOpcode(Types.STORE, variableType);

				if (push == LDC) {
					m.visitLdcInsn(value);
					this.data.increaseStackSize();
				} else {
					m.visitVarInsn(push, Integer.parseInt(value.toString()));
					this.data.increaseStackSize();
				}

				m.visitVarInsn(store, idx);
			} else {
				if (isRef) {
					m.visitVarInsn(Types.getOpcode(Types.STORE, valueType), idx);
				}
			}

			this.data.addLocal(
					new LocalData(variableName, variableType, start, end, idx, body.startsWith("var") ? 0 : ACC_FINAL));
			this.data.addLocalVariable();
		} else {
			boolean ref = true;

			if (body.contains("=")) {
				String[] split = body.split("=", 2);
				String name = split[0].trim();
				String value = split[1].trim();

				ReferenceCompiler compiler = new ReferenceCompiler(true, this.data);
				compiler.setLoadVariableReference(false);
				compiler.compile(data, m, start, end, name, new String[] { name });

				String refName = compiler.getReferenceName();
				FieldData field = null;
				if (this.data.hasLocal(refName, start, end)) {
					field = this.data.getLocal(refName, start, end);
				} else if (compiler.getReferenceOwner().hasField(refName)) {
					field = compiler.getReferenceOwner().getField(refName);
				}

				if (field != null) {
					ref = false;

					String valueType = Types.getType(value, field.getType());
					if (valueType != null) {
						if (!Types.isSuitable(field.getType(), Types.getTypeSignature(valueType))) {
							throw new CompileError(valueType + " is not assignable to " + field.getType());
						}

						Object obj = Types.parseLiteral(valueType, value);

						int push = Types.getOpcode(Types.PUSH, valueType);

						if (push == LDC) {
							m.visitLdcInsn(obj);
							this.data.increaseStackSize();
						} else {
							m.visitVarInsn(push, Integer.parseInt(obj.toString()));
							this.data.increaseStackSize();
						}

						if (field instanceof LocalData) {
							int store = Types.getOpcode(Types.STORE, field.getType());
							m.visitVarInsn(store, ((LocalData) field).getIndex());
						} else if (field instanceof FieldData) {
							m.visitFieldInsn(field.hasModifier(ACC_STATIC) ? PUTSTATIC : PUTFIELD,
									compiler.getReferenceOwner().getClassName(), refName,
									compiler.getReferenceSignature());
						}
					}
				}
			}

			if (ref) {
				new ReferenceCompiler(true, this.data).compile(data, m, start, end, body, lines);
			}
		}
	}

	public int getType() {
		return type;
	}
}
