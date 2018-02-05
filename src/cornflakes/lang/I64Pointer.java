package cornflakes.lang;

public class I64Pointer extends Pointer {
	private static final long serialVersionUID = 7728877480676918055L;
	private long val;

	public I64Pointer(long val) {
		this.val = val;
	}

	public void setValue(long val) {
		this.val = val;
	}

	public long getValue() {
		return this.val;
	}

	@Override
	public String toString() {
		return String.valueOf(val);
	}

	@Override
	public int hashCode() {
		return (int) (val ^ (val >>> 32));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof I64Pointer) {
			I64Pointer ptr = (I64Pointer) obj;
			return ptr.val == this.val;
		}
		return false;
	}
}
