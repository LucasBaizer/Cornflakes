package cornflakes.lang;

import cornflakes.compiler.MainCompiler;

public class Main {
	public static void main(String[] args) throws Exception {
		MainCompiler.main(new String[] { "src" });

		/*
		 * Thread thread = new Thread(() -> { MainServer.main(new String[0]);
		 * }); thread.start(); TestClient.main(new String[0]);
		 */
		// PointerTest.main(new String[0]);
	}
}
