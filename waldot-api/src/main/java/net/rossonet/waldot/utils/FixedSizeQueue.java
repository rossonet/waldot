package net.rossonet.waldot.utils;

import java.util.ArrayDeque;
import java.util.Deque;

public class FixedSizeQueue<E> {
	private final Deque<E> deque;
	private final int maxSize;

	public FixedSizeQueue(int maxSize) {
		this.maxSize = maxSize;
		this.deque = new ArrayDeque<>(maxSize);
	}

	public void add(E element) {
		if (deque.size() == maxSize) {
			deque.removeFirst();
		}
		deque.addLast(element);
	}

	public E poll() {
		return deque.pollFirst();
	}

	public int size() {
		return deque.size();
	}
}
