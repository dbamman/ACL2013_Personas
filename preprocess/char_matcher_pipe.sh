#!/bin/bash
set -eux
here=$(dirname $0)
rm -rf tmp.in tmp.out
mkdir -p tmp.in tmp.out
cat > tmp.in/corefile
# INTERNAL NOTE: on cab.ark for the ACL paper (~Feb 2013) we used the line
# python java/narrative/char_matcher.py ~/mv/dmv/metadata/all.character.metadata tmp.in tmp.out
python char_matcher.py MovieSummaries/character.metadata.tsv tmp.in tmp.out
cat tmp.out/corefile

