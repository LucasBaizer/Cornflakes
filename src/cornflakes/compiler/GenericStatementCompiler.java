package cornflakes.compiler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.asm.MethodVisitor;

import cornflakes.compiler.CompileUtils.VariableDeclaration;

public class GenericStatementCompiler implements GenericCompiler {
	public static final int RETURN = 1;
	public static final int VAR = 2;
	public static final int THROW = 3;
	public static final int SET_VAR = 4;
	public static final int YIELD = 5;

	private MethodData data;
	private int type;

	public GenericStatementCompiler(MethodData data) {
		this.data = data;
	}

	@Override
	public void compile(ClassData data, MethodVisitor m, Block block, String body, String[] lines) {
		if (body.startsWith("yield")) {
			type = YIELD;

			if (!this.data.isIterator()) {
				throw new CompileError("Cannot yield to a non-iterator method");
			}

			m.visitVarInsn(ALOAD, this.data.getIterator());
			String str = body.substring(5).trim();
			String type = Types.getType(str, null);
			if (type != null) {
				Object val = Types.parseLiteral(type, str);
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
				this.data.ics();
			} else {
				ExpressionCompiler exp = new ExpressionCompiler(true, this.data);
				exp.compile(data, m, block, str, new String[] { str });
			}

			m.visitMethodInsn(INVOKEVIRTUAL, "cornflakes/lang/YieldIterator", "yield", "(Ljava/lang/Object;)V", false);
		} else if (body.startsWith("return")) {
			type = RETURN;

			body = Strings.normalizeSpaces(body);

			String[] split = body.split(" ", 2);
			if (split.length == 2) {
				if (this.data.getReturnType().getTypeSignature().equals("V")) {
					throw new CompileError("Cannot return a value to a void function");
				}
				if (this.data.isIterator()) {
					throw new CompileError("Cannot return a value to an iterator function");
				}

				String par = split[1].trim();

				String type = Types.getType(par, this.data.getReturnType().getTypeName().toLowerCase());
				if (type != null) {
					if (type.equals("string")) {
						if (!this.data.getReturnType().getTypeSignature().equals("Ljava/lang/String;")) {
							throw new CompileError(
									"A return value of type " + Types.beautify(this.data.getReturnType().getTypeName())
											+ " is expected, but one of type string was given");
						}

						Object val = Types.parseLiteral(type, par);
						m.visitLdcInsn(val);
						m.visitInsn(ARETURN);

						this.data.ics();
					} else {
						if (!Types.isSuitable(this.data.getReturnType().getTypeSignature(),
								Types.getTypeSignature(type))) {
							throw new CompileError(
									"A return value of type " + Types.beautify(this.data.getReturnType().getTypeName())
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
						this.data.ics();

						int op = Types.getOpcode(Types.RETURN, type);
						m.visitInsn(op);
					}
				} else {
					ExpressionCompiler compiler = new ExpressionCompiler(true, this.data);
					compiler.compile(data, m, block, par, new String[] { par });

					if (!Types.isSuitable(this.data.getReturnType(), compiler.getReferenceType())) {
						throw new CompileError("A return value of type "
								+ Types.beautify(this.data.getReturnType().getTypeName())
								+ " is expected, but one of type "
								+ Types.beautify(compiler.getReferenceType().getTypeSignature()) + " was given");
					}

					int op = Types.getOpcode(Types.RETURN, compiler.getReferenceType().getTypeName());
					m.visitInsn(op);
				}
			} else {
				if (this.data.getReturnType().getTypeSignature().equals("V")) {
					m.visitInsn(RETURN);
				} else if (this.data.isIterator()) {
					m.visitVarInsn(ALOAD, this.data.getIterator());
					m.visitInsn(ARETURN);
				} else {
					throw new CompileError("A return value of type "
							+ Types.beautify(this.data.getReturnType().getTypeSignature()) + " is expected");
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

			ExpressionCompiler ref = new ExpressionCompiler(true, this.data);
			ref.compile(data, m, block, split[1], new String[] { split[1] });

			DefinitiveType signature = ref.getReferenceType();
			if (signature.isPrimitive()) {
				throw new CompileError("Only types which are subclasses of java.lang.Throwable can be thrown");
			}

			try {
				if (!signature.getObjectType().is("java/lang/Throwable")) {
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
			DefinitiveType valueType = decl.getValueType();
			DefinitiveType variableType = decl.getVariableType();

			int idx = this.data.getLocalVariables();
			String signature = null;
			if (decl.isGenericTyped()) {
				signature = variableType.getTypeSignature();
				signature = signature.substring(0, signature.length() - 1);
				signature += "<";
				for (GenericType type : decl.getGenericTypes()) {
					signature += Types.getTypeSignature(type.getType());
				}
				signature += ">;";
			}
			m.visitLocalVariable(variableName, variableType.getAbsoluteTypeSignature(), signature,
					block.getStartLabel(), block.getEndLabel(), idx);
			if (value != null) {
				int push = Types.getOpcode(Types.PUSH, valueType.getTypeSignature());
				int store = Types.getOpcode(Types.STORE, variableType.getTypeSignature());

				if (push == LDC) {
					m.visitLdcInsn(value);
					this.data.ics();
				} else if (value != null) {
					String toString = value.toString();
					if (toString.equals("true") || toString.equals("false")) {
						m.visitInsn(toString.equals("false") ? ICONST_0 : ICONST_1);
					} else {
						m.visitVarInsn(push, Integer.parseInt(toString));
						this.data.ics();
					}
				}

				m.visitVarInsn(store, idx);
			} else {
				if (valueType == null || valueType.isNull()) {
					if (variableType.equals("B") || variableType.equals("S") || variableType.equals("I")
							|| variableType.equals("C") || variableType.equals("B")) {
						m.visitInsn(ICONST_0);
					} else if (variableType.equals("J")) {
						m.visitInsn(LCONST_0);
					} else if (variableType.equals("F")) {
						m.visitInsn(FCONST_0);
					} else if (variableType.equals("D")) {
						m.visitInsn(DCONST_0);
					} else {
						m.visitInsn(ACONST_NULL);
					}

					this.data.ics();
				}
				m.visitVarInsn(Types.getOpcode(Types.STORE, variableType.getTypeSignature()), idx);
			}

			LocalData local = new LocalData(variableName, variableType, block, idx,
					body.startsWith("var") ? 0 : ACC_FINAL);
			if (decl.isGenericTyped()) {
				local.setGeneric(true);
				local.setGenericTypes(decl.getGenericTypes());
			}
			this.data.addLocal(local);
			this.data.addLocalVariable();
		} else {
			boolean ref = true;

			if (body.contains("=")) {
				type = SET_VAR;
				String[] split = body.split("=", 2);
				String name = split[0].trim();
				String value = split[1].trim();

				ExpressionCompiler compiler = null;
				try {
					compiler = new ExpressionCompiler(true, this.data);
					compiler.setLoadVariableReference(false);
					compiler.setAllowImplicitGetters(false);
					compiler.compile(data, m, block, name, new String[] { name });
				} catch (CompileError e) {
					compiler = new ExpressionCompiler(true, this.data);
					compiler.setLoadVariableReference(false);

					String total = null;
					if (Strings.contains(name, ".")) {
						String old = name;
						name = name.substring(name.lastIndexOf('.') + 1);
						total = old.substring(0, old.lastIndexOf('.'));
					}
					String to = "set" + Strings.capitalize(name) + "(" + value + ")";
					String end = total == null ? to : total + "." + to;

					try {
						compiler.compile(data, m, block, end, new String[] { end });
					} catch (CompileError e2) {
						throw e;
					}

					return;
				}

				String refName = compiler.getReferenceName();
				FieldData field = compiler.getField();

				String regex = "(.+?)\\[(.+?)\\]";
				if (name.matches(regex)) {
					Matcher x = Pattern.compile(regex).matcher(name);
					x.find();
					String arrayName = x.group(1);
					String arrayIndex = x.group(2);
					String idxType = Types.getType(arrayIndex, null);
					DefinitiveType type = field.getType();
					ClassData typeClass = type.getObjectType();
					DefinitiveType tupleType = null;
					MethodData indexer = typeClass != null && typeClass.isSetIndexedClass()
							? typeClass.getMethods("_set_index_")[0] : null;

					ExpressionCompiler array = new ExpressionCompiler(true, this.data);
					array.compile(data, m, block, arrayName, new String[] { arrayName });

					if (idxType == null) {
						ExpressionCompiler exp = new ExpressionCompiler(true, this.data);
						exp.compile(data, m, block, arrayIndex, new String[] { arrayIndex });

						try {
							if (typeClass.is("java.util.List") || type.getTypeSignature().startsWith("[")) {
								if (!exp.getReferenceType().equals("I")) {
									throw new CompileError("Arrays can only be indexed by integers");
								}
							} else if (type.equals("Lcornflakes/lang/Tuple")) {
								throw new CompileError("Tuple can only be indexed by integer literals");
							}
						} catch (ClassNotFoundException e) {
							throw new CompileError(e);
						}
					} else {
						if (idxType.equals("string")) {
							m.visitLdcInsn(Types.parseLiteral("string", arrayIndex));
						} else {
							int y = Integer.parseInt(arrayIndex);
							if (y < 0) {
								throw new CompileError("Array literal indexes must be greater than or equal to 0");
							}
							m.visitLdcInsn(y);
							if (type.isTuple()) {
								TupleClassData clz = (TupleClassData) typeClass;
								if (y >= clz.getTypes().length) {
									throw new CompileError("Tuple index out of range");
								}
								tupleType = clz.getType(y);
							}
						}
					}

					pushValue(value, field, compiler, refName, m, block, data, true);

					try {
						if (typeClass.isSetIndexedClass()) {
							m.visitMethodInsn(INVOKEVIRTUAL, typeClass.getClassName(), "_set_index_",
									indexer.getSignature(), false);
						} else if (type.equals("Ljava/lang/String;")) {
							m.visitMethodInsn(INVOKESTATIC, "cornflakes/lang/StringUtility", "replaceChar",
									"(Ljava/lang/String;IC)Ljava/lang/String;", false);
							storeVariable(field, refName, compiler, m);
						} else if (type.isTuple()) {
							if (tupleType.isPrimitive()) {
								m.visitMethodInsn(INVOKEVIRTUAL, "cornflakes/lang/Tuple", "item",
										"(I" + tupleType.getAbsoluteTypeSignature() + ")V", false);
							} else {
								m.visitMethodInsn(INVOKEVIRTUAL, "cornflakes/lang/Tuple", "item",
										"(ILjava/lang/Object;)V", false);
							}
						} else if (typeClass.is("java.util.List")) {
							m.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "set",
									"(ILjava/lang/Object;)Ljava/lang/Object;", true);
						} else if (typeClass.is("java.util.Map")) {
							m.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put",
									"(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
						} else if (type.isArray()) {
							m.visitInsn(Types.getArrayOpcode(Types.STORE, type.getTypeSignature()));
						} else {
							throw new CompileError("Cannot index a non-indexable object");
						}

						return;
					} catch (ClassNotFoundException e) {
						throw new CompileError(e);
					}
				} else {
					if (field != null) {
						ref = false;

						pushValue(value, field, compiler, refName, m, block, data, false);
						storeVariable(field, refName, compiler, m);
					}
				}
			}

			if (ref) {
				new ExpressionCompiler(true, this.data).compile(data, m, block, body, lines);
			}
		}
	}

	private void pushValue(String value, FieldData field, ExpressionCompiler compiler, String refName, MethodVisitor m,
			Block block, ClassData data, boolean array) {
		String valueType = Types.getType(value, field.getType().getTypeSignature());
		if (valueType != null) {
			if (!array && !Types.isSuitable(field.getType().getTypeSignature(), Types.getTypeSignature(valueType))) {
				throw new CompileError(Types.beautify(valueType) + " is not assignable to "
						+ Types.beautify(field.getType().getTypeSignature()));
			}

			Object obj = Types.parseLiteral(valueType, value);

			int push = Types.getOpcode(Types.PUSH, valueType);

			if (push == LDC) {
				m.visitLdcInsn(obj);
			} else {
				if (valueType.equals("bool")) {
					if (value.equals("true")) {
						m.visitInsn(ICONST_1);
					} else {
						m.visitInsn(ICONST_0);
					}
				} else if (valueType.equals("char")) {
					m.visitVarInsn(push, (int) (char) obj);
				} else {
					m.visitVarInsn(push, Integer.parseInt(obj.toString()));
				}
			}

			this.data.ics();
		} else {
			ExpressionCompiler compiler1 = new ExpressionCompiler(true, this.data);
			compiler1.compile(data, m, block, value, new String[] { value });

			if (!array && !Types.isSuitable(field.getType(), compiler1.getReferenceType())) {
				throw new CompileError(compiler1.getReferenceType() + " is not assignable to " + field.getType());
			}
		}
	}

	private void storeVariable(FieldData field, String refName, ExpressionCompiler compiler, MethodVisitor m) {
		if (field instanceof LocalData) {
			int store = Types.getOpcode(Types.STORE, field.getType().getTypeSignature());
			m.visitVarInsn(store, ((LocalData) field).getIndex());
		} else if (field instanceof FieldData) {
			m.visitFieldInsn(field.hasModifier(ACC_STATIC) ? PUTSTATIC : PUTFIELD,
					compiler.getReferenceOwner().getClassName(), refName,
					compiler.getReferenceType().getAbsoluteTypeSignature());
		}
	}

	public int getType() {
		return type;
	}
}
