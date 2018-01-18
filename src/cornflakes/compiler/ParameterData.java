package cornflakes.compiler;

public class ParameterData {
	private String name;
	private MethodData context;
	private String type;
	private int modifiers;

	public ParameterData(MethodData context, String name, String type, int mods) {
		this.context = context;
		this.name = name;
		this.type = type;
		this.modifiers = mods;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getModifiers() {
		return modifiers;
	}

	public void setModifiers(int modifiers) {
		this.modifiers = modifiers;
	}

	@Override
	public String toString() {
		return name + ": " + type;
	}

	public MethodData getContext() {
		return this.context;
	}
}
