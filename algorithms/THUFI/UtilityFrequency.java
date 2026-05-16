package thufi;

import java.util.Map;
import java.util.Set;


class UtilityFrequency
{
	Map<Integer, Double> mapLeafMap1 = null;
	Map<Integer, Double> mapLeafMap2 = null;
	public UtilityFrequency(Map<Integer, Double> mapLeafItem1, Map<Integer, Double> mapLeafItem2)
	{
		this.mapLeafMap1 = mapLeafItem1;
		this.mapLeafMap2 = mapLeafItem2;
	}
	public Set<Integer> keySet1()
	{
		return this.mapLeafMap1.keySet();
	}
	public Set<Integer> keySet2()
	{
		return this.mapLeafMap2.keySet();
	}
}