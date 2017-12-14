package cornflakes.compiler;

public class Types {
	private static final String INTEGER = "-0123456789";

	public static boolean isSuitable(String target, String test) {
		if (target.equals(test)) {
			return true;
		}

		if (isPrimitive(target) || isPrimitive(test)) {
			// TODO check more
			return false;
		}

		ClassData targetClass = getTypeFromSignature(unpadSignature(target));
		ClassData testClass = getTypeFromSignature(unpadSignature(test));
		return targetClass.isAssignableFrom(testClass);
	}

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

	public static String padSignature(String sig) {
		if (sig.length() == 1) {
			return sig;
		}
		if (!sig.endsWith(";")) {
			sig += ";";
		}
		if (sig.startsWith("[")) {
			return sig;
		} else {
			if (!sig.startsWith("L")) {
				return "L" + sig;
			}
		}
		return sig;
	}

	public static String unpadSignature(String sig) {
		if (sig.startsWith("L")) {
			sig = sig.substring(1);
		}
		if (sig.endsWith(";")) {
			sig = sig.substring(0, sig.length() - 1);
		}
		return sig;
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
						} else if (!context.isEmpty()) {
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
		if (type == Void.class || type.equals(Void.TYPE)) {
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
		type = unpadSignature(type);
		
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

	public static ClassData getTypeFromSignature(String sig) {
		switch (sig) {
		case "V":
			return ClassData.fromJavaClass(Void.class);
		case "Z":
			return ClassData.fromJavaClass(boolean.class);
		case "B":
			return ClassData.fromJavaClass(byte.class);
		case "C":
			return ClassData.fromJavaClass(char.class);
		case "D":
			return ClassData.fromJavaClass(double.class);
		case "F":
			return ClassData.fromJavaClass(float.class);
		case "I":
			return ClassData.fromJavaClass(int.class);
		case "J":
			return ClassData.fromJavaClass(long.class);
		case "S":
			return ClassData.fromJavaClass(short.class);
		}

		if (sig.startsWith("[")) {
			try {
				return ClassData.forName(sig);
			} catch (ClassNotFoundException e) {
				throw new CompileError(e);
			}
		} else {
			try {
				return ClassData.forName(unpadSignature(sig));
			} catch (ClassNotFoundException e) {
				throw new CompileError(e);
			}
		}
	}
}
