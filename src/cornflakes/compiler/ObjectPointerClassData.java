package cornflakes.compiler;

import java.util.Arrays;

public class ObjectPointerClassData extends PointerClassData {
	private String type;
	
	public ObjectPointerClassData(String type) {
		super(type);
		
		this.type = type;
		
		System.out.println(type + " <- " + Arrays.asList(Thread.currentThread().getStackTrace()));

		setClassName("cornflakes/lang/ObjectPointer");
	}
	
	@Override
	public DefinitiveType getValueType() {
		return DefinitiveType.object(type);
	}
}
