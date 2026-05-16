package thufi;

import java.io.IOException;


public class RunTHUI
{
	public static void main(String[] args) throws IOException
	{
		String input = "./thufi.txt";
		String output = "./output.txt";
		int topN = 20;
		double alpha = 0.5;
		double beta = 0.5;
		boolean eucsPrune = false;
		
		AlgoTHUFI topk = new AlgoTHUFI(topN, alpha, beta);
		topk.runAlgorithm(input, output, eucsPrune);
		topk.printStats();
	}
}