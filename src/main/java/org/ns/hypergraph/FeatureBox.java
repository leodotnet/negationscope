package org.ns.hypergraph;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

/**
 * The class used by {@link FeatureArray} to store the list of feature indices and 
 * the cached score of the features associated with this list, as a time-saving mechanism.<br>
 * This can also be used to save memory usage by not allocating new FeatureBox with 
 * the same feature indices as the one that is already created, by storing a cache in a LocalNetworkParam object.
 */
public class FeatureBox implements Serializable {

	private static final long serialVersionUID = 1779316632297457057L;

	/** Feature index array */
	protected int[] _fs;
	/** The total score (weights*values) of the feature in the current _fs. */
	protected double _currScore;
	
	/** The time-saving mechanism, by not recomputing the score if the version is up-to-date. */
	protected int _version;
	/** For now this is used for Mean-Field implementation, to update the weights during MF internal iterations */
	protected boolean _alwaysChange = false;
	
	/** Feature value array */
	protected double[] _fv;
	
	public FeatureBox(int[] fs) {
		this._fs = fs;
		this._version = -1; //the score is not calculated yet.
	}
	
	public FeatureBox(int[] fs, double[] fv) {
		this._fs = fs;
		this._version = -1; //the score is not calculated yet.
		this._fv = fv;
	}
	
	public int length() {
		return this._fs.length;
	}
	
	public int[] get() {
		return this._fs;
	}
	
	public int get(int pos) {
		return this._fs[pos];
	}

	/**
	 * Use the map to cache the feature index array to save the memory.
	 * @param fs
	 * @param param
	 * @return
	 */
	public static FeatureBox getFeatureBox(int[] fs, LocalNetworkParam param){
		return getFeatureBox(fs, null, param);
	}
	
	/**
	 * Use the map to cache the feature index array to save the memory.
	 * @param fs
	 * @param fv
	 * @param param
	 * @return
	 */
	public static FeatureBox getFeatureBox(int[] fs, double[] fv, LocalNetworkParam param){
		FeatureBox fb = new FeatureBox(fs, fv);
		if (!NetworkConfig.AVOID_DUPLICATE_FEATURES) {
			return fb;
		}
		if (param.fbMap == null) {
			param.fbMap = new HashMap<FeatureBox, FeatureBox>();
		}
		if (param.fbMap.containsKey(fb)) {
			return param.fbMap.get(fb);
		} else{
			param.fbMap.put(fb, fb);
			return fb;
		}
	}
	
	@Override
	public int hashCode() {
		int hash = 1;
		int a = Arrays.hashCode(_fs);
		hash = hash * 17 + a;
		int b = Arrays.hashCode(_fv);
		hash = hash * 17 + b;
		return hash;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof FeatureBox){
			FeatureBox other = (FeatureBox)obj;
			boolean isEqual = Arrays.equals(_fs, other._fs);
			if (this._fv != null && other._fv == null) return false;
			if (other._fv != null && this._fv == null) return false;
			if (this._fv == null && other._fv == null)
				return isEqual;
			else {
				return isEqual && Arrays.equals(_fv, other._fv); 
			}
		}
		return false;
	}
}
