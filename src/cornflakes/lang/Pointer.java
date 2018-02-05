package cornflakes.lang;

import java.io.Serializable;

public abstract class Pointer implements Serializable {
	private static final long serialVersionUID = -374039382700575277L;

	public static Pointer from(int val) {
		return new I32Pointer(val);
	}
	
	public static Pointer from(Object val) {
		return new ObjectPointer(val);
	}
}
