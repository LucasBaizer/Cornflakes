package cornflakes.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class FunctionCompiler extends Compiler implements PostCompiler {
	private MethodData methodData;
	private FunctionType type;
	private boolean write;
	private int accessor;
	private ClassData data;
	private ClassWriter cw;
	private Line body;
	private Line[] lines;
	private boolean isBodyless;

	public FunctionCompiler(FunctionType type, boolean write, boolean bodyless) {
		this.type = type;
		this.write = write;
		this.isBodyless = bodyless;
	}

	@Override
	public void compile(ClassData data, ClassWriter cw, Line line, Line[] lines) {
		if (!write) {
			Compiler.addPostCompiler(data.getClassName(), this);

			this.data = data;
			this.cw = cw;
			this.body = line;
			this.lines = lines;

			boolean override = false;
			String keywords = lines[0].substring(0, lines[0].indexOf(this.type.getKeyword())).trim().getLine();
			List<String> usedKeywords = new ArrayList<>();
			if (type == FunctionType.OPERATOR_OVERLOAD) {
				usedKeywords.add("static");
			}
			if (!keywords.isEmpty()) {
				String[] split = keywords.split(" ");
				for (String key : split) {
					key = key.trim();
					if (usedKeywords.contains(key)) {
						if (type == FunctionType.OPERATOR_OVERLOAD && key.equals("static")) {
							throw new CompileError("Operator overloads are implicitly static");
						} else {
							throw new CompileError("Duplicate keyword: " + key);
						}
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
					} else if (key.equals("override")) {
						override = true;
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
				} else if (key.equals("override")) {
					if (isBodyless) {
						throw new CompileError("Bodyless functions cannot be overrides for functions from a parent");
					}
				}
			}

			if (isBodyless) {
				if (!usedKeywords.contains("abstract")) {
					throw new CompileError("Bodyless functions must be abstract");
				}
			}

			String after = lines[0].substring(lines[0].indexOf(type.getKeyword()) + type.getKeyword().length()).trim()
					.getLine();

			String withoutBracket = after.substring(0, after.length() - 1).trim();
			String exceptionString = null;
			if (withoutBracket.contains(" throws ")) {
				exceptionString = withoutBracket.substring(withoutBracket.indexOf(" throws ") + " throws ".length())
						.trim();
				withoutBracket = withoutBracket.substring(0, withoutBracket.indexOf(" throws ")).trim();
			}

			if (type == FunctionType.INDEXER) {
				Strings.handleMatching(withoutBracket, '[', ']');
			} else {
				Strings.handleMatching(withoutBracket, '(', ')');
			}

			String methodName = withoutBracket
					.substring(0, withoutBracket.indexOf(type == FunctionType.INDEXER ? '[' : '(')).trim();
			int operatorType = -1;
			if (type == FunctionType.OPERATOR_OVERLOAD) {
				operatorType = Operator.toOp(methodName);
				methodName = Operator.getOperatorOverloadFunction(operatorType);
			}
			Strings.handleLetterString(methodName, Strings.VARIABLE_NAME);

			String returnType = "V";
			if (withoutBracket.indexOf(":", withoutBracket.lastIndexOf(')')) != -1) {
				String afterParams = withoutBracket.substring(withoutBracket.lastIndexOf(":") + 1).trim();
				Strings.handleLetterString(afterParams, Strings.TYPE);

				if (Types.isTupleDefinition(afterParams)) {
					returnType = afterParams;
				} else {
					returnType = data.resolveClass(afterParams).getTypeSignature();
				}

				if (type == FunctionType.ITERATOR) {
					if (!(returnType.equals("Ljava/util/Iterator;")
							|| returnType.equals("Lcornflakes/lang/FunctionalIterator;"))) {
						throw new CompileError(
								"Iterator functions do not need a specified type; if one is supplied, it should be of explicit type java.util.Iterator or cornflakes.lang.FunctionalIterator");
					}
				} else if (type == FunctionType.OPERATOR_OVERLOAD) {
					if (Operator.isMathOperator(operatorType)) {
						if (!returnType.equals(Types.padSignature(data.getClassName()))) {
							throw new CompileError(
									"Mathematical operator overloads do not need a specified type; if one is supplied, it should be the type of the declaring class");
						}
					} else if (Operator.isBooleanOperator(operatorType)) {
						if (!returnType.equals("Z")) {
							throw new CompileError(
									"Boolean operator overloads do not need a specified type; if one is supplied, it should be of type bool");
						}
					}
				}
			} else {
				if (type == FunctionType.ITERATOR) {
					returnType = "Lcornflakes/lang/FunctionalIterator;";
				} else if (type == FunctionType.OPERATOR_OVERLOAD) {
					returnType = Operator.isMathOperator(operatorType) ? Types.padSignature(data.getClassName()) : "Z";
				}
			}

			this.methodData = new MethodData(data, null, null, false, -1);
			List<ParameterData> parameters = new ArrayList<>();
			if (type == FunctionType.INDEXER) {
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
								DefinitiveType.assume(Types.padSignature(resolvedType)), ACC_FINAL));
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
							parameters.add(
									new ParameterData(this.methodData, name, DefinitiveType.assume(type), ACC_FINAL));
						} else {
							String resolvedType = Types.isPrimitive(type) ? Types.getTypeSignature(type)
									: data.resolveClass(type).getTypeSignature();
							parameters.add(new ParameterData(this.methodData, name, DefinitiveType.assume(resolvedType),
									ACC_FINAL));
						}
					}
				}
			}

			if (type == FunctionType.OPERATOR_OVERLOAD) {
				if (parameters.size() != 2 || !parameters.get(0).getType().equals(data.getClassName())) {
					throw new CompileError(
							"Operator overloads have 2 parameters, where the first one has the type of the declaring class");
				}
			}

			methodData.setName(methodName);
			methodData.setReturnType(DefinitiveType.assume(returnType));
			methodData.setModifiers(accessor);
			methodData.setParameters(parameters);

			if (exceptionString != null) {
				String[] exceptionSplit = exceptionString.split(",");
				for (String rawException : exceptionSplit) {
					rawException = rawException.trim();

					DefinitiveType exception = data.resolveClass(rawException);
					try {
						if (!exception.isObject() && exception.getObjectType().is("java.lang.Throwable")) {
							throw new CompileError(
									"Cannot add 'throws' declaration for a type which is not the type of or a subclass of java.lang.Throwable: "
											+ Types.beautify(exception.getTypeName()));
						}
					} catch (ClassNotFoundException e) {
						throw new CompileError(e);
					}
					methodData.addExceptionType(exception);
				}
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
				if (!override && hasAny) {
					if (data.hasMethodBySignature(methodName, methodData.getSignature())) {
						throw new CompileError("Duplicate function: " + methodName);
					}
				}
			} catch (ClassNotFoundException e) {
				throw new CompileError(e);
			}
			if (type == FunctionType.ITERATOR) {
				methodData.setIterator(-3);
			}

			data.addMethod(methodData);
		} else {
			String[] ex = null;
			if (this.methodData.getExceptionTypes().size() > 0) {
				ex = this.methodData.getExceptionTypes().stream().map(x -> x.getAbsoluteTypeName())
						.toArray(String[]::new);
			}

			MethodVisitor m = cw.visitMethod(accessor, methodData.getName(), methodData.getSignature(), null, ex);
			m.visitCode();

			Label start = new Label();
			Label post = new Label();

			m.visitLabel(start);
			m.visitLineNumber(this.body.getNumber(), start);

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
				m.visitTypeInsn(NEW, "cornflakes/lang/FunctionalIterator");
				m.visitInsn(DUP);
				m.visitMethodInsn(INVOKESPECIAL, "cornflakes/lang/FunctionalIterator", "<init>", "()V", false);
				m.visitVarInsn(ASTORE, itrIdx);
			}

			Line[] inner = Strings.before(Strings.after(lines, 1), 1);

			GenericBodyCompiler gbc = new GenericBodyCompiler(methodData);
			gbc.compile(data, m, block, inner);

			if (!gbc.returns()) {
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

			m.visitLabel(post);
			if (!methodData.hasModifier(ACC_STATIC)) {
				m.visitLocalVariable("this", Types.padSignature(data.getClassName()), null, start, post, 0);
			}

			if (methodData.isIterator()) {
				m.visitLocalVariable("_iterator", "Lcornflakes/lang/FunctionalIterator;", null, start, post, itrIdx);
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

	public FunctionType getType() {
		return type;
	}

	public void setType(FunctionType type) {
		this.type = type;
	}
}
