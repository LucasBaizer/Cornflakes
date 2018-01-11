package cornflakes.lang;

import cornflakes.compiler.MainCompiler;
import cornflakes.compiler.cfs.CFSTestClass;

public class Main {
	public static void main(String[] args) throws Exception {
		MainCompiler.main(args);
		CFSTestClass.main(args);
	}
}
