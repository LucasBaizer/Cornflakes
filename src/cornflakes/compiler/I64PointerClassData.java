package cornflakes.compiler;

public class I64PointerClassData extends PointerClassData {
	public I64PointerClassData(String type) {
		super(type);

		setClassName("cornflakes/lang/I64Pointer");
	}

	@Override
	public DefinitiveType getValueType() {
		return DefinitiveType.primitive("J");
	}
}
