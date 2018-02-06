package cornflakes.lang;

import java.util.Iterator;

/**
 * The <code>I32Range</code> class is an iterable <code>Range</code>
 * implementation used for defining a range between two integer values,
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
public class I32Range extends Range implements Iterable<Integer> {
	private int start;
	private int end;
	private int increment;

	/**
	 * Creates a new <code>I32Range</code> with a default increment size of 1.
	 * 
	 * @param start
	 *            The value at which the range starts at (inclusive)
	 * @param end
	 *            The value at which the range ends at (exclusive)
	 */
	public I32Range(int start, int end) {
		this(start, end, 1);
	}

	/**
	 * Creates a new <code>I32Range</code> with a specified increment size.
	 * 
	 * @param start
	 *            The value at which the range starts at (inclusive)
	 * @param end
	 *            The value at which the range ends at (exclusive)
	 * @param increment
	 *            The value of which is added for each iteration of the range
	 */
	public I32Range(int start, int end, int increment) {
		this.start = start;
		this.end = end;
		this.increment = increment;
	}

	/**
	 * @return The value that the range started at.
	 */
	public int getStart() {
		return this.start;
	}

	/**
	 * @return The value that the range exclusively ends at. This value will not
	 *         be included in the iteration.
	 */
	public int getEnd() {
		return this.end;
	}

	/**
	 * @return The amount that is added each iteration until the end it reached.
	 */
	public int getIncrement() {
		return this.increment;
	}

	/**
	 * @return A new iterator which iterates using the values assigned to this
	 *         <code>Range</code> object.
	 */
	@Override
	public Iterator<Integer> iterator() {
		return new I32RangeIterator(start, end, increment);
	}

	private static class I32RangeIterator implements Iterator<Integer> {
		private int max;
		private int increment;
		private int pointer;

		public I32RangeIterator(int min, int max, int increment) {
			this.max = max;
			this.increment = increment;
			this.pointer = min;
		}

		@Override
		public boolean hasNext() {
			return pointer + increment <= max;
		}

		@Override
		public Integer next() {
			int p = pointer;
			pointer += increment;
			return p;
		}
	}
}
