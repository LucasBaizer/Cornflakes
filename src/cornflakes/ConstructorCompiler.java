package cornflakes;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class ConstructorCompiler extends Compiler {
	@Override
	public void compile(ClassData data, ClassWriter cw, String body, String[] lines) {
	}

	public void compileDefault(ClassData data, ClassWriter cw) {
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitLineNumber(4, l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, Strings.transformClassName(data.getParentName()), "<init>", "()V", false);
		mv.visitInsn(RETURN);
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", "L" + Strings.transformClassName(data.getClassName()) + ";", null, l0, l1, 0);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
	}
}
