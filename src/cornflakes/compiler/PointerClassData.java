package cornflakes.compiler;

import org.objectweb.asm.Opcodes;

public abstract class PointerClassData extends ClassData {
	private DefinitiveType type;

	public static PointerClassData from(String type) {
		if (type.equals("i8*") || type.equals("cornflakes/lang/I8Pointer")) {
			return new I8PointerClassData(type);
		} else if (type.equals("i16*") || type.equals("cornflakes/lang/I16Pointer")) {
			return new I16PointerClassData(type);
		} else if (type.equals("i32*") || type.equals("cornflakes/lang/I32Pointer")) {
			return new I32PointerClassData(type);
		} else if (type.equals("i64*") || type.equals("cornflakes/lang/I64Pointer")) {
			return new I64PointerClassData(type);
		} else if (type.equals("f32*") || type.equals("cornflakes/lang/F32Pointer")) {
			return new F32PointerClassData(type);
		} else if (type.equals("f64*") || type.equals("cornflakes/lang/F64Pointer")) {
			return new F64PointerClassData(type);
		} else if (type.equals("bool*") || type.equals("cornflakes/lang/BoolPointer")) {
			return new BoolPointerClassData(type);
		} else if (type.equals("char*") || type.equals("cornflakes/lang/CharPointer")) {
			return new CharPointerClassData(type);
		} else {
			return new ObjectPointerClassData(type);
		}
	}

	protected PointerClassData(String type) {
		super(false);

		setPackageName("cornflakes/lang");
		setModifiers(Opcodes.ACC_PUBLIC);
		setParentName(true, "java/lang/Object");
		setInterfaces(new String[] { "java/io/Serializable" });
		setHasConstructor(true);

		this.type = DefinitiveType.object(type);
	}

	public DefinitiveType getType() {
		return this.type;
	}

	@Override
	public boolean isAssignableFrom(ClassData test) {
		if (test instanceof PointerClassData) {
			PointerClassData ptr = (PointerClassData) test;

			if (this.getClass() == test.getClass()) {
				return true;
			}
			return Types.isSuitable(type, ptr.type);
		}
		return false;
	}

	@Override
	public boolean is(String test) {
		return false;
	}

	public abstract DefinitiveType getValueType();
}
