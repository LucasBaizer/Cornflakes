package cornflakes.compiler;

import org.objectweb.asm.Opcodes;

public interface Accessible {
	public ClassData getContext();

	public int getModifiers();

	public default boolean hasModifier(int mod) {
		return (getModifiers() & mod) == mod;
	}

	public default boolean isAccessible(ClassData context) {
		if (hasModifier(Opcodes.ACC_PUBLIC)) {
			return true;
		}
		if (hasModifier(Opcodes.ACC_PRIVATE)) {
			return context == getContext();
		}
		if (hasModifier(Opcodes.ACC_PROTECTED)) {
			return context.getPackageName().equals(getContext().getPackageName()) || context.isSubclassOf(getContext());
		}
		return context.getPackageName().equals(getContext().getPackageName());
	}
}
