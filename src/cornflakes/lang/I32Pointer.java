package cornflakes.lang;

/**
 * The <code>I32Pointer</code> class is the wrapper type for the
 * <code>i32*</code> Cornflakes type. It is a heap-stored object which contains
 * a stack-stored 32-bit integer value.
 * 
 * @author Lucas Baizer
 */
public class I32Pointer extends Pointer {
	private static final long serialVersionUID = -6435560814040737047L;
	private int val;

	/**
	 * Creates a new pointer with an initial value.
	 * 
	 * @param val
	 *            The initial value for the pointer
	 */
	public I32Pointer(int val) {
		this.val = val;
	}

	/**
	 * Sets the value that the pointer points to.
	 * 
	 * @param val
	 *            The new value
	 */
	public void setValue(int val) {
		this.val = val;
	}

	/**
	 * @return the value that the pointer points to
	 */
	public int getValue() {
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
	 *         <code>I32Pointer</code> which points to the same value as this
	 *         <code>I32Pointer</code>, otherwise <code>false</code>
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof I32Pointer) {
			I32Pointer ptr = (I32Pointer) obj;
			return ptr.val == this.val;
		}
		return false;
	}
}
