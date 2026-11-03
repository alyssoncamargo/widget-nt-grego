import sys, re, collections
nt_path, xref_path, out_path = sys.argv[1:4]
ob = dict(zip("Matt Mark Luke John Acts Rom 1Cor 2Cor Gal Eph Phil Col 1Thess 2Thess 1Tim 2Tim Titus Phlm Heb Jas 1Pet 2Pet 1John 2John 3John Jude Rev".split(),
 "MAT MRK LUK JHN ACT ROM 1CO 2CO GAL EPH PHP COL 1TH 2TH 1TI 2TI TIT PHM HEB JAS 1PE 2PE 1JN 2JN 3JN JUD REV".split()))
verses=[l.rstrip("\n").split("|",3) for l in open(nt_path,encoding="utf-8")]
key={(b,int(c),int(v)):i for i,(b,c,v,t) in enumerate(verses)}
score=collections.Counter()
def add(ref,w):
    m=re.match(r"(\w+)\.(\d+)\.(\d+)$",ref)
    if m and m.group(1) in ob:
        k=(ob[m.group(1)],int(m.group(2)),int(m.group(3)))
        if k in key: score[key[k]]+=w
for l in open(xref_path,encoding="utf-8"):
    p=l.split("\t")
    if len(p)<3 or not p[2].strip().lstrip("-").isdigit(): continue
    w=int(p[2]); 
    if w<=0: continue
    add(p[0],w)                       # refs leaving the verse
    add(p[1].split("-")[0],w)         # refs arriving at the verse
END=re.compile(r"[.;]\s*[’]?\s*[⟧]?$")
SOFT=re.compile(r"[.;·]\s*[’]?\s*[⟧]?$")
def text(i): return verses[i][3]
def same_ch(a,b): return verses[a][0]==verses[b][0] and verses[a][1]==verses[b][1]
units={}
MAXV, MAXC = 3, 420
for i in range(len(verses)):
    s=i
    while s>0 and same_ch(s-1,i) and not SOFT.search(text(s-1)): s-=1
    e=i
    while e<len(verses)-1 and same_ch(e+1,i) and not END.search(text(e)): e+=1
    if e-s+1>MAXV: continue
    t=" ".join(text(j) for j in range(s,e+1))
    if len(t)>MAXC or len(t)<40: continue
    if not END.search(text(e)): continue
    sc=max(score[j] for j in range(s,e+1))
    units[(s,e)]=sc
ranked=sorted(units.items(), key=lambda x:-x[1])
print("units:",len(units))
K=int(sys.argv[4])
top=ranked[:K]
print("min score in top:",top[-1][1])
bc=collections.Counter(verses[s][0] for (s,e),_ in top); print(bc.most_common())
top.sort()
with open(out_path,"w",encoding="utf-8") as f:
    for (s,e),sc in top: f.write(f"{s}|{e}\n")
