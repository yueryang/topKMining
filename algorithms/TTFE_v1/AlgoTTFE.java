package ttfe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;


public class AlgoTTFE
{
	/** Main parameters **/
	public static final String algoName = "TTFE"; // name of the algorithm (four characters)
	public static final int defaultTopK = 10, defaultWidth = 100; // default values
	public static final double defaultAlpha = 0.5, defaultBeta = 0.5; // default values
	public static final boolean defaultRaise1 = true, defaultRaise2 = true, defaultPrint = false; // default values
	int topK = defaultTopK; // topK
	double alpha = defaultAlpha, beta = defaultBeta; // weighting parameters
	Double delta = null; // initial a key variable (null is used as -float("inf") in Python)
	boolean raise1 = defaultRaise1, raise2 = defaultRaise2, isPrint = defaultPrint; // switches
	
	private String inputFile = null, outputFile = null; // input and output
	private ArrayList<Transaction> transactions = new ArrayList<>(); // per line
	private LinkedHashMap<Integer, Double> TWTF = new LinkedHashMap<>(); // TWTF<event -> twtf>: for each event -> twtf = sum([ttf if event in transaction])
	private LinkedHashMap<Integer, Double> ETF = new LinkedHashMap<>(); // ETF<event -> etf>: for each event -> etf = sum(each TF of events)
	private Table LETF = null;
	private PriorityQueue<Double> letf_e = new PriorityQueue<Double>(), lb_letf = new PriorityQueue<Double>();
	private ArrayList<TreeNode> finalResults = new ArrayList<TreeNode>();
	
	double startTimestamp = 0, endTimestamp = 0;
	double maxMemory = -1; // the maximum memory cost
	int htfeCount = 0;
	Double minTTFEValue = null; // null is used as -float("inf") in Python
	
	
	/** Constructed functions **/
	public AlgoTTFE()
	{
		this.topK = defaultTopK;
		this.alpha = defaultAlpha;
		this.beta = defaultBeta;
		this.raise1 = defaultRaise1;
		this.raise2 = defaultRaise2;
		this.isPrint = defaultPrint;
	}
	public AlgoTTFE(int topK)
	{
		if (checkTopK(topK))
			this.topK = topK;
		else
		{
			printTopKStatement(topK, defaultTopK);
			this.topK = defaultTopK;
		}
		this.alpha = defaultAlpha;
		this.beta = defaultBeta;
		this.raise1 = defaultRaise1;
		this.raise2 = defaultRaise2;
		this.isPrint = defaultPrint;
	}
	public AlgoTTFE(int topK, double alpha, double beta)
	{
		if (checkTopK(topK))
			this.topK = topK;
		else
		{
			printTopKStatement(topK, defaultTopK);
			this.topK = defaultTopK;
		}
		if (checkAlphaAndBeta(alpha, beta))
		{
			this.alpha = alpha;
			this.beta = beta;
		}
		else
		{
			printAlphaAndBetaStatement(alpha, beta, defaultAlpha, defaultBeta);
			this.alpha = defaultAlpha;
			this.beta = defaultBeta;
		}
		this.raise1 = defaultRaise1;
		this.raise2 = defaultRaise2;
		this.isPrint = defaultPrint;
	}
	public AlgoTTFE(int topK, double alpha, double beta, boolean raise1, boolean raise2)
	{
		if (checkTopK(topK))
			this.topK = topK;
		else
		{
			printTopKStatement(topK, defaultTopK);
			this.topK = defaultTopK;
		}
		if (checkAlphaAndBeta(alpha, beta))
		{
			this.alpha = alpha;
			this.beta = beta;
		}
		else
		{
			printAlphaAndBetaStatement(alpha, beta, defaultAlpha, defaultBeta);
			this.alpha = defaultAlpha;
			this.beta = defaultBeta;
		}
		this.raise1 = raise1;
		this.raise2 = raise2;
		this.isPrint = defaultPrint;
	}
	public AlgoTTFE(int topK, double alpha, double beta, boolean raise1, boolean raise2, boolean isPrint)
	{
		if (checkTopK(topK))
			this.topK = topK;
		else
		{
			printTopKStatement(topK, defaultTopK);
			this.topK = defaultTopK;
		}
		if (checkAlphaAndBeta(alpha, beta))
		{
			this.alpha = alpha;
			this.beta = beta;
		}
		else
		{
			printAlphaAndBetaStatement(alpha, beta, defaultAlpha, defaultBeta);
			this.alpha = defaultAlpha;
			this.beta = defaultBeta;
		}
		this.raise1 = raise1;
		this.raise2 = raise2;
		this.isPrint = isPrint;
	}
	
	
	/** Child classes **/
	public class TF extends AlgoTTFE
	{
		double threat = 0, frequency = 0, tf = 0;
		public TF(double threat, double frequency)
		{
			this.threat = threat;
			this.frequency = frequency;
			this.tf = this.alpha * this.threat + this.beta * this.frequency;
		}
		public double update(int topK, double alpha, double beta)
		{
			this.topK = topK;
			this.alpha = alpha;
			this.beta = beta;
			this.tf = this.alpha * this.threat + this.beta * this.frequency;
			return this.tf;
		}
		public String getString(boolean isThreat, boolean isFrequency, boolean isTf)
		{
			if (isThreat && isFrequency && isTf)
				return "[" + this.threat + ", " + this.frequency + ", " + this.tf + "]";
			else
			{
				String tmpStr = "{";
				if (isThreat)
					tmpStr += "\"threat\":" + this.threat + ", ";
				if (isFrequency)
					tmpStr += "\"frequency\":" + this.frequency + ", ";
				if (isTf)
					tmpStr += "\"tf\":" + this.tf + ", ";
				if (tmpStr.endsWith(", "))
					tmpStr = tmpStr.substring(0, tmpStr.length() - 2); // remove extra ", "
				tmpStr += "}";
				return tmpStr;
			}
		}
		public String toString()
		{
			return this.getString(true, true, true);
		}
	}
	
	public class TreeNode implements Comparable<TreeNode>
	{
		ArrayList<Integer> series = new ArrayList<>(); // sequences
		double aetf = 0, eetf = 0;
		public TreeNode(ArrayList<Integer> series, double aetf, double eetf)
		{
			this.series = series;
			this.aetf = aetf;
			this.eetf = eetf;
		}
		public boolean equals(TreeNode treeNode, boolean isAtf, boolean isEtf)
		{
			if (null == treeNode || !(treeNode instanceof TreeNode))
				return false;
			else if (treeNode == this)
				return true;
			else if (treeNode.series.size() != this.series.size())
				return false;
			else
			{
				for (int i = 0; i < this.series.size(); ++i)
					if (treeNode.series.get(i) != this.series.get(i))
						return false;
				if (isAtf && treeNode.aetf != this.aetf)
					return false;
				if (isEtf && treeNode.eetf != this.eetf)
					return false;
				return true;
			}
		}
		public boolean equals(TreeNode treeNode) { return this.equals(treeNode, false, false); }
		public String getString(boolean isAtf, boolean isEtf)
		{
			String sRet = "(" + this.series.get(0);
			for (int i = 1; i < this.series.size(); ++i)
				sRet += ", " + this.series.get(i);
			sRet += ")";
			if (isAtf && isEtf)
				sRet += ":[" + this.aetf + ", " + this.eetf + "]";
			else if (isAtf)
				sRet += ":{\"atf\":" + this.aetf + "}";
			else if (isEtf)
				sRet += ":" + this.eetf;
			return sRet;
		}
		@Override
		public int compareTo(TreeNode treeNode)
		{
			if (this.eetf < treeNode.eetf)
				return 1;
			else if (this.eetf > treeNode.eetf)
				return -1;
			else
				return 0;
		}
		public String toString()
		{
			return this.getString(true, true);
		}
	}
	
	public class Transaction extends AlgoTTFE // a line of the database
	{
		int tid = 0;
		LinkedHashMap<Integer, TF> events = new LinkedHashMap<>();
		Double ttf = null;
		ArrayList<TreeNode> treeNodes = new ArrayList<>(); // There are $(n - 1)^2$ tree nodes per transaction
		ArrayList<Double> resultEtf = new ArrayList<Double>();
		public Transaction(int tid)
		{
			this.tid = tid;
		}
		public boolean contains(Integer item)
		{
			return this.events.containsKey(item);
		}
		public boolean put(Integer item, TF tf)
		{
			if (this.contains(item))
				return false;
			events.put(item, tf);
			return true;
		}
		public boolean remove(Integer item)
		{
			if (!this.contains(item))
				return false;
			events.remove(item);
			return true;
		}
		public double update(int topK, double alpha, double beta)
		{
			this.topK = topK;
			this.alpha = alpha;
			this.beta = beta;
			this.ttf = (double)0; // initial
			Set<Entry<Integer, TF>> set = this.events.entrySet();
			Iterator<Entry<Integer, TF>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, TF> entry = iterator.next();
				entry.getValue().update(topK, alpha, beta);
				this.ttf += entry.getValue().tf;
			}
			return this.ttf;
		}
		public double update() { return update(this.topK, this.alpha, this.beta); }
		public int index(int item)
		{
			int tmp = -1;
			Set<Entry<Integer, TF>> set = this.events.entrySet();
			Iterator<Entry<Integer, TF>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				++tmp;
				Entry<Integer, TF> entry = iterator.next();
				if (entry.getKey() == item)
					return tmp;
			}
			return -1;
		}
		public boolean isSeries(ArrayList<Integer> lists)
		{
			int tmp = this.index(lists.get(0));
			if (-1 == tmp) // not found
				return false;
			for (int i = 1; i < lists.size(); ++i)
				if (++tmp != this.index(lists.get(i)))
					return false;
			return true;
		}
		public void addNode(ArrayList<Integer> series, double atf, double etf)
		{
			ArrayList<Integer> newSeries = new ArrayList<>();
			for (int i = 0; i < series.size(); ++i)
				newSeries.add(series.get(i));
			TreeNode tmpNode = new TreeNode(newSeries, atf, etf);
			this.treeNodes.add(tmpNode);
			return;
		}
		
		public double getAetfPerTransaction(ArrayList<Integer> series)
		{
			double tranAtf = 0;
			for (int i = 0; i < series.size(); ++i)
				if (this.events.containsKey(series.get(i)))
					tranAtf += this.events.get(series.get(i)).tf;
				else
					return 0;
			return tranAtf;
		}
		public ArrayList<Integer> getUnmatchedEventPerTransaction(ArrayList<Integer> series)
		{
			ArrayList<Integer> missingNodes = new ArrayList<>();
			if (this.getAetfPerTransaction(series) <= 0) // the current sequence is invalid
				return missingNodes;
			Set<Entry<Integer, TF>> set = this.events.entrySet();
			Iterator<Entry<Integer, TF>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, TF> entry = iterator.next();
				if ((Integer)entry.getKey().intValue() == series.get(0).intValue()) // find the first matched sequence
				{
					for (int cnt = 1; cnt < series.size(); ++cnt) // go through the whole sequence to find out the non-continuous events
					{
						entry = iterator.next();
						if ((Integer)entry.getKey().intValue() != series.get(cnt).intValue())
							missingNodes.add(series.get(cnt));
					}
					break;
				}
			}
			return missingNodes;
		}
		public void sortTree()
		{
			Collections.sort(this.treeNodes);
			return;
		}
		public void printTree()
		{
			System.out.print(this.tid + " - {");
			if (!this.treeNodes.isEmpty())
			{
				System.out.print(this.treeNodes.get(0));
				for (int i = 1; i < this.treeNodes.size(); ++i)
					System.out.print(", " + this.treeNodes.get(i));
			}
			System.out.println("}");
			return;
		}
		public String getResult()
		{
			String sRet = this.tid + " - {";
			if (!this.treeNodes.isEmpty())
			{
				sRet += this.treeNodes.get(0).getString(false, true);
				for (int i = 1; i < this.treeNodes.size() && i < this.topK; ++i)
					sRet += ", " + this.treeNodes.get(i).getString(false, true);
			}
			sRet += "}";
			return sRet;
		}
		public String getString(boolean isThreat, boolean isFrequency, boolean isTf)
		{
			if (null == this.ttf)
				this.update();
			String sRet = this.tid + " - {";
			Set<Entry<Integer, TF>> set = this.events.entrySet();
			Iterator<Entry<Integer, TF>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, TF> entry = iterator.next();
				sRet += (Integer)entry.getKey() + ":" + ((TF)(entry.getValue())).getString(isThreat, isFrequency, isTf) + (iterator.hasNext() ? ", " : "} - " + this.ttf);
			}
			return sRet;
		}
		public String toString()
		{
			return this.getString(true, true, true);
		}
	}
	
	public class Table extends AlgoTTFE
	{
		String name = null;
		int[] index = null; // from index to columns to form a sequence
		int[] columns = null;
		int[] series = null;
		double[][] values;
		public Table(double[][] values, int[] index, int[] columns, String name)
		{
			this.values = values;
			this.index = index;
			this.columns = columns;
			this.series = new int[index.length + 1];
			this.series[0] = index[0];
			for (int i = 0; i < columns.length; ++i)
				this.series[i + 1] = columns[i];
			this.name = name;
		}
		public Table(double[][] values, int[] series, String name)
		{
			this.values = values;
			this.series = series;
			this.columns = new int[series.length - 1];
			this.index = new int[series.length - 1];
			for (int cnt = 0; cnt < series.length; ++cnt)
			{
				if (0 == cnt)
					index[cnt] = series[cnt];
				else if (series.length - 1 == cnt)
					columns[cnt - 1] = series[cnt];
				else
				{
					index[cnt] = series[cnt];
					columns[cnt - 1] = series[cnt];
				}
			}
			this.name = name;
		}
		private int getColumnsIndex(int columns)
		{
			for (int i = 0; i < this.columns.length; ++i)
				if (this.columns[i] == columns)
					return i;
			return -1;
		}
		private int getIndexIndex(int index)
		{
			for (int i = 0; i < this.index.length; ++i)
				if (this.index[i] == index)
					return i;
			return -1;
		}
		public boolean addValueByID(int index_id, int columns_id, double value)
		{
			if (index_id < 0 || columns_id < 0 || index_id >= this.index.length || columns_id >= this.columns.length)
				return false;
			this.values[index_id][columns_id] += value;
			return true;
		}
		public boolean addValueByName(int index_name, int columns_name, double value)
		{
			int columnsIndex = this.getColumnsIndex(columns_name), indexIndex = this.getIndexIndex(index_name);
			if (-1 == columnsIndex || -1 == indexIndex)
				return false;
			this.values[indexIndex][columnsIndex] += value;
			return true;
		}
		private ArrayList<ArrayList<Integer>> combine_series(int m, int n, int controller[])
		{
			ArrayList<ArrayList<Integer>> combines = new ArrayList<>();
			if (1 == m) // end recursion
			{
				for (int i = m; i <= n; ++i)
				{
					ArrayList<Integer> sub_combine = new ArrayList<>();
					controller[0] = i;
					for (int j = 0; j < controller.length; ++j)
						sub_combine.add(this.series[controller[j] - 1]);
					combines.add(sub_combine);
				}
				return combines;
			}
			for (int i = m; i <= n; ++i)
			{
				controller[m - 1] = i;
				combines.addAll(this.combine_series(m - 1, i - 1, controller));
			}
			return combines;
		}
		public ArrayList<ArrayList<Integer>> combineSeries(int n)
		{
			ArrayList<ArrayList<Integer>> combines = new ArrayList<>();
			for (int i = 2; i <= n; ++i)
			{
				int[] controller = new int[i]; // used to control the output
				combines.addAll(this.combine_series(i, n, controller));
			}
			return combines;
		}
		public ArrayList<Integer> getMiddleElements(int p, int q, boolean ht)
		{
			ArrayList<Integer> array = new ArrayList<>();
			boolean isAdd = false;
			for (Integer element : this.series)
			{
				if (element.intValue() == p) // sequence begins
					isAdd = true;
				else if (element.intValue() == q) // sequence ends
					break;
				else if (isAdd)
					array.add(element);
			}
			if (ht)
			{
				array.add(0, p);
				array.add(q);
			}
			return array;
		}
		public String toString()
		{
			String sRet = "Series: " + this.series[0];
			for (int i = 1; i < this.series.length; ++i)
				sRet += " -> " + this.series[i];
			sRet += "\n" + this.name;
			for (int c : this.columns)
				sRet += "\t" + c;
			sRet += "\n";
			for (int i = 0; i < this.index.length; ++i)
			{
				sRet += this.index[i];
				for (int j = 0; j < this.values[i].length; ++j)
					sRet += "\t" + this.values[i][j];
				sRet += "\n";
			}
			return sRet;
		}
	}
	
	
	/** Child functions **/
	public static boolean checkTopK(int topK)
	{
		return topK > 0;
	}
	public static void printTopKStatement(int originalTopK, int revisedTopK)
	{
		System.out.printf("The passed parameter topK should be a positive integer. It is defaulted to %d. \nParameter: topK = %d -> topK = %d\n\n", revisedTopK, originalTopK, revisedTopK);
		return;
	}
	public static void printTopKStatement(String originalTopK, int revisedTopK)
	{
		System.out.printf("The parameter topK inputted from the file should be a positive integer. It is revised to %d. \nFile: thisLine = %s -> topK = %d\n\n", revisedTopK, originalTopK, revisedTopK);
		return;
	}
	
	public static boolean checkAlphaAndBeta(double alpha, double beta)
	{
		return 0 <= alpha && alpha <= 1 && 0 <= beta && beta <= 1 && alpha + beta == 1;
	}
	public static void printAlphaAndBetaStatement(double originalAlpha, double originalBeta, double revisedAlpha, double revisedBeta)
	{
		System.out.printf("The passed parameters alpha and beta should be in the interval [0, 1] and meet the requirement that $\\alpha + \\beta = 1$. They are defaulted to %s and %s respectively. \nParameter: (alpha, beta) = (%s, %s) -> (alpha, beta) = (%s, %s)\n\n", "" + revisedAlpha, "" + revisedBeta, "" + originalAlpha, "" + originalBeta, "" + revisedAlpha, "" + revisedBeta);
		return;
	}
	public static void printAlphaAndBetaStatement(String originalAlpha, String originalBeta, double revisedAlpha, double revisedBeta)
	{
		System.out.printf("The parameters alpha and beta inputted from the file should be in the interval [0, 1] and meet the requirement that $\\alpha + \\beta = 1$. They are defaulted to %s and %s respectively. \nFile/setAlphaLine: %s -> alpha = %s\nFile/setBetaLine: %s -> beta = %s\n\n", "" + revisedAlpha, "" + revisedBeta, originalAlpha, "" + revisedAlpha, originalBeta, "" + revisedBeta);
		return;
	}
	
	public static boolean printTitle(String titleName, int width)
	{
		if (titleName.length() + 6 <= width)
		{
			System.out.print("/*");
			for (int i = 0; i < (width - titleName.length() - 6) >> 1; ++i)
				System.out.print("*");
			System.out.print(" " + titleName + " ");
			for (int i = 0; i < (width - titleName.length() - 6) >> 1; ++i)
				System.out.print("*");
			System.out.println("*/");
			return true;
		}
		else
		{
			System.out.println("/* " + titleName + " */");
			return false;
		}
	}
	public static boolean printTitle(String titleName) { return printTitle(titleName, defaultWidth); }
	
	
	/** Main TTFE implementation **/
	/* Initial TTFE */
	private boolean initTTFE()
	{
		BufferedReader myInput = null;
		String thisLine = null;
		int topK = this.topK;
		double alpha = this.alpha, beta = this.beta;
		String setTopKLine = null, setAlphaLine = null, setBetaLine = null;
		int tid = 0;
		
		try
		{
			myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(this.inputFile))));
			while ((thisLine = myInput.readLine()) != null) // read per line
			{
				if (
					thisLine.isEmpty() || thisLine.charAt(0) == '#'
					|| thisLine.charAt(0) == '%' || thisLine.charAt(0) == '@'
					|| thisLine.length() > 1 && thisLine.charAt(0) == '/' && thisLine.charAt(1) == '/'
				) // skip empty and commented lines
					continue;
				String backupThisLine = thisLine; // store configure lines
				
				/* Item : Threat : Frequency : TTF */
				thisLine = thisLine.replace("\t", " "); // remove '\t'
				while (thisLine.contains("  ")) // remove "  " (' ' * 2)
					thisLine = thisLine.replace("  ", " ");
				thisLine = thisLine.replace(" : ", ":"); // remove spaces next to ':'
				while (thisLine.startsWith(" ")) // remove the spaces at the beginning of lines
					thisLine = thisLine.substring(1);
				while (thisLine.endsWith(" ")) // remove the spaces at the end of lines
					thisLine = thisLine.substring(0, thisLine.length() - 1);
				
				if (thisLine.contains("=")) // configures
				{
					thisLine = thisLine.replace(" ", "").replace("\t", "").toLowerCase();
					if (thisLine.startsWith("topk=")) // read the topK value from the file
					{
						try
						{
							topK = Integer.parseInt(thisLine.substring(5));
						}
						catch (Exception e)
						{
							System.out.printf("The following line has been skipped due to the failure of parsing the topK value (int). \nFile: %s\n\n", backupThisLine);
						}
						setTopKLine = backupThisLine;
					}
					else if (thisLine.startsWith("alpha=")) // read the alpha value from the file
					{
						try
						{
							alpha = Double.parseDouble(thisLine.substring(6));
						}
						catch (Exception e)
						{
							System.out.printf("The following line has been skipped due to the failure of parsing the alpha value (double). \nFile: %s\n\n", backupThisLine);
						}
						setAlphaLine = backupThisLine;
					}
					else if (thisLine.startsWith("beta=")) // read the beta value from the file
					{
						try
						{
							beta = Double.parseDouble(thisLine.substring(5));
						}
						catch (Exception e)
						{
							System.out.printf("The following line has been skipped due to the failure of parsing the beta value (double). \nFile: %s\n\n", backupThisLine);
						}
						setBetaLine = backupThisLine;
					}
					else if (thisLine.startsWith("raise1=")) // read the raise1 value from the file
						try
						{
							this.raise1 = Boolean.parseBoolean(thisLine.substring(7));
						}
						catch (Exception e)
						{
							System.out.printf("The following line has been skipped due to the failure of parsing the raise1 value (boolean). \nFile: %s\n\n", backupThisLine);
						}
					else if (thisLine.startsWith("raise2=")) // read the raise2 value from the file
						try
						{
							this.raise2 = Boolean.parseBoolean(thisLine.substring(7));
						}
						catch (Exception e)
						{
							System.out.printf("The following line has been skipped due to the failure of parsing the raise2 value (boolean). \nFile: %s\n\n", backupThisLine);
						}
					else if (thisLine.startsWith("isprint=")) // read the isPrint value from the file
						try
						{
							this.isPrint = Boolean.parseBoolean(thisLine.substring(8));
						}
						catch (Exception e)
						{
							System.out.printf("The following line has been skipped due to the failure of parsing the isPrint value (boolean). \nFile: %s\n\n", backupThisLine);
						}
				}
				else
				{
					Transaction transaction = new Transaction(++tid);
					int errorCount = 0;
					
					/* Handle ':' */
					String[] split = thisLine.split(":");
					String[] item_str = split[0].split(" ");
					String[] threat_str = split[1].split(" ");
					String[] frequency_str = split[2].split(" ");
					try // try to fetch the TTF value
					{
						transaction.ttf = new Double(Double.parseDouble(split[3]));
					}
					catch (Exception e)
					{
						transaction.ttf = null; // keep null if it is failed
					}
					
					/* Handle ' ' */
					for (int i = 0; i < item_str.length; ++i) // build transactions
						try
						{
							transaction.put(new Integer(Integer.parseInt(item_str[i])), new TF(Double.parseDouble(threat_str[i]), Double.parseDouble(frequency_str[i])));
						}
						catch (Exception e)
						{
							errorCount += 1;
						}
					if (errorCount > 1)
						System.out.printf("There are %d unrecognized items in the following line. They have been skipped. \nFile: %s\n\n", errorCount, thisLine);
					else if (errorCount > 0)
						System.out.printf("There is an unrecognized item in the following line. It has been skipped. \nFile: %s\n\n", thisLine);
					
					this.transactions.add(transaction);
				}
			}
		}
		catch (Throwable e)
		{
			System.out.printf("Error parsing the database fom the file \"%s\". Details are as follows. \n", this.inputFile);
			e.printStackTrace();
			return false;
		}
		finally
		{
			if (myInput != null)
				try
				{
					myInput.close();
				}
				catch (Throwable e) {}
		}
		
		if (checkTopK(topK))
			this.topK = topK;
		else
			printTopKStatement(setTopKLine, this.topK);
		if (null == setAlphaLine ^ null == setBetaLine) // automatically fill in alpha and beta if one of them is not specified
			if (null == setAlphaLine)
				alpha = 1 - beta;
			else
				beta = 1 - alpha;
		if (checkAlphaAndBeta(alpha, beta))
		{
			this.alpha = alpha;
			this.beta = beta;
		}
		else
			printAlphaAndBetaStatement(setAlphaLine, setBetaLine, this.alpha, this.beta);
		
		for (Transaction transaction:this.transactions)
		{
			Double ttf = transaction.ttf;
			if (ttf != transaction.update(this.topK, this.alpha, this.beta)) // check TTF values
				System.out.printf("The value of the input TTF of Transaction %d is not the same as it is computed. It is revised to %s. \nTransaction %s: ttf = %s -> %s\n\n", transaction.tid, transaction.ttf.toString(), transaction.toString(), ttf.toString(), transaction.ttf.toString());
		}
		
		if (this.isPrint)
		{
			printTitle(algoName + " - initTTFE()");
			System.out.println("topK: " + this.topK + "\t\talpha: " + this.alpha + "\t\tbeta: " + this.beta);
			for (Transaction transaction : transactions)
				System.out.println(transaction);
			System.out.println();
		}
		return true;
	}
	
	/* Compute TWTF */
	private void computeTWTF()
	{
		for (Transaction transaction : this.transactions)
		{
			LinkedHashMap<Integer, TF> items = transaction.events;
			Set<Entry<Integer, TF>> set = items.entrySet();
			Iterator<Entry<Integer, TF>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, TF> entry = iterator.next();
				Integer key = (Integer)(entry.getKey());
				this.TWTF.put(key, (this.TWTF.containsKey(key) ? this.TWTF.get(key) : 0) + transaction.ttf);
			}
		}
		
		if (this.isPrint)
		{
			printTitle(algoName + " - computeTWTF()");
			System.out.printf("TWTF: {");
			Set<Entry<Integer, Double>> set = this.TWTF.entrySet();
			Iterator<Entry<Integer, Double>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, Double> entry = iterator.next();
				System.out.print((Integer)entry.getKey() + ":" + (Double)entry.getValue() + (iterator.hasNext() ? ", " : ""));
			}
			System.out.println("}\n");
		}
		return;
	}
	
	/* Sort TWTF */
	private void sortTWTF() // Selection sort (up)
	{
		ArrayList<Entry<Integer, Double>> entryList = new ArrayList<Entry<Integer, Double>>(this.TWTF.entrySet());
		Collections.sort(entryList, new Comparator<Entry<Integer, Double>>() {
			@Override
			public int compare(Entry<Integer, Double> entry1, Entry<Integer, Double> entry2) {
				return entry1.getValue().compareTo(entry2.getValue());
			}
		});
		this.TWTF.clear();
		for (Entry<Integer, Double> entry : entryList)
			this.TWTF.put(entry.getKey(), entry.getValue());
		
		if (this.isPrint)
		{
			printTitle(algoName + " - sortTWTF()");
			System.out.printf("TWTF (UP): {");
			Set<Entry<Integer, Double>> set = this.TWTF.entrySet();
			Iterator<Entry<Integer, Double>> iterator = set.iterator();
			ArrayList<Integer> entrySeries = new ArrayList<Integer>();
			while (iterator.hasNext())
			{
				Entry<Integer, Double> entry = iterator.next();
				entrySeries.add((Integer)entry.getKey());
				System.out.printf((Integer)entry.getKey() + ":" + (Double)entry.getValue() + (iterator.hasNext() ? ", " : ""));
			}
			System.out.println("}");
			if (!entrySeries.isEmpty())
			{
				System.out.printf("Series: %d", entrySeries.get(0));
				for (int i = 1; i < entrySeries.size(); ++i)
					System.out.printf(" -> %d", entrySeries.get(i));
				System.out.println();
			}
			System.out.println();
		}
		return;
	}
	
	/* Compute ETF */
	private void computeETF()
	{
		for (Transaction transaction : this.transactions)
		{
			LinkedHashMap<Integer, TF> items = transaction.events;
			Set<Entry<Integer, TF>> set = items.entrySet();
			Iterator<Entry<Integer, TF>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, TF> entry = iterator.next();
				Integer key = (Integer)(entry.getKey());
				TF value = (TF)(entry.getValue());
				this.ETF.put(key, (this.ETF.containsKey(key) ? this.ETF.get(key) : 0) + value.tf);
			}
		}
		
		if (this.isPrint)
		{
			printTitle(algoName + " - computeETF()");
			System.out.printf("ETF: {");
			Set<Entry<Integer, Double>> set = this.ETF.entrySet();
			Iterator<Entry<Integer, Double>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, Double> entry = iterator.next();
				System.out.printf((Integer)entry.getKey() + ":" + (Double)entry.getValue() + (iterator.hasNext() ? ", " : ""));
			}
			System.out.println("}\n");
		}
		return;
	}
	
	/* Sort ETF */
	private void sortETF() // Selection sort (down)
	{
		ArrayList<Entry<Integer, Double>> entryList = new ArrayList<Entry<Integer, Double>>(this.ETF.entrySet());
		Collections.sort(entryList, new Comparator<Entry<Integer, Double>>() {
			@Override
			public int compare(Entry<Integer, Double> entry1, Entry<Integer, Double> entry2) {
				return entry2.getValue().compareTo(entry1.getValue());
			}
		});
		this.ETF.clear();
		for (Entry<Integer, Double> entry : entryList)
			this.ETF.put(entry.getKey(), entry.getValue());
		this.delta = new Double(entryList.get((entryList.size() <= this.topK ? entryList.size() : this.topK) - 1).getValue());
		
		if (this.isPrint)
		{
			printTitle(algoName + " - sortETF()");
			System.out.printf("ETF (DOWN): {");
			Set<Entry<Integer, Double>> set = this.ETF.entrySet();
			Iterator<Entry<Integer, Double>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, Double> entry = iterator.next();
				System.out.printf((Integer)entry.getKey() + ":" + (Double)entry.getValue() + (iterator.hasNext() ? ", " : "}\n"));
			}
			System.out.println("delta = " + delta + "\n");
		}
		return;
	}
	
	/* topK pruning */
	private void pruneItem()
	{
		LinkedHashMap<Integer, Double> OTWTF = new LinkedHashMap<>();
		Set<Entry<Integer, Double>> set = this.TWTF.entrySet();
		Iterator<Entry<Integer, Double>> iterator = set.iterator();
		while (iterator.hasNext())
		{
			Entry<Integer, Double> entry = iterator.next();
			if (null == this.delta || (double)entry.getValue() >= this.delta)
				OTWTF.put(entry.getKey(), entry.getValue());
			else
				for (int i = 0; i < this.transactions.size(); ++i)
					this.transactions.get(i).remove(entry.getKey());
		}
		this.TWTF = OTWTF;
		
		if (this.isPrint)
		{
			printTitle(algoName + " - pruneItem()");
			System.out.println("topK: " + this.topK + "\t\talpha: " + this.alpha + "\t\tbeta: " + this.beta + "\t\tdelta: " + this.delta);
			System.out.println("OriginalSize: " + this.ETF.size() + "\t\tCurrentSize: " + this.TWTF.size() + "\t\tCut: " + (this.ETF.size() - this.TWTF.size()));
			set = this.TWTF.entrySet();
			iterator = set.iterator();
			System.out.print("Pruned TWTF: {");
			while (iterator.hasNext())
			{
				Entry<Integer, Double> entry = iterator.next();
				System.out.printf((Integer)entry.getKey() + ":" + (Double)entry.getValue() + (iterator.hasNext() ? ", " : ""));
			}
			System.out.println("}");
			if (!this.transactions.isEmpty())
			{
				System.out.println("Pruned TTFE: ");
				for (Transaction transaction : this.transactions)
					System.out.println(transaction);
			}
			System.out.println();
		}
		return;
	}
	
	/* Sort TTFE */
	private void sortTTFE()
	{
		for (int i = 0; i < this.transactions.size(); ++i)
		{
			Transaction transaction = this.transactions.get(i), newTransaction = new Transaction(transaction.tid);
			Set<Entry<Integer, Double>> set = this.TWTF.entrySet();
			Iterator<Entry<Integer, Double>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, Double> entry = iterator.next();
				if (transaction.contains((Integer)entry.getKey()))
					newTransaction.put(entry.getKey(), transaction.events.get(entry.getKey()));
			}
			this.transactions.set(i, newTransaction);
		}
		
		if (this.isPrint)
		{
			printTitle(algoName + " - sortTTFE()");
			System.out.println("topK: " + this.topK + "\t\talpha: " + this.alpha + "\t\tbeta: " + this.beta + "\t\tdelta: " + this.delta);
			for (Transaction transaction : transactions)
				System.out.println(transaction);
			System.out.println();
		}
		return;
	}
	
	/* Generate Table */
	private void generateTable()
	{
		if (this.TWTF.isEmpty())
			return;
		int columns[] = new int[this.TWTF.size() - 1], index[] = new int[this.TWTF.size() - 1];
		double[][] values = new double[this.TWTF.size() - 1][this.TWTF.size() - 1];
		Set<Entry<Integer, Double>> setTwtf = this.TWTF.entrySet();
		Iterator<Entry<Integer, Double>> iteratorTwtf = setTwtf.iterator();
		for (int cnt = 0; iteratorTwtf.hasNext(); ++cnt)
		{
			Entry<Integer, Double> entry = iteratorTwtf.next();
			if (0 == cnt)
				index[cnt] = (Integer)entry.getKey();
			else if (this.TWTF.size() - 1 == cnt)
				columns[cnt - 1] = (Integer)entry.getKey();
			else
			{
				index[cnt] = (Integer)entry.getKey();
				columns[cnt - 1] = (Integer)entry.getKey();
			}
		}
		this.LETF = new Table(values, index, columns, "LETF");
		
		for (Transaction transaction : this.transactions) // walk through every transaction
		{
			Set<Entry<Integer, TF>> setEvents = transaction.events.entrySet();
			Iterator<Entry<Integer, TF>> iteratorEvent = setEvents.iterator();
			ArrayList<Integer> itemSeries = new ArrayList<>(); // series in a line
			while (iteratorEvent.hasNext()) // per item
			{
				Entry<Integer, TF> entryItem = iteratorEvent.next();
				itemSeries.add((Integer)entryItem.getKey());
			}
			for (int i = 0; i < this.LETF.series.length - 1; ++i)
			{
				int p = this.LETF.series[i]; // the head of the sequence
				if (!transaction.events.containsKey(p)) // skip events not found
					continue;
				ArrayList<Integer> subSeries = new ArrayList<>(); // subsequence
				subSeries.add(p);
				double pqValue = transaction.events.get(p).tf;
				for (int j = i + 1; j < this.LETF.series.length; ++j)
				{
					int q = this.LETF.series[j]; // the tail of the sequence
					if (!transaction.events.containsKey(q)) // break if it is not found
						break;
					subSeries.add(q);
					pqValue += transaction.events.get(q).tf; // the sum of the sequence
					if (transaction.isSeries(subSeries))
						this.LETF.addValueByName(p, q, pqValue);
					else // the inner loop is not continuous
						break; // break the inner loop
				}
			}
		}
		
		if (this.isPrint)
		{
			printTitle(algoName + " - generateTable()");
			System.out.println(this.LETF);
		}
		return;
	}
	
	/* Threshold raising strategy 1 based on LETF_E */
	public void raiseThreshold_LETF_E()
	{
		if (this.LETF != null && this.LETF.values.length > 0)
		{
			this.letf_e.offer(this.LETF.values[this.LETF.values.length - 1][this.LETF.values.length - 1]);
			for (int j = this.LETF.values.length - 2; j > -1; --j)
			{
				for (int i = j; i > -1 && this.letf_e.size() < this.topK; --i)
					if (this.LETF.values[i][j + 1] > this.LETF.values[j][j])
						this.letf_e.offer(this.LETF.values[i][j + 1]);
					else
					{
						this.letf_e.offer(this.LETF.values[j][j]);
						break;
					}
				if (this.letf_e.size() >= this.topK) // there are already k elements in the queue
					break;
			}
			if (null == this.delta || this.delta < this.letf_e.peek())
				this.delta = this.letf_e.peek();
		}
		
		if (this.isPrint)
		{
			printTitle(algoName + " - raiseThreshold_LETF_E()");
			System.out.println("topK = " + this.topK);
			System.out.println("letf_e = " + this.letf_e);
			System.out.println("delta = " + this.delta + "\n");
		}
		return;
	}
	
	/* Threshold raising strategy based on LB_LETF */
	public void raiseThreshold_LB_LETF()
	{
		if (this.LETF != null && this.LETF.values.length > 0)
			for (int i = 0; i < this.LETF.values.length; ++i)
				for (int j = 0; j < this.LETF.values[i].length; ++j)
				{
					int p = this.LETF.index[i], q = this.LETF.columns[j];
					ArrayList<Integer> array = this.LETF.getMiddleElements(p, q, false); // sub elements between i and j 
					double tmpValue = this.LETF.values[i][j];
					for (int m = 0; m < 3 && m < array.size(); ++m)
					{
						tmpValue -= this.ETF.get(array.get(m));
						if (null == this.delta || tmpValue > this.delta) // above delta
						{
							this.lb_letf.offer(tmpValue);
							while (this.lb_letf.size() > this.topK) // keep only k elements in the queue
								this.lb_letf.poll();
						}
						else
							break;
					}
				}
		if (!this.lb_letf.isEmpty())
			this.delta = this.lb_letf.peek();
		
		if (this.isPrint)
		{
			printTitle(algoName + " - raiseThreshold_LB_LETF()");
			System.out.println("topK = " + this.topK);
			System.out.println("lb_letf = " + this.lb_letf);
			System.out.println("delta = " + this.delta + "\n");
		}
		return;
	}
		
	/* Generate the tree */
	public void generateTree()
	{
		if (this.LETF != null)
			for (ArrayList<Integer> series : this.LETF.combineSeries(this.LETF.series.length))
				for (Transaction trans : this.transactions)
				{
					double aetf = 0, retf = 0;
					for (Transaction action : this.transactions) // sum up
						aetf += action.getAetfPerTransaction(series);
					ArrayList<Integer> nodeEtf = trans.getUnmatchedEventPerTransaction(series);
					for (int i = 0; i < nodeEtf.size(); ++i)
						retf += this.ETF.get(nodeEtf.get(i));
					trans.addNode(series, aetf, aetf - retf);
				}
		
		if (this.isPrint)
		{
			printTitle(algoName + " - generateTree()");
			if (this.transactions.isEmpty())
				System.out.println("No trees were generated. ");
			else
				for (Transaction transaction : this.transactions)
					transaction.printTree();
			System.out.println();
		}
		return;
	}
	
	/* Tree pruning */
	public void pruneTree()
	{
		for (Transaction transaction : this.transactions)
			for (int i = transaction.treeNodes.size() - 1; i > -1; --i)
				if (transaction.treeNodes.get(i).aetf + transaction.treeNodes.get(i).eetf < this.delta)
					transaction.treeNodes.remove(i);
		
		if (this.isPrint)
		{
			printTitle(algoName + " - pruneTree()");
			System.out.println("delta = " + this.delta);
			for (Transaction transaction : this.transactions)
				transaction.printTree();
			System.out.println();
		}
		return;
	}
	
	/* Tree sorting */
	public void sortTree()
	{
		for (Transaction transaction : this.transactions)
			transaction.sortTree();
		
		if (this.isPrint)
		{
			printTitle(algoName + " - sortTree()");
			System.out.println("delta = " + this.delta);
			for (Transaction transaction : this.transactions)
				transaction.printTree();
			System.out.println();
		}
		return;
	}
	
	/* Get results */
	public void getResults() throws IOException
	{
		for (Transaction transaction : this.transactions)
			for (TreeNode treeNode : transaction.treeNodes)
			{
				boolean isAdd = true;
				for (TreeNode tmpTN : this.finalResults)
					if (tmpTN.equals(treeNode))
					{
						isAdd = false;
						break;
					}
				if (isAdd)
					this.finalResults.add(treeNode);
			}
		for (int i = 1; i < this.finalResults.size(); ++i)
		{
			TreeNode current = this.finalResults.get(i);
			int j = i - 1;
			while (j >= 0 && this.finalResults.get(j).eetf < current.eetf)
			{
				this.finalResults.set(j + 1, this.finalResults.get(j));
				--j;
			}
			this.finalResults.set(j + 1, current);
		}
		for (int i = this.finalResults.size() - 1; i >= this.topK; --i)
			this.finalResults.remove(i);
		if (!this.finalResults.isEmpty())
			this.minTTFEValue = new Double(this.finalResults.get(this.finalResults.size() - 1).eetf);
		for (int i = 0; i < this.finalResults.size() && i < this.topK; ++i)
			this.htfeCount += this.finalResults.get(i).series.size();
		
		if (this.isPrint)
			printTitle(algoName + " - getResults()");
		File file = new File(this.outputFile.replace("{topK}", "" + this.topK).replace("{alpha}", "" + this.alpha).replace("{beta}", "" + this.beta).replace("{delta}", "" + this.delta).replace("{raise1}", "" + this.raise1).replace("{raise2}", "" + this.raise2).replace("{isPrint}", "" + this.isPrint));
		FileWriter fw = new FileWriter(file);
		fw.write("topK = " + this.topK + "\t\talpha = " + this.alpha + "\t\tbeta = " + this.beta + "\t\tdelta = " + this.delta + "\n");
		fw.write("raise1 = " + this.raise1 + "\t\traise2 = " + this.raise2 + "\t\tisPrint = " + this.isPrint + "\n");
		fw.write("Time: " + (endTimestamp - startTimestamp) / 1000.0 + " s\t\tSpace: " + new java.text.DecimalFormat("#.00").format(this.maxMemory) + " MB");
		for (Transaction transaction : this.transactions)
		{
			String tmpResult = transaction.getResult();
			if (this.isPrint)
				System.out.println(tmpResult);
			fw.write("\n" + tmpResult);
		}
		fw.write("\nFinal results: {");
		if (this.isPrint)
			System.out.printf("Final results: {");
		if (!this.finalResults.isEmpty())
		{
			fw.write(this.finalResults.get(0).getString(false, true));
			if (this.isPrint)
				System.out.printf(this.finalResults.get(0).getString(false, true));
			for (int i = 1; i < this.finalResults.size(); ++i)
			{
				fw.write(", " + this.finalResults.get(i).getString(false, true));
				if (this.isPrint)
					System.out.printf(", " + this.finalResults.get(i).getString(false, true));
			}
		}
		fw.write("}\nDump time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()));
		fw.close();
		if (this.isPrint)
			System.out.println("}\n");
		
		return;
	}
	
	/* Running entries */
	public boolean runAlgorithm(String inputFile, String outputFile) throws IOException
	{
		this.inputFile = inputFile;
		this.outputFile = outputFile;
		
		if (!(this.initTTFE() && this.checkMemory()))
			return false;
		startTimestamp = System.currentTimeMillis();
		this.computeTWTF(); this.checkMemory();
		this.sortTWTF(); this.checkMemory();
		this.computeETF(); this.checkMemory();
		this.sortETF(); this.checkMemory();
		this.pruneItem(); this.checkMemory();
		this.sortTTFE(); this.checkMemory();
		this.generateTable(); this.checkMemory();
		if (this.raise1) { this.raiseThreshold_LETF_E(); this.checkMemory(); }
		if (this.raise2) { this.raiseThreshold_LB_LETF(); this.checkMemory(); }
		this.generateTree(); this.checkMemory();
		this.pruneTree(); this.checkMemory();
		this.sortTree(); this.checkMemory();
		endTimestamp = System.currentTimeMillis();
		this.getResults();
		return true;
	}
	
	
	/** Performance functions **/
	private boolean checkMemory()
	{
		double currentMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024d / 1024d;
		if (currentMemory > maxMemory)
		{
			maxMemory = currentMemory;
			return true;
		}
		else
			return false;
	}
	
	public void printStats()
	{
		System.out.println("==========  " + algoName + " Algorithm - Results  ==========");
		System.out.println("\ttopK: " + this.topK);
		System.out.println("\talpha: " + this.alpha);
		System.out.println("\tbeta: " + this.beta);
		System.out.println("\tdelta: " + this.delta);
		System.out.println("\traise1: " + this.raise1);
		System.out.println("\traise2: " + this.raise2);
		System.out.println("\tisPrint: " + this.isPrint);
		System.out.println("\tTotal time: " + (endTimestamp - startTimestamp) / 1000.0 + " s");
		System.out.println("\tMax memory: " + new java.text.DecimalFormat("#.00").format(this.maxMemory) + " MB");
		System.out.println("\tHTFEs count: " + this.htfeCount);
		System.out.println("\tMinimum threat-frequency: " + this.minTTFEValue);
		System.out.println("\tDataset: " + this.inputFile.substring(this.inputFile.replace('\\', '/').contains("/") ? this.inputFile.replace('\\', '/').lastIndexOf('/') + 1 : 0, this.inputFile.contains(".") ? this.inputFile.lastIndexOf('.') : this.inputFile.length()));
		System.out.println("\tPrint time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()));
		System.out.println("================================================");
		return;
	}
}