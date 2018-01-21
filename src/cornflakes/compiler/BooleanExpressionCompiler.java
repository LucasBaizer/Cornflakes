package cornflakes.compiler;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class BooleanExpressionCompiler implements GenericCompiler {
	private static final int EQUAL = 0;
	private static final int NOT_EQUAL = 1;
	private static final int GREATER_THAN = 2;
	private static final int LESS_THAN = 3;
	private static final int GREATER_THAN_OR_EQUAL = 4;
	private static final int LESS_THAN_OR_EQUAL = 5;
	private static final int IS = 6;
	private static final int AND = 7;
	private static final int OR = 8;

	private int ifType = -1;
	private MethodData data;
	private Label end;
	private boolean write;
	private boolean valid = true;

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
		if (Strings.contains(body, "==")) {
			split = Strings.split(body, "==");
			ifType = EQUAL;
		} else if (Strings.contains(body, "!=")) {
			split = Strings.split(body, "!=");
			ifType = NOT_EQUAL;
		} else if (Strings.contains(body, ">")) {
			split = Strings.split(body, ">");
			ifType = GREATER_THAN;
		} else if (Strings.contains(body, "<")) {
			split = Strings.split(body, "<");
			ifType = LESS_THAN;
		} else if (Strings.contains(body, ">=")) {
			split = Strings.split(body, ">=");
			ifType = GREATER_THAN_OR_EQUAL;
		} else if (Strings.contains(body, "<=")) {
			split = Strings.split(body, "<=");
			ifType = LESS_THAN_OR_EQUAL;
		} else if (Strings.contains(body, "is")) {
			split = Strings.split(body, "is");
			ifType = IS;
		} else if (Strings.contains(body, "and")) {
			split = Strings.split(body, "and");
			ifType = AND;
		} else if (Strings.contains(body, "or")) {
			split = Strings.split(body, "or");
			ifType = OR;
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

				if (ref.getReferenceType() == null || !ref.getReferenceType().equals("Z")) {
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

			String leftType = pushToStack(left, data, m, block);
			String rightType = null;
			if (ifType != IS) {
				rightType = pushToStack(right, data, m, block);
			}

			if (Types.isNumeric(leftType) && Types.isNumeric(rightType)) {
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
						m.visitTypeInsn(INSTANCEOF, data.resolveClass(right));
						m.visitJumpInsn(IFEQ, end);
					}
				} else {
					if (write) {
						m.visitJumpInsn(op, end);
					}
				}
			}
		}
	}

	private String pushToStack(String term, ClassData data, MethodVisitor m, Block thisBlock) {
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

			return type;
		} else {
			ExpressionCompiler ref = new ExpressionCompiler(this.write, this.data);
			ref.compile(data, m, thisBlock, term, new String[] { term });

			return ref.getReferenceType().getTypeSignature();
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
}
