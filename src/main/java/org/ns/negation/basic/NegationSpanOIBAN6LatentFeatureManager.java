package org.ns.negation.basic;

import java.util.AbstractMap.SimpleImmutableEntry;

import org.ns.commons.types.Label;
import org.ns.hypergraph.FeatureArray;
import org.ns.hypergraph.GlobalNetworkParam;
import org.ns.hypergraph.Network;
import org.ns.hypergraph.NetworkConfig;
import org.ns.hypergraph.NetworkIDMapper;
import org.ns.negation.common.Negation;
import org.ns.negation.common.NegationFeatureManager;
import org.ns.negation.common.NegationGlobal;
import org.ns.negation.common.NegationInstance;
import org.ns.negation.common.Utils;
import org.ns.negation.common.NegationCompiler.NodeType;

import java.util.ArrayList;
import java.util.Arrays;

public class NegationSpanOIBAN6LatentFeatureManager extends NegationFeatureManager {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7680873374572037698L;

	public NegationSpanOIBAN6LatentFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
	}

	public NegationSpanOIBAN6LatentFeatureManager(GlobalNetworkParam param_g, String neuralType, boolean moreBinaryFeatures) {
		super(param_g, neuralType, moreBinaryFeatures);
	}

	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k, int children_k_index) {
		if (children_k.length > 2)
			throw new RuntimeException("The number of children should be at most 2, but it is " + children_k.length);

		NegationInstance inst = ((NegationInstance) network.getInstance());

		long node_parent = network.getNode(parent_k);

		if (children_k.length == 0)
			return FeatureArray.EMPTY;

		int size = inst.size();
		String sentence = inst.getSentence();

		long node_child = network.getNode(children_k[0]);

		int[] ids_parent = NetworkIDMapper.toHybridNodeArray(node_parent);
		int[] ids_child = NetworkIDMapper.toHybridNodeArray(node_child);

		ArrayList<String[]> inputs = (ArrayList<String[]>) inst.getInput();
		ArrayList<Label> outputs = (ArrayList<Label>) inst.getOutput();

		int pos_parent = size - ids_parent[0];
		int pos_child = size - ids_child[0];

		int tag_parent = ids_parent[1];
		int tag_child = ids_child[1];

		int nodetype_parent = ids_parent[2];
		int nodetype_child = ids_child[2];
		
		int latentId_parent = ids_parent[3];
		int latentId_child = ids_child[3];

		Negation neg = inst.negation;

	
		

		ArrayList<Integer> featureList = new ArrayList<Integer>();
		ArrayList<Integer> continuousFeatureList = new ArrayList<Integer>();
		ArrayList<Double> continuousFeatureValueList = new ArrayList<Double>();

		FeatureArray fa = createFeatureArray(network, new int[] {});
		FeatureArray fp = fa;

		int tagId = -1;
		String tagStr = null;
		String word = null;

		ArrayList<String[]> featureArr = new ArrayList<String[]>();

		if (nodetype_parent == NodeType.Root.ordinal()) {
			featureList.add(this._param_g.toFeature(network, "transition_", START, NodeType.values()[nodetype_parent] + "=>" + NodeType.values()[nodetype_child]));

		} else {
			tagId = tag_parent;
			tagStr = this.compiler._labels[tagId].getForm();

			if (nodetype_child == NodeType.Node.ordinal())
				featureList.add(this._param_g.toFeature(network, "transition_", tagStr, NodeType.values()[nodetype_parent] + "=>" + NodeType.values()[nodetype_child]));
			else
				featureList.add(this._param_g.toFeature(network, "transition_", tagStr, NodeType.values()[nodetype_parent] + "=>" + END));
			
			String w = Utils.getToken(inputs, pos_parent, NegationInstance.FEATURE_TYPES.word.ordinal());
			String lw = Utils.getToken(inputs, pos_parent - 1, NegationInstance.FEATURE_TYPES.word.ordinal());
			String llw = Utils.getToken(inputs, pos_parent - 2, NegationInstance.FEATURE_TYPES.word.ordinal());
			String rw = Utils.getToken(inputs, pos_parent + 1, NegationInstance.FEATURE_TYPES.word.ordinal());
			String rrw = Utils.getToken(inputs, pos_parent + 2, NegationInstance.FEATURE_TYPES.word.ordinal());
			
			word =  Utils.getToken(inputs, pos_parent, NegationInstance.FEATURE_TYPES.word.ordinal());
			
			String ct = tagStr;
			String nt = (nodetype_child == NodeType.Node.ordinal()) ? this.compiler._labels[tag_child].getForm() : this.END;

			if (NegationGlobal.ENABLE_DISCRETE_FEATURE) {
				
				
				
				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, w));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, w + rw));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, lw + w));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, lw + w + rw));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, llw + lw + w + rw + rrw));
				
				int[] featureCandicate = new int[]{3, 4, 5, 6};
				
				if (!NegationGlobal.SYNTAX_FEATURE) {
					featureCandicate = new int[]{3, 4, 5};
				}
				
				/*
				for(int i = NegationInstance.FEATURE_TYPES.lemma.ordinal(); i <= NegationInstance.FEATURE_TYPES.pos_tag.ordinal(); i++) {
					String featureType = NegationInstance.FEATURE_TYPES.values()[i].name();
					
					String t = Utils.getToken(inputs, pos_parent, i);
					String lt = Utils.getToken(inputs, pos_parent - 1, i);
					String llt = Utils.getToken(inputs, pos_parent - 2, i);
					String rt = Utils.getToken(inputs, pos_parent + 1, i);
					String rrt = Utils.getToken(inputs, pos_parent + 2, i);
					
					
					featureList.add(this._param_g.toFeature(network, featureType + "", tagStr, t));
					featureList.add(this._param_g.toFeature(network, featureType + "", tagStr, t + rt));
					featureList.add(this._param_g.toFeature(network, featureType + "", tagStr, lt + t));
					featureList.add(this._param_g.toFeature(network, featureType + "", tagStr, lt + t + rt));
					featureList.add(this._param_g.toFeature(network, featureType + "", tagStr, llt + lt + t + rt + rrt));
				}*/
				
				
				if (neg.cue[pos_parent] == 1) {
					String cueForm = neg.cueForm[pos_parent];
					featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, cueForm));
					featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, cueForm + rw));
					featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, lw + cueForm));
					featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, lw + cueForm + rw));
					featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, llw + lw + cueForm + rw + rrw));
				}
				
				
			
				
				//for(int i = NegationInstance.FEATURE_TYPES.word.ordinal(); i <= NegationInstance.FEATURE_TYPES.syntax.ordinal(); i++) 
				for(int i : featureCandicate)
				{
					String featureType = NegationInstance.FEATURE_TYPES.values()[i].name();
					featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "curr_" + featureType + ":" + Utils.getToken(inputs, pos_parent    , i)));
					featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "next_" + featureType + ":" + Utils.getToken(inputs, pos_parent + 1, i)));
					featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "last_" + featureType + ":" + Utils.getToken(inputs, pos_parent - 1, i)));
					
					
					
					featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "next_" + featureType + ":" + Utils.getToken(inputs, pos_parent    , i) + Utils.getToken(inputs, pos_parent + 1, i)));
					featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "last_" + featureType + ":" + Utils.getToken(inputs, pos_parent - 1, i) + Utils.getToken(inputs, pos_parent + 0, i)));
					
					
				
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

				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, "IsAllCap:" + Utils.isAllCap(w)));
				//featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, "IsAllCap:" + Utils.isAllCap(w) + w));

				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, "IsPunc:" + Utils.isPunctuation(w)));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, "IsPunc:" + Utils.isPunctuation(w) + w));
				
				featureList.add(this._param_g.toFeature(network, FeaType.word + "before", tagStr, "IsPunc:" + Utils.isPunctuation(lw)));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "after", tagStr, "IsPunc:" + Utils.isPunctuation(rw)));
				
				
				
				
				//for(int i = NegationInstance.FEATURE_TYPES.word.ordinal(); i <= NegationInstance.FEATURE_TYPES.syntax.ordinal(); i++)
				for(int i : featureCandicate)
				{
					String featureType = NegationInstance.FEATURE_TYPES.values()[i].name();
					
					featureList.add(this._param_g.toFeature(network, featureType + "before", tagStr, Utils.getToken(inputs, pos_parent - 1, i)));
					featureList.add(this._param_g.toFeature(network, featureType + "after", tagStr, Utils.getToken(inputs, pos_parent + 1, i)));
					
					featureList.add(this._param_g.toFeature(network, featureType + "", tagStr, Utils.getToken(inputs, pos_parent, i)));
				}
				


				if (neg.cue[pos_parent] == 1) {
					featureList.add(this._param_g.toFeature(network, FeaType.word + ":cueForm", tagStr, neg.cueForm[pos_parent]));
					featureList.add(this._param_g.toFeature(network, FeaType.word + ":cueWord", tagStr, w));
					
					
					//for(int i = NegationInstance.FEATURE_TYPES.word.ordinal(); i <= NegationInstance.FEATURE_TYPES.syntax.ordinal(); i++)
					for(int i : featureCandicate)
					{
						String featureType = NegationInstance.FEATURE_TYPES.values()[i].name();
						featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "curr_cue_" + featureType + ":" + Utils.getToken(inputs, pos_parent    , i)));
						
						
						featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "before_cue_" + featureType + ":" + Utils.getToken(inputs, pos_parent + 1, i)));
						featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "after_cue_" + featureType + ":" + Utils.getToken(inputs, pos_parent - 1, i)));
						
					
						
						featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "before_cue_" + featureType + ":" + Utils.getToken(inputs, pos_parent    , i) + Utils.getToken(inputs, pos_parent + 1, i)));
						featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "after_cue_" + featureType + ":" + Utils.getToken(inputs, pos_parent - 1, i) + Utils.getToken(inputs, pos_parent    , i)));
					
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
			
			
			fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_parent, ct + "-" + latentId_parent, nt + "-" + latentId_child, "latent"));
			
			

		}
		
		
		if (NetworkConfig.USE_NEURAL_FEATURES) {
			if (tagId != -1) {
				Object input = null;
				if (neuralType.startsWith("lstm")) {
					String sentenceInput = sentence;
					input = new SimpleImmutableEntry<String, Integer>(sentenceInput, pos_parent);

					this.addNeural(network, 0, parent_k, children_k_index, input, tagId);
				} else if (neuralType.equals("continuous0")) { 
					
					
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
						continuousFeatureList.add(this._param_g.toFeature(network, "continuous-emb:", tagStr + "-" + latentId_parent, "dim" + i  + ":"));
						continuousFeatureValueList.add(vec[i]);
						
					}
				
				} else {
					input = word;//.toLowerCase();
					
					if (tagId != -1) {
					
						if (NegationGlobal.EMBEDDING_WORD_LOWERCASE)
							input = word.toLowerCase();
						this.addNeural(network, 0, parent_k, children_k_index, input, tagId);
					}
				}

			}
		} else {
			// featureList.add(this._param_g.toFeature(network,
			// FeaType.word.name(), entity, word));
		}

		//FeatureArray contFa = this.createFeatureArray(network, continuousFeatureList, continuousFeatureValueList);
		fp = fp.addNext(this.createFeatureArray(network, continuousFeatureList, continuousFeatureValueList));
		fp = fp.addNext(this.createFeatureArray(network, featureList));
		
		//fa = this.createFeatureArray(network, featureList, contFa);
		return fa;

	}

}
