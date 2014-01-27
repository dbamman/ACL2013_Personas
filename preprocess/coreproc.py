# Extract what we care about from the CoreXML files
# some code borrowed from https://github.com/brendano/parseviz/blob/master/docviz.py

import sys,re,itertools
from pprint import pprint
# import ujson as json
import json
from collections import defaultdict, Counter

def uniq_c(seq):
  ret = defaultdict(lambda:0)
  for x in seq:
    ret[x] += 1
  return dict(ret)

def rowgen():
    for line in sys.stdin:
        row = line.rstrip('\n').split('\t')
        yield row

def parse_docrows(rows):
    rows = list(rows)
    sents = []
    for row in (r for r in rows if r[1].startswith('S')):
        s = {}
        s['id']=row[1]
        s['word']=row[2].split()
        s['lemma']=row[3].split()
        s['pos']=row[4].split()
        s['sstag']=row[-1].split()
        s['deps']=json.loads(row[6])
        assert len(s['word'])==len(s['sstag'])
        sents.append(s)
    ents  = [json.loads(row[-1]) for row in rows if row[1].startswith('E')]
    return sents,ents

def process_entity(ent, sents):
    headinds = [(m['sentence'],m['head']) for m in ent['mentions']]
    headlemmas = [sents[s]['lemma'][t] for s,t in headinds]
    mention_texts = [ u' '.join([sents[m['sentence']]['word'][t] for t in range(m['start'],m['end'])]) for m in ent['mentions']]

    sstags = [sents[s]['sstag'][t].split('-')[-1] for s,t in headinds]
    ssc = uniq_c(sstags)
    if '0' in ssc: del ssc['0']
    if ssc:
        most_common_sstag = max(ssc.keys(), key=lambda k: ssc[k])
        ent['sstag'] = most_common_sstag
    else:
        ent['sstag'] = None

    ent['sstag_c'] = ssc
    ent['lemma_c'] = uniq_c(headlemmas)
    ent['fulltext_c'] = uniq_c(mention_texts)

    ## Deduplicate mentions that Stanford sometimes gives us
    seen_sentheads = set()
    mentions2 = []
    for ment in ent['mentions']:
        s,h = ment['sentence'],ment['head']
        if (s,h) in seen_sentheads: continue
        seen_sentheads.add((s,h))
        mentions2.append(ment)
    ent['mentions'] = mentions2

    for ment in ent['mentions']:
        sents[ment['sentence']]['entities'][ment['head']] = ent['num']

    decide_canonical_mention(ent, sents)

def is_pronoun(sent, t):
    return sent['pos'][t].startswith('PRP')

def is_proper(sent, t):
    return sent['pos'][t].startswith('NNP')

def is_verb(sent, t):
    pos = sent['pos'][t]
    return pos.startswith('V') or pos=='MD'

def entity_summaries(sents,ents):
    for ent in ents:
        del ent['mentions'], ent['first_mention']
        print "{}\t{}".format(ent['id'], json.dumps(ent))

def decide_canonical_mention(ent, sents):
    good_texts = Counter()
    fallback_texts = Counter()
    really_fallbacks = Counter()
    for m in ent['mentions']:
        sent = sents[m['sentence']]
        tokinds = range(m['start'], m['end'])
        text = u' '.join(sent['word'][t] for t in tokinds).lower()
        # print [sent['pos'][t] for t in tokinds], '\t|||\t', text
        if all(is_pronoun(sent, t) for t in tokinds):
            really_fallbacks[text] += 1
            continue
        if len(tokinds) > 5:
            text = sent['lemma'][m['head']]
            fallback_texts[text] += 1
            continue
        for itr in range(5):
            if not tokinds: break
            if tokinds and sent['pos'][tokinds[0]]=='DT':
                tokinds = tokinds[1:]
            if tokinds and sent['pos'][tokinds[-1]]=='POS':
                tokinds = tokinds[:-1]
        if not tokinds: continue

        text = u' '.join(sent['word'][t] for t in tokinds).lower()
        good_texts[text] += 1
    if good_texts:
        best = sorted(good_texts.items(), key=lambda (text,c): (-c, -len(text.split()))
                )[0][0]
    elif fallback_texts:
        best = sorted(fallback_texts.items(), key=lambda (text,c): (-c, len(text.split()))
                )[0][0]
    elif really_fallbacks:
        best = sorted(really_fallbacks.items(), key=lambda (text,c): (-c, -len(text.split()))
                )[0][0]
    elif len(ent['mentions']) > 0:
        m = ent['mentions'][0]
        best = u' '.join(sents[m['sentence']]['word'][t] for t in range(m['start'], m['end'])).lower()
    else:
        best = 'NO_TEXT'
    ent['canonical_text'] = best


def process_sentences(sents):
    for sent in sents:
        # per-token entityid's
        sent['entities'] = [None] * len(sent['word'])

def clean_sstag(sstag):
    if sstag=='0': return None
    return sstag.split('-')[-1]

# path element is tuple (direction, rel, targetnode)
# 
def enlargen_paths(seedpaths, sent):
    newpaths = set()
    for path in seedpaths:
        seen_nodes = {i for dir,rel,i in path}
        newn = []
        for i in seen_nodes:
            newn += [('D',rel,di) for rel,di,gi in sent['deps'] if gi==i and di not in seen_nodes]
            newn += [('G',rel,gi) for rel,di,gi in sent['deps'] if di==i and gi not in seen_nodes]
        for tpl in newn:
            newpaths.add( path + (tpl,) )
    return newpaths

def basepaths(t, sent):
    neighbors = [('D',rel,di) for rel,di,gi in sent['deps'] if gi==t]
    neighbors +=[('G',rel,gi) for rel,di,gi in sent['deps'] if di==t]
    return [(tpl,) for tpl in neighbors]

def pathendpoint_filter((dir,rel,i)):
    if rel=='appos': return False
    if rel.startswith('conj'): return False
    if rel=='parataxis': return False
    if rel=='det': return False
    if rel=='advcl': return False
    if rel=='ccomp': return False
    if rel=='dep': return False
    if rel=='rcmod': return False
    return True

def highlight(mention, sent):
    toks = [w if i!=mention['head'] else '**{}**'.format(w) for i,w in enumerate(sent['lemma'])]
    return u' '.join(toks)

def showsent(sent):
    toks = [w for i,w in enumerate(sent['word'])]
    return u' '.join(toks)

def filter_paths(paths):
    paths = [p for p in paths if pathendpoint_filter(p[-1])]
    return paths

def normalize_edgelabels(path):
    path = [list(x) for x in path]
    for x in path[1:]:
        if x[1] in ('dobj','nsubjpass'):
            x[1]='semobj'
        # can this ever happen? dont think so but just in case.
        if x[1] in ('nsubj','agent'):
            x[1] = 'semsubj'
    return path

def get_connecting_path(s,t, sent):
    paths = basepaths(s, sent)
    paths = filter_paths(paths)
    for plen in range(4):
        for path in paths:
            if path[-1][2] == t:
                return path
        paths2 = enlargen_paths(paths, sent)
        paths = paths2
        paths = filter_paths(paths)
    return None

def get_unarypaths(t, sent):
    paths = basepaths(t, sent)
    paths = filter_paths(paths)
    paths += enlargen_paths(paths, sent)
    paths = filter_paths(paths)
    return paths

def extend_path_for_superobj(paths, sent):
    for path in paths:
        seen_nodes = {i for d,r,i in path}
        last = path[-1][2]
        # if sent['entities'][last] is not None:
        #     continue
        for rel,di,gi in sent['deps']:
            if not (gi==last or di==last): continue
            index = di if gi==last else gi
            if index in seen_nodes: continue
            direction = 'U' if di==last else 'D'
            new_pathpoint = (direction,rel,index)
            if not pathendpoint_filter(new_pathpoint): continue
            yield path + [new_pathpoint]

def get_superobj_paths(seedpath, sent, sizelim=7):
    allpaths = [seedpath]
    last_paths = [seedpath]
    new_paths = []
    for i in range(sizelim):
        new_paths = list(extend_path_for_superobj(last_paths, sent))
        allpaths += new_paths
        last_paths = new_paths
    return allpaths

def new_stuff(sent, ents, snum):
    T = len(sent['word'])
    all_entids = set(sent['entities'])
    if None in all_entids: all_entids.remove(None)
    # print '---'

    all_pathtuples = []
    all_agent_unaries = set()
    all_patient_unaries = set()
    for s in range(T):
        ent = sent['entities'][s]
        if ent is None: continue
        print 'ENTMENT',ent,sent['word'][s]

        govs   = [(rel,gi) for rel,di,gi in sent['deps'] if di==s]
        childs = [(rel,di) for rel,di,gi in sent['deps'] if gi==s]

        # governing verb analysis
        immediate_verbgovs = [(r,i) for r,i in govs if is_verb(sent,i)]
        agent_verbgovs = [(r,i) for r,i in immediate_verbgovs if r=='nsubj' or r=='agent']
        all_agent_unaries |= {(s,verbind) for r,verbind in agent_verbgovs}
        patient_verbgovs = [(r,i) for r,i in immediate_verbgovs if is_patient_rel(r)]
        all_patient_unaries |= {(s,verbind) for r,verbind in patient_verbgovs}
        all_pathtuples += pathtuples_with_agent(sent, s, agent_verbgovs)

        def printmod(direction, rel,ind):
            sys.stdout.write("M.{}.{}.{}\t".format(snum, s, ind))
            # output_word = compact_json([direction, rel, sent['lemma'][ind]])
            output_word = sent['lemma'][ind]
            print ' '.join(str(x) for x in [
                output_word,
                clean_sstag(sent['sstag'][s]),
                'M:bla',
                "E{}".format(sent['entities'][s]),
                sent['lemma'][s]
            ])

        # print 'GOVS', [(r,sent['word'][i],sent['pos'][i]) for r,i in govs]
        # nonverb_govs = [(rel,i) for rel,i in govs if not is_verb(sent,i)]
        # if nonverb_govs:
        #     print 'GOVS', [(rel,sent['word'][i]) for rel,i in nonverb_govs]
        # if childs:
        #     print 'CHILDS', [(rel,sent['word'][i]) for rel,i in childs]

        def is_modifier_pos(t):
            # basically want common nouns and adjectives.
            return not is_pronoun(sent,t) and not is_proper(sent,t) and not is_verb(sent,t)

        if is_modifier_pos(s):
            printmod('SM', 'selfmention', s)

        mod_filter = lambda i: is_modifier_pos(i) and sent['entities'][i] is None
        mod_govs = [(r,i) for r,i in govs if r in ('nsubj','appos') and mod_filter(i)]
        mod_childs = [(r,i) for r,i in childs if r in ('nsubj','appos','amod','nn') and mod_filter(i)]
        for r,i in mod_govs:
            printmod('U',r,i)
        for r,i in mod_childs:
            printmod('D',r,i)


    for pairid,(agentind, patientind, verbind, pathstr) in enumerate(all_pathtuples):
        def printstr(pathside, entityind):
            sys.stdout.write("Tpair{}.{}\t".format(snum, pairid))
            print ' '.join(str(x) for x in [
                pathstr,
                clean_sstag(sent['sstag'][entityind]),
                pathside, 
                "E{}".format(sent['entities'][entityind]),
                sent['lemma'][entityind] ])
        printstr('A:bla', agentind)
        printstr('P:bla', patientind)

def pathtuples_with_agent(sent, agentind, agent_verbgovs):
    path_tuples = []
    for r,verbind in agent_verbgovs:
        seedpath = [(None,None,agentind), ('U',r,verbind)]
        superobj_paths = get_superobj_paths(seedpath, sent, 3)
        for path in superobj_paths:
            last = path[-1][2]
            if sent['entities'][last] is None: continue
            if not is_patient_rel(path[-1][1]): continue
            path = normalize_edgelabels(path)
            pathstr = pathstring(path,sent)
            # print 'PATHTUPLE', sent['lemma'][s], sent['lemma'][last], pathstr
            path_tuples.append((agentind, last, path[1][2], pathstr))
    return path_tuples



def compact_json(x):
    return json.dumps(x, separators=(',', ':'))

def pathstring(path, sent):
    newlist = [(dir,rel,sent['lemma'][i]) for dir,rel,i in path[2:-1]]
    newlist = [sent['lemma'][path[1][2]]] + newlist + [path[-1][:2]]
    s = json.dumps(newlist, separators=(',', ':'))
    # s = s.replace(' ','_') ## 
    assert ' ' not in s
    return s

# def ezpathstring(pathstr):
#     path = json.loads(pathstr)
#     ret = []
#     ret.append(path[0])
#     s += path[0]

def is_agent_rel(rel):
    return rel in ('nsubj', 'agent')

def is_patient_rel(rel):
    return rel.startswith('prep_') or rel in ('nsubjpass','iobj','dobj')

## relation counts for depchildren of verb that are entity mention heads
# 372632 nsubj
# 139370 dobj
# 102142 prep_
# 45322 nsubjpass
# 11835 agent
# 3716 iobj
# 3371 xcomp
# 3125 dep
# 2684 advmod
# 1209 conj_and
# 1080 advcl
# 443 pobj
# 262 parataxis
# 257 conj_but
# 197 npadvmod
# 163 prepc_
# 138 attr
# 114 poss

def verb_analysis(sent, ents):
    T = len(sent['word'])
    verbinds = [t for t in range(T) if sent['pos'][t].startswith('VB')]
    for verbid, verbind in enumerate(verbinds):
        childs = [(rel,di) for rel,di,gi in sent['deps'] \
                if gi==verbind and sent['entities'][di] is not None]
        childs = [(rel,c) for rel,c in childs if is_patient_rel(rel) or is_agent_rel(rel)]
        if not childs: continue
        ## dedupe by headword position. stanford dep bug that these exist?
        childs = set(childs)
        ## dedupe by entity. prefer later positions, therefore reverse sort here
        childs = [(rel,i, sent['entities'][i]) for rel,i in childs]
        childs.sort(key=lambda (rel,i,e): (-i,rel,e))
        childs_by_ent = {e:(rel,i,e) for rel,i,e in childs}
        childs = childs_by_ent.values()
        childs.sort(key=lambda (rel,i,e): (i,rel,e))
        childs = [(rel,i) for rel,i,e in childs]
        # print 'CHILDS', len(childs), childs
        agents =  [(rel,i) for rel,i in childs if is_agent_rel(rel)]
        patients= [(rel,i) for rel,i in childs if is_patient_rel(rel)]
        # print 'AGENTS', len(agents), agents
        # print 'PATIENTS', len(patients), patients
        if not agents and not patients: continue

        # add these so crossproduct sees singletons
        if not agents:   agents = [(None,None)]
        if not patients: patients=[(None,None)]

        sentid=re.sub("^S", "", sent['id'])

        for pairid, ((arel, agent_i), (prel, patient_i)) \
                in enumerate(itertools.product(agents,patients)):
            if arel is not None:
                sys.stdout.write("T{}.{}.{}\t".format(sentid, verbid, pairid))
                print eventarg_str(sent, 'A:'+arel, verbind, agent_i)
            if prel is not None:
                sys.stdout.write("T{}.{}.{}\t".format(sentid, verbid, pairid))
                print eventarg_str(sent, 'P:'+prel, verbind, patient_i)

def eventarg_str(sent, rel, verbind, argind, suffix=''):
    return ' '.join(str(x) for x in [
        sent['lemma'][verbind],
        clean_sstag(sent['sstag'][verbind]),
        rel, 
        "E{}".format(sent['entities'][argind]), 
        sent['lemma'][argind]
    ])

def process_doc(sents,ents):
    process_sentences(sents)
    for ent in ents:
        process_entity(ent, sents)

def make_shortform(sents,ents):
    entity_summaries(sents,ents)
    for snum,sent in enumerate(sents):
        print "{}\t{}".format(sent['id'], showsent(sent))
        verb_analysis(sent, ents)
        new_stuff(sent, ents, snum)

def main():
    for docid,rows in itertools.groupby(rowgen(), key=lambda r: r[0]):
        sents,ents = parse_docrows(rows)
        # print>>sys.stderr, docid
        print '\n=== DOC', docid, len(sents), len(ents)
        process_doc(sents,ents)
        make_shortform(sents,ents)


main()
