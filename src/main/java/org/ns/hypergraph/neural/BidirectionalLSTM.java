package org.ns.hypergraph.neural;

import java.util.AbstractMap.SimpleImmutableEntry;

public class BidirectionalLSTM extends NeuralNetworkCore {
	
	private static final long serialVersionUID = 4592893499307238510L;

	public BidirectionalLSTM(int hiddenSize, boolean bidirection, String optimizer, double learningRate, int clipping, int numLabels, int gpuId, String embedding) {
		super(numLabels);
		config.put("class", "SimpleBiLSTM");
        config.put("hiddenSize", hiddenSize);
        config.put("bidirection", bidirection);
        config.put("optimizer", optimizer);
        config.put("numLabels", numLabels);
        config.put("embedding", embedding);
        config.put("gpuid", gpuId);
        config.put("learningRate", learningRate);
        config.put("clipping", clipping);
	}

	@Override
	public Object hyperEdgeInput2NNInput(Object edgeInput) {
		@SuppressWarnings("unchecked")
		SimpleImmutableEntry<String, Integer> sentAndPos = (SimpleImmutableEntry<String, Integer>) edgeInput;
		return sentAndPos.getKey();
	}
	
	@Override
	public int hyperEdgeInput2OutputRowIndex (Object edgeInput) {
		@SuppressWarnings("unchecked")
		SimpleImmutableEntry<String, Integer> sentAndPos = (SimpleImmutableEntry<String, Integer>) edgeInput;
		int sentID = this.getNNInputID(sentAndPos.getKey()); 
		int row = sentAndPos.getValue() * this.getNNInputSize() + sentID;
		return row;
	}

}
