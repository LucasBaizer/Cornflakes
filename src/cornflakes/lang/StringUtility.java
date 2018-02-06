package cornflakes.lang;

/**
 * Internally used by the compiler to append strings to other strings and other
 * types.
 * 
 * @author Lucas Baizer
 */
public final class StringUtility {
	private StringUtility() {
	}

	/**
	 * Combines a string and another string
	 * 
	 * @param x
	 *            The first string
	 * @param y
	 *            The second string
	 * @return The resulting string
	 */
	public static String combine(String x, String y) {
		return x + y;
	}

	/**
	 * Combines a string and a byte
	 * 
	 * @param x
	 *            The string
	 * @param y
	 *            The byte
	 * @return The resulting string
	 */
	public static String combine(String x, byte y) {
		return x + y;
	}

	/**
	 * Combines a string and a short
	 * 
	 * @param x
	 *            The string
	 * @param y
	 *            The short
	 * @return The resulting string
	 */
	public static String combine(String x, short y) {
		return x + y;
	}

	/**
	 * Combines a string and an int
	 * 
	 * @param x
	 *            The string
	 * @param y
	 *            The int
	 * @return The resulting string
	 */
	public static String combine(String x, int y) {
		return x + y;
	}

	/**
	 * Combines a string and a long
	 * 
	 * @param x
	 *            The string
	 * @param y
	 *            The long
	 * @return The resulting string
	 */
	public static String combine(String x, long y) {
		return x + y;
	}

	/**
	 * Combines a string and a float
	 * 
	 * @param x
	 *            The string
	 * @param y
	 *            The float
	 * @return The resulting string
	 */
	public static String combine(String x, float y) {
		return x + y;
	}

	/**
	 * Combines a string and a double
	 * 
	 * @param x
	 *            The string
	 * @param y
	 *            The double
	 * @return The resulting string
	 */
	public static String combine(String x, double y) {
		return x + y;
	}

	/**
	 * Combines a string and a boolean
	 * 
	 * @param x
	 *            The string
	 * @param y
	 *            The boolean
	 * @return The resulting string
	 */
	public static String combine(String x, boolean y) {
		return x + y;
	}

	/**
	 * Combines a string and a char
	 * 
	 * @param x
	 *            The string
	 * @param y
	 *            The char
	 * @return The resulting string
	 */
	public static String combine(String x, char y) {
		return x + y;
	}

	/**
	 * Combines a byte and a string
	 * 
	 * @param x
	 *            The byte
	 * @param y
	 *            The string
	 * @return The resulting string
	 */
	public static String combine(byte x, String y) {
		return x + y;
	}

	/**
	 * Combines a short and a string
	 * 
	 * @param x
	 *            The short
	 * @param y
	 *            The string
	 * @return The resulting string
	 */
	public static String combine(short x, String y) {
		return x + y;
	}

	/**
	 * Combines an int and a string
	 * 
	 * @param x
	 *            The int
	 * @param y
	 *            The string
	 * @return The resulting string
	 */
	public static String combine(int x, String y) {
		return x + y;
	}

	/**
	 * Combines a long and a string
	 * 
	 * @param x
	 *            The long
	 * @param y
	 *            The string
	 * @return The resulting string
	 */
	public static String combine(long x, String y) {
		return x + y;
	}

	/**
	 * Combines a float and a string
	 * 
	 * @param x
	 *            The float
	 * @param y
	 *            The string
	 * @return The resulting string
	 */
	public static String combine(float x, String y) {
		return x + y;
	}

	/**
	 * Combines a double and a string
	 * 
	 * @param x
	 *            The double
	 * @param y
	 *            The string
	 * @return The resulting string
	 */
	public static String combine(double x, String y) {
		return x + y;
	}

	/**
	 * Combines a boolean and a string
	 * 
	 * @param x
	 *            The boolean
	 * @param y
	 *            The string
	 * @return The resulting string
	 */
	public static String combine(boolean x, String y) {
		return x + y;
	}

	/**
	 * Combines a char and a string
	 * 
	 * @param x
	 *            The char
	 * @param y
	 *            The string
	 * @return The resulting string
	 */
	public static String combine(char x, String y) {
		return x + y;
	}

	/**
	 * Replaces a single character in a string, and returns a new string with
	 * the replaced character.
	 * 
	 * @param x
	 *            The string that will have a character be replaced
	 * @param index
	 *            The index of the character to be replaced
	 * @param c
	 *            The character which will be the replacement
	 * @return The new string
	 */
	public static String replaceChar(String x, int index, char c) {
		StringBuilder builder = new StringBuilder(x);
		builder.setCharAt(index, c);
		return builder.toString();
	}
}
