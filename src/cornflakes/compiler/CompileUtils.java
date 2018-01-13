package cornflakes.compiler;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.MethodVisitor;

public class CompileUtils {
	public static class VariableDeclaration {
		private String variableType;
		private Object value;
		private String valueType;
		private String rawValue;
		private boolean isReference;
		private List<GenericType> genericTypes;

		public VariableDeclaration(String type, Object val, String valType, String raw, List<GenericType> generics,
				boolean ref) {
			this.variableType = type;
			this.value = val;
			this.genericTypes = generics;
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
			Block block, String body, String[] split) {
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
				ref.compile(data, m, block, givenValue, new String[] { givenValue });

				if ((valueType = ref.getReferenceSignature()) == null) {
					throw new CompileError("A type for the variable could not be assumed; one must be assigned");
				}

				generics = ref.getGenericTypes();
				isRef = !ref.isPrimitiveReference();
			} else {
				value = Types.parseLiteral(valueType, givenValue);
			}

			variableType = Types.getTypeSignature(valueType);
		} else {
			String[] spaces = split[1].trim().split(" ");
			variableType = spaces[0];

			if (!Types.isPrimitive(variableType)) {
				variableType = Types.padSignature(data.resolveClass(variableType));
			} else {
				variableType = Types.getTypeSignature(variableType);
			}

			String[] set = body.split("=", 2);
			if (set.length > 1) {
				String givenValue = set[1].trim();

				raw = givenValue;
				valueType = Types.getType(givenValue, variableType);
				boolean math = false;

				if (!isMember && valueType == null) {
					ExpressionCompiler compiler = new ExpressionCompiler(true, methodData);
					compiler.compile(data, m, block, givenValue, new String[] { givenValue });
					valueType = compiler.getReferenceSignature();
					math = compiler.isMath();
					generics = compiler.getGenericTypes();
					isRef = !compiler.isPrimitiveReference();
				}

				if (valueType != null) {
					if (!Types.isSuitable(variableType, Types.getTypeSignature(valueType))) {
						throw new CompileError(valueType + " is not assignable to " + variableType);
					}
				}

				if (!isRef && !math && valueType != null) {
					value = Types.parseLiteral(valueType, givenValue);
				}

				if (isMember && value == null) {
					value = givenValue;
				}
			}
		}

		return new VariableDeclaration(variableType, value, valueType, raw, generics, isRef);
	}
}
