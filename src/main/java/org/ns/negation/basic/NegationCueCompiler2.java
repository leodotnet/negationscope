package org.ns.negation.basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.ns.commons.types.Instance;
import org.ns.commons.types.Label;
import org.ns.example.base.BaseNetwork;
import org.ns.example.base.BaseNetwork.NetworkBuilder;
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
import org.ns.negation.common.NegationInstance.FEATURE_TYPES;

public class NegationCueCompiler2 extends NegationCompiler {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5689984204595365160L;

	public NegationCueCompiler2() {
		super();
	}

	@Override
	public NegationInstance decompile(Network network) {
		BaseNetwork lcrfNetwork = (BaseNetwork) network;
		NegationInstance inst = (NegationInstance) network.getInstance();
		int size = inst.size();
		List<String[]> inputs = (List<String[]>) inst.getInput();

		ArrayList<Label> predication_array = new ArrayList<Label>();

		long rootNode = toNode_Root(size);
		int node_k = Arrays.binarySearch(lcrfNetwork.getAllNodes(), rootNode);
		// node_k = 0;
		while (true) {
			// System.out.print(node_k + " ");
			int[] parent_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(node_k));
			int parent_pos = size - parent_ids[0];
			int parent_tag_id = parent_ids[1];
			int parent_node_type = parent_ids[2];

			if (parent_node_type == NodeType.X.ordinal()) {
				break;
			}

			int[] childs = network.getMaxPath(node_k);

			int[] child_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(childs[0]));
			;

			if (parent_node_type == NodeType.Root.ordinal()) {

			} else if (parent_node_type == NodeType.Node.ordinal()) {

				Label label = this._labelsMap.get(parent_tag_id);

				predication_array.add(new Label(label));

			}

			node_k = childs[0];

		}

		inst.setPrediction(predication_array);
		
		String dataSet = NegationGlobal.dataSet;
		inst.negationList.clear();
		Arrays.fill(inst.negation.cue, 0);
		Arrays.fill(inst.negation.cueForm , "_");
		boolean hasNegation = false;
		for(int i = 0; i < predication_array.size(); i++) {
			Label label = predication_array.get(i);
			String token = inputs.get(i)[FEATURE_TYPES.word.ordinal()];
			String pos_tag = inputs.get(i)[FEATURE_TYPES.pos_tag.ordinal()];
			
			
			if (label.getForm().startsWith("I")) {
				
				Negation neg = null;
				
				String phrase1 = token.toLowerCase();
				String phrase2 = Utils.getPhrase(inputs, i, i + 1, FEATURE_TYPES.word.ordinal(), true);
				String phrase3 = Utils.getPhrase(inputs, i, i + 2, FEATURE_TYPES.word.ordinal(), true);
				
				if (dataSet.startsWith("cdsco") && token.startsWith("un") || token.startsWith("im") || token.startsWith("in") || token.startsWith("ir")) {
					
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
					
					
				} else if (NegationGlobal.NegExpList.contains(phrase1) && (!token.equals("neither")) && (!token.equals("nor"))) {
					neg = Negation.createEmptyNegation(size);
					neg.cue[i] = 1;
					neg.cueForm[i] = token;
					
				} else if (NegationGlobal.NegExpList.contains(phrase2)) {
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
					inst.negationList.add(neg);
				}
				
			} 
		}
		
		
		if (!hasNegation) {
			inst.hasNegation = false;
			/*Arrays.fill(inst.negation.span, 0);
			Arrays.fill(inst.negation.spanForm, "_");
			Arrays.fill(inst.negation.target, 0);
			Arrays.fill(inst.negation.targetForm, "_");*/
		}
		

		return inst;
	}
	
	public String getCueForm(String w) {
		String cueForm = w;
		if (w.startsWith("un") ||  w.startsWith("im") || w.startsWith("in") ||  w.startsWith("ir")) {
			cueForm = w.substring(0, 2);
		} else if (w.startsWith("dis") ||w.startsWith("non") ) {
			cueForm = w.substring(0, 3);
		} else if (w.contains("less") || w.endsWith("less") || (w.endsWith("lessly"))) {
			cueForm = "less";
		}
		
		return cueForm;
			
	}

	@Override
	public Network compileUnlabeled(int networkId, Instance instance, LocalNetworkParam param) {

		NegationInstance inst = (NegationInstance) instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		
		if (inst.getOutput() == null) {
			ArrayList<Label> outputs = this.convert2Output(inst);
			inst.setOutput(outputs); // the following code will not use the outputs
		}
		
		int size = inst.size();

		long start = this.toNode_Root(size);
		lcrfNetwork.addNode(start);

		long[][] node_array = new long[size][this._labels.length];

		// build node array
		for (int pos = 0; pos < size; pos++) {

			for (int tag_id = 0; tag_id < this._labels.length; tag_id++) {
				long node = this.toNode_Node(size, pos, tag_id);
				lcrfNetwork.addNode(node);
				node_array[pos][tag_id] = node;
			}

		}

		long X = this.toNode_X(inst.size());
		lcrfNetwork.addNode(X);

		// ///////
		long from = start;
		int[] tag_id_set = new int[]{0, 1};
		int[] last_tag_id_set = new int[]{0, 1};
		
		
		
		
		if (!inst.hasNegation) {
			tag_id_set = new int[]{0};
		}
		
		
		for (int tag_id : tag_id_set) {
			long to = node_array[0][tag_id];
			lcrfNetwork.addEdge(from, new long[] { to });
		}

		for (int pos = 1; pos < size; pos++) {
			
			last_tag_id_set = new int[]{0, 1};
			tag_id_set = new int[]{0, 1};
			
			
			
			if  (!inst.hasNegation) {
				tag_id_set = new int[]{0};
				last_tag_id_set = new int[]{0};
			}
			
			
			
			
			for (int last_tag_id : last_tag_id_set) {
				for (int tag_id : tag_id_set) {
					from = node_array[pos - 1][last_tag_id];
					long to = node_array[pos][tag_id];
					lcrfNetwork.addEdge(from, new long[] { to });
				}
			}
		}

		last_tag_id_set = new int[]{0, 1};
		
		
		if  (!inst.hasNegation) {
			last_tag_id_set = new int[]{0};
		}
		
		
		for (int last_tag_id : last_tag_id_set) {
			from = node_array[size - 1][last_tag_id];
			lcrfNetwork.addEdge(from, new long[] { X });
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
		if (inst.getOutput() == null) {
			inst.setOutput(outputs);
		}

		int size = inst.size();

		long start = this.toNode_Root(size);
		lcrfNetwork.addNode(start);

		long[][] node_array = new long[size][this._labels.length];

		// build node array
		for (int pos = 0; pos < size; pos++) {

			for (int tag_id = 0; tag_id < this._labels.length; tag_id++) {
				long node = this.toNode_Node(size, pos, tag_id);
				lcrfNetwork.addNode(node);
				node_array[pos][tag_id] = node;
			}

		}

		long X = this.toNode_X(inst.size());
		lcrfNetwork.addNode(X);

		// ///////
		long from = start;

		for (int pos = 0; pos < size; pos++) {
			int tag_id = outputs.get(pos).getId();
			long to = node_array[pos][tag_id];
			lcrfNetwork.addEdge(from, new long[] { to });
			from = to;
		}

		lcrfNetwork.addEdge(from, new long[] { X });

		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		return network;

	}

	@Override
	public Label[] getLabels() {
		String[] labelForms = new String[] { "O", "I"};

		Label[] labels = new Label[labelForms.length];
		for (int i = 0; i < labels.length; i++)
			labels[i] = new Label(labelForms[i], i);

		return labels;
	}

	@Override
	public ArrayList<Label> convert2Output(NegationInstance inst) {
		
		ArrayList<Label> output = new ArrayList<Label>();
		
		if (inst.hasNegation == false) {
			for (int i = 0; i < inst.size(); i++) {
				output.add(this._labelsMap.get(0));
			}
			
			return output;
		}
		
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
		
		inst.negation.leftmost_cue_pos = l_cue_pos;
		inst.negation.rightmost_cue_pos = r_cue_pos;

		

		for (int i = 0; i < inst.size(); i++) {
			int lable_id = inst.negation.cue[i];
			output.add(this._labelsMap.get(lable_id));

		}

		return output;
	}

}
