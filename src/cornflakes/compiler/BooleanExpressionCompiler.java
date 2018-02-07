package cornflakes.compiler;

import static cornflakes.compiler.Operator.*;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class BooleanExpressionCompiler implements GenericCompiler {
	private static final int IS = 15;

	private int ifType = -1;
	private MethodData data;
	private Label end;
	private boolean write;
	private boolean valid = true;
	private boolean requireBranch = true;

	public BooleanExpressionCompiler(MethodData data, Label end, boolean val) {
		this.data = data;
		this.end = end;
		this.write = val;
	}

	private void invalid(RuntimeException thr) {
		if (write) {
			throw thr;
		}
		valid = false;
	}

	@Override
	public void compile(ClassData data, MethodVisitor m, Block block, String body, String[] lines) {
		String[] split = null;

		if (Strings.contains(body, "&&")) {
			split = Strings.split(body, "&&");
			ifType = AND;
		} else if (Strings.contains(body, "||")) {
			split = Strings.split(body, "||");
			ifType = OR;
		} else if (Strings.contains(body, "==")) {
			split = Strings.split(body, "==");
			ifType = EQUAL;
		} else if (Strings.contains(body, "!=")) {
			split = Strings.split(body, "!=");
			ifType = NOT_EQUAL;
		} else if (Strings.contains(body, ">=")) {
			split = Strings.split(body, ">=");
			ifType = GREATER_THAN_OR_EQUAL;
		} else if (Strings.contains(body, "<=")) {
			split = Strings.split(body, "<=");
			ifType = LESS_THAN_OR_EQUAL;
		} else if (Strings.contains(body, ">")) {
			split = Strings.split(body, ">");
			ifType = GREATER_THAN;
		} else if (Strings.contains(body, "<")) {
			split = Strings.split(body, "<");
			ifType = LESS_THAN;
		} else if (Strings.contains(body, "is")) {
			split = Strings.split(body, "is");
			ifType = IS;
		}

		String left = null;
		String right = null;

		if (split == null) {
			String bool = body;

			String type = Types.getType(bool, "");
			if (type != null) {
				if (type.equals("bool")) {
					if (this.write) {
						m.visitInsn(bool.equals("false") ? ICONST_0 : ICONST_1);
						this.data.ics();
					}
				} else {
					invalid(new CompileError("Expecting type 'bool'"));
				}
			} else {
				ExpressionCompiler ref = new ExpressionCompiler(this.write, this.data);
				ref.setAllowBoolean(false);
				ref.setAllowMath(false);
				ref.setSource(this);
				ref.compile(data, m, block, bool, new String[] { bool });

				if (ref.getResultType() == null || !ref.getResultType().equals("Z")) {
					invalid(new CompileError("The given reference is not a boolean"));
				}
			}

			if (this.write) {
				m.visitFrame(F_SAME, this.data.getLocalVariables(), null, this.data.getCurrentStack(), null);
				m.visitJumpInsn(IFEQ, end);
			}
		} else {
			left = split[0].trim();
			right = split[1].trim();

			DefinitiveType leftType = pushToStack(left, data, m, block);
			DefinitiveType rightType = null;
			if (ifType != IS) {
				rightType = pushToStack(right, data, m, block);
			}

			if (leftType.isPrimitive() && rightType.isPrimitive()) {
				int op = 0;
				int stack = this.data.getCurrentStack();
				if (ifType == EQUAL) {
					op = IF_ICMPNE;
				} else if (ifType == NOT_EQUAL) {
					op = IF_ICMPEQ;
				} else if (ifType == LESS_THAN) {
					op = IF_ICMPGE;
				} else if (ifType == GREATER_THAN) {
					op = IF_ICMPLE;
				} else if (ifType == LESS_THAN_OR_EQUAL) {
					op = IF_ICMPGT;
				} else if (ifType == GREATER_THAN_OR_EQUAL) {
					op = IF_ICMPLT;
				} else if (ifType == AND || ifType == OR) {
					if (!leftType.equals("Z") || !rightType.equals("Z")) {
						invalid(new CompileError("Only booleans can be compared with 'and' / 'or'"));
					}

					op = IFEQ;
					if (this.write) {
						m.visitInsn(ifType == AND ? IAND : IOR);
						stack--;
					}
				} else {
					invalid(new CompileError("Cannot compare " + leftType + " to " + rightType));
				}
				if (this.write) {
					m.visitFrame(F_SAME, this.data.getLocalVariables(), null, stack, null);
					m.visitJumpInsn(op, end);
				}
			} else {
				boolean aleft = Types.isPrimitive(leftType);
				boolean aright = Types.isPrimitive(rightType);

				if (aleft ^ aright) {
					invalid(new CompileError("Cannot compare " + leftType + " to " + rightType));
				}

				if (!aleft) {
					if (leftType.equals("java/lang/String") && rightType.equals("java/lang/String")) {
						if (write) {
							m.visitMethodInsn(INVOKESTATIC, "cornflakes/lang/StringUtility", "equal",
									"(Ljava/lang/String;Ljava/lang/String;)Z", false);
						}
						requireBranch = false;
						return;
					} else {
						try {
							ClassData type = leftType.getObjectType();

							if (type.hasOperatorOverload(this.ifType)) {
								MethodData[] overloads = type.getOperatorOverloads(this.ifType);
								for (MethodData overload : overloads) {
									if (!Types.isSuitable(overload.getParameters().get(1).getType(), rightType)) {
										continue;
									}

									if (write) {
										m.visitMethodInsn(INVOKESTATIC, type.getClassName(), overload.getName(),
												overload.getSignature(), false);
									}
									requireBranch = false;
									return;
								}
							}
						} catch (ClassNotFoundException e) {
							invalid(new CompileError(e));
						}
					}
				}

				int op = 0;
				if (ifType == EQUAL) {
					op = IF_ACMPNE;
				} else if (ifType == NOT_EQUAL) {
					op = IF_ACMPEQ;
				} else if (ifType != IS) {
					invalid(new CompileError("References cannot be compared using the given comparator"));
				}
				if (ifType == IS) {
					if (write) {
						m.visitTypeInsn(INSTANCEOF, data.resolveClass(right).getAbsoluteTypeName());
					}
					requireBranch = false;
				} else {
					if (write) {
						m.visitJumpInsn(op, end);
					}
				}
			}
		}
	}

	private DefinitiveType pushToStack(String term, ClassData data, MethodVisitor m, Block thisBlock) {
		String type = Types.getType(term, "");
		if (type != null) {
			if (type.equals("bool")) {
				if (this.write) {
					m.visitInsn(term.equals("false") ? ICONST_0 : ICONST_1);
					this.data.ics();
				}
			} else {
				int oc = Types.getOpcode(Types.PUSH, type);
				Object lit = Types.parseLiteral(type, term);
				if (oc == LDC) {
					if (this.write) {
						m.visitLdcInsn(lit);
					}
				} else {
					if (this.write) {
						String toString = lit.toString();
						if (toString.equals("true")) {
							m.visitInsn(ICONST_1);
						} else if (toString.equals("false")) {
							m.visitInsn(ICONST_0);
						} else {
							m.visitVarInsn(oc, Integer.parseInt(toString));
						}
					}
				}

				if (this.write) {
					this.data.ics();
				}
			}

			return DefinitiveType.assume(type);
		} else {
			ExpressionCompiler ref = new ExpressionCompiler(this.write, this.data);
			ref.compile(data, m, thisBlock, term, new String[] { term });

			return ref.getResultType();
		}
	}

	public boolean isValid() {
		return valid;
	}

	public void setWrite(boolean write) {
		this.write = write;
	}

	public void setEnd(Label label) {
		this.end = label;
	}

	public boolean isRequireBranch() {
		return requireBranch;
	}

	public void setRequireBranch(boolean requireBranch) {
		this.requireBranch = requireBranch;
	}
}
