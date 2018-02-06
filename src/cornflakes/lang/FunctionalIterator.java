package cornflakes.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * The <code>FunctionalIterator</code> class is an iterator implementation which
 * has several convenience functions commonly found in other languages. It is
 * comparable to the new Java 8 <code>Stream</code> structure.
 * 
 * The FunctionalIterator is partially immutable. All functions, except for the
 * {@link cornflakes.lang.FunctionalIterator#next() next()} function, return a
 * new iterator and do not modify the original.
 */
public class FunctionalIterator<T> implements Iterator<T> {
	private List<T> list = new ArrayList<>();

	/**
	 * Creates a new empty iterator.
	 */
	public FunctionalIterator() {
	}

	/**
	 * Creates a new iterator given a series of initial values.
	 * 
	 * @param items
	 *            The initial values
	 */
	@SafeVarargs
	public FunctionalIterator(T... items) {
		list = new ArrayList<>(Arrays.asList(items));
	}

	/**
	 * Creates a new iterator given an <code>Iterable</code> object.
	 * 
	 * @param itr
	 *            The object of which to copy to elements of
	 */
	public FunctionalIterator(Iterable<T> itr) {
		this(itr.iterator());
	}

	/**
	 * Creates a new iterator given an <code>Iterator</code> object.
	 * 
	 * @param itr
	 *            The iterator of which to copy to elements of
	 */
	public FunctionalIterator(Iterator<T> itr) {
		while (itr.hasNext()) {
			list.add(itr.next());
		}
	}

	/**
	 * Creates a new iterator given a <code>Collection</code> object.
	 * 
	 * @param itr
	 *            The object of which to copy to elements of
	 */
	public FunctionalIterator(Collection<T> col) {
		list = new ArrayList<>(col);
	}

	@Override
	public boolean hasNext() {
		return list.size() > 0;
	}

	/**
	 * Returns the next item in the iterator. This mutates the iterator that the
	 * function is called on by removing the next object, and then returning the
	 * removed object.
	 * 
	 * @return the next item in the iterator, or <code>null</code> if the
	 *         iterator is empty
	 */
	@Override
	public T next() {
		return list.size() == 0 ? null : list.remove(0);
	}

	/**
	 * Adds an object to a copy iterator and returns it.
	 * 
	 * @param obj
	 *            The object to add
	 * @return The copied iterator with the added object
	 */
	public FunctionalIterator<T> add(T obj) {
		FunctionalIterator<T> copy = copy();
		copy.list.add(obj);

		return copy;
	}

	/**
	 * Skips the first object in a copy iterator. See
	 * {@link cornflakes.lang.FunctionalIterator#skip(int) skip(int)}.
	 * 
	 * @return The copied iterator with the first element removed
	 */
	public FunctionalIterator<T> skip() {
		return skip(1);
	}

	/**
	 * Skips a given amount of objects at the start of a copy iterator.
	 * 
	 * @param amount
	 *            The amount of objects to skip
	 * @return The copied iterator with the objects removed
	 */
	public FunctionalIterator<T> skip(int amount) {
		FunctionalIterator<T> copy = copy();
		for (int i = 0; i < amount; i++) {
			checkEmpty(copy.list);
			copy.list.remove(0);
		}
		return copy;
	}

	/**
	 * Skips the last object in a copy iterator. See
	 * {@link cornflakes.lang.FunctionalIterator#before(int) before(int)}.
	 * 
	 * @return The copied iterator with the last element removed
	 */
	public FunctionalIterator<T> before() {
		return before(1);
	}

	/**
	 * Skips a given amount of objects at the end of a copy iterator.
	 * 
	 * @param amount
	 *            The amount of objects to skip
	 * @return The copied iterator with the objects removed
	 */
	public FunctionalIterator<T> before(int amount) {
		FunctionalIterator<T> copy = copy();
		for (int i = 0; i < amount; i++) {
			checkEmpty(copy.list);
			copy.list.remove(copy.list.size() - 1);
		}
		return copy;
	}

	/**
	 * Creates a copy iterator and reverses all the items in it.
	 * 
	 * @return The reversed copy iterator
	 */
	public FunctionalIterator<T> reverse() {
		FunctionalIterator<T> copy = copy();
		for (int i = copy.list.size() - 1; i >= 0; i--) {
			checkEmpty(copy.list);
			copy.list.remove(0);
		}
		return copy;
	}

	/**
	 * @return The first object in the iterator
	 * @throws IteratorException
	 *             If the iterator is empty
	 */
	public T first() throws IteratorException {
		checkEmpty(list);

		return list.get(0);
	}

	/**
	 * @return The last object in the iterator
	 * @throws IteratorException
	 *             If the iterator is empty
	 */
	public T last() throws IteratorException {
		checkEmpty(list);

		return list.get(list.size() - 1);
	}

	/**
	 * Returns an object at a given index in the iterator.
	 * 
	 * @param idx
	 *            The index to query
	 * @return The object at the given index
	 * @throws IteratorException
	 *             If the index exceeds the iterator's size
	 */
	public T at(int idx) throws IteratorException {
		if (idx >= list.size()) {
			throw new IteratorException("Index exceeds iterator size");
		}
		return list.get(idx);
	}

	/**
	 * @return All the elements in the iterator into an array
	 */
	@SuppressWarnings("unchecked")
	public T[] toArray() {
		return (T[]) list.toArray(new Object[list.size()]);
	}

	/**
	 * @return All the elements in the iterator into an immutable list
	 */
	public List<T> toList() {
		return Arrays.asList(toArray());
	}

	/**
	 * @return All the elements in the iterator into a stream
	 */
	public Stream<T> toStream() {
		return Stream.of(this.toArray());
	}

	/**
	 * Zips two iterators together. Given two iterators of equal size, this
	 * function will return a new iterator which contains the first item in the
	 * first iterator followed by the first item in the second iterator,
	 * followed by the second item in the first iterator followed by the second
	 * item in the second iterator, etc.
	 * 
	 * @param other
	 *            The iterator to zip the current iterator with
	 * @return A new iterator, which contains the zip of the current iterator
	 *         the given iterator
	 * @throws IteratorException
	 *             If either iterator is empty, or if the iterators have an
	 *             inequal length
	 */
	public FunctionalIterator<T> zip(FunctionalIterator<T> other) throws IteratorException {
		checkEmpty(list);
		other.checkEmpty(other.list);

		if (this.getLength() != other.getLength()) {
			throw new IteratorException("Cannot zip iterators of inequal length");
		}
		FunctionalIterator<T> result = new FunctionalIterator<>();
		while (hasNext()) {
			result.add(next());
			result.add(other.next());
		}
		return result;
	}

	/**
	 * @return the amount of elements in the iterator
	 */
	public int getLength() {
		return list.size();
	}

	private FunctionalIterator<T> copy() {
		FunctionalIterator<T> iter = new FunctionalIterator<>();
		iter.list = new ArrayList<>(this.list);
		return iter;
	}

	private void checkEmpty(List<T> list) throws IteratorException {
		if (list.size() == 0) {
			throw new IteratorException("Iterator is empty");
		}
	}
}
