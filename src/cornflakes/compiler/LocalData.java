package cornflakes.compiler;

public class LocalData extends FieldData {
	private Block block;
	private int index;

	public LocalData(String name, String type, Block block, int index, int mods) {
		super(name, type, mods);

		this.setBlock(block);
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public Block getBlock() {
		return block;
	}

	public void setBlock(Block block) {
		this.block = block;
	}
}