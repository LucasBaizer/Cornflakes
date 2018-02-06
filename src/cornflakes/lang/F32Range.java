package cornflakes.lang;

import java.util.Iterator;

/**
 * The <code>F32Range</code> class is an iterable <code>Range</code>
 * implementation used for defining a range between two floating-point values,
 * including an optional increment size. When the iterator is created, a
 * "pointer" value is initialized to the starting value. After each iteration of
 * the range, the increment size is added to the current pointer. This process
 * repeats until the equation <code>pointer + increment >= max</code> is met.
 * Thus, the end value is never reached, but the value at
 * <code>end - increment</code> is reached. Therefore, the end value is
 * exclusive.
 * 
 * @author Lucas Baizer
 */
public class F32Range extends Range implements Iterable<Float> {
	private float start;
	private float end;
	private float increment;

	/**
	 * Creates a new <code>F32Range</code> with a default increment size of 1.
	 * 
	 * @param start
	 *            The value at which the range starts at (inclusive)
	 * @param end
	 *            The value at which the range ends at (exclusive)
	 */
	public F32Range(float start, float end) {
		this(start, end, 1);
	}

	/**
	 * Creates a new <code>F32Range</code> with a specified increment size.
	 * 
	 * @param start
	 *            The value at which the range starts at (inclusive)
	 * @param end
	 *            The value at which the range ends at (exclusive)
	 * @param increment
	 *            The value of which is added for each iteration of the range
	 */
	public F32Range(float start, float end, float increment) {
		this.start = start;
		this.end = end;
		this.increment = increment;
	}

	/**
	 * @return The value that the range started at.
	 */
	public float getStart() {
		return this.start;
	}

	/**
	 * @return The value that the range exclusively ends at. This value will not
	 *         be included in the iteration.
	 */
	public float getEnd() {
		return this.end;
	}

	/**
	 * @return The amount that is added each iteration until the end it reached.
	 */
	public float getIncrement() {
		return this.increment;
	}

	/**
	 * @return A new iterator which iterates using the values assigned to this
	 *         <code>Range</code> object.
	 */
	@Override
	public Iterator<Float> iterator() {
		return new I32RangeIterator(start, end, increment);
	}

	private static class I32RangeIterator implements Iterator<Float> {
		private float max;
		private float increment;
		private float pointer;

		public I32RangeIterator(float min, float max, float increment) {
			this.max = max;
			this.increment = increment;
			this.pointer = min;
		}

		@Override
		public boolean hasNext() {
			return pointer + increment <= max;
		}

		@Override
		public Float next() {
			float p = pointer;
			pointer += increment;
			return p;
		}
	}
}
