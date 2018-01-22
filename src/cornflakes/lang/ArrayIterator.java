package cornflakes.lang;

import java.util.Iterator;

public final class ArrayIterator implements Iterator<Object> {
	private int index = 0;
	private Object[] objects;

	public ArrayIterator(Object[] objects) {
		this.objects = objects;
	}

	@Override
	public boolean hasNext() {
		return index < objects.length;
	}

	@Override
	public Object next() {
		return objects[index++];
	}
}
