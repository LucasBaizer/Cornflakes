package cornflakes.lang;

/**
 * The <code>I8Pointer</code> class is the wrapper type for the <code>i8*</code>
 * Cornflakes type. It is a heap-stored object which contains a stack-stored
 * 8-bit integer value.
 * 
 * @author Lucas Baizer
 */
public class I8Pointer extends Pointer {
	private static final long serialVersionUID = 2678549531554508871L;
	private byte val;

	/**
	 * Creates a new pointer with an initial value.
	 * 
	 * @param val
	 *            The initial value for the pointer
	 */
	public I8Pointer(byte val) {
		this.val = val;
	}

	/**
	 * Sets the value that the pointer points to.
	 * 
	 * @param val
	 *            The new value
	 */
	public void setValue(byte val) {
		this.val = val;
	}

	/**
	 * @return the value that the pointer points to
	 */
	public byte getValue() {
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
	 *         <code>I8Pointer</code> which points to the same value as this
	 *         <code>I8Pointer</code>, otherwise <code>false</code>
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof I8Pointer) {
			I8Pointer ptr = (I8Pointer) obj;
			return ptr.val == this.val;
		}
		return false;
	}
}
