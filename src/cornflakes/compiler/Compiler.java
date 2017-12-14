package cornflakes.compiler;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public abstract class Compiler implements Opcodes {
	private static Map<String, AfterCompile> postCompilers = new HashMap<>();

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

		return data;
	}

	public static void addPostCompiler(String name, PostCompiler compiler) {
		postCompilers.get(name).getCompilers().add(compiler);
	}

	public static void executePostCompilers() {
		for (AfterCompile after : postCompilers.values()) {
			after.finish();
		}
	}
	
	public static void endPostCompilers() {
		for (AfterCompile after : postCompilers.values()) {
			after.end();
		}
	}

	public static void register(ClassWriter cw, ClassData data) {
		postCompilers.put(data.getClassName(), new AfterCompile(cw, data));
	}
}
