package cornflakes.lang;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class YieldIterator implements Iterator<Object>, Iterable<Object> {
	private List<Object> list = new ArrayList<>();
	
	public YieldIterator() {
	}

	@Override
	public boolean hasNext() {
		return list.size() > 0;
	}

	@Override
	public Object next() {
		return list.size() == 0 ? null : list.remove(0);
	}

	public void yield(Object obj) {
		list.add(obj);
	}

	@Override
	public Iterator<Object> iterator() {
		return this;
	}
}
