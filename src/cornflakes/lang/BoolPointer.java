package cornflakes.lang;

public class BoolPointer extends Pointer {
	private static final long serialVersionUID = -3032961738606851781L;
	private boolean val;

	public BoolPointer(boolean val) {
		this.val = val;
	}

	public void setValue(boolean val) {
		this.val = val;
	}

	public boolean getValue() {
		return this.val;
	}

	@Override
	public String toString() {
		return String.valueOf(val);
	}

	@Override
	public int hashCode() {
		return val ? 1 : 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BoolPointer) {
			BoolPointer ptr = (BoolPointer) obj;
			return ptr.val == this.val;
		}
		return false;
	}
}
