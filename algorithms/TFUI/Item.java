package tfui;

import java.io.Serializable;


@SuppressWarnings("serial")
public class Item implements Serializable
{
	double twu = 0;
	double fre = 0;
	double utility = 0;

	public String toString()
	{
		return String.valueOf(utility);
	}
}