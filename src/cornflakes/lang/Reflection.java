package cornflakes.lang;

import java.lang.reflect.Member;

public class Reflection {
	public static boolean hasModifier(Member member, String mod) {
		if (mod == null || member == null) {
			throw new NullPointerException();
		}

		int acc = member.getModifiers();
		int a = toFlag(mod);
		return (acc & a) == a;
	}

	public static int toFlag(String mod) {
		switch (mod) {
			case "public":
				return 1;
			case "private":
				return 2;
			case "protected":
				return 4;
			case "static":
				return 8;
			case "const":
			case "final":
				return 16;
			case "sync":
				return 32;
			case "volatile":
				return 64;
			case "transient":
				return 128;
			case "native":
				return 256;
			case "default":
				return 512;
			case "abstract":
				return 1024;
			case "strictfp":
				return 2048;
			case "synthetic":
				return 4096;
			default:
				throw new IllegalArgumentException(mod);
		}
	}
}
