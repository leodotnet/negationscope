package org.ns.negation.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.AbstractMap.SimpleImmutableEntry;

import org.ns.commons.types.Label;
import org.ns.hypergraph.FeatureArray;
import org.ns.hypergraph.FeatureManager;
import org.ns.hypergraph.GlobalNetworkParam;
import org.ns.hypergraph.Network;
import org.ns.hypergraph.NetworkConfig;
import org.ns.hypergraph.NetworkIDMapper;
import org.ns.hypergraph.neural.MLP;
import org.ns.hypergraph.neural.MultiLayerPerceptron;
import org.ns.hypergraph.neural.NeuralNetworkCore;
import org.ns.negation.common.NegationCompiler.*;
import org.ns.negation.common.NegationFeatureManager.FeaType;


public abstract class NegationFeatureManager extends FeatureManager {

	/**
	 * 
	 */
	private static final long serialVersionUID = 592253662868854534L;
	protected NeuralNetworkCore net;
	public String neuralType;
	public boolean moreBinaryFeatures = false;

	public enum FeaType {
		word, tag, lw, lt, ltt, rw, rt, prefix, suffix, transition, MP
	};

	protected String OUT_SEP = MLP.OUT_SEP;
	protected String IN_SEP = MLP.IN_SEP;
	protected final String START = "STR";
	protected final String END = "END";

	public NegationFeatureManager(GlobalNetworkParam param_g) {
		super(param_g, null);
		if (NetworkConfig.USE_NEURAL_FEATURES) {
			this.net = (NeuralNetworkCore) param_g.getNNParamG().getNet(0);
		}
	}

	public NegationFeatureManager(GlobalNetworkParam param_g, String neuralType, boolean moreBinaryFeatures) {
		super(param_g);
		if (NetworkConfig.USE_NEURAL_FEATURES && !NegationGlobal.neuralType.equals("continuous0")) {
			this.net = (NeuralNetworkCore) param_g.getNNParamG().getNet(0);
		}
		this.neuralType = neuralType;
		this.moreBinaryFeatures = moreBinaryFeatures;
	}


	int NodeTypeSize = NodeType.values().length;
	int WordFeatureTypeSize = NegationInstance.FEATURE_TYPES.values().length;

	protected NegationCompiler compiler = null;
	
	public void setCompiler(NegationCompiler compiler) {
		this.compiler = compiler;
	}
	
	public FeatureArray getFeatureAtPos(Network network, NegationInstance inst, int pos_parent, String ct, String nt) {
		
		return getFeatureAtPos(network, inst, pos_parent, ct, nt, "");
	}
	
	public FeatureArray getFeatureAtPos(Network network, NegationInstance inst, int pos_parent, String ct, String nt, String featureType) {
		
		if (pos_parent < 0 || pos_parent >= inst.size())
			return this.createFeatureArray(network, new int[] {});
		
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		ArrayList<Integer> continuousFeatureList = new ArrayList<Integer>();
		ArrayList<Double> continuousFeatureValueList = new ArrayList<Double>();
		
		ArrayList<String[]> inputs = (ArrayList<String[]>) inst.getInput();
		
		String w = Utils.getToken(inputs, pos_parent, NegationInstance.FEATURE_TYPES.word.ordinal());
		String lw = Utils.getToken(inputs, pos_parent - 1, NegationInstance.FEATURE_TYPES.word.ordinal());
		String llw = Utils.getToken(inputs, pos_parent - 2, NegationInstance.FEATURE_TYPES.word.ordinal());
		String rw = Utils.getToken(inputs, pos_parent + 1, NegationInstance.FEATURE_TYPES.word.ordinal());
		String rrw = Utils.getToken(inputs, pos_parent + 2, NegationInstance.FEATURE_TYPES.word.ordinal());
		
		String word =  Utils.getToken(inputs, pos_parent, NegationInstance.FEATURE_TYPES.word.ordinal());
		
		
		
		if (inst.negation.cue[pos_parent] == 1)
			ct += "N";
		
		if (pos_parent + 1 < inst.size() && inst.negation.cue[pos_parent + 1] == 1)
			nt += "N";
		
		
		if (pos_parent < inst.negation.leftmost_cue_pos) {
			ct += "B";
		} else if (pos_parent > inst.negation.rightmost_cue_pos) {
			ct += "A";
		} else {
			ct += "M";
		}
		
		if (pos_parent + 1 < inst.negation.leftmost_cue_pos) {
			nt += "B";
		} else if (pos_parent  + 1 > inst.negation.rightmost_cue_pos) {
			nt += "A";
		} else {
			nt += "M";
		}
		

		String tagStr = ct;
		
		if (NegationGlobal.ENABLE_DISCRETE_FEATURE) {
			
			
			
			
			
			
			
			featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + "", tagStr, w));
			featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + "", tagStr, w + rw));
			featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + "", tagStr, lw + w));
			featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + "", tagStr, lw + w + rw));
			featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + "", tagStr, llw + lw + w + rw + rrw));
			
			int[] featureCandicate = new int[]{3, 4, 5, 6};
			
			if (!NegationGlobal.SYNTAX_FEATURE) {
				featureCandicate = new int[]{3, 4, 5};
			}
			
			
			
			
			if (inst.negation.cue[pos_parent] == 1) {
				String cueForm = inst.negation.cueForm[pos_parent];
				featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + "", tagStr, cueForm));
				featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + "", tagStr, cueForm + rw));
				featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + "", tagStr, lw + cueForm));
				featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + "", tagStr, lw + cueForm + rw));
				featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + "", tagStr, llw + lw + cueForm + rw + rrw));
			}
			
			
		
			
			//for(int i = NegationInstance.FEATURE_TYPES.word.ordinal(); i <= NegationInstance.FEATURE_TYPES.syntax.ordinal(); i++) 
			for(int i : featureCandicate)
			{
				String featuretype = NegationInstance.FEATURE_TYPES.values()[i].name();
				featureList.add(this._param_g.toFeature(network, featureType + FeaType.tag + "", ct + "=>" + nt, "curr_" + featuretype + ":" + Utils.getToken(inputs, pos_parent    , i)));
				featureList.add(this._param_g.toFeature(network, featureType + FeaType.tag + "", ct + "=>" + nt, "next_" + featuretype + ":" + Utils.getToken(inputs, pos_parent + 1, i)));
				featureList.add(this._param_g.toFeature(network, featureType + FeaType.tag + "", ct + "=>" + nt, "last_" + featuretype + ":" + Utils.getToken(inputs, pos_parent - 1, i)));
				
				
				
				featureList.add(this._param_g.toFeature(network, featureType + FeaType.tag + "", ct + "=>" + nt, "next_" + featuretype + ":" + Utils.getToken(inputs, pos_parent    , i) + Utils.getToken(inputs, pos_parent + 1, i)));
				featureList.add(this._param_g.toFeature(network, featureType + FeaType.tag + "", ct + "=>" + nt, "last_" + featuretype + ":" + Utils.getToken(inputs, pos_parent - 1, i) + Utils.getToken(inputs, pos_parent + 0, i)));
				
				
			
			}
			
			/*
			featureList.add(this._param_g.toFeature(network, FeaType.tag + "", t + "=>" + nt, "curr_word:" + w));
			featureList.add(this._param_g.toFeature(network, FeaType.tag + "", t + "=>" + nt, "next_word:" +rw));
			
			featureList.add(this._param_g.toFeature(network, FeaType.tag + "", t + "=>" + nt, "curr_lemma:" + Utils.getToken(inputs, pos_parent, NegationInstance.FEATURE_TYPES.lemma.ordinal())));
			featureList.add(this._param_g.toFeature(network, FeaType.tag + "", t + "=>" + nt, "next_lemma:" + Utils.getToken(inputs, pos_parent + 1, NegationInstance.FEATURE_TYPES.lemma.ordinal())));
			
			featureList.add(this._param_g.toFeature(network, FeaType.tag + "", t + "=>" + nt, "curr_pos_tag:" + Utils.getToken(inputs, pos_parent, NegationInstance.FEATURE_TYPES.lemma.ordinal())));
			featureList.add(this._param_g.toFeature(network, FeaType.tag + "", t + "=>" + nt, "next_pos_tag:" + Utils.getToken(inputs, pos_parent + 1, NegationInstance.FEATURE_TYPES.lemma.ordinal())));
			*/

			//featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, "Is1stCap:" + Utils.isFirstCap(w)));
			//featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, "Is1stCap:" + Utils.isFirstCap(w) + w));

			featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + "", tagStr, "IsAllCap:" + Utils.isAllCap(w)));
			//featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, "IsAllCap:" + Utils.isAllCap(w) + w));

			featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + "", tagStr, "IsPunc:" + Utils.isPunctuation(w)));
			featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + "", tagStr, "IsPunc:" + Utils.isPunctuation(w) + w));
			
			featureList.add(this._param_g.toFeature(network,featureType +  FeaType.word + "before", tagStr, "IsPunc:" + Utils.isPunctuation(lw)));
			featureList.add(this._param_g.toFeature(network,featureType +  FeaType.word + "after", tagStr, "IsPunc:" + Utils.isPunctuation(rw)));
			
			
			
			
			//for(int i = NegationInstance.FEATURE_TYPES.word.ordinal(); i <= NegationInstance.FEATURE_TYPES.syntax.ordinal(); i++)
			for(int i : featureCandicate)
			{
				String featuretype = NegationInstance.FEATURE_TYPES.values()[i].name();
				
				featureList.add(this._param_g.toFeature(network, featureType + featuretype + "before", tagStr, Utils.getToken(inputs, pos_parent - 1, i)));
				featureList.add(this._param_g.toFeature(network, featureType + featuretype + "after", tagStr, Utils.getToken(inputs, pos_parent + 1, i)));
				
				featureList.add(this._param_g.toFeature(network, featureType + featuretype + "", tagStr, Utils.getToken(inputs, pos_parent, i)));
			}
			


			if (inst.negation.cue[pos_parent] == 1) {
				featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + ":cueForm", tagStr, inst.negation.cueForm[pos_parent]));
				featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + ":cueWord", tagStr, w));
				
				
				//for(int i = NegationInstance.FEATURE_TYPES.word.ordinal(); i <= NegationInstance.FEATURE_TYPES.syntax.ordinal(); i++)
				for(int i : featureCandicate)
				{
					String featuretype = NegationInstance.FEATURE_TYPES.values()[i].name();
					featureList.add(this._param_g.toFeature(network, featureType + FeaType.tag + "", ct + "=>" + nt, "curr_cue_" + featuretype + ":" + Utils.getToken(inputs, pos_parent    , i)));
					
					
					featureList.add(this._param_g.toFeature(network, featureType + FeaType.tag + "", ct + "=>" + nt, "before_cue_" + featuretype + ":" + Utils.getToken(inputs, pos_parent + 1, i)));
					featureList.add(this._param_g.toFeature(network, featureType + FeaType.tag + "", ct + "=>" + nt, "after_cue_" + featuretype + ":" + Utils.getToken(inputs, pos_parent - 1, i)));
					
				
					
					featureList.add(this._param_g.toFeature(network, featureType + FeaType.tag + "", ct + "=>" + nt, "before_cue_" + featuretype + ":" + Utils.getToken(inputs, pos_parent    , i) + Utils.getToken(inputs, pos_parent + 1, i)));
					featureList.add(this._param_g.toFeature(network,featureType +  FeaType.tag + "", ct + "=>" + nt, "after_cue_" + featuretype + ":" + Utils.getToken(inputs, pos_parent - 1, i) + Utils.getToken(inputs, pos_parent    , i)));
				
					/*
					featureList.add(this._param_g.toFeature(network, FeaType.tag + "", t + "=>" + nt, "near_cue_" + featureType + ":" + Utils.getToken(inputs, pos_parent + 1, i)));
					featureList.add(this._param_g.toFeature(network, FeaType.tag + "", t + "=>" + nt, "near_cue_" + featureType + ":" + Utils.getToken(inputs, pos_parent - 1, i)));
					
					featureList.add(this._param_g.toFeature(network, FeaType.tag + "", t + "=>" + nt, "near_cue_" + featureType + ":" + Utils.getToken(inputs, pos_parent    , i) + Utils.getToken(inputs, pos_parent + 1, i)));
					featureList.add(this._param_g.toFeature(network, FeaType.tag + "", t + "=>" + nt, "near_cue_" + featureType + ":" + Utils.getToken(inputs, pos_parent - 1, i) + Utils.getToken(inputs, pos_parent    , i)));
					 */
				}

				/*
				featureList.add(this._param_g.toFeature(network, FeaType.word + "before_cue", tagStr, lw));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "after_cue", tagStr, rw));
				
				featureList.add(this._param_g.toFeature(network, FEATURE_TYPES.lemma + "before_cue", tagStr, Utils.getToken(inputs, pos_parent - 1, NegationInstance.FEATURE_TYPES.lemma.ordinal())));
				featureList.add(this._param_g.toFeature(network, FEATURE_TYPES.lemma + "after_cue", tagStr, Utils.getToken(inputs, pos_parent + 1, NegationInstance.FEATURE_TYPES.lemma.ordinal())));
				
				featureList.add(this._param_g.toFeature(network, FEATURE_TYPES.pos_tag + "before_cue", tagStr, Utils.getToken(inputs, pos_parent - 1, NegationInstance.FEATURE_TYPES.pos_tag.ordinal())));
				featureList.add(this._param_g.toFeature(network, FEATURE_TYPES.pos_tag + "after_cue", tagStr, Utils.getToken(inputs, pos_parent + 1, NegationInstance.FEATURE_TYPES.pos_tag.ordinal())));
				
				featureList.add(this._param_g.toFeature(network, FEATURE_TYPES.syntax + "before_cue", tagStr, Utils.getToken(inputs, pos_parent - 1, NegationInstance.FEATURE_TYPES.syntax.ordinal())));
				featureList.add(this._param_g.toFeature(network, FEATURE_TYPES.syntax + "after_cue", tagStr, Utils.getToken(inputs, pos_parent + 1, NegationInstance.FEATURE_TYPES.syntax.ordinal())));
				
				
				featureList.add(this._param_g.toFeature(network, FeaType.word + "before_cue", tagStr, lw + w));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "after_cue", tagStr, w + rw));*/
			}
		}
		
		/*
		if (NetworkConfig.USE_NEURAL_FEATURES) {
			if (!ct.equals("")) {
				Object input = null;
				if (neuralType.equals("continuous0")) { 
					
					
					if (NegationGlobal.EMBEDDING_WORD_LOWERCASE)
						word = word.toLowerCase();
					
					double[] vec = NegationGlobal.Word2Vec.getVector(word);
					if (vec == null) {
						vec = NegationGlobal.Word2Vec.getVector(NegationGlobal.UNK);
						
						if (vec == null) {
							vec = new double[NegationGlobal.Word2Vec.ShapeSize];
							Arrays.fill(vec, 0);
						}
					}
					
					for(int i = 0; i < NegationGlobal.Word2Vec.ShapeSize; i++) {
						continuousFeatureList.add(this._param_g.toFeature(network, featureType + "continuous-emb:", ct, "dim" + i  + ":"));
						continuousFeatureValueList.add(vec[i]);
						
					}
				
				} 

			}
		} else {
			// featureList.add(this._param_g.toFeature(network,
			// FeaType.word.name(), entity, word));
		}*/
		
		
		FeatureArray contFa = this.createFeatureArray(network, continuousFeatureList, continuousFeatureValueList);
		FeatureArray fa = this.createFeatureArray(network, featureList, contFa); 
		
		return fa;

	}

}
