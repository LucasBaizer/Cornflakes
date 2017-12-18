package cornflakes.compiler;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Opcodes;

public class MainCompiler implements Opcodes {
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

		// compile class head
		Compiler.executePostCompilers();

		for (ClassData data : list) {
			for (ClassData other : list) {
				if (data.getPackageName().equals(other.getPackageName())) {
					data.use(other.getClassName());
				}
			}
		}

		// compile function signatures
		Compiler.executePostCompilers();

		for (ClassData data : list) {
			if (!data.hasConstructor()) {
				new ConstructorCompiler(true).compileDefault(data, data.getClassWriter());
			}

			new StaticInitializerCompiler().compile(data, data.getClassWriter(), null, null);
		}

		// compile functions
		Compiler.executePostCompilers();

		// ensure all neccesary functions are implemented
		for (ClassData data : list) {
			if (data.isInterface() || data.hasModifier(Opcodes.ACC_ABSTRACT)) {
				continue;
			}

			for (String ifn : data.getInterfaces()) {
				if (!ifn.equals("java/io/Serializable")) {
					ClassData ifd = ClassData.forName(ifn);

					for (MethodData method : ifd.getMethods()) {
						boolean match = false;
						for (MethodData impl : data.getMethods()) {
							if (method.toString().equals(impl.toString())) {
								match = true;
							}
						}

						if (!match) {
							throw new CompileError("The interface " + ifd.getSimpleClassName()
									+ " requires the method '" + method.getName() + "' to be implemented");
						}
					}
				}
			}
		}

		// clean up and finish
		Compiler.endPostCompilers();
		System.out.println("Compiled after " + (System.currentTimeMillis() - time) + "ms.");
		System.out.println();

		for (ClassData datum : list) {
			Files.write(Paths.get("bin/" + datum.getClassName() + ".class"), datum.getByteCode());
		}
	}
}
