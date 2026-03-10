package net.rossonet.waldot.utils;

import java.util.ArrayDeque;
import java.util.Deque;

public class FixedSizeQueue<E> {
	private final Deque<E> deque;
	private final int maxSize;

	/**
	 * Creates a FixedSizeQueue with the specified maximum size.
	 * 
	 * @param maxSize the maximum number of elements the queue can hold
	 */
	public FixedSizeQueue(int maxSize) {
		this.maxSize = maxSize;
		this.deque = new ArrayDeque<>(maxSize);
	}

	/**
	 * Adds an element to the queue.
	 * If the queue is at maximum capacity, removes the oldest element first.
	 * 
	 * @param element the element to add
	 */
	public void add(E element) {
		if (deque.size() == maxSize) {
			deque.removeFirst();
		}
		deque.addLast(element);
	}

	/**
	 * Retrieves and removes the oldest element from the queue.
	 * 
	 * @return the oldest element, or null if the queue is empty
	 */
	public E poll() {
		return deque.pollFirst();
	}

	/**
	 * Returns the number of elements in the queue.
	 * 
	 * @return the current size of the queue
	 */
	public int size() {
		return deque.size();
	}
}
