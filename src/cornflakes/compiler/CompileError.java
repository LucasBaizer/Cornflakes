package cornflakes.compiler;

public class CompileError extends RuntimeException {
	private static final long serialVersionUID = 8292164110201192875L;

	public CompileError(String err) {
		super(err);
	}

	public CompileError(Throwable e) {
		super(e);
	}
}
