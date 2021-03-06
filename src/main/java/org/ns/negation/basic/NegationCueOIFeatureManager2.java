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
import org.ns.negation.common.NegationInstance.FEATURE_TYPES;

import java.util.ArrayList;
import java.util.Arrays;

public class NegationCueOIFeatureManager2 extends NegationFeatureManager {

	public NegationCueOIFeatureManager2(GlobalNetworkParam param_g) {
		super(param_g);
	}

	public NegationCueOIFeatureManager2(GlobalNetworkParam param_g, String neuralType, boolean moreBinaryFeatures) {
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

		FeatureArray fa = FeatureArray.EMPTY;

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

			word = Utils.getToken(inputs, pos_parent, NegationInstance.FEATURE_TYPES.word.ordinal());

			String ct = tagStr;
			String nt = (nodetype_child == NodeType.Node.ordinal()) ? this.compiler._labels[tag_child].getForm() : this.END;

			if (NegationGlobal.ENABLE_DISCRETE_FEATURE) {

				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, w));
				
				 featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, w + rw));
				 featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, lw + w));
				 featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, lw + w + rw));
				 featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, llw + lw + w + rw + rrw));
				 
				String target = w;

				String token = word.toLowerCase();
				String lemma = Utils.getToken(inputs, pos_parent, FEATURE_TYPES.lemma.ordinal());
				String postag = Utils.getToken(inputs, pos_parent, FEATURE_TYPES.pos_tag.ordinal());
				String postag_cat = (postag.length() >= 3) ? postag.substring(0, 3) : postag;

				featureList.add(this._param_g.toFeature(network, FeaType.word + "_tokenL", tagStr, token));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "_lemma-1:", tagStr, Utils.getToken(inputs, pos_parent - 1, FEATURE_TYPES.lemma.ordinal())));
				//featureList.add(this._param_g.toFeature(network, FeaType.word + "_lemma+1:", tagStr, Utils.getToken(inputs, pos_parent + 1, FEATURE_TYPES.lemma.ordinal())));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "_lemma:", tagStr, lemma));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "_postag:", tagStr, postag));
				//featureList.add(this._param_g.toFeature(network, FeaType.word + "_postag+1:", tagStr, Utils.getToken(inputs, pos_parent - 1, FEATURE_TYPES.pos_tag.ordinal())));
				//featureList.add(this._param_g.toFeature(network, FeaType.word + "_postag-1:", tagStr, Utils.getToken(inputs, pos_parent - 1, FEATURE_TYPES.pos_tag.ordinal())));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "_postag_cat:", tagStr, postag_cat));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "IsPunc:", tagStr, "" + Utils.isPunctuation(token)));

				if (tagStr.startsWith("I")) {

					//String cueForm = inst.negation.cueForm[pos_parent].toLowerCase();

					String phrase2 = Utils.getPhrase(inputs, pos_parent, pos_parent + 1, FEATURE_TYPES.word.ordinal(), true);
					String phrase3 = Utils.getPhrase(inputs, pos_parent, pos_parent + 2, FEATURE_TYPES.word.ordinal(), true);
					
						if (token.startsWith("un") || token.startsWith("im") || token.startsWith("in") || token.startsWith("ir")) {
							featureList.add(this._param_g.toFeature(network, FeaType.word + "_prefix:", tagStr, ""));
							featureList.add(this._param_g.toFeature(network, FeaType.word + "_prefix:", tagStr, token.substring(0, 2)));
						} else if (token.startsWith("dis") || token.startsWith("non")) {
							featureList.add(this._param_g.toFeature(network, FeaType.word + "_prefix:", tagStr, ""));
							featureList.add(this._param_g.toFeature(network, FeaType.word + "_prefix:", tagStr, token.substring(0, 3)));
						} else if (token.endsWith("less") || (token.endsWith("lessly")) || (token.endsWith("lessness"))) {
							featureList.add(this._param_g.toFeature(network, FeaType.word + "_suffix:", tagStr, ""));
							featureList.add(this._param_g.toFeature(network, FeaType.word + "_prefix:", tagStr, "less"));
						} else if (NegationGlobal.NegExpList.contains(phrase2)) {
							featureList.add(this._param_g.toFeature(network, FeaType.word + "_negexpression:", tagStr, ""));
							featureList.add(this._param_g.toFeature(network, FeaType.word + "_negexpression:", tagStr, phrase2));
						}  else if (NegationGlobal.NegExpList.contains(phrase3)) {
							featureList.add(this._param_g.toFeature(network, FeaType.word + "_negexpression:", tagStr, ""));
							//featureList.add(this._param_g.toFeature(network, FeaType.word + "_negexpression:", tagStr, phrase3));
						} else if (NegationGlobal.NegExpList.contains(token)) {
							featureList.add(this._param_g.toFeature(network, FeaType.word + "_negexpression:", tagStr, ""));
							featureList.add(this._param_g.toFeature(network, FeaType.word + "_negexpression:", tagStr, token));
						} else {
							featureList.add(this._param_g.toFeature(network, FeaType.word + "_oneword:", tagStr, ""));
							//featureList.add(this._param_g.toFeature(network, FeaType.word + "_oneword:", tagStr, token));
						}
						
		
					

				} else {

				}

				
				
				
				

				/*
				if (tagStr.startsWith("I")) {

					if (w.startsWith("un") || w.startsWith("im") || w.startsWith("in") || w.startsWith("ir")) {

						String prefix = w.substring(0, 2);
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_prefix_", tagStr, prefix));
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_prefix_", tagStr, ""));
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_affix_", tagStr, ""));
						target = prefix;
						

					} else if (w.startsWith("dis") || w.startsWith("non")) {
						String prefix = w.substring(0, 3);
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_prefix_", tagStr, prefix));
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_prefix_", tagStr, ""));
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_affix_", tagStr, ""));
						target = prefix;
						
					} else if (w.endsWith("less") || (w.endsWith("lessly")) || (w.endsWith("lessness"))) {

						String suffix = "less";
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_suffix_", tagStr, suffix));
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_suffix_", tagStr, ""));
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_affix_", tagStr, ""));
						target = suffix;
						
					} else if (w.equals("neither") || w.equals("or") || w.equals("nobody")) {
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_word_", tagStr, w));
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_word_", tagStr, ""));
						target = w;
					} else {

						featureList.add(this._param_g.toFeature(network, FeaType.word + "_word_", tagStr, w));
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_word_", tagStr, ""));
						target = w;
						
					}
					target = w;

					
				}

				int[] featureCandicate = new int[] { 4, 5, 6 };

				if (!NegationGlobal.SYNTAX_FEATURE) {
					featureCandicate = new int[] { 4, 5 };
				}

				for (int i : featureCandicate) {
					String featureType = NegationInstance.FEATURE_TYPES.values()[i].name();

					featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct, "curr_" + featureType + ":" + target + Utils.getToken(inputs, pos_parent, i)));
					if (i == 5) {
						featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct, "curr_" + featureType + ":" + target + Utils.getToken(inputs, pos_parent, i).substring(0, 1)));
					}

					featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "curr_" + featureType + ":" + Utils.getToken(inputs, pos_parent, i)));
					
				}*/


			}

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

					for (int i = 0; i < NegationGlobal.Word2Vec.ShapeSize; i++) {
						continuousFeatureList.add(this._param_g.toFeature(network, "continuous-emb:", tagStr, "dim" + i + ":"));
						continuousFeatureValueList.add(vec[i]);

					}

				} else {
					input = word;// .toLowerCase();

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

		FeatureArray contFa = this.createFeatureArray(network, continuousFeatureList, continuousFeatureValueList);
		fa = this.createFeatureArray(network, featureList, contFa);
		return fa;

	}

}
