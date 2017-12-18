package cornflakes.compiler;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassWriter;

public class HeadCompiler extends Compiler implements PostCompiler {
	private String[] after;
	private ClassWriter cw;
	private ClassData data;
	
	@Override
	public void compile(ClassData data, ClassWriter cw, String body, String[] lines) {
		this.cw = cw;
		this.data = data;
		
		String className = "";
		String simple = "";
		String parent = "java/lang/Object";
		String packageName = "";
		String firstLine = Strings.normalizeSpaces(lines[0]);
		int index = 1;

		if (firstLine.startsWith("package")) {
			if (!firstLine.startsWith("package ")) {
				throw new CompileError("Expecting space ' ' between identifiers");
			}

			className = firstLine.substring(firstLine.indexOf(" ") + 1);
			Strings.handleLetterString(className, Strings.PERIOD);
			className = Strings.transformClassName(className) + "/";
			packageName = className.substring(0, className.length() - 1);

			firstLine = Strings.normalizeSpaces(lines[1]);
			index = 2;
		}

		int accessor = ACC_SUPER;
		if (!firstLine.contains("class ")) {
			throw new CompileError("Expecting class definition");
		} else {
			String before = firstLine.substring(0, firstLine.indexOf("class")).trim();

			if (!before.isEmpty()) {
				List<String> usedKeywords = new ArrayList<>();
				String[] split = before.split(" ");
				for (String key : split) {
					key = key.trim();
					if (usedKeywords.contains(key)) {
						throw new CompileError("Duplicate keyword: " + key);
					}
					if (key.equals("abstract")) {
						accessor |= ACC_ABSTRACT;
					} else if (key.equals("public")) {
						if (usedKeywords.contains("private") || usedKeywords.contains("protected")) {
							throw new CompileError("Cannot have multiple access modifiers");
						}

						accessor |= ACC_PUBLIC;
					} else if (key.equals("protected")) {
						if (usedKeywords.contains("private") || usedKeywords.contains("public")) {
							throw new CompileError("Cannot have multiple access modifiers");
						}

						accessor |= ACC_PROTECTED;
					} else if (key.equals("sealed")) {
						accessor |= ACC_FINAL;
					} else {
						throw new CompileError("Unexpected keyword: " + key);
					}
					usedKeywords.add(key);
				}
			}

			String after = firstLine.substring(firstLine.indexOf("class")).trim();
			String[] keywordSplit = after.split(" ");
			className += simple = keywordSplit[1];

			Strings.handleLetterString(keywordSplit[1]);

			if (keywordSplit.length > 2) {
				if (keywordSplit[2].equals("extends")) {
					if (keywordSplit.length < 4) {
						throw new CompileError("Expecting identifier after keyword");
					}
					parent = data.resolveClass(keywordSplit[3]);
					Strings.handleLetterString(parent, Strings.SLASH);
				} else {
					throw new CompileError("Unexpected token: " + keywordSplit[3]);
				}
			}
		}

		data.setClassName(className);
		data.setClassWriter(cw);
		data.setSimpleClassName(simple);
		data.setParentName(parent);
		data.setModifiers(accessor);
		data.setPackageName(packageName);

		Compiler.register(cw, data);
		ClassData.registerCornflakesClass(data);

		cw.visit(V1_8, accessor, className, null, parent, null);
		cw.visitSource(data.getSourceName(), null);

		after = Strings.after(lines, index);
		
		Compiler.addPostCompiler(className, this);
	}

	@Override
	public void write() {
		BodyCompiler compiler = new BodyCompiler(data, cw, Strings.accumulate(after), after);
		Compiler.addPostCompiler(data.getClassName(), compiler);
	}
}
