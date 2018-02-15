package cornflakes.compiler;

import static cornflakes.compiler.Operator.*;

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
	public void compile(ClassData data, MethodVisitor m, Block block, Line[] lines) {
		Line body = lines[0];
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
						String type = pushToStack(body, body.substring(0, start).getLine(), data, m, block)
								.getTypeSignature();
						if (this.write) {
							m.visitInsn(DUP);
						}

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
								if (ref.getField().hasModifier(ACC_FINAL)) {
									throw new CompileError("Cannot assign a value to a const after initialization");
								}
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

		Line[] split = null;
		if (Strings.contains(body, "&")) {
			split = Strings.split(body, "&", 2);
			type = BITWISE_AND;
		} else if (Strings.contains(body, "+")) {
			split = Strings.split(body, "+", 2);
			type = ADD;
		} else if (Strings.contains(body, "- ")) {
			split = Strings.split(body, "- ", 2);
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
			type = BITWISE_OR;
		} else if (Strings.contains(body, "%")) {
			split = Strings.split(body, "%", 2);
			type = MOD;
		} else if (Strings.contains(body, ">>>")) {
			split = Strings.split(body, ">>>", 2);
			type = RIGHT_LOGICAL_SHIFT;
		} else if (Strings.contains(body, "<<")) {
			split = Strings.split(body, "<<", 2);
			type = LEFT_SHIFT;
		} else if (Strings.contains(body, ">>")) {
			split = Strings.split(body, ">>", 2);
			type = RIGHT_SHIFT;
		} else {
			invalid(new CompileError("Expecting mathematical operator"));
			return;
		}

		Line left = split[0].trim();
		Line right = split[1].trim();

		DefinitiveType leftType = pushToStack(left, left.getLine(), data, m, block);
		DefinitiveType rightType = pushToStack(right, right.getLine(), data, m, block);
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

		if (leftType.equals("Ljava/lang/String;") || rightType.equals("Ljava/lang/String;")) {
			if (write) {
				this.data.ics();
				m.visitMethodInsn(INVOKESTATIC, "cornflakes/lang/StringUtility", "combine",
						"(" + leftType + rightType + ")Ljava/lang/String;", false);
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
			} else if (type == MOD) {
				if (isDouble) {
					op = DREM;
				} else if (isFloat) {
					op = FREM;
				} else if (isLong) {
					op = LREM;
				} else if (isInt) {
					op = IREM;
				}
				if (this.write)
					this.data.dcs();
			} else if (type == LEFT_SHIFT) {
				op = isLong ? LSHL : ISHL;
			} else if (type == RIGHT_SHIFT) {
				op = isLong ? LSHR : ISHR;
			} else if (type == RIGHT_LOGICAL_SHIFT) {
				op = isLong ? LUSHR : IUSHR;
			} else if (type == BITWISE_AND) {
				op = isLong ? LAND : IAND;
			} else if (type == BITWISE_OR) {
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
							Operator.getOperatorOverloadName(this.type),
							"(Ljava/math/BigInteger;)Ljava/math/BigInteger;", false);
				}
			} else if (type.getClassName().equals("java/math/BigDecimal")) {
				if (write) {
					m.visitMethodInsn(INVOKEVIRTUAL, "java/math/BigDecimal",
							Operator.getOperatorOverloadName(this.type),
							"(Ljava/math/BigDecimal;)Ljava/math/BigDecimal;", false);
				}
			} else {
				try {
					if (!type.hasOperatorOverload(this.type)) {
						invalid(new CompileError(
								"Operator not overloaded for class " + Types.beautify(type.getClassName())));
					}

					MethodData[] overloads = type.getOperatorOverloads(this.type);
					boolean wrote = true;
					for (MethodData overload : overloads) {
						if (!Types.isSuitable(overload.getParameters().get(1).getType(), rightType)) {
							continue;
						}
						wrote = true;
						if (write) {
							m.visitMethodInsn(INVOKESTATIC, type.getClassName(), overload.getName(),
									overload.getSignature(), false);
						}
					}
					if (!wrote) {
						invalid(new CompileError(
								"No overload accepts parameter of type " + Types.beautify(rightType.getTypeName())));
					}
				} catch (ClassNotFoundException e) {
					throw new CompileError(e);
				}
			}

			resultType = leftType;
		}
	}

	private DefinitiveType pushToStack(Line line, String term, ClassData data, MethodVisitor m, Block thisBlock) {
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

			ref.compile(data, m, thisBlock, new Line[] { line.derive(term) });

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
