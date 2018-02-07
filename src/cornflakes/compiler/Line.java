package cornflakes.compiler;

public class Line {
	private int number;
	private String line;

	public Line(int number, String line) {
		this.number = number;
		this.line = line;
	}

	public static String[] toString(Line[] lines) {
		String[] str = new String[lines.length];

		for (int i = 0; i < lines.length; i++) {
			str[i] = lines[i].line;
		}
		return str;
	}

	public Line[] toLineFragment(String[] lines) {
		Line[] str = new Line[lines.length];

		for (int i = 0; i < lines.length; i++) {
			str[i] = derive(lines[i]);
		}
		return str;
	}

	public String getLine() {
		return line;
	}

	public int length() {
		return line.length();
	}

	public char charAt(int x) {
		return line.charAt(x);
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public Line[] split(String x) {
		return toLineFragment(line.split(x));
	}

	public Line[] split(String x, int val) {
		return toLineFragment(line.split(x, val));
	}

	public Line substring(int start) {
		return derive(line.substring(start));
	}

	public Line substring(int start, int end) {
		return derive(line.substring(start, end));
	}

	public Line trim() {
		return derive(line.trim());
	}

	public int indexOf(String str) {
		return line.indexOf(str);
	}

	public int indexOf(String str, int x) {
		return line.indexOf(str, x);
	}

	public boolean isEmpty() {
		return line.isEmpty();
	}

	public boolean startsWith(String txt) {
		return line.startsWith(txt);
	}

	public boolean endsWith(String txt) {
		return line.endsWith(txt);
	}

	public boolean contains(String str) {
		return line.contains(str);
	}

	public int lastIndexOf(char str) {
		return line.lastIndexOf(str);
	}

	public char[] toCharArray() {
		return line.toCharArray();
	}

	public Line derive(String line) {
		return new Line(this.number, line);
	}

	public void append(Line sub) {
		this.line += sub.getLine();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof String) {
			return line.equals((String) obj);
		} else if (obj instanceof Line) {
			Line o = (Line) obj;
			return number == o.number && line.equals(o.line);
		}

		return false;
	}

	@Override
	public String toString() {
		return line;
	}

	public void rethrow(CompileError e) {
		if (e.getCause() == null) {
			throw new CompileError(e.getMessage() + " on line " + number);
		} else {
			throw new CompileError("on line " + number, e.getCause());
		}
	}
}
