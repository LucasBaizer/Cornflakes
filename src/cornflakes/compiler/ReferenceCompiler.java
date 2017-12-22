package cornflakes.compiler;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class ReferenceCompiler implements GenericCompiler {
	public static final int LOCAL_VARIABLE = 0;
	public static final int MEMBER_VARIABLE = 1;
	public static final int METHOD = 2;
	public static final int CONSTRUCTOR = 3;
	public static final int THIS = 4;
	public static final int BOOLEAN_EXPRESSION = 5;
	public static final int MATH_EXPRESSION = 6;

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

	public ReferenceCompiler(boolean write, MethodData data) {
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

	private void compile(ReferenceCompiler last, String containerClass, ClassData containerData, ClassData data,
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
					ClassData parent = toUse;
					while ((parent = parent.getParentClass()) != null) {
						if (parent.hasMethod(name)) {
							compileMethodCall(last, toUse.getClassName(), toUse, data, m, block, part, superCall);
							next = true;
							break;
						}
					}

					if (!next && !thisType) {
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
					}
				}
			}
		} else {
			boolean arr = part.contains("[");
			String varPart = part.substring(0, !arr ? part.length() : part.indexOf('['));
			String arrayIndex = null;
			if (arr) {
				arrayIndex = part.substring(part.indexOf('[') + 1, part.indexOf(']')).trim();
			}
			if (this.data != null && this.data.hasLocal(varPart, block)) {
				compileVariableReference(last, 1, containerClass, containerData, data, m, block, varPart, arrayIndex,
						end == body.length());
				next = true;
			} else {
				if (data.hasField(varPart)) {
					compileVariableReference(last, 0, data.getClassName(), data, data, m, block, varPart, arrayIndex,
							end == body.length());
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
							MathExpressionCompiler compiler = new MathExpressionCompiler(this.data, this.allowBoolean,
									false);
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
			} else if (part.equals("int")) {
				clazz = "java.lang.Integer";
			} else if (part.equals("bool")) {
				clazz = "java.lang.Boolean";
			} else if (part.equals("object")) {
				clazz = "java.lang.Object";
			} else if (part.equals("string")) {
				clazz = "java.lang.String";
			} else if (part.equals("byte")) {
				clazz = "java.lang.Byte";
			} else if (part.equals("char")) {
				clazz = "java.lang.Character";
			} else if (part.equals("short")) {
				clazz = "java.lang.Short";
			} else if (part.equals("long")) {
				clazz = "java.lang.Long";
			} else if (part.equals("float")) {
				clazz = "java.lang.Float";
			} else if (part.equals("double")) {
				clazz = "java.lang.Double";
			} else {
				clazz = data.resolveClass(part);
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

	private void compileVariableReference(ReferenceCompiler last, int source, String containerClass,
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

			referenceSignature = field.getType();
			referenceType = MEMBER_VARIABLE;
			referenceName = field.getName();
			referenceOwner = containerData;
		} else if (source == 1) {
			LocalData local = this.data.getLocal(body, block);
			String type = local.getType();

			if (write) {
				if (!(!loadVariableReference && isLast)) {
					int op = Types.getOpcode(Types.LOAD, type);
					m.visitVarInsn(op, local.getIndex());

					if (arrayIndex != null) {
						try {
							int x = Integer.parseInt(arrayIndex);
							if (x < 0) {
								throw new CompileError("Array literal indexes must be greater than or equal to 0");
							}
							m.visitLdcInsn(x);
						} catch (Exception e) {
							ReferenceCompiler compiler = new ReferenceCompiler(true, this.data);
							compiler.compile(data, m, block, arrayIndex, new String[] { arrayIndex });

							if (!compiler.getReferenceSignature().equals("I")) {
								throw new CompileError("Arrays can only be indexed by integers");
							}
						}
						m.visitInsn(Types.getArrayOpcode(Types.LOAD, type));
					}

					if (this.data != null) {
						this.data.ics();
					}
				}
			}

			referenceSignature = type;
			referenceType = LOCAL_VARIABLE;
			referenceName = local.getName();
			referenceOwner = containerData;
		}
	}

	private void compileConstructorCall(String containerClass, ClassData containerData, ClassData data, MethodVisitor m,
			Block block, String body, String clazz) throws ClassNotFoundException {
		String pars = body.substring(body.indexOf('(') + 1, body.lastIndexOf(')')).trim();

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
						ReferenceCompiler compiler = new ReferenceCompiler(false, this.data);
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
			throw new CompileError("No constructor overload takes the given parameters");

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
				ReferenceCompiler compiler = new ReferenceCompiler(this.write, this.data);
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

	private void compileMethodCall(ReferenceCompiler last, String containerClass, ClassData containerData,
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
						ReferenceCompiler compiler = new ReferenceCompiler(false, this.data);
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
				ReferenceCompiler compiler = new ReferenceCompiler(this.write, this.data);
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
					m.visitMethodInsn(INVOKEVIRTUAL, containerData.getClassName(), before, method.getSignature(),
							method.isInterfaceMethod());
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
}
