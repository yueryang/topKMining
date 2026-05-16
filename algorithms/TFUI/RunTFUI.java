package tfui;

import java.io.IOException;


public class RunTFUI
{
	public static void main(String[] args) throws IOException
	{
		String input = "./tfui_1.txt";
		String output = "./output.txt";
		int topN = 10;
		double alpha = 0.5;
		double beta = 0.5;
		boolean eucsPrune = false;
		
		AlgoTFUI topk = new AlgoTFUI(topN, alpha, beta);
		topk.runAlgorithm(input, output, eucsPrune);
		topk.printStats();
	}
}