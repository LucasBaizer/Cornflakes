package cornflakes.lang;

/**
 * An <code>IteratorException</code> is an unchecked exception thrown when an
 * invalid operation is performed on a {@link cornflakes.lang.FunctionalIterator
 * FunctionalIterator}.
 * 
 * @author Lucas Baizer
 */
public class IteratorException extends RuntimeException {
	private static final long serialVersionUID = -1373533174321003567L;

	/**
	 * {@inheritDoc}
	 */
	public IteratorException() {
	}

	/**
	 * {@inheritDoc}
	 */
	public IteratorException(String message) {
		super(message);
	}

	/**
	 * {@inheritDoc}
	 */
	public IteratorException(Throwable cause) {
		super(cause);
	}

	/**
	 * {@inheritDoc}
	 */
	public IteratorException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * {@inheritDoc}
	 */
	public IteratorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
