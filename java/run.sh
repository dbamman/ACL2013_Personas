#!/bin/zsh
set -eu

name=$1
input=$2

mkdir -p $name.out

cat > $name.properties  <<-EOF
# number of topics
K=50

# number of personas
A=50

# max vocabulary size
V=1000

# initial dirichlet document-persona smoother (this gets optimized)
alpha=10

# initial dirichlet topic-word smoother (this gets optimized)
gamma=1

# L2 regularization parameter (for Persona Regression model)
L2=.01

# max number of iterations
maxIterations=50000

# true = run Persona Regression model; false = run Dirichlet Persona Model.
runPersonaRegressionModel=true

# input
data=$input
movieMetadata=input/all.movies.metadata
characterMetadata=input/all.character.metadata

# output
characterPosteriorFile=$name.out/25.100.lda.log.txt
characterConditionalPosteriorFile=$name.out/25.100.lda.cond.log.txt
outPhiWeights=$name.out/out.phi.weights
weights=$name.out/lr.weights.txt
featureMeans=$name.out/featureMeans.txt
featureFile=$name.out/featureFile.txt

personaFile=$name.out/personaFile
finalLAgentsFile=$name.out/finalLAgentsFile
finalLPatientsFile=$name.out/finalLPatientsFile
finalLModFile=$name.out/finalLModFile

EOF

./runjava personas.ark.cs.cmu.edu/PersonaModel $name.properties

#./runjava personas.ark.cs.cmu.edu/PersonaModel $name.properties 2>&1 | cat > $name.log

