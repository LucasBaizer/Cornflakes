package cornflakes.compiler;

import org.objectweb.asm.Label;

public class LocalData {
	private Label start;
	private Label end;
	private String name;
	private String type;
	private int index;
	private int modifiers;

	public LocalData(String name, String type, Label start, Label end, int index, int mods) {
		this.name = name;
		this.type = type;
		this.start = start;
		this.end = end;
		this.index = index;
		this.modifiers = mods;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getModifiers() {
		return modifiers;
	}

	public void setModifiers(int modifiers) {
		this.modifiers = modifiers;
	}

	public boolean hasModifier(int mod) {
		return (this.modifiers & mod) == mod;
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

	@Override
	public String toString() {
		return name + "(" + type + "), starts=" + start.getOffset() + ", end=" + end.getOffset();
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}
}
