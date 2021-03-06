package cornflakes.compiler;

import java.util.ArrayList;
import static org.objectweb.asm.Opcodes.*;
import java.util.List;

import org.objectweb.asm.MethodVisitor;

public class CompileUtils {
	public static class VariableDeclaration {
		private DefinitiveType variableType;
		private Object value;
		private DefinitiveType valueType;
		private String rawValue;
		private boolean isReference;
		private List<GenericType> genericTypes;

		public VariableDeclaration(DefinitiveType type, Object val, DefinitiveType valType, String raw,
				List<GenericType> generics, boolean ref) {
			this.variableType = type;
			this.value = val;
			this.genericTypes = generics;
			this.valueType = valType;
			this.isReference = ref;
			this.rawValue = raw;
		}

		public DefinitiveType getVariableType() {
			return variableType;
		}

		public void setVariableType(DefinitiveType variableType) {
			this.variableType = variableType;
		}

		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			this.value = value;
		}

		public DefinitiveType getValueType() {
			return valueType;
		}

		public void setValueType(DefinitiveType valueType) {
			this.valueType = valueType;
		}

		public boolean isReference() {
			return isReference;
		}

		public void setReference(boolean isReference) {
			this.isReference = isReference;
		}

		public String getRawValue() {
			return rawValue;
		}

		public void setRawValue(String rawValue) {
			this.rawValue = rawValue;
		}

		public List<GenericType> getGenericTypes() {
			return genericTypes;
		}

		public void setGenericTypes(List<GenericType> genericTypes) {
			this.genericTypes = genericTypes;
		}

		public boolean isGenericTyped() {
			return this.genericTypes.size() > 0;
		}
	}

	public static VariableDeclaration declareVariable(MethodData methodData, ClassData data, MethodVisitor m,
			Block block, Line line, Line[] split) {
		String body = line.getLine();

		String variableType = null;
		Object value = null;
		String valueType = null;
		String raw = null;
		List<GenericType> generics = new ArrayList<>();
		boolean isRef = false;
		boolean isMember = m == null || block == null;

		if (split.length == 1) {
			if (isMember) {
				throw new CompileError("A type for member variables can not be assumed; one must be assigned");
			}

			String[] set = body.split("=", 2);
			if (set.length == 1) {
				throw new CompileError("A variable with an unspecified type must have an initial value");
			}

			String givenValue = set[1].trim();
			raw = givenValue;
			valueType = Types.getType(givenValue, "");
			if (valueType == null) {
				ExpressionCompiler ref = new ExpressionCompiler(true, methodData);
				ref.compile(data, m, block, new Line[] { line.derive(givenValue) });

				if ((valueType = ref.getResultType().getTypeSignature()) == null) {
					throw new CompileError("A type for the variable could not be assumed; one must be assigned");
				}

				generics = ref.getGenericTypes();
				isRef = !ref.isPrimitiveResult();
			} else {
				value = Types.parseLiteral(valueType, givenValue);
			}

			variableType = Types.getTypeSignature(valueType);
		} else {
			String[] spaces = Line.toString(split[1].trim().split("="));
			variableType = spaces[0].trim();

			if (!Types.isTupleDefinition(variableType)) {
				if (!Types.isPrimitive(variableType)) {
					variableType = data.resolveClass(variableType).getTypeSignature();
				} else {
					variableType = Types.getTypeSignature(variableType);
				}
			}

			String[] set = body.split("=", 2);
			if (set.length > 1) {
				String givenValue = set[1].trim();

				raw = givenValue;
				String rawType = Types.getType(givenValue, variableType);
				if (rawType != null) {
					valueType = Types.getTypeSignature(rawType);
				} else {
					ExpressionCompiler compiler = new ExpressionCompiler(!isMember, methodData);
					compiler.compile(data, m, block, new Line[] { line.derive(givenValue) });
					valueType = compiler.getResultType().getTypeSignature();
				}

				boolean math = false;
				ExpressionCompiler compiler = null;

				if (!isMember && valueType == null) {
					compiler = new ExpressionCompiler(true, methodData);
					compiler.compile(data, m, block, new Line[] { line.derive(givenValue) });
					valueType = compiler.getResultType().getTypeSignature();
					math = compiler.isMath();
					generics = compiler.getGenericTypes();
					isRef = !compiler.isPrimitiveResult();

					if (valueType.equals("null")) {
						value = "null";
					}
				}

				if (valueType != null) {
					if (!Types.isSuitable(variableType, valueType)) {
						throw new CompileError(
								Types.beautify(valueType) + " is not assignable to " + Types.beautify(variableType));
					}
				}

				if (!isRef && !math && valueType != null
						&& (compiler == null || compiler.getExpressionType() != ExpressionCompiler.CAST)
						&& rawType != null) {
					value = Types.parseLiteral(valueType, givenValue);
				}

				if (isMember && value == null) {
					value = givenValue;
				}
			}
		}

		return new VariableDeclaration(DefinitiveType.assume(variableType), value,
				valueType == null ? null : DefinitiveType.assume(valueType), raw, generics, isRef);
	}

	public static DefinitiveType push(String raw, ClassData cdata, MethodVisitor m, Block block, Line line,
			MethodData data) {
		return push(raw, cdata, m, block, line, data, true);
	}

	public static DefinitiveType push(String raw, ClassData cdata, MethodVisitor m, Block block, Line line,
			MethodData data, boolean actual) {
		String type = Types.getType(raw, null);
		if (type != null) {
			Object val = Types.parseLiteral(type, raw);
			int push = Types.getOpcode(Types.PUSH, type);
			if (push == LDC) {
				if (actual)
					m.visitLdcInsn(val);
			} else {
				String toString = val.toString();

				if (toString.equals("true") || toString.equals("false")) {
					if (actual)
						m.visitInsn(toString.equals("false") ? ICONST_0 : ICONST_1);
				} else {
					if (actual)
						m.visitVarInsn(push, Integer.parseInt(val.toString()));
				}
			}
			data.ics();

			return DefinitiveType.assume(type);
		} else {
			ExpressionCompiler exp = new ExpressionCompiler(actual, data);
			exp.compile(cdata, m, block, new Line[] { line.derive(raw) });
			return exp.getResultType();
		}
	}
}
