package thufi;


class Element
{
	final int tid;
	final int fre;
	final int iutils;
	final int rutils;

	public Element(int tid, int fre, int iutils, int rutils)
	{
		this.tid = tid;
		this.fre = fre;
		this.iutils = iutils;
		this.rutils = rutils;
	}

	public void print(int depth)
	{
		for (int i = 0; i < depth; ++i)
			System.out.print("\t");
		System.out.println("\t" + tid + " fre: " + fre + " iutils: " + iutils + " rutils: " + rutils);
	}
}
