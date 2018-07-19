package org.ns.negation.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.ns.commons.types.Instance;
import org.ns.commons.types.Label;
import org.ns.example.base.BaseNetwork;
import org.ns.example.base.BaseNetwork.NetworkBuilder;
import org.ns.hypergraph.LocalNetworkParam;
import org.ns.hypergraph.Network;
import org.ns.hypergraph.NetworkCompiler;
import org.ns.hypergraph.NetworkIDMapper;
import org.ns.negation.common.NegationGlobal;
import org.ns.negation.common.NegationInstance;


public abstract class NegationCompiler extends NetworkCompiler {

	public Map<Integer, Label> _labelsMap;
	public Label[] _labels;
	
	public static Label[] LABELS;
	
	
	
	public NegationCompiler() {
		super(null);
		NetworkIDMapper.setCapacity(new int[] { NegationGlobal.MAX_SENTENCE_LENGTH, 20, NodeTypeSize});
		
		_labels = this.getLabels();
		LABELS = _labels;
		this._labelsMap = new HashMap<Integer, Label>();
		for(Label label: _labels){
			this._labelsMap.put(label.getId(), new Label(label));
		}
	}


	/**
	 * 
	 */
	private static final long serialVersionUID = 2100499563741744475L;

	public enum NodeType {
		X, Node, Root
	};


	public int NodeTypeSize = NodeType.values().length;
	
	public Label[] getLabels() {
		String[] labelForms = new String[]{"O", "I"};
		
		Label[] labels = new Label[labelForms.length];
		for(int i = 0; i < labels.length; i++)
			labels[i] = new Label(labelForms[i], i);
		
		return labels;
	}
	
	public ArrayList<Label> convert2Output(NegationInstance inst) {
		ArrayList<Label> output = new ArrayList<Label>();
		for(int i = 0; i < inst.size(); i++) {
			output.add(this._labelsMap.get(inst.negation.span[i]));
		}
		
		return output;
	}
	

	protected long toNode_Root(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size + 1, 0, NodeType.Root.ordinal() });
	}

	protected long toNode_Node(int size, int pos, int tag_id) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size - pos, tag_id, NodeType.Node.ordinal()});
	}
	
	protected long toNode_O(int size, int pos) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size - pos, 0, NodeType.Node.ordinal()});
	}
	
	protected long toNode_I(int size, int pos) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size - pos, 1, NodeType.Node.ordinal()});
	}

	protected long toNode_X(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] { 0, 0, NodeType.X.ordinal() });
	}


}
