package cornflakes;

import org.objectweb.asm.ClassWriter;

public class BodyCompiler extends Compiler {
	@Override
	public void compile(ClassData data, ClassWriter cw, String body, String[] lines) {
		int cursor = 0;
		while (cursor < body.length()) {
			String line = body.substring(cursor, body.indexOf(System.lineSeparator(), cursor));
			if (line == null) {
				break;
			}
			line = Strings.normalizeSpaces(line);

			if (line.endsWith("{")) {
				int close = Strings.findClosing(body.toCharArray(), '{', '}', cursor + line.length() - 1) + 1;
				String block = body.substring(cursor, close);
				String[] blockLines = Strings.accumulate(block);
				new BlockCompiler().compile(data, cw, block, blockLines);

				cursor = close;
				while (cursor < body.length() && Character.isWhitespace(body.charAt(cursor))) {
					cursor++;
				}
			} else {
				new StatementCompiler().compile(data, cw, line, new String[] { line });
				cursor += line.length();
				while (cursor < body.length() && Character.isWhitespace(body.charAt(cursor))) {
					cursor++;
				}
			}
		}
	}
}
