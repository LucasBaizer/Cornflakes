package cornflakes.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * An immutable iterator.
 */
public class FunctionalIterator<T> implements Iterator<T> {
	private List<T> list = new ArrayList<>();

	public FunctionalIterator() {
	}

	@SafeVarargs
	public FunctionalIterator(T... items) {
		list = new ArrayList<>(Arrays.asList(items));
	}

	public FunctionalIterator(Iterable<T> itr) {
		this(itr.iterator());
	}

	public FunctionalIterator(Iterator<T> itr) {
		while (itr.hasNext()) {
			list.add(itr.next());
		}
	}

	public FunctionalIterator(Collection<T> col) {
		list = new ArrayList<>(col);
	}

	@Override
	public boolean hasNext() {
		return list.size() > 0;
	}

	@Override
	public T next() {
		return list.size() == 0 ? null : list.remove(0);
	}

	public FunctionalIterator<T> add(T obj) {
		list.add(obj);

		return this;
	}

	public FunctionalIterator<T> skip() {
		return skip(1);
	}

	public FunctionalIterator<T> skip(int amount) {
		FunctionalIterator<T> copy = copy();
		for (int i = 0; i < amount; i++) {
			checkEmpty(copy.list);
			copy.list.remove(0);
		}
		return copy;
	}

	public FunctionalIterator<T> before() {
		return before(1);
	}

	public FunctionalIterator<T> before(int amount) {
		FunctionalIterator<T> copy = copy();
		for (int i = 0; i < amount; i++) {
			checkEmpty(copy.list);
			copy.list.remove(copy.list.size() - 1);
		}
		return copy;
	}

	public FunctionalIterator<T> reverse() {
		FunctionalIterator<T> copy = copy();
		for (int i = copy.list.size() - 1; i >= 0; i--) {
			checkEmpty(copy.list);
			copy.list.remove(0);
		}
		return copy;
	}

	public T first() {
		checkEmpty(list);

		return list.get(0);
	}

	public T last() {
		checkEmpty(list);

		return list.get(list.size() - 1);
	}

	public T at(int idx) {
		if (idx >= list.size()) {
			throw new IteratorException("Index exceeds iterator size");
		}
		return list.get(idx);
	}

	@SuppressWarnings("unchecked")
	public T[] toArray() {
		return (T[]) list.toArray(new Object[list.size()]);
	}

	public List<T> toList() {
		return Arrays.asList(toArray());
	}

	public Stream<T> toStream() {
		return Stream.of(this.toArray());
	}

	public FunctionalIterator<T> zip(FunctionalIterator<T> other) {
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

	public int getLength() {
		return list.size();
	}

	private FunctionalIterator<T> copy() {
		FunctionalIterator<T> iter = new FunctionalIterator<>();
		iter.list = new ArrayList<>(this.list);
		return iter;
	}

	private void checkEmpty(List<T> list) {
		if (list.size() == 0) {
			throw new IteratorException("Iterator is empty");
		}
	}
}
