package cornflakes;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ClassData {
	private static final HashMap<String, ClassData> classes = new HashMap<>();
	private String simpleClassName;
	private String parentName;
	private String className;
	private String sourceName;
	private boolean hasConstructor;
	private int modifiers;
	private byte[] byteCode;
	private ArrayList<String> use = new ArrayList<>();
	private List<MethodData> methods = new ArrayList<>();
	private List<MethodData> constructors = new ArrayList<>();
	private List<FieldData> fields = new ArrayList<>();

	public static ClassData forName(String name) throws ClassNotFoundException {
		name = Strings.transformClassName(name);

		if (classes.containsKey(name)) {
			return classes.get(name);
		}

		return fromJavaClass(Class.forName(name.replace('/', '.')));
	}

	public static ClassData fromJavaClass(Class<?> cls) {
		String t = Strings.transformClassName(cls.getName());

		if (classes.containsKey(t)) {
			return classes.get(t);
		}

		ClassData container = new ClassData(false);
		container.setClassName(t);
		container.setSimpleClassName(cls.getSimpleName());

		for (Method method : cls.getDeclaredMethods()) {
			container.addMethod(MethodData.fromJavaMethod(method));
		}

		for (Method method : cls.getMethods()) {
			MethodData data = MethodData.fromJavaMethod(method);
			if (!container.methods.contains(data)) {
				container.addMethod(data);
			}
		}

		for (Constructor<?> constructor : cls.getConstructors()) {
			container.addConstructor(MethodData.fromJavaConstructor(constructor));
		}

		for (Field field : cls.getDeclaredFields()) {
			container.fields
					.add(new FieldData(field.getName(), Types.getTypeSignature(field.getType()), field.getModifiers()));
		}

		classes.put(t, container);
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
			ClassData.forName(use);
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
			return ClassData.forName(name).getClassName();
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

	public boolean hasConstructor(String name) {
		return getMethods(name).length > 0;
	}

	public MethodData[] getConstructors() {
		return constructors.toArray(new MethodData[constructors.size()]);
	}

	public void addConstructor(MethodData method) {
		constructors.add(method);
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

	public boolean isAssignableFrom(ClassData testClass) {
		return false; // TODO
	}

	public static void registerCornflakesClass(ClassData data) {
		classes.put(data.getClassName(), data);
	}

	@Override
	public String toString() {
		return "class " + getClassName();
	}
}
