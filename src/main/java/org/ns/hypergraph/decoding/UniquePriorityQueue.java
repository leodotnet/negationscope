/**
 * 
 */
package org.ns.hypergraph.decoding;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * An extension of {@link PriorityQueue} that only accepts unique elements in the queue.
 */
public class UniquePriorityQueue<E> extends PriorityQueue<E> implements IUniquePriorityQueue<E> {
	
	private static final long serialVersionUID = -2850348252649385868L;

    /**
     * Creates a {@code UniquePriorityQueue} with the default initial
     * capacity (11) that orders its elements according to their
     * {@linkplain Comparable natural ordering}.
     */
	public UniquePriorityQueue(){
		super();
	}

    /**
     * Creates a {@code UniquePriorityQueue} with the specified initial
     * capacity that orders its elements according to their
     * {@linkplain Comparable natural ordering}.
     *
     * @param  initialCapacity the initial capacity for this priority queue
     * @throws IllegalArgumentException if {@code initialCapacity} is
     *         less than 1
     */
	public UniquePriorityQueue(int initialCapacity) {
		super(initialCapacity);
	}

    /**
     * Creates a {@code UniquePriorityQueue} with the specified initial capacity
     * that orders its elements according to the specified comparator.
     *
     * @param  initialCapacity the initial capacity for this priority queue
     * @param  comparator the comparator that will be used to order this
     *         priority queue.  If {@code null}, the {@linkplain Comparable
     *         natural ordering} of the elements will be used.
     * @throws IllegalArgumentException if {@code initialCapacity} is
     *         less than 1
     */
	public UniquePriorityQueue(int initialCapacity, Comparator<? super E> comparator){
		super(initialCapacity, comparator);
	}

	@Override
	public boolean add(E e){
		return offer(e);
	}

	@Override
	public boolean offer(E e){
		if(contains(e)){
			return false;
		}
		super.offer(e);
		return true;
	}

}
