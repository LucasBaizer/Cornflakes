package cornflakes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.regex.Pattern;

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
		// String[] operandSplit = body.split("\\(.*?\\)|(\\.)");
		String[] operandSplit = body.split("\\.");
		String part = operandSplit[0].trim();
		System.out.println(part);

		if (Strings.hasMatching(part, '(', ')')) {
			String name = part.substring(0, part.indexOf('(')).trim();

			if (containerData.hasMethod(name)) {
				return compileMethodCall(containerClass, containerData, data, m, num, body, lines);
			} else {
				throw new CompileError("Undefined function: " + name);
			}
		}

		String clazz = null;
		try {
			clazz = data.resolveClass(part);
		} catch (CompileError e) {
			throw new CompileError("Unexpected token: " + part);
		}

		Class<?> cls = null;
		try {
			cls = Class.forName(clazz.replace('/', '.'));
		} catch (ClassNotFoundException e) {
			throw new CompileError("Class not found: " + clazz.replace('/', '.'));
		}

		String newBody = Strings.accumulate(Strings.after(operandSplit, 1)).trim();
		ClassData container = new ClassData();
		container.setClassName(Strings.transformClassName(cls.getName()));
		container.setSimpleClassName(cls.getSimpleName());

		for (Method method : cls.getDeclaredMethods()) {
			MethodData mData = new MethodData(method.getName(), Types.getTypeSignature(method.getReturnType()),
					method.getModifiers());
			Parameter[] params = method.getParameters();
			for (int i = 0; i < params.length; i++) {
				mData.addParameter(params[i].getName(), Types.getTypeSignature(params[i].getType()));
			}

			container.addMethod(method.getName(), mData);
		}

		return compile(clazz, container, data, m, num, newBody, new String[] { newBody });
	}

	private int compileMethodCall(String containerClass, ClassData containerData, ClassData data, MethodVisitor m,
			int num, String body, String[] lines) {
		String[] operandSplit = body.split(Pattern.quote("."));

		String part = operandSplit[0].trim();

		String before = part.substring(0, part.indexOf('(')).trim();
		String pars = part.substring(part.indexOf('(') + 1, part.lastIndexOf(')')).trim();
		String[] split = pars.isEmpty() ? new String[0] : Strings.split(pars, ",");

		MethodData method = containerData.getMethodData(before);

		if (method.getParameters().size() != split.length) {
			throw new CompileError("Method " + method.getName() + " expects " + method.getParameters().size()
					+ " parameter" + (method.getParameters().size() == 1 ? "" : "s") + ", but " + split.length + " "
					+ (split.length == 1 ? "was" : "were") + " given");
		}

		int idx = 0;
		for (String par : split) {
			String type = Types.getType(par, this.data.getReturnType().getSimpleName().toLowerCase());

			if (type != null) {
				String paramType = new ArrayList<>(method.getParameters().values()).get(idx);
				if (!paramType.equals(Types.getTypeSignature(type))) {
					throw new CompileError("A parameter of type "
							+ Types.getTypeFromSignature(Types.unpadSignature(paramType)).getSimpleName()
							+ " was expected, but one of " + type + " was given");
				}

				m.visitLdcInsn(Types.parseLiteral(type, par));
			} else {
				ReferenceCompiler compiler = new ReferenceCompiler(this.label, this.data);
				num = compiler.compile(data, m, num, par, new String[] { par });
			}
			idx++;
		}

		if ((method.getModifiers() & ACC_STATIC) == ACC_STATIC) {
			m.visitMethodInsn(INVOKESTATIC, containerData.getClassName(), before, method.getSignature(), false);
		} else {
			if ((this.data.getModifiers() & ACC_STATIC) == ACC_STATIC) {
				throw new CompileError("Cannot access instance method from a static context");
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
