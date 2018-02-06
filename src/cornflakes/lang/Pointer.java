package cornflakes.lang;

import java.io.Serializable;

/**
 * The <code>Pointer</code> class is the superclass type for all Cornflakes
 * pointer wrapper classes. It contains several static methods for creating
 * pointers.
 * 
 * @author Lucas Baizer
 */
public abstract class Pointer implements Serializable {
	private static final long serialVersionUID = -374039382700575277L;

	/**
	 * Creates a new <code>I32Pointer</code> given an initial 32-bit integer
	 * value.
	 * 
	 * @param val
	 *            The initial value
	 * @return The new pointer
	 */
	public static Pointer from(int val) {
		return new I32Pointer(val);
	}

	/**
	 * Creates a new <code>I16Pointer</code> given an initial 16-bit integer
	 * value.
	 * 
	 * @param val
	 *            The initial value
	 * @return The new pointer
	 */
	public static Pointer from(short val) {
		return new I16Pointer(val);
	}

	/**
	 * Creates a new <code>I64Pointer</code> given an initial 64-bit integer
	 * value.
	 * 
	 * @param val
	 *            The initial value
	 * @return The new pointer
	 */
	public static Pointer from(long val) {
		return new I64Pointer(val);
	}

	/**
	 * Creates a new <code>I8Pointer</code> given an initial 8-bit integer
	 * value.
	 * 
	 * @param val
	 *            The initial value
	 * @return The new pointer
	 */
	public static Pointer from(byte val) {
		return new I8Pointer(val);
	}

	/**
	 * Creates a new <code>F32Pointer</code> given an initial 32-bit
	 * floating-point value.
	 * 
	 * @param val
	 *            The initial value
	 * @return The new pointer
	 */
	public static Pointer from(float val) {
		return new F32Pointer(val);
	}

	/**
	 * Creates a new <code>F64Pointer</code> given an initial 64-bit
	 * floating-point value.
	 * 
	 * @param val
	 *            The initial value
	 * @return The new pointer
	 */
	public static Pointer from(double val) {
		return new F64Pointer(val);
	}

	/**
	 * Creates a new <code>BoolPointer</code> given an initial boolean value.
	 * 
	 * @param val
	 *            The initial value
	 * @return The new pointer
	 */
	public static Pointer from(boolean val) {
		return new BoolPointer(val);
	}

	/**
	 * Creates a new <code>CharPointer</code> given an initial character
	 * constant.
	 * 
	 * @param val
	 *            The initial value
	 * @return The new pointer
	 */
	public static Pointer from(char val) {
		return new CharPointer(val);
	}

	/**
	 * Creates a new <code>ObjectPointer</code> given an initial object value.
	 * 
	 * @param val
	 *            The initial value
	 * @return The new pointer
	 */
	public static Pointer from(Object val) {
		return new ObjectPointer(val);
	}
}
