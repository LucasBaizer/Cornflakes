package cornflakes.compiler;

public class DefinitiveType {
	private String type;
	private ClassData data;
	private boolean init = true;

	private DefinitiveType() {
	}

	public static DefinitiveType assume(String name) {
		if (Types.isPrimitive(name)) {
			return primitive(name);
		}
		if (name.equals("null")) {
			return primitive(name);
		}

		return object(name);
	}

	public static DefinitiveType primitive(String name) {
		DefinitiveType type = new DefinitiveType();
		type.type = Types.unpadSignature(name);
		return type;
	}

	public static DefinitiveType object(String name) {
		if (name == null) {
			throw new CompileError("Invalid object");
		}
		if (name.equals("string")) {
			name = "Ljava/lang/String;";
		}
		if (Types.isPrimitive(name)) {
			return primitive(name);
		}

		DefinitiveType type = new DefinitiveType();
		type.type = Types.unpadSignature(name);
		type.init = false;
		return type;
	}

	public static DefinitiveType object(ClassData name) {
		DefinitiveType type = new DefinitiveType();
		type.data = name;
		type.type = name.getClassName();
		return type;
	}

	public String getTypeName() {
		return type == null ? getObjectType().getClassName() : type;
	}

	public String getTypeSignature() {
		if (getTypeName().equals("string")) {
			return "string";
		}
		return Types.padSignature(getTypeName());
	}

	public String getAbsoluteTypeSignature() {
		if (Types.isTupleDefinition(getTypeSignature())) {
			return "Lcornflakes/lang/Tuple;";
		}
		return getTypeSignature();
	}

	public String getAbsoluteTypeName() {
		if (Types.isTupleDefinition(getTypeSignature())) {
			return "cornflakes/lang/Tuple";
		}
		return getTypeName();
	}

	public boolean isPrimitive() {
		if (!init) {
			return false;
		}
		return data == null;
	}

	public boolean isObject() {
		if (!init) {
			getObjectType();
			return true;
		}

		return !isPrimitive();
	}

	public boolean isTuple() {
		return isObject() && !isNull() && this.data instanceof TupleClassData;
	}

	public boolean isPointer() {
		return isObject() && !isNull() && this.data instanceof PointerClassData;
	}

	public ClassData getObjectType() {
		if (!init) {
			try {
				this.data = ClassData.forName(this.type);
				this.init = true;
			} catch (ClassNotFoundException e) {
				throw new CompileError(e);
			}
		}
		return this.data;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		result = prime * result + (init ? 1231 : 1237);
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DefinitiveType) {
			DefinitiveType def = (DefinitiveType) obj;
			return def.getTypeName().equals(this.getTypeName());
		} else if (obj instanceof String) {
			String str = Types.unpadSignature((String) obj);
			if (isTuple()) {
				if (str.equals("cornflakes/lang/Tuple")) {
					return true;
				}
			}
			return getTypeName().equals(str);
		}
		return false;
	}

	public boolean isNull() {
		if (type != null && type.equals("null")) {
			return true;
		}
		if (isObject()) {
			return this.type == null;
		}

		return false;
	}

	@Override
	public String toString() {
		if (isNull()) {
			return "_null";
		}
		return getTypeSignature();
	}

	public boolean isArray() {
		return getTypeName().startsWith("[");
	}
}
