package cornflakes.compiler;

import static cornflakes.compiler.MathOperator.*;

import org.objectweb.asm.MethodVisitor;

public class MathExpressionCompiler implements GenericCompiler {
	private int type = -1;
	private MethodData data;
	private DefinitiveType resultType;
	private boolean write;
	private boolean valid = true;
	private boolean bool;
	private ExpressionCompiler ref;

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
		if (body.length() >= 3) {
			int start = -1;
			for (int x = 0; x < body.length(); x++) {
				if (!Character.isLetterOrDigit(body.charAt(x))) {
					start = x;
					break;
				}
			}
			if (start != -1) {
				char first = body.charAt(start);
				if (first == '+' || first == '-') {
					if (body.charAt(start + 1) == first) {
						String type = pushToStack(body.substring(0, start), data, m, block).getTypeSignature();

						boolean isLong = type.equals("J");
						boolean isInt = type.equals("I");
						boolean isFloat = type.equals("F");
						boolean isDouble = type.equals("D");

						if (isLong) {
							resultType = DefinitiveType.primitive("J");
						} else if (isInt) {
							resultType = DefinitiveType.primitive("I");
						} else if (isFloat) {
							resultType = DefinitiveType.primitive("F");
						} else if (isDouble) {
							resultType = DefinitiveType.primitive("D");
						} else {
							invalid(new CompileError("Expecting numerical variable"));
						}

						if (this.write) {
							int wop = Types.getOpcode(Types.PUSH, type);
							if (wop == LDC) {
								m.visitLdcInsn(Types.parseLiteral(type, "1"));
							} else {
								m.visitVarInsn(wop, 1);
							}
							if (this.write) {
								this.data.ics();
							}

							int op = 0;
							if (first == '+') {
								if (isDouble) {
									op = DADD;
								} else if (isFloat) {
									op = FADD;
								} else if (isLong) {
									op = LADD;
								} else if (isInt) {
									op = IADD;
								}
								if (this.write)
									this.data.dcs();
							} else {
								if (isDouble) {
									op = DSUB;
								} else if (isFloat) {
									op = FSUB;
								} else if (isLong) {
									op = LSUB;
								} else if (isInt) {
									op = ISUB;
								}
								if (this.write)
									this.data.dcs();
							}
							m.visitInsn(op);

							if (ref != null && ref.getField() != null) {
								if (ref.getField() instanceof LocalData) {
									LocalData local = (LocalData) ref.getField();
									m.visitVarInsn(Types.getOpcode(Types.STORE, type), local.getIndex());
								} else {
									m.visitFieldInsn(ref.getField().hasModifier(ACC_STATIC) ? PUTSTATIC : PUTFIELD,
											ref.getResultOwner().getClassName(), ref.getResultName(),
											ref.getResultType().getTypeSignature());
								}
							}
						}
						return;
					}
				}
			}
		}

		String[] split = null;
		if (Strings.contains(body, "&")) {
			split = Strings.split(body, "&", 2);
			type = AND;
		} else if (Strings.contains(body, "+")) {
			split = Strings.split(body, "+", 2);
			type = ADD;
		} else if (Strings.contains(body, "-")) {
			split = Strings.split(body, "-", 2);
			type = SUBTRACT;
		} else if (Strings.contains(body, "/")) {
			split = Strings.split(body, "/", 2);
			type = DIVIDE;
		} else if (Strings.contains(body, "*")) {
			split = Strings.split(body, "*", 2);
			type = MULTIPLY;
		} else if (Strings.contains(body, "^")) {
			split = Strings.split(body, "^", 2);
			type = XOR;
		} else if (Strings.contains(body, "|")) {
			split = Strings.split(body, "|", 2);
			type = OR;
		} else {
			invalid(new CompileError("Expecting mathematical operator"));
			return;
		}

		String left = split[0].trim();
		String right = split[1].trim();

		DefinitiveType leftType = pushToStack(left, data, m, block);
		DefinitiveType rightType = pushToStack(right, data, m, block);
		boolean isLong = leftType.equals("J") || rightType.equals("J");
		boolean isInt = leftType.equals("I") || rightType.equals("I");
		boolean isFloat = leftType.equals("F") || rightType.equals("F");
		boolean isDouble = leftType.equals("D") || rightType.equals("D");
		if (isLong) {
			resultType = DefinitiveType.primitive("J");
		} else if (isInt) {
			resultType = DefinitiveType.primitive("I");
		} else if (isFloat) {
			resultType = DefinitiveType.primitive("F");
		} else if (isDouble) {
			resultType = DefinitiveType.primitive("D");
		}

		if (leftType.equals("Ljava/lang/String;") && rightType.equals("Ljava/lang/String;")) {
			if (write) {
				this.data.ics();
				m.visitMethodInsn(INVOKESTATIC, "cornflakes/lang/StringUtility", "combine",
						"(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", false);
			}
			resultType = DefinitiveType.object("Ljava/lang/String;");
			return;
		}

		if (leftType.isPrimitive() && rightType.isPrimitive()) {
			int op = 0;
			if (type == ADD) {
				if (isDouble) {
					op = DADD;
				} else if (isFloat) {
					op = FADD;
				} else if (isLong) {
					op = LADD;
				} else if (isInt) {
					op = IADD;
				}
				if (this.write)
					this.data.dcs();
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
				if (this.write)
					this.data.dcs();
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
				if (this.write)
					this.data.dcs();
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
				if (this.write)
					this.data.dcs();
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
			ClassData type = leftType.getObjectType();
			if (type.getClassName().equals("java/math/BigInteger")) {
				if (write) {
					m.visitMethodInsn(INVOKEVIRTUAL, "java/math/BigInteger",
							MathOperator.getOperatorOverloadName(this.type),
							"(Ljava/math/BigInteger;)Ljava/math/BigInteger;", false);
				}
			} else if (type.getClassName().equals("java/math/BigDecimal")) {
				if (write) {
					m.visitMethodInsn(INVOKEVIRTUAL, "java/math/BigDecimal",
							MathOperator.getOperatorOverloadName(this.type),
							"(Ljava/math/BigDecimal;)Ljava/math/BigDecimal;", false);
				}
			} else {
				try {
					if (!type.hasOperatorOverload(this.type)) {
						invalid(new CompileError(
								"Operator not overloaded for class " + Types.beautify(type.getClassName())));
					}

					MethodData overload = type.getOperatorOverload(this.type);
					if (!Types.isSuitable(overload.getParameters().get(1).getType(), rightType)) {
						invalid(new CompileError("Overload does not accept parameter of type "
								+ Types.beautify(rightType.getTypeName())));
					}
					if (write) {
						m.visitMethodInsn(INVOKESTATIC, type.getClassName(), overload.getName(),
								overload.getSignature(), false);
					}
				} catch (ClassNotFoundException e) {
					throw new CompileError(e);
				}
			}

			resultType = leftType;
		}
	}

	private DefinitiveType pushToStack(String term, ClassData data, MethodVisitor m, Block thisBlock) {
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
				this.data.ics();
			}

			return DefinitiveType.assume(Types.getTypeSignature(type));
		} else {
			ref = new ExpressionCompiler(this.write, this.data);
			ref.setAllowMath(false);
			ref.setAllowBoolean(this.bool);
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

	public DefinitiveType getResultType() {
		return resultType;
	}

	public void setResultType(DefinitiveType resultType) {
		this.resultType = resultType;
	}
}
