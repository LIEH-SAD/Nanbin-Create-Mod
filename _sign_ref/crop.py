from PIL import Image
import os

out = "_sign_ref/crops"
os.makedirs(out, exist_ok=True)

im1 = Image.open("_sign_ref/train1.png").convert("RGB")
im2 = Image.open("_sign_ref/train2.png").convert("RGB")

def crop(im, name, box, scale=3):
    c = im.crop(box)
    c = c.resize((c.width*scale, c.height*scale), Image.NEAREST)
    p = os.path.join(out, name)
    c.save(p)
    print(p, c.size)

# train1 left: logo + line groups
crop(im1, "t1_left.png", (20, 20, 800, 180), 2)
# train1 center
crop(im1, "t1_center.png", (880, 20, 1160, 180), 3)
# train1 right
crop(im1, "t1_right.png", (1750, 20, 2048, 180), 3)
# train2 left area with loop line name
crop(im2, "t2_left.png", (20, 20, 600, 180), 2)
crop(im2, "t2_loopname.png", (270, 20, 600, 180), 4)
# train2 right
crop(im2, "t2_right.png", (1600, 20, 2048, 180), 3)
