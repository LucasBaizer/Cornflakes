package cornflakes.lang;

import java.util.ArrayList;

import cornflakes.compiler.MainCompiler;

public class Main {
	public static void main(String[] args) throws Exception {
		MainCompiler.main(args);
		HelloWorld.main(args);

		ArrayList<Object> list = new ArrayList<>();
	}
}
