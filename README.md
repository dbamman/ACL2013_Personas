This repository contains code to:

1. Generate input data from Stanford CoreNLP-processed XML files and attendant movie/character metadata from Freebase.
2. Train a persona model on this data.


GENERATE INPUT DATA (optional)
-----

This step is provided to document the process of text conversion so that the models below can be run on new, different texts.  If you want run the models on existing movie data (found in java/input/movies.data), this step can be skipped. Follow the instructions in pipeline.sh to run the preprocessing pipeline to generate input data. 

`cd preprocess`

`./pipeline.sh`                           (Takes several hours)

TRAIN MODELS
-----

This step trains a new persona model. The run.sh script has a number of variables that can be set, including the number of latent personas (A), the number of latent topics (K), etc.  Here, `INPUT` is the path to movie data (either the default (located in java/input/movies.data) or the output from step 1 above.  `OUTPUT_DIRECTORY` is the location to write all output files to.

`cd ../java`

`gunzip input/movies.data.gz`

`./run.sh $OUTPUT_DIRECTORY $INPUT`   (Takes several hours)
