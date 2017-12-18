package cornflakes.compiler;

import org.objectweb.asm.MethodVisitor;

public class GenericBodyCompiler implements GenericCompiler {
	private MethodData data;
	private boolean returns;

	public GenericBodyCompiler(MethodData data) {
		this.data = data;
	}

	@Override
	public void compile(ClassData data, MethodVisitor m, Block block, String body, String[] lines) {
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

			if (line.endsWith("{")) {
				int close = Strings.findClosing(body.toCharArray(), '{', '}', cursor + line.length() - 1) + 1;
				String newBlock = body.substring(cursor, close).trim();
				String[] blockLines = Strings.accumulate(newBlock);
				new GenericBlockCompiler(this.data).compile(data, m, block, newBlock, blockLines);

				cursor = close;
				while (cursor < body.length() && Character.isWhitespace(body.charAt(cursor))) {
					cursor++;
				}
			} else {
				GenericStatementCompiler gsc = new GenericStatementCompiler(this.data);
				gsc.compile(data, m, block, line, new String[] { line });

				if (gsc.getType() == GenericStatementCompiler.RETURN) {
					returns = true;
				}

				cursor += line.length();
				while (cursor < body.length() && Character.isWhitespace(body.charAt(cursor))) {
					cursor++;
				}
			}
		}
	}

	public boolean returns() {
		return returns;
	}
}
