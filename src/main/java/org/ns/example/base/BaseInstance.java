package org.ns.example.base;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.ns.commons.types.Instance;
import org.ns.util.GeneralUtils;

public abstract class BaseInstance<SELF extends BaseInstance<SELF, IN, OUT>, IN, OUT> extends Instance {
	
	private static final long serialVersionUID = -5422835104552434445L;
	public IN input;
	public OUT output;
	public OUT prediction;
	/** The top-K predictions of this instance */
	protected List<OUT> _topKPredictions;

	public BaseInstance(int instanceId, double weight) {
		super(instanceId, weight);
		setLabeled();
	}

	@SuppressWarnings("unchecked")
	@Override
	public SELF duplicate() {
		SELF result = null;
		try {
			result = (SELF)this.getClass().getConstructor(int.class, double.class).newInstance(this.getInstanceId(), this.getWeight());
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new RuntimeException("Instance cannot be automatically duplicated. Please override duplicate() function in Instance implementation.");
		}
		result.input = duplicateInput();
		result.output = duplicateOutput();
		result.prediction = duplicatePrediction();
		result._topKPredictions = duplicateTopKPredictions();
		return result;
	}

	/**
	 * Duplicate the input.<br>
	 * Note that generally it is expected that the returned object is not the same object
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public IN duplicateInput(){
		try {
			return this.input == null ? null : (IN)GeneralUtils.getMatchingAvailableConstructor(this.input.getClass(), this.input.getClass()).newInstance(this.input);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new RuntimeException("Cannot duplicate input automatically, please override duplicateInput method.");
		}
	}
	
	/**
	 * Duplicate the output.<br>
	 * Note that generally it is expected that the returned object is not the same object
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public OUT duplicateOutput(){
		try {
			return this.output == null ? null : (OUT)GeneralUtils.getMatchingAvailableConstructor(this.output.getClass(), this.output.getClass()).newInstance(this.output);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new RuntimeException("Cannot duplicate output automatically, please override duplicateOutput method.");
		}
	}
	
	/**
	 * Duplicate the prediction.<br>
	 * Note that generally it is expected that the returned object is not the same object
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public OUT duplicatePrediction(){
		try {
			return this.prediction == null ? null : (OUT)GeneralUtils.getMatchingAvailableConstructor(this.prediction.getClass(), this.prediction.getClass()).newInstance(this.prediction);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new RuntimeException("Cannot duplicate prediction automatically, please override duplicatePrediction method.");
		}
	}
	
	/**
	 * Duplicate the top-k prediction list.<br>
	 * Note that generally it is expected that the returned object is not the same object
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<OUT> duplicateTopKPredictions(){
		try {
			return this._topKPredictions == null ? null : (List<OUT>)GeneralUtils.getMatchingAvailableConstructor(this._topKPredictions.getClass(), this._topKPredictions.getClass()).newInstance(this._topKPredictions);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new RuntimeException("Cannot duplicate prediction automatically, please override duplicatePrediction method.");
		}
	}

	@Override
	public void removeOutput() {
		output = null;
	}

	@Override
	public void removePrediction() {
		prediction = null;
	}

	@Override
	public IN getInput() {
		return input;
	}

	@Override
	public OUT getOutput() {
		return output;
	}

	@Override
	public OUT getPrediction() {
		return prediction;
	}
	
	public List<OUT> getTopKPredictions(){
		if(this._topKPredictions != null){
			return this._topKPredictions;
		}
		List<OUT> result = new ArrayList<OUT>();
		result.add(getPrediction());
		return result;
	}
	
	@Override
	public boolean hasOutput() {
		return output != null;
	}

	@Override
	public boolean hasPrediction() {
		return prediction != null;
	}
	
	@SuppressWarnings("unchecked")
	public void setOutput(Object o) {
		output = (OUT)o;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setPrediction(Object o) {
		prediction = (OUT)o;
	}
	
	public void setTopKPredictions(List<OUT> topKPredictions){
		this._topKPredictions = topKPredictions;
	}
	
	@SuppressWarnings("unchecked")
	public void setTopKPredictions(Object topKPredictions){
		setTopKPredictions((List<OUT>)topKPredictions);
	}

}
