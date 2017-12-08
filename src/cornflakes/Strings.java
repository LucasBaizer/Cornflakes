package cornflakes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Strings {
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
}
