package org.ns.negation.basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ns.commons.types.Instance;
import org.ns.commons.types.Label;
import org.ns.example.base.BaseNetwork;
import org.ns.example.base.BaseNetwork.NetworkBuilder;
import org.ns.hypergraph.LocalNetworkParam;
import org.ns.hypergraph.Network;
import org.ns.hypergraph.NetworkIDMapper;
import org.ns.negation.common.NegationCompiler;
import org.ns.negation.common.NegationGlobal;
import org.ns.negation.common.NegationInstance;
import org.ns.negation.common.NegationCompiler.NodeType;

public class NegationSpanSemiROILatent2Compiler extends NegationCompiler {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5689984204595365160L;

	public NegationSpanSemiROILatent2Compiler() {
		super();
		NetworkIDMapper.setCapacity(new int[] { NegationGlobal.MAX_SENTENCE_LENGTH, 20, NodeTypeSize, 100 });
	}

	protected long toNode_Root(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size + 1, 0, NodeType.Root.ordinal(), 0 });
	}

	protected long toNode_Node(int size, int pos, int tag_id, int latentId) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size - pos, tag_id, NodeType.Node.ordinal(), latentId });
	}

	protected long toNode_X(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] { 0, 0, NodeType.X.ordinal(), 0 });
	}

	@Override
	public NegationInstance decompile(Network network) {
		BaseNetwork nsnetwork = (BaseNetwork) network;
		NegationInstance inst = (NegationInstance) network.getInstance();
		int size = inst.size();
		// List<String[]> inputs = (List<String[]>) inst.getInput();

		ArrayList<Label> predication_array = new ArrayList<Label>();

		long rootNode = toNode_Root(size);
		int node_k = Arrays.binarySearch(nsnetwork.getAllNodes(), rootNode);
		// node_k = 0;
		while (true) {
			// System.out.print(node_k + " ");
			int[] parent_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(node_k));
			int parent_pos = size - parent_ids[0];
			int parent_tag_id = parent_ids[1];
			int parent_node_type = parent_ids[2];
			String tagParent = this._labels[parent_tag_id].getForm();
			if (parent_node_type == NodeType.X.ordinal()) {
				break;
			}

			int[] childs = network.getMaxPath(node_k);

			int[] child_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(childs[0]));
			int child_pos = size - child_ids[0];

			if (parent_node_type == NodeType.Root.ordinal()) {

			} else if (parent_node_type == NodeType.Node.ordinal()) {
				
				if (tagParent.startsWith("I")) {
					if (parent_pos < size) {
						Label label = this._labelsMap.get(parent_tag_id);
						predication_array.add(new Label(label));
					}
				} else {
				
					for(int pos = parent_pos; pos < child_pos; pos++) {

						Label label = this._labelsMap.get(parent_tag_id);

						predication_array.add(new Label(label));
					}
				}

			}
			node_k = childs[0];

		}

		inst.setPrediction(predication_array);

		return inst;
	}

	@Override
	public Network compileUnlabeled(int networkId, Instance instance, LocalNetworkParam param) {

		NegationInstance inst = (NegationInstance) instance;
		NetworkBuilder<BaseNetwork> nsnetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();

		if (inst.getOutput() == null) {
			ArrayList<Label> outputs = this.convert2Output(inst);
			inst.setOutput(outputs); // the following code will not use the
										// outputs
		}

		int size = inst.size();

		long start = this.toNode_Root(size);
		nsnetwork.addNode(start);

		long[][][] node_array = new long[size + 1][this._labels.length][NegationGlobal.MAX_LATENT_NUMBER];

		// build node array
		for (int pos = 0; pos < size + 1; pos++) {

			for (int tag_id = 0; tag_id < this._labels.length; tag_id++) {
				
				for(int latentId = 0; latentId < NegationGlobal.MAX_LATENT_NUMBER; latentId++) {
					long node = this.toNode_Node(size, pos, tag_id, latentId);
					nsnetwork.addNode(node);
					node_array[pos][tag_id][latentId] = node;
				}
			}

		}

		long X = this.toNode_X(inst.size());
		nsnetwork.addNode(X);

		// ///////

		int[] tag_id_set = new int[] { 0, 1 };
		int[] last_tag_id_set = new int[] { 0, 1 };

		long from = start;
		long to = -1;

		for (int tag_id : tag_id_set) {
			for(int latentId = 0; latentId < NegationGlobal.MAX_LATENT_NUMBER; latentId++) {
				nsnetwork.addEdge(start, new long[] { node_array[0][tag_id][latentId] });
			}
		}

		for (int pos = 0; pos < size; pos++) {

			// node I
			for(int latentId = 0; latentId < NegationGlobal.MAX_LATENT_NUMBER; latentId++) {
				from = node_array[pos][1][latentId];
				{
					for(int nextLatentId = 0; nextLatentId < NegationGlobal.MAX_LATENT_NUMBER; nextLatentId++) {
						long node_I = node_array[pos + 1][1][nextLatentId];
						nsnetwork.addEdge(from, new long[] { node_I });
		
						if (pos < size - 1) {
							long node_O = node_array[pos + 1][0][nextLatentId];
							nsnetwork.addEdge(from, new long[] { node_O });
						}
						
						if (pos + 1 == size)
							break;
					}
				}
			

				// node O
				from = node_array[pos][0][latentId];
	
				for (int M = 1; M < NegationGlobal.M_MAX && pos + M <= size; M++) {
	
					for(int nextLatentId = 0; nextLatentId < NegationGlobal.MAX_LATENT_NUMBER; nextLatentId++) {
						long node_I = node_array[pos + M][1][nextLatentId];
						nsnetwork.addEdge(from, new long[] { node_I });
						
						if (pos + M == size)
							break;
					}
	
				}
			}

		}

		nsnetwork.addEdge(node_array[size][1][0], new long[] { X });

		BaseNetwork network = nsnetwork.build(networkId, inst, param, this);
		return network;
	}

	@Override
	public Network compileLabeled(int networkId, Instance instance, LocalNetworkParam param) {

		NegationInstance inst = (NegationInstance) instance;
		NetworkBuilder<BaseNetwork> nsnetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		ArrayList<Label> outputs = this.convert2Output(inst);
		if (inst.getOutput() == null) {
			inst.setOutput(outputs);
		}

		int size = inst.size();

		long start = this.toNode_Root(size);
		nsnetwork.addNode(start);

		long[][][] node_array = new long[size + 1][this._labels.length][NegationGlobal.MAX_LATENT_NUMBER];

		// build node array
		for (int pos = 0; pos < size + 1; pos++) {

			for (int tag_id = 0; tag_id < this._labels.length; tag_id++) {
				
				for(int latentId = 0; latentId < NegationGlobal.MAX_LATENT_NUMBER; latentId++) {
					long node = this.toNode_Node(size, pos, tag_id, latentId);
					nsnetwork.addNode(node);
					node_array[pos][tag_id][latentId] = node;
				}
			}

		}

		long X = this.toNode_X(inst.size());
		nsnetwork.addNode(X);

		// ///////
		//long from = start;
		ArrayList<Long> fromSet = new ArrayList<Long>();
		fromSet.add(start);
		int lastTagId = 1;

		for (int pos = 0; pos < size; pos++) {
			int tagId = outputs.get(pos).getId();
			
			
			
			if (lastTagId == 1 || (lastTagId == 0 && tagId == 1)) {
				
				ArrayList<Long> toSet = new ArrayList<Long>();
				
				for(long from : fromSet) {
					for(int latentId = 0; latentId < NegationGlobal.MAX_LATENT_NUMBER; latentId++) {
						long to = node_array[pos][tagId][latentId];
						nsnetwork.addEdge(from, new long[] { to });
						toSet.add(to);
					}
				
				}
				
				fromSet.clear();
				fromSet.addAll(toSet);
			}

			lastTagId = tagId;

		}

		for(long from : fromSet)
			nsnetwork.addEdge(from, new long[] { node_array[size][1][0]});

		nsnetwork.addEdge(node_array[size][1][0], new long[] { X });

		BaseNetwork network = nsnetwork.build(networkId, inst, param, this);
		return network;

	}

	@Override
	public Label[] getLabels() {
		String[] labelForms = new String[] { "O", "I" };

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
			int lable_id = inst.negation.span[i];

			output.add(this._labelsMap.get(lable_id));

		}

		return output;
	}

}
