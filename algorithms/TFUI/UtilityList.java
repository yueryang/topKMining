package tfui;

import java.util.ArrayList;
import java.util.List;


class UtilityList
{
	int item; // the item
	double sumFre = 0; // the sum of item frequency
	double sumIutils = 0; // the sum of item utilities
	double sumRutils = 0; // the sum of remaining utilities
	double sumRFre = 0; // the sum of remaining frequency

	List<Element> elements = new ArrayList<Element>(); // the elements

	public UtilityList(int item)
	{
		this.item = item;
	}

	public int getItem() {
		return item;
	}

	public double getFre()
	{
		return this.sumFre;
	}
	
	public double getIUtils()
	{
		return this.sumIutils;
	}
	
	public double getRUtils()
	{
		return this.sumRutils;
	}

	public double getSumRFre() {
		return sumRFre;
	}

	public void addElement(Element element)
	{
		sumFre += element.fre;
		sumIutils += element.iutils;
		sumRutils += element.rutils;
		sumRFre += element.rfrequency;
		elements.add(element);
	}
}