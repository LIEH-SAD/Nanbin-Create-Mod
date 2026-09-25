function create(ctx, state, sign) {
}

function render(ctx, state, sign) {
    var names = sign.getStationNames();

    //先画背景
    Rect.create()
        .pos(0, 0)
        .size(sign.getWidth(), sign.getHeight())
        .color(0xFFFFFF)
        .draw(ctx);

    if (names.length > 0) {
        Text.create()
            .pos(0, 0)
            .size(sign.getWidth(), sign.getHeight())
            .text(names)
            .color(0x000000)
            .scale(0.9)
            .bold()
            .centered()
            .draw(ctx);
    } else {
        print("请点编辑按钮选择站台")

        Text.create()
            .pos(0, 0)
            .size(sign.getWidth(), sign.getHeight())
            .text("请点编辑按钮选择站台|Please press the Edit button to continue")
            .color(0x000000)
            .scale(0.9)
            .centered()
            .draw(ctx);
    }
}

function dispose(ctx, state, sign) {
}