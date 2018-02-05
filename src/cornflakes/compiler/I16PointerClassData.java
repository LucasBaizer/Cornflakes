package cornflakes.compiler;

public class I16PointerClassData extends PointerClassData {
	public I16PointerClassData(String type) {
		super(type);

		setClassName("cornflakes/lang/I16Pointer");
	}
	
	@Override
	public DefinitiveType getValueType() {
		return DefinitiveType.primitive("S");
	}
}
