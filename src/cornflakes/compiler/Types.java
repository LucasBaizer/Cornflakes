package cornflakes.compiler;

import org.objectweb.asm.Opcodes;

public class Types implements Opcodes {
	public static final int STORE = 0;
	public static final int LOAD = 1;
	public static final int PUSH = 2;
	public static final int RETURN = 3;

	private static final String INTEGER = "-0123456789";

	public static int getArrayOpcode(int op, String type) {
		if (type == null) {
			if (op == STORE) {
				return AASTORE;
			} else if (op == LOAD) {
				return AALOAD;
			}
		}

		if (type.equals("[B")) {
			if (op == STORE) {
				return BASTORE;
			} else if (op == LOAD) {
				return BALOAD;
			}
		}
		if (type.equals("[J")) {
			if (op == STORE) {
				return LASTORE;
			} else if (op == LOAD) {
				return LALOAD;
			}
		}
		if (type.equals("[I")) {
			if (op == STORE) {
				return IASTORE;
			} else if (op == LOAD) {
				return IALOAD;
			}
		}
		if (type.equals("[S")) {
			if (op == STORE) {
				return SASTORE;
			} else if (op == LOAD) {
				return SALOAD;
			}
		}
		if (type.equals("[F")) {
			if (op == STORE) {
				return FASTORE;
			} else if (op == LOAD) {
				return FALOAD;
			}
		}
		if (type.equals("[D")) {
			if (op == STORE) {
				return DASTORE;
			} else if (op == LOAD) {
				return DALOAD;
			}
		}

		if (op == STORE) {
			return AASTORE;
		} else if (op == LOAD) {
			return AALOAD;
		}

		throw new CompileError("Could not get array opcode for type '" + type + "' with code " + op);
	}

	public static int getOpcode(int op, String type) {
		if (type == null) {
			if (op == STORE) {
				return ASTORE;
			} else if (op == LOAD) {
				return ALOAD;
			} else if (op == RETURN) {
				return ARETURN;
			}

			throw new CompileError("APUSH is not a valid opcode");
		}

		if (type.equals("byte") || type.equals("B")) {
			if (op == STORE) {
				return ISTORE;
			} else if (op == LOAD) {
				return ILOAD;
			} else if (op == PUSH) {
				return BIPUSH;
			} else if (op == RETURN) {
				return IRETURN;
			}
		} else if (type.equals("bool") || type.equals("Z")) {
			if (op == STORE) {
				return ISTORE;
			} else if (op == LOAD) {
				return ILOAD;
			} else if (op == PUSH) {
				return SIPUSH;
			} else if (op == RETURN) {
				return IRETURN;
			}
		} else if (type.equals("i16") || type.equals("short") || type.equals("S")) {
			if (op == STORE) {
				return ISTORE;
			} else if (op == LOAD) {
				return ILOAD;
			} else if (op == PUSH) {
				return SIPUSH;
			} else if (op == RETURN) {
				return IRETURN;
			}
		} else if (type.equals("i32") || type.equals("int") || type.equals("I")) {
			if (op == STORE) {
				return ISTORE;
			} else if (op == LOAD) {
				return ILOAD;
			} else if (op == PUSH) {
				return LDC;
			} else if (op == RETURN) {
				return IRETURN;
			}
		} else if (type.equals("i64") || type.equals("long") || type.equals("J")) {
			if (op == STORE) {
				return LSTORE;
			} else if (op == LOAD) {
				return LLOAD;
			} else if (op == PUSH) {
				return LDC;
			} else if (op == RETURN) {
				return LRETURN;
			}
		} else if (type.equals("char") || type.equals("C")) {
			if (op == STORE) {
				return ISTORE;
			} else if (op == LOAD) {
				return ILOAD;
			} else if (op == PUSH) {
				return SIPUSH;
			} else if (op == RETURN) {
				return IRETURN;
			}
		} else if (type.equals("f32") || type.equals("float") || type.equals("F")) {
			if (op == STORE) {
				return FSTORE;
			} else if (op == LOAD) {
				return FLOAD;
			} else if (op == PUSH) {
				return LDC;
			} else if (op == RETURN) {
				return FRETURN;
			}
		} else if (type.equals("f64") || type.equals("double") || type.equals("D")) {
			if (op == STORE) {
				return DSTORE;
			} else if (op == LOAD) {
				return DLOAD;
			} else if (op == PUSH) {
				return LDC;
			} else if (op == RETURN) {
				return DRETURN;
			}
		}

		if (op == STORE) {
			return ASTORE;
		} else if (op == LOAD) {
			return ALOAD;
		} else if (op == RETURN) {
			return ARETURN;
		} else {
			if (type.equals("string") || type.equals("Ljava/lang/String;")) {
				if (op == PUSH) {
					return LDC;
				}
			}
		}

		throw new CompileError("Could not get opcode for type '" + type + "' with code " + op);
	}

	/**
	 * @return padded wrapper signature
	 */
	public static String getWrapperType(String type) {
		switch (type) {
		case "I":
		case "i32":
			return "Ljava/lang/Integer";
		case "S":
		case "i16":
			return "Ljava/lang/Short";
		case "J":
		case "i64":
			return "Ljava/lang/Long";
		case "B":
		case "byte":
			return "Ljava/lang/Byte";
		case "Z":
		case "bool":
			return "Ljava/lang/Boolean";
		case "F":
		case "f32":
			return "Ljava/lang/Float";
		case "D":
		case "f64":
			return "Ljava/lang/Double";
		case "C":
		case "char":
			return "Ljava/lang/Character";
		default:
			throw new CompileError("Unknown type: " + type);
		}
	}

	public static boolean isNumeric(String type) {
		if (type == null)
			return false;
		return isPrimitive(type);
	}

	public static boolean isSuitable(String target, String test) {
		if (test == null) {
			return true;
		}
		if (target.equals(test)) {
			return true;
		}

		boolean isInt = target.equals("i32") || target.equals("I") || target.equals("int");
		boolean isShort = target.equals("i16") || target.equals("S") || target.equals("short");
		boolean isByte = target.equals("byte") || target.equals("B");
		boolean isChar = target.equals("char") || target.equals("C");
		if (isInt) {
			return isShort || isByte || isChar;
		} else if (isShort) {
			return isByte || isChar;
		}

		target = unpadSignature(target);
		test = unpadSignature(test);

		if (isPrimitive(target) || isPrimitive(test)) {
			return false;
		}

		ClassData targetClass = getTypeFromSignature(target);
		ClassData testClass = getTypeFromSignature(test);
		return targetClass.isSuperclassOf(testClass);
	}

	public static Object parseLiteral(String type, String val) {
		if (type.equals("string") || type.equals("Ljava/lang/String;")) {
			return val.substring(1, val.length() - 1);
		} else if (type.equals("bool") || type.equals("Z")) {
			return Boolean.parseBoolean(val);
		} else if (type.equals("char") || type.equals("C")) {
			return val.charAt(1);
		} else if (type.equals("f32") || type.equals("F")) {
			return Float.parseFloat(val);
		} else if (type.equals("f64") || type.equals("D")) {
			return Double.parseDouble(val);
		} else if (type.equals("byte") || type.equals("B")) {
			return Byte.parseByte(val);
		} else if (type.equals("i16") || type.equals("S")) {
			return Short.parseShort(val);
		} else if (type.equals("i32") || type.equals("I")) {
			return Integer.parseInt(val);
		} else if (type.equals("i64") || type.equals("L")) {
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
		if (!sig.startsWith("[") && sig.endsWith(";")) {
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

		if (x.startsWith("\"") && x.endsWith("\"") && !Strings.contains(x, "+")) {
			return "string";
		}

		for (char l : x.toCharArray()) {
			if (!INTEGER.contains(Character.toString(l))) {
				if (l == '.') {
					if (Strings.countOccurrences(x, ".") == 1) {
						if (context.equals("f32") || context.equals("F")) {
							return "f32";
						} else if (context.equals("f64") || context.equals("D")) {
							return "f64";
						} else if (context != null && !context.isEmpty()) {
							throw new CompileError("A non-fractional type was expected, but one was given");
						} else {
							return "f32";
						}
					}
				} else {
					return null;
				}
			}
		}

		if (context != null) {
			if (context.equals("byte") || context.equals("B")) {
				return "byte";
			} else if (context.equals("i16") || context.equals("S")) {
				return "i16";
			} else if (context.equals("i32") || context.equals("I")) {
				return "i32";
			} else if (context.equals("i64") || context.equals("J")) {
				return "i64";
			}
		}

		return "i32";
	}

	public static boolean isPrimitive(String name) {
		if (name == null)
			return false;
		switch (name) {
		case "I":
		case "Z":
		case "B":
		case "C":
		case "S":
		case "J":
		case "F":
		case "D":
		case "void":
		case "bool":
		case "byte":
		case "char":
		case "i16":
		case "i32":
		case "i64":
		case "f32":
		case "f64":
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
		case "i16":
			return short.class;
		case "i32":
			return int.class;
		case "i64":
			return long.class;
		case "f32":
			return float.class;
		case "f64":
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

		if (type.length() == 1) {
			return type;
		}

		if (type.equals("void")) {
			return "V";
		} else if (type.equals("bool")) {
			return "Z";
		} else if (type.equals("byte")) {
			return "B";
		} else if (type.equals("char")) {
			return "C";
		} else if (type.equals("f64")) {
			return "D";
		} else if (type.equals("f32")) {
			return "F";
		} else if (type.equals("i32")) {
			return "I";
		} else if (type.equals("i64")) {
			return "J";
		} else if (type.equals("i16")) {
			return "S";
		} else if (type.equals("string")) {
			return "Ljava/lang/String;";
		} else if (!type.endsWith("[]") && !type.startsWith("[")) {
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
				return ClassData.forName(unpadSignature(sig));
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
