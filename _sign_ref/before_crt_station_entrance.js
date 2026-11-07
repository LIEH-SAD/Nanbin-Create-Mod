// 原脚本（修复前，保留用于对比 —— 线路块因数组+1不生效而完全不绘制）
function create(ctx, state, sign) {
}

function render(ctx, state, sign) {
    var names = sign.getStationNames();
    let a = sign.getRouteNumbers() + 1;
    let e = sign.getCustomText();

    let w = sign.getWidth();
    let h = sign.getHeight();
    let rectH = h * 1 / 5;
    let rectW = h * 4 / 5;

    // 背景
    Rect.create()
        .pos(0, 0)
        .size(w, h)
        .color(0xFFFFFF)
        .draw(ctx);

    // 居中文字
    Text.create()
        .pos(0, 0)
        .size(w, h)
        .text(names)
        .color(0x000000)
        .scale(0.9)
        .bold()
        .centered()
        .draw(ctx);

    // 元素绘制
    Texture.create()
        .texture("nanbin:textures/block/sign/crt_railway.png")
        .pos(h / 5, h / 5)
        .size(h * 4 / 5, h * 4 / 5)
        .draw(ctx);

    // 递归内容

    for (let i = 0 ;i < a; i++) {
        Rect.create()
            .pos(h + i * h, h - rectH)
            .size(rectW, rectH)
            .color(sign.getRouteColor(i))
            .draw(ctx);

        Text.create()
            .pos(h + (i * h) + rectW, h - rectH)
            .size(w - rectW, rectH)
            .text(sign.getRouteNumber(i))
            .color(0x000000)
            .scale(0.8)
            .bold()
            .draw(ctx);
    }

    Text.create()
        .pos(h + (a-1) * h, 0)
        .size(h, h)
        .text("号线|Line")
        .color(0x000000)
        .scale(0.8)
        .draw(ctx);

}

function dispose(ctx, state, sign) {
}
