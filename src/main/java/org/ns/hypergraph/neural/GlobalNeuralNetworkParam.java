package org.ns.hypergraph.neural;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ns.hypergraph.LocalNetworkParam;
import org.ns.hypergraph.Network;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;

public class GlobalNeuralNetworkParam implements Serializable{

	private static final long serialVersionUID = 6065466652568298006L;

	protected List<NeuralNetworkCore> nets;
	
	protected transient LocalNetworkParam[] params_l;
	
	protected transient List<Map<Object,Integer>> allNNInput2Id;
	
	public GlobalNeuralNetworkParam() {
		this(new ArrayList<NeuralNetworkCore>());
	}
	
	public GlobalNeuralNetworkParam(List<NeuralNetworkCore> nets) {
		this.nets = nets;
		allNNInput2Id = new ArrayList<>();
		for (int id = 0; id < this.nets.size(); id++) {
			this.nets.get(id).setNeuralNetId(id);
			allNNInput2Id.add(new HashMap<>());
		}
	}
	
	/**
	 * Copy the global neural network 
	 * @return
	 */
	public GlobalNeuralNetworkParam copyNNParamG() {
		List<NeuralNetworkCore> newNets = new ArrayList<>(this.nets.size());
		for (int i = 0; i < this.nets.size(); i++) {
			newNets.add(this.nets.get(i).clone());
		}
		return new GlobalNeuralNetworkParam(newNets);
	}
	
	public void copyNNParam(GlobalNeuralNetworkParam src) {
		for (int id = 0; id < this.nets.size(); id++) {
			this.nets.get(id).params = src.getNet(id).params;
		}
	}
	
	public void setLocalNetworkParams(LocalNetworkParam[] params_l) {
		this.params_l = params_l;
		for (int id = 0; id < this.nets.size(); id++) {
			this.nets.get(id).setLocalNetworkParams(params_l);
		}
	}
	
	public void setLearningState() {
		for (int id = 0; id < this.nets.size(); id++) {
			this.nets.get(id).isTraining = true;
		}
	}
	
	public void setDecodeState() {
		for (int id = 0; id < this.nets.size(); id++) {
			this.nets.get(id).isTraining = false;
		}
	}
	
	public boolean isLearningState() {
		return this.nets.get(0).isTraining;
	}
	
	/**
	 * Return all the neural network. 
	 * @return
	 */
	public List<NeuralNetworkCore> getAllNets() {
		return this.nets;
	}
	
	public NeuralNetworkCore getNet(int netId) {
		return this.nets.get(netId);
	}
	
	public void prepareInputId() {
		for (LocalNetworkParam param_l : params_l) {
			for (int netId = 0; netId < this.nets.size(); netId++) {
				allNNInput2Id.get(netId).putAll(param_l.getLocalNNInput2Id().get(netId));	
			}
			param_l.setLocalNNInput2Id(null);
		}
		//System.out.println(allNNInput2Id.get(0).size());
		for (int netId = 0; netId < this.nets.size(); netId++) {
			this.nets.get(netId).nnInput2Id = new HashMap<Object, Integer>();
			int inputId = 0;
			for (Object input : allNNInput2Id.get(netId).keySet()) {
				this.nets.get(netId).nnInput2Id.put(input, inputId);
				inputId++;
			}
			allNNInput2Id.set(netId, null);
		}
		allNNInput2Id = null;
	}
	
	/**
	 * Used for batch training.
	 */
	public void prepareInstId2NNInputId() {
		for (int netId = 0; netId < this.nets.size(); netId++) {
			this.nets.get(netId).instId2NNInputId = new TIntObjectHashMap<>();
		}
		for (LocalNetworkParam param_l : params_l) {
			for (int netId = 0; netId < this.nets.size(); netId++) {
				TIntObjectMap<Set<Object>> instId2NNInput =  param_l.getLocalInstId2NNInput().get(netId);
				for (int instId : instId2NNInput.keys()) {
					Set<Object> set = instId2NNInput.get(instId);
					TIntList list = new TIntArrayList();
					for (Object obj : set) {
						list.add(this.nets.get(netId).nnInput2Id.get(obj));
					}
					if (this.nets.get(netId).instId2NNInputId.containsKey(instId)) {
						throw new RuntimeException("should unique for each local param.");
					} else {
						this.nets.get(netId).instId2NNInputId.put(instId, list);
					}
				}
			}
		}
	}
	
	/**
	 * Building the neural network structure.
	 */
	public void initializeNetwork() {
		for (NeuralNetworkCore net : nets) {
			net.initialize();
		}
	}
	
	/**
	 * forward all the networks
	 */
	public void forward(TIntSet batchInstIds) {
		for (NeuralNetworkCore net : nets) {
			net.forward(batchInstIds);
		}
	}
	
	/**
	 * Backpropagation.
	 */
	public void backward() {
		for (NeuralNetworkCore net : nets) {
			net.backward();
		}
	}
	
	/**
	 * Sum the provider scores for a given hyper-edge
	 * @param network
	 * @param parent_k
	 * @param children_k
	 * @param children_k_index
	 * @return
	 */
	public double getNNScore(Network network, int parent_k, int[] children_k, int children_k_index) {
		double score = 0.0;
		for (NeuralNetworkCore net : nets) {
			score += net.getScore(network, parent_k, children_k_index);
		}
		return score;
	}
	
	/**
	 * Send the count information for a given hyper-edge to each provider
	 * @param count
	 * @param network
	 * @param parent_k
	 * @param children_k_index
	 */
	public void setNNGradOutput(double count, Network network, int parent_k, int children_k_index) {
		for (NeuralNetworkCore net : nets) {
			net.update(count, network, parent_k, children_k_index);
		}
	}
	
	/**
	 * Close the Lua state connection
	 */
	public void closeNNConnections() {
		for (NeuralNetworkCore net : this.nets) {
			net.closeProvider();
		}
	}
	
	/**
	 * Reset accumulated gradient in each neural network
	 */
	public void resetAllNNGradients() {
		for (NeuralNetworkCore net : this.nets) {
			net.resetGrad();
		}
	}
	
}
