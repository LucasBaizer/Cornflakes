package cornflakes.lang;

public class I32Pointer extends Pointer {
	private static final long serialVersionUID = -6435560814040737047L;
	private int val;

	public I32Pointer(int val) {
		this.val = val;
	}

	public void setValue(int val) {
		this.val = val;
	}

	public int getValue() {
		return this.val;
	}

	@Override
	public String toString() {
		return String.valueOf(val);
	}

	@Override
	public int hashCode() {
		return val;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof I32Pointer) {
			I32Pointer ptr = (I32Pointer) obj;
			return ptr.val == this.val;
		}
		return false;
	}
}
