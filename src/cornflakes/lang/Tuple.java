package cornflakes.lang;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

public final class Tuple implements Serializable, Iterable<Object> {
	private static final long serialVersionUID = 3065105932170095516L;
	private Object[] items;

	public Tuple(int length) {
		items = new Object[length];
	}

	public void item(int index, Object item) {
		items[index] = item;
	}

	public void item(int index, int item) {
		items[index] = item;
	}

	public void item(int index, float item) {
		items[index] = item;
	}

	public void item(int index, double item) {
		items[index] = item;
	}

	public void item(int index, byte item) {
		items[index] = item;
	}

	public void item(int index, short item) {
		items[index] = item;
	}

	public void item(int index, char item) {
		items[index] = item;
	}

	public void item(int index, long item) {
		items[index] = item;
	}

	public void item(int index, boolean item) {
		items[index] = item;
	}

	public int getLength() {
		return items.length;
	}

	public Object item(int index) {
		return items[index];
	}

	public int i32Item(int index) {
		return (int) items[index];
	}

	public float f32Item(int index) {
		return (float) items[index];
	}

	public double f64Item(int index) {
		return (double) items[index];
	}

	public short i16Item(int index) {
		return (short) items[index];
	}

	public byte i8Item(int index) {
		return (byte) items[index];
	}

	public long i64Item(int index) {
		return (long) items[index];
	}

	public boolean boolItem(int index) {
		return (boolean) items[index];
	}

	public char charItem(int index) {
		return (char) items[index];
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(items);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Tuple)) {
			return false;
		}
		Tuple other = (Tuple) obj;
		if (!Arrays.equals(items, other.items)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		String str = "(";
		for (int i = 0; i < items.length; i++) {
			str += items[i] == null ? "null" : items[i].toString();
			if (i < items.length - 1) {
				str += ", ";
			}
		}

		return str + ")";
	}

	@Override
	public Iterator<Object> iterator() {
		return new TupleIterator();
	}
	
	private class TupleIterator implements Iterator<Object> {
		private int idx = 0;
		
		@Override
		public boolean hasNext() {
			return idx < items.length;
		}

		@Override
		public Object next() {
			return items[idx++];
		}
	}
}
