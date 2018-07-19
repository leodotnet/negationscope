/** Statistical Natural Language Processing System
    Copyright (C) 2014-2016  Lu, Wei

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ns.hypergraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.ns.commons.types.Instance;
import org.ns.hypergraph.NetworkConfig.InferenceType;

import gnu.trove.iterator.TLongIterator;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;

/**
 * An extension of {@link Network} which defines more functions related to managing nodes and edges.<br>
 * Subclasses might want to override {@link #isRemoved(int)} and {@link #remove(int)} to disable the auto-removal 
 * of nodes performed in this class.<br>
 * The main functions of this class are {@link #addNode(long)}, {@link #addEdge(long, long[])}, and {@link #finalizeNetwork()}.<br>
 * Always call {@link #finalizeNetwork()} after no more nodes and edges are going to be added 
 * @author Wei Lu <luwei@statnlp.com>
 *
 */
public abstract class TableLookupNetwork extends Network{
	
	private static final long serialVersionUID = -7250820762892368213L;
	
	/**
	 * The temporary data structures while creating partial network.
	 * @deprecated The temporary data structures are moved to NetworkBuilder.
	 */
	@Deprecated
	private transient TLongObjectHashMap<ArrayList<long[]>> _children_tmp;
	
	//at each index, store the node's ID
	protected long[] _nodes;
	//at each index, store the node's list of children's indices (with respect to _nodes)
	protected int[][][] _children;
	//will be useful when doing decoding.
	protected boolean[] _isSumNode;
	
	public void setSumNode(long node){
		int node_k = Arrays.binarySearch(this._nodes, node);
		if(node_k<0){
			throw new RuntimeException("This node does not exist:"+Arrays.toString(NetworkIDMapper.toHybridNodeArray(node)));
		}
		this._isSumNode[node_k] = true;
	}
	
	private long[] toNodes(int[] ks){
		long[] nodes = new long[ks.length];
		for(int i = 0; i<nodes.length; i++){
			if(ks[i] < 0){
				nodes[i] = ks[i];
			} else {
				nodes[i] = this.getNode(ks[i]);
			}
		}
		return nodes;
	}
	
	/**
	 * A convenience method to check whether a network is contained in (is a subgraph of) another network 
	 * @param network
	 * @return
	 */
	public boolean contains(TableLookupNetwork network){
		if(this.countNodes() < network.countNodes()){
			System.err.println(String.format("Size of this network (%d) is less than the size of the input network (%d)", this.countNodes(), network.countNodes()));
			return false;
		}
		int start = 0;
		for(int j = 0;j<network.countNodes(); j++){
			long node1 = network.getNode(j);
			int[][] children1 = network.getChildren(j);
			boolean found = false;
			for(int k = start; k<this.countNodes() ; k++){
				long node2 = this.getNode(k);
				int[][] children2 = this.getChildren(k);
				if(node1==node2){
					for(int[] child1 : children1){
						long[] child1_nodes = network.toNodes(child1);
						boolean child_found = false;
						for(int[] child2 : children2){
							long[] child2_nodes = this.toNodes(child2);
							if(Arrays.equals(child1_nodes, child2_nodes)){
								child_found = true;
							}
						}
						if(!child_found){
							System.err.println("supposingly smaller:"+Arrays.toString(child1_nodes)+"\t"+children1.length);
							for(int t = 0; t<children2.length; t++){
								System.err.println("supposingly larger :"+Arrays.toString(this.toNodes(children2[t]))+"\t"+children2.length);
							}
							System.err.println(node1+"\t"+Arrays.toString(NetworkIDMapper.toHybridNodeArray(node1)));
							System.err.println(node2+"\t"+Arrays.toString(NetworkIDMapper.toHybridNodeArray(node2)));
							return false;
						}
					}
					
					found = true;
					start = k;
					break;
				}
			}
			if(!found){
				System.err.println(String.format("The node (%s) in input network not found in this network.", Arrays.toString(NetworkIDMapper.toHybridNodeArray(node1))));
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Default constructor. Note that the network constructed using this default constructor is lacking 
	 * the {@link LocalNetworkParam} object required for actual use.
	 * Use this only for generating generic network, which is later actualized using another constructor.
	 * @see #TableLookupNetwork(int, Instance, LocalNetworkParam)
	 */
	public TableLookupNetwork(){
		this._children_tmp = new TLongObjectHashMap<ArrayList<long[]>>();
	}

	/**
	 * Construct a network with the specified instance and parameter
	 * @param networkId
	 * @param inst
	 * @param param
	 * @deprecated Use {@link #TableLookupNetwork(int, Instance, LocalNetworkParam, NetworkCompiler)} instead.
	 */
	@Deprecated
	public TableLookupNetwork(int networkId, Instance inst, LocalNetworkParam param){
		this(networkId, inst, param, null);
	}
	
	/**
	 * Construct a network with the specified instance and parameter, and with the compiler that created this network
	 * @param networkId
	 * @param inst
	 * @param param
	 * @param compiler
	 */
	public TableLookupNetwork(int networkId, Instance inst, LocalNetworkParam param, NetworkCompiler compiler){
		super(networkId, inst, param, compiler);
		this._children_tmp = new TLongObjectHashMap<ArrayList<long[]>>();
	}

	/**
	 * Construct a network with the specified nodes and edges<br>
	 * This is mainly used to create a subgraph of a larger graph by modifying the number of nodes
	 * by overriding {@link #countNodes()}
	 * @param networkId
	 * @param inst
	 * @param nodes
	 * @param children
	 * @param param
	 * @deprecated Use {@link #TableLookupNetwork(int, Instance, long[], int[][][], LocalNetworkParam, NetworkCompiler)} instead.
	 */
	@Deprecated
	public TableLookupNetwork(int networkId, Instance inst, long[] nodes, int[][][] children, LocalNetworkParam param){
		this(networkId, inst, nodes, children, param, null);
	}
	
	/**
	 * Construct a network with the specified nodes and edges, and with the compiler that created this network<br>
	 * This is mainly used to create a subgraph of a larger graph by modifying the number of nodes
	 * by overriding {@link #countNodes()}
	 * @param networkId
	 * @param inst
	 * @param nodes
	 * @param children
	 * @param param
	 * @param compiler
	 */
	public TableLookupNetwork(int networkId, Instance inst, long[] nodes, int[][][] children, LocalNetworkParam param, NetworkCompiler compiler){
		super(networkId, inst, param, compiler);
		this._nodes = nodes;
		this._children = children;
	}
	
	@Override
	public long getNode(int k){
		return this._nodes[k];
	}
	
	@Override
	public int[][] getChildren(int k){
		return this._children[k];
	}
	
	public long[] getAllNodes(){
		return this._nodes;
	}
	
	public int[][][] getAllChildren(){
		return this._children;
	}
	
	@Override
	public int countNodes() {
		return this._nodes.length;
	}
	
	/**
	 * Remove the node k from the network.
	 */
	public void remove(int k){
//		if(this.isVisible == null){
//			this.isVisible = new boolean[this.countNodes()];
//			Arrays.fill(this.isVisible, true);
//		}
		this.isVisible[k] = false;
	}
	
	/**
	 * Check if the node k is removed from the network.
	 */
	public boolean isRemoved(int k){
//		if(this.isVisible == null){
//			this.isVisible = new boolean[this.countNodes()];
//			Arrays.fill(this.isVisible, true);
//		}
		return !this.isVisible[k];
	}
	
	/**
	 * Make the node with index k visible again.
	 */
	public void recover(int k){
		this.isVisible[k] = true;
	}
	
	public int getNodeIndex(long node){
		return Arrays.binarySearch(this._nodes, node);
	}
	
	@Override
	public boolean isRoot(int k){
		return this.countNodes()-1 == k;
	}
	
	@Override
	public boolean isLeaf(int k){
		int[][] v= this._children[k];
		if(v.length==0) return false;
		if(v[0].length==0) return true;
		return false;
	}
	
	/**
	 * Count the number of invalid nodes
	 * @return
	 */
	public int countInvalidNodes(){
		int count = 0;
		for(int k = 0; k<this._nodes.length; k++){
			if(this._inside[k]==Double.NEGATIVE_INFINITY || this._outside[k]==Double.NEGATIVE_INFINITY){
				count++;
			}
		}
		return count;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("nodes:");
		sb.append('[');
		sb.append('\n');
		for(int k = 0; k<this.countNodes(); k++){
			sb.append(Arrays.toString(NetworkIDMapper.toHybridNodeArray(this._nodes[k])));
			sb.append('\n');
		}
		sb.append(']');
		sb.append('\n');
		sb.append("links:");
		sb.append('[');
		sb.append('\n');
		for(int k = 0; k<this.countNodes(); k++){
			sb.append('<');
			long parent = this._nodes[k];
			sb.append(Arrays.toString(NetworkIDMapper.toHybridNodeArray(parent)));
			int[][] childrenList = this._children[k];
			for(int i = 0; i<childrenList.length; i++){
				sb.append('\n');
				sb.append('\t');
				sb.append('(');
				int[] children = childrenList[i];
				for(int j = 0; j<children.length; j++){
					if(children[j] < 0){
						continue;
					}
					sb.append('\n');
					sb.append('\t'+Arrays.toString(NetworkIDMapper.toHybridNodeArray(this._nodes[children[j]])));
				}
				sb.append('\n');
				sb.append('\t');
				sb.append(')');
			}
			sb.append('>');
			sb.append('\n');
		}
		sb.append(']');
		sb.append('\n');
		
		return sb.toString();
	}
	
	/* Below are methods which have been moved to NetworkBuilder */
	
	/**
	 * Returns the temporary data structure
	 * @param node
	 * @return
	 * @deprecated This method has been moved into NetworkBuilder
	 */
	@Deprecated
	public ArrayList<long[]> getChildren_tmp(long node){
		return this._children_tmp.get(node);
	}

	/**
	 * Returns the current list of nodes.
	 * @return
	 * @deprecated This method has been moved into NetworkBuilder
	 */
	@Deprecated
	public long[] getNodes_tmp(){
		TLongIterator nodes_key = this._children_tmp.keySet().iterator();
		long[] nodes = new long[this._children_tmp.size()];
		for(int k = 0; k<nodes.length; k++)
			nodes[k] = nodes_key.next();
		return nodes;
	}

	/**
	 * Removes the specified node from this network.
	 * @param node
	 * @return
	 * @deprecated This method has been moved into NetworkBuilder
	 */
	@Deprecated
	public boolean remove_tmp(long node){
		if(!this._children_tmp.containsKey(node))
			return false;
		this._children_tmp.remove(node);
		return true;
	}

	/**
	 * Returns the number of nodes in the current network
	 * @return
	 * @deprecated This method has been moved into NetworkBuilder
	 */
	@Deprecated
	public int countTmpNodes_tmp(){
		return this._children_tmp.size();
	}
	
	/**
	 * Check if the node is present in this network.
	 * @return
	 * @deprecated This method has been moved into NetworkBuilder
	 */
	@Deprecated
	public boolean contains(long node){
		return this._children_tmp.containsKey(node);
	}
	
	/**
	 * Check if the edge is present in this network.
	 * @param parent
	 * @param child
	 * @return
	 * @deprecated This method has been moved into NetworkBuilder
	 */
	@Deprecated
	public boolean contains(long parent, long[] child){
		if(!this._children_tmp.containsKey(parent)){
			return false;
		}
		ArrayList<long[]> children = this._children_tmp.get(parent);
		for(long[] presentChild: children){
			if(Arrays.equals(presentChild, child)){
				return true;
			}
		}
		return false;
	}

	
	/**
	 * Add one node to the network.
	 * @param node The node to be added
	 * @return true if the node was successfully added, false if the node was previously present.
	 * @deprecated This method has been moved into NetworkBuilder
	 */
	@Deprecated
	public boolean addNode(long node){
		if(this._children_tmp.containsKey(node)){
			return false;
		}
		this._children_tmp.put(node, null);
		return true;
	}
	
	/**
	 * Remove all such nodes that is not a descendent of the root<br>
	 * This is a useful method to reduce the number of nodes and edges during network creation.
	 * @deprecated This method has been moved into NetworkBuilder
	 */
	@Deprecated
	public void checkValidNodesAndRemoveUnused(){
		long[] nodes = new long[this.countTmpNodes_tmp()];
		double[] validity = new double[this.countTmpNodes_tmp()];
		int v = 0;
		for(long node: this._children_tmp.keys()){
			nodes[v++] = node;
		}
		Arrays.sort(nodes);
		this.checkValidityHelper(validity, nodes, this.countTmpNodes_tmp()-1);
		for(int k = 0; k<validity.length; k++){
			if(validity[k]==0){
				this.remove_tmp(nodes[k]);
			}
		}
	}

	@Deprecated
	private void checkValidityHelper(double[] validity, long[] nodes, int node_k){
		if(validity[node_k]==1){
			return;
		}
		
		validity[node_k] = 1;
		ArrayList<long[]> children = this.getChildren_tmp(nodes[node_k]);
		if(children==null){
			return;
		}
		for(long[] child : children){
			for(long c : child){
				int c_k = Arrays.binarySearch(nodes, c);
				if(c_k<0)
					throw new RuntimeException("Can not find this node? Position:"+c_k+",value:"+c);
				this.checkValidityHelper(validity, nodes, c_k);
			}
		}
	}
	
	/**
	 * Finalize this network, by converting the temporary arrays for nodes and edges into the finalized one.<br>
	 * This method must be called before this network can be used.
	 * @deprecated This method has been moved into NetworkBuilder
	 */
	@Deprecated
	public void finalizeNetwork(){
		ArrayList<Long> nodes = new ArrayList<Long>();
		for(long node: this._children_tmp.keys()){
			nodes.add(node);
		}
		Collections.sort(nodes);
		this._nodes = new long[this._children_tmp.size()];
		this.isVisible = new boolean[this._nodes.length];
		TLongIntHashMap nodesValue2IdMap = new TLongIntHashMap();
		for(int k = 0 ; k<this._nodes.length; k++){
			this._nodes[k] = nodes.get(k);
			this.isVisible[k] = true;
			nodesValue2IdMap.put(this._nodes[k], k);
		}
		if (NetworkConfig.INFERENCE == InferenceType.MEAN_FIELD) {
			this.structArr = new int[this._nodes.length];
		}
		
		this._children = new int[this._nodes.length][][];
		
		//TLongObjectHashMap<ArrayList<long[]>>
		for(long parent: this._children_tmp.keys()){
			int parent_index = nodesValue2IdMap.get(parent);
			ArrayList<long[]> childrens = this._children_tmp.get(parent);
			if(childrens==null){
				// If any node has no child edge, assume there is one edge with no child node
				// This is done so that every node is visited in the feature extraction step
				// This is consistent with what's written in http://portal.acm.org/citation.cfm?doid=1654494.1654500
				// See Definition 3 on "source vertex"
				this._children[parent_index] = new int[1][0];
			} else {
				this._children[parent_index] = new int[childrens.size()][];
				for(int k = 0 ; k <this._children[parent_index].length; k++){
					long[] children = childrens.get(k);
					int[] children_index = new int[children.length];
					for(int m = 0; m<children.length; m++){
						if(children[m] < 0){
							children_index[m] = (int)children[m];
						} else {
							children_index[m] = nodesValue2IdMap.get(children[m]);
						}
					}
					this._children[parent_index][k] = children_index;
				}
			}
		}
		this._children_tmp = null;
	}

	@Deprecated
	private void checkLinkValidity(long parent, long[] children) throws NetworkException {
		for(long child : children){
			if(child < 0){
				// A negative child_k is not a reference to a node, it's just a number associated with this edge
				continue;
			}
			if(child >= parent){
				System.err.println(Arrays.toString(NetworkIDMapper.toHybridNodeArray(parent)));
				System.err.println(Arrays.toString(NetworkIDMapper.toHybridNodeArray(child)));
				System.err.println();
				throw new NetworkException("In an edge, the parent needs to have larger node ID in order to "
						+ "have a proper schedule for inference. Violation: "+parent+"\t"+Arrays.toString(children));
			}
		}
		/**/
		
		this.checkNodeValidity(parent);
		for(long child : children){
			if(child < 0){
				// A negative child_k is not a reference to a node, it's just a number associated with this edge
				continue;
			}
			this.checkNodeValidity(child);
		}
	}

	@Deprecated
	private void checkNodeValidity(long node) throws NetworkException {
		if(!this._children_tmp.containsKey(node)){
			throw new NetworkException("Node not found:"+Arrays.toString(NetworkIDMapper.toHybridNodeArray(node)));
		}
	}
	
	/**
	 * Add an edge to this network. Only do this after the respective nodes are added.
	 * @param parent The parent node
	 * @param child The child nodes of a SINGLE hyperedge. Note that this only add one edge, 
	 * 				   with the parent as the root and the children as the leaves in the hyperedge.
	 * 				   To add multiple edges, multiple calls to this method is necessary.
	 * @return true if the edge was added successfully, false if the edge was previously present in the network.
	 * @throws NetworkException If the edge is already added
	 * @deprecated This method has been moved into NetworkBuilder
	 */
	@Deprecated
	public boolean addEdge(long parent, long[] child){
		this.checkLinkValidity(parent, child);
		if(!this._children_tmp.containsKey(parent) || this._children_tmp.get(parent)==null){
			this._children_tmp.put(parent, new ArrayList<long[]>());
		}
		ArrayList<long[]> existing_children = this._children_tmp.get(parent);
		for(int k = 0; k<existing_children.size(); k++){
			if(Arrays.equals(existing_children.get(k), child)){
				return false;
			}
		}
		existing_children.add(child);
		return true;
	}
	
}
