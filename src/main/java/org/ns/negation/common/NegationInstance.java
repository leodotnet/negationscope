package org.ns.negation.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ns.commons.types.Instance;
import org.ns.commons.types.Label;
import org.ns.commons.types.LinearInstance;
import org.ns.commons.types.Sentence;
import org.ns.example.base.BaseInstance;
import org.ns.hypergraph.NetworkConfig;
import org.ns.negation.common.NegEval;
import org.ns.negation.common.NegationGlobal;
import org.ns.negation.common.NegationInstance;
import org.ns.negation.common.Utils;
import org.ns.negation.common.NegationInstance.FEATURE_TYPES;

public class NegationInstance<T> extends LinearInstance<T> {

	private static final long serialVersionUID = 1851514046050983662L;

	public boolean hasNegation = true;
	public ArrayList<int[]> scopes = null;
	String sentence = null;
	List<T> outputBackup = null;

	public Negation[] negations = null;
	public Negation[] negationsPred = null;

	public Negation negation = null;
	public Negation negationPred = null;
	
	public int sentID = -1;

	/**
	 * Only for output convenience
	 */
	public ArrayList<Negation> negationList = new ArrayList<Negation>();

	public enum FEATURE_TYPES {
		chaptername, sent_id, token_id, word, lemma, pos_tag, syntax
	};

	public NegationInstance(int instanceId, double weight) {
		this(instanceId, weight, null);
	}

	public NegationInstance(int instanceId, double weight, ArrayList<String[]> input) {
		this(instanceId, weight, input, null, null);
	}

	public NegationInstance(int instanceId, double weight, ArrayList<String[]> input, ArrayList<T> output) {
		this(instanceId, weight, input, output, null);
	}

	public NegationInstance(int instanceId, double weight, ArrayList<String[]> input, ArrayList<T> output, ArrayList<T> prediction) {
		super(instanceId, weight);
		this.input = input;
		this.output = output;
		this.prediction = prediction;
	}

	public void setOutput(ArrayList<T> output) {
		this.output = output;
	}

	@Override
	public int size() {
		return this.input.size();
	}

	public List<String[]> duplicateInput() {
		return input;
	}

	@SuppressWarnings("unchecked")
	public ArrayList<T> duplicateOutput() {
		// ArrayList<T> o = (ArrayList<T>) this.output;
		// return (ArrayList<T>) o.clone();
		return null;
	}

	@Override
	public NegationInstance duplicate() {
		NegationInstance inst = (NegationInstance) super.duplicate();
		inst.scopes = this.scopes;
		inst.negation = this.negation;
		inst.negations = this.negations;
		inst.sentID = this.sentID;
		inst.hasNegation = this.hasNegation;
		return inst;
	}

	public void preprocess() {
		preprocess(false);
	}

	public void preprocess(boolean onlyWord) {

	}

	public String getSentence() {
		if (this.sentence == null) {
			this.sentence = "";
			for (int i = 0; i < input.size(); i++) {
				String word = input.get(i)[FEATURE_TYPES.word.ordinal()];
				if (NegationGlobal.EMBEDDING_WORD_LOWERCASE)
					word = word.toLowerCase();

				this.sentence += word + " ";
			}
		}

		this.sentence = this.sentence.trim();

		return this.sentence;
	}

	public void setScopes(ArrayList<int[]> scopes) {
		this.scopes = scopes;
	}

	public void setPredictionAsOutput() {
		this.outputBackup = this.output;
		this.output = this.prediction;
	}

	public String getSentID() {
		String[] first_token = this.input.get(0);
		return (NegationGlobal.dataSet.startsWith("cdsco")) ? first_token[0] + ":" + first_token[1] : this.sentID + "";
	}

	public Negation pred2NegationPred() {
		ArrayList<Label> pred = (ArrayList<Label>) this.getPrediction();
		this.negationPred = this.negation.clone();
		for (int i = 0; i < pred.size(); i++) {
			if (pred.get(i).getForm().startsWith("O")) {
				this.negationPred.span[i] = 0;
			} else {
				this.negationPred.span[i] = 1;
			}
		}

		return this.negationPred;
	}

	public static NegationInstance<Label>[] readData(String dataSet, String fileName, boolean withLabels, boolean isLabeled, int TRIAL, boolean discardNoNgeation) throws IOException {

		NegationInstance[] insts = null;
		if (dataSet.startsWith("cdsco") || dataSet.startsWith("simple_wiki")) {
			if (NegationGlobal.NEG_CUE_DETECTION) {
				insts = readCoNLLDataForCueDetection(fileName, withLabels, isLabeled, discardNoNgeation);
			} else {
				insts = readCoNLLData(fileName, withLabels, isLabeled, discardNoNgeation);
			}
		} else if (dataSet.startsWith("bioscope") || dataSet.startsWith("cnesp")) {
			if (NegationGlobal.NEG_CUE_DETECTION) {
				insts = readBioScopeDataForCueDetection(fileName, withLabels, isLabeled, discardNoNgeation);
			} else {
				insts = readBioScopeData(fileName, withLabels, isLabeled, discardNoNgeation);
			}
		}

		if (TRIAL > 0)
			insts = Utils.portionInstances(insts, TRIAL);

		return insts;
	}
	
	@SuppressWarnings("unchecked")
	private static NegationInstance<Label>[] readBioScopeDataForCueDetection(String fileName, boolean withLabels, boolean isLabeled, boolean discardNoNgeation) throws IOException {
		System.out.println("Read " + fileName);
		InputStreamReader isr = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
		BufferedReader br = new BufferedReader(isr);
		ArrayList<NegationInstance<Label>> result = new ArrayList<NegationInstance<Label>>();
		ArrayList<String[]> words = null;
		ArrayList<String[]> negs = null;
		ArrayList<Label> labels = null;
		int numNegationinSentence = 0;
		int numDiscardInstance = 0;
		int numNegation = 0;
		int instanceId = 1;
		int numSentence = 0;
		HashMap<Integer, Integer> entityLengthStat = new HashMap<Integer, Integer>();
		while (br.ready()) {
			if (words == null) {
				words = new ArrayList<String[]>();
			}
			if (negs == null) {
				negs = new ArrayList<String[]>();
			}
			if (withLabels && labels == null) {
				labels = new ArrayList<Label>();
			}
			String line = br.readLine().trim();
			if (line.startsWith("##")) {
				continue;
			}
			if (line.length() == 0) {

			} else {

				numSentence++;
				ArrayList<Negation> negationList = new ArrayList<Negation>();

				String[] fields = line.split("\\|\\|\\|");
				String[] tokens = fields[0].trim().split(" ");
				String[] postags = fields[1].trim().split(" ");

				assert (tokens.length == postags.length);

				int size = tokens.length;

				for (int i = 0; i < size; i++) {
					String[] features = new String[FEATURE_TYPES.values().length];
					Arrays.fill(features, null);

					features[FEATURE_TYPES.word.ordinal()] = tokens[i];
					features[FEATURE_TYPES.pos_tag.ordinal()] = postags[i];

					words.add(features);
				}

				if (fields.length > 2) {
					for (int i = 2; i < fields.length; i++) {
						String[] negInfo = fields[i].trim().split(" ");

						Negation negation = new Negation();
						int countNegCue = 0;
						ArrayList<Integer> NegCues = new ArrayList<Integer>();
						int cuePos = -1;
						int[] cue = new int[size];
						String[] cueForm = new String[size];
						int[] span = new int[size];
						String[] spanForm = new String[size];
						int[] target = new int[size];
						String[] targetForm = new String[size];

						Arrays.fill(cue, 0);
						Arrays.fill(cueForm, null);
						Arrays.fill(span, 0);
						Arrays.fill(spanForm, null);
						Arrays.fill(target, 0);
						Arrays.fill(targetForm, null);

						for (int pos = Integer.parseInt(negInfo[0]); pos < Integer.parseInt(negInfo[1]); pos++) { //if (pos >= size) continue;
							span[pos] = 1;
							spanForm[pos] = words.get(pos)[FEATURE_TYPES.word.ordinal()];
						}

						for (int j = 2; j < negInfo.length; j += 3) {
							String cueType = negInfo[j];
							//if (cueType.equals("negation")) {
								int cueFrom = Integer.parseInt(negInfo[j + 1]);
								int cueTo = Integer.parseInt(negInfo[j + 2]);

								for (int pos = cueFrom; pos < cueTo; pos++) { //if (pos >= size) continue;
									cue[pos] = 1;
									cueForm[pos] = words.get(pos)[FEATURE_TYPES.word.ordinal()];
									
									if (isLabeled) NegationGlobal.NegExpList.add(cueForm[pos]);
								}
							//}
						}

						negation.setType("negation");

						negation.setNegation(cue, cueForm, span, spanForm, target, targetForm);

						negationList.add(negation);
					}

					if (!negationList.isEmpty()) {

						for (Negation negation : negationList) {

							NegationInstance<Label> instance = new NegationInstance<Label>(instanceId, 1, words, null);

							instance.negation = negation;
							instance.sentID = numSentence;

							if (isLabeled) {
								instance.setLabeled(); // Important!
							} else {
								instance.setUnlabeled();
							}
							instanceId++;
							instance.preprocess();
							result.add(instance);

							numNegation++;

						}
					} else {

						if (discardNoNgeation) {
							numDiscardInstance++;
						} else {
							numSentence++;
							NegationInstance<Label> instance = new NegationInstance<Label>(instanceId, 1, words, null);

							int[] cue = new int[size];
							String[] cueForm = new String[size];
							int[] span = new int[size];
							String[] spanForm = new String[size];
							int[] target = new int[size];
							String[] targetForm = new String[size];

							Arrays.fill(cue, 0);
							Arrays.fill(cueForm, null);
							Arrays.fill(span, 0);
							Arrays.fill(spanForm, null);
							Arrays.fill(target, 0);
							Arrays.fill(targetForm, null);

							instance.negation = new Negation(cue, cueForm, span, spanForm, target, targetForm);
							instance.hasNegation = false;
							instance.sentID = numSentence;
							
							if (isLabeled) {
								instance.setLabeled(); // Important!
							} else {
								instance.setUnlabeled();
							}
							instanceId++;
							instance.preprocess();
							result.add(instance);
						}

					}

				} else { // no negation cue,scope found

					if (discardNoNgeation) {
						numDiscardInstance++;
					} else {
						numSentence++;
						NegationInstance<Label> instance = new NegationInstance<Label>(instanceId, 1, words, null);

						int[] cue = new int[size];
						String[] cueForm = new String[size];
						int[] span = new int[size];
						String[] spanForm = new String[size];
						int[] target = new int[size];
						String[] targetForm = new String[size];

						Arrays.fill(cue, 0);
						Arrays.fill(cueForm, null);
						Arrays.fill(span, 0);
						Arrays.fill(spanForm, null);
						Arrays.fill(target, 0);
						Arrays.fill(targetForm, null);

						instance.negation = new Negation(cue, cueForm, span, spanForm, target, targetForm);
						instance.hasNegation = false;
						instance.sentID = numSentence;
						
						if (isLabeled) {
							instance.setLabeled(); // Important!
						} else {
							instance.setUnlabeled();
						}
						instanceId++;
						instance.preprocess();
						result.add(instance);
					}

				}

				words = null;
				labels = null;
				negs = null;

			}
		}
		br.close();

		System.out.println("There are " + numNegation + " negation left in current total #inst: " + result.size() + " in " + numSentence + " sentences.");
		System.out.println(numDiscardInstance + " instances are discarded.");
		// System.out.println("Entity Length: " + entityLengthStat.toString());
		System.out.println();
		return result.toArray(new NegationInstance[result.size()]);
	}


	@SuppressWarnings("unchecked")
	private static NegationInstance<Label>[] readBioScopeData(String fileName, boolean withLabels, boolean isLabeled, boolean discardNoNgeation) throws IOException {
		System.out.println("Read " + fileName);
		InputStreamReader isr = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
		BufferedReader br = new BufferedReader(isr);
		ArrayList<NegationInstance<Label>> result = new ArrayList<NegationInstance<Label>>();
		ArrayList<String[]> words = null;
		ArrayList<String[]> negs = null;
		ArrayList<Label> labels = null;
		int numNegationinSentence = 0;
		int numDiscardInstance = 0;
		int numNegation = 0;
		int instanceId = 1;
		int numSentence = 0;
		int numMaxSpan = 0;
		int maxL = 0;
		int maxM = 0;
		
		HashMap<Integer, Integer> entityLengthStat = new HashMap<Integer, Integer>();
		while (br.ready()) {
			if (words == null) {
				words = new ArrayList<String[]>();
			}
			if (negs == null) {
				negs = new ArrayList<String[]>();
			}
			if (withLabels && labels == null) {
				labels = new ArrayList<Label>();
			}
			String line = br.readLine().trim();
			if (line.startsWith("##")) {
				continue;
			}
			if (line.length() == 0) {

			} else {

				numSentence++;
				ArrayList<Negation> negationList = new ArrayList<Negation>();

				String[] fields = line.split("\\|\\|\\|");
				String[] tokens = fields[0].trim().split(" ");
				String[] postags = fields[1].trim().split(" ");

				assert (tokens.length == postags.length);

				int size = tokens.length;

				for (int i = 0; i < size; i++) {
					String[] features = new String[FEATURE_TYPES.values().length];
					Arrays.fill(features, null);

					features[FEATURE_TYPES.word.ordinal()] = tokens[i];
					features[FEATURE_TYPES.pos_tag.ordinal()] = postags[i];

					words.add(features);
				}

				if (fields.length > 2) {
					for (int i = 2; i < fields.length; i++) {
						String[] negInfo = fields[i].trim().split(" ");

						Negation negation = new Negation();
						int countNegCue = 0;
						ArrayList<Integer> NegCues = new ArrayList<Integer>();
						int cuePos = -1;
						int[] cue = new int[size];
						String[] cueForm = new String[size];
						int[] span = new int[size];
						String[] spanForm = new String[size];
						int[] target = new int[size];
						String[] targetForm = new String[size];

						Arrays.fill(cue, 0);
						Arrays.fill(cueForm, null);
						Arrays.fill(span, 0);
						Arrays.fill(spanForm, null);
						Arrays.fill(target, 0);
						Arrays.fill(targetForm, null);

						for (int pos = Integer.parseInt(negInfo[0]); pos < Integer.parseInt(negInfo[1]); pos++) { //if (pos >= size) continue;
							span[pos] = 1;
							spanForm[pos] = words.get(pos)[FEATURE_TYPES.word.ordinal()];
						}

						for (int j = 2; j < negInfo.length; j += 3) {
							String cueType = negInfo[j];
							//if (cueType.equals("negation")) {
								int cueFrom = Integer.parseInt(negInfo[j + 1]);
								int cueTo = Integer.parseInt(negInfo[j + 2]);

								for (int pos = cueFrom; pos < cueTo; pos++) { //if (pos >= size) continue;
									cue[pos] = 1;
									cueForm[pos] = words.get(pos)[FEATURE_TYPES.word.ordinal()];
								}
							//}
						}

						negation.setType("negation");

						negation.setNegation(cue, cueForm, span, spanForm, target, targetForm);

						negationList.add(negation);
						
						int[] arrSpanLength = Utils.getNumSpan(span, 1);
						if (arrSpanLength.length > numMaxSpan) {
							numMaxSpan = arrSpanLength.length;
						}
						
						
						
						//if (arrSpanLength.length == 3) {
							arrSpanLength = Utils.getNumSpan(span, 1);
							//if (span[0] == 0)
							//range.add(arrSpanLength[0]);
							for(int l : arrSpanLength) {
								//range.add(l);
								//System.out.println(x);
								if (l > maxL)
									maxL = l;
							}
							
							arrSpanLength = Utils.getNumSpan(span, 0);
							//if (span[0] == 0)
							//range.add(arrSpanLength[0]);
							for(int l : arrSpanLength) {
								//range.add(l);
								//System.out.println(x);
								if (l > maxM)
									maxM = l;
							}
						//}
					}

					if (!negationList.isEmpty()) {

						for (Negation negation : negationList) {

							NegationInstance<Label> instance = new NegationInstance<Label>(instanceId, 1, words, null);

							instance.negation = negation;
							instance.sentID = numSentence;
							if (isLabeled) {
								instance.setLabeled(); // Important!
							} else {
								instance.setUnlabeled();
							}
							instanceId++;
							instance.preprocess();
							result.add(instance);

							numNegation++;

						}
					} else {

						if (discardNoNgeation) {
							numDiscardInstance++;
						} else {
							numSentence++;
							NegationInstance<Label> instance = new NegationInstance<Label>(instanceId, 1, words, null);

							int[] cue = new int[size];
							String[] cueForm = new String[size];
							int[] span = new int[size];
							String[] spanForm = new String[size];
							int[] target = new int[size];
							String[] targetForm = new String[size];

							Arrays.fill(cue, 0);
							Arrays.fill(cueForm, null);
							Arrays.fill(span, 0);
							Arrays.fill(spanForm, null);
							Arrays.fill(target, 0);
							Arrays.fill(targetForm, null);

							instance.negation = new Negation(cue, cueForm, span, spanForm, target, targetForm);
							instance.hasNegation = false;
							instance.sentID = numSentence;
							
							if (isLabeled) {
								instance.setLabeled(); // Important!
							} else {
								instance.setUnlabeled();
							}
							instanceId++;
							instance.preprocess();
							result.add(instance);
						}

					}

				} else { // no negation cue,scope found

					if (discardNoNgeation) {
						numDiscardInstance++;
					} else {
						numSentence++;
						NegationInstance<Label> instance = new NegationInstance<Label>(instanceId, 1, words, null);

						int[] cue = new int[size];
						String[] cueForm = new String[size];
						int[] span = new int[size];
						String[] spanForm = new String[size];
						int[] target = new int[size];
						String[] targetForm = new String[size];

						Arrays.fill(cue, 0);
						Arrays.fill(cueForm, null);
						Arrays.fill(span, 0);
						Arrays.fill(spanForm, null);
						Arrays.fill(target, 0);
						Arrays.fill(targetForm, null);

						instance.negation = new Negation(cue, cueForm, span, spanForm, target, targetForm);
						instance.hasNegation = false;
						instance.sentID = numSentence;
						
						if (isLabeled) {
							instance.setLabeled(); // Important!
						} else {
							instance.setUnlabeled();
						}
						instanceId++;
						instance.preprocess();
						result.add(instance);
					}

				}

				words = null;
				labels = null;
				negs = null;

			}
		}
		br.close();

		System.out.println("There are " + numNegation + " negation left in current total #inst: " + result.size() + " in " + numSentence + " sentences.");
		System.out.println(numDiscardInstance + " instances are discarded.");
		System.out.println("numMaxSpan:" + numMaxSpan + "\tmaxL:" + maxL + "\tmaxM:" + maxM );
		if (isLabeled) {
			NegationGlobal.M_MAX = maxM + 1;
			NegationGlobal.L_MAX = maxL + 1;
			System.out.println("set " + "\tmaxL:" + maxL + "\tmaxM:" + maxM);
		}
		// System.out.println("Entity Length: " + entityLengthStat.toString());
		System.out.println();
		return result.toArray(new NegationInstance[result.size()]);
	}
	
	@SuppressWarnings("unchecked")
	private static NegationInstance<Label>[] readCoNLLDataForCueDetection(String fileName, boolean withLabels, boolean isLabeled, boolean discardNoNgeation) throws IOException {
		System.out.println("Read " + fileName);
		InputStreamReader isr = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
		BufferedReader br = new BufferedReader(isr);
		ArrayList<NegationInstance<Label>> result = new ArrayList<NegationInstance<Label>>();
		ArrayList<String[]> words = null;
		ArrayList<String[]> negs = null;
		ArrayList<Label> labels = null;
		
		Utils.Counter counter = new Utils.Counter();
		int numTokens = 0;
		
		int numNegationinSentence = 0;
		int numDiscardInstance = 0;
		int numNegation = 0;
		int instanceId = 1;
		int numSentence = 0;
		int numNegSentence = 0;
		HashMap<Integer, Integer> entityLengthStat = new HashMap<Integer, Integer>();
		while (br.ready()) {
			if (words == null) {
				words = new ArrayList<String[]>();
			}
			if (negs == null) {
				negs = new ArrayList<String[]>();
			}
			if (withLabels && labels == null) {
				labels = new ArrayList<Label>();
			}
			String line = br.readLine().trim();
			if (line.startsWith("##")) {
				continue;
			}
			if (line.length() == 0) {
				if (words.size() == 0) {
					continue;
				}

				int size = words.size();

				if (negs.get(0).length == 1) {

					if (discardNoNgeation) {
						numDiscardInstance++;
					} else {
						numSentence++;
						NegationInstance<Label> instance = new NegationInstance<Label>(instanceId, 1, words, null);

						int[] cue = new int[size];
						String[] cueForm = new String[size];
						int[] span = new int[size];
						String[] spanForm = new String[size];
						int[] target = new int[size];
						String[] targetForm = new String[size];

						Arrays.fill(cue, 0);
						Arrays.fill(cueForm, null);
						Arrays.fill(span, 0);
						Arrays.fill(spanForm, null);
						Arrays.fill(target, 0);
						Arrays.fill(targetForm, null);

						instance.negation = new Negation(cue, cueForm, span, spanForm, target, targetForm);
						instance.hasNegation = false;

						;
						if (isLabeled) {
							instance.setLabeled(); // Important!
						} else {
							instance.setUnlabeled();
						}
						instanceId++;
						instance.preprocess();
						result.add(instance);
					}
				} else {
					numSentence++;
					numNegSentence++;
					ArrayList<Negation> negationList = new ArrayList<Negation>();

					for (int col = 0; col < negs.get(0).length; col += 3) {
						Negation negation = new Negation();
						int countNegCue = 0;
						ArrayList<Integer> NegCues = new ArrayList<Integer>();
						int cuePos = -1;
						int[] cue = new int[size];
						String[] cueForm = new String[size];
						int[] span = new int[size];
						String[] spanForm = new String[size];
						int[] target = new int[size];
						String[] targetForm = new String[size];

						Arrays.fill(cue, 0);
						Arrays.fill(cueForm, null);
						Arrays.fill(span, 0);
						Arrays.fill(spanForm, null);
						Arrays.fill(target, 0);
						Arrays.fill(targetForm, null);

						for (int i = 0; i < words.size(); i++) {

							if (col >= negs.get(i).length)
								continue;

							if (!negs.get(i)[col].equals("_")) {
								cue[i] = 1;
								cueForm[i] = negs.get(i)[col];
								NegationGlobal.addAffix(words.get(i)[FEATURE_TYPES.word.ordinal()].toLowerCase(), cueForm[i].toLowerCase(), isLabeled);
								NegCues.add(cuePos);
								countNegCue++;
							}

							if (!negs.get(i)[col + 1].equals("_")) {
								span[i] = 1;
								spanForm[i] = negs.get(i)[col + 1];
							}

							if (!negs.get(i)[col + 2].equals("_")) {
								target[i] = 1;
								targetForm[i] = negs.get(i)[col + 2];
							}
						}

						/*
						 * if (countNegCue > 1) {
						 * System.err.println("countNegCue:" + countNegCue);
						 * System.err.println(Arrays.toString(words.get(0)));
						 * System.err.println(NegCues); //System.exit(-1); }
						 */

						negation.setNegation(cue, cueForm, span, spanForm, target, targetForm);
						
						int[] boundary = Utils.getBoundary(cue);
						negation.leftmost_cue_pos = boundary[0];
						negation.rightmost_cue_pos = boundary[1];

						negationList.add(negation);

					}
					
					NegationInstance<Label> instance = new NegationInstance<Label>(instanceId, 1, words, null);

					instance.negation = new Negation();
					int[] cue = new int[size];
					String[] cueForm = new String[size];
					instance.negations = new Negation[negationList.size()];
					
					for(int i = 0; i < negationList.size(); i++) {
						instance.negations[i] = negationList.get(i);
						for(int j = 0; j < cue.length; j++) {
							if (instance.negations[i].cue[j] == 1) {
								cue[j] = 1;
								cueForm[j] = instance.negations[i].cueForm[j];
							}
						}
					}
					
					if (NegationGlobal.SORT_NEGATION_CUE) {
						Arrays.sort(instance.negations);
					}
					
					int[] span = new int[size];
					String[] spanForm = new String[size];
					int[] target = new int[size];
					String[] targetForm = new String[size];

					Arrays.fill(span, 0);
					Arrays.fill(spanForm, "_");
					Arrays.fill(target, 0);
					Arrays.fill(targetForm, "_");
					
					instance.negation.setNegation(cue, cueForm, span, spanForm, target, targetForm);
					
					

					if (isLabeled) {
						instance.setLabeled(); // Important!
					} else {
						instance.setUnlabeled();
					}
					instanceId++;
					instance.preprocess();
					result.add(instance);

					numNegation++;

				}

				words = null;
				labels = null;
				negs = null;
			} else {
				// int lastSpace = line.lastIndexOf(NegationGlobal.SEPERATOR);
				String[] fields = line.split(NegationGlobal.SEPERATOR);
				String[] features = Arrays.copyOf(fields, FEATURE_TYPES.values().length);
				
				if (NegationGlobal.USE_UNIVERSAL_POSTAG) {
					String POSTag = features[FEATURE_TYPES.pos_tag.ordinal()];
					
					if (!POSTag.startsWith("NC")) {
						String UniPOS = Utils.UniPOSMapping.getUniPOS(POSTag);
						if (UniPOS != null) {
							POSTag = UniPOS;
						}
						
						//if (POSTag.length() > 2) POSTag = POSTag.substring(0, 2);
							
					} else {
						String word = features[FEATURE_TYPES.word.ordinal()];
						/*
						
						if (word.equals("lessly")) {
							word = "less";
						}*/
						
						
						if (POSTag.endsWith("0")) {
							word = word + "-";
						} else if (POSTag.endsWith("1")) {
							word = "-" + word;
						} else {
							word = "-" + word;
						}
						
						POSTag = "NC";
						
						features[FEATURE_TYPES.word.ordinal()] = word;
						
					}
					
					features[FEATURE_TYPES.pos_tag.ordinal()] =POSTag;// POSTag.equals("NC") ? "NC" : POSTag.substring(0, 2);
					
					
				}
				
				String[] neg_features = Arrays.copyOfRange(fields, FEATURE_TYPES.values().length, fields.length);
				words.add(features);
				negs.add(neg_features);
				
				counter.addWord(features[FEATURE_TYPES.word.ordinal()]);
				numTokens++;
			}
		}
		br.close();
		System.out.println("numNegSentence:" + numNegSentence);
		System.out.println("There are " + numNegation + " negation left in current total #inst: " + result.size() + " in " + numSentence + " sentences.");
		System.out.println(numDiscardInstance + " instances are discarded.");
		System.out.println("#Unique words: " + counter.getVocabSize() + " in #tokens: " + numTokens);
		// System.out.println("Entity Length: " + entityLengthStat.toString());
		System.out.println();
		return result.toArray(new NegationInstance[result.size()]);
	}

	@SuppressWarnings("unchecked")
	private static NegationInstance<Label>[] readCoNLLData(String fileName, boolean withLabels, boolean isLabeled, boolean discardNoNgeation) throws IOException {
		System.out.println("Read " + fileName);
		InputStreamReader isr = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
		BufferedReader br = new BufferedReader(isr);
		ArrayList<NegationInstance<Label>> result = new ArrayList<NegationInstance<Label>>();
		ArrayList<String[]> words = null;
		ArrayList<String[]> negs = null;
		ArrayList<Label> labels = null;
		
		Utils.Counter counter = new Utils.Counter();
		int numTokens = 0;
		
		int numNegationinSentence = 0;
		int numDiscardInstance = 0;
		int numNegation = 0;
		int instanceId = 1;
		int numSentence = 0;
		int numNegSentence = 0;
		int numMaxSpan = 0;
		int maxL = 0;
		int maxM = 0;
		int[] maxspan1 = new int[10];
		int[] maxspan0 = new int[10];
		Set<Integer> range = new HashSet<Integer>();
		
		HashMap<Integer, Integer> entityLengthStat = new HashMap<Integer, Integer>();
		while (br.ready()) {
			if (words == null) {
				words = new ArrayList<String[]>();
			}
			if (negs == null) {
				negs = new ArrayList<String[]>();
			}
			if (withLabels && labels == null) {
				labels = new ArrayList<Label>();
			}
			String line = br.readLine().trim();
			if (line.startsWith("##")) {
				continue;
			}
			if (line.length() == 0) {
				if (words.size() == 0) {
					continue;
				}

				int size = words.size();
				
				if (NegationGlobal.REVERSE_INPUT) {
					Collections.reverse(words);
					Collections.reverse(negs);
				}
				

				if (negs.get(0).length == 1) {

					if (discardNoNgeation) {
						numDiscardInstance++;
					} else {
						numSentence++;
						NegationInstance<Label> instance = new NegationInstance<Label>(instanceId, 1, words, null);

						int[] cue = new int[size];
						String[] cueForm = new String[size];
						int[] span = new int[size];
						String[] spanForm = new String[size];
						int[] target = new int[size];
						String[] targetForm = new String[size];

						Arrays.fill(cue, 0);
						Arrays.fill(cueForm, null);
						Arrays.fill(span, 0);
						Arrays.fill(spanForm, null);
						Arrays.fill(target, 0);
						Arrays.fill(targetForm, null);

							
						
						instance.negation = new Negation(cue, cueForm, span, spanForm, target, targetForm);
						instance.hasNegation = false;

						;
						if (isLabeled) {
							instance.setLabeled(); // Important!
						} else {
							instance.setUnlabeled();
						}
						instanceId++;
						instance.preprocess();
						result.add(instance);
					}
				} else {
					numSentence++;
					numNegSentence++;
					ArrayList<Negation> negationList = new ArrayList<Negation>();

					for (int col = 0; col < negs.get(0).length; col += 3) {
						Negation negation = new Negation();
						int countNegCue = 0;
						ArrayList<Integer> NegCues = new ArrayList<Integer>();
						int cuePos = -1;
						int[] cue = new int[size];
						String[] cueForm = new String[size];
						int[] span = new int[size];
						String[] spanForm = new String[size];
						int[] target = new int[size];
						String[] targetForm = new String[size];

						Arrays.fill(cue, 0);
						Arrays.fill(cueForm, null);
						Arrays.fill(span, 0);
						Arrays.fill(spanForm, null);
						Arrays.fill(target, 0);
						Arrays.fill(targetForm, null);

						for (int i = 0; i < words.size(); i++) {

							if (col >= negs.get(i).length)
								continue;

							if (!negs.get(i)[col].equals("_")) {
								cue[i] = 1;
								cueForm[i] = negs.get(i)[col];
								NegationGlobal.addAffix(words.get(i)[FEATURE_TYPES.word.ordinal()].toLowerCase(), cueForm[i].toLowerCase(), isLabeled);
								NegCues.add(cuePos);
								countNegCue++;
							}

							if (!negs.get(i)[col + 1].equals("_")) {
								span[i] = 1;
								spanForm[i] = negs.get(i)[col + 1];
							}

							if (!negs.get(i)[col + 2].equals("_")) {
								target[i] = 1;
								targetForm[i] = negs.get(i)[col + 2];
							}
						}

						/*
						 * if (countNegCue > 1) {
						 * System.err.println("countNegCue:" + countNegCue);
						 * System.err.println(Arrays.toString(words.get(0)));
						 * System.err.println(NegCues); //System.exit(-1); }
						 */
						int[] arrSpanLength = Utils.getNumSpan(span, 1);
						if (arrSpanLength.length > numMaxSpan) {
							numMaxSpan = arrSpanLength.length;
						}
						
						
						
						//if (arrSpanLength.length == 3) {
							arrSpanLength = Utils.getNumSpan(span, 1);
							//if (span[0] == 0)
							//range.add(arrSpanLength[0]);
							for(int l : arrSpanLength) {
								//range.add(l);
								//System.out.println(x);
								if (l > maxL)
									maxL = l;
							}
							
							arrSpanLength = Utils.getNumSpan(span, 0);
							//if (span[0] == 0)
							//range.add(arrSpanLength[0]);
							for(int l : arrSpanLength) {
								//range.add(l);
								//System.out.println(x);
								if (l > maxM)
									maxM = l;
							}
						//}
						
						negation.setNegation(cue, cueForm, span, spanForm, target, targetForm);
						
						int[] b = Utils.getBoundary(cue);
						negation.leftmost_cue_pos = b[0];
						negation.rightmost_cue_pos = b[1];

						negationList.add(negation);

					}
					boolean first = true;
					for (Negation negation : negationList) {
						//if (first) {first = false; continue;}
						NegationInstance<Label> instance = new NegationInstance<Label>(instanceId, 1, words, null);

						instance.negation = negation;

						if (isLabeled) {
							instance.setLabeled(); // Important!
						} else {
							instance.setUnlabeled();
						}
						instanceId++;
						instance.preprocess();
						result.add(instance);

						numNegation++;

					}

				}

				words = null;
				labels = null;
				negs = null;
			} else {
				// int lastSpace = line.lastIndexOf(NegationGlobal.SEPERATOR);
				String[] fields = line.split(NegationGlobal.SEPERATOR);
				String[] features = Arrays.copyOf(fields, FEATURE_TYPES.values().length);
				
				if (NegationGlobal.USE_UNIVERSAL_POSTAG) {
					String POSTag = features[FEATURE_TYPES.pos_tag.ordinal()];
					
					if (!POSTag.startsWith("NC")) {
						String UniPOS = Utils.UniPOSMapping.getUniPOS(POSTag);
						if (UniPOS != null) {
							POSTag = UniPOS;
						}
						
						//if (POSTag.length() > 2) POSTag = POSTag.substring(0, 2);
							
					} else {
						String word = features[FEATURE_TYPES.word.ordinal()];
						/*
						
						if (word.equals("lessly")) {
							word = "less";
						}*/
						
						/*
						if (POSTag.endsWith("0")) {
							word = word + "-";
						} else if (POSTag.endsWith("1")) {
							word = "-" + word;
						} else {
							word = "-" + word;
						}*/
						
						//POSTag = "NC";
						
						features[FEATURE_TYPES.word.ordinal()] = word;
						
					}
					
					features[FEATURE_TYPES.pos_tag.ordinal()] =POSTag;// POSTag.equals("NC") ? "NC" : POSTag.substring(0, 2);
					
					
				}
				
				String[] neg_features = Arrays.copyOfRange(fields, FEATURE_TYPES.values().length, fields.length);
				words.add(features);
				negs.add(neg_features);
				
				counter.addWord(features[FEATURE_TYPES.word.ordinal()]);
				numTokens++;
			}
		}
		br.close();
		System.out.println("numNegSentence:" + numNegSentence);
		System.out.println("There are " + numNegation + " negation left in current total #inst: " + result.size() + " in " + numSentence + " sentences.");
		System.out.println(numDiscardInstance + " instances are discarded.");
		System.out.println("#Unique words: " + counter.getVocabSize() + " in #tokens: " + numTokens);
		System.out.println("numMaxSpan:" + numMaxSpan + "\tmaxL:" + maxL + "\tmaxM:" + maxM + "\trange:" + range);
		if (isLabeled) {
			NegationGlobal.M_MAX = maxM + 1;
			NegationGlobal.L_MAX = maxL + 1;
			System.out.println("set " + "\tmaxL:" + maxL + "\tmaxM:" + maxM);
		}
		//System.exit(0);
		// System.out.println("Entity Length: " + entityLengthStat.toString());
		System.out.println();
		return result.toArray(new NegationInstance[result.size()]);
	}

	@SuppressWarnings("unchecked")
	public static String inst2Str(NegationInstance inst) {
		StringBuffer sb = new StringBuffer();
		ArrayList<String[]> input = (ArrayList<String[]>) inst.getInput();
		if (NegationGlobal.REVERSE_INPUT) {
			Collections.reverse(input);
		}
		for (int i = 0; i < inst.size(); i++) {
			String[] fields = input.get(i);
			String word = fields[FEATURE_TYPES.word.ordinal()];
			for (String item : fields) {
				sb.append(item + "\t");
			}

			if (inst.hasNegation == false) {
				sb.append("***");
			} else {
				for (Negation neg : (ArrayList<Negation>) inst.negationList) {
					if (neg.cue[i] == 1) {
						sb.append(neg.cueForm[i] + "\t");
					} else {
						sb.append("_" + "\t");
					}

					if (neg.span[i] == 1) {
						
						if (NegationGlobal.USE_SYSTEM_CUE) {
							neg.spanForm[i] = word;
							if (neg.cue[i] == 1 && !neg.cueForm[i].equals(word)) {
								neg.spanForm[i] = word.replace(neg.cueForm[i], "");
							}
						}
						
						sb.append(neg.spanForm[i] + "\t");
					} else {
						sb.append("_" + "\t");
					}

					if (neg.target[i] == 1) {
						sb.append(neg.targetForm[i] + "\t");
					} else {
						sb.append("_" + "\t");
					}

				}
			}

			sb.append("\n");
		}

		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	public static String inst2SeqStr(NegationInstance inst) {
		StringBuffer sb = new StringBuffer();
		ArrayList<String[]> input = (ArrayList<String[]>) inst.getInput();
		ArrayList<Label> gold = (ArrayList<Label>) inst.getOutput();
		ArrayList<Label> pred = (ArrayList<Label>) inst.getPrediction();

		for (int i = 0; i < inst.size(); i++) {
			String[] fields = input.get(i);
			sb.append(input.get(i)[FEATURE_TYPES.word.ordinal()] + "\t");
			sb.append((gold.get(i).getForm().startsWith("I") ? 1 : 0) + "\t");
			sb.append((pred.get(i).getForm().startsWith("I") ? 1 : 0));
			sb.append("\n");
		}

		sb.append("\n");

		return sb.toString();
	}
	
	@SuppressWarnings("unchecked")
	public static String inst2BioScopeStr(NegationInstance inst) {
		StringBuffer sb = new StringBuffer();
		ArrayList<String[]> input = (ArrayList<String[]>) inst.getInput();
		ArrayList<Label> gold = (ArrayList<Label>) inst.getOutput();
		ArrayList<Label> pred = (ArrayList<Label>) inst.getPrediction();
		
		List<String> sentence = Utils.getPhraseList(input, 0, input.size() - 1, FEATURE_TYPES.word.ordinal(), false);
		for(int i = 0; i < sentence.size(); i++)
			sentence.set(i, sentence.get(i) + "(" + i  + ")");
		sb.append(Utils.join(" ", sentence) + " ||| ");
		sb.append(Utils.getPhrase(input, 0, input.size() - 1, FEATURE_TYPES.pos_tag.ordinal(), false));

		for (int i = 0; i < inst.negationList.size(); i++) {
			Negation neg = (Negation)inst.negationList.get(i);
			
			sb.append(" ||| " + neg.toBioScopeFormat());
		}

		sb.append("\n");

		return sb.toString();
	}

	public static void writeResult(Instance[] preds, String goldfile, String filename_output) {
		// String filename_output = (String) getParameters("filename_output");
		// String filename_standard = (String)
		// getParameters("filename_standard");

		PrintWriter p = null;

		if (NegationGlobal.OUTPUT_SEM2012_FORMAT) {

			try {
				p = new PrintWriter(new File(filename_output), "UTF-8");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (NegationGlobal.DEBUG)
				System.out.println("Result: ");

			HashMap<String, NegationInstance> accuInsts = new HashMap<String, NegationInstance>();
			// HashMap<String, ArrayList<Negation>> outputs = new
			// HashMap<String, ArrayList<Negation>>();
			ArrayList<String> sentList = new ArrayList<String>();

			if (NegationGlobal.modelname.startsWith("JOINT")) {
				for (int i = 0; i < preds.length; i++) {
					NegationInstance inst = (NegationInstance) preds[i];
					
					String sentID = inst.getSentID();
					if (NegationGlobal.dataSet.startsWith("bioscope") || NegationGlobal.dataSet.startsWith("cnesp")) {
						String instStr = inst2BioScopeStr(inst);
						p.write(instStr);
						p.flush();
					} else {
						p.write(inst2Str(inst));
						p.write("\n");
					}
					

				}
			} else {
				for (int i = 0; i < preds.length; i++) {
					NegationInstance inst = (NegationInstance) preds[i];
					String sentID = inst.getSentID();

					NegationInstance accuInst = accuInsts.get(sentID);

					if (accuInst == null) {
						accuInsts.put(sentID, inst);
						sentList.add(sentID);
						accuInst = inst;
					}

					if (accuInst.hasNegation) {
						if (!NegationGlobal.NEG_CUE_DETECTION)
							accuInst.negationList.add(inst.pred2NegationPred());
					}

				}
				
				int counter=0;
				for (String sentID : sentList) {
					NegationInstance inst = accuInsts.get(sentID);
					counter++;
					if (NegationGlobal.dataSet.startsWith("bioscope") || NegationGlobal.dataSet.startsWith("cnesp")) {
						String instStr = inst2BioScopeStr(inst);
						p.write(instStr);
						p.flush();
					} else {
						p.write(inst2Str(inst));
						p.write("\n");
					}

				}
			}

			

			p.close();
		}

		try {
			p = new PrintWriter(new File(filename_output + ".seq"), "UTF-8");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (int i = 0; i < preds.length; i++) {
			NegationInstance inst = (NegationInstance) preds[i];
			if (inst.hasNegation)
				p.write(inst2SeqStr(inst));
		}

		p.close();
		
		
		

		if (NegationGlobal.DEBUG) {
			System.out.println("\n");
		}
		System.out.println(NegationGlobal.modelname + " Evaluation Completed");

		// NegEval.evalbyScript(goldfile, filename_output);

		if (NegationGlobal.OUTPUT_SENTIMENT_SPAN) {
			try {
				p = new PrintWriter(new File(filename_output + ".span.html"), "UTF-8");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			String css = "/Users/Leo/workspace/ui/overlap.css";
			if (NetworkConfig.OS.equals("linux")) {
				css = "/home/lihao/workspace/ui/overlap.css";
			}

			String header = "<html><head><link rel='stylesheet' type='text/css' href='" + css + "' /></head> <body><br><br>\n";
			String footer = "\n</body></html>";
			p.write(header);

			//int pInst = 0, counter = 0;
			//int[][] splits = new int[preds.length][];
			//ArrayList<Integer> split = new ArrayList<Integer>();
			/*if (NegationGlobal.dataSet.startsWith("JOINT")) {
				ArrayList<NegationInstance> predList = new ArrayList<NegationInstance>();
				for(int i = 0; i < preds.length; i++) {
					NegationInstance inst = (NegationInstance)preds[i];
					
					
					
					predList.add((NegationInstance) preds[i]);
				}
				
				
			}*/

			for (int i = 0; i < preds.length; i++) {
				NegationInstance inst = (NegationInstance) preds[i];
				ArrayList<String[]> input = (ArrayList<String[]>) inst.getInput();

				ArrayList<Label> gold = (ArrayList<Label>) inst.getOutput();
				ArrayList<Label> pred = (ArrayList<Label>) inst.getPrediction();

				ArrayList<int[]> scopes = inst.scopes;

				String t = "";

				t += gold + "<br>\n";

				t += pred + "<br>\n";

				t += outputSent(gold, inst, "positive");

				t += outputSent(pred, inst, "negative");

				t += "<br>\n";

				p.println(t);

			}

			p.write(footer);

			p.close();

		}
		
		
		if (NegationGlobal.OUTPUT_ERROR) {
			try {
				p = new PrintWriter(new File(filename_output + ".span.error.html"), "UTF-8");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			String css = "/Users/Leo/workspace/ui/overlap.css";
			if (NetworkConfig.OS.equals("linux")) {
				css = "/home/lihao/workspace/ui/overlap.css";
			}

			String header = "<html><head><link rel='stylesheet' type='text/css' href='" + css + "' /><title>Error Analysis</title></head> <body><br><br>\n";
			String footer = "\n</body></html>";
			p.write(header);
			
			if (NegationGlobal.modelname.startsWith("JOINT") || NegationGlobal.dataSet.endsWith("end2end")) {
				
				printErrorJoint(p, preds);
				p.write(footer + "\n");
				p.close();
				return;
			}

			int pInst = 0, counter = 0;
			int[][] splits = new int[preds.length][];
			ArrayList<Integer> split = new ArrayList<Integer>();
			
			int numError = 0;
			int numTotal = 0;
			int[] errorType = new int[2];
			Arrays.fill(errorType, 0);
			
			int totalNumGoldTokenInWrongInstance = 0;
			int totalNumMisMatchTokenInWrongInstance = 0;
			
			int totalNumGoldTokenInRightInstance = 0;
			int totalNumMisMatchTokenInRightInstance = 0;
			
			Utils.IntStat correctStat = new Utils.IntStat();
			Utils.IntStat inCorrectStat = new Utils.IntStat();
			Utils.IntStat totalStat = new Utils.IntStat();

			for (int i = 0; i < preds.length; i++) {
				NegationInstance inst = (NegationInstance) preds[i];
				
				if (!inst.hasNegation) continue;
				
				ArrayList<String[]> input = (ArrayList<String[]>) inst.getInput();

				ArrayList<Label> gold = (ArrayList<Label>) inst.getOutput();
				ArrayList<Label> pred = (ArrayList<Label>) inst.getPrediction();
				
				
				NegMetric m = new NegMetric();
				
				NegEval.evalNegationSpanonSingleSentence(gold, pred, m, input);
				
				numTotal++;
				totalStat.addInt(pred.size());
				
				if (m.numSpanExact[2] == 1 || (m.numSpanExact[0] == 0 && m.numSpanExact[1] == 0)) {
					totalNumGoldTokenInRightInstance += m.numSpanToken[0];
					totalNumMisMatchTokenInRightInstance += m.numSpanToken[0] - m.numSpanToken[2];
					
					correctStat.addInt(pred.size());
					
					continue;
				}
				
				numError++;
				inCorrectStat.addInt(pred.size());
				
				totalNumGoldTokenInWrongInstance += m.numSpanToken[0];
				totalNumMisMatchTokenInWrongInstance += m.numSpanToken[0] - m.numSpanToken[2];
				
				if (m.numSpanToken[0] >= m.numSpanToken[1]) {
					errorType[0]++;
				} else {
					errorType[1]++;
				}

				ArrayList<int[]> scopes = inst.scopes;

				String t = "";

				t += gold + "<br>\n";

				t += pred + "<br>\n";

				t += outputSent(gold, inst, "positive");

				t += outputSent(pred, inst, "negative");

				t += "<br>\n";

				p.println(t);

			}
			
			p.write("<br><p>numTotal:" + numTotal + "</p>\n");
			
			p.write("<br><p> #Errors:" + numError + " . Type 1:" + errorType[0]  + " , Type 2:" + errorType[1] + " , </p>\n");
			
			p.write("<br><p> In Wrong, totalNumGoldToken:" + totalNumGoldTokenInWrongInstance + " , totalNumMisMatchToken:" + totalNumMisMatchTokenInWrongInstance + "  </p>\n");

			p.write("<br><p> In Right, totalNumGoldToken:" + totalNumGoldTokenInRightInstance + " , totalNumMisMatchToken:" + totalNumMisMatchTokenInRightInstance + "  </p>\n");

			p.write("<br><p>Stat  Total:<br>" + totalStat.toString() + " \ncorrect:<br>" + correctStat.toString()  + "\n incorect:<br>" + inCorrectStat+ "</p>\n");
			
			p.write(footer);

			p.close();

		}


	}

	static String outputSent(ArrayList<Label> output, NegationInstance inst, String color) {

		String t = "";
		char lastTag = 'O';
		ArrayList<String[]> input = (ArrayList<String[]>) inst.getInput();
		
		if (!inst.hasNegation)
			return "";

		for (int k = 0; k < input.size(); k++) {
			
			String labelStr = output.get(k).getForm();
			char tag = labelStr.charAt(0);
			char nextTag = (k + 1 < input.size()) ? output.get(k + 1).getForm().charAt(0) : 'O';

			if (tag != 'O' && lastTag == 'O') {
				t += "<div class='tooltip entity_" + color + "'>";
			}

			if (inst.negation.cue[k] == 1) {
				t += "<div class='tooltip entity_neutral_incorrect'>";
			}
			t += input.get(k)[NegationInstance.FEATURE_TYPES.word.ordinal()] + "&nbsp;";

			if (inst.negation.cue[k] == 1) {
				t += "</div>&nbsp;";
			}

			if (tag != 'O' && nextTag == 'O') {
				t += "</div>&nbsp;";
			}

			lastTag = tag;
		}

		t += "<br>\n";

		return t;

	}
	
	public static String printErrorJointNeg(int[] cue, int[] span, List<String> words, String color) {
		
		
		String t = "";
		int lastTag = 0;

		for (int k = 0; k < words.size(); k++) {

			
			int tag = span[k];
			int nextTag = (k + 1 < words.size()) ? span[k + 1] : 0;

			if (tag != 0 && lastTag == 0) {
				t += "<div class='tooltip entity_" + color + "'>";
			}

			if (cue[k] == 1) {
				t += "<div class='tooltip entity_neutral_incorrect'>";
			}
			t += words.get(k) + "&nbsp;";

			if (cue[k] == 1) {
				t += "</div>&nbsp;";
			}

			if (tag != 0 && nextTag == 0) {
				t += "</div>&nbsp;";
			}

			lastTag = tag;
		}

		t += "<br>\n";

		return t;
		
	}
	
	public static void printErrorJoint(PrintWriter p, Instance[] preds) {
		
		for(int i = 0; i < preds.length; i++) {
			NegationInstance inst = (NegationInstance)preds[i];
			
			ArrayList<Negation> negationList = inst.negationList;
			
			List<String[]> inputs = (List<String[]>)inst.getInput();
			List<String> words = Utils.getPhraseList(inputs, FEATURE_TYPES.word.ordinal(), false);
			if (inst.hasNegation)
			for(Negation predNeg : negationList) {
				
				boolean found = false;
				Negation neg = null;
				
				if (inst.negations != null)
					for(Negation goldNeg : inst.negations) {
						
						if (Utils.spanArrayEqual(predNeg.cue, goldNeg.cue)) {
							
							if (Utils.spanArrayEqual(predNeg.span, goldNeg.span)) {
								found = true;
								neg = goldNeg;
								break;
							}
							
							
						}
					}
				
				
				if (neg == null) {
					
					String t = "";
					
					t += printErrorJointNeg(predNeg.cue, predNeg.span, words, "negative");
					t += "<br>\n";
					
					t += "#negation:";
					if (inst.negations == null)
						t +=  "0";
					else
						t += "" + inst.negations.length;
					t += "    hasNegation:" + inst.hasNegation + "<br>";
					if (inst.negations != null)
						for(Negation goldNeg : inst.negations) {
							t += printErrorJointNeg(goldNeg.cue, goldNeg.span, words, "positive");
						}
					
					t += "<br>\n";
					t += "<br>\n";
					p.write(t);
					
					/*
					p.write(inst.getSentence() + "<br>\n");
					p.write(Arrays.toString(predNeg.cue) + "<br>\n");
					p.write(Arrays.toString(predNeg.span) + "<br>\n");
					p.write("<br>\n");*/
				}
				
			}
			
			
		}
		
	}

}
