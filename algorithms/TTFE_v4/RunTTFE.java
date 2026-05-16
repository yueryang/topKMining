package ttfe;

import java.io.File;


/**
 * TTFE (Powered by Universe)
 * @author Yuer Yang, Department of Computer Science, The University of Hong Kong
 * database: the name of the database
 * topK: specify to mine the top-$k$ high threat and frequency event sets
 * alpha & beta: tf(x) = alpha * t(x) + beta * f(x)
 * switches: six switches -> {																	\
 * 		0:"whether specify an initial delta", 													\
 * 		1:"whether switches delta according to $k$ and the sorted ETF", 						\
 * 		2:"whether switches delta by running switchesThreshold_LETF_E()", 						\
 * 		3:"whether switches delta by running switchesThreshold_LETF_LB()", 						\
 * 		4:"whether build a tree to perform the pruning to speed up the mining process", 		\
 * 		5:"whether update delta during offering the queue of final results"						\
 * }
 * debugLevel: four debug levels -> {		\
 * 		0: disable console outputs, 		\
 * 		1: show procedures only, 			\
 * 		2: show detailed information, 		\
 * 		3: show all information, 			\
 * }
 * inputFilePath & outputFilePath: paths of files for inputting and outputting
 * timeForSleep: specify how many milliseconds should the program sleep for to finish GC cleaning
 */
public class RunTTFE
{
	public static final int EXIT_SUCCESS = 0, EXIT_FAILURE = 1, EOF = -1;
	public static void main(String[] args)
	{
		boolean runTTFESample = true, runExperiments = false, runGUMMExperiments = true, runTHUIExperiments = true, runTTFEExperiments = true, bRet = true;
		final long timeForSleep = 1000;
		
		if (runTTFESample)
		{
			int topK = 5, debugLevel = AlgoTTFE.DEBUG_LEVEL_INFO;
			double alpha = 0.5, beta = 0.5;
			boolean[] switches = { false, true, true, false, true, true };
			String inputFilePath = "./ttfe.txt", outputFilePath = "./output.txt";
			
			AlgoTTFE ttfe = new AlgoTTFE.Builder().buildDatabase("ttfe").buildTopK(topK).buildAlphaBeta(alpha, beta).buildSwitches(switches).buildDebugLevel(debugLevel).build();
			try
			{
				ttfe.runAlgorithm(inputFilePath, outputFilePath);
				ttfe.printStats();
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
				try
				{
					Thread.sleep(timeForSleep);
				} catch (InterruptedException e) {}
			}
			
			System.out.println("\n");
		}
		
		if (runExperiments)
		{
			String databaseList[] = { "accidents", "chess", "kosarak", "mushroom" }, inputFolderPath = "./input/", outputFolderPath = "./output/", inputFileExt = ".txt", outputFileExt = ".txt";
			double[] betaList = { 0, 0.25, 0.5, 0.75, 1 };
			int topKList[] = { 10000, 5000, 1000, 500, 100, 50, 10, 5 }, debugLevel = AlgoTTFE.DEBUG_LEVEL_PROCEDURE, succeedCount = 0, runCount = 0, expectedCount = 0;
			
			/* Handle output folder */
			File outputFolderPointer = new File(outputFolderPath);
			if (!outputFolderPointer.isDirectory())
				try
				{
					outputFolderPointer.mkdir();
				}
				catch (Throwable e)
				{
					System.out.println("The output folder is not created successfully. \n\n\n");
					System.exit(EOF);
				}
			if (!outputFolderPointer.isDirectory())
			{
				System.out.println("The output folder is not created successfully. \n\n\n");
				System.exit(EOF);
			}
			
			for (String database : databaseList)
			{
				if (runGUMMExperiments)
				{
					expectedCount += topKList.length * betaList.length;
					boolean switches[] = { false, true, false, false, false, false };
					
					final String inputFilePath = inputFolderPath + database + inputFileExt, outputFilePath = outputFolderPath + "GUMM_{database}_{topK}_{alpha}_{beta}" + outputFileExt;
					File inputFilePointer = new File(inputFilePath);
					if (inputFilePointer.isFile())
						for (int topK : topKList)
							for (double beta : betaList)
							{
								double alpha = 1 - beta;
								++runCount;
								AlgoTTFE ttfe = new AlgoTTFE.Builder().buildDatabase(database).buildTopK(topK).buildAlphaBeta(alpha, beta).buildSwitches(switches).buildDebugLevel(debugLevel).build();
								try
								{
									ttfe.runAlgorithm(inputFilePath, outputFilePath);
									ttfe.printStats();
									++succeedCount;
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
									try
									{
										Thread.sleep(timeForSleep);
									} catch (InterruptedException e) {}
								}
								System.out.println();
							}
				}
				
				if (runTHUIExperiments)
				{
					expectedCount += topKList.length * betaList.length;
					boolean switches[] = { false, true, true, false, true, true };
					
					final String inputFilePath = inputFolderPath + database + inputFileExt, outputFilePath = outputFolderPath + "THUI_{database}_{topK}_{alpha}_{beta}" + outputFileExt;
					final String outputFilePathThreatComponent = outputFolderPath + "THUIT_{database}_{topK}_{alpha}_{beta}" + outputFileExt;
					final String outputFilePathFrequencyComponent = outputFolderPath + "THUIF_{database}_{topK}_{alpha}_{beta}" + outputFileExt;
					File inputFilePointer = new File(inputFilePath);
					if (inputFilePointer.isFile())
						for (int topK : topKList)
							for (double beta : betaList)
							{
								double alpha = 1 - beta;
								++runCount;
								if (0 == beta || 1 == beta)
								{
									AlgoTTFE ttfe = new AlgoTTFE.Builder().buildDatabase(database).buildTopK(topK).buildAlphaBeta(alpha, beta).buildSwitches(switches).buildDebugLevel(debugLevel).build();
									try
									{
										ttfe.runAlgorithm(inputFilePath, outputFilePath);
										ttfe.printStats();
										++succeedCount;
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
										try
										{
											Thread.sleep(timeForSleep);
										} catch (InterruptedException e) {}
									}
								}
								else
								{
									AlgoTTFE ttfe1 = new AlgoTTFE.Builder().buildDatabase(database).buildTopK(topK).buildAlphaBeta(1, 0).buildSwitches(switches).buildDebugLevel(debugLevel).build();
									AlgoTTFE ttfe2 = new AlgoTTFE.Builder().buildDatabase(database).buildTopK(topK).buildAlphaBeta(0, 1).buildSwitches(switches).buildDebugLevel(debugLevel).build();
									AlgoTTFE ttfe = new AlgoTTFE.Builder().buildDatabase(database).buildTopK(topK).buildAlphaBeta(alpha, beta).buildSwitches(switches).buildDebugLevel(debugLevel).build();
									boolean tmpSucceed = true;
									try
									{
										ttfe1.runAlgorithm(inputFilePath, outputFilePathThreatComponent.replace("{alpha}", "" + alpha).replace("{beta}", "" + beta));
										ttfe1.printStats();
									}
									catch (Throwable e)
									{
										final String errorInfo = e.getMessage();
										ttfe1.printStats(null == errorInfo ? "Unknown errors" : errorInfo);
										tmpSucceed = false;
									}
									try
									{
										ttfe2.runAlgorithm(inputFilePath, outputFilePathFrequencyComponent.replace("{alpha}", "" + alpha).replace("{beta}", "" + beta));
										ttfe2.printStats();
									}
									catch (Throwable e)
									{
										final String errorInfo = e.getMessage();
										ttfe2.printStats(null == errorInfo ? "Unknown errors" : errorInfo);
										tmpSucceed = false;
									}
									if (tmpSucceed)
										try
										{
											double startTime = System.currentTimeMillis();
											ttfe.setDelta(alpha * ttfe1.getDelta().doubleValue() + beta * ttfe2.getDelta().doubleValue());
											ttfe.setFinalResults(ttfe1.getFinalResults(), ttfe2.getFinalResults());
											double endTime = System.currentTimeMillis();
											ttfe.setTimeConsumption(ttfe1.getTimeConsumption() + ttfe2.getTimeConsumption() + endTime - startTime);
											ttfe.setMaxMemory(Math.max(ttfe1.getMaxMemory(), ttfe2.getMaxMemory()));
											ttfe.setTreeNodeCount(Math.max(ttfe1.getTreeNodeCount(), ttfe2.getTreeNodeCount()));
											ttfe.setOutputFilePath(outputFilePath);
											ttfe.getResults();
											ttfe.printStats();
											++succeedCount;
										}
										catch (Throwable e)
										{
											final String errorInfo = e.getMessage();
											ttfe.printStats(null == errorInfo ? "Unknown errors" : errorInfo);
											bRet = false;
										}
									else
										bRet = false;
									ttfe1 = null; // release citation for GC cleaning
									ttfe2 = null; // release citation for GC cleaning
									ttfe = null; // release citation for GC cleaning
									try
									{
										Thread.sleep(timeForSleep);
									} catch (InterruptedException e) {}
								}
								System.out.println();
							}
				}
				
				if (runTTFEExperiments)
				{
					expectedCount += topKList.length * betaList.length;
					boolean switches[] = { false, true, true, false, true, true };
					
					final String inputFilePath = inputFolderPath + database + inputFileExt, outputFilePath = outputFolderPath + "TTFE_{database}_{topK}_{alpha}_{beta}" + outputFileExt;
					File inputFilePointer = new File(inputFilePath);
					if (inputFilePointer.isFile())
						for (int topK : topKList)
							for (double beta : betaList)
							{
								double alpha = 1 - beta;
								++runCount;
								AlgoTTFE ttfe = new AlgoTTFE.Builder().buildDatabase(database).buildTopK(topK).buildAlphaBeta(alpha, beta).buildSwitches(switches).buildDebugLevel(debugLevel).build();
								try
								{
									ttfe.runAlgorithm(inputFilePath, outputFilePath);
									ttfe.printStats();
									++succeedCount;
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
									try
									{
										Thread.sleep(timeForSleep);
									} catch (InterruptedException e) {}
								}
								System.out.println();
							}
				}
			}
			
			System.out.printf("(Succeed, Run, Expected) = (%d, %d, %d)\n\n\n\n", succeedCount, runCount, expectedCount);
		}
		
		System.exit(bRet ? EXIT_SUCCESS : EXIT_FAILURE);
	}
}