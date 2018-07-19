package org.ns.hypergraph.neural;

public abstract class ContinuousFeatureValueProvider extends NeuralNetworkCore {

	private static final long serialVersionUID = -4464018923146788406L;
	
	protected int numFeatureValues;
	
	public ContinuousFeatureValueProvider(int numLabels) {
		this(1, numLabels);
	}
	
	public ContinuousFeatureValueProvider(int numFeatureValues ,int numLabels) {
		super(numLabels);
		this.optimizeNeural = true; //for continuous feature, optimize neural is always true.
		this.continuousFeatureValue = true;
		this.numFeatureValues = numFeatureValues;
		config.put("class", "ContinuousFeature");
		config.put("numLabels", numLabels);
		config.put("numValues", numFeatureValues);
	}

	/**
	 * Fill the featureValue array using the input object
	 * @param input
	 * @return
	 */
	public abstract double[] getFeatureValue(Object input);
	
	protected void prepareContinuousFeatureValue() { 
		double[][] featureValues = new double[nnInput2Id.size()][this.numFeatureValues];
		for (Object input : nnInput2Id.keySet()) {
			featureValues[nnInput2Id.get(input)] = this.getFeatureValue(input);
		}
		config.put("nnInputs", featureValues);
	}
	
	@Override
	public Object hyperEdgeInput2NNInput(Object edgeInput) {
		return edgeInput;
	}

	@Override
	public int hyperEdgeInput2OutputRowIndex(Object input) {
		return nnInput2Id.get(input);
	}

}
