package cornflakes.lang;

/**
 * The <code>I64Pointer</code> class is the wrapper type for the
 * <code>i64*</code> Cornflakes type. It is a heap-stored object which contains
 * a stack-stored 64-bit integer value.
 * 
 * @author Lucas Baizer
 */
public class I64Pointer extends Pointer {
	private static final long serialVersionUID = 7728877480676918055L;
	private long val;

	/**
	 * Creates a new pointer with an initial value.
	 * 
	 * @param val
	 *            The initial value for the pointer
	 */
	public I64Pointer(long val) {
		this.val = val;
	}

	/**
	 * Sets the value that the pointer points to.
	 * 
	 * @param val
	 *            The new value
	 */
	public void setValue(long val) {
		this.val = val;
	}

	/**
	 * @return the value that the pointer points to
	 */
	public long getValue() {
		return this.val;
	}

	/**
	 * @return the string representation of the value that the pointer points to
	 */
	@Override
	public String toString() {
		return String.valueOf(val);
	}

	@Override
	public int hashCode() {
		return (int) (val ^ (val >>> 32));
	}

	/**
	 * @return <code>true</code> if the other Object is an
	 *         <code>I64Pointer</code> which points to the same value as this
	 *         <code>I64Pointer</code>, otherwise <code>false</code>
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof I64Pointer) {
			I64Pointer ptr = (I64Pointer) obj;
			return ptr.val == this.val;
		}
		return false;
	}
}
