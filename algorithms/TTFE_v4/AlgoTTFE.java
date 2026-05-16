package ttfe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;


/**
 * TTFE (Powered by Universe)
 * @author Yuer Yang, Department of Computer Science, The University of Hong Kong
 * @supervisor Zhihua Zhong, School of Computing, Tokyo Institute of Technology
 * 
 * initTTFE()* -> Scan the database D -> TTF<transaction -> ttf>: for each transaction tau in database D -> ttf(tau) = alpha * sum([t(x) for x in tau]) + beta * sum([f(x) for x in tau])
 * computeTWTF() -> TWTF<event -> twtf>: for each event x -> twtf(x) = sum([ttf(tau) for transaction tau in database D if event x in transaction tau])
 * sortTWTF() -> TWTF: for each event -> sorted(TWTF, key = lambda x:TWTF(x), reverse = False)
 * computeRTF() -> RTF: for each transaction tau for each event x -> rtf(x) = sum([tf(y) for y in "all events after x in sequence"])
 * computeETF() -> ETF<event -> etf>: for each event x -> etf(x) = sum([TF(x) for transaction tau in database D if event x in transaction tau])
 * sortETF() -> ETF: for each event x -> sorted(ETF, key = lambda x:ETF(x), reverse = True) -> delta
 * pruneItem() -> TWTF & Transactions: cut off the events in TWTF whose value is less than delta
 * sortedTTFE() -> TTFE: for each transaction -> sorted(transaction) according to TWTF (Up)
 * generateTable() -> LETF: adding continuous tf, get 2D table sorted by TWTF (Up)
 * raiseThreshold_LETF_E() -> delta: switches delta to topK value of LETF
 * raiseThreshold_LETF_LB() -> delta: vaguely switches delta according to SLB
 * generateTree() -> Tree: build tree layer by layer using a queue and generate prior queue to get the final results
 * getResults()* -> Results: print results and evaluation metrics
 */
public class AlgoTTFE
{
	/** Main parameters **/
	public static final String algoName = "TTFE"; // name of the algorithm (four characters)
	public static final String defaultDatabase = "TTFE"; // default database value
	public static final int DEBUG_LEVEL_ALL = 3, DEBUG_LEVEL_INFO = 2, DEBUG_LEVEL_PROCEDURE = 1, DEBUG_LEVEL_CLOSE = 0;
	public static final int defaultTopK = 5, defaultDebugLevel = DEBUG_LEVEL_INFO, defaultWidth = 100; // default values
	public static final double defaultAlpha = 0.5, defaultBeta = 0.5; // default values
	public static final Double defaultDeltaInput = null; // default delta value
	public static final boolean defaultSwitches[] = { false, true, true, false, true, true }; // default values
	public static final java.text.DecimalFormat defaultDecimalFormatter = new java.text.DecimalFormat("#.###");
	public static final SimpleDateFormat defaultDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	private String database = defaultDatabase; // database
	private int topK = defaultTopK, debugLevel = defaultDebugLevel; // topK and debugLevel
	private double alpha = defaultAlpha, beta = defaultBeta; // weighting parameters
	private Double delta = null, deltaInput = defaultDeltaInput; // initial a key variable (null is used as -float("inf") in Python here)
	private boolean switches[] = { defaultSwitches[0], defaultSwitches[1], defaultSwitches[2], defaultSwitches[3], defaultSwitches[4], defaultSwitches[5] };
	
	private String inputFilePath = null, outputFilePath = null; // input and output
	private ArrayList<Transaction> transactions = new ArrayList<Transaction>(); // per line
	private LinkedHashMap<Integer, Double> TWTF = new LinkedHashMap<>(); // TWTF<event -> twtf>: for each event -> twtf = sum([ttf if event in transaction])
	private int[] sequence = null;
	private Event[] events = null; // per event
	private LinkedHashMap<Integer, Double> ETF = new LinkedHashMap<>(); // ETF<event -> etf>: for each event -> etf = sum(each TF of events)
	private Table LETF = null;
	private PriorityQueue<Double> letf_e = new PriorityQueue<Double>(), letf_lb = new PriorityQueue<Double>();
	private Tree tree = null;
	private PriorityQueue<HTFE> finalResults = new PriorityQueue<HTFE>();
	
	private double startTimestamp = 0, endTimestamp = 0, maxMemory = -1; // the time consumption and the maximum memory cost 
	private int treeNodeCount = 1; // root
	private boolean securityFlag = false;
	
	
	/** Construction functions **/
	public static class Builder
	{
		private String database = defaultDatabase;
		private int topK = defaultTopK, debugLevel = defaultDebugLevel;
		private double alpha = defaultAlpha, beta = defaultBeta;
		private Double deltaInput = defaultDeltaInput;
		private boolean switches[] = { defaultSwitches[0], defaultSwitches[1], defaultSwitches[2], defaultSwitches[3], defaultSwitches[4], defaultSwitches[5] };
		
		public Builder() {}
		public Builder buildDatabase(String database)
		{
			if (checkDatabase(database))
				this.database = database;
			else
			{
				this.database = defaultDatabase;
				printDatabaseStatement(database, defaultDatabase);
			}
			return this;
		}
		public Builder buildTopK(int topK)
		{
			if (checkTopK(topK))
				this.topK = topK;
			else
			{
				this.topK = defaultTopK;
				printTopKStatement(topK, defaultTopK);
			}
			return this;
		}
		public Builder buildAlpha(double alpha)
		{
			return buildAlphaBeta(alpha, 1 - alpha);
		}
		public Builder buildBeta(double beta)
		{
			return buildAlphaBeta(1 - beta, beta);
		}
		public Builder buildAlphaBeta(double alpha, double beta)
		{
			if (checkAlphaAndBeta(alpha, beta))
			{
				this.alpha = alpha;
				this.beta = beta;
			}
			else
			{
				this.alpha = defaultAlpha;
				this.beta = defaultBeta;
				printAlphaAndBetaStatement(alpha, beta, defaultAlpha, defaultBeta);
			}
			return this;
		}
		public Builder buildDelta(Double delta)
		{
			this.deltaInput = null == delta ? null : Double.valueOf(delta);
			return this;
		}
		public Builder buildSwitches(boolean[] switches)
		{
			for (int i = 0; i < Math.min(this.switches.length, switches.length); ++i)
				this.switches[i] = switches[i];
			return this;
		}
		public Builder buildDebugLevel(int debugLevel)
		{
			if (checkDebugLevel(debugLevel))
				this.debugLevel = debugLevel;
			else
			{
				this.debugLevel = defaultDebugLevel;
				printDebugLevelStatement(debugLevel, defaultDebugLevel);
			}
			return this;
		}
		public AlgoTTFE build()
		{
            return new AlgoTTFE(this);
        }
	}
	public AlgoTTFE()
	{
		this.database = defaultDatabase;
		this.topK = defaultTopK;
		this.alpha = defaultAlpha;
		this.beta = defaultBeta;
		this.deltaInput = defaultDeltaInput;
		for (int i = 0; i < Math.min(this.switches.length, defaultSwitches.length); ++i)
			this.switches[i] = defaultSwitches[i];
		this.debugLevel = defaultDebugLevel;
	}
	public AlgoTTFE(String database)
	{
		if (checkDatabase(database))
			this.database = database;
		else
		{
			this.database = defaultDatabase;
			printDatabaseStatement(database, defaultDatabase);
		}
		this.topK = defaultTopK;
		this.alpha = defaultAlpha;
		this.beta = defaultBeta;
		this.deltaInput = defaultDeltaInput;
		for (int i = 0; i < Math.min(this.switches.length, defaultSwitches.length); ++i)
			this.switches[i] = defaultSwitches[i];
		this.debugLevel = defaultDebugLevel;
	}
	public AlgoTTFE(String database, int topK)
	{
		if (checkDatabase(database))
			this.database = database;
		else
		{
			this.database = defaultDatabase;
			printDatabaseStatement(database, defaultDatabase);
		}
		if (checkTopK(topK))
			this.topK = topK;
		else
		{
			printTopKStatement(topK, defaultTopK);
			this.topK = defaultTopK;
		}
		this.alpha = defaultAlpha;
		this.beta = defaultBeta;
		this.deltaInput = defaultDeltaInput;
		for (int i = 0; i < Math.min(this.switches.length, defaultSwitches.length); ++i)
			this.switches[i] = defaultSwitches[i];
		this.debugLevel = defaultDebugLevel;
	}
	public AlgoTTFE(String database, int topK, double alpha, double beta)
	{
		if (checkDatabase(database))
			this.database = database;
		else
		{
			this.database = defaultDatabase;
			printDatabaseStatement(database, defaultDatabase);
		}
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
		this.deltaInput = defaultDeltaInput;
		for (int i = 0; i < Math.min(this.switches.length, defaultSwitches.length); ++i)
			this.switches[i] = defaultSwitches[i];
		this.debugLevel = defaultDebugLevel;
	}
	public AlgoTTFE(String database, int topK, double alpha, double beta, double deltaInput)
	{
		if (checkDatabase(database))
			this.database = database;
		else
		{
			this.database = defaultDatabase;
			printDatabaseStatement(database, defaultDatabase);
		}
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
		this.deltaInput = deltaInput;
		for (int i = 0; i < Math.min(this.switches.length, defaultSwitches.length); ++i)
			this.switches[i] = switches[i];
		this.debugLevel = defaultDebugLevel;
	}
	public AlgoTTFE(String database, int topK, double alpha, double beta, double deltaInput, boolean[] switches)
	{
		if (checkDatabase(database))
			this.database = database;
		else
		{
			this.database = defaultDatabase;
			printDatabaseStatement(database, defaultDatabase);
		}
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
		this.deltaInput = deltaInput;
		for (int i = 0; i < Math.min(this.switches.length, switches.length); ++i)
			this.switches[i] = switches[i];
		this.debugLevel = defaultDebugLevel;
	}
	public AlgoTTFE(String database, int topK, double alpha, double beta, double deltaInput, boolean[] switches, int debugLevel)
	{
		if (checkDatabase(database))
			this.database = database;
		else
		{
			this.database = defaultDatabase;
			printDatabaseStatement(database, defaultDatabase);
		}
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
		this.deltaInput = deltaInput;
		for (int i = 0; i < Math.min(this.switches.length, defaultSwitches.length); ++i)
			this.switches[i] = switches[i];
		this.debugLevel = debugLevel;
	}
	private AlgoTTFE(Builder builder)
	{
		this.database = builder.database;
		this.topK = builder.topK;
		this.alpha = builder.alpha;
		this.beta = builder.beta;
		this.deltaInput = builder.deltaInput;
		for (int i = 0; i < Math.min(this.switches.length, builder.switches.length); ++i)
			this.switches[i] = builder.switches[i];
		this.debugLevel = builder.debugLevel;
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
	
	private class HTFE implements Comparable<HTFE>
	{
		ArrayList<Integer> sequence = null;
		double eetf = 0;
		public HTFE(ArrayList<Integer> sequence, double eetf)
		{
			this.sequence = new ArrayList<Integer>(sequence);
			this.eetf = eetf;
		}
		public boolean equals(HTFE htfe)
		{
			if (htfe.sequence.size() != this.sequence.size())
				return false;
			for (int i = 0; i < this.sequence.size(); ++i)
				if (htfe.sequence.get(i) != this.sequence.get(i))
					return false;
			return true;
		}
		public String toString()
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
			sRet += this.eetf;
			return sRet;
		}
		@Override
		public int compareTo(HTFE htfe)
		{
			if (this.eetf < htfe.eetf)
				return -1;
			else if (this.eetf > htfe.eetf)
				return 1;
			else
				return 0;
		}
	}
	
	private class TreeNode
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
		
		public Double update(PriorityQueue<HTFE> finalResults, int topK, Double delta)
		{
			HTFE htfe = new HTFE(this.sequence, this.eetf);
			finalResults.offer(htfe);
			while (finalResults.size() > topK)
				finalResults.poll();
			return finalResults.size() >= topK ? Double.valueOf(finalResults.peek().eetf) : delta;
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
	
	
	/** Getter and setter **/
	public void setTimeConsumption(double timeConsumption)
	{
		this.startTimestamp = 0;
		this.endTimestamp = timeConsumption;
		this.securityFlag = false; // no longer secure until the algorithm is run again
		return;
	}
	public double getTimeConsumption()
	{
		return this.endTimestamp - this.startTimestamp;
	}
	
	public void setMaxMemory(double maxMemory)
	{
		this.maxMemory = maxMemory;
		this.securityFlag = false; // no longer secure until the algorithm is run again
		return;
	}
	public double getMaxMemory()
	{
		return this.maxMemory;
	}
	
	public void setDelta(Double delta)
	{
		this.delta = Double.valueOf(delta);
		this.securityFlag = false; // no longer secure until the algorithm is run again
		return;
	}
	public void setDelta(double delta)
	{
		this.delta = Double.valueOf(delta);
		this.securityFlag = false; // no longer secure until the algorithm is run again
		return;
	}
	public Double getDelta()
	{
		return this.delta;
	}
	
	public void setFinalResults(PriorityQueue<HTFE> fr1, PriorityQueue<HTFE> fr2)
	{
		this.finalResults.clear();
		for (HTFE htfe1 : fr1)
		{
			boolean breakFlag = false;
			for (HTFE htfe : this.finalResults)
				if (htfe1.equals(htfe)) // already exists
				{
					breakFlag = true;
					break;
				}
			if (breakFlag)
				continue;
			for (HTFE htfe2 : fr2)
				if (htfe1.equals(htfe2)) // both own
				{
					this.finalResults.offer(new HTFE(htfe1.sequence, this.alpha * htfe1.eetf + this.beta * htfe2.eetf));
					breakFlag = true;
					break;
				}
			if (breakFlag)
				continue;
			this.finalResults.offer(new HTFE(htfe1.sequence, this.alpha * htfe1.eetf));
		}
		for (HTFE htfe2 : fr2)
		{
			boolean breakFlag = false;
			for (HTFE htfe : this.finalResults)
				if (htfe2.equals(htfe)) // already exists
				{
					breakFlag = true;
					break;
				}
			if (breakFlag)
				continue;
			for (HTFE htfe1 : fr1)
				if (htfe2.equals(htfe1)) // both own
				{
					this.finalResults.offer(new HTFE(htfe2.sequence, this.alpha * htfe1.eetf + this.beta * htfe2.eetf));
					breakFlag = true;
					break;
				}
			if (breakFlag)
				continue;
			this.finalResults.offer(new HTFE(htfe2.sequence, this.beta * htfe2.eetf));
		}
		this.securityFlag = false; // no longer secure until the algorithm is run again
		return;
	}
	public PriorityQueue<HTFE> getFinalResults()
	{
		return this.finalResults;
	}
	
	public void setTreeNodeCount(int treeNodeCount)
	{
		this.treeNodeCount = treeNodeCount;
		this.securityFlag = false; // no longer secure until the algorithm is run again
		return;
	}
	public int getTreeNodeCount()
	{
		return this.treeNodeCount;
	}
	
	public void setOutputFilePath(String outputFilePath)
	{
		this.outputFilePath = outputFilePath;
	}
	
	
	/** Child functions **/
	public static boolean checkDatabase(String database)
	{
		return database.matches("[A-Za-z_][A-Za-z0-9_]*");
	}
	public static String getOriginalDatabaseExpression(String originalDatabase)
	{
		return originalDatabase.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t").replace("\"", "\\\"");
	}
	public static void printDatabaseStatement(String originalDatabase, String revisedDatabase)
	{
		System.out.printf(
			"The passed parameter database should be a string that meets the regex expression \"[A-Za-z_][A-Za-z0-9_]*\". It is defaulted to %s. \nParameter: database = \"%s\" -> database = %s\n\n", 
			revisedDatabase, getOriginalDatabaseExpression(originalDatabase), revisedDatabase
		);
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
		System.out.printf(
			"The passed parameters alpha and beta should be in the interval [0, 1] and meet the requirement that $\\alpha + \\beta = 1$. They are defaulted to %s and %s respectively. \nParameter: (alpha, beta) = (%s, %s) -> (alpha, beta) = (%s, %s)\n\n", 
			"" + revisedAlpha, "" + revisedBeta, "" + originalAlpha, "" + originalBeta, "" + revisedAlpha, "" + revisedBeta
		);
		return;
	}
	public static void printAlphaAndBetaStatement(String originalAlpha, String originalBeta, double revisedAlpha, double revisedBeta)
	{
		System.out.printf(
			"The parameters alpha and beta inputted from the file should be in the interval [0, 1] and meet the requirement that $\\alpha + \\beta = 1$. They are defaulted to %s and %s respectively. \nFile/setAlphaLine: %s -> alpha = %s\nFile/setBetaLine: %s -> beta = %s\n\n", 
			"" + revisedAlpha, "" + revisedBeta, originalAlpha, "" + revisedAlpha, originalBeta, "" + revisedBeta
		);
		return;
	}
	
	public static boolean checkDebugLevel(int debugLevel)
	{
		return DEBUG_LEVEL_CLOSE <= debugLevel && debugLevel <= DEBUG_LEVEL_ALL;
	}
	public static void printDebugLevelStatement(int originalDebugLevel, int revisedDebugLevel)
	{
		System.out.printf("The passed parameter debugLevel should be 0, 1, 2, or 3. It is defaulted to %d. \nParameter: debugLevel = %d -> debugLevel = %d\n\n", revisedDebugLevel, originalDebugLevel, revisedDebugLevel);
		return;
	}
	public static void printDebugLevelStatement(String originalDebugLevel, int revisedDebugLevel)
	{
		System.out.printf("The parameter debugLevel inputted from the file should be 0, 1, 2, or 3. It is revised to %d. \nFile: thisLine = %s -> topK = %d\n\n", revisedDebugLevel, originalDebugLevel, revisedDebugLevel);
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
	
	public static String formatDecimal(double val)
	{
		return defaultDecimalFormatter.format(val);
	}
	public static String formatDecimal(Double val)
	{
		return null == val ? "null" : defaultDecimalFormatter.format(val);
	}
	private boolean checkMemory()
	{
		double currentMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024d / 1024d;
		if (currentMemory > this.maxMemory)
		{
			this.maxMemory = currentMemory;
			return true;
		}
		else
			return false;
	}
	
	
	/** Main TTFE implementation **/
	/* Initial TTFE */
	private boolean initTTFE()
	{
		BufferedReader myInput = null;
		String thisLine = null;
		int topK = this.topK, debugLevel = this.debugLevel;
		double alpha = this.alpha, beta = this.beta;
		String setTopKLine = null, setAlphaLine = null, setBetaLine = null, setDebugLevelLine = null;
		int tid = 0;
		
		try
		{
			myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(this.inputFilePath))));
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
					else if (thisLine.startsWith("delta=")) // read the input delta value from the file
						if (thisLine.substring(6) == "null" || thisLine.substring(6) == "-inf" || thisLine.substring(6) == "-float(\"inf\")" || thisLine.substring(6) == "-float(\'inf\')")
							this.deltaInput = null;
						else
							try
							{
								this.deltaInput = Double.parseDouble(thisLine.substring(6));
							}
							catch (Exception e)
							{
								System.out.printf("The following line has been skipped due to the failure of parsing the input delta value (double). \nFile: %s\n\n", backupThisLine);
							}
					else if (thisLine.length() > 7 && thisLine.startsWith("switches") && '0' <= thisLine.charAt(5) && thisLine.charAt(5) <= '5' && '=' == thisLine.charAt(6) ) // read the ``switches`` value from the file
					{
						try
						{
							this.switches[thisLine.charAt(5) - '0'] = Boolean.parseBoolean(thisLine.substring(7));
						}
						catch (Exception e)
						{
							System.out.printf("The following line has been skipped due to the failure of parsing the switches%c value (boolean). \nFile: %s\n\n", thisLine.charAt(5), backupThisLine);
						}
					}
					else if (thisLine.startsWith("debuglevel=")) // read the debugLevel value from the file
						try
						{
							debugLevel = Integer.parseInt(thisLine.substring(11));
						}
						catch (Exception e)
						{
							System.out.printf("The following line has been skipped due to the failure of parsing the debugLevel value (int). \nFile: %s\n\n", backupThisLine);
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
						transaction.ttf = Double.valueOf(Double.parseDouble(split[3]));
					}
					catch (Exception e)
					{
						transaction.ttf = null; // keep null if it is failed
					}
					
					/* Handle ' ' */
					for (int i = 0; i < item_str.length; ++i) // build transactions
						try
						{
							transaction.put(Integer.valueOf(Integer.parseInt(item_str[i])), new TF(Double.parseDouble(threat_str[i]), Double.parseDouble(frequency_str[i]), this.alpha, this.beta));
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
			System.out.printf("Error parsing the database fom the file \"%s\". Details are as follows. \n", this.inputFilePath);
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
		if ((null == setAlphaLine) ^ (null == setBetaLine)) // automatically fill in alpha and beta if one of them is not specified
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
		if (checkDebugLevel(debugLevel))
			this.debugLevel = debugLevel;
		else
			printDebugLevelStatement(setDebugLevelLine, this.debugLevel);
		if (this.switches[0])
			this.delta = null == this.deltaInput ? null : Double.valueOf(this.deltaInput);
		
		for (Transaction transaction : this.transactions)
		{
			Double ttf = transaction.ttf;
			if (this.debugLevel >= DEBUG_LEVEL_ALL && ttf != transaction.update(this.topK, this.alpha, this.beta)) // check TTF values
				System.out.printf(
					"The value of the input TTF of Transaction %d is not the same as it is computed. It is revised to %s. \nTransaction %s: ttf = %s -> %s\n\n", 
					transaction.tid, transaction.ttf.toString(), transaction.toString(), ttf.toString(), transaction.ttf.toString()
				);
		}
		
		if (this.debugLevel >= DEBUG_LEVEL_PROCEDURE)
		{
			printTitle(algoName + " - initTTFE()");
			if (this.debugLevel >= DEBUG_LEVEL_INFO)
			{
				System.out.println("topK: " + this.topK + "\t\talpha: " + this.alpha + "\t\tbeta: " + this.beta + (this.switches[0] ? "\t\tdelta: " + this.delta + " (#0)" : ""));
				System.out.println("Transactions: ");
				for (Transaction transaction : transactions)
					System.out.println(transaction.getString(true, true, true, false));
				System.out.println();
			}
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
		
		if (this.debugLevel >= DEBUG_LEVEL_PROCEDURE)
		{
			printTitle(algoName + " - computeTWTF()");
			if (this.debugLevel >= DEBUG_LEVEL_INFO)
			{
				System.out.print("TWTF: {");
				Set<Entry<Integer, Double>> set = this.TWTF.entrySet();
				Iterator<Entry<Integer, Double>> iterator = set.iterator();
				while (iterator.hasNext())
				{
					Entry<Integer, Double> entry = iterator.next();
					System.out.print((Integer)entry.getKey() + ":" + (Double)entry.getValue() + (iterator.hasNext() ? ", " : ""));
				}
				System.out.println(this.debugLevel >= DEBUG_LEVEL_ALL ? "}\nStart time: " + defaultDateFormatter.format(this.startTimestamp) + " (" + formatDecimal(this.startTimestamp) + ")\n" : "}\n");
			}
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
		
		if (this.debugLevel >= DEBUG_LEVEL_PROCEDURE)
		{
			printTitle(algoName + " - sortTWTF()");
			if (this.debugLevel >= DEBUG_LEVEL_INFO)
			{
				System.out.print("TWTF (Up): {");
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
		
		if (this.debugLevel >= DEBUG_LEVEL_PROCEDURE)
		{
			printTitle(algoName + " - computeRTF()");
			if (this.debugLevel >= DEBUG_LEVEL_INFO)
			{
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
		
		if (this.debugLevel >= DEBUG_LEVEL_PROCEDURE)
		{
			printTitle(algoName + " - computeETF()");
			if (this.debugLevel >= DEBUG_LEVEL_INFO)
			{
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
		if (this.switches[1] && (entryList.size() <= this.topK ? entryList.size() : this.topK) > 0) // Threshold raising strategy 1
		{
			double tmpDelta = entryList.get((entryList.size() <= this.topK ? entryList.size() : this.topK) - 1).getValue();
			if (null == this.delta || this.delta.doubleValue() < tmpDelta)
				this.delta = Double.valueOf(tmpDelta);
		}
		
		if (this.debugLevel >= DEBUG_LEVEL_PROCEDURE)
		{
			printTitle(algoName + " - sortETF()");
			if (this.debugLevel >= DEBUG_LEVEL_INFO)
			{
				System.out.print("ETF (Down): {");
				Set<Entry<Integer, Double>> set = this.ETF.entrySet();
				Iterator<Entry<Integer, Double>> iterator = set.iterator();
				while (iterator.hasNext())
				{
					Entry<Integer, Double> entry = iterator.next();
					System.out.print((Integer)entry.getKey() + ":" + (Double)entry.getValue() + (iterator.hasNext() ? ", " : "}\n"));
				}
				System.out.println(this.switches[1] ? "delta = " + this.delta + " (#1)\n" : "");
			}
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
			if (null == this.delta || entry.getValue().doubleValue() >= this.delta.doubleValue())
				OTWTF.put(entry.getKey(), entry.getValue());
			else
				for (int i = 0; i < this.transactions.size(); ++i)
					this.transactions.get(i).remove(entry.getKey());
		}
		this.TWTF = OTWTF;
		
		if (this.debugLevel >= DEBUG_LEVEL_PROCEDURE)
		{
			printTitle(algoName + " - pruneItem()");
			if (this.debugLevel >= DEBUG_LEVEL_INFO)
			{
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
		
		if (this.debugLevel >= DEBUG_LEVEL_PROCEDURE)
		{
			printTitle(algoName + " - sortTTFE()");
			if (this.debugLevel >= DEBUG_LEVEL_INFO)
			{
				System.out.println("topK: " + this.topK + "\t\talpha: " + this.alpha + "\t\tbeta: " + this.beta + "\t\tdelta: " + this.delta);
				for (Transaction transaction : this.transactions)
					System.out.println(transaction);
				System.out.println();
			}
		}
		return;
	}
	
	/* Generate Table */
	private void generateTable()
	{
		if (this.TWTF.isEmpty() || null == this.sequence)
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
		
		if (this.debugLevel >= DEBUG_LEVEL_PROCEDURE)
		{
			printTitle(algoName + " - generateTable()");
			if (this.debugLevel >= DEBUG_LEVEL_INFO)
				System.out.println(this.LETF);
		}
		return;
	}
	
	/* Threshold raising strategy 2 based on LETF_E (exact) */
	private void raiseThreshold_LETF_E()
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
			if (null == this.delta || this.delta.doubleValue() < this.letf_e.peek().doubleValue())
				this.delta = Double.valueOf(this.letf_e.peek());
		}
		
		if (this.debugLevel >= DEBUG_LEVEL_PROCEDURE)
		{
			printTitle(algoName + " - raiseThreshold_LETF_E()");
			if (this.debugLevel >= DEBUG_LEVEL_INFO)
			{
				System.out.println("topK = " + this.topK);
				System.out.println("letf_e = " + this.letf_e);
				System.out.println("delta = " + this.delta + " (#2)\n");
			}
		}
		return;
	}
	
	/* Threshold raising strategy 3 based on LETF_LB (fuzzy) */
	private void raiseThreshold_LETF_LB()
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
						if (null == this.delta || this.delta.doubleValue() < tmpValue) // above delta
						{
							this.letf_lb.offer(tmpValue);
							while (this.letf_lb.size() > this.topK) // keep only k elements in the queue
								this.letf_lb.poll();
						}
						else
							break;
					}
				}
		if (!this.letf_lb.isEmpty() && (null == this.delta || this.delta.doubleValue() < this.letf_lb.peek().doubleValue()))
			this.delta = Double.valueOf(this.letf_lb.peek());
		
		if (this.debugLevel >= DEBUG_LEVEL_PROCEDURE)
		{
			printTitle(algoName + " - raiseThreshold_LETF_LB()");
			if (this.debugLevel >= DEBUG_LEVEL_INFO)
			{
				System.out.println("topK = " + this.topK);
				System.out.println("letf_lb = " + this.letf_lb);
				System.out.println("delta = " + this.delta + " (#3)\n");
			}
		}
		return;
	}
	
	/* Generate the tree */
	private void generateTree()
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
			while (!queueIndex.isEmpty() && !queueTreeNode.isEmpty()) // && is for security consideration
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
				if (null == this.delta || currentTreeNode.eetf > this.delta.doubleValue()) // can be added into prior queue
					if (this.switches[5])
						this.delta = currentTreeNode.update(this.finalResults, this.topK, this.delta); // update delta if the prior queue is full (length >= topK)
					else
						currentTreeNode.update(this.finalResults, this.topK, this.delta);
				if (
					!this.switches[4] // do not build tree
					|| (null == this.delta || currentTreeNode.aetf > this.delta.doubleValue()) // can build children
				)
				{
					for (int subEventIndex = subSequenceIndex.get(subSequenceIndex.size() - 1) + 1; subEventIndex < this.sequence.length; ++subEventIndex)		
					{
						ArrayList<Integer> childSequenceIndex = new ArrayList<Integer>(subSequenceIndex);
						childSequenceIndex.add(Integer.valueOf(subEventIndex));
						ArrayList<Integer> childSequence = new ArrayList<Integer>(subSequence);
						childSequence.add(Integer.valueOf(this.sequence[subEventIndex]));
						TreeNode childTreeNode = new TreeNode(childSequence);
						currentTreeNode.children.add(childTreeNode);
						childTreeNode.parent = currentTreeNode;
					}
					int pointer = 0;
					for (int subEventIndex = subSequenceIndex.get(subSequenceIndex.size() - 1) + 1; subEventIndex < this.sequence.length; ++subEventIndex)		
					{
						ArrayList<Integer> childSequenceIndex = new ArrayList<Integer>(subSequenceIndex);
						childSequenceIndex.add(Integer.valueOf(subEventIndex));
						queueIndex.offer(childSequenceIndex);
						queueTreeNode.offer(currentTreeNode.children.get(pointer++));
					}
				}
			}
		}
		
		if (this.debugLevel >= DEBUG_LEVEL_PROCEDURE)
		{
			printTitle(algoName + " - generateTree()");
			if (this.debugLevel >= DEBUG_LEVEL_INFO)
			{
				System.out.println("delta = " + this.delta + (this.switches[4] ? " (#4)" : "") + (this.switches[5] ? " (#5)" : ""));
				System.out.println("Tree: " + this.tree);
				System.out.println("Count of tree nodes: " + this.treeNodeCount + "\n");
			}
		}
		return;
	}
	
	
	/** Output functions **/
	/* Get results */
	public boolean getResults(boolean printEndTime)
	{
		boolean bRet = true;
		if (null != this.outputFilePath && "" != this.outputFilePath)
			try
			{
				File file = new File(
					this.outputFilePath.replace("{algoName}", algoName).replace("{database}", this.database).replace(
						"{topK}", "" + this.topK).replace("{alpha}", "" + this.alpha).replace("{beta}", "" + this.beta
					).replace("{delta}", "" + this.delta).replace("{switches0}", "" + this.switches[0]).replace(
						"{switches1}", "" + this.switches[1]).replace("{switches2}", "" + this.switches[2]
					).replace("{switches3}", "" + this.switches[3]).replace("{switches4}", "" + this.switches[4]).replace(
						"{switches5}", "" + this.switches[5]).replace("{debugLevel}", "" + this.debugLevel
					).replace("{securityFlag}", "" + this.securityFlag)
				);
				FileWriter fw = new FileWriter(file);
				fw.write("Database: " + this.database + "\n");
				fw.write("topK: " + this.topK + "\n");
				fw.write("alpha: " + this.alpha + "\n");
				fw.write("beta: " + this.beta + "\n");
				fw.write("delta (input): " + formatDecimal(this.deltaInput) + "\n");
				fw.write("switches: [" + this.switches[0] + ", " + this.switches[1] + ", " + this.switches[2] + ", " + this.switches[3] + ", " + this.switches[4] + ", " + this.switches[5] + "]\n");
				fw.write("debugLevel: " + this.debugLevel + "\n");
				fw.write("Time: " + formatDecimal((endTimestamp - startTimestamp) / 1000.0) + " s\n");
				fw.write("Space: " + formatDecimal(this.maxMemory) + " MB\n");
				fw.write("delta (output): " + formatDecimal(this.delta) + "\n");
				fw.write("Final minimum value: " + (this.finalResults.isEmpty() ? "null" : formatDecimal(this.finalResults.peek().eetf)) + "\n");
				fw.write("Count of tree nodes: " + this.treeNodeCount + "\n");
				fw.write("Final results: {");
				PriorityQueue<HTFE> pq = new PriorityQueue<HTFE>(this.finalResults);
				while (!pq.isEmpty())
					fw.write(pq.poll() + (pq.isEmpty() ? "" : ", "));
				double dumpTimestamp = System.currentTimeMillis();
				fw.write("}\nDump time: " + defaultDateFormatter.format(dumpTimestamp) + " (" + formatDecimal(dumpTimestamp) + ")");
				fw.close();
			}
			catch (Throwable e)
			{
				bRet = false;
			}
		else
			bRet = false;
		
		if (this.debugLevel >= DEBUG_LEVEL_PROCEDURE)
		{
			printTitle(algoName + " - getResults()");
			if (this.debugLevel >= DEBUG_LEVEL_INFO)
			{
				System.out.print("Final results: {");
				PriorityQueue<HTFE> pq = new PriorityQueue<HTFE>(this.finalResults);
				while (!pq.isEmpty())
					System.out.print(pq.poll() + (pq.isEmpty() ? "" : ", "));
				System.out.println(this.debugLevel >= DEBUG_LEVEL_ALL && printEndTime ? "}\nEnd time: " + defaultDateFormatter.format(this.endTimestamp) + " (" + formatDecimal(this.endTimestamp) + ")\n" : "}\n");
			}
		}
		return bRet;
	}
	public boolean getResults() { return getResults(false); }
	
	public void printStats(String errorInfo)
	{
		System.out.println("====================  " + algoName + " Algorithm - Results  ====================");
		System.out.println("\tDatabase: " + this.database);
		System.out.println("\ttopK: " + this.topK);
		System.out.println("\talpha: " + this.alpha);
		System.out.println("\tbeta: " + this.beta);
		System.out.println("\tdelta (input): " + formatDecimal(this.deltaInput));
		System.out.println("\tswitches: [" + this.switches[0] + ", " + this.switches[1] + ", " + this.switches[2] + ", " + this.switches[3] + ", " + this.switches[4] + ", " + this.switches[5] + "]");
		System.out.println("\tdebugLevel: " + this.debugLevel);
		if (null == errorInfo) // no errors
		{
			System.out.println("\tTime: " + formatDecimal((endTimestamp - startTimestamp) / 1000.0) + " s");
			System.out.println("\tSpace: " + formatDecimal(this.maxMemory) + " MB");
			System.out.println("\tdelta (output): " + formatDecimal(this.delta));
			System.out.println("\tFinal minimum value: " + (this.finalResults.isEmpty() ? "null" : formatDecimal(this.finalResults.peek().eetf)));
			System.out.println("\tCount of tree nodes: " + this.treeNodeCount);
		}
		else
			System.out.println("\tError info: " + errorInfo);
		System.out.println("\tSecurity flag: " + this.securityFlag);
		double printTimestamp = System.currentTimeMillis();
		System.out.println("\tPrint time: " + defaultDateFormatter.format(printTimestamp) + (this.debugLevel >= DEBUG_LEVEL_ALL ? " (" + formatDecimal(printTimestamp) + ")" : ""));
		System.out.println("====================================================================\n");
		return;
	}
	public void printStats() { printStats(null); }
	
	
	/** Running entries **/
	public boolean runAlgorithm(String inputFilePath, String outputFilePath)
	{
		this.inputFilePath = inputFilePath;
		this.outputFilePath = outputFilePath;
		
		if (!(this.initTTFE() && this.checkMemory()))
			return false;
		this.startTimestamp = System.currentTimeMillis();
		this.computeTWTF(); this.checkMemory();
		this.sortTWTF(); this.checkMemory();
		this.computeRTF(); this.checkMemory();
		this.computeETF(); this.checkMemory();
		this.sortETF(); this.checkMemory();
		this.pruneItem(); this.checkMemory();
		this.sortTTFE(); this.checkMemory();
		this.generateTable(); this.checkMemory();
		if (this.switches[2]) { this.raiseThreshold_LETF_E(); this.checkMemory(); }
		if (this.switches[3]) { this.raiseThreshold_LETF_LB(); this.checkMemory(); }
		this.generateTree(); this.checkMemory();
		this.endTimestamp = System.currentTimeMillis();
		this.securityFlag = true;
		return this.getResults();
	}
}