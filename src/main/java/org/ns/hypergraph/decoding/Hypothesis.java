/**
 * 
 */
package org.ns.hypergraph.decoding;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.ns.hypergraph.NetworkConfig;

/**
 * This class represents a (possibly partial) hypothesis of
 * an output structure.
 * There are two types of Hypothesis: {@link NodeHypothesis} and {@link EdgeHypothesis}
 */
public abstract class Hypothesis {

	/**
	 * The node index in which this hypothesis is applicable
	 * For NodeHypothesis, this represents that node's index.
	 * For EdgeHypothesis, this represents the node index of the parent node.
	 */
	protected int nodeIndex;
	/**
	 * The children of this hypothesis, which is the previous partial hypothesis.
	 * For EdgeHypothesis, the children would be a list of NodeHypothesis.
	 * Similarly, for NodeHypothesis, the children would be a list of EdgeHypothesis.
	 */
	protected Hypothesis[] children;
	/**
	 * Whether there are more hypothesis to be predicted.
	 * Note that this is different from simply checking whether the queue is empty,
	 * because the queue is populated only when necessary by looking at the last best index.
	 */
	protected boolean hasMoreHypothesis;
	/**
	 * The priority queue storing the possible next best child.<br>
	 * Since this is a priority queue, the next best child is the one in front of the queue.<br>
	 * This priority queue will always contains unique elements.
	 */
	protected IUniquePriorityQueue<ScoredIndex> nextBestChildQueue;
	/**
	 * The cache to store the list of best children, which will contain the list of 
	 * best children up to the highest k on which {@link #getKthBestHypothesis(int)} has been called.
	 */
	protected List<ScoredIndex> bestChildrenList;

	protected void init() {
		if(NetworkConfig.PRIORITY_QUEUE_SIZE_LIMIT > 0){
			// Create a bounded priority queue
			nextBestChildQueue = new BoundedUniquePriorityQueue<ScoredIndex>(NetworkConfig.PRIORITY_QUEUE_SIZE_LIMIT);
		} else {
			// Create a priority queue
			// Initialized with size 2 since most applications only need 1-best
			// And based on experiments, somehow initializing with size 2 uses less memory compared to size 1.
			nextBestChildQueue = new UniquePriorityQueue<ScoredIndex>(2);
		}
		bestChildrenList = new ArrayList<ScoredIndex>();
		hasMoreHypothesis = true;
	}
	
	/**
	 * Returns the k-th best path at this hypothesis.<br>
	 * Note that k is 0-based, so having k=0 will give the best prediction,
	 * k=1 will give the second best prediction, and so on.
	 * @param k
	 * @return
	 */
	public ScoredIndex getKthBestHypothesis(int k){
		// Assuming the k is 0-based. So k=0 will return the best prediction
		// Below we fill the cache until we satisfy the number of top-k paths requested.
		while(bestChildrenList.size() <= k){
			ScoredIndex nextBest = setAndReturnNextBestPath();
			if(nextBest == null){
				return null;
			}
		}
		return bestChildrenList.get(k);
	}
	
	/**
	 * Return the next best path, or return null if there is no next best path.
	 * @return
	 */
	public abstract ScoredIndex setAndReturnNextBestPath();
	
	public int nodeIndex(){
		return this.nodeIndex;
	}
	
	/**
	 * Sets node index of this hypothesis accordingly.
	 * For NodeHypothesis, this should be that node's index.
	 * For EdgeHypothesis, this should be the node index of the parent node.
	 * @param nodeIndex
	 */
	public void setNodeIndex(int nodeIndex){
		this.nodeIndex = nodeIndex;
	}
	
	/**
	 * @return The children of this hypothesis.
	 */
	public Hypothesis[] children() {
		return children;
	}
	
	/**
	 * @param children The children to set
	 */
	public void setChildren(Hypothesis[] children) {
		this.children = children;
	}
	
	/**
	 * Returns the last best index calculated on this hypothesis.
	 * @return
	 */
	public ScoredIndex getLastBestIndex(){
		if(bestChildrenList.size() == 0){
			return null;
		}
		return bestChildrenList.get(bestChildrenList.size()-1);
	}

	public List<ScoredIndex> bestChildrenList() {
		return bestChildrenList;
	}

	public void setBestChildrenList(ArrayList<ScoredIndex> bestChildrenList) {
		this.bestChildrenList = bestChildrenList;
	}

	public Queue<ScoredIndex> nextBestChildQueue() {
		return nextBestChildQueue;
	}

	public void setNextBestChildQueue(BoundedUniquePriorityQueue<ScoredIndex> nextBestChildQueue) {
		this.nextBestChildQueue = nextBestChildQueue;
	}

}
