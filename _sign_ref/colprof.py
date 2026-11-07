from PIL import Image
import numpy as np

def colprofile(path, x0, x1, y0=20, y1=160, label=""):
    im = Image.open(path).convert("RGB")
    a = np.asarray(im).astype(np.int16)
    sub = a[y0:y1, x0:x1]
    dark = (sub.max(axis=2) < 120)
    cols = dark.any(axis=0)
    runs = []
    s = None
    for i, v in enumerate(cols):
        if v and s is None: s = i
        elif not v and s is not None:
            runs.append((x0+s, x0+i-1)); s = None
    if s is not None: runs.append((x0+s, x0+len(cols)-1))
    print(f"--- {path} {label} region x{x0}-{x1} y{y0}-{y1}")
    for (a0, a1) in runs:
        rsub = dark[:, a0-x0:a1-x0+1]
        rows = np.where(rsub.any(axis=1))[0]
        print(f"  x {a0}-{a1} (w {a1-a0+1}) y {y0+rows.min()}-{y0+rows.max()} (h {rows.max()-rows.min()+1})")

# train2 loop name area
colprofile("_sign_ref/train2.png", 280, 580, 20, 160, "loop name")
# train1 line3 label area
colprofile("_sign_ref/train1.png", 330, 470, 20, 160, "line3 label")
# train1 station center
colprofile("_sign_ref/train1.png", 900, 1150, 20, 165, "station")
# train1 exit
colprofile("_sign_ref/train1.png", 1760, 2040, 20, 165, "exit")
# train2 exit
colprofile("_sign_ref/train2.png", 1600, 2040, 20, 165, "exit2")
# numbers
colprofile("_sign_ref/train1.png", 240, 335, 20, 165, "num3")
colprofile("_sign_ref/train2.png", 285, 420, 20, 165, "loopbig")
