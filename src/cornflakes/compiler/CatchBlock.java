package cornflakes.compiler;

import org.objectweb.asm.Label;

public class CatchBlock extends Block {
	private DefinitiveType exceptionType;

	public CatchBlock(int start, Label slabel, Label elabel) {
		super(start, slabel, elabel);
	}

	public DefinitiveType getExceptionType() {
		return exceptionType;
	}

	public void setExceptionType(DefinitiveType exceptionType) {
		this.exceptionType = exceptionType;
	}
}
