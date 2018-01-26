package cornflakes.lang;

public class IteratorException extends RuntimeException {
	private static final long serialVersionUID = -1373533174321003567L;

	public IteratorException() {
	}

	public IteratorException(String message) {
		super(message);
	}

	public IteratorException(Throwable cause) {
		super(cause);
	}

	public IteratorException(String message, Throwable cause) {
		super(message, cause);
	}

	public IteratorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
