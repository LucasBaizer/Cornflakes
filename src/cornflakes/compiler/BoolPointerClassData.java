package cornflakes.compiler;

public class BoolPointerClassData extends PointerClassData {
	public BoolPointerClassData(String type) {
		super(type);

		setClassName("cornflakes/lang/BoolPointer");
	}

	@Override
	public DefinitiveType getValueType() {
		return DefinitiveType.primitive("Z");
	}
}
