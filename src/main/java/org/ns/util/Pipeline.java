/**
 *
 */
package org.ns.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.ns.commons.ml.opt.GradientDescentOptimizer;
import org.ns.commons.ml.opt.OptimizerFactory;
import org.ns.commons.ml.opt.OptimizerFactory.OptimizerFactoryEnum;
import org.ns.commons.types.Instance;
import org.ns.hypergraph.DiscriminativeNetworkModel;
import org.ns.hypergraph.FeatureManager;
import org.ns.hypergraph.GenerativeNetworkModel;
import org.ns.hypergraph.GlobalNetworkParam;
import org.ns.hypergraph.NetworkCompiler;
import org.ns.hypergraph.NetworkConfig;
import org.ns.hypergraph.NetworkModel;
import org.ns.hypergraph.NetworkConfig.ModelType;
import org.ns.hypergraph.NetworkConfig.StoppingCriteria;
import org.ns.util.instance_parser.InstanceParser;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentAction;
import net.sourceforge.argparse4j.inf.ArgumentChoice;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

/**
 * The generic main class to all StatNLP models.
 */
public abstract class Pipeline<THIS extends Pipeline<?>> {
	
	public static final Logger LOGGER = GeneralUtils.createLogger(Pipeline.class);
	
	/** The information required to build a proper pipeline */
	public InstanceParser instanceParser;
	public Class<? extends InstanceParser> instanceParserClass;
	public NetworkCompiler networkCompiler;
	public Class<? extends NetworkCompiler> networkCompilerClass;
	public FeatureManager featureManager;
	public Class<? extends FeatureManager> featureManagerClass;
	//public Class<? extends VisualizationViewerEngine> visualizerClass;
	public NetworkModel networkModel;
	public GlobalNetworkParam param;
	public OptimizerFactory optimizerFactory;
	
	/** */
	public Consumer<Instance[]> evaluateCallback;
	
	/** Records the start time of the pipeline */
	private long timer = 0;
	
	private String currentTaskName;

	// Argument Parser-related class members
	public Map<String, Object> parameters;
	public String[] unprocessedArgs;
	public ArgumentParser argParser;
	protected HashMap<String, Argument> argParserObjects = new HashMap<String, Argument>();
	
	// Main tasks
	public static final String TASK_NOOP = "noop";
	public static final String TASK_TRAIN = "train";
	public static final String TASK_TUNE = "tune";
	public static final String TASK_TEST = "test";
	public static final String TASK_EVALUATE = "evaluate";
	public static final String TASK_VISUALIZE = "visualize";
	
	// Auxiliary Tasks
	public static final String TASK_LOAD_MODEL = "loadModel";
	public static final String TASK_SAVE_MODEL = "saveModel";
	public static final String TASK_READ_INSTANCES = "readInstances";
	public static final String TASK_SAVE_PREDICTIONS = "savePredictions";
	public static final String TASK_EXTRACT_FEATURES = "extractFeatures";
	public static final String TASK_WRITE_FEATURES = "writeFeatures";
	
	/** The list of tasks which have been registered and can be run. */
	public final LinkedHashMap<String, Runnable> registeredTasks = new LinkedHashMap<String, Runnable>();
	
	/** The list storing the list of tasks to be executed in the current pipeline. */
	public List<String> taskList;
	
	public Pipeline(){
		taskList = new ArrayList<String>();
		initRegisteredTasks();
		initArgumentParser();
	}
	
	private void initRegisteredTasks(){
		// No-op task
		registerTask(TASK_NOOP, new Runnable(){public void run() {}});
		
		// Main tasks
		registerTask(TASK_TRAIN, this::Train);
		registerTask(TASK_TUNE, this::Tune);
		registerTask(TASK_TEST, this::Test);
		registerTask(TASK_EVALUATE, this::Evaluate);
		registerTask(TASK_VISUALIZE, this::Visualize);
		
		// Auxiliary tasks
		registerTask(TASK_LOAD_MODEL, this::LoadModel);
		registerTask(TASK_SAVE_MODEL, this::SaveModel);
		registerTask(TASK_READ_INSTANCES, this::ReadInstances);
		registerTask(TASK_SAVE_PREDICTIONS, this::SavePredictions);
		registerTask(TASK_EXTRACT_FEATURES, this::ExtractFeatures);
		registerTask(TASK_WRITE_FEATURES, this::WriteFeatures);
	}
	
	/**
	 * Registers a task.
	 * @param taskName
	 * @param action
	 */
	protected void registerTask(String taskName, Runnable action){
		registeredTasks.put(taskName, action);
	}
	
	/**
	 * Whether a number is a double value.
	 * @param val
	 * @return
	 */
	private static boolean isDouble(String val){
		try{
			Double.valueOf(val);
			return true;
		} catch (NumberFormatException e){
			return false;
		}
	}
	
	private static String[] argsAsArray(Object value){
		String[] args;
		if(value instanceof List){
			args = ((List<?>)value).toArray(new String[0]);
		} else {
			args = (String[])value;
		}
		return args;
	}
	
	private void initArgumentParser(){
		argParser = ArgumentParsers.newArgumentParser("StatNLP Framework: "+this.getClass().getSimpleName())
                .defaultHelp(true)
                .description("Execute the model.");
		
		// The tasks to be executed
		argParserObjects.put("tasks", argParser.addArgument("tasks")
				.type(String.class)
				.metavar("tasks")
				.choices(registeredTasks.keySet())
				.nargs("+")
				.action(new ArgumentAction(){

					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						attrs.put("tasks", value);
						String[] args = argsAsArray(value);
						try{
							addTasks(args);
						} catch (IllegalArgumentException e){
							throw new ArgumentParserException(e, argParser);
						}
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return true;
					}
					
				})
				.help("The list of tasks to be executed. The registered tasks are:\n"
						+ registeredTasks.keySet()));
		
		// Model Settings
		argParserObjects.put("--instanceParserClass", argParser.addArgument("--instanceParserClass")
				.type(String.class)
				.action(new ArgumentAction(){

					@SuppressWarnings("unchecked")
					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						try {
							withInstanceParser((Class<? extends InstanceParser>)Class.forName((String)value));
						} catch (ClassNotFoundException e) {
							throw LOGGER.throwing(new RuntimeException(e));
						}
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return true;
					}
					
				})
				.help("The class of the instance parser to be used."));
		argParserObjects.put("--networkCompilerClass", argParser.addArgument("--networkCompilerClass")
				.type(String.class)
				.action(new ArgumentAction(){

					@SuppressWarnings("unchecked")
					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						try {
							withNetworkCompiler((Class<? extends NetworkCompiler>)Class.forName((String)value));
						} catch (ClassNotFoundException e) {
							throw LOGGER.throwing(new RuntimeException(e));
						}
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return true;
					}
					
				})
				.help("The class of the network compiler to be used."));
		argParserObjects.put("--featureManagerClass", argParser.addArgument("--featureManagerClass")
				.type(String.class)
				.action(new ArgumentAction(){

					@SuppressWarnings("unchecked")
					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						try {
							withFeatureManager((Class<? extends FeatureManager>)Class.forName((String)value));
						} catch (ClassNotFoundException e) {
							throw LOGGER.throwing(new RuntimeException(e));
						}
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return true;
					}
					
				})
				.help("The class of the feature manager to be used."));
		argParserObjects.put("--evaluateCallback", argParser.addArgument("--evaluateCallback")
				.type(String.class)
				.action(new ArgumentAction(){

					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						try {
							String[] tokens = ((String)value).split("::");
							String className = tokens[0];
							String funcName = tokens[1];
							Class<?> clazz = Class.forName(className);
							final Method evaluateCallback = clazz.getDeclaredMethod(funcName, Instance[].class);
							withEvaluateCallback(new Consumer<Instance[]>(){

								@Override
								public void accept(Instance[] t) {
									try {
										evaluateCallback.invoke(null, (Object)t);
									} catch (IllegalAccessException | IllegalArgumentException
											| InvocationTargetException e) {
										e.printStackTrace();
									}
								}
								
							});
						} catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
							throw LOGGER.throwing(new RuntimeException(e));
						}
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return true;
					}
					
				})
				.help("The evaluator function to be used in the format CLASS_NAME::FUNCTION_NAME."));
		
		
		// Training Settings
		argParserObjects.put("--maxIter", argParser.addArgument("--maxIter")
				.type(Integer.class)
				.setDefault(1000)
				.action(new ArgumentAction(){

					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						withMaxIter((int)value);
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return true;
					}
					
				})
				.help("The maximum number of training iterations."));
		argParserObjects.put("--modelType", argParser.addArgument("--modelType")
				.type(ModelType.class)
				.choices(ModelType.values())
				.setDefault(NetworkConfig.MODEL_TYPE)
				.action(new ArgumentAction(){

					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						withModelType((ModelType)value);
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return true;
					}
					
				})
				.help("The type of the model during training. "
						+ "Model types SSVM and SOFTMAX_MARGIN require cost function to be defined"));
		argParserObjects.put("--stoppingCriteria", argParser.addArgument("--stoppingCriteria")
				.type(StoppingCriteria.class)
				.choices(StoppingCriteria.values())
				.setDefault(NetworkConfig.STOPPING_CRITERIA)
				.action(new ArgumentAction(){

					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						withStoppingCriteria((StoppingCriteria)value);
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return true;
					}
					
				})
				.help("The stopping criteria to be used.\n"
						+ "MAX_ITERATION_REACHED: Stop when the specified number of --maxIter is reached.\n"
						+ "SMALL_ABSOLUTE_CHANGE: Stop when the change in objective function is small.\n"
						+ "SMALL_RELATIVE_CHANGE: Stop when the ratio of change of objective function is small for three consecutive iterations."));
		argParserObjects.put("--objtol", argParser.addArgument("--objtol")
				.type(Double.class)
				.setDefault(NetworkConfig.OBJTOL)
				.action(new ArgumentAction(){

					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						withObjtol((double)value);
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return true;
					}
					
				})
				.help("The minimum change in objective function to be considered not converged yet."));
		argParserObjects.put("--margin", argParser.addArgument("--margin")
				.type(Double.class)
				.setDefault(NetworkConfig.MARGIN)
				.action(new ArgumentAction(){

					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						withMargin((double)value);
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return true;
					}
					
				})
				.help("The margin for margin-based methods (SSVM and SOFTMAX_MARGIN)."));
		argParserObjects.put("--nodeMismatchCost", argParser.addArgument("--nodeMismatchCost")
				.type(Double.class)
				.setDefault(NetworkConfig.NODE_COST)
				.action(new ArgumentAction(){

					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						withNodeMismatchCost((double)value);
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return true;
					}
					
				})
				.help("The cost for a node mismatch in cost-augmented models (SSVM and SOFTMAX_MARGIN)."));
		argParserObjects.put("--edgeMismatchCost", argParser.addArgument("--edgeMismatchCost")
				.type(Double.class)
				.setDefault(NetworkConfig.EDGE_COST)
				.action(new ArgumentAction(){

					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						withEdgeMismatchCost((double)value);
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return true;
					}
					
				})
				.help("The cost for a edge mismatch in cost-augmented models (SSVM and SOFTMAX_MARGIN)."));
		argParserObjects.put("--weightInit", argParser.addArgument("--weightInit")
				.type(String.class)
				.setDefault(new String[]{"0.0"})
				.choices(new ArgumentChoice(){
					private final List<String> allowedArgs = Arrays.asList(new String[]{"random"});

					@Override
					public boolean contains(Object val) {
						try{
							if(isDouble((String)val)){
								return true;
							}
							if(allowedArgs.contains((String)val)){
								return true;
							}
						} catch (Exception e){
							return false;
						}
						return false;
					}

					@Override
					public String textualFormat() {
						return "?";
					}
					
				})
				.action(new ArgumentAction(){

					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						String[] args = argsAsArray(value);
						try{
							double initialWeight = Double.parseDouble((String)args[0]);
							withWeightInit(initialWeight);
						} catch (NumberFormatException e){
							if(args[0].equals("random")){
								if(args.length > 1){
									int seed = Integer.parseInt(args[1]);
									withWeightInitRandom(true, seed);
								} else {
									withWeightInitRandom(true);
								}
							} else {
								try{
									withWeightInit(args);
								} catch (Exception e1){
									throw new ArgumentParserException(e1, argParser);
								}
							}
						}
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return true;
					}
					
				})
				.nargs("+")
				.help("The margin for margin-based methods (SSVM and SOFTMAX_MARGIN).\n"
						+ "Use --weightInit <numeric> to specify an initial value to all weights,\n"
						+ "Use --weightInit random [optional_seed] to randomly assign values to all"
						+ "weights using the given seed."));
		argParserObjects.put("--useGenerativeModel", argParser.addArgument("--useGenerativeModel")
				.setDefault(NetworkConfig.TRAIN_MODE_IS_GENERATIVE)
				.action(new ArgumentAction(){

					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						withUseGenerativeModel(true);
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return false;
					}
					
				})
				.help("Whether to use generative model (like HMM) or discriminative model (like CRF)."));
		argParserObjects.put("--optimizer", argParser.addArgument("--optimizer")
				.action(new ArgumentAction(){

					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						@SuppressWarnings("unchecked")
						String[] tokens = ((ArrayList<String>)value).toArray(new String[0]);
						double[] params = new double[tokens.length-1];
						for(int i=0; i<params.length; i++){
							params[i] = Double.valueOf(tokens[i+1]);
						}
						OptimizerFactoryEnum optimizerFactoryType = OptimizerFactoryEnum.valueOf(tokens[0].toUpperCase());
						OptimizerFactory optimizerFactory = null;
						switch(optimizerFactoryType){
						case LBFGS:
							optimizerFactory = OptimizerFactory.getLBFGSFactory();
							break;
						case GD:
							if(params.length > 0){
								optimizerFactory = OptimizerFactory.getGradientDescentFactory(params[0]);
							} else {
								optimizerFactory = OptimizerFactory.getGradientDescentFactory();
							}
							break;
						case GD_ADAGRAD:
							if(params.length > 0){
								optimizerFactory = OptimizerFactory.getGradientDescentFactoryUsingAdaGrad(params[0]);
							} else {
								optimizerFactory = OptimizerFactory.getGradientDescentFactoryUsingAdaGrad();
							}
							break;
						case GD_ADADELTA:
							if(params.length > 0){
								optimizerFactory = OptimizerFactory.getGradientDescentFactoryUsingAdaDelta(params[0], params[1]);
							} else {
								optimizerFactory = OptimizerFactory.getGradientDescentFactoryUsingAdaDelta();
							}
							break;
						case GD_RMSPROP:
							if(params.length > 0){
								optimizerFactory = OptimizerFactory.getGradientDescentFactoryUsingRMSProp(params[0], params[1], params[2]);
							} else {
								optimizerFactory = OptimizerFactory.getGradientDescentFactoryUsingRMSProp();
							}
							break;
						case GD_ADAM:
							if(params.length > 0){
								optimizerFactory = OptimizerFactory.getGradientDescentFactoryUsingAdaM(params[0], params[1], params[2], params[3]);
							} else {
								optimizerFactory = OptimizerFactory.getGradientDescentFactoryUsingAdaM();
							}
							break;
						}
						withOptimizerFactory(optimizerFactory);
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return true;
					}
					
				})
				.nargs("+")
				.help("Specifies the optimizer used. The options are:\n"
						+ "- LBFGS\n"
						+ "- GD [learning_rate] (default: "+OptimizerFactory.DEFAULT_LEARNING_RATE+")\n"
						+ "- GD_ADAGRAD [learning_rate] (default: "+OptimizerFactory.DEFAULT_LEARNING_RATE+")\n"
						+ "- GD_ADADELTA [phi eps] (default: "+OptimizerFactory.DEFAULT_ADADELTA_PHI+" "+OptimizerFactory.DEFAULT_ADADELTA_EPS+")\n"
						+ "- GD_RMSPROP [learning_rate decay_rate eps] (default: "+OptimizerFactory.DEFAULT_LEARNING_RATE+" "+OptimizerFactory.DEFAULT_RMSPROP_DECAY+" "+OptimizerFactory.DEFAULT_RMSPROP_EPS+")\n"
						+ "- GD_ADAM [learning_rate beta1 beta2 eps] (default: "+OptimizerFactory.DEFAULT_LEARNING_RATE+" "+OptimizerFactory.DEFAULT_ADAM_BETA1+" "+OptimizerFactory.DEFAULT_ADAM_BETA2+" "+OptimizerFactory.DEFAULT_ADAM_EPS+")"));
		argParserObjects.put("--useBatchTraining", argParser.addArgument("--useBatchTraining")
				.setDefault(NetworkConfig.USE_BATCH_TRAINING)
				.action(new ArgumentAction(){

					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						withUseBatchTraining(true);
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return false;
					}
					
				})
				.help("Whether to use mini-batches during training."));
		argParserObjects.put("--useRandomBatch", argParser.addArgument("--useRandomBatch")
				.setDefault(false)
				.action(new ArgumentAction(){

					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						withUseRandomBatch(true);
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return false;
					}
					
				})
				.help("Whether to pick mini-batches randomly for each iteration."));
		argParserObjects.put("--batchSize", argParser.addArgument("--batchSize")
				.type(Integer.class)
				.setDefault(NetworkConfig.BATCH_SIZE)
				.action(new ArgumentAction(){

					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						try{
							withBatchSize((int)value);
						} catch (Exception e){
							throw new ArgumentParserException(e, argParser);
						}
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return true;
					}
					
				})
				.help("The size of batch to be used, should be greater than 0."));
		argParserObjects.put("--l2", argParser.addArgument("--l2")
				.type(Double.class)
				.setDefault(NetworkConfig.L2_REGULARIZATION_CONSTANT)
				.action(new ArgumentAction(){

					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						withL2((double)value);
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return true;
					}
					
				})
				.help("The L2 regularization value."));
		
		// Testing settings
		argParserObjects.put("--touchTestSeparately", argParser.addArgument("--touchTestSeparately")
				.type(Boolean.class)
				.setDefault(false)
				.action(new ArgumentAction(){

					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						withTouchTestSeparately((boolean)value);
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return true;
					}
					
				})
				.help("Whether to extract features from all test data before decoding any instance."));
		argParserObjects.put("--predictTopK", argParser.addArgument("--predictTopK")
				.type(Integer.class)
				.setDefault(1)
				.action(new ArgumentAction(){

					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						withPredictTopK((int)value);
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return true;
					}
					
				})
				.help("The number of predictions to be made during testing."));

		// Threading Settings
		argParserObjects.put("--numThreads", argParser.addArgument("--numThreads")
				.type(Integer.class)
				.setDefault(NetworkConfig.NUM_THREADS)
				.action(new ArgumentAction(){

					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						withNumThreads((int)value);
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return true;
					}
					
				})
				.help("The number of threads to be used."));
		
		// Feature Extraction
		argParserObjects.put("--serialTouch", argParser.addArgument("--serialTouch")
				.setDefault(!NetworkConfig.PARALLEL_FEATURE_EXTRACTION)
				.action(new ArgumentAction(){

					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						withParallelFeatureExtraction(false);
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return false;
					}
					
				})
				.help("Whether to serialize the feature extraction process. "
						+ "By default the feature extraction is parallelized, which is faster."));
		argParserObjects.put("--touchLabeledOnly", argParser.addArgument("--touchLabeledOnly")
				.setDefault(NetworkConfig.BUILD_FEATURES_FROM_LABELED_ONLY)
				.action(new ArgumentAction(){

					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						withExtractFeaturesFromLabeledOnly(true);
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return false;
					}
					
				})
				.help("Whether to define the feature set based on the labeled data only. "
						+ "By default the feature set is created based on all possibilities "
						+ "(e.g., all possible transitions in linear-chain CRF vs only seen transitions)"));
		argParserObjects.put("--attemptMemorySaving", argParser.addArgument("--attemptMemorySaving")
				.setDefault(NetworkConfig.AVOID_DUPLICATE_FEATURES)
				.action(new ArgumentAction(){

					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						withAttemptMemorySaving(true);
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return false;
					}
					
				})
				.help("Whether to attempt to reduce memory usage. "
						+ "The actual saving depends on the feature extractor implementation."));
		
		// Other Settings
		argParserObjects.put("--debugMode", argParser.addArgument("--debugMode")
				.setDefault(NetworkConfig.DEBUG_MODE)
				.action(new ArgumentAction(){

					@Override
					public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
							Object value) throws ArgumentParserException {
						setParameter(flag.substring(2), value);
						withDebugMode(true);
					}

					@Override
					public void onAttach(Argument arg) {}

					@Override
					public boolean consumeArgument() {
						return false;
					}
					
				})
				.help("Whether to enable debug mode."));
	}

	@SuppressWarnings("unchecked")
	public THIS getThis(){
		return (THIS)this;
	}
	
	/**
	 * Adds a task to be executed.
	 * @param task The task to be executed. The task has to be registered.
	 * @return
	 * @throws IllegalArgumentException If the task is not registered.
	 */
	public THIS addTask(String task) throws IllegalArgumentException {
		if(!registeredTasks.containsKey(task)){
			throw new IllegalArgumentException("Unrecognized task: "+task);
		}
		taskList.add(task);
		return getThis();
	}

	/**
	 * Adds a list of tasks to be executed in order.
	 * @param tasks The tasks to be executed. The tasks have to be registered.
	 * @return
	 * @throws IllegalArgumentException If some of the tasks are not registered.
	 */
	public THIS addTasks(String... tasks) throws IllegalArgumentException {
		List<String> unknownTasks = new ArrayList<String>();
		for(String task: tasks){
			if(!registeredTasks.containsKey(task)){
				unknownTasks.add(task);
			}
		}
		if(!unknownTasks.isEmpty()){
			throw new IllegalArgumentException("Unrecognized tasks: "+unknownTasks);
		}
		for(String task: tasks){
			taskList.add(task);
		}
		return getThis();
	}
	
	/**
	 * With the specified maximum number of iterations the training should go.
	 * @param maxIter The maximum number of iterations.
	 * @return
	 */
	public THIS withMaxIter(int maxIter){
		return withParameter("maxIter", maxIter);
	}
	
	/**
	 * With the specified type of the objective function.
	 * @param modelType
	 * @return
	 * @see {@link NetworkConfig.ModelType}
	 */
	public THIS withModelType(ModelType modelType){
		NetworkConfig.MODEL_TYPE = modelType;
		return getThis();
	}
	
	/**
	 * With the specified stopping criteria while training.<br>
	 * This is only taken into account if {@link #withOptimizerFactory(OptimizerFactory)} is not specified.
	 * @param stoppingCriteria
	 * @return
	 */
	public THIS withStoppingCriteria(StoppingCriteria stoppingCriteria){
		NetworkConfig.STOPPING_CRITERIA = stoppingCriteria;
		return getThis();
	}
	
	/**
	 * With the specified tolerance in objective value change.<br>
	 * This is only relevant when {@link NetworkConfig.StoppingCriteria#SMALL_ABSOLUTE_CHANGE} is used. 
	 * @param objtol
	 * @return
	 */
	public THIS withObjtol(double objtol){
		NetworkConfig.OBJTOL = objtol;
		return getThis();
	}
	
	/**
	 * With the specified value for the margin.<br>
	 * This is only relevant when cost is used in the selected {@link NetworkConfig.ModelType}.
	 * @param margin
	 * @return
	 */
	public THIS withMargin(double margin){
		NetworkConfig.MARGIN = margin;
		return getThis();
	}
	
	/**
	 * With the specified cost when a node in prediction is not found in the gold network.<br>
	 * This is only relevant when cost is used in the selected {@link NetworkConfig.ModelType}.
	 * @param cost
	 * @return
	 */
	public THIS withNodeMismatchCost(double cost){
		NetworkConfig.NODE_COST = cost;
		return getThis();
	}

	/**
	 * With the specified cost when an edge in prediction is not found in the gold network.<br>
	 * This is only relevant when cost is used in the selected {@link NetworkConfig.ModelType}.
	 * @param cost
	 * @return
	 */
	public THIS withEdgeMismatchCost(double cost){
		NetworkConfig.EDGE_COST = cost;
		return getThis();
	}
	
	/**
	 * With the specified value as the initial value for the weights.
	 * @param initialValue
	 * @return
	 */
	public THIS withWeightInit(double initialValue){
		NetworkConfig.FEATURE_INIT_WEIGHT = initialValue;
		return getThis();
	}
	
	/**
	 * With random initialization of the weights.
	 * @return
	 */
	public THIS withWeightInitRandom(boolean randomInit){
		NetworkConfig.RANDOM_INIT_WEIGHT = randomInit;
		return getThis();
	}
	
	/**
	 * With random initialization of the weights, with the specified seed.
	 * @param seed
	 * @return
	 */
	public THIS withWeightInitRandom(boolean randomInit, int seed){
		NetworkConfig.RANDOM_INIT_WEIGHT = randomInit;
		NetworkConfig.RANDOM_INIT_FEATURE_SEED = seed;
		return getThis();
	}
	
	public THIS withWeightInit(String... args){
		throw new UnsupportedOperationException("Weight initialization parameter not recognized: "+Arrays.toString(args));
	}
	
	/**
	 * Whether to use a generative model.
	 * @param useGenerativeModel
	 * @return
	 */
	public THIS withUseGenerativeModel(boolean useGenerativeModel){
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = useGenerativeModel;
		return getThis();
	}

	/**
	 * With the specified optimizer factory to be used when training.
	 * @param factory
	 * @return
	 */
	public THIS withOptimizerFactory(OptimizerFactory factory){
		this.optimizerFactory = factory;
		return getThis();
	}
	
	/**
	 * Whether to use batch training.
	 * @param useBatchTraining
	 * @return
	 * @see #withBatchSize(int)
	 * @see #withUseRandomBatch(boolean)
	 */
	public THIS withUseBatchTraining(boolean useBatchTraining){
		NetworkConfig.USE_BATCH_TRAINING = useBatchTraining;
		return getThis();
	}
	
	/**
	 * Whether to randomize the batches per epoch when batch training is used.
	 * @param useRandomBatch
	 * @return
	 * @see #withUseBatchTraining(boolean)
	 */
	public THIS withUseRandomBatch(boolean useRandomBatch){
		NetworkConfig.RANDOM_BATCH = useRandomBatch;
		return getThis();
	}
	
	/**
	 * With the specified batch size.<br>
	 * This is only relevant when batch mode is used.
	 * @param batchSize
	 * @return
	 * @see #withUseBatchTraining(boolean)
	 * @throws IllegalArgumentException
	 */
	public THIS withBatchSize(int batchSize) throws IllegalArgumentException {
		if(batchSize <= 0){
			throw new IllegalArgumentException("--batchSize should be greater than 0.");
		}
		NetworkConfig.BATCH_SIZE = batchSize;
		return getThis();
	}
	
	/**
	 * With the specified l2-regularization parameter.
	 * @param l2
	 * @return
	 */
	public THIS withL2(double l2){
		NetworkConfig.L2_REGULARIZATION_CONSTANT = l2;
		return getThis();
	}
	
	/**
	 * Whether to extract features from all test data before decoding any instance.<br>
	 * By default it's true, to support neural network feature extractor.<br>
	 * If false, the features will be extracted as needed. Due to the code flow, setting this
	 * to false makes overall decoding decoding slightly faster.
	 * @param flag
	 * @return
	 */
	public THIS withTouchTestSeparately(boolean flag){
		NetworkConfig.FEATURE_TOUCH_TEST = flag;
		return getThis();
	}

	/**
	 * With the specified number of predictions made during testing.
	 * @param k
	 * @return
	 */
	public THIS withPredictTopK(int k){
		setParameter("predictTopK", k);
		return getThis();
	}
	
	/**
	 * With the specified number of threads used when training and testing.
	 * @param numThreads
	 * @return
	 */
	public THIS withNumThreads(int numThreads){
		NetworkConfig.NUM_THREADS = numThreads;
		return getThis();
	}
	
	/**
	 * Whether to extract features in parallel or serially.
	 * @param useParallelFeatureExtraction
	 * @return
	 */
	public THIS withParallelFeatureExtraction(boolean useParallelFeatureExtraction){
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = useParallelFeatureExtraction;
		return getThis();
	}
	
	/**
	 * Whether to extract features only from labeled networks.
	 * @param fromLabeledOnly
	 * @return
	 */
	public THIS withExtractFeaturesFromLabeledOnly(boolean fromLabeledOnly){
		NetworkConfig.BUILD_FEATURES_FROM_LABELED_ONLY = fromLabeledOnly;
		return getThis();
	}
	
	/**
	 * Whether to attempt to save memory by avoiding duplicate features.
	 * @param attempt
	 * @return
	 */
	public THIS withAttemptMemorySaving(boolean attempt){
		NetworkConfig.AVOID_DUPLICATE_FEATURES = attempt;
		return getThis();
	}
	
	/**
	 * Whether the framework should run in debug mode.
	 * @param debug
	 * @return
	 */
	public THIS withDebugMode(boolean debug){
		NetworkConfig.DEBUG_MODE = debug;
		return getThis();
	}
	
	/**
	 * With the specified instance parser.
	 * @param instanceParser
	 * @return
	 */
	public THIS withInstanceParser(InstanceParser instanceParser){
		setInstanceParser(instanceParser);
		return getThis();
	}
	
	/**
	 * With the specified instance parser class.
	 * @param instanceParserClass
	 * @return
	 */
	public THIS withInstanceParser(Class<? extends InstanceParser> instanceParserClass){
		setInstanceParserClass(instanceParserClass);
		return getThis();
	}

	/**
	 * With the specified network compiler.
	 * @param networkCompiler
	 * @return
	 */
	public THIS withNetworkCompiler(NetworkCompiler networkCompiler){
		setNetworkCompiler(networkCompiler);
		return getThis();
	}

	/**
	 * With the specified network compiler class
	 * @param networkCompilerClass
	 * @return
	 */
	public THIS withNetworkCompiler(Class<? extends NetworkCompiler> networkCompilerClass){
		setNetworkCompilerClass(networkCompilerClass);
		return getThis();
	}
	
	/**
	 * With the specified feature manager.
	 * @param featureManager
	 * @return
	 */
	public THIS withFeatureManager(FeatureManager featureManager){
		setFeatureManager(featureManager);
		return getThis();
	}
	
	/**
	 * With the specified feature manager class
	 * @param featureManagerClass
	 * @return
	 */
	public THIS withFeatureManager(Class<? extends FeatureManager> featureManagerClass){
		setFeatureManagerClass(featureManagerClass);
		return getThis();
	}
	
	/**
	 * With the specified visualizer class
	 * @param visualizerClass
	 * @return
	 */
	/*
	public THIS withVisualizerClass(Class<? extends VisualizationViewerEngine> visualizerClass){
		//setVisualizerClass(visualizerClass);
		return getThis();
	}*/
	
	/**
	 * With the specified parameter set.<br>
	 * This is the with* version of the {@link #setParameter(String, Object)} function.
	 * @param key
	 * @param value
	 * @return
	 */
	public THIS withParameter(String key, Object value){
		setParameter(key, value);
		return getThis();
	}
	
	/**
	 * With the specified function for evaluation
	 * @param evaluateCallback
	 * @return
	 */
	public THIS withEvaluateCallback(Consumer<Instance[]> evaluateCallback){
		this.evaluateCallback = evaluateCallback;
		return getThis();
	}
	
	/**
	 * Initialize InstanceParser in the context of current pipeline
	 */
	protected abstract InstanceParser initInstanceParser();
	
	protected void initAndSetInstanceParser(){
		setInstanceParser(initInstanceParser());
	}
	
	public void setInstanceParser(InstanceParser instanceParser){
		this.instanceParser = instanceParser;
	}
	
	public void setInstanceParserClass(Class<? extends InstanceParser> instanceParserClass){
		this.instanceParserClass = instanceParserClass;
	}
	
	/**
	 * Initialize NetworkCompiler in the context of current pipeline
	 * It will call getNetworkCompilerParameters() from Parser to get necessary parameters
	 */
	protected abstract NetworkCompiler initNetworkCompiler();
	
	protected void initAndSetNetworkCompiler(){
		setNetworkCompiler(initNetworkCompiler());
	}
	
	public void setNetworkCompiler(NetworkCompiler compiler){
		this.networkCompiler = compiler;
	}
	
	public void setNetworkCompilerClass(Class<? extends NetworkCompiler> networkCompilerClass){
		this.networkCompilerClass = networkCompilerClass;
	}
	
	/**
	 * Initialize FeatureManager in the context of current pipeline
	 * It will call getFeatureMgrParameters() from Parser to get necessary parameters
	 */
	protected abstract FeatureManager initFeatureManager();
	
	protected void initAndSetFeatureManager(){
		setFeatureManager(initFeatureManager());
	}
	
	public void setFeatureManagerClass(Class<? extends FeatureManager> featureManagerClass){
		this.featureManagerClass = featureManagerClass;
	}
	
	public void setFeatureManager(FeatureManager fm){
		this.featureManager = fm;
	}
	
	/*
	public void setVisualizerClass(Class<? extends VisualizationViewerEngine> visualizerClass){
		//this.visualizerClass = visualizerClass;
	}*/
	
	/**
	 * Initialize training stuff, including argument parsing, variable initialization
	 */
	protected abstract void initTraining();
	
	/**
	 * Initialize tuning stuff, including argument parsing, variable initialization
	 */
	protected abstract void initTuning();
	
	/**
	 * Initialize testing stuff, including argument parsing, variable initialization
	 */
	protected abstract void initTesting();
	
	/**
	 *  Initialize evaluation stuff, including argument parsing, variable initialization
	 */
	protected abstract void initEvaluation();
	
	/**
	 *  Initialize visualization stuff, including argument parsing, variable initialization
	 */
	protected abstract void initVisualization();
	
	public abstract Instance[] getInstancesForTraining();
	public abstract Instance[] getInstancesForTuning();
	public abstract Instance[] getInstancesForTesting();
	public abstract Instance[] getInstancesForEvaluation();
	public abstract Instance[] getInstancesForVisualization();
	
	/**
	 * Train the model on the given training instances
	 * @param trainInstances
	 */
	protected abstract void train(Instance[] trainInstances);

	/**
	 * Tune the model on the given development instances.
	 * @param devInstances
	 */
	protected abstract void tune(Instance[] devInstances);

	/**
	 * Test the model on the test instances
	 * @param testInstances
	 */
	protected abstract void test(Instance[] testInstances);

	/**
	 * Evaluate the prediction performance
	 * @param output
	 */
	protected abstract void evaluate(Instance[] output);
	
	/**
	 * Visualize the given instances
	 * @param instances Instances to be visualized
	 */
	protected abstract void visualize(Instance[] instances);
	
	/**
	 * Save the trained model into disk
	 * @throws IOException
	 */
	protected abstract void saveModel() throws IOException;

	/**
	 * Load the trained model into memory
	 * @throws IOException
	 */
	protected abstract void loadModel() throws IOException;

	/**
	 * The task of saving predictions
	 */
	protected abstract void savePredictions();
	
	/**
	 * The task of just extracting features
	 */
	protected abstract void extractFeatures(Instance[] instances);
	
	/**
	 * The task of just writing features to file
	 */
	protected abstract void writeFeatures(Instance[] instances);

	/**
	 * Returns the optimizer factory to be used during training<br>
	 * By default uses L-BFGS for those learning algorithm using softmax and not using batch, and
	 * in other cases uses gradient descent with AdaM with default hyperparameters, which stops
	 * after seeing no progress after {@link GradientDescentOptimizer#DEFAULT_MAX_STAGNANT_ITER_COUNT}
	 * iterations.
	 * @return
	 */
	protected OptimizerFactory getOptimizerFactory(){
		if(this.optimizerFactory != null){
			return this.optimizerFactory;
		}
		if(NetworkConfig.MODEL_TYPE.USE_SOFTMAX && NetworkConfig.USE_BATCH_TRAINING == false){
			return OptimizerFactory.getLBFGSFactory();
		} else {
			switch (NetworkConfig.STOPPING_CRITERIA){
			case SMALL_ABSOLUTE_CHANGE:
			case SMALL_RELATIVE_CHANGE:
				return OptimizerFactory.getGradientDescentFactoryUsingAdaMThenStop();
			case MAX_ITERATION_REACHED:
			default:
				return OptimizerFactory.getGradientDescentFactoryUsingAdaM();
			}
		}
	}
	
	/**
	 * Initialize the {@link GlobalNetworkParam} object
	 */
	protected void initGlobalNetworkParam(){
		this.param = new GlobalNetworkParam(getOptimizerFactory());
	}
	
	protected abstract void handleSaveModelError(Exception e);
	
	protected abstract void handleLoadModelError(Exception e);

	protected void initNetworkModel() {
		if(param == null){
			initGlobalNetworkParam();
		}
		if(featureManager == null){
			initAndSetFeatureManager();
		}
		if(networkCompiler == null){
			initAndSetNetworkCompiler();
		}
		if(instanceParser == null){
			initAndSetInstanceParser();
		}
		if(NetworkConfig.TRAIN_MODE_IS_GENERATIVE){
			networkModel = GenerativeNetworkModel.create(featureManager, networkCompiler, instanceParser);
		} else {
			networkModel = DiscriminativeNetworkModel.create(featureManager, networkCompiler, instanceParser);
		}
	}
	
	public void Train(){
		//defined by user
		initTraining();
		
		initGlobalNetworkParam();
		
		//defined by user
		initAndSetInstanceParser();
		
		Instance[] trainInstances = getInstancesForTraining();
		
		initAndSetNetworkCompiler();
		initAndSetFeatureManager();
		
		initNetworkModel();
		
		train(trainInstances);
		
		SaveModel();
	}

	public void Tune(){
	}
	
	public void Test(){
		//defined by user
		initTesting();
		
		LoadModel();

		initAndSetInstanceParser();
		initAndSetNetworkCompiler();
		initAndSetFeatureManager();

		Instance[] testInstances = getInstancesForTesting();
		for(int k = 0; k < testInstances.length; k++){
			testInstances[k].setUnlabeled();
		}
		
		test(testInstances);
	}
	
	public void Evaluate(){
		initEvaluation();
		Instance[] instanceForEvaluation = getInstancesForEvaluation();
		if(evaluateCallback == null){
			evaluate(instanceForEvaluation);
		} else {
			evaluateCallback.accept(instanceForEvaluation);
		}
	}
	
	public void Visualize(){
		initVisualization();
		Instance[] instances = getInstancesForVisualization();
		visualize(instances);
	}
	
	public void LoadModel() {
		try {
			//defined by user
			loadModel();
		} catch (IOException e) {
			handleLoadModelError(e);
		}
	}

	public void SaveModel() {
		try {
			saveModel();
		} catch (IOException e) {
			handleSaveModelError(e);
		}
	}
	
	public void ReadInstances(){
		getInstancesForTraining();
		getInstancesForTuning();
		getInstancesForTesting();
	}
	
	public void SavePredictions(){
		savePredictions();
	}
	
	public void ExtractFeatures(){
		Instance[] instances;
		instances = getInstancesForTraining();
		extractFeatures(instances);
		instances = getInstancesForTuning();
		extractFeatures(instances);
		instances = getInstancesForTesting();
		extractFeatures(instances);
	}
	
	public void WriteFeatures(){
		Instance[] instances;
		instances = getInstancesForTraining();
		writeFeatures(instances);
		instances = getInstancesForTuning();
		writeFeatures(instances);
		instances = getInstancesForTesting();
		writeFeatures(instances);
	}

	public THIS parseArgs(String[] args){
		return parseArgs(args, true);
	}
	
	public THIS parseArgs(String[] args, boolean retainExistingState){
//		// If we want to support typo in arguments
//		String[] mainArgs = null;
//		String[] restArgs = null;
//		for(int i=0; i<args.length; i++){
//			if(args[i].equals("--")){
//				mainArgs = new String[i];
//				restArgs = new String[args.length-i];
//				for(int j=0; j<i; j++){
//					mainArgs[j] = args[j];
//				}
//				for(int j=i+1; j<args.length; j++){
//					restArgs[j-i-1] = args[j];
//				}
//			}
//		}
//		if(mainArgs == null){
//			mainArgs = args;
//			restArgs = new String[0];
//		}
//    	parameters = argParser.parseArgsOrFail(mainArgs);
//    	unprocessedArgs = restArgs;
    	
		List<String> unknownArgs = new ArrayList<String>();
		try{
			if(retainExistingState){
				if(parameters == null){
					initParameters();
				}
				argParser.parseKnownArgs(args, unknownArgs, parameters);
			} else {
				parameters = argParser.parseKnownArgs(args, unknownArgs).getAttrs();
			}
		} catch (ArgumentParserException e){
			LOGGER.error(argParser.formatHelp());
			LOGGER.error(e);
			System.exit(1);
		}
		unprocessedArgs = unknownArgs.toArray(new String[unknownArgs.size()]);
		parseUnknownArgs(unprocessedArgs);
		return getThis();
	}
	
	public void parseUnknownArgs(String[] args){
		int argIdx = 0;
		while(argIdx < args.length){
			String flag = args[argIdx];
			argIdx += 1;
			if(flag.startsWith("--")){
				flag = flag.substring(2);
			} else if (flag.startsWith("-")){
				if(isDouble(flag)){
					LOGGER.warn("Ignoring number in argument: %s", flag);
					continue;
				} else {
					flag = flag.substring(1);
				}
			}
			String[] tokens = flag.split("=");
			if(tokens.length == 1){
				LOGGER.info("Setting unknown argument %s to true", tokens[0]);
				setParameter(tokens[0], true);
			} else if(tokens.length == 2){
				LOGGER.info("Setting unknown argument %s to %s", tokens[0], tokens[1]);
				setParameter(tokens[0], tokens[1]);
			}
//			// Consume arguments
//			List<Object> arguments = new ArrayList<Object>();
//			while(argIdx < args.length){
//				String nextFlag = args[argIdx];
//				if(nextFlag.startsWith("-")){
//					if(isDouble(nextFlag)){
//						arguments.add(Double.parseDouble(nextFlag));
//					} else {
//						break;
//					}
//				} else {
//					arguments.add(nextFlag);
//					argIdx += 1;
//				}
//			}
//			if(arguments.size() == 0){
//				LOGGER.info("Setting unknown argument %s to true", flag);
//				setParameter(flag, true);
//			} else if(arguments.size() == 1){
//				LOGGER.info("Setting unknown argument %s to %s", flag, arguments.get(0));
//				setParameter(flag, arguments.get(0));
//			} else {
//				LOGGER.info("Setting unknown argument %s to %s", flag, arguments);
//				setParameter(flag, arguments);
//			}
		}
	}
	
	protected void setCurrentTask(String task){
		currentTaskName = task;
	}
	
	protected String getCurrentTask(){
		return currentTaskName;
	}
	
	public void initExecute(){
		LOGGER.info("Tasks to be executed: %s", taskList);
		LOGGER.info("Configurations:\n%s\n%s", NetworkConfig.getConfig(), GeneralUtils.toPrettyString(parameters));
	}
	
	public void execute(){
		initExecute();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {}
		resetTimer();
		for(String task: taskList){
			Runnable action = registeredTasks.get(task);
			setCurrentTask(task);
			action.run();
		}
		long duration = getElapsedTime();
		LOGGER.info("Total execution time: %.3fs", duration/1.0e9);
	}
	
	public void execute(String[] args){
		execute(args, true);
	}
	
	public void execute(String[] args, boolean retainExistingState) {
		parseArgs(args, retainExistingState);
		execute();
	}
	
	@SuppressWarnings("unchecked")
	public <T extends FeatureManager> T getFeatureManager(){
		return (T) this.featureManager;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends NetworkCompiler> T getNetworkCompiler(){
		return (T) this.networkCompiler;
	}
	
	public NetworkModel getNetworkModel(){
		return this.networkModel;
	}
	
	/**
	 * Resets the start time to current time.
	 */
	public void resetTimer(){
		this.timer = System.nanoTime();
	}
	
	/**
	 * Returns the start time of this pipeline as recorded by the last call to {@link #resetTimer()}.
	 * @return
	 */
	public long getTimer(){
		return this.timer;
	}
	
	/**
	 * Returns the elapsed time since the start of this pipeline or since the last call to {@link #resetTimer()}.
	 * @return
	 */
	public long getElapsedTime(){
		return System.nanoTime()-this.timer;
	}
	
	/**
	 * Initialize the parameters, filling in the default values.
	 */
	public void initParameters(){
		parameters = argParser.parseKnownArgsOrFail(new String[]{TASK_NOOP}, null).getAttrs();
		taskList.remove(TASK_NOOP);
	}
	
	/**
	 * Whether the parameter with the specified key has been provided and is not null.
	 * @param key
	 * @return
	 */
	public boolean hasParameter(String key){
		if(this.parameters == null){
			initParameters();
		}
		return this.parameters.containsKey(key) && this.parameters.get(key)!=null;
	}
	
	/**
	 * Returns the argument associated with the specified key.<br>
	 * @param key
	 * @return
	 */
	public <T> T getParameter(String key){
		if(this.parameters == null){
			initParameters();
		}
		@SuppressWarnings("unchecked")
		T result = (T)this.parameters.get(key);
		return result;
	}
	
	/**
	 * Sets the parameter as specified by the key to the given value.
	 * @param key
	 * @param value
	 */
	public void setParameter(String key, Object value){
		if(this.parameters == null){
			initParameters();
		}
		this.parameters.put(key, value);
	}
	
	/**
	 * Removes the parameter associated with the specified key.
	 * @param key
	 * @return Whether the key previously existed.
	 */
	public boolean removeParameter(String key){
		if(this.parameters == null){
			initParameters();
		}
		if(!hasParameter(key)){
			return false;
		}
		this.parameters.remove(key);
		return true;
	}
	
}
