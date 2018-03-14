package cornflakes.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.objectweb.asm.Label;

public class TryBlock extends Block {
	private List<CatchBlock> catchBlocks = new ArrayList<>();

	public TryBlock(int start, Label slabel, Label elabel) {
		super(start, slabel, elabel);
	}

	public void appendCatchBlock(CatchBlock block) {
		catchBlocks.add(block);
	}

	public List<DefinitiveType> getHandledExceptions() {
		return catchBlocks.stream().map(e -> e.getExceptionType()).collect(Collectors.toList());
	}
}
