package ttfe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;


public class AlgoTTFE
{
	/** Main parameters **/
	public static final String algoName = "TTFE"; // name of the algorithm (four characters)
	public static final String defaultDatabase = "ttfe"; // default value
	public static final int defaultTopK = 5, defaultWidth = 100; // default values
	public static final double defaultAlpha = 0.5, defaultBeta = 0.5; // default values
	public static final boolean defaultRaise1 = true, defaultRaise2 = true, defaultPrint = false; // default values
	String database = defaultDatabase; // database
	int topK = defaultTopK; // topK
	double alpha = defaultAlpha, beta = defaultBeta; // weighting parameters
	Double delta = null; // initial a key variable (null is used as -float("inf") in Python)
	boolean raise1 = defaultRaise1, raise2 = defaultRaise2, isPrint = defaultPrint; // switches
	
	private String inputFile = null, outputFile = null; // input and output
	private ArrayList<Transaction> transactions = new ArrayList<Transaction>(); // per line
	private LinkedHashMap<Integer, Double> TWTF = new LinkedHashMap<>(); // TWTF<event -> twtf>: for each event -> twtf = sum([ttf if event in transaction])
	private int[] sequence = null;
	private Event[] events = null; // per event
	private LinkedHashMap<Integer, Double> ETF = new LinkedHashMap<>(); // ETF<event -> etf>: for each event -> etf = sum(each TF of events)
	private Table LETF = null;
	private PriorityQueue<Double> letf_e = new PriorityQueue<Double>(), lb_letf = new PriorityQueue<Double>();
	private Tree tree = null;
	private PriorityQueue<TreeNode> finalResults = new PriorityQueue<TreeNode>();
	
	double startTimestamp = 0, endTimestamp = 0;
	double maxMemory = -1; // the maximum memory cost
	int treeNodeCount = 1; // root
	
	
	/** Constructed functions **/
	public AlgoTTFE()
	{
		this.database = defaultDatabase;
		this.topK = defaultTopK;
		this.alpha = defaultAlpha;
		this.beta = defaultBeta;
		this.raise1 = defaultRaise1;
		this.raise2 = defaultRaise2;
		this.isPrint = defaultPrint;
	}
	public AlgoTTFE(String database)
	{
		this.database = database;
		this.topK = defaultTopK;
		this.alpha = defaultAlpha;
		this.beta = defaultBeta;
		this.raise1 = defaultRaise1;
		this.raise2 = defaultRaise2;
		this.isPrint = defaultPrint;
	}
	public AlgoTTFE(String database, int topK)
	{
		this.database = database;
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
	public AlgoTTFE(String database, int topK, double alpha, double beta)
	{
		this.database = database;
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
	public AlgoTTFE(String database, int topK, double alpha, double beta, boolean raise1, boolean raise2)
	{
		this.database = database;
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
	public AlgoTTFE(String database, int topK, double alpha, double beta, boolean raise1, boolean raise2, boolean isPrint)
	{
		this.database = database;
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
	private class TF
	{
		double alpha = 0.5, beta = 0.5, threat = 0, frequency = 0, tf = 0, rtf = 0;
		public TF(double alpha, double beta,  double threat, double frequency, double tf, double rtf)
		{
			this.threat = threat;
			this.frequency = frequency;
			this.alpha = alpha;
			this.beta = beta;
			this.tf = tf;
			this.rtf = rtf;
		}
		public TF(double threat, double frequency, double alpha, double beta)
		{
			this.threat = threat;
			this.frequency = frequency;
			this.alpha = alpha;
			this.beta = beta;
			this.tf = this.alpha * this.threat + this.beta * this.frequency;
		}
		public double update(double alpha, double beta)
		{
			this.alpha = alpha;
			this.beta = beta;
			this.tf = this.alpha * this.threat + this.beta * this.frequency;
			return this.tf;
		}
		public String getString(boolean isThreat, boolean isFrequency, boolean isTf, boolean isRtf)
		{
			if (isThreat && isFrequency && isTf && isRtf)
				return "[" + this.threat + ", " + this.frequency + ", " + this.tf + ", " + this.rtf + "]";
			else if (isThreat && isFrequency && isTf && !isRtf)
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
			return this.getString(true, true, true, true);
		}
	}
		
	private class Transaction // a line of the database
	{
		int tid = 0, topK = 0;
		double alpha = 0, beta = 0;
		LinkedHashMap<Integer, TF> events = new LinkedHashMap<>();
		Double ttf = null;
		public Transaction(int tid, int topK, double alpha, double beta)
		{
			this.tid = tid;
			this.topK = topK;
			this.alpha = alpha;
			this.beta = beta;
		}
		public boolean contains(Integer item)
		{
			return this.events.containsKey(item);
		}
		public boolean put(Integer item, TF tf)
		{
			if (this.contains(item))
				return false;
			this.events.put(item, tf);
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
				entry.getValue().update(alpha, beta);
				this.ttf += entry.getValue().tf;
			}
			return this.ttf;
		}
		public double update() { return update(this.topK, this.alpha, this.beta); }
		public int index(int event)
		{
			int tmp = -1;
			Set<Entry<Integer, TF>> set = this.events.entrySet();
			Iterator<Entry<Integer, TF>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				++tmp;
				Entry<Integer, TF> entry = iterator.next();
				if (entry.getKey() == event)
					return tmp;
			}
			return -1;
		}
		public boolean isSequence(ArrayList<Integer> events)
		{
			int tmp = this.index(events.get(0));
			if (-1 == tmp) // not found
				return false;
			for (int i = 1; i < events.size(); ++i)
				if (++tmp != this.index(events.get(i)))
					return false;
			return true;
		}
		@SuppressWarnings("unused")
		public boolean containsAll(ArrayList<Integer> events)
		{
			for (Integer event : events)
				if (!this.events.containsKey(event))
					return false;
			return true;
		}
		
		public double getAetfPerTransaction(ArrayList<Integer> sequence)
		{
			double transactionAetf = 0;
			for (int i = 0; i < sequence.size(); ++i)
				if (this.events.containsKey(sequence.get(i)))
					transactionAetf += this.events.get(sequence.get(i)).tf;
				else
					return 0;
			return transactionAetf;
		}
		@SuppressWarnings("unused")
		public ArrayList<Integer> getUnmatchedEventsPerTransaction(ArrayList<Integer> sequence)
		{
			ArrayList<Integer> missingEvents = new ArrayList<>();
			if (this.getAetfPerTransaction(sequence) <= 0) // the current sequence is invalid
				return missingEvents;
			Set<Entry<Integer, TF>> set = this.events.entrySet();
			Iterator<Entry<Integer, TF>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, TF> entry = iterator.next();
				if (((Integer)(entry.getKey())).intValue() == sequence.get(0).intValue()) // find the first matched sequence
				{
					for (int cnt = 1; cnt < sequence.size(); ++cnt) // go through the whole sequence to find out the non-continuous events
					{
						entry = iterator.next();
						if (((Integer)(entry.getKey())).intValue() != sequence.get(cnt).intValue())
							missingEvents.add(sequence.get(cnt));
					}
					break;
				}
			}
			return missingEvents;
		}
		
		public String getString(boolean isThreat, boolean isFrequency, boolean isTf, boolean isRtf)
		{
			if (null == this.ttf)
				this.update();
			String sRet = this.tid + " - {";
			Set<Entry<Integer, TF>> set = this.events.entrySet();
			Iterator<Entry<Integer, TF>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, TF> entry = iterator.next();
				sRet += (Integer)(entry.getKey()) + ":" + ((TF)(entry.getValue())).getString(isThreat, isFrequency, isTf, isRtf) + (iterator.hasNext() ? ", " : "} - " + this.ttf);
			}
			return sRet;
		}
		public String toString()
		{
			return this.getString(true, true, true, true);
		}
	}
	
	private class Event
	{
		int event = 0;
		LinkedHashMap<Integer, TF> transactions = new LinkedHashMap<Integer, TF>();
		public Event(int event)
		{
			this.event = event;
		}
		@SuppressWarnings("unused")
		public HashSet<Integer> getInterset(Event event)
		{
			HashSet<Integer> interSet = new HashSet<Integer>(this.transactions.keySet());
			interSet.retainAll((Set<Integer>)(event.transactions.keySet()));
			return interSet;
		}
		@SuppressWarnings("unused")
		public HashSet<Integer> getInterset(HashSet<Integer> keySet)
		{
			HashSet<Integer> interSet = (HashSet<Integer>)(this.transactions.keySet());
			interSet.retainAll(keySet);
			return interSet;
		}
		public String getString(boolean isThreat, boolean isFrequency, boolean isTf, boolean isRtf)
		{
			String sRet = event + " - {";
			Set<Entry<Integer, TF>> set = this.transactions.entrySet();
			Iterator<Entry<Integer, TF>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, TF> entry = iterator.next();
				sRet += (Integer)(entry.getKey()) + ":" + ((TF)(entry.getValue())).getString(isThreat, isFrequency, isTf, isRtf) + (iterator.hasNext() ? ", " : "}");
			}
			return sRet;
		}
		public String toString()
		{
			return this.getString(true, true, true, true);
		}
	}
	
	private class Table
	{
		String name = null;
		int[] index = null; // from index to columns to form a sequence
		int[] columns = null;
		int[] sequence = null;
		double[][] values = null;
		public Table(double[][] values, int[] index, int[] columns, String name)
		{
			this.values = values;
			this.index = index;
			this.columns = columns;
			this.sequence = new int[index.length + 1];
			this.sequence[0] = index[0];
			for (int i = 0; i < columns.length; ++i)
				this.sequence[i + 1] = columns[i];
			this.name = name;
		}
		@SuppressWarnings("unused")
		public Table(double[][] values, int[] sequence, String name)
		{
			this.values = values;
			this.sequence = sequence;
			if (sequence.length > 0)
			{
				this.columns = new int[sequence.length - 1];
				this.index = new int[sequence.length - 1];
			}
			else
			{
				this.columns = new int[0];
				this.index = new int[0];
			}
			for (int cnt = 0; cnt < sequence.length; ++cnt)
			{
				if (0 == cnt)
					index[cnt] = sequence[cnt];
				else if (sequence.length - 1 == cnt)
					columns[cnt - 1] = sequence[cnt];
				else
				{
					index[cnt] = sequence[cnt];
					columns[cnt - 1] = sequence[cnt];
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
		@SuppressWarnings("unused")
		public boolean addValueByIndex(int indexIdx, int columnsIdx, double value)
		{
			if (indexIdx < 0 || columnsIdx < 0 || indexIdx >= this.index.length || columnsIdx >= this.columns.length)
				return false;
			this.values[indexIdx][columnsIdx] += value;
			return true;
		}
		public boolean addValueByName(int indexName, int columnName, double value)
		{
			int columnsIndex = this.getColumnsIndex(columnName), indexIndex = this.getIndexIndex(indexName);
			if (-1 == columnsIndex || -1 == indexIndex)
				return false;
			this.values[indexIndex][columnsIndex] += value;
			return true;
		}
		public ArrayList<Integer> getMiddleElements(int p, int q, boolean ht)
		{
			ArrayList<Integer> array = new ArrayList<>();
			boolean isAdd = false;
			for (Integer element : this.sequence)
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
			String sRet = "Sequence: " + this.sequence[0];
			for (int i = 1; i < this.sequence.length; ++i)
				sRet += " -> " + this.sequence[i];
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
	
	private class TreeNode implements Comparable<TreeNode>
	{
		ArrayList<Integer> sequence = null; // current sequence
		double aetf = 0, eetf = 0, retf = 0;
		TreeNode parent = null;
		LinkedHashMap<Integer, TF> transactions = new LinkedHashMap<Integer, TF>();
		ArrayList<TreeNode> children = new ArrayList<TreeNode>();
		public TreeNode(int event) // event (root)
		{
			this.sequence = new ArrayList<Integer>();
			this.sequence.add(event);
		}
		public TreeNode(Integer event) // event (root)
		{
			this.sequence = new ArrayList<Integer>();
			this.sequence.add(event);
		}
		public TreeNode(ArrayList<Integer> sequence) // sequence
		{
			this.sequence = new ArrayList<Integer>(sequence);
		}
		public boolean equals(TreeNode treeNode, boolean isAetf, boolean isEetf, boolean isRetf)
		{
			if (null == treeNode || !(treeNode instanceof TreeNode))
				return false;
			else if (treeNode == this)
				return true;
			else if (treeNode.sequence.size() != this.sequence.size())
				return false;
			else
			{
				for (int i = 0; i < this.sequence.size(); ++i)
					if (treeNode.sequence.get(i) != this.sequence.get(i))
						return false;
				if (isAetf && treeNode.aetf != this.aetf)
					return false;
				if (isEetf && treeNode.eetf != this.eetf)
					return false;
				if (isRetf && treeNode.retf != this.retf)
					return false;
				return true;
			}
		}
		@SuppressWarnings("unused")
		public boolean equals(TreeNode treeNode) { return this.equals(treeNode, false, false, false); }
		public TreeNode findTargetChildTreeNode(ArrayList<Integer> sequence)
		{
			if (sequence.isEmpty() || sequence.size() <= this.sequence.size())
				return null;
			for (TreeNode childTreeNode : this.children)
				if (childTreeNode.sequence.get(this.sequence.size()).intValue() == sequence.get(this.sequence.size()).intValue())
				{
					if (childTreeNode.sequence.size() == sequence.size())
						return childTreeNode;
					else
						return childTreeNode.findTargetChildTreeNode(sequence);
				}
			return null;
		}
		@Override
		public int compareTo(TreeNode treeNode)
		{
			if (this.eetf < treeNode.eetf)
				return -1;
			else if (this.eetf > treeNode.eetf)
				return 1;
			else
				return 0;
		}
		
		public double update(PriorityQueue<TreeNode> finalResults, int topK, double delta)
		{
			finalResults.offer(this);
			while (finalResults.size() > topK)
				finalResults.poll();
			return finalResults.size() >= topK ? finalResults.peek().eetf : delta;
		}
		private String getChildren(boolean isAetf, boolean isEetf, boolean isRetf, boolean isChildren)
		{
			String sRet = "{";
			for (TreeNode child : this.children)
				sRet += child.getString(isAetf, isEetf, isRetf, isChildren) + ", ";
			if (sRet.endsWith(", "))
				sRet = sRet.substring(0, sRet.length() - 2); // remove extra ", "
			sRet += "}";
			return sRet;
		}
		public HashSet<Integer> getInterset(TreeNode treeNode)
		{
			HashSet<Integer> interSet = new HashSet<Integer>(this.transactions.keySet());
			interSet.retainAll((Set<Integer>)(treeNode.transactions.keySet()));
			return interSet;
		}
		public HashSet<Integer> getInterset(Event event)
		{
			HashSet<Integer> interSet = new HashSet<Integer>(this.transactions.keySet());
			interSet.retainAll((Set<Integer>)(event.transactions.keySet()));
			return interSet;
		}
		public HashSet<Integer> getInterset(HashSet<Integer> keySet)
		{
			HashSet<Integer> interSet = new HashSet<Integer>(this.transactions.keySet());
			interSet.retainAll(keySet);
			return interSet;
		}
		public String getString(boolean isAetf, boolean isEetf, boolean isRetf, boolean isChildren)
		{
			String sRet = "";
			if (this.sequence.isEmpty()) // "":
				sRet += "\"\"";
			else if (this.sequence.size() == 1) // int:
				sRet += this.sequence.get(0);
			else // (, ):
			{
				sRet += "(" + this.sequence.get(0);
				for (int i = 1; i < this.sequence.size(); ++i)
					sRet += ", " + this.sequence.get(i);
				sRet += ")";
			}
			if (isAetf && isEetf && isRetf && isChildren) // :[, ] (include children)
				sRet += ":[" + this.aetf + ", " + this.eetf + ", " + this.retf + ", " + this.getChildren(isAetf, isEetf, isRetf, isChildren) + "]";
			else if (isAetf && isEetf && isRetf && !isChildren) // :[, ] (exclude children)
				sRet += ":[" + this.aetf + ", " + this.eetf + ", " + this.retf + "]";
			else if (!isAetf && isEetf && !isRetf && isChildren) // :[double, children]
				sRet += ":[" + this.eetf + ", " + this.getChildren(isAetf, isEetf, isRetf, isChildren) + "]";
			else if (!isAetf && isEetf && !isRetf && !isChildren) // :double
				sRet += ":" + this.eetf;
			else // // {:, :}
			{
				sRet += ":{";
				if (isAetf)
					sRet += "\"aetf\":" + this.aetf + ", ";
				if (isEetf)
					sRet += "\"eetf\":" + this.eetf + ", ";
				if (isRetf)
					sRet += "\"retf\":" + this.retf + ", ";
				if (isChildren)
					sRet += "\"children\":" + this.children + ", ";
				if (sRet.endsWith(", "))
					sRet = sRet.substring(0, sRet.length() - 2); // remove extra ", "
				sRet += "}";
			}
			return sRet;
		}
		public String toString()
		{
			return this.getString(false, true, false, true);
		}
	}
	
	private class Tree
	{
		LinkedHashMap<Integer, TreeNode> roots = new LinkedHashMap<Integer, TreeNode>();
		public Tree(int[] sequence) // sequence
		{
			for (int event : sequence)
				this.roots.put(event, new TreeNode(event));
		}
		@SuppressWarnings("unused")
		public Tree(ArrayList<Integer> sequence) // sequence
		{
			for (Integer event : sequence)
				this.roots.put(event, new TreeNode(event));
		}
		public boolean containsRoot(int root)
		{
			Set<Entry<Integer, TreeNode>> set = this.roots.entrySet();
			Iterator<Entry<Integer, TreeNode>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, TreeNode> entry = iterator.next();
				if (entry.getKey().doubleValue() == root)
					return true;
			}
			return false;
		}
		public boolean containsRoot(Integer root)
		{
			return containsRoot(root.intValue());
		}
		@SuppressWarnings("unused")
		public TreeNode findTargetTreeNode(ArrayList<Integer> sequence)
		{
			if (sequence.isEmpty() || !this.containsRoot(sequence.get(0)))
				return null;
			else if (sequence.size() == 1)
				return this.roots.get(sequence.get(0));
			else
				return this.roots.get(sequence.get(0)).findTargetChildTreeNode(sequence);
		}
		public String getString(boolean isAetf, boolean isEetf, boolean isRetf, boolean isChildren)
		{
			String sRet = "{";
			Set<Entry<Integer, TreeNode>> set = this.roots.entrySet();
			Iterator<Entry<Integer, TreeNode>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, TreeNode> entry = iterator.next();
				sRet += entry.getValue().getString(isAetf, isEetf, isRetf, isChildren) + (iterator.hasNext() ? ", " : "");
			}
			sRet += "}";
			return sRet;
		}
		public String toString()
		{
			return this.getString(true, true, true, true);
		}
	}
	
	
	/** Child functions **/
	public static boolean checkDatabase(String database)
	{
		for (int i = 0; i < database.length(); ++i)
			if (database.charAt(i) < 'a' || database.charAt(i) > 'z')
				return false;
		return true;
	}
	public static String getOriginalDatabaseExpression(String originalDatabase)
	{
		return originalDatabase.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t").replace("\"", "\\\"");
	}
	public static void printDatabaseStatement(String originalDatabase, String revisedDatabase)
	{
		System.out.printf("The passed parameter database should be a string filled with lower-case letters. It is defaulted to %s. \nParameter: database = \"%s\" -> database = %s\n\n", revisedDatabase, getOriginalDatabaseExpression(originalDatabase), revisedDatabase);
		return;
	}
	
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
					Transaction transaction = new Transaction(++tid, this.topK, this.alpha, this.beta);
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
							transaction.put(new Integer(Integer.parseInt(item_str[i])), new TF(Double.parseDouble(threat_str[i]), Double.parseDouble(frequency_str[i]), this.alpha, this.beta));
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
		
		for (Transaction transaction : this.transactions)
		{
			Double ttf = transaction.ttf;
			if (this.isPrint && ttf != transaction.update(this.topK, this.alpha, this.beta)) // check TTF values
				System.out.printf("The value of the input TTF of Transaction %d is not the same as it is computed. It is revised to %s. \nTransaction %s: ttf = %s -> %s\n\n", transaction.tid, transaction.ttf.toString(), transaction.toString(), ttf.toString(), transaction.ttf.toString());
		}
		
		if (this.isPrint)
		{
			printTitle(algoName + " - initTTFE()");
			System.out.println("topK: " + this.topK + "\t\talpha: " + this.alpha + "\t\tbeta: " + this.beta);
			System.out.println("Transactions: ");
			for (Transaction transaction : transactions)
				System.out.println(transaction.getString(true, true, true, false));
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
			System.out.print("TWTF: {");
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
		this.sequence = new int[entryList.size()];
		this.events = new Event[entryList.size()];
		int pointer = 0;
		for (Entry<Integer, Double> entry : entryList)
		{
			this.TWTF.put(entry.getKey(), entry.getValue());
			this.sequence[pointer] = entry.getKey().intValue();
			this.events[pointer++] = new Event(entry.getKey().intValue());
		}
		
		if (this.isPrint)
		{
			printTitle(algoName + " - sortTWTF()");
			System.out.print("TWTF (UP): {");
			Set<Entry<Integer, Double>> set = this.TWTF.entrySet();
			Iterator<Entry<Integer, Double>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, Double> entry = iterator.next();
				System.out.print((Integer)entry.getKey() + ":" + (Double)entry.getValue() + (iterator.hasNext() ? ", " : ""));
			}
			System.out.println("}");
			if (this.sequence.length > 0)
			{
				System.out.printf("Sequence: %d", this.sequence[0]);
				for (int i = 1; i < this.sequence.length; ++i)
					System.out.printf(" -> %d", this.sequence[i]);
				System.out.println();
			}
			System.out.println();
		}
		return;
	}
	
	/* Compute RTF */
	private void computeRTF()
	{
		for (Transaction transaction : this.transactions)
			for (int i = 0; i < this.sequence.length; ++i)
			{
				/* Compute RTF */
				if (transaction.events.containsKey(this.sequence[i]))
					for (int j = i + 1; j < this.sequence.length; ++j)
						if (transaction.events.containsKey(this.sequence[j]))
							transaction.events.get(this.sequence[i]).rtf += transaction.events.get(this.sequence[j]).tf;
				
				/* Build Index (Event -> Transaction) */
				if (transaction.events.containsKey(this.sequence[i]))
					this.events[i].transactions.put(transaction.tid, transaction.events.get(this.sequence[i]));
			}
		
		if (this.isPrint)
		{
			printTitle(algoName + " - computeRTF()");
			System.out.println("topK: " + this.topK + "\t\talpha: " + this.alpha + "\t\tbeta: " + this.beta);
			System.out.println("Transactions: ");
			for (Transaction transaction : this.transactions)
				System.out.println(transaction);
			System.out.println("Events: ");
			for (Event event : this.events)
				System.out.println(event);
			System.out.println();
		}
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
			System.out.print("ETF: {");
			Set<Entry<Integer, Double>> set = this.ETF.entrySet();
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
	
	/* Sort ETF */
	private void sortETF()
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
		if ((entryList.size() <= this.topK ? entryList.size() : this.topK) > 0) // for security issue
				this.delta = new Double(entryList.get((entryList.size() <= this.topK ? entryList.size() : this.topK) - 1).getValue());
		
		if (this.isPrint)
		{
			printTitle(algoName + " - sortETF()");
			System.out.print("ETF (DOWN): {");
			Set<Entry<Integer, Double>> set = this.ETF.entrySet();
			Iterator<Entry<Integer, Double>> iterator = set.iterator();
			while (iterator.hasNext())
			{
				Entry<Integer, Double> entry = iterator.next();
				System.out.print((Integer)entry.getKey() + ":" + (Double)entry.getValue() + (iterator.hasNext() ? ", " : "}\n"));
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
				System.out.print((Integer)entry.getKey() + ":" + (Double)entry.getValue() + (iterator.hasNext() ? ", " : ""));
			}
			System.out.println("}");
			if (!this.transactions.isEmpty())
			{
				System.out.println("Pruned Transactions: ");
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
			Transaction transaction = this.transactions.get(i), newTransaction = new Transaction(transaction.tid, transaction.topK, transaction.alpha, transaction.beta);
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
			for (Transaction transaction : this.transactions)
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
			ArrayList<Integer> itemSequence = new ArrayList<>(); // sequence in a line
			while (iteratorEvent.hasNext()) // per item
			{
				Entry<Integer, TF> entryItem = iteratorEvent.next();
				itemSequence.add((Integer)entryItem.getKey());
			}
			for (int i = 0; i < this.sequence.length - 1; ++i)
			{
				int p = this.sequence[i]; // the head of the sequence
				if (!transaction.events.containsKey(p)) // skip events not found
					continue;
				ArrayList<Integer> subSequence = new ArrayList<>(); // subsequence
				subSequence.add(p);
				double pqValue = transaction.events.get(p).tf;
				for (int j = i + 1; j < this.sequence.length; ++j)
				{
					int q = this.sequence[j]; // the tail of the sequence
					if (!transaction.events.containsKey(q)) // break if it is not found
						break;
					subSequence.add(q);
					pqValue += transaction.events.get(q).tf; // the sum of the sequence
					if (transaction.isSequence(subSequence))
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
		{
			this.tree = new Tree(this.sequence);
			ArrayDeque<ArrayList<Integer>> queueIndex = new ArrayDeque<ArrayList<Integer>>(); // to implement layer-by-layer tree building
			ArrayDeque<TreeNode> queueTreeNode = new ArrayDeque<TreeNode>();
			for (int eventIndex = 0; eventIndex < this.sequence.length; ++eventIndex)
			{
				ArrayList<Integer> subSequenceIndex = new ArrayList<Integer>();
				subSequenceIndex.add(eventIndex);
				queueIndex.offer(subSequenceIndex);
				queueTreeNode.offer(this.tree.roots.get(this.sequence[subSequenceIndex.get(0)]));
			}
			while (!queueIndex.isEmpty() && !queueTreeNode.isEmpty()) // && for security consideration
			{
				++this.treeNodeCount;
				ArrayList<Integer> subSequenceIndex = queueIndex.poll(), subSequence = new ArrayList<Integer>();
				for (Integer eventIndex : subSequenceIndex)
					subSequence.add(this.sequence[eventIndex.intValue()]);
				TreeNode currentTreeNode = queueTreeNode.poll();
				
				if (subSequence.size() == 1) // 1-itemset
				{
					currentTreeNode.eetf = this.ETF.get(subSequence.get(0));
					for (Transaction transaction : this.transactions)
						if (transaction.events.containsKey(subSequence.get(0)))
						{
							currentTreeNode.transactions.put(transaction.tid, transaction.events.get(subSequence.get(0)));
							currentTreeNode.retf += transaction.events.get(subSequence.get(0)).rtf;
						}
				}
				else if (subSequence.size() == 2) // 2-itemset
				{
					HashSet<Integer> interSet = currentTreeNode.parent.getInterset(this.events[subSequenceIndex.get(1)]);
					
					if (!interSet.isEmpty())
						for (Integer transactionID : interSet)
						{
							currentTreeNode.transactions.put(
								transactionID, 
								new TF(
									this.alpha, 
									this.beta, 
									this.events[subSequenceIndex.get(0)].transactions.get(transactionID).threat + this.events[subSequenceIndex.get(1)].transactions.get(transactionID).threat,
									this.events[subSequenceIndex.get(0)].transactions.get(transactionID).frequency + this.events[subSequenceIndex.get(1)].transactions.get(transactionID).frequency, 
									this.events[subSequenceIndex.get(0)].transactions.get(transactionID).tf + this.events[subSequenceIndex.get(1)].transactions.get(transactionID).tf, 
									this.events[subSequenceIndex.get(subSequenceIndex.size() - 1)].transactions.get(transactionID).rtf
								)
							);
							currentTreeNode.eetf += this.events[subSequenceIndex.get(0)].transactions.get(transactionID).tf + this.events[subSequenceIndex.get(1)].transactions.get(transactionID).tf;
							currentTreeNode.retf += this.events[subSequenceIndex.get(subSequenceIndex.size() - 1)].transactions.get(transactionID).rtf;
						}
				}
				else if (subSequence.size() > 2) // 3-itemset and above
				{
					ArrayList<Integer> subSequenceB = new ArrayList<Integer>(subSequence);
					subSequenceB.remove(subSequenceB.size() - 2);
					TreeNode parentNearTreeNode = currentTreeNode.parent.parent.findTargetChildTreeNode(subSequenceB);
					HashSet<Integer> interSet = currentTreeNode.parent.parent.getInterset(currentTreeNode.parent.getInterset(parentNearTreeNode));
					
					if (!interSet.isEmpty())
						for (Integer transactionID : interSet)
						{
							currentTreeNode.transactions.put(
									transactionID, 
									new TF(
										this.alpha, 
										this.beta, 
										currentTreeNode.parent.transactions.get(transactionID).threat + parentNearTreeNode.transactions.get(transactionID).threat - currentTreeNode.parent.parent.transactions.get(transactionID).threat, 
										currentTreeNode.parent.transactions.get(transactionID).frequency + parentNearTreeNode.transactions.get(transactionID).frequency - currentTreeNode.parent.parent.transactions.get(transactionID).frequency, 
										currentTreeNode.parent.transactions.get(transactionID).tf + parentNearTreeNode.transactions.get(transactionID).tf - currentTreeNode.parent.parent.transactions.get(transactionID).tf, 
										parentNearTreeNode.transactions.get(transactionID).rtf
									)
								);
							currentTreeNode.eetf += currentTreeNode.transactions.get(transactionID).tf;
							currentTreeNode.retf += currentTreeNode.transactions.get(transactionID).rtf;
						}
				}
				
				currentTreeNode.aetf = currentTreeNode.eetf + currentTreeNode.retf;
				if (currentTreeNode.eetf > this.delta) // can be added into prior queue
					this.delta = currentTreeNode.update(this.finalResults, this.topK, this.delta); // update delta if the prior queue is full (length >= topK)
				if (currentTreeNode.aetf > this.delta) // can build children
				{
					for (int subEventIndex = subSequenceIndex.get(subSequenceIndex.size() - 1) + 1; subEventIndex < this.sequence.length; ++subEventIndex)		
					{
						ArrayList<Integer> childSequenceIndex = new ArrayList<Integer>(subSequenceIndex);
						childSequenceIndex.add(new Integer(subEventIndex));
						ArrayList<Integer> childSequence = new ArrayList<Integer>(subSequence);
						childSequence.add(new Integer(this.sequence[subEventIndex]));
						TreeNode childTreeNode = new TreeNode(childSequence);
						currentTreeNode.children.add(childTreeNode);
						childTreeNode.parent = currentTreeNode;
					}
					int pointer = 0;
					for (int subEventIndex = subSequenceIndex.get(subSequenceIndex.size() - 1) + 1; subEventIndex < this.sequence.length; ++subEventIndex)		
					{
						ArrayList<Integer> childSequenceIndex = new ArrayList<Integer>(subSequenceIndex);
						childSequenceIndex.add(new Integer(subEventIndex));
						queueIndex.offer(childSequenceIndex);
						queueTreeNode.offer(currentTreeNode.children.get(pointer++));
					}
				}
			}
		}
		
		if (this.isPrint)
		{
			printTitle(algoName + " - generateTree()");
			System.out.println("delta = " + this.delta);
			System.out.println("Tree: " + this.tree);
			System.out.println("Count of tree nodes: " + this.treeNodeCount + "\n");
		}
		return;
	}
	
	/* Get results */
	public void getResults() throws IOException
	{
		if (this.isPrint)
			printTitle(algoName + " - getResults()");
		File file = new File(this.outputFile.replace("{database}", this.database).replace("{topK}", "" + this.topK).replace("{alpha}", "" + this.alpha).replace("{beta}", "" + this.beta).replace("{delta}", "" + this.delta).replace("{raise1}", "" + this.raise1).replace("{raise2}", "" + this.raise2).replace("{isPrint}", "" + this.isPrint));
		FileWriter fw = new FileWriter(file);
		fw.write("Database: " + this.database + "\t\ttopK = " + this.topK + "\n");
		fw.write("alpha = " + this.alpha + "\t\tbeta = " + this.beta + "\t\tdelta = " + this.delta + "\n");
		fw.write("raise1 = " + this.raise1 + "\t\traise2 = " + this.raise2 + "\t\tisPrint = " + this.isPrint + "\n");
		fw.write("Time: " + (endTimestamp - startTimestamp) / 1000.0 + " s\t\tSpace: " + new java.text.DecimalFormat("#.00").format(this.maxMemory) + " MB\t\tTree node count: " + this.treeNodeCount + "\nFinal results: {");
		if (this.isPrint)
			System.out.print("Final results: {");
		while (!this.finalResults.isEmpty())
		{
			TreeNode treeNode = this.finalResults.poll();
			String tmpString = treeNode.getString(false, true, false, false) + (this.finalResults.isEmpty() ? "" : ", ");
			fw.write(tmpString);
			if (this.isPrint)
				System.out.print(tmpString);
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
		this.computeRTF(); this.checkMemory();
		this.computeETF(); this.checkMemory();
		this.sortETF(); this.checkMemory();
		this.pruneItem(); this.checkMemory();
		this.sortTTFE(); this.checkMemory();
		this.generateTable(); this.checkMemory();
		if (this.raise1) { this.raiseThreshold_LETF_E(); this.checkMemory(); }
		if (this.raise2) { this.raiseThreshold_LB_LETF(); this.checkMemory(); }
		this.generateTree(); this.checkMemory();
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
	
	public void printStats(String errorInfo)
	{
		System.out.println("==========  " + algoName + " Algorithm - Results  ==========");
		System.out.println("\tDatabase: " + this.database);
		System.out.println("\ttopK: " + this.topK);
		System.out.println("\talpha: " + this.alpha);
		System.out.println("\tbeta: " + this.beta);
		System.out.println("\tdelta: " + this.delta);
		System.out.println("\traise1: " + this.raise1);
		System.out.println("\traise2: " + this.raise2);
		System.out.println("\tisPrint: " + this.isPrint);
		if (null == errorInfo) // no errors
		{
			System.out.println("\tTotal time: " + (endTimestamp - startTimestamp) / 1000.0 + " s");
			System.out.println("\tMax memory: " + new java.text.DecimalFormat("#.00").format(this.maxMemory) + " MB");
			System.out.println("\tCount of tree nodes: " + this.treeNodeCount);
		}
		else
			System.out.println("\tError info: " + errorInfo);
		System.out.println("\tPrint time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()));
		System.out.println("================================================\n");
		return;
	}
}