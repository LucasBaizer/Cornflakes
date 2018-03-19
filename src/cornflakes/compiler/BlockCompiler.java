package cornflakes.compiler;

import org.objectweb.asm.ClassWriter;

public class BlockCompiler extends Compiler {
	@Override
	public void compile(ClassData data, ClassWriter cw, Line body, Line[] lines) {
		String firstLine = Strings.normalizeSpaces(lines[0].getLine());

		if (firstLine.contains("constructor")) {
			new ConstructorCompiler(false).compile(data, cw, body, lines);
			data.setHasConstructor(true);
		} else {
			for (FunctionType type : FunctionType.values()) {
				if (firstLine.contains(type.getKeyword() + " ")) {
					new FunctionCompiler(type, false, false).compile(data, cw, body, lines);
					return;
				}
			}

			throw new CompileError("Expecting statement");
		}
	}
}
