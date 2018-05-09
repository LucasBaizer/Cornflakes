package cornflakes.compiler;

import org.objectweb.asm.Opcodes;

public class Types implements Opcodes {
	public static final int STORE = 0;
	public static final int LOAD = 1;
	public static final int PUSH = 2;
	public static final int RETURN = 3;

	private static final String INTEGER = "-0123456789";

	public static String primitiveToJava(String prim) {
		if (prim.equals("i8") || prim.equals("B")) {
			return "byte";
		} else if (prim.equals("i16") || prim.equals("S")) {
			return "short";
		} else if (prim.equals("i32") || prim.equals("I")) {
			return "int";
		} else if (prim.equals("i64") || prim.equals("J")) {
			return "long";
		} else if (prim.equals("f32") || prim.equals("F")) {
			return "float";
		} else if (prim.equals("f64") || prim.equals("D")) {
			return "double";
		} else if (prim.equals("bool") || prim.equals("Z")) {
			return "boolean";
		} else if (prim.equals("C")) {
			return "char";
		}

		return prim;
	}

	public static String primitiveToSignature(String prim) {
		if (prim.equals("i8") || prim.equals("byte")) {
			return "B";
		} else if (prim.equals("i16") || prim.equals("short")) {
			return "S";
		} else if (prim.equals("i32") || prim.equals("int")) {
			return "I";
		} else if (prim.equals("i64") || prim.equals("long")) {
			return "J";
		} else if (prim.equals("f32") || prim.equals("float")) {
			return "F";
		} else if (prim.equals("f64") || prim.equals("double")) {
			return "D";
		} else if (prim.equals("bool") || prim.equals("bool")) {
			return "Z";
		} else if (prim.equals("C") || prim.equals("char")) {
			return "C";
		}

		return prim;
	}

	public static String primitiveToCornflakes(String prim) {
		if (prim.equals("B") || prim.equals("byte")) {
			return "i8";
		} else if (prim.equals("S") || prim.equals("short")) {
			return "i16";
		} else if (prim.equals("I") || prim.equals("int")) {
			return "i32";
		} else if (prim.equals("J") || prim.equals("long")) {
			return "i64";
		} else if (prim.equals("F") || prim.equals("float")) {
			return "f32";
		} else if (prim.equals("D") || prim.equals("double")) {
			return "f64";
		} else if (prim.equals("Z") || prim.equals("bool")) {
			return "bool";
		} else if (prim.equals("C") || prim.equals("char")) {
			return "char";
		}

		return prim;
	}

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

		throw new CompileError("Could not get array opcode for type " + Types.beautify(type) + " with code " + op);
	}

	public static boolean isTupleDefinition(String def) {
		return def.startsWith("(") && def.endsWith(")") && Strings.countOccurrences(def, "(") == 1
				&& Strings.countOccurrences(def, ")") == 1;
	}

	public static boolean isPointer(String def) {
		if (def.endsWith("*"))
			return true;

		switch (def) {
			case "cornflakes/lang/I8Pointer":
			case "cornflakes/lang/I16Pointer":
			case "cornflakes/lang/I32Pointer":
			case "cornflakes/lang/I64Pointer":
			case "cornflakes/lang/F32Pointer":
			case "cornflakes/lang/F64Pointer":
			case "cornflakes/lang/BoolPointer":
			case "cornflakes/lang/CharPointer":
			case "cornflakes/lang/ObjectPointer":
				return true;
		}

		return false;
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

		if (type.equals("i8") || type.equals("B")) {
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

		throw new CompileError(
				"Could not get opcode for type " + Types.beautify(type) + " (raw " + type + ") with code " + op);
	}

	public static String beautify(DefinitiveType type) {
		return beautify(type.getTypeSignature());
	}

	public static String beautify(String txt1) {
		if (Types.isTupleDefinition(txt1)) {
			return txt1;
		}

		boolean array = false;
		if (txt1.startsWith("[")) {
			txt1 = txt1.substring(1);
			array = true;
		}
		String txt2 = Types.unpadSignature(txt1);
		if (Types.isPrimitive(txt2)) {
			String ret = txt2;
			if (txt2.equals("I")) {
				ret = "i32";
			} else if (txt2.equals("S")) {
				ret = "i16";
			} else if (txt2.equals("B")) {
				ret = "i8";
			} else if (txt2.equals("J")) {
				ret = "i64";
			} else if (txt2.equals("F")) {
				ret = "f32";
			} else if (txt2.equals("D")) {
				ret = "f64";
			} else if (txt2.equals("Z")) {
				ret = "bool";
			} else if (txt2.equals("C")) {
				ret = "char";
			}

			if (array) {
				ret += "[]";
			}
			return ret;
		}

		if (txt2.equals("string") || txt2.equals("object")) {
			return txt2;
		}

		try {
			String ret = ClassData.forName(txt2).getClassName().replace('/', '.');
			if (array) {
				ret += "[]";
			}
			return ret;
		} catch (ClassNotFoundException e) {
			throw new CompileError(e);
		}
	}

	/**
	 * @return padded wrapper signature
	 */
	public static String getWrapperType(String type) {
		switch (type) {
			case "I":
			case "i32":
				return "java/lang/Integer";
			case "S":
			case "i16":
				return "java/lang/Short";
			case "J":
			case "i64":
				return "java/lang/Long";
			case "B":
			case "i8":
				return "java/lang/Byte";
			case "Z":
			case "bool":
				return "java/lang/Boolean";
			case "F":
			case "f32":
				return "java/lang/Float";
			case "D":
			case "f64":
				return "java/lang/Double";
			case "C":
			case "char":
				return "java/lang/Character";
			case "i32*":
				return "cornflakes/lang/I32Pointer";
			default:
				throw new CompileError("Unknown type: " + type);
		}
	}

	public static boolean isNumeric(String type) {
		if (type == null)
			return false;
		return isPrimitive(type);
	}

	public static boolean isSuitable(DefinitiveType target, DefinitiveType test) {
		if (test == null || test.isNull()) {
			return target.isObject();
		}
		if (target.isObject() && test.isObject()) {
			return target.getObjectType().isAssignableFrom(test.getObjectType());
		}

		return isSuitable(target.getTypeSignature(), test.getTypeSignature());
	}

	public static boolean isSuitable(String target, String test) {
		if (test == null || test.equals("null")) {
			return !Types.isPrimitive(target);
		}
		if (target.equals(test)) {
			return true;
		}

		boolean tInt = target.equals("i32") || target.equals("I") || target.equals("int");
		boolean tShort = target.equals("i16") || target.equals("S") || target.equals("short");
		boolean tByte = target.equals("i8") || target.equals("B") || target.equals("byte");
		boolean tChar = target.equals("char") || target.equals("C");
		boolean sInt = test.equals("i32") || test.equals("I") || test.equals("int");
		boolean sShort = test.equals("i16") || test.equals("S") || test.equals("short");
		boolean sByte = test.equals("i8") || test.equals("B") || test.equals("byte");
		boolean sChar = test.equals("char") || test.equals("C");

		if (tInt) {
			return sShort || sByte || sChar;
		} else if (tShort) {
			return sByte || sChar;
		} else if (tByte) {
			return sChar;
		} else if (tChar) {
			return sInt || sShort || sByte;
		}

		target = unpadSignature(target);
		test = unpadSignature(test);

		if (isPrimitive(target) || isPrimitive(test)) {
			return false;
		}

		ClassData targetClass = getTypeFromSignature(target);
		ClassData testClass = getTypeFromSignature(test);
		return targetClass.isAssignableFrom(testClass);
	}

	public static Object parseLiteral(String type, String val) {
		if (type.equals("string") || type.equals("Ljava/lang/String;")) {
			return val.substring(1, val.length() - 1);
		} else if (type.equals("bool") || type.equals("Z")) {
			return Boolean.parseBoolean(val);
		} else if (type.equals("char") || type.equals("C")) {
			return (int) val.charAt(1);
		} else if (type.equals("f32") || type.equals("F")) {
			if (val.toLowerCase().endsWith("f")) {
				val = val.substring(0, val.length() - 1);
			}
			return Float.parseFloat(val);
		} else if (type.equals("f64") || type.equals("D")) {
			if (val.toLowerCase().endsWith("d")) {
				val = val.substring(0, val.length() - 1);
			}
			return Double.parseDouble(val);
		} else if (type.equals("i8") || type.equals("B")) {
			Byte.parseByte(val);
			return Integer.parseInt(val);
		} else if (type.equals("i16") || type.equals("S")) {
			Short.parseShort(val);
			return Integer.parseInt(val);
		} else if (type.equals("i32") || type.equals("I")) {
			return Integer.parseInt(val);
		} else if (type.equals("i64") || type.equals("J")) {
			if (val.toLowerCase().endsWith("l")) {
				val = val.substring(0, val.length() - 1);
			}
			return Long.parseLong(val);
		}
		throw new CompileError("Invalid literal: " + val);
	}

	public static String padSignature(String sig) {
		if (sig == null || sig.equals("null")) {
			return sig;
		}

		if (Types.isTupleDefinition(sig)) {
			return sig;
		}

		if (Types.isPrimitive(sig)) {
			sig = Types.primitiveToSignature(sig);
		}
		if (sig.length() == 1) {
			return sig;
		}
		if (sig.length() == 2 && sig.startsWith("[")) {
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

		String xl = x.toLowerCase();
		if (xl.matches("([1-9.]+?)[a-z]")) {
			if (xl.endsWith("l")) {
				return "i64";
			} else if (xl.endsWith("f")) {
				return "f32";
			} else if (xl.endsWith("d")) {
				return "f64";
			}
		}

		boolean frac = false;
		for (char l : x.toCharArray()) {
			if (!INTEGER.contains(Character.toString(l))) {
				if (l == '.') {
					if (Strings.countOccurrences(x, ".") == 1) {
						if (context == null) {
							return "f32";
						}
						if (context.equals("f32") || context.equals("F")) {
							return "f32";
						} else if (context.equals("f64") || context.equals("D")) {
							return "f64";
						} else if (context != null && !context.isEmpty()) {
							if (frac) {
								return null;
							}
							frac = true;
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
			if (context.equals("i8") || context.equals("B")) {
				return "i8";
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

	public static boolean isPrimitive(DefinitiveType name) {
		return name.isPrimitive();
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
			case "i8":
			case "char":
			case "i16":
			case "i32":
			case "i64":
			case "f32":
			case "f64":
			case "V":
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
			case "i8":
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
				throw new CompileError("Unresolved type: " + Types.beautify(primitive));
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
		} else if (type.equals("i8")) {
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
