package cornflakes.compiler;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class GenericBlockCompiler implements GenericCompiler {
	private MethodData data;

	public GenericBlockCompiler(MethodData data) {
		this.data = data;
	}

	@Override
	public int compile(ClassData data, MethodVisitor m, Label startLabel, Label endLabel, int num, String body,
			String[] lines) {
		String firstLine = Strings.normalizeSpaces(lines[0]);

		String condition = firstLine.substring(0, firstLine.lastIndexOf('{')).trim();
		if (condition.isEmpty()) {
			Label start = new Label();

			m.visitLabel(start);
			m.visitLineNumber(num, start);

			String[] newLines = Strings.before(Strings.after(lines, 1), 1);
			String newBlock = Strings.accumulate(newLines).trim();
			
			return new GenericBodyCompiler(this.data).compile(data, m, start, endLabel, num, newBlock, Strings.accumulate(newBlock));
		} else {
			throw new CompileError("Unresolved block condition: " + condition);
		}
	}
}
