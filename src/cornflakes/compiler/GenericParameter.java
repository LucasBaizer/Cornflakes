package cornflakes.compiler;

public class GenericParameter {
	private String name;
	private String type;
	private String extendsType;

	public GenericParameter(String name) {
		this(name, null, null);
	}

	public GenericParameter(String name, String explicit, String extendsType) {
		this.name = name;
		this.type = explicit;
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean hasExplicitType() {
		return this.type != null;
	}
}
