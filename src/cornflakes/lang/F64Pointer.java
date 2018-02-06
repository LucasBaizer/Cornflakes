package cornflakes.lang;

/**
 * The <code>F64Pointer</code> class is the wrapper type for the
 * <code>f64*</code> Cornflakes type. It is a heap-stored object which contains
 * a stack-stored 64-bit floating-point value.
 * 
 * @author Lucas Baizer
 */
public class F64Pointer extends Pointer {
	private static final long serialVersionUID = -4077871289905340566L;
	private double val;

	/**
	 * Creates a new pointer with an initial value.
	 * 
	 * @param val
	 *            The initial value for the pointer
	 */
	public F64Pointer(double val) {
		this.val = val;
	}

	/**
	 * Sets the value that the pointer points to.
	 * 
	 * @param val
	 *            The new value
	 */
	public void setValue(double val) {
		this.val = val;
	}

	/**
	 * @return the value that the pointer points to
	 */
	public double getValue() {
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
		long b = Double.doubleToLongBits(val);
		return (int) (b ^ (b >>> 32));
	}

	/**
	 * @return <code>true</code> if the other Object is an
	 *         <code>F64Pointer</code> which points to the same value as this
	 *         <code>F64Pointer</code>, otherwise <code>false</code>
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof F64Pointer) {
			F64Pointer ptr = (F64Pointer) obj;
			return ptr.val == this.val;
		}
		return false;
	}
}
