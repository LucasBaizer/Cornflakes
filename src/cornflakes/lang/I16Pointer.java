package cornflakes.lang;

/**
 * The <code>I16Pointer</code> class is the wrapper type for the
 * <code>i16*</code> Cornflakes type. It is a heap-stored object which contains
 * a stack-stored 16-bit integer value.
 * 
 * @author Lucas Baizer
 */
public class I16Pointer extends Pointer {
	private static final long serialVersionUID = -866473203367921503L;
	private short val;

	/**
	 * Creates a new pointer with an initial value.
	 * 
	 * @param val
	 *            The initial value for the pointer
	 */
	public I16Pointer(short val) {
		this.val = val;
	}

	/**
	 * Sets the value that the pointer points to.
	 * 
	 * @param val
	 *            The new value
	 */
	public void setValue(short val) {
		this.val = val;
	}

	/**
	 * @return the value that the pointer points to
	 */
	public short getValue() {
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
		return val;
	}

	/**
	 * @return <code>true</code> if the other Object is an
	 *         <code>I16Pointer</code> which points to the same value as this
	 *         <code>I16Pointer</code>, otherwise <code>false</code>
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof I16Pointer) {
			I16Pointer ptr = (I16Pointer) obj;
			return ptr.val == this.val;
		}
		return false;
	}
}
