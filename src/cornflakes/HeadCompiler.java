package cornflakes;

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

			className = Strings.transformClassName(firstLine.substring(firstLine.indexOf(" ") + 1)) + "/";

			Strings.handleLetterString(className, Strings.PERIOD);
			firstLine = Strings.normalizeSpaces(lines[1]);
			index = 2;
		}

		if (!firstLine.startsWith("class")) {
			throw new CompileError("Expecting class definition");
		} else {
			if (!firstLine.startsWith("class ")) {
				throw new CompileError("Expecting space ' ' between identifiers");
			}

			String[] keywordSplit = firstLine.split(" ");
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

		cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, className, null, parent, null);
		cw.visitSource(data.getSourceName(), null);

		String[] after = Strings.after(lines, index);
		new BodyCompiler().compile(data, cw, Strings.accumulate(after), after);
	}
}
