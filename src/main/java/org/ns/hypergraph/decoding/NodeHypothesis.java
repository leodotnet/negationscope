package org.ns.hypergraph.decoding;

import java.util.Arrays;

/**
 * This class represents a (possibly partial) hypothesis of
 * an output structure at a specific node.
 */
public class NodeHypothesis extends Hypothesis{
	
	/**
	 * Creates an empty hypothesis for the specified node index.
	 */
	public NodeHypothesis(int nodeIndex) {
		this(nodeIndex, null);
	}
	
	/**
	 * Creates a hypothesis with a specific children and specific node index
	 * @param parent
	 * @param score
	 */
	public NodeHypothesis(int nodeIndex, EdgeHypothesis[] children){
		setNodeIndex(nodeIndex);
		setChildren(children);
		init();
	}
	
	public EdgeHypothesis[] children() {
		return (EdgeHypothesis[])children;
	}
	
	public ScoredIndex setAndReturnNextBestPath(){
		if(!hasMoreHypothesis){
			return null;
		} else if(getLastBestIndex() == null){
			// This case means this is the first time this method is called, means
			// we are looking for the best path, so we compare the best path from all
			// child hyperedges, and later we will take the best one.

			// This corresponds to the GetCandidates(v, k') call of Algorithm 3 line 7 in Huang and Chiang (2005) paper.
			for(int i=0; i<children.length; i++){
				if(children[i] == null){
					continue;
				}
				nextBestChildQueue.offer(ScoredIndex.get(nodeIndex, new int[]{i, 0}, this));
			}
		} else {
			ScoredIndex lastBestIndex = getLastBestIndex();
			int[] newIndex = Arrays.copyOf(lastBestIndex.index, lastBestIndex.index.length);
			// Remember that the IndexedScore in NodeHypothesis contains an index with only two elements.
			// The first element represent the edge id, and the second element represent the k-th best candidate from that edge
			
			// Below, since we have "consumed" current edge, then we consider the next best one of that edge.
			newIndex[1] += 1;
			// The next line corresponds to the LazyNext(cand[v],e,j,k'), where cand[v] is represented by
			// lastBestIndex[0].node_k and this NodeHypothesis, e is represented by this.children()[newIndex[0]],
			// and j is represented by e.bestChildList.get(newIndex[1])
			ScoredIndex nextBestCandidate = ScoredIndex.get(lastBestIndex.node_k, newIndex, (NodeHypothesis)this);
			if(nextBestCandidate != null){
				nextBestChildQueue.offer(nextBestCandidate);
			}
		}
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
		return String.format("%d -> %s", nodeIndex, Arrays.toString(children));
	}

}
