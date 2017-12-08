package cornflakes;

import org.objectweb.asm.ClassWriter;

public class StatementCompiler extends Compiler {
	@Override
	public void compile(ClassData data, ClassWriter cw, String body, String[] lines) {
		if (!body.contains(" ")) {
			throw new CompileError("Expecting parameter after statement");
		}

		String[] split = body.split(" ", 2);
		String cmd = split[0];

		if (cmd.equals("use")) {
			String val = split[1].trim().replace("{", "").replace("}", "");

			String prefix = val.substring(0, val.lastIndexOf('.') + 1);
			String[] spl = val.substring(val.lastIndexOf('.') + 1).split(",");
			for (String s : spl) {
				data.use(prefix + s.trim());
			}
		} else {
			throw new CompileError("Unexpected statement: " + cmd);
		}
	}
}
