package org.ns.hypergraph.decoding;

/**
 * The metric interface used for evaluate the model on the development set.
 * @author Allan (allanmcgrady@gmail.com)
 *
 */
public interface Metric {

	/**
	 * See if the current result is better than other metric.
	 * @param other
	 * @return
	 */
	public boolean isBetter(Metric other);
	
	public Object getMetricValue();

}
