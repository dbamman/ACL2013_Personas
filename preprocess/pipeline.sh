#!/bin/zsh

# Before running this script, you need to unzip (or move or symlink)
# several directories into this one.
#
# Step (1.1): get the CoreNLP plot summaries and unzip them here.
# http://www.ark.cs.cmu.edu/personas/data/corenlp_plot_summaries.tar
# You should have a 678 MB directory in here called
#   corenlp_plot_summaries/
#
# Step (1.2): get SupersenseTagger-10-01-12.tar.gz and unzip it in this directory.
# http://www.ark.cs.cmu.edu/mheilman/questions/SupersenseTagger-10-01-12.tar.gz
# You should have a 115 MB directory in here called
#   SupersenseTagger/
#
# Step (1.3): get the movie metadata files and unzip into this directory.
# http://www.ark.cs.cmu.edu/personas/data/MovieSummaries.tar.gz
# You should have a 128 MB directory in here called
#   MovieSummaries/

# After those steps, it should be possible to run this script
#   ./pipeline.sh
#
# This script requires 'zsh' to be installed (it's a zsh script)
# Though you can just run the commands yourself too, of course.

set -eu

if [ ! -d SupersenseTagger ]; then
  echo "ERROR cannot find SupersenseTagger directory."
  exit -1
fi

if [ ! -d corenlp_plot_summaries ]; then
  echo "ERROR cannot fine corenlp_plot_summaries directory (the CoreNLP XML files)"
  exit -1
fi

# This script runs each step in serial. After the JSON-ification, each pipeline step operates on one giant file.
# Commented-out is a parallelized version for the SST step (requires GNU Parallel)
# that's a bit faster if you have the cores/memory to spare.
#
# Note that individual steps can of course be run on any subset of the
# documents you want.  This is good for testing: for example, insert "head -5"
# (or whatever) into step (2) to only work on a few documents.

outname=all

# (2) Convert CoreNLP's XML format to JSON
echo "\nCONVERTING CoreNLP XML to TSV/JSON format"

# mkdir -p batches
print -l corenlp_plot_summaries/*.xml.gz | 
 # head -1000 |
  python core2sent.py --sentmode tsent |
  cat > $outname.noss
  # awk '{print > "batches/" ($1 % 10) ".noss"}'

echo
echo "How many sentences:"
wc -l $outname.noss
echo "How many documents:"
cat $outname.noss | cut -f1 | uniq | wc -l

# (3) Run the supersense tagger
echo "\nRUNNING the supersense tagger"

# print -l batches/*.noss | parallel -j5 --ungroup -v './run_sst.sh < {} > {.}.ss'
./run_sst.sh < $outname.noss > $outname.ss

echo "\nCONVERTING to entity/tuples and Freebase matches"

# (4) Derive entity-centric tuple files
cat $outname.ss | python coreproc.py > $outname.coreproc

# (5) Do the Freebase name matching
./char_matcher_pipe.sh < $outname.coreproc > $outname.coreproc.fb

# (6) Do the final slim-ification in preparation for the model
cat $outname.coreproc.fb | grep -v '^Tpair' | python conv_act_ent.py > $outname.data

echo "\nFINAL file ready for the model:"
ls -l $outname.data

