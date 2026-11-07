import os
from PIL import Image, ImageDraw

ROOT = r"D:/NCM+mtr4.0/Nanbin/_sign_ref"
MOCK = r"C:/Users/Sadli/.dsh/attachments/v1/objects/29/2921594e004660652f2c6dbc6ee7d13ba7fd06583fbbddf935de826cb4e19cf2"

for name in os.listdir(ROOT + "/out"):
    if name.startswith("q_") and name.endswith(".png"):
        target = "final_" + name[2:]
        src = ROOT + "/out/" + name
        dst = ROOT + "/out/" + target
        if os.path.exists(dst):
            os.remove(dst)
        os.rename(src, dst)
        print("rename", name, "->", target)


def load(p):
    return Image.open(p).convert("RGB")


def compose(out_path, rows, labels, width=2048):
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


compose(ROOT + "/out/mock_vs_lianglukou.png",
        [load(MOCK), load(ROOT + "/out/final_lianglukou.png")],
        ["MOCKUP (1 / 3 lines share one 号线/Line, English left-aligned)",
         "NEW SCRIPT - 7 cells"])

cases = [
    ("final_jiangtiao.png", "Jiang Tiao Xian: name drawn as small label (same size as 号线) + English, left-aligned"),
    ("final_nanwu.png", "Nan Wu Hao Xian: big 'Nan Wu Hao Xian'? no - name as small label + 'South Line'"),
    ("final_train2.png", "Huan Xian (loop) -> 'Huan Xian' small + 'Loop Line'"),
    ("final_xian8.png", "8 Xian -> big '8' + 'Xian / Line'"),
    ("final_mixed.png", "3 Hao Xian + Jiang Tiao Xian"),
    ("final_train1.png", "3 Hao Xian + 5 Hao Xian (11 cells)"),
    ("final_branch.png", "Tong Qin 1 Hao Xian branch -> big '1' + stripped name"),
]
compose(ROOT + "/out/preview_name_cases.png",
        [load(ROOT + "/out/" + f) for f, _ in cases],
        [label for _, label in cases])
