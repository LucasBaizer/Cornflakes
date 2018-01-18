package cornflakes.compiler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;

public class ConstructorData extends MethodData {
	public ConstructorData(ClassData context, String name, int mods) {
		super(context, name, "V", false, mods);
	}

	public static ConstructorData fromJavaConstructor(ClassData data, Constructor<?> method) {
		ConstructorData mData = new ConstructorData(data, method.getName(), method.getModifiers());
		Parameter[] params = method.getParameters();
		for (int i = 0; i < params.length; i++) {
			mData.addParameter(params[i].getName(), Types.getTypeSignature(params[i].getType()));
		}
		return mData;
	}

	@Override
	public String getReturnTypeSignature() {
		return "V";
	}
}
