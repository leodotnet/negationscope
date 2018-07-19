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

public class NegationScopeSemiCompactLatent4Compiler extends NegationCompiler {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5689984204595365160L;
	
	int max_latent_number = -1;

	public NegationScopeSemiCompactLatent4Compiler() {
		super();
		max_latent_number = NegationGlobal.MAX_LATENT_NUMBER;
		NetworkIDMapper.setCapacity(new int[] { NegationGlobal.MAX_SENTENCE_LENGTH, 1,  myNodeTypeSize, max_latent_number + 1});
	}
	
	public enum NodeType {
		Y, X, Node, O, I3, I2, I1, Root
	};
	
	int myNodeTypeSize = NodeType.values().length;
	
	protected long toNode_Root() {
		return NetworkIDMapper.toHybridNodeID(new int[] { 1000, 0, NodeType.Root.ordinal(), 0 });
	}
	
	
	protected long toNode_O(int pos) {
		return NetworkIDMapper.toHybridNodeID(new int[] { 1000 - pos, 0, NodeType.O.ordinal(), 0});
	}
	
	protected long toNode_I1(int pos, int latentIdx) {
		return NetworkIDMapper.toHybridNodeID(new int[] { 1000 - pos, 0, NodeType.I1.ordinal(), latentIdx});
	}
	
	protected long toNode_I2(int pos,  int latentIdx) {
		return NetworkIDMapper.toHybridNodeID(new int[] { 1000 - pos, 0, NodeType.I2.ordinal(), latentIdx});
	}
	
	protected long toNode_I3(int pos,  int latentIdx) {
		return NetworkIDMapper.toHybridNodeID(new int[] { 1000 - pos, 0, NodeType.I3.ordinal(), latentIdx});
	}
/*
	protected long toNode_Node(int size, int pos, int tag_id, int numSpan) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size - pos, tag_id, NegationGlobal.MAX_NUM_SPAN - numSpan, NodeType.Node.ordinal()});
	}
*/
	protected long toNode_X(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] { 1000 - size, 0, NodeType.X.ordinal(), 0 });
	}

	protected long toNode_Y(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] { 0, 0, NodeType.Y.ordinal(), 0 });
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
			int nodetype_parent = parent_ids[2];
			
			if (nodetype_parent == NodeType.X.ordinal()) {
				break;
			} 
			
			int[] childs = network.getMaxPath(node_k);

			int[] child_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(childs[0]));
			
			int pos_child = 1000 - child_ids[0];
			int nodetype_child = child_ids[2];
			
			int nextNode = -1;
			
			if (nodetype_child == NodeType.Y.ordinal()) {
				break;
			} 
			
			{
				
				
				long node_child2 = network.getNode(childs[1]);
				int[] ids_child2 = NetworkIDMapper.toHybridNodeArray(node_child2);
				
				int pos_child2 = 1000 - ids_child2[0];
				int nodetype_child2 = ids_child2[2];
				
				nextNode = childs[1];
				
				//edge: Root O X
				if (nodetype_parent == NodeType.Root.ordinal() && nodetype_child == NodeType.O.ordinal() && nodetype_child2 == NodeType.X.ordinal()) {
					break;
				}
				//edge: Root O I1
				else if (nodetype_parent == NodeType.Root.ordinal() && nodetype_child == NodeType.O.ordinal() && nodetype_child2 == NodeType.I1.ordinal()) {
					
				} 
				//edge: I1 O X
				else if (nodetype_parent == NodeType.I1.ordinal() && nodetype_child == NodeType.O.ordinal() && nodetype_child2 == NodeType.X.ordinal()) {
					
					for(int i = pos_parent; i < pos_child; i++) {
						span[i] = 1;
					}
					
				}
				//edge: I1 O I2
				else if (nodetype_parent == NodeType.I1.ordinal() && nodetype_child == NodeType.O.ordinal() && nodetype_child2 == NodeType.I2.ordinal()) {
					
					for(int i = pos_parent; i < pos_child; i++) {
						span[i] = 1;
					}
					
					
				}
				//edge: I2 O X
				else if (nodetype_parent == NodeType.I2.ordinal() && nodetype_child == NodeType.O.ordinal() && nodetype_child2 == NodeType.X.ordinal()) {
					for(int i = pos_parent; i < pos_child; i++) {
						span[i] = 1;
					}
					
				}
				//edge: I2 O I3
				else if (nodetype_parent == NodeType.I2.ordinal() && nodetype_child == NodeType.O.ordinal() && nodetype_child2 == NodeType.I3.ordinal()) {
					for(int i = pos_parent; i < pos_child; i++) {
						span[i] = 1;
					}
					
				}
				//edge: I3 O X
				else if (nodetype_parent == NodeType.I3.ordinal() && nodetype_child == NodeType.O.ordinal() && nodetype_child2 == NodeType.X.ordinal()) {
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
			for(int latentIdx  = 0; latentIdx < max_latent_number; latentIdx++) {
				long node_I1 = this.toNode_I1(k, latentIdx);
				nsnetwork.addNode(node_I1);
				long node_I2 = this.toNode_I2(k, latentIdx);
				nsnetwork.addNode(node_I2);
				long node_I3 = this.toNode_I3(k, latentIdx);
				nsnetwork.addNode(node_I3);
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
				for(int latentIdx  = 0; latentIdx < max_latent_number; latentIdx++) {
					long node_I1 = this.toNode_I1(M, latentIdx);
					nsnetwork.addEdge(root, new long[] {node_O, node_I1});
				}
			}
			nsnetwork.addEdge(root, new long[] {node_O, node_X});
		}
		
		//edges for I1
		for(int pos = 0; pos<size; pos++) {
			
			for(int latentIdx  = 0; latentIdx < max_latent_number; latentIdx++) {
				long node_I1 = this.toNode_I1(pos, latentIdx);
				
				for(int L = 1; L+pos < size && L< L_max; L++) {
					long node_O = this.toNode_O(pos+L);
					for(int M = 1; L + M + pos < size && M < M_max; M++) {
						for(int latentNextIdx  = 0; latentNextIdx < max_latent_number; latentNextIdx++) {
							long node_I2 = this.toNode_I2(pos+L+M, latentNextIdx);
							nsnetwork.addEdge(node_I1, new long[] {node_O, node_I2});
						}
					}
					nsnetwork.addEdge(node_I1, new long[] {node_O, node_X});
				}
			}
		}
		
		//edges for I2
		for(int pos = 0; pos<size; pos++) {
			for(int latentIdx  = 0; latentIdx < max_latent_number; latentIdx++) {
				long node_I2 = this.toNode_I2(pos, latentIdx);
				
				for(int L = 1; L+pos < size && L< L_max; L++) {
					long node_O = this.toNode_O(pos+L);
					for(int M = 1; L + M + pos < size && M < M_max; M++) {
						for(int latentNextIdx  = 0; latentNextIdx < max_latent_number; latentNextIdx++) {
							long node_I3 = this.toNode_I3(pos+L+M, latentNextIdx);
							nsnetwork.addEdge(node_I2, new long[] {node_O, node_I3});
						}
					}
					nsnetwork.addEdge(node_I2, new long[] {node_O, node_X});
				}
			}
		}

		//edges for I3
		for(int pos = 0; pos<size; pos++) {
			for(int latentIdx  = 0; latentIdx < max_latent_number; latentIdx++) {
				long node_I3 = this.toNode_I3(pos, latentIdx);
				
				for(int L = 1; L+pos < size && L< L_max; L++) {
					long node_O = this.toNode_O(pos+L);
					nsnetwork.addEdge(node_I3, new long[] {node_O, node_X});
			}
			}
		}
		

		for(int k = 0; k<size; k++) {
			for(int latentIdx  = 0; latentIdx < max_latent_number; latentIdx++) {
				long node_I1 = this.toNode_I1(k, latentIdx);
				long node_I2 = this.toNode_I2(k, latentIdx);
				long node_I3 = this.toNode_I3(k, latentIdx);
				
				if(nsnetwork.getChildren_tmp(node_I1)==null) {
					nsnetwork.addEdge(node_I1, new long[] {node_Y});
				}
				
				if(nsnetwork.getChildren_tmp(node_I2)==null) {
					nsnetwork.addEdge(node_I2, new long[] {node_Y});
				}
				
				if(nsnetwork.getChildren_tmp(node_I3)==null) {
					nsnetwork.addEdge(node_I3, new long[] {node_Y});
				}
			}
			
		}

		

		BaseNetwork network = nsnetwork.build(networkId, inst, param, this);
		//if (NetworkConfig.STATUS == ModelStatus.TRAINING)
		//	unlabelnetworks[-inst.getInstanceId() ] = network;
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
			for(int latentIdx  = 0; latentIdx < max_latent_number; latentIdx++) {
				long node_I1 = this.toNode_I1(k, latentIdx);
				nsnetwork.addNode(node_I1);
				long node_I2 = this.toNode_I2(k, latentIdx);
				nsnetwork.addNode(node_I2);
				long node_I3 = this.toNode_I3(k, latentIdx);
				nsnetwork.addNode(node_I3);
			}
			
		}

		long node_X = this.toNode_X(size);
		nsnetwork.addNode(node_X);
		
		long node_Y = this.toNode_Y(size);
		nsnetwork.addNode(node_Y);

		
		
		ArrayList<int[]> spans = Utils.getAllSpans(inst.negation.span, 1);
		
		int numSpan = spans.size();
		
		if (numSpan == 0) {
			long node_O = this.toNode_O(0);
			nsnetwork.addEdge(root, new long[] {node_O, node_X});
		}
		else if (numSpan== 1) {
			
			int[] span = spans.get(0); //first span
			int beginIdx = span[0];
			int endIdx = span[1]; //exclusive
			
			for(int latentIdx  = 0; latentIdx < max_latent_number; latentIdx++) {
				long node_I1 = this.toNode_I1(beginIdx, latentIdx);
				nsnetwork.addEdge(root, new long[] {this.toNode_O(0), node_I1});
				long node_O = this.toNode_O(endIdx);
				nsnetwork.addEdge(node_I1, new long[] {node_O, node_X});
			}
			
		} else if (numSpan == 2) {
			
			int[] span1 = spans.get(0); //first span
			int beginIdx1 = span1[0];
			int endIdx1 = span1[1]; //exclusive
			
			int[] span2 = spans.get(1); //second span
			int beginIdx2 = span2[0];
			int endIdx2 = span2[1]; //exclusive
			
			for(int latentIdx  = 0; latentIdx < max_latent_number; latentIdx++) {
				long node_I1 = this.toNode_I1(beginIdx1, latentIdx);
				nsnetwork.addEdge(root, new long[] {this.toNode_O(0), node_I1});
			}
			
			
			long node_O = this.toNode_O(endIdx1);
			
			for(int latentIdx  = 0; latentIdx < max_latent_number; latentIdx++) {
				for(int latentNextIdx  = 0; latentNextIdx < max_latent_number; latentNextIdx++) {
					long node_I1 = this.toNode_I1(beginIdx1, latentIdx);
					long node_I2 = this.toNode_I2(beginIdx2, latentNextIdx);
					nsnetwork.addEdge(node_I1, new long[] {node_O, node_I2});
				}
			}
			
			node_O = this.toNode_O(endIdx2);
			for(int latentIdx  = 0; latentIdx < max_latent_number; latentIdx++) {
				long node_I2 = this.toNode_I2(beginIdx2, latentIdx);
				nsnetwork.addEdge(node_I2, new long[] {node_O, node_X});
			}
			
			
		} else if (numSpan == 3) {
			
			
			int[] span1 = spans.get(0); //first span
			int beginIdx1 = span1[0];
			int endIdx1 = span1[1]; //exclusive
			
			int[] span2 = spans.get(1); //second span
			int beginIdx2 = span2[0];
			int endIdx2 = span2[1]; //exclusive
			
			int[] span3 = spans.get(2); //third span
			int beginIdx3 = span3[0];
			int endIdx3 = span3[1]; //exclusive
			
			//Root => I1
			for(int latentIdx  = 0; latentIdx < max_latent_number; latentIdx++) {
				long node_I1 = this.toNode_I1(beginIdx1, latentIdx);
				nsnetwork.addEdge(root, new long[] {this.toNode_O(0), node_I1});
			}
			long node_O = this.toNode_O(endIdx1);
			
			for(int latentIdx  = 0; latentIdx < max_latent_number; latentIdx++) {
				for(int latentNextIdx  = 0; latentNextIdx < max_latent_number; latentNextIdx++) {
					long node_I1 = this.toNode_I1(beginIdx1, latentIdx);
					long node_I2 = this.toNode_I2(beginIdx2, latentNextIdx );
					nsnetwork.addEdge(node_I1, new long[] {node_O, node_I2});
				}
			}
			
			//I1 => I2
			for(int latentIdx  = 0; latentIdx < max_latent_number; latentIdx++) {
				for(int latentNextIdx  = 0; latentNextIdx < max_latent_number; latentNextIdx++) {
					long node_I2 = this.toNode_I2(beginIdx2, latentIdx );
					long node_I3 = this.toNode_I3(beginIdx3, latentNextIdx);
					node_O = this.toNode_O(endIdx2);
					nsnetwork.addEdge(node_I2, new long[] {node_O, node_I3});
				}
			}
			
			//I2 => I3
			node_O = this.toNode_O(endIdx3);
			for(int latentIdx  = 0; latentIdx < max_latent_number; latentIdx++) {
				long node_I3 = this.toNode_I3(beginIdx3, latentIdx);
				nsnetwork.addEdge(node_I3, new long[] {node_O, node_X});
			}
			
			
			
		} else {
			System.err.println("spans.size() >= " + max_latent_number);
			System.exit(-1);
		}

		

		BaseNetwork network = nsnetwork.build(networkId, inst, param, this);
		//labelnetworks[inst.getInstanceId()] = network;
		//System.out.println(unlabelnetworks[inst.getInstanceId()].contains(network));
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


}
