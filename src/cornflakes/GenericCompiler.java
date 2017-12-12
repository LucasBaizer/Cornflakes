package cornflakes;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public interface GenericCompiler extends Opcodes {
	public int compile(ClassData data, MethodVisitor m, int num, String body, String[] lines);
}
