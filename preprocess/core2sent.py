#!/usr/bin/env python
# Convert Stanford CoreNLP XML files into jsent format

# INTERNAL NOTE FOR BRENDAN: this is very slightly tweaked from rev 25784 of svn+ssh://malbec.ark.cs.cmu.edu/usr0/svn-base/brendano/sem/lib

import sys,os,re,itertools
import ujson as json
#import json
import xml.etree.ElementTree as ET
from pprint import pprint
import gzip  # not strictly necessary

# Convert to a JSON-able representation
# The XML is 1-indexed for both sentences and tokens
# Convert to 0-indexed for indexing convenience

def convert_sentences(doc_x):
    sents_x = doc_x.find('document').find('sentences').findall('sentence')
    sents = []
    for sent_x in sents_x:
        sent_infos = {}

        toks_x = sent_x.findall(".//token")
        toks_j = [(t.findtext(".//word"), t.findtext(".//lemma"), t.findtext(".//POS"), t.findtext(".//NER")) for t in toks_x]
        sent_infos['tokens'] = toks_j

        char_offsets = []
        for t in toks_x:
            start = int(t.findtext('CharacterOffsetBegin'))
            end = int(t.findtext('CharacterOffsetEnd'))
            char_offsets.append( (start,end) )
        sent_infos['char_offsets'] = char_offsets

        deps_x = sent_x.find('.//collapsed-ccprocessed-dependencies')
        if deps_x is not None:
            deps_j = []
            #deps_x = sent_x.find('.//collapsed-dependencies')
            #deps_x = sent_x.find('.//basic-dependencies')
            for dep_x in deps_x.findall('.//dep'):
                gov = dep_x.find('.//governor')
                gi = int(gov.get('idx')) - 1
                dept= dep_x.find('.//dependent')
                di = int(dept.get('idx')) - 1
                tupl = [dep_x.get('type'), di,gi]
                deps_j.append(tupl)
            sent_infos['deps'] = deps_j
        if sent_x.findtext(".//parse") is not None:
            parse = sent_x.findtext(".//parse").strip()
            parse = re.sub(r'\s+', ' ', parse)
            sent_infos['parse'] = parse
        # yield sent_infos
        sents.append(sent_infos)

    add_sentence_info_for_jsent(sents)
    return sents

def add_sentence_info_for_jsent(sents):
    for i in range(len(sents)):
        sents[i]['id'] = 'S{}'.format(i)

def partokify(jsent):
    row = []
    row.append(u' '.join(t[0] for t in jsent['tokens']).encode('utf8'))
    row.append(u' '.join(t[1] for t in jsent['tokens']).encode('utf8'))
    row.append(u' '.join(t[2] for t in jsent['tokens']).encode('utf8'))
    row.append(u' '.join(t[3] for t in jsent['tokens']).encode('utf8'))
    row.append(json.dumps(jsent['deps']))
    row.append(jsent['parse'].encode('utf8'))
    row.append(json.dumps(jsent['char_offsets']))
    return row

### Entity coref conversion

class Entity(dict):
  def __hash__(self):
    return hash('entity::' + self['id'])

def convert_coref(doc_etree, sentences):
  coref_x = doc_etree.find('document').find('coreference')
  if coref_x is None:
      return []

  entities = []
  for entity_x in coref_x.findall('coreference'):
    mentions = []
    for mention_x in entity_x.findall('mention'):
      m = {}
      m['sentence'] = int(mention_x.find('sentence').text) - 1
      m['start'] = int(mention_x.find('start').text) - 1
      m['end'] = int(mention_x.find('end').text) - 1
      m['head'] = int(mention_x.find('head').text) - 1
      mentions.append(m)
    ent = Entity()
    ent['mentions'] = mentions
    first_mention = min((m['sentence'],m['head']) for m in mentions)
    ent['first_mention'] = first_mention
    # ent['id'] = '%s:%s' % first_mention
    entities.append(ent)
  entities.sort()
  for i in range(len(entities)):
    ent = entities[i]
    ent['num'] = i
    s,pos = ent['first_mention']
    ent['id'] = "E%s" % i
    # ent['nice_name'] = sentences[s]['tokens'][pos]['word']

  return entities

def corexmls_autodetect():
    firstline = sys.stdin.readline()
    if '\t' in firstline:
        print>>sys.stderr, "Assuming CoreXML as TSV lines"
        fn = corexmls_from_tsv
    else:
        print>>sys.stderr, "Assuming filenames input"
        fn = corexmls_from_files
    gen = itertools.chain([firstline], sys.stdin)
    gen = (L.rstrip('\n') for L in gen)
    for item in fn(gen):
        yield item

def corexmls_from_files(linegen):
    for doc_i,filename in enumerate(linegen):
        if doc_i % 100==0: sys.stderr.write('.')
        fp = gzip.open(filename) if filename.endswith('.gz') else open(filename)
        data = fp.read().decode('utf-8','replace').encode('utf-8')
        s = filename
        s = os.path.basename(s)
        s = re.sub(r'\.gz$','',s)
        s = re.sub(r'\.txt\.xml$','',s)
        s = re.sub(r'\.xml$','',s)
        docid = s
        yield docid, data

def corexmls_from_tsv(linegen):
    for line in linegen:
        parts = line.split('\t')
        if len(parts)<2:
            print>>sys.stderr, "skipping line starting with: ", line[:50]
            continue
        docid = '\t'.join(parts[:-1])
        data = parts[-1]
        yield docid, data

def convert_document(xm):
  sentences = convert_sentences(xm)
  entities = convert_coref(xm, sentences)
  return sentences,entities

if __name__=='__main__':
    import argparse; p=argparse.ArgumentParser()
    p.add_argument('--sentmode', choices=['tsent','jsent','doc'], default='doc')
    args = p.parse_args()

    for docid,data in corexmls_autodetect():
        try:
            doc_etree = ET.fromstring(data)
        except ET.ParseError, e:
            print>>sys.stderr, "XML parse failed on doc: ",docid
            continue

        sentences, entities = convert_document(doc_etree)

        if args.sentmode=='dsent':
            sent_texts = [u' '.join(t[0] for t in jsent['tokens']) for jsent in sentences]
            print "{docid}\t{sent_texts}\t{fullinfo}".format(docid=docid,
                    sent_texts=json.dumps(sent_texts),
                    fullinfo = json.dumps({'sentences':sentences, 'entities':entities}),
            )

        elif args.sentmode=='jsent':
            for sent_i,sent_info in enumerate(sentences):
                print u"{docid}\t{sentid}\t{sent_text}\t{sent_info}".format(
                        docid = docid,
                        sentid="S{}".format(sent_i),
                        sent_text = u' '.join(t[0] for t in sent_info['tokens']),
                        sent_info = json.dumps(sent_info).decode('utf8')
                    ).encode('utf8')
        elif args.sentmode=='tsent':
            for sent_i,sent_info in enumerate(sentences):
                row = [docid, "S{}".format(sent_i)]
                row += partokify(sent_info)
                print '\t'.join(row)

        if args.sentmode != 'dsent':
            for ent in entities:
                print "{docid}\t{entid}\t{ent_info}".format(
                        docid=docid, entid=ent['id'], ent_info=json.dumps(ent))
