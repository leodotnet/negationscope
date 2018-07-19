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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.ns.commons.ml.opt.GradientDescentOptimizer;
import org.ns.commons.ml.opt.LBFGS;
import org.ns.commons.ml.opt.MathsVector;
import org.ns.commons.ml.opt.Optimizer;
import org.ns.commons.ml.opt.OptimizerFactory;
import org.ns.commons.ml.opt.LBFGS.ExceptionWithIflag;
import org.ns.hypergraph.NetworkConfig.StoppingCriteria;
import org.ns.hypergraph.neural.AbstractNeuralNetwork;
import org.ns.hypergraph.neural.GlobalNeuralNetworkParam;
import org.ns.hypergraph.neural.NeuralNetworkCore;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

//TODO: other optimization and regularization methods. Such as the L1 regularization.

/**
 * The set of parameters (such as weights, training method, optimizer, etc.) in the global scope
 * @author Wei Lu <luwei@statnlp.com>
 *
 */
public class GlobalNetworkParam implements Serializable{
	
	private static final long serialVersionUID = -1216927656396018976L;
	
	//these parameters are used for discriminative training using LBFGS.
	/** The L2 regularization parameter weight */
	protected transient double _kappa;
	/** The optimizer */
	protected transient Optimizer _opt;
	/** The optimizer factory */
	protected transient OptimizerFactory _optFactory;
	/** The gradient for each dimension */
	protected transient double[] _counts;
	/** A variable to store previous value of the objective function */
	protected transient double _obj_old;
	/** A variable to store current value of the objective function */
	protected transient double _obj;
	/** A variable for batch SGD optimization, if applicable */
	protected transient int _batchSize;
	
	protected transient int _version;
	
	/** Map from feature type to [a map from output to [a map from input to feature ID]] */
	protected TIntObjectHashMap<TIntObjectHashMap<TIntIntHashMap>> _featureIntMap;

	/** Map from feature type to input */
	protected TIntObjectHashMap<ArrayList<Integer>> _type2inputMap;
	/** A feature int map (similar to {@link #_featureIntMap}) for each local thread */
	protected ArrayList<TIntObjectHashMap<TIntObjectHashMap<TIntIntHashMap>>> _subFeatureIntMaps;
	/** The size of each feature int maps for each local thread */
	protected int[] _subSize;
	
	protected StringIndex _stringIndex;
	
	protected int[][] _feature2rep;//three-dimensional array representation of the feature.
	/** The weights parameter */
	protected double[] _weights;
	/** A flag whether the model is discriminative */
	protected boolean _isDiscriminative;
	
	/**
	 * The current number of features that will be updated as the process goes.
	 * @see #_fixedFeaturesSize
	 */
	protected int _size;
	/**
	 * The final number of features
	 * @see #_size
	 */
	protected int _fixedFeaturesSize;
	/** A flag describing whether the set of features is already fixed */
	protected boolean _locked = false;
	
	/** A counter for how many consecutive times the decrease in objective value is less than 0.01% */
	protected int smallChangeCount = 0;
	/** The total number of instances for the coefficient the batch SGD regularization term*/
	protected int totalNumInsts;
	
	/** The weights that some of them will be replaced by neural net if NNCRF is enabled. */
	private transient double[] concatWeights, concatCounts;
	
	protected GlobalNeuralNetworkParam _nn_param_g;
	
	public GlobalNetworkParam(){
		this(OptimizerFactory.getLBFGSFactory());
	}
	
	public GlobalNetworkParam(OptimizerFactory optimizerFactory) {
		this(optimizerFactory, new GlobalNeuralNetworkParam());
	}
	
	public GlobalNetworkParam(OptimizerFactory optimizerFactory, GlobalNeuralNetworkParam nn_param_g){
		this._locked = false;
		this._version = -1;
		this._size = 0;
		this._fixedFeaturesSize = 0;
		this._obj_old = Double.NEGATIVE_INFINITY;
		this._obj = Double.NEGATIVE_INFINITY;
		this._isDiscriminative = !NetworkConfig.TRAIN_MODE_IS_GENERATIVE;
		if(this.isDiscriminative()){
			this._batchSize = NetworkConfig.BATCH_SIZE;
			this._kappa = NetworkConfig.L2_REGULARIZATION_CONSTANT;
		}
		this._featureIntMap = new TIntObjectHashMap<TIntObjectHashMap<TIntIntHashMap>>();
		if(NetworkConfig.TRAIN_MODE_IS_GENERATIVE){
			this._type2inputMap = new TIntObjectHashMap<ArrayList<Integer>>();
		}
		this._optFactory = optimizerFactory;
		if (NetworkConfig.PARALLEL_FEATURE_EXTRACTION && NetworkConfig.NUM_THREADS > 1){
			this._subFeatureIntMaps = new ArrayList<TIntObjectHashMap<TIntObjectHashMap<TIntIntHashMap>>>();
			for (int i = 0; i < NetworkConfig.NUM_THREADS; i++){
				this._subFeatureIntMaps.add(new TIntObjectHashMap<TIntObjectHashMap<TIntIntHashMap>>());
			}
			this._subSize = new int[NetworkConfig.NUM_THREADS];
		}
		this._nn_param_g = nn_param_g;
		if (!NetworkConfig.PARALLEL_FEATURE_EXTRACTION || NetworkConfig.NUM_THREADS == 1) {
			this._stringIndex = new StringIndex();
		}
	}
	
	public void mergeStringIndex(LocalNetworkLearnerThread[] learners){
		if(_stringIndex != null){
			return;
		}
		StringIndex[] stringIndexes = new StringIndex[learners.length];
		for(LocalNetworkLearnerThread learner: learners){
			stringIndexes[learner.getThreadId()] = learner.getLocalNetworkParam()._stringIndex;
		}
		this._stringIndex = StringIndex.merge(stringIndexes);
		for(LocalNetworkLearnerThread learner: learners){
			TIntIntHashMap localStr2Global = new TIntIntHashMap();
			StringIndex localIndex = learner.getLocalNetworkParam()._stringIndex;
			for(String key: stringIndexes[learner.getThreadId()].keys()){
				localStr2Global.put(localIndex.get(key), this._stringIndex.get(key));
			}
			learner.getLocalNetworkParam()._localStr2Global = localStr2Global;
			learner.getLocalNetworkParam()._stringIndex = null;
		}
		this._stringIndex.lock();
	}
	
	public int toInt(String s){
		return this._stringIndex.getOrPut(s);
	}
	
	public StringIndex getStringIndex(){
		return _stringIndex;
	}
	
	/**
	 * Get the map from feature type to [a map from output to [a map from input to feature ID]]
	 * @return
	 */
	public TIntObjectHashMap<TIntObjectHashMap<TIntIntHashMap>> getFeatureIntMap(){
		return this._featureIntMap;
	}
	
	public double[] getWeights(){
		return this._weights;
	}
	
	public void setWeights(double[] newWeights){
		this._weights = newWeights;
	}
	
	/**
	 * Return the current number of features
	 * @see #countFixedFeatures()
	 * @return
	 */
	public int countFeatures(){
		return this._size;
	}
	
	/**
	 * Return the final number of features
	 * @return
	 * @see #countFeatures()
	 */
	public int countFixedFeatures(){
		return this._fixedFeaturesSize;
	}
	
	public boolean isFixed(int f_global){
		return f_global < this._fixedFeaturesSize;
	}
	
	/**
	 * Return the String[] representation of the feature with the specified index
	 * @param f_global
	 * @return
	 */
	public int[] getFeatureRep(int f_global){
		if(!this._storeFeatureReps) return null;
		return this._feature2rep[f_global];
	}
	
	private boolean _storeFeatureReps = false;
	
	public void setStoreFeatureReps() {
		this._storeFeatureReps = true;
	}
	
	/**
	 * Add certain value to the specified feature (identified by the id)
	 * @param feature
	 * @param count
	 */
	public synchronized void addCount(int feature, double count){
		if(Double.isNaN(count)){
			throw new RuntimeException("count is NaN.");
		}
		
		if(this.isFixed(feature))
			return;
		//if the model is discriminative model, we will flip the sign for
		//the counts because we will need to use LBFGS.
		if(this.isDiscriminative()){
			this._counts[feature] -= count;
		} else {
			this._counts[feature] += count;
		}
		
	}
	
	public synchronized void addObj(double obj){
		this._obj += obj;
	}
	
	public double getObj(){
		return this._obj;
	}
	
	public double getObj_old(){
		return this._obj_old;
	}
	
	private double getCount(int f){
		return this._counts[f];
	}
	
	public double getWeight(int f){
		//if the feature is just newly created, for example, return the initial weight, which is zero.
//		if(f>=this._weights.length)
//			return NetworkConfig.FEATURE_INIT_WEIGHT;
		return this._weights[f];
	}
	
	/**
	 * Set a weight at the specified index if it is not fixed yet
	 * @param f
	 * @param weight
	 * @see #overRideWeight(int, double)
	 */
	public synchronized void setWeight(int f, double weight){
		if(this.isFixed(f)) return;
		this._weights[f] = weight;
	}
	
	/**
	 * Force set a weight at the specified index
	 * @param f
	 * @param weight
	 * @see #setWeight(int, double)
	 */
	public synchronized void overRideWeight(int f, double weight){
		this._weights[f] = weight;
	}
	
	public void unlock(){
		if(!this.isLocked())
			throw new RuntimeException("This param is not locked.");
		this._locked = false;
	}
	
	public void unlockForNewFeaturesAndFixCurrentFeatures(){
		if(!this.isLocked())
			throw new RuntimeException("This param is not locked.");
		this.fixCurrentFeatures();
		this._locked = false;
	}
	
	public void fixCurrentFeatures(){
		this._fixedFeaturesSize = this._size;
	}
	
	/**
	 * Expand the feature set to include possible combinations not seen during training.
	 * Only works for non-discriminative model
	 */
	private void expandFeaturesForGenerativeModelDuringTesting(){
//		this.unlockForNewFeaturesAndFixCurrentFeatures();
		
		System.err.println("==EXPANDING THE FEATURES===");
		System.err.println("Before expansion:"+this.size());
		for(int type_id: this._featureIntMap.keys()){
			TIntObjectHashMap<TIntIntHashMap> outputId2inputId = this._featureIntMap.get(type_id);
			ArrayList<Integer> input_ids = this._type2inputMap.get(type_id);
			System.err.println("Feature of type "+type_id+" has "+input_ids.size()+" possible inputs.");
			for(int output_id: outputId2inputId.keys()){
				for(int input_id : input_ids){
					this.toFeature(null, type_id, output_id, input_id);
				}
			}
		}
		System.err.println("After expansion:"+this.size());
		
//		this.lockIt();
	}
	
	/**
	 * Lock the features but keep existing feature weights (for whatever reasons)
	 * @deprecated Please use {@link #lockIt(boolean)} instead.
	 */
	@Deprecated
	public void lockItAndKeepExistingFeatureWeights(){
		lockIt(true);
	}

	private void initWeights(double[] weights, int from, int to){
		if(NetworkConfig.RANDOM_INIT_WEIGHT){
			Random r = new Random(NetworkConfig.RANDOM_INIT_FEATURE_SEED);
			for(int k=from; k<to; k++){
				weights[k] = (r.nextDouble()-0.5)/10;
			}
		} else {
			for(int k=from; k<to; k++){
				weights[k] = NetworkConfig.FEATURE_INIT_WEIGHT;
			}
		}
	}

	/**
	 * Lock current features.
	 * If this is locked it means no new features will be allowed.
	 */
	public void lockIt(){
		lockIt(false);
	}
	
	/**
	 * Lock current features.
	 * If this is locked it means no new features will be allowed.
	 * @param keepExistingWeights Whether to keep existing weights
	 */
	public void lockIt(boolean keepExistingWeights){
		if(this.isLocked()) return;
		
		if(!this.isDiscriminative()){
			this.expandFeaturesForGenerativeModelDuringTesting();
		}
		
		double[] weights_new = new double[this._size];
		this._counts = new double[this._size];
		int numWeightsKept;
		if(keepExistingWeights){
			numWeightsKept = this._weights.length;
		} else {
			numWeightsKept = this._fixedFeaturesSize;
		}
		initWeights(weights_new, numWeightsKept, this._size);
		this._weights = weights_new;
		
		this._nn_param_g.initializeNetwork();
		
		/** Must prepare the feature map before reset counts and obj
		 * The reset will use feature2rep.
		 * **/
		if(this._storeFeatureReps){
			this._feature2rep = new int[this._size][];
			for(int type: this._featureIntMap.keys()){
				TIntObjectHashMap<TIntIntHashMap> output2input = this._featureIntMap.get(type);
				for(int output: output2input.keys()){
					TIntIntHashMap input2id = output2input.get(output);
					for(int input: input2id.keys()){
						int id = input2id.get(input);
						this._feature2rep[id] = new int[]{type, output, input};
					}
				}
			}
		}
		this.resetCountsAndObj();
		/**********/
		
		this._version = 0;
		this._stringIndex.lock();
		int numWeights = this._weights.length + this.getAllNNParamSize();
		this._opt = this._optFactory.create(numWeights, getFeatureIntMap(), this._stringIndex);
		this._locked = true;
		
		System.err.println(this._size+" features.");
		
	}
	
	public int size(){
		return this._size;
	}
	
	public boolean isLocked(){
		return this._locked;
	}
	
	public void setVersion(int version){
		this._version = version;
	}
	
	public int getVersion(){
		return this._version;
	}
	
	/**
	 * Converts a tuple of feature type, input, and output into the feature index.
	 * @param type
	 * @param output
	 * @param input
	 * @return
	 * @deprecated Please use {@link #toFeature(Network, String, String, String)} instead.
	 */
	public int toFeature(String type , String output , String input){
		return this.toFeature(null, type, output, input);
	}
	
	/**
	 * Converts a tuple of feature type, input, and output into the feature index.
	 * @param type_id
	 * @param output_id
	 * @param input_id
	 * @return
	 * @deprecated Please use {@link #toFeature(Network, int, int, int)} instead.
	 */
	@Deprecated
	public int toFeature(int type_id, int output_id, int input_id){
		return toFeature(null, type_id, output_id, input_id);
	}
	
	/**
	 * Converts a tuple of feature type, input, and output into the feature index.
	 * @param type The feature type (e.g., "EMISSION", "FEATURE_1", etc.)
	 * @param output The string representing output label associated with this feature. 
	 * 				 Note that this does not have to be the surface form of the label, as
	 * 				 any distinguishing string value will work (so, instead of "NN", "DT", 
	 * 				 you can just as well put the indices, like "0", "1")
	 * @param input The input (e.g., for emission feature in HMM this might be the word itself) 
	 * @return
	 */
	public int toFeature(Network network , String type , String output , String input){
		int type_id = _stringIndex == null ? network._param.toInt(type) : toInt(type);
		int output_id = _stringIndex == null ? network._param.toInt(output) : toInt(output);
		int input_id = _stringIndex == null ? network._param.toInt(input) : toInt(input);
		return toFeature(network, type_id, output_id, input_id);
	}

	/**
	 * Converts a tuple of feature type, input, and output into the feature index.
	 * @param type_id The feature type (e.g., "EMISSION", "FEATURE_1", etc.)
	 * @param output_id The string representing output label associated with this feature. 
	 * 				 Note that this does not have to be the surface form of the label, as
	 * 				 any distinguishing string value will work (so, instead of "NN", "DT", 
	 * 				 you can just as well put the indices, like "0", "1")
	 * @param input_id The input (e.g., for emission feature in HMM this might be the word itself) 
	 * @return
	 */
	public int toFeature(Network network, int type_id , int output_id , int input_id){
		int threadId = network != null ? network.getThreadId() : -1;
		boolean shouldNotCreateNewFeature = false;
		try{
			shouldNotCreateNewFeature = (NetworkConfig.BUILD_FEATURES_FROM_LABELED_ONLY && network.getInstance().getInstanceId() < 0);
		} catch (NullPointerException e){
			throw new NetworkException("Missing network on some toFeature calls while trying to extract only from labeled networks.");
		}
		TIntObjectHashMap<TIntObjectHashMap<TIntIntHashMap>> featureIntMap = null;
		if(!NetworkConfig.PARALLEL_FEATURE_EXTRACTION || NetworkConfig.NUM_THREADS == 1 || this.isLocked()){
			featureIntMap = this._featureIntMap;
		} else {
			if(threadId == -1){
				throw new NetworkException("Missing network on some toFeature calls while in parallel touch.");
			}
			featureIntMap = this._subFeatureIntMaps.get(threadId);
		}
		
		//if it is locked, then we might return a dummy feature
		//if the feature does not appear to be present.
		if(this.isLocked() || shouldNotCreateNewFeature){
			return this.getFeatureId(type_id, output_id, input_id, featureIntMap);
		}
		
		if(!featureIntMap.containsKey(type_id)){
			featureIntMap.put(type_id, new TIntObjectHashMap<TIntIntHashMap>());
		}
		
		TIntObjectHashMap<TIntIntHashMap> outputToInputToIdx = featureIntMap.get(type_id);
		if(!outputToInputToIdx.containsKey(output_id)){
			outputToInputToIdx.put(output_id, new TIntIntHashMap());
		}
		
		TIntIntHashMap inputToIdx = outputToInputToIdx.get(output_id);
		if(!inputToIdx.containsKey(input_id)){
			if(!NetworkConfig.PARALLEL_FEATURE_EXTRACTION || NetworkConfig.NUM_THREADS == 1){
				inputToIdx.put(input_id, this._size++);
			} else {
				inputToIdx.put(input_id, this._subSize[threadId]++);
			}
		}

		return inputToIdx.get(input_id);
	}

	/**
	 * Returns the feature ID of the specified feature from the global feature index.<br>
	 * If the feature is not present in the feature index, return -1.
	 * @param type The feature type
	 * @param output The feature output type
	 * @param input The feature input type
	 * @return
	 */
	public int getFeatureId(String type, String output, String input){
		return getFeatureId(toInt(type), toInt(output), toInt(input), this._featureIntMap);
	}

	/**
	 * Returns the feature ID of the specified feature from the specified feature index.<br>
	 * If the feature is not present in the feature index, return -1.
	 * @param type The feature type
	 * @param output The feature output type
	 * @param input The feature input type
	 * @param featureIntMap The feature index
	 * @return
	 */
	public int getFeatureId(int type, int output, int input,
			TIntObjectHashMap<TIntObjectHashMap<TIntIntHashMap>> featureIntMap) {
		if(!featureIntMap.containsKey(type)){
			return -1;
		}
		TIntObjectHashMap<TIntIntHashMap> output2input = featureIntMap.get(type);
		if(!output2input.containsKey(output)){
			return -1;
		}
		TIntIntHashMap input2id = output2input.get(output);
		if(!input2id.containsKey(input)){
			return -1;
		}
		return input2id.get(input);
	}
	
	/**
	 * Globally update the parameters.
	 * This will also set {@link #_obj_old} to the value of {@link #_obj}.
	 * @return true if the optimization is deemed to be finished, false otherwise
	 */
	public synchronized boolean update(){
		boolean done;
		if(this.isDiscriminative()){
			done = this.updateDiscriminative();
		} else {
			done = this.updateGenerative();
		}
		
		this._obj_old = this._obj;
		
		return done;
	}
	
	public double[] getCounts(){
		return this._counts;
	}
	
	/**
	 * Update the weights using generative algorithm (e.g., for HMM)
	 * @return true if the difference between previous and current objective function value
	 * 		   is less than {@link NetworkConfig#objtol}, false otherwise.
	 */
	private boolean updateGenerative(){
		for(int type: this._featureIntMap.keys()){
			TIntObjectHashMap<TIntIntHashMap> output2input = this._featureIntMap.get(type);
			for(int output: output2input.keys()){
				TIntIntHashMap input2feature;
				double sum = 0;
				input2feature = output2input.get(output);
				for(int input: input2feature.keys()){
					int feature = input2feature.get(input);
					sum += this.getCount(feature);
				}
				
				input2feature = output2input.get(output);
				for(int input: input2feature.keys()){
					int feature = input2feature.get(input);
					double value = sum != 0 ? this.getCount(feature)/sum : 1.0/input2feature.size();
					this.setWeight(feature, Math.log(value));
					
					if(Double.isNaN(Math.log(value))){
						throw new RuntimeException("x"+value+"\t"+this.getCount(feature)+"/"+sum+"\t"+input2feature.size());
					}
				}
			}
		}
		boolean done = Math.abs(this._obj-this._obj_old) < NetworkConfig.OBJTOL;
		
		this._version ++;
		
//		System.err.println("Word2count:");
//		Iterator<String> words = word2count.keySet().iterator();
//		while(words.hasNext()){
//			String word = words.next();
//			double count = word2count.get(word);
//			System.err.println(word+"\t"+count);
//		}
//		System.exit(1);
		
		return done;
	}
	
	public List<Double> toList(double[] arr){
		List<Double> result = new ArrayList<Double>();
		for(double num: arr){
			result.add(num);
		}
		return result;
	}
	
	public void setCurrentAsBestParameters () {
		if(this._opt.name().contains("Gradient Descent Optimizer")){
			GradientDescentOptimizer gdOptimizer = (GradientDescentOptimizer)this._opt;
			gdOptimizer.setCurrentAsBestParameters();
		}
	}
	
	/**
	 * Set the best parameters (only used in gradient optimizer.)
	 */
	public void setBestParameters() {
		if(this._opt.name().contains("Gradient Descent Optimizer")){
    		System.out.println("Copying the best parameters");
    		GradientDescentOptimizer gdOptimizer = (GradientDescentOptimizer)this._opt;
    		gdOptimizer.copyBest();
    		if (NetworkConfig.USE_NEURAL_FEATURES) {
        		// De-concatenate into their corresponding weight vectors 
        		int ptr = 0;
        		System.arraycopy(concatWeights, ptr, _weights, 0, _weights.length);
        		ptr += _weights.length;
    			for (AbstractNeuralNetwork net : this._nn_param_g.getAllNets()) {
    				double[] params = net.getParams();
    				double[] gradParams = net.getGradParams();
    				if (params == null || gradParams == null) continue;
    				System.arraycopy(concatWeights, ptr, params, 0, params.length);
    				ptr += params.length;
    			}
        	}
    	}
	}
	
	/**
	 * Update the parameters using discriminative algorithm (e.g., CRF).
	 * If the optimization seems to be done, it will return true.
	 * @return true if the difference between previous objective value and
	 * 		   current objective value is less than {@link NetworkConfig#OBJTOL}
	 * 		   or the optimizer deems the optimization is finished, or
	 * 		   the decrease is less than 0.01% for three iterations, false otherwise.
	 */
	protected boolean updateDiscriminative(){
		if (NetworkConfig.USE_NEURAL_FEATURES) {
			if (concatWeights == null) {
				int concatDim = getFeatureSize();
				concatWeights = new double[concatDim];
				concatCounts = new double[concatDim];
			}
			
			// Concatenate discrete weights+continuous weights+provider params
			// and similarly for their gradient vectors.
			int ptr = 0;
			System.arraycopy(_weights, 0, concatWeights, ptr, _weights.length);
			System.arraycopy(_counts, 0, concatCounts, ptr, _counts.length);
			ptr += _weights.length;
			for (AbstractNeuralNetwork net : this._nn_param_g.getAllNets()) {
				double[] params = net.getParams();
				double[] gradParams = net.getGradParams();
				if (params == null || gradParams == null) continue;
				System.arraycopy(params, 0, concatWeights, ptr, params.length);
				System.arraycopy(gradParams, 0, concatCounts, ptr, gradParams.length);
				ptr += params.length;
			}
			this._opt.setVariables(concatWeights);
			this._opt.setGradients(concatCounts);
		}else{
	    	this._opt.setVariables(this._weights);
	    	this._opt.setGradients(this._counts);
		}
    	
    	this._opt.setObjective(-this._obj);
    	
    	boolean done = false;
    	
    	try{
    		// The _weights parameters will be updated inside this optimize method.
    		// This is possible since the _weights array is passed to the optimizer above,
    		// and the optimizer will set the weights directly, as arrays are passed by reference
        	done = this._opt.optimize();
    	} catch(ExceptionWithIflag e){
    		throw new NetworkException("Exception with Iflag:"+e.getMessage());
    	}
    	double diff = this.getObj()-this.getObj_old();
    	double diffRatio = Math.abs(diff/this.getObj_old());
    	if(NetworkConfig.STOPPING_CRITERIA == StoppingCriteria.SMALL_ABSOLUTE_CHANGE){
	    	if(diff >= 0 && diff < NetworkConfig.OBJTOL){
	    		done = true;
	    	}
    	} else if(NetworkConfig.STOPPING_CRITERIA == StoppingCriteria.SMALL_RELATIVE_CHANGE){
	    	if(diff >= 0 && diffRatio < 1e-4){
	    		this.smallChangeCount += 1;
	    	} else {
	    		this.smallChangeCount = 0;
	    	}
	    	if(this.smallChangeCount == 3){
	    		done = true;
	    	}
    	}
    	
    	if(done && this._opt.name().contains("LBFGS Optimizer") && !NetworkConfig.USE_NEURAL_FEATURES){
    		// If we stop early, we need to copy solution_cache,
    		// as noted in the Javadoc for solution_cache in LBFGS class.
    		// This is because the _weights will contain the next value to be evaluated, 
    		// and so does not correspond to the current objective value.
    		// In practice, though, the two are usually very close to each other (if we
    		// are stopping near the solution), so not copying will also work.
    		for(int i=0; i<this._weights.length; i++){
        		this._weights[i] = LBFGS.solution_cache[i];
        	}
    	}
    	
    	if (NetworkConfig.USE_NEURAL_FEATURES) {
    		// De-concatenate into their corresponding weight vectors 
    		int ptr = 0;
    		System.arraycopy(concatWeights, ptr, _weights, 0, _weights.length);
    		ptr += _weights.length;
			for (AbstractNeuralNetwork net : this._nn_param_g.getAllNets()) {
				double[] params = net.getParams();
				double[] gradParams = net.getGradParams();
				if (params == null || gradParams == null) continue;
				System.arraycopy(concatWeights, ptr, params, 0, params.length);
				ptr += params.length;
			}
    	}
    	
		this._version ++;
		return done;
	}
	
	private int getFeatureSize() {
		int size = this.countFeatures();
		for (AbstractNeuralNetwork net : this._nn_param_g.getAllNets()) {
			size += net.getParamSize();
		}
		return size;
	}
	
	public boolean isDiscriminative(){
		return this._isDiscriminative;
	}
	
	/**
	 * Set {@link #_counts} (the gradient) and {@link #_obj} to the regularization term, 
	 * essentially zeroing the values to be updated with the model gradient and objective value. 
	 */
	protected synchronized void resetCountsAndObj(){
		
		double coef = 1.0;
		if(NetworkConfig.USE_BATCH_TRAINING){
			coef = this._batchSize*1.0/this.totalNumInsts;
			if(coef>1) coef = 1.0;
		}
		
		
		for(int k = 0 ; k<this._size; k++){
			this._counts[k] = 0.0;
			//for regularization
			if(this.isDiscriminative() && this._kappa > 0 && k>=this._fixedFeaturesSize){
				this._counts[k] += 2 * coef * this._kappa * this._weights[k];
			}
		}
		
		this._obj = 0.0;
		//for regularization
		if(this.isDiscriminative() && this._kappa > 0){
			this._obj += MathsVector.square(this._weights);
			if (NetworkConfig.USE_NEURAL_FEATURES) {
				for (NeuralNetworkCore net : this._nn_param_g.getAllNets()) {
					net.setScale(coef);
					if (NetworkConfig.REGULARIZE_NEURAL_FEATURES) {
						this._obj += net.getL2Params();
					}
				}
			}
			this._obj *= - coef * this._kappa;
		}
		//NOTES:
		//for additional terms such as regularization terms:
		//always add to _obj the term g(x) you would like to maximize.
		//always add to _counts the NEGATION of the term g(x)'s gradient.
	}
	
	
	public void setNNParamG(GlobalNeuralNetworkParam nn_param_g) {
		this._nn_param_g = nn_param_g;
	}
	
	public int getAllNNParamSize() {
		int size = 0;
		for (AbstractNeuralNetwork net : this._nn_param_g.getAllNets()) {
			size += net.getParamSize();
		}
		return size;
	}
	
	
	/**
	 * Get the global neural net param
	 * @return
	 */
	public GlobalNeuralNetworkParam getNNParamG() {
		return this._nn_param_g;
	}
	
	public void setInstsNum(int number){
		this.totalNumInsts = number;
	}
	
//	public boolean checkEqual(GlobalNetworkParam p){
//		boolean v1 = Arrays.equals(this._weights, p._weights);
//		boolean v2 = Arrays.deepEquals(this._feature2rep, p._feature2rep);
//		return v1 && v2;
//	}
	
	private void writeObject(ObjectOutputStream out) throws IOException{
		out.writeObject("Version 1");
		
		out.writeObject("_featureIntMap");
		out.writeObject(this._featureIntMap);
		
		out.writeObject("_stringIndex");
		out.writeObject(this._stringIndex);
		
		out.writeObject("_weights");
		out.writeObject(this._weights);
		
		out.writeObject("_size");
		out.writeObject(this._size);
		
		out.writeObject("_fixedFeaturesSize");
		out.writeObject(this._fixedFeaturesSize);
		
		out.writeObject("_locked");
		out.writeObject(this._locked);
		
		out.writeObject("_nn_param_g");
		out.writeObject(this._nn_param_g);
	}
	
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException{
		Object obj = in.readObject();
		String version = null;
		try{
			version = (String)obj;
		} catch (Exception e){
			this._featureIntMap = (TIntObjectHashMap<TIntObjectHashMap<TIntIntHashMap>>)obj;
		}
		if(version == null){
			this._weights = (double[])in.readObject();
			this._size = in.readInt();
			this._fixedFeaturesSize = in.readInt();
			this._locked = in.readBoolean();
			if(in.available() > 0)
				this._nn_param_g = (GlobalNeuralNetworkParam)in.readObject();
		} else {
			if(version.equals("Version 1")){
				while(true){
					try{
						String varName;
						try{
							varName = (String)in.readObject();
						} catch (IOException e){
							break;
						}
						obj = in.readObject();
						Field field = this.getClass().getDeclaredField(varName);
						field.setAccessible(true);
						field.set(this, obj);
					} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e){
						throw new RuntimeException(e);
					}
				}
			} else {
				throw new IllegalArgumentException("The model version string: "+version+" is not recognized.");
			}
		}
	}
}