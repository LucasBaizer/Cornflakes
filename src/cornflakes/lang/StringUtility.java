package cornflakes.lang;

public final class StringUtility {
	private StringUtility() {
	}

	public static String combine(String x, String y) {
		return x + y;
	}

	public static String replaceChar(String x, int index, char c) {
		StringBuilder builder = new StringBuilder(x);
		builder.setCharAt(index, c);
		return builder.toString();
	}
}
