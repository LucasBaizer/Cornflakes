package cornflakes.lang;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

public final class Tuple implements Serializable, Iterable<Object>, Cloneable {
	private static final long serialVersionUID = 3065105932170095516L;

	private static final int OBJECT = 0;
	private static final int I32 = 1;
	private static final int I64 = 2;
	private static final int I16 = 3;
	private static final int I8 = 4;
	private static final int F32 = 5;
	private static final int F64 = 6;
	private static final int BOOL = 7;
	private static final int CHAR = 8;

	private int length;
	private Object[] items;
	private int[] i32Items;
	private float[] f32Items;
	private double[] f64Items;
	private byte[] i8Items;
	private short[] i16Items;
	private char[] charItems;
	private boolean[] boolItems;
	private long[] i64Items;
	private int[] types;

	public Tuple(int length) {
		this.length = length;

		items = new Object[length];
		i32Items = new int[length];
		i64Items = new long[length];
		f32Items = new float[length];
		f64Items = new double[length];
		i8Items = new byte[length];
		i16Items = new short[length];
		charItems = new char[length];
		boolItems = new boolean[length];
		types = new int[length];
	}

	public void item(int index, Object item) {
		items[index] = item;
		types[index] = OBJECT;
	}

	public void item(int index, int item) {
		i32Items[index] = item;
		types[index] = I32;
	}

	public void item(int index, float item) {
		f32Items[index] = item;
		types[index] = F32;
	}

	public void item(int index, double item) {
		f64Items[index] = item;
		types[index] = F64;
	}

	public void item(int index, byte item) {
		i8Items[index] = item;
		types[index] = I8;
	}

	public void item(int index, short item) {
		i16Items[index] = item;
		types[index] = I16;
	}

	public void item(int index, char item) {
		charItems[index] = item;
		types[index] = CHAR;
	}

	public void item(int index, long item) {
		i64Items[index] = item;
		types[index] = I64;
	}

	public void item(int index, boolean item) {
		boolItems[index] = item;
		types[index] = BOOL;
	}

	public int getLength() {
		return length;
	}

	public Object item(int index) {
		return items[index];
	}

	public int i32Item(int index) {
		return i32Items[index];
	}

	public float f32Item(int index) {
		return f32Items[index];
	}

	public double f64Item(int index) {
		return f64Items[index];
	}

	public short i16Item(int index) {
		return i16Items[index];
	}

	public byte i8Item(int index) {
		return i8Items[index];
	}

	public long i64Item(int index) {
		return i64Items[index];
	}

	public boolean boolItem(int index) {
		return boolItems[index];
	}

	public char charItem(int index) {
		return charItems[index];
	}

	@Override
	public Tuple clone() {
		Tuple x = new Tuple(this.items.length);
		x.items = Arrays.copyOf(items, length);
		x.i8Items = Arrays.copyOf(i8Items, length);
		x.i16Items = Arrays.copyOf(i16Items, length);
		x.i32Items = Arrays.copyOf(i32Items, length);
		x.i64Items = Arrays.copyOf(i64Items, length);
		x.f32Items = Arrays.copyOf(f32Items, length);
		x.f64Items = Arrays.copyOf(f64Items, length);
		x.boolItems = Arrays.copyOf(boolItems, length);
		x.charItems = Arrays.copyOf(charItems, length);

		return x;
	}

	@Override
	public String toString() {
		String str = "(";
		for (int i = 0; i < length; i++) {
			switch (types[i]) {
				case OBJECT:
					str += items[i];
					break;
				case I8:
					str += i8Items[i];
					break;
				case I16:
					str += i16Items[i];
					break;
				case I32:
					str += i32Items[i];
					break;
				case I64:
					str += i64Items[i];
					break;
				case F32:
					str += f32Items[i];
					break;
				case F64:
					str += f64Items[i];
					break;
				case BOOL:
					str += boolItems[i];
					break;
				case CHAR:
					str += charItems[i];
					break;
			}

			if (i < length - 1) {
				str += ", ";
			}
		}

		return str + ")";
	}

	public Object[] toArray() {
		Object[] array = new Object[length];
		for (int i = 0; i < length; i++) {
			switch (types[i]) {
				case OBJECT:
					array[i] = items[i];
					break;
				case I8:
					array[i] = i8Items[i];
					break;
				case I16:
					array[i] = i16Items[i];
					break;
				case I32:
					array[i] = i32Items[i];
					break;
				case I64:
					array[i] = i64Items[i];
					break;
				case F32:
					array[i] = f32Items[i];
					break;
				case F64:
					array[i] = f64Items[i];
					break;
				case BOOL:
					array[i] = boolItems[i];
					break;
				case CHAR:
					array[i] = charItems[i];
					break;
			}
		}

		return array;
	}

	@Override
	public Iterator<Object> iterator() {
		return new FunctionalIterator<>(this.toArray());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + length;
		result = prime * result + Arrays.hashCode(boolItems);
		result = prime * result + Arrays.hashCode(charItems);
		result = prime * result + Arrays.hashCode(f32Items);
		result = prime * result + Arrays.hashCode(f64Items);
		result = prime * result + Arrays.hashCode(i16Items);
		result = prime * result + Arrays.hashCode(i32Items);
		result = prime * result + Arrays.hashCode(i64Items);
		result = prime * result + Arrays.hashCode(i8Items);
		result = prime * result + Arrays.hashCode(items);
		result = prime * result + Arrays.hashCode(types);
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
		if (length != other.length) {
			return false;
		}
		if (!Arrays.equals(boolItems, other.boolItems)) {
			return false;
		}
		if (!Arrays.equals(charItems, other.charItems)) {
			return false;
		}
		if (!Arrays.equals(f32Items, other.f32Items)) {
			return false;
		}
		if (!Arrays.equals(f64Items, other.f64Items)) {
			return false;
		}
		if (!Arrays.equals(i16Items, other.i16Items)) {
			return false;
		}
		if (!Arrays.equals(i32Items, other.i32Items)) {
			return false;
		}
		if (!Arrays.equals(i64Items, other.i64Items)) {
			return false;
		}
		if (!Arrays.equals(i8Items, other.i8Items)) {
			return false;
		}
		if (!Arrays.equals(items, other.items)) {
			return false;
		}
		if (!Arrays.equals(types, other.types)) {
			return false;
		}
		return true;
	}
}
