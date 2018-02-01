package cornflakes.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

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
				String[] split = trim.split(" as ");
				if (split.length == 1) {
					Strings.handleLetterString(trim, Strings.NUMBERS);
					data.use(prefix + trim);
				} else {
					String trim0 = split[0].trim();
					Strings.handleLetterString(trim0, Strings.NUMBERS);
					data.use(prefix + trim0, split[1].trim());
				}
			}
		} else if (cmd.equals("macro")) {
			String[] macroCmd = Strings.split(body, " ");
			if (macroCmd.length < 3) {
				throw new CompileError("Invalid macro statement");
			}
			String subcmd = macroCmd[1];
			if (subcmd.equals("use")) {
				if (macroCmd.length < 4) {
					throw new CompileError("Expecting 'macro use <name> from <class>'");
				}

				String after = body.split(" ", 3)[2];
				String[] within = Strings.split(after, " from ");
				if (within.length != 2) {
					throw new CompileError("Expecting 'macro use <name> as <class>'");
				}

				String name = within[0];
				String clazz = within[1];

				String resolved = data.resolveClass(clazz).getTypeSignature();
				try {
					ClassData classData = ClassData.forName(resolved);
					if (classData.isJavaClass() || classData.getMacros().size() == 0) {
						throw new CompileError("No macros are defined in class " + resolved);
					}

					if (name.equals("*")) {
						for (Entry<String, String> entry : classData.getMacros().entrySet()) {
							data.useMacro(entry.getKey(), entry.getValue());
						}
					} else {
						if (!classData.hasMacro(name)) {
							throw new CompileError("Undefined macro '" + name + "' in class " + resolved);
						}

						data.useMacro(name, classData.resolveMacro(name));
					}
				} catch (ClassNotFoundException e) {
					throw new CompileError(e);
				}
			} else if (subcmd.equals("define")) {
				if (macroCmd.length < 4) {
					throw new CompileError("Expecting 'macro define <name> as <replacement>'");
				}

				String after = body.split(" ", 3)[2];
				String[] within = Strings.split(after, " as ");
				if (within.length != 2) {
					throw new CompileError("Expecting 'macro define <name> as <replacement>'");
				}

				String name = within[0];
				String replacement = within[1];

				data.useMacro(name, replacement);
			} else {
				throw new CompileError("Expecting either 'macro use' or 'macro define'");
			}
		} else if (Strings.contains(body, "var") || Strings.contains(body, "const")) {
			String type = Strings.contains(body, "var") ? "var" : "const";

			int accessor = 0;
			if (type.equals("const")) {
				accessor |= ACC_FINAL;
			}
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

			VariableDeclaration decl = CompileUtils.declareVariable(null, data, null, null, body, split);
			Object value = decl.getValue();
			DefinitiveType valueType = decl.getValueType();
			DefinitiveType variableType = decl.getVariableType();
			boolean useValue = false;

			FieldData fdata = new FieldData(data, variableName, variableType, accessor);
			if ((accessor & ACC_STATIC) == ACC_STATIC) {
				if (!valueType.isNull() && (valueType.equals("I") || valueType.equals("J") || valueType.equals("F")
						|| valueType.equals("D") || valueType.equals("string"))) {
					useValue = true;
				} else {
					fdata.setProposedData(value);
				}
			} else {
				fdata.setProposedData(value);
			}

			cw.visitField(accessor, variableName, variableType.getAbsoluteTypeSignature(), null,
					useValue ? value : null).visitEnd();
			data.addField(fdata);
		} else if (Strings.contains(body, " func ")) {
			FunctionCompiler comp = new FunctionCompiler(true, true);
			comp.compile(data, cw, body, lines);
		} else {
			throw new CompileError("Unexpected statement: " + cmd);
		}
	}
}
