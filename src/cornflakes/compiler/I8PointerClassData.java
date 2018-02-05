package cornflakes.compiler;

public class I8PointerClassData extends PointerClassData {
	public I8PointerClassData(String type) {
		super(type);

		setClassName("cornflakes/lang/I8Pointer");
	}
	
	@Override
	public DefinitiveType getValueType() {
		return DefinitiveType.primitive("B");
	}
}
