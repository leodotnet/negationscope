package org.ns.negation.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.ns.commons.types.Instance;
import org.ns.commons.types.Label;
import org.ns.hypergraph.NetworkConfig;
import org.ns.hypergraph.decoding.Metric;

import java.nio.file.Paths;



public class NegEval {

	public static Metric eval(Instance[] insts)
	{
		NegMetric metric = new NegMetric();
		
		metric.total = 0;
		
		for(int i = 0; i < insts.length; i++)
		{
			NegationInstance inst = (NegationInstance)insts[i];
			
			if (inst.hasNegation == false)
				continue;
			
			metric.total++;
			
			ArrayList<String[]> input = (ArrayList<String[]>)inst.getInput(); 
			ArrayList<Label> gold = (ArrayList<Label>)inst.getOutput();
			ArrayList<Label> pred = (ArrayList<Label>)inst.getPrediction();
			
			
			
			evalNegationSpanonSingleSentence(gold, pred, metric, input);
			
		}
		
		
		
		
		return metric.compute();
	}
	
	
	/**
	 * 
	 * @param testInsts
	 * @param nerOut: word, true pos, true entity, pred entity
	 * @throws IOException
	 */
	public static void evalNegationSpanonSingleSentence(ArrayList<Label> gold, ArrayList<Label> pred, NegMetric metric, ArrayList<String[]> input){
		
		int[] token = new int[]{0, 0, 0}; //gold, pred, match
		int[] tokenPunc = new int[]{0, 0, 0}; //gold, pred, match
		
		
		int[] exact = new int[]{0, 0, 1}; //gold, pred, match
		int[] exactA = new int[]{0, 0, 1}; //measurement A
		
		int[] exactPunc = new int[]{0, 0, 1}; //gold, pred, match
		int[] exactAPunc = new int[]{0, 0, 1}; //measurement A
	
		for(int i = 0; i < gold.size(); i++) {
			
			int gold_id = gold.get(i).getForm().startsWith("O")  ? 0 : 1;
			int pred_id = pred.get(i).getForm().startsWith("O")  ? 0 : 1;
			
			boolean isPunc = Utils.isPunctuation(input.get(i)[3]);
			
			if (gold_id != pred_id && (!isPunc)) {
				exact[2] = 0;
				exactA[2] = 0;
			}
			
			if (gold_id != pred_id) {
				exactPunc[2] = 0;
				exactAPunc[2] = 0;
			}
			
			
			
			
			if (gold_id == 1) {
				exact[0] = 1;
				exactA[0] = 1;
				
				exactPunc[0] = 1;
				exactAPunc[0] = 1;
				
				
				
				token[0]++;
				
				if (!isPunc) {
					tokenPunc[0]++;
				}
			}
			
			if (pred_id == 1) {
				exact[1] = 1;
				exactPunc[1] = 1;
				
				token[1]++;
				
				if (!isPunc) {
					tokenPunc[1]++;
				}
			}
			
			if (gold_id == pred_id && pred_id == 1) {
				token[2]++;
				
				if (!isPunc) {
					tokenPunc[2]++;
				}
			}
			
			
		}
		
		if (exact[0] == 0 || exact[1] == 0) {
			exact[2] = 0;
			exactA[2] = 0;
		}
		
		if (exactPunc[0] == 0 || exactPunc[1] == 0) {
			exactPunc[2] = 0;
			exactAPunc[2] = 0;
		}
		
		//gold has no scope, pred has a scope
		if ((exact[0] == 0 && exact[1] == 1) || exact[2] == 1) {
			exactA[1] = 1;
		}
		
		//gold has no scope, pred has a scope
		if ((exactPunc[0] == 0 && exactPunc[1] == 1) || exactPunc[2] == 1) {
			exactAPunc[1] = 1;
		}
		
		
		metric.numSpanExact = Utils.vectorAdd(metric.numSpanExact, exact);
		metric.numSpanExactA = Utils.vectorAdd(metric.numSpanExactA, exactA);
		
		metric.numSpanExactPunc = Utils.vectorAdd(metric.numSpanExactPunc, exactPunc);
		metric.numSpanExactAPunc = Utils.vectorAdd(metric.numSpanExactAPunc, exactAPunc);
		
		metric.numSpanToken = Utils.vectorAdd(metric.numSpanToken, token);
		metric.numSpanTokenPunc = Utils.vectorAdd(metric.numSpanTokenPunc, tokenPunc);
		
		
	}
	
	public static String evalScript = "scripts/eval.cd-sco.pl";  //remember to make the script runnable
	
	public static void evalbyScript(String goldfile, String predfile, String workingDir){
		evalbyScript(NegationDataConfig.pathJoin(workingDir, goldfile), NegationDataConfig.pathJoin(workingDir , predfile));
	}
	
	public static void evalbyScript(String goldfile, String predfile){
		
		
		StringBuffer sb = new StringBuffer();
		double fscore = 0;
		try{
			String cmd = "/usr/bin/perl";// + evalScript ;//+" -g "+ goldfile + " -s " + predfile;
			
			if (!NegationGlobal.dataSet.startsWith("cdsco")) {
				cmd = "/Users/Leo/anaconda/bin/python3";
				if (NetworkConfig.OS.equals("linux"))
					cmd = "/usr/bin/python3";
				evalScript = "scripts/eval-bioscope.py";
			}
			
			System.err.println(cmd + " " + evalScript +" -g "+ goldfile + " -s " + predfile);
			ProcessBuilder pb = null;
			/*if(windows){
				pb = new ProcessBuilder("D:/Perl64/bin/perl","E:/Framework/data/semeval10t1/conlleval.pl"); 
			}else
			{*/
				pb = new ProcessBuilder(cmd, evalScript, "-g "+ goldfile, "-s " + predfile); 
			//}
			//pb.redirectInput(new File(outputFile));
			//pb.redirectOutput(Redirect.INHERIT);
			//pb.redirectError(Redirect.INHERIT);
			Process process = pb.start();
			
			
			
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8")) ;
			while (!br.ready()) ; // wait until buffered reader is ready.
			System.out.println("ready");
			while (br.ready()) {
				String line = br.readLine();
				System.out.println(line);
				sb.append(line + "\n");
				/*
				if (line.startsWith("accuracy")) {
					String[] vals = line.trim().split("\\s+");
					fscore = Double.valueOf(vals[vals.length - 1]);
				}*/
			}
			br.close();
			pb = null;
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
		
		PrintWriter p = null;
		try {
			p = new PrintWriter(predfile + ".stat");
			p.println(sb.toString());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		p.close();
		//return new NEMetric(fscore);
	}
	
	/*
	public static void main(String[] args) {
		
	
		String goldfile = "data//negation//SEM-2012-SharedTask-CD-SCO-test-merge-GOLD.txt";
		String predfile = "experiments//negation//models//negationspanOIBAN6//en//20171201_discrete//SEM-2012-SharedTask-CD-SCO-test-merge-GOLD.txt.out";
		
		evalbyScript(goldfile, predfile);
	}*/
	
}
