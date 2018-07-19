/**
 * 
 */
package org.ns.hypergraph.decoding;

import java.util.Arrays;

/**
 * A wrapper for configuration object (the {@link #index} array) together with its score.
 * This is the object being put in the priority queue during top-k decoding.<br> 
 * This behaves differently when used in NodeHypothesis or EdgeHypothesis.
 */
public class ScoredIndex implements Comparable<ScoredIndex>{
	
	public double score;
	public int[] index;
	public int node_k;

	public ScoredIndex(int node_k, double score, int[] index) {
		this.score = score;
		this.index = index;
		this.node_k = node_k;
	}
	
	/**
	 * Returns the hypothesis according to the index configuration.
	 * This will return the IndexedScore object corresponding to taking the k-th best
	 * path for each child node of the specified edge hypothesis.
	 * Note that the index array will be an array containing the k-th best path requested
	 * of each child node. 
	 * @param node_k The node index of the parent node of this edge.
	 * @param index The index configuration. The length should be the same as the number of children that
	 * 				this edge has. Each representing the k-th best path that should be considered.
	 * @param hypothesis The EdgeHypothesis object representing the specified edge.
	 * @return The path based on the configuration in index array. If the configuration is invalid,
	 * 		   this will return null.
	 */
	public static ScoredIndex get(int node_k, int[] index, EdgeHypothesis hypothesis){
		NodeHypothesis[] children = hypothesis.children();
		double score = hypothesis.score();
		for(int i=0; i<index.length; i++){
			if(children[i].nodeIndex < 0){
				continue;
			}
			// The following line corresponds to line 16 in Algorithm 3 in Huang and Chiang (2005) paper.
			ScoredIndex kthBestChildrenAtIthPos = children[i].getKthBestHypothesis(index[i]);
			if(kthBestChildrenAtIthPos == null){
				// If the request contains an invalid k-th best path for any child,
				// then return null, to say that this configuration is invalid (there is no k-th best
				// for the specified child).
				return null;
			}
			score += kthBestChildrenAtIthPos.score;
		}
		return new ScoredIndex(node_k, score, index);
	}
	
	/**
	 * Returns the hypothesis according to the index configuration.
	 * This will return the k-th best path of the m-th edge of the specified node hypothesis,
	 * where k = index[1] and m = index[0].
	 * @param node_k The specified node index
	 * @param index Specifies the configuration to take. This should be a two-element array,
	 * 				where index[0] represents the m-th edge of this node,
	 * 				and index[1] represents the k-th best path of that edge.
	 * @param hypothesis The NodeHypothesis object representing the specified node.
	 * @return The path based on the configuration in index array. If the configuration is invalid,
	 * 		   this will return null.
	 */
	public static ScoredIndex get(int node_k, int[] index, NodeHypothesis hypothesis){
		EdgeHypothesis[] children = hypothesis.children();
		ScoredIndex kthBestChildrenAtIthPos = children[index[0]].getKthBestHypothesis(index[1]);
		if(kthBestChildrenAtIthPos == null){
			return null;
		}
		double score = kthBestChildrenAtIthPos.score;
//		System.out.println(String.format("Best of %s: %.3f", hypothesis, score));
		return new ScoredIndex(node_k, score, index);
	}
	
	public boolean equals(Object o){
		if(o instanceof ScoredIndex){
			ScoredIndex s = (ScoredIndex)o;
			if(s.node_k != node_k){
				return false;
			}
			return Arrays.equals(index, s.index);
		}
		return false;
	}

	@Override
	public int compareTo(ScoredIndex o) {
		int result = Double.compare(o.score, this.score);
		if(result != 0)	return result;
		result = Integer.compare(node_k, o.node_k);
		if(result != 0) return result;
		result = Integer.compare(index.length, o.index.length);
		if(result != 0) return result;
		for(int i=0; i<index.length; i++){
			result = Integer.compare(index[i], o.index[i]);
			if(result != 0) return result;
		}
		return 0;
	}
	
	@Override
	public String toString(){
		return String.format("%.3f %s", score, Arrays.toString(index));
	}
	
	@Override
	public int hashCode(){
		return Double.hashCode(score)^Integer.hashCode(node_k)^index.hashCode();
	}

}
