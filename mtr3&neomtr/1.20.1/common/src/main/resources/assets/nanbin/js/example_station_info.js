// 示例脚本：站台信息屏
// 演示：station / platform / route 三类数据接口，顶部线路色条 + 站名 + 站台号。
// 适用屏幕：platform（站台屏）、station（车站屏）。
//
// 站名里的 "|" 是换行符，所以 "站名|Station Name" 会自动排成中英两行。

function render(ctx, state, sign) {
    var w = sign.getWidth();
    var h = sign.getHeight();

    var colors = sign.getSelectedColors();
    var colorCount = colors.length;
    var stationNames = String(sign.getStationNames());
    var platforms = sign.getPlatformNumbers();

    // 站台号合并成 "1/2" 形式
    var platformText = "";
    for (var i = 0; i < platforms.length; i++) {
        platformText += (i > 0 ? "/" : "") + platforms[i];
    }

    // 背景
    Rect.create().pos(0, 0).size(w, h).color(0xFFFFFF).draw(ctx);

    // 顶部线路色条：每条选中线路占一格，格内写线路编号
    var barH = colorCount > 0 ? h * 0.34 : 0;
    if (colorCount > 0) {
        var barW = w / colorCount;
        for (var c = 0; c < colorCount; c++) {
            var color = colors[c];
            Rect.create().pos(c * barW, 0).size(barW, barH).color(color).draw(ctx);

            var number = String(sign.getRouteNumber(color));
            if (number.length > 0) {
                Text.create()
                    .pos(c * barW, 0)
                    .size(barW, barH)
                    .text(number)
                    .color(0xFFFFFF)
                    .bold()
                    .scale(0.55)
                    .centered()
                    .draw(ctx);
            }
        }
    }

    // 站名（支持 "中文|English" 两行）
    if (stationNames.length > 0) {
        Text.create()
            .pos(0, barH)
            .size(w, h - barH)
            .text(stationNames)
            .color(0x000000)
            .bold()
            .scale(0.85)
            .centered()
            .draw(ctx);
    } else {
        Text.create()
            .pos(0, barH)
            .size(w, h - barH)
            .text("未选择车站|No station selected")
            .color(0x999999)
            .scale(0.6)
            .centered()
            .draw(ctx);
    }

    // 站台号：右下角小字
    if (platformText.length > 0) {
        Text.create()
            .pos(w * 0.55, h * 0.80)
            .size(w * 0.45, h * 0.20)
            .text(platformText)
            .color(0x444444)
            .scale(0.5)
            .centered()
            .draw(ctx);
    }
}

function dispose(ctx, state, sign) {
}
