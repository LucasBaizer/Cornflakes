package cornflakes.lang;

import cornflakes.compiler.MainCompiler;

public class Main {
	public static void main(String[] args) throws Exception {
		MainCompiler.main(new String[] { "src" });
		Test.test();
	}
}
