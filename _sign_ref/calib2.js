// calibration v2: elements not clipped at top, known boxes
function create(ctx, state, sign) {
}

function render(ctx, state, sign) {
    var w = sign.getWidth();
    var h = sign.getHeight();
    Rect.create().pos(0, 0).size(w, h).color(0xFFFFFF).draw(ctx);
    // design px -> world: world = designPx * h / 192
    var k = h / 192;
    var bx = 100 * k;      // box x
    var by = 40 * k;       // box y
    var bw = w - 200 * k;  // box width
    var B = 150 * k;       // box height (150 design px)

    Text.create().pos(bx, by).size(bw, B).text("3").color(0x000000).centered(false).draw(ctx);
    Text.create().pos(bx, by).size(bw, B).text("9A").color(0x000000).centered(false).draw(ctx);
    Text.create().pos(bx, by).size(bw, B).text("1").color(0x000000).centered(false).draw(ctx);
    Text.create().pos(bx, by).size(bw, B).text("10").color(0x000000).centered(false).draw(ctx);
    Text.create().pos(bx, by).size(bw, B).text("号线").color(0x000000).centered(false).draw(ctx);
    Text.create().pos(bx, by).size(bw, B).text("出入口").color(0x000000).centered(false).draw(ctx);
    Text.create().pos(bx, by).size(bw, B).text("环").color(0x000000).centered(false).draw(ctx);
    Text.create().pos(bx, by).size(bw, B).text("线").color(0x000000).centered(false).draw(ctx);
    Text.create().pos(bx, by).size(bw, B).text("Line").color(0x000000).centered(false).draw(ctx);
    Text.create().pos(bx, by).size(bw, B).text("Entrance").color(0x000000).centered(false).draw(ctx);
    Text.create().pos(bx, by).size(bw, B).text("Loop Line").color(0x000000).centered(false).draw(ctx);
    Text.create().pos(bx, by).size(bw, B).text("Station").color(0x000000).bold().centered(false).draw(ctx);
    Text.create().pos(bx, by).size(bw, B).text("地铁站").color(0x000000).bold().centered(false).draw(ctx);
    Text.create().pos(bx, by).size(bw, B).text("地铁站||Station").color(0x000000).bold().centered(false).draw(ctx);
    Text.create().pos(bx, by).size(bw, B).text("小什字||Xiaoshizi").color(0x000000).bold().centered(false).draw(ctx);
    Text.create().pos(bx, by).size(bw, B).text("3").color(0x000000).centered(true).draw(ctx);
    Text.create().pos(bx, by).size(bw, B).text("地铁站||Station").color(0x000000).bold().centered(true).draw(ctx);
    Text.create().pos(bx, by).size(bw, B).text("Xiaoshizi").color(0x000000).bold().centered(true).draw(ctx);
    Text.create().pos(bx, by).size(bw, B).text("3号线").color(0x000000).centered(false).draw(ctx);
}

function dispose(ctx, state, sign) {
}
