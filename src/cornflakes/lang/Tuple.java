package cornflakes.lang;

import java.util.Arrays;

public final class Tuple {
	private Object[] items;
	private int[] types;

	public static final int OBJECT = 0;
	public static final int I8 = 1;
	public static final int I16 = 2;
	public static final int I32 = 3;
	public static final int I64 = 4;
	public static final int F32 = 5;
	public static final int F64 = 6;
	public static final int BOOL = 7;
	public static final int CHAR = 8;

	public Tuple(int length) {
		items = new Object[length];
		types = new int[length];
	}

	public int type(int index) {
		return types[index];
	}

	public void type(int index, int type) {
		types[index] = type;
	}

	public int[] types() {
		return types;
	}

	public void item(int index, Object item) {
		if (item == null) {
			types[index] = OBJECT;
		} else {
			types[index] = type(item.getClass());
		}
		items[index] = item;
	}

	public void item(int index, int item) {
		types[index] = I32;
		items[index] = item;
	}

	public void item(int index, float item) {
		types[index] = F32;
		items[index] = item;
	}

	public void item(int index, double item) {
		types[index] = F64;
		items[index] = item;
	}

	public void item(int index, byte item) {
		types[index] = I8;
		items[index] = item;
	}

	public void item(int index, short item) {
		types[index] = I16;
		items[index] = item;
	}

	public void item(int index, char item) {
		types[index] = CHAR;
		items[index] = item;
	}

	public void item(int index, long item) {
		types[index] = I64;
		items[index] = item;
	}

	public void item(int index, boolean item) {
		types[index] = BOOL;
		items[index] = item;
	}

	public int length() {
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

	public static int type(String prim) {
		if (prim == null) {
			return OBJECT;
		}
		if (prim.equals("B") || prim.equals("i8") || prim.equals("byte")) {
			return I8;
		} else if (prim.equals("S") || prim.equals("i16") || prim.equals("short")) {
			return I16;
		} else if (prim.equals("I") || prim.equals("i32") || prim.equals("int")) {
			return I32;
		} else if (prim.equals("J") || prim.equals("i64") || prim.equals("long")) {
			return I64;
		} else if (prim.equals("F") || prim.equals("f32") || prim.equals("float")) {
			return F32;
		} else if (prim.equals("D") || prim.equals("f64") || prim.equals("double")) {
			return F64;
		} else if (prim.equals("Z") || prim.equals("bool")) {
			return BOOL;
		} else if (prim.equals("C") || prim.equals("char")) {
			return CHAR;
		}
		return OBJECT;
	}

	public static String getTypeName(int type) {
		switch (type) {
			case I8:
				return "i8";
			case I16:
				return "i16";
			case I32:
				return "i32";
			case I64:
				return "i64";
			case F32:
				return "f32";
			case F64:
				return "f64";
			case CHAR:
				return "char";
			case BOOL:
				return "bool";
			case OBJECT:
				return "object";
			default:
				throw new IllegalArgumentException(String.valueOf(type));
		}
	}

	public static int type(Class<?> val) {
		if (val == null) {
			return OBJECT;
		}
		if (val.isPrimitive()) {
			if (val == Byte.class) {
				return I8;
			} else if (val == Short.class) {
				return I16;
			} else if (val == Integer.class) {
				return I32;
			} else if (val == Long.class) {
				return I64;
			} else if (val == Float.class) {
				return F32;
			} else if (val == Double.class) {
				return F64;
			} else if (val == Boolean.class) {
				return BOOL;
			} else if (val == Character.class) {
				return CHAR;
			}
		}
		return OBJECT;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		if (!Arrays.equals(items, other.items)) {
			return false;
		}
		if (!Arrays.equals(types, other.types)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		String str = "(";
		for (int i = 0; i < items.length; i++) {
			str += items[i].toString();
			if (i < items.length - 1) {
				str += ", ";
			}
		}

		return str + ")";
	}

	public String toTypeString() {
		String str = "(";
		for (int i = 0; i < types.length; i++) {
			str += items[i].toString();
			if (i < items.length - 1) {
				str += ", ";
			}
		}

		return str + ")";
	}
}
