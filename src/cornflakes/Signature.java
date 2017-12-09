package cornflakes;

public class Signature {
	public static boolean isPrimitive(String name) {
		switch (name) {
		case "void":
		case "boolean":
		case "byte":
		case "char":
		case "short":
		case "int":
		case "long":
		case "float":
		case "double":
			return true;
		default:
			return false;
		}
	}

	public static Class<?> getClassFromPrimitive(String primitive) {
		switch (primitive) {
		case "void":
			return Void.class;
		case "boolean":
			return boolean.class;
		case "byte":
			return byte.class;
		case "char":
			return char.class;
		case "short":
			return short.class;
		case "int":
			return int.class;
		case "long":
			return long.class;
		case "float":
			return float.class;
		case "double":
			return double.class;
		default:
			throw new CompileError("Unresolved type: " + primitive);
		}
	}

	public static String getTypeSignature(Class<?> type) {
		if (type == null || type == Void.class) {
			return "V";
		} else if (type == boolean.class || type == Boolean.class) {
			return "Z";
		} else if (type == byte.class || type == Byte.class) {
			return "B";
		} else if (type == char.class || type == Character.class) {
			return "C";
		} else if (type == double.class || type == Double.class) {
			return "D";
		} else if (type == float.class || type == Float.class) {
			return "F";
		} else if (type == int.class || type == Integer.class) {
			return "I";
		} else if (type == long.class || type == Long.class) {
			return "J";
		} else if (type == short.class || type == Short.class) {
			return "S";
		} else if (!type.isArray()) {
			return "L" + Strings.transformClassName(type.getName()) + ";";
		} else {
			return "[" + getTypeSignature(type.getComponentType());
		}
	}
}
