import java.util.ArrayList;
import java.util.List;


public class TopKMining
{
	public static enum LogLevel
	{
		All((byte)0), 
		Trace((byte)1), 
		Debug((byte)2), 
		Info((byte)3), 
		Warning((byte)4), 
		Error((byte)5), 
		Fatal((byte)6), 
		Off((byte)7);
		
		private final byte value;
		
		private LogLevel(final byte level) 
		{
			this.value = level;
		}
		public byte getValue()
		{
			return this.value;
		}
		@Override
		public String toString()
		{
			switch (this)
			{
				case All:
					return "All (" + this.value + ")";
				case Trace:
					return "Trace (" + this.value + ")";
				case Debug:
					return "Debug (" + this.value + ")";
				case Info:
					return "Info (" + this.value + ")";
				case Warning:
					return "Warning (" + this.value + ")";
				case Error:
					return "Error (" + this.value + ")";
				case Fatal:
					return "Fatal (" + this.value + ")";
				case Off:
					return "Off (" + this.value + ")";
				default:
					return "Unknown (" + this.value + ")";
			}
		}
	}
	
	public static class ArgumentParser
	{
		private static final LogLevel DefaultLogLevel = LogLevel.Info;
		private static final int DefaultK = 10;
		private static final String[] HelpArguments = { "h", "/h", "-h", "help", "/help", "--help" };
		private static final String[] AlgorithmArguments = { "a", "/a", "-a", "algorithm", "/algorithm", "--algorithm" };
		private static final String[] DatabaseArguments = { "d", "/d", "-d", "database", "/database", "--database" };
		private static final String[] KArguments = { "k", "/k", "-k" };
		private static final String[] LogLevelArguments = { "l", "/l", "-l", "logLevel", "/logLevel", "--logLevel" };
		private static final String[] OutputArguments = { "o", "/o", "-o", "output", "/output", "--output" };
		
		private String warnings = null;
		private String logs = null;
		private boolean exitFlag = false;
		private String algorithm = null;
		private String database = null;
		private int k = DefaultK;
		private LogLevel logLevel = LogLevel.Info;
		private String output = null;
		
		public ArgumentParser()
		{
			
		}
		public static LogLevel getDefaultLogLevel()
		{
			return DefaultLogLevel;
		}
		private static boolean contains(final String[] array, final String target)
		{
			if (null == array || 0 == array.length || null == target)
				return false;
			else
			{
				for (final String element : array)
					if (element != null && element.equalsIgnoreCase(target))
						return true;
				return false;
			}
		}
		private static String array2String(final String[] arguments, final String prefix, final String separator, final String suffix)
		{
			StringBuilder sb = new StringBuilder(null == prefix ? "" : prefix);
			if (arguments != null && arguments.length > 0)
			{
				final String realSeparator = null == separator ? "" : separator;
				for (int index = 0; index < arguments.length; ++index)
					if (arguments[index] != null)
					{
						sb.append(arguments[index]);
						for (++index; index < arguments.length; ++index)
							if (arguments[index] != null)
							{
								sb.append(realSeparator);
								sb.append(arguments[index]);
							}
						break;
					}
			}
			sb.append(null == suffix ? "" : suffix);
			return sb.toString();
		}
		private static String array2String(final String[] arguments) { return array2String(arguments, "[", "|", "]"); }
		private static void printHelp()
		{
			System.out.println("This is a runner for multiple top-$k$ mining algorithms. ");
			System.out.println();
			System.out.println("Options:");
			System.out.println("\t" + array2String(HelpArguments) + "\t\tPrint the help warnings.");
			System.out.println("\t" + array2String(AlgorithmArguments) + " <name>\t\tSpecify the algorithm (e.g., THUI, THUFI, and TTFE). ");
			System.out.println("\t" + array2String(DatabaseArguments) + " <database>\t\tSpecify the database. ");
			System.out.println("\t" + array2String(LogLevelArguments) + " <level>\t\tSpecify the log level from " + LogLevel.All + " to " + LogLevel.Off + ". The default value is " + DefaultLogLevel + ". ");
			System.out.println("\t" + array2String(OutputArguments) + " <output>\t\tSpecify the output. ");
			System.out.println();
			System.out.println("Notes:");
			System.out.println("\t1) All arguments are optional and processed sequentially. If the same argument is provided multiple times, the last valid one will overwrite previous ones. Unrecognized or invalid arguments will be skipped with a warning. ");
			System.out.println("\t2) Each unrecognized line in the database will be skipped with a warning. ");
			System.out.println("\t3) If the output is not provided, the result will be printed to the console. The parent directory for the output will be automatically created if the output is a file path and its parent directory does not exist. ");
			System.out.println();
			return;
		}
		private static String arrayList2String(final List<String> arrayList)
		{
			if (null == arrayList || arrayList.isEmpty())
				return "";
			else
			{
				final int size = arrayList.size();
				StringBuilder sb = new StringBuilder();
				switch (size)
				{
				case 0:
					break;
				case 1:
					sb.append(arrayList.get(0));
					break;
				case 2:
					sb.append(arrayList.get(0)).append(" and ").append(arrayList.get(1));
					break;
				default:
					for (int i = 0; i < size - 1; ++i)
						sb.append(arrayList.get(i)).append(", ");
					sb.append("and ").append(arrayList.get(size - 1));
				}
				return sb.toString();
			}
		}
		private static String arrayList2String(final List<Integer> arrayList, final String itemPrefix, final String itemSuffix)
		{
			if (null == arrayList || arrayList.isEmpty())
				return "";
			else
			{
				final int size = arrayList.size();
				StringBuilder sb = new StringBuilder();
				switch (size)
				{
				case 0:
					break;
				case 1:
					sb.append(itemPrefix).append(arrayList.get(0)).append(itemSuffix);
					break;
				case 2:
					sb.append(itemPrefix).append(arrayList.get(0)).append(itemSuffix).append(" and ").append(itemPrefix).append(arrayList.get(1)).append(itemSuffix);
					break;
				default:
					for (int i = 0; i < size - 1; ++i)
						sb.append(itemPrefix).append(arrayList.get(i)).append(itemSuffix).append(", ");
					sb.append("and ").append(itemPrefix).append(arrayList.get(size - 1)).append(itemSuffix);
				}
				return sb.toString();
			}
		}
		private boolean parseK(final String s)
		{
			if (null == s || s.isEmpty())
				return false;
			else
			{
				int frontIndex = 0, radix = 0, endIndex = s.length() - 1, value = 0;
				boolean isNegative = false;
				for (boolean breakFlag = false; frontIndex < s.length(); ++frontIndex)
				{
					switch (s.charAt(frontIndex))
					{
					case '\t':
					case ' ':
					case '+':
					case '_':
						continue;
					case '-':
						isNegative = !isNegative;
						break;
					default:
						breakFlag = true;
						break;
					}
					if (breakFlag)
						break;
				}
				for (boolean breakFlag = false; frontIndex < s.length(); ++frontIndex) // make ``frontIndex`` point to the first effective digit
				{
					switch (s.charAt(frontIndex))
					{
					case '\t':
					case ' ':
					case '0':
					case '_':
						continue;
					case 'X':
					case 'x':
						radix = 16;
						++frontIndex;
						breakFlag = true;
						break;
					case 'D':
					case 'd':
						radix = 10;
						++frontIndex;
						breakFlag = true;
						break;
					case 'O':
					case 'o':
						radix = 8;
						++frontIndex;
						breakFlag = true;
						break;
					case 'Q':
					case 'q':
						radix = 4;
						++frontIndex;
						breakFlag = true;
						break;
					case 'B':
					case 'b':
						radix = 2;
						++frontIndex;
						breakFlag = true;
						break;
					default:
						breakFlag = true;
						break;
					}
					if (breakFlag)
						break;
				}
				if (0 == radix) // prefix is prior to suffix
					for (boolean breakFlag = false; endIndex > frontIndex; --endIndex) // make ``endIndex`` point to the first effective digit
					{
						switch (s.charAt(endIndex))
						{
						case '\t':
						case ' ':
						case '_':
							continue;
						case 'X':
						case 'x':
							radix = 16;
							--endIndex;
							breakFlag = true;
							break;
						case 'D':
						case 'd':
							radix = 10;
							--endIndex;
							breakFlag = true;
							break;
						case 'O':
						case 'o':
							radix = 8;
							--endIndex;
							breakFlag = true;
							break;
						case 'Q':
						case 'q':
							radix = 4;
							--endIndex;
							breakFlag = true;
							break;
						case 'B':
						case 'b':
							radix = 2;
							--endIndex;
							breakFlag = true;
							break;
						default:
							breakFlag = true;
							break;
						}
						if (breakFlag)
							break;
					}
				if (0 == radix)
					radix = 10;
				boolean isOverflowed = false, isIllegalDigitDetected = false;
				for (int index = frontIndex; index <= endIndex; ++index)
				{
					final int digit = Character.digit(s.charAt(index), radix);
					if (0 <= digit && digit < radix)
					{
						long testValue = (long)value * radix + digit; // test whether it is overflowed
						if (testValue > Integer.MAX_VALUE)
						{
							value = Integer.MAX_VALUE;
							isOverflowed = true;
							break;
						}
						else
							value = (int)testValue;
					}
					else
						isIllegalDigitDetected = true;
				}
				this.k = value;
				List<String> issues = new ArrayList<String>();
				if (isNegative)
					issues.add("the negative sign removed");
				if (isOverflowed)
					issues.add("an overflow signal captured");
				if (isIllegalDigitDetected)
					issues.add("at least an illegal digit detected");
				if (!issues.isEmpty())
					this.warnings = "Parsed $k = " + this.k + "$ with " + this.arrayList2String(issues) + ". ";
				else
					this.logs = "Parsed $k = " + this.k + "$. ";
				return true;
			}
		}
		public boolean parseArguments(final String[] arguments, final boolean resetBeforeParsing)
		{
			this.warnings = null;
			this.logs = null;
			this.exitFlag = false;
			if (resetBeforeParsing)
			{
				this.algorithm = null;
				this.database = null;
				this.k = DefaultK;
				this.logLevel = DefaultLogLevel;
				this.output = null;
			}
			boolean missingArgument = false;
			List<Integer> invalidArgumentIndexes = new ArrayList<Integer>();
			for (int i = 0; i < arguments.length; ++i)
			{
				if (null == arguments[i])
					invalidArgumentIndexes.add(i);
				else if (this.contains(HelpArguments, arguments[i]))
				{
					this.printHelp();
					this.exitFlag = true;
					return true;
				}
				else if (this.contains(AlgorithmArguments, arguments[i]))
					if (++i < arguments.length)
						this.algorithm = arguments[i];
					else
						missingArgument = true;
				else if (this.contains(DatabaseArguments, arguments[i]))
					if (++i < arguments.length)
						this.database = arguments[i];
					else
						missingArgument = true;
				else if (this.contains(KArguments, arguments[i]))
					if (++i < arguments.length)
					{
						if (!this.parseK(arguments[i]))
							invalidArgumentIndexes.add(i);
					}
					else
						missingArgument = true;
				else if (this.contains(LogLevelArguments, arguments[i]))
					if (++i < arguments.length)
					{
						if (null == arguments[i] || arguments[i].isEmpty())
							invalidArgumentIndexes.add(i);
						else
							switch ((byte)arguments[i].charAt(0))
							{
							case 'A':
							case 'a':
								this.logLevel = LogLevel.All;
								break;
							case 'T':
							case 't':
								this.logLevel = LogLevel.Trace;
								break;
							case 'D':
							case 'd':
								this.logLevel = LogLevel.Debug;
								break;
							case 'I':
							case 'i':
								this.logLevel = LogLevel.Info;
								break;
							case 'W':
							case 'w':
								this.logLevel = LogLevel.Warning;
								break;
							case 'E':
							case 'e':
								this.logLevel = LogLevel.Error;
								break;
							case 'F':
							case 'f':
								this.logLevel = LogLevel.Fatal;
								break;
							case 'O':
							case 'o':
								this.logLevel = LogLevel.Off;
								break;
							default:
							{
								byte lowerBound = LogLevel.All.getValue(), upperBound = LogLevel.Off.getValue();
								if (0 <= lowerBound && lowerBound <= upperBound && upperBound <= 9)
								{
									byte x = (byte)(arguments[i].charAt(0) >= '0' ? arguments[i].charAt(0) - '0' : arguments[i].charAt(0));
									switch (x)
									{
									case 0:
										this.logLevel = LogLevel.All;
										break;
									case 1:
										this.logLevel = LogLevel.Trace;
										break;
									case 2:
										this.logLevel = LogLevel.Debug;
										break;
									case 3:
										this.logLevel = LogLevel.Info;
										break;
									case 4:
										this.logLevel = LogLevel.Warning;
										break;
									case 5:
										this.logLevel = LogLevel.Error;
										break;
									case 6:
										this.logLevel = LogLevel.Fatal;
										break;
									case 7:
										this.logLevel = LogLevel.Off;
										break;
									default:
										invalidArgumentIndexes.add(i);
										break;
									}
								}
								else
									invalidArgumentIndexes.add(i);
								break;
							}
							}
					}
					else
						missingArgument = true;
				else if (this.contains(OutputArguments, arguments[i]))
					if (++i < arguments.length)
						this.output = arguments[i];
					else
						missingArgument = true;
				else
					invalidArgumentIndexes.add(i);
			}
			StringBuilder sb = new StringBuilder();
			if (missingArgument)
				sb.append("The corresponding value for the last argument is missing. ");
			final int invalidArgumentCount = invalidArgumentIndexes.size();
			if (1 == invalidArgumentCount)
				sb.append("The argument whose index is [" + invalidArgumentIndexes.get(0) + "] could not be recognized, which has been skipped. ");
			else if (invalidArgumentCount >= 2)
				sb.append(invalidArgumentCount + " arguments, whose indexes are ").append(arrayList2String(invalidArgumentIndexes, "[", "]")).append(", could not be recognized, which have been skipped. ");
			this.warnings = null == this.warnings || this.warnings.isEmpty() ? this.warnings : this.warnings + sb.toString();
			return this.database != null && !this.database.isEmpty();
		}
		public boolean parseArguments(String[] arguments) { return this.parseArguments(arguments, true); }
		public String getWarnings()
		{
			return this.warnings;
		}
		public String getLogs()
		{
			return this.logs;
		}
		public boolean getExitFlag()
		{
			return this.exitFlag;
		}
		public String getAlgorithm()
		{
			return this.algorithm;
		}
		public Integer getK()
		{
			return this.k;
		}
		public LogLevel getLogLevel()
		{
			return this.logLevel;
		}
		public String getOutput()
		{
			return this.output;
		}
	}
	
	public static class Logger
	{
		private LogLevel logLevel = ArgumentParser.getDefaultLogLevel();
		
		public Logger()
		{
			
		}
		public Logger(final LogLevel level)
		{
			this.setLogLevel(level);
		}
		public boolean setLogLevel(final LogLevel level)
		{
			if (null == level)
			{
				this.logLevel = ArgumentParser.getDefaultLogLevel();
				return false;
			}
			else
			{
				this.logLevel = level;
				return true;
			}
		}
		public boolean print(final String content, final LogLevel level)
		{
			if (level.getValue() >= this.logLevel.getValue())
				switch (level)
				{
				case Trace:
					System.err.println("Trace: " + content);
					return true;
				case Debug:
					System.err.println("Debug: " + content);
					return true;
				case Info:
					System.err.println("Info: " + content);
					return true;
				case Warning:
					System.err.println("Warning: " + content);
					return true;
				case Error:
					System.err.println("Error: " + content);
					return true;
				case Fatal:
					System.err.println("Fatal: " + content);
					return true;
				default:
					return false;
				}
			else
				return false;
		}
	}
	
	public static abstract class Algorithm
	{
		private String database = null;
		private String output = null;

		public Algorithm(final String database, final String output)
		{
			this.database = database;
			this.output = output;
		}
		public abstract boolean loadDatabase();
		public abstract boolean runAlgorithm();
		public abstract void saveTo();
	}
	
	/* !--! */
	
	final static int EXIT_SUCCESS = 0;
	final static int EXIT_FAILURE = 1;
	final static int EOF = (-1);
	
	public static void main(final String[] arguments)
	{
		final ArgumentParser argumentParser = new ArgumentParser();
		final boolean flag = argumentParser.parseArguments(arguments);
		final String warnings = argumentParser.getWarnings();
		final String logs = argumentParser.getLogs();
		final Logger logger = new Logger(argumentParser.getLogLevel());
		if (warnings != null && !warnings.isEmpty())
			logger.print(warnings, LogLevel.Warning);
		if (logs != null && !logs.isEmpty())
			logger.print(logs, LogLevel.Debug);
		if (flag)
		{
			/* !--! */
			System.exit(EXIT_SUCCESS);
		}
		else
		{
			logger.print("The database is not specified. ", LogLevel.Fatal);
			System.exit(EOF);
		}
	}
}