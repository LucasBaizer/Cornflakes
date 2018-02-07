package cornflakes.compiler;

import org.objectweb.asm.Label;

public class ConstructorBlock extends Block {
	public ConstructorBlock(int start, Label slabel, Label elabel) {
		super(start, slabel, elabel);
	}

	private boolean calledConstructor;

	public boolean hasCalledConstructor() {
		return calledConstructor;
	}

	public void setCalledConstructor(boolean calledConstructor) {
		this.calledConstructor = calledConstructor;
	}
}
