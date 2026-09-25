// 示例脚本：时钟指示牌
// 演示：Rect 背景与进度条、Text 文字、时间接口、state 跨帧状态。
// 适用屏幕：任意（RailwaySign / StationInfo 均可）。

function create(ctx, state, sign) {
    // state 随每块指示牌独立存储，首次渲染前初始化
    state.lastTime = "";
}

function render(ctx, state, sign) {
    var w = sign.getWidth();
    var h = sign.getHeight();

    // 背景
    Rect.create().pos(0, 0).size(w, h).color(0xFFFFFF).draw(ctx);

    // getFormattedTime 返回 Java String，先转成 JS 字符串再参与比较/拼接
    var time = String(sign.getFormattedTime("HH:mm"));
    var date = String(sign.getFormattedTime("yyyy-MM-dd"));
    var seconds = Number(sign.getFormattedTime("ss"));

    // state 只在值变化时写入（这里只是演示用法）
    if (time !== state.lastTime) {
        state.lastTime = time;
    }

    // 主时间：大字加粗居中
    Text.create()
        .pos(0, h * 0.08)
        .size(w, h * 0.52)
        .text(time)
        .color(0x000000)
        .bold()
        .centered()
        .draw(ctx);

    // 日期：小字
    Text.create()
        .pos(0, h * 0.60)
        .size(w, h * 0.30)
        .text(date)
        .color(0x666666)
        .scale(0.7)
        .centered()
        .draw(ctx);

    // 秒进度条
    var barH = h * 0.06;
    var barY = h - barH;
    Rect.create().pos(0, barY).size(w, barH).color(0xDDDDDD).draw(ctx);
    Rect.create().pos(0, barY).size(w * seconds / 60.0, barH).color(0xE60012).draw(ctx);
}

function dispose(ctx, state, sign) {
}
