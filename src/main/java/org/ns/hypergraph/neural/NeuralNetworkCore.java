package org.ns.hypergraph.neural;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.ns.hypergraph.Network;
import org.ns.hypergraph.NetworkConfig;
import org.ns.hypergraph.NetworkConfig.ModelStatus;
import org.ns.hypergraph.neural.util.LuaFunctionHelper;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import th4j.Tensor.DoubleTensor;

public abstract class NeuralNetworkCore extends AbstractNeuralNetwork implements Cloneable {
	
	private static final long serialVersionUID = -2638896619016178432L;

	protected HashMap<String,Object> config;
	
	protected transient boolean isTraining;
	
	/**
	 * Corresponding Torch tensors for params and gradParams
	 */
	protected transient DoubleTensor paramsTensor, gradParamsTensor;
	
	/**
	 * Corresponding Torch tensors for output and gradOutput
	 */
	protected transient DoubleTensor outputTensorBuffer, countOutputTensorBuffer;
	
	public transient boolean optimizeNeural;
	
	/**
	 * Neural network input to index (id)
	 * If you are using batch training, do not directly use this to obtain input id.
	 * Use the method # {@link #getNNInputID()}
	 */
	protected transient Map<Object, Integer> nnInput2Id;
	
	/**
	 * Save the mapping from instance id to neural network input id.
	 */
	protected transient TIntObjectMap<TIntList> instId2NNInputId;
	
	/**
	 * Dynamically save the batch instance id in batch training.
	 */
	protected transient TIntIntMap dynamicNNInputId2BatchInputId;
	
	protected boolean continuousFeatureValue = false;
	
	protected String nnModelFile = null;
	
	public NeuralNetworkCore(int numLabels) {
		super(numLabels);
		config = new HashMap<>();
		optimizeNeural = NetworkConfig.OPTIMIZE_NEURAL;
		config.put("optimizeNeural", optimizeNeural);
		config.put("numLabels", numLabels);
	}
	
	@Override
	public void initialize() {
		List<Object> nnInputs = new ArrayList<>(nnInput2Id.size());
		for (Object obj : nnInput2Id.keySet()) {
			nnInputs.add(obj);
		}
		if (!this.continuousFeatureValue) {
			config.put("nnInputs", nnInputs);
		} else {
			this.prepareContinuousFeatureValue();
		}
		Object[] args = null;
		if (isTraining || this.outputTensorBuffer == null) {
			this.countOutputTensorBuffer = new DoubleTensor();
			this.outputTensorBuffer = new DoubleTensor();
			args = new Object[]{config, outputTensorBuffer, countOutputTensorBuffer};
		} else {
			args = new Object[]{config};
		}
		
		config.put("isTraining", isTraining);
        Class<?>[] retTypes;
        if (optimizeNeural && isTraining) {
        	retTypes = new Class[]{DoubleTensor.class, DoubleTensor.class};
        } else {
        	retTypes = new Class[]{};
        }
        Object[] outputs = LuaFunctionHelper.execLuaFunction(this.L, "initialize", args, retTypes);
        
		if(optimizeNeural && isTraining) {
			this.paramsTensor = (DoubleTensor) outputs[0];
			this.gradParamsTensor = (DoubleTensor) outputs[1];
			if (this.paramsTensor.nElement() > 0) {
				this.params = this.getArray(this.paramsTensor, this.params);
				//TODO: this one might not be needed. Because the gradient at the first initialization is 0..
				this.gradParams = this.getArray(this.gradParamsTensor, this.gradParams);
				if (NetworkConfig.INIT_FV_WEIGHTS) {
					Random rng = new Random(NetworkConfig.RANDOM_INIT_FEATURE_SEED);
					//also be careful that you may overwrite the initialized embedding if you use this.
					for(int i = 0; i < this.params.length; i++) {
						this.params[i] = NetworkConfig.RANDOM_INIT_WEIGHT ? (rng.nextDouble()-.5)/10 :
							NetworkConfig.FEATURE_INIT_WEIGHT;
					}
				}
			}
		}
	}

	protected void prepareContinuousFeatureValue() {
		//should be overrided
	}
	
	/**
	 * Calculate the input position in the output/countOuput matrix position
	 * @return
	 */
	public abstract int hyperEdgeInput2OutputRowIndex(Object edgeInput);
	
	public int getNNInputID(Object nnInput) {
		if (NetworkConfig.USE_BATCH_TRAINING && isTraining) {
			return this.dynamicNNInputId2BatchInputId.get(this.nnInput2Id.get(nnInput));
		} else {
			return this.nnInput2Id.get(nnInput);
		}
	}
	
	public int getNNInputSize() {
		if (NetworkConfig.USE_BATCH_TRAINING && isTraining) {
			return this.dynamicNNInputId2BatchInputId.size();
		} else {
			return this.nnInput2Id.size();
		}
	}
	
	/**
	 * Neural network's forward
	 */
	@Override
	public void forward(TIntSet batchInstIds) {
		if (optimizeNeural && isTraining || NetworkConfig.STATUS == ModelStatus.TESTING || NetworkConfig.STATUS == ModelStatus.DEV_IN_TRAINING) { // update with new params
			if (getParamSize() > 0) {
				this.paramsTensor.storage().copy(this.params); // we can do this because params is contiguous
				//System.out.println("java side forward weights: " + this.params[0] + " " + this.params[1]);
			}
		}
		Object[] args = null;
		if (NetworkConfig.USE_BATCH_TRAINING && isTraining && batchInstIds != null
				&& batchInstIds.size() > 0) {
			//pass the batch input id.
			TIntIterator iter = batchInstIds.iterator();
			TIntHashSet set = new TIntHashSet();
			while(iter.hasNext()) {
				int positiveInstId = iter.next();
				if (this.instId2NNInputId.containsKey(positiveInstId))
					set.addAll(this.instId2NNInputId.get(positiveInstId));
			}
			TIntList batchInputIds = new TIntArrayList(set);
			this.dynamicNNInputId2BatchInputId = new TIntIntHashMap(batchInputIds.size());
			for (int i = 0; i < batchInputIds.size(); i++) {
				this.dynamicNNInputId2BatchInputId.put(batchInputIds.get(i), i);
			}
			args = new Object[]{isTraining, batchInputIds};
		} else {
			args = new Object[]{isTraining};
		}
		LuaFunctionHelper.execLuaFunction(this.L, "forward", args, new Class[]{});
		output = this.getArray(outputTensorBuffer, output);
		if (isTraining) {
			//check if countOutput is null.
			if (countOutput == null || countOutput.length < outputTensorBuffer.nElement()) 
				countOutput = new double[(int) outputTensorBuffer.nElement()];
			if (!countOutputTensorBuffer.isSameSizeAs(outputTensorBuffer)) {
				countOutputTensorBuffer.resize(outputTensorBuffer.size());
			}
		}
	}
	
	@Override
	public double getScore(Network network, int parent_k, int children_k_index) {
		double val = 0.0;
		NeuralIO io = getHyperEdgeInputOutput(network, parent_k, children_k_index);
		if (io != null) {
			Object edgeInput = io.getInput();
			int outputLabel = io.getOutput();
			int idx = this.hyperEdgeInput2OutputRowIndex(edgeInput) * this.numLabels + outputLabel;
			val = output[idx];
		}
		return val;
	}
	
	/**
	 * Neural network's backpropagation
	 */
	@Override
	public void backward() {
		countOutputTensorBuffer.storage().copy(this.countOutput);
		Object[] args = new Object[]{};
		Class<?>[] retTypes = new Class[0];
		LuaFunctionHelper.execLuaFunction(this.L, "backward", args, retTypes);
		if(optimizeNeural && getParamSize() > 0) { // copy gradParams computed by Torch
			gradParams = this.getArray(this.gradParamsTensor, gradParams);
			if (NetworkConfig.REGULARIZE_NEURAL_FEATURES) {
				addL2ParamsGrad();
			}
		}
		this.resetCountOutput();
	}
	
	@Override
	public void update(double count, Network network, int parent_k, int children_k_index) {
		NeuralIO io = getHyperEdgeInputOutput(network, parent_k, children_k_index);
		if (io != null) {
			Object edgeInput = io.getInput();
			int outputLabel = io.getOutput();
			int idx = this.hyperEdgeInput2OutputRowIndex(edgeInput) * this.numLabels + outputLabel;
			synchronized (countOutput) {
				//TODO: alternatively, create #threads of countOutput array.
				//Then aggregate them together.
				countOutput[idx] -= count;
			}
		}
	}
	
	public void resetCountOutput() {
		Arrays.fill(countOutput, 0.0);
	}
	
	/**
	 * Save the model by calling the specific function in Torch
	 * @param func : the function in torch
	 * @param prefix : model prefix
	 */
	public void save(String func, String prefix) {
		LuaFunctionHelper.execLuaFunction(this.L, func, new Object[]{prefix}, new Class[]{});
	}
	
	/**
	 * Save the trained model, implement the "save_model" method in torch
	 * @param prefix
	 */
	public void save(String prefix) {
		this.save("save_model", prefix);
	}
	
	/**
	 * Load a trained model, using the specific function in Torch
	 * @param func: the specific function for loading model
	 * @param prefix: model prefix.
	 */
	public void load(String func, String prefix) {
		LuaFunctionHelper.execLuaFunction(this.L, func, new Object[]{prefix}, new Class[]{});
	}
	
	/**
	 * Load a model from disk, implement the "load_model" method in torch
	 * @param prefix
	 */
	public void load() {
		this.load("load_model", this.nnModelFile);
	}
	
	@Override
	public void closeProvider() {
		this.cleanUp();
	}
	
	/**
	 * Clean up resources, currently, we clean up the resource after decoding
	 */
	public void cleanUp() {
		L.close();
	}
	
	/**
	 * Read a DoubleTensor to a buffer.
	 * @param t
	 * @param buf
	 * @return
	 */
	protected double[] getArray(DoubleTensor t, double[] buf) {
		if (buf == null || buf.length < t.nElement()) {
			buf = new double[(int) t.nElement()];
        }
		t.storage().getRawData().read(0, buf, 0, (int) t.nElement());
		return buf;
	}

	@Override
	protected NeuralNetworkCore clone(){
		NeuralNetworkCore c = null;
		try {
			c = (NeuralNetworkCore) super.clone();
			c.nnInput2Id = null;
			c.params = this.params;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return c;
	}
	
	public NeuralNetworkCore setModelFile(String nnModelFile) {
		this.nnModelFile = nnModelFile;
		return this;
	}

	private void writeObject(ObjectOutputStream out) throws IOException{
		out.writeObject(this.config);
		out.writeBoolean(this.continuousFeatureValue);
		out.writeInt(this.netId);
		out.writeInt(this.numLabels);
		out.writeDouble(this.scale);
		out.writeObject(this.nnModelFile);
		this.save(this.nnModelFile);
	}
	
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException{
		this.config = (HashMap<String, Object>) in.readObject();
		this.continuousFeatureValue = in.readBoolean();
		this.netId = in.readInt();
		this.numLabels = in.readInt();
		this.scale = in.readDouble();
		this.nnModelFile = (String) in.readObject();
		this.config.put("nnModelFile", this.nnModelFile);
		this.configureJNLua();
		this.load();
	}
	
}


