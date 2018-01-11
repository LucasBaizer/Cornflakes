package cornflakes.compiler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.objectweb.asm.ClassWriter;

public class ClassData {
	private static final HashMap<String, ClassData> classes = new HashMap<>();
	private String simpleClassName;
	private String parentName;
	private String className;
	private String sourceName;
	private String packageName;
	private boolean hasConstructor;
	private int modifiers;
	private byte[] byteCode;
	private String[] interfaces;
	private Map<String, String> use = new HashMap<>();
	private Set<MethodData> methods = new HashSet<>();
	private Set<ConstructorData> constructors = new HashSet<>();
	private Set<FieldData> fields = new HashSet<>();
	private ClassWriter classWriter;
	private Class<?> javaClass;
	private boolean isInterface;

	public static ClassData forName(String name) throws ClassNotFoundException {
		name = Strings.transformClassName(Types.unpadSignature(name));

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
		container.javaClass = cls;
		container.setIsInterface(cls.isInterface());
		container.setClassName(t);
		container.setSimpleClassName(cls.getSimpleName());
		container.setParentName(
				cls.getSuperclass() == null ? "java/lang/Object" : cls.getSuperclass().getName().replace('.', '/'));

		List<String> ifs = new ArrayList<>();
		for (Class<?> c : cls.getInterfaces()) {
			ifs.add(Strings.transformClassName(c.getName()));
		}
		container.setInterfaces(ifs.toArray(new String[ifs.size()]));

		for (Method method : cls.getDeclaredMethods()) {
			container.addMethod(MethodData.fromJavaMethod(method));
		}

		for (Method method : cls.getMethods()) {
			MethodData data = MethodData.fromJavaMethod(method);
			if (!container.methods.contains(data)) {
				container.addMethod(data);
			}
		}

		if (!cls.isInterface()) {
			for (Constructor<?> constructor : cls.getConstructors()) {
				container.addConstructor(ConstructorData.fromJavaConstructor(constructor));
			}
			for (Field field : cls.getDeclaredFields()) {
				container.fields.add(
						new FieldData(field.getName(), Types.getTypeSignature(field.getType()), field.getModifiers()));
			}
		}

		classes.put(t, container);
		return container;
	}

	public ClassData() {
		this(true);
	}

	private ClassData(boolean use) {
		if (use) {
			use("java.lang.Object", "object");
			use("java.lang.String", "string");
			use("java.lang.Boolean", "string");
			use("java.lang.Integer", "string");
			use("java.lang.Double", "string");
			use("java.lang.Float", "string");
			use("java.lang.Byte", "string");
			use("java.lang.Short", "string");
			use("java.lang.Character", "char");
			use("java.lang.String", "string");

			use("java.lang.System");
			use("cornflakes.lang.Console");
		}
	}

	public void use(String use) {
		use(use, use.substring(use.lastIndexOf('/') + 1));
	}

	public void use(String use, String as) {
		try {
			ClassData.forName(use);
			this.use.put(as, Strings.transformClassName(use));
		} catch (ClassNotFoundException e) {
			throw new CompileError("Unresolved class: " + use);
		}
	}

	public boolean isUsing(String name) {
		for (Entry<String, String> use : this.use.entrySet()) {
			if (use.getKey().equals(use) || use.getValue().endsWith("/" + name)) {
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

		Strings.handleLetterString(name, Strings.combineExceptions(Strings.NUMBERS, Strings.PERIOD));

		try {
			return ClassData.forName(name).getClassName();
		} catch (ClassNotFoundException e) {
			// if (name.equals("string")) {
			// return arrayType ? "[Ljava/lang/String" : "java/lang/String";
			// } else if (name.equals("object")) {
			// return arrayType ? "[Ljava/lang/Object" : "java/lang/Object";
			// }

			for (Entry<String, String> use : this.use.entrySet()) {
				if (use.getKey().equals(name) || use.equals(name.replace('.', '/'))
						|| use.getValue().endsWith("/" + name)) {
					return arrayType ? "[L" + use.getValue().replace('.', '/') : use.getValue().replace('.', '/');
				}
			}
			throw new CompileError("Unresolved type: " + name);
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

	public ClassData getParentClass() throws ClassNotFoundException {
		return className.equals("java/lang/Object") ? null : ClassData.forName(parentName);
	}

	public void setParentName(String parentName) {
		this.parentName = parentName;
	}

	public boolean hasMethod(String name) {
		return getMethods(name).length > 0;
	}

	public boolean hasMethodBySignature(String name, String sig) throws ClassNotFoundException {
		return getAllMethodsBySignature(name, sig).length > 0;
	}

	public MethodData[] getMethods() {
		return this.methods.toArray(new MethodData[this.methods.size()]);
	}

	public MethodData[] getMethods(String name) {
		List<MethodData> methods = new ArrayList<>();

		getMethods(name, methods);

		return methods.toArray(new MethodData[methods.size()]);
	}

	public MethodData[] getAllMethods(String name) throws ClassNotFoundException {
		List<MethodData> methods = new ArrayList<>();
		this.getMethods(name, methods);

		ClassData parent = this;
		while ((parent = parent.getParentClass()) != null) {
			parent.getMethods(name, methods);
		}

		if (interfaces.length > 0) {
			for (String itf : interfaces) {
				ClassData.forName(itf).getMethods(name, methods);
			}
		}

		return methods.toArray(new MethodData[methods.size()]);
	}

	public MethodData[] getAllMethodsBySignature(String name, String signature) throws ClassNotFoundException {
		List<MethodData> methods = new ArrayList<>();
		this.getMethodsBySignature(name, signature, methods);

		ClassData parent = this;
		while ((parent = parent.getParentClass()) != null) {
			parent.getMethodsBySignature(name, signature, methods);
		}

		if (interfaces.length > 0) {
			for (String itf : interfaces) {
				ClassData.forName(itf).getMethodsBySignature(name, signature, methods);
			}
		}

		return methods.toArray(new MethodData[methods.size()]);
	}

	private void getMethods(String name, List<MethodData> methods) {
		for (MethodData data : this.methods) {
			if (data.getName().equals(name)) {
				methods.add(data);
			}
		}
	}

	private void getMethodsBySignature(String name, String signature, List<MethodData> methods) {
		for (MethodData data : this.methods) {
			if (data.getName().equals(name) && data.getSignature().equals(signature)) {
				methods.add(data);
			}
		}
	}

	public void addMethod(MethodData method) {
		methods.add(method);
	}

	public ConstructorData[] getConstructors() {
		return constructors.toArray(new ConstructorData[constructors.size()]);
	}

	public void addConstructor(ConstructorData method) {
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

	public boolean isAssignableFrom(ClassData test) {
		if (this.isInterface) {
			for (String iface : test.interfaces) {
				if (iface.equals(className)) {
					return true;
				}
			}
		}

		if (test.className.equals(className)) {
			return true;
		}
		if (javaClass != null && test.javaClass != null) {
			return javaClass.isAssignableFrom(test.javaClass);
		}

		while (!test.className.equals("java/lang/Object")) {
			try {
				test = ClassData.forName(test.parentName);

				if (test.className.equals(className)) {
					return true;
				}
			} catch (ClassNotFoundException e) {
				throw new CompileError("Invalid parent: " + test.parentName);
			}
		}

		return false; // TODO
	}

	public static void registerCornflakesClass(ClassData data) {
		classes.put(data.getClassName(), data);
	}

	@Override
	public String toString() {
		return "class " + getClassName();
	}

	public ClassWriter getClassWriter() {
		return classWriter;
	}

	public void setClassWriter(ClassWriter classWriter) {
		this.classWriter = classWriter;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public Set<FieldData> getFields() {
		return fields;
	}

	public void setInterfaces(String[] intArr) {
		this.interfaces = intArr;
	}

	public String[] getInterfaces() {
		return this.interfaces;
	}

	public boolean isInterface() {
		return isInterface;
	}

	public void setIsInterface(boolean isInterface) {
		this.isInterface = isInterface;
	}

	public boolean is(String type) throws ClassNotFoundException {
		if (className.equals(type)) {
			return true;
		}
		if (Arrays.asList(interfaces).contains(type)) {
			return true;
		}

		ClassData parent = this;
		while ((parent = parent.getParentClass()) != null) {
			if (parent.className.equals(type)) {
				return true;
			}
			if (Arrays.asList(parent.interfaces).contains(type)) {
				return true;
			}
		}

		return false;
	}
}
