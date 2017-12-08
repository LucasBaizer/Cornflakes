package cornflakes;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class FunctionCompiler extends Compiler {
	@Override
	public void compile(ClassData data, ClassWriter cw, String body, String[] lines) {
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "()V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitLineNumber(7, l0);
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitLdcInsn("Hello, world!");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLineNumber(8, l1);
		mv.visitInsn(RETURN);
		Label l2 = new Label();
		mv.visitLabel(l2);
		// mv.visitLocalVariable("args", "[Ljava/lang/String;", null, l0,
		// l2, 0);
		mv.visitMaxs(2, 1);
		mv.visitEnd();
	}
}
