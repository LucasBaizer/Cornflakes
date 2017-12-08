package cornflakes;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class FunctionCompiler extends Compiler {
	@Override
	public void compile(ClassData data, ClassWriter cw, String body, String[] lines) {
		String after = lines[0].substring("function".length()).trim();
		String withoutBracket = after.substring(0, after.length() - 1).trim();
		Strings.handleMatching(withoutBracket, '(', ')');

		int accessor = ACC_PUBLIC + ACC_STATIC;
		String params = withoutBracket.substring(withoutBracket.indexOf('(') + 1, withoutBracket.indexOf(')')).trim();
		List<String> parameterNames = new ArrayList<>();
		if (!params.isEmpty()) {
			String[] split = params.split(",");
			for (String par : split) {
				par = Strings.normalizeSpaces(par);
				Strings.handleLetterString(par, Strings.combineExceptions(Strings.NUMBERS, Strings.SPACE));

				if (par.equals("this")) {
					if (parameterNames.contains(par)) {
						throw new CompileError("Duplicate parameter name: " + par);
					}

					accessor -= ACC_STATIC;
					parameterNames.add(par);
					continue;
				}

				String[] spl = par.split(" ");
				if (spl.length != 2) {
					throw new CompileError("Expecting format 'type name'");
				}

				String type = spl[0];
				String name = spl[1];

				if (parameterNames.contains(name)) {
					throw new CompileError("Duplicate parameter name: " + par);
				}
				
				data.resolve(type);

				parameterNames.add(par);
			}
		}

		MethodVisitor mv = cw.visitMethod(accessor, "main", "()V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitLineNumber(7, l0);
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitLdcInsn("Hello, world!");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLineNumber(8, l1);
		mv.visitInsn(RETURN);
		Label l2 = new Label();
		mv.visitLabel(l2);
		// mv.visitLocalVariable("args", "[Ljava/lang/String;", null, l0,
		// l2, 0);
		mv.visitMaxs(2, 1);
		mv.visitEnd();
	}
}
