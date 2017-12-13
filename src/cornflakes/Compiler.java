package cornflakes;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public abstract class Compiler implements Opcodes {
	private static List<FunctionCompiler> postCompilers = new ArrayList<>();
	
	public static Dimension dim = new Dimension(5, 5);
	
	public abstract void compile(ClassData data, ClassWriter cw, String body, String[] lines);

	public static ClassData compile(String file, String cls) {
		List<String> list = Arrays.asList(cls.split(System.lineSeparator())).stream().map((x) -> {
			String trim = x.trim();
			if (trim.endsWith(";")) {
				trim = trim.substring(0, trim.length() - 1).trim();
			}
			return trim;
		}).filter((x) -> !x.isEmpty() && !x.startsWith("//")).collect(Collectors.toList());
		String[] lines = list.toArray(new String[list.size()]);

		ClassWriter cw = new ClassWriter(0);
		ClassData data = new ClassData();
		data.setSourceName(file);

		new HeadCompiler().compile(data, cw, Strings.accumulate(lines), lines);
		for(FunctionCompiler compiler : postCompilers) {
			compiler.write();
		}

		if (!data.hasConstructor()) {
			new ConstructorCompiler().compileDefault(data, cw);
		}

		cw.visitEnd();
		data.setByteCode(cw.toByteArray());

		return data;
	}
	
	public static void addPostCompiler(FunctionCompiler compiler) {
		postCompilers.add(compiler);
	}
}
