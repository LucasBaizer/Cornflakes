package cornflakes.compiler;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class GenericBlockCompiler implements GenericCompiler {
	private MethodData data;
	private List<Line[]> lines = new ArrayList<>();

	public GenericBlockCompiler(MethodData data, List<Line[]> lines) {
		this.data = data;
		this.lines = lines;
	}

	@Override
	public void compile(ClassData data, MethodVisitor m, Block block, Line[] rawlines) {
		Label start = new Label();

		this.data.addBlock();

		m.visitLabel(start);
		m.visitLineNumber(this.data.getBlocks(), start);

		Line firstLine = Strings.normalizeSpaces(lines.get(0)[0]);
		Line condition = firstLine.substring(0, firstLine.lastIndexOf('{')).trim();

		if (condition.isEmpty()) {
			Block thisBlock = new Block(block.getStart() + 1, start, null);
			block.addBlock(thisBlock);
			Line[] newLines = Strings.before(Strings.after(lines.get(0), 1), 1);

			thisBlock.setEndLabel(block.getEndLabel());
			new GenericBodyCompiler(this.data).compile(data, m, thisBlock, newLines);
		} else {
			if (condition.startsWith("if ")) {
				boolean hasElse = false;
				Label finalEnd = new Label();

				int last = block.getStart() + 1;
				for (int i = 0; i < lines.size(); i++) {
					Block currentBlock = new Block(last++, start, null);
					block.addBlock(currentBlock);
					Label theEnd = new Label();
					currentBlock.setEndLabel(theEnd);

					firstLine = Strings.normalizeSpaces(lines.get(i)[0]);
					condition = firstLine.substring(0, firstLine.lastIndexOf('{')).trim();
					Line[] newLines = Strings.before(Strings.after(lines.get(i), 1), 1);

					int val;
					if (condition.startsWith("if")) {
						val = 2;
					} else if (condition.startsWith("else")) {
						val = 4;
					} else {
						throw new CompileError("Invalid part of chain");
					}
					Line parse = condition.substring(val).trim();
					if (!parse.isEmpty()) {
						if (parse.startsWith("if ")) {
							parse = parse.substring(2).trim();
						}
						new BooleanExpressionCompiler(this.data, theEnd, true).compile(data, m, currentBlock,
								new Line[] { parse });
					} else {
						if (hasElse) {
							throw new CompileError("Cannot have multiple else blocks attached to one if chain");
						}
						hasElse = true;
					}

					new GenericBodyCompiler(this.data).compile(data, m, block, newLines);
					m.visitFrame(F_SAME, this.data.getLocalVariables(), null, this.data.getCurrentStack(), null);
					m.visitJumpInsn(GOTO, finalEnd);

					m.visitLabel(theEnd);
				}

				m.visitLabel(finalEnd);
			} else if (condition.equals("try")) {
				TryBlock tryBlock = new TryBlock(block.getStart() + 1, start, null);
				block.addBlock(tryBlock);
				Line[] within = Strings.before(Strings.after(lines.get(0), 1), 1);

				Label endLabel = block.getEndLabel();
				int itrEnd = lines.size();
				if (lines.get(lines.size() - 1)[0].getLine().trim().matches("\\}( *?)finally( *?)\\{")) {
					itrEnd--;
					endLabel = new Label();
				}

				tryBlock.setEndLabel(endLabel);
				new GenericBodyCompiler(this.data).compile(data, m, tryBlock, within);

				if (lines.size() == 1) {
					throw new CompileError("Expecting catch block");
				}

				int last = tryBlock.getStart() + 1;
				Label lastLabel = start;
				for (int i = 1; i < itrEnd; i++) {
					CatchBlock currentBlock = new CatchBlock(last++, lastLabel, null);
					block.addBlock(currentBlock);
					Label theEnd = new Label();
					currentBlock.setEndLabel(theEnd);

					firstLine = Strings.normalizeSpaces(lines.get(i)[0]);
					condition = firstLine.substring(0, firstLine.lastIndexOf('{')).trim();
					Line[] newLines = Strings.before(Strings.after(lines.get(i), 1), 1);

					String parse = condition.substring(5).trim().toString();

					DefinitiveType type;
					String varName = null;
					if (parse.contains(" -> ")) {
						String[] spl = parse.split(" -> ", 2);
						type = data.resolveClass(spl[0].trim());
						varName = spl[1].trim();
					} else {
						type = data.resolveClass(parse);
					}

					try {
						if (!type.isObject() || !type.getObjectType().is("java.lang.Throwable")) {
							throw new CompileError("Catch type must be an exception");
						}
					} catch (ClassNotFoundException e) {
						throw new CompileError(e);
					}

					if (varName != null) {
						m.visitLocalVariable(varName, type.getAbsoluteTypeSignature(), null,
								currentBlock.getStartLabel(), currentBlock.getEndLabel(),
								this.data.getLocalVariables() + 1);
						this.data.addLocal(
								new LocalData(varName, type, currentBlock, this.data.getLocalVariables(), ACC_FINAL));
					}

					// m.visitTryCatchBlock(tryBlock.getStartLabel(), endLabel,
					// handler, type);

					new GenericBodyCompiler(this.data).compile(data, m, block, newLines);
					m.visitFrame(F_SAME, this.data.getLocalVariables(), null, this.data.getCurrentStack(), null);
					m.visitJumpInsn(GOTO, endLabel);

					m.visitLabel(theEnd);
					lastLabel = theEnd;
				}

				m.visitLabel(endLabel);
			} else if (condition.startsWith("while ")) {
				Block currentBlock = new Block(block.getStart() + 1, start, null);
				block.addBlock(currentBlock);

				Label outOfLoop = new Label();
				currentBlock.setEndLabel(outOfLoop);

				Line parse = condition.substring(5).trim();
				Line[] newLines = Strings.before(Strings.after(lines.get(0), 1), 1);

				Label afterGoto = new Label();
				Label after = new Label();

				m.visitFrame(F_SAME, this.data.getLocalVariables(), null, this.data.getCurrentStack(), null);
				m.visitJumpInsn(GOTO, after);

				m.visitLabel(afterGoto);
				new GenericBodyCompiler(this.data).compile(data, m, currentBlock, newLines);
				m.visitLabel(after);
				new BooleanExpressionCompiler(this.data, outOfLoop, true).compile(data, m, currentBlock,
						new Line[] { parse });

				m.visitFrame(F_SAME, this.data.getLocalVariables(), null, this.data.getCurrentStack(), null);
				m.visitJumpInsn(GOTO, afterGoto);
				m.visitLabel(outOfLoop);
			} else if (condition.startsWith("for ")) {
				Block currentBlock = new Block(block.getStart() + 1, start, null);
				block.addBlock(currentBlock);

				Label outOfLoop = new Label();
				currentBlock.setEndLabel(outOfLoop);

				Line parse = condition.substring(4).trim();
				Line[] newLines = Strings.before(Strings.after(lines.get(0), 1), 1);

				Label afterGoto = new Label();
				Label after = new Label();

				Line[] spl = parse.split(";");
				if (spl.length != 3) {
					throw new CompileError("For-loop format should be 'declaration; condition; modification;'");
				}
				Line declaration = spl[0].trim();
				Line conditionBool = spl[1].trim();
				Line increment = spl[2].trim();

				new GenericStatementCompiler(this.data).compile(data, m, currentBlock, new Line[] { declaration });

				m.visitFrame(F_SAME, this.data.getLocalVariables(), null, this.data.getCurrentStack(), null);
				m.visitJumpInsn(GOTO, after);

				m.visitLabel(afterGoto);

				new GenericBodyCompiler(this.data).compile(data, m, currentBlock, newLines);
				new MathExpressionCompiler(this.data, false, true).compile(data, m, currentBlock,
						new Line[] { increment });

				m.visitLabel(after);
				new BooleanExpressionCompiler(this.data, outOfLoop, true).compile(data, m, currentBlock,
						new Line[] { conditionBool });

				m.visitFrame(F_SAME, this.data.getLocalVariables(), null, this.data.getCurrentStack(), null);
				m.visitJumpInsn(GOTO, afterGoto);
				m.visitLabel(outOfLoop);
			} else if (condition.startsWith("foreach ")) {
				Block currentBlock = new Block(block.getStart() + 1, start, null);
				block.addBlock(currentBlock);

				Label outOfLoop = new Label();
				currentBlock.setEndLabel(outOfLoop);

				Line parse = condition.substring(8).trim();
				Line[] newLines = Strings.before(Strings.after(lines.get(0), 1), 1);

				Line[] parseSplit = Strings.split(parse, " in ");
				if (parseSplit.length != 2) {
					throw new CompileError("Foreach-loop format should be 'variable in iterator/iterable'");
				}

				Line var = parseSplit[0].trim();
				Line itr = parseSplit[1].trim();

				ExpressionCompiler exp = new ExpressionCompiler(false, this.data);
				exp.compile(data, m, currentBlock, new Line[] { itr });

				String x = "Ljava/lang/Object;";
				if (exp.getResultType().equals("Lcornflakes/lang/I32Range;")) {
					x = "I";
				} else if (exp.getResultType().equals("Lcornflakes/lang/F32Range;")) {
					x = "F";
				}

				int idx = this.data.getLocalVariables();
				LocalData objData = new LocalData(var.getLine(), DefinitiveType.assume(x), currentBlock, idx, 0);
				this.data.addLocal(objData);

				String y = objData.getType().getAbsoluteTypeSignature();
				m.visitLocalVariable(objData.getName(), y, null, start, currentBlock.getEndLabel(), idx);
				this.data.addLocalVariable();

				int itrIdx = 16384 + this.data.getSyntheticVariables();
				LocalData itrData = new LocalData("_itr_" + itrIdx, DefinitiveType.assume("Ljava/lang/Iterator;"),
						currentBlock, itrIdx, ACC_FINAL);
				this.data.addLocal(itrData);
				m.visitLocalVariable(itrData.getName(), itrData.getType().getAbsoluteTypeSignature(), null, start,
						currentBlock.getEndLabel(), itrIdx);
				this.data.addSyntheticVariable();

				try {
					DefinitiveType type = exp.getResultType();
					if (type.isPrimitive()) {
						throw new CompileError("Cannot for-each over a primitive type");
					}

					if (type.getObjectType().getClassName().startsWith("[")) {
						m.visitTypeInsn(NEW, "cornflakes/lang/ArrayIterator");
						m.visitInsn(DUP);

						exp.setWrite(true);
						exp.compile(data, m, currentBlock, new Line[] { itr });

						m.visitMethodInsn(INVOKESPECIAL, "cornflakes/lang/ArrayIterator", "<init>",
								"([Ljava/lang/Object;)V", false);
					} else if (type.getObjectType().is("java.util.Iterator")) {
						exp.setWrite(true);
						exp.compile(data, m, currentBlock, new Line[] { itr });
					} else if (type.getObjectType().is("java.lang.Iterable") || type.isTuple()) {
						exp.setWrite(true);
						exp.compile(data, m, currentBlock, new Line[] { itr });

						m.visitMethodInsn(INVOKEINTERFACE, "java/lang/Iterable", "iterator", "()Ljava/util/Iterator;",
								true);
					} else {
						throw new CompileError(
								"Cannot for-each over the given object; should be an instance of java.util.Iterator or java.util.Iterable");
					}
				} catch (ClassNotFoundException e) {
					throw new CompileError(e);
				}
				m.visitVarInsn(ASTORE, itrIdx);

				Label afterGoto = new Label();
				Label after = new Label();

				m.visitFrame(F_SAME, this.data.getLocalVariables(), null, this.data.getCurrentStack(), null);
				m.visitJumpInsn(GOTO, after);

				m.visitLabel(afterGoto);
				m.visitVarInsn(ALOAD, itrIdx);
				m.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);
				this.data.ics();
				if (y.equals("I")) {
					m.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
				}
				m.visitVarInsn(Types.getOpcode(Types.STORE, y), idx);

				new GenericBodyCompiler(this.data).compile(data, m, currentBlock, newLines);
				m.visitLabel(after);
				m.visitVarInsn(ALOAD, itrIdx);
				m.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);
				this.data.ics();

				m.visitFrame(F_SAME, this.data.getLocalVariables(), null, this.data.getCurrentStack(), null);
				m.visitJumpInsn(IFEQ, outOfLoop);

				m.visitFrame(F_SAME, this.data.getLocalVariables(), null, this.data.getCurrentStack(), null);
				m.visitJumpInsn(GOTO, afterGoto);
				m.visitLabel(outOfLoop);
			} else {
				throw new CompileError("Unresolved block condition: " + condition);
			}
		}
	}
}
