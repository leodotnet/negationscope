/**
 * 
 */
package org.ns.example.base;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.ns.commons.types.Instance;
import org.ns.hypergraph.LocalNetworkParam;
import org.ns.hypergraph.NetworkCompiler;
import org.ns.hypergraph.NetworkConfig;
import org.ns.hypergraph.NetworkException;
import org.ns.hypergraph.NetworkIDMapper;
import org.ns.hypergraph.TableLookupNetwork;
import org.ns.hypergraph.NetworkConfig.InferenceType;
import org.ns.util.GeneralUtils;

/**
 * A basic network implementation.
 * @author Aldrian Obaja (aldrianobaja.m@gmail.com)
 */
public class BaseNetwork extends TableLookupNetwork {
	
	private static final long serialVersionUID = 293079042713879263L;
	/** Stores the actual count of relevant nodes */
	int nodeCount;
	
	/**
	 * To create an empty network.<br>
	 * The recommended way to create a network is through {@link NetworkBuilder}.
	 * @see NetworkBuilder#builder()
	 */
	protected BaseNetwork(){}

	/**
	 * Creates an empty raw network.<br>
	 * Networks created through this constructor requires {@link #finalizeNetwork()} to be called before use.<br>
	 * The recommended way to create a network is through {@link NetworkBuilder}.
	 * @param networkId
	 * @param inst
	 * @param param
	 * @param compiler
	 * @see NetworkBuilder#builder(Class)
	 * @see NetworkBuilder#build(Integer, Instance, LocalNetworkParam, NetworkCompiler)
	 */
	protected BaseNetwork(int networkId, Instance inst, LocalNetworkParam param, NetworkCompiler compiler) {
		super(networkId, inst, param, compiler);
		this.nodeCount = -1;
	}

	/**
	 * Creates a network with the node and edge arrays taken from existing ones, and the nodeCount specifies 
	 * the actual number of relevant nodes.<br>
	 * This constructor is used to take a subset of an existing network.<br>
	 * The recommended way to create a network is through {@link NetworkBuilder}.
	 * @param networkId
	 * @param inst
	 * @param nodes
	 * @param children
	 * @param param
	 * @param compiler
	 * @see NetworkBuilder#quickBuild(Class, int, Instance, long[], int[][][], int, LocalNetworkParam, NetworkCompiler)
	 */
	protected BaseNetwork(int networkId, Instance inst, long[] nodes, int[][][] children, int nodeCount, LocalNetworkParam param,
			NetworkCompiler compiler) {
		super(networkId, inst, nodes, children, param, compiler);
		this.nodeCount = nodeCount;
		this.isVisible = new boolean[nodeCount];
		Arrays.fill(this.isVisible, true);
	}
	
	public int countNodes(){
		return this.nodeCount;
	}
	
	/**
	 * A helper class that follows Builder pattern to ensure that the Network constructed is always complete.<br>
	 * This class is supposed to be used in a {@link NetworkCompiler} implementation.<br>
	 * The network type T should extend {@link BaseNetwork}.
	 * @param <T>
	 */
	public static class NetworkBuilder<T extends BaseNetwork> {
		private HashMap<Long, List<long[]>> _children_tmp;
		private Class<T> networkClass;
		
		/**
		 * Returns a network builder for BaseNetwork.
		 * @return
		 */
		public static NetworkBuilder<BaseNetwork> builder(){
			return new NetworkBuilder<BaseNetwork>(BaseNetwork.class);
		}
		
		/**
		 * Returns a network builder for the specified network class.
		 * @param networkClass
		 * @return
		 */
		public static <T extends BaseNetwork> NetworkBuilder<T> builder(Class<T> networkClass){
			return new NetworkBuilder<T>(networkClass);
		}
		
		/**
		 * Gets a network builder for the specified network class.<br>
		 * The network class provided must extend {@link BaseNetwork} and has an empty constructor.
		 * @param networkClass
		 * @return
		 */
		private static <T extends BaseNetwork> T quickBuild(Class<T> networkClass) throws
				SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException {
			return networkClass.newInstance();
		}
		
		/**
		 * Quickly builds a BaseNetwork with the required parameters.<br>
		 * @param networkId The network id
		 * @param inst The instance associated with the network to be constructed
		 * @param nodes The nodes that might be present in the network to be constructed.
		 * @param children The edges that might be present in the network to be constructed.
		 * @param nodeCount The actual number of nodes from the given nodes array that are actually part of the network to be constructed.
		 * @param param The LocalNetworkParam object associated with the network to be constructed.
		 * @param compiler The network compiler that builds the network.
		 * @return
		 */
		public static BaseNetwork quickBuild(int networkId, Instance inst, long[] nodes,
				int[][][] children, int nodeCount, LocalNetworkParam param, NetworkCompiler compiler){
			return quickBuild(BaseNetwork.class, networkId, inst, nodes, children, nodeCount, param, compiler);
		}
		
		/**
		 * Quickly builds a network with the specified network class and the required parameters.<br>
		 * The network class provided must extend {@link BaseNetwork} and has a constructor with the following signature:
		 * (int networkId, Instance instance, long[] nodes, int[][][] children, int nodeCount, LocalNetworkParam param, NetworkCompiler compiler)
		 * @param networkClass The class of the network to be constructed
		 * @param networkId The network id
		 * @param inst The instance associated with the network to be constructed
		 * @param nodes The nodes that might be present in the network to be constructed.
		 * @param children The edges that might be present in the network to be constructed.
		 * @param nodeCount The actual number of nodes from the given nodes array that are actually part of the network to be constructed.
		 * @param param The LocalNetworkParam object associated with the network to be constructed.
		 * @param compiler The network compiler that builds the network.
		 * @return
		 */
		public static <T extends BaseNetwork> T quickBuild(Class<T> networkClass, int networkId, Instance inst,
				long[] nodes, int[][][] children, int nodeCount, LocalNetworkParam param, NetworkCompiler compiler){
			try {
				Constructor<T> constructor = GeneralUtils.getMatchingAvailableConstructor(networkClass,
						int.class, Instance.class, long[].class, int[][][].class, int.class,
						LocalNetworkParam.class, NetworkCompiler.class);
				return constructor.newInstance(networkId, inst, nodes, children, nodeCount, param, compiler);
			} catch (NoSuchMethodException | SecurityException e) {
				throw new RuntimeException("Network class "+networkClass.getName()+" does not support the "
						+ "required public constructor: (int, Instance, long[], int[][][], int, "
						+ "LocalNetworkParam, NetworkCompiler)", e);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException("Error in constructing a sub-network of class "+networkClass.getName()
						+ " (id="+networkId+",nodeCount="+nodeCount+")\nInstance: "+inst, e);
			}
		}
		
		public NetworkBuilder(Class<T> networkClass){
			this._children_tmp = new HashMap<Long, List<long[]>>();
			this.networkClass = networkClass;
		}
		
		/**
		 * Add one node to the network.
		 * @param node The node to be added
		 * @return true if the node was successfully added, false if the node was previously present.
		 */
		public boolean addNode(long node){
			if(this._children_tmp.containsKey(node)){
				return false;
			}
			this._children_tmp.put(node, null);
			return true;
		}
		
		/**
		 * Add an edge to this network. Only do this after the respective nodes are added.
		 * @param parent The parent node
		 * @param child The child nodes of a SINGLE hyperedge. Note that this only add one edge, 
		 * 				   with the parent as the root and the children as the leaves in the hyperedge.
		 * 				   To add multiple edges, multiple calls to this method is necessary.
		 * @return true if the edge was added successfully, false if the edge was previously present in the network.
		 * @throws NetworkException If the edge is already added
		 */
		public boolean addEdge(long parent, long[] child){
			this.checkLinkValidity(parent, child);
			if(!this._children_tmp.containsKey(parent) || this._children_tmp.get(parent)==null){
				this._children_tmp.put(parent, new ArrayList<long[]>());
			}
			List<long[]> existing_children = this._children_tmp.get(parent);
			for(int k = 0; k<existing_children.size(); k++){
				if(Arrays.equals(existing_children.get(k), child)){
					return false;
				}
			}
			existing_children.add(child);
			return true;
		}

		public int numNodes_tmp(){
			return this._children_tmp.size();
		}
		
		public List<long[]> getChildren_tmp(long node){
			return this._children_tmp.get(node);
		}
		
		public int countTmpNodes_tmp(){
			return this._children_tmp.size();
		}
		
		public long[] getNodes_tmp(){
			Iterator<Long> nodes_key = this._children_tmp.keySet().iterator();
			long[] nodes = new long[this._children_tmp.size()];
			for(int k = 0; k<nodes.length; k++)
				nodes[k] = nodes_key.next();
			return nodes;
		}
		
		public boolean remove_tmp(long node){
			if(!this._children_tmp.containsKey(node))
				return false;
			this._children_tmp.remove(node);
			return true;
		}
		
		/**
		 * Check if the node is present in this network.
		 */
		public boolean contains(long node){
			return this._children_tmp.containsKey(node);
		}
		
		/**
		 * Check if the edge is present in this network.
		 * @param parent
		 * @param child
		 * @return
		 */
		public boolean contains(long parent, long[] child){
			if(!this._children_tmp.containsKey(parent)){
				return false;
			}
			List<long[]> children = this._children_tmp.get(parent);
			for(long[] presentChild: children){
				if(Arrays.equals(presentChild, child)){
					return true;
				}
			}
			return false;
		}
		
		/**
		 * Remove all such nodes that is not a descendant of the root<br>
		 * This is a useful method to reduce the number of nodes and edges during network creation.
		 */
		public void checkValidNodesAndRemoveUnused(){
			long[] nodes = new long[this.countTmpNodes_tmp()];
			double[] validity = new double[this.countTmpNodes_tmp()];
			int v = 0;
			Iterator<Long> nodes_it = this._children_tmp.keySet().iterator();
			while(nodes_it.hasNext()){
				nodes[v++] = nodes_it.next();
			}
			Arrays.sort(nodes);
			this.checkValidityHelper(validity, nodes, this.countTmpNodes_tmp()-1);
			for(int k = 0; k<validity.length; k++){
				if(validity[k]==0){
					this.remove_tmp(nodes[k]);
				}
			}
		}
		
		private void checkValidityHelper(double[] validity, long[] nodes, int node_k){
			if(validity[node_k]==1){
				return;
			}
			
			validity[node_k] = 1;
			List<long[]> children = this.getChildren_tmp(nodes[node_k]);
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
		 * <strong>NOTE:</strong> This will create a network without any associated networkID, instance, parameters,
		 * and compiler.
		 * Use this build function if the network is not to be used in the computation directly (for example to
		 * be used as a generic unlabeled network).<br>
		 * See the {@link #build(Integer, Instance, LocalNetworkParam, NetworkCompiler)} method for creating 
		 * networks suitable for computation.
		 * @return The finalized network.
		 * @see #build(Integer, Instance, LocalNetworkParam, NetworkCompiler)
		 */
		public T buildRudimentaryNetwork(){
			return build(null, null, null, null);
		}
		
		/**
		 * Finalize this network, by converting the temporary arrays for nodes and edges into the finalized one.<br>
		 * The supplied values, if not all null, will be passed on to the constructor of the network class.
		 * @param networkID
		 * @param instance
		 * @param localNetworkParam
		 * @param compiler
		 * @return
		 */
		public T build(Integer networkID, Instance instance, LocalNetworkParam param, NetworkCompiler compiler){
			Iterator<Long> node_ids = this._children_tmp.keySet().iterator();
			ArrayList<Long> values = new ArrayList<Long>();
			while(node_ids.hasNext()){
				values.add(node_ids.next());
			}
			long[] nodeList = new long[this._children_tmp.keySet().size()];
			boolean[] isVisible = new boolean[nodeList.length];
			HashMap<Long, Integer> nodesValue2IdMap = new HashMap<Long, Integer>();
			Collections.sort(values);
			for(int k = 0 ; k<values.size(); k++){
				nodeList[k] = values.get(k);
				isVisible[k] = true;
				nodesValue2IdMap.put(nodeList[k], k);
			}
			
			int[][][] childrenList = new int[nodeList.length][][];

			Iterator<Long> parents = this._children_tmp.keySet().iterator();
			while(parents.hasNext()){
				long parent = parents.next();
				int parent_index = nodesValue2IdMap.get(parent);
				List<long[]> childrens = _children_tmp.get(parent);
				if(childrens==null){
					childrenList[parent_index] = new int[1][0];
				} else {
					childrenList[parent_index] = new int[childrens.size()][];
					for(int k = 0 ; k <childrenList[parent_index].length; k++){
						long[] children = childrens.get(k);
						int[] children_index = new int[children.length];
						for(int m = 0; m<children.length; m++){
							if(children[m] < 0){
								children_index[m] = (int)children[m];
							} else {
								children_index[m] = nodesValue2IdMap.get(children[m]);
							}
						}
						childrenList[parent_index][k] = children_index;
					}
				}
			}
			// If any node has no child edge, assume there is one edge with no child node
			// This is done so that every node is visited in the feature extraction step
			// This is consistent with what's written in http://portal.acm.org/citation.cfm?doid=1654494.1654500
			// See Definition 3 on "source vertex"
			for(int k = 0 ; k<childrenList.length; k++){
				if(childrenList[k]==null){
					childrenList[k] = new int[1][0];
				}
			}
			T result = null;
			if(networkID != null || instance != null || param != null || compiler != null){
				result = quickBuild(networkClass, networkID, instance, nodeList, childrenList, nodeList.length, param, compiler);
			} else {
				try {
					result = quickBuild(networkClass);
				} catch (SecurityException | InstantiationException | IllegalAccessException
						| IllegalArgumentException e) {
					throw new RuntimeException("No public empty constructor found for network class "+networkClass.getName(), e);
				}
				result._nodes = nodeList;
				result._children = childrenList;
				result.nodeCount = nodeList.length;
			}
			if (NetworkConfig.INFERENCE == InferenceType.MEAN_FIELD) {
				result.structArr = new int[nodeList.length];
			}
			result.isVisible = isVisible;
			return result;
		}
		
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
			
			this.checkNodeValidity(parent);
			for(long child : children){
				if(child < 0){
					// A negative child_k is not a reference to a node, it's just a number associated with this edge
					continue;
				}
				this.checkNodeValidity(child);
			}
		}
		
		private void checkNodeValidity(long node) throws NetworkException {
			if(!this._children_tmp.containsKey(node)){
				throw new NetworkException("Node not found:"+Arrays.toString(NetworkIDMapper.toHybridNodeArray(node)));
			}
		}
	}

}
