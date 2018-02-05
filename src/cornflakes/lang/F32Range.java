package cornflakes.lang;

import java.util.Iterator;

public class F32Range extends Range implements Iterable<Float> {
	private float min;
	private float max;
	private float increment;

	public F32Range(float min, float max) {
		this(min, max, 1);
	}

	public F32Range(float min, float max, float increment) {
		this.min = min;
		this.max = max;
		this.increment = increment;
	}

	public float getStart() {
		return this.min;
	}

	public float getEnd() {
		return this.max;
	}

	public float getIncrement() {
		return this.increment;
	}

	@Override
	public Iterator<Float> iterator() {
		return new I32RangeIterator(min, max, increment);
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
