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

	private MethodData data;
	private boolean write;
	private boolean loadVariableReference = true;
	private String referenceSignature;
	private String referenceName;
	private ClassData referenceOwner;
	private int referenceType;

	public ReferenceCompiler(boolean write, MethodData data) {
		this.write = write;
		this.data = data;
	}

	@Override
	public void compile(ClassData data, MethodVisitor m, Label start, Label end, String body, String[] lines) {
		compile(data.getClassName(), data, data, m, start, end, body, lines);
	}

	private void compile(String containerClass, ClassData containerData, ClassData data, MethodVisitor m,
			Label startLabel, Label endLabel, String body, String[] lines) {
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
					end = i;
					break;
				}
			}
		}
		String part = body.substring(0, end).trim();

		boolean next = false;
		if (Strings.hasMatching(part, '(', ')')) {
			String name = part.substring(0, part.indexOf('(')).trim();

			if (containerData.hasMethod(name)) {
				compileMethodCall(containerClass, containerData, data, m, startLabel, endLabel, part);
				next = true;
			} else {
				String resolved = null;
				try {
					resolved = data.resolveClass(name);
				} catch (CompileError e) {
					throw new CompileError("Undefined function: " + name);
				}

				try {
					compileConstructorCall(resolved, ClassData.forName(resolved), data, m, startLabel, endLabel, part,
							resolved);
				} catch (ClassNotFoundException e) {
					throw new CompileError(e);
				}
				next = true;
			}
		} else {
			if (containerData.hasField(part)) {
				compileVariableReference(0, containerClass, containerData, data, m, startLabel, endLabel, part,
						end == body.length());
				next = true;
			} else if (this.data.hasLocal(part, startLabel, endLabel)) {
				compileVariableReference(1, containerClass, containerData, data, m, startLabel, endLabel, part,
						end == body.length());
				next = true;
			}
		}

		if (next) {
			if (end != body.length()) {
				String newBody = body.substring(end + 1, body.length()).trim();

				ClassData newClass = null;
				try {
					newClass = ClassData.forName(Types.unpadSignature(referenceSignature));
				} catch (ClassNotFoundException e) {
					throw new CompileError(e);
				}
				compile(newClass.getClassName(), newClass, data, m, startLabel, endLabel, newBody,
						new String[] { newBody });
			}
			return;
		}

		String clazz = null;
		try {
			clazz = data.resolveClass(part);
		} catch (CompileError e) {
			throw new CompileError("Could not find a class, variable, method, or keyword named '" + part.split(" ")[0]
					+ "' in class " + containerData.getSimpleClassName());
		}

		ClassData cls = null;
		try {
			cls = ClassData.forName(clazz);
		} catch (ClassNotFoundException e) {
			throw new CompileError("Unresolved class: " + clazz.replace('/', '.'));
		}

		String newBody = body.substring(end + 1).trim();

		compile(clazz, cls, data, m, startLabel, endLabel, newBody, new String[] { newBody });

	}

	private void compileVariableReference(int source, String containerClass, ClassData containerData, ClassData data,
			MethodVisitor m, Label startLabel, Label endLabel, String body, boolean isLast) {
		if (source == 0) {
			FieldData field = containerData.getField(body);
			if (write) {
				if (!(!loadVariableReference && isLast)) {
					if (field.hasModifier(ACC_STATIC)) {
						m.visitFieldInsn(GETSTATIC, containerClass, body, field.getType());
					} else {
						m.visitFieldInsn(GETFIELD, containerClass, body, field.getType());
					}

					this.data.increaseStackSize();
				}
			}

			referenceSignature = field.getType();
			referenceType = MEMBER_VARIABLE;
			referenceName = field.getName();
			referenceOwner = containerData;
		} else if (source == 1) {
			LocalData local = this.data.getLocal(body, startLabel, endLabel);
			String type = local.getType();

			if (write) {
				if (!(!loadVariableReference && isLast)) {
					int op = Types.getOpcode(Types.LOAD, type);
					m.visitVarInsn(op, local.getIndex());
				}
			}

			referenceSignature = type;
			referenceType = LOCAL_VARIABLE;
			referenceName = local.getName();
			referenceOwner = containerData;
		}
	}

	private void compileConstructorCall(String containerClass, ClassData containerData, ClassData data, MethodVisitor m,
			Label start, Label end, String body, String clazz) {
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
						compiler.compile(containerClass, containerData, data, m, start, end, par, new String[] { par });

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

			this.data.increaseStackSize();
		}

		for (String par : split) {
			String type = Types.getType(par, this.data.getReturnType().getSimpleClassName().toLowerCase());

			if (type != null) {
				if (write) {
					m.visitLdcInsn(Types.parseLiteral(type, par));
					this.data.increaseStackSize();
				}
			} else {
				ReferenceCompiler compiler = new ReferenceCompiler(this.write, this.data);
				compiler.compile(data, m, start, end, par, new String[] { par });
			}
		}

		if (write) {
			m.visitMethodInsn(INVOKESPECIAL, containerData.getClassName(), "<init>", method.getSignature(), false);
			this.data.increaseStackSize();
		}

		referenceSignature = Types.getTypeSignature(containerData.getClassName());
		referenceType = CONSTRUCTOR;
		referenceName = containerData.getSimpleClassName();
		referenceOwner = containerData;
	}

	private void compileMethodCall(String containerClass, ClassData containerData, ClassData data, MethodVisitor m,
			Label startLabel, Label endLabel, String body) {
		String before = body.substring(0, body.indexOf('(')).trim();

		String pars = body.substring(body.indexOf('(') + 1, body.lastIndexOf(')')).trim();

		String[] split = getParameters(pars);

		MethodData[] methods = containerData.getMethods(before);
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
						compiler.compile(containerClass, containerData, data, m, startLabel, endLabel, par,
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
			throw new CompileError("No overload for method " + before + " takes the given parameters");
		}

		for (String par : split) {
			String type = Types.getType(par, this.data.getReturnType().getSimpleClassName().toLowerCase());

			if (type != null) {
				if (write) {
					m.visitLdcInsn(Types.parseLiteral(type, par));
					this.data.increaseStackSize();
				}
			} else {
				ReferenceCompiler compiler = new ReferenceCompiler(this.write, this.data);
				compiler.compile(data, m, startLabel, endLabel, par, new String[] { par });
			}
		}

		if ((method.getModifiers() & ACC_STATIC) == ACC_STATIC) {
			if (write) {
				m.visitMethodInsn(INVOKESTATIC, containerData.getClassName(), before, method.getSignature(), false);
				if (!method.getReturnTypeSignature().equals("V")) {
					this.data.increaseStackSize();
				}
			}
		} else {
			if (containerData.getClassName().equals(data.getClassName())) {
				if ((this.data.getModifiers() & ACC_STATIC) == ACC_STATIC) {
					throw new CompileError("Cannot access instance method from a static context");
				}
			}
			if (write) {
				m.visitMethodInsn(INVOKEVIRTUAL, containerData.getClassName(), before, method.getSignature(), false);
				if (!method.getReturnTypeSignature().equals("V")) {
					this.data.increaseStackSize();
				}
			}
		}

		referenceSignature = method.getReturnTypeSignature();
		referenceType = METHOD;
		referenceName = method.getName();
		referenceOwner = containerData;
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
}
