package cornflakes.compiler;

public class F64PointerClassData extends PointerClassData {
	public F64PointerClassData(String type) {
		super(type);

		setClassName("cornflakes/lang/F64Pointer");
	}
	
	@Override
	public DefinitiveType getValueType() {
		return DefinitiveType.primitive("D");
	}
}
