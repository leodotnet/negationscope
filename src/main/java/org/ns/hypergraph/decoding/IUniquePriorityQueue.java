/**
 * 
 */
package org.ns.hypergraph.decoding;

import java.util.Queue;

/**
 * An interface defining a priority queue with unique elements.
 */
public interface IUniquePriorityQueue<T> extends Queue<T> {

	/**
	 * Inserts the specified element into this priority queue.<br>
	 * If the element already exists in the queue, it is not added into the queue.
	 * 
	 * @param e
	 * 		The element to be inserted
	 * @return {@code true} if the element was added to this queue, else {@code false}
	 */
	public boolean add(T e);

	/**
	 * Inserts the specified element into this priority queue.<br>
	 * If the element already exists in the queue, it is not added into the queue.
	 * 
	 * @param e
	 * 		The element to be inserted
	 * @return {@code true} if the element was added to this queue, else {@code false}
	 */
	public boolean offer(T e);
}
