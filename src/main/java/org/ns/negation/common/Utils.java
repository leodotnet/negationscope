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
import java.util.HashMap;
import java.util.List;

import org.ns.commons.types.Label;
import org.ns.negation.common.NegationInstance;
import org.ns.negation.common.NegationInstance.FEATURE_TYPES;

public class Utils {
	
	public static UniPOSMapper UniPOSMapping = new UniPOSMapper();
	
	public static class UniPOSMapper {
		HashMap<String, String> pos2unipos = new HashMap<String, String>();
		
		public UniPOSMapper() {
			try {
				load();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public void load() throws IOException {
			pos2unipos.clear();
			InputStreamReader isr = new InputStreamReader(new FileInputStream(NegationDataConfig.UNIPOS_MAP_PATH), "UTF-8");
			BufferedReader br = new BufferedReader(isr);
			while(br.ready()) {
				String line = br.readLine().trim();
				String[] fields = line.split("\t");
				String unipos = fields[1].trim();
				
				for(String pos : fields[0].split("\\|")) {
					pos2unipos.put(pos.trim(), unipos);
				}
			}
			
			br.close();
			
			System.out.println("UniPOS Mapping is loaded with total #mapping = " + pos2unipos.size());
		}
		
		public String getUniPOS(String POS) {
			return pos2unipos.get(POS);
		}
	}
	
	public static class Counter {
		HashMap<String, Integer> wordCount = new HashMap<String, Integer>();
		
		public Counter() {
			wordCount.clear();
		}
		
		public int addWord(String word) {
			Integer count = wordCount.get(word);
			if (count == null) {		
				count = 1;
			} else {
				count = count + 1;
			}
			
			wordCount.put(word, count);
			
			return count;
		}
		
		public int getCount(String word) {
			Integer count = wordCount.get(word);
			return (count == null) ? 0 : count;
		}
		
		public int getVocabSize() {
			return wordCount.size();
		}
	}
	
	public static class IntStat {
		HashMap<Integer, Integer> stats = new HashMap<Integer, Integer>();
		
		public IntStat() {
			stats.clear();
		}
		
		public int addInt(int a) {
			Integer count = stats.get(a);
			if (count == null) {
				count = 1;
			} else {
				count = count + 1;
			}
			
			stats.put(a, count);
			return count;
		}
		
		public int getCount(int a) {
			Integer count = stats.get(a);
			return (count == null) ? 0 : count;
		}
		
		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			for(Integer i : stats.keySet()) {
				sb.append(i + "\t" + stats.get(i) + "<br>\n");
			}
			
			return sb.toString();
		}
	}
	
	
	
	public static int[] sorted(int[] arr)
	{
		int[] arr_sorted = arr.clone();
		Arrays.sort(arr_sorted);
		return arr_sorted;
	}
	
	public static NegationInstance[] portionInstances(NegationInstance[] instances, double percentage) {
		return portionInstances(instances, (int)(percentage * instances.length));
	}
	
	public static NegationInstance[] portionInstances(NegationInstance[] instances, int num) {
		//NegationInstance[] insts = new NegationInstance[num];
		if (num > instances.length)
			num = instances.length;
		System.out.println("Truncate " + num + " instances.");
		return Arrays.copyOf(instances, num);
	}
	
	public static NegationInstance[] mergeInstances(NegationInstance[] instances1, NegationInstance[] instances2) {
		NegationInstance[] instances = new NegationInstance[instances1.length + instances2.length];
		for(int i = 0; i < instances1.length; i++)
			instances[i] = instances1[i];
		
		
		
		for(int i = 0; i < instances2.length; i++) {
			instances[instances1.length + i] = instances2[i];
			instances[instances1.length + i].setInstanceId(instances1.length + 1 + i);
		}
		
		return instances;
	}
	
	public static boolean isPunctuation(char c) {
        return c == ','
            || c == '.'
            || c == '!'
            || c == '?'
            || c == ':'
            || c == ';'
            || c == '`'
            ;
    }
	
	public static boolean isPunctuation(String s) {
        return (s.length() == 1 && isPunctuation(s.charAt(0))) || (s.equals("-LRB-") || s.equals("-RRB-") || s.equals("``") || s.equals("''"));
    }
	
	public static void writeVocab(String filename, NegationInstance[][] instancesList, boolean lowercase) {
		PrintWriter p = null;
		try {
			p = new PrintWriter(new File(filename), "UTF-8");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (NegationInstance[] instances : instancesList)
			for (NegationInstance inst : instances) {
				ArrayList<String[]> input = (ArrayList<String[]>) inst.getInput();
				for (String[] token : input) {
					;
					String output = token[NegationInstance.FEATURE_TYPES.word.ordinal()];
					if (lowercase)
						output = output.toLowerCase();
					p.println(output);
				}
			}
		
		p.close();
	}
	
	public static boolean startOfEntity(int pos, int size, ArrayList<Label> outputs) {
		String label = outputs.get(pos).getForm();
		if (label.startsWith("B"))
			return true;

		if (pos == 0 && label.startsWith("I"))
			return true;

		if (pos > 0) {
			String prev_label = outputs.get(pos - 1).getForm();
			if (label.startsWith("I") && prev_label.startsWith("O"))
				return true;
		}

		return false;
	}

	public static boolean endofEntity(int pos, int size, ArrayList<Label> outputs) {
		String label = outputs.get(pos).getForm();
		if (!label.startsWith("O")) {
			if (pos == size - 1)
				return true;
			else {
				String next_label = outputs.get(pos + 1).getForm();
				if (next_label.startsWith("O") || next_label.startsWith("B"))
					return true;
			}
		}

		return false;
	}
	
	
	public void addPrefix(ArrayList<String[]> featureArr, String[] f, String prefix, String Sent, String NE) {

		featureArr.add(new String[] { prefix + f[0], f[1], f[2] });
	}
	
	public String escape(String s) {
		for (Character val : string_map.keySet()) {
			String target = val + "";
			if (s.indexOf(target) >= 0) {
				String repl = string_map.get(val);
				s = s.replace(target, "");

			}
		}

		return s;
	}

	public String norm_digits(String s) {
		s = s.replaceAll("\\d+", "0");

		return s;

	}

	public String clean(String s) {
		String str;
		if (s.startsWith("http://") || s.startsWith("https://")) {
			str = "<WEBLINK>";
		} else if (s.startsWith("@")) {
			str = "<USERNAME>";
		} else {
			str = norm_digits(s.toLowerCase());
			// String str1 = escape(str);
			// if (str1.length() >= 0)

			String str1 = str.replaceAll("[^A-Za-z0-9_]", "");
			if (str1.length() > 0)
				str = str1;
		}

		return str;
	}

	public static final HashMap<Character, String> string_map = new HashMap<Character, String>() {
		{
			put('.', "_P_");
			put(',', "_C_");
			put('\'', "_A_");
			put('%', "_PCT_");
			put('-', "_DASH_");
			put('$', "_DOL_");
			put('&', "_AMP_");
			put(':', "_COL_");
			put(';', "_SCOL_");
			put('\\', "_BSL_");
			put('/', "_SL_");
			put('`', "_QT_");
			put('?', "_Q_");
			put('¿', "_QQ_");
			put('=', "_EQ_");
			put('*', "_ST_");
			put('!', "_E_");
			put('¡', "_EE_");
			put('#', "_HSH_");
			put('@', "_AT_");
			put('(', "_LBR_");
			put(')', "_RBR_");
			put('\"', "_QT0_");
			put('Á', "_A_ACNT_");
			put('É', "_E_ACNT_");
			put('Í', "_I_ACNT_");
			put('Ó', "_O_ACNT_");
			put('Ú', "_U_ACNT_");
			put('Ü', "_U_ACNT0_");
			put('Ñ', "_N_ACNT_");
			put('á', "_a_ACNT_");
			put('é', "_e_ACNT_");
			put('í', "_i_ACNT_");
			put('ó', "_o_ACNT_");
			put('ú', "_u_ACNT_");
			put('ü', "_u_ACNT0_");
			put('ñ', "_n_ACNT_");
			put('º', "_deg_ACNT_");
		}
	};
	
	public static String getToken(ArrayList<String[]> inputs, int pos, int idx) {
		if (pos >= 0 && pos < inputs.size()) {
			return inputs.get(pos)[idx];
		} else if (pos == -1) {
			return "<START>";
		} else if (pos == inputs.size()){
			return "<END>";
		} else {
			return "<PAD>";
		}
	}

	public static boolean isAllCap(String word) {
		for (int i = 0; i < word.length(); i++)
			if (Character.isLowerCase(word.charAt(i)))
				return false;

		return true;
	}
	
	public static boolean isFirstCap(String word) {
		return !Character.isLowerCase(word.charAt(0));
	}
	
	public static double[] vectorAdd(double[] a, double[] b) {
		if (a.length != b.length)
			return null;
		
		double[] c = a.clone();
		for(int i = 0; i < c.length; i++)
			c[i] += b[i];
		return c;
	}
	
	public static int[] vectorAdd(int[] a, int[] b) {
		if (a.length != b.length)
			return null;
		
		int[] c = a.clone();
		for(int i = 0; i < c.length; i++)
			c[i] += b[i];
		return c;
	}
	
	public static double[] vectorScale(double[] a, double scale) {
		double[] c = a.clone();
		for(int i = 0; i < c.length; i++)
			c[i] *= scale;
		return c;
	}
	
	public static double center(int[] a) {
		int sum = 0;
		int count = 0;
		for(int i = 0; i < a.length; i++) {
			count += a[i];
			sum += a[i] * i;
		}
		
		return (sum + 0.0) / count;
	}
	
	public static boolean isNumeric(String str)
	{
	  return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
	}
	
	public static String getPhrase(List<String[]> inputs, int fromIdx, int toIdx, int featureIdx) {
		return getPhrase(inputs, fromIdx, toIdx, featureIdx, true);
	}
	
	public static String getPhrase(List<String[]> inputs, int fromIdx, int toIdx, int featureIdx, boolean isToLowercase) {
		List<String> list = new ArrayList<String>();
		for(int i = fromIdx; i <= toIdx && i < inputs.size(); i++) {
			if (isToLowercase)
				list.add(inputs.get(i)[featureIdx].toLowerCase());
			else
				list.add(inputs.get(i)[featureIdx]);
		}
		
		return join(" ", list);
	}
	
	public static List<String> getPhraseList(List<String[]> inputs, int fromIdx, int toIdx, int featureIdx, boolean isToLowercase) {
		List<String> list = new ArrayList<String>();
		for(int i = fromIdx; i <= toIdx && i < inputs.size(); i++) {
			if (isToLowercase)
				list.add(inputs.get(i)[featureIdx].toLowerCase());
			else
				list.add(inputs.get(i)[featureIdx]);
		}
		
		return list;
	}
	
	
	public static List<String> getPhraseList(List<String[]> inputs, int featureIdx, boolean isToLowercase) {
		List<String> list = new ArrayList<String>();
		for(int i = 0; i < inputs.size(); i++) {
			if (isToLowercase)
				list.add(inputs.get(i)[featureIdx].toLowerCase());
			else
				list.add(inputs.get(i)[featureIdx]);
		}
		
		return list;
	}
	
	
	

	public static String join(String Separator, List<String> list) {
		String ret = list.get(0).toString();
		
		for(int i = 1; i < list.size(); i++) {
			ret += Separator + list.get(i).toString();
		}
		
		return ret;
	}
	
	
	public static int getPosNextToken(List<String[]> inputs, String target, int fromIdx, int featureIdx) {
		
		for(int i = fromIdx; i < inputs.size() ; i++) {
			String token = inputs.get(i)[featureIdx];
			if (token.equals(target))
				return i;
		}
		
		return -1;
	}
	
	public static int[] getBoundary(int[] a) {
		int[] boundary = new int[] {0, a.length - 1};
		for(int i = 0; i < a.length; i++) {
			if (a[i] == 1) {
				boundary[0] = i;
				break;
			}
		}
		
		for(int i = a.length - 1; i >= 0; i--) {
			if (a[i] == 1) {
				boundary[1] = i;
				break;
			}
		}
		
		return boundary;
	}
	
	public static ArrayList<NegationInstance> convertOutputPred(NegationInstance inst) {
		return null;
	}
	
	
	public static int countVal(int[] a, int val) {
		int count = 0;
		for(int i = 0; i < a.length; i++)
			if (a[i] == val)
				count++;
		
		return count;
	}
	
	
	public static double cueF(int[] cueGold, int[] cuePred) {
		
		int size = cueGold.length;
		
		int matched = 0;
		for(int i = 0; i < size; i++) {
			if (cueGold[i] == 1 && cueGold[i] == cuePred[i]) {
				matched++;
			}
		}
		
		int spanGold = countVal(cueGold, 1);
		int spanPred = countVal(cuePred, 1);
		
		double p = (matched + 0.0) / spanPred;
		double r = (matched + 0.0) / spanGold;
		double f = (Math.abs(p + r) < 1e-5) ? 0 : 2 * p * r / (p + r);
		
		return f;
	}
	
	public static boolean spanArrayEqual(int[] cueGold, int[] cuePred) {
		if (cueGold.length != cuePred.length)
			return false;
		
		for(int i = 0; i < cueGold.length; i++)
			if (cueGold[i] != cuePred[i])
				return false;
		
		return true;
	}
	
	
	public static boolean spanArrayEqual(int[] cueGold, int from, int to) {
		return (cueGold[from] == 1 && cueGold[to - 1] == 1);
	}
	
	
	public static int getSpanEnd(int[] a, int val, int startIdx) { //exclusive
		for(int i = startIdx; i < a.length; i++) {
			if (a[i] != val)
				return i;
		}
		
		return a.length;
	}
	
	public static int[] getNumSpan(int[] a, int val) {
		int p = 0;
		int spanCount = 0;
		
		ArrayList<Integer> spanLengthCount = new ArrayList<Integer>();
		
		while(p < a.length) {
			if (a[p] == val) {
				int endIdx = getSpanEnd(a, val, p);
				spanLengthCount.add(endIdx - p);
				spanCount++;
				p = endIdx - 1;
				
				
			}
			
			p++;
		}
		
		int[] spanLengthArr = new int[spanLengthCount.size()];
		for(int i = 0; i < spanLengthArr.length; i++)
			spanLengthArr[i] = spanLengthCount.get(i);
		
		return spanLengthArr;
	}
	
	
	public static ArrayList<int[]> getAllSpans(int[] a, int val) { //fromIdx, endIdx, val
		int p = 0;
		
		ArrayList<int[]> spans = new ArrayList<int[]>();
		
		while(p < a.length) {
			if (a[p] == val) {
				int endIdx = getSpanEnd(a, val, p);
				spans.add(new int[] {p, endIdx, val});
				p = endIdx - 1;
				
			}
			
			p++;
		}
		
		return spans;
	}
	

	
	
}
