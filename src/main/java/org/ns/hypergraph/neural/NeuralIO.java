package org.ns.hypergraph.neural;

import java.io.Serializable;

public class NeuralIO implements Serializable{

	private static final long serialVersionUID = -4422598036768376675L;
	
	protected Object input;
	protected int output;
	
	public NeuralIO(Object input, int output) {
		this.input = input;
		this.output = output;
	}
	
	public Object getInput() {
		return input;
	}
	
	public int getOutput() {
		return output;
	}
	
	public void setInput(Object input) {
		this.input = input;
	}
	
	public void setOutput(int output) {
		this.output = output;
	}

}
