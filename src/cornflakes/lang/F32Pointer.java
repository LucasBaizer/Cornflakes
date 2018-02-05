package cornflakes.lang;

public class F32Pointer extends Pointer {
	private static final long serialVersionUID = 7472427928156425700L;
	private float val;

	public F32Pointer(float val) {
		this.val = val;
	}

	public void setValue(float val) {
		this.val = val;
	}

	public float getValue() {
		return this.val;
	}

	@Override
	public String toString() {
		return String.valueOf(val);
	}

	@Override
	public int hashCode() {
		return Float.floatToIntBits(val);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof F32Pointer) {
			F32Pointer ptr = (F32Pointer) obj;
			return ptr.val == this.val;
		}
		return false;
	}
}
