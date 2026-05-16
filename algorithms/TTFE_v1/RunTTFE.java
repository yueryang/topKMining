package ttfe;

import java.io.IOException;


/**
 * TTFE (Powered by Universe)
 * @author Yuer Yang, Department of Computer Science, The University of Hong Kong
 * TTF<transaction -> ttf>: for each transaction -> ttf = \alpha * sum(t) + \beta * sum(f)
 * TWTF<event -> twtf>: for each event -> twtf = sum([ttf if event in transaction])
 * TWTF: for each event -> sorted(TWTF, reverse = False)
 * ETF<event -> etf>: for each event -> etf = sum(each TF of events)
 * ETF: for each event -> sorted(ETF, reverse = True) -> delta
 * TWTF: cut off the nodes in TWTF whose value is less than delta
 * TTFE: for each transaction -> sorted(transaction) according to TWTF
 * LETF: adding continuous tf, get 2D table sorted by TWTF (UP)
 * delta: raise delta to topK value of LETF
 * Tree: for each transaction -> build tree
 * Tree: for each transaction -> pruning tree
 * Results: topK
 */
public class RunTTFE
{
	public static void main(String[] args) throws IOException
	{
		String inputFile = "./ttfe.txt";
		String outputFile = "./output.txt"; // "./output_{topK}_{alpha}_{beta}.txt"
		int topK = 5;
		double alpha = 0.5, beta = 0.5;
		boolean raise1 = true, raise2 = true, isPrint = false;
		
		AlgoTTFE ttfe = new AlgoTTFE(topK, alpha, beta, raise1, raise2, isPrint);
		ttfe.runAlgorithm(inputFile, outputFile);
		ttfe.printStats();
		return;
	}
}