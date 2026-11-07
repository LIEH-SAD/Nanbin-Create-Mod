import numpy as np
from PIL import Image
import json

def analyze(path):
    im = Image.open(path).convert("RGB")
    a = np.asarray(im).astype(np.int16)
    h, w, _ = a.shape
    print("="*70)
    print(path, "size", w, h, "aspect", w/h)
    # top colors
    flat = a.reshape(-1,3)
    colors, counts = np.unique(flat, axis=0, return_counts=True)
    order = np.argsort(-counts)
    print("top colors:")
    for i in order[:8]:
        print("  #%02X%02X%02X" % tuple(colors[i]), counts[i])
    # non-white mask (threshold: any channel < 240)
    nonwhite = (a.min(axis=2) < 240)
    # colored mask: saturation high
    mx = a.max(axis=2); mn = a.min(axis=2)
    colored = (mx - mn) > 40
    dark = (mx < 120) & ~colored
    print("nonwhite px:", nonwhite.sum(), "colored:", colored.sum(), "dark:", dark.sum())
    # column projection of nonwhite
    cols = nonwhite.any(axis=0)
    # find runs
    runs = []
    start = None
    for x in range(w):
        if cols[x] and start is None: start = x
        elif not cols[x] and start is not None:
            runs.append((start, x-1)); start = None
    if start is not None: runs.append((start, w-1))
    print("column runs (nonwhite):")
    for (x0,x1) in runs:
        sub = nonwhite[:, x0:x1+1]
        rows = np.where(sub.any(axis=1))[0]
        print(f"  x {x0:4d}-{x1:4d} width {x1-x0+1:4d}  y {rows.min():3d}-{rows.max():3d} h {rows.max()-rows.min()+1:3d}")
    # colored region bbox
    if colored.any():
        ys, xs = np.where(colored)
        print("colored bbox x %d-%d y %d-%d" % (xs.min(), xs.max(), ys.min(), ys.max()))
        # sample dominant color in colored region
        cr = a[colored]
        cc, cn = np.unique(cr.reshape(-1,3), axis=0, return_counts=True)
        print("  dominant colored:", "#%02X%02X%02X" % tuple(cc[np.argmax(cn)]), cn.max())
    # dark components via simple flood fill (PIL)
    lbl = np.zeros((h,w), dtype=np.int32)
    cur = 0
    from collections import deque
    visited = np.zeros((h,w), dtype=bool)
    comps = []
    for y in range(h):
        for x in range(w):
            if dark[y,x] and not visited[y,x]:
                cur += 1
                q = deque([(y,x)]); visited[y,x]=True
                minx=maxx=x; miny=maxy=y; n=0
                while q:
                    cy,cx = q.popleft(); n+=1
                    if cx<minx: minx=cx
                    if cx>maxx: maxx=cx
                    if cy<miny: miny=cy
                    if cy>maxy: maxy=cy
                    for dy in (-1,0,1):
                        for dx in (-1,0,1):
                            ny,nx = cy+dy, cx+dx
                            if 0<=ny<h and 0<=nx<w and dark[ny,nx] and not visited[ny,nx]:
                                visited[ny,nx]=True; q.append((ny,nx))
                if n > 30:
                    comps.append(dict(x0=int(minx),x1=int(maxx),y0=int(miny),y1=int(maxy),n=int(n)))
    comps.sort(key=lambda c:(c["x0"]))
    print("dark components >30px (by x):", len(comps))
    for c in comps:
        print("  x %4d-%4d (w %3d) y %3d-%3d (h %3d) area %d" % (c["x0"],c["x1"],c["x1"]-c["x0"]+1,c["y0"],c["y1"],c["y1"]-c["y0"]+1,c["n"]))
    return dict(w=w,h=h,comps=comps)

for f in ["_sign_ref/train1.png", "_sign_ref/train2.png"]:
    analyze(f)
