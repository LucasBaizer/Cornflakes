package cornflakes.lang;

import java.util.Objects;

public class ObjectPointer extends Pointer {
	private static final long serialVersionUID = -8973464555906809564L;
	private Object val;

	public ObjectPointer(Object val) {
		this.val = val;
	}

	public void setValue(Object val) {
		this.val = val;
	}

	public Object getValue() {
		return this.val;
	}

	@Override
	public String toString() {
		return String.valueOf(val);
	}

	@Override
	public int hashCode() {
		return val.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ObjectPointer) {
			ObjectPointer ptr = (ObjectPointer) obj;
			return Objects.equals(val, ptr.val);
		}
		return false;
	}
}
