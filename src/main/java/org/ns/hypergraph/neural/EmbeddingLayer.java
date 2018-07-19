package org.ns.hypergraph.neural;

import org.ns.hypergraph.neural.NeuralNetworkCore;

public class EmbeddingLayer extends NeuralNetworkCore {

	private static final long serialVersionUID = 4951822203204790448L;
	
	public static final String IN_SEP = "#IN#";
	public static final String OUT_SEP = "#OUT#";
	
	public EmbeddingLayer(int numLabels, int hiddenSize) {
		this(numLabels,hiddenSize, "random", false, null);
	}

	public EmbeddingLayer(int numLabels, int hiddenSize, String emb, boolean fixEmbedding, String testVocabFile) {
		super(numLabels);
		config.put("class", "EmbeddingLayer");
        config.put("hiddenSize", hiddenSize);
        config.put("embedding", emb);
        config.put("fixEmbedding", fixEmbedding);
        config.put("numLabels", numLabels);
        config.put("IN_SEP", IN_SEP);
	    config.put("OUT_SEP", OUT_SEP);
	    if (testVocabFile != null)
	    	config.put("testVocab", testVocabFile);
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
