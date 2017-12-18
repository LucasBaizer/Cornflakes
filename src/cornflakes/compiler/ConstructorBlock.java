package cornflakes.compiler;

import org.objectweb.asm.Label;

public class ConstructorBlock extends Block {
	public ConstructorBlock(int start, Label slabel, Label elabel) {
		super(start, slabel, elabel);
	}

	private boolean calledSuper;

	public boolean hasCalledSuper() {
		return calledSuper;
	}

	public void setCalledSuper(boolean calledSuper) {
		this.calledSuper = calledSuper;
	}
}
