import sys,re,json,operator
from os import listdir
from os.path import isfile, join

docs={}
actors={}
def readMetadata(nameFile):
	global docs, actors
	file=open(nameFile)
	for line in file:
		cols=line.rstrip().lower().split("\t")
		if len(cols) > 12:
			id=cols[0]
			fbid=cols[10]
			actor=cols[12]
			name=cols[3]
			actors[fbid]=actor
	
			myindex=None
			if id in docs:
				myindex=docs[id]
			else:
				docs[id]={}
	
			parts=name.split(" ")
			for p in parts:
				docs[id][p]=fbid

	file.close()
        # print>>sys.stderr, len(actors), actors.items()[:5]
        # print>>sys.stderr, len(docs), docs.items()[:5]


def main(nameFile, outDirectory):

	onlyfiles = [ f for f in listdir(nameFile) if isfile(join(nameFile,f)) ]

	for f in onlyfiles:
		outfile="%s/%s"% (outDirectory,f)
		out=open(outfile, "w" )
		
		key=f
		key=re.sub(".sent", "", f)

		index=None

		dirf= "%s/%s" % (nameFile, f)
		file=open(dirf)
		for line in file:        


			# locate the movie id in the DOC header, which must be before everything else
			if line.startswith("=== DOC"):
				cols=line.rstrip().split(" ")
				key=cols[2]
				if key in docs:
					index=docs[key]

			cols=line.rstrip().split("\t")
			if len(cols) > 1:
				id=cols[0]
				if id.startswith("E"):
					info = json.loads(cols[1])
					mdict=info['lemma_c']
					sorted_mdict = sorted(mdict.iteritems(), key=operator.itemgetter(1), reverse=True)
					count=0
					best=""
					for name,count in sorted_mdict:
						n=name.lower()
						count+=mdict[name]

						if n in index:
							best=index[n]
							break
					if best == "" and count > 5:
						# try harder
						pass
					#if count >= 2:
					#	print "%s\t\t%s\t%s" % (best,count,mdict)
					
					actor=""
					if best in actors:
						actor=actors[best]
					info["fb"]=best
					info["fba"]=actor
					
					out.write(id + "\t" + json.dumps(info) + "\n")
				else:
					out.write(line.rstrip() + "\n")
			else:
				out.write(line.rstrip() + "\n")

		file.close()
		out.close()

if __name__ == "__main__":

	#python ~/char_matcher.py metadata/all.character.metadata prc/ prcn/
	# prc = folder containing post coreproc.py processed docs
	# prcn = output directory, containing one file for each in prc/

	readMetadata(sys.argv[1])
	main(sys.argv[2], sys.argv[3])
