package cornflakes.compiler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;

public class ConstructorData extends MethodData {
	public ConstructorData(ClassData context, int mods) {
		super(context, "<init>", DefinitiveType.primitive("V"), false, mods);
	}

	public static ConstructorData fromJavaConstructor(ClassData data, Constructor<?> method) {
		ConstructorData mData = new ConstructorData(data, method.getModifiers());
		Parameter[] params = method.getParameters();
		for (int i = 0; i < params.length; i++) {
			mData.addParameter(new ParameterData(mData, params[i].getName(),
					DefinitiveType.assume(Types.getTypeSignature(params[i].getType())), 0));
		}
		return mData;
	}
}
