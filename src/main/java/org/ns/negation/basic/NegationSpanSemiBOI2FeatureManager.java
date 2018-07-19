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
import org.ns.negation.common.NegationFeatureManager.FeaType;
import org.ns.negation.common.NegationInstance.FEATURE_TYPES;

import java.util.ArrayList;
import java.util.Arrays;

public class NegationSpanSemiBOI2FeatureManager extends NegationFeatureManager {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1777202275474581371L;

	public NegationSpanSemiBOI2FeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
	}

	public NegationSpanSemiBOI2FeatureManager(GlobalNetworkParam param_g, String neuralType, boolean moreBinaryFeatures) {
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

		Negation neg = inst.negation;

	
		

		ArrayList<Integer> featureList = new ArrayList<Integer>();
		ArrayList<Integer> continuousFeatureList = new ArrayList<Integer>();
		ArrayList<Double> continuousFeatureValueList = new ArrayList<Double>();

		FeatureArray fa = this.createFeatureArray(network, new int[] {});
		FeatureArray fp = fa;

		String tagStr = null;
		

		if (nodetype_parent == NodeType.Root.ordinal()) {
			featureList.add(this._param_g.toFeature(network, "transition_", START, NodeType.values()[nodetype_parent] + "=>" + NodeType.values()[nodetype_child]));

		} else {
			
			String ct = this.compiler._labels[tag_parent].getForm();
			
			String reverse_ct = (ct.startsWith("O")) ? "I" : "O";
			
			tagStr = ct;

			if (nodetype_child == NodeType.Node.ordinal())
				featureList.add(this._param_g.toFeature(network, "transition_", ct, NodeType.values()[nodetype_parent] + "=>" + NodeType.values()[nodetype_child]));
			else
				featureList.add(this._param_g.toFeature(network, "transition_", ct, NodeType.values()[nodetype_parent] + "=>" + END));
			
			
			/*if (ct.startsWith("I")) {
				
				if (pos_parent < size) {
					String nt = (pos_parent == size - 1) ? "<END>" :  this.compiler._labels[tag_child].getForm();
					fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_parent, ct, nt, ""));
				}
				
			} else if (ct.startsWith("O")) */
			{
				
				boolean containCC = false;
				ArrayList<String> gapFeature = new ArrayList<String>();
				gapFeature.add(" ");
				
				for(int pos = pos_parent; pos < pos_child; pos++) {
					
					String nt = (pos == pos_child - 1) ? (pos < size ? reverse_ct : "<END>") :  ct;
					fp = fp.addNext(this.getFeatureAtPos(network, inst, pos, ct, nt, ""));
					
					String word = Utils.getToken(inputs, pos, FEATURE_TYPES.word.ordinal()).toLowerCase();
					String POSTag = Utils.getToken(inputs, pos, FEATURE_TYPES.pos_tag.ordinal());
					
					//gapFeature.add(POSTag);
					if (POSTag.equals("CC")) {
						featureList.add(this._param_g.toFeature(network, ct + "_", "contains_CC", ""));
						gapFeature.add("containCC");
						containCC = true;
					}
					
				}
				
				
				
				
				//Semi Features
				
				fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_parent - 1, "", "", ct + "_begin_-1"));
				fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_parent, "", "", ct + "_begin_0"));
				fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_parent + 1, "", "",ct + "_begin_+1"));
				
				
				fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_child - 2, "", "", ct + "_end_-1"));
				fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_child - 1, "", "", ct + "_end_0"));
				fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_child, "", "", ct + "_end_+1"));
				
				
				
				if (pos_parent - 1 >= 0 && pos_child < size) {

					for (String gapF : gapFeature) {

						String featureType = "long_distance_" + gapF;

						int[] featureCandicate = new int[] { 3, 4, 5, 6 };

						if (!NegationGlobal.SYNTAX_FEATURE) {
							featureCandicate = new int[] { 3, 4, 5 };
						}

						for (int i : featureCandicate) {

							String featuretype = NegationInstance.FEATURE_TYPES.values()[i].name();
							
							featureList.add(this._param_g.toFeature(network, featureType + FeaType.tag + "", reverse_ct, "B0" + featuretype + ":" + Utils.getToken(inputs, pos_parent - 1, i)));
							featureList.add(this._param_g.toFeature(network, featureType + FeaType.tag + "", reverse_ct, "B+1" + featuretype + ":" + Utils.getToken(inputs, pos_parent, i)));
							featureList.add(this._param_g.toFeature(network, featureType + FeaType.tag + "", reverse_ct, "B-1" + featuretype + ":" + Utils.getToken(inputs, pos_parent - 2, i)));

							featureList.add(this._param_g.toFeature(network, featureType + FeaType.tag + "", reverse_ct, "A0" + featuretype + ":" + Utils.getToken(inputs, pos_child, i)));
							featureList.add(this._param_g.toFeature(network, featureType + FeaType.tag + "", reverse_ct, "A+1" + featuretype + ":" + Utils.getToken(inputs, pos_child + 1, i)));
							featureList.add(this._param_g.toFeature(network, featureType + FeaType.tag + "", reverse_ct, "A-1" + featuretype + ":" + Utils.getToken(inputs, pos_child - 1, i)));
							
							featureList.add(this._param_g.toFeature(network, featureType + FeaType.tag + "", reverse_ct, "B0-A0" + featuretype + ":" + Utils.getToken(inputs, pos_parent - 1, i) + Utils.getToken(inputs, pos_child, i)));

							featureList.add(this._param_g.toFeature(network, featureType + FeaType.tag + "", reverse_ct, "B0-A0-A1" + featuretype + ":" + Utils.getToken(inputs, pos_parent - 1, i) + Utils.getToken(inputs, pos_child, i) + Utils.getToken(inputs, pos_child + 1, i)));

							featureList.add(this._param_g.toFeature(network, featureType + FeaType.tag + "", reverse_ct, "B0-A0-A-1" + featuretype + ":" + Utils.getToken(inputs, pos_parent - 1, i) + Utils.getToken(inputs, pos_child, i) + Utils.getToken(inputs, pos_child - 1, i)));
							
							
						}
					}
				}
				
				
				
			}
		}

		if (NetworkConfig.USE_NEURAL_FEATURES) {
			if (tagStr != null) {
				Object input = null;
				if (neuralType.equals("continuous0")) { 
					
					
					
					for(int pos = pos_parent; pos < pos_child; pos++) {
						
						String output = tagStr;
						if (inst.negation.cue[pos] == 1)
							output += "N";

						if (pos < inst.negation.leftmost_cue_pos) {
							output += "B";
						} else if (pos > inst.negation.rightmost_cue_pos) {
							output += "A";
						} else {
							output += "M";
						}
						
						String word =  Utils.getToken(inputs, pos, NegationInstance.FEATURE_TYPES.word.ordinal());
					
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
							continuousFeatureList.add(this._param_g.toFeature(network, "continuous-emb:", output, "dim" + i  + ":"));
							continuousFeatureValueList.add(vec[i]);
							
						}
					}
				
				} 

			}
		} 

		fp = fp.addNext(this.createFeatureArray(network, continuousFeatureList, continuousFeatureValueList));
		fp = fp.addNext(this.createFeatureArray(network, featureList));
		
		return fa;

	}

}
