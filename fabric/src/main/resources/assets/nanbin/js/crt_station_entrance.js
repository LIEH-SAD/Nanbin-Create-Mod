// 重庆轨道交通 入站指示牌（JS 全局样式）
//
// 布局（从左到右）：CRT 图标 | 各线路“色条 + 编号” | 共用的“号线 / Line” | 居中站名 | 出口编号 + “出入口 / Entrance”
//
// 多线路时只在最后一个编号后面画一次“号线 / Line”（参考 1/3 号线并排的入站牌）：
//   [红条] 1  [蓝条] 3  号线/Line   两路口   6 出入口/Entrance
//
// 线路编号 / 小字的取法和 RenderCRTRailwaySign 的 crt_route_name（V2）保持同一套逻辑：
//   - 大号编号：优先 getRouteNumber(color)（即服务端写入的线路编号）；
//     编号为空时用 deriveNumberFromName()：取名称 “||” 之前、“|” 之前的中文段（如“江跳线”“环线”）。
//   - 只有“纯数字编号”才画成大字；名称当编号的线路（江跳线 / 环线 / xx支线）不放大，
//     直接按小字样式画“名称 + 英文”，和“号线 / Line”一样大。
//   - 共用小字：stripNumberFromName()：把名称按 “|” 分段，逐段删掉编号字符串、再删掉所有数字、去空格，
//     非空段用 “|” 连起来（“1号线|Line 1” + 编号 1 → “号线|Line”）。
//   - 所以 “1号线 / 8线 / 江跳线 / 南五号线 / 环线 / xx支线” 都由同一套规则得到，不再按后缀写死。
//   - 中英文成对的文字（号线/Line、出入口/Entrance、名称/英文）按左对齐画（两行左边线一致），
//     不再每行各自居中；站名因为整块居中，仍保持每行居中。
//
// 所有尺寸以格高 h 为单位；牌面不够宽时整套布局等比缩小，任意格数都不会画到牌外。
//
// 注意：JS 文本 API 的字号 = box 高度 × scale，而墨迹在 box 内并不居中（单行尤其明显），
// 所以下面 REL_* 常量是各文字样式在 box 高 = h、scale = 1 时实测的墨迹位置/高度比例，
// 配合 drawInk()/drawInkLeft() 才能把文字精确摆到位。
// 这些比例与 MTR 客户端“动态贴图分辨率”有关，按默认值 3（512px，scale=1.0）标定。

// ===== 布局常量（单位：h） =====
var LOGO_SIZE = 0.706;      // CRT 图标绘制边长（贴图四周自带留白，按墨迹 0.6h 反推）
var LOGO_CY = 0.500;        // 图标垂直中心

var BAR_W = 0.20;           // 线路色条宽
var BAR_CY = 0.60;          // 线路色条垂直中心（顶 0.2h、底 1.0h）
var BAR_H = 0.80;
var GAP = 0.115;            // 线路色条 / 编号 / 小字之间的间距

var BIG_INK_H = 0.530;      // 大号编号（数字）字高
var BIG_MAX_W = 0.62;       // 大号数字编号最大宽度（超出则整体缩小）
var BIG_BOLD = true;        // 数字编号加粗（对应 V2 用 Source Sans 渲染数字）

var LABEL_INK_H = 0.430;    // 共用小字“号线 / Line”整体字高
var LABEL_CN_INK_H = 0.260; // 只有中文后缀时（如“号线”）的字高
var LABEL_EN_INK_H = 0.160; // 只有英文小字时的字高
var LABEL_MAX_W = 2.20;     // 共用小字最大宽度

var STATION_INK_H = 0.580;  // 站名整体（中文+英文）字高
var STATION_MAX_RATIO = 0.62; // 站名最多占牌面宽度的比例
var STATION_MIN = 1.00;     // 排布时为站名预留的最小宽度

var EXIT_W = 1.95;          // 出口区域占用的宽度（含外边距）
var EXIT_BIG_INK_H = 0.530; // 出口编号字高
var EXIT_BIG_MAX_W = 0.90;  // 出口编号最大宽度
var EXIT_SUB_INK_H = 0.430; // “出入口 / Entrance”整体字高
var EXIT_GAP = 0.16;        // 出口编号与小字之间的间距

var CY_BIG = 0.495;         // 大号编号 / 出口编号的垂直中心
var CY_LABEL = 0.440;       // 共用小字（号线/Line）的垂直中心
var CY_LABEL_CN = 0.350;    // 只有中文后缀时的垂直中心
var CY_LABEL_EN = 0.500;    // 只有英文小字时的垂直中心
var CY_EXIT_SUB = 0.440;    // “出入口/Entrance”垂直中心

// Text.measure 返回的宽度含文字贴图的内边距，比实际墨迹宽；下面按文字类型换算成墨迹宽度。
var K_SINGLE_1 = 0.60;      // 单行、1 个字符（数字等）
var K_SINGLE_CJK = 0.81;    // 单行、1 个汉字
var K_SINGLE_CJK_MULTI = 0.95; // 单行、多个汉字
var K_SINGLE_LONG = 0.85;   // 单行、多个字符（英文等）
var K_MULTI = 0.90;         // 中文|English 两行文字

// ===== 各文字样式的墨迹比例（box 高 = h、scale = 1 时实测） =====
var REL_DIGIT = { top: 0.0000, h: 0.5295 };   // 阿拉伯数字
var REL_DIGIT_BOLD = { top: 0.0260, h: 0.5556 }; // 阿拉伯数字（加粗）
var REL_CJK = { top: 0.2257, h: 0.5990 };     // 单个汉字
var REL_SUB = { top: 0.1302, h: 0.6250 };     // 中文|English 两行小字
var REL_SUB_DESC = { top: 0.1302, h: 0.6684 };// 同上，但英文带 g/j/p/q/y 下伸笔画
var REL_STATION = { top: 0.1476, h: 0.6337 }; // 粗体站名（中文|English）
var REL_STATION_DESC = { top: 0.1476, h: 0.6858 }; // 同上，但英文带下伸笔画
var REL_EXIT_SUB = { top: 0.1215, h: 0.6337 };// 出入口|Entrance
var REL_HINT_CJK = { top: 0.1736, h: 0.6597 };// 提示文字：单行中文
var REL_HINT_EN = { top: -0.0434, h: 0.7031 };// 提示文字 / 单行英文

function create(ctx, state, sign) {
}

function render(ctx, state, sign) {
    var w = sign.getWidth();
    var h = sign.getHeight();

    // 背景
    Rect.create()
        .pos(0, 0)
        .size(w, h)
        .color(0xFFFFFF)
        .draw(ctx);

    var colors = sign.getSelectedColors();
    var names = String(sign.getStationNames());
    var exitNumber = pickExitNumber(sign.getExitNumbers());

    // 解析所有线路（编号 / 后缀 / 英文）
    var infos = [];
    for (var c = 0; c < colors.length; c++) {
        infos.push(routeInfo(sign, colors[c]));
    }

    // 什么都没选：提示
    if (infos.length === 0 && names.length === 0 && exitNumber.length === 0) {
        drawInk(ctx, "请点编辑按钮选择站台", w / 2, 0.40 * h, 0.20 * h, REL_HINT_CJK, false, w * 0.92, 0x999999);
        drawInk(ctx, "Please press the Edit button to continue", w / 2, 0.62 * h, 0.10 * h, REL_HINT_EN, false, w * 0.92, 0x999999);
        return;
    }

    // 先估算线路组需要多宽（单位 h），牌面不够宽时整套等比缩小
    var groupW = estimateGroupWidth(infos);
    var k = h * layoutScale(w, h, groupW);

    // 1. 左侧 CRT 图标（占第 1 格）
    var logoS = LOGO_SIZE * k;
    Texture.create()
        .texture("nanbin:textures/block/sign/crt_railway.png")
        .pos(0.5 * k - logoS / 2, vy(LOGO_CY, h, k) - logoS / 2)
        .size(logoS, logoS)
        .draw(ctx);

    // 2. 线路：数字编号用大字；没有数字编号（江跳线/环线/xx支线）直接把名称当"小字"画，
//    和"号线 / Line"同样大小；最后的共用小字只在最后一条是数字编号时才补
    var x = k;
    var lastInfo = null;
    for (var i = 0; i < infos.length; i++) {
        var info = infos[i];
        lastInfo = info;

        Rect.create()
            .pos(x, vy(BAR_CY, h, k) - BAR_H * k / 2)
            .size(BAR_W * k, BAR_H * k)
            .color(colors[i])
            .draw(ctx);
        x += (BAR_W + GAP) * k;

        if (info.numeric) {
            var idW = drawRouteId(ctx, info, x, h, k);
            if (idW > 0) {
                x += idW + GAP * k;
            }
        } else {
            // 名称当编号：不放大，按“小字”样式左对齐画（名称 + 英文）
            var nameLabel = routeLabel(routePairText(info));
            if (nameLabel.text.length > 0) {
                x += drawRouteLabel(ctx, nameLabel, x, h, k) + GAP * k;
            }
        }
    }
    if (lastInfo != null && lastInfo.numeric) {
        var sharedLabel = routeLabel(lastInfo.text);
        if (sharedLabel.text.length > 0) {
            x += drawRouteLabel(ctx, sharedLabel, x, h, k);
        }
    }
    var routesEnd = x;

    // 3. 站名：默认整牌居中；线路组太长时贴着线路组右边放
    if (names.length > 0) {
        var stationRel = hasDescender(names) ? REL_STATION_DESC : REL_STATION;
        var exitLeft = w - EXIT_W * k;
        var available = Math.max(0.3 * k, exitLeft - routesEnd - 0.12 * k);
        var maxW = Math.min(w * STATION_MAX_RATIO, available * 0.98);
        var stationInkH = STATION_INK_H * k;
        var stationW = measureInk(names, stationInkH, stationRel, false);
        if (stationW > maxW) {
            stationInkH = stationInkH * maxW / stationW;
            stationW = maxW;
        }
        var stationCx = w / 2;
        // 窄牌面（线路组之后剩余空间不多）时，把站名放在剩余空间中间，避免挤在一边
        if (available < 2.2 * k) {
            var gapCenter = (routesEnd + exitLeft) / 2;
            if (gapCenter - stationW / 2 > routesEnd + 0.10 * k && gapCenter + stationW / 2 < exitLeft - 0.10 * k) {
                stationCx = gapCenter;
            }
        }
        stationCx = Math.max(stationCx, routesEnd + 0.12 * k + stationW / 2);
        drawInk(ctx, names, stationCx, 0.5 * h, stationInkH, stationRel, true, 0);
    }

    // 4. 右侧出口
    if (exitNumber.length > 0) {
        drawExit(ctx, w, h, k, exitNumber);
    }
}

// 解析线路名 → 编号 / 名称 / 英文。
// 编号与小字的取法移植自 RenderCRTRailwaySign：
//   deriveNumberFromName(名称) 取 “||” 之前、“|” 之前的中文段；
//   stripNumberFromName(名称, 编号) 逐段删编号字符串、删数字、去空格，非空段用 “|” 拼接。
function routeInfo(sign, color) {
    var raw = trimStr(String(sign.getRouteName(color)));
    var enApi = trimStr(String(sign.getRouteName(color, true)));

    // 先去掉 “||其它”，再按单个 “|” 分中英文（兼容 JS API 已按 “||” 拆过的情况）
    var visible = raw;
    var hide = visible.indexOf("||");
    if (hide >= 0) {
        visible = visible.substring(0, hide);
    }
    var cjk = visible;
    var en = "";
    var sep = visible.indexOf("|");
    if (sep >= 0) {
        cjk = visible.substring(0, sep);
        en = visible.substring(sep + 1);
    } else if (enApi.length > 0 && enApi !== raw) {
        en = enApi;
    }
    cjk = trimStr(cjk);
    en = trimStr(en);

    // 编号：优先服务端编号，空则退回名称中文段（deriveNumberFromName）
    var number = trimStr(String(sign.getRouteNumber(color)));
    if (number.length === 0) {
        number = cjk;
    }

    // 小字：剥掉编号字符串和所有数字（stripNumberFromName）
    var text = stripNumberFromName(cjk, en, number);

    return { id: number, text: text, numeric: isNumeric(number), cjk: cjk, en: en };
}

// 名称当编号时，小字 = 名称 + 英文（和“号线 / Line”一样大，不再把名称放大）
function routePairText(info) {
    if (info.cjk.length > 0 && info.en.length > 0) {
        return info.cjk + "|" + info.en;
    }
    if (info.cjk.length > 0) {
        return info.cjk;
    }
    return info.en;
}

// 对应 RenderCRTRailwaySign#stripNumberFromName：名称按 “|” 分段，逐段删编号、删数字
function stripNumberFromName(cjk, en, number) {
    var numberSegments = number.length > 0 ? number.split("|") : [];
    var segments = [cjk, en];
    var parts = [];
    for (var i = 0; i < segments.length; i++) {
        var stripped = trimStr(segments[i]);
        if (stripped.length === 0) {
            continue;
        }
        for (var n = 0; n < numberSegments.length; n++) {
            var numSeg = numberSegments[n];
            if (numSeg.length > 0) {
                stripped = replaceStr(stripped, numSeg, "");
            }
        }
        stripped = trimStr(removeDigits(stripped));
        if (stripped.length > 0) {
            parts.push(stripped);
        }
    }
    var out = "";
    for (var p = 0; p < parts.length; p++) {
        out += (p > 0 ? "|" : "") + parts[p];
    }
    return out;
}

// 小字样式：可能是 “中文|English” 两行，也可能只剩中文或英文一行
function routeLabel(text) {
    if (text.length === 0) {
        return { text: "", kind: 0, rel: REL_SUB };
    }
    var pipe = text.indexOf("|");
    if (pipe >= 0) {
        var enPart = text.substring(pipe + 1);
        return { text: text, kind: 0, rel: hasDescender(enPart) ? REL_SUB_DESC : REL_SUB };
    }
    if (isCjk(text)) {
        return { text: text, kind: 1, rel: REL_CJK };
    }
    return { text: text, kind: 2, rel: REL_HINT_EN };
}

// 估算线路组总宽度（单位 h）
function estimateGroupWidth(infos) {
    var width = 0;
    for (var i = 0; i < infos.length; i++) {
        var info = infos[i];
        if (info.numeric) {
            width += BAR_W + GAP + idWidthInH(info) + GAP;
        } else {
            width += BAR_W + GAP + labelWidthInH(routeLabel(routePairText(info))) + GAP;
        }
    }
    if (infos.length > 0) {
        var last = infos[infos.length - 1];
        if (last.numeric) {
            width += labelWidthInH(routeLabel(last.text));
        }
    }
    return width;
}

function idWidthInH(info) {
    if (info.id.length === 0) {
        return 0;
    }
    return Math.min(measureInk(info.id, BIG_INK_H, digitRel(), true), BIG_MAX_W);
}

// 数字用的墨迹比例（V2 里数字用 Source Sans、名称用 Alibaba，这里用加粗/常规区分）
function digitRel() {
    return BIG_BOLD ? REL_DIGIT_BOLD : REL_DIGIT;
}

function labelWidthInH(label) {
    if (label.text.length === 0) {
        return 0;
    }
    var inkH = label.kind === 0 ? LABEL_INK_H : (label.kind === 1 ? LABEL_CN_INK_H : LABEL_EN_INK_H);
    return Math.min(measureInk(label.text, inkH, label.rel, label.kind === 2), LABEL_MAX_W);
}

// 画数字编号（大字），返回占用宽度（world 单位）
function drawRouteId(ctx, info, x, h, k) {
    if (info.id.length === 0) {
        return 0;
    }
    return drawFlowItemLeft(ctx, info.id, x, vy(CY_BIG, h, k),
        BIG_INK_H * k, digitRel(), BIG_MAX_W * k, BIG_BOLD);
}

// 画小字（左对齐：中文和英文都从同一条左边线开始），返回占用宽度（world 单位）
function drawRouteLabel(ctx, label, x, h, k) {
    if (label.text.length === 0) {
        return 0;
    }
    if (label.kind === 0) {
        return drawFlowItemLeft(ctx, label.text, x, vy(CY_LABEL, h, k),
            LABEL_INK_H * k, label.rel, LABEL_MAX_W * k);
    }
    if (label.kind === 1) {
        return drawFlowItemLeft(ctx, label.text, x, vy(CY_LABEL_CN, h, k),
            LABEL_CN_INK_H * k, label.rel, LABEL_MAX_W * k);
    }
    return drawFlowItemLeft(ctx, label.text, x, vy(CY_LABEL_EN, h, k),
        LABEL_EN_INK_H * k, label.rel, LABEL_MAX_W * k);
}

// 从 getExitNumbers() 里挑出真正的出口编号。
// 该接口会把 selectedIds 里的每一项都按“出口 ID”反序列化，站台 / 线路颜色等
// 其它数据的 ID 也会被解成乱码字符串，所以这里过滤掉含控制字符 / 长度异常的项。
function pickExitNumber(exits) {
    for (var i = 0; i < exits.length; i++) {
        var name = String(exits[i]);
        if (isExitName(name)) {
            return name;
        }
    }
    return "";
}

function isExitName(name) {
    if (name.length === 0 || name.length > 8) {
        return false;
    }
    for (var i = 0; i < name.length; i++) {
        var code = name.charCodeAt(i);
        if (!(code >= 32 && code < 127) && code < 0x2E80) {
            return false;
        }
    }
    return true;
}

// 牌面需要的总宽度（h 为单位）；不够则返回整体缩放系数
function layoutScale(w, h, groupW) {
    if (h <= 0) {
        return 1;
    }
    var need = (1.0 + groupW + STATION_MIN + EXIT_W) * h;
    return need > w ? w / need : 1.0;
}

// 垂直位置：base 为按 h（0~1）定义的中心，缩放时保持整体垂直居中
function vy(base, h, k) {
    return 0.5 * h + (base - 0.5) * k;
}

// 右侧出口：编号 + “出入口 / Entrance”（小字左对齐，靠右摆放）
function drawExit(ctx, w, h, k, number) {
    var subInkW = measureInk("出入口|Entrance", EXIT_SUB_INK_H * k, REL_EXIT_SUB, false);
    var subLeft = w - 0.20 * k - subInkW;
    drawInkLeft(ctx, "出入口|Entrance", subLeft, vy(CY_EXIT_SUB, h, k),
        EXIT_SUB_INK_H * k, REL_EXIT_SUB, false, 0);

    var bigInkH = EXIT_BIG_INK_H * k;
    var bigW = measureInk(number, bigInkH, digitRel(), true);
    if (bigW > EXIT_BIG_MAX_W * k) {
        bigInkH = bigInkH * EXIT_BIG_MAX_W * k / bigW;
        bigW = EXIT_BIG_MAX_W * k;
    }
    var bigCx = subLeft - EXIT_GAP * k - bigW / 2;
    drawInk(ctx, number, bigCx, vy(CY_BIG, h, k), bigInkH, digitRel(), true, 0);
}

// 从左到右摆放一段文字（左对齐：中文行/英文行都从 x 开始），返回估算墨迹宽度
function drawFlowItemLeft(ctx, text, x, cy, inkH, rel, maxW, bold) {
    if (text.length === 0 || inkH <= 0) {
        return 0;
    }
    var singleLine = text.indexOf("|") < 0;
    var inkW = measureInk(text, inkH, rel, singleLine);
    if (maxW > 0 && inkW > maxW) {
        inkH = inkH * maxW / inkW;
        inkW = maxW;
    }
    drawInkLeft(ctx, text, x, cy, inkH, rel, bold, 0);
    return inkW;
}

// 左对齐绘制：box 左端 = 墨迹左端，多行文字每一行都从同一条左边线开始
function drawInkLeft(ctx, text, x, cy, inkH, rel, bold, maxW, color) {
    if (text.length === 0 || inkH <= 0) {
        return 0;
    }

    var baseH = 0.5;
    var singleLine = text.indexOf("|") < 0;
    var inkW = measureInk(text, inkH, rel, singleLine);
    if (maxW > 0 && inkW > maxW) {
        inkH = inkH * maxW / inkW;
        inkW = maxW;
    }

    var boxH = inkH / rel.h;
    var boxW = Math.max(Text.measure(text, baseH) * boxH / baseH, inkH) * 1.04;
    var boxY = cy - (rel.top + rel.h / 2) * boxH;

    var builder = Text.create()
        .pos(x, boxY)
        .size(boxW, boxH)
        .text(text)
        .color(color === undefined ? 0x000000 : color)
        .scale(1.0);
    if (bold) {
        builder.bold();
    }
    builder.centered(false);
    builder.draw(ctx);

    return inkW;
}

// 估算墨迹宽度（world 单位）：box 高 = inkH / rel.h
// 若 inkH 以 h 为单位传入，返回值也是 h 单位（用于排布预估）。
function measureInk(text, inkH, rel, singleLine) {
    var baseH = 0.5;
    var boxH = inkH / rel.h;
    var est = Text.measure(text, baseH) * boxH / baseH;
    var k;
    if (!singleLine) {
        k = K_MULTI;
    } else if (text.length === 1) {
        k = isCjk(text) ? K_SINGLE_CJK : K_SINGLE_1;
    } else if (isCjk(text)) {
        k = K_SINGLE_CJK_MULTI;
    } else {
        k = K_SINGLE_LONG;
    }
    return Math.max(est * k, inkH * 0.5);
}

// 把文字按“墨迹中心 (cx, cy) + 墨迹高度 inkH”精确摆放；maxW > 0 时超出会整体缩小。
// 返回摆放后估算的墨迹宽度。
function drawInk(ctx, text, cx, cy, inkH, rel, bold, maxW, color) {
    if (text.length === 0 || inkH <= 0) {
        return 0;
    }

    var baseH = 0.5;
    var singleLine = text.indexOf("|") < 0;
    var inkW = measureInk(text, inkH, rel, singleLine);
    if (maxW > 0 && inkW > maxW) {
        inkH = inkH * maxW / inkW;
        inkW = maxW;
    }

    var boxH = inkH / rel.h;
    var boxW = Math.max(Text.measure(text, baseH) * boxH / baseH, inkH) * 1.04;
    var boxX = cx - boxW / 2;
    var boxY = cy - (rel.top + rel.h / 2) * boxH;

    var builder = Text.create()
        .pos(boxX, boxY)
        .size(boxW, boxH)
        .text(text)
        .color(color === undefined ? 0x000000 : color)
        .scale(1.0);
    if (bold) {
        builder.bold();
    }
    builder.centered();
    builder.draw(ctx);

    return inkW;
}

// ===== 小工具（Nashorn 里不要用 ES6+ 的 startsWith/endsWith/trim 等） =====

function isCjk(text) {
    return text.length > 0 && text.charCodeAt(0) >= 0x2E80;
}

// 纯数字（允许 “/” 分隔），对应 RenderCRTRailwaySign#isNumeric
function isNumeric(text) {
    if (text.length === 0) {
        return false;
    }
    var hasDigit = false;
    for (var i = 0; i < text.length; i++) {
        var c = text.charAt(i);
        if (isDigitChar(c)) {
            hasDigit = true;
        } else if (c !== "/") {
            return false;
        }
    }
    return hasDigit;
}

function isDigitChar(c) {
    return c >= "0" && c <= "9";
}

function removeDigits(text) {
    var out = "";
    for (var i = 0; i < text.length; i++) {
        var c = text.charAt(i);
        if (!isDigitChar(c)) {
            out += c;
        }
    }
    return out;
}

// 字符串替换（Nashorn 里没有 replaceAll(String, String)，自己实现）
function replaceStr(text, target, replacement) {
    var out = "";
    var i = 0;
    while (i < text.length) {
        if (target.length > 0 && text.substring(i, i + target.length) === target) {
            out += replacement;
            i += target.length;
        } else {
            out += text.charAt(i);
            i++;
        }
    }
    return out;
}

// 英文里带下伸笔画的字母（g/j/p/q/y）会让墨迹整体变高
function hasDescender(text) {
    for (var i = 0; i < text.length; i++) {
        var c = text.charAt(i);
        if (c === "g" || c === "j" || c === "p" || c === "q" || c === "y") {
            return true;
        }
    }
    return false;
}

function trimStr(text) {
    var start = 0;
    var end = text.length;
    while (start < end && text.charCodeAt(start) <= 32) {
        start++;
    }
    while (end > start && text.charCodeAt(end - 1) <= 32) {
        end--;
    }
    return text.substring(start, end);
}

function dispose(ctx, state, sign) {
}
