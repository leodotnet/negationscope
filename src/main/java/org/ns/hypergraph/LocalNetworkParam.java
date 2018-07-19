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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ns.hypergraph.neural.NeuralIO;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

//one thread should have one such LocalFeatureMap.
/**
 * The set of parameters (such as weights, training method, optimizer, etc.) for each thread
 * @author Wei Lu <luwei@statnlp.com>
 *
 */
public class LocalNetworkParam implements Serializable{
	
	private static final long serialVersionUID = -2097104968915519992L;
	
	//the id of the thread.
	protected int _threadId;
	//the feature manager.
	protected FeatureManager _fm;
	//the version number.
	protected int _version;
	
	//the partial objective function.
	protected double _obj;
	//the local feature sparse array.
	protected int[] _fs;
	//the counts for all the features.
	protected double[] _counts;
	/**
	 * Mapping from global features to local features. Used when extracting features.
	 * If cache is used, this one can be discarded after touch process completes.
	 */
	protected TIntIntHashMap _globalFeature2LocalFeature;
	//check if it is finalized.
	protected boolean _isFinalized;
	
	//the cache that stores the features
	protected FeatureArray[][][] _cache;
	//the cache that stores the features
	protected Double[][][] _costCache;
	//check whether the cache is enabled.
	protected boolean _cacheEnabled = true;
	//the cache that store the input-output pair in each hyperedge and neural network
	protected NeuralIO[][][][] _neuralCache;
	
	protected List<Map<Object,Integer>> localNNInput2Id;
	protected List<TIntObjectMap<Set<Object>>> localInstId2NNInput;
	
	protected int _numNetworks;
	
	//if this is true, then we bypass the local params.
	protected boolean _globalMode;
	
	protected StringIndex _stringIndex;
	protected TIntIntHashMap _localStr2Global;
	
	/**
	 * For memory-optimized, map an integer array object to a feature array object
	 * avoid creating duplicate integer array. 
	 * **/
	protected Map<FeatureBox, FeatureBox> fbMap;
	
	public LocalNetworkParam(int threadId, FeatureManager fm, int numNetworks){
		this._threadId = threadId;
		this._numNetworks = numNetworks;
		this._fm = fm;
		this._obj = 0.0;
		this._fs = null;
		//this gives you the mapping from global to local features.
		this._globalFeature2LocalFeature = new TIntIntHashMap();
		this._isFinalized = false;
		this._version = 0;
		this._globalMode = false;
		if (NetworkConfig.USE_NEURAL_FEATURES && fm._param_g.getNNParamG() != null) {
			this._neuralCache = new NeuralIO[fm._param_g.getNNParamG().getAllNets().size()][][][];
			this.initializeLocalNNInput2Id(fm._param_g.getNNParamG().getAllNets().size());
			if (NetworkConfig.USE_BATCH_TRAINING && fm._param_g.getNNParamG().isLearningState()) {
				this.initializeLocalInstId2Input(fm._param_g.getNNParamG().getAllNets().size());
			}
		}
		if(!NetworkConfig.CACHE_FEATURES_DURING_TRAINING){
			this.disableCache();
		}
		if(NetworkConfig.NUM_THREADS == 1){
			this._globalMode = true;
		}
		this._stringIndex = new StringIndex(numNetworks*100);

	}
	
	//when doing testing, we can set it to global model for improved efficiency.
	//the reason that we can do this global mode is because we don't update the params
	//during testing.
	public void setGlobalMode(){
		this._globalMode = true;
		this._globalFeature2LocalFeature = null;
	}
	
	//check whether it is in global mode.
	public boolean isGlobalMode(){
		return this._globalMode;
	}
	
	public void initializeLocalNNInput2Id(int numNets) {
		this.localNNInput2Id = new ArrayList<>(numNets);
		for (int i = 0; i < numNets; i++) {
			this.localNNInput2Id.add(new HashMap<>());
		}
	}
	
	public void initializeLocalInstId2Input(int numNets) {
		this.localInstId2NNInput = new ArrayList<>(numNets);
		for (int i = 0; i < numNets; i++) {
			this.localInstId2NNInput.add(new TIntObjectHashMap<>());
		}
	}
	
	public int toLocalFeature(int f_global){
		if(this._globalMode){
			throw new RuntimeException("The current mode is global mode, converting a global feature to a local feature is not supported.");
		}
		//if it is not really a valid feature.
		if(f_global == -1){
			throw new RuntimeException("LocalNetworkParam receives invalid feature [-1] to be converted into local feature");
//			return -1;
		}
		if(!this._globalFeature2LocalFeature.containsKey(f_global)){
			if(this._isFinalized){
				throw new NetworkException("New global feature ["+f_global+"] encountered after parameters are finalized");
			}
			this._globalFeature2LocalFeature.put(f_global, this._globalFeature2LocalFeature.size());
		}
		return this._globalFeature2LocalFeature.get(f_global);
	}
	
	public int[] getFeatures(){
		return this._fs;
	}
	
	//get the version.
	public int getVersion(){
		return this._version;
	}
	
	//get the id of the thread.
	public int getThreadId(){
		return this._threadId;
	}
	
	//get the objective value.
	public double getObj(){
		return this._obj;
	}
	
	//add the objective value by a certain value.
	public void addObj(double obj){
		if(this._globalMode){
			this._fm.getParam_G().addObj(obj);
			return;
		}
		this._obj += obj;
	}
	
	/**
	 * Get the weight of a feature identified by the feature ID
	 * @param featureID
	 * @return
	 */
	public double getWeight(int featureID){
		if(this.isGlobalMode()){
			//note that during testing, it is global mode.
			return this._fm.getParam_G().getWeight(featureID);
		} else {
			//so _fs[featureID] must be the global feature Index
			//featureID is the local feature index.
			//System.out.println(featureID+" "+this._fs[featureID]+" "+ Arrays.toString(this._fm.getParam_G().getFeatureRep(this._fs[featureID])));
			return this._fm.getParam_G().getWeight(this._fs[featureID]);
		}
	}
	
	/**
	 * The number of features.
	 * @return
	 */
	public int size(){
		return this._fs.length;
	}
	
	public void addCount(int f_local, double count){
//		if(this._globalMode){
//			throw new RuntimeException("The current mode is global mode, adding counts is not supported.");
//		}
		if(f_local == -1){
			throw new RuntimeException("x");
		}
		
		if(Double.isNaN(count)){
			throw new RuntimeException("NaN");
		}
		if(this._globalMode){
			this._fm.getParam_G().addCount(f_local, count);
			return;
		}
		this._counts[f_local] += count;
		
	}
	
	public double getCount(int f_local){
		if(this._globalMode){
			throw new RuntimeException("It's global mode, why do you do this?");
		}
		return this._counts[f_local];
	}
	
	public void reset(){
		if(this._globalMode){
			return;
		}
		this._obj = 0.0;
		Arrays.fill(this._counts, 0.0);
	}
	
	public void disableCache(){
		this._cache = null;
		this._cacheEnabled = false;
	}
	
	public List<Map<Object,Integer>> getLocalNNInput2Id () {
		return this.localNNInput2Id;
	}
	
	public List<TIntObjectMap<Set<Object>>> getLocalInstId2NNInput () {
		return this.localInstId2NNInput;
	}
	
	public void setLocalNNInput2Id(List<Map<Object,Integer>> localNNInput2Id) {
		this.localNNInput2Id = localNNInput2Id;
	}
	
	public void enableCache(){
		this._cacheEnabled = true;
	}
	
	public boolean isCacheEnabled(){
		return this._cacheEnabled;
	}
	
	public NeuralIO getHyperEdgeIO(Network network, int netId, int parent_k, int children_k_idx) {
		if (this._neuralCache[netId] == null){
			return null;
		}
		if (this._neuralCache[netId][network.getNetworkId()] == null){
			return null;
		}
		if (this._neuralCache[netId][network.getNetworkId()][parent_k] == null){
			return null;
		}
		return this._neuralCache[netId][network.getNetworkId()][parent_k][children_k_idx];
	}
	
	public int toInt(String str){
		return this._stringIndex.getOrPut(str);
	}
	
	/**
	 * Extract hyper edges information for a specific network.
	 * @param network
	 * @param netId
	 * @param parent_k
	 * @param children_k_idx
	 * @param edgeInput
	 * @param output
	 */
	public void addHyperEdge(Network network, int netId, int parent_k, int children_k_idx, Object edgeInput, int output) {
		if (_neuralCache[netId] == null) {
			this._neuralCache[netId] = new NeuralIO[this._numNetworks][][];
		}
		int networkId = network.getNetworkId();
		if (this._neuralCache[netId][networkId] == null) {
			this._neuralCache[netId][networkId] = new NeuralIO[network.countNodes()][];
		}
		if (this._neuralCache[netId][networkId][parent_k] == null) {
			this._neuralCache[netId][networkId][parent_k] = new NeuralIO[network.getChildren(parent_k).length];
		}
		if (this._neuralCache[netId][networkId][parent_k][children_k_idx] != null) {
			throw new NetworkException("nn input-output pair added for this edge, add again?");
		}
		this._neuralCache[netId][networkId][parent_k][children_k_idx] = new NeuralIO(edgeInput, output); 
		Object nnInput = this._fm._param_g.getNNParamG().getAllNets().get(netId).hyperEdgeInput2NNInput(edgeInput);
		if (!localNNInput2Id.get(netId).containsKey(nnInput)) {
			localNNInput2Id.get(netId).put(nnInput, localNNInput2Id.size());
		}
		if (NetworkConfig.USE_BATCH_TRAINING && this._fm._param_g.getNNParamG().isLearningState()) {
			//only need positive instance ID
			int instId = network.getInstance().getInstanceId();
			if (instId > 0) {
				TIntObjectMap<Set<Object>> map =  this.localInstId2NNInput.get(netId);
				if (map.containsKey(instId)) {
					map.get(instId).add(nnInput);
				} else {
					Set<Object> set = new HashSet<>();
					set.add(nnInput);
					map.put(instId, set);
				}
			}
		}
	}
	
	/**
	 * Extract features from the specified network at current hyperedge, specified by its parent node
	 * index (parent_k) and its children node indices (children_k).<br>
	 * The children_k_index represents the index of current hyperedge in the list of hyperedges coming out of
	 * the parent node.<br>
	 * Note that a node with no outgoing hyperedge will still be considered here with empty children_k
	 * @param network
	 * @param parent_k
	 * @param children_k
	 * @param children_k_index
	 * @return
	 */
	public FeatureArray extract(Network network, int parent_k, int[] children_k, int children_k_index){
		// Do not cache in the first touch when parallel touch and extract only from labeled is enabled,
		// since the local feature indices will change
		boolean shouldCache = this.isCacheEnabled() && (!NetworkConfig.PARALLEL_FEATURE_EXTRACTION
														|| NetworkConfig.NUM_THREADS == 1
														|| !NetworkConfig.BUILD_FEATURES_FROM_LABELED_ONLY
														|| this._isFinalized);
  		if(shouldCache){
			if(this._cache == null){
				this._cache = new FeatureArray[this._numNetworks][][];
			}
			if(this._cache[network.getNetworkId()] == null){
				this._cache[network.getNetworkId()] = new FeatureArray[network.countNodes()][];
			}
			if(this._cache[network.getNetworkId()][parent_k] == null){
				this._cache[network.getNetworkId()][parent_k] = new FeatureArray[network.getChildren(parent_k).length];
			}
			if(this._cache[network.getNetworkId()][parent_k][children_k_index] != null){
				return this._cache[network.getNetworkId()][parent_k][children_k_index];
			}
		}
		
		FeatureArray fa = this._fm.extract(network, parent_k, children_k, children_k_index);
		if(!this.isGlobalMode()){
			fa = fa.toLocal(this);
		}
		
		if(shouldCache){
			this._cache[network.getNetworkId()][parent_k][children_k_index] = fa;
		}
		
		return fa;
	}
	
	public double cost(Network network, int parent_k, int[] children_k, int children_k_index, NetworkCompiler compiler){
		// Do not cache in the first touch when parallel touch and extract only from labeled is enabled,
		// since the local feature indices will change
		boolean shouldCache = this.isCacheEnabled();
		if(shouldCache){
			if(this._costCache == null){
				this._costCache = new Double[this._numNetworks][][];
			}
			if(this._costCache[network.getNetworkId()] == null){
				this._costCache[network.getNetworkId()] = new Double[network.countNodes()][];
			}
			if(this._costCache[network.getNetworkId()][parent_k] == null){
				this._costCache[network.getNetworkId()][parent_k] = new Double[network.getChildren(parent_k).length];
			}
			if(this._costCache[network.getNetworkId()][parent_k][children_k_index] != null){
				return this._costCache[network.getNetworkId()][parent_k][children_k_index];
			}
		}
		
		double cost = compiler.cost(network, parent_k, children_k);
		
		if(shouldCache){
			this._costCache[network.getNetworkId()][parent_k][children_k_index] = cost;
		}
		
		return cost;
	}
	
	/**
	 * Finalize the features extracted by copying the local features into global features.<br>
	 * This is not required if this is in global mode, which means the features are stored into
	 * global feature index directly.
	 */
	public void finalizeIt(){
		//if it is global mode, do not have to do this at all.
		if(this.isGlobalMode()){
			System.err.println("Finalizing local features in global mode: not required");
			this._isFinalized = true;
			return;
		}
		this._fs = new int[this._globalFeature2LocalFeature.size()];
		for(int f_global: this._globalFeature2LocalFeature.keys()){
			int f_local = this._globalFeature2LocalFeature.get(f_global);
			this._fs[f_local] = f_global;
		}
		this._isFinalized = true;
		this._counts = new double[this._fs.length];
		System.err.println("Finalized local param. size:"+this._fs.length);
	}

}
