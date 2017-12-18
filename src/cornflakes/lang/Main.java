package cornflakes.lang;

import java.util.concurrent.ThreadLocalRandom;

import cornflakes.compiler.MainCompiler;

public class Main {
	public static void main(String[] args) throws Exception {
		MainCompiler.main(args);
		
		int x = ThreadLocalRandom.current().nextInt();
		
		int y = x & 155;
	}
}
