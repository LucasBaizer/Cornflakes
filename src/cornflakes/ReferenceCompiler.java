package cornflakes;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class ReferenceCompiler implements GenericCompiler {
	private MethodData data;
	private Label label;
	private String referenceType;

	public ReferenceCompiler(Label label, MethodData data) {
		this.label = label;
		this.data = data;
	}

	@Override
	public int compile(ClassData data, MethodVisitor m, int num, String body, String[] lines) {
		return compile(data.getClassName(), data, data, m, num, body, lines);
	}

	private int compile(String containerClass, ClassData containerData, ClassData data, MethodVisitor m, int num,
			String body, String[] lines) {
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
				num = compileMethodCall(containerClass, containerData, data, m, num, part, new String[] { part });
				next = true;
			} else {
				String resolved = null;
				try {
					resolved = data.resolveClass(name);
				} catch (CompileError e) {
					throw new CompileError("Undefined function: " + name);
				}

				try {
					num = compileConstructorCall(resolved, ClassData.forName(resolved), data, m, num, part, resolved);
				} catch (ClassNotFoundException e) {
					throw new CompileError(e);
				}
				next = true;
			}
		} else {
			if (containerData.hasField(part)) {
				num = compileVariableReference(0, containerClass, containerData, data, m, num, part,
						new String[] { part });
				next = true;
			} else if (this.data.hasLocal(part)) {
				num = compileVariableReference(1, containerClass, containerData, data, m, num, part,
						new String[] { part });
				next = true;
			}
		}

		if (next) {
			if (end != body.length()) {
				String newBody = body.substring(end + 1, body.length()).trim();

				ClassData newClass = null;
				try {
					newClass = ClassData.forName(Types.unpadSignature(referenceType));
				} catch (ClassNotFoundException e) {
					throw new CompileError(e);
				}
				return compile(newClass.getClassName(), newClass, data, m, num, newBody, new String[] { newBody });
			}
			return num;
		}

		String clazz = null;
		try {
			clazz = data.resolveClass(part);
		} catch (CompileError e) {
			throw new CompileError("Unexpected token: " + part);
		}

		ClassData cls = null;
		try {
			cls = ClassData.forName(clazz);
		} catch (ClassNotFoundException e) {
			throw new CompileError("Unresolved class: " + clazz.replace('/', '.'));
		}

		String newBody = body.substring(end + 1).trim();
		return compile(clazz, cls, data, m, num, newBody, new String[] { newBody });
	}

	private int compileVariableReference(int source, String containerClass, ClassData containerData, ClassData data,
			MethodVisitor m, int num, String body, String[] lines) {
		if (source == 0) {
			FieldData field = containerData.getField(body);
			if (field.hasModifier(ACC_STATIC)) {
				m.visitFieldInsn(GETSTATIC, containerClass, body, field.getType());
			} else {
				m.visitFieldInsn(GETFIELD, containerClass, body, field.getType());
			}
			referenceType = field.getType();
		} else if (source == 1) {
			String type = this.data.getLocalType(body);

			int op = ALOAD;
			if (type.equals("I")) {
				op = ILOAD;
			} else if (type.equals("J")) {
				op = LLOAD;
			} else if (type.equals("D")) {
				op = DLOAD;
			} else if (type.equals("F")) {
				op = FLOAD;
			}

			m.visitVarInsn(op, new ArrayList<>(this.data.getLocals().keySet()).indexOf(body));
			referenceType = type;
		}
		return num;
	}

	private int compileConstructorCall(String containerClass, ClassData containerData, ClassData data, MethodVisitor m,
			int num, String body, String clazz) {
		String before = body.substring(0, body.indexOf('(')).trim();

		String pars = body.substring(body.indexOf('(') + 1, body.lastIndexOf(')')).trim();

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
		String[] split = splitList.toArray(new String[splitList.size()]);

		MethodData[] methods = containerData.getConstructors();
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
						// TODO ensure that non-literals are accepted, but for
						// now, just let it slide
						if (this.data.hasLocal(par)) {
							if (!Types.isSuitable(paramType, Types.getTypeSignature(this.data.getLocalType(par)))) {
								success = false;
								break;
							}
						}
						break;
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

		for (String par : split) {
			String type = Types.getType(par, this.data.getReturnType().getSimpleClassName().toLowerCase());

			if (type != null) {
				m.visitLdcInsn(Types.parseLiteral(type, par));
			} else {
				ReferenceCompiler compiler = new ReferenceCompiler(this.label, this.data);
				num = compiler.compile(data, m, num, par, new String[] { par });
			}
		}

		m.visitMethodInsn(INVOKESPECIAL, containerData.getClassName(), before, method.getSignature(), false);

		referenceType = method.getReturnTypeSignature();

		return num;
	}

	private int compileMethodCall(String containerClass, ClassData containerData, ClassData data, MethodVisitor m,
			int num, String body, String[] lines) {
		String before = body.substring(0, body.indexOf('(')).trim();

		String pars = body.substring(body.indexOf('(') + 1, body.lastIndexOf(')')).trim();

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
		String[] split = splitList.toArray(new String[splitList.size()]);

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
						// TODO ensure that non-literals are accepted, but for
						// now, just let it slide
						if (this.data.hasLocal(par)) {
							if (!Types.isSuitable(paramType, Types.getTypeSignature(this.data.getLocalType(par)))) {
								success = false;
								break;
							}
						}
						break;
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
				m.visitLdcInsn(Types.parseLiteral(type, par));
			} else {
				ReferenceCompiler compiler = new ReferenceCompiler(this.label, this.data);
				num = compiler.compile(data, m, num, par, new String[] { par });
			}
		}

		if ((method.getModifiers() & ACC_STATIC) == ACC_STATIC) {
			m.visitMethodInsn(INVOKESTATIC, containerData.getClassName(), before, method.getSignature(), false);
		} else {
			if (containerData.getClassName().equals(data.getClassName())) {
				if ((this.data.getModifiers() & ACC_STATIC) == ACC_STATIC) {
					throw new CompileError("Cannot access instance method from a static context");
				}
			}
			m.visitMethodInsn(INVOKEVIRTUAL, containerData.getClassName(), before, method.getSignature(), false);
		}

		referenceType = method.getReturnTypeSignature();

		return num;
	}

	public String getReferenceType() {
		return referenceType;
	}

	public void setReferenceType(String referenceType) {
		this.referenceType = referenceType;
	}
}
