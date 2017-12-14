package cornflakes.compiler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;

public class ConstructorData extends MethodData {
	public ConstructorData(String name, String ret, int mods) {
		super(name, ret, mods);
	}

	public static ConstructorData fromJavaConstructor(Constructor<?> method) {
		ConstructorData mData = new ConstructorData(method.getName(), Types.getTypeSignature(method.getDeclaringClass()),
				method.getModifiers());
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
