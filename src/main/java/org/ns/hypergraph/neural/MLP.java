package org.ns.hypergraph.neural;

import org.ns.hypergraph.neural.NeuralNetworkCore;

public class MLP extends NeuralNetworkCore {

	private static final long serialVersionUID = -6457902817484777222L;
	
	public static final String IN_SEP = "#IN#";
	public static final String OUT_SEP = "#OUT#";

	public MLP(int numLabels, int embeddingSize, int halfWindowSize, int hiddenSize, int cnnWindowSize, String embedding, boolean usePositionEmbedding) {
		super(numLabels);
		config.put("class", "MultiLayerPerceptron");
		config.put("embedding", embedding);
	    config.put("embeddingSize", embeddingSize);
	    config.put("windowSize", halfWindowSize * 2 + 1);
	    config.put("hiddenSize", hiddenSize);
	    config.put("cnnWindowSize", cnnWindowSize);
	    config.put("IN_SEP", IN_SEP);
	    config.put("OUT_SEP", OUT_SEP);
	    config.put("positionEmbedding", usePositionEmbedding);
	    /*
	    config.put("positionVocabSize", TargetSentimentGlobal.POSITION_VOCAB_SZIE);
	    config.put("positionEmbeddingSize", TargetSentimentGlobal.POSITION_EMBEDDING_SIZE);
	    config.put("sentimentWordEmbeddingSize", 25);
	    config.put("fixEmbedding", TargetSentimentGlobal.FIX_EMBEDDING);
	    if (TargetSentimentGlobal.testVocabFile != null) config.put("testVocab", TargetSentimentGlobal.testVocabFile);*/
	}

	@Override
	public int hyperEdgeInput2OutputRowIndex(Object edgeInput) {
		return this.getNNInputID(edgeInput);
	}

	@Override
	public Object hyperEdgeInput2NNInput(Object edgeInput) {
		return edgeInput;
	}

}
