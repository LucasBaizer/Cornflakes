package cornflakes.compiler;

public class CharPointerClassData extends PointerClassData {
	public CharPointerClassData(String type) {
		super(type);

		setClassName("cornflakes/lang/CharPointer");
	}
	
	@Override
	public DefinitiveType getValueType() {
		return DefinitiveType.primitive("C");
	}
}
