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
import org.ns.hypergraph.NetworkConfig.ModelStatus;
import org.ns.negation.common.NegationCompiler;
import org.ns.negation.common.NegationGlobal;
import org.ns.negation.common.NegationInstance;
import org.ns.negation.common.Utils;
import org.ns.negation.common.NegationCompiler.NodeType;

public class NegationScopeSemiCompactLatentNCompiler extends NegationCompiler {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5689984204595365160L;

	public NegationScopeSemiCompactLatentNCompiler() {
		super();
		NetworkIDMapper.setCapacity(new int[] { NegationGlobal.MAX_SENTENCE_LENGTH, 150, 150, myNodeTypeSize});
	}
	
	public enum NodeType {
		Y, X, Node, O, I, Root
	};
	
	int myNodeTypeSize = NodeType.values().length;
	
	protected long toNode_Root() {
		return NetworkIDMapper.toHybridNodeID(new int[] { 1000, 150, 0, NodeType.Root.ordinal() });
	}
	
	
	protected long toNode_O(int pos) {
		return NetworkIDMapper.toHybridNodeID(new int[] { 1000 - pos, 0, 0, NodeType.O.ordinal()});
	}
	
	protected long toNode_I(int pos, int spanIdx, int latentIdx) {
		return NetworkIDMapper.toHybridNodeID(new int[] { 1000 - pos, 100 - spanIdx, 100 - latentIdx, NodeType.I.ordinal()});
	}
	
/*
	protected long toNode_Node(int size, int pos, int tag_id, int numSpan) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size - pos, tag_id, NegationGlobal.MAX_NUM_SPAN - numSpan, NodeType.Node.ordinal()});
	}
*/
	protected long toNode_X(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] { 1000 - size, 0, 0, NodeType.X.ordinal() });
	}

	protected long toNode_Y(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] { 0, 0, 0, NodeType.Y.ordinal() });
	}
	
	

	@Override
	public NegationInstance decompile(Network network) {
		BaseNetwork msnetwork = (BaseNetwork) network;
		NegationInstance inst = (NegationInstance) network.getInstance();
		int size = inst.size();
		// List<String[]> inputs = (List<String[]>) inst.getInput();

		if (!inst.hasNegation)
			return inst;

		long root = toNode_Root();
		int node_k = Arrays.binarySearch(msnetwork.getAllNodes(), root);
		
		int[] span = new int[size];
		Arrays.fill(span, 0);
		
		// node_k = 0;
		while (true) {
			// System.out.print(node_k + " ");
			int[] parent_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(node_k));
			int pos_parent = 1000 - parent_ids[0];
			int span_parent = 100 - parent_ids[1];
			int latentIdx_parent = 100 - parent_ids[2];
			int nodetype_parent = parent_ids[3];
			
			if (nodetype_parent == NodeType.X.ordinal()) {
				break;
			} 
			
			int[] childs = network.getMaxPath(node_k);

			int[] child_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(childs[0]));
			
			int pos_child = 1000 - child_ids[0];
			int span_child = 100 - child_ids[1];
			int latentIdx_child = 100 - child_ids[2];
			int nodetype_child = child_ids[3];
			
			int nextNode = -1;
			
			if (nodetype_child == NodeType.Y.ordinal()) {
				break;
			} 
			
			{
				
				
				long node_child2 = network.getNode(childs[1]);
				int[] ids_child2 = NetworkIDMapper.toHybridNodeArray(node_child2);
				
				int pos_child2 = 1000 - ids_child2[0];
				int span_child2 = 100 - ids_child2[1];
				int latentIdx_child2 = 100 - ids_child2[2];
				int nodetype_child2 = ids_child2[3];
				
				nextNode = childs[1];
				
				//edge: Root O X
				if (nodetype_parent == NodeType.Root.ordinal() && nodetype_child == NodeType.O.ordinal() && nodetype_child2 == NodeType.X.ordinal()) {
					break;
				}
				//edge: Root O I
				else if (nodetype_parent == NodeType.Root.ordinal() && nodetype_child == NodeType.O.ordinal() && nodetype_child2 == NodeType.I.ordinal()) {
					
				} 
				//edge: I O X
				else if (nodetype_parent == NodeType.I.ordinal() && nodetype_child == NodeType.O.ordinal() && nodetype_child2 == NodeType.X.ordinal()) {
					
					for(int i = pos_parent; i < pos_child; i++) {
						span[i] = 1;
					}
					
				}
				//edge: I O I
				else if (nodetype_parent == NodeType.I.ordinal() && nodetype_child == NodeType.O.ordinal() && nodetype_child2 == NodeType.I.ordinal()) {
					
					for(int i = pos_parent; i < pos_child; i++) {
						span[i] = 1;
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
		NetworkBuilder<BaseNetwork> nsnetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		
		if (inst.getOutput() == null) {
			ArrayList<Label> outputs = this.convert2Output(inst);
			inst.setOutput(outputs); // the following code will not use the outputs
		}
		
		int size = inst.size();
		
		int L_max = Math.min(NegationGlobal.L_MAX, size);
		int M_max = Math.min(NegationGlobal.M_MAX, size);
		
		//adding nodes..
		
		long root = this.toNode_Root();
		nsnetwork.addNode(root);
		
		for(int k = 0; k<size; k++) {
			long node_O = this.toNode_O(k);
			nsnetwork.addNode(node_O);
			for(int s = 0; s < NegationGlobal.SPAN_MAX; s++) {
				
				for(int latentIdx = 0; latentIdx < NegationGlobal.SPAN_MAX; latentIdx++) {
					long node_I = this.toNode_I(k, s, latentIdx);
					nsnetwork.addNode(node_I);
				}
			}
		}

		long node_X = this.toNode_X(size);
		nsnetwork.addNode(node_X);
		
		long node_Y = this.toNode_Y(size);
		nsnetwork.addNode(node_Y);
		

		//adding edges..
		
		//edges for the root.
		/*
		nsnetwork.addEdge(root, new long[] {this.toNode_O(0)});
		nsnetwork.addEdge(root, new long[] {this.toNode_I1(0)});
		
		
		//edges for O
		for(int pos = 0; pos<size; pos++) {
			long node_O = this.toNode_O(pos);
			
			for(int L = 1; L+pos < size && L< L_max; L++) {
				long node_I1 = this.toNode_I1(pos+L);
				nsnetwork.addEdge(node_O, new long[] {node_I1});
			}
			nsnetwork.addEdge(node_O, new long[] {node_X});
		}*/


		//edges for I0 / root
		{
			long node_O = this.toNode_O(0);
			for(int M = 0; M<M_max && M < size; M++) {
				for(int latentIdx = 0; latentIdx < NegationGlobal.SPAN_MAX; latentIdx++) {
					long node_I = this.toNode_I(M, 0, latentIdx);
					nsnetwork.addEdge(root, new long[] {node_O, node_I});
				}
			}
			nsnetwork.addEdge(root, new long[] {node_O, node_X});
		}
		
		//edges for I
		for(int pos = 0; pos<size; pos++) {
			
			for(int s = 0; s < NegationGlobal.SPAN_MAX; s++) {
			
				for(int latentIdx = 0; latentIdx < NegationGlobal.SPAN_MAX; latentIdx++) {
					long node_I = this.toNode_I(pos, s, latentIdx);
					
					for(int L = 1; L+pos < size && L< L_max; L++) {
						long node_O = this.toNode_O(pos+L);
						
						if (s + 1 < NegationGlobal.SPAN_MAX) {
							for(int M = 1; L + M + pos < size && M < M_max; M++) {
								for(int latentNextIdx = 0; latentNextIdx < NegationGlobal.SPAN_MAX; latentNextIdx++) {
									long node_Inext = this.toNode_I(pos+L+M, s + 1, latentNextIdx);
									nsnetwork.addEdge(node_I, new long[] {node_O, node_Inext});
								}
							}
						}
						nsnetwork.addEdge(node_I, new long[] {node_O, node_X});
					}
				}
			}
		}
		
		

		for(int k = 0; k<size; k++) {
			
			for(int s = 0; s < NegationGlobal.SPAN_MAX + 1; s++) {
				for(int latentIdx = 0; latentIdx < NegationGlobal.SPAN_MAX; latentIdx++) {
					long node_I = this.toNode_I(k, s, latentIdx);
					
					if(nsnetwork.getChildren_tmp(node_I)==null) {
						nsnetwork.addEdge(node_I, new long[] {node_Y});
					}
				}
			}
		}

		

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

		long root = this.toNode_Root();
		nsnetwork.addNode(root);
		
		for(int k = 0; k<size; k++) {
			long node_O = this.toNode_O(k);
			nsnetwork.addNode(node_O);
			for(int s = 0; s < NegationGlobal.SPAN_MAX; s++) {
				
				for(int latentIdx = 0; latentIdx < NegationGlobal.SPAN_MAX; latentIdx++) {
					long node_I = this.toNode_I(k, s, latentIdx);
					nsnetwork.addNode(node_I);
				}
			}
		}

		long node_X = this.toNode_X(size);
		nsnetwork.addNode(node_X);
		
		long node_Y = this.toNode_Y(size);
		nsnetwork.addNode(node_Y);

		
		
		ArrayList<int[]> spans = Utils.getAllSpans(inst.negation.span, 1);
		
		int numSpan = spans.size();
		
		//long from = root;
		ArrayList<Long> fromSet = new ArrayList<Long>();
		fromSet.add(root);
		long node_O = this.toNode_O(0);
		
		assert (numSpan <= NegationGlobal.SPAN_MAX);
		
		for(int k = 0; k < numSpan; k++) {
			int[] span = spans.get(k);
			//long node_I = this.toNode_I(span[0], k);
			ArrayList<Long> node_I_set = new ArrayList<Long>();
			for(int latentIdx = 0; latentIdx < NegationGlobal.SPAN_MAX; latentIdx++ ) {
				node_I_set.add(this.toNode_I(span[0], k, latentIdx));
			}
			
			for(long from : fromSet) {
				for(long node_I : node_I_set) {
					nsnetwork.addEdge(from, new long[] {node_O, node_I});
				}
			}
			
			node_O = this.toNode_O(span[1]);
			//from = node_I;
			fromSet.clear();
			fromSet.addAll(node_I_set);
		}
		
		for(long from : fromSet)
			nsnetwork.addEdge(from, new long[] {node_O, node_X});
		
		

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


}
