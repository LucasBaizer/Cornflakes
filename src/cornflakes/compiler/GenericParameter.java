package cornflakes.compiler;

public class GenericParameter {
	private String name;
	private String extendsType;

	public GenericParameter(String name) {
		this(name, null);
	}

	public GenericParameter(String name, String extendsType) {
		this.name = name;
		this.extendsType = extendsType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getExtendsType() {
		return extendsType;
	}

	public void setExtendsType(String extendsType) {
		this.extendsType = extendsType;
	}

	public boolean extendsType() {
		return this.extendsType != null;
	}
}
