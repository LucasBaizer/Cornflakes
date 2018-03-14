package cornflakes.compiler;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.MethodVisitor;

public class GenericBodyCompiler implements GenericCompiler {
	private MethodData data;
	private boolean returns;

	public GenericBodyCompiler(MethodData data) {
		this.data = data;
	}

	@Override
	public void compile(ClassData data, MethodVisitor m, Block block, Line[] lines) {
		Line body = Strings.accumulate(lines);

		int cursor = 0;
		while (cursor < body.length()) {
			int idx = body.indexOf(System.lineSeparator(), cursor);
			if (idx == -1) {
				idx = body.length();
			}
			Line line = body.substring(cursor, idx);
			line = Strings.normalizeSpaces(line);

			if (line.endsWith("{")) {
				int close = Strings.findClosing(body.toCharArray(), '{', '}', cursor + line.length() - 1) + 1;
				Line newBlock = body.substring(cursor, close).trim();
				Line next = body.substring(close);
				Line[] blockLines = Strings.accumulate(newBlock);
				List<Line[]> list = new ArrayList<>();
				list.add(blockLines);

				while (next.trim().startsWith("else") || next.trim().startsWith("catch")
						|| next.trim().startsWith("finally")) {
					int spaces = next.trim().length() - next.length();
					Line[] acc = Strings.accumulate(next);
					int old = close;
					close = Strings.findClosing(body.toCharArray(), '{', '}', close + acc[0].length() + spaces) + 1;
					Line sub = body.substring(old, close);
					list.add(Strings.accumulate(sub));
					newBlock.append(sub);
					next = body.substring(close);
				}

				new GenericBlockCompiler(this.data, list).compile(data, m, block, blockLines);

				cursor = close;
				while (cursor < body.length() && Character.isWhitespace(body.charAt(cursor))) {
					cursor++;
				}
			} else {
				GenericStatementCompiler gsc = new GenericStatementCompiler(this.data);
				gsc.compile(data, m, block, new Line[] { line });

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
