package cornflakes.lang;

import java.util.Objects;

/**
 * The <code>ObjectPointer</code> class is the wrapper type for any Cornflakes
 * non-primtive pointer type. It is a heap-stored object which contains a
 * reference to another heap-stored object value.
 * 
 * @author Lucas Baizer
 */
public class ObjectPointer extends Pointer {
	private static final long serialVersionUID = -8973464555906809564L;
	private Object val;

	/**
	 * Creates a new pointer with an initial value.
	 * 
	 * @param val
	 *            The initial value for the pointer
	 */
	public ObjectPointer(Object val) {
		this.val = val;
	}

	/**
	 * Sets the value that the pointer points to.
	 * 
	 * @param val
	 *            The new value
	 */
	public void setValue(Object val) {
		this.val = val;
	}

	/**
	 * @return the value that the pointer points to
	 */
	public Object getValue() {
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
		return val.hashCode();
	}

	/**
	 * @return <code>true</code> if the other Object is an
	 *         <code>ObjectPointer</code> which points to the same value as this
	 *         <code>ObjectPointer</code>, otherwise <code>false</code>. This is
	 *         determined by calling
	 *         {@link java.util.Objects#equals(Object, Object)
	 *         Objects.equals(Object, Object)} on the reference values in each
	 *         of the pointers.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ObjectPointer) {
			ObjectPointer ptr = (ObjectPointer) obj;
			return Objects.equals(val, ptr.val);
		}
		return false;
	}
}
