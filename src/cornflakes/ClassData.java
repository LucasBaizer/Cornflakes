package cornflakes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClassData {
	private String simpleClassName;
	private String parentName;
	private String className;
	private String fullName;
	private String sourceName;
	private boolean hasConstructor;
	private byte[] byteCode;
	private ArrayList<String> use = new ArrayList<>();
	private Map<String, String> methods = new LinkedHashMap<>();
	private Map<String, String> memberVariables = new LinkedHashMap<>();

	public ClassData() {
		use("java.lang.Object");
		use("java.lang.String");
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

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public boolean hasMethod(String name) {
		return methods.containsKey(name);
	}

	public String getMethodType(String name) {
		return methods.get(name);
	}

	public void addMethod(String name, String type) {
		methods.put(name, type);
	}
}
