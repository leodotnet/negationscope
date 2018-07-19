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

import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import org.ns.commons.types.Instance;
import org.ns.util.Pipeline;

/**
 * The base class for network compiler, a class to convert a problem representation between 
 * {@link Instance} (the surface form) and {@link Network} (the modeled form)<br>
 * When implementing the {@link #compile(int, Instance, LocalNetworkParam)} method, you might 
 * want to split the case into two cases: labeled and unlabeled, where the labeled network contains
 * only the existing nodes and edges in the instance, and the unlabeled network contains all
 * possible nodes and edges in the instance.
 * @author Wei Lu <luwei@statnlp.com>
 *
 */
public abstract class NetworkCompiler implements Serializable{
	
	/**
	 * Creates an empty network compiler
	 */
	public NetworkCompiler(){
		
	}
	
	/**
	 * A constructor taking a Pipeline object as an input
	 * @param pipeline
	 */
	public NetworkCompiler(Pipeline<?> pipeline){
		
	}
	
	/**
	 * A class to store information about a single instance (both labeled and unlabeled versions)
	 * @author Aldrian Obaja <aldrianobaja.m@gmail.com>
	 *
	 */
	public static class InstanceInfo implements Serializable {
		private static final long serialVersionUID = 8576388720516676443L;
		public int instanceId;
		public Network labeledNetwork;
		public Network unlabeledNetwork;
		
		public double score;
		
		public InstanceInfo(int instanceID){
			if(instanceID <= 0){
				throw new RuntimeException("InstanceInfo objects should have positive ID, received ID: "+instanceID);
			}
			this.instanceId = instanceID;
		}
	}
	
	private static final long serialVersionUID = 1052885626598299680L;
	public final ConcurrentHashMap<Integer, InstanceInfo> instanceInfos = new ConcurrentHashMap<Integer, InstanceInfo>();
	
	/**
	 * Clears the cache of compiled network.<br>
	 * This should be done between training and testing.
	 */
	public void reset(){
		instanceInfos.clear();
	}
	
	/**
	 * Compile and store the networks per instance basis (each instance has two networks: labeled and unlabeled)
	 * @param networkId
	 * @param inst
	 * @param param
	 * @return
	 */
	public Network compileAndStore(int networkId, Instance inst, LocalNetworkParam param){
		int absInstID = Math.abs(inst.getInstanceId());
		InstanceInfo info = instanceInfos.putIfAbsent(absInstID, new InstanceInfo(absInstID));
		if(info == null){ // This means previously there is no InstanceInfo
			info = instanceInfos.get(absInstID);
		}
		Network network;
		if(inst.isLabeled()){
			if(info.labeledNetwork != null){
				return info.labeledNetwork;
			}
			network = compileLabeled(networkId, inst, param);
			info.labeledNetwork = network;
			if(info.unlabeledNetwork != null){
				info.unlabeledNetwork.setLabeledNetwork(network);
				network.setUnlabeledNetwork(info.unlabeledNetwork);
			}
		} else {
			if(info.unlabeledNetwork != null){
				return info.unlabeledNetwork;
			}
			network = compileUnlabeled(networkId, inst, param);
			info.unlabeledNetwork = network;
			if(info.labeledNetwork != null){
				info.labeledNetwork.setUnlabeledNetwork(network);
				network.setLabeledNetwork(info.labeledNetwork);
			}
		}
		return network;
	}
	
	/**
	 * Convert an instance into the network representation.<br>
	 * This process is also called the encoding part (e.g., to create the trellis network 
	 * of POS tags for a given sentence)<br>
	 * @param networkId
	 * @param inst
	 * @param param
	 * @return
	 */
	public Network compile(int networkId, Instance inst, LocalNetworkParam param){
		if(inst.isLabeled()){
			return compileLabeled(networkId, inst, param);
		} else {
			return compileUnlabeled(networkId, inst, param);
		}
	}
	
	/**
	 * Compile a labeled network.<br>
	 * A labeled network is a network which shows only the correct path in the graph.<br>
	 * A correct implementation of a labeled network should make it a sub-graph of the corresponding 
	 * unlabeled network when passed in the same arguments.
	 * @param networkId
	 * @param inst
	 * @param param
	 * @return
	 * @see #compileUnlabeled(int, Instance, LocalNetworkParam)
	 */
	public abstract Network compileLabeled(int networkId, Instance inst, LocalNetworkParam param);
	
	/**
	 * Compile an unlabeled network.<br>
	 * An unlabeled network is a network which shows all possible path in the graph.<br>
	 * A correct implementation of an unlabeled network should make it a super-graph of the corresponding 
	 * labeled network when passed in the same arguments.
	 * @param networkId
	 * @param inst
	 * @param param
	 * @return
	 * @see #compileLabeled(int, Instance, LocalNetworkParam)
	 */
	public abstract Network compileUnlabeled(int networkId, Instance inst, LocalNetworkParam param);
	
	/**
	 * Convert a network into an instance, the surface form.<br>
	 * This process is also called the decoding part (e.g., to get the sequence with maximum 
	 * probability in an HMM)
	 * @param network
	 * @return
	 */
	public abstract Instance decompile(Network network);
	
	public Instance decompile(Network network, int k){
		throw new UnsupportedOperationException("The top-k decompiler is not implemented.\n"
				+ "If you are a developer, please override decompile(Network, int) "
				+ "in your custom NetworkCompiler");
	}
	
	/**
	 * The cost of the structure from leaf nodes up to node <code>k</code>.<br>
	 * This is used for structured SVM, and generally the implementation requires the labeled Instance.<br>
	 * Cost is not calculated during test, since there is no labeled instance.<br>
	 * This will call {@link #costAt(int, int[])}, where the actual implementation resides.
	 * @param k
	 * @param child_k
	 * @return
	 */
	public double cost(Network network, int k, int[] child_k){
		if(network.getInstance().getInstanceId() > 0){
			return 0.0;
		}
		return costAt(network, k, child_k);
	}

	/**
	 * The cost of the structure at the edge connecting node <code>k</code> with its specific
	 * child node <code>child_k</code>.<br>
	 * This is used for structured SVM, and generally the implementation requires the labeled Instance, which
	 * can be accessed through {@link Network#getLabeledNetwork}.<br>
	 * @param network
	 * @param parent_k
	 * @param child_k
	 * @return
	 */	
	public double costAt(Network network, int parent_k, int[] child_k){
		int size = network.getInstance().size();
		Network labeledNet = network.getLabeledNetwork();
		long node = network.getNode(parent_k);
		int node_k = labeledNet.getNodeIndex(node);
		if(node_k < 0){
			double nodeCost = NetworkConfig.NODE_COST;
			if(NetworkConfig.NORMALIZE_COST){
				nodeCost /= size;
			}
			nodeCost *= NetworkConfig.MARGIN;
			double edgeCost = NetworkConfig.EDGE_COST;
			if(NetworkConfig.NORMALIZE_COST){
				edgeCost /= size;
			}
			edgeCost *= NetworkConfig.MARGIN;
			return nodeCost+edgeCost;
		}
		long[] childNodes = new long[child_k.length];
		for(int i=0; i<child_k.length; i++){
			if(child_k[i] < 0){
				// A negative child_k is not a reference to a node, it's just a number associated with this edge
				continue;
			}
			childNodes[i] = network.getNode(child_k[i]);
		}
		int[][] children_k = labeledNet.getChildren(node_k);
		boolean edgePresentInLabeled = false;
		for(int[] children: children_k){
			long[] childrenNodes = new long[children.length];
			for(int i=0; i<children.length; i++){
				if(children[i] < 0){
					// A negative child_k is not a reference to a node, it's just a number associated with this edge
					continue;
				}
				childrenNodes[i] = labeledNet.getNode(children[i]);
			}
			if(Arrays.equals(childrenNodes, childNodes)){
				edgePresentInLabeled = true;
				break;
			}
		}
		if(edgePresentInLabeled || network.isRoot(parent_k)){
			return 0.0;
		} else {
			double edgeCost = NetworkConfig.EDGE_COST;
			if(NetworkConfig.NORMALIZE_COST){
				edgeCost /= size;
			}
			edgeCost *= NetworkConfig.MARGIN;
			return edgeCost;
		}
	}

	/**
	 * Returns the position in the input represented by the given node.
	 * @return
	 */
	public int getPosForNode(int[] nodeArray){
		return nodeArray[0];
	}
	
	public Object getOutputForNode(int[] nodeArray){
		return nodeArray[1];
	}
	
}