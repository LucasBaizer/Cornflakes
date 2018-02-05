package cornflakes.compiler;

public class F32PointerClassData extends PointerClassData {
	public F32PointerClassData(String type) {
		super(type);

		setClassName("cornflakes/lang/F32Pointer");
	}
	
	@Override
	public DefinitiveType getValueType() {
		return DefinitiveType.primitive("F");
	}
}
