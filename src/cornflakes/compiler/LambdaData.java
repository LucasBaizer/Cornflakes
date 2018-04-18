package cornflakes.compiler;

public class LambdaData extends MethodData {
	private MethodData parent;

	public LambdaData(ClassData data, MethodData parent, String name, DefinitiveType ret, boolean ifm, int mods) {
		super(data, name, ret, ifm, mods);

		this.parent = parent;
	}

	@Override
	public boolean hasLocal(String name, Block block) {
		return parent.hasLocal(name, block) || super.hasLocal(name, block);
	}

	@Override
	public LocalData getLocal(String name, Block block) {
		return coalesce(parent.getLocal(name, block), super.getLocal(name, block));
	}

	private static <T> T coalesce(T a, T b) {
		if (a == null)
			return b;

		return a;
	}
}
