package cornflakes;

import java.util.Scanner;

public class Console {
	private static final Scanner in = new Scanner(System.in);

	public static void log(String x) {
		System.out.println(x);
	}

	public static String readLine() {
		return in.nextLine();
	}
}
