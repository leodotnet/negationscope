package org.ns.negation.common;

import java.util.ArrayList;
import java.util.Arrays;

public class Negation implements Comparable<Negation>{
	
	public int[] cue = null;
	public String[] cueForm = null;
	public int[] span = null;
	public String[] spanForm = null;
	public int[] target = null;
	public String[] targetForm = null;
	
	public int leftmost_cue_pos = 1000000;
	public int rightmost_cue_pos = 1000000;
	
	public enum NegationType {negation, speculation};
	public NegationType type = NegationType.negation;
	
	public Negation() {
		
	}
	
	public static Negation createEmptyNegation(int size) {
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
		
		Negation neg = new Negation(cue, cueForm, span, spanForm, target, targetForm);
		
		return neg;
	}
	

	public Negation(int[] cue, String[] cueForm,  int[] span, String[] spanForm, int[] target, String[] targetForm) {
		this.setNegation(cue, cueForm, span, spanForm, target, targetForm);
	}
	
	public void setType(String type) {
		this.type = NegationType.valueOf(type);
	}
	
	public void setType(int type) {
		this.type = NegationType.values()[type];
	}
	
	public void setNegation(int[] cue, String[] cueForm, int[] span, String[] spanForm, int[] target, String[] targetForm) {
		this.cue = cue;
		this.cueForm = cueForm;
		this.span = span;
		this.spanForm = spanForm;
		this.target = target;
		this.targetForm = targetForm;
	}
	
	public Negation clone() {
		Negation neg = new Negation();
		neg.cue = this.cue.clone();
		neg.cueForm = this.cueForm.clone();
		neg.span = this.span.clone();
		neg.spanForm = this.spanForm.clone();
		neg.target = this.target.clone();
		neg.targetForm = this.targetForm.clone();
		neg.type = this.type;
		return neg;
	}
	
	public String toBioScopeFormat() {
		int[] cueBoundary = Utils.getBoundary(this.cue);
		int[] spanBoundary = Utils.getBoundary(this.span);
		return spanBoundary[0] + " " + (spanBoundary[1] + 1) + " negation " + cueBoundary[0] + " " + (cueBoundary[1] + 1);
	}

	@Override
	public int compareTo(Negation o) {
		// TODO Auto-generated method stub
		return this.leftmost_cue_pos - o.leftmost_cue_pos;
	}

}
