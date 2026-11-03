import sys, os, glob
src, out = sys.argv[1], sys.argv[2]
usfm = {"Mt":"MAT","Mk":"MRK","Lk":"LUK","Jn":"JHN","Ac":"ACT","Ro":"ROM","1Co":"1CO","2Co":"2CO","Ga":"GAL","Eph":"EPH","Php":"PHP","Col":"COL","1Th":"1TH","2Th":"2TH","1Ti":"1TI","2Ti":"2TI","Tit":"TIT","Phm":"PHM","Heb":"HEB","Jas":"JAS","1Pe":"1PE","2Pe":"2PE","1Jn":"1JN","2Jn":"2JN","3Jn":"3JN","Jud":"JUD","Re":"REV"}
lines=[]
for f in sorted(glob.glob(os.path.join(src,"*-morphgnt.txt"))):
    code=usfm[os.path.basename(f).split("-")[1]]
    cur=None; words=[]
    for l in open(f,encoding="utf-8"):
        p=l.split()
        ref=p[0]; c=int(ref[2:4]); v=int(ref[4:6])
        if cur!=(c,v):
            if cur: lines.append(f"{code}|{cur[0]}|{cur[1]}|{' '.join(words)}")
            cur=(c,v); words=[]
        words.append(p[3])
    lines.append(f"{code}|{cur[0]}|{cur[1]}|{' '.join(words)}")
open(out,"w",encoding="utf-8").write("\n".join(lines)+"\n")
print(len(lines))
