package cornflakes.lang;

import java.util.Scanner;

public class Console {
	private static final Scanner in = new Scanner(System.in);

	private static String replace(Object msg, Object... values) {
		if (msg == null) {
			return null;
		}
		if (!(msg instanceof String)) {
			return msg.toString();
		}

		String str = (String) msg;
		for (int i = 0; i < values.length; i++) {
			str = str.replace("{" + i + "}", values[i] == null ? "null" : values[i].toString());
		}

		return str;
	}

	public static void println(Object msg, Object... values) {
		String str = replace(msg, values);
		System.out.println(str);
	}

	public static void print(Object msg, Object... values) {
		String str = replace(msg, values);
		System.out.print(str);
	}

	public static Scanner getInput() {
		return in;
	}

	public static String readLine() {
		return in.nextLine();
	}
}
