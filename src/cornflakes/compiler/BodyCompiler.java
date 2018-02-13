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
			int idx = body.indexOf(System.lineSeparator(), cursor);
			int len = 0;
			int num = -1;

			for (int i = 0; i < lines.length; i++) {
				Line line = lines[i];
				len += line.length();
				
				if (len >= idx) {
					num = line.getNumber();
					break;
				}
			}

			Line line = new Line(num, body.substring(cursor, idx).getLine());
			line = Strings.normalizeSpaces(line);

			if (line.endsWith("{")) {
				int close = Strings.findClosing(body.toCharArray(), '{', '}', cursor + line.length() - 1) + 1;
				Line block = new Line(num, body.substring(cursor, close).getLine());
				Line[] blockLines = Strings.accumulate(block, line.getNumber());
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
