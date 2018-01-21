package cornflakes.compiler;

import org.objectweb.asm.Opcodes;

public class TupleClassData extends ClassData {
	private DefinitiveType[] types;

	public TupleClassData(String tuple) {
		super(false);

		setClassName("cornflakes/lang/Tuple");
		setPackageName("cornflakes/lang");
		setModifiers(Opcodes.ACC_PUBLIC);
		setParentName(true, "java/lang/Object");
		setHasConstructor(true);
		setInterfaces(new String[] { "java/io/Serializable" });
		addMethod(new MethodData(this, "getLength", DefinitiveType.primitive("I"), false, Opcodes.ACC_PUBLIC));

		String inner = tuple.substring(1, tuple.length() - 1).trim();
		String[] split = inner.split(",");
		if (split.length == 1) {
			throw new CompileError("Tuple must have more than 1 parameter");
		}

		types = new DefinitiveType[split.length];
		for (int i = 0; i < types.length; i++) {
			DefinitiveType resolved = ClassData.getCurrentClass().resolveClass(split[i].trim());
			types[i] = resolved;
		}
	}

	public DefinitiveType getType(int index) {
		return types[index];
	}

	public DefinitiveType[] getTypes() {
		return types;
	}

	public void setTypes(DefinitiveType[] types) {
		this.types = types;
	}

	@Override
	public boolean isAssignableFrom(ClassData test) {
		if (test instanceof TupleClassData) {
			TupleClassData tuple = (TupleClassData) test;
			if (tuple.types.length != this.types.length) {
				return false;
			}
			for (int i = 0; i < tuple.types.length; i++) {
				if (!Types.isSuitable(this.types[i], tuple.types[i])) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean isSubclassOf(ClassData test) {
		return false;
	}
}
