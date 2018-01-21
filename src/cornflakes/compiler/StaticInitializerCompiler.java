package cornflakes.compiler;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class StaticInitializerCompiler extends Compiler {
	@Override
	public void compile(ClassData data, ClassWriter cw, String body, String[] lines) {
		boolean create = false;

		for (FieldData datum : data.getFields()) {
			if (datum.hasModifier(ACC_STATIC) && datum.getProposedData() != null) {
				create = true;
				break;
			}
		}

		if (!create) {
			return;
		}

		MethodVisitor m = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
		m.visitCode();

		MethodData method = new MethodData(data, "<clinit>", DefinitiveType.primitive("V"), false, ACC_STATIC);

		Label start = new Label();
		Label post = new Label();

		m.visitLabel(start);
		m.visitLineNumber(0, start);

		Block block = new Block(0, start, post);
		for (FieldData datum : data.getFields()) {
			if (datum.hasModifier(ACC_STATIC) && datum.getProposedData() != null) {
				String type = datum.getType().getTypeSignature();

				if (Types.isPrimitive(type) || type.equals("Ljava/lang/String;")) {
					int push = Types.getOpcode(Types.PUSH, type);
					if (push == LDC) {
						m.visitLdcInsn(datum.getProposedData());
						method.ics();
					} else {
						m.visitVarInsn(push, Integer.parseInt(datum.getProposedData().toString()));
						method.ics();
					}

					m.visitFieldInsn(PUTSTATIC, data.getClassName(), datum.getName(),
							datum.getType().getTypeSignature());
				} else {
					String raw = (String) datum.getProposedData();

					ExpressionCompiler compiler = new ExpressionCompiler(true, method);
					compiler.compile(data, m, block, raw, new String[] { raw });

					if (!Types.isSuitable(datum.getType().getTypeSignature(),
							compiler.getReferenceType().getTypeSignature())) {
						throw new CompileError(
								compiler.getReferenceType() + " is not assignable to " + datum.getType());
					}

					m.visitFieldInsn(PUTSTATIC, data.getClassName(), datum.getName(),
							datum.getType().getTypeSignature());
				}
			}
		}

		m.visitInsn(RETURN);

		m.visitMaxs(method.getStackSize(), 0); // TODO 128
		m.visitEnd();
	}
}
