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

import java.lang.reflect.Field;

import org.ns.hypergraph.decoding.Hypothesis;

public class NetworkConfig {
	
	/**
	 * The enumeration of available model type<br>
	 * <ul>
	 * <li>{@link #STRUCTURED_PERCEPTRON} (<tt>USE_COST=false</tt>, <tt>USE_SOFTMAX=false</tt>)</li>
	 * <li>{@link #CRF} (<tt>USE_COST=false</tt>, <tt>USE_SOFTMAX=true</tt>)</li>
	 * <li>{@link #SSVM} (<tt>USE_COST=true</tt>, <tt>USE_SOFTMAX=false</tt>)</li>
	 * <li>{@link #SOFTMAX_MARGIN} (<tt>USE_COST=true</tt>, <tt>USE_SOFTMAX=true</tt>)</li>
	 * </ul>
	 * 
	 * Each model has two boolean parameters: {@link #USE_COST} and {@link #USE_SOFTMAX}.<br>
	 * <tt>USE_COST</tt> determines whether the cost function is used, while
	 * <tt>USE_SOFTMAX</tt> determines whether the softmax function is used instead of max.
	 */
	public static enum ModelType {
		STRUCTURED_PERCEPTRON(false, false),
		CRF(false, true),
		SSVM(true, false),
		SOFTMAX_MARGIN(true, true),
		;
		
		public final boolean USE_COST;
		public final boolean USE_SOFTMAX;
		
		private ModelType(boolean useCost, boolean useSoftmax){
			USE_COST = useCost;
			USE_SOFTMAX = useSoftmax;
		}
	}
	
	/**
	 * Model Status enumeration if users need 
	 */
	public static enum ModelStatus {
		TRAINING,
		DEV_IN_TRAINING,
		TESTING;
	}
	
	/**
	 * The status to define the current model status. by default it's training.
	 */
	public static ModelStatus STATUS = ModelStatus.TRAINING;
	
	/**
	 * The value to initialize the weights to if {@link #RANDOM_INIT_WEIGHT} is <tt>false</tt>.
	 */
	public static double FEATURE_INIT_WEIGHT = 0;
	/**
	 * Whether to initialize the weight vector randomly or fixed to {@link #FEATURE_INIT_WEIGHT}.
	 */
	public static boolean RANDOM_INIT_WEIGHT = true;
	/**
	 * The seed for random weight vector initialization (for reproducibility)
	 */
	public static int RANDOM_INIT_FEATURE_SEED = 1234;
	
	/**
	 * Whether to use feature value in CRF part implementation
	 */
	public static boolean USE_FEATURE_VALUE = false;
	
	/**
	 * Whether generative training is used instead of discriminative
	 */
	public static boolean TRAIN_MODE_IS_GENERATIVE = false;
	/**
	 * The L2 regularization parameter
	 */
	public static double L2_REGULARIZATION_CONSTANT = 0.01;
	
	/**
	 * Network is the core of StatNLP framework.<br>
	 * This defines the default capacity for defining the nodes of the network<br>
	 * For more information, see {@link Network}
	 * @see Network
	 */
	public static final int[] DEFAULT_CAPACITY_NETWORK = new int[]{4096, 4096, 4096, 4096, 4096};
	
	public static enum StoppingCriteria {
		MAX_ITERATION_REACHED,
		SMALL_ABSOLUTE_CHANGE,
		SMALL_RELATIVE_CHANGE,
		;
	}
	
	public static StoppingCriteria STOPPING_CRITERIA = StoppingCriteria.SMALL_RELATIVE_CHANGE;
	
	/**
	 * The value used for stopping criterion of change in objective value in generative models
	 */
	public static double OBJTOL = 10e-15;

	public static boolean DEBUG_MODE = false;
	
	/**
	 * The model type used for learning.<br>
	 * The options are in {@link ModelType}
	 */
	public static ModelType MODEL_TYPE = ModelType.CRF;
	/** Whether to use batch */
	public static boolean USE_BATCH_TRAINING = false;
	/**
	 * Batch size for batch training (if {@link #USE_BATCH_TRAINING} is <tt>true</tt>) for each thread
	 */
	public static int BATCH_SIZE = 20;
	public static int RANDOM_BATCH_SEED = 2345;
	public static boolean PRINT_BATCH_OBJECTIVE = false;
	
	/** The weight of the cost function for SSVM and Softmax-Margin */
	public static double MARGIN = 0.5;
	/**
	 * A flag whether to normalize the default cost function in cost-based models
	 * like {@link ModelType#SSVM} and {@link ModelType#SOFTMAX_MARGIN}<br>
	 * 
	 * This is one of the three flags for controlling default cost function:
	 * <ul>
	 * <li>{@link #NORMALIZE_COST}</li>
	 * <li>{@link #EDGE_COST}</li>
	 * <li>{@link #NODE_COST}</li>
	 * </ul>
	 */
	public static boolean NORMALIZE_COST = false;
	/**
	 * The cost for having node mismatch in cost-based models
	 * like {@link ModelType#SSVM} and {@link ModelType#SOFTMAX_MARGIN}<br>
	 * 
	 * This is one of the three flags for controlling default cost function:
	 * <ul>
	 * <li>{@link #NORMALIZE_COST}</li>
	 * <li>{@link #EDGE_COST}</li>
	 * <li>{@link #NODE_COST}</li>
	 * </ul>
	 */
	public static double NODE_COST = 1.0;
	/**
	 * The cost for having edge mismatch in cost-based models
	 * like {@link ModelType#SSVM} and {@link ModelType#SOFTMAX_MARGIN}<br>
	 * 
	 * This is one of the three flags for controlling default cost function:
	 * <ul>
	 * <li>{@link #NORMALIZE_COST}</li>
	 * <li>{@link #EDGE_COST}</li>
	 * <li>{@link #NODE_COST}</li>
	 * </ul>
	 */
	public static double EDGE_COST = 0.0;

	/**
	 * Whether features are cached during training.<br>
	 * Without caching training might be very slow.
	 */
	public static boolean CACHE_FEATURES_DURING_TRAINING = true;
	/**
	 * Build features in parallel during the touch process 
	 */
	public static boolean PARALLEL_FEATURE_EXTRACTION = true;
	/**
	 * Build features only from labeled instances
	 */
	public static boolean BUILD_FEATURES_FROM_LABELED_ONLY = false;

	/**
	 * Enable to try to save memory by caching feature arrays to avoid duplicate feature arrays to be stored
	 * in memory.<br>
	 * Note that the amount of memory-saving depends on how the FeatureArrays are defined.<br>
	 * If there are lots of repeating feature arrays with the exact same sequence of feature indices,
	 * then enabling this might be beneficial, but otherwise, it will actually increase memory usage and time.<br>
	 * If you are using this, it's best to split feature arrays into multiple arrays, and then
	 * chain them together using the "next" mechanism in FeatureArray.<br>
	 * See {@link FeatureArray#FeatureArray(int[], FeatureArray)} for more information.
	 */
	public static boolean AVOID_DUPLICATE_FEATURES = false;
	
	/**
	 * The number of threads to be used for parallel execution
	 */
	public static int NUM_THREADS = 4;
	
	/** Decoding the max-marginal for each node as well. if set to true */
	public static boolean MAX_MARGINAL_DECODING = false;
	
	/**
	 * Enumerates the supported inference type
	 */
	public static enum InferenceType {
		MEAN_FIELD,
		FORWARD_BACKWARD
		;
	}
	
	/**
	 * The inference type of the model.
	 */
	public static InferenceType INFERENCE = InferenceType.FORWARD_BACKWARD;
	
	/**
	 * Limit the size of the priority queue used in {@link Hypothesis} class when decoding.<br>
	 * Setting this to 0 will remove the limit, making the data structure slightly faster.<br>
	 * The size limit of the priority queue will also affect the highest supported k in top-k decoding,
	 * with <br>
	 */
	public static int PRIORITY_QUEUE_SIZE_LIMIT = 0;
	
	/***
	 * Neural network related flags.
	 * Please read carefully about the README.txt to install the NN server and also the communication package for Neural CRF
	 */
	/** If enable the neural CRF model, set it true.  */
	public static boolean USE_NEURAL_FEATURES = false;
	/** "torch" (socket) or "torch-jni" (TH4J + JNLua) */
	public static String NEURAL_BACKEND = "torch";
	/** Regularized the neural features in CRF or not. set to false then can be done by dropout***/
	public static boolean REGULARIZE_NEURAL_FEATURES = false;
	/** If true: Optimized the neural net in CRF. optimizer in neural config must be set to none **/
	public static boolean OPTIMIZE_NEURAL = true;   //false means not update the neural network parameters in CRF. false is faster
	/** false: the feature is the word itself. true: word is the indexed word **/
	public static boolean IS_INDEXED_NEURAL_FEATURES = false;
	/** Randomly choose the batch at every iteration. (false may give better result) */
	public static boolean RANDOM_BATCH = false;
	/**
	 * Initialize the feature value provider weights in java
	 * It will override the embedding weights if you have.
	 */
	public static boolean INIT_FV_WEIGHTS = false;
	
	public static String NEURAL_RANDOM_TYPE = "default";
	
	public static String OS = "osx"; // for Lua native library
	
	/***
	 * Mean field-related flags.
	 */
	/** The number of internal mean-field updates to be done in between each normal iterations. */
	public static int MAX_MF_UPDATES = 0;
	/** The number of distinct structures in mean-field model. */
	public static int NUM_STRUCTS = 2;
	/** Currently only used by Mean-field inference. That's why protected. true if mean-field, false otherwise */
	protected static boolean PRE_COMPILE_NETWORKS;
	
	/**
	 * Returns the value of all the configurations
	 * @return
	 */
	public static String getConfig(){
		StringBuilder builder = new StringBuilder();
		for(Field field: NetworkConfig.class.getDeclaredFields()){
			try {
				builder.append(field.getName()+"="+field.get(null)+"\n");
			} catch (IllegalArgumentException | IllegalAccessException e) {
				return null;
			}
		}
		return builder.toString();
	}
	
	public static boolean FEATURE_TOUCH_TEST = true;
	
	public static int BEST_ITER_DEV = -1;
}