package cornflakes.compiler;

import java.util.Arrays;

import org.objectweb.asm.Opcodes;

import cornflakes.lang.Tuple;

public class TupleClassData extends ClassData {
	private int[] types;

	public TupleClassData(String tuple) {
		super(false);

		setClassName("cornflakes/lang/Tuple");
		setPackageName("cornflakes/lang");
		setModifiers(Opcodes.ACC_PUBLIC);
		setParentName(true, "java/lang/Object");
		setHasConstructor(true);

		String inner = tuple.substring(1, tuple.length() - 1).trim();
		String[] split = inner.split(",");
		if (split.length == 1) {
			throw new CompileError("Tuple must have more than 1 parameter");
		}

		types = new int[split.length];
		for (int i = 0; i < types.length; i++) {
			String resolved = ClassData.getCurrentClass().resolveClass(split[i].trim());
			types[i] = Tuple.type(resolved);
		}
	}

	public int type(int index) {
		return types[index];
	}

	public int[] getTypes() {
		return types;
	}

	public void setTypes(int[] types) {
		this.types = types;
	}

	@Override
	public boolean isAssignableFrom(ClassData test) {
		if (test instanceof TupleClassData) {
			return Arrays.equals(((TupleClassData) test).types, this.types);
		}
		return false;
	}

	@Override
	public boolean isSubclassOf(ClassData test) {
		return false;
	}
}
