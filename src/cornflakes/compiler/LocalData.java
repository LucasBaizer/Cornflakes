package cornflakes.compiler;

import org.objectweb.asm.Label;

public class LocalData extends FieldData {
	private Label start;
	private Label end;
	private int index;

	public LocalData(String name, String type, Label start, Label end, int index, int mods) {
		super(name, type, mods);

		this.start = start;
		this.end = end;
		this.index = index;
	}

	public Label getStart() {
		return start;
	}

	public void setStart(Label start) {
		this.start = start;
	}

	public Label getEnd() {
		return end;
	}

	public void setEnd(Label end) {
		this.end = end;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}
}