package org.ns.negation.basic;

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
import org.ns.negation.basic.NegationScopeJointCompiler.*;
import org.ns.negation.common.Negation;
import org.ns.negation.common.NegationCompiler;
import org.ns.negation.common.NegationFeatureManager;
import org.ns.negation.common.NegationGlobal;
import org.ns.negation.common.NegationInstance;
import org.ns.negation.common.Utils;
import org.ns.negation.common.NegationFeatureManager.FeaType;
import org.ns.negation.common.NegationInstance.FEATURE_TYPES;

public class NegationScopeJointFeatureManager extends NegationFeatureManager {

	public NegationScopeJointCompiler compiler = null;

	public void setCompiler(NegationCompiler compiler) {
		super.compiler = compiler;
		this.compiler = (NegationScopeJointCompiler) compiler;
	}

	public NegationScopeJointFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
	}

	public NegationScopeJointFeatureManager(GlobalNetworkParam param_g, String neuralType, boolean moreBinaryFeatures) {
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

		int pos_parent = size - ids_parent[1];
		int pos_child = size - ids_child[1];

		int tag_parent = ids_parent[4];
		int tag_child = ids_child[4];

		int nodetype_parent = ids_parent[0];
		int nodetype_child = ids_child[0];

		int cueleftboundary_parent = size - ids_parent[2];
		int cueleftboundary_child = size - ids_child[2];

		int cuerightboundary_parent = size - ids_parent[3];
		int cuerightboundary_child = size - ids_child[3];

		//Negation neg = inst.negation;

		ArrayList<Integer> featureList = new ArrayList<Integer>();
		ArrayList<Integer> continuousFeatureList = new ArrayList<Integer>();
		ArrayList<Double> continuousFeatureValueList = new ArrayList<Double>();

		FeatureArray fa = FeatureArray.EMPTY;

		int tagId = -1;
		String tagStr = null;
		String word = null;

		ArrayList<String[]> featureArr = new ArrayList<String[]>();

		if (nodetype_parent == NodeType.Root.ordinal()) {

			if (nodetype_child == NodeType.CueNode.ordinal()) {
				String tagStrNext = this.compiler._labels[tag_child].getForm();
				featureList.add(this._param_g.toFeature(network, "transition_", "", START + "=>" + NodeType.values()[nodetype_child] + tagStrNext));
			} else if (nodetype_child == NodeType.ScopeNode.ordinal()) {
				String tagStrNext = this.compiler._labels[tag_child + this.compiler.CueTagSize].getForm();
				featureList.add(this._param_g.toFeature(network, "transition_", "", START + "=>" + NodeType.values()[nodetype_child] + tagStrNext));
			}

		} else if (nodetype_parent == NodeType.CueNode.ordinal()) {
			tagId = tag_parent;
			tagStr = this.compiler._labels[tagId].getForm();

			if (nodetype_child == NodeType.CueNode.ordinal()) {
				String tagStrNext = this.compiler._labels[tag_child].getForm();
				featureList.add(this._param_g.toFeature(network, "transition_", tagStr, NodeType.values()[nodetype_parent] + tagStr + "=>" + NodeType.values()[nodetype_child] + tagStrNext));
			} else if (nodetype_child == NodeType.ScopeNode.ordinal()) {
				String tagStrNext = this.compiler._labels[tag_child + this.compiler.CueTagSize].getForm();
				featureList.add(this._param_g.toFeature(network, "transition_", tagStr, NodeType.values()[nodetype_parent] + tagStr + "=>" + NodeType.values()[nodetype_child] + tagStrNext));
			} else {
				featureList.add(this._param_g.toFeature(network, "transition_", tagStr, NodeType.values()[nodetype_parent] + tagStr + "=>" + END));
			}

			// for(int pos = pos_parent; pos < pos_child; pos++)
			int pos = pos_parent;
			
			
			
			{
				String cueForm = Utils.getPhrase(inputs, pos_parent, pos_child - 1, NegationInstance.FEATURE_TYPES.word.ordinal(), true);

				String w = Utils.getToken(inputs, pos_parent, NegationInstance.FEATURE_TYPES.word.ordinal());
				String last_w = Utils.getToken(inputs, pos_child - 1, NegationInstance.FEATURE_TYPES.word.ordinal());

				String lw = Utils.getToken(inputs, pos_parent - 1, NegationInstance.FEATURE_TYPES.word.ordinal());
				String llw = Utils.getToken(inputs, pos_parent - 2, NegationInstance.FEATURE_TYPES.word.ordinal());
				String rw = Utils.getToken(inputs, pos_child, NegationInstance.FEATURE_TYPES.word.ordinal());
				String rrw = Utils.getToken(inputs, pos_child + 1, NegationInstance.FEATURE_TYPES.word.ordinal());

				String ct = tagStr;
				String nt = (nodetype_child == NodeType.CueNode.ordinal()) ? this.compiler._labels[tag_child].getForm() : this.END;

				if (NegationGlobal.ENABLE_DISCRETE_FEATURE) {


					if (tagStr.startsWith("B")) 
					{
						featureList.add(this._param_g.toFeature(network, FeaType.MP + "Cue", "", ""));
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_firsttoken", tagStr, w));
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_lasttoken", tagStr, last_w));
					} else {
						
						if (NegationGlobal.ENABLE_OCUE_CHAIN) {
							String OTagStr = "O-Scope";
							if (NegationGlobal.SCOPE_CHAIN_U_APPROACH)
								OTagStr = "U-Scope";

							for (int j = 0; j < size; j++) {

								String wj = Utils.getToken(inputs, j, NegationInstance.FEATURE_TYPES.word.ordinal());
								String lwj = Utils.getToken(inputs, j - 1, NegationInstance.FEATURE_TYPES.word.ordinal());
								String llwj = Utils.getToken(inputs, j - 2, NegationInstance.FEATURE_TYPES.word.ordinal());
								String rwj = Utils.getToken(inputs, j + 1, NegationInstance.FEATURE_TYPES.word.ordinal());
								String rrwj = Utils.getToken(inputs, j + 2, NegationInstance.FEATURE_TYPES.word.ordinal());

								featureList.add(this._param_g.toFeature(network, FeaType.word + "", OTagStr, wj));
								featureList.add(this._param_g.toFeature(network, FeaType.word + "", OTagStr, wj + rwj));
								featureList.add(this._param_g.toFeature(network, FeaType.word + "", OTagStr, lwj + wj));
								featureList.add(this._param_g.toFeature(network, FeaType.word + "", OTagStr, lwj + wj + rwj));
								featureList.add(this._param_g.toFeature(network, FeaType.word + "", OTagStr, llwj + lwj + wj + rwj + rrwj));

								int[] featureCandicate = new int[] { 3, 4, 5, 6 };

								if (!NegationGlobal.SYNTAX_FEATURE) {
									featureCandicate = new int[] { 3, 4, 5 };
								}

								for (int i : featureCandicate) {
									String featureType = NegationInstance.FEATURE_TYPES.values()[i].name();
									featureList.add(this._param_g.toFeature(network, FeaType.tag + "", OTagStr + "=>" + OTagStr, "curr_" + featureType + ":" + Utils.getToken(inputs, j, i)));
									featureList.add(this._param_g.toFeature(network, FeaType.tag + "", OTagStr + "=>" + OTagStr, "next_" + featureType + ":" + Utils.getToken(inputs, j + 1, i)));
									featureList.add(this._param_g.toFeature(network, FeaType.tag + "", OTagStr + "=>" + OTagStr, "last_" + featureType + ":" + Utils.getToken(inputs, j - 1, i)));

									featureList.add(this._param_g.toFeature(network, FeaType.tag + "", OTagStr + "=>" + OTagStr, "next_" + featureType + ":" + Utils.getToken(inputs, j, i) + Utils.getToken(inputs, j + 1, i)));
									featureList.add(this._param_g.toFeature(network, FeaType.tag + "", OTagStr + "=>" + OTagStr, "last_" + featureType + ":" + Utils.getToken(inputs, j - 1, i) + Utils.getToken(inputs, j + 0, i)));

								}

								featureList.add(this._param_g.toFeature(network, FeaType.word + "", OTagStr, "IsAllCap:" + Utils.isAllCap(wj)));

								featureList.add(this._param_g.toFeature(network, FeaType.word + "", OTagStr, "IsPunc:" + Utils.isPunctuation(wj)));
								featureList.add(this._param_g.toFeature(network, FeaType.word + "", OTagStr, "IsPunc:" + Utils.isPunctuation(wj) + wj));

								featureList.add(this._param_g.toFeature(network, FeaType.word + "before", OTagStr, "IsPunc:" + Utils.isPunctuation(lwj)));
								featureList.add(this._param_g.toFeature(network, FeaType.word + "after", OTagStr, "IsPunc:" + Utils.isPunctuation(rwj)));

								for (int i : featureCandicate) {
									String featureType = NegationInstance.FEATURE_TYPES.values()[i].name();

									featureList.add(this._param_g.toFeature(network, featureType + "before", OTagStr, Utils.getToken(inputs, j - 1, i)));
									featureList.add(this._param_g.toFeature(network, featureType + "after", OTagStr, Utils.getToken(inputs, j + 1, i)));

									featureList.add(this._param_g.toFeature(network, featureType + "", OTagStr, Utils.getToken(inputs, j, i)));
								}
							}

						}
						
					}
					
					featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, cueForm));
					featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, cueForm + rw));
					featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, lw + cueForm));
					featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, lw + cueForm + rw));
					featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, llw + lw + cueForm + rw + rrw));

					
					//for (int i = pos_parent; i < pos_child; i++) 
					int i = pos_parent;	
					{
						int offset = i - pos_parent;
						word = Utils.getToken(inputs, i, NegationInstance.FEATURE_TYPES.word.ordinal());
						String token = word.toLowerCase();
						String lemma = Utils.getToken(inputs, i, FEATURE_TYPES.lemma.ordinal());
						String postag = Utils.getToken(inputs, i, FEATURE_TYPES.pos_tag.ordinal());
						String postag_cat = (postag.length() >= 3) ? postag.substring(0, 3) : postag;

						featureList.add(this._param_g.toFeature(network, FeaType.word + "_tokenL" + offset, tagStr, token));
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_lemma-1:"+ offset, tagStr, Utils.getToken(inputs, i - 1, FEATURE_TYPES.lemma.ordinal())));
	
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_lemma:"+ offset, tagStr, lemma));
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_postag:"+ offset, tagStr, postag));
						
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_postag_cat:"+ offset, tagStr, postag_cat));
						featureList.add(this._param_g.toFeature(network, FeaType.word + "IsPunc:"+ offset, tagStr, "" + Utils.isPunctuation(token)));
						
						
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_tokenL" , tagStr, token));
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_lemma-1:", tagStr, Utils.getToken(inputs, i - 1, FEATURE_TYPES.lemma.ordinal())));
	
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_lemma:", tagStr, lemma));
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_postag:", tagStr, postag));
						
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_postag_cat:", tagStr, postag_cat));
						featureList.add(this._param_g.toFeature(network, FeaType.word + "IsPunc:", tagStr, "" + Utils.isPunctuation(token)));
					}

					if (tagStr.startsWith("B")) {
						
						// String cueForm =
						// inst.negation.cueForm[pos_parent].toLowerCase();
						String token = w.toLowerCase();
						String phrase2 = Utils.getPhrase(inputs, pos, pos + 1, FEATURE_TYPES.word.ordinal(), true);
						String phrase3 = Utils.getPhrase(inputs, pos, pos + 2, FEATURE_TYPES.word.ordinal(), true);
						
						int cueLength = pos_child - pos_parent;

						if (token.equals("neither")) {
							featureList.add(this._param_g.toFeature(network, FeaType.word + "_oneword:", tagStr, ""));
						}
						else if (cueLength == 1) {
							if (token.startsWith("un") || token.startsWith("im") ||  (token.startsWith("in") && !token.startsWith("int")) || token.startsWith("ir")) {
								
								
								featureList.add(this._param_g.toFeature(network, FeaType.word + "_cue:", tagStr, ""));
								featureList.add(this._param_g.toFeature(network, FeaType.word + "_prefix:", tagStr, ""));
								featureList.add(this._param_g.toFeature(network, FeaType.word + "_prefix:", tagStr, token.substring(0, 2)));
							} else if (token.startsWith("dis") || token.startsWith("non")) {
								featureList.add(this._param_g.toFeature(network, FeaType.word + "_cue:", tagStr, ""));
								featureList.add(this._param_g.toFeature(network, FeaType.word + "_prefix:", tagStr, ""));
								featureList.add(this._param_g.toFeature(network, FeaType.word + "_prefix:", tagStr, token.substring(0, 3)));
							} else if (token.endsWith("less") || (token.endsWith("lessly")) || (token.endsWith("lessness"))) {
								featureList.add(this._param_g.toFeature(network, FeaType.word + "_cue:", tagStr, ""));
								featureList.add(this._param_g.toFeature(network, FeaType.word + "_suffix:", tagStr, ""));
								featureList.add(this._param_g.toFeature(network, FeaType.word + "_prefix:", tagStr, "less"));
							} else if (NegationGlobal.NegExpList.contains(token)) {
								featureList.add(this._param_g.toFeature(network, FeaType.word + "_cue:", tagStr, ""));
								featureList.add(this._param_g.toFeature(network, FeaType.word + "_negexpression:", tagStr, ""));
								featureList.add(this._param_g.toFeature(network, FeaType.word + "_negexpression:", tagStr, token));
							} else {
								featureList.add(this._param_g.toFeature(network, FeaType.word + "_cue:", tagStr, ""));
								featureList.add(this._param_g.toFeature(network, FeaType.word + "_oneword:", tagStr, ""));
							}
						} else {
							if (NegationGlobal.NegExpList.contains(phrase2)) {
								featureList.add(this._param_g.toFeature(network, FeaType.word + "_negexpression:", tagStr, ""));
								featureList.add(this._param_g.toFeature(network, FeaType.word + "_negexpression:", tagStr, phrase2));
							} else if (NegationGlobal.NegExpList.contains(phrase3)) {
								featureList.add(this._param_g.toFeature(network, FeaType.word + "_negexpression:", tagStr, ""));
								featureList.add(this._param_g.toFeature(network, FeaType.word + "_negexpression:", tagStr, phrase3));
							} else {
								featureList.add(this._param_g.toFeature(network, FeaType.word + "_negexpression:", tagStr, ""));
								featureList.add(this._param_g.toFeature(network, FeaType.word + "_negexpression:", tagStr, cueForm));
							}
							featureList.add(this._param_g.toFeature(network, FeaType.word + "_cue:", tagStr, ""));
						}

					}
				}

			}

		}

		else if (nodetype_parent == NodeType.ScopeNode.ordinal()) {  // scope node

			tagId = tag_parent;
			tagStr = this.compiler._labels[tagId + this.compiler.CueTagSize].getForm();

			if (nodetype_child == NodeType.ScopeNode.ordinal()) {
				String tagStrNext = this.compiler._labels[tag_child + this.compiler.CueTagSize].getForm();
				featureList.add(this._param_g.toFeature(network, "transition_", tagStr, NodeType.values()[nodetype_parent] + tagStr + "=>" + NodeType.values()[nodetype_child] + tagStrNext));
			} else {
				featureList.add(this._param_g.toFeature(network, "transition_", tagStr, NodeType.values()[nodetype_parent] + tagStr + "=>" + END));
			}
			
			String w = Utils.getToken(inputs, pos_parent, NegationInstance.FEATURE_TYPES.word.ordinal());
			String lw = Utils.getToken(inputs, pos_parent - 1, NegationInstance.FEATURE_TYPES.word.ordinal());
			String llw = Utils.getToken(inputs, pos_parent - 2, NegationInstance.FEATURE_TYPES.word.ordinal());
			String rw = Utils.getToken(inputs, pos_parent + 1, NegationInstance.FEATURE_TYPES.word.ordinal());
			String rrw = Utils.getToken(inputs, pos_parent + 2, NegationInstance.FEATURE_TYPES.word.ordinal());

			word = Utils.getToken(inputs, pos_parent, NegationInstance.FEATURE_TYPES.word.ordinal());

			String ct = tagStr;
			String nt = (nodetype_child == NodeType.ScopeNode.ordinal()) ? this.compiler._labels[tag_child + this.compiler.CueTagSize].getForm() : this.END;

			if (NegationGlobal.ENABLE_DISCRETE_FEATURE) {

				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, w));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, w + rw));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, lw + w));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, lw + w + rw));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, llw + lw + w + rw + rrw));

				int[] featureCandicate = new int[] { 3, 4, 5, 6 };

				if (!NegationGlobal.SYNTAX_FEATURE) {
					featureCandicate = new int[] { 3, 4, 5 };
				}

				if (tagStr.startsWith("I")) {
					featureList.add(this._param_g.toFeature(network, FeaType.MP + "Scope", "", ""));
				}
					if (pos_parent >= cueleftboundary_parent && pos_parent < cuerightboundary_parent) {
						String cueForm = Utils.getPhrase(inputs, cueleftboundary_parent, cuerightboundary_parent - 1, FEATURE_TYPES.word.ordinal(), true);
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_cue", tagStr, cueForm + w));
						
						//String cueForm = w;
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_cue", tagStr, cueForm));
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_cue", tagStr, cueForm + rw));
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_cue", tagStr, lw + cueForm));
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_cue", tagStr, lw + cueForm + rw));
						featureList.add(this._param_g.toFeature(network, FeaType.word + "_cue", tagStr, llw + lw + cueForm + rw + rrw));
					}
				

				// for(int i = NegationInstance.FEATURE_TYPES.word.ordinal(); i
				// <= NegationInstance.FEATURE_TYPES.syntax.ordinal(); i++)
				for (int i : featureCandicate) {
					String featureType = NegationInstance.FEATURE_TYPES.values()[i].name();
					featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "curr_" + featureType + ":" + Utils.getToken(inputs, pos_parent, i)));
					featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "next_" + featureType + ":" + Utils.getToken(inputs, pos_parent + 1, i)));
					featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "last_" + featureType + ":" + Utils.getToken(inputs, pos_parent - 1, i)));

					featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "next_" + featureType + ":" + Utils.getToken(inputs, pos_parent, i) + Utils.getToken(inputs, pos_parent + 1, i)));
					featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "last_" + featureType + ":" + Utils.getToken(inputs, pos_parent - 1, i) + Utils.getToken(inputs, pos_parent + 0, i)));

				}

				

				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, "IsAllCap:" + Utils.isAllCap(w)));
				// featureList.add(this._param_g.toFeature(network, FeaType.word
				// + "", tagStr, "IsAllCap:" + Utils.isAllCap(w) + w));

				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, "IsPunc:" + Utils.isPunctuation(w)));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, "IsPunc:" + Utils.isPunctuation(w) + w));

				featureList.add(this._param_g.toFeature(network, FeaType.word + "before", tagStr, "IsPunc:" + Utils.isPunctuation(lw)));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "after", tagStr, "IsPunc:" + Utils.isPunctuation(rw)));

				// for(int i = NegationInstance.FEATURE_TYPES.word.ordinal(); i
				// <= NegationInstance.FEATURE_TYPES.syntax.ordinal(); i++)
				String cueForm = Utils.getPhrase(inputs, cueleftboundary_parent, cuerightboundary_parent - 1, FEATURE_TYPES.word.ordinal(), true);
				for (int i : featureCandicate) {
					String featureType = NegationInstance.FEATURE_TYPES.values()[i].name();

					featureList.add(this._param_g.toFeature(network, featureType + "before", tagStr, Utils.getToken(inputs, pos_parent - 1, i)));
					featureList.add(this._param_g.toFeature(network, featureType + "after", tagStr, Utils.getToken(inputs, pos_parent + 1, i)));
					
					featureList.add(this._param_g.toFeature(network, featureType + "before_w", tagStr, Utils.getToken(inputs, pos_parent - 1, i) + w));
					featureList.add(this._param_g.toFeature(network, featureType + "w_after", tagStr, w + Utils.getToken(inputs, pos_parent + 1, i)));

					featureList.add(this._param_g.toFeature(network, featureType + "", tagStr, Utils.getToken(inputs, pos_parent, i)));
					
					featureList.add(this._param_g.toFeature(network, featureType + "_cue:", tagStr, Utils.getToken(inputs, pos_parent, i) + "-" + cueForm));
				}

				if (pos_parent >= cueleftboundary_parent && pos_parent < cuerightboundary_parent) {
					
					//String cueForm = Utils.getPhrase(inputs, cueleftboundary_parent, cuerightboundary_parent - 1, FEATURE_TYPES.word.ordinal(), true);
					//String cueForm = w;
					featureList.add(this._param_g.toFeature(network, FeaType.word + ":cueForm", tagStr, cueForm));
					featureList.add(this._param_g.toFeature(network, FeaType.word + ":cueWord", tagStr, w));

					// for(int i =
					// NegationInstance.FEATURE_TYPES.word.ordinal(); i <=
					// NegationInstance.FEATURE_TYPES.syntax.ordinal(); i++)
					for (int i : featureCandicate) {
						String featureType = NegationInstance.FEATURE_TYPES.values()[i].name();
						featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "curr_cue_" + featureType + ":" + Utils.getToken(inputs, pos_parent, i)));

						featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "before_cue_" + featureType + ":" + Utils.getToken(inputs, pos_parent + 1, i)));
						featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "after_cue_" + featureType + ":" + Utils.getToken(inputs, pos_parent - 1, i)));

						featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "before_cue_" + featureType + ":" + Utils.getToken(inputs, pos_parent, i) + Utils.getToken(inputs, pos_parent + 1, i)));
						featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "after_cue_" + featureType + ":" + Utils.getToken(inputs, pos_parent - 1, i) + Utils.getToken(inputs, pos_parent, i)));

					
						
						featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "curr_cue_" + featureType + ":" + Utils.getToken(inputs, pos_parent, i) + "-" + cueForm));

					}

					
				}

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
					
					if (nodetype_parent == NodeType.ScopeNode.ordinal()) {
						tagId = tag_parent;
						tagStr = this.compiler._labels[tagId + this.compiler.CueTagSize].getForm();
						
						double[] vec = NegationGlobal.Word2Vec.getVector(word);
						if (vec == null) {
							vec = NegationGlobal.Word2Vec.getVector(NegationGlobal.UNK);

							/*if (vec == null) {
								vec = new double[NegationGlobal.Word2Vec.ShapeSize];
								Arrays.fill(vec, 0);
							}*/
						}

						if (vec != null) {
							for (int i = 0; i < NegationGlobal.Word2Vec.ShapeSize; i++) {
								continuousFeatureList.add(this._param_g.toFeature(network, "continuous-emb:", tagStr, "dim" + i + ":"));
								continuousFeatureValueList.add(vec[i]);
	
							}
						}

					} else if (nodetype_parent == NodeType.CueNode.ordinal()) {
						tagId = tag_parent;
						tagStr = this.compiler._labels[tagId].getForm();
						
						if (tagStr.startsWith("O")) {
							
							for(int pos = 0; pos < size; pos++) {
								String token = Utils.getToken(inputs, pos, NegationInstance.FEATURE_TYPES.word.ordinal()).toLowerCase();
								double[] vec = NegationGlobal.Word2Vec.getVector(token);
								if (vec == null) {
									vec = NegationGlobal.Word2Vec.getVector(NegationGlobal.UNK);
								}

								if (vec != null) {
									for (int i = 0; i < NegationGlobal.Word2Vec.ShapeSize; i++) {
										continuousFeatureList.add(this._param_g.toFeature(network, "continuous-emb:", tagStr, "dim" + i + ":"));
										continuousFeatureValueList.add(vec[i]);
									}
								}
							}
							
						}
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
