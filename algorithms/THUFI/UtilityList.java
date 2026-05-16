package thufi;

import java.util.ArrayList;
import java.util.List;


class UtilityList
{
	int item; // the item
	int sumFre = 0; // the sum of item frequency
	int sumIutils = 0; // the sum of item utilities
	int sumRutils = 0; // the sum of remaining utilities
	
	List<Element> elements = new ArrayList<Element>(); // the elements

	public UtilityList(int item)
	{
		this.item = item;
	}
	
	public int getFre()
	{
		return this.sumFre;
	}
	
	public int getIUtils()
	{
		return this.sumIutils;
	}
	
	public int getRUtils()
	{
		return this.sumRutils;
	}

	public void addElement(Element element)
	{
		sumFre += element.fre;
		sumIutils += element.iutils;
		sumRutils += element.rutils;
		elements.add(element);
	}
}