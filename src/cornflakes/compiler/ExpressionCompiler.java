package cornflakes.compiler;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import cornflakes.lang.Tuple;

public class ExpressionCompiler implements GenericCompiler {
	public static final int LOCAL_VARIABLE = 0;
	public static final int MEMBER_VARIABLE = 1;
	public static final int METHOD = 2;
	public static final int CONSTRUCTOR = 3;
	public static final int THIS = 4;
	public static final int BOOLEAN_EXPRESSION = 5;
	public static final int MATH_EXPRESSION = 6;
	public static final int NEW_ARRAY = 7;
	public static final int TYPEOF = 8;
	public static final int CAST = 9;
	public static final int TUPLE = 9;

	private MethodData data;
	private boolean write;
	private boolean loadVariableReference = true;
	private DefinitiveType resultType;
	private String resultName;
	private ClassData resultOwner;
	private int expressionType;
	private boolean thisType;
	private boolean allowBoolean = true;
	private boolean allowMath = true;
	private boolean math;
	private GenericCompiler source;
	private FieldData field;
	private List<GenericType> genericTypes = new ArrayList<>();
	private boolean allowImplicitGetters = true;

	public ExpressionCompiler(boolean write, MethodData data) {
		this.write = write;
		this.data = data;
	}

	@Override
	public void compile(ClassData data, MethodVisitor m, Block block, String body, String[] lines) {
		try {
			compile(null, data, data, m, block, body, lines);
		} catch (ClassNotFoundException e) {
			throw new CompileError(e);
		}
	}

	private void compile(ExpressionCompiler last, ClassData containerData, ClassData data, MethodVisitor m, Block block,
			String body, String[] lines) throws ClassNotFoundException {
		if (body.equals("null")) {
			m.visitInsn(ACONST_NULL);
			return;
		}

		int end = body.length();
		int opens = 0;
		int brack = 0;
		for (int i = 0; i < body.length(); i++) {
			char c = body.charAt(i);
			if (c == '(') {
				opens++;
			} else if (c == ')') {
				opens--;
			} else if (c == '[') {
				brack++;
			} else if (c == ']') {
				brack--;
			}
			if (opens == 0 && brack == 0) {
				if (c == '.') {
					if (!(body.charAt(i + 1) >= '0' && body.charAt(i + 1) <= '9')) {
						end = i;
						break;
					}
				}
			}
		}
		String part = body.substring(0, end).trim();

		boolean next = false;
		if (part.equals("this")) {
			if (this.data.hasModifier(ACC_STATIC)) {
				throw new CompileError("Cannot access this from a static context");
			}
			if (block instanceof ConstructorBlock) {
				ConstructorBlock cblock = (ConstructorBlock) block;
				if (!cblock.hasCalledSuper()) {
					throw new CompileError("Cannot reference this until super has been called");
				}
			}
			if (write) {
				m.visitVarInsn(ALOAD, 0);
				this.data.ics();
			}

			thisType = true;
			resultName = "this";
			resultOwner = data;
			resultType = DefinitiveType.assume(Types.getTypeSignature(data.getClassName()));
			expressionType = THIS;
			next = true;
		} else if (Types.isTupleDefinition(part)) {
			String[] tuple = Strings.splitParameters(part.substring(1, part.length() - 1).trim());

			int idx = 16384 + this.data.getSyntheticVariables();
			if (write) {
				m.visitLocalVariable("_tuple_init_" + idx, "Lcornflakes/lang/Tuple;", null, block.getStartLabel(),
						block.getEndLabel(), idx);

				m.visitTypeInsn(NEW, "cornflakes/lang/Tuple");
				m.visitInsn(DUP);
				m.visitLdcInsn(tuple.length);
				m.visitLdcInsn(tuple.length);
				m.visitIntInsn(NEWARRAY, T_INT);
				int idx2 = 16385 + this.data.getSyntheticVariables();
				m.visitLocalVariable("_tuple_init_typearr_" + idx2, "[I", null, block.getStartLabel(),
						block.getEndLabel(), idx2);
				m.visitVarInsn(ASTORE, idx2);

				for (int i = 0; i < tuple.length; i++) {
					m.visitVarInsn(ALOAD, idx2);
					m.visitLdcInsn(i);

					String type = Types.getType(tuple[i].trim(), null);
					int t = -1;
					if (type == null || type.equals("string")) {
						t = Tuple.OBJECT;
					} else {
						if (type.equals("i8")) {
							t = Tuple.I8;
						} else if (type.equals("i16")) {
							t = Tuple.I16;
						} else if (type.equals("i32")) {
							t = Tuple.I32;
						} else if (type.equals("i64")) {
							t = Tuple.I64;
						} else if (type.equals("f32")) {
							t = Tuple.F32;
						} else if (type.equals("f64")) {
							t = Tuple.F64;
						} else if (type.equals("bool")) {
							t = Tuple.BOOL;
						} else if (type.equals("char")) {
							t = Tuple.CHAR;
						}
					}

					m.visitLdcInsn(t);
				}

				m.visitMethodInsn(INVOKESPECIAL, "cornflakes/lang/Tuple", "<init>", "(I[I)V", false);
				m.visitVarInsn(ASTORE, idx);
				this.data.addSyntheticVariable();
				this.data.addSyntheticVariable();
			}

			String[] types = new String[tuple.length];
			for (int i = 0; i < tuple.length; i++) {
				String par = tuple[i].trim();
				String type = Types.getType(par, null);
				if (write) {
					m.visitVarInsn(ALOAD, idx);
					m.visitLdcInsn(i);
				}
				if (type == null) {
					ExpressionCompiler exp = new ExpressionCompiler(this.write, this.data);
					exp.compile(data, m, block, par, new String[] { par });

					String sig = exp.getResultType().getAbsoluteTypeSignature();
					if (write) {
						if (Types.isPrimitive(sig)) {
							m.visitMethodInsn(INVOKEVIRTUAL, "cornflakes/lang/Tuple", "item", "(I" + sig + ")V", false);
						} else {
							m.visitMethodInsn(INVOKEVIRTUAL, "cornflakes/lang/Tuple", "item", "(ILjava/lang/Object;)V",
									false);
						}
					}

					types[i] = Types.primitiveToCornflakes(exp.getResultType().getTypeSignature());
				} else {
					Object value = Types.parseLiteral(type, par);
					int push = Types.getOpcode(Types.PUSH, type);

					if (write) {
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
					}

					String sig = Types.getTypeSignature(type);
					types[i] = Types.primitiveToCornflakes(sig);
					if (write) {
						if (Types.isPrimitive(sig)) {
							m.visitMethodInsn(INVOKEVIRTUAL, "cornflakes/lang/Tuple", "item", "(I" + sig + ")V", false);
						} else {
							m.visitMethodInsn(INVOKEVIRTUAL, "cornflakes/lang/Tuple", "item", "(ILjava/lang/Object;)V",
									false);
						}
					}
				}
			}

			if (write) {
				m.visitVarInsn(ALOAD, idx);
			}

			resultName = part;
			resultOwner = ClassData.forName("cornflakes/lang/Tuple");

			String str = "(";
			for (int i = 0; i < types.length; i++) {
				str += types[i];
				if (i < types.length - 1) {
					str += ", ";
				}
			}
			str += ")";
			resultType = DefinitiveType.object(str);
			expressionType = TUPLE;

			next = true;
		} else if (Strings.contains(part, " as ")) {
			String[] split = Strings.split(part, " as ");
			String val = split[0].trim();
			DefinitiveType cls = data.resolveClass(split[1].trim());

			String type = Types.getType(val, cls.getTypeName());
			String cast = null;
			boolean prim = false;
			if (type == null) {
				ExpressionCompiler sub = new ExpressionCompiler(this.write, this.data);
				sub.compile(data, m, block, val, new String[] { val });
				cast = sub.getResultType().getTypeSignature();
				prim = sub.isPrimitiveResult();
			} else {
				cast = type;
				prim = true;

				if (this.write) {
					Object obj = Types.parseLiteral(type, val);

					int push = Types.getOpcode(Types.PUSH, type);
					if (push == LDC) {
						m.visitLdcInsn(obj);
					} else {
						String toString = obj.toString();

						if (toString.equals("true") || toString.equals("false")) {
							m.visitInsn(toString.equals("false") ? ICONST_0 : ICONST_1);
						} else {
							m.visitVarInsn(push, Integer.parseInt(obj.toString()));
						}
					}
					this.data.ics();
				}
			}

			if (write) {
				if (prim) {
					if (cast.equals("i32")) {
						if (cls.equals("B")) {
							m.visitInsn(I2B);
						} else if (cls.equals("C")) {
							m.visitInsn(I2C);
						} else if (cls.equals("S")) {
							m.visitInsn(I2S);
						} else if (cls.equals("F")) {
							m.visitInsn(I2F);
						} else if (cls.equals("D")) {
							m.visitInsn(I2D);
						} else if (cls.equals("L")) {
							m.visitInsn(I2L);
						}
					} else if (cast.equals("i64")) {
						if (cls.equals("F")) {
							m.visitInsn(L2F);
						} else if (cls.equals("D")) {
							m.visitInsn(L2D);
						} else if (cls.equals("I")) {
							m.visitInsn(L2I);
						} else if (cls.equals("C")) {
							m.visitInsn(L2I);
							m.visitInsn(I2C);
						} else if (cls.equals("B")) {
							m.visitInsn(L2I);
							m.visitInsn(I2B);
						} else if (cls.equals("S")) {
							m.visitInsn(L2I);
							m.visitInsn(I2S);
						}
					} else if (cast.equals("f32")) {
						if (cls.equals("L")) {
							m.visitInsn(F2L);
						} else if (cls.equals("D")) {
							m.visitInsn(F2D);
						} else if (cls.equals("I")) {
							m.visitInsn(F2I);
						} else if (cls.equals("C")) {
							m.visitInsn(F2I);
							m.visitInsn(I2C);
						} else if (cls.equals("B")) {
							m.visitInsn(F2I);
							m.visitInsn(I2B);
						} else if (cls.equals("S")) {
							m.visitInsn(F2I);
							m.visitInsn(I2S);
						}
					} else if (cast.equals("f64")) {
						if (cls.equals("L")) {
							m.visitInsn(D2L);
						} else if (cls.equals("D")) {
							m.visitInsn(D2F);
						} else if (cls.equals("I")) {
							m.visitInsn(D2I);
						} else if (cls.equals("C")) {
							m.visitInsn(D2I);
							m.visitInsn(I2C);
						} else if (cls.equals("B")) {
							m.visitInsn(D2I);
							m.visitInsn(I2B);
						} else if (cls.equals("S")) {
							m.visitInsn(D2I);
							m.visitInsn(I2S);
						}
					}
				} else {
					m.visitTypeInsn(CHECKCAST, cls.getAbsoluteTypeName());
				}
			}

			resultName = val;
			resultOwner = data;
			resultType = cls;
			expressionType = CAST;

			next = true;
		} else {
			boolean match = part.matches("^([a-zA-Z0-9!_]*?)\\(.*?\\)");
			if (allowImplicitGetters) {
				String test = "get" + Strings.capitalize(part);
				if (containerData.getAllMethods(test).length > 0) {
					if (containerData != data || !data.hasField(test)) {
						compileMethodCall(last, containerData, data, m, block, test + "()", false);
						if (resultType.equals("V")) {
							throw new CompileError("Cannot reference void-returning getters");
						}
						next = true;
					}
				}
			}
			if (!next) {
				if (match) {
					String name = part.substring(0, part.indexOf('(')).trim();
					if (!name.isEmpty()) {
						if (name.endsWith("!")) {
							String macro = name.substring(0, name.length() - 1);
							if (!data.hasMacro(macro)) {
								throw new CompileError("Undefined macro: " + macro);
							}
							name = data.resolveMacro(macro);

							String theNew = name + part.substring(part.indexOf('(')).trim();
							ExpressionCompiler compiler = new ExpressionCompiler(this.write, this.data);
							compiler.compile(data, m, block, theNew, new String[] { theNew });
							next = true;
						}

						if (!next) {
							boolean superCall = false;
							if (this.data instanceof ConstructorData) {
								if (name.equals("super")) {
									if (((ConstructorBlock) block).hasCalledSuper()) {
										throw new CompileError("Cannot call super more than once");
									}
									superCall = true;
								}
							} else {
								if (name.equals("super")) {
									throw new CompileError("Cannot call super outside of a constructor");
								}
							}

							ClassData toUse = thisType ? data : containerData;

							if (superCall || toUse.hasMethod(name)) {
								compileMethodCall(last, toUse, data, m, block, part, superCall);
								next = true;
							} else if (name.equals("typeof")) {
								compileMethodCall(last, toUse, data, m, block, part, superCall);
								next = true;
							} else {
								MethodData[] methods = toUse.getAllMethods(name);
								if (methods.length > 0) {
									compileMethodCall(last, toUse, data, m, block, part, superCall);
									next = true;
								}

								if (!next && !thisType) {
									if (!name.equals("array")) {
										DefinitiveType resolved = null;
										try {
											if (Strings.contains(name, "<")) {
												name = name.substring(0, name.indexOf('<'));
											}
											resolved = data.resolveClass(name);
										} catch (CompileError e) {
											throw new CompileError("Undefined function: " + name);
										}

										try {
											compileConstructorCall(resolved.getObjectType(), data, m, block, part,
													resolved.getAbsoluteTypeName());
										} catch (ClassNotFoundException e) {
											throw new CompileError(e);
										}
										next = true;
									} else {
										compileConstructorCall(data, data, m, block, part, "__array__");
										next = true;
									}
								}
							}
						}
					} else {
						throw new CompileError("Method name cannot be empty");
					}
				} else {
					if (part.equals("length") && resultType.getTypeName().startsWith("[")) {
						if (this.write) {
							m.visitInsn(ARRAYLENGTH);
							if (this.data != null) {
								this.data.ics();
							}
						}
						resultName = "length";
						resultType = DefinitiveType.primitive("I");
						expressionType = LOCAL_VARIABLE;
						next = true;
					} else {
						boolean arr = part.contains("[");
						String varPart = part.substring(0, !arr ? part.length() : part.indexOf('['));
						String arrayIndex = null;
						if (arr) {
							arrayIndex = part.substring(part.indexOf('[') + 1, part.indexOf(']')).trim();
						}
						if (!thisType && this.data != null && this.data.hasLocal(varPart, block)) {
							compileVariableReference(last, 1, containerData, data, m, block, varPart, arrayIndex,
									end == body.length());
							next = true;
						} else {
							if (data.hasField(varPart)) {
								compileVariableReference(last, 0, data, data, m, block, varPart, arrayIndex,
										end == body.length());
								next = true;
							} else if (!thisType && containerData.hasField(varPart)) {
								compileVariableReference(last, 0, containerData, data, m, block, varPart, arrayIndex,
										end == body.length());
								next = true;
							} else {
								boolean math = true;
								if (allowBoolean) {
									BooleanExpressionCompiler compiler = new BooleanExpressionCompiler(this.data, null,
											false);
									compiler.compile(data, m, block, part, new String[] { part });

									if (compiler.isValid()) {
										Label iconst = new Label();
										Label label = new Label();

										compiler.setWrite(this.write);
										compiler.setEnd(iconst);

										compiler.compile(data, m, block, part, new String[] { part });
										m.visitInsn(ICONST_1);
										m.visitFrame(F_SAME, this.data.getLocalVariables(), null,
												this.data.getCurrentStack(), null);
										m.visitJumpInsn(GOTO, label);
										m.visitLabel(iconst);
										m.visitInsn(ICONST_0);

										m.visitLabel(label);

										resultName = body;
										resultOwner = data;
										resultType = DefinitiveType.primitive("Z");
										expressionType = BOOLEAN_EXPRESSION;
										next = true;
										math = false;
									}
								}
								if (math) {
									if (source instanceof BooleanExpressionCompiler) {
										next = true;
									} else {
										MathExpressionCompiler compiler = new MathExpressionCompiler(this.data,
												this.allowBoolean, false);
										compiler.compile(data, m, block, part, new String[] { part });

										if (compiler.isValid()) {
											compiler.setWrite(this.write);
											compiler.compile(data, m, block, part, new String[] { part });

											resultName = body;
											resultOwner = data;
											resultType = compiler.getResultType();
											expressionType = MATH_EXPRESSION;
											this.math = true;
											next = true;
										}
									}
								}
							}
						}
					}
				}
			}
		}

		if (next) {
			if (end != body.length()) {
				String newBody = body.substring(end + 1, body.length()).trim();

				if (resultType != null) {
					if (resultType.getTypeSignature().length() == 1) {
						return;
					}

					ClassData newClass = null;
					try {
						newClass = ClassData.forName(Types.unpadSignature(resultType.getTypeSignature()));
					} catch (ClassNotFoundException e) {
						throw new CompileError(e);
					}

					compile(this, newClass, data, m, block, newBody, new String[] { newBody });
				} else {
					compile(this, data, data, m, block, newBody, new String[] { newBody });
				}
			}
			return;
		}

		DefinitiveType clazz = null;

		if (part.equals("true") || part.equals("false")) {
			m.visitInsn(part.equals("true") ? ICONST_1 : ICONST_0);

			resultName = body;
			resultOwner = data;
			resultType = DefinitiveType.primitive("Z");
			expressionType = BOOLEAN_EXPRESSION;

			return;
		} else {
			try {
				clazz = data.resolveClass(part, false);
			} catch (CompileError e) {
				throw new CompileError(
						"Could not find a class, variable, method, or keyword derived from '" + part + "'");
			}
		}

		ClassData cls = clazz.getObjectType();

		if (end == body.length()) {
			return;
		}
		String newBody = body.substring(end + 1).trim();

		compile(this, cls, data, m, block, newBody, new String[] { newBody });

	}

	private void compileVariableReference(ExpressionCompiler last, int source, ClassData containerData, ClassData data,
			MethodVisitor m, Block block, String body, String arrayIndex, boolean isLast) {
		FieldData field = null;
		if (source == 0) {
			field = containerData.getField(body);
			if (!field.isAccessible(data)) {
				throw new CompileError("Field is not accessible from this context");
			}
			if (write) {
				if (this.data != null) {
					if (!this.data.hasModifier(ACC_STATIC) && !thisType && (last == null || !last.thisType)
							&& containerData.getClassName().equals(data.getClassName())) {
						m.visitVarInsn(ALOAD, 0);
						this.data.ics();
					} else {
						if (this.data.hasModifier(ACC_STATIC) && !field.hasModifier(ACC_STATIC)
								&& containerData.getClassName().equals(data.getClassName())
								&& (last == null || (last.getExpressionType() != ExpressionCompiler.LOCAL_VARIABLE
										&& last.getExpressionType() != ExpressionCompiler.MEMBER_VARIABLE))) {
							throw new CompileError("Cannot access instance variable from a static context");
						}
					}
				}

				if (!(!loadVariableReference && isLast)) {
					if (field.hasModifier(ACC_STATIC)) {
						m.visitFieldInsn(GETSTATIC, containerData.getClassName(), body,
								field.getType().getAbsoluteTypeSignature());
					} else {
						m.visitFieldInsn(GETFIELD, containerData.getClassName(), body,
								field.getType().getAbsoluteTypeSignature());
					}

					if (this.data != null)
						this.data.ics();
				}
			}

			this.field = field;
			resultType = field.getType();
			expressionType = MEMBER_VARIABLE;
			resultName = field.getName();
			resultOwner = containerData;
		} else if (source == 1) {
			field = this.data.getLocal(body, block);

			if (!(!loadVariableReference && isLast)) {
				int op = Types.getOpcode(Types.LOAD, field.getType().getTypeSignature());
				if (write) {
					m.visitVarInsn(op, ((LocalData) field).getIndex());
				}

				if (this.write && this.data != null) {
					this.data.ics();
				}
			}

			this.field = field;
		}

		DefinitiveType type = field.getType();
		ClassData typeClass = type.getObjectType();
		MethodData indexer = typeClass != null && typeClass.isGetIndexedClass() ? typeClass.getMethods("_get_index_")[0]
				: null;
		DefinitiveType tupleType = null;
		if (arrayIndex != null) {
			String idxType = Types.getType(arrayIndex, null);
			if (idxType == null) {
				ExpressionCompiler compiler = new ExpressionCompiler(this.write, this.data);
				compiler.compile(data, m, block, arrayIndex, new String[] { arrayIndex });

				try {
					if (typeClass.is("java.util.List") || type.getTypeSignature().startsWith("[")) {
						if (!compiler.getResultType().equals("I")) {
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
					if (this.write && !(!loadVariableReference && isLast)) {
						m.visitLdcInsn(Types.parseLiteral("string", arrayIndex));
					}
				} else {
					int x = Integer.parseInt(arrayIndex);
					if (x < 0) {
						throw new CompileError("Array literal indexes must be greater than or equal to 0");
					}
					if (this.write && !(!loadVariableReference && isLast)) {
						m.visitLdcInsn(x);
					}
					if (type.isTuple()) {
						TupleClassData clz = (TupleClassData) typeClass;
						if (x >= clz.getTypes().length) {
							throw new CompileError("Tuple index out of range");
						}
						tupleType = clz.getType(x);
					}
				}
			}

			if (this.write && !(!loadVariableReference && isLast)) {
				try {
					if (typeClass.isGetIndexedClass()) {
						m.visitMethodInsn(INVOKEVIRTUAL, typeClass.getClassName(), "_get_index_",
								indexer.getSignature(), false);
					} else if (type.equals("Ljava/lang/String;")) {
						m.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false);
					} else if (type.isTuple()) {
						if (tupleType.isPrimitive()) {
							m.visitMethodInsn(INVOKEVIRTUAL, "cornflakes/lang/Tuple",
									Types.primitiveToCornflakes(tupleType.getAbsoluteTypeSignature()) + "Item",
									"(I)" + tupleType.getAbsoluteTypeSignature(), false);
						} else {
							m.visitMethodInsn(INVOKEVIRTUAL, "cornflakes/lang/Tuple", "item", "(I)Ljava/lang/Object;",
									false);
						}
					} else if (typeClass.is("java.util.List")) {
						m.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;", true);
					} else if (typeClass.is("java.util.Map")) {
						m.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get",
								"(Ljava/lang/Object;)Ljava/lang/Object;", true);
					} else {
						m.visitInsn(Types.getArrayOpcode(Types.LOAD, type.getTypeSignature()));
					}
				} catch (ClassNotFoundException e) {
					throw new CompileError(e);
				}
			}
		}

		if (arrayIndex == null) {
			resultType = type;
		} else {
			try {
				if (indexer != null) {
					resultType = indexer.getReturnType();
				} else if (type.equals("Ljava/lang/String;")) {
					resultType = DefinitiveType.primitive("C");
				} else if (type.equals("Lcornflakes/lang/Tuple;")) {
					resultType = tupleType;
				} else if (typeClass.is("java.util.List") || typeClass.is("java.util.Map")) {
					resultType = DefinitiveType.object("Ljava/lang/Object;");
				} else {
					resultType = DefinitiveType.assume(type.getTypeSignature().substring(1));
				}
			} catch (ClassNotFoundException e) {
				throw new CompileError(e);
			}
		}

		if (field.isGeneric()) {
			this.genericTypes = field.getGenericTypes();
		}
	}

	private void compileConstructorCall(ClassData containerData, ClassData data, MethodVisitor m, Block block,
			String body, String clazz) throws ClassNotFoundException {
		String pars = body.substring(body.indexOf('(') + 1, body.lastIndexOf(')')).trim();
		if (Strings.hasMatching(body, '<', '>')) {
			String generic = body.substring(body.indexOf('<') + 1, body.lastIndexOf('>')).trim();
			String[] params = generic.split(",");
			if (params.length != containerData.getGenerics().length) {
				throw new CompileError(Types.beautify(containerData.getClassName()) + " expects "
						+ containerData.getGenerics().length + " generic parameter"
						+ (containerData.getGenerics().length != 1 ? "s" : "") + " (" + params.length + " were given)");
			}

			for (String param : params) {
				String type = param.trim();
				boolean ext = false;

				if (type.contains(" is ")) {
					String[] split = type.split(" is ");
					String p1 = split[0].trim();
					if (!p1.equals("?")) {
						throw new CompileError("Expecting generic type '?'");
					}

					type = split[1].trim();
					ext = true;
				}

				if (Types.isPrimitive(type)) {
					type = Types.getWrapperType(type);
				} else {
					type = data.resolveClass(type).getTypeSignature();
				}

				genericTypes.add(new GenericType(type, ext));
			}
		} else {
			if (containerData.hasGenericParameters()) {
				throw new CompileError(
						Types.beautify(containerData.getClassName()) + " expects " + containerData.getGenerics().length
								+ " generic parameter" + (containerData.getGenerics().length != 1 ? "s" : ""));
			}
		}
		if (clazz.equals("__array__")) {
			String[] split = Strings.splitParameters(pars);
			if (split.length != 2) {
				throw new CompileError("Array declarations should be in the form 'array(type, size)'");
			}

			String type = split[0].trim();
			String size = split[1].trim();

			DefinitiveType resolved = data.resolveClass(type);
			try {
				int x = Integer.parseInt(size);
				if (x < 0) {
					throw new CompileError("Array literal indexes must be greater than or equal to 0");
				}
				m.visitLdcInsn(x);
			} catch (Exception e) {
				ExpressionCompiler compiler = new ExpressionCompiler(true, this.data);
				compiler.compile(data, m, block, size, new String[] { size });

				if (!compiler.getResultType().equals("I")) {
					throw new CompileError("Arrays can only be indexed by integers");
				}
			}

			if (Types.isPrimitive(resolved)) {
				int val;
				if (resolved.equals("I")) {
					val = T_INT;
				} else if (resolved.equals("J")) {
					val = T_LONG;
				} else if (resolved.equals("S")) {
					val = T_SHORT;
				} else if (resolved.equals("B")) {
					val = T_BYTE;
				} else if (resolved.equals("Z")) {
					val = T_BOOLEAN;
				} else if (resolved.equals("F")) {
					val = T_FLOAT;
				} else if (resolved.equals("D")) {
					val = T_DOUBLE;
				} else if (resolved.equals("C")) {
					val = T_CHAR;
				} else {
					throw new CompileError("Invalid primitive");
				}
				m.visitIntInsn(NEWARRAY, val);
			} else {
				m.visitTypeInsn(ANEWARRAY, resolved.getAbsoluteTypeName());
			}

			resultType = DefinitiveType.assume("[" + resolved.getTypeSignature());
			expressionType = NEW_ARRAY;
			resultName = "array";
			resultOwner = containerData;
		} else {
			MethodData[] methods = containerData.getConstructors();
			MethodData method = null;

			String[] split = Strings.splitParameters(pars);

			for (MethodData met : methods) {
				if (met.getParameters().size() == split.length) {
					int idx = 0;
					boolean success = true;
					for (String par : split) {
						String type = Types.getType(par, met.getReturnType().getTypeSignature());
						DefinitiveType paramType = met.getParameters().get(idx).getType();

						if (type != null) {
							if (!Types.isSuitable(paramType, DefinitiveType.assume(Types.getTypeSignature(type)))) {
								success = false;
								break;
							}
						} else {
							ExpressionCompiler compiler = new ExpressionCompiler(false, this.data);
							compiler.compile(this, data, data, m, block, par, new String[] { par });

							if (!Types.isSuitable(paramType, compiler.getResultType())) {
								success = false;
								break;
							}
						}
						idx++;
					}

					if (success) {
						method = met;
						break;
					}
				}
			}

			if (method == null) {
				throw new CompileError("No constructor overload for type "
						+ Types.beautify(containerData.getClassName()) + " takes the given parameters");
			}

			if (!method.isAccessible(data)) {
				throw new CompileError("Constructor is not accessible from this context");
			}

			if (containerData.hasModifier(ACC_ABSTRACT)) {
				throw new CompileError("Cannot instantiate an abstract type");
			}

			if (write) {
				m.visitTypeInsn(NEW, containerData.getClassName());
				m.visitInsn(DUP);

				if (this.data != null)
					this.data.ics();
			}

			for (String par : split) {
				String type = Types.getType(par, this.data == null ? "" : this.data.getReturnType().getTypeSignature());

				if (type != null) {
					if (write) {
						m.visitLdcInsn(Types.parseLiteral(type, par));

						if (this.data != null)
							this.data.ics();
					}
				} else {
					ExpressionCompiler compiler = new ExpressionCompiler(this.write, this.data);
					compiler.compile(data, m, block, par, new String[] { par });
				}
			}

			if (write) {
				m.visitMethodInsn(INVOKESPECIAL, containerData.getClassName(), "<init>", method.getSignature(), false);

				if (this.data != null)
					this.data.ics();
			}

			resultType = DefinitiveType.object(Types.getTypeSignature(containerData.getClassName()));
			expressionType = CONSTRUCTOR;
			resultName = containerData.getSimpleClassName();
			resultOwner = containerData;
		}
	}

	private void compileMethodCall(ExpressionCompiler last, ClassData containerData, ClassData data, MethodVisitor m,
			Block block, String body, boolean superCall) throws ClassNotFoundException {
		String before = body.substring(0, body.indexOf('(')).trim();
		String pars = body.substring(body.indexOf('(') + 1, body.lastIndexOf(')')).trim();
		String[] split = Strings.splitParameters(pars);

		if (before.equals("typeof")) {
			if (split.length != 1) {
				throw new CompileError("typeof function takes 1 parameter, but " + split.length + " were given");
			}
			String type = split[0].trim();
			DefinitiveType resolve = data.resolveClass(type);
			if (write) {
				m.visitLdcInsn(Type.getType(resolve.getAbsoluteTypeName()));
			}

			resultType = DefinitiveType.object("Ljava/lang/Class;");
			expressionType = TYPEOF;
			resultName = "typeof";
			resultOwner = containerData;
		} else {
			MethodData[] methods = superCall ? ClassData.forName(containerData.getParentName()).getConstructors()
					: containerData.getAllMethods(before);
			MethodData method = null;

			for (MethodData met : methods) {
				if (met.getParameters().size() == split.length) {
					int idx = 0;
					boolean success = true;
					for (String par : split) {
						String type = Types.getType(par, met.getReturnType().getTypeSignature());
						ParameterData parData = met.getParameters().get(idx);
						DefinitiveType paramType = parData.getType();

						if (type != null) {
							if (!Types.isSuitable(paramType, DefinitiveType.object(Types.getTypeSignature(type)))) {
								success = false;
								break;
							}
						} else {
							ExpressionCompiler compiler = new ExpressionCompiler(false, this.data);
							compiler.compile(this, data, data, m, block, par, new String[] { par });

							if (!Types.isSuitable(paramType, compiler.getResultType())) {
								success = false;
								break;
							}
						}
						idx++;
					}

					if (success) {
						method = met;
						break;
					}
				}
			}

			if (method == null) {
				throw new CompileError("No overload for method " + before + " takes the given parameters");
			}

			if (!method.isAccessible(data)) {
				throw new CompileError("Method is not accessible from this context");
			}

			if (!superCall) {
				if (!this.data.hasModifier(ACC_STATIC) && !thisType && (last == null || !last.thisType)
						&& containerData.getClassName().equals(data.getClassName()) && write) {
					m.visitVarInsn(ALOAD, 0);

					if (this.data != null)
						this.data.ics();
				}
			} else if (!thisType && (last == null || !last.thisType) && write) {
				m.visitVarInsn(ALOAD, 0);

				if (this.data != null)
					this.data.ics();
			}

			for (String par : split) {
				String type = Types.getType(par,
						this.data == null ? "" : this.data.getReturnType().getTypeSignature().toLowerCase());

				if (type != null) {
					if (write) {
						m.visitLdcInsn(Types.parseLiteral(type, par));

						if (this.data != null)
							this.data.ics();
					}
				} else {
					ExpressionCompiler compiler = new ExpressionCompiler(this.write, this.data);
					compiler.compile(data, m, block, par, new String[] { par });
				}
			}

			if (superCall) {
				((ConstructorBlock) block).setCalledSuper(true);
				m.visitMethodInsn(INVOKESPECIAL, containerData.getParentName(), "<init>", method.getSignature(), false);

				resultType = DefinitiveType.object(Types.getTypeSignature(containerData.getParentName()));
				expressionType = CONSTRUCTOR;
				resultName = "<init>";
				resultOwner = containerData;
			} else {
				if ((method.getModifiers() & ACC_STATIC) == ACC_STATIC) {
					if (write) {
						m.visitMethodInsn(INVOKESTATIC, method.getContext().getClassName(), before,
								method.getSignature(), method.isInterfaceMethod());
						if (!method.getReturnType().equals("V")) {
							if (this.data != null)
								this.data.ics();
						}
					}
				} else {
					if (containerData.getClassName().equals(data.getClassName()) && resultType == null) {
						if ((this.data.getModifiers() & ACC_STATIC) == ACC_STATIC) {
							throw new CompileError("Cannot access instance method from a static context");
						}
					}
					if (write) {
						m.visitMethodInsn(method.isInterfaceMethod() ? INVOKEINTERFACE : INVOKEVIRTUAL,
								method.getContext().getClassName(), before, method.getSignature(),
								method.isInterfaceMethod());
						if (!method.getReturnType().equals("V")) {
							if (this.data != null)
								this.data.ics();
						}
					}
				}

				resultType = method.getReturnType();
				expressionType = METHOD;
				resultName = method.getName();
				resultOwner = containerData;
			}
		}
	}

	public DefinitiveType getResultType() {
		return resultType;
	}

	public void setLoadVariableReference(boolean loadVariableReference) {
		this.loadVariableReference = loadVariableReference;
	}

	public int getExpressionType() {
		return expressionType;
	}

	public String getResultName() {
		return resultName;
	}

	public ClassData getResultOwner() {
		return resultOwner;
	}

	public boolean isPrimitiveResult() {
		return Types.isPrimitive(resultType);
	}

	public boolean isAllowBoolean() {
		return allowBoolean;
	}

	public void setAllowBoolean(boolean allowBoolean) {
		this.allowBoolean = allowBoolean;
	}

	public boolean isAllowMath() {
		return allowMath;
	}

	public void setAllowMath(boolean allowMath) {
		this.allowMath = allowMath;
	}

	public GenericCompiler getSource() {
		return source;
	}

	public void setSource(GenericCompiler source) {
		this.source = source;
	}

	public boolean isMath() {
		return math;
	}

	public void setMath(boolean math) {
		this.math = math;
	}

	public FieldData getField() {
		return field;
	}

	public void setWrite(boolean val) {
		this.write = val;
	}

	public List<GenericType> getGenericTypes() {
		return genericTypes;
	}

	public void setGenericTypes(List<GenericType> genericTypes) {
		this.genericTypes = genericTypes;
	}

	public boolean isAllowImplicitGetters() {
		return allowImplicitGetters;
	}

	public void setAllowImplicitGetters(boolean allowImplicitGetters) {
		this.allowImplicitGetters = allowImplicitGetters;
	}
}
