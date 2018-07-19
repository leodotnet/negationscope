package org.ns.negation.common;

import java.util.ArrayList;


public class NegationFeature {
	
	public ArrayList<String[]>[] feature = null;
	public ArrayList<String[]>[] horizon_feature = null;
	
	public void init(int size)
	{
		feature = new ArrayList[size];
		horizon_feature = new ArrayList[size];
	}
	

}
