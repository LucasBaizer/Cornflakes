package cornflakes.compiler;

public class GenericType {
	private boolean extendsType;
	private String type;

	public GenericType(String type, boolean ext) {
		this.setType(type);
		this.setExtendsType(ext);
	}

	public boolean isExtendsType() {
		return extendsType;
	}

	public void setExtendsType(boolean extendsType) {
		this.extendsType = extendsType;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
