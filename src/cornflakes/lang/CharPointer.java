package cornflakes.lang;

public class CharPointer extends Pointer {
	private static final long serialVersionUID = 2990653635239705286L;
	private char val;

	public CharPointer(char val) {
		this.val = val;
	}

	public void setValue(char val) {
		this.val = val;
	}

	public char getValue() {
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
		if (obj instanceof CharPointer) {
			CharPointer ptr = (CharPointer) obj;
			return ptr.val == this.val;
		}
		return false;
	}
}
