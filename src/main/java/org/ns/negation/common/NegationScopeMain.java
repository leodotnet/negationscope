package org.ns.negation.common;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

import org.ns.commons.ml.opt.OptimizerFactory;
import org.ns.commons.ml.opt.GradientDescentOptimizer.BestParamCriteria;
import org.ns.commons.types.Instance;
import org.ns.commons.types.Label;
import org.ns.hypergraph.DiscriminativeNetworkModel;
import org.ns.hypergraph.FeatureManager;
import org.ns.hypergraph.GlobalNetworkParam;
import org.ns.hypergraph.NetworkCompiler;
import org.ns.hypergraph.NetworkConfig;
import org.ns.hypergraph.NetworkModel;
import org.ns.hypergraph.StringIndex;
import org.ns.hypergraph.NetworkConfig.ModelType;
import org.ns.hypergraph.decoding.Metric;
import org.ns.hypergraph.neural.BiLSTMCharCNNWord;
import org.ns.hypergraph.neural.BiLSTMCharWord;
import org.ns.hypergraph.neural.BidirectionalLSTM;
import org.ns.hypergraph.neural.EmbeddingLayer;
import org.ns.hypergraph.neural.GlobalNeuralNetworkParam;
import org.ns.hypergraph.neural.MLP;
import org.ns.hypergraph.neural.NeuralNetworkCore;
import org.ns.negation.common.NegEval;
import org.ns.negation.common.NegMetric;
import org.ns.negation.common.NegationCompiler;
import org.ns.negation.common.NegationDataConfig;
import org.ns.negation.common.NegationFeatureManager;
import org.ns.negation.common.NegationFeatureValueProvider;
import org.ns.negation.common.NegationGlobal;
import org.ns.negation.common.NegationInstance;
import org.ns.negation.common.Utils;
import org.ns.negation.common.WordEmbedding;
import org.ns.negation.common.NegationFeatureManager.FeaType;



public class NegationScopeMain {
	
	
	public static int num_iter = 3000;
	public static int begin_index = 0;
	public static int end_index = 0;
	public static boolean mentionpenalty = false;
	public static int NEMaxLength = 7;
	public static int SpanMaxLength = 8;
	public static int numThreads = 20;
	public static double l2 = 0.1;
	public static String embedding = "";
	public static int gpuId = -1;
	public static String neuralType =  "none";
	public static String nnOptimizer = "lbfgs";
	public static boolean iobes = true;
	public static OptimizerFactory optimizer = OptimizerFactory.getLBFGSFactory();
	public static boolean DEBUG = false;
	public static String SEPERATOR = "\t";
	public static int evaluateEvery = 0;
	public static String UNK = "<UNK>";
	public static int embeddingSize = 100;
	public static int charHiddenSize = 25;
	public static int evalFreq = -1;
	public static int batchSize = 64;
	public static int test_batchSize = -1;
	public static int numLabel = 3;
	public static String embeddingModelpath = "models//";
	public static int numFilter = 50;
	//public static int windowSize = 3;
	public static int numSentimentLabel = 3;
	public static int cnnWindowSize = 3;
	public static double learningRate = 0.05;
	public static boolean EVALTEST = false;
	public static boolean TESTTRAIN = true;
	public static boolean TESTDEV = true;
	public static boolean USEPERL = true;

	
	public static boolean SKIP_TRAIN = false;
	public static boolean SKIP_TEST = false;
	public static String in_path = "data//Twitter_";
	public static String out_path = "experiments//negation//models//<modelname>//en";
	public static String feature_file_path = in_path + "//feature_files//";
	public static boolean visual = false;
	public static String lang = "en";
	public static String embedding_suffix = ""; 
	public static boolean word_feature_on = true;
	public static String subpath = "default";
	public static String modelname = "";
	public static NetworkModel model = null;
	public static NegationCompiler compiler = null;
	public static String dataSet = "negation";
	public static int TRIAL = -1;
	public static int FOLDIDX = -1;
	
	public static NegMetric summary_train = new NegMetric(true);
	public static NegMetric summary_dev = new NegMetric(true);
	public static NegMetric summary_test = new NegMetric(true);
	
	public static void main(String args[]) throws IOException, InterruptedException{
		
		
		processArgs(args);
		
		NegationGlobal.init();
		
		if (modelname.startsWith("JOINT")) {
			NegationGlobal.NEG_CUE_DETECTION = true;
			NegationGlobal.SORT_NEGATION_CUE = true;
			NegationGlobal.USE_SYSTEM_CUE = true;
		}
		
		NetworkConfig.AVOID_DUPLICATE_FEATURES = true;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = l2;
		NetworkConfig.NUM_THREADS = numThreads;
		NegationGlobal.modelname = modelname;
		out_path = out_path.replace("<modelname>", modelname);
		out_path = NegationDataConfig.pathJoin(out_path, subpath);
		
		
		System.out.println("#iter=" + num_iter + " L2=" + NetworkConfig.L2_REGULARIZATION_CONSTANT + " lang=" + lang + " modelname="+modelname );
		
		if (NetworkConfig.USE_NEURAL_FEATURES && !embedding.equals(""))
		{
			NegationGlobal.ENABLE_WORD_EMBEDDING = true;
			
			if (embedding_suffix.equals("fasttext"))
			{
				NegationGlobal.EMBEDDING_WORD_LOWERCASE = true;
				UNK = "</s>";
			} else if (embedding.startsWith("glove")) {
				NegationGlobal.EMBEDDING_WORD_LOWERCASE = true;
				UNK = "unk";
			} else if (embedding.equals("polyglot")) {
				NegationGlobal.EMBEDDING_WORD_LOWERCASE = true;
				UNK = "<UNK>";
			}
			
			if (NetworkConfig.OS.equals("linux")) {
				WordEmbedding.MODELPATH = "/home/lihao/corpus/embedding/";
			}
		}
		
		NegationGlobal.UNK = UNK;
		NegationGlobal.neuralType = neuralType;
		
		
		if (NegationGlobal.ENABLE_WORD_EMBEDDING )
		{
			if (neuralType.equals("continuous") || neuralType.equals("continuous0"))
			if (embedding.equals("polyglot") || embedding.startsWith("glove")) {
				NegationGlobal.initWordEmbedding(lang + "_" + embedding, embeddingSize);
				
			}
		}
		System.out.println("ENABLE_WORD_EMBEDDING=" + NegationGlobal.ENABLE_WORD_EMBEDDING);
		
		
		NegationGlobal.feature_file_path = feature_file_path;
		
		File directory = new File(out_path);
		if (!directory.exists())
        {
            directory.mkdirs();
        }
		
		NegationGlobal.setLang(lang);
		
		
		System.out.println("Configurations:\n" + NetworkConfig.getConfig());
		
		String[] fold = NegationDataConfig.getDataFold(dataSet, lang);
		
		
		if (fold != null) {
			runFolds(fold);
		} else {
			runNormalDataset();
		}
		
		/*
		if (dataSet.equals("bioscope_abstracts")) {
			begin_index = 0;
			end_index = 9;
			TESTTRAIN = false;
			TESTDEV = false;
			run10Folds();
		} else {
			runNormalDataset();
		}*/
		
		
	
		
		return;
	}
	
	public static void runModel(String[] dataFiles, String dataSet, String dataFilePath, String outputPath) throws IOException, InterruptedException {
		
		String train_file = dataFiles[0];
		String dev_file = dataFiles[1];
		String test_file = dataFiles[2];
		String model_file = NegationDataConfig.pathJoin(outputPath, modelname + "." + train_file +".model");
		String trainPath = null;
		
		if (dataSet.startsWith("simple_wiki")) {
			train_file = NegationDataConfig.getDataFiles(dataSet.replace("simple_wiki", "cdsco"), lang)[0];
			trainPath = NegationDataConfig.getDataPath(dataSet.replace("simple_wiki", "cdsco"), lang);
			String preTrainModelPath = NegationDataConfig.PRETRAIN_MODEL_PATH + "//" + (neuralType.equals("continuous") ? "continuous" : "discrete");
			model_file = NegationDataConfig.pathJoin(preTrainModelPath, modelname +  ".model");
			SKIP_TRAIN = true;
			USEPERL = false;
			
			if(dataSet.equals("simple_wiki_suffixal")) {
				SKIP_TRAIN = false;
			}
		} 
		
		if (dataSet.startsWith("bioscope") || dataSet.startsWith("cnesp")) {
			NegationGlobal.OUTPUT_SEM2012_FORMAT = true;
			TESTTRAIN = false;
			TESTDEV = true;
			//USEPERL = true;
		}
		
		NegationGlobal.resetNegExpList() ;
		
		String result_file = NegationDataConfig.pathJoin(outputPath, dataFiles[2] + ".out");
		String iter = num_iter + "";
		//String mentionpenalty = NegationDataConfig.pathJoin(outputPath, dataFiles[2] + ".mentionpenalty");;
		
		StringBuffer stats = new StringBuffer(modelname + "\t" + dataSet + "\n");
		
		
		
		System.out.println("Execute data ");
		System.out.println("Result file:" + result_file);
		System.out.println();
		
		NegationInstance<Label>[] trainInstances;
		NegationInstance<Label>[] testInstances;
		NegationInstance<Label>[] test2Instances;
		NegationInstance<Label>[] devInstances;
				
		
		trainInstances = NegationInstance.readData(dataSet, NegationDataConfig.pathJoin(trainPath == null ? dataFilePath : trainPath, train_file), true, true, TRIAL, true);

		testInstances = NegationInstance.readData(dataSet, NegationDataConfig.pathJoin(dataFilePath, test_file), true, false, TRIAL, NegationGlobal.DISCARD_NONEG_SENTENCE_IN_TEST);
		
		
		if (EVALTEST) {
			devInstances = NegationInstance.readData(dataSet, NegationDataConfig.pathJoin(dataFilePath, test_file), true, false, TRIAL, NegationGlobal.DISCARD_NONEG_SENTENCE_IN_TEST);
		} else {
			devInstances = NegationInstance.readData(dataSet, NegationDataConfig.pathJoin(dataFilePath, dev_file), true, false, TRIAL ,NegationGlobal.DISCARD_NONEG_SENTENCE_IN_TEST);
		}
		
		
		NegationGlobal.testVocabFile = NegationDataConfig.pathJoin(dataFilePath, test_file) + ".vocab";
		Utils.writeVocab(NegationGlobal.testVocabFile, new NegationInstance[][]{devInstances, testInstances}, true);
		NegationGlobal.printAffix();
		
		int num_iter_recount = num_iter;
		int eval_freq_recount = evalFreq;
		
		if(NetworkConfig.USE_BATCH_TRAINING)
		{
			NetworkConfig.BATCH_SIZE = batchSize;
			System.out.println("#epoch=" + num_iter);
			System.out.println("#batch=" + batchSize);
			num_iter_recount = (trainInstances.length / batchSize) * num_iter;
			eval_freq_recount = (trainInstances.length / batchSize  ) * evalFreq;
			System.out.println("#evalFreq [epoch]=" + eval_freq_recount);
			System.out.println("#iter in epochs=" + num_iter_recount);
		}
		else
		{
			System.out.println("#evalFreq [iter]=" + evalFreq);
		}
				
		List<NeuralNetworkCore> fvps = new ArrayList<NeuralNetworkCore>();
		if(NetworkConfig.USE_NEURAL_FEATURES){
			int numLabel = Integer.parseInt(modelname.substring(modelname.length() - 1));
			System.out.println("neuralType:" + neuralType + " with #Label = " + numLabel);
//			gnp =  new GlobalNetworkParam(OptimizerFactory.getGradientDescentFactory());
			/*if (neuralType.equals("lstm")) {
				
				String optimizer = nnOptimizer;
				boolean bidirection = true;
				fvps.add(new BidirectionalLSTM(embeddingSize, bidirection, optimizer, 0.05, 5, numLabel, gpuId, embedding).setModelFile(model_file + ".nn"));
				//fvps.add(new BidirectionalLSTM(hiddenSize, bidirection, optimizer, 0.05, 5, 3, gpuId, embedding));
			}else if (neuralType.equals("lstmchar")) { 
				String optimizer = nnOptimizer;
				boolean bidirection = true;
				fvps.add(new BiLSTMCharWord(embeddingSize, charHiddenSize ,bidirection, optimizer, 0.05, 5, numLabel, gpuId, embedding).setModelFile(model_file + ".nn"));
				System.out.println("Use Bi-LSTM Word Char");
			}else if (neuralType.equals("lstmcharcnn")) { 
				String optimizer = nnOptimizer;
				boolean bidirection = true;
				fvps.add(new BiLSTMCharCNNWord(embeddingSize, charHiddenSize, numFilter, cnnWindowSize, bidirection, optimizer, 0.05, 5, numLabel, gpuId, embedding).setModelFile(model_file + ".nn"));
				System.out.println("Use Bi-LSTM Word Char");
			}else if (neuralType.equals("mlp")) { 
				//fvps.add(new MLP(numSentimentLabel, embeddingSize, NegationGlobal.SENTIMENT_WINDOW_SIZE, numFilter, cnnWindowSize, embedding, NegationGlobal.USE_POSITION_EMBEDDEING).setModelFile(model_file + ".mlp.nn"));
				System.out.println("Use MLP CNN");
			}else if (neuralType.equals("continuous")) {
				fvps.add(new NegationFeatureValueProvider(NegationGlobal.Word2Vec, numLabel).setUNK(UNK).setModelFile(model_file + ".nn"));
			}else if (neuralType.equals("continuous*")) {
							
				fvps.add(new EmbeddingLayer(numLabel, embeddingSize,embedding, NegationGlobal.FIX_EMBEDDING, NegationGlobal.testVocabFile).setModelFile(model_file+ ".cont"));
			} else */
				
			if (neuralType.equals("continuous0")) {
				NetworkConfig.USE_FEATURE_VALUE = true;
			}  		
			else {
				throw new RuntimeException("Unknown neural type: " + neuralType);
			}
		} 
		GlobalNeuralNetworkParam nn = new GlobalNeuralNetworkParam(fvps);
		GlobalNetworkParam gnp = new GlobalNetworkParam(optimizer, nn);
		
		
		
		//Class<? extends VisualizationViewerEngine> visualizerClass = getViewer(modelname);
		
		
		model = createNetworkModel(modelname, gnp, neuralType);//NetworkModel model = DiscriminativeNetworkModel.create(fa, compiler);
		
		Function<Instance[], Metric> evalFunc = new Function<Instance[], Metric>() {
			@Override
			public Metric apply(Instance[] t) {
				Metric result = NegEval.eval(t);
				System.out.println("Eval on dev:\n" + result);
				return result;
			}
			
		};
		
		
		NegationGlobal.clearTemporalData();
		//model.visualize(visualizerClass, trainInstances);
		if (!SKIP_TRAIN)
		{
			
			model.train(trainInstances, num_iter_recount, devInstances, evalFunc, eval_freq_recount);
			
			if (!neuralType.equals("continuous*")) 
			{
				saveModel(model, gnp, model_file);
			}
			
			//if (visual) model.visualize(visualizerClass, devInstances);
		}
		
		
		
		
		if (!SKIP_TEST)
		{
			if (SKIP_TRAIN)
			{
				model = loadModel(model_file);
			}
			
			setMP(model.getFeatureManager());
			
			stats.append("Best Dev Result is at iter=" + NetworkConfig.BEST_ITER_DEV + "\n");
			Instance[] predictions;

			if (TESTTRAIN) {
				NegationGlobal.clearTemporalData();
				trainInstances = NegationInstance.readData(dataSet, NegationDataConfig.pathJoin(dataFilePath, train_file), true, false, TRIAL, NegationGlobal.DISCARD_NONEG_SENTENCE_IN_TEST);
				predictions = model.test(trainInstances);
				stats.append(printStat(predictions, "Train ", summary_train));
				NegationInstance.writeResult(predictions, NegationDataConfig.pathJoin(dataFilePath, train_file), result_file.replace(test_file, train_file));
				predictions = null;
			}
			
			if (TESTDEV) {
				NegationGlobal.clearTemporalData();
				predictions = model.test(devInstances);
				stats.append(printStat(predictions, "Dev ", summary_dev));
				NegationInstance.writeResult(predictions, NegationDataConfig.pathJoin(dataFilePath, dev_file), result_file.replace(test_file, dev_file));
				predictions = null;
			}
			
			NegationGlobal.clearTemporalData();
			predictions = model.test(testInstances);
			stats.append(printStat(predictions, "Test ", summary_test));
			NegationInstance.writeResult(predictions, NegationDataConfig.pathJoin(dataFilePath,test_file), result_file); 
			
			
			
			
			
			if (visual) {
				for(int k = 0; k <predictions.length; k++) {
					NegationInstance inst = (NegationInstance)predictions[k];
					inst.setPredictionAsOutput();
					inst.setLabeled();
				}
				//model.visualize(visualizerClass, predictions);
			}
			
			

		}
		
		nn.closeNNConnections();
	
		
		System.out.println(stats.toString());
		
		
		
		if (USEPERL) {
			if (dataSet.endsWith("end2end")) {
				test_file = test_file.replace(".txt.out", ".txt");
				dataFilePath = dataFilePath.replace("-end2end", "");
			}
			
			if (NegationGlobal.NEG_CUE_DETECTION) {
				Files.copy(new File(result_file).toPath(), new File(NegationDataConfig.pathJoin(dataFilePath + "-end2end", test_file)).toPath(), StandardCopyOption.REPLACE_EXISTING);
				if (TESTDEV)
					Files.copy(new File(result_file.replace(test_file, dev_file)).toPath(), new File(NegationDataConfig.pathJoin(dataFilePath + "-end2end", dev_file)).toPath(), StandardCopyOption.REPLACE_EXISTING);
			} 
			
			NegEval.evalbyScript(NegationDataConfig.pathJoin(dataFilePath, test_file), result_file);
			
			
		}
		
	}
	
	
	public static void setMP(FeatureManager fm) {
		
		if (NegationGlobal.CUE_MP != null) {
			double newWeight = NegationGlobal.CUE_MP;
			setMP( fm,  FeaType.MP + "Cue", newWeight);
		}
		
		if (NegationGlobal.SCOPE_MP != null) {
			double newWeight = NegationGlobal.SCOPE_MP;
			setMP( fm,  FeaType.MP + "Scope", newWeight);
		}
		
	}
	
	public static void setMP(FeatureManager fm, String featureName, double W) {
		int MentionPenaltyFeatureIndex = fm.getParam_G().getFeatureId(featureName, "", "");
		double MPWeight = fm.getParam_G().getWeight(MentionPenaltyFeatureIndex);
		fm.getParam_G().setWeight(MentionPenaltyFeatureIndex, W);
		System.out.println( featureName + " MPWeight: from " + MPWeight + " to " + W);
	}
	
	public static void runNormalDataset() throws IOException, InterruptedException {
		
		summary_train = new NegMetric(true);
		summary_dev = new NegMetric(true);
		summary_test = new NegMetric(true);
		
		String[] dataFiles = NegationDataConfig.getDataFiles(dataSet, lang).clone();
		String dataPath = NegationDataConfig.getDataPath(dataSet, lang);
		runModel(dataFiles, dataSet,dataPath , out_path);

	}
	
	public static void runFolds(String[] fold) throws InterruptedException, IOException {
		
		//begin_index = 0;
		//end_index = 9;
		TESTTRAIN = false;
		TESTDEV = false;
		
		summary_train = new NegMetric(true);
		summary_dev = new NegMetric(true);
		summary_test = new NegMetric(true);
		
		if (dataSet.startsWith("cnesp") && !dataSet.endsWith("end2end")) {
			SKIP_TRAIN = false;
			TESTDEV = true;
		}
		
		for(int i = 0; i < fold.length; i++)
		{	
			String[] dataFiles = NegationDataConfig.getDataFiles(dataSet, lang).clone();
			for(int j = 0; j < dataFiles.length; j++)
				dataFiles[j] = dataFiles[j].replace("[*]", fold[i]);
			FOLDIDX = i;
			runModel(dataFiles, dataSet, NegationDataConfig.getDataPath(dataSet, lang), out_path);
			
			if (dataSet.startsWith("cnesp") ) {
				SKIP_TRAIN = true;
				TESTDEV = false;
			}
		}
		FOLDIDX = -1;
		
		if (TESTTRAIN) {
			System.out.println("Summary Train");
			System.out.println(summary_train.compute());
		}
		
		if (TESTDEV) {
			System.out.println("Summary Dev");
			System.out.println(summary_dev.compute());
		}
		
		System.out.println("Summary Test");
		System.out.println(summary_test.compute());
		
	}
	
	public static String printStat(Instance[] predictions, String expname, NegMetric summary) {
		StringBuffer sb = new StringBuffer(expname + "\n");

		Metric metric = NegEval.eval(predictions);
		sb.append(metric.toString() + "\n");
		
		summary.aggregate((NegMetric)metric);

		return sb.toString();
	}
	
	
	
	
	
	
	
	public static void processArgs(String[] args){
		
		if (args.length == 0)
		{
			return;
		}
		
		if(args[0].equals("-h") || args[0].equals("help") || args[0].equals("-help") ){
			System.err.println("Sentiment Scope Version: Joint Entity Recognition and Sentiment Prediction TASK: ");
			//System.err.println("\t usage: java -jar dpe.jar -trainNum -1 -testNum -1 -thread 5 -iter 100 -pipe true");
			//System.err.println("\t put numTrainInsts/numTestInsts = -1 if you want to use all the training/testing instances");
			System.exit(0);
		}else{
			for(int i=0;i<args.length;i=i+2){
				switch(args[i]){
					case "-modelname": modelname = args[i+1]; break;   //default: all 
					case "-reg": l2 = Double.valueOf(args[i+1]);  break;
					case "-num_iter": num_iter = Integer.valueOf(args[i+1]); break;    //default:all
					case "-beginindex": begin_index = Integer.valueOf(args[i+1]); break;    //default:all
					case "-endindex": end_index = Integer.valueOf(args[i+1]); break;   //default:100;
					case "-lang": lang = args[i+1]; break;
					case "-mentionpenalty" : mentionpenalty = Boolean.getBoolean(args[i+1]); break;
					case "-subpath" : subpath = args[i+1]; break;
					case "-NEMaxLength": NEMaxLength = Integer.valueOf(args[i+1]); break;
					case "-thread": numThreads = Integer.valueOf(args[i+1]); break;   //default:5
					case "-emb" : embedding = args[i+1]; break;
					case "-gpuid": gpuId = Integer.valueOf(args[i+1]); break;
					case "-fixemb" : NegationGlobal.FIX_EMBEDDING = Boolean.parseBoolean(args[i+1]); break;
					
					
					case "-dumpfeature" : NegationGlobal.DUMP_FEATURE = Boolean.parseBoolean(args[i+1]); break;
					case "-visual" : visual = Boolean.parseBoolean(args[i+1]); break;
					//case "-visualID" : VisualizerFrame.INSTANCE_INIT_ID = Integer.valueOf(args[i+1]); break;
					case "-skiptest" : SKIP_TEST = Boolean.parseBoolean(args[i+1]); break;
					case "-skiptrain" : SKIP_TRAIN = Boolean.parseBoolean(args[i+1]); break;
					//case "-windows":ECRFEval.windows = true; break;            //default: false (is using windows system to run the evaluation script)
					//case "-batch": NetworkConfig.USE_BATCH_TRAINING = true;
					//				batchSize = Integer.valueOf(args[i+1]); break;
					//case "-model": NetworkConfig.MODEL_TYPE = args[i+1].equals("crf")? ModelType.CRF:ModelType.SSVM;   break;
					case "-neural": if(args[i+1].equals("mlp") || args[i+1].startsWith("lstm")|| args[i+1].startsWith("continuous")){ 
											NetworkConfig.USE_NEURAL_FEATURES = true;
											neuralType = args[i+1]; //by default optim_neural is false.
											NetworkConfig.IS_INDEXED_NEURAL_FEATURES = false; //only used when using the senna embedding.
											NetworkConfig.REGULARIZE_NEURAL_FEATURES = true;
									}
									break;
					case "-initNNweight": 
						NetworkConfig.INIT_FV_WEIGHTS = args[i+1].equals("true") ? true : false; //optimize the neural features or not
						break;
					case "-optimNeural": 
						NetworkConfig.OPTIMIZE_NEURAL = args[i+1].equals("true") ? true : false; //optimize the neural features or not
						if (!NetworkConfig.OPTIMIZE_NEURAL) {
							nnOptimizer = args[i+2];
							i++;
						}break;
					case "-lr":
						learningRate = Double.valueOf(args[i+1]);  break;
					case "-optimizer":
						 if(args[i+1].equals("sgd")) {
							 optimizer = OptimizerFactory.getGradientDescentFactoryUsingGradientClipping(BestParamCriteria.BEST_ON_DEV, learningRate, 5); 
							 NetworkConfig.USE_BATCH_TRAINING = true;
							 batchSize = 1;
							 NetworkConfig.BATCH_SIZE = 1;
							 NetworkConfig.RANDOM_BATCH = false;
						 } else if (args[i+1].equals("gd")) {
							 optimizer = OptimizerFactory.getGradientDescentFactory();
						 } else if (args[i+1].equals("adagrad")) {
							 optimizer = OptimizerFactory.getGradientDescentFactoryUsingAdaGrad();
						 }
						break;
					case "-randombatch" : NetworkConfig.RANDOM_BATCH = Boolean.parseBoolean(args[i+1]); break;
					
					
					//case "-lr": adagrad_learningRate = Double.valueOf(args[i+1]); break;
					case "-backend": NetworkConfig.NEURAL_BACKEND = args[i+1]; break;
					case "-os": NetworkConfig.OS = args[i+1]; break; // for Lua native lib, "osx" or "linux" 
					case "-embsuffix": embedding_suffix = args[i+1]; break;
					case "-unk" : UNK = args[i+1]; System.out.println("set UNK = " + UNK);break;
					case "-ngram" : NegationGlobal.NGRAM = Boolean.parseBoolean(args[i+1]); break;
					//case "-hidesink" : VisualizeGraph.hideEdgetoSink = Boolean.parseBoolean(args[i+1]); break;
					case "-outputscope": NegationGlobal.OUTPUT_SENTIMENT_SPAN = Boolean.parseBoolean(args[i+1]); break;
					case "-embsize": embeddingSize = Integer.valueOf(args[i+1]); break;    //default:all
					case "-charhiddensize": charHiddenSize = Integer.valueOf(args[i+1]); break;    //default:all
					case "-evalfreq": evalFreq = Integer.valueOf(args[i+1]); break;  
					case "-batch": batchSize = Integer.valueOf(args[i+1]); 
							if (batchSize > 0)
								NetworkConfig.USE_BATCH_TRAINING = true;
							break;
					case "-testbatch": test_batchSize = Integer.valueOf(args[i+1]); break;
					case "-label": numLabel = Integer.valueOf(args[i+1]); break;
					case "-numfilter": numFilter = Integer.valueOf(args[i+1]); break;
					case "-cnnwindowSize": cnnWindowSize = Integer.valueOf(args[i+1]); break;
					case "-dataset": dataSet = args[i+1]; NegationGlobal.dataSet = dataSet; break;
					case "-wordonly" : NegationGlobal.USE_WORD_ONLY = Boolean.parseBoolean(args[i+1]); break;
					case "-trial" : TRIAL = Integer.valueOf(args[i+1]); break;
					case "-demo" : boolean demo = Boolean.parseBoolean(args[i + 1]); 
								if (demo) {
									for(String dataset : NegationDataConfig.dataset2Files.keySet()) {
										NegationDataConfig.dataset2Files.put(dataset, new String[] { "trial.txt", "trial.txt","trial.txt" }); 
									}
									//NegationDataConfig.dataset2Files.put("cdsco_en", new String[] { "trial.txt", "trial.txt","trial.txt" }); 
									//NegationDataConfig.dataset2Files.put("bioscope_full_en", new String[] { "trial.txt", "trial.txt","trial.txt" }); 
								}
								break;
					
					case "-posemb" : NegationGlobal.USE_POSITION_EMBEDDEING = Boolean.parseBoolean(args[i+1]); break;
					case "-discrete" : NegationGlobal.ENABLE_DISCRETE_FEATURE = Boolean.parseBoolean(args[i+1]); break;
					case "-evaltest" : EVALTEST = Boolean.parseBoolean(args[i+1]); break;
					case "-debug" : NegationGlobal.DEBUG = Boolean.parseBoolean(args[i+1]); break;
					case "-discardintest" : NegationGlobal.DISCARD_NONEG_SENTENCE_IN_TEST= Boolean.parseBoolean(args[i+1]); break;
					case "-bac" : NegationGlobal.BA_CONSTRIANT= Boolean.parseBoolean(args[i+1]); break;
					case "-syntax" : NegationGlobal.SYNTAX_FEATURE= Boolean.parseBoolean(args[i+1]); break;
					case "-testrain" : TESTTRAIN = Boolean.parseBoolean(args[i+1]); break;
					case "-useperl" : USEPERL = Boolean.parseBoolean(args[i+1]); break;
					case "-outputsem2012" : NegationGlobal.OUTPUT_SEM2012_FORMAT = Boolean.parseBoolean(args[i+1]); break;
					case "-outputerror" : NegationGlobal.OUTPUT_ERROR = Boolean.parseBoolean(args[i+1]); break;
					case "-beston" : NegMetric.BESTON = NegMetric.EVALOPTION.valueOf(args[i+1]); break;
					case "-unipos" : NegationGlobal.USE_UNIVERSAL_POSTAG = Boolean.parseBoolean(args[i+1]); break;
					case "-nocueconstraintintrain" : NegationGlobal.NO_CUE_CONSTRAINT_IN_TRAIN = Boolean.parseBoolean(args[i+1]); break;
					case "-nocueconstraintintest" : NegationGlobal.NO_CUE_CONSTRAINT_IN_TEST = Boolean.parseBoolean(args[i+1]); break;
					case "-findcue" : NegationGlobal.NEG_CUE_DETECTION = Boolean.parseBoolean(args[i+1]); break;
					case "-systemcue" : NegationGlobal.USE_SYSTEM_CUE = Boolean.parseBoolean(args[i+1]); break;
					case "-cuemp": NegationGlobal.CUE_MP = Double.valueOf(args[i+1]);  break;
					case "-scopemp": NegationGlobal.SCOPE_MP = Double.valueOf(args[i+1]);  break;
					case "-ocuechain" : NegationGlobal.ENABLE_OCUE_CHAIN = Boolean.parseBoolean(args[i+1]); break;
					case "-uapproach" : NegationGlobal.SCOPE_CHAIN_U_APPROACH = Boolean.parseBoolean(args[i+1]); break;
					case "-modeltype" : if (args[i+1].equals("softmaxmargin")) {
											NetworkConfig.MODEL_TYPE = ModelType.SOFTMAX_MARGIN;
											System.out.println("Model Type:" + NetworkConfig.MODEL_TYPE);
										}
										break;
					case "-recallbeta": NegationGlobal.RECALL_BETA = Double.valueOf(args[i+1]);  break;
					case "-spanmax": NegationGlobal.SPAN_MAX = Integer.valueOf(args[i+1]); break;
					case "-latentmax": NegationGlobal.MAX_LATENT_NUMBER = Integer.valueOf(args[i+1]); break;
					case "-reverse" : boolean reverse = Boolean.parseBoolean(args[i + 1]); 
										if (reverse) NegationGlobal.REVERSE_INPUT = true;
										break;
					
					
					default: System.err.println("Invalid arguments "+args[i]+", please check usage."); System.exit(0);
				}
			}
			System.err.println("[Info] beginIndex: "+begin_index);
			System.err.println("[Info] endIndex: "+end_index);
			System.err.println("[Info] numIter: "+ num_iter);
			System.err.println("[Info] numThreads: "+numThreads);
			System.err.println("[Info] Regularization Parameter: "+ l2);
		}
	}
	
	public static NetworkModel createNetworkModel(String modelname, GlobalNetworkParam gnp, String neuralType) {
		String modelpath = "org.statnlp.negation";
		NegationFeatureManager fm = null;
		NegationCompiler compiler = null;


		if (modelname.equals("OI2")) {
			fm = new org.ns.negation.basic.NegationSpan2FeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationSpan2Compiler();
		} else if (modelname.equals("negationspan3")) {
			fm = new org.ns.negation.basic.NegationSpan2FeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationSpan3Compiler();
		} else if (modelname.equals("OIBAM4")) {
			fm = new org.ns.negation.basic.NegationSpanIBA4FeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationSpanIBA4Compiler();
		} else if (modelname.equals("BIBA6")) {
			fm = new org.ns.negation.basic.NegationSpan2FeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationSpanBIBA6Compiler();
		} else if (modelname.equals("OIN4")) {
			fm = new org.ns.negation.basic.NegationSpan2FeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationSpanOIN4Compiler();
		} else if (modelname.equals("OIBAMN6")) {
			fm = new org.ns.negation.basic.NegationSpan2FeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationSpanOIBAN6Compiler();
		} else if (modelname.equals("SEMIOI2")) {
			fm = new org.ns.negation.basic.NegationSpanSemiOI2FeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationSpanSemiOI2Compiler();
		} else if (modelname.equals("SEMIROI2")) {
			fm = new org.ns.negation.basic.NegationSpanSemiROI2FeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationSpanSemiROI2Compiler();
		} else if (modelname.equals("SEMIBOI2")) {
			fm = new org.ns.negation.basic.NegationSpanSemiBOI2FeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationSpanSemiBOI2Compiler();
		} else if (modelname.equals("SEMIOILatent2")) {
			fm = new org.ns.negation.basic.NegationSpanSemiOILatent2FeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationSpanSemiOILatent2Compiler();
		} else if (modelname.equals("SEMIROILatent2")) {
			fm = new org.ns.negation.basic.NegationSpanSemiROILatent2FeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationSpanSemiROILatent2Compiler();
		} else if (modelname.equals("SEMIOILatentO2")) {
			fm = new org.ns.negation.basic.NegationSpanSemiOILatent2FeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationSpanSemiOILatentO2Compiler();
		} else if (modelname.equals("SEMIROILatentI2")) {
			fm = new org.ns.negation.basic.NegationSpanSemiROILatent2FeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationSpanSemiROILatentI2Compiler();
		} else if (modelname.equals("SEMIROISLatent2")) {
			fm = new org.ns.negation.basic.NegationSpanSemiROILatent2FeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationSpanSemiROISLatent2Compiler();
		} else if (modelname.equals("OIBAMNLatent6")) {
			fm = new org.ns.negation.basic.NegationSpanOIBAN6LatentFeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationSpanOIBAN6LatentCompiler();
		}else if (modelname.equals("OIBAMNLatentI6")) {
			fm = new org.ns.negation.basic.NegationSpanOIBAN6LatentFeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationSpanOIBAN6LatentICompiler();
		} else if (modelname.equals("OIBAMNLatentO6")) {
			fm = new org.ns.negation.basic.NegationSpanOIBAN6LatentFeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationSpanOIBAN6LatentOCompiler();
		}  else if (modelname.equals("CUEOI2")) {
			fm = new org.ns.negation.basic.NegationCueOIFeatureManager2(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationCueCompiler2();
		} else if (modelname.equals("JOINTOI2")) {
			fm = new org.ns.negation.basic.NegationScopeJointFeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationScopeJointCompiler();
		} else if (modelname.equals("JOINTOIN4")) {
			fm = new org.ns.negation.basic.NegationScopeJointFeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationScopeJointNCompiler4();
		}  else if (modelname.equals("JOINTOIBAMN6")) {
			fm = new org.ns.negation.basic.NegationScopeJointFeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationScopeJointBAMNCompiler6();
		} else if (modelname.equals("FAROI4")) {
			fm = new org.ns.negation.basic.NegationScopeSemi4FeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationScopeSemi4Compiler();
		} else if (modelname.equals("SEMICOMPACTOI4")) { //O I1, I2, I3
			fm = new org.ns.negation.basic.NegationScopeSemiCompact4FeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationScopeSemiCompact4Compiler();
		} else if (modelname.equals("SEMICOMPACTOI2")) {// O I
			fm = new org.ns.negation.basic.NegationScopeSemiCompact2FeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationScopeSemiCompact2Compiler();
		} else if (modelname.equals("SEMICOMPACTOI5")) {// O I1, I2, I3, I4
			fm = new org.ns.negation.basic.NegationScopeSemiCompact5FeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationScopeSemiCompact5Compiler();
		} else if (modelname.equals("SEMICOMPACTOIN")) {// O I1, I2, I3, I4 ....
			fm = new org.ns.negation.basic.NegationScopeSemiCompactNFeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationScopeSemiCompactNCompiler();
		} else if (modelname.equals("SEMICOMPACTLatentOIN")) {// O I1-1, I1-2, I1-3, I2, I3, I4...
			fm = new org.ns.negation.basic.NegationScopeSemiCompactLatentNFeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationScopeSemiCompactLatentNCompiler();
		} else if (modelname.equals("SEMICOMPACTLatentOI4")) {// O I1-1, I1-2, I1-3, I2, I3...
			fm = new org.ns.negation.basic.NegationScopeSemiCompactLatent4FeatureManager(gnp, neuralType, NegationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.ns.negation.basic.NegationScopeSemiCompactLatent4Compiler();
		}
	
		
		NegationScopeMain.compiler = compiler;

		fm.setCompiler(compiler);
		NetworkModel model = DiscriminativeNetworkModel.create(fm, compiler);

		return model;
	}
	
	/*
	public static Class<? extends VisualizationViewerEngine> getViewer(String modelname)
	{
		String visualModelPath = "basic.NegationViewer";
		switch (modelname)
		{
		case "JOINTOI2":
			visualModelPath = "basic.NegationScopeJointViewer";
			break;
		case "JOINTOIN4":
			visualModelPath = "basic.NegationScopeJointViewer";
			break;
		case "JOINTOIBAMN6":
			visualModelPath = "basic.NegationScopeJointViewer";
			break;
		case "FAROI4":
			visualModelPath = "basic.NegationScopeSemi4Viewer";
			break;
		case "SEMICOMPACTOI4":
			visualModelPath = "basic.NegationScopeSemiCompact4Viewer";
			break;
		case "SEMICOMPACTOI5":
			visualModelPath = "basic.NegationScopeSemiCompact5Viewer";
			break;
		case "SEMICOMPACTOI2":
			visualModelPath = "basic.NegationScopeSemiCompact2Viewer";
			break;
		case "SEMICOMPACTOIN":
			visualModelPath = "basic.NegationScopeSemiCompactNViewer";
			break;
		case "SEMICOMPACTLatentOIN":
			visualModelPath = "basic.NegationScopeSemiCompactLatentNViewer";
			break;
		case "SEMICOMPACTLatentOI4":
			visualModelPath = "basic.NegationScopeSemiCompactLatent4Viewer";
			break;
		}
		
		if (visualModelPath == null)
			return null;
		
		String visualizerModelName = "org.statnlp.negation." + visualModelPath;
		Class<? extends VisualizationViewerEngine> visualizerClass = null;
		
		try {
			visualizerClass = (Class<VisualizationViewerEngine>) Class.forName(visualizerModelName);
		} catch (ClassNotFoundException e) {
			System.err.println("Class not found");
		}
		
		return visualizerClass;
	}*/
	
	public static void saveModel(NetworkModel model, GlobalNetworkParam param, String filename_model ) throws IOException {
		
		
		System.out.println();
		System.err.println("Saving Model:" +  filename_model + " ...");
		ObjectOutputStream out;
		out = new ObjectOutputStream(new FileOutputStream(filename_model));
		
		out.writeObject(model);
		out.flush();
		out.close();
		System.err.println("Model Saved.");
		
		if (NegationGlobal.DUMP_FEATURE)
			printFeature(param, filename_model);
		
	}
	
	public static void printFeature(GlobalNetworkParam param, String filename_model )
	{
		
		StringIndex string2Idx = param.getStringIndex();
		string2Idx.buildReverseIndex();
		
		PrintWriter modelTextWriter = null;
		try {
			modelTextWriter = new PrintWriter(filename_model + ".dump");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		modelTextWriter.println("Num features: "+param.countFeatures());

		modelTextWriter.println("Features:");

		TIntObjectHashMap<TIntObjectHashMap<TIntIntHashMap>> featureIntMap =  param.getFeatureIntMap();

		for(int featureTypeId: Utils.sorted(featureIntMap.keys())){ //sorted

		     //.println(featureType);

			 TIntObjectHashMap<TIntIntHashMap> outputInputMap = featureIntMap.get(featureTypeId);

		     for(int outputId: Utils.sorted(outputInputMap.keys())){ //sorted

		          //modelTextWriter.println("\t"+output);

		    	 TIntIntHashMap inputMap = outputInputMap.get(outputId);

		          for(int inputId: inputMap.keys()){

		               int featureId = inputMap.get(inputId);
		               
		               String featureType = string2Idx.get(featureTypeId);
		               String input = string2Idx.get(inputId);
		               String output = string2Idx.get(outputId);

		               modelTextWriter.println(featureType + input+ ":= " + output + "="+param.getWeight(featureId));
		               if (NegationGlobal.ECHO_FEATURE)
		            	   System.out.println(featureType + input+ ":= " + output + "="+param.getWeight(featureId));
		          
		          }

		     }
		     
		     modelTextWriter.flush();

		}

		modelTextWriter.close();
	}
	


	public static NetworkModel loadModel(String filename_model) throws IOException {
		
		
		NetworkModel model = null;
		System.err.println("Loading Model...");
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename_model));
		try {
			model = (NetworkModel)in.readObject();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		in.close();
			
		
		System.err.println("Model Loaded.");

		return model;		
	}
}
