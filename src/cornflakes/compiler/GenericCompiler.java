package cornflakes.compiler;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public interface GenericCompiler extends Opcodes {
	public int compile(ClassData data, MethodVisitor m, Label startLabel, Label endLabel, int num, String body, String[] lines);
}
