package cornflakes;

import java.util.ArrayList;

public class ClassData {
	private String simpleClassName;
	private String parentName;
	private String className;
	private String sourceName;
	private boolean hasConstructor;
	private byte[] byteCode;
	private ArrayList<String> use = new ArrayList<>();

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

	public String resolve(String name) {
		try {
			return Strings.transformClassName(Class.forName(name).getName());
		} catch (ClassNotFoundException e) {
			if (name.equals("string")) {
				return "java/lang/String";
			} else if(name.equals("object")) {
				return "java/lang/Object";
			}

			for (String use : this.use) {
				if (use.endsWith("/" + name)) {
					return use;
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
}
