package cornflakes.lang;

import java.io.Serializable;

public abstract class Pointer implements Serializable {
	private static final long serialVersionUID = -374039382700575277L;

	public static Pointer from(int val) {
		return new I32Pointer(val);
	}

	public static Pointer from(short val) {
		return new I16Pointer(val);
	}

	public static Pointer from(long val) {
		return new I64Pointer(val);
	}

	public static Pointer from(byte val) {
		return new I8Pointer(val);
	}

	public static Pointer from(float val) {
		return new F32Pointer(val);
	}

	public static Pointer from(double val) {
		return new F64Pointer(val);
	}

	public static Pointer from(boolean val) {
		return new BoolPointer(val);
	}

	public static Pointer from(char val) {
		return new CharPointer(val);
	}

	public static Pointer from(Object val) {
		return new ObjectPointer(val);
	}
}
