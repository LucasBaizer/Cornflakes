package cornflakes.compiler;

public class MathOperator {
	public static final int AND = 0;
	public static final int ADD = 1;
	public static final int SUBTRACT = 2;
	public static final int MULTIPLY = 3;
	public static final int DIVIDE = 4;
	public static final int XOR = 5;
	public static final int OR = 6;

	public static int toOp(String str) {
		switch (str) {
			case "&":
				return AND;
			case "+":
				return ADD;
			case "-":
				return SUBTRACT;
			case "*":
				return MULTIPLY;
			case "/":
				return DIVIDE;
			case "^":
				return XOR;
			case "|":
				return OR;
			default:
				throw new CompileError("Invalid op string");
		}
	}

	public static String toString(int op) {
		switch (op) {
			case AND:
				return "&";
			case ADD:
				return "+";
			case SUBTRACT:
				return "-";
			case MULTIPLY:
				return "*";
			case DIVIDE:
				return "/";
			case XOR:
				return "^";
			case OR:
				return "|";
			default:
				throw new CompileError("Invalid operator ID");
		}
	}

	public static String getOperatorOverloadFunction(int op) {
		String ch = "";
		switch (op) {
			case ADD:
				ch = "_op_add";
				break;
			case SUBTRACT:
				ch = "_op_sub";
				break;
			case MULTIPLY:
				ch = "_op_mul";
				break;
			case DIVIDE:
				ch = "_op_div";
				break;
			case OR:
				ch = "_op_or";
				break;
			case AND:
				ch = "_op_and";
				break;
			case XOR:
				ch = "_op_xor";
				break;
			default:
				throw new CompileError("Invalid operator ID: " + op);
		}

		return ch;
	}

	public static String getOperatorOverloadName(int op) {
		String ch = "";
		switch (op) {
			case ADD:
				ch = "add";
				break;
			case SUBTRACT:
				ch = "subtract";
				break;
			case MULTIPLY:
				ch = "multiply";
				break;
			case DIVIDE:
				ch = "divide";
				break;
			case OR:
				ch = "or";
				break;
			case AND:
				ch = "and";
				break;
			case XOR:
				ch = "xor";
				break;
			default:
				throw new CompileError("Invalid operator ID: " + op);
		}

		return ch;
	}
}
