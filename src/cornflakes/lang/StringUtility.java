package cornflakes.lang;

public final class StringUtility {
	private StringUtility() {
	}

	public static String combine(String x, String y) {
		return x + y;
	}

	public static String combine(String x, byte y) {
		return x + y;
	}

	public static String combine(String x, short y) {
		return x + y;
	}

	public static String combine(String x, int y) {
		return x + y;
	}

	public static String combine(String x, long y) {
		return x + y;
	}

	public static String combine(String x, float y) {
		return x + y;
	}

	public static String combine(String x, double y) {
		return x + y;
	}

	public static String combine(String x, boolean y) {
		return x + y;
	}

	public static String combine(String x, char y) {
		return x + y;
	}

	public static String combine(byte x, String y) {
		return x + y;
	}

	public static String combine(short x, String y) {
		return x + y;
	}

	public static String combine(int x, String y) {
		return x + y;
	}

	public static String combine(long x, String y) {
		return x + y;
	}

	public static String combine(float x, String y) {
		return x + y;
	}

	public static String combine(double x, String y) {
		return x + y;
	}

	public static String combine(boolean x, String y) {
		return x + y;
	}

	public static String combine(char x, String y) {
		return x + y;
	}

	public static String replaceChar(String x, int index, char c) {
		StringBuilder builder = new StringBuilder(x);
		builder.setCharAt(index, c);
		return builder.toString();
	}
}
