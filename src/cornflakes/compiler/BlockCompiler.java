package cornflakes.compiler;

import org.objectweb.asm.ClassWriter;

public class BlockCompiler extends Compiler {
	@Override
	public void compile(ClassData data, ClassWriter cw, String body, String[] lines) {
		String firstLine = Strings.normalizeSpaces(lines[0]);

		if (firstLine.contains("func ")) {
			new FunctionCompiler(false, false).compile(data, cw, body, lines);
		} else if (firstLine.contains("constructor")) {
			new ConstructorCompiler(false).compile(data, cw, body, lines);
			data.setHasConstructor(true);
		} else {
			throw new CompileError("Expecting statement");
		}
	}
}
