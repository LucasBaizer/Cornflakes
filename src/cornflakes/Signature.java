package cornflakes;

import java.lang.reflect.Method;

public class Signature {
	public static String from(Method method) {
		String sig = "(";
	}

	public static String from(Class<?> type) {
		if (type == null || type == Void.class) {
			return "V";
		} else if (type == boolean.class) {
			return "Z";
		} else if (type == byte.class) {
			return "B";
		} else if (type == char.class) {
			return "C";
		} else if (type == double.class) {
			return "D";
		} else if (type == float.class) {
			return "F";
		} else if (type == int.class) {
			return "I";
		} else if (type == long.class) {
			return "J";
		} else if (type == short.class) {
			return "S";
		} else if (!type.isArray()) {
			return "L" + Strings.transformClassName(type.getName()) + ";";
		} else {
			return "[" + from(type); // TODO ahhh
		}
	}
}
