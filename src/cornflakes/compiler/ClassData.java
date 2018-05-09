package cornflakes.compiler;

import static cornflakes.compiler.Operator.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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

	public static final int TYPE_CLASS = 1;
	public static final int TYPE_STRUCT = 2 | TYPE_CLASS;
	public static final int TYPE_INTERFACE = 4;
	public static final int TYPE_ENUM = 8 | TYPE_CLASS;
	public static final int TYPE_ANNOTATION = 16 | TYPE_INTERFACE;

	private static ClassData currentClass;
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
	private Map<String, String> macros = new HashMap<>();
	private Set<GenericParameter> genericParameters = new HashSet<>();
	private ClassWriter classWriter;
	private Class<?> javaClass;
	private boolean isGetIndexed;
	private boolean isSetIndexed;
	private boolean isInterface;
	private int classType = TYPE_CLASS;
	private int lambdas;

	public static ClassData forName(String name) throws ClassNotFoundException {
		if (Types.isTupleDefinition(name)) {
			return new TupleClassData(name);
		} else if (Types.isPointer(name)) {
			return PointerClassData.from(name);
		} else {
			name = Strings.transformClassName(Types.unpadSignature(name));

			if (classes.containsKey(name)) {
				return classes.get(name);
			}

			return fromJavaClass(Class.forName(name.replace('/', '.')));
		}
	}

	public static ClassData fromJavaClass(Class<?> cls) {
		String t = Strings.transformClassName(cls.getName());

		if (classes.containsKey(t)) {
			return classes.get(t);
		}

		ClassData container;
		if (cls.isInterface()) {
			int i = 0;
			for (Method method : cls.getMethods()) {
				if (!method.isDefault()) {
					if (++i == 2) { // there is more than 1 interface method
						container = new ClassData(false);
						break;
					}
				}
			}
			container = new LambdaClassData();
		} else {
			container = new ClassData(false);
		}
		container.javaClass = cls;
		container.setIsInterface(cls.isInterface());
		container.setClassName(t);
		container.setSimpleClassName(cls.getSimpleName());
		container.setParentName(true,
				cls.getSuperclass() == null ? "java/lang/Object" : cls.getSuperclass().getName().replace('.', '/'));

		if (cls.getGenericSuperclass() != null) {
			if (cls.getGenericSuperclass() instanceof ParameterizedType) {
				Set<GenericParameter> par = new HashSet<>();

				for (Type type : ((ParameterizedType) cls.getGenericSuperclass()).getActualTypeArguments()) {
					par.add(new GenericParameter(type.getTypeName()));
				}

				container.genericParameters = par;
			}
		}

		List<String> ifs = new ArrayList<>();
		for (Class<?> c : cls.getInterfaces()) {
			ifs.add(Strings.transformClassName(c.getName()));
		}
		container.setInterfaces(ifs.toArray(new String[ifs.size()]));

		for (Method method : cls.getDeclaredMethods()) {
			container.addMethod(MethodData.fromJavaMethod(container, method));
		}

		for (Method method : cls.getMethods()) {
			MethodData data = MethodData.fromJavaMethod(cls == Object.class || method.getDeclaringClass() == cls
					? container : ClassData.fromJavaClass(method.getDeclaringClass()), method);
			if (!container.methods.contains(data)) {
				container.addMethod(data);
			}
		}

		if (!cls.isInterface()) {
			for (Constructor<?> constructor : cls.getConstructors()) {
				container.addConstructor(ConstructorData.fromJavaConstructor(container, constructor));
			}
			for (Field field : cls.getDeclaredFields()) {
				container.fields.add(new FieldData(container, field.getName(),
						DefinitiveType.assume(Types.getTypeSignature(field.getType())), field.getModifiers()));
			}
		}

		classes.put(t, container);
		return container;
	}

	public ClassData() {
		this(true);
	}

	ClassData(boolean use) {
		if (use) {
			use("java.lang.Object", "object");
			use("java.lang.String", "string");
			use("java.lang.Boolean", "bool");
			use("java.lang.Integer", "i32");
			use("java.lang.Double", "f64");
			use("java.lang.Float", "f32");
			use("java.lang.Long", "i64");
			use("java.lang.Byte", "i8");
			use("java.lang.Short", "i16");
			use("java.lang.Character", "char");
			use("java.lang.System");
			use("java.lang.Thread");
			use("java.lang.Throwable");
			use("java.lang.Exception");
			use("java.lang.RuntimeException");
			use("java.lang.Error");
			use("cornflakes.lang.Tuple");
			use("cornflakes.lang.FunctionalIterator");
			use("cornflakes.lang.Range");
			use("cornflakes.lang.I32Range");
			use("cornflakes.lang.F32Range");
			useMacro("println", "System.out.println");
			useMacro("iter", "FunctionalIterator");
			useMacro("range", "Range.from");
		}
	}

	public MethodData[] getOperatorOverloads(int op) throws ClassNotFoundException {
		return getAllMethods(getOperatorOverloadFunction(op));
	}

	public boolean hasOperatorOverload(int op) throws ClassNotFoundException {
		return getAllMethods(getOperatorOverloadFunction(op)).length > 0;
	}

	public void use(String use) {
		use(use, use.substring(use.lastIndexOf('/') + 1));
	}

	public void use(String use, String as) {
		try {
			ClassData.forName(use);
			this.use.put(as, Strings.transformClassName(use));
		} catch (ClassNotFoundException e) {
			throw new CompileError("Unresolved class: " + Types.beautify(use));
		}
	}

	public void useMacro(String macro, String result) {
		macros.put(macro, result);
	}

	public String resolveMacro(String macro) {
		return macros.get(macro);
	}

	public boolean hasMacro(String name) {
		return macros.containsKey(name);
	}

	public boolean isUsing(String name) {
		for (Entry<String, String> use : this.use.entrySet()) {
			if (use.getKey().equals(use) || use.getValue().endsWith("/" + name)) {
				return true;
			}
		}
		return false;
	}

	public DefinitiveType resolveClass(String name) {
		return resolveClass(name, true);
	}

	public DefinitiveType resolveClass(String name, boolean prim) {
		boolean arrayType = false;

		if (name.endsWith("[]")) {
			name = name.replace("[]", "");
			arrayType = true;
		}

		if (prim) {
			if (Types.isPrimitive(name)) {
				String sig = Types.getTypeSignature(Types.getClassFromPrimitive(name));
				return arrayType ? DefinitiveType.object("[" + sig) : DefinitiveType.primitive(sig);
			}
		}

		Strings.handleLetterString(name, Strings.TYPE);

		try {
			return DefinitiveType.object(ClassData.forName(name));
		} catch (ClassNotFoundException e) {
			for (Entry<String, String> use : this.use.entrySet()) {
				if (use.getKey().equals(name) || use.getValue().equals(name.replace('.', '/'))
						|| use.getValue().endsWith("/" + name)) {
					return DefinitiveType.assume(
							arrayType ? "[L" + use.getValue().replace('.', '/') : use.getValue().replace('.', '/'));
				}
			}
			throw new CompileError("Unresolved type: " + name);
		}
	}

	public boolean isTuple() {
		return this instanceof TupleClassData;
	}

	public boolean isPointer() {
		return this instanceof PointerClassData;
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
		setParentName(false, parentName);
	}

	public void setParentName(boolean isJavaClass, String parentName) {
		this.parentName = parentName;

		try {
			if (!isJavaClass) {
				ClassData parent = ClassData.forName(parentName);
				this.isGetIndexed = parent.isGetIndexed;
				this.isSetIndexed = parent.isSetIndexed;
			}
		} catch (ClassNotFoundException e) {
			throw new CompileError(e);
		}
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

	public boolean isAssignableFrom(String test) throws ClassNotFoundException {
		return isAssignableFrom(ClassData.forName(test));
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
				throw new CompileError("Invalid parent: " + Types.beautify(test.parentName));
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

	public boolean is(DefinitiveType type) throws ClassNotFoundException {
		return is(type.getAbsoluteTypeSignature());
	}

	public boolean is(String type) throws ClassNotFoundException {
		type = Types.unpadSignature(Strings.transformClassName(type));
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

	public boolean isGetIndexedClass() {
		return isGetIndexed;
	}

	public void setGetIndexedClass(boolean isIndexed) {
		this.isGetIndexed = isIndexed;
	}

	public boolean isSetIndexedClass() {
		return isSetIndexed;
	}

	public void setSetIndexedClass(boolean isIndexed) {
		this.isSetIndexed = isIndexed;
	}

	public boolean hasGenericParameters() {
		return this.genericParameters.size() > 0;
	}

	public boolean hasGeneric(String name) {
		return getGeneric(name) != null;
	}

	public GenericParameter getGeneric(String name) {
		for (GenericParameter par : genericParameters) {
			if (par.getName().equals(name)) {
				return par;
			}
		}
		return null;
	}

	public GenericParameter[] getGenerics() {
		return this.genericParameters.toArray(new GenericParameter[this.genericParameters.size()]);
	}

	public boolean isJavaClass() {
		return javaClass != null;
	}

	public Map<String, String> getMacros() {
		return macros;
	}

	public static ClassData getCurrentClass() {
		return currentClass;
	}

	public static void setCurrentClass(ClassData currentClass) {
		ClassData.currentClass = currentClass;
	}

	public int getClassType() {
		return classType;
	}

	public void setClassType(int classType) {
		this.classType = classType;
	}

	public int getLambdas() {
		return lambdas;
	}

	public void setLambdas(int lambdas) {
		this.lambdas = lambdas;
	}

	public void addLambda() {
		this.lambdas++;
	}
}
