package cornflakes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class ClassData {
	private String simpleClassName;
	private String parentName;
	private String className;
	private String sourceName;
	private boolean hasConstructor;
	private int modifiers;
	private byte[] byteCode;
	private ArrayList<String> use = new ArrayList<>();
	private List<MethodData> methods = new ArrayList<>();
	private List<FieldData> fields = new ArrayList<>();

	public static ClassData fromJavaClass(Class<?> cls) {
		ClassData container = new ClassData(false);
		container.setClassName(Strings.transformClassName(cls.getName()));
		container.setSimpleClassName(cls.getSimpleName());

		for (Method method : cls.getDeclaredMethods()) {
			MethodData mData = new MethodData(method.getName(), Types.getTypeSignature(method.getReturnType()),
					method.getModifiers());
			Parameter[] params = method.getParameters();
			for (int i = 0; i < params.length; i++) {
				mData.addParameter(params[i].getName(), Types.getTypeSignature(params[i].getType()));
			}

			container.addMethod(mData);
		}

		for (Field field : cls.getDeclaredFields()) {
			container.fields
					.add(new FieldData(field.getName(), Types.getTypeSignature(field.getType()), field.getModifiers()));
		}

		return container;
	}

	public ClassData() {
		this(true);
	}

	private ClassData(boolean use) {
		if (use) {
			use("java.lang.Object");
			use("java.lang.String");
			use("java.lang.System");
		}
	}

	public void use(String use) {
		try {
			Class.forName(use);
			this.use.add(Strings.transformClassName(use));
		} catch (ClassNotFoundException e) {
			throw new CompileError("Unresolved class: " + use);
		}
	}

	public boolean isUsing(String name) {
		for (String use : this.use) {
			if (use.endsWith("/" + name)) {
				return true;
			}
		}
		return false;
	}

	public String resolveClass(String name) {
		boolean arrayType = false;

		if (name.endsWith("[]")) {
			name = name.replace("[]", "");
			arrayType = true;
		}

		if (Types.isPrimitive(name)) {
			return Types.getTypeSignature(Types.getClassFromPrimitive(name));
		}

		Strings.handleLetterString(name, Strings.NUMBERS);

		try {
			return Strings.transformClassName(Class.forName(name).getName());
		} catch (ClassNotFoundException e) {
			if (name.equals("string")) {
				return arrayType ? "[Ljava/lang/String" : "java/lang/String";
			} else if (name.equals("object")) {
				return arrayType ? "[Ljava/lang/Object" : "java/lang/Object";
			}

			for (String use : this.use) {
				if (use.endsWith("/" + name)) {
					return arrayType ? "[L" + use : use;
				}
			}
			throw new CompileError("Unresolved class: " + name);
		}
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public byte[] getByteCode() {
		return byteCode;
	}

	public void setByteCode(byte[] byteCode) {
		this.byteCode = byteCode;
	}

	public String getSourceName() {
		return sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	public boolean hasConstructor() {
		return hasConstructor;
	}

	public void setHasConstructor(boolean hasConstructor) {
		this.hasConstructor = hasConstructor;
	}

	public String getSimpleClassName() {
		return simpleClassName;
	}

	public void setSimpleClassName(String simpleClassName) {
		this.simpleClassName = simpleClassName;
	}

	public String getParentName() {
		return parentName;
	}

	public void setParentName(String parentName) {
		this.parentName = parentName;
	}

	public boolean hasMethod(String name) {
		return getMethods(name).length > 0;
	}

	public MethodData[] getMethods(String name) {
		List<MethodData> methods = new ArrayList<>();

		for (MethodData data : this.methods) {
			if (data.getName().equals(name)) {
				methods.add(data);
			}
		}

		return methods.toArray(new MethodData[methods.size()]);
	}

	public void addMethod(MethodData method) {
		methods.add(method);
	}

	public boolean hasField(String name) {
		return getField(name) != null;
	}

	public FieldData getField(String name) {
		for (FieldData data : this.fields) {
			if (data.getName().equals(name)) {
				return data;
			}
		}

		return null;
	}

	public void addField(FieldData method) {
		fields.add(method);
	}

	public int getModifiers() {
		return modifiers;
	}

	public void setModifiers(int modifiers) {
		this.modifiers = modifiers;
	}

	public boolean hasModifier(int mod) {
		return (this.modifiers & mod) == mod;
	}
}
