package cornflakes.lang;

public abstract class Range {
	public static I32Range from(int min, int max) {
		return new I32Range(min, max);
	}
	
	public static I32Range from(int min, int max, int increment) {
		return new I32Range(min, max, increment);
	}
	
	public static F32Range from(float min, float max) {
		return new F32Range(min, max);
	}
	
	public static F32Range from(float min, float max, float increment) {
		return new F32Range(min, max, increment);
	}
}
