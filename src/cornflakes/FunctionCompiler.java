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
		String methodName = withoutBracket.substring(0, withoutBracket.indexOf('(')).trim();
		Strings.handleLetterString(methodName);

		String returnType = "V";
		if (withoutBracket.contains("->")) {
			String afterParams = withoutBracket.substring(withoutBracket.indexOf("->") + 2).trim();
			Strings.handleLetterString(afterParams, Strings.NUMBERS);
			
			returnType = data.resolveClass(afterParams);
		}

		String params = withoutBracket.substring(withoutBracket.indexOf('(') + 1, withoutBracket.indexOf(')')).trim();
		List<String> parameterNames = new ArrayList<>();
		if (!params.isEmpty()) {
			String[] split = params.split(",");
			for (String par : split) {
				par = Strings.normalizeSpaces(par);

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

				Strings.handleLetterString(name, Strings.NUMBERS);
				Strings.handleLetterString(type, Strings.combineExceptions(Strings.NUMBERS, Strings.SQUARE_BRACKETS));

				if (parameterNames.contains(name)) {
					throw new CompileError("Duplicate parameter name: " + par);
				}

				String resolvedType = data.resolveClass(type);
				if (methodName.equals("main")) {
					if (!resolvedType.equals("[Ljava/lang/String;")) {
						throw new CompileError("Main method should either have no parameter or one of type string[]");
					}
					if(!returnType.equals("I")) {
						throw new CompileError("Main method should have return type 'int'");
					}
				}

				parameterNames.add(par);
			}
		}

		MethodVisitor mv = cw.visitMethod(accessor, methodName, "()V", null, null);
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
