package thui;

import java.io.IOException;


public class RunTHUI
{
	public static void main(String[] args) throws IOException
	{
		String input = "./thui.txt";
		String output = "./output.txt";
		boolean eucsPrune = false;
		int topK = 20;
		
		AlgoTHUI topk = new AlgoTHUI();
		topk.runAlgorithm(input, output, eucsPrune, topK);
		topk.printStats();
		return;
	}
}