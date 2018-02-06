package cornflakes.lang;

/**
 * The <code>F32Pointer</code> class is the wrapper type for the
 * <code>f32*</code> Cornflakes type. It is a heap-stored object which contains
 * a stack-stored 32-bit floating-point value.
 * 
 * @author Lucas Baizer
 */
public class F32Pointer extends Pointer {
	private static final long serialVersionUID = 7472427928156425700L;
	private float val;

	/**
	 * Creates a new pointer with an initial value.
	 * 
	 * @param val
	 *            The initial value for the pointer
	 */
	public F32Pointer(float val) {
		this.val = val;
	}

	/**
	 * Sets the value that the pointer points to.
	 * 
	 * @param val
	 *            The new value
	 */
	public void setValue(float val) {
		this.val = val;
	}

	/**
	 * @return the value that the pointer points to
	 */
	public float getValue() {
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
		return Float.floatToIntBits(val);
	}

	/**
	 * @return <code>true</code> if the other Object is an
	 *         <code>F32Pointer</code> which points to the same value as this
	 *         <code>F32Pointer</code>, otherwise <code>false</code>
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof F32Pointer) {
			F32Pointer ptr = (F32Pointer) obj;
			return ptr.val == this.val;
		}
		return false;
	}
}
