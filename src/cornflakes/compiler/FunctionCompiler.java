package cornflakes.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
	private boolean isBodyless;

	public FunctionCompiler(boolean write, boolean bodyless) {
		this.write = write;
		this.isBodyless = bodyless;
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
			boolean index = false;
			boolean iter = false;
			boolean operator = false;
			String keywords = lines[0].substring(0, lines[0].indexOf("func")).trim();
			List<String> usedKeywords = new ArrayList<>();
			if (!keywords.isEmpty()) {
				String[] split = keywords.split(" ");
				for (String key : split) {
					key = key.trim();
					if (usedKeywords.contains(key)) {
						throw new CompileError("Duplicate keyword: " + key);
					}
					if (key.equals("abstract")) {
						accessor |= ACC_ABSTRACT;
					} else if (key.equals("public")) {
						accessor |= ACC_PUBLIC;
					} else if (key.equals("private")) {
						accessor |= ACC_PRIVATE;
					} else if (key.equals("protected")) {
						accessor |= ACC_PROTECTED;
					} else if (key.equals("final")) {
						accessor |= ACC_FINAL;
					} else if (key.equals("static")) {
						accessor |= ACC_STATIC;
					} else if (key.equals("sync")) {
						accessor |= ACC_SYNCHRONIZED;
					} else if (key.equals("this")) {
						index = true;
					} else if (key.equals("override")) {
						override = true;
					} else if (key.equals("iter")) {
						iter = true;
					} else if (key.equals("operator")) {
						operator = true;
					} else {
						throw new CompileError("Unexpected keyword: " + key);
					}
					usedKeywords.add(key);
				}
			}

			for (String key : usedKeywords) {

				if (key.equals("abstract")) {
					if (!data.hasModifier(ACC_ABSTRACT)) {
						throw new CompileError("Cannot have abstract functions in a non-abstract class");
					}

					if (usedKeywords.contains("static")) {
						throw new CompileError("Abstract functions cannot be static");
					}

					if (usedKeywords.contains("private")) {
						throw new CompileError("Abstract functions cannot be private");
					}

					if (!isBodyless) {
						throw new CompileError("Abstract functions cannot have a body");
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

					if (isBodyless) {
						throw new CompileError("Bodyless functions cannot be private");
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
					if (usedKeywords.contains("this")) {
						throw new CompileError("Indexer functions cannot be static");
					}
					if (isBodyless) {
						throw new CompileError("Bodyless functions cannot be static");
					}

					accessor |= ACC_STATIC;
				} else if (key.equals("sync")) {
					accessor |= ACC_SYNCHRONIZED;
				} else if (key.equals("this")) {
					index = true;

					if (usedKeywords.contains("operator")) {
						throw new CompileError("Operator overloads cannot be indexer functions");
					}
				} else if (key.equals("override")) {
					override = true;

					if (isBodyless) {
						throw new CompileError("Bodyless functions cannot be overrides for functions from a parent");
					}
				} else if (key.equals("iter")) {
					iter = true;

					if (usedKeywords.contains("operator")) {
						throw new CompileError("Operator overloads cannot be iterator functions");
					}
				} else if (key.equals("operator")) {
					operator = true;

					if (!usedKeywords.contains("static")) {
						throw new CompileError("Operator overloads must be static");
					}
				}
			}

			if (isBodyless) {
				if (!usedKeywords.contains("abstract")) {
					throw new CompileError("Bodyless functions must be abstract");
				}
			}

			String after = lines[0].substring(lines[0].indexOf("func") + "func".length()).trim();
			String withoutBracket = after.substring(0, after.length() - 1).trim();
			if (index) {
				Strings.handleMatching(withoutBracket, '[', ']');
			} else {
				Strings.handleMatching(withoutBracket, '(', ')');
			}

			String methodName = withoutBracket.substring(0, withoutBracket.indexOf(index ? '[' : '(')).trim();
			if (operator) {
				methodName = MathOperator.getOperatorOverloadFunction(MathOperator.toOp(methodName));
			}
			Strings.handleLetterString(methodName, Strings.VARIABLE_NAME);

			if (data.hasMethod(methodName)) {
				throw new CompileError("Duplicate function: " + methodName);
			}

			String returnType = "V";
			if (withoutBracket.contains("->")) {
				String afterParams = withoutBracket.substring(withoutBracket.indexOf("->") + 2).trim();
				Strings.handleLetterString(afterParams, Strings.TYPE);

				if (Types.isTupleDefinition(afterParams)) {
					returnType = afterParams;
				} else {
					returnType = data.resolveClass(afterParams).getTypeSignature();
				}

				if (iter) {
					if (!(returnType.equals("Ljava/util/Iterator;")
							|| returnType.equals("Lcornflakes/lang/YieldIterator;"))) {
						throw new CompileError(
								"Iterator functions do not need a specified type; if one is supplied, it should be of explicit type java.util.Iterator or cornflakes.lang.YieldIterator");
					}
				} else if (operator) {
					if (!returnType.equals(Types.padSignature(data.getClassName()))) {
						throw new CompileError(
								"Operator overloads do not need a specified type; if one is supplied, it should be the type of the declaring class");
					}
				}
			} else {
				if (iter) {
					returnType = "Lcornflakes/lang/YieldIterator;";
				} else if (operator) {
					returnType = Types.padSignature(data.getClassName());
				}
			}

			this.methodData = new MethodData(data, null, null, false, -1);
			List<ParameterData> parameters = new ArrayList<>();
			if (index) {
				String params = withoutBracket.substring(withoutBracket.indexOf('[') + 1, withoutBracket.indexOf(']'))
						.trim();
				if (!params.isEmpty()) {
					String[] split = params.split(",");
					if (split.length != 1 && split.length != 2) {
						throw new CompileError("Indexer functions have 1 parameter, and set-indexer functions have 2");
					}

					if (split.length == 1) {
						methodName = "_get_index_";
						this.data.setGetIndexedClass(true);
					} else {
						methodName = "_set_index_";
						this.data.setSetIndexedClass(true);
					}

					for (int i = 0; i < split.length; i++) {
						String[] spl = split[i].trim().split(":");
						if (spl.length == 1) {
							throw new CompileError(
									"Parameters must have a specified type, in the format 'name': 'type'");
						} else if (spl.length > 2) {
							throw new CompileError("Unexpected symbol: " + spl[2]);
						}

						String name = spl[0].trim();
						String type = spl[1].trim();

						Strings.handleLetterString(name, Strings.VARIABLE_NAME);
						Strings.handleLetterString(type, Strings.TYPE);

						String resolvedType = Types.isPrimitive(type) ? Types.getTypeSignature(type)
								: data.resolveClass(type).getTypeSignature();
						parameters.add(new ParameterData(this.methodData, name,
								DefinitiveType.assume(Types.padSignature(resolvedType)), 0));
					}
				} else {
					throw new CompileError("Indexer functions have 1 parameter");
				}
			} else {
				String params = withoutBracket;
				if (Strings.contains(params, "->")) {
					params = params.substring(0, params.lastIndexOf("->"));
				}
				params = params.substring(params.indexOf('(') + 1, params.lastIndexOf(')')).trim();
				if (!params.isEmpty()) {
					String[] split = Strings.splitParameters(params);
					for (String par : split) {
						par = Strings.normalizeSpaces(par);

						String[] spl = par.split(":");
						if (spl.length == 1) {
							throw new CompileError(
									"Parameters must have a specified type, in the format 'name': 'type'");
						} else if (spl.length > 2) {
							throw new CompileError("Unexpected symbol: " + spl[2]);
						}

						String name = spl[0].trim();
						String type = spl[1].trim();

						Strings.handleLetterString(name, Strings.VARIABLE_NAME);
						Strings.handleLetterString(type, Strings.TYPE);

						for (ParameterData datum : parameters) {
							if (datum.getName().equals(name)) {
								throw new CompileError("Duplicate parameter name: " + name);
							}
						}

						if (Types.isTupleDefinition(type)) {
							parameters.add(new ParameterData(this.methodData, name, DefinitiveType.assume(type), 0));
						} else {
							String resolvedType = Types.isPrimitive(type) ? Types.getTypeSignature(type)
									: data.resolveClass(type).getTypeSignature();
							parameters.add(
									new ParameterData(this.methodData, name, DefinitiveType.assume(resolvedType), 0));
						}
					}
				}
			}

			if (operator) {
				if (parameters.size() != 2 || !parameters.get(0).getType().equals(data.getClassName())) {
					throw new CompileError(
							"Operator overloads have 2 parameters, where the first one has the type of the declaring class");
				}
			}

			methodData.setName(methodName);
			methodData.setReturnType(DefinitiveType.assume(returnType));
			methodData.setModifiers(accessor);
			methodData.setParameters(parameters);
			if (iter) {
				methodData.setIterator(-3);
			}

			try {
				boolean hasAny = data.hasMethodBySignature(methodName, methodData.getSignature());
				if (hasAny) {
					if (!override) {
						throw new CompileError(
								"A superclass of " + Types.beautify(data.getClassName()) + " has a function named "
										+ methodName + ". If you meant to override it, use the 'override' keyword");
					}
				} else {
					if (override) {
						throw new CompileError("There is no superclass function named " + methodName);
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

			m.visitLabel(start);
			m.visitLineNumber(line++, start);

			Block block = new Block(0, start, post);
			this.methodData.setBlock(block);

			if (!methodData.hasModifier(ACC_STATIC)) {
				this.methodData.addLocalVariable();
			}
			if (methodData.isIterator()) {
				this.methodData.addLocalVariable();
			}

			int itrIdx = !methodData.hasModifier(ACC_STATIC) ? 1 : 0;
			if (methodData.isIterator()) {
				methodData.setIterator(itrIdx);
			}

			HashMap<String, Integer> paramMap = new HashMap<>();
			for (ParameterData par : methodData.getParameters()) {
				paramMap.put(par.getName(), this.methodData.getLocalVariables());
				this.methodData.addLocalVariable();
			}

			if (methodData.isIterator()) {
				m.visitTypeInsn(NEW, "cornflakes/lang/YieldIterator");
				m.visitInsn(DUP);
				m.visitMethodInsn(INVOKESPECIAL, "cornflakes/lang/YieldIterator", "<init>", "()V", false);
				m.visitVarInsn(ASTORE, itrIdx);
			}

			String[] inner = Strings.before(Strings.after(lines, 1), 1);
			String innerBody = Strings.accumulate(inner).trim();
			String[] inner2 = Strings.accumulate(innerBody);

			GenericBodyCompiler gbc = new GenericBodyCompiler(methodData);
			gbc.compile(data, m, block, innerBody, inner2);

			if (!gbc.returns()) {
				if (!block.doesThrow()) {
					if (methodData.getReturnType().getTypeSignature().equals("V")) {
						if (methodData.getName().equals("_get_index_")) {
							throw new CompileError("Get-indexer functions must return a value");
						}

						m.visitInsn(RETURN);
					} else if (methodData.isIterator()) {
						m.visitVarInsn(ALOAD, itrIdx);
						m.visitInsn(ARETURN);
					} else {
						throw new CompileError("A non-void function must return a value");
					}
				}
			}

			m.visitLabel(post);
			if (!methodData.hasModifier(ACC_STATIC)) {
				m.visitLocalVariable("this", Types.padSignature(data.getClassName()), null, start, post, 0);
			}

			if (methodData.isIterator()) {
				m.visitLocalVariable("_iterator", "Lcornflakes/lang/YieldIterator;", null, start, post, itrIdx);
			}
			for (ParameterData par : methodData.getParameters()) {
				m.visitLocalVariable(par.getName(), par.getType().getAbsoluteTypeSignature(), null, start, post,
						paramMap.get(par.getName()));
			}

			m.visitMaxs(this.methodData.getStackSize(), this.methodData.getLocalVariables());
			m.visitEnd();
		}
	}

	public void write() {
		write = true;
		compile(data, cw, body, lines);
	}

	public boolean isBodyless() {
		return isBodyless;
	}

	public void setBodyless(boolean isBodyless) {
		this.isBodyless = isBodyless;
	}
}
