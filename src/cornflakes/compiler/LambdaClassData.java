package cornflakes.compiler;

public class LambdaClassData extends ClassData {
	public LambdaClassData() {
		super(false);
	}
	
	public MethodData getLambdaMethod() {
		for (MethodData data : this.getMethods()) {
			if (data.isInterfaceMethod()) {
				return data;
			}
		}

		return null;
	}
}
