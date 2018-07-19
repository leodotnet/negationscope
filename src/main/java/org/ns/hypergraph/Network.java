
package org.ns.hypergraph;

import java.io.Serializable;
import java.util.Arrays;
import java.util.NoSuchElementException;

import org.ns.commons.types.Instance;
import org.ns.hypergraph.NetworkConfig.InferenceType;
import org.ns.hypergraph.decoding.EdgeHypothesis;
import org.ns.hypergraph.decoding.NodeHypothesis;
import org.ns.hypergraph.decoding.ScoredIndex;

/**
 * The base class for representing networks. This class is equipped with algorithm to calculate the 
 * inside-outside score, which is also a generalization to the forward-backward score.<br>
 * You might want to use {@link TableLookupNetwork} for more functions such as adding nodes and edges.
 * @see TableLookupNetwork
 * @author Wei Lu (luwei@statnlp.com)
 *
 */
public abstract class Network implements Serializable, HyperGraph{
	
	public static enum NODE_TYPE {sum, max};
	
	private static final long serialVersionUID = -3630379919120581209L;
	
	/**
	 * The working array for each thread for calculating inside scores
	 * This is done to avoid reallocating a new array for each network
	 */
	protected static double[][] insideSharedArray = new double[NetworkConfig.NUM_THREADS][]; // TODO: The value of NetworkConfig.NUM_THREADS might change after first access to Network class
	/**
	 * The working array for each thread for calculating outside scores
	 * This is done to avoid reallocating a new array for each network
	 */
	protected static double[][] outsideSharedArray = new double[NetworkConfig.NUM_THREADS][];

	/**
	 * The working array for each thread for calculating max scores
	 * This is done to avoid reallocating a new array for each network
	 */
	protected static double[][] maxSharedArray = new double[NetworkConfig.NUM_THREADS][];
	/**
	 * The working array for each thread for storing max paths (for backtracking)
	 * This is done to avoid reallocating a new array for each network
	 */
	protected static int[][][] maxPathsSharedArrays = new int[NetworkConfig.NUM_THREADS][][];

//	protected static NodeHypothesis[][] hypothesisSharedArray = new NodeHypothesis[NetworkConfig.NUM_THREADS][];
	/** 
	 * The working array for each thread for calculating outside scores 
	 */
	protected static double[][] unlabeledMarginalSharedArray = new double[NetworkConfig.NUM_THREADS][];
	
	/** 
	 * The working array for each thread for calculating outside scores 
	 */
	protected static double[][] unlabeledNewMarginalSharedArray = new double[NetworkConfig.NUM_THREADS][];
	
	/** The IDs associated with the network (within the scope of the thread). */
	protected int _networkId;
	/** The id of the thread */
	protected int _threadId;
	/** The instance */
	protected transient Instance _inst;
	/** The weight */
	protected transient double _weight;
	/** The feature parameters */
	protected transient LocalNetworkParam _param;
	
	/** At each index, store the node's inside score */
	protected transient double[] _inside;
	/** At each index, store the node's outside score */
	protected transient double[] _outside;
	/** At each index, store the score of the max tree */
	protected transient double[] _max;
	/** Stores the paths associated with the above tree */
	protected transient int[][] _max_paths;
	/** Stores the hypothesis (listing possible direction to take) */
//	protected transient NodeHypothesis[] _hypotheses;
	/** To mark whether a node has been visited in one iteration */
	protected transient boolean[] _visited;
	/** The marginal score for each node */
	protected transient double[] _marginal;
	
	/**
	 * The compiler that created this network.<br>
	 * This is used to get the cost.
	 */
	protected NetworkCompiler _compiler;
	/** The labeled version of this network, if exists, null otherwise */
	private Network _labeledNetwork;
	/** The unlabeled version of this network, if exists, null otherwise */
	private Network _unlabeledNetwork;
	
	/** store the information of removal of each node **/
	protected transient boolean[] isVisible;
		
	protected transient double[] _newMarginal;
		
	
	/** The current structure that the network is using*/
	protected int currentStructure; 
	
	/** 
	 * store the information of structure of each node in network.
	 * the value (structure) of each node is specified by user.
	 * Currently used for mean-field inference
	 * **/
	protected transient int[] structArr;
	
	/**
	 * Default constructor. Note that the network constructed using this default constructor is lacking 
	 * the {@link LocalNetworkParam} object required for actual use.
	 * Use this only for generating generic network, which is later actualized using another constructor.
	 * @see #Network(int, Instance, LocalNetworkParam)
	 */
	public Network(){}
	
	/**
	 * Construct a network
	 * @param networkId
	 * @param inst
	 * @param param
	 */
	public Network(int networkId, Instance inst, LocalNetworkParam param){
		this(networkId, inst, param, null);
	}
	
	/**
	 * Construct a network, specifying the NetworkCompiler that created this network
	 * @param networkId
	 * @param inst
	 * @param param
	 * @param compiler
	 */
	public Network(int networkId, Instance inst, LocalNetworkParam param, NetworkCompiler compiler){
		this._networkId = networkId;
		this._threadId = param.getThreadId();
		this._inst = inst;
		this._weight = this._inst.getWeight();
		this._param = param;
		this._compiler = compiler;
	}
	
	protected double[] getInsideSharedArray(){
		if(insideSharedArray[this._threadId] == null || this.countNodes() > insideSharedArray[this._threadId].length)
			insideSharedArray[this._threadId] = new double[this.countNodes()];
		return insideSharedArray[this._threadId];
	}
	
	protected double[] getOutsideSharedArray(){
		if(outsideSharedArray[this._threadId] == null || this.countNodes() > outsideSharedArray[this._threadId].length)
			outsideSharedArray[this._threadId] = new double[this.countNodes()];
		return outsideSharedArray[this._threadId];
	}

	protected double[] getMaxSharedArray(){
		if(maxSharedArray[this._threadId] == null || this.countNodes() > maxSharedArray[this._threadId].length)
			maxSharedArray[this._threadId] = new double[this.countNodes()];
		return maxSharedArray[this._threadId];
	}

	protected int[][] getMaxPathSharedArray(){
		if(maxPathsSharedArrays[this._threadId] == null || this.countNodes() > maxPathsSharedArrays[this._threadId].length)
			maxPathsSharedArrays[this._threadId] = new int[this.countNodes()][];
		return maxPathsSharedArrays[this._threadId];
	}
	
//	protected NodeHypothesis[] getHypothesisSharedArray(){
//		if(hypothesisSharedArray[this._threadId] == null || this.countNodes() > hypothesisSharedArray[this._threadId].length){
//			hypothesisSharedArray[this._threadId] = new NodeHypothesis[this.countNodes()];
//		}
//		return hypothesisSharedArray[this._threadId];
//	}
	
	protected double[] getMarginalSharedArray(){
		if(unlabeledMarginalSharedArray[this._threadId] == null || this.countNodes() > unlabeledMarginalSharedArray[this._threadId].length)
			unlabeledMarginalSharedArray[this._threadId] = new double[this.countNodes()];
		return unlabeledMarginalSharedArray[this._threadId];
	}
	
	protected double[] getNewMarginalSharedArray(){
		if(unlabeledNewMarginalSharedArray[this._threadId] == null || this.countNodes() > unlabeledNewMarginalSharedArray[this._threadId].length)
			unlabeledNewMarginalSharedArray[this._threadId] = new double[this.countNodes()];
		return unlabeledNewMarginalSharedArray[this._threadId];
	}
	
	public int getNetworkId(){
		return this._networkId;
	}
	
	public int getThreadId(){
		return this._threadId;
	}
	
	/**
	 * Returns the instance modeled by this network
	 * @return
	 */
	public Instance getInstance(){
		return this._inst;
	}
	
	/**
	 * Returns the compiler that compiled this network
	 * @return
	 */
	public NetworkCompiler getCompiler(){
		return this._compiler;
	}
	
	/**
	 * Sets the compiler that compiled this network
	 * @param compiler
	 */
	public void setCompiler(NetworkCompiler compiler){
		this._compiler = compiler;
	}
	
	/**
	 * Returns the labeled network related to this network<br>
	 * If this network represents a labeled network, this will return itself
	 * @return
	 */
	public Network getLabeledNetwork(){
		if(getInstance().isLabeled()){
			return this;
		}
		return this._labeledNetwork;
	}
	
	/**
	 * Sets the labeled network related to this network
	 * @param network
	 */
	public void setLabeledNetwork(Network network){
		this._labeledNetwork = network;
	}
	
	/**
	 * Returns the unlabeled network related to this network<br>
	 * If this network represents an unlabeled network, this will return itself
	 * @return
	 */
	public Network getUnlabeledNetwork(){
		if(!getInstance().isLabeled()){
			return this;
		}
		return this._unlabeledNetwork;
	}
	
	/**
	 * Sets the unlabeled network related to this network
	 * @param network
	 */
	public void setUnlabeledNetwork(Network network){
		this._unlabeledNetwork = network;
	}
	
	/**
	 * Returns the inside score for the root node
	 * @return
	 */
	public double getInside(){
		return this._inside[this.countNodes()-1];
	}
	
	/**
	 * Return the marginal score for the network at a specific index (Note: do not support SSVM yet)
	 * @param k
	 * @return
	 */
	public double getMarginal(int k){
		return this._marginal[k];
	}
	
	/**
	 * Return the maximum score for this network (which is the max score for the root node)
	 * @return
	 */
	public double getMax(){
		int rootIdx = this.countNodes()-1;
		return this._max[rootIdx];
	}

	/**
	 * Return the maximum score for this network ending in the node with the specified index
	 * @param k
	 * @return
	 */
	public double getMax(int k){
		return this._max[k];
	}
	
	/**
	 * Return the children of the hyperedge which is part of the maximum path of this network
	 * @return
	 */
	public int[] getMaxPath(){
		return this._max_paths[this.countNodes()-1];
	}
	
	/**
	 * Return the children of the hyperedge which is part of the maximum path of this network
	 * ending at the node at the specified index
	 * @return
	 */
	public int[] getMaxPath(int k){
		return this._max_paths[k];
	}
	
	/**
	 * Return the max path according to the configuration specified in the bestPath IndexedScore object.<br>
	 * This is used in returning the top-k result also.
	 * @param node
	 * @param bestPath
	 * @return
	 * @throws NoSuchElementException
	 */
//	public ScoredIndex[] getMaxPath(NodeHypothesis node, ScoredIndex bestPath) throws NoSuchElementException {
//		try{
//			EdgeHypothesis edge = node.children()[bestPath.index[0]];
//			ScoredIndex score = edge.getKthBestHypothesis(bestPath.index[1]);
//			ScoredIndex[] result = new ScoredIndex[edge.children().length];
//			for(int i=0; i<edge.children().length; i++){
//				result[i] = edge.children()[i].getKthBestHypothesis(score.index[i]);
//			}
//			return result;
//		} catch (NullPointerException e){
//			throw new NoSuchElementException("The requested k-best exceeds the maximum number of structures.");
//		}
//	}
//	
//	public NodeHypothesis getNodeHypothesis(int k){
//		return this._hypotheses[k];
//	}

	/**
	 * Calculate the marginal score for all nodes
	 * this marginal is used for mean-field inference 
	 */
	public void marginal(){
		this._newMarginal = this.getNewMarginalSharedArray();
		//Arrays.fill(this._newMarginal, Double.NEGATIVE_INFINITY);
		for(int k=0; k<this.countNodes(); k++){
			//for mean-field, only need to gather the marginal from the unlabeled network
			if( NetworkConfig.MAX_MARGINAL_DECODING || (NetworkConfig.INFERENCE == InferenceType.MEAN_FIELD && !this.getInstance().isLabeled()
					&& !this.isRemoved(k)) ){
				this.marginal(k);
				this._newMarginal[k] = Math.exp(this._newMarginal[k]);
			}
		}
	}
	
	
	/**
	 * Calculate the marginal score at the specific node
	 * @param node_k
	 */
	protected void marginal(int node_k){
		if(this.isRemoved(node_k)){
			return;
		}
		//since inside and outside are in log space
		this._newMarginal[node_k] = this._inside[node_k] + this._outside[node_k] - this.getInside();
	}
	
	/**
	 * Get the sum of the network (i.e., the inside score)
	 * @return
	 */
	public double sum(){
		this.inside();
		return this.getInside();
	}
	
	/**
	 * Inference in the Network without updating the parameters
	 */
	public void inference(boolean marginalize){
		if(this._weight == 0)
			return;
		if(NetworkConfig.MODEL_TYPE.USE_SOFTMAX){
			this.inside();
			this.outside();
			if(marginalize){
				//save the marginal score;
				//actually for the labeled network, no need to do the marginalize
				//since the marginal score of labeled network is not used.
				this.marginal();
			}
		} else { // Use real max
			this.max();
		}
	}
	
	/**
	 * Train the network
	 */
	public void train(){
		inference(false); //no need to marginalize when we're going to update later
		this.updateGradient();
		this.updateObjective();
	}
	
	/**
	 * Calculate the inside score of all nodes
	 */
	protected void inside(){
		this._inside = this.getInsideSharedArray();
		Arrays.fill(this._inside, 0.0);
		for(int k=0; k<this.countNodes(); k++){
			this.inside(k);
		}
		if(this.getInside()==Double.NEGATIVE_INFINITY){
			throw new RuntimeException("Error: network (ID="+_networkId+") has zero inside score");
		}
	}
	
	/**
	 * Calculate the outside score of all nodes
	 */
	protected void outside(){
		this._outside = this.getOutsideSharedArray();
		Arrays.fill(this._outside, Double.NEGATIVE_INFINITY);
		for(int k=this.countNodes()-1; k>=0; k--){
			this.outside(k);
		}
	}
	
	/**
	 * Calculate and update the inside-outside score of all nodes
	 */
	protected void updateGradient(){
		if(NetworkConfig.MODEL_TYPE.USE_SOFTMAX){
			for(int k=0; k<this.countNodes(); k++){
				this.updateGradient(k);
			}
		} else { // Use real max
			// Max is already calculated
			int rootIdx = this.countNodes()-1;
			resetVisitedMark();
			this.updateGradient(rootIdx);
		}			
	}
	
	private void resetVisitedMark(){
		this._visited = new boolean[countNodes()];
		for(int i=0; i<this._visited.length; i++){
			this._visited[i] = false;
		}
	}
	
	protected void updateObjective(){
		double objective = 0.0;
		if(NetworkConfig.MODEL_TYPE.USE_SOFTMAX){
			objective = this.getInside() * this._weight;
		} else { // Use real max
			objective = this.getMax() * this._weight;
		}
		this._param.addObj(objective);
	}
	
	/**
	 * Goes through each nodes in the network to gather list of features
	 */
	public synchronized void touch(){
		for(int k=0; k<this.countNodes(); k++)
			this.touch(k);
	}
	
	/**
	 * Calculate the maximum score for all nodes
	 */
	public void max(){
		this._max = this.getMaxSharedArray();
		this._max_paths = this.getMaxPathSharedArray();
//		this._hypotheses = this.getHypothesisSharedArray();
		for(int k=0; k<this.countNodes(); k++){
			this.max(k);
		}
	}
	
	/**
	 * Calculate the inside score for the specified node
	 * @param k
	 */
	protected void inside(int k){
		if(this.isRemoved(k)){
			this._inside[k] = Double.NEGATIVE_INFINITY;
			return;
		}
		
		double inside = Double.NEGATIVE_INFINITY;
		int[][] childrenList_k = this.getChildren(k);
		
		// If this node has no child edge, assume there is one edge with no child node
		// This is done so that every node is visited in the feature extraction step below
		if(childrenList_k.length==0){ 
			childrenList_k = new int[1][0];
		}
		
		{
			int children_k_index = 0;
			int[] children_k = childrenList_k[children_k_index];

			boolean ignoreflag = false;
			for(int child_k : children_k){
				if(child_k < 0){
					// A negative child_k is not a reference to a node, it's just a number associated with this edge
					continue;
				}
				if(this.isRemoved(child_k)){
					ignoreflag = true;
				}
			}
			if(ignoreflag){
				inside = Double.NEGATIVE_INFINITY;
			} else {
				FeatureArray fa = this._param.extract(this, k, children_k, children_k_index);
				int globalParamVersion = this._param._fm.getParam_G().getVersion();
				double score = NetworkConfig.INFERENCE==InferenceType.MEAN_FIELD  ?
			 			fa.getScore_MF_Version(this._param, this.getUnlabeledNetwork().getMarginalSharedArray(), globalParamVersion):
			 				fa.getScore(this._param, globalParamVersion);
			 	
			 	if(NetworkConfig.MODEL_TYPE.USE_COST){
					score += this._param.cost(this, k, children_k, children_k_index, this._compiler);
				}
			 	if (NetworkConfig.USE_NEURAL_FEATURES) {
			 		score += this._param._fm.getParam_G().getNNParamG().getNNScore(this, k, children_k, children_k_index);
			 	}
			 	
				for(int child_k : children_k){
					if(child_k < 0){
						// A negative child_k is not a reference to a node, it's just a number associated with this edge
						continue;
					}
					score += this._inside[child_k];
				}
				inside = score;
			}
		}

		for(int children_k_index = 1; children_k_index < childrenList_k.length; children_k_index++){
			int[] children_k = childrenList_k[children_k_index];

			boolean ignoreflag = false;
			for(int child_k : children_k){
				if(child_k < 0){
					// A negative child_k is not a reference to a node, it's just a number associated with this edge
					continue;
				}
				if(this.isRemoved(child_k)){
					ignoreflag = true;
				}
			}
			if(ignoreflag) continue;
			
			FeatureArray fa = this._param.extract(this, k, children_k, children_k_index);
			int globalParamVersion = this._param._fm.getParam_G().getVersion();
			double score = NetworkConfig.INFERENCE==InferenceType.MEAN_FIELD ?
		 			fa.getScore_MF_Version(this._param, this.getUnlabeledNetwork().getMarginalSharedArray(), globalParamVersion):
		 				fa.getScore(this._param, globalParamVersion);

 			if(NetworkConfig.MODEL_TYPE.USE_COST){
				score += this._param.cost(this, k, children_k, children_k_index, this._compiler);
			}
 			if (NetworkConfig.USE_NEURAL_FEATURES) {
 				score += this._param._fm.getParam_G().getNNParamG().getNNScore(this, k, children_k, children_k_index);
 			} 			
 			
			for(int child_k : children_k){
				if(child_k < 0){
					// A negative child_k is not a reference to a node, it's just a number associated with this edge
					continue;
				}
				score += this._inside[child_k];
			}
			inside = sumLog(inside, score);
		}
		
		this._inside[k] = inside;
		
		if(this._inside[k]==Double.NEGATIVE_INFINITY){
			this.remove(k);
		}
	}
	
	/**
	 * Calculate the outside score for the specified node
	 * @param k
	 */
	protected void outside(int k){
		if(this.isRemoved(k)){
			this._outside[k] = Double.NEGATIVE_INFINITY;
			return;
		}
		else
			this._outside[k] = this.isRoot(k) ? 0.0 : this._outside[k];
		
		if(this._inside[k]==Double.NEGATIVE_INFINITY)
			this._outside[k] = Double.NEGATIVE_INFINITY;
		
		int[][] childrenList_k = this.getChildren(k);
		for(int children_k_index = 0; children_k_index< childrenList_k.length; children_k_index++){
			int[] children_k = childrenList_k[children_k_index];
			
			boolean ignoreflag = false;
			for(int child_k : children_k){
				if(child_k < 0){
					// A negative child_k is not a reference to a node, it's just a number associated with this edge
					continue;
				}
				if(this.isRemoved(child_k)){
					ignoreflag = true; break;
				}
			}
			if(ignoreflag)
				continue;
			
			FeatureArray fa = this._param.extract(this, k, children_k, children_k_index);
			int globalParamVersion = this._param._fm.getParam_G().getVersion();
			double score = NetworkConfig.INFERENCE==InferenceType.MEAN_FIELD?
		 			fa.getScore_MF_Version(this._param, this.getUnlabeledNetwork().getMarginalSharedArray(), globalParamVersion):
		 				fa.getScore(this._param, globalParamVersion);
			if(NetworkConfig.MODEL_TYPE.USE_COST){
				score += this._param.cost(this, k, children_k, children_k_index, this._compiler);
			}
			if (NetworkConfig.USE_NEURAL_FEATURES) {
				score += this._param._fm.getParam_G().getNNParamG().getNNScore(this, k, children_k, children_k_index);
			}
			score += this._outside[k];
			for(int child_k : children_k){
				if(child_k < 0){
					// A negative child_k is not a reference to a node, it's just a number associated with this edge
					continue;
				}
				score += this._inside[child_k];
			}

			if(score == Double.NEGATIVE_INFINITY)
				continue;
			
			for(int child_k : children_k){
				if(child_k < 0){
					// A negative child_k is not a reference to a node, it's just a number associated with this edge
					continue;
				}
				double v1 = this._outside[child_k];
				double v2 = score - this._inside[child_k];
				this._outside[child_k] = sumLog(v1, v2);
			}
		}
		
		if(this._outside[k]==Double.NEGATIVE_INFINITY){
			this.remove(k);
		}
	}
	
	/**
	 * Calculate and update the gradient for features present at the specified node
	 * @param k
	 */
	protected void updateGradient(int k){
		if(this.isRemoved(k))
			return;
		
		int[][] childrenList_k = this.getChildren(k);
		int[] maxChildren = null;
		if(!NetworkConfig.MODEL_TYPE.USE_SOFTMAX){
			if(this._visited[k]) return;
			this._visited[k] = true;
			maxChildren = this.getMaxPath(k); // For Structured SVM
		}
		
		for(int children_k_index = 0; children_k_index<childrenList_k.length; children_k_index++){
			double count = 0.0;
			int[] children_k = childrenList_k[children_k_index];
			
			boolean ignoreflag = false;
			for(int child_k : children_k){
				if(child_k < 0){
					// A negative child_k is not a reference to a node, it's just a number associated with this edge
					continue;
				}
				if(this.isRemoved(child_k)){
					ignoreflag = true;
					break;
				}
			}
			if(!NetworkConfig.MODEL_TYPE.USE_SOFTMAX){ // Consider only max path
				if(!Arrays.equals(children_k, maxChildren)){
					continue;
				}
			}
			if(ignoreflag){
				continue;
			}
			
			FeatureArray fa = this._param.extract(this, k, children_k, children_k_index);
			int globalParamVersion = this._param._fm.getParam_G().getVersion();
			if(NetworkConfig.MODEL_TYPE.USE_SOFTMAX){
				double score = NetworkConfig.INFERENCE==InferenceType.MEAN_FIELD ?
			 			fa.getScore_MF_Version(this._param, this.getUnlabeledNetwork().getMarginalSharedArray(), globalParamVersion):
			 				fa.getScore(this._param, globalParamVersion);
				if(NetworkConfig.MODEL_TYPE.USE_COST){
					score += this._param.cost(this, k, children_k, children_k_index, this._compiler);
				}
				if (NetworkConfig.USE_NEURAL_FEATURES) {
					score += this._param._fm.getParam_G().getNNParamG().getNNScore(this, k, children_k, children_k_index);
				}
				score += this._outside[k];  // beta(s')
				for(int child_k : children_k){
					if(child_k < 0){
						// A negative child_k is not a reference to a node, it's just a number associated with this edge
						continue;
					}
					score += this._inside[child_k]; // alpha(s)
				}
				double normalization = this.getInside();
				count = Math.exp(score-normalization); // Divide by normalization term Z
				if(Double.isNaN(count))
					throw new RuntimeException("count is NaN in updating gradient? "+score+"\t"+normalization);
			} else { // Use real max
				count = 1;
			}
			count *= this._weight;
//			if(Double.isNaN(count))
//				throw new RuntimeException("count is NaN in updating gradient?");
			if (NetworkConfig.INFERENCE == InferenceType.MEAN_FIELD){
				fa.update_MF_Version(this._param, count, this.getUnlabeledNetwork().getMarginalSharedArray());
			}else{
				fa.update(this._param, count);
			}
			if (NetworkConfig.USE_NEURAL_FEATURES) {
				this._param._fm.getParam_G().getNNParamG().setNNGradOutput(count, this, k, children_k_index); // todo
			}
			if(!NetworkConfig.MODEL_TYPE.USE_SOFTMAX){
				for(int child_k: children_k){
					if(child_k < 0){
						// A negative child_k is not a reference to a node, it's just a number associated with this edge
						continue;
					}
					this.updateGradient(child_k);	
				}
			}
		}
	}
	
	/**
	 * Gather features from the specified node
	 * @param k
	 */
	protected void touch(int k){
		if(this.isRemoved(k))
			return;
		int[][] childrenList_k = this.getChildren(k);
		for(int children_k_index = 0; children_k_index < childrenList_k.length; children_k_index++){
			int[] children_k = childrenList_k[children_k_index];
			this._param.extract(this, k, children_k, children_k_index);
		}
	}
	
	/**
	 * Calculate the maximum score at the specified node
	 * @param k
	 */
	protected void max(int k){
		if(this.isRemoved(k)){
			this._max[k] = Double.NEGATIVE_INFINITY;
			return;
		}
		
		if(this.isSumNode(k)){

			double inside = 0.0;
			int[][] childrenList_k = this.getChildren(k);
			
			if(childrenList_k.length==0){
				childrenList_k = new int[1][0];
			}
			
			{
				int children_k_index = 0;
				int[] children_k = childrenList_k[children_k_index];
				
				boolean ignoreflag = false;
				for(int child_k : children_k){
					if(child_k < 0){
						// A negative child_k is not a reference to a node, it's just a number associated with this edge
						continue;
					}
					if(this.isRemoved(child_k)){
						ignoreflag = true;
					}
				}
				if(ignoreflag){
					inside = Double.NEGATIVE_INFINITY;
				} else {
					FeatureArray fa = this._param.extract(this, k, children_k, children_k_index);
					int globalParamVersion = this._param._fm.getParam_G().getVersion();
					double score = NetworkConfig.INFERENCE==InferenceType.MEAN_FIELD  ? 
				 			fa.getScore_MF_Version(this._param, this.getUnlabeledNetwork().getMarginalSharedArray(), globalParamVersion):
				 				fa.getScore(this._param, globalParamVersion);
					if(NetworkConfig.MODEL_TYPE.USE_COST){
						try{
							score += this._param.cost(this, k, children_k, children_k_index, this._compiler);
						} catch (NullPointerException e){
							System.err.println("WARNING: Compiler was not specified during network creation, setting cost to 0.0");
						}
					}
					if (NetworkConfig.USE_NEURAL_FEATURES) {
						score += this._param._fm.getParam_G().getNNParamG().getNNScore(this, k, children_k, children_k_index);
					}
					for(int child_k : children_k){
						if(child_k < 0){
							// A negative child_k is not a reference to a node, it's just a number associated with this edge
							continue;
						}
						score += this._max[child_k];
					}
					inside = score;
				}
				
				//if it is a sum node, then any path is the same for such a node.
				//this is something you need to make sure when constructing such a network.
				this._max_paths[k] = children_k;
			}
			
			for(int children_k_index = 1; children_k_index < childrenList_k.length; children_k_index++){
				int[] children_k = childrenList_k[children_k_index];

				boolean ignoreflag = false;
				for(int child_k : children_k){
					if(child_k < 0){
						// A negative child_k is not a reference to a node, it's just a number associated with this edge
						continue;
					}
					if(this.isRemoved(child_k)){
						ignoreflag = true;
					}
				}
				if(ignoreflag)
					continue;
				
				FeatureArray fa = this._param.extract(this, k, children_k, children_k_index);
				int globalParamVersion = this._param._fm.getParam_G().getVersion();
				double score = NetworkConfig.INFERENCE==InferenceType.MEAN_FIELD? 
			 			fa.getScore_MF_Version(this._param, this.getUnlabeledNetwork().getMarginalSharedArray(), globalParamVersion):
			 				fa.getScore(this._param, globalParamVersion);
				if(NetworkConfig.MODEL_TYPE.USE_COST){
					try{
						score += this._param.cost(this, k, children_k, children_k_index, this._compiler);
					} catch (NullPointerException e){
						System.err.println("WARNING: Compiler was not specified during network creation, setting cost to 0.0");
					}
				}
				if (NetworkConfig.USE_NEURAL_FEATURES) {
					score += this._param._fm.getParam_G().getNNParamG().getNNScore(this, k, children_k, children_k_index);
				}
				for(int child_k : children_k){
					if(child_k < 0){
						// A negative child_k is not a reference to a node, it's just a number associated with this edge
						continue;
					}
					score += this._max[child_k];
				}
				
				inside = sumLog(inside, score);
				
			}
			
			this._max[k] = inside;
		} else { // This is a max node, not a sum node
			int[][] childrenList_k = this.getChildren(k);
			this._max[k] = Double.NEGATIVE_INFINITY;
			
			EdgeHypothesis[] childrenOfThisNodeHypothesis = new EdgeHypothesis[childrenList_k.length];
			
			for(int children_k_index = 0; children_k_index < childrenList_k.length; children_k_index++){
				int[] children_k = childrenList_k[children_k_index];
				boolean ignoreflag = false;
				for(int child_k : children_k){
					if(child_k < 0){
						// A negative child_k is not a reference to a node, it's just a number associated with this edge
						continue;
					}
					if(this.isRemoved(child_k)){
						ignoreflag = true;
						break;
					}
				}
				if(ignoreflag)
					continue;
				
				FeatureArray fa = this._param.extract(this, k, children_k, children_k_index);
				int globalParamVersion = this._param._fm.getParam_G().getVersion();
				double score = NetworkConfig.INFERENCE==InferenceType.MEAN_FIELD  ?
			 			fa.getScore_MF_Version(this._param, this.getUnlabeledNetwork().getMarginalSharedArray(), globalParamVersion):
			 				fa.getScore(this._param, globalParamVersion);
				if(NetworkConfig.MODEL_TYPE.USE_COST){
					try{
						score += this._param.cost(this, k, children_k, children_k_index, this._compiler);
					} catch (NullPointerException e){
						System.err.println("WARNING: Compiler was not specified during network creation, setting cost to 0.0");
					}
				}
				if (NetworkConfig.USE_NEURAL_FEATURES) {
					score += this._param._fm.getParam_G().getNNParamG().getNNScore(this, k, children_k, children_k_index);
				}
				for(int child_k : children_k){
					score += this._max[child_k];
				}
				if(score >= this._max[k]){
					this._max[k] = score;
					this._max_paths[k] = children_k;
				}
//				NodeHypothesis[] children = new NodeHypothesis[children_k.length];
//				for(int i=0; i<children.length; i++){
//					if(children_k[i] < 0){
//						children[i] = new NodeHypothesis(children_k[i]);
//					} else {
//						children[i] = this._hypotheses[children_k[i]];
//					}
//				}
//				childrenOfThisNodeHypothesis[children_k_index] = new EdgeHypothesis(k, children, score);
			}
//			this._hypotheses[k] = new NodeHypothesis(k, childrenOfThisNodeHypothesis);
//			ScoredIndex bestPath = this._hypotheses[k].getKthBestHypothesis(0);
////			System.out.println("Node: "+this._hypotheses[k]);
////			System.out.println("Edges: "+Arrays.toString(childrenOfThisNodeHypothesis));
////			System.out.println(bestPath);
//			EdgeHypothesis edge = this._hypotheses[k].children()[bestPath.index[0]];
//			this._max_paths[k] = new int[edge.children().length];
//			for(int i=0; i<edge.children().length; i++){
//				this._max_paths[k][i] = edge.children()[i].nodeIndex();
//			}
//			this._max[k] = bestPath.score;
		}
	}
	
	private double sumLog(double inside, double score) {
		double v1 = inside;
		double v2 = score;
		if(v1==v2 && v2==Double.NEGATIVE_INFINITY){
			return Double.NEGATIVE_INFINITY;
		} else if(v1==v2 && v2==Double.POSITIVE_INFINITY){
			return Double.POSITIVE_INFINITY;
		} else if(v1>v2){
			return Math.log1p(Math.exp(v2-v1))+v1;
		} else {
			return Math.log1p(Math.exp(v1-v2))+v2;
		}
	}

	/**
	 * Count the number of removed nodes
	 */
	public int countRemovedNodes(){
		int count = 0;
		for(int k = 0; k<this.countNodes(); k++)
			if(this.isRemoved(k))
				count++;
		return count;
	}
	
	/**
	 * Get the root node of the network.
	 * @return
	 */
	public long getRoot(){
		return this.getNode(this.countNodes()-1);
	}
	
	/**
	 * Get the index of the root node in the network
	 * @return
	 */
	public int getRootId(){
		return this.countNodes()-1;
	}
	
	/**
	 * Get the array form of the node at the specified index in the node array
	 */
	public int[] getNodeArray(int k){
		long node = this.getNode(k);
		return NetworkIDMapper.toHybridNodeArray(node);
	}
	
	//this ad-hoc method is useful when performing
	//some special sum operations (in conjunction with max operations)
	//in the decoding phase.
	protected boolean isSumNode(int k){
		return false;
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i<this.countNodes(); i++)
			sb.append(Arrays.toString(NetworkIDMapper.toHybridNodeArray(this.getNode(i))));
		return sb.toString();
	}
	
	/**
	 * To check if the node is visible
	 * @return
	 */
	public boolean[] getVisible(){
		return this.isVisible;
	}
	
	
	/**Abstract methods for mean-field inference.
	 * 
	 * Not really abstract methods here since other projects do not implement due to old version.
	 * **/
	/**
	 * Only required when we used the mean-field inference method.
	 * Need to implemented in user's own network. No need to implement if not using mean-field inference.
	 */
	public void enableKthStructure(int kthStructure){}
	
	/**
	 * For mean-field inference, set the current structure in this network  
	 * @param structure: the structure we want to used
	 */
	public void setStructure(int structure){
		this.currentStructure = structure;
	}
	
	/**
	 * Get the current structure of this network
	 * @return the current structure id. used in decoding
	 */
	public int getStructure(){
		return this.currentStructure;
	}
	
	/**
	 * specifically for mean-field experiments
	 * Need to implement this while using mean field inference 
	 */
	public void initStructArr(){
		
	};
	
	/**
	 * new the marginal map, clear all elements
	 */
	public void clearMarginalMap(){
		double[] marginal = this.getMarginalSharedArray();
		Arrays.fill(marginal, 0.0);
	}
	
	public void renewCurrentMarginalMap(){
		this._marginal = this.getMarginalSharedArray();
		for (int k = 0; k < this._marginal.length; k++) {
			this._marginal[k] = this._newMarginal[k];
		}
		Arrays.fill(this._newMarginal, 0.0);
	}
	
	/**
	 * Compare the new and old marginal map
	 * decide to continue mean-field update or not 
	 * @return almost equal OR not
	 */
	public boolean compareMarginalMap(){
		if(this._marginal == null)
			return false;
		double diff = 0;
		for (int k = 0; k < this._marginal.length; k++) {
			diff += Math.abs(this._marginal[k]-this._newMarginal[k]);
		}
		diff /= this._marginal.length;
		if(diff < 0.0001)
			return true;
		return false;
	}
	
	public int getPosForNode(int[] nodeArray) {
		return getCompiler().getPosForNode(nodeArray);
	}
	
	public Object getOutputForNode(int[] nodeArray) {
		return getCompiler().getOutputForNode(nodeArray);
	}
}
