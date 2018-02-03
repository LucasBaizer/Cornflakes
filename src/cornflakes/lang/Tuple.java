package cornflakes.lang;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

public final class Tuple implements Serializable, Iterable<Object>, Cloneable {
	private static final long serialVersionUID = 3065105932170095516L;

	public static final int OBJECT = 0;
	public static final int I32 = 1;
	public static final int I64 = 2;
	public static final int I16 = 3;
	public static final int I8 = 4;
	public static final int F32 = 5;
	public static final int F64 = 6;
	public static final int BOOL = 7;
	public static final int CHAR = 8;

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
	private int[] typeCount;
	private int[] types;

	public Tuple(int length, int[] types) {
		this.length = length;
		this.typeCount = types;
		this.types = new int[length];

		if (types[OBJECT] > 0)
			items = new Object[length];
		if (types[I32] > 0)
			i32Items = new int[length];
		if (types[I64] > 0)
			i64Items = new long[length];
		if (types[F32] > 0)
			f32Items = new float[length];
		if (types[F64] > 0)
			f64Items = new double[length];
		if (types[I8] > 0)
			i8Items = new byte[length];
		if (types[I16] > 0)
			i16Items = new short[length];
		if (types[CHAR] > 0)
			charItems = new char[length];
		if (types[BOOL] > 0)
			boolItems = new boolean[length];
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
		Tuple x = new Tuple(length, Arrays.copyOf(typeCount, length));
		x.items = items == null ? null : Arrays.copyOf(items, length);
		x.i8Items = i8Items == null ? null : Arrays.copyOf(i8Items, length);
		x.i16Items = i16Items == null ? null : Arrays.copyOf(i16Items, length);
		x.i32Items = i32Items == null ? null : Arrays.copyOf(i32Items, length);
		x.i64Items = i64Items == null ? null : Arrays.copyOf(i64Items, length);
		x.f32Items = f32Items == null ? null : Arrays.copyOf(f32Items, length);
		x.f64Items = f64Items == null ? null : Arrays.copyOf(f64Items, length);
		x.boolItems = boolItems == null ? null : Arrays.copyOf(boolItems, length);
		x.charItems = charItems == null ? null : Arrays.copyOf(charItems, length);

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
		result = prime * result + Arrays.hashCode(typeCount);
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
		if (!Arrays.equals(typeCount, other.typeCount)) {
			return false;
		}
		return true;
	}
}
