package cornflakes.compiler;

import org.objectweb.asm.MethodVisitor;

public class CompileUtils {
	public static class VariableDeclaration {
		private String variableType;
		private Object value;
		private String valueType;
		private String rawValue;
		private boolean isReference;

		public VariableDeclaration(String type, Object val, String valType, String raw, boolean ref) {
			this.variableType = type;
			this.value = val;
			this.valueType = valType;
			this.isReference = ref;
			this.rawValue = raw;
		}

		public String getVariableType() {
			return variableType;
		}

		public void setVariableType(String variableType) {
			this.variableType = variableType;
		}

		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			this.value = value;
		}

		public String getValueType() {
			return valueType;
		}

		public void setValueType(String valueType) {
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
	}

	public static VariableDeclaration declareVariable(MethodData methodData, ClassData data, MethodVisitor m,
			Block block, String body, String[] split) {
		String variableType = null;
		Object value = null;
		String valueType = null;
		String raw = null;
		boolean isRef = false;
		boolean isMember = m == null || block == null;

		if (split.length == 1) {
			if (isMember) {
				throw new CompileError("A type for member variables can not be assumed; one must be assigned");
			}

			String[] set = body.split("=");
			if (set.length == 1) {
				throw new CompileError("A variable with an unspecified type must have an initial value");
			}

			String givenValue = set[1].trim();
			raw = givenValue;
			valueType = Types.getType(givenValue, "");
			if (valueType == null) {
				ReferenceCompiler ref = new ReferenceCompiler(true, methodData);
				ref.compile(data, m, block, givenValue, new String[] { givenValue });

				if ((valueType = ref.getReferenceSignature()) == null) {
					throw new CompileError("A type for the variable could not be assumed; one must be assigned");
				}

				isRef = !ref.isPrimitiveReference();
			} else {
				value = Types.parseLiteral(valueType, givenValue);
			}

			variableType = Types.getTypeSignature(valueType);
		} else {
			String[] spaces = split[1].trim().split(" ");
			variableType = spaces[0];

			if (!Types.isPrimitive(variableType)) {
				variableType = data.resolveClass(variableType);
			}

			String[] set = body.split("=");
			if (set.length > 1) {
				String givenValue = set[1].trim();

				raw = givenValue;
				valueType = Types.getType(givenValue, variableType);

				if (!isMember) {
					if (valueType == null) {
						ReferenceCompiler compiler = new ReferenceCompiler(true, methodData);
						compiler.compile(data, m, block, givenValue, new String[] { givenValue });
						valueType = compiler.getReferenceSignature();
						isRef = !compiler.isPrimitiveReference();
					}
				}

				if (valueType != null) {
					if (!Types.isSuitable(Types.getTypeSignature(variableType), Types.getTypeSignature(valueType))) {
						throw new CompileError(valueType + " is not assignable to " + variableType);
					}
				}

				if (!isRef && valueType != null) {
					value = Types.parseLiteral(valueType, givenValue);
				}

				if (isMember && value == null) {
					value = givenValue;
				}
			}

			variableType = Types.getTypeSignature(variableType);
		}

		return new VariableDeclaration(variableType, value, valueType, raw, isRef);
	}
}
