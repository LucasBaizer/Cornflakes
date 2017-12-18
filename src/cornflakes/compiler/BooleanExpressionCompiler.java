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

	private MethodData data;
	private Label end;

	public BooleanExpressionCompiler(MethodData data, Label end) {
		this.data = data;
		this.end = end;
	}

	@Override
	public void compile(ClassData data, MethodVisitor m, Block block, String body, String[] lines) {
		String[] split = null;
		int ifType = -1;
		if (body.contains("==")) {
			split = body.split("==");
			ifType = EQUAL;
		} else if (body.contains("!=")) {
			split = body.split("!=");
			ifType = NOT_EQUAL;
		} else if (body.contains(">")) {
			split = body.split(">");
			ifType = GREATER_THAN;
		} else if (body.contains("<")) {
			split = body.split("<");
			ifType = LESS_THAN;
		} else if (body.contains(">=")) {
			split = body.split(">=");
			ifType = GREATER_THAN_OR_EQUAL;
		} else if (body.contains("<=")) {
			split = body.split("<=");
			ifType = LESS_THAN_OR_EQUAL;
		}

		String left = null;
		String right = null;

		if (split == null) {
			String bool = body;

			String type = Types.getType(bool, "");
			if (type != null) {
				if (type.equals("bool")) {
					m.visitInsn(bool.equals("false") ? ICONST_0 : ICONST_1);
					this.data.increaseStackSize();
				} else {
					throw new CompileError("Expecting type 'bool'");
				}
			} else {
				ReferenceCompiler ref = new ReferenceCompiler(true, this.data);
				ref.compile(data, m, block, bool, new String[] { bool });
			}

			m.visitJumpInsn(IFEQ, end);
		} else {
			left = split[0].trim();
			right = split[1].trim();

			String leftType = pushToStack(left, data, m, block);
			String rightType = pushToStack(right, data, m, block);

			if (Types.isNumeric(leftType) && Types.isNumeric(rightType)) {
				int op = 0;
				if (ifType == EQUAL) {
					op = IF_ICMPNE;
				} else if (ifType == NOT_EQUAL) {
					op = IF_ICMPEQ;
				} else if (ifType == LESS_THAN) {
					op = IF_ICMPGT;
				} else if (ifType == GREATER_THAN) {
					op = IF_ICMPLT;
				} else if (ifType == LESS_THAN_OR_EQUAL) {
					op = IF_ICMPGE;
				} else if (ifType == GREATER_THAN_OR_EQUAL) {
					op = IF_ICMPLE;
				}

				m.visitJumpInsn(op, end);
			} else {
				boolean aleft = Types.isPrimitive(leftType);
				boolean aright = Types.isPrimitive(rightType);

				if ((aleft && !aright) || (!aleft && aright)) {
					throw new CompileError("Cannot compare " + leftType + " to " + rightType);
				}

				int op = 0;
				if (ifType == EQUAL) {
					op = IF_ACMPNE;
				} else if (ifType == NOT_EQUAL) {
					op = IF_ACMPEQ;
				} else {
					throw new CompileError("References can only be compared with == or !=");
				}
				m.visitJumpInsn(op, end);
			}
		}
	}

	private String pushToStack(String term, ClassData data, MethodVisitor m, Block thisBlock) {
		String type = Types.getType(term, "");
		if (type != null) {
			if (type.equals("bool")) {
				m.visitInsn(term.equals("false") ? ICONST_0 : ICONST_1);
				this.data.increaseStackSize();
			}

			int oc = Types.getOpcode(Types.PUSH, type);
			Object lit = Types.parseLiteral(type, term);
			if (oc == LDC) {
				m.visitLdcInsn(lit);
			} else {
				m.visitVarInsn(oc, Integer.parseInt(lit.toString()));
			}

			return type;
		} else {
			ReferenceCompiler ref = new ReferenceCompiler(true, this.data);
			ref.compile(data, m, thisBlock, term, new String[] { term });

			return ref.getReferenceSignature();
		}
	}
}
