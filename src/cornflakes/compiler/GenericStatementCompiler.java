package cornflakes.compiler;

import org.objectweb.asm.MethodVisitor;

import cornflakes.compiler.CompileUtils.VariableDeclaration;

public class GenericStatementCompiler implements GenericCompiler {
	public static final int RETURN = 1;
	public static final int VAR = 2;
	public static final int THROW = 3;

	private MethodData data;
	private int type;

	public GenericStatementCompiler(MethodData data) {
		this.data = data;
	}

	@Override
	public void compile(ClassData data, MethodVisitor m, Block block, String body, String[] lines) {
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
							String toString = val.toString();

							if (toString.equals("true") || toString.equals("false")) {
								m.visitInsn(toString.equals("false") ? ICONST_0 : ICONST_1);
							} else {
								m.visitVarInsn(push, Integer.parseInt(val.toString()));
							}
						}
						this.data.increaseStackSize();

						int op = Types.getOpcode(Types.RETURN, type);
						m.visitInsn(op);
					}
				} else {
					ReferenceCompiler compiler = new ReferenceCompiler(true, this.data);
					compiler.compile(data, m, block, par, new String[] { par });

					String ref = Types.getTypeFromSignature(Types.unpadSignature(compiler.getReferenceSignature()))
							.getSimpleClassName();

					if (!Types.isSuitable(this.data.getReturnTypeSignature(), compiler.getReferenceSignature())) {
						throw new CompileError("A return value of type "
								+ this.data.getReturnType().getSimpleClassName() + " is expected, but one of type "
								+ Types.getTypeFromSignature(compiler.getReferenceSignature()).getSimpleClassName()
								+ " was given");
					}

					int op = Types.getOpcode(Types.RETURN, ref);
					m.visitInsn(op);
				}
			} else {
				if (this.data.getReturnTypeSignature().equals("V")) {
					m.visitInsn(RETURN);
				} else {
					throw new CompileError("A return value of type " + this.data.getReturnType() + " is expected");
				}
			}
		} else if (body.startsWith("throw")) {
			type = THROW;
			String[] split = body.split(" ");
			if (split.length == 1) {
				throw new CompileError("Expecting statement after token 'throw'");
			} else if (split.length > 2) {
				throw new CompileError("Unexpected symbol: " + split[2]);
			}

			ReferenceCompiler ref = new ReferenceCompiler(true, this.data);
			ref.compile(data, m, block, split[1], new String[] { split[1] });

			String signature = ref.getReferenceSignature();
			if (Types.isPrimitive(signature)) {
				throw new CompileError("Only types which are subclasses of java.lang.Throwable can be thrown");
			}

			try {
				ClassData classData = ClassData.forName(signature);
				if (!classData.is("java/lang/Throwable")) {
					throw new CompileError("Only types which are subclasses of java.lang.Throwable can be thrown");
				}
				m.visitInsn(ATHROW);
				block.setDoesThrow(true);
			} catch (ClassNotFoundException e) {
				throw new CompileError(e);
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

			if (this.data.hasLocal(variableName, block)) {
				throw new CompileError("Duplicate variable: " + variableName);
			}

			VariableDeclaration decl = CompileUtils.declareVariable(this.data, data, m, block, body, split);
			Object value = decl.getValue();
			String valueType = decl.getValueType();
			String variableType = decl.getVariableType();

			int idx = this.data.getLocalVariables();
			m.visitLocalVariable(variableName, variableType, null, block.getStartLabel(), block.getEndLabel(), idx);
			if (value != null || valueType.length() == 1) {
				int push = Types.getOpcode(Types.PUSH, valueType);
				int store = Types.getOpcode(Types.STORE, variableType);

				if (push == LDC) {
					m.visitLdcInsn(value);
					this.data.increaseStackSize();
				} else if (value != null) {
					String toString = value.toString();
					if (toString.equals("true") || toString.equals("false")) {
						m.visitInsn(toString.equals("false") ? ICONST_0 : ICONST_1);
					} else {
						m.visitVarInsn(push, Integer.parseInt(toString));
						this.data.increaseStackSize();
					}
				}

				m.visitVarInsn(store, idx);
			} else {
				if (decl.isReference()) {
					m.visitVarInsn(Types.getOpcode(Types.STORE, valueType), idx);
				}
			}

			this.data.addLocal(
					new LocalData(variableName, variableType, block, idx, body.startsWith("var") ? 0 : ACC_FINAL));
			this.data.addLocalVariable();
		} else {
			boolean ref = true;

			if (body.contains("=")) {
				String[] split = body.split("=", 2);
				String name = split[0].trim();
				String value = split[1].trim();

				ReferenceCompiler compiler = new ReferenceCompiler(true, this.data);
				compiler.setLoadVariableReference(false);
				compiler.compile(data, m, block, name, new String[] { name });

				String refName = compiler.getReferenceName();
				FieldData field = null;
				if (this.data.hasLocal(refName, block)) {
					field = this.data.getLocal(refName, block);
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
					} else {
						ReferenceCompiler compiler1 = new ReferenceCompiler(true, this.data);
						compiler1.compile(data, m, block, value, new String[] { body });

						if (!Types.isSuitable(field.getType(), compiler1.getReferenceSignature())) {
							throw new CompileError(
									compiler1.getReferenceSignature() + " is not assignable to " + field.getType());
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
				new ReferenceCompiler(true, this.data).compile(data, m, block, body, lines);
			}
		}
	}

	public int getType() {
		return type;
	}
}
