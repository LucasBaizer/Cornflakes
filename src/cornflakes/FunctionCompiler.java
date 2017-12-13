package cornflakes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class FunctionCompiler extends Compiler {
	private MethodData methodData;
	private boolean write;
	private int accessor;
	private ClassData data;
	private ClassWriter cw;
	private String body;
	private String[] lines;

	public FunctionCompiler(boolean write) {
		this.write = write;
	}

	@Override
	public void compile(ClassData data, ClassWriter cw, String body, String[] lines) {
		if (!write) {
			Compiler.addPostCompiler(this);

			this.data = data;
			this.cw = cw;
			this.body = body;
			this.lines = lines;

			String keywords = lines[0].substring(0, lines[0].indexOf("function")).trim();
			List<String> usedKeywords = new ArrayList<>();
			if (!keywords.isEmpty()) {
				String[] split = keywords.split(" ");
				for (String key : split) {
					key = key.trim();
					if (usedKeywords.contains(key)) {
						throw new CompileError("Duplicate keyword: " + key);
					}
					if (key.equals("abstract")) {
						if (!data.hasModifier(ACC_ABSTRACT)) {
							throw new CompileError("Cannot have abstract methods in a non-abstract class");
						}

						if (usedKeywords.contains("static")) {
							throw new CompileError("Abstract methods cannot be static");
						}

						if (usedKeywords.contains("private")) {
							throw new CompileError("Abstract methods cannot be private");
						}

						accessor |= ACC_ABSTRACT;
					} else if (key.equals("public")) {
						if (usedKeywords.contains("private") || usedKeywords.contains("protected")) {
							throw new CompileError("Cannot have multiple access modifiers");
						}

						accessor |= ACC_PUBLIC;
					} else if (key.equals("private")) {
						if (usedKeywords.contains("public") || usedKeywords.contains("protected")) {
							throw new CompileError("Cannot have multiple access modifiers");
						}

						if (usedKeywords.contains("abstract")) {
							throw new CompileError("Abstract methods cannot be private");
						}

						accessor |= ACC_PRIVATE;
					} else if (key.equals("protected")) {
						if (usedKeywords.contains("private") || usedKeywords.contains("public")) {
							throw new CompileError("Cannot have multiple access modifiers");
						}

						accessor |= ACC_PROTECTED;
					} else if (key.equals("final")) {
						accessor |= ACC_FINAL;
					} else if (key.equals("static")) {
						if (usedKeywords.contains("abstract")) {
							throw new CompileError("Abstract methods cannot be static");
						}

						accessor |= ACC_STATIC;
					} else {
						throw new CompileError("Unexpected keyword: " + key);
					}
					usedKeywords.add(key);
				}
			}

			String after = lines[0].substring(lines[0].indexOf("function") + "function".length()).trim();
			String withoutBracket = after.substring(0, after.length() - 1).trim();
			Strings.handleMatching(withoutBracket, '(', ')');

			String methodName = withoutBracket.substring(0, withoutBracket.indexOf('(')).trim();
			Strings.handleLetterString(methodName);

			if (data.hasMethod(methodName)) {
				throw new CompileError("Duplicate method: " + methodName);
			}

			String returnType = "V";
			if (withoutBracket.contains("->")) {
				String afterParams = withoutBracket.substring(withoutBracket.indexOf("->") + 2).trim();
				Strings.handleLetterString(afterParams, Strings.VARIABLE_TYPE);

				returnType = Types.padSignature(data.resolveClass(afterParams));
			}

			String params = withoutBracket.substring(withoutBracket.indexOf('(') + 1, withoutBracket.indexOf(')'))
					.trim();
			Map<String, String> parameters = new LinkedHashMap<>();
			if (!params.isEmpty()) {
				String[] split = params.split(",");
				for (String par : split) {
					par = Strings.normalizeSpaces(par);

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
							throw new CompileError(
									"Main method should either have no parameter or one of type string[]");
						}
						if (!returnType.equals("I")) {
							throw new CompileError("Main method should have return type 'int'");
						}
					}

					parameters.put(name, Types.padSignature(resolvedType));
				}
			}

			methodData = new MethodData(methodName, returnType, accessor);
			methodData.setLocals(parameters);
			methodData.setParameters(parameters);

			data.addMethod(methodData);
		} else {
			MethodVisitor m = cw.visitMethod(accessor, methodData.getName(), methodData.getSignature(), null, null);
			m.visitCode();

			int line = 0;
			int localVariables = 0;

			Label start = new Label();
			Label post = new Label();
			{
				m.visitLabel(start);
				m.visitLineNumber(line++, start);

				if (!methodData.hasModifier(ACC_STATIC)) {
					m.visitVarInsn(ALOAD, 0);
					localVariables++;
				}
			}

			String[] inner = Strings.before(Strings.after(lines, 1), 1);
			String innerBody = Strings.accumulate(inner).trim();
			String[] inner2 = Strings.accumulate(innerBody);
			GenericBodyCompiler gbc = new GenericBodyCompiler(methodData);
			line = gbc.compile(data, m, line, innerBody, inner2);

			if (!gbc.returns()) {
				if (methodData.getReturnTypeSignature().equals("V")) {
					m.visitInsn(RETURN);
				} else {
					throw new CompileError("A non-void method must return a value");
				}
			}

			m.visitLabel(post);
			int index = 0;
			for (Entry<String, String> par : methodData.getParameters().entrySet()) {
				m.visitLocalVariable(par.getKey(), par.getValue(), null, start, post, index);
				index++;
				localVariables++;
			}

			m.visitMaxs(128, localVariables); // TODO 128
			m.visitEnd();
		}
	}

	public void write() {
		write = true;
		compile(data, cw, body, lines);
	}
}
