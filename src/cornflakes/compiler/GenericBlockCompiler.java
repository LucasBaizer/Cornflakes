package cornflakes.compiler;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class GenericBlockCompiler implements GenericCompiler {
	private MethodData data;

	public GenericBlockCompiler(MethodData data) {
		this.data = data;
	}

	@Override
	public void compile(ClassData data, MethodVisitor m, Label startLabel, Label endLabel, String body,
			String[] lines) {
		String firstLine = Strings.normalizeSpaces(lines[0]);

		String condition = firstLine.substring(0, firstLine.lastIndexOf('{')).trim();
		if (condition.isEmpty()) {
			Label start = new Label();

			this.data.addBlock();

			m.visitLabel(start);
			m.visitLineNumber(this.data.getBlocks(), start);

			String[] newLines = Strings.before(Strings.after(lines, 1), 1);
			String newBlock = Strings.accumulate(newLines).trim();

			new GenericBodyCompiler(this.data).compile(data, m, start, endLabel, newBlock,
					Strings.accumulate(newBlock));
		} else {
			throw new CompileError("Unresolved block condition: " + condition);
		}
	}
}
