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
import org.ns.negation.common.NegationCompiler;
import org.ns.negation.common.NegationGlobal;
import org.ns.negation.common.NegationInstance;
import org.ns.negation.common.Utils;
import org.ns.negation.common.NegationCompiler.NodeType;

public class NegationScopeSemi4Compiler extends NegationCompiler {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5689984204595365160L;

	public NegationScopeSemi4Compiler() {
		super();
		NetworkIDMapper.setCapacity(new int[] { NegationGlobal.MAX_SENTENCE_LENGTH, 20, NegationGlobal.MAX_NUM_SPAN + 1, NodeTypeSize});
	}
	
	protected long toNode_Root(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size + 1, 0, 0, NodeType.Root.ordinal() });
	}

	protected long toNode_Node(int size, int pos, int tag_id, int numSpan) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size - pos, tag_id, NegationGlobal.MAX_NUM_SPAN - numSpan, NodeType.Node.ordinal()});
	}

	protected long toNode_X(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] { 0, 0, 0, NodeType.X.ordinal() });
	}
	
	BaseNetwork[] labelnetworks = new BaseNetwork[100];
	BaseNetwork[] unlabelnetworks = new BaseNetwork[100];
	

	@Override
	public NegationInstance decompile(Network network) {
		BaseNetwork lcrfNetwork = (BaseNetwork) network;
		NegationInstance inst = (NegationInstance) network.getInstance();
		int size = inst.size();
		// List<String[]> inputs = (List<String[]>) inst.getInput();

		

		long rootNode = toNode_Root(size);
		int node_k = Arrays.binarySearch(lcrfNetwork.getAllNodes(), rootNode);
		
		int[] span = new int[size];
		Arrays.fill(span, 0);
		
		// node_k = 0;
		while (true) {
			// System.out.print(node_k + " ");
			int[] parent_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(node_k));
			int parent_pos = size - parent_ids[0];
			int parent_tag_id = parent_ids[1];
			int parent_num_span = NegationGlobal.MAX_NUM_SPAN - parent_ids[2];
			int parent_node_type = parent_ids[3];
			
			int nextNode = -1;

			if (parent_node_type == NodeType.X.ordinal()) {
				break;
			}

			int[] childs = network.getMaxPath(node_k);

			int[] child_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(childs[0]));
			
			int child_pos = size - child_ids[0];
			int child_tag_id = child_ids[1];
			int child_num_span = NegationGlobal.MAX_NUM_SPAN - child_ids[2];
			int child_node_type = child_ids[3];
			

			if (parent_node_type == NodeType.Root.ordinal()) {
				
				if (child_node_type == NodeType.X.ordinal()) {
					break;
				} else {
					nextNode = childs[0];
				}

			} else if (parent_node_type == NodeType.Node.ordinal()) {

				if (parent_num_span == 1) {
					
					for(int i = parent_pos; i < child_pos; i++) {
						span[i] = 1;
					}
					
					break;
				}
				
				if (parent_num_span >= 2) {
					
					if (parent_tag_id > 0) {
						
						for(int i = parent_pos; i < child_pos; i++) {
							span[i] = 1;
						}
						
						nextNode = childs[1];
						
					} else {
						nextNode = childs[0];
					}
					
				}

			}

			node_k = nextNode;

		}

		
		ArrayList<Label> predication_array = new ArrayList<Label>();
		
		for(int i = 0; i < size; i++) {
			Label label = this._labelsMap.get(span[i]);
			predication_array.add(label);
		}
		
		inst.setPrediction(predication_array);
		
		return inst;
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

		long[][][] node_array = new long[size][NegationGlobal.MAX_NUM_SPAN + 1][this.LABELS.length];

		// build node array
		for (int pos = 0; pos < size; pos++) {
			for(int numSpan = 1; numSpan <= NegationGlobal.MAX_NUM_SPAN ; numSpan++)
			for (int tag_id = 0; tag_id < this._labels.length; tag_id++) {
				long node = this.toNode_Node(size, pos, tag_id, numSpan);
				lcrfNetwork.addNode(node);
				node_array[pos][numSpan][tag_id] = node;
			}

		}

		long X = this.toNode_X(inst.size());
		lcrfNetwork.addNode(X);

		// ///////
		
		int[] tag_id_set = new int[]{0, 1, 2, 3};
		int[] last_tag_id_set = new int[]{0, 1, 2, 3};
		
		
		long from = start;
		long to = -1;
		int numSpan = -1;
		
		//***if (numSpan == 0)
		numSpan = 0;
		lcrfNetwork.addEdge(start, new long[] {X});
		
		
		//***else if (numSpan== 1) {
		numSpan = 1;
		from = start;
		to = node_array[0][numSpan][0];
		lcrfNetwork.addEdge(from, new long[] { to }); //first O
		
		from = to;
		for(int i = 1; i <= NegationGlobal.G0_1 && i < size; i++) {
			to = node_array[0 + i][numSpan][1];
			lcrfNetwork.addEdge(from, new long[] { to }); //span1 begin
			
			for(int j = 1; j <= NegationGlobal.L1_1 && i + j <= size; j++) {
				long to1 = (i + j >= size) ? X : node_array[i + j][numSpan][0];
				lcrfNetwork.addEdge(to, new long[] { to1, X });
			}
		}
		
	
			//span1 begin at pos = 0
		from = start;
		to = node_array[0][numSpan][1];
		lcrfNetwork.addEdge(from, new long[] { to }); //first O
		
		from = to;
		for(int j = 1; j <= NegationGlobal.L1_1; j++) {
			long to1 = (0 + j >= size) ? X : node_array[0 + j][numSpan][0];
			lcrfNetwork.addEdge(to, new long[] { to1, X });
		}
		
		
		
		//***else if (numSpan== 2) {
		numSpan = 2;
		from = start;
		to = node_array[0][numSpan][0];
		lcrfNetwork.addEdge(start, new long[] { to }); //first O
		
		to = node_array[0][numSpan][1];
		lcrfNetwork.addEdge(start, new long[] { to }); //first I1
		
		from = node_array[0][numSpan][0]; //O
		for(int i = 1; i < NegationGlobal.G0_2 && i < size; i++) {
			to = node_array[i][numSpan][1];
			lcrfNetwork.addEdge(from, new long[] { to }); //first I1
		}
		
		
		for(int i = 0; i < NegationGlobal.G0_2 && i < size; i++) {
			from = node_array[i][numSpan][1];
			
			for(int j = 1; j < NegationGlobal.L1_2 && i + j < size; j++) {
				
				long to1 = node_array[i + j][numSpan][0];
				
				for(int k = 1;  k < NegationGlobal.G1_2 && i + j + k < size - 1; k++) {
					
					long to2 = node_array[i + j + k][numSpan][2];
					lcrfNetwork.addEdge(from, new long[] { to1, to2 });  //second I2
				}
				
			}
		}
		
		
		
		for(int i = 1; i < NegationGlobal.G0_2 + NegationGlobal.L1_2 + NegationGlobal.G1_2 && i < size - 1; i++) {
			from = node_array[i][numSpan][2];
			
			for(int j = 1; j < NegationGlobal.L2_2 && i + j <= size; j++) {
				to = (i + j) >= size ? X : node_array[i + j][numSpan][0];
				lcrfNetwork.addEdge(from, new long[] { to, X });  //second I2
			}
		}
		
		
		
		
		
		//***else if (numSpan== 3) {
		
		from = start;
		to = node_array[0][numSpan][0];
		lcrfNetwork.addEdge(start, new long[] { to }); //first O
		
		to = node_array[0][numSpan][1];
		lcrfNetwork.addEdge(start, new long[] { to }); //first I1
		
		from = node_array[0][numSpan][0]; //O
		for(int i = 1; i < NegationGlobal.G0_3 && i < size - 2; i++) {
			to = node_array[i][numSpan][1];
			lcrfNetwork.addEdge(from, new long[] { to }); //first I1
		}
		
		
		for(int i = 0; i < NegationGlobal.G0_3 && i < size - 2; i++) {
			from = node_array[i][numSpan][1];
			
			for(int j = 1; j < NegationGlobal.L1_3 && i + j < size - 2; j++) {
				
				long to1 = node_array[i + j][numSpan][0];
				
				for(int k = 1;  k < NegationGlobal.G1_3 && i + j + k < size - 2; k++) {
					
					long to2 = node_array[i + j + k][numSpan][2];
					lcrfNetwork.addEdge(from, new long[] { to1, to2 });  //second I2
				}
				
			}
		}
		
		
		
		for(int i = 1; i < NegationGlobal.G0_3 + NegationGlobal.L1_3 + NegationGlobal.G1_3 + NegationGlobal.L2_3 + NegationGlobal.G2_3 && i < size - 1; i++) {
			from = node_array[i][numSpan][2];
			
			for(int j = 1; j < NegationGlobal.L2_3 && i + j < size - 1; j++) {
				long to1 = node_array[i + j][numSpan][0];
				
				for(int k = 1; k < NegationGlobal.G2_3 && i + j + k < size; k++) {
					long to2 = node_array[i + j + k][numSpan][3];
					lcrfNetwork.addEdge(from, new long[] { to1, to2});  //second I3
				}
				
			}
		}
		
		
		for(int i = 2; i < size; i++) {
			from = node_array[i][numSpan][3];
			
			for(int j = 1; j < NegationGlobal.L3_3 && i + j <= size; j++) {
				long to1 = (i + j >= size) ? X : node_array[i + j][numSpan][3];
				lcrfNetwork.addEdge(from, new long[] { to1, X});  //
			}
		}
		
		
		
		
		
		

		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		unlabelnetworks[-inst.getInstanceId() ] = network;
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

		long[][][] node_array = new long[size][NegationGlobal.MAX_NUM_SPAN + 1][this.LABELS.length];

		// build node array
		for (int pos = 0; pos < size; pos++) {
			for(int numSpan = 1; numSpan <= NegationGlobal.MAX_NUM_SPAN ; numSpan++)
			for (int tag_id = 0; tag_id < this._labels.length; tag_id++) {
				long node = this.toNode_Node(size, pos, tag_id, numSpan);
				lcrfNetwork.addNode(node);
				node_array[pos][numSpan][tag_id] = node;
			}

		}

		long X = this.toNode_X(inst.size());
		lcrfNetwork.addNode(X);

		// ///////
		long from = start;
		long to = -1;
		
		ArrayList<int[]> spans = Utils.getAllSpans(inst.negation.span, 1);
		
		int numSpan = spans.size();
		
		if (numSpan == 0) {
			lcrfNetwork.addEdge(from, new long[] {X});
		}
		else if (numSpan== 1) {
			
			int[] span = spans.get(0); //first span
			int beginIdx = span[0];
			int endIdx = span[1]; //exclusive
			
			if (beginIdx == 0) {
				to = node_array[beginIdx][numSpan][1];
				lcrfNetwork.addEdge(from, new long[] { to });
				
			} else {
				to = node_array[0][numSpan][0];
				lcrfNetwork.addEdge(from, new long[] { to });
				
				from = to;
				to = node_array[beginIdx][numSpan][1];
				lcrfNetwork.addEdge(from, new long[] { to });
				
			}
			
			from = to;
			long to1 = (endIdx >= size) ? X : node_array[endIdx][numSpan][0];
			long to2 = X;
			lcrfNetwork.addEdge(from, new long[] { to1, to2 });
			
			
		} else if (numSpan == 2) {
			
			int[] span1 = spans.get(0); //first span
			int[] span2 = spans.get(1); //second span
			
			int beginIdx1 = span1[0];
			int endIdx1 = span1[1]; //exclusive
			
			if (beginIdx1 == 0) {
				to = node_array[beginIdx1][numSpan][1];
				lcrfNetwork.addEdge(from, new long[] { to });
				
			} else {
				to = node_array[0][numSpan][0];
				lcrfNetwork.addEdge(from, new long[] { to });
				
				from = to;
				to = node_array[beginIdx1][numSpan][1];
				lcrfNetwork.addEdge(from, new long[] { to });
				
			}
			
			
			
			int beginIdx2 = span2[0];
			int endIdx2 = span2[1]; //exclusive
			
			
			from = to;
			long to1 = node_array[endIdx1][numSpan][0];
			long to2 = node_array[beginIdx2][numSpan][2];
			lcrfNetwork.addEdge(from, new long[] { to1, to2 });
			
			
			from = to2;
			to1 = (endIdx2 >= size) ? X : node_array[endIdx2][numSpan][0];
			to2 = X;
			lcrfNetwork.addEdge(from, new long[] { to1, to2 });
			
			
			
		} else if (numSpan == 3) {
			
			
			int[] span1 = spans.get(0); //first span
			int[] span2 = spans.get(1); //second span
			int[] span3 = spans.get(2); //thrid span
			
			int beginIdx1 = span1[0];
			int endIdx1 = span1[1]; //exclusive
			
			if (beginIdx1 == 0) {
				to = node_array[beginIdx1][numSpan][1];
				lcrfNetwork.addEdge(from, new long[] { to });
				
			} else {
				to = node_array[0][numSpan][0];
				lcrfNetwork.addEdge(from, new long[] { to });
				
				from = to;
				to = node_array[beginIdx1][numSpan][1];
				lcrfNetwork.addEdge(from, new long[] { to });
				
			}
			
			
			
			int beginIdx2 = span2[0];
			int endIdx2 = span2[1]; //exclusive
			
			
			from = to;
			long to1 = node_array[endIdx1][numSpan][0];
			long to2 = node_array[beginIdx2][numSpan][2];
			lcrfNetwork.addEdge(from, new long[] { to1, to2 });
			
			
			int beginIdx3 = span3[0];
			int endIdx3 = span3[1]; //exclusive
			
			
			from = to2;
			to1 = node_array[endIdx2][numSpan][0];
			to2 = node_array[beginIdx3][numSpan][3];
			lcrfNetwork.addEdge(from, new long[] { to1, to2 });
			
			
			from = to2;
			to1 = endIdx3 >= size ? X : node_array[endIdx3][numSpan][0];
			to2 = X;
			lcrfNetwork.addEdge(from, new long[] { to1, to2 });
			
			
			
		} else {
			System.err.println("spans.size() >= 3");
			System.exit(-1);
		}

		

		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		labelnetworks[inst.getInstanceId()] = network;
		System.out.println(unlabelnetworks[inst.getInstanceId()].contains(network));
		return network;

	}

	@Override
	public Label[] getLabels() {
		String[] labelForms = new String[] { "O", "I1", "I2", "I3" };

		Label[] labels = new Label[labelForms.length];
		for (int i = 0; i < labels.length; i++)
			labels[i] = new Label(labelForms[i], i);

		return labels;
	}

	/*
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

			if (lable_id >= 1)
				if (i < l_cue_pos) {
					lable_id = 1;
				} else if (i > r_cue_pos) {
					lable_id = 2;
				} else {
					lable_id = 3;
				}
			
			if (cues[i] == 1) {
				if (lable_id == 0) {
					lable_id = 4;
				} else if (lable_id == 3) {
					lable_id = 5;
				} else {
					System.err.println("NOOOO!!");
					System.exit(-1);
				}
			}

			output.add(this._labelsMap.get(lable_id));

		}

		return output;
	}*/

}
