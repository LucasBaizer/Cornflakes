package cornflakes;

public class Types {
	private static final String INTEGER = "-0123456789";

	public static Object parseLiteral(String type, String val) {
		if (type.equals("string")) {
			return val.substring(1, val.length() - 1);
		} else if (type.equals("bool")) {
			return Boolean.parseBoolean(val);
		} else if (type.equals("char")) {
			return val.charAt(1);
		} else if (type.equals("float")) {
			return Float.parseFloat(val);
		} else if (type.equals("double")) {
			return Double.parseDouble(val);
		} else if (type.equals("byte")) {
			return Byte.parseByte(val);
		} else if (type.equals("short")) {
			return Short.parseShort(val);
		} else if (type.equals("int")) {
			return Integer.parseInt(val);
		} else if (type.equals("long")) {
			return Long.parseLong(val);
		}
		throw new CompileError("Invalid literal: " + val);
	}

	/**
	 * @returns null for objects/arrays, but returns a valid string for
	 *          primitives which represents the type of the primitive. String
	 *          types also count as primitives.
	 */
	public static String getType(String x, String context) {
		if (x.equals("true") || x.equals("false")) {
			return "bool";
		}

		if (x.startsWith("'") && x.endsWith("'")) {
			String ch = x.substring(1, x.length() - 1);
			if (ch.isEmpty()) {
				throw new CompileError("Character cannot be empty");
			}
			if (ch.length() > 1) {
				throw new CompileError("Character cannot consist of multiple letters");
			}

			return "char";
		}

		if (x.startsWith("\"") && x.endsWith("\"")) {
			return "string";
		}

		for (char l : x.toCharArray()) {
			if (!INTEGER.contains(Character.toString(l))) {
				if (l == '.') {
					if (Strings.countOccurrences(x, ".") == 1) {
						if (context.equals("float")) {
							return "float";
						} else if (context.equals("double")) {
							return "double";
						} else {
							throw new CompileError("A non-fractional type was expected, but one was given");
						}
					} else {
						throw new CompileError("Unexpected token: " + l);
					}
				} else {
					return null;
				}
			}
		}

		if (context.equals("byte")) {
			return "byte";
		} else if (context.equals("short")) {
			return "short";
		} else if (context.equals("int")) {
			return "int";
		} else if (context.equals("long")) {
			return "long";
		}

		return "int";
	}

	public static boolean isPrimitive(String name) {
		switch (name) {
			case "void":
			case "bool":
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
			case "bool":
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
		if (type == Void.class) {
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

	public static String getTypeSignature(String type) {
		if (type.equals("void")) {
			return "V";
		} else if (type.equals("bool")) {
			return "Z";
		} else if (type.equals("byte")) {
			return "B";
		} else if (type.equals("char")) {
			return "C";
		} else if (type.equals("double")) {
			return "D";
		} else if (type.equals("float")) {
			return "F";
		} else if (type.equals("int")) {
			return "I";
		} else if (type.equals("long")) {
			return "J";
		} else if (type.equals("short")) {
			return "S";
		} else if (type.equals("string")) {
			return "Ljava/lang/String;";
		} else if (!type.endsWith("[]")) {
			return "L" + Strings.transformClassName(type) + ";";
		} else {
			return "[" + getTypeSignature(type.replace("[", "").replace("]", ""));
		}
	}

	public static Class<?> getTypeFromSignature(String sig) {
		switch (sig) {
			case "V":
				return Void.class;
			case "Z":
				return boolean.class;
			case "B":
				return byte.class;
			case "C":
				return char.class;
			case "D":
				return double.class;
			case "F":
				return float.class;
			case "I":
				return int.class;
			case "J":
				return long.class;
			case "S":
				return short.class;
		}

		if (sig.startsWith("[")) {
			try {
				return Class.forName(sig.replace('/', '.') + ";");
			} catch (ClassNotFoundException e) {
				throw new CompileError(e);
			}
		} else {
			try {
				return Class.forName(sig.replace('/', '.'));
			} catch (ClassNotFoundException e) {
				throw new CompileError(e);
			}
		}
	}
}
