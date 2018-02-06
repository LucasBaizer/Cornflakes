package cornflakes.lang;

/**
 * The <code>CharPointer</code> class is the wrapper type for the
 * <code>char*</code> Cornflakes type. It is a heap-stored object which contains
 * a stack-stored character constant.
 * 
 * @author Lucas Baizer
 */
public class CharPointer extends Pointer {
	private static final long serialVersionUID = 2990653635239705286L;
	private char val;

	/**
	 * Creates a new pointer with an initial value.
	 * 
	 * @param val
	 *            The initial value for the pointer
	 */
	public CharPointer(char val) {
		this.val = val;
	}

	/**
	 * Sets the value that the pointer points to.
	 * 
	 * @param val
	 *            The new value
	 */
	public void setValue(char val) {
		this.val = val;
	}

	/**
	 * @return the value that the pointer points to
	 */
	public char getValue() {
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
	 *         <code>CharPointer</code> which points to the same value as this
	 *         <code>CharPointer</code>, otherwise <code>false</code>
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CharPointer) {
			CharPointer ptr = (CharPointer) obj;
			return ptr.val == this.val;
		}
		return false;
	}
}
