package cornflakes;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class Compiler implements Opcodes {
	public static CompileResult compile(String cls) {
		List<String> list = Arrays.asList(cls.split(System.lineSeparator())).stream().filter((x) -> !x.trim().isEmpty())
				.collect(Collectors.toList());
		String[] split = list.toArray(new String[list.size()]);

		String className = "";
		String firstLine = split[0];

		if (firstLine.startsWith("package")) {
			if (!firstLine.startsWith("package ")) {
				throw new CompileError("Expecting space ' ' between identifiers");
			}
			className = firstLine.substring(firstLine.indexOf(" ") + 1).replace('.', '/') + "/";
			firstLine = split[1];
		}

		if (!firstLine.startsWith("class")) {
			throw new CompileError("Expecting class definition");
		} else {
			if (!firstLine.startsWith("class ")) {
				throw new CompileError("Expecting space ' ' between identifiers");
			}
			className += firstLine.substring(firstLine.indexOf(" ") + 1);
		}
		
		System.out.println(className);

		ClassWriter cw = new ClassWriter(0);
		cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object", null);
		cw.visitSource("HelloWorld.java", null);

		return new CompileResult(className, cw.toByteArray());
	}
}
