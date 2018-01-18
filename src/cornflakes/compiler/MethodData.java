package cornflakes.compiler;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.Opcodes;

public class MethodData implements Accessible {
	private ClassData context;
	private String name;
	private String returnType;
	private List<ParameterData> parameters = new ArrayList<>();
	private Set<GenericParameter> genericParameters = new HashSet<>();
	private List<LocalData> locals = new ArrayList<>();
	private int stackSize;
	private int localVariables;
	private int modifiers;
	private int blocks;
	private boolean interfaceMethod;

	public static MethodData fromJavaMethod(ClassData context, Method method) {
		MethodData mData = new MethodData(context, method.getName(), Types.getTypeSignature(method.getReturnType()),
				method.getDeclaringClass().isInterface(), method.getModifiers());
		Parameter[] params = method.getParameters();
		Type[] genericTypes = method.getGenericParameterTypes();
		for (int i = 0; i < params.length; i++) {
			Parameter param = params[i];
			Type type = genericTypes[i];
			if (type instanceof ParameterizedType) {
				ParameterizedType parized = (ParameterizedType) type;
				Type par = parized.getActualTypeArguments()[0];
				if (par instanceof WildcardType) {
					WildcardType wildcard = (WildcardType) par;
					mData.addGenericParameter(new GenericParameter(param.getName(), null, Types
							.getTypeSignature(Strings.transformClassName(wildcard.getUpperBounds()[0].getTypeName()))));
				}
			} else {
				mData.addParameter(new ParameterData(mData, param.getName(), Types.getTypeSignature(param.getType()),
						param.getModifiers()));
			}
		}

		return mData;
	}

	public MethodData(ClassData data, String name, String ret, boolean ifm, int mods) {
		this.name = name;
		this.context = data;
		this.returnType = ret;
		this.setInterfaceMethod(ifm);
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

	public int getCurrentStack() {
		return currentStack;
	}

	public int getLocalVariables() {
		return localVariables;
	}

	public void setLocalVariables(int localVariables) {
		this.localVariables = localVariables;
	}

	public void addLocalVariable() {
		this.localVariables++;
	}

	public boolean hasLocal(String name, Block block) {
		return getLocal(name, block) != null;
	}

	public LocalData getLocal(String name, Block block) {
		for (LocalData data : this.locals) {
			// TODO
			if (data.getName().equals(name))
				return data;
			if (data.getName().equals(name) && block.getStart() >= data.getBlock().getStart()) {
				return data;
			}
		}
		return null;
	}

	public void addLocal(LocalData local) {
		locals.add(local);
	}

	public void setParameters(List<ParameterData> params) {
		this.parameters = new ArrayList<>(params);

		int idx = hasModifier(Opcodes.ACC_STATIC) ? 0 : 1;
		for (ParameterData par : this.parameters) {
			this.locals.add(new LocalData(par.getName(), par.getType(), null, idx++, 0));
		}
	}

	public void setBlock(Block block) {
		for (LocalData local : this.locals) {
			local.setBlock(block);
		}
	}

	public void addParameter(ParameterData data) {
		this.parameters.add(data);
	}

	public List<ParameterData> getParameters() {
		return this.parameters;
	}

	public ParameterData getParameter(String name) {
		for (ParameterData data : this.parameters) {
			if (data.getName().equals(name)) {
				return data;
			}
		}
		return null;
	}

	public boolean hasParameter(String name) {
		return getParameter(name) != null;
	}

	@Override
	public int getModifiers() {
		return modifiers;
	}

	public void setModifiers(int modifiers) {
		this.modifiers = modifiers;
	}

	public String getSignature() {
		String desc = "(";
		for (ParameterData par : parameters) {
			desc += par.getType();
		}
		desc += ")" + getReturnTypeSignature();

		return desc;
	}

	@Override
	public boolean equals(Object obj) {
		if ((obj instanceof MethodData)) {
			MethodData data = (MethodData) obj;
			return data.toString().equals(this.toString());
		}
		return false;
	}

	@Override
	public String toString() {
		return getName() + getSignature();
	}

	public List<LocalData> getLocals() {
		return this.locals;
	}

	public int getBlocks() {
		return blocks;
	}

	public void setBlocks(int blocks) {
		this.blocks = blocks;
	}

	public void addBlock() {
		this.blocks++;
	}

	public boolean isInterfaceMethod() {
		return interfaceMethod;
	}

	public void setInterfaceMethod(boolean interfaceMethod) {
		this.interfaceMethod = interfaceMethod;
	}

	private int currentStack = 0;

	public void ics() {
		this.currentStack++;
		if (this.currentStack > stackSize) {
			stackSize = this.currentStack;
		}
	}

	public void dcs() {
		this.currentStack--;
	}

	public Set<GenericParameter> getGenericParameters() {
		return genericParameters;
	}

	public void setGenericParameters(Set<GenericParameter> genericParameters) {
		this.genericParameters = genericParameters;
	}

	public boolean isGenericParameter(String name) {
		return getGenericParameter(name) != null;
	}

	public void addGenericParameter(GenericParameter type) {
		genericParameters.add(type);
	}

	public GenericParameter getGenericParameter(String name) {
		for (GenericParameter type : genericParameters) {
			if (type.getName().equals(name)) {
				return type;
			}
		}

		return null;
	}

	@Override
	public ClassData getContext() {
		return context;
	}
}