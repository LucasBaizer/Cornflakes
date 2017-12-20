package cornflakes.compiler;

import org.objectweb.asm.MethodVisitor;

public class MathExpressionCompiler implements GenericCompiler {
	private static final int AND = 0;
	private static final int ADD = 1;
	private static final int SUBTRACT = 2;
	private static final int MULTIPLY = 3;
	private static final int DIVIDE = 4;
	private static final int XOR = 5;
	private static final int OR = 6;

	private int type = -1;
	private MethodData data;
	private String resultType;
	private boolean write;
	private boolean valid = true;
	private boolean bool;

	public MathExpressionCompiler(MethodData data, boolean b, boolean val) {
		this.data = data;
		this.bool = b;
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
		if (Strings.contains(body, "&")) {
			split = Strings.split(body, "&");
			type = AND;
		} else if (Strings.contains(body, "+")) {
			split = Strings.split(body, "+");
			type = ADD;
		} else if (Strings.contains(body, "-")) {
			split = Strings.split(body, "-");
			type = SUBTRACT;
		} else if (Strings.contains(body, "/")) {
			split = Strings.split(body, "/");
			type = DIVIDE;
		} else if (Strings.contains(body, "*")) {
			split = Strings.split(body, "*");
			type = MULTIPLY;
		} else if (Strings.contains(body, "^")) {
			split = Strings.split(body, "^");
			type = XOR;
		} else if (Strings.contains(body, "|")) {
			split = Strings.split(body, "|");
			type = OR;
		} else {
			invalid(new CompileError("Expecting mathematical operator"));
			return;
		}

		String left = split[0].trim();
		String right = split[1].trim();

		String leftType = pushToStack(left, data, m, block);
		String rightType = pushToStack(right, data, m, block);
		boolean isLong = leftType.equals("J") || rightType.equals("J");
		boolean isInt = leftType.equals("I") || rightType.equals("I");
		boolean isFloat = leftType.equals("F") || rightType.equals("F");
		boolean isDouble = leftType.equals("D") || rightType.equals("D");
		if (isLong) {
			resultType = "J";
		} else if (isInt) {
			resultType = "I";
		} else if (isFloat) {
			resultType = "F";
		} else if (isDouble) {
			resultType = "D";
		}

		if (Types.isNumeric(leftType) && Types.isNumeric(rightType)) {
			int op = 0;
			if (type == AND) {
				op = isLong ? LAND : IAND;
			} else if (type == ADD) {
				if (isDouble) {
					op = DADD;
				} else if (isFloat) {
					op = FADD;
				} else if (isLong) {
					op = LADD;
				} else if (isInt) {
					op = IADD;
				}
			} else if (type == SUBTRACT) {
				if (isDouble) {
					op = DSUB;
				} else if (isFloat) {
					op = FSUB;
				} else if (isLong) {
					op = LSUB;
				} else if (isInt) {
					op = ISUB;
				}
			} else if (type == MULTIPLY) {
				if (isDouble) {
					op = DMUL;
				} else if (isFloat) {
					op = FMUL;
				} else if (isLong) {
					op = LMUL;
				} else if (isInt) {
					op = IMUL;
				}
			} else if (type == DIVIDE) {
				if (isDouble) {
					op = DDIV;
				} else if (isFloat) {
					op = FDIV;
				} else if (isLong) {
					op = LDIV;
				} else if (isInt) {
					op = IDIV;
				}
			} else if (type == AND) {
				op = isLong ? LAND : IAND;
			} else if (type == OR) {
				op = isLong ? LOR : IOR;
			} else if (type == XOR) {
				op = isLong ? LXOR : IXOR;
			}
			if (this.write) {
				m.visitInsn(op);
			}
		} else {
			invalid(new CompileError("Types must be numeric"));
		}
	}

	private String pushToStack(String term, ClassData data, MethodVisitor m, Block thisBlock) {
		String type = Types.getType(term, "");
		if (type != null) {
			int oc = Types.getOpcode(Types.PUSH, type);
			Object lit = Types.parseLiteral(type, term);
			if (oc == LDC) {
				if (this.write) {
					m.visitLdcInsn(lit);
				}
			} else {
				if (this.write) {
					String toString = lit.toString();
					m.visitVarInsn(oc, Integer.parseInt(toString));
				}
			}

			if (this.write) {
				this.data.increaseStackSize();
			}

			return Types.getTypeSignature(type);
		} else {
			ReferenceCompiler ref = new ReferenceCompiler(this.write, this.data);
			ref.setAllowMath(false);
			ref.setAllowBoolean(this.bool);
			ref.compile(data, m, thisBlock, term, new String[] { term });

			return ref.getReferenceSignature();
		}
	}

	public boolean isValid() {
		return valid;
	}

	public void setWrite(boolean write) {
		this.write = write;
	}

	public String getResultType() {
		return resultType;
	}

	public void setResultType(String resultType) {
		this.resultType = resultType;
	}
}
