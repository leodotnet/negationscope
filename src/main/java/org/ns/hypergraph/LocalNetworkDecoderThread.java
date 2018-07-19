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
import java.util.List;

import org.ns.commons.types.Instance;
import org.ns.hypergraph.NetworkConfig.InferenceType;
import org.ns.hypergraph.NetworkConfig.ModelStatus;
import org.ns.hypergraph.decoding.EdgeHypothesis;
import org.ns.hypergraph.decoding.NodeHypothesis;
import org.ns.hypergraph.decoding.ScoredIndex;

public class LocalNetworkDecoderThread extends Thread{
	
	//the id of the thread.
	private int _threadId = -1;
	//the local feature map.
	private LocalNetworkParam _param;
	//the instances assigned to this thread.
	private Instance[] _instances_input;
	//the instances assigned to this thread.
	private Instance[] _instances_output;
	//the builder.
	private NetworkCompiler _compiler;
	private boolean _cacheParam = true;
	private int numPredictionsGenerated = 1;
	private boolean isTouching;
	
	//please make sure the threadId is 0-indexed.
	public LocalNetworkDecoderThread(int threadId, FeatureManager fm, Instance[] instances, NetworkCompiler compiler){
		this(threadId, fm, instances, compiler, false);
	}
	
	public LocalNetworkDecoderThread(int threadId, FeatureManager fm, Instance[] instances, NetworkCompiler compiler, int numPredictionsGenerated){
		this(threadId, fm, instances, compiler, false, numPredictionsGenerated);
	}
	
	public LocalNetworkDecoderThread(int threadId, FeatureManager fm, Instance[] instances, NetworkCompiler compiler, boolean cacheParam){
		this(threadId, fm, instances, compiler, cacheParam, 1);
	}
	
	public LocalNetworkDecoderThread(int threadId, FeatureManager fm, Instance[] instances, NetworkCompiler compiler, boolean cacheParam, int numPredictionsGenerated){
		this(threadId, fm, instances, compiler, new LocalNetworkParam(threadId, fm, instances.length), cacheParam, numPredictionsGenerated);
	}

	public LocalNetworkDecoderThread(int threadId, FeatureManager fm, Instance[] instances, NetworkCompiler compiler, LocalNetworkParam param, boolean cacheParam){
		this(threadId, fm, instances, compiler, param, cacheParam, 1);
	}
	
	public LocalNetworkDecoderThread(int threadId, FeatureManager fm, Instance[] instances, NetworkCompiler compiler, LocalNetworkParam param, boolean cacheParam, int numPredictionsGenerated){
		this(threadId, fm, instances, compiler, param, cacheParam, numPredictionsGenerated, false);
	}
	
	//please make sure the threadId is 0-indexed.
	public LocalNetworkDecoderThread(int threadId, FeatureManager fm, Instance[] instances, NetworkCompiler compiler, LocalNetworkParam param, boolean cacheParam, int numPredictionsGenerated, boolean isTouching){
		this._threadId = threadId;
		this._param = param;
		this._param.setGlobalMode();//set it to global mode
		this._instances_input = instances;
		this._compiler = compiler;
		this._cacheParam = cacheParam;
		this.numPredictionsGenerated = numPredictionsGenerated;
		this.isTouching = isTouching;
	}
	
	public LocalNetworkDecoderThread copyThread(FeatureManager fm) {
		return new LocalNetworkDecoderThread(_threadId, fm, _instances_input, 
				_compiler, _param, _cacheParam, numPredictionsGenerated, isTouching);
	}
	
	public LocalNetworkParam getParam(){
		return this._param;
	}
	
	@Override
	public void run(){
		if(!isTouching){
    		this.max();
    	} else {
    		this.touch();
    	}
	}
	
	public void max(){
		int numSentences = this._instances_input.length;
		int numTokens = 0;
		for(Instance instance: this._instances_input){
			numTokens += instance.size();
		}
		long time = System.currentTimeMillis();
		this._instances_output = new Instance[this._instances_input.length];
		for(int k = 0; k<this._instances_input.length; k++){
//			System.err.println("Thread "+this._threadId+"\t"+k);
			this._instances_output[k] = this.max(this._instances_input[k], k);
		}
		time = System.currentTimeMillis() - time;
		double timeInSecond = time/1000.0;
		/*if (NetworkConfig.STATUS == ModelStatus.TESTING)
		System.err.println("Decoding time for thread "+this._threadId+" = "+ timeInSecond +" secs "+
				String.format("(%d/%.3f = %.3f tokens/s, %d/%.3f = %.3f sentences/s)", numTokens, timeInSecond, numTokens/timeInSecond, numSentences, timeInSecond, numSentences/timeInSecond));*/
	}
	
	public Instance max(Instance instance, int networkId){
		Network network = this._compiler.compileAndStore(networkId, instance, this._param);
		if(!_cacheParam){
			this._param.disableCache();
		}
		if(NetworkConfig.INFERENCE == InferenceType.MEAN_FIELD){
			//initialize the joint feature map and also the marginal score map.
			network.initStructArr();
			network.clearMarginalMap();
			boolean prevDone = false;
			for (int it = 0; it < NetworkConfig.MAX_MF_UPDATES; it++) {
				for (int curr = 0; curr < NetworkConfig.NUM_STRUCTS; curr++) {
					network.enableKthStructure(curr);
					network.inference(true);
				}
				boolean done = network.compareMarginalMap();
				if (prevDone && done){
					network.renewCurrentMarginalMap();
					break;
				}
				prevDone = done;
				network.renewCurrentMarginalMap();
			}
			Instance inst = null;
			for (int curr = 0; curr < NetworkConfig.NUM_STRUCTS; curr++) {
				network.enableKthStructure(curr);
				network.max();
				network.setStructure(curr);
				inst = this._compiler.decompile(network);
			}
			return inst;
		}else if(NetworkConfig.MAX_MARGINAL_DECODING){
			network.inference(true);
			network.renewCurrentMarginalMap();
		}else{
			network.max();
		}
		
		//if(numPredictionsGenerated == 1){
			return this._compiler.decompile(network);
		//}
			/*else {
			try{
				// Try calling the implementation for top-K decompiler (not decoding)
				return this._compiler.decompile(network, numPredictionsGenerated);
			} catch (UnsupportedOperationException e){
				// If not implemented, then do a workaround by changing the max array into the k-th best prediction
				// Then call decompile to get the k-th best structure.
				Instance result = this._compiler.decompile(network);
				List<Object> topKPredictions = new ArrayList<>();
				result.setTopKPredictions(topKPredictions);
				topKPredictions.add(result.getPrediction());
				NodeHypothesis rootHypothesis = network.getNodeHypothesis(network.getRootId());
				for(int i=1; i<numPredictionsGenerated; i++){
					ScoredIndex kthPrediction = rootHypothesis.getKthBestHypothesis(i);
					if(kthPrediction == null){
						break;
					}
					setMaxArrayForKthPrediction(network, kthPrediction, rootHypothesis);
					Instance tmp = this._compiler.decompile(network);
					topKPredictions.add(tmp.getPrediction());
				}
				// Set the max to the true max again
				setMaxArrayForBestPrediction(network);
				return result;
			}
		}*/
	}
	
	/**
	 * This method is to restore the max array with the best prediction structure.<br>
	 * This is used after {@link #setMaxArrayForKthPrediction(Network, ScoredIndex, NodeHypothesis)}
	 * in order to restore the max and max_path arrays. Note that running {@link #setMaxArrayForKthPrediction(Network, ScoredIndex, NodeHypothesis)}
	 * with the best path from root will not restore the max and max_path arrays for all nodes, since 
	 * not all nodes will be visited in the best prediction, which might have been altered by other 
	 * k-th best structures.
	 * @param network
	 *//*
	private void setMaxArrayForBestPrediction(Network network){
		for(int k=0; k<network.countNodes(); k++){
			NodeHypothesis node = network._hypotheses[k];
			ScoredIndex bestPath = node.getKthBestHypothesis(0);
			EdgeHypothesis edge = node.children()[bestPath.index[0]];
			ScoredIndex score = edge.getKthBestHypothesis(bestPath.index[1]);
			int nodeIndex = node.nodeIndex();
			if(network._max_paths[nodeIndex].length != edge.children().length){
				network._max_paths[nodeIndex] = new int[edge.children().length];
			}
			for(int i=0; i<edge.children().length; i++){
				network._max_paths[nodeIndex][i] = edge.children()[i].nodeIndex();
			}
			network._max[nodeIndex] = score.score;
		}
	}*/
	
	/**
	 * This method is to override the max array with the k-th prediction structure.<br>
	 * In the case where there is no decompile (not decoding) method defined for top-K prediction,
	 * we override the max array based on the k-th prediction, then we can call max decompile to
	 * get the k-th output prediction.
	 * @param network
	 * @param kthPrediction
	 * @param nodeHypothesis
	 */
	private void setMaxArrayForKthPrediction(Network network, ScoredIndex kthPrediction, NodeHypothesis nodeHypothesis){
		int nodeIndex = nodeHypothesis.nodeIndex();
		EdgeHypothesis edge = nodeHypothesis.children()[kthPrediction.index[0]];
		ScoredIndex score = edge.getKthBestHypothesis(kthPrediction.index[1]);
		ScoredIndex[] nextPath = new ScoredIndex[edge.children().length];
		if(network._max_paths[nodeIndex].length != edge.children().length){
			network._max_paths[nodeIndex] = new int[edge.children().length];
		}
		for(int i=0; i<edge.children().length; i++){
			nextPath[i] = edge.children()[i].getKthBestHypothesis(score.index[i]);
			network._max_paths[nodeIndex][i] = edge.children()[i].nodeIndex();
		}
		network._max[nodeIndex] = kthPrediction.score;
		for(int i=0; i<edge.children().length; i++){
			setMaxArrayForKthPrediction(network, nextPath[i], edge.children()[i]);
		}
	}
	
	public Instance[] getOutputs(){
		return this._instances_output;
	}
	
	/**
     * Go through all networks to know the possible features, 
     * and caching the networks if {@link #_cacheNetworks} is true.
     */
	public void touch(){
		long time = System.currentTimeMillis();
		//extract the features..
		for(int networkId = 0; networkId< this._instances_input.length; networkId++){
			/*if(networkId%100==0)
				if (NetworkConfig.STATUS == ModelStatus.TESTING)
				System.err.print('.');*/
			Network network = this._compiler.compileAndStore(networkId, this._instances_input[networkId], this._param);
			network.touch();
		}
		/*if (NetworkConfig.STATUS == ModelStatus.TESTING)
			System.err.println();*/
		time = System.currentTimeMillis() - time;
		/*if (NetworkConfig.STATUS == ModelStatus.TESTING)
			System.out.println("Thread "+this._threadId + " touch time: "+ time/1000.0+" secs.");*/
	}
	
	public void setTouch(){
		this.isTouching = true;
	}

	public void setUnTouch(){
		this.isTouching = false;
	}
}
