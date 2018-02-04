package cornflakes.lang;

import java.util.Iterator;

public class I32Range implements Iterable<Integer> {
	private int min;
	private int max;
	private int increment;

	public I32Range(int min, int max) {
		this(min, max, 1);
	}

	public I32Range(int min, int max, int increment) {
		this.min = min;
		this.max = max;
		this.increment = increment;
	}

	public int getStart() {
		return this.min;
	}

	public int getEnd() {
		return this.max;
	}

	public int getIncrement() {
		return this.increment;
	}

	@Override
	public Iterator<Integer> iterator() {
		return new I32RangeIterator(min, max, increment);
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
