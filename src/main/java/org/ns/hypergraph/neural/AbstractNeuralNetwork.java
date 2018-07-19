package org.ns.hypergraph.neural;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.ns.commons.ml.opt.MathsVector;
import org.ns.hypergraph.LocalNetworkParam;
import org.ns.hypergraph.Network;
import org.ns.hypergraph.NetworkConfig;

import com.naef.jnlua.LuaState;
import com.sun.jna.Library;
import com.sun.jna.Native;

import gnu.trove.set.TIntSet;


public abstract class AbstractNeuralNetwork implements Serializable{
	
	private static final long serialVersionUID = 1501009887917654699L;

	public static String LUA_VERSION = "5.2";
	
	protected int netId;
	
	/**
	 * A LuaState instance for loading Lua scripts
	 */
	public transient LuaState L;
	
	/**
	 * The total number of unique output labels
	 */
	protected int numLabels;
	
	/**
	 * The neural net's internal weights and gradients
	 */
	protected transient double[] params, gradParams;
	
	/**
	 * A flattened matrix containing the continuous values
	 * with the shape (inputSize x numLabels).
	 */
	protected transient double[] output, countOutput;
	
	/**
	 * The coefficient used for regularization, i.e., batchSize/totalInstNum.
	 */
	protected double scale;
	
	protected transient LocalNetworkParam[] params_l;
	
	public AbstractNeuralNetwork(int numLabels) {
		this.numLabels = numLabels;
		this.configureJNLua();
	}
	
	public void setLocalNetworkParams (LocalNetworkParam[] params_l) {
		this.params_l = params_l;
	}
	
	/**
	 * Configure paths for JNLua and create a new LuaState instance
	 * for loading the backend Torch/Lua script
	 */
	protected void configureJNLua() {
		System.setProperty("jna.library.path","./nativeLib");
		System.setProperty("java.library.path", "./nativeLib:" + System.getProperty("java.library.path"));
		Field fieldSysPath = null;
		try {
			fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
			fieldSysPath.setAccessible(true);
			fieldSysPath.set(null, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		String jnluaLib = null;
		if (LUA_VERSION.equals("5.2")) {
			jnluaLib = "libjnlua52";
		} else if (LUA_VERSION.equals("5.1")) {
			jnluaLib = "libjnlua5.1";
		}
		if (NetworkConfig.OS.equals("osx")) {
			jnluaLib += ".jnilib";
		} else if (NetworkConfig.OS.equals("linux")) {
			jnluaLib += ".so";
		}
		Native.loadLibrary(jnluaLib, Library.class);
		
		this.L = new LuaState();
		this.L.openLibs();
		
		try {
			this.L.load(Files.newInputStream(Paths.get("nn-crf-interface/neural_server/NetworkInterface.lua")),"NetworkInterface.lua","bt");
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.L.call(0,0);
	}
	
	public abstract Object hyperEdgeInput2NNInput(Object edgeInput);
	
	/**
	 * Initialize this provider (e.g., create a network and prepare its input)
	 */
	public abstract void initialize();
	
	/**
	 * Get the score associated with a specified hyper-edge
	 * @param network
	 * @param parent_k
	 * @param children_k_index
	 * @return score
	 */
	public abstract double getScore(Network network, int parent_k, int children_k_index);
	
	/**
	 * Pre-compute all scores for each hyper-edge.
	 * In neural network, this is equivalent to forward.
	 */
	public abstract void forward(TIntSet batchInstIds);
	
	/**
	 * Accumulate count for a specified hyper-edge
	 * @param count
	 * @param network
	 * @param parent_k
	 * @param children_k_index
	 */
	public abstract void update(double count, Network network, int parent_k, int children_k_index);
	
	/**
	 * Compute gradient based on the accumulated counts from all hyper-edges.
	 * In neural network, this is equivalent to backward.
	 */
	public abstract void backward();
	
	/**
	 * Get the input associated with a specified hyper-edge
	 * @param network
	 * @param parent_k
	 * @param children_k_index
	 * @return input
	 */
	public NeuralIO getHyperEdgeInputOutput(Network network, int parent_k, int children_k_index) {
		return this.params_l[network.getThreadId()].getHyperEdgeIO(network, this.netId, parent_k, children_k_index);
	}
	
	public abstract void closeProvider();
	
	/**
	 * Reset gradient
	 */
	public void resetGrad() {
		if (countOutput != null) {
			Arrays.fill(countOutput, 0.0);
		}
		if (gradParams != null && getParamSize() > 0) {
			Arrays.fill(gradParams, 0.0);
		}
	}
	
	public double getL2Params() {
		if (getParamSize() > 0) {
			return MathsVector.square(params);
		}
		return 0.0;
	}
	
	public void addL2ParamsGrad() {
		if (getParamSize() > 0) {
			double _kappa = NetworkConfig.L2_REGULARIZATION_CONSTANT;
			for(int k = 0; k<gradParams.length; k++) {
				if(_kappa > 0) {
					gradParams[k] += 2 * scale * _kappa * params[k];
				}
			}
		}
	}
	
	public void setNeuralNetId(int netId){
		this.netId = netId;
	}
	
	public int getNeuralNetId() {
		return this.netId;
	}
	
	public int getParamSize() {
		return params == null ? 0 : params.length;
	}

	public double[] getParams() {
		return params;
	}

	public double[] getGradParams() {
		return gradParams;
	}
	
	public void setScale(double coef) {
		scale = coef;
	}
	
}
