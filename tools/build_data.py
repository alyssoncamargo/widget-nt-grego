import sys, os, glob, json, re, unicodedata as ud
import xml.etree.ElementTree as ET
sbl, tbjson, dodson, outdir = sys.argv[1:5]
usfm = {"Mt":"MAT","Mk":"MRK","Lk":"LUK","Jn":"JHN","Ac":"ACT","Ro":"ROM","1Co":"1CO","2Co":"2CO","Ga":"GAL","Eph":"EPH","Php":"PHP","Col":"COL","1Th":"1TH","2Th":"2TH","1Ti":"1TI","2Ti":"2TI","Tit":"TIT","Phm":"PHM","Heb":"HEB","Jas":"JAS","1Pe":"1PE","2Pe":"2PE","1Jn":"1JN","2Jn":"2JN","3Jn":"3JN","Jud":"JUD","Re":"REV"}
order=list(usfm.values())
APP=re.compile(r'[⸀-⹿]')
verses=[]; morph=[]; lemmas=set()
for f in sorted(glob.glob(os.path.join(sbl,"*-morphgnt.txt"))):
    code=usfm[os.path.basename(f).split("-")[1]]
    cur=None; toks=[]
    def flush():
        verses.append((code,cur[0],cur[1],' '.join(t[0] for t in toks)))
        morph.append('\t'.join('|'.join(t) for t in toks))
    for l in open(f,encoding="utf-8"):
        p=l.split(); c=int(p[0][2:4]); v=int(p[0][4:6])
        if cur!=(c,v):
            if cur: flush()
            cur=(c,v); toks=[]
        w=APP.sub('',p[3]); toks.append((w,p[1],p[2],p[6])); lemmas.add(p[6])
    flush()
# TB
tb=json.load(open(tbjson,encoding='utf-8'))
nt=tb[39:]
assert len(nt)==27, len(nt)
tbmap={}
for bi,b in enumerate(nt):
    for ci,ch in enumerate(b['chapters']):
        for vi,t in enumerate(ch): tbmap[(order[bi],ci+1,vi+1)]=t.strip()
tbl=[tbmap.get((b,c,v),'') for b,c,v,_ in verses]
print("TB missing:",sum(1 for x in tbl if not x),"of",len(tbl))
# Lexicon
ns={'t':'http://www.crosswire.org/2008/TEIOSIS/namespace'}
root=ET.parse(dodson).getroot()
def strip(s): return ''.join(ch for ch in ud.normalize('NFD',s) if not ud.combining(ch)).lower()
lex={}; lexs={}
for e in root.iter('{%s}entry'%ns['t']):
    orth=e.find('t:orth',ns).text or ''
    head=ud.normalize('NFC',orth.split(',')[0].strip())
    d=e.find("t:def[@role='brief']",ns); g=(d.text or '').strip() if d is not None else ''
    if not g: continue
    lex.setdefault(head,g); lexs.setdefault(strip(head),g)
out={}; miss=[]
for lm in lemmas:
    n=ud.normalize('NFC',lm)
    g=lex.get(n) or lexs.get(strip(n))
    if g: out[lm]=g
    else: miss.append(lm)
print("lemmas:",len(lemmas),"glossed:",len(out),"missing:",len(miss), miss[:15])
os.makedirs(outdir,exist_ok=True)
open(os.path.join(outdir,'nt.txt'),'w',encoding='utf-8').write('\n'.join(f"{b}|{c}|{v}|{t}" for b,c,v,t in verses)+'\n')
open(os.path.join(outdir,'morph.txt'),'w',encoding='utf-8').write('\n'.join(morph)+'\n')
open(os.path.join(outdir,'tb.txt'),'w',encoding='utf-8').write('\n'.join(tbl)+'\n')
open(os.path.join(outdir,'lex.txt'),'w',encoding='utf-8').write('\n'.join(f"{k}\t{v}" for k,v in sorted(out.items()))+'\n')
