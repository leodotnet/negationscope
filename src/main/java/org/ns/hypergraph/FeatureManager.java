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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.ns.util.instance_parser.InstanceParser;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * The base class for the feature manager.
 * The only function to be implemented is the {@link #extract_helper(Network, int, int[])} method.
 * @author Wei Lu <luwei@statnlp.com>
 *
 */
public abstract class FeatureManager implements Serializable{
	
	private static final long serialVersionUID = 7999836838043433954L;
	
	/** The number of networks. */
	protected transient int _numNetworks;
	/**
	 * The cache that stores the features for each network and each edge (an edge is specified by its parent
	 * node index and the edge index)
	 */
	protected transient FeatureArray[][][] _cache;
	
	/**
	 * The parameters associated with the network.
	 */
	protected GlobalNetworkParam _param_g;
	
	/**
	 * The parser used to parse the instance. This might hold important information about the data statistics
	 * that might be used in extracting features.
	 */
	protected InstanceParser _instanceParser;
	/**
	 * The local feature maps, one for each thread.
	 */
	protected transient LocalNetworkParam[] _params_l;
	/** A flag specifying whether the cache is enabled. */
	protected boolean _cacheEnabled = false;
	
	protected int _numThreads;
	
	public FeatureManager(GlobalNetworkParam param_g){
		this(param_g, null);
	}
	
	public FeatureManager(GlobalNetworkParam param_g, InstanceParser instanceParser){
		this._param_g = param_g;
		this._instanceParser = instanceParser;
		this._numThreads = NetworkConfig.NUM_THREADS;
		this._params_l = new LocalNetworkParam[this._numThreads];
		this._cacheEnabled = false;
	}
	
	public void setLocalNetworkParams(int threadId, LocalNetworkParam param_l){
		this._params_l[threadId] = param_l;
	}
	
	/**
	 * Go through all threads, accumulating the value of the objective function and the gradients, 
	 * and then update the weights to be evaluated next
	 * @return
	 */
	public synchronized boolean update(){
		return update(false);
	}
	
	/**
	 * Go through all threads, accumulating the value of the objective function and the gradients, 
	 * and then update the weights to be evaluated next, unless justUpdateObjectiveAndGradient is <tt>true</tt>,
	 * in which case no new weights are estimated.
	 * @param justUpdateObjectiveAndGradient No weight estimation is done
	 * @return A boolean, telling whether the optimization process is done. Will always return false if
	 * 		   justUpdateObjectiveAndGradient is true.
	 */
	public synchronized boolean update(boolean justUpdateObjectiveAndGradient){
		//if the number of thread is 1, then your local param fetches information directly from the global param.
		if(NetworkConfig.NUM_THREADS != 1){
			this._param_g.resetCountsAndObj();
			
			for(LocalNetworkParam param_l : this._params_l){
				int[] fs = param_l.getFeatures();
				for(int f_local = 0; f_local<fs.length; f_local++){
					int f_global = fs[f_local];
					double count = param_l.getCount(f_local);
					this._param_g.addCount(f_global, count);
				}
				this._param_g.addObj(param_l.getObj());
			}
		}
		if(justUpdateObjectiveAndGradient){
			this._param_g._obj_old = this._param_g._obj;
			return false;
		}
		if (NetworkConfig.USE_NEURAL_FEATURES) {
			this._param_g.getNNParamG().backward();
		}
		
		boolean done = this._param_g.update();

		if(NetworkConfig.NUM_THREADS != 1){
			for(LocalNetworkParam param_l : this._params_l){
				param_l.reset();
			}
		} else {
			this._param_g.resetCountsAndObj();
		}
		return done;
	}
	
	public void enableCache(int numNetworks){
		this._numNetworks = numNetworks;
		this._cache = new FeatureArray[numNetworks][][];
		this._cacheEnabled = true;
	}
	
	public void disableCache(){
		this._cache = null;
		this._cacheEnabled = false;
	}
	
	public boolean isCacheEnabled(){
		return this._cacheEnabled;
	}
	
	/**
	 * Returns the global feature index
	 * @return
	 */
	public GlobalNetworkParam getParam_G(){
		return this._param_g;
	}
	
	/**
	 * Returns the list of local feature index
	 * @return
	 */
	public LocalNetworkParam[] getParams_L(){
		return this._params_l;
	}
	
	/**
	 * Starts the routine to copy all local feature index into global feature index.
	 */
	protected void mergeSubFeaturesToGlobalFeatures(){
		TIntObjectHashMap<TIntObjectHashMap<TIntIntHashMap>> globalFeature2IntMap = this._param_g.getFeatureIntMap();

		this._param_g._size = 0;
		for(int t=0;t<this._param_g._subFeatureIntMaps.size();t++){
			//This method basically filling the _globalFeature2LocalFeature map for each thread.
			addIntoGlobalFeatures(globalFeature2IntMap, this._param_g._subFeatureIntMaps.get(t), this._params_l[t]._globalFeature2LocalFeature, this._params_l[t]._localStr2Global);
			this._param_g._subFeatureIntMaps.set(t, null);
			this._params_l[t]._localStr2Global = null;
		}
		this._param_g._subSize = null;
	}

	/**
	 * Used during parallel touch, this method copies features extracted from each thread into the global feature index.
	 * @param globalMap The global feature index, storing the features from all thread.
	 * 		  map<type, <output, <input, globalFeatureIndex>>> The feature string is comprised by <type+output+input>
	 * @param localMap The local feature index, storing the features from one thread.
	 * @param gf2lf The feature indices mapping from global feature indices to local feature indices.<br>
	 * 				This is used in each local network param to get the correct local feature indices.
	 */
	private void addIntoGlobalFeatures(TIntObjectHashMap<TIntObjectHashMap<TIntIntHashMap>> globalMap,
			TIntObjectHashMap<TIntObjectHashMap<TIntIntHashMap>> localMap, TIntIntHashMap gf2lf,
			TIntIntHashMap ls2gs){
		for(int localType: localMap.keys()){
			TIntObjectHashMap<TIntIntHashMap> localOutput2input = localMap.get(localType);
			localType = ls2gs.get(localType);
			if(!globalMap.containsKey(localType)){
				globalMap.put(localType, new TIntObjectHashMap<TIntIntHashMap>());
			}
			TIntObjectHashMap<TIntIntHashMap> globalOutput2input = globalMap.get(localType);
			for(int localOutput: localOutput2input.keys()){
				TIntIntHashMap localInput2int = localOutput2input.get(localOutput);
				localOutput = ls2gs.get(localOutput);
				if(!globalOutput2input.containsKey(localOutput)){
					globalOutput2input.put(localOutput, new TIntIntHashMap());
				}
				TIntIntHashMap globalInput2int = globalOutput2input.get(localOutput);
				for(int localInput: localInput2int.keys()){
					int featureId = localInput2int.get(localInput);
					localInput = ls2gs.get(localInput);
					if(!globalInput2int.containsKey(localInput)){
						globalInput2int.put(localInput, this._param_g._size++);
					}
					gf2lf.put(globalInput2int.get(localInput), featureId);
				}
			}
		}
	}

	/**
	 * Used for parallel touch when training from labeled only<br>
	 * This method copies the features from global feature index into the local feature index specified,
	 * if the features are not already present in the local feature index.
	 * @param globalFeaturesToLocalFeatures The mapping from global feature indices into local feature indices
	 */
	protected void addIntoLocalFeatures(TIntIntHashMap globalFeaturesToLocalFeatures){
		TIntObjectHashMap<TIntObjectHashMap<TIntIntHashMap>> globalMap = this._param_g.getFeatureIntMap();
		for(int type: globalMap.keys()){
			TIntObjectHashMap<TIntIntHashMap> outputToInputToIndex = globalMap.get(type);
			for(int output: outputToInputToIndex.keys()){
				TIntIntHashMap inputToIndex = outputToInputToIndex.get(output);
				for(int featureIndex: inputToIndex.values()){
					if(!globalFeaturesToLocalFeatures.containsKey(featureIndex)){
						globalFeaturesToLocalFeatures.put(featureIndex, globalFeaturesToLocalFeatures.size());
					}
				}
			}
		}
	}

	/**
	 * Used during generative training, this method completes the cross product between the type features and 
	 * the input features.
	 */
	protected void completeType2Int(){
		TIntObjectHashMap<TIntObjectHashMap<TIntIntHashMap>> globalMap = this._param_g._featureIntMap;
		TIntObjectHashMap<ArrayList<Integer>> type2Input = this._param_g._type2inputMap;
		for(int type: globalMap.keys()){
			if(!type2Input.containsKey(type)){
				type2Input.put(type, new ArrayList<Integer>());
			}
			ArrayList<Integer> inputs = type2Input.get(type);
			TIntObjectHashMap<TIntIntHashMap> output2input  = globalMap.get(type);
			for(int output: output2input.keys()){
				TIntIntHashMap input2int = output2input.get(output);
				for(int input: input2int.keys()){
					int index = Collections.binarySearch(inputs, input);
					if(index<0){
						inputs.add(-1-index, input);
					}
				}
			}
		}
	}

	/**
	 * Extract the features from the specified network at a hyperedge, specified by its parent index
	 * and child indices, caching if necessary.<br>
	 * <code>children_k</code> is the child node indices of the current hyperedge in this network 
	 * with the parent as the root node (the "tail", following Gallo et al. (1993) notation).<br>
	 * The <code>children_k_index</code> specifies the index of the child (<code>children_k</code>) 
	 * in the parent's list of children. This is mainly used for caching purpose.<br>
	 * Note that nodes with no outgoing hyperedge are still considered here, with empty children_k 
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
														|| this._param_g.isLocked());
		if(shouldCache){
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
		
		FeatureArray fa = this.extract_helper(network, parent_k, children_k, children_k_index);
		
		if(shouldCache){
			this._cache[network.getNetworkId()][parent_k][children_k_index] = fa;
		}
		return fa;
	}
	
	/**
	 * Extract the features from the specified network, parent index, and child indices<br>
	 * <code>children_k</code> is the child nodes of a SINGLE hyperedge in this network 
	 * with the parent as the root node.<br>
	 * Note that nodes with no outgoing hyperedge are still considered here, with empty children_k 
	 * @param network The network
	 * @param parent_k The node index of the parent node
	 * @param children_k The node indices of the children of a SINGLE hyperedge
	 * @return
	 */
	protected abstract FeatureArray extract_helper(Network network, int parent_k, int[] children_k, int children_k_index);

	/**
	 * Creates a FeatureArray object based on the feature indices given, possibly with caching to ensure no duplicate
	 * FeatureArray objects are created with the exact same sequence of featureIndices.<br>
	 * The caching can be enabled by setting {@link NetworkConfig#AVOID_DUPLICATE_FEATURES} to true.<br>
	 * @param network Required to handle the FeatureArray object cache (this cache is different from FeatureArray position cache)
	 * @param featureIndices The feature indices for this FeatureArray object
	 * @return
	 */
	public FeatureArray createFeatureArray(Network network, Collection<Integer> featureIndices){
		return createFeatureArray(network, featureIndices, null, null);
	}
	
	/**
	 * Creates a FeatureArray object based on the feature indices given, possibly with caching to ensure no duplicate
	 * FeatureArray objects are created with the exact same sequence of featureIndices.<br>
	 * The caching can be enabled by setting {@link NetworkConfig#AVOID_DUPLICATE_FEATURES} to true.<br>
	 * @param network Required to handle the FeatureArray object cache (this cache is different from FeatureArray position cache)
	 * @param featureIndices The feature indices for this FeatureArray object
	 * @param featureValues The feature values for this FeatureArray object
	 * @return
	 */
	public FeatureArray createFeatureArray(Network network, Collection<Integer> featureIndices, Collection<Double> featureValues){
		return createFeatureArray(network, featureIndices, featureValues, null);
	}
	
	/**
	 * Creates a FeatureArray object based on the feature indices given, possibly with caching to ensure no duplicate
	 * FeatureArray objects are created with the exact same sequence of featureIndices.<br>
	 * The caching can be enabled by setting {@link NetworkConfig#AVOID_DUPLICATE_FEATURES} to true.<br>
	 * @param network Required to handle the FeatureArray object cache (this cache is different from FeatureArray position cache)
	 * @param featureIndices The feature indices for this FeatureArray object
	 * @param next Another FeatureArray object to be chained after the newly created FeatureArray object.
	 * @return
	 */
	public FeatureArray createFeatureArray(Network network, Collection<Integer> featureIndices, FeatureArray next){
		return createFeatureArray(network, featureIndices, null, next);
	}
	
	/**
	 * Creates a FeatureArray object based on the feature indices given, possibly with caching to ensure no duplicate
	 * FeatureArray objects are created with the exact same sequence of featureIndices.<br>
	 * The caching can be enabled by setting {@link NetworkConfig#AVOID_DUPLICATE_FEATURES} to true.<br>
	 * @param network Required to handle the FeatureArray object cache (this cache is different from FeatureArray position cache)
	 * @param featureIndices The feature indices for this FeatureArray object
	 * @param featureValues The feature values for this FeatureArray object
	 * @param next Another FeatureArray object to be chained after the newly created FeatureArray object.
	 * @return
	 */
	public FeatureArray createFeatureArray(Network network, Collection<Integer> featureIndices, Collection<Double> featureValues, FeatureArray next){
		int[] features = new int[featureIndices.size()];
		int i = 0;
		for(Iterator<Integer> iter = featureIndices.iterator(); iter.hasNext();){
			features[i] = iter.next();
			i += 1;
		}
		double[] fvs = null;
		if (featureValues != null) {
			fvs = new double[featureValues.size()];
			i = 0;
			for(Iterator<Double> iter = featureValues.iterator(); iter.hasNext();){
				fvs[i] = iter.next();
				i += 1;
			}
		}
		return createFeatureArray(network, features, fvs, next);
	}
	
	/**
	 * Creates a FeatureArray object based on the feature indices given, possibly with caching to ensure no duplicate
	 * FeatureArray objects are created with the exact same sequence of featureIndices.<br>
	 * The caching can be enabled by setting {@link NetworkConfig#AVOID_DUPLICATE_FEATURES} to true.<br>
	 * @param network Required to handle the FeatureArray object cache (this cache is different from FeatureArray position cache)
	 * @param featureIndices The feature indices for this FeatureArray object
	 * @return
	 */
	public FeatureArray createFeatureArray(Network network, int[] featureIndices){
		return createFeatureArray(network, featureIndices, null);
	}
	
	/**
	 * Creates a FeatureArray object based on the feature indices given, possibly with caching to ensure no duplicate
	 * FeatureArray objects are created with the exact same sequence of featureIndices.<br>
	 * The caching can be enabled by setting {@link NetworkConfig#AVOID_DUPLICATE_FEATURES} to true.<br>
	 * @param network Required to handle the FeatureArray object cache (this cache is different from FeatureArray position cache)
	 * @param featureIndices The feature indices for this FeatureArray object
	 * @param fvs The feature values for this FeatureArray object
	 * @return
	 */
	public FeatureArray createFeatureArray(Network network, int[] featureIndices, double[] fvs){
		if (!network.getInstance().isLabeled() && network.getInstance().getInstanceId() > 0) {
			//testing instance, also new feature array
			return new FeatureArray(featureIndices, fvs);
		}
		if(NetworkConfig.AVOID_DUPLICATE_FEATURES){
			return new FeatureArray(FeatureBox.getFeatureBox(featureIndices, fvs, this.getParams_L()[network.getThreadId()]));
		} else {
			return new FeatureArray(featureIndices);
		}
	}
	
	/**
	 * Creates a FeatureArray object based on the feature indices given, possibly with caching to ensure no duplicate
	 * FeatureArray objects are created with the exact same sequence of featureIndices.<br>
	 * The caching can be enabled by setting {@link NetworkConfig#AVOID_DUPLICATE_FEATURES} to true.<br>
	 * @param network Required to handle the FeatureArray object cache (this cache is different from FeatureArray position cache)
	 * @param featureIndices The feature indices for this FeatureArray object
	 * @param fvs The feature values for this FeatureArray object
	 * @param next Another FeatureArray object to be chained after the newly created FeatureArray object.
	 * @return
	 */
	public FeatureArray createFeatureArray(Network network, int[] featureIndices, double[] fvs, FeatureArray next){
		if (!network.getInstance().isLabeled() && network.getInstance().getInstanceId() > 0) {
			//testing instance, also new feature array
			return new FeatureArray(featureIndices, fvs, next);
		}
		if(NetworkConfig.AVOID_DUPLICATE_FEATURES){
			return new FeatureArray(FeatureBox.getFeatureBox(featureIndices, fvs, this.getParams_L()[network.getThreadId()]), next);
		} else {
			return new FeatureArray(featureIndices, fvs, next);
		}
	}
	
	/**
	 * Saving the mappings from hyper edges to input-output pair in local network param cache.
	 * @param network:
	 * @param netId: specify add this neural feature to which neural network.
	 * @param parent_k
	 * @param children_k_idx
	 * @param edgeInput
	 * @param output
	 */
	public void addNeural(Network network, int netId, int parent_k, int children_k_idx, Object edgeInput, int output) {
		this._params_l[network.getThreadId()].addHyperEdge(network, netId, parent_k, children_k_idx, edgeInput, output);
	}
	
	private void writeObject(ObjectOutputStream oos) throws IOException{
		oos.writeObject(this._param_g);
		oos.writeBoolean(this._cacheEnabled);
		oos.writeInt(this._numThreads);
	}
	
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException{
		this._param_g = (GlobalNetworkParam)ois.readObject();
		this._cacheEnabled = ois.readBoolean();
		this._numThreads = ois.readInt();
		this._params_l = new LocalNetworkParam[NetworkConfig.NUM_THREADS];
	}
	
	public static String NEURAL_FEATURE_TYPE_PREFIX = "neural";
	
}