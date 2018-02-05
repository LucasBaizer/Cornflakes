package cornflakes.lang;

public class F64Pointer extends Pointer {
	private static final long serialVersionUID = -4077871289905340566L;
	private double val;

	public F64Pointer(double val) {
		this.val = val;
	}

	public void setValue(double val) {
		this.val = val;
	}

	public double getValue() {
		return this.val;
	}

	@Override
	public String toString() {
		return String.valueOf(val);
	}

	@Override
	public int hashCode() {
		long b = Double.doubleToLongBits(val);
		return (int) (b ^ (b >>> 32));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof F64Pointer) {
			F64Pointer ptr = (F64Pointer) obj;
			return ptr.val == this.val;
		}
		return false;
	}
}
