package cornflakes;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public abstract class Compiler implements Opcodes {
	public abstract void compile(ClassData data, ClassWriter cw, String body, String[] lines);

	public static ClassData compile(String file, String cls) {
		List<String> list = Arrays.asList(cls.split(System.lineSeparator())).stream().map((x) -> x.trim())
				.filter((x) -> !x.trim().isEmpty() && !x.trim().startsWith("//")).collect(Collectors.toList());
		String[] lines = list.toArray(new String[list.size()]);

		ClassWriter cw = new ClassWriter(0);
		ClassData data = new ClassData();
		data.setSourceName(file);

		new HeadCompiler().compile(data, cw, Strings.accumulate(lines), lines);
		
		if(!data.hasConstructor()) {
			new ConstructorCompiler().compileDefault(data, cw);
		}
		
		cw.visitEnd();
		data.setByteCode(cw.toByteArray());

		return data;
	}
}
