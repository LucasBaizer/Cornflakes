package cornflakes.lang;

import java.util.Iterator;

/**
 * The <code>ArrayIterator</code> class is used internally in for-each loops to
 * create an iterator object to iterate over an array.
 * 
 * @author Lucas Baizer
 */
public final class ArrayIterator implements Iterator<Object> {
	private int index = 0;
	private Object[] objects;

	/**
	 * Creates a new ArrayIterator, given an array.
	 * 
	 * @param objects The array
	 */
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
