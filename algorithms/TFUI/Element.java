package tfui;


class Element
{
	final int tid;
	final double fre;
	final double iutils;
	final double rutils;
	final double rfrequency;

	public Element(int tid, double fre, double iutils, double rutils,double rfrequency)
	{
		this.tid = tid;
		this.fre = fre;
		this.iutils = iutils;
		this.rutils = rutils;
		this.rfrequency = rfrequency;
	}

	public void print(int depth)
	{
		for (int i = 0; i < depth; ++i)
			System.out.print("\t");
		System.out.println("\t" + tid + " fre: " + fre + " iutils: " + iutils + " rutils: " + rutils);
	}
}