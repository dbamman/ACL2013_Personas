#!/bin/bash

# Run Mike Heilman's supersense tagger on the tab-sep sentence formatted files.
# Assume it was unzipped in this directory.

# First have to cd into the SupersenseTagger directory.
sstdir=SupersenseTagger
if [ ! -d $sstdir ]; then
  echo "ERROR the SupersenseTagger directory does not seem to be here."
  exit -1
fi

set -eux
cd $sstdir

# Note that paths are now relative to that directory!
narrjava=../../java
CLASSES=supersense-tagger.jar
CLASSES=$CLASSES:$narrjava/lib/stanford-corenlp-2012-04-09.jar
CLASSES=$CLASSES:$narrjava/narrative.jar
java -XX:ParallelGCThreads=2 -Xmx3000m -ea -classpath $CLASSES personas.ark.cs.cmu.edu.util.RunSS
