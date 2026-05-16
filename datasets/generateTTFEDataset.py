import os
os.chdir(os.path.abspath(os.path.dirname(__file__)))
EXIT_SUCCESS = 0
EXIT_FAILURE = 1
EOF = -1
datasetNames = ["accidents", "chess", "kosarak", "mushroom", "pumsb", "retail"]
positiveFileFormat = "./{0}_positive.txt"
negativeFileFormat = "./{0}_negative.txt"
outputFileFormat = "./{0}.txt"
prefix = "# Event : Threat : Frequency : TTF\n"


def getTxt(filepath, index = 0) -> str: # get .txt content
	coding = ("utf-8", "gbk", "utf-16") # codings
	if 0 <= index < len(coding): # in the range
		try:
			with open(filepath, "r", encoding = coding[index]) as f:
				content = f.read()
			return content[1:] if content.startswith("\ufeff") else content # if utf-8 with BOM, remove BOM
		except (UnicodeError, UnicodeDecodeError):
			return getTxt(filepath, index + 1) # recursion
		except:
			return None
	else:
		return None # out of range

def main():
	bRet = True
	for datasetName in datasetNames:
		pos = getTxt(positiveFileFormat.format(datasetName))
		neg = getTxt(negativeFileFormat.format(datasetName))
		if pos and neg:
			lines = []
			posLines = pos.split("\n")
			negLines = neg.split("\n")
			for i in range(min(len(posLines), len(negLines))):
				posLineList = posLines[i].split(":")
				negLineList = negLines[i].split(":")
				if len(posLineList) == 3 and len(negLineList) == 3 and posLineList[0] == negLineList[0]:
					lines.append("{0}:{1}:{2}:".format(posLineList[0], negLineList[-1], posLineList[-1]))
					lines[-1] += str(sum([float(item) for item in posLineList[-1].split(" ") + negLineList[-1].split(" ")]) / 2)
			try:
				with open(outputFileFormat.format(datasetName), "w", encoding = "utf-8") as f:
					f.write(prefix + "\n".join(lines))
				print("\"{0}\" -> ({1}, {2}, {3})".format(outputFileFormat.format(datasetName), len(posLines), len(negLines), len(lines)))
			except Exception as e:
				bRet = False
				print("\"{0}\" -> {1}".format(outputFileFormat.format(datasetName), EOF))
		else:
			print("\"{0}\" -> 0".format(outputFileFormat.format(datasetName)))
	return EXIT_SUCCESS if bRet else EXIT_FAILURE



if __name__ == "__main__":
	exit(main())