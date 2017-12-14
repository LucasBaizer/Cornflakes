package cornflakes;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedHashMap;
import java.util.Map;

public class MethodData {
	private String name;
	private String returnType;
	private Map<String, String> parameters = new LinkedHashMap<>();
	private Map<String, String> locals = new LinkedHashMap<>();
	private int stackSize;
	private int localVariables;
	private int modifiers;

	public static MethodData fromJavaMethod(Method method) {
		MethodData mData = new MethodData(method.getName(), Types.getTypeSignature(method.getReturnType()),
				method.getModifiers());
		Parameter[] params = method.getParameters();
		for (int i = 0; i < params.length; i++) {
			mData.addParameter(params[i].getName(), Types.getTypeSignature(params[i].getType()));
		}
		return mData;
	}

	public static MethodData fromJavaConstructor(Constructor<?> method) {
		MethodData mData = new MethodData(method.getName(), Types.getTypeSignature(method.getDeclaringClass()),
				method.getModifiers());
		Parameter[] params = method.getParameters();
		for (int i = 0; i < params.length; i++) {
			mData.addParameter(params[i].getName(), Types.getTypeSignature(params[i].getType()));
		}
		return mData;
	}

	public MethodData(String name, String ret, int mods) {
		this.name = name;
		this.returnType = ret;
		this.modifiers = mods;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getReturnTypeSignature() {
		return returnType;
	}

	public void setReturnTypeSignature(String returnType) {
		this.returnType = returnType;
	}

	public ClassData getReturnType() {
		return Types.getTypeFromSignature(Types.unpadSignature(returnType));
	}

	public int getStackSize() {
		return stackSize;
	}

	public void setStackSize(int stackSize) {
		this.stackSize = stackSize;
	}

	public int getLocalVariables() {
		return localVariables;
	}

	public void setLocalVariables(int localVariables) {
		this.localVariables = localVariables;
	}

	public void addLocalVariable() {
		this.localVariables++;
		this.stackSize++;
	}

	public void increaseStackSize() {
		this.stackSize++;
	}

	public boolean hasLocal(String name) {
		return locals.containsKey(name);
	}

	public String getLocalType(String name) {
		return locals.get(name);
	}

	public void addLocal(String name, String val) {
		locals.put(name, val);
	}

	public Map<String, String> getLocals() {
		return locals;
	}

	public void setLocals(Map<String, String> locals) {
		this.locals = new LinkedHashMap<>(locals);
	}

	public void setParameters(Map<String, String> params) {
		this.parameters = new LinkedHashMap<>(params);
	}

	public void addParameter(String name, String type) {
		this.parameters.put(name, type);
	}

	public Map<String, String> getParameters() {
		return this.parameters;
	}

	public int getModifiers() {
		return modifiers;
	}

	public void setModifiers(int modifiers) {
		this.modifiers = modifiers;
	}

	public boolean hasModifier(int mod) {
		return (this.modifiers & mod) == mod;
	}

	public String getSignature() {
		String desc = "(";
		for (String par : parameters.values()) {
			desc += par;
		}
		desc += ")" + returnType;

		return desc;
	}

	@Override
	public boolean equals(Object obj) {
		if ((obj instanceof MethodData)) {
			MethodData data = (MethodData) obj;
			return data.getSignature().equals(this.getSignature());
		}
		return false;
	}
}
