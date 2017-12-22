package cornflakes.lang;

import cornflakes.compiler.MainCompiler;
import cornflakes.compiler.cfs.CFSMain;

public class Main {
	public static void main(String[] args) throws Exception {
		MainCompiler.main(args);
		CFSMain.main(null);
	}
}
