package cornflakes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

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
			Strings.handleLetterString(afterParams, Strings.VARIABLE_TYPE);

			returnType = data.resolveClass(afterParams);
			if (returnType.length() > 1) {
				returnType = "L" + returnType + ";";
			}
		}

		String params = withoutBracket.substring(withoutBracket.indexOf('(') + 1, withoutBracket.indexOf(')')).trim();
		Map<String, String> parameters = new LinkedHashMap<>();
		if (!params.isEmpty()) {
			String[] split = params.split(",");
			for (String par : split) {
				par = Strings.normalizeSpaces(par);

				if (par.equals("this")) {
					if (parameters.containsKey(par)) {
						throw new CompileError("Duplicate parameter name: " + par);
					}

					accessor -= ACC_STATIC;
					parameters.put("this", "L" + data.getClassName() + ";");
					continue;
				}

				String[] spl = par.split(" ");
				if (spl.length != 2) {
					throw new CompileError("Expecting format 'type name'");
				}

				String type = spl[0];
				String name = spl[1];

				Strings.handleLetterString(name, Strings.VARIABLE_NAME);
				Strings.handleLetterString(type, Strings.VARIABLE_TYPE);

				if (parameters.containsKey(name)) {
					throw new CompileError("Duplicate parameter name: " + par);
				}

				String resolvedType = data.resolveClass(type);
				if (methodName.equals("main")) {
					if (!resolvedType.equals("[Ljava/lang/String")) {
						throw new CompileError("Main method should either have no parameter or one of type string[]");
					}
					if (!returnType.equals("I")) {
						throw new CompileError("Main method should have return type 'int'");
					}
				}

				parameters.put(name, resolvedType.length() > 1 ? resolvedType + ";" : resolvedType);
			}
		}

		MethodData methodData = new MethodData(methodName, returnType, accessor);
		methodData.setLocals(parameters);
		methodData.setParameters(parameters);

		MethodVisitor m = cw.visitMethod(accessor, methodName, methodData.getSignature(), null, null);
		m.visitCode();

		int line = 0;
		int localVariables = 0;

		Label start = new Label();
		Label post = new Label();
		{
			m.visitLabel(start);
			m.visitLineNumber(line++, start);
		}

		String[] inner = Strings.before(Strings.after(lines, 1), 1);
		String innerBody = Strings.accumulate(inner).trim();
		String[] inner2 = Strings.accumulate(innerBody);
		line = new GenericBodyCompiler(methodData).compile(data, m, line, innerBody, inner2);
		{
			m.visitLabel(post);
			int index = 0;
			for (Entry<String, String> par : parameters.entrySet()) {
				m.visitLocalVariable(par.getKey(), par.getValue(), null, start, post, index);
				index++;
				localVariables++;
			}

			m.visitMaxs(128, localVariables); // TODO 128
		}
		m.visitEnd();

		data.addMethod(methodName, methodData);

		// Label l0 = new Label();
		// mv.visitLabel(l0);
		// mv.visitLineNumber(7, l0);
		// mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out",
		// "Ljava/io/PrintStream;");
		// mv.visitLdcInsn("Hello, world!");
		// mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
		// "(Ljava/lang/String;)V", false);
		// Label l1 = new Label();
		// mv.visitLabel(l1);
		// mv.visitLineNumber(8, l1);
		// mv.visitLdcInsn(0);
		// Label l2 = new Label();
		// mv.visitLabel(l2);
		// mv.visitLineNumber(9, l2);
		// mv.visitInsn(IRETURN);
		// Label l3 = new Label();
		// mv.visitLabel(l3);
		// // mv.visitLocalVariable("args", "[Ljava/lang/String;", null, l0,
		// // l3, 0);
		// mv.visitMaxs(2, 1);
		// mv.visitEnd();
	}
}
