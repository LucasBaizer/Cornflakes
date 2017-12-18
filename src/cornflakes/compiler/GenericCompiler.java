package cornflakes.compiler;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public interface GenericCompiler extends Opcodes {
	public void compile(ClassData data, MethodVisitor m, Block block, String body, String[] lines);
}
