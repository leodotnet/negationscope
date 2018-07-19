/**
 * 
 */
package org.ns.hypergraph;

import java.io.Serializable;
import java.util.Set;

import gnu.trove.impl.Constants;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * A class to store string as integers<br>
 * This class is not thread-safe.
 * @author Aldrian Obaja (aldrianobaja.m@gmail.com)
 */
public class StringIndex implements Serializable {
	
	private static final long serialVersionUID = 4121856507993725358L;
	private TObjectIntHashMap<String> index;
	private String[] array;
	private boolean locked;
	
	/**
	 * Creates an empty StringIndex with default capacity.
	 */
	public StringIndex(){
		index = new TObjectIntHashMap<String>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1);
		locked = false;
	}

	/**
	 * Creates a StringIndex with the specified capacity.
	 * @param capacity
	 */
	public StringIndex(int capacity) {
		index = new TObjectIntHashMap<String>(capacity, Constants.DEFAULT_LOAD_FACTOR, -1);
		locked = false;
	}
	
	/**
	 * Returns the integer value associated with the specified string, 
	 * assigning it the next available index if it is not already in the index.<br>
	 * If this index is locked and the string is not found, it will return -1.
	 * @param str
	 * @return
	 * @see #put(String)
	 */
	public int getOrPut(String str){
		if(!index.containsKey(str)){
			if(locked){
				return -1;
			}
			index.put(str, index.size());
		}
		return index.get(str);
	}
	
	/**
	 * Returns the integer value associated with the specified string, 
	 * assigning it the next available index if it is not already in the index.<br>
	 * If this index is locked and the string is not found, it will return -1.
	 * @param str
	 * @return
	 * @see #getOrPut(String)
	 */
	public int put(String str){
		return getOrPut(str);
	}
	
	/**
	 * Put all the strings in the given index into current index.<br>
	 * If this index is locked and a string is not found, it will not be inserted.
	 * @param index
	 */
	public void putAll(StringIndex index){
		for(String key: index.keys()){
			getOrPut(key);
		}
	}
	
	/**
	 * Force put the specified (str -> value) mapping<br>
	 * <strong>IMPORTANT</strong>: The use of this method is discouraged, as it might make the index skip an integer value.
	 * For normal usage, consider {@link #getOrPut(String)}.<br>
	 * This method is also not affected by lock status of this index.
	 * @param str
	 * @param value
	 * @return
	 */
	public int forcePut(String str, int value){
		return index.put(str, value);
	}
	
	/**
	 * Returns the id of the given string.<br>
	 * If the string is not found, it will return -1.
	 * @param str
	 * @return
	 */
	public int get(String str){
		return index.get(str);
	}
	
	/**
	 * Lock this index, preventing new strings to be added.
	 */
	public void lock(){
		locked = true;
	}
	
	/**
	 * Unlock this index, enabling it to accept new strings.
	 */
	public void unlock(){
		locked = false;
	}
	
	/**
	 * Whether this index has the reverse index built.
	 * @return
	 */
	public boolean hasReverseIndex(){
		return array != null;
	}
	
	/**
	 * Builds the reverse index, enabling the method {@link #get(int)}.
	 * @see #get(int)
	 */
	public void buildReverseIndex(){
		array = new String[index.size()];
		for(Object key: index.keys()){
			array[index.get(key)] = (String)key;
		}
	}
	
	/**
	 * Removes the reverse index, disabling the method {@link #get(int)}.
	 * @see #get(int)
	 */
	public void removeReverseIndex(){
		array = null;
	}
	
	/**
	 * Returns the string mapped to the given id.<br>
	 * <strong>NOTE</strong>: This method can only return strings registered before the 
	 * last call to {@link #buildReverseIndex()}
	 * @param id The id to be looked up in the index.
	 * @return
	 */
	public String get(int id){
		return array[id];
	}
	
	/**
	 * Returns the size of this index (i.e., the number of strings registered).
	 * @return
	 */
	public int size(){
		return this.index.size();
	}

    /**
     * Returns a {@link Set} view of the keys contained in this index.
     * The set is backed by the index, so changes to the index are
     * reflected in the set, and vice-versa.  If the index is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation), the results of
     * the iteration are undefined.  The set supports element removal,
     * which removes the corresponding mapping from the index, via the
     * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
     * operations.  It does not support the <tt>add</tt> or <tt>addAll</tt>
     * operations.
     *
     * @return a set view of the keys contained in this map
     */
	public Set<String> keys(){
		return index.keySet();
	}

    /**
     * @return an iterator over the entries in this map
     */
	public TObjectIntIterator<String> iterator(){
		return index.iterator();
	}
	
	/**
	 * Creates a new index from the given indexes.
	 * @param indexes
	 * @return
	 */
	public static StringIndex merge(StringIndex... indexes){
		int totalSize = 0;
		for(int i=0; i<indexes.length; i++){
			totalSize += indexes[i].size(); // Usually there are not much overlap
		}
		StringIndex result = new StringIndex(totalSize);
		for(StringIndex index: indexes){
			result.putAll(index);
		}
		return result;
	}

}
