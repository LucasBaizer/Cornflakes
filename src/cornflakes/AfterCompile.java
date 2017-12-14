package cornflakes;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassWriter;

public class AfterCompile {
	private ClassWriter cw;
	private ClassData data;
	private List<PostCompiler> compilers = new ArrayList<>();
	
	public AfterCompile(ClassWriter cw, ClassData data) {
		this.cw = cw;
		this.data = data;
	}
	
	public void finish() {
		List<PostCompiler> clone = new ArrayList<>(compilers);
		
		compilers.clear();
		
		for(PostCompiler compiler : clone) {
			compiler.write();
		}
	}
	
	public void end() {
		if (!data.hasConstructor()) {
			new ConstructorCompiler().compileDefault(data, cw);
		}

		cw.visitEnd();
		data.setByteCode(cw.toByteArray());		
	}

	public List<PostCompiler> getCompilers() {
		return compilers;
	}

	public void setCompilers(List<PostCompiler> compilers) {
		this.compilers = compilers;
	}
}
