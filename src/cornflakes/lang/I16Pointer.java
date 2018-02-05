package cornflakes.lang;

public class I16Pointer extends Pointer {
	private static final long serialVersionUID = -866473203367921503L;
	private short val;

	public I16Pointer(short val) {
		this.val = val;
	}

	public void setValue(short val) {
		this.val = val;
	}

	public short getValue() {
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
		if (obj instanceof I16Pointer) {
			I16Pointer ptr = (I16Pointer) obj;
			return ptr.val == this.val;
		}
		return false;
	}
}
