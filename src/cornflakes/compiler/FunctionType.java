package cornflakes.compiler;

public enum FunctionType {
	FUNCTION("func"), OPERATOR_OVERLOAD("operator"), INDEXER("indexer"), ITERATOR("iter");

	private String keyword;

	private FunctionType(String keyword) {
		this.keyword = keyword;
	}

	public String getKeyword() {
		return keyword;
	}
}