/**
 * 
 */
package org.ns.example.base;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.ns.commons.types.LinearInstance;
import org.ns.hypergraph.FeatureArray;
import org.ns.hypergraph.FeatureManager;
import org.ns.hypergraph.GlobalNetworkParam;
import org.ns.hypergraph.Network;
import org.ns.util.Pipeline;

/**
 * A default feature extractor that is based on a template.
 */
public class TemplateBasedFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 7646305220242895824L;
	
	private static final List<String> DEFAULT_FEATURE_TEMPLATES = Arrays.asList(
			new String[]{
					"U00:%x[-2,0]",
					"U01:%x[-1,0]",
					"U02:%x[0,0]",
					"U03:%x[1,0]",
					"U04:%x[2,0]",
//					"U05:%x[-1,0]/%x[0,0]",
//					"U06:%x[0,0]/%x[1,0]",

//					"U10:%x[-2,1]",
//					"U11:%x[-1,1]",
//					"U12:%x[0,1]",
//					"U13:%x[1,1]",
//					"U14:%x[2,1]",
//					"U15:%x[-2,1]/%x[-1,1]",
//					"U16:%x[-1,1]/%x[0,1]",
//					"U17:%x[0,1]/%x[1,1]",
//					"U18:%x[1,1]/%x[2,1]",
//
//					"U20:%x[-2,1]/%x[-1,1]/%x[0,1]",
//					"U21:%x[-1,1]/%x[0,1]/%x[1,1]",
//					"U22:%x[0,1]/%x[1,1]/%x[2,1]",

					"B"
			});
	private List<String> featureTemplates;
	private Map<String, int[][]> compiledFeatureTemplates;
	
	public TemplateBasedFeatureManager(Pipeline<?> pipeline) {
		this(pipeline.param, DEFAULT_FEATURE_TEMPLATES);
	}
	
	public TemplateBasedFeatureManager(Pipeline<?> pipeline, String templateFilePath){
		this(pipeline.param, readTemplate(templateFilePath));
	}
	
	public TemplateBasedFeatureManager(Pipeline<?> pipeline, List<String> featureTemplates){
		this(pipeline.param, featureTemplates);
	}

	/**
	 * @param param_g
	 */
	public TemplateBasedFeatureManager(GlobalNetworkParam param_g) {
		this(param_g, DEFAULT_FEATURE_TEMPLATES);
	}
	
	public TemplateBasedFeatureManager(GlobalNetworkParam param_g, String templateFilePath){
		this(param_g, readTemplate(templateFilePath));
	}
	
	public TemplateBasedFeatureManager(GlobalNetworkParam param_g, List<String> featureTemplates){
		super(param_g);
		this.featureTemplates = featureTemplates;
		compileTemplate();
	}
	
	private void compileTemplate(){
		compiledFeatureTemplates = new HashMap<String, int[][]>();
		for(String featureTemplate: featureTemplates){
			compiledFeatureTemplates.put(featureTemplate, findEmissions(featureTemplate));
		}
	}
	
	private static List<String> readTemplate(String templateFilePath){
		Scanner sc = null;
		try {
			sc = new Scanner(new File(templateFilePath));
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Template file "+templateFilePath+" not found");
		}
		List<String> featureTemplates = new ArrayList<String>();
		while(sc.hasNextLine()){
			String line = sc.nextLine().trim();
			if(line.length() == 0 || line.startsWith("#")){
				continue;
			}
			featureTemplates.add(line);
		}
		sc.close();
		return featureTemplates;
	}

	/* (non-Javadoc)
	 * @see org.statnlp.hypergraph.FeatureManager#extract_helper(org.statnlp.hypergraph.Network, int, int[])
	 */
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k, int children_k_index) {
//		if (true){
//			FeatureArray fa = extract_helper2(network, parent_k, children_k);
//			System.out.println(Arrays.toString(fa.getCurrent()));
//			return fa;
//		}
		if(!LinearInstance.class.isInstance(network.getInstance())){
			throw new RuntimeException("TemplateBasedFeatureManager currently only works with LinearInstance.");
		}
		LinearInstance<?> instance = (LinearInstance<?>)network.getInstance();
		List<String[]> inputs = instance.input;
		int[] nodeArr = network.getNodeArray(parent_k);
		int pos = network.getPosForNode(nodeArr);
		GlobalNetworkParam param = this._param_g;
		List<Integer> featuresList = new ArrayList<Integer>();
		for(String featureTemplate: featureTemplates){
			String featureType = featureTemplate;
			String outputFeature = "";
			Object output = network.getOutputForNode(nodeArr);
			if(output == null){
				continue;
			}
			if(featureTemplate.charAt(0) == 'U'){
				outputFeature = output.toString();
			} else if(featureTemplate.charAt(0) == 'B'){
				Object childOutput = network.getOutputForNode(network.getNodeArray(children_k[0]));
				if(childOutput == null){
					continue;
				}
				outputFeature = childOutput.toString();
				outputFeature += "||";
				outputFeature += output.toString();
			} else {
				outputFeature = "";
			}
			String inputFeature = "";
			int[][] emissions = compiledFeatureTemplates.get(featureTemplate);
			for(int[] emission: emissions){
				if(inputFeature.length() > 0){
					inputFeature += featureTemplate;
				}
				if(pos+emission[0] >= 0 && pos+emission[0] < inputs.size()){
					inputFeature += inputs.get(pos+emission[0])[emission[1]];
				} else {
					inputFeature += "***";
				}
			}
			featuresList.add(param.toFeature(network, featureType, outputFeature, inputFeature));
		}
		return createFeatureArray(network, featuresList);
	}
	
	private int[][] findEmissions(String template){
		List<int[]> emissions = new ArrayList<int[]>();
		for(int i=0; i<template.length()-1; i++){
			if(template.substring(i, i+2).equals("%x")){
				int nextOpen = template.indexOf('[', i+1);
				int nextComma = template.indexOf(',', nextOpen+1);
				int nextClose = template.indexOf(']', nextComma+1);
				emissions.add(new int[]{Integer.parseInt(template.substring(nextOpen+1, nextComma)),
										Integer.parseInt(template.substring(nextComma+1, nextClose))});
			}
		}
		return emissions.toArray(new int[emissions.size()][]);
	}
	
}
