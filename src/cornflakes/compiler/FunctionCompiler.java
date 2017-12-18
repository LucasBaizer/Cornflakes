package cornflakes.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class FunctionCompiler extends Compiler implements PostCompiler {
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
			Compiler.addPostCompiler(data.getClassName(), this);

			this.data = data;
			this.cw = cw;
			this.body = body;
			this.lines = lines;

			boolean override = false;
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
					} else if (key.equals("sync")) {
						accessor |= ACC_SYNCHRONIZED;
					} else if (key.equals("override")) {
						override = true;
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

					String[] spl = par.split(":");
					if (spl.length == 1) {
						throw new CompileError("Parameters must have a specified type, in the format 'name': 'type'");
					} else if (spl.length > 2) {
						throw new CompileError("Unexpected symbol: " + spl[2]);
					}

					String name = spl[0].trim();
					String type = spl[1].trim();

					Strings.handleLetterString(name, Strings.VARIABLE_NAME);
					Strings.handleLetterString(type, Strings.VARIABLE_TYPE);

					if (parameters.containsKey(name)) {
						throw new CompileError("Duplicate parameter name: " + par);
					}

					String resolvedType = Types.isPrimitive(type) ? Types.getTypeSignature(type)
							: data.resolveClass(type);
					parameters.put(name, Types.padSignature(resolvedType));
				}
			}

			methodData = new MethodData(methodName, returnType, accessor);
			methodData.setParameters(parameters);

			try {
				boolean hasAny = data.hasMethodBySignature(methodName, methodData.getSignature());
				if (hasAny) {
					if (!override) {
						throw new CompileError("A superclass of " + data.getSimpleClassName() + " has a method named "
								+ methodName + ". If you meant to override it, use the 'override' keyword");
					}
				} else {
					if (override) {
						throw new CompileError("There is no superclass method named " + methodName);
					}
				}
			} catch (ClassNotFoundException e) {
				throw new CompileError(e);
			}

			data.addMethod(methodData);
		} else {
			MethodVisitor m = cw.visitMethod(accessor, methodData.getName(), methodData.getSignature(), null, null);
			m.visitCode();

			int line = 0;

			Label start = new Label();
			Label post = new Label();
			m.visitLabel(post);
			m.visitLineNumber(127, post); // TODO 127

			m.visitLabel(start);
			m.visitLineNumber(line++, start);

			this.methodData.setLabels(start, post);

			if (!methodData.hasModifier(ACC_STATIC)) {
				this.methodData.addLocalVariable();
			}

			HashMap<String, Integer> paramMap = new HashMap<>();
			for (Entry<String, String> par : methodData.getParameters().entrySet()) {
				paramMap.put(par.getKey(), this.methodData.getLocalVariables());
				this.methodData.addLocalVariable();
			}

			String[] inner = Strings.before(Strings.after(lines, 1), 1);
			String innerBody = Strings.accumulate(inner).trim();
			String[] inner2 = Strings.accumulate(innerBody);

			GenericBodyCompiler gbc = new GenericBodyCompiler(methodData);
			gbc.compile(data, m, start, post, innerBody, inner2);

			if (!gbc.returns()) {
				if (methodData.getReturnTypeSignature().equals("V")) {
					m.visitInsn(RETURN);
				} else {
					throw new CompileError("A non-void method must return a value");
				}
			}

			m.visitLabel(post);
			for (Entry<String, String> par : methodData.getParameters().entrySet()) {
				m.visitLocalVariable(par.getKey(), par.getValue(), null, start, post, paramMap.get(par.getKey()));
			}

			m.visitMaxs(this.methodData.getStackSize(), this.methodData.getLocalVariables());
			m.visitEnd();
		}
	}

	public void write() {
		write = true;
		compile(data, cw, body, lines);
	}
}
