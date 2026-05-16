package thufi;

import java.io.Serializable;


@SuppressWarnings("serial")
public class Item implements Serializable
{
	long twu = 0L;
	int fre = 0;
	int utility = 0;

	public String toString()
	{
		return String.valueOf(utility);
	}
}