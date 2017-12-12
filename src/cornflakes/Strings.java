package cornflakes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Strings {
	private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvywxyz";
	public static final char[] NUMBERS = "1234567890".toCharArray();
	public static final char[] PERIOD = new char[] { '.' };
	public static final char[] SPACE = new char[] { ' ' };
	public static final char[] SQUARE_BRACKETS = new char[] { '[', ']' };
	public static final char[] VARIABLE_NAME = Arrays.copyOf(NUMBERS, NUMBERS.length);
	public static final char[] VARIABLE_TYPE = combineExceptions(NUMBERS, SQUARE_BRACKETS);

	public static char[] combineExceptions(char[]... exceptions) {
		int len = 0;
		for (char[] x : exceptions) {
			len += x.length;
		}

		int i = 0;
		char[] arr = new char[len];
		for (char[] x : exceptions) {
			System.arraycopy(x, 0, arr, i, x.length);
			i += x.length;
		}

		return arr;
	}

	public static boolean isLetterString(Response<Character> res, String test, boolean handle, char... exceptions) {
		String str = LETTERS;

		for (char ch : exceptions) {
			str += Character.toString(ch);
		}

		for (int i = 0; i < test.length(); i++) {
			if (!str.contains(Character.toString(test.charAt(i)))) {
				if (handle) {
					throw new CompileError("Unexpected token: " + test.charAt(i));
				}
				res.setResponse(test.charAt(i));
				return false;
			}
		}

		return true;
	}

	public static boolean handleLetterString(String test, char... exceptions) {
		return isLetterString(null, test, true, exceptions);
	}

	public static String normalizeSpaces(String str) {
		return str.replaceAll("\\s+", " ").trim();
	}

	public static String transformClassName(String cls) {
		return cls.replace('.', '/');
	}

	public static String[] after(String[] arr, int index) {
		String[] arr2 = new String[arr.length - index];
		System.arraycopy(arr, index, arr2, 0, arr2.length);
		return arr2;
	}

	public static String[] before(String[] arr, int before) {
		String[] arr2 = new String[arr.length - before];
		System.arraycopy(arr, 0, arr2, 0, arr2.length);
		return arr2;
	}

	public static String[] accumulate(String line) {
		List<String> list = new ArrayList<>(Arrays.asList(line.split(System.lineSeparator())));
		return list.toArray(new String[list.size()]);
	}

	public static String accumulate(String[] arr) {
		String x = "";
		for (String y : arr) {
			x += y + System.lineSeparator();
		}
		return x.trim();
	}

	public static String getLine(String[] lines, int cursor) {
		int chars = 0;
		for (String line : lines) {
			chars += line.length();
			if (cursor < chars) {
				return line;
			}
		}
		return null;
	}

	public static int findClosing(char[] text, char open, char close, int openPos) {
		int closePos = openPos;
		int counter = 1;
		while (counter > 0) {
			char c = text[++closePos];
			if (c == open) {
				counter++;
			} else if (c == close) {
				counter--;
			}
		}
		return closePos;
	}

	public static int countOccurrences(String str, String findStr) {
		int lastIndex = 0;
		int count = 0;

		while (lastIndex != -1) {
			lastIndex = str.indexOf(findStr, lastIndex);

			if (lastIndex != -1) {
				count++;
				lastIndex += findStr.length();
			}
		}

		return count;
	}

	public static void handleMatching(String str, char open, char close) {
		String openStr = Character.toString(open);
		String closeStr = Character.toString(close);

		if (countOccurrences(str, openStr) == 0) {
			throw new CompileError("Expecting '" + open + "'");
		}
		if (countOccurrences(str, closeStr) == 0) {
			throw new CompileError("Expecting '" + close + "'");
		}
	}
}
