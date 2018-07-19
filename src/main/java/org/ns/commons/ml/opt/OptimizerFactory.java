/** Statistical Natural Language Processing System
    Copyright (C) 2014-2016  Lu, Wei

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ns.commons.ml.opt;

import java.io.Serializable;

import org.ns.commons.ml.opt.GradientDescentOptimizer.AdaptiveStrategy;
import org.ns.commons.ml.opt.GradientDescentOptimizer.BestParamCriteria;
import org.ns.hypergraph.StringIndex;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public abstract class OptimizerFactory implements Serializable {
	
	public static enum OptimizerFactoryEnum {
		LBFGS,
		GD,
		GD_ADAGRAD,
		GD_ADADELTA,
		GD_ADAM,
		GD_RMSPROP,
		;
	}
	
	private static final long serialVersionUID = 70815268952763513L;
	public static final double DEFAULT_LEARNING_RATE = 1e-3;
	public static final double DEFAULT_LEARNING_RATE_DECAY = 0.9;
	public static final double DEFAULT_ADADELTA_PHI = 0.95;
	public static final double DEFAULT_ADADELTA_EPS = 1e-7;
	public static final double DEFAULT_ADADELTA_GRAD_DECAY = 0.75;
	public static final double DEFAULT_RMSPROP_DECAY = 0.9;
	public static final double DEFAULT_RMSPROP_EPS = 1e-7;
	public static final double DEFAULT_ADAM_BETA1 = 0.9;
	public static final double DEFAULT_ADAM_BETA2 = 0.95;
	public static final double DEFAULT_ADAM_EPS = 1e-7;
	public static final double DEFAULT_CLIPPING_THRESHOLD = 5.0;
	
	protected OptimizerFactory() {}
	
	/**
	 * Return the factory object to create an L-BFGS optimizer.
	 * @return
	 */
	public static LBFGSOptimizerFactory getLBFGSFactory(){
		return new LBFGSOptimizerFactory();
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with normal (S)GD procedure.<br>
	 * The default learning rate will be set to {@value #DEFAULT_LEARNING_RATE}.
	 * @param learningRate
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactory(){
		return new GradientDescentOptimizerFactory(BestParamCriteria.BEST_OBJECTIVE, AdaptiveStrategy.NONE, DEFAULT_LEARNING_RATE);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with normal (S)GD procedure.<br>
	 * @param learningRate
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactory(double learningRate){
		return new GradientDescentOptimizerFactory(BestParamCriteria.BEST_OBJECTIVE, AdaptiveStrategy.NONE, learningRate);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer with gradient clipping default threshold 5..<br>
	 * The returned factory will create instances of GradientDescentOptimizer with normal (S)GD procedure.<br>
	 * @param learningRate
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingGradientClipping(double learningRate){
		return new GradientDescentOptimizerFactory(BestParamCriteria.BEST_OBJECTIVE, AdaptiveStrategy.NONE, learningRate, DEFAULT_CLIPPING_THRESHOLD);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer with gradient clipping with threshold.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with normal (S)GD procedure.<br>
	 * @param learningRate
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingGradientClipping(double learningRate, double threshold){
		return new GradientDescentOptimizerFactory(BestParamCriteria.BEST_OBJECTIVE, AdaptiveStrategy.NONE, learningRate, threshold);
	}
	
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingGradientClipping(BestParamCriteria criteria, double learningRate, double threshold){
		return new GradientDescentOptimizerFactory(criteria, AdaptiveStrategy.NONE, learningRate, threshold);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with AdaGrad adaptive method.<br>
	 * The default learning rate will be set to {@value #DEFAULT_LEARNING_RATE}.
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingAdaGrad(){
		return new GradientDescentOptimizerFactory(BestParamCriteria.BEST_OBJECTIVE, AdaptiveStrategy.ADAGRAD, DEFAULT_LEARNING_RATE);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with AdaGrad adaptive method.
	 * @param learningRate
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingAdaGrad(double learningRate){
		return new GradientDescentOptimizerFactory(BestParamCriteria.BEST_OBJECTIVE, AdaptiveStrategy.ADAGRAD, learningRate);
	}

	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with AdaDelta adaptive method.<br>
	 * By default the hyperparameters are set as follows:
	 * <ol>
	 * <li>phi = {@value #DEFAULT_ADADELTA_PHI}</li>
	 * <li>eps = {@value #DEFAULT_ADADELTA_EPS}</li>
	 * </ol>
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingAdaDelta(){
		return new GradientDescentOptimizerFactory(BestParamCriteria.BEST_OBJECTIVE, AdaptiveStrategy.ADADELTA, 0.0, DEFAULT_ADADELTA_PHI, DEFAULT_ADADELTA_EPS);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with AdaDelta adaptive method.<br>
	 * The hyperparameters are set according to the passed values.
	 * @param phi
	 * @param eps
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingAdaDelta(double phi, double eps){
		return new GradientDescentOptimizerFactory(BestParamCriteria.BEST_OBJECTIVE, AdaptiveStrategy.ADADELTA, 0.0, phi, eps);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with AdaDelta adaptive method.<br>
	 * This formulation of AdaDelta differs from the standard one with smoothed gradient used instead of the current gradient, which might be noisy<br>
	 * The hyperparameters are set according to the passed values.
	 * @param phi
	 * @param eps
	 * @param decay The hyperparameter related to the smoothing of the gradient
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingSmoothedAdaDelta(double phi, double eps, double decay){
		return new GradientDescentOptimizerFactory(BestParamCriteria.BEST_OBJECTIVE, AdaptiveStrategy.ADADELTA, 0.0, phi, eps, decay);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with AdaDelta adaptive method.<br>
	 * The epsilon hyperparameter will decay when no progress is seen after some number of iterations (specified by {@link GradientDescentOptimizer#maxStagnantIterCount}).<br>
	 * Note that this is well-defined only when full-batch is used (i.e., no mini-batch)<br>
	 * The hyperparameters are set according to the passed values.
	 * @param phi
	 * @param eps
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingAdaDeltaDecaying(double phi, double eps){
		return new GradientDescentOptimizerFactory(BestParamCriteria.BEST_OBJECTIVE, AdaptiveStrategy.ADADELTA_DECAYING, 0.0, phi, eps);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with AdaDelta adaptive method.<br>
	 * The epsilon hyperparameter will decay when no progress is seen after some number of iterations (specified by {@link GradientDescentOptimizer#maxStagnantIterCount}).<br>
	 * Note that this is well-defined only when full-batch is used (i.e., no mini-batch)<br>
	 * The hyperparameters are set according to the passed values.
	 * @param phi
	 * @param eps
	 * @param decay The smoothing coefficient
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingSmoothedAdaDeltaDecaying(double phi, double eps, double decay){
		return new GradientDescentOptimizerFactory(BestParamCriteria.BEST_OBJECTIVE, AdaptiveStrategy.ADADELTA_DECAYING, 0.0, phi, eps, decay);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with AdaDelta adaptive method,
	 * then changes to ADAGRAD when no progress is seen after some number of iterations (specified by {@link GradientDescentOptimizer#maxStagnantIterCount}).<br>
	 * Note that this is well-defined only when full-batch is used (i.e., no mini-batch)<br>
	 * The hyperparameters are set according to the passed values.
	 * @param learningRate
	 * @param phi
	 * @param eps
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingAdaDeltaThenAdaGrad(double learningRate, double phi, double eps){
		return new GradientDescentOptimizerFactory(BestParamCriteria.BEST_OBJECTIVE, AdaptiveStrategy.ADADELTA_THEN_ADAGRAD, learningRate, phi, eps);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with AdaDelta adaptive method,
	 * then stops when no progress is seen after some number of iterations (specified by {@link GradientDescentOptimizer#maxStagnantIterCount}).<br>
	 * Note that this is well-defined only when full-batch is used (i.e., no mini-batch)<br>
	 * The hyperparameters are set according to the passed values.
	 * @param phi
	 * @param eps
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingAdaDeltaThenStop(double phi, double eps){
		return new GradientDescentOptimizerFactory(BestParamCriteria.BEST_OBJECTIVE, AdaptiveStrategy.ADADELTA_THEN_STOP, 0.0, phi, eps);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with AdaDelta adaptive method,
	 * then changes to gradient descent when no progress is seen after some number of iterations (specified by {@link GradientDescentOptimizer#maxStagnantIterCount}).<br>
	 * Note that this is well-defined only when full-batch is used (i.e., no mini-batch)<br>
	 * By default the hyperparameters are set as follows:
	 * <ol>
	 * <li>learning rate = {@value #DEFAULT_LEARNING_RATE}</li>
	 * <li>phi = {@value #DEFAULT_ADADELTA_PHI}</li>
	 * <li>eps = {@value #DEFAULT_ADADELTA_EPS}</li>
	 * <li>decay = {@value #DEFAULT_ADADELTA_GRAD_DECAY}</li>
	 * </ol>
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingSmoothedAdaDeltaThenGD(){
		return new GradientDescentOptimizerFactory(BestParamCriteria.BEST_OBJECTIVE, AdaptiveStrategy.ADADELTA_THEN_GD, DEFAULT_LEARNING_RATE, DEFAULT_ADADELTA_PHI, DEFAULT_ADADELTA_EPS, DEFAULT_ADADELTA_GRAD_DECAY);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with AdaDelta adaptive method,
	 * then changes to gradient descent when no progress is seen after some number of iterations (specified by {@link GradientDescentOptimizer#maxStagnantIterCount}).<br>
	 * Note that this is well-defined only when full-batch is used (i.e., no mini-batch)<br>
	 * The hyperparameters are set according to the passed values.
	 * @param learningRate
	 * @param phi
	 * @param eps
	 * @param decay The smoothing coefficient
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingSmoothedAdaDeltaThenGD(double learningRate, double phi, double eps, double decay){
		return new GradientDescentOptimizerFactory(BestParamCriteria.BEST_OBJECTIVE, AdaptiveStrategy.ADADELTA_THEN_GD, learningRate, phi, eps, decay);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with AdaDelta adaptive method,
	 * then changes to ADAGRAD when no progress is seen after some number of iterations (specified by {@link GradientDescentOptimizer#maxStagnantIterCount}).<br>
	 * Note that this is well-defined only when full-batch is used (i.e., no mini-batch)<br>
	 * The hyperparameters are set according to the passed values.
	 * @param learningRate
	 * @param phi
	 * @param eps
	 * @param decay The smoothing coefficient
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingSmoothedAdaDeltaThenAdaGrad(double learningRate, double phi, double eps, double decay){
		return new GradientDescentOptimizerFactory(BestParamCriteria.BEST_OBJECTIVE, AdaptiveStrategy.ADADELTA_THEN_ADAGRAD, learningRate, phi, eps, decay);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with AdaDelta adaptive method,
	 * then stops when no progress is seen after some number of iterations (specified by {@link GradientDescentOptimizer#maxStagnantIterCount}).<br>
	 * Note that this is well-defined only when full-batch is used (i.e., no mini-batch)<br>
	 * By default the hyperparameters are set as follows:
	 * <ol>
	 * <li>phi = {@value #DEFAULT_ADADELTA_PHI}</li>
	 * <li>eps = {@value #DEFAULT_ADADELTA_EPS}</li>
	 * <li>decay = {@value #DEFAULT_ADADELTA_GRAD_DECAY}</li>
	 * </ol>
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingSmoothedAdaDeltaThenStop(){
		return new GradientDescentOptimizerFactory(BestParamCriteria.BEST_OBJECTIVE, AdaptiveStrategy.ADADELTA_THEN_STOP, 0.0, DEFAULT_ADADELTA_PHI, DEFAULT_ADADELTA_EPS, DEFAULT_ADADELTA_GRAD_DECAY);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with AdaDelta adaptive method,
	 * then stops when no progress is seen after some number of iterations (specified by {@link GradientDescentOptimizer#maxStagnantIterCount}).<br>
	 * Note that this is well-defined only when full-batch is used (i.e., no mini-batch)<br>
	 * The hyperparameters are set according to the passed values.
	 * @param phi
	 * @param eps
	 * @param decay The smoothing coefficient
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingSmoothedAdaDeltaThenStop(double phi, double eps, double decay){
		return new GradientDescentOptimizerFactory(BestParamCriteria.BEST_OBJECTIVE, AdaptiveStrategy.ADADELTA_THEN_STOP, 0.0, phi, eps, decay);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with AdaDelta adaptive method,
	 * then changes to normal gradient descent when no progress is seen after some number of iterations (specified by {@link GradientDescentOptimizer#maxStagnantIterCount}).<br>
	 * Note that this is well-defined only when full-batch is used (i.e., no mini-batch)<br>
	 * The hyperparameters are set according to the passed values.
	 * @param phi
	 * @param eps
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingAdaDeltaThenGD(double learningRate, double phi, double eps){
		return new GradientDescentOptimizerFactory(BestParamCriteria.BEST_OBJECTIVE, AdaptiveStrategy.ADADELTA_THEN_GD, learningRate, phi, eps);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with RMSProp adaptive method.<br>
	 * By default the hyperparameters are set as follows:
	 * <ol>
	 * <li>learningRate = {@value #DEFAULT_LEARNING_RATE}</li>
	 * <li>rmsPropDecay = {@value #DEFAULT_RMSPROP_DECAY}</li>
	 * <li>rmsPropEps = {@value #DEFAULT_RMSPROP_EPS}</li>
	 * </ol>
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingRMSProp(){
		return new GradientDescentOptimizerFactory(BestParamCriteria.BEST_OBJECTIVE, AdaptiveStrategy.RMSPROP, DEFAULT_LEARNING_RATE, DEFAULT_LEARNING_RATE_DECAY, 0.0, 0.0, 0.0, DEFAULT_RMSPROP_DECAY, DEFAULT_RMSPROP_EPS, 0.0, 0.0, 0.0, false, 0);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with RMSProp adaptive method.<br>
	 * The hyperparameters are set according to the passed values.
	 * @param learningRate
	 * @param rmsPropDecay
	 * @param rmsPropEps
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingRMSProp(double learningRate, double rmsPropDecay, double rmsPropEps){
		return new GradientDescentOptimizerFactory(BestParamCriteria.BEST_OBJECTIVE, AdaptiveStrategy.RMSPROP, learningRate, DEFAULT_LEARNING_RATE_DECAY, 0.0, 0.0, 0.0, rmsPropDecay, rmsPropEps, 0.0, 0.0, 0.0, false, 0);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with AdaM adaptive method.<br>
	 * By default the hyperparameters are set as follows:
	 * <ol>
	 * <li>learningRate = {@value #DEFAULT_LEARNING_RATE}</li>
	 * <li>adamBeta1 = {@value #DEFAULT_ADAM_BETA1}</li>
	 * <li>adamBeta2 = {@value #DEFAULT_ADAM_BETA2}</li>
	 * <li>adamEps = {@value #DEFAULT_ADAM_EPS}</li>
	 * </ol>
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingAdaM(){
		return new GradientDescentOptimizerFactory(BestParamCriteria.BEST_OBJECTIVE, AdaptiveStrategy.ADAM, DEFAULT_LEARNING_RATE, DEFAULT_LEARNING_RATE_DECAY, 0.0, 0.0, 0.0, 0.0, 0.0, DEFAULT_ADAM_BETA1, DEFAULT_ADAM_BETA2, DEFAULT_ADAM_EPS, false, 0);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with AdaM adaptive method.<br>
	 * The hyperparameters are set according to the passed values.
	 * @param learningRate
	 * @param adamBeta1
	 * @param adamBeta2
	 * @param adamEps
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingAdaM(double learningRate, double adamBeta1, double adamBeta2, double adamEps){
		return new GradientDescentOptimizerFactory(BestParamCriteria.BEST_OBJECTIVE, AdaptiveStrategy.ADAM, learningRate, DEFAULT_LEARNING_RATE_DECAY, 0.0, 0.0, 0.0, 0.0, 0.0, adamBeta1, adamBeta2, adamEps, false, 0);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with AdaM adaptive method,
	 * then stops when no progress is seen after some number of iterations (specified by {@link GradientDescentOptimizer#maxStagnantIterCount}).<br>
	 * Note that this is well-defined only when full-batch is used (i.e., no mini-batch)<br>
	 * The hyperparameters are set according to the passed values.
	 * @param learningRate
	 * @param adamBeta1
	 * @param adamBeta2
	 * @param adamEps
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingAdaMThenStop(double learningRate, double adamBeta1, double adamBeta2, double adamEps){
		return new GradientDescentOptimizerFactory(BestParamCriteria.BEST_OBJECTIVE, AdaptiveStrategy.ADAM_THEN_STOP, learningRate, DEFAULT_LEARNING_RATE_DECAY, 0.0, 0.0, 0.0, 0.0, 0.0, adamBeta1, adamBeta2, adamEps, false, 0);
	}
	
	/**
	 * Return the factory object to create a gradient descent optimizer.<br>
	 * The returned factory will create instances of GradientDescentOptimizer with AdaM adaptive method,
	 * then stops when no progress is seen after some number of iterations (specified by {@link GradientDescentOptimizer#maxStagnantIterCount}).<br>
	 * Note that this is well-defined only when full-batch is used (i.e., no mini-batch)<br>
	 * The hyperparameters are set according to the passed values.
	 * @return
	 */
	public static GradientDescentOptimizerFactory getGradientDescentFactoryUsingAdaMThenStop(){
		return getGradientDescentFactoryUsingAdaMThenStop(DEFAULT_LEARNING_RATE*5, DEFAULT_ADAM_BETA1, DEFAULT_ADAM_BETA2, DEFAULT_ADAM_EPS);
	}
	
	public abstract Optimizer create(int numWeights);
	
	public Optimizer create(int numWeights, TIntObjectHashMap<TIntObjectHashMap<TIntIntHashMap>> featureIntMap, StringIndex stringIndex){
		return create(numWeights);
	}

}
