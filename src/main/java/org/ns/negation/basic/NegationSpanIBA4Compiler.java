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
import org.ns.hypergraph.NetworkIDMapper;
import org.ns.negation.common.NegationCompiler;
import org.ns.negation.common.NegationGlobal;
import org.ns.negation.common.NegationInstance;

public class NegationSpanIBA4Compiler extends NegationCompiler {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5689984204595365160L;

	public NegationSpanIBA4Compiler() {
		super();
	}

	@Override
	public NegationInstance decompile(Network network) {
		BaseNetwork lcrfNetwork = (BaseNetwork) network;
		NegationInstance inst = (NegationInstance) network.getInstance();
		int size = inst.size();
		// List<String[]> inputs = (List<String[]>) inst.getInput();

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
		for (int tag_id = 0; tag_id < this._labels.length; tag_id++) {
			long to = node_array[0][tag_id];
			lcrfNetwork.addEdge(from, new long[] { to });
		}

		for (int pos = 1; pos < size; pos++) {
			for (int last_tag_id = 0; last_tag_id < this._labels.length; last_tag_id++) {
				for (int tag_id = 0; tag_id < this._labels.length; tag_id++) {
					from = node_array[pos - 1][last_tag_id];
					long to = node_array[pos][tag_id];
					lcrfNetwork.addEdge(from, new long[] { to });
				}
			}
		}

		for (int last_tag_id = 0; last_tag_id < this._labels.length; last_tag_id++) {
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
		inst.setOutput(outputs);

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
		String[] labelForms = new String[] { "O", "IB", "IA", "IM" };

		Label[] labels = new Label[labelForms.length];
		for (int i = 0; i < labels.length; i++)
			labels[i] = new Label(labelForms[i], i);

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

		for (int i = 0; i < inst.size(); i++) {
			int lable_id = inst.negation.span[i];

			if (lable_id >= 1)
				if (i < l_cue_pos) {
					lable_id = 1;
				} else if (i > r_cue_pos) {
					lable_id = 2;
				} else {
					lable_id = 3;
				}

			output.add(this._labelsMap.get(lable_id));

		}

		return output;
	}

}
