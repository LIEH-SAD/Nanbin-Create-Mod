import numpy as np
from PIL import Image
import sys

def analyze(path):
    im = Image.open(path).convert("RGB")
    a = np.asarray(im).astype(np.int16)
    h, w, _ = a.shape
    print("=" * 60)
    print(path, "size", w, h, "cell(px)=", h)
    nonwhite = (a.min(axis=2) < 240)
    cols = nonwhite.any(axis=0)
    runs = []
    start = None
    for x in range(w):
        if cols[x] and start is None:
            start = x
        elif not cols[x] and start is not None:
            runs.append((start, x - 1)); start = None
    if start is not None:
        runs.append((start, w - 1))
    print("column runs (nonwhite), in h units where h=%d px:" % h)
    for (x0, x1) in runs:
        sub = nonwhite[:, x0:x1 + 1]
        rows = np.where(sub.any(axis=1))[0]
        print("  x %5.3f-%5.3f h (w %5.3f)  y %5.3f-%5.3f h (h %5.3f)" % (
            x0 / h, (x1 + 1) / h, (x1 - x0 + 1) / h,
            rows.min() / h, (rows.max() + 1) / h, (rows.max() - rows.min() + 1) / h))

for f in sys.argv[1:]:
    analyze(f)
