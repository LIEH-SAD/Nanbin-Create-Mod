// 示例脚本：一个脚本同时适配多种屏幕
// 演示：getScreenType() 分流（platform / route / exit / station / custom），
//       以及同一个 js 文件被多种指示牌复用时的写法。
//
// 注意：getScreenType() 返回 Java String，用 String() 转成 JS 字符串后再比较。

function render(ctx, state, sign) {
    var type = String(sign.getScreenType());
    if (type === "route") {
        renderRoute(ctx, sign);
    } else if (type === "exit") {
        renderExit(ctx, sign);
    } else if (type === "platform") {
        renderPlatform(ctx, sign);
    } else if (type === "station") {
        renderStation(ctx, sign);
    } else {
        renderCustom(ctx, sign);
    }
}

function dispose(ctx, state, sign) {
}

// ---------------- 线路屏：线路色块 + 线路名 ----------------

function renderRoute(ctx, sign) {
    var w = sign.getWidth();
    var h = sign.getHeight();
    var colors = sign.getSelectedColors();

    Rect.create().pos(0, 0).size(w, h).color(0xFFFFFF).draw(ctx);

    if (colors.length === 0) {
        drawHint(ctx, sign, "未选择线路|No route selected");
        return;
    }

    // 按选中线路数横向等分，每块一个线路色 + 线路编号
    var cellW = w / colors.length;
    for (var i = 0; i < colors.length; i++) {
        var color = colors[i];
        Rect.create().pos(i * cellW, 0).size(cellW, h).color(color).draw(ctx);
        Text.create()
            .pos(i * cellW, 0)
            .size(cellW, h)
            .text(String(sign.getRouteNumber(color)))
            .color(0xFFFFFF)
            .bold()
            .scale(0.7)
            .centered()
            .draw(ctx);
    }
}

// ---------------- 出口屏：出口编号 + 目的地 ----------------

function renderExit(ctx, sign) {
    var w = sign.getWidth();
    var h = sign.getHeight();
    var count = sign.getExitCount();

    Rect.create().pos(0, 0).size(w, h).color(0xF2F2F2).draw(ctx);

    if (count === 0) {
        drawHint(ctx, sign, "未选择出口|No exits selected");
        return;
    }

    // 左侧竖条 + 右侧每行一个出口
    Rect.create().pos(0, 0).size(w * 0.08, h).color(0x007B3E).draw(ctx);

    var rowH = h / count;
    for (var i = 0; i < count; i++) {
        var destinations = sign.getExitDestinations(i);
        var label = String(sign.getExitNumbers()[i]);
        if (destinations.length > 0) {
            label += "  " + joinArray(destinations, "/");
        }
        Text.create()
            .pos(w * 0.10, i * rowH)
            .size(w * 0.90, rowH)
            .text(label)
            .color(0x000000)
            .scale(0.6)
            .draw(ctx);
    }
}

// ---------------- 站台屏 ----------------

function renderPlatform(ctx, sign) {
    var w = sign.getWidth();
    var h = sign.getHeight();
    var numbers = sign.getPlatformNumbers();

    Rect.create().pos(0, 0).size(w, h).color(0xFFFFFF).draw(ctx);

    if (numbers.length === 0) {
        drawHint(ctx, sign, "未选择站台|No platform selected");
        return;
    }

    Rect.create().pos(0, h * 0.15).size(w, h * 0.70).color(0x007B3E).draw(ctx);

    Text.create()
        .pos(0, h * 0.15)
        .size(w, h * 0.70)
        .text("站台|Platform " + joinArray(numbers, "/"))
        .color(0xFFFFFF)
        .bold()
        .centered()
        .draw(ctx);
}

// ---------------- 车站屏 ----------------

function renderStation(ctx, sign) {
    var w = sign.getWidth();
    var h = sign.getHeight();
    var names = String(sign.getStationNames());

    Rect.create().pos(0, 0).size(w, h).color(0xFFFFFF).draw(ctx);

    if (names.length === 0) {
        drawHint(ctx, sign, "未选择车站|No station selected");
        return;
    }

    // 站名（含 "|" 时自动换行）
    Text.create()
        .pos(0, 0)
        .size(w, h)
        .text(names)
        .color(0x000000)
        .bold()
        .scale(0.85)
        .centered()
        .draw(ctx);
}

// ---------------- 自定义文本屏 / 其它 ----------------

function renderCustom(ctx, sign) {
    var w = sign.getWidth();
    var h = sign.getHeight();
    var text = String(sign.getCustomText());

    Rect.create().pos(0, 0).size(w, h).color(0xFFFFFF).draw(ctx);

    if (text.length === 0) {
        drawHint(ctx, sign, "请在编辑屏输入文本|Enter text in the edit screen");
        return;
    }

    Text.create()
        .pos(0, 0)
        .size(w, h)
        .text(text)
        .color(0x000000)
        .bold()
        .centered()
        .draw(ctx);
}

// ---------------- 公共辅助函数 ----------------

// 数据接口返回的是 Java 数组（String[] / long[]），它没有 Array.prototype 上的方法，
// 不能用 join / map / forEach / slice。这里手工拼接；也可以用 Java.from(arr) 转成 JS 数组。
function joinArray(array, separator) {
    var result = "";
    for (var i = 0; i < array.length; i++) {
        result += (i > 0 ? separator : "") + array[i];
    }
    return result;
}

function drawHint(ctx, sign, text) {
    Text.create()
        .pos(0, 0)
        .size(sign.getWidth(), sign.getHeight())
        .text(text)
        .color(0x999999)
        .scale(0.55)
        .centered()
        .draw(ctx);
}
