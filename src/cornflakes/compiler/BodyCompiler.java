package cornflakes.compiler;

import org.objectweb.asm.ClassWriter;

public class BodyCompiler extends Compiler implements PostCompiler {
	private ClassData data;
	private ClassWriter cw;
	private Line body;
	private Line[] after;

	public BodyCompiler(ClassData data, ClassWriter cw, Line body, Line[] after) {
		this.data = data;
		this.cw = cw;
		this.body = body;
		this.after = after;
	}

	@Override
	public void compile(ClassData data, ClassWriter cw, Line body, Line[] lines) {
		ClassData.setCurrentClass(data);

		int cursor = 0;
		while (cursor < body.length()) {
			Line line = body.substring(cursor, body.indexOf(System.lineSeparator(), cursor));

			if (line == null) {
				break;
			}
			line = Strings.normalizeSpaces(line);

			if (line.endsWith("{")) {
				int close = Strings.findClosing(body.toCharArray(), '{', '}', cursor + line.length() - 1) + 1;
				Line block = body.substring(cursor, close);
				Line[] blockLines = Strings.accumulate(block,
						line.getNumber() + Strings.countOccurrences(
								body.substring(0, body.indexOf(System.lineSeparator(), cursor)).getLine(),
								System.lineSeparator()));
				System.out.println(line + " " + blockLines[0].getNumber());
				new BlockCompiler().compile(data, cw, block, blockLines);

				cursor = close;
				while (cursor < body.length() && Character.isWhitespace(body.charAt(cursor))) {
					cursor++;
				}
			} else {
				new StatementCompiler().compile(data, cw, line, new Line[] { line });
				cursor += line.length();
				while (cursor < body.length() && Character.isWhitespace(body.charAt(cursor))) {
					cursor++;
				}
			}
		}
	}

	@Override
	public void write() {
		compile(data, cw, body, after);
	}
}
