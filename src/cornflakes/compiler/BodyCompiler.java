package cornflakes.compiler;

import org.objectweb.asm.ClassWriter;

public class BodyCompiler extends Compiler implements PostCompiler {
	private ClassData data;
	private ClassWriter cw;
	private String accumulate;
	private String[] after;

	public BodyCompiler(ClassData data, ClassWriter cw, String accumulate, String[] after) {
		this.data = data;
		this.cw = cw;
		this.accumulate = accumulate;
		this.after = after;
	}

	@Override
	public void compile(ClassData data, ClassWriter cw, String body, String[] lines) {
		ClassData.setCurrentClass(data);

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

	@Override
	public void write() {
		compile(data, cw, accumulate, after);
	}
}
