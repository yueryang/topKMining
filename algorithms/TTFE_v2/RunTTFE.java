package ttfe;

import java.io.File;


/**
 * TTFE (Powered by Universe)
 * @author Yuer Yang, Department of Computer Science, The University of Hong Kong
 * TTF<transaction -> ttf>: for each transaction -> ttf = \alpha * sum(t) + \beta * sum(f)
 * TWTF<event -> twtf>: for each event -> twtf = sum([ttf if event in transaction])
 * TWTF: for each event -> sorted(TWTF, reverse = False)
 * ETF<event -> etf>: for each event -> etf = sum(each TF of events)
 * ETF: for each event -> sorted(ETF, reverse = True) -> delta
 * TTF: for each transaction for each event -> rtf(x) = sum([tf(x) for x in "all events after x in sequence"]) 
 * TWTF: cut off the nodes in TWTF whose value is less than delta
 * TTFE: for each transaction -> sorted(transaction) according to TWTF
 * LETF: adding continuous tf, get 2D table sorted by TWTF (UP)
 * delta: raise delta to topK value of LETF
 * Tree: build tree by queue and generate prior queue for getting final results
 * Results: topK
 */
public class RunTTFE
{
	public static final int EXIT_SUCCESS = 0, EXIT_FAILURE = 1, EOF = -1;
	public static void main(String[] args)
	{
		boolean runSample = true, runExperiments = true, bRet = true;
		
		if (runSample)
		{
			String inputFilePath = "./ttfe.txt", outputFilePath = "./output.txt";
			int topK = 5;
			double alpha = 0.5, beta = 0.5;
			boolean raise1 = true, raise2 = true, isPrint = true;
			
			AlgoTTFE ttfe = new AlgoTTFE("ttfe", topK, alpha, beta, raise1, raise2, isPrint);
			try
			{
				ttfe.runAlgorithm(inputFilePath, outputFilePath);
				ttfe.printStats(null);
			}
			catch (Throwable e)
			{
				final String errorInfo = e.getMessage();
				ttfe.printStats(null == errorInfo ? "Unknown errors" : errorInfo);
				bRet = false;
			}
			finally
			{
				ttfe = null; // release citation for GC cleaning
			}
			
			System.out.println();
		}
		
		if (runExperiments)
		{
			String inputFolderPath = "./input/", outputFolderPath = "./output/", inputFileExt = ".txt", outputFileExt = ".txt";
			String[] databaseList = { "chess", "mushroom" }; // { "accidents", "chess", "kosorak", "mushroom" };
			int[] topKList = { 5, 10, 50, 100, 500, 1000 };
			double[] betaList = { 0, 0.25, 0.5, 0.75, 1 };
			boolean raise1 = true, raise2 = true, isPrint = false;
			int succeedCount = 0, runCount = 0, expectedCount = databaseList.length * topKList.length * betaList.length;
			
			File outputFolderPointer = new File(outputFolderPath);
			if (!outputFolderPointer.isDirectory())
				outputFolderPointer.mkdir();
			
			if (outputFolderPointer.isDirectory())
			{
				for (String database : databaseList)
				{
					final String inputFilePath = inputFolderPath + database + inputFileExt, outputFilePath = outputFolderPath + "{database}_{topK}_{alpha}_{beta}" + outputFileExt;
					File inputFilePointer = new File(inputFilePath);
					if (inputFilePointer.isFile())
						for (int topK : topKList)
							for (double beta : betaList)
							{
								double alpha = 1 - beta;
								++runCount;
								AlgoTTFE ttfe = new AlgoTTFE(database, topK, alpha, beta, raise1, raise2, isPrint);
								try
								{
									ttfe.runAlgorithm(inputFilePath, outputFilePath);
									ttfe.printStats(null);
									++succeedCount;
								}
								catch (Throwable e)
								{
									final String errorInfo = e.getMessage();
									ttfe.printStats(null == errorInfo ? "Unknown errors" : errorInfo);
								}
								finally
								{
									ttfe = null; // release citation for GC cleaning
								}
							}
				}
				
				System.out.printf("(Succeed, Run, Expected) = (%d, %d, %d)\n\n\n", succeedCount, runCount, expectedCount);
				if (succeedCount != runCount)
					bRet = false;
			}
			else
			{
				System.out.println("The output folder is not created successfully. \n");
				System.exit(EOF);
			}
		}
		
		System.exit(bRet ? EXIT_SUCCESS : EXIT_FAILURE);
	}
}