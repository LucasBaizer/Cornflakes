package cornflakes;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassWriter;

public class HeadCompiler extends Compiler {
	@Override
	public void compile(ClassData data, ClassWriter cw, String body, String[] lines) {
		String className = "";
		String simple = "";
		String parent = "java/lang/Object";
		String firstLine = Strings.normalizeSpaces(lines[0]);
		int index = 1;

		if (firstLine.startsWith("package")) {
			if (!firstLine.startsWith("package ")) {
				throw new CompileError("Expecting space ' ' between identifiers");
			}

			className = Strings.transformClassName(firstLine.substring(firstLine.indexOf(" ") + 1));
			Strings.handleLetterString(className, Strings.PERIOD);
			className += "/";

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
			if (keywordSplit.length != 2) {
				throw new CompileError("Class declaration should be in format [modifiers] class [name]");
			}
			className += simple = keywordSplit[1];

			Strings.handleLetterString(keywordSplit[1]);

			if (keywordSplit.length > 2) {
				if (keywordSplit[2].equals("extends")) {
					if (keywordSplit.length < 4) {
						throw new CompileError("Expecting identifier after keyword");
					}
					parent = data.resolveClass(keywordSplit[3]);
					Strings.handleLetterString(parent, Strings.PERIOD);
				} else {
					throw new CompileError("Unexpected token: " + keywordSplit[3]);
				}
			}
		}

		data.setClassName(className);
		data.setSimpleClassName(simple);
		data.setParentName(parent);
		data.setModifiers(accessor);

		cw.visit(V1_8, accessor, className, null, parent, null);
		cw.visitSource(data.getSourceName(), null);

		String[] after = Strings.after(lines, index);
		new BodyCompiler().compile(data, cw, Strings.accumulate(after), after);
	}
}
