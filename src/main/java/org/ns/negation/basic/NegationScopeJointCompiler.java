package org.ns.negation.basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.ns.commons.types.Instance;
import org.ns.commons.types.Label;
import org.ns.example.base.BaseNetwork;
import org.ns.example.base.BaseNetwork.NetworkBuilder;
import org.ns.hypergraph.FeatureArray;
import org.ns.hypergraph.LocalNetworkParam;
import org.ns.hypergraph.Network;
import org.ns.hypergraph.NetworkCompiler;
import org.ns.hypergraph.NetworkConfig;
import org.ns.hypergraph.NetworkIDMapper;
import org.ns.negation.common.Negation;
import org.ns.negation.common.NegationCompiler;
import org.ns.negation.common.NegationGlobal;
import org.ns.negation.common.NegationInstance;
import org.ns.negation.common.Utils;
import org.ns.negation.common.NegationCompiler.NodeType;
import org.ns.negation.common.NegationInstance.FEATURE_TYPES;

public class NegationScopeJointCompiler extends NegationCompiler {

	/**
	 * 
	 */
	private static final long serialVersionUID = 961660059763158561L;

	public NegationScopeJointCompiler() {
		super();
		NetworkIDMapper.setCapacity(new int[] {  NodeType.values().length, NegationGlobal.MAX_SENTENCE_LENGTH, NegationGlobal.MAX_SENTENCE_LENGTH, NegationGlobal.MAX_SENTENCE_LENGTH, 20 });
	}

	protected long toNode_Root(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] { NodeType.Root.ordinal(), size + 1, 0, 0, 0 });
	}

	protected long toNode_CueNode(int size, int pos, int tag_id) {
		return NetworkIDMapper.toHybridNodeID(new int[] { NodeType.CueNode.ordinal(), size - pos, 0, 0, tag_id });
	}

	protected long toNode_ScopeStartNode(int size, int cueFromIdx, int cueEndIdx) {
		return NetworkIDMapper.toHybridNodeID(new int[] { NodeType.ScopeStart.ordinal(), size, size - cueFromIdx, size - cueEndIdx, 0 });
	}

	protected long toNode_ScopeNode(int size, int pos, int tag_id, int cueFromIdx, int cueEndIdx) {
		return NetworkIDMapper.toHybridNodeID(new int[] { NodeType.ScopeNode.ordinal(), size - pos, size - cueFromIdx, size - cueEndIdx, tag_id });
	}

	protected long toNode_X(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] { NodeType.X.ordinal(), 0, 0, 0, 0, });
	}

	public enum NodeType {
		X, ScopeNode, ScopeStart, CueNode, Root
	};

	/*
	public enum CueTag {
		O, B
	}

	public enum ScopeTag {
		O, I
	}*/



	public int CueTagSize = 2;
	public int ScopeTagSize = this._labels.length - CueTagSize;

	@Override
	public NegationInstance decompile(Network network) {
		BaseNetwork lcrfNetwork = (BaseNetwork) network;
		NegationInstance inst = (NegationInstance) network.getInstance();
		int size = inst.size();
		List<String[]> inputs = (List<String[]>) inst.getInput();
		String dataSet = NegationGlobal.dataSet;

		ArrayList<Label> predication_array = new ArrayList<Label>();
		
		for(int i = 0; i < size; i++) {
			Label label = this._labelsMap.get(0);
			predication_array.add(new Label(label));
		}

		long rootNode = toNode_Root(size);
		int node_k = Arrays.binarySearch(lcrfNetwork.getAllNodes(), rootNode);
		inst.negationList.clear();
		boolean hasNegation = false;
		// node_k = 0;
		while (true) {
			// System.out.print(node_k + " ");
			int[] parent_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(node_k));
			int parent_pos = size - parent_ids[1];
			int parent_tag_id = parent_ids[4];
			int parent_node_type = parent_ids[0];

			if (parent_node_type == NodeType.X.ordinal()) {
				break;
			}

			int[] childs = network.getMaxPath(node_k);

			int[] child_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(childs[0]));
			;

			if (parent_node_type == NodeType.Root.ordinal()) {

			} else if (parent_node_type == NodeType.CueNode.ordinal()) {

				if (parent_tag_id == 1) {

					int child_pos = size - child_ids[1];
					int child_tag_id = child_ids[4];
					int child_node_type = child_ids[0];

					Negation neg = null;//Negation.createEmptyNegation(size);
					
					
					
					//for (int i = parent_pos; i < child_pos; i++) 
					int i = parent_pos;
					{
						
						String token = inputs.get(i)[FEATURE_TYPES.word.ordinal()];
						
						String phrase1 = token.toLowerCase();
						String phrase2 = Utils.getPhrase(inputs, i, i + 1, FEATURE_TYPES.word.ordinal(), true);
						String phrase3 = Utils.getPhrase(inputs, i, i + 2, FEATURE_TYPES.word.ordinal(), true);
						if  (NegationGlobal.NegExpList.contains(phrase2)) {
							neg = Negation.createEmptyNegation(size);
							neg.cue[i] = 1;
							neg.cueForm[i] = inputs.get(i)[FEATURE_TYPES.word.ordinal()];
							neg.cue[i + 1] = 1;
							neg.cueForm[i + 1] = inputs.get(i + 1)[FEATURE_TYPES.word.ordinal()];
							
							
						} else if (NegationGlobal.NegExpList.contains(phrase3)) {
							neg = Negation.createEmptyNegation(size);
							neg.cue[i] = 1;
							neg.cueForm[i] = inputs.get(i)[FEATURE_TYPES.word.ordinal()];
							neg.cue[i + 1] = 1;
							neg.cueForm[i + 1] = inputs.get(i + 1)[FEATURE_TYPES.word.ordinal()];
							neg.cue[i + 2] = 1;
							neg.cueForm[i + 2] = inputs.get(i + 2)[FEATURE_TYPES.word.ordinal()];
							
						} else if (NegationGlobal.NegExpList.contains(phrase1) && (!token.equals("neither")) && (!token.equals("nor"))) {
							neg = Negation.createEmptyNegation(size);
							neg.cue[i] = 1;
							neg.cueForm[i] = token;
							
						} else if (dataSet.startsWith("cdsco") && token.startsWith("un") || token.startsWith("im") || (token.startsWith("in") && !token.startsWith("int")) || token.startsWith("ir")) {
							
							neg = Negation.createEmptyNegation(size);
							neg.cue[i] = 1;
							neg.cueForm[i] = token.substring(0, 2);
							
							
							
						} else if (dataSet.startsWith("cdsco") && token.startsWith("dis") || (token.startsWith("non") && !token.equals("none"))) {
							neg = Negation.createEmptyNegation(size);
							neg.cue[i] = 1;
							neg.cueForm[i] = token.substring(0, 3);
							
							

						} else if (dataSet.startsWith("cdsco") && token.endsWith("less") || (token.endsWith("lessly")) || (token.endsWith("lessness")) || token.contains("less")) {
							neg = Negation.createEmptyNegation(size);
							neg.cue[i] = 1;
							neg.cueForm[i] = "less";
							
							
						} else if (token.equals("neither")) {
							neg = Negation.createEmptyNegation(size);
							neg.cue[i] = 1;
							neg.cueForm[i] = inputs.get(i)[FEATURE_TYPES.word.ordinal()];
							int posNext = Utils.getPosNextToken(inputs, "nor", i, FEATURE_TYPES.word.ordinal());
							
							if (posNext != -1) {
								neg.cue[posNext] = 1;
								neg.cueForm[posNext] = inputs.get(posNext)[FEATURE_TYPES.word.ordinal()];
							}
						} 
						else {
							neg = Negation.createEmptyNegation(size);
							neg.cue[i] = 1;
							neg.cueForm[i] = token;
						}
						
						if (neg != null)
						{
							hasNegation = true;
							
							int nextScope_k = childs[1];
							int[] scope_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(nextScope_k));

							while (true) {
								nextScope_k = network.getMaxPath(nextScope_k)[0];
								scope_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(nextScope_k));

								int scope_pos = size - scope_ids[1];
								int scope_tag_id = scope_ids[4];
								int scope_node_type = scope_ids[0];

								if (scope_node_type == NodeType.ScopeNode.ordinal()) {
									String labelStr = this._labels[this.CueTagSize + scope_tag_id].getForm();
									if (labelStr.startsWith("O"))
										neg.span[scope_pos] = 0;
									else
										neg.span[scope_pos] = 1;
								} else if (scope_node_type == NodeType.X.ordinal()) {
									break;
								} else {
									System.err.println("Unexpected Scope Node");
									System.exit(-1);
								}

							}

							
							inst.negationList.add(neg);
						}
						
						
					}

					
					//inst.negationList.add(neg);
				}

			}

			node_k = childs[0];

		}
		
		if (!hasNegation || inst.hasNegation == false) {
			inst.hasNegation = false;
		}

		inst.setPrediction(predication_array);

		return inst;
	}

	@Override
	public Network compileUnlabeled(int networkId, Instance instance, LocalNetworkParam param) {

		NegationInstance inst = (NegationInstance) instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		ArrayList<Label> outputs = this.convert2Output(inst);
		inst.setOutput(outputs);

		int size = inst.size();

		long start = this.toNode_Root(size);
		lcrfNetwork.addNode(start);

		ArrayList<Negation> negationList = inst.negationList;

		long[][] cue_node_array = new long[size][this._labels.length];
		long[][][][] scope_node_array = new long[size][this._labels.length][size][size + 1];
		long[][] scope_nodeStart_array = new long[size][size + 1];
		// build node array
		// build cueNode
		for (int pos = 0; pos < size; pos++) {

			for (int tag_id = 0; tag_id < this.CueTagSize; tag_id++) {
				long node = this.toNode_CueNode(size, pos, tag_id);
				lcrfNetwork.addNode(node);
				cue_node_array[pos][tag_id] = node;
			}

		}

		// build scope node array
		for (int cuepos = 0; cuepos < size; cuepos++) {
			for (int cueL = 1; cueL <= NegationGlobal.CUE_MAX_L; cueL++) {
				if (cuepos + cueL <= size) {
					for (int tag_id = 0; tag_id < this.ScopeTagSize; tag_id++) {
						for (int pos = 0; pos < size; pos++) {
							long node = this.toNode_ScopeNode(size, pos, tag_id, cuepos, cuepos + cueL);
							lcrfNetwork.addNode(node);
							scope_node_array[pos][tag_id][cuepos][cuepos + cueL] = node;
						}
					}
					{
						long node = this.toNode_ScopeStartNode(size, cuepos, cuepos + cueL);
						lcrfNetwork.addNode(node);
						scope_nodeStart_array[cuepos][cuepos + cueL] = node;
					}
				}
			}
		}

		long X = this.toNode_X(inst.size());
		lcrfNetwork.addNode(X);

		// ///////
		long from = start;
		
		int[] cue_tag_id_set = new int[]{0, 1};
		
		if (!inst.hasNegation) {
			cue_tag_id_set = new int[]{0};
		}

		for (int tag_id : cue_tag_id_set) {
			long to = cue_node_array[0][tag_id];
			lcrfNetwork.addEdge(from, new long[] { to });
		}

		for (int pos = 0; pos < size; pos++) {

			// O
			{
				for (int cueTagId : cue_tag_id_set) {
					from = cue_node_array[pos][0];
					long to = (pos + 1 >= size) ? X : cue_node_array[pos + 1][cueTagId];
					lcrfNetwork.addEdge(from, new long[] { to });
				}

			}

			// B
			if (inst.hasNegation) 
			
			for (int L = 1; L <= NegationGlobal.CUE_MAX_L; L++) {
				if (pos + L > size)
					continue;
				for (int cueTagId = 0; cueTagId < this.CueTagSize; cueTagId++) {
					from = cue_node_array[pos][1];
					long to = (pos + L == size) ? X : cue_node_array[pos + L][cueTagId];
					long scopeStartNode = scope_nodeStart_array[pos][pos + L];

					int[] fromIds = NetworkIDMapper.toHybridNodeArray(from);
					int[] toIds = NetworkIDMapper.toHybridNodeArray(to);
					int[] startScopeIds = NetworkIDMapper.toHybridNodeArray(scopeStartNode);

					lcrfNetwork.addEdge(from, new long[] { to, scopeStartNode });

					int[] last_tag_id_set = new int[] { 0, 1 };
					int[] tag_id_set = new int[] { 0, 1 };

					for (int tag_id : tag_id_set) {
						from = scopeStartNode;
						to = scope_node_array[0][tag_id][pos][pos + L];
						lcrfNetwork.addEdge(from, new long[] { to });
					}

					// ****build scope chain
					for (int scopePos = 1; scopePos < size; scopePos++) {

						last_tag_id_set = new int[] { 0, 1 };
						tag_id_set = new int[] { 0, 1 };

						for (int last_tag_id : last_tag_id_set) {
							for (int tag_id : tag_id_set) {
								from = scope_node_array[scopePos - 1][last_tag_id][pos][pos + L];
								to = scope_node_array[scopePos][tag_id][pos][pos + L];
								lcrfNetwork.addEdge(from, new long[] { to });
							}
						}
					}

					for (int last_tag_id : last_tag_id_set) {
						from = scope_node_array[size - 1][last_tag_id][pos][pos + L];
						to = X;
						lcrfNetwork.addEdge(from, new long[] { to });
					}
					// *****build scope chain

				}

			}

		}

		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		return network;
	}

	@Override
	public Network compileLabeled(int networkId, Instance instance, LocalNetworkParam param) {

		NegationInstance inst = (NegationInstance) instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		ArrayList<Label> outputs = this.convert2Output(inst);
		inst.setOutput(outputs);

		ArrayList<Negation> negationList = inst.negationList;

		int size = inst.size();

		long start = this.toNode_Root(size);
		lcrfNetwork.addNode(start);

		long[][] cue_node_array = new long[size][this._labels.length];
		long[][][][] scope_node_array = new long[size][this._labels.length][size][size + 1];
		long[][] scope_nodeStart_array = new long[size][size + 1];
		// build node array
		// build cueNode
		for (int pos = 0; pos < size; pos++) {

			for (int tag_id = 0; tag_id < 2; tag_id++) {
				long node = this.toNode_CueNode(size, pos, tag_id);
				lcrfNetwork.addNode(node);
				cue_node_array[pos][tag_id] = node;
			}

		}

		// build scope node array
		for (int cuepos = 0; cuepos < size; cuepos++) {
			for (int cueL = 1; cueL <= NegationGlobal.CUE_MAX_L; cueL++) {
				if (cuepos + cueL <= size) {
					for (int tag_id = 0; tag_id < this.ScopeTagSize; tag_id++) {
						for (int pos = 0; pos < size; pos++) {
							long node = this.toNode_ScopeNode(size, pos, tag_id, cuepos, cuepos + cueL);
							// lcrfNetwork.addNode(node);
							scope_node_array[pos][tag_id][cuepos][cuepos + cueL] = node;
						}
					}

					{
						long node = this.toNode_ScopeStartNode(size, cuepos, cuepos + cueL);
						// lcrfNetwork.addNode(node);
						scope_nodeStart_array[cuepos][cuepos + cueL] = node;
					}

				}
			}
		}

		long X = this.toNode_X(inst.size());
		lcrfNetwork.addNode(X);

		// ///////
		long from = start;
		int lastPos = -1;
		long lastNode = start;
		for (int negIdx = 0; negIdx < inst.negations.length; negIdx++) {

			Negation neg = inst.negations[negIdx];

			int leftBoundary = neg.leftmost_cue_pos;
			int rightBoundary = neg.rightmost_cue_pos;

			for (int pos = lastPos + 1; pos < leftBoundary; pos++) {
				if (pos == 0)
					lastNode = start;

				long currNode = cue_node_array[pos][0];

				int[] fromIds = NetworkIDMapper.toHybridNodeArray(lastNode);
				int[] toIds = NetworkIDMapper.toHybridNodeArray(currNode);

				lcrfNetwork.addEdge(lastNode, new long[] { currNode });

				/*
				 * try {
				 * 
				 * } catch (Exception e) { System.out.println("lastNode:" +
				 * Arrays.toString(fromIds) + " to:" + Arrays.toString(toIds));
				 * System.out.println(e.getMessage()); System.exit(-1);
				 * 
				 * }
				 */

				lastNode = currNode;
				lastPos = pos;
			}

			{
				long currNode = cue_node_array[leftBoundary][1]; // B

				int[] fromIds = NetworkIDMapper.toHybridNodeArray(lastNode);
				int[] toIds = NetworkIDMapper.toHybridNodeArray(currNode);

				if (lastNode != currNode)
					lcrfNetwork.addEdge(lastNode, new long[] { currNode });

				lastNode = currNode;
				lastPos = leftBoundary;
			}

			if (neg.cueForm[leftBoundary].equals("neither")) {
				rightBoundary = leftBoundary;
			}

			{
				long currNode = rightBoundary + 1 >= size ? X : cue_node_array[rightBoundary + 1][0];

				if (negIdx + 1 < inst.negations.length) {
					Negation nextneg = inst.negations[negIdx + 1];
					if (nextneg.leftmost_cue_pos == rightBoundary + 1) {
						currNode = rightBoundary + 1 >= size ? X : cue_node_array[rightBoundary + 1][1];
					}
				}

				long scopeStartNode = scope_nodeStart_array[leftBoundary][rightBoundary + 1];// scope_node_array[0][neg.span[0]
																								// ==
																								// 0
																								// ?
																								// 0
																								// :
																								// 1][leftBoundary][rightBoundary
																								// +
																								// 1];
				lcrfNetwork.addNode(scopeStartNode);
				lcrfNetwork.addEdge(lastNode, new long[] { currNode, scopeStartNode });
				lastNode = currNode;
				lastPos = rightBoundary + 1;

				// scope
				{
					long lastScopeNode = scopeStartNode;

					for (int posScope = 0; posScope < size; posScope++) {
						int scopeTagId = neg.span[posScope];
						if (scopeTagId == 0 ) {
							scopeTagId = 0;
						} else {
							scopeTagId = 1;
						}
						long currScopeNode = scope_node_array[posScope][scopeTagId][leftBoundary][rightBoundary + 1];
						lcrfNetwork.addNode(currScopeNode);

						int[] fromIds = NetworkIDMapper.toHybridNodeArray(lastScopeNode);
						int[] toIds = NetworkIDMapper.toHybridNodeArray(currScopeNode);

						lcrfNetwork.addEdge(lastScopeNode, new long[] { currScopeNode });

						lastScopeNode = currScopeNode;
					}

					lcrfNetwork.addEdge(lastScopeNode, new long[] { X });

				}
			}

		}

		if (lastNode != X) {

			for (int pos = lastPos + 1; pos < size; pos++) {

				long currNode = cue_node_array[pos][0];

				lcrfNetwork.addEdge(lastNode, new long[] { currNode });
				lastNode = currNode;
				lastPos = pos;
			}

			lcrfNetwork.addEdge(lastNode, new long[] { X });

		}

		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		return network;

	}

	@Override
	public Label[] getLabels() {
		String[] labelForms = new String[] { "O-Cue", "B-Cue", "O-Scope", "I-Scope" };

		Label[] labels = new Label[labelForms.length];
		for (int i = 0; i < labels.length; i++)
			labels[i] = new Label(labelForms[i], i);
		
		//this.CueTagSize = 2;
		//this.ScopeTagSize = labels.length - this.CueTagSize;

		return labels;
	}

	@Override
	public ArrayList<Label> convert2Output(NegationInstance inst) {

		int[] cues = inst.negation.cue;
		int l_cue_pos = inst.size();
		int r_cue_pos = 0;
		for (int i = 0; i < inst.size(); i++) {
			if (cues[i] == 1 && i < l_cue_pos) {
				l_cue_pos = i;
			}

			if (cues[i] == 1 && i > r_cue_pos) {
				r_cue_pos = i;
			}
		}

		ArrayList<Label> output = new ArrayList<Label>();
		int last_lable_id = 0;
		for (int i = 0; i < inst.size(); i++) {
			int lable_id = inst.negation.span[i];

			if (lable_id >= 1) {
				if (i < l_cue_pos) {
					lable_id = 1;
				} else if (i > r_cue_pos) {
					lable_id = 2;
				} else {
					lable_id = 3;
				}

				if (last_lable_id == 0 && lable_id == 1) {
					lable_id += 3;
				}
			}

			output.add(this._labelsMap.get(lable_id));

			last_lable_id = lable_id;
		}

		return output;
	}
	
	
	/**
	 * The cost of the structure at the edge connecting node <code>k</code> with its specific
	 * child node <code>child_k</code>.<br>
	 * This is used for structured SVM, and generally the implementation requires the labeled Instance, which
	 * can be accessed through {@link Network#getLabeledNetwork}.<br>
	 * @param network
	 * @param parent_k
	 * @param child_k
	 * @return
	 */	
	public double costAt(Network network, int parent_k, int[] child_k){
		int size = network.getInstance().size();
		Network labeledNet = network.getLabeledNetwork();
		long node = network.getNode(parent_k);
		int node_k = labeledNet.getNodeIndex(node);
		
		
		NegationInstance inst = ((NegationInstance) labeledNet.getInstance());

		long node_parent = network.getNode(parent_k);

		if (child_k.length == 0) {
			return 0;
		}
		
		long node_child = network.getNode(child_k[0]);

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
		
		double cost = 0;
		NetworkConfig.NORMALIZE_COST = true;
		
		if (nodetype_parent == NodeType.CueNode.ordinal()) {
			
			if (cueleftboundary_parent >= size) {
				return cost;
			}
			
			String tagStr = _labels[tag_parent].getForm();
			
			int[] cue = new int[size];
			Arrays.fill(cue, 0);
			for(int i = pos_parent; i < pos_child; i++) {
				cue[i] = 1;
			}
			
			double fTotal = 0.0;
			
			Negation negation = null;
			
			for(Negation neg : inst.negations) {
				boolean ret1 = Utils.spanArrayEqual(neg.cue, cue);
				boolean ret2 = Utils.spanArrayEqual(neg.cue, cueleftboundary_parent, cuerightboundary_parent);

				if (ret1 || ret2) {
					negation = neg;
				}
			}
				
			
			if (tagStr.startsWith("B")) {
				//cost = Math.abs(fTotal) < 1e-5 ? 1 : 1 / fTotal;
				if (negation != null) {
					cost += 0;
				} else {
					cost += 1;
				}
			} else {
				//cost = Math.abs(fTotal) < 1e-5 ? 0 : 1 / fTotal; //beta
				if (negation != null) {
					cost += NegationGlobal.RECALL_BETA;
				} else {
					cost += 0;
				}
			}
			
			cost *= NetworkConfig.NODE_COST;
			if(NetworkConfig.NORMALIZE_COST){
				cost /= size;
			}
				
			
		} else if (nodetype_parent == NodeType.ScopeStart.ordinal()) {
			
		} else if (nodetype_parent == NodeType.ScopeNode.ordinal()) {
			
			
			String tagStr = this._labels[tag_parent + CueTagSize].getForm();
			int ScopeId = (tagStr.startsWith("I")) ? 1 : 0;
			
			int[] cue = new int[size];
			Arrays.fill(cue, 0);
			for(int i = cueleftboundary_parent; i < cuerightboundary_parent; i++) {
				cue[i] = 1;
			}
			
			
			Negation negation = null;
			
			for(Negation neg : inst.negations) {
				
				if (Utils.spanArrayEqual(neg.cue, cue) || Utils.spanArrayEqual(neg.cue, cueleftboundary_parent, cuerightboundary_parent)) {
					negation = neg;
				}
				
				
			}

			if (negation != null) {
				//cost +=  (negation.span[pos_parent] == ScopeId) ? 0 : 1;
				if (negation.span[pos_parent] == ScopeId) {
					cost += 0;
				} else if (ScopeId == 0 && negation.span[pos_parent] == 1) {
					cost += NegationGlobal.RECALL_BETA;
				} else {
					cost += 1;
				}
			} else {
				cost += 1;
			}
			
			
			cost *= NetworkConfig.NODE_COST;
			if(NetworkConfig.NORMALIZE_COST) {
				cost /= size;
			}
			
			
			
		}
	

		
		return cost;
		
	}


}
