package cornflakes.lang;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

	private transient int length;
	private transient Object[] items;
	private transient int[] i32Items;
	private transient float[] f32Items;
	private transient double[] f64Items;
	private transient byte[] i8Items;
	private transient short[] i16Items;
	private transient char[] charItems;
	private transient boolean[] boolItems;
	private transient long[] i64Items;
	private transient int[] typeCount;
	private transient int[] types;

	public Tuple(int length, int[] typeCount) {
		this.length = length;
		this.typeCount = typeCount;
		this.types = new int[length];

		if (typeCount[OBJECT] > 0)
			items = new Object[length];
		if (typeCount[I32] > 0)
			i32Items = new int[length];
		if (typeCount[I64] > 0)
			i64Items = new long[length];
		if (typeCount[F32] > 0)
			f32Items = new float[length];
		if (typeCount[F64] > 0)
			f64Items = new double[length];
		if (typeCount[I8] > 0)
			i8Items = new byte[length];
		if (typeCount[I16] > 0)
			i16Items = new short[length];
		if (typeCount[CHAR] > 0)
			charItems = new char[length];
		if (typeCount[BOOL] > 0)
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

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();

		out.writeInt(length);
		out.writeObject(typeCount);
		out.writeObject(types);

		if (typeCount[OBJECT] > 0)
			out.writeObject(items);
		if (typeCount[I32] > 0)
			out.writeObject(i32Items);
		if (typeCount[I64] > 0)
			out.writeObject(i64Items);
		if (typeCount[F32] > 0)
			out.writeObject(f32Items);
		if (typeCount[F64] > 0)
			out.writeObject(f64Items);
		if (typeCount[I8] > 0)
			out.writeObject(i8Items);
		if (typeCount[I16] > 0)
			out.writeObject(i16Items);
		if (typeCount[CHAR] > 0)
			out.writeObject(charItems);
		if (typeCount[BOOL] > 0)
			out.writeObject(boolItems);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();

		length = in.readInt();
		typeCount = (int[]) in.readObject();
		types = (int[]) in.readObject();

		if (typeCount[OBJECT] > 0)
			items = (Object[]) in.readObject();
		if (typeCount[I32] > 0)
			i32Items = (int[]) in.readObject();
		if (typeCount[I64] > 0)
			i64Items = (long[]) in.readObject();
		if (typeCount[F32] > 0)
			f32Items = (float[]) in.readObject();
		if (typeCount[F64] > 0)
			f64Items = (double[]) in.readObject();
		if (typeCount[I8] > 0)
			i8Items = (byte[]) in.readObject();
		if (typeCount[I16] > 0)
			i16Items = (short[]) in.readObject();
		if (typeCount[CHAR] > 0)
			charItems = (char[]) in.readObject();
		if (typeCount[BOOL] > 0)
			boolItems = (boolean[]) in.readObject();
	}
}
