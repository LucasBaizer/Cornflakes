package cornflakes.compiler;

public class ObjectPointerClassData extends PointerClassData {
	private String type;

	public ObjectPointerClassData(String type) {
		super(type);

		this.type = type;

		setClassName("cornflakes/lang/ObjectPointer");
	}

	@Override
	public DefinitiveType getValueType() {
		return DefinitiveType.object(type);
	}
}
