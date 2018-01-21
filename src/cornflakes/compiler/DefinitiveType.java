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

		return object(name);
	}

	public static DefinitiveType primitive(String name) {
		DefinitiveType type = new DefinitiveType();
		type.type = Types.unpadSignature(name);
		return type;
	}

	public static DefinitiveType object(String name) {
		return uninitializedObject(name);
	}

	public static DefinitiveType uninitializedObject(String name) {
		DefinitiveType type = new DefinitiveType();
		type.type = name;
		type.init = false;
		return type;
	}

	public static DefinitiveType object(ClassData name) {
		DefinitiveType type = new DefinitiveType();
		type.data = name;
		return type;
	}

	public String getTypeName() {
		return type == null ? getObjectType().getClassName() : type;
	}

	public String getTypeSignature() {
		return Types.padSignature(getTypeName());
	}
	
	public String getAbsoluteTypeSignature() {
		if(Types.isTupleDefinition(getTypeSignature())) {
			return "Lcornflakes/lang/Tuple;";
		}
		return getTypeSignature();
	}
	
	public String getAbsoluteTypeName() {
		if(Types.isTupleDefinition(getTypeSignature())) {
			return "cornflakes/lang/Tuple";
		}
		return getTypeName();
	}

	public boolean isPrimitive() {
		return data == null;
	}

	public boolean isObject() {
		return type == null;
	}
	
	public boolean isTuple() {
		return isObject() && !isNull() && this.data instanceof TupleClassData;
	}

	public ClassData getObjectType() {
		if (!init) {
			try {
				this.data = ClassData.forName(this.type);
			} catch (ClassNotFoundException e) {
				throw new CompileError(e);
			}
		}
		return this.data;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DefinitiveType) {
			DefinitiveType def = (DefinitiveType) obj;
			if (def.isPrimitive()) {
				return def.type.equals(this.type);
			}
			return def.data.getClassName().equals(this.data.getClassName());
		} else if (obj instanceof String) {
			String str = Types.unpadSignature((String) obj);
			if (isPrimitive()) {
				return type.equals(str);
			}
			return data.getClassName().equals(str);
		}
		return false;
	}

	public boolean isNull() {
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
}
