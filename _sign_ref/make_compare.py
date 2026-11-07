import io
from PIL import Image, ImageDraw

ROOT = r"D:/NCM+mtr4.0/Nanbin/_sign_ref"


def load(p):
    return Image.open(p).convert("RGB")


def compose(out_path, rows, labels):
    width = 2048
    gap = 16
    scaled = [img.resize((width, round(img.height * width / img.width)), Image.LANCZOS) for img in rows]
    total_h = sum(i.height for i in scaled) + gap * (len(scaled) - 1) + 28 * len(scaled)
    canvas = Image.new("RGB", (width, total_h), (235, 235, 235))
    draw = ImageDraw.Draw(canvas)
    y = 0
    for label, img in zip(labels, scaled):
        draw.text((8, y + 6), label, fill=(90, 90, 90))
        y += 28
        canvas.paste(img, (0, y))
        y += img.height + gap
    canvas.save(out_path)
    print("saved", out_path, canvas.size)


compose(ROOT + "/out/compare_train1.png",
        [load(ROOT + "/train1.png"), load(ROOT + "/out/final_train1.png")],
        ["REFERENCE (target design)", "FIXED SCRIPT - 11 cells"])

compose(ROOT + "/out/compare_train2.png",
        [load(ROOT + "/train2.png"), load(ROOT + "/out/final_train2.png")],
        ["REFERENCE (target design)", "FIXED SCRIPT - 11 cells"])

compose(ROOT + "/out/compare_before_after.png",
        [load(ROOT + "/out/before_train1.png"), load(ROOT + "/out/final_train1.png")],
        ["BEFORE - route blocks never render, text overflows the sign",
         "AFTER - 11 cells"])

scenarios = [
    ("train1", "3/5 line + station + exit 1"),
    ("train2", "loop line (no number) + exit 9A"),
    ("rich", "3 lines"),
    ("longstation", "long station name"),
    ("empty", "nothing selected"),
]
compose(ROOT + "/out/preview_scenarios.png",
        [load(ROOT + "/out/final_%s.png" % s) for s, _ in scenarios],
        [label for _, label in scenarios])

narrow = [("11 cells", "final_train1.png"), ("7 cells", "final_train1_7.png"),
          ("5 cells", "final_train1_5.png"), ("3 cells", "final_train1_3.png")]
compose(ROOT + "/out/preview_widths.png",
        [load(ROOT + "/out/" + p) for _, p in narrow],
        [n for n, _ in narrow])
