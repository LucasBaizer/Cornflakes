package cornflakes;

import java.util.regex.Pattern;

import org.objectweb.asm.MethodVisitor;

public class ReferenceCompiler implements GenericCompiler {
	private MethodData data;

	public ReferenceCompiler(MethodData data) {
		this.data = data;
	}

	@Override
	public int compile(ClassData data, MethodVisitor m, int num, String body, String[] lines) {
		String[] operandSplit = body.split(Pattern.quote("."));

		for (String part : operandSplit) {
			
		}

		return num;
	}
}
