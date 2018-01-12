package cornflakes.compiler;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class ExpressionCompiler implements GenericCompiler {
	public static final int LOCAL_VARIABLE = 0;
	public static final int MEMBER_VARIABLE = 1;
	public static final int METHOD = 2;
	public static final int CONSTRUCTOR = 3;
	public static final int THIS = 4;
	public static final int BOOLEAN_EXPRESSION = 5;
	public static final int MATH_EXPRESSION = 6;
	public static final int NEW_ARRAY = 7;

	private MethodData data;
	private boolean write;
	private boolean loadVariableReference = true;
	private String referenceSignature;
	private String referenceName;
	private ClassData referenceOwner;
	private int referenceType;
	private boolean thisType;
	private boolean allowBoolean = true;
	private boolean allowMath = true;
	private boolean math;
	private GenericCompiler source;
	private FieldData field;

	public ExpressionCompiler(boolean write, MethodData data) {
		this.write = write;
		this.data = data;
	}

	@Override
	public void compile(ClassData data, MethodVisitor m, Block block, String body, String[] lines) {
		try {
			compile(null, data.getClassName(), data, data, m, block, body, lines);
		} catch (ClassNotFoundException e) {
			throw new CompileError(e);
		}
	}

	private void compile(ExpressionCompiler last, String containerClass, ClassData containerData, ClassData data,
			MethodVisitor m, Block block, String body, String[] lines) throws ClassNotFoundException {
		if (body.equals("null")) {
			m.visitInsn(ACONST_NULL);
			return;
		}

		int end = body.length();
		int opens = 0;
		for (int i = 0; i < body.length(); i++) {
			char c = body.charAt(i);
			if (c == '(') {
				opens++;
			} else if (c == ')') {
				opens--;
			}
			if (opens == 0) {
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
			m.visitVarInsn(ALOAD, 0);
			this.data.ics();

			thisType = true;
			referenceName = "this";
			referenceOwner = data;
			referenceSignature = Types.getTypeSignature(data.getClassName());
			referenceType = THIS;
			next = true;
		} else if (Strings.hasMatching(part, '(', ')')) {
			String name = part.substring(0, part.indexOf('(')).trim();
			if (!name.isEmpty()) {
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
					compileMethodCall(last, toUse.getClassName(), toUse, data, m, block, part, superCall);
					next = true;
				} else {
					MethodData[] methods = toUse.getAllMethods(name);
					if (methods.length > 0) {
						compileMethodCall(last, toUse.getClassName(), toUse, data, m, block, part, superCall);
						next = true;
					}

					if (!next && !thisType) {
						if (!name.equals("array")) {
							String resolved = null;
							try {
								resolved = data.resolveClass(name);
							} catch (CompileError e) {
								throw new CompileError("Undefined function: " + name);
							}

							try {
								compileConstructorCall(resolved, ClassData.forName(resolved), data, m, block, part,
										resolved);
							} catch (ClassNotFoundException e) {
								throw new CompileError(e);
							}
							next = true;
						} else {
							compileConstructorCall(data.getClassName(), data, data, m, block, part, "__array__");
							next = true;
						}
					}
				}
			}
		} else {
			if (part.equals("length") && referenceSignature.startsWith("[")) {
				if (this.write) {
					m.visitInsn(ARRAYLENGTH);
					if (this.data != null) {
						this.data.ics();
					}
				}
				referenceName = "length";
				referenceSignature = "I";
				referenceType = LOCAL_VARIABLE;
				next = true;
			} else {
				boolean arr = part.contains("[");
				String varPart = part.substring(0, !arr ? part.length() : part.indexOf('['));
				String arrayIndex = null;
				if (arr) {
					arrayIndex = part.substring(part.indexOf('[') + 1, part.indexOf(']')).trim();
				}
				if (!thisType && this.data != null && this.data.hasLocal(varPart, block)) {
					compileVariableReference(last, 1, containerClass, containerData, data, m, block, varPart,
							arrayIndex, end == body.length());
					next = true;
				} else {
					if (data.hasField(varPart)) {
						compileVariableReference(last, 0, data.getClassName(), data, data, m, block, varPart,
								arrayIndex, end == body.length());
						next = true;
					} else if (!thisType && containerData.hasField(varPart)) {
						compileVariableReference(last, 0, containerClass, containerData, data, m, block, varPart,
								arrayIndex, end == body.length());
						next = true;
					} else {
						boolean math = true;
						if (allowBoolean) {
							BooleanExpressionCompiler compiler = new BooleanExpressionCompiler(this.data, null, false);
							compiler.compile(data, m, block, part, new String[] { part });

							if (compiler.isValid()) {
								Label iconst = new Label();
								Label label = new Label();

								compiler.setWrite(this.write);
								compiler.setEnd(iconst);

								compiler.compile(data, m, block, part, new String[] { part });
								m.visitInsn(ICONST_1);
								m.visitFrame(F_SAME, this.data.getLocalVariables(), null, this.data.getCurrentStack(),
										null);
								m.visitJumpInsn(GOTO, label);
								m.visitLabel(iconst);
								m.visitInsn(ICONST_0);

								m.visitLabel(label);

								referenceName = body;
								referenceOwner = data;
								referenceSignature = "Z";
								referenceType = BOOLEAN_EXPRESSION;
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

									referenceName = body;
									referenceOwner = data;
									referenceSignature = compiler.getResultType();
									referenceType = MATH_EXPRESSION;
									this.math = true;
									next = true;
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

				if (referenceSignature != null) {
					if (referenceSignature.length() == 1) {
						return;
					}

					ClassData newClass = null;
					try {
						newClass = ClassData.forName(Types.unpadSignature(referenceSignature));
					} catch (ClassNotFoundException e) {
						throw new CompileError(e);
					}

					compile(this, newClass.getClassName(), newClass, data, m, block, newBody, new String[] { newBody });
				} else {
					compile(this, data.getClassName(), data, data, m, block, newBody, new String[] { newBody });
				}
			}
			return;
		}

		String clazz = null;
		try {
			if (part.equals("true") || part.equals("false")) {
				m.visitInsn(part.equals("true") ? ICONST_1 : ICONST_0);

				referenceName = body;
				referenceOwner = data;
				referenceSignature = "Z";
				referenceType = BOOLEAN_EXPRESSION;
			} else {
				clazz = data.resolveClass(part, false);
			}
		} catch (CompileError e) {
			throw new CompileError("Could not find a class, variable, method, or keyword derived from '" + part + "'");
		}

		ClassData cls = null;
		try {
			cls = ClassData.forName(Types.padSignature(clazz));
		} catch (ClassNotFoundException e) {
			throw new CompileError("Unresolved class: " + clazz.replace('/', '.'));
		}

		if (end == body.length()) {
			return;
		}
		String newBody = body.substring(end + 1).trim();

		compile(this, clazz, cls, data, m, block, newBody, new String[] { newBody });

	}

	private void compileVariableReference(ExpressionCompiler last, int source, String containerClass,
			ClassData containerData, ClassData data, MethodVisitor m, Block block, String body, String arrayIndex,
			boolean isLast) {
		if (source == 0) {
			FieldData field = containerData.getField(body);
			if (write) {
				if (this.data != null) {
					if (!this.data.hasModifier(ACC_STATIC) && !thisType && (last == null || !last.thisType)
							&& containerClass.equals(data.getClassName())) {
						m.visitVarInsn(ALOAD, 0);
						this.data.ics();
					}
				}

				if (!(!loadVariableReference && isLast)) {
					if (field.hasModifier(ACC_STATIC)) {
						m.visitFieldInsn(GETSTATIC, containerClass, body, field.getType());
					} else {
						m.visitFieldInsn(GETFIELD, containerClass, body, field.getType());
					}

					if (this.data != null)
						this.data.ics();
				}
			}

			this.field = field;
			referenceSignature = field.getType();
			referenceType = MEMBER_VARIABLE;
			referenceName = field.getName();
			referenceOwner = containerData;
		} else if (source == 1) {
			LocalData local = this.data.getLocal(body, block);
			String type = local.getType();

			ClassData typeClass = null;
			try {
				if (!Types.isPrimitive(type)) {
					typeClass = ClassData.forName(type);
				}
			} catch (ClassNotFoundException e) {
				throw new CompileError(e);
			}

			MethodData indexer = typeClass != null && typeClass.isIndexedClass() ? typeClass.getMethods("_index_")[0] : null;
			if (write) {
				if (!(!loadVariableReference && isLast)) {
					int op = Types.getOpcode(Types.LOAD, type);
					m.visitVarInsn(op, local.getIndex());

					if (arrayIndex != null) {
						String idxType = Types.getType(arrayIndex, null);
						if (idxType == null) {
							ExpressionCompiler compiler = new ExpressionCompiler(true, this.data);
							compiler.compile(data, m, block, arrayIndex, new String[] { arrayIndex });

							try {
								if (typeClass.isSubclassOf("java.util.List") || type.startsWith("[")) {
									if (!compiler.getReferenceSignature().equals("I")) {
										throw new CompileError("Arrays can only be indexed by integers");
									}
								}
							} catch (ClassNotFoundException e) {
								throw new CompileError(e);
							}
						} else {
							if (idxType.equals("string")) {
								m.visitLdcInsn(Types.parseLiteral("string", arrayIndex));
							} else {
								int x = Integer.parseInt(arrayIndex);
								if (x < 0) {
									throw new CompileError("Array literal indexes must be greater than or equal to 0");
								}
								m.visitLdcInsn(x);
							}
						}

						try {
							if (typeClass.isIndexedClass()) {
								m.visitMethodInsn(INVOKEVIRTUAL, typeClass.getClassName(), "_index_",
										indexer.getSignature(), false);
							} else if (type.equals("Ljava/lang/String;")) {
								m.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false);
							} else if (typeClass.isSubclassOf("java.util.List")) {
								m.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;",
										true);
							} else if (typeClass.isSubclassOf("java.util.Map")) {
								m.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get",
										"(Ljava/lang/Object;)Ljava/lang/Object;", true);
							} else {
								m.visitInsn(Types.getArrayOpcode(Types.LOAD, type));
							}
						} catch (ClassNotFoundException e) {
							throw new CompileError(e);
						}
					}

					if (this.data != null) {
						this.data.ics();
					}
				}
			}

			this.field = local;
			if (arrayIndex == null) {
				referenceSignature = type;
			} else {
				try {
					if (indexer != null) {
						referenceSignature = indexer.getReturnTypeSignature();
					} else if (type.equals("Ljava/lang/String;")) {
						referenceSignature = "C";
					} else if (typeClass.isSubclassOf("java.util.List") || typeClass.isSubclassOf("java.util.Map")) {
						referenceSignature = "Ljava/lang/Object;";
					} else {
						referenceSignature = type.substring(1);
					}
				} catch (ClassNotFoundException e) {
					throw new CompileError(e);
				}
			}
			referenceType = LOCAL_VARIABLE;
			referenceName = local.getName();
			referenceOwner = containerData;
		}
	}

	private void compileConstructorCall(String containerClass, ClassData containerData, ClassData data, MethodVisitor m,
			Block block, String body, String clazz) throws ClassNotFoundException {
		String pars = body.substring(body.indexOf('(') + 1, body.lastIndexOf(')')).trim();
		if (clazz.equals("__array__")) {
			String[] split = pars.split(",");
			if (split.length != 2) {
				throw new CompileError("Array declarations should be in the form 'array(type, size)'");
			}

			String type = split[0].trim();
			String size = split[1].trim();

			String resolved = data.resolveClass(type);

			try {
				int x = Integer.parseInt(size);
				if (x < 0) {
					throw new CompileError("Array literal indexes must be greater than or equal to 0");
				}
				m.visitLdcInsn(x);
			} catch (Exception e) {
				ExpressionCompiler compiler = new ExpressionCompiler(true, this.data);
				compiler.compile(data, m, block, size, new String[] { size });

				if (!compiler.getReferenceSignature().equals("I")) {
					throw new CompileError("Arrays can only be indexed by integers");
				}
			}

			if (!Types.isPrimitive(resolved)) {
				m.visitTypeInsn(ANEWARRAY, resolved);
			}

			referenceSignature = "[" + Types.getTypeSignature(resolved);
			referenceType = NEW_ARRAY;
			referenceName = "array";
			referenceOwner = containerData;
		} else {
			MethodData[] methods = containerData.getConstructors();
			MethodData method = null;

			String[] split = getParameters(pars);

			for (MethodData met : methods) {
				if (met.getParameters().size() == split.length) {
					int idx = 0;
					boolean success = true;
					for (String par : split) {
						String type = Types.getType(par, met.getReturnType().getSimpleClassName().toLowerCase());
						String paramType = new ArrayList<>(met.getParameters().values()).get(idx);

						if (type != null) {
							if (!Types.isSuitable(paramType, Types.getTypeSignature(type))) {
								success = false;
								break;
							}
						} else {
							ExpressionCompiler compiler = new ExpressionCompiler(false, this.data);
							compiler.compile(this, data.getClassName(), data, data, m, block, par,
									new String[] { par });

							if (!Types.isSuitable(paramType, compiler.getReferenceSignature())) {
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
				throw new CompileError(
						"No constructor overload for type " + containerClass + " takes the given parameters");

			}

			if (write) {
				m.visitTypeInsn(NEW, containerData.getClassName());
				m.visitInsn(DUP);

				if (this.data != null)
					this.data.ics();
			}

			for (String par : split) {
				String type = Types.getType(par,
						this.data == null ? "" : this.data.getReturnType().getSimpleClassName().toLowerCase());

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

			referenceSignature = Types.getTypeSignature(containerData.getClassName());
			referenceType = CONSTRUCTOR;
			referenceName = containerData.getSimpleClassName();
			referenceOwner = containerData;
		}
	}

	private void compileMethodCall(ExpressionCompiler last, String containerClass, ClassData containerData,
			ClassData data, MethodVisitor m, Block block, String body, boolean superCall)
			throws ClassNotFoundException {
		String before = body.substring(0, body.indexOf('(')).trim();

		String pars = body.substring(body.indexOf('(') + 1, body.lastIndexOf(')')).trim();

		String[] split = getParameters(pars);

		MethodData[] methods = superCall ? ClassData.forName(containerData.getParentName()).getConstructors()
				: containerData.getAllMethods(before);
		MethodData method = null;

		for (MethodData met : methods) {
			if (met.getParameters().size() == split.length) {
				int idx = 0;
				boolean success = true;
				for (String par : split) {
					String type = Types.getType(par, met.getReturnType().getSimpleClassName().toLowerCase());
					String paramType = new ArrayList<>(met.getParameters().values()).get(idx);

					if (type != null) {
						if (!Types.isSuitable(paramType, Types.getTypeSignature(type))) {
							success = false;
							break;
						}
					} else {
						ExpressionCompiler compiler = new ExpressionCompiler(false, this.data);
						compiler.compile(this, data.getClassName(), data, data, m, block, par, new String[] { par });

						if (!Types.isSuitable(paramType, compiler.getReferenceSignature())) {
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

		if (!superCall) {
			if (!this.data.hasModifier(ACC_STATIC) && !thisType && (last == null || !last.thisType)
					&& containerClass.equals(data.getClassName())) {
				m.visitVarInsn(ALOAD, 0);

				if (this.data != null)
					this.data.ics();
			}
		} else if (!thisType && (last == null || !last.thisType)) {
			m.visitVarInsn(ALOAD, 0);

			if (this.data != null)
				this.data.ics();
		}

		for (String par : split) {
			String type = Types.getType(par,
					this.data == null ? "" : this.data.getReturnType().getSimpleClassName().toLowerCase());

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

			referenceSignature = Types.getTypeSignature(containerData.getParentName());
			referenceType = CONSTRUCTOR;
			referenceName = "<init>";
			referenceOwner = containerData;
		} else {
			if ((method.getModifiers() & ACC_STATIC) == ACC_STATIC) {
				if (write) {
					m.visitMethodInsn(INVOKESTATIC, containerData.getClassName(), before, method.getSignature(),
							method.isInterfaceMethod());
					if (!method.getReturnTypeSignature().equals("V")) {
						if (this.data != null)
							this.data.ics();
					}
				}
			} else {
				if (containerData.getClassName().equals(data.getClassName()) && referenceSignature == null) {
					if ((this.data.getModifiers() & ACC_STATIC) == ACC_STATIC) {
						throw new CompileError("Cannot access instance method from a static context");
					}
				}
				if (write) {
					m.visitMethodInsn(method.isInterfaceMethod() ? INVOKEINTERFACE : INVOKEVIRTUAL,
							containerData.getClassName(), before, method.getSignature(), method.isInterfaceMethod());
					if (!method.getReturnTypeSignature().equals("V")) {
						if (this.data != null)
							this.data.ics();
					}
				}
			}

			referenceSignature = method.getReturnTypeSignature();
			referenceType = METHOD;
			referenceName = method.getName();
			referenceOwner = containerData;
		}
	}

	private String[] getParameters(String pars) {
		List<String> splitList = new ArrayList<>();
		if (!pars.isEmpty()) {
			int open = 0;
			boolean quote = false;
			int last = 0;
			for (int i = 0; i < pars.length(); i++) {
				char c = pars.charAt(i);
				if (c == '(') {
					open++;
				} else if (c == ')') {
					open--;
				}
				if (c == '"') {
					quote = !quote;
				}

				if (open == 0 && !quote) {
					if (c == ',') {
						splitList.add(pars.substring(last, i).trim());
						last = i + 1;
					}
				}
			}
			splitList.add(pars.substring(last, pars.length()).trim());
		}
		return splitList.toArray(new String[splitList.size()]);
	}

	public String getReferenceSignature() {
		return referenceSignature;
	}

	public void setLoadVariableReference(boolean loadVariableReference) {
		this.loadVariableReference = loadVariableReference;
	}

	public int getReferenceType() {
		return referenceType;
	}

	public String getReferenceName() {
		return referenceName;
	}

	public ClassData getReferenceOwner() {
		return referenceOwner;
	}

	public boolean isPrimitiveReference() {
		return Types.isPrimitive(referenceSignature);
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
}
