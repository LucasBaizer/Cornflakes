package cornflakes.compiler;

public class I32PointerClassData extends PointerClassData {
	public I32PointerClassData(String type) {
		super(type);

		setClassName("cornflakes/lang/I32Pointer");
	}
	
	@Override
	public DefinitiveType getValueType() {
		return DefinitiveType.primitive("I");
	}
}
