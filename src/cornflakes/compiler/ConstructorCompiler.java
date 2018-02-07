package cornflakes.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class ConstructorCompiler extends Compiler implements PostCompiler {
	private ConstructorData methodData;
	private boolean write;
	private int accessor;
	private ClassData data;
	private ClassWriter cw;
	private Line body;
	private Line[] lines;

	public ConstructorCompiler(boolean write) {
		this.write = write;
	}

	@Override
	public void compile(ClassData data, ClassWriter cw, Line body, Line[] lines) {
		if (!write) {
			Compiler.addPostCompiler(data.getClassName(), this);

			this.data = data;
			this.cw = cw;
			this.body = body;
			this.lines = lines;

			String keywords = lines[0].substring(0, lines[0].indexOf("constructor")).trim().getLine();
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

						if (usedKeywords.contains("abstract")) {
							throw new CompileError("Abstract methods cannot be private");
						}

						accessor |= ACC_PRIVATE;
					} else if (key.equals("protected")) {
						if (usedKeywords.contains("private") || usedKeywords.contains("public")) {
							throw new CompileError("Cannot have multiple access modifiers");
						}

						accessor |= ACC_PROTECTED;
					} else {
						throw new CompileError("Unexpected keyword: " + key);
					}
					usedKeywords.add(key);
				}
			}

			String after = lines[0].substring(lines[0].indexOf("constructor") + "constructor".length()).trim()
					.getLine();
			String withoutBracket = after.substring(0, after.length() - 1).trim();
			Strings.handleMatching(withoutBracket, '(', ')');

			String methodName = withoutBracket.substring(0, withoutBracket.indexOf('(')).trim();
			Strings.handleLetterString(methodName);

			if (data.hasMethod(methodName)) {
				throw new CompileError("Duplicate method: " + methodName);
			}

			String params = withoutBracket.substring(withoutBracket.indexOf('(') + 1, withoutBracket.lastIndexOf(')'))
					.trim();
			List<ParameterData> parameters = new ArrayList<>();
			methodData = new ConstructorData(data, -1);
			if (!params.isEmpty()) {
				String[] split = Strings.splitParameters(params);
				for (String par : split) {
					par = Strings.normalizeSpaces(par);

					String[] spl = par.split(":");
					if (spl.length == 1) {
						throw new CompileError("Parameters must have a specified type");
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
						parameters
								.add(new ParameterData(this.methodData, name, DefinitiveType.assume(type), ACC_FINAL));
					} else {
						String resolvedType = Types.isPrimitive(type) ? Types.getTypeSignature(type)
								: data.resolveClass(type).getTypeSignature();
						parameters.add(new ParameterData(this.methodData, name, DefinitiveType.assume(resolvedType),
								ACC_FINAL));
					}
				}
			}

			methodData.setModifiers(accessor);
			methodData.setParameters(parameters);

			data.addConstructor(methodData);
		} else {
			MethodVisitor m = cw.visitMethod(accessor, "<init>", methodData.getSignature(), null, null);
			m.visitCode();

			Label start = new Label();
			Label post = new Label();
			m.visitLabel(start);
			m.visitLineNumber(0, start);

			ConstructorBlock block = new ConstructorBlock(0, start, post);
			this.methodData.setBlock(block);

			this.methodData.addLocalVariable();
			HashMap<String, Integer> paramMap = new HashMap<>();
			for (ParameterData par : methodData.getParameters()) {
				paramMap.put(par.getName(), this.methodData.getLocalVariables());
				this.methodData.addLocalVariable();
			}

			assignDefaults(m, data, this.methodData, block);

			Line[] inner = Strings.before(Strings.after(lines, 1), 1);
			GenericBodyCompiler gbc = new GenericBodyCompiler(methodData);
			gbc.compile(data, m, block, inner);

			if (!block.hasCalledConstructor()) {
				throw new CompileError("Super must be called exactly one time before the constructor ends");
			}

			if (!gbc.returns()) {
				m.visitInsn(RETURN);
			}

			m.visitLabel(post);
			m.visitLocalVariable("this", Types.padSignature(data.getClassName()), null, start, post, 0);
			for (ParameterData par : methodData.getParameters()) {
				m.visitLocalVariable(par.getName(), par.getType().getTypeName(), null, start, post,
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

	public void compileDefault(ClassData data, ClassWriter cw) {
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		Label l1 = new Label();
		mv.visitLabel(l0);
		mv.visitLineNumber(0, l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, data.getParentName(), "<init>", "()V", false);
		ConstructorData mData = new ConstructorData(data, ACC_PUBLIC);
		assignDefaults(mv, data, mData, new ConstructorBlock(0, l0, l1));
		mv.visitInsn(RETURN);
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", "L" + data.getClassName() + ";", null, l0, l1, 0);
		mv.visitMaxs(1 + mData.getStackSize(), 1);
		mv.visitEnd();

		data.addConstructor(mData);
	}

	private void assignDefaults(MethodVisitor m, ClassData data, MethodData mData, Block block) {
		for (FieldData datum : data.getFields()) {
			if (!datum.hasModifier(ACC_STATIC) && datum.getProposedData() != null) {
				m.visitVarInsn(ALOAD, 0);

				String type = datum.getType().getTypeSignature();
				if (Types.isPrimitive(type) || type.equals("Ljava/lang/String;")) {
					int push = Types.getOpcode(Types.PUSH, type);
					if (push == LDC) {
						m.visitLdcInsn(datum.getProposedData());
					} else {
						m.visitVarInsn(push, Integer.parseInt(datum.getProposedData().toString()));
					}
					mData.ics();

					m.visitFieldInsn(PUTFIELD, data.getClassName(), datum.getName(),
							datum.getType().getAbsoluteTypeSignature());
				} else {
					String raw = (String) datum.getProposedData();

					ExpressionCompiler compiler = new ExpressionCompiler(true, this.methodData);
					compiler.compile(data, m, block, new Line[] { body.derive(raw) });

					if (!Types.isSuitable(datum.getType(), compiler.getResultType())) {
						throw new CompileError(Types.beautify(compiler.getResultType().getTypeName())
								+ " is not assignable to " + Types.beautify(datum.getType().getTypeName()));
					}

					m.visitFieldInsn(PUTFIELD, data.getClassName(), datum.getName(),
							datum.getType().getAbsoluteTypeSignature());
				}
			}
		}
	}
}
