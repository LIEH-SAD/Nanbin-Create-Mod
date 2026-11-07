from PIL import Image
import numpy as np

im = Image.open("fabric/src/main/resources/assets/nanbin/textures/block/sign/crt_railway.png").convert("RGBA")
a = np.asarray(im)
print("size", im.size, "mode", im.mode)
alpha = a[:,:,3]
ys, xs = np.where(alpha > 8)
print("alpha bbox x %d-%d y %d-%d" % (xs.min(), xs.max(), ys.min(), ys.max()))
# check if black square touches edges
for name, (x0,y0,x1,y1) in {
  "full": (0,0,im.width-1,im.height-1),
}.items():
    pass
# rows/cols coverage of alpha
print("alpha rows min/max:", ys.min(), ys.max(), "of", im.height)
print("alpha cols min/max:", xs.min(), xs.max(), "of", im.width)
# color histogram of opaque
op = a[alpha>200]
colors, counts = np.unique(op[:,:3].reshape(-1,3), axis=0, return_counts=True)
order = np.argsort(-counts)
for i in order[:6]:
    print("#%02X%02X%02X" % tuple(colors[i]), counts[i])
# corner pixels
print("corners:", a[0,0], a[0,-1], a[-1,0], a[-1,-1])
