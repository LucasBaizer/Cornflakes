package cornflakes.compiler;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassWriter;

import cornflakes.compiler.CompileUtils.VariableDeclaration;

public class StatementCompiler extends Compiler {
	@Override
	public void compile(ClassData data, ClassWriter cw, String body, String[] lines) {
		if (!body.contains(" ")) {
			throw new CompileError("Expecting parameter after statement");
		}

		String[] spaceSplit = body.split(" ", 2);
		String cmd = spaceSplit[0];

		if (cmd.equals("use")) {
			String val = spaceSplit[1].trim().replace("{", "").replace("}", "");

			String prefix = val.substring(0, val.lastIndexOf('.') + 1);
			String[] spl = val.substring(val.lastIndexOf('.') + 1).split(",");
			for (String s : spl) {
				String trim = s.trim();
				Strings.handleLetterString(trim);
				data.use(prefix + trim);
			}
		} else if (body.contains("var") || body.contains("const")) {
			String type = body.contains("var") ? "var" : "const";

			int accessor = 0;
			String keywords = Strings.normalizeSpaces(body.substring(0, body.indexOf(type)));
			List<String> usedKeywords = new ArrayList<>();
			if (!keywords.isEmpty()) {
				String[] split = keywords.split(" ");
				for (String key : split) {
					key = key.trim();
					if (usedKeywords.contains(key)) {
						throw new CompileError("Duplicate keyword: " + key);
					}
					if (key.equals("public")) {
						if (usedKeywords.contains("private") || usedKeywords.contains("protected")) {
							throw new CompileError("Cannot have multiple access modifiers");
						}

						accessor |= ACC_PUBLIC;
					} else if (key.equals("private")) {
						if (usedKeywords.contains("public") || usedKeywords.contains("protected")) {
							throw new CompileError("Cannot have multiple access modifiers");
						}

						accessor |= ACC_PRIVATE;
					} else if (key.equals("protected")) {
						if (usedKeywords.contains("private") || usedKeywords.contains("public")) {
							throw new CompileError("Cannot have multiple access modifiers");
						}

						accessor |= ACC_PROTECTED;
					} else if (key.equals("final")) {
						throw new CompileError(
								"The 'final' keyword is not used in Cornflakes- use the 'const' variable declararor if you need a constant variable");
					} else if (key.equals("static")) {
						accessor |= ACC_STATIC;
					} else {
						throw new CompileError("Unexpected keyword: " + key);
					}
					usedKeywords.add(key);
				}
			}

			String var = Strings.normalizeSpaces(body.substring(body.indexOf(type) + type.length())).trim();
			String[] split = var.split(":");
			if (split.length == 1) {
				throw new CompileError("Expecting variable type");
			}

			String variableName = split[0].trim();

			if (data.hasField(variableName)) {
				throw new CompileError("Duplicate variable: " + variableName);
			}

			VariableDeclaration decl = CompileUtils.declareVariable(null, data, null, null, null, body, split);
			Object value = decl.getValue();
			String valueType = decl.getValueType();
			String variableType = decl.getVariableType();
			boolean useValue = false;

			FieldData fdata = new FieldData(variableName, variableType, accessor);
			if ((accessor & ACC_STATIC) == ACC_STATIC) {
				if (valueType != null && (valueType.equals("I") || valueType.equals("J") || valueType.equals("F")
						|| valueType.equals("string"))) {
					useValue = true;
				} else {
					fdata.setProposedData(value);
				}
			} else {
				fdata.setProposedData(value);
			}

			cw.visitField(accessor, variableName, variableType, null, useValue ? value : null).visitEnd();
			data.addField(fdata);
		} else {
			throw new CompileError("Unexpected statement: " + cmd);
		}
	}
}
