package cornflakes.lang;

public class I8Pointer extends Pointer {
	private static final long serialVersionUID = 2678549531554508871L;
	private byte val;

	public I8Pointer(byte val) {
		this.val = val;
	}

	public void setValue(byte val) {
		this.val = val;
	}

	public byte getValue() {
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
		if (obj instanceof I8Pointer) {
			I8Pointer ptr = (I8Pointer) obj;
			return ptr.val == this.val;
		}
		return false;
	}
}
