package org.ns.hypergraph.decoding;

import java.util.Arrays;

/**
 * This class represents a (possibly partial) hypothesis of
 * an output structure at a specific edge.
 * This holds the tuple <score, NodeHyperedge[]>, representing
 * the local score of this edge.
 * The path score from this edge down to the leaf node is stored as a top-k
 * list in {@link #bestChildrenList()}, depending on the choice of the child nodes'
 * k-th best path.
 */
public class EdgeHypothesis extends Hypothesis{
	
	private double score;

	/**
	 * Creates an empty hypothesis with 0 score and no children
	 */
	public EdgeHypothesis(int nodeIndex) {
		this(nodeIndex, null, 0.0);
	}
	
	/**
	 * Creates a hypothesis with specific children (the list of nodes connected to this hyperedge,
	 * excluding the parent) and specific score.
	 * @param parent
	 * @param score
	 */
	public EdgeHypothesis(int nodeIndex, NodeHypothesis[] children, double score){
		this.score = score;
		setNodeIndex(nodeIndex);
		setChildren(children);
		init();
	}
	
	public NodeHypothesis[] children() {
		return (NodeHypothesis[])children;
	}
	
	/**
	 * The score of this hypothesis, which is the sum of the scores from this point to the leaf node.
	 * @return
	 */
	public double score(){
		return this.score;
	}
	
	public ScoredIndex setAndReturnNextBestPath(){
		if(!hasMoreHypothesis){
			return null;
		} else if(getLastBestIndex() == null){
			// This case means this is the first time this method is called, means
			// we are looking for the best path, so we take all the best from the child nodes
			// by setting the bestIndex array to all 0.
			int[] bestIndex = new int[children.length];
			Arrays.fill(bestIndex, 0);
			
			nextBestChildQueue.offer(ScoredIndex.get(nodeIndex, bestIndex, this));
		} else {
			ScoredIndex lastBestIndex = getLastBestIndex();
			// The index length in this EdgeHypothesis represents the degree of this hyperedge minus one.
			// So this represents the number of child nodes connected to the single parent node.
			
			// Below, we consider the next best candidate for each child node in this hyperedge 
			// and put them to the candidate priority queue.
			// The for-loop below corresponds to line 14-18 of Algorithm 3 in Huang and Chiang (2005) paper.
			for(int i=0; i<lastBestIndex.index.length; i++){
				if(children()[i].nodeIndex < 0){
					continue;
				}
				int[] newIndex = Arrays.copyOf(lastBestIndex.index, lastBestIndex.index.length);
				newIndex[i] += 1;
				ScoredIndex nextBestChildCandidate = ScoredIndex.get(nodeIndex, newIndex, (EdgeHypothesis)this);
				if(nextBestChildCandidate != null){
					nextBestChildQueue.offer(nextBestChildCandidate);
				}
			}
		}
		// After lazily expand the last best node, take the best scoring one.
		ScoredIndex nextBestIndex = nextBestChildQueue.poll();
		if(nextBestIndex == null){
			hasMoreHypothesis = false;
			return null;
		}
		
		// Cache this next best candidate in the list
		bestChildrenList.add(nextBestIndex);
//		System.out.println("["+this+"] Generated the "+(k+1)+"-th best");
		return nextBestIndex;
	}
	
	public String toString(){
		int[] parentNodeIndex = new int[children.length];
		for(int i=0; i<children.length; i++){
			parentNodeIndex[i] = children[i].nodeIndex();
		}
		return String.format("%d -> %s: %.3f", nodeIndex, Arrays.toString(parentNodeIndex), score);
	}

}
