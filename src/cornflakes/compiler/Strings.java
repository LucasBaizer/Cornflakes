package cornflakes.compiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Strings {
	private static final String[] KEYWORDS = { "package", "use", "public", "private", "protected", "func", "final",
			"const", "sealed", "abstract", "if", "do", "while", "else", "for", "var", "sync", "serial", "throw",
			"foreach", "in", "is", "extends", "implements", "array", "as", "try", "catch", "finally" };
	private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvywxyz";
	public static final char[] NUMBERS = "1234567890".toCharArray();
	public static final char[] PERIOD = new char[] { '.' };
	public static final char[] SPACE = new char[] { ' ' };
	public static final char[] SQUARE_BRACKETS = new char[] { '[', ']' };
	public static final char[] VARIABLE_NAME = combineExceptions(NUMBERS, new char[] { '_' });
	public static final char[] SLASH = new char[] { '/' };
	public static final char[] TYPE = combineExceptions(NUMBERS, PERIOD, SPACE,
			new char[] { '(', ')', ',', '/', '[', ']', '*' });

	public static boolean contains(Line x, String value) {
		return contains(x.getLine(), value);
	}

	public static boolean contains(String x, String value) {
		return Pattern.compile(Pattern.quote(value) + "(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)").matcher(x).find();
	}

	public static String[] split(String x, String value) {
		return x.split(Pattern.quote(value) + "(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
	}

	public static Line[] split(Line x, String value) {
		return x.split(Pattern.quote(value) + "(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
	}

	public static Line[] split(Line x, String value, int max) {
		return x.split(Pattern.quote(value) + "(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", max);
	}

	public static String[] split(String x, String value, int max) {
		return x.split(Pattern.quote(value) + "(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", max);
	}

	public static String[] trim(String[] x) {
		String[] y = new String[x.length];
		for (int i = 0; i < x.length; i++) {
			y[i] = x[i].trim();
		}
		return y;
	}

	public static String[] splitParameters(String x) {
		if (x.isEmpty()) {
			return new String[0];
		}

		int par = 0;
		List<String> list = new ArrayList<>();
		int start = 0;
		boolean quote = false;
		for (int i = 0; i < x.length(); i++) {
			char c = x.charAt(i);
			if (c == '(') {
				par++;
			} else if (c == ')') {
				par--;
			} else if (c == '"') {
				quote = !quote;
			} else if (par == 0 && !quote) {
				if (c == ',') {
					list.add(x.substring(start, i).trim());
					start = i + 2;
				}
			}
		}

		list.add(x.substring(start, x.length()));
		return list.toArray(new String[list.size()]);
	}

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
		if (Arrays.asList(KEYWORDS).contains(test)) {
			if (handle) {
				throw new CompileError("Unexpected keyword: " + test);
			}
			return false;
		}

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

	public static Line normalizeSpaces(Line line) {
		return line.derive(normalizeSpaces(line.getLine()));
	}

	public static String normalizeSpaces(String str) {
		return str.replaceAll("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", " ").trim();
	}

	public static String transformClassName(String cls) {
		return cls.replace('.', '/');
	}

	public static String[] after(String[] arr, int index) {
		String[] arr2 = new String[arr.length - index];
		System.arraycopy(arr, index, arr2, 0, arr2.length);
		return arr2;
	}

	public static Line[] after(Line[] arr, int index) {
		Line[] arr2 = new Line[arr.length - index];
		System.arraycopy(arr, index, arr2, 0, arr2.length);
		return arr2;
	}

	public static Line[] before(Line[] arr, int before) {
		Line[] arr2 = new Line[arr.length - before];
		System.arraycopy(arr, 0, arr2, 0, arr2.length);
		return arr2;
	}

	public static String[] before(String[] arr, int before) {
		String[] arr2 = new String[arr.length - before];
		System.arraycopy(arr, 0, arr2, 0, arr2.length);
		return arr2;
	}

	public static Line accumulate(Line[] arr) {
		if (arr.length == 0) {
			return new Line(-1, "");
		}
		String x = "";
		for (Line y : arr) {
			x += y.getLine() + System.lineSeparator();
		}
		return new Line(arr[0].getNumber(), x.trim());
	}

	public static Line[] accumulate(Line line) {
		return accumulate(line, line.getNumber());
	}

	public static Line[] accumulate(Line line, int num) {
		List<Line> list = new ArrayList<>(Arrays.asList(line.split(System.lineSeparator())));
		for (int i = 0; i < list.size(); i++) {
			list.get(i).setNumber(num + i);
		}
		return list.toArray(new Line[list.size()]);
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

	public static boolean hasMatching(String str, char open, char close) {
		String openStr = Character.toString(open);
		String closeStr = Character.toString(close);

		if (countOccurrences(str, openStr) == 0) {
			return false;
		}
		if (countOccurrences(str, closeStr) == 0) {
			return false;
		}

		return true;
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

	public static String capitalize(String str) {
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}
}
