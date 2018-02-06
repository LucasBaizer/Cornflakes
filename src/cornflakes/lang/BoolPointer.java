package cornflakes.lang;

/**
 * The <code>BoolPointer</code> class is the wrapper type for the
 * <code>bool*</code> Cornflakes type. It is a heap-stored object which contains
 * a stack-stored boolean value.
 * 
 * @author Lucas Baizer
 */
public class BoolPointer extends Pointer {
	private static final long serialVersionUID = -3032961738606851781L;
	private boolean val;

	/**
	 * Creates a new pointer with an initial value.
	 * 
	 * @param val
	 *            The initial value for the pointer
	 */
	public BoolPointer(boolean val) {
		this.val = val;
	}

	/**
	 * Sets the value that the pointer points to.
	 * 
	 * @param val
	 *            The new value
	 */
	public void setValue(boolean val) {
		this.val = val;
	}

	/**
	 * @return the value that the pointer points to
	 */
	public boolean getValue() {
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
		return val ? 1 : 0;
	}

	/**
	 * @return <code>true</code> if the other Object is an
	 *         <code>BoolPointer</code> which points to the same value as this
	 *         <code>BoolPointer</code>, otherwise <code>false</code>
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BoolPointer) {
			BoolPointer ptr = (BoolPointer) obj;
			return ptr.val == this.val;
		}
		return false;
	}
}
