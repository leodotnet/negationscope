/**
 * 
 */
package org.ns.hypergraph.decoding;

import java.util.Collection;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A min-priority queue that contains unique elements and size limit.
 * Adding an element that is already inside the queue is a no-op.
 * When adding an element causes the limit to be violated, the greatest element after the addition will
 * be removed from the queue.
 * The size limit can be set to non-positive values, in which case the queue becomes unbounded.<br>
 * The time complexity for the various operations are as follows (n is the number of elements in the queue):
 * <ul>
 * <li>{@link #add(E)}/{@link #offer(E)} - O(log n)</li>
 * <li>{@link #remove(E)}/{@link #poll(E)} - O(log n)</li>
 * <li>{@link #contains(Object)} - O(log n)</li>
 * <li>{@link #element(E)}/{@link #peek(E)} - O(1)</li>
 * </ul>
 * The underlying implementation of this queue is a {@link TreeSet}.
 */
public class BoundedUniquePriorityQueue<E> extends TreeSet<E> implements IUniquePriorityQueue<E>{

	private static final long serialVersionUID = -1713861356866461868L;
	private int limit;
	
	/**
	 * Creates an unbounded min-priority queue with unique elements.
	 */
	public BoundedUniquePriorityQueue(){
		this(0);
	}

	/**
	 * Creates a min-priority queue with unique elements and the specified maximum size.
	 * @param limit
	 */
    public BoundedUniquePriorityQueue(final int limit) {
        super();
        this.limit = limit;
    }

    /**
     * Creates a min-priority queue with unique elements from the specified collection.
     * @param limit
     * @param c
     */
    public BoundedUniquePriorityQueue(final int limit, final Collection<? extends E> c) {
        this(limit);
        addAll(c);
    }

    /**
     * Creates a min-priority queue with unique elements with the specified comparator.
     * @param limit
     * @param c
     */
    public BoundedUniquePriorityQueue(final int limit, final Comparator<? super E> comparator) {
        super(comparator);
        this.limit = limit;
    }

    /**
     * Creates a min-priority queue with unique elements from the specified set.
     * @param limit
     * @param c
     */
    public BoundedUniquePriorityQueue(final int limit, final SortedSet<E> s) {
    	this(limit);
    	addAll(s);
    }

    /**
     * Adds the specified element to this set if it is not already present.
     * More formally, adds the specified element e to this set if e is non-null and the set contains no
     * element e2 such that e.equals(e2).
     * If this set already contains the element, the call leaves the set unchanged and returns false.<br>
     * This method will also return false when the size limit is reached and it is
     * greater than the greatest element in the set.
     * @return true if the set changes as the result of this operation
     * @throws ClassCastException if the specified object cannot be compared with the elements currently in this set
     * @throws NullPointerException if the specified object is null
     */
    @Override
    public boolean add(final E e) {
    	if(e == null){
    		throw new NullPointerException("Only non-null objects are allowed.");
    	}
        boolean result = super.add(e);
        if(limit > 0 && size() > limit){
        	result = e != pollLast();
        }
        return result;
    }

    /**
     * Adds all of the elements in the specified collection to this collection (optional operation).
     * The behavior of this operation is undefined if the specified collection is modified while
     * the operation is in progress.
     * (This implies that the behavior of this call is undefined if the specified collection is
     * this collection, and this collection is nonempty.)<br>
     * This is implemented by repeatedly calling {@link #add(Object)} to each element in the collection.
     */
    @Override
    public boolean addAll(Collection<? extends E> coll) {
    	boolean result = false;
    	for(E el: coll){
    		result = result || add(el);
    	}
    	return result;
    }

    /**
     * Adds the specified element to this set if it is not already present.
     * More formally, adds the specified element e to this set if e is non-null and the set contains no
     * element e2 such that e.equals(e2).
     * If this set already contains the element, the call leaves the set unchanged and returns false.<br>
     * This method will also return false when the size limit is reached and it is
     * greater than the greatest element in the queue.
     * @param e The element to be added into the queue
     * @return true if the queue changes as the result of this operation
     * @throws ClassCastException if the specified object cannot be compared with the elements currently in this set
     * @throws NullPointerException if the specified object is null
     */
	@Override
	public boolean offer(E e) {
		return add(e);
	}

	@Override
	public E remove() {
		if(size() == 0){
			throw new NoSuchElementException("Queue is empty!");
		}
		return pollFirst();
	}

	@Override
	public E poll() {
		return pollFirst();
	}

	@Override
	public E element() {
		if(size() == 0){
			throw new NoSuchElementException("Queue is empty!");
		}
		return first();
	}

	@Override
	public E peek() {
		return first();
	}
	
	/**
	 * Resizes the queue to the specified new limit.
	 * If there are more elements currently in the queue compared to the new limit,
	 * the greatest element is repeatedly removed until the size is within the new limit.
	 * If the new limit is non-positive, the queue becomes unbounded.
	 * @param newLimit
	 * @return
	 */
	public boolean resize(final int newLimit){
		boolean result = true;
		if(newLimit > 0 && newLimit < this.limit){
			while(size() > newLimit){
				pollLast();
			}
		}
		this.limit = newLimit;
		return result;
	}

}
