package org.ns.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.message.StringFormatterMessageFactory;

/**
 * Class storing general utility methods.
 */
public class GeneralUtils {
	
	public static final PrintStream SYSOUT = System.out;
	public static final PrintStream SYSERR = System.err;
	
	private static List<org.apache.logging.log4j.core.Logger> loggers;

	public static List<String> sorted(Set<String> coll){
		List<String> result = new ArrayList<String>(coll);
		Collections.sort(result);
		return result;
	}
	
	/**
	 * Search for a constructor that can be called given the given parameters.
	 * @param input
	 * @param parameters
	 * @return
	 * @throws NoSuchMethodException
	 */
	@SuppressWarnings("unchecked")
	public static <T> Constructor<T> getMatchingAvailableConstructor(Class<T> input, Class<?>... parameters) throws NoSuchMethodException{
		for(Constructor<T> constructor: (Constructor<T>[])input.getDeclaredConstructors()){
			Class<?>[] paramTypes = constructor.getParameterTypes();
			if(paramTypes.length != parameters.length){
				continue;
			}
			boolean matching = true;
			for(int i=0; i<parameters.length; i++){
				Class<?> paramType = paramTypes[i];
				if(!paramType.isAssignableFrom(parameters[i])){
					matching = false;
					break;
				}
			}
			if(matching){
				return constructor;
			}
		}
		throw new NoSuchMethodException();
	}

	public static Logger createLogger(Class<?> clazz){
		Logger logger = LogManager.getLogger(clazz, new StringFormatterMessageFactory());
		if(loggers == null){
			loggers = new ArrayList<org.apache.logging.log4j.core.Logger>();
		}
		loggers.add((org.apache.logging.log4j.core.Logger)logger);
		return logger;
	}
	
	/**
	 * Update all loggers (including redirecting the System.out) to also print to the specified file,
	 * effectively recording the console output in a file.
	 * @param logPath
	 * @throws FileNotFoundException
	 */
	public static void updateLogger(String logPath) throws FileNotFoundException {
		PrintStream logPrinter = new PrintStream(new File(logPath));
		TeePrinter outPrinter = new TeePrinter(logPrinter, SYSOUT);
		TeePrinter errPrinter = new TeePrinter(logPrinter, SYSERR);
		System.setOut(outPrinter);
		System.setErr(errPrinter);
		
		// Reset all loggers
		LogManager.shutdown();
		Configuration config = ((LoggerContext)LogManager.getContext()).getConfiguration();
		for(org.apache.logging.log4j.core.Logger logger: loggers){
			logger.getContext().updateLoggers(config);
		}
		((org.apache.logging.log4j.core.LoggerContext)LogManager.getContext()).updateLoggers(config);
		((org.apache.logging.log4j.core.Logger)LogManager.getRootLogger()).getContext().updateLoggers(config);
	}
	
	/**
	 * Prints a text to both the specified file and other output streams.
	 */
	public static class TeePrinter extends PrintStream{
		
		private PrintStream[] streams;
		
		/**
		 * Creates a {@link TeePrinter} that prints to the specified file and to the specified streams.<br>
		 * The default charset will be used.
		 * @param file
		 * @param streams
		 * @throws FileNotFoundException
		 */
		public TeePrinter(PrintStream stream, PrintStream... streams) throws FileNotFoundException {
			super(stream);
			this.streams = streams;
		}
		
		public void flush(){
			super.flush();
			for(PrintStream stream: streams){
				stream.flush();
			}
		}
		
		public void close(){
			super.close();
			for(PrintStream stream: streams){
				stream.close();
			}
		}
		
		public void write(int b){
			super.write(b);
			for(PrintStream stream: streams){
				stream.write(b);
			}
		}
		
		public void write(byte[] buf, int off, int len){
			super.write(buf, off, len);
			for(PrintStream stream: streams){
				stream.write(buf, off, len);
			}
		}
		
	}
	
	public static String toPrettyString(Map<?,?> map){
		StringBuilder builder = new StringBuilder();
		builder.append("{\n");
		for(Entry<?,?> entry: map.entrySet()){
			builder.append("\t");
			builder.append(entry.getKey());
			builder.append("=");
			builder.append(entry.getValue());
			builder.append("\n");
		}
		builder.append("}");
		return builder.toString();
	}

}
