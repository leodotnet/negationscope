package org.ns.negation.common;

import java.util.Arrays;

import org.ns.hypergraph.decoding.Metric;

public class NegMetric implements Metric {

	public static String SEPARATOR = "\t";
	
	public enum EVALOPTION {Exact, Token, PCS};
	public static EVALOPTION BESTON = EVALOPTION.Exact;
	
	//P, R, F
	double[] spanTokenStat = new double[3];
	double[] spanTokenStatPunc = new double[3];
	
	double[] spanExactStat = new double[3];
	double[] spanExactStatA = new double[3];
	
	double[] spanExactStatPunc = new double[3];
	double[] spanExactStatAPunc = new double[3];
	
	double spanAcc = 0;
		
	public int[] numSpanToken = new int[3];  //gold pred match
	public int[] numSpanTokenPunc = new int[3];  //gold pred match
	
	public int[] numSpanExact = new int[3];  //gold pred match
	public int[] numSpanExactA = new int[3];
	
	public int[] numSpanExactPunc = new int[3];  //gold pred match
	public int[] numSpanExactAPunc = new int[3];
	
	public int total = 0;
	
	
	
	public boolean aggregation = false;
	
	public int numResult = 0;
	
	public NegMetric() {
		Arrays.fill(numSpanToken, 0);
		Arrays.fill(numSpanTokenPunc, 0);
		Arrays.fill(numSpanExact, 0);
		Arrays.fill(numSpanExactA, 0);
		
		Arrays.fill(numSpanExactPunc, 0);
		Arrays.fill(numSpanExactAPunc, 0);
	}
	
	public NegMetric(boolean aggregation) {
		this.aggregation = aggregation;
		
		if (this.aggregation) {
			numResult = 0;
			
			Arrays.fill(spanTokenStat, 0);
			Arrays.fill(spanTokenStatPunc, 0);
			Arrays.fill(spanExactStat, 0);
			Arrays.fill(spanExactStatA, 0);
			
			Arrays.fill(spanExactStatPunc, 0);
			Arrays.fill(spanExactStatAPunc, 0);
			
		}
	}
	
	public void aggregate(NegMetric metric) {
		
		if (this.aggregation) {
			numResult++;
			
			this.spanTokenStat = Utils.vectorAdd(this.spanTokenStat, metric.spanTokenStat);
			this.spanTokenStatPunc = Utils.vectorAdd(this.spanTokenStatPunc, metric.spanTokenStatPunc);
			this.spanExactStat = Utils.vectorAdd(this.spanExactStat, metric.spanExactStat);
			this.spanExactStatA = Utils.vectorAdd(this.spanExactStatA, metric.spanExactStatA);
			
			this.spanExactStatPunc = Utils.vectorAdd(this.spanExactStatPunc, metric.spanExactStatPunc);
			this.spanExactStatAPunc = Utils.vectorAdd(this.spanExactStatAPunc, metric.spanExactStatAPunc);
			
		}
		
	}

	@Override
	public boolean isBetter(Metric other) {
		NegMetric metric = (NegMetric)other;
		
		boolean better = false;
		
		if (BESTON == EVALOPTION.Exact) {
			if (metric.spanExactStat[2] == Double.NaN)
			metric.spanExactStat[2] = 0.0;
			better = Math.abs(spanExactStat[2] - metric.spanExactStat[2]) > 1e-5 ? spanExactStat[2] > metric.spanExactStat[2] : spanTokenStat[2] > metric.spanTokenStat[2];
		} else if (BESTON == EVALOPTION.Token) {
			if (metric.spanTokenStat[2] == Double.NaN)
				metric.spanTokenStat[2] = 0.0;
			
			better = Math.abs(spanTokenStat[2] - metric.spanTokenStat[2]) > 1e-5 ? spanTokenStat[2] > metric.spanTokenStat[2] : spanExactStat[2] > metric.spanExactStat[2];
		} else { //BESTON == EVALOPTION.PCS
			better = spanAcc > metric.spanAcc;
		}
		
		
		return better;
	}

	@Override
	public String getMetricValue() {
		return this.spanExactStat[2] + "\t\t" + spanTokenStat[2] + "\t\t" + spanAcc;
	}
	
	String double2Str(double x) {
		return String.format ("%.2f", x * 100);
	}
	
	String metric2Str(double[] m) {
		String ret = "";
		for(int i = 0; i < m.length; i++) {
			ret += double2Str(m[i]) + " & " + this.SEPARATOR;
		}
		
		return ret;
	}
	
	String metric2Str(int[] m) {
		String ret = "";
		for(int i = 0; i < m.length; i++) {
			ret += m[i] + this.SEPARATOR;
		}
		
		return ret;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Metric (- Punc)   [P     \tR  \tF]\n");
		sb.append("Exact Scope A     [" + metric2Str(spanExactStatA) + "]\n");
		sb.append("Exact Scope A*    [" + metric2Str(numSpanExactA) + "]\n");
		
		sb.append("Exact Scope B     [" + metric2Str(spanExactStat) + "]\n");
		sb.append("Exact Scope B*    [" + metric2Str(numSpanExact) + "]\n");
		
		sb.append("PCS               " + double2Str(spanExactStatA[1]) + "\n");
		sb.append("Token Scope       [" + metric2Str(spanTokenStat) + "]\n");
		sb.append("Token Scope *     [" + metric2Str(numSpanToken) + "]\n");
	
		
		sb.append("\n");
		
		sb.append("Metric            [P     \tR  \tF]\n");
		sb.append("Exact Scope A     [" + metric2Str(spanExactStatAPunc) + "]\n");
		sb.append("Exact Scope A*    [" + metric2Str(numSpanExactAPunc) + "]\n");
		
		sb.append("Exact Scope B     [" + metric2Str(spanExactStatPunc) + "]\n");
		sb.append("Exact Scope B*    [" + metric2Str(numSpanExactPunc) + "]\n");
				
		sb.append("PCS               " + double2Str(spanExactStatAPunc[1]) + "\n");
		//sb.append("PCS *             " + total + "\n");
	
	
		
		sb.append("Token Scope       [" + metric2Str(spanTokenStatPunc) + "]\n");
		sb.append("Token Scope       *[" + metric2Str(numSpanTokenPunc) + "]\n");
		
		if (this.aggregation) {
			sb.append("#Total Test: " + numResult  + "\n");
		}
		
		sb.append("\n");
		return sb.toString();
	}
	
	public NegMetric compute() {
		
		if (this.aggregation) {
			
			double r = 1.0 / this.numResult;
			
			this.spanTokenStat = Utils.vectorScale(this.spanTokenStat, r);
			this.spanTokenStatPunc = Utils.vectorScale(this.spanTokenStatPunc, r);
			this.spanExactStat = Utils.vectorScale(this.spanExactStat, r);
			this.spanExactStatA = Utils.vectorScale(this.spanExactStatA, r);
			
			this.spanExactStatPunc = Utils.vectorScale(this.spanExactStatPunc, r);
			this.spanExactStatAPunc = Utils.vectorScale(this.spanExactStatAPunc, r);
						
		}
		else {
			this.spanExactStat = compute(numSpanExact[0], numSpanExact[1], numSpanExact[2]);
			this.spanExactStatA = compute(numSpanExactA[0], numSpanExactA[1], numSpanExactA[2]);
			this.spanTokenStat = compute(numSpanToken[0], numSpanToken[1], numSpanToken[2]);
			this.spanTokenStatPunc = compute(numSpanTokenPunc[0], numSpanTokenPunc[1], numSpanTokenPunc[2]);
			
			this.spanExactStatPunc = compute(numSpanExactPunc[0], numSpanExactPunc[1], numSpanExactPunc[2]);
			this.spanExactStatAPunc = compute(numSpanExactAPunc[0], numSpanExactAPunc[1], numSpanExactAPunc[2]);
			
			this.spanAcc = (numSpanExact[2] + 0.0) / this.total;
			
		}
		return this;
	}
	
	double[] compute(int gold, int pred, int match) {
		double[] stat = new double[3];
		
		double m = match;
		
		
		if (pred == 0) 
			stat[0] = 0;
		else
			stat[0] = m / pred; 
		
		stat[1] = m / gold;
		
		if (Math.abs(stat[0] + stat[1]) < 1e-8) 
			stat[2] = 0;
		else 
			stat[2] = 2 * stat[0] * stat[1] / (stat[0] + stat[1]);
		
		
		return stat;
	}
	
}
