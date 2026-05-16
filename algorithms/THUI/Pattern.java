package thui;


public class Pattern implements Comparable<Pattern>
{
	String prefix; // prefix
	double utility; // utility value
	int sup; // support value
	int idx; // for sorting patterns in order of insertion
	
	/**
	 * Pattern
	 * 
	 * @param prefix
	 * @param length
	 * @param X
	 * @param idx
	 */
	public Pattern(int[] prefix, int length, UtilityList X, int idx)
	{
		String buffer = "";
		for (int i = 0; i < length; ++i)
		{
			buffer += prefix[i];
			buffer += " ";
		}
		buffer += "" + X.item;
		this.prefix = buffer;
		this.idx = idx;
		this.utility = X.getUtils();
		this.sup = X.elements.size();
	}

	public String getPrefix()
	{
		return this.prefix;
	}

	public int compareTo(Pattern o)
	{
		if (o == this) // prevent one equation note missed
			return 0;
		double compare = this.utility - o.utility;
		if (compare != 0)
			return (int)compare;
		return this.hashCode() - o.hashCode();
	}
}