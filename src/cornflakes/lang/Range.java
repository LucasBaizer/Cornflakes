package cornflakes.lang;

/**
 * The <code>Range</code> class represents any given range between two primitive
 * values. It is the common superclass among all Cornflakes ranges.
 * 
 * @author Lucas Baizer
 */
public abstract class Range {
	/**
	 * Creates a new <code>I32Range</code>.
	 * 
	 * @see {@link cornflakes.lang.I32Range#I32Range(int, int)}
	 */
	public static I32Range from(int start, int end) {
		return new I32Range(start, end);
	}

	/**
	 * Creates a new <code>I32Range</code>.
	 * 
	 * @see {@link cornflakes.lang.I32Range#I32Range(int, int, int)}
	 */
	public static I32Range from(int start, int end, int increment) {
		return new I32Range(start, end, increment);
	}

	/**
	 * Creates a new <code>F32Range</code>.
	 * 
	 * @see {@link cornflakes.lang.F32Range#F32Range(float, float)}
	 */
	public static F32Range from(float start, float end) {
		return new F32Range(start, end);
	}

	/**
	 * Creates a new <code>F32Range</code>.
	 * 
	 * @see {@link cornflakes.lang.F32Range#F32Range(float, float, float)}
	 */
	public static F32Range from(float start, float end, float increment) {
		return new F32Range(start, end, increment);
	}
}
