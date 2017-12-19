package cornflakes.compiler;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class GenericBlockCompiler implements GenericCompiler {
	private MethodData data;

	public GenericBlockCompiler(MethodData data) {
		this.data = data;
	}

	@Override
	public void compile(ClassData data, MethodVisitor m, Block block, String body, String[] lines) {
		String firstLine = Strings.normalizeSpaces(lines[0]);

		Label start = new Label();

		this.data.addBlock();

		m.visitLabel(start);
		m.visitLineNumber(this.data.getBlocks(), start);

		String condition = firstLine.substring(0, firstLine.lastIndexOf('{')).trim();
		String[] newLines = Strings.before(Strings.after(lines, 1), 1);
		String newBlock = Strings.accumulate(newLines).trim();

		Block thisBlock = new Block(block.getStart() + 1, start, null);
		block.addBlock(thisBlock);
		if (condition.isEmpty()) {
			thisBlock.setEndLabel(block.getEndLabel());
			new GenericBodyCompiler(this.data).compile(data, m, thisBlock, newBlock, Strings.accumulate(newBlock));
		} else {
			if (condition.startsWith("if")) {
				Label end = new Label();
				thisBlock.setEndLabel(end);

				String parse = condition.substring(2).trim();
				new BooleanExpressionCompiler(this.data, end, true).compile(data, m, thisBlock, parse,
						new String[] { parse });
				new GenericBodyCompiler(this.data).compile(data, m, block, newBlock, Strings.accumulate(newBlock));

				m.visitLabel(end);
			} else if (body.startsWith("else")) {
				String after = body.substring(4).trim();
				new GenericBlockCompiler(this.data).compile(data, m, block, after, Strings.accumulate(after));
			} else {
				throw new CompileError("Unresolved block condition: " + condition);
			}
		}
	}
}
