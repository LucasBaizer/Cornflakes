package cornflakes;

import org.objectweb.asm.MethodVisitor;

public class GenericBodyCompiler implements GenericCompiler {
	private MethodData data;

	public GenericBodyCompiler(MethodData data) {
		this.data = data;
	}

	@Override
	public int compile(ClassData data, MethodVisitor m, int num, String body, String[] lines) {
		int cursor = 0;
		while (cursor < body.length()) {
			int idx = body.indexOf(System.lineSeparator(), cursor);
			if (idx == -1) {
				idx = body.length();
			}
			String line = body.substring(cursor, idx);
			if (line == null) {
				break;
			}
			line = Strings.normalizeSpaces(line);

			// if (line.endsWith("{")) {
			// int close = Strings.findClosing(body.toCharArray(), '{', '}',
			// cursor + line.length() - 1) + 1;
			// String block = body.substring(cursor, close);
			// String[] blockLines = Strings.accumulate(block);
			// new BlockCompiler().compile(data, cw, block, blockLines);
			//
			// cursor = close;
			// while (cursor < body.length() &&
			// Character.isWhitespace(body.charAt(cursor))) {
			// cursor++;
			// }
			// } else {
			num = new GenericStatementCompiler(this.data).compile(data, m, num, line, new String[] { line });
			cursor += line.length();
			while (cursor < body.length() && Character.isWhitespace(body.charAt(cursor))) {
				cursor++;
			}
			// }
		}

		return num;
	}
}
