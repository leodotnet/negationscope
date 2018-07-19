/** Statistical Natural Language Processing System
    Copyright (C) 2014  Lu, Wei

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
package org.ns.commons.types;

import java.util.List;

import org.ns.example.base.BaseInstance;

public class LinearInstance<T> extends BaseInstance<LinearInstance<T>, List<String[]>, List<T>>{
	
	private static final long serialVersionUID = 3336336220436168888L;
	
	public LinearInstance(int instanceId, double weight){
		this(instanceId, weight, null);
	}
	
	public LinearInstance(int instanceId, double weight, List<String[]> input){
		this(instanceId, weight, input, null, null);
	}
	
	public LinearInstance(int instanceId, double weight, List<String[]> input, List<T> output){
		this(instanceId, weight, input, output, null);
	}
	
	public LinearInstance(int instanceId, double weight, List<String[]> input, List<T> output, List<T> prediction){
		super(instanceId, weight);
		this.input = input;
		this.output = output;
		this.prediction = prediction;
	}

	public int size(){
		return this.input.size();
	}
	
	/**
	 * Returns the number of positions in this linear instance where the output matches the prediction
	 * @return
	 */
	public int countNumCorrectlyPredicted(){
		if(this.output == null){
			throw new RuntimeException("Instance "+this._instanceId+" has no output.");
		}
		if(this.prediction == null){
			throw new RuntimeException("Instance "+this._instanceId+" has no prediction.");
		}
		
		int count = 0;
		for(int k = 0; k<this.output.size(); k++){
			T corr = this.output.get(k);
			T pred = this.prediction.get(k);
			if(corr.equals(pred)){
				count += 1;
			}
		}
		return count;
	}
	
	public String toString(){
		StringBuilder result = new StringBuilder();
		for(int i=0; i<size(); i++){
			String[] inputValues = input.get(i);
			for(int j=0; j<inputValues.length; j++){
				if(j > 0){
					result.append("\t");
				}
				result.append(inputValues[j]);
			}
			result.append("\t");
			result.append(prediction.get(i));
			result.append("\t");
			result.append(output.get(i));
			result.append("\n");
		}
		return result.toString();
	}

}