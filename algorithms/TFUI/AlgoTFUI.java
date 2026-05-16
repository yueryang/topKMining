package tfui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;


/**
 * Krishnamoorthy, Srikumar. "Mining top-k high utility itemsets with effective threshold
 * raising strategies." Expert Systems with Applications 117 (2019): 148-165.
 *
 * @author wsgan
 */
public class AlgoTFUI 
{
	/* variable for statistics */
	public double maxMemory = 0; // the maximum memory usage
	public long startTimestamp = 0; // the time the algorithm started
	public long startTimestampPha2 = 0;
	public long endTimestamp = 0; // the time the algorithm terminated
	public int huiCount = 0; // the number of HUI generated
	public int candidateCount = 0;
	
	Map<Integer, Double> mapItemToTWU;
	Map<Integer, Map<Integer, Double>> mapLeafMAP = null;
	double minUtilityAndFrequency=0;
	double minTopKValue = 0;
	int topkstatic = 0;
	double alpha = 0.5;
	double beta = 0.5;
	
	/* writer to write the output file */
	BufferedWriter writer = null;
	
	PriorityQueue<Pattern> kPatterns = new PriorityQueue<Pattern>();
	PriorityQueue<Double> leafPruneUtils = null;
	
	boolean debug = false;
	public boolean bRet = true;
	public long totalContructTime = 0;
	public long totalWhile = 0;
	public int totalItem = 0;
	final int BUFFERS_SIZE = 200;
	private int[] itemsetBuffer = null;
	
	Map<Integer, Map<Integer, Item>> mapFMAP = null;	
	/* private StringBuilder buffer = new StringBuilder(32); */
	
	Map<Integer, UtilityFrequency> utifre = null; // {key, [{utility}, {frequency}]}
	double riuRaiseValue = 0, leafRaiseValue = 0;
	int leafMapSize = 0, leafMapSize2 = 0;
	
	int[] totUtil;
	int[] ej;
	int[] pos;
	int[] twu;
	
	boolean EUCS_PRUNE = false;
	boolean LEAF_PRUNE = true;
	
	class Pair 
	{
		int item = 0;
		double fre = 0;
		double utility = 0;
		
		Pair(int item, double fre, double utility) 
		{
			this.item = item;
			this.fre = fre;
			this.utility = utility;
		}
		
		public String toString() // overloaded function
		{
			return "[" + item + ", " + fre + ", " + utility + "]";
		}
	}
	
	class PairComparator implements Comparator<Pair> 
	{
		@Override
		public int compare(Pair o1, Pair o2) 
		{
			return compareItems(o1.item, o2.item);
		}
	}
	
	class UtilComparator implements Comparator<UtilityList> 
	{
		@Override
		public int compare(UtilityList o1, UtilityList o2) 
		{
			return compareItems(o1.item, o2.item);
		}
	}
	
	public AlgoTFUI() 
	{
		
	}
	
	public AlgoTFUI(int top, double alpha, double beta) 
	{
		this.topkstatic = top;
		this.alpha = alpha;
		this.beta = beta;
	}
	
	String inputFile;
	
	/**
	 * Run the algorithm
	 *
	 * @param input
	 * @param output
	 * @param eucsPrune
	 * @throws IOException
	 */
	public void runAlgorithm(String input, String output, boolean eucsPrune) throws IOException 
	{
		System.out.println("The TFUI Algorithm has started. \n");
		maxMemory = 0;
		itemsetBuffer = new int[BUFFERS_SIZE];
		this.EUCS_PRUNE = eucsPrune;
		Map<Integer, Double> RIU = new HashMap<>();
		Map<Integer, Double> RIF = new HashMap<>();
		
		inputFile = input;
		if (EUCS_PRUNE)
			mapFMAP = new HashMap<>();
		
		if (LEAF_PRUNE) 
		{
			/* UtilityFrequency uf = new UtilityFrequency(new HashMap<Integer, Long>(), new HashMap<Integer, Integer>()); */
			utifre = new HashMap<>();
			mapLeafMAP = new HashMap<Integer, Map<Integer, Double>>();
			/* System.out.println(utifre); */
			leafPruneUtils = new PriorityQueue<>();
		}
		
		startTimestamp = System.currentTimeMillis();
		writer = new BufferedWriter(new FileWriter(output));
		
		mapItemToTWU = new HashMap<>();
		
		BufferedReader myInput = null;
		String thisLine;
		try 
		{
			myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(input))));
			while ((thisLine = myInput.readLine()) != null) 
			{
				if (
						thisLine.isEmpty() == true || thisLine.charAt(0) == '#'
								|| thisLine.charAt(0) == '%' || thisLine.charAt(0) == '@'
								|| (thisLine.length() > 1 && thisLine.charAt(0) == '/' && thisLine.charAt(1) == '/')
				) // annotation symbol
					continue;
				String split[] = thisLine.split(":");
				String items[] = split[0].split(" ");
				String frequencyValues[] = split[1].split(" ");
				
				String utilityValues[] = split[2].split(" ");
				double transactionUtility = Double.parseDouble(split[3]);
				
				for (int i = 0; i < items.length; ++i) 
				{
					Integer item = Integer.parseInt(items[i]);
					Double twu = mapItemToTWU.get(item);
					twu = (null == twu) ? transactionUtility : twu + transactionUtility;
					mapItemToTWU.put(item, twu);
					
					/* set the RIU value */
					double util1 = Double.parseDouble(utilityValues[i]);
					double util2 = Double.parseDouble(frequencyValues[i]);
					Double real1 = RIU.get(item);
					Double real2 = RIF.get(item);
					real1 = (null == real1) ? util1 : util1 + real1;
					real2 = (null == real2) ? util2 : util2 + real2;
					RIU.put(item, real1);
					RIF.put(item, real2);
				}
			}
		}
		 catch (Exception e) 
		{
			e.printStackTrace();
			this.bRet = false;
		} 
		finally 
		{
			if (myInput != null)
				myInput.close();
		}
		
		/* RIU */
		for (Integer item : RIU.keySet()) 
		{
			double util = RIU.get(item);
			double freq = RIF.get(item);
			RIU.put(item, alpha * util + beta * freq);
		}
		
		/* RIU,RIU */
		for (Integer item : RIU.keySet()) 
		{
			System.out.println("item: "+ item);
			System.out.println("real: " + RIU.get(item));
		}
		
		/* Raising threshold by real item utility */
		raisingThresholdRIU(RIU, topkstatic);
		/* System.out.println("raising RIU: " + minTopKValue + "  topk " + topkstatic + "  item " + mapItemToTWU.keySet().size()); */
		riuRaiseValue = minTopKValue;
		System.out.println("minutility: " + riuRaiseValue);
		
		List<UtilityList> listOfUtilityLists = new ArrayList<UtilityList>();
		Map<Integer, UtilityList> mapItemToUtilityList = new HashMap<Integer, UtilityList>();
		
		for (Integer item : mapItemToTWU.keySet())
			if (mapItemToTWU.get(item) >= minTopKValue) 
			{
				UtilityList uList = new UtilityList(item);
				mapItemToUtilityList.put(item, uList);
				listOfUtilityLists.add(uList);
			}
		
		Collections.sort(listOfUtilityLists, new UtilComparator());
		System.out.printf("Series: " + listOfUtilityLists.get(0).getItem());
		for (int i = 1; i < listOfUtilityLists.size(); ++i)
			System.out.printf(" -> " + listOfUtilityLists.get(i).getItem());
		System.out.println("\n");
		
		double remainingUtility = 0;
		double remainingFrequency=0;
		double newTWU = 0;
		
		/* dataset structure: Transaction:Frequance:Utility:TU */
		try 
		{
			myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(input))));
			int tid = 0;
			while ((thisLine = myInput.readLine()) != null) 
			{
				if (
						thisLine.isEmpty() || thisLine.charAt(0) == '#'
								|| thisLine.charAt(0) == '%' || thisLine.charAt(0) == '@'
								|| (thisLine.length() > 1 && thisLine.charAt(0) == '/' && thisLine.charAt(1) == '/')
				)
					continue;
				
				String split[] = thisLine.split(":");
				String items[] = split[0].split(" ");
				String frequencyValues[] = split[1].split(" ");
				String utilityValues[] = split[2].split(" ");
				remainingUtility = 0;
				remainingFrequency=0;
				newTWU = 0; // NEW OPTIMIZATION
				
				List<Pair> revisedTransaction = new ArrayList<Pair>();
				for (int i = 0; i < items.length; ++i) 
				{
					Pair pair = new Pair(Integer.parseInt(items[i]), Double.parseDouble(frequencyValues[i]), Double.parseDouble(utilityValues[i]));
					if (mapItemToTWU.get(pair.item) >= minTopKValue) 
					{
						revisedTransaction.add(pair);
						remainingUtility += pair.utility;
						remainingFrequency += pair.fre;
						newTWU += alpha*pair.utility + beta* pair.fre; // NEW OPTIMIZATION
					}
				}
				
				if (0 == revisedTransaction.size())
					continue;
				Collections.sort(revisedTransaction, new PairComparator());
				
				remainingUtility = 0;
				remainingFrequency=0;
				for (int i = revisedTransaction.size() - 1; i >= 0; --i) 
				{
					Pair pair = revisedTransaction.get(i);
					UtilityList utilityListOfItem = mapItemToUtilityList.get(pair.item);
					Element element = new Element(tid, pair.fre, pair.utility, remainingUtility, remainingFrequency);
					utilityListOfItem.addElement(element);
					
					/* update the minimum utility */
					if (EUCS_PRUNE)
						updateEUCSprune(i, pair, revisedTransaction, newTWU);
					if (LEAF_PRUNE)
						updateLeafprune(i, pair, revisedTransaction, listOfUtilityLists);
					
					remainingUtility += pair.utility;
					remainingFrequency += pair.fre;
				}
				
				++tid; // increase tid number for next transaction
			}
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			this.bRet = false;
		} 
		finally 
		{
			if (myInput != null)
				myInput.close();
		}
		
		if (EUCS_PRUNE) 
		{
			/* call function to raise threshold */
			raisingThresholdCUDOptimize(topkstatic);
			removeEntry();
		}
		RIU.clear();
		
		startTimestampPha2 = System.currentTimeMillis();
		
		if (LEAF_PRUNE) 
		{
			/* call function to raise threshold */
			raisingThresholdLeaf(listOfUtilityLists);
			setLeafMapSize();
			removeLeafEntry();
			leafPruneUtils = null;
		}
		
		leafRaiseValue = minTopKValue;
		mapItemToUtilityList = null;
		
		checkMemory();
		/* call the THUI mining function */
		thui(itemsetBuffer, 0, null, listOfUtilityLists); 
		checkMemory(); 
		
		writeResultTofile();
		writer.close();
		
		endTimestamp = System.currentTimeMillis();
		kPatterns.clear();
	}
	
	
	/**
	 * update EUCS prune
	 *
	 * @param i
	 * @param pair
	 * @param revisedTransaction
	 * @param newTWU
	 */
	public void updateEUCSprune(int i, Pair pair, List<Pair> revisedTransaction, double newTWU) 
	{
		
		Map<Integer, Item> mapFMAPItem = mapFMAP.get(pair.item);
		if (null == mapFMAPItem) 
		{
			mapFMAPItem = new HashMap<Integer, Item>();
			mapFMAP.put(pair.item, mapFMAPItem);
		}
		
		for (int j = i + 1; j < revisedTransaction.size(); ++j) 
		{
			if (pair.item == revisedTransaction.get(j).item)
				continue; // kosarak dataset has duplicate items
			Pair pairAfter = revisedTransaction.get(j);
			Item twuItem = mapFMAPItem.get(pairAfter.item);
			if (null == twuItem)
				twuItem = new Item();
			twuItem.twu += newTWU;
			twuItem.utility += pair.utility + pairAfter.utility;
			
			mapFMAPItem.put(pairAfter.item, twuItem);
		}
	}
	
	/**
	 * update Leaf prune
	 *
	 * @param i
	 * @param pair
	 * @param revisedTransaction
	 * @param ULs
	 */
	public void updateLeafprune(int i, Pair pair, List<Pair> revisedTransaction, List<UtilityList> ULs) 
	{
		double cutil = alpha * pair.utility + beta * pair.fre;
		int followingItemIdx = getTWUindex(pair.item, ULs);
		Map<Integer, Double> mapLeafItem = mapLeafMAP.get(followingItemIdx);
		/* UtilityFrequency tmp_mapLeafItem1 = utifre.get(followingItemIdx); 
		UtilityFrequency tmp_mapLeafItem2 = utifre.get(followingItemIdx);
		Map<Integer, Double> mapLeafItem1 = null;
		Map<Integer, Double> mapLeafItem2 = null;
		// System.out.println(utifre.get(followingItemIdx)); 
		// if (null != utifre.get(followingItemIdx)) 
		//	System.out.println(utifre.get(followingItemIdx).mapLeafMap2); 
		
		if ((null == tmp_mapLeafItem1 || null == tmp_mapLeafItem2)
				|| (null == mapLeafItem1 || null == mapLeafItem2))
		{
			mapLeafItem1 = new HashMap<Integer, Double>();
			mapLeafItem2 = new HashMap<Integer, Double>();
			UtilityFrequency mapLeafItem3 = new UtilityFrequency(mapLeafItem1, mapLeafItem2);
			utifre.put(followingItemIdx, mapLeafItem3);
		}*/
		
		if (null == mapLeafItem) 
		{
			mapLeafItem = new HashMap<Integer, Double>();
			mapLeafMAP.put(followingItemIdx, mapLeafItem);
		}
		
		for (int j = i - 1; j >= 0; --j) 
		{
			if (pair.item == revisedTransaction.get(j).item)
				continue; // kosarak dataset has duplicate items
			Pair pairAfter = revisedTransaction.get(j);
			
			if (ULs.get(--followingItemIdx).item != pairAfter.item)
				break;
			/* Double twuItem1 = null;
			Double twuItem2 = null; */
			/* if (null == mapLeafItem1 || null == twuItem1)
				twuItem1 = new Double(0);
			else
				twuItem1 = mapLeafItem1.get(followingItemIdx);
			if (null == mapLeafItem2 || null == twuItem2)
				twuItem2 = new Double(0);
			else
				twuItem2 = mapLeafItem2.get(followingItemIdx); */
			Double twuItem = mapLeafItem.get(followingItemIdx);
			if (Objects.isNull(twuItem))
				twuItem = new Double(0);
			cutil += alpha*pairAfter.utility + beta*pairAfter.fre;
			twuItem += cutil;
			// twuItem2 += (int)cutil;
			/* mapLeafItem1.put(followingItemIdx, twuItem1);
			mapLeafItem2.put(followingItemIdx, twuItem2); */
			mapLeafItem.put(followingItemIdx, twuItem);
		}
	}
	
	/**
	 * get TWU-index
	 *
	 * @param item
	 * @param ULs
	 * @return
	 */
	public int getTWUindex(int item, List<UtilityList> ULs) 
	{
		for (int i = ULs.size() - 1; i >= 0; --i)
			if (ULs.get(i).item == item)
				return i;
		return -1;
	}
	
	/**
	 * Set leaf map-size
	 */
	public void setLeafMapSize() 
	{
		/* for (Entry<Integer, UtilityFrequency> entry : utifre.entrySet()) 
		{
			leafMapSize1 += entry.getValue().keySet1().size();
			leafMapSize1 += entry.getValue().keySet2().size();
		} */
		for (Entry<Integer, Map<Integer, Double>> entry : mapLeafMAP.entrySet()) 
		{
			leafMapSize += entry.getValue().keySet().size();
		}
	}
	
	/**
	 * Compare items
	 *
	 * @param item1
	 * @param item2
	 * @return
	 */
	private int compareItems(int item1, int item2) 
	{
		int compare = (int) (mapItemToTWU.get(item1) - mapItemToTWU.get(item2));
		return (compare == 0) ? item1 - item2 : compare;
	}
	
	/**
	 * write result to file unord
	 *
	 * @throws IOException
	 */
	public void writeResultTofileUnord() throws IOException 
	{
		Iterator<Pattern> iter = kPatterns.iterator();
		while (iter.hasNext()) 
		{
			huiCount++; // increase the number of high utility itemsets found
			
			Pattern pattern = (Pattern) iter.next();
			StringBuilder buffer = new StringBuilder();
			buffer.append(pattern.prefix.toString());
			/* write separator */
			buffer.append(" #UTIL: ");
			/* write support */
			buffer.append(pattern.utility);
			writer.write(buffer.toString());
			writer.newLine();
		}
		
		writer.close();
	}
	
	/**
	 * The THUI algorithm
	 *
	 * @param prefix
	 * @param prefixLength
	 * @param pUL
	 * @param ULs
	 * @throws IOException
	 */
	private void thui(int[] prefix, int prefixLength, UtilityList pUL, List<UtilityList> ULs) throws IOException 
	{
		for (int i = ULs.size() - 1; i >= 0; --i)
			if ((alpha*ULs.get(i).getIUtils() + beta*ULs.get(i).getFre()) >= minTopKValue)
				save(prefix, prefixLength, ULs.get(i));
		
		for (int i = ULs.size() - 2; i >= 0; --i) 
		{
			/* last item is a single item, and hence no extension */
			checkMemory();
			UtilityList X = ULs.get(i);
						
			if (alpha*(X.sumIutils + X.sumRutils) + beta*(X.sumFre + X.sumRFre)>= minTopKValue && alpha*X.sumIutils + beta*X.sumFre > 0) 
			{
				/* the utility value of zero cases can be safely ignored, as it is unlikely to generate a HUI; */
				/* besides the lowest min utility will be 1 */
				if (EUCS_PRUNE) 
				{
					Map<Integer, Item> mapTWUF = mapFMAP.get(X.item);
					if (null == mapTWUF)
						continue;
				}
				
				List<UtilityList> exULs = new ArrayList<UtilityList>();
				for (int j = i + 1; j < ULs.size(); ++j) 
				{
					UtilityList Y = ULs.get(j);
					++candidateCount;
					
					/* call the function */
					UtilityList exul = construct(pUL, X, Y);
					if (exul != null)
						exULs.add(exul);
				}
				
				prefix[prefixLength] = X.item;
				
				/* call the THUI function */
				thui(prefix, prefixLength + 1, X, exULs);
			}
		}
	}
	
	/**
	 * get prefix string
	 *
	 * @param prefix
	 * @param length
	 * @return
	 */
	public String getPrefixString(int[] prefix, int length) 
	{
		String buffer = "";
		for (int i = 0; i < length; ++i) 
		{
			buffer += prefix[i];
			buffer += " ";
		}
		return buffer;
	}
	
	/**
	 * construct
	 *
	 * @param P
	 * @param px
	 * @param py
	 * @return
	 */
	private UtilityList construct(UtilityList P, UtilityList px, UtilityList py) 
	{
		UtilityList pxyUL = new UtilityList(py.item);
		double totUtil = alpha * (px.sumIutils + px.sumRutils) + beta * (px.sumFre + px.sumRFre);
		int ei = 0, ej = 0, Pi = -1;
		
		Element ex = null, ey = null, e = null;
		while (ei < px.elements.size() && ej < py.elements.size()) 
		{
			if (px.elements.get(ei).tid > py.elements.get(ej).tid) 
			{
				++ej;
				continue;
			}
			
			/* px not present, py pres */
			if (px.elements.get(ei).tid < py.elements.get(ej).tid) 
			{
				/* px present, py not present */
				totUtil = totUtil - alpha * (px.elements.get(ei).iutils + px.elements.get(ei).rutils)
						- beta * (px.elements.get(ei).fre + px.elements.get(ei).rfrequency);
				if (totUtil < minTopKValue) 
				{
					return null;
				}
				++ei;
				++Pi;
				/* if a parent is present, it should be as large or larger than px; besides the ordering is by tid */
				continue;
			}
			
			ex = px.elements.get(ei);
			ey = py.elements.get(ej);
			
			if (null == P)
				pxyUL.addElement(new Element(ex.tid, ex.fre + ey.fre, ex.iutils + ey.iutils, ey.rutils, ey.rfrequency));
			else 
			{
				while (Pi < P.elements.size() && P.elements.get(++Pi).tid < ex.tid) ;
				e = P.elements.get(Pi);
				pxyUL.addElement(new Element(ex.tid, ex.fre + ey.fre - e.fre, ex.iutils + ey.iutils - e.iutils, ey.rutils, ey.rfrequency));
			}
			
			++ei;
			++ej;
		}
		
		while (ei < px.elements.size()) 
		{
			totUtil = totUtil - alpha * (px.elements.get(ei).iutils + px.elements.get(ei).rutils) - beta * (px.elements.get(ei).fre + px.elements.get(ei).rfrequency);
			if (totUtil < minTopKValue)
				return null;
			++ei;
		}
		
		return pxyUL;
	}
	
	
	/**
	 * Write result to file
	 *
	 * @throws IOException
	 */
	public void writeResultTofile() throws IOException 
	{
		if (kPatterns.size() == 0)
			return;
		List<Pattern> lp = new ArrayList<Pattern>();
		do 
		{
			++huiCount;
			Pattern pattern = kPatterns.poll();
			lp.add(pattern);
		} while (kPatterns.size() > 0);
		
		Collections.sort(lp, new Comparator<Pattern>() 
		{
			public int compare(Pattern o1, Pattern o2) 
			{
				return comparePatterns(o1, o2);
				/* return comparePatternsIdx(o1, o2); */
			}
		});
		
		for (Pattern pattern : lp) 
		{
			StringBuilder buffer = new StringBuilder();
			
			buffer.append(pattern.prefix.toString());
			buffer.append(" #UTIL_FRE: ");
			
			/* write support */
			buffer.append(alpha * pattern.utility + beta* pattern.fre);
			
			writer.write(buffer.toString());
			writer.newLine();
		}
		
		writer.close();
	}
	
	
	/**
	 * compare patterns
	 *
	 * @param item1
	 * @param item2
	 * @return
	 */
	private int comparePatterns(Pattern item1, Pattern item2) 
	{
		/* int compare = (int) (Integer.parseInt(item1.split(" ")[0]) - Integer.parseInt(item2.split(" ")[0])); */
		int i1 = (int) Integer.parseInt(item1.prefix.split(" ")[0]);
		int i2 = (int) Integer.parseInt(item2.prefix.split(" ")[0]);
		
		int compare = (int) (mapItemToTWU.get(i1) - mapItemToTWU.get(i2));
		return compare;
	}
	
//	private int comparePatternsIdx(Pattern item1, Pattern item2) 
//	{
//		int compare = item1.idx - item2.idx;
//		return compare;
//	}
//	
//	private double getObjectSize(Object object) throws IOException 
//	{
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		ObjectOutputStream oos = new ObjectOutputStream(baos);
//		oos.writeObject(object);
//		oos.close();
//		double maxMemory = baos.size() / 1024d / 1024d;
//		return maxMemory;
//	} 
	
	public int getMax(Map<Integer, Integer> map) 
	{
		int r = 0;
		for (Integer value : map.values())
			if (value >= minTopKValue)
				++r;
		return r;
	}
	
	/**
	 * raising threshold RIU
	 *
	 * @param rIU
	 * @param k
	 */
	public void raisingThresholdRIU(Map<Integer, Double> rIU, int k) 
	{
		List<Entry<Integer, Double>> list = new LinkedList<Entry<Integer, Double>>(rIU.entrySet());
		
		Collections.sort(list, new Comparator<Entry<Integer, Double>>() 
		{
			public int compare(Entry<Integer, Double> value1, Entry<Integer, Double> value2) 
			{
				return (value2.getValue()).compareTo(value1.getValue());
			}
		});
		
		/* raising threshold minTopKValue */
		if ((list.size() >= k) && (k > 0))
			minTopKValue = list.get(k - 1).getValue();
		
		list = null;
	}
	
	
	/**
	 * raising threshold CUD optimize
	 *
	 * @param k
	 */
	public void raisingThresholdCUDOptimize(int k) 
	{
		PriorityQueue<Double> ktopls = new PriorityQueue<Double>();
		double value1 = 0, value2 = 0;
		for (Entry<Integer, Map<Integer, Item>> entry : mapFMAP.entrySet())
			for (Entry<Integer, Item> entry2 : entry.getValue().entrySet()) 
			{
				value1 = entry2.getValue().utility;
				value2 = entry2.getValue().fre;
				
				if (value1 * alpha + value2 * beta >= minTopKValue) 
				{
					if (ktopls.size() < k)
						ktopls.add(value1);
					else if (value1 > ktopls.peek()) 
					{
						ktopls.add(value1);
						do 
						{
							ktopls.poll();
						} while (ktopls.size() > k);
					}
				}
			}
		
		/* raising threshold minTopKValue */
		if ((ktopls.size() > k - 1) && (ktopls.peek() > minTopKValue))
			minTopKValue = ktopls.peek();
		
		ktopls.clear();
	}
	
	/**
	 * Add to leaf prune utility-list
	 *
	 * @param value_utility
	 */
	public void addToLeafPruneUtils(Double value_utility) 
	{
		if (leafPruneUtils.size() < topkstatic)
			leafPruneUtils.add(value_utility);
		else if (value_utility > leafPruneUtils.peek()) 
		{
			leafPruneUtils.add(value_utility);
			do 
			{
				leafPruneUtils.poll();
			} while (leafPruneUtils.size() > topkstatic);
		}
	}
	
	
	/**
	 * Raising threshold leaf
	 *
	 * @param ULs
	 */
	public void raisingThresholdLeaf(List<UtilityList> ULs) 
	{
		double value_utilityAndFreency = 0;
		/* LIU-Exact strategy */
		for (Entry<Integer, Map<Integer, Double>> entry : mapLeafMAP.entrySet()) 
		{
			for (Entry<Integer, Double> entry2 : entry.getValue().entrySet()) 
			{
				 value_utilityAndFreency = entry2.getValue();
				 if(value_utilityAndFreency >= minTopKValue)
				{
					 addToLeafPruneUtils(value_utilityAndFreency);
				 }
			}
		}
		/* for (Entry<Integer, UtilityFrequency> entry : utifre.entrySet()) 
		{
			UtilityFrequency tmp_utifre = entry.getValue();
			Set<Integer> set1 = tmp_utifre.mapLeafMap1.keySet();
			Set<Integer> set2 = tmp_utifre.mapLeafMap2.keySet();
			Set<Integer> set3 = tmp_utifre.mapLeafMap1.keySet();
			set3.retainAll(set2);
			set1.removeAll(set3);
			set2.removeAll(set3);
			
			Iterator<Integer> iter1 = set1.iterator();
			Iterator<Integer> iter2 = set2.iterator();
			Iterator<Integer> iter3 = set3.iterator();
			while (iter1.hasNext()) 
			{
				Integer it = iter1.next();
				value_utility = tmp_utifre.mapLeafMap1.get(it);
				value_utifre = value_utility;
				if (value_utifre >= minTopKValue)
					addToLeafPruneUtils(value_utility);
			}
			while (iter2.hasNext()) 
			{
				Integer it = iter2.next();
				value_frequency = tmp_utifre.mapLeafMap2.get(it);
				value_utifre = value_frequency;
				if (value_utifre >= minTopKValue)
					addToLeafPruneUtils(value_utility);
			}
			while (iter3.hasNext()) 
			{
				Integer it = iter3.next();
				value_utility = tmp_utifre.mapLeafMap1.get(it);
				value_frequency = tmp_utifre.mapLeafMap2.get(it);
				value_utifre = value_utility * alpha + value_frequency * beta;
				if (value_utifre >= minTopKValue)
					addToLeafPruneUtils(value_utility);
			}
		} */
		
		/* LIU-LB strategy */
		for (Entry<Integer, Map<Integer, Double>> entry : mapLeafMAP.entrySet()) 
		{
			for (Entry<Integer, Double> entry2 : entry.getValue().entrySet()) 
			{
				value_utilityAndFreency = entry2.getValue();
				if(value_utilityAndFreency >= minTopKValue)
				{
					/* master contains the end reference 85 (leaf) */
					int end = entry.getKey() + 1;
					
					/* local map contains the start reference 76-85 (76 as parent) */
					int st = entry2.getKey();
					
					/* all entries between st and end processed, there will be go gaps */
					/* in-between (only leaf with consecutive entries inserted in mapLeafMAP) */
					double value2_utilityAndFrequency = 0;
					for(int i = st+1; i<end-1;i++)
					{
						value2_utilityAndFrequency = value_utilityAndFreency-alpha*ULs.get(i).getIUtils() - beta*ULs.get(i).getFre();
						if(value2_utilityAndFrequency >= minTopKValue)
						{
							addToLeafPruneUtils(value2_utilityAndFrequency);
						}
						for(int j = i + 1; j < end-1; j++)
						{
							value2_utilityAndFrequency = value_utilityAndFreency - alpha*(ULs.get(i).getIUtils()+ULs.get(j).getIUtils())
									- beta*(ULs.get(i).getFre()+ULs.get(i).getFre());
							if(value2_utilityAndFrequency >= minTopKValue)
							{
								addToLeafPruneUtils(value2_utilityAndFrequency);
							}
							for (int k = j+1; k + 1 < end -1; k++)
							{
								value2_utilityAndFrequency = value_utilityAndFreency - alpha*(ULs.get(i).getIUtils() + ULs.get(j).getIUtils() + ULs.get(k).getIUtils())
										- beta*(ULs.get(i).getFre()+ULs.get(i).getFre() + ULs.get(k).getFre());
								if(value2_utilityAndFrequency >= minTopKValue)
								{
									addToLeafPruneUtils(value2_utilityAndFrequency);
								}
							}
						}
					}
				}
			}
		}
		/* for (Entry<Integer, UtilityFrequency> entry : utifre.entrySet()) 
		{
			UtilityFrequency tmp_utifre = entry.getValue();
			Set<Integer> set1 = tmp_utifre.mapLeafMap1.keySet();
			Set<Integer> set2 = tmp_utifre.mapLeafMap2.keySet();
			Set<Integer> set3 = tmp_utifre.mapLeafMap1.keySet();
			set3.retainAll(set2);
			set1.removeAll(set3);
			set2.removeAll(set3);
			
			Iterator<Integer> iter1 = set1.iterator();
			Iterator<Integer> iter2 = set2.iterator();
			Iterator<Integer> iter3 = set3.iterator();
			while (iter1.hasNext()) 
			{
				Integer it = iter1.next();
				value_utility = tmp_utifre.mapLeafMap1.get(it);
				value_utifre = value_utility;
				
				if (value_utifre >= minTopKValue) 
				{
					// master contains the end reference 85 (leaf) 
					int end = entry.getKey() + 1;
					
					// local map contains the start reference 76-85 (76 as parent) 
					int st = it.intValue();
					
					// all entries between st and end processed, there will be go gaps 
					// in-between (only leaf with consecutive entries inserted in mapLeafMAP) 
					double value2 = 0;
					
					for (int i = st + 1; i < end - 1; ++i) 
					{
						// exclude the first and last e.g. 12345 -> 1345,1245,1235 estimates 
						value2 = value_utifre - ULs.get(i).getIUtils();
						if (value2 >= minTopKValue)
							addToLeafPruneUtils(value2);
						
						for (int j = i + 1; j < end - 1; ++j) 
						{
							value2 = value_utifre - ULs.get(i).getIUtils() - ULs.get(j).getIUtils();
							if (value2 >= minTopKValue)
								addToLeafPruneUtils(value2);
							
							for (int k = j + 1; k + 1 < end - 1; ++k) 
							{
								value2 = value_utifre - ULs.get(i).getIUtils() - ULs.get(j).getIUtils() - ULs.get(k).getIUtils();
								if (value2 >= minTopKValue)
									addToLeafPruneUtils(value2);
							}
						}
					}
				}
			}
			while (iter2.hasNext()) 
			{
				Integer it = iter2.next();
				value_frequency = tmp_utifre.mapLeafMap2.get(it);
				value_utifre = value_frequency;
				
				if (value_utifre >= minTopKValue) 
				{
					// master contains the end reference 85 (leaf) 
					int end = entry.getKey() + 1;
					
					// local map contains the start reference 76-85 (76 as parent) 
					int st = it.intValue();
					
					// all entries between st and end processed, there will be go gaps 
					// in-between (only leaf with consecutive entries inserted in mapLeafMAP) 
					double value2 = 0;
					
					for (int i = st + 1; i < end - 1; ++i) 
					{
						// exclude the first and last e.g. 12345 -> 1345,1245,1235 estimates 
						value2 = value_utifre - ULs.get(i).getIUtils();
						if (value2 >= minTopKValue)
							addToLeafPruneUtils(value2);
						
						for (int j = i + 1; j < end - 1; ++j) 
						{
							value2 = value_utifre - ULs.get(i).getIUtils() - ULs.get(j).getIUtils();
							if (value2 >= minTopKValue)
								addToLeafPruneUtils(value2);
							
							for (int k = j + 1; k + 1 < end - 1; ++k) 
							{
								value2 = value_utifre - ULs.get(i).getIUtils() - ULs.get(j).getIUtils() - ULs.get(k).getIUtils();
								if (value2 >= minTopKValue)
									addToLeafPruneUtils(value2);
							}
						}
					}
				}
			}
			while (iter3.hasNext()) 
			{
				Integer it = iter3.next();
				value_utility = tmp_utifre.mapLeafMap1.get(it);
				value_frequency = tmp_utifre.mapLeafMap2.get(it);
				value_utifre = value_utility * alpha + value_frequency * beta;
				
				if (value_utifre >= minTopKValue) 
				{
					// master contains the end reference 85 (leaf) 
					int end = entry.getKey() + 1;
					
					// local map contains the start reference 76-85 (76 as parent) 
					int st = it.intValue();
					
					// all entries between st and end processed, there will be go gaps 
					// in-between (only leaf with consecutive entries inserted in mapLeafMAP) 
					double value2 = 0;
					
					for (int i = st + 1; i < end - 1; ++i) 
					{
						// exclude the first and last e.g. 12345 -> 1345,1245,1235 estimates 
						value2 = value_utifre - ULs.get(i).getIUtils();
						if (value2 >= minTopKValue)
							addToLeafPruneUtils(value2);
						
						for (int j = i + 1; j < end - 1; ++j) 
						{
							value2 = value_utifre - ULs.get(i).getIUtils() - ULs.get(j).getIUtils();
							if (value2 >= minTopKValue)
								addToLeafPruneUtils(value2);
							
							for (int k = j + 1; k + 1 < end - 1; ++k) 
							{
								value2 = value_utifre - ULs.get(i).getIUtils() - ULs.get(j).getIUtils() - ULs.get(k).getIUtils();
								if (value2 >= minTopKValue)
									addToLeafPruneUtils(value2);
							}
						}
					}
				}
			}
		} */
		for (UtilityList u : ULs) 
		{
			/* add all 1 items */
			value_utilityAndFreency = alpha*u.getIUtils() + beta*u.getFre();
			if (value_utilityAndFreency >= minTopKValue)
				addToLeafPruneUtils(value_utilityAndFreency);
		}
		
		/* raising threshold minTopKValue */
		if ((leafPruneUtils.size() > topkstatic - 1) && (leafPruneUtils.peek() > minTopKValue))
			minTopKValue = leafPruneUtils.peek();
	}
	
	/**
	 * Remove entry
	 */
	private void removeEntry() 
	{
		for (Entry<Integer, Map<Integer, Item>> entry : mapFMAP.entrySet())
			for (Iterator<Entry<Integer, Item>> it = entry.getValue().entrySet().iterator(); it.hasNext(); ) 
			{
				Entry<Integer, Item> entry2 = it.next();
				if (entry2.getValue().twu < minTopKValue)
					it.remove();
			}
	}
	
	/**
	 * Remove leaf entry
	 */
	private void removeLeafEntry() 
	{
		for (Entry<Integer, UtilityFrequency> entry : utifre.entrySet()) 
		{
			for (Iterator<Entry<Integer, Double>> it = entry.getValue().mapLeafMap1.entrySet().iterator(); it.hasNext(); ) 
			{
				/* Map.Entry<Integer, Long> entry2 = */
				it.next(); // Optimize
				it.remove();
			}
			for (Iterator<Entry<Integer, Double>> it = entry.getValue().mapLeafMap2.entrySet().iterator(); it.hasNext(); ) 
			{
				/* Map.Entry<Integer, Long> entry2 = */
				it.next(); // Optimize
				it.remove();
			}
		}
	}
	
	/**
	 * Save function
	 *
	 * @param prefix
	 * @param length
	 * @param X
	 */
	private void save(int[] prefix, int length, UtilityList X) 
	{
		kPatterns.add(new Pattern(prefix, length, X, candidateCount));
		if (kPatterns.size() > topkstatic) 
		{
			if (X.getIUtils() >= minTopKValue) 
			{
				do 
				{
					kPatterns.poll();
				} while (kPatterns.size() > topkstatic);
			}
			
			minTopKValue = kPatterns.peek().utility;
		}
	}
	
	/**
	 * Check memory
	 */
	private void checkMemory() 
	{
		double currentMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024d / 1024d;
		if (currentMemory > maxMemory)
			maxMemory = currentMemory;
	}
	
	public void printStats() throws IOException 
	{
		java.text.DecimalFormat df = new java.text.DecimalFormat("#.00");
		System.out.println("============  TFUI ALGORITHM - STATS  ============");
		System.out.println("\talpha: " + this.alpha);
		System.out.println("\tbeta: " + this.beta);
		System.out.println("\tTotal time: " + (endTimestamp - startTimestamp) / 1000.0 + " s");
		System.out.println("\tMax memory: " + df.format(maxMemory) + " MB");
		System.out.println("\tHUIs count: " + huiCount);
		System.out.println("\tCandidates: " + candidateCount);
		System.out.println("\tFinal minimum utility: " + minTopKValue);
		File f = new File(inputFile);
		String tmp = f.getName();
		tmp = tmp.substring(0, tmp.lastIndexOf('.'));
		System.out.println("\tDataset: " + tmp);
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
		System.out.println("\tEnd time " + timeStamp);
		System.out.println("==================================================");
	}
}