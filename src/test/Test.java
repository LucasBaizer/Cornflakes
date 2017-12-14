package test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Opcodes;

import cornflakes.compiler.ClassData;
import cornflakes.compiler.Compiler;

public class Test implements Opcodes {
	public static class DynamicClassLoader extends ClassLoader {
		public Class<?> define(String className, byte[] bytecode) {
			return super.defineClass(className, bytecode, 0, bytecode.length);
		}
	};

	public static void main(String[] args) throws Exception {
		long time = System.currentTimeMillis();

		List<ClassData> list = new ArrayList<>();
		for (File file : new File("src").listFiles()) {
			if (file.isFile()) {
				if (file.getName().endsWith(".cf")) {
					list.add(Compiler.compile(file.getName(), new String(Files.readAllBytes(file.toPath()))
							.replaceAll("\\r\\n|\\r|\\n", System.lineSeparator())));
				}
			}
		}

		// compile bodies
		Compiler.executePostCompilers();

		// compile functions
		Compiler.executePostCompilers();

		// clean up and finish
		Compiler.endPostCompilers();
		System.out.println("Compiled after " + (System.currentTimeMillis() - time) + "ms.");
		System.out.println();

		for (ClassData datum : list) {
			Files.write(Paths.get("bin/" + datum.getClassName() + ".class"), datum.getByteCode());
		}

		/*
		 * DynamicClassLoader loader = new DynamicClassLoader(); Class<?>
		 * helloWorldClass = loader.define(result.getClassName().trim(),
		 * result.getByteCode()); Method method =
		 * helloWorldClass.getDeclaredMethod("main");
		 * method.setAccessible(true);
		 * 
		 * Object invok = method.invoke(null); System.out.println();
		 * System.out.println("Exit code: " + invok);
		 * 
		 * System.exit((int) invok);
		 */
	}
}
