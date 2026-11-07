// calibration: measure ink boxes / ratios of each text style
function create(ctx, state, sign) {
}

function render(ctx, state, sign) {
    var w = sign.getWidth();
    var h = sign.getHeight();
    var B = h * 0.9;
    Rect.create().pos(0, 0).size(w, h).color(0xFFFFFF).draw(ctx);

    Text.create().pos(0, 0).size(w, B).text("3").color(0x000000).centered(false).draw(ctx);
    Text.create().pos(0, 0).size(w, B).text("9A").color(0x000000).centered(false).draw(ctx);
    Text.create().pos(0, 0).size(w, B).text("号线").color(0x000000).centered(false).draw(ctx);
    Text.create().pos(0, 0).size(w, B).text("出入口").color(0x000000).centered(false).draw(ctx);
    Text.create().pos(0, 0).size(w, B).text("环").color(0x000000).centered(false).draw(ctx);
    Text.create().pos(0, 0).size(w, B).text("Line").color(0x000000).centered(false).draw(ctx);
    Text.create().pos(0, 0).size(w, B).text("Entrance").color(0x000000).centered(false).draw(ctx);
    Text.create().pos(0, 0).size(w, B).text("Station").color(0x000000).bold().centered(false).draw(ctx);
    Text.create().pos(0, 0).size(w, B).text("地铁站").color(0x000000).bold().centered(false).draw(ctx);
    Text.create().pos(0, 0).size(w, B).text("地铁站||Station").color(0x000000).bold().centered(false).draw(ctx);
    Text.create().pos(0, 0).size(w, B).text("号线|Line").color(0x000000).centered(false).draw(ctx);
    Text.create().pos(0, 0).size(w, B).text("Station").color(0x000000).bold().centered(true).draw(ctx);
    Text.create().pos(0, 0).size(w, B).text("地铁站||Station").color(0x000000).bold().centered(true).draw(ctx);
}

function dispose(ctx, state, sign) {
}
