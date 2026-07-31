include(Resources.id("nanbin:pids/scripts/crt_pids_1.js"));
include(Resources.id("jsblock:scripts/pids_util.js")); 

function create(ctx, state, pids) {
    print("Hello World ^^");
}
function render(ctx, state, pids) {
    //定义类
    let Title = "重庆轨道交通集团"
    let firstColText1 = "本次列车：|This Train:"
    let secondColText1 = "开往："
    let thirdColText1 = "下次列车：|Next Train:"
    let min_zh = "分钟"
    let min_en = "min"
    let firstColpos = 4
    let secondColpos = 30
    let thirdColpos = 75
    let firstRowpos = 12
    let secondRowpos = 19
    let ftime = Math.ceil((pids.arrivals().get(0).arrivalTime() - Date.now()) / 60000);
    let reftime = 0
    let stime = Math.ceil((pids.arrivals().get(1).arrivalTime() - Date.now()) / 60000);
    let restime = 0
    let whiteColor = 0xFFFFFF

    //判断逻辑
    if (ftime < 1){
        reftime = "即将到达|Arriving";
    }
    else{
        reftime = ftime + min_zh +"|" +ftime + min_en
    }

    if (stime < 1){
        restime = "即将到达|Arriving";
    }
    else{
        restime = stime + min_zh +"|" +stime + min_en
    }

    //背景图片
    Texture.create("Background")
    .texture("nanbin:pids/image/crt_pids_1.png")
    .size(pids.width, pids.height)
    .draw(ctx);

    //小广告
    /*
    to developer
    最好选择16：9的图片
    */
    Texture.create("Advertisement")
    .texture("mtr:textures/texture/advertisement.png")
    .pos(0, 9.5)
    .size(96.3, 54.03)
    .draw(ctx);

    //文字渲染
    Text.create("Clock")
    .font("nanbin:harmonyos_sanssc_medium")
    .text(PIDSUtil.formatTime(MinecraftClient.worldDayTime(), true))
    .color(0xFFFFFF)
    .pos(124, 6)
    .scale(0.8)
    .rightAlign()
    .draw(ctx);

    Text.create("Title")
    .font("nanbin:harmonyos_sanssc_medium")
    .text(TextUtil.cycleString(Title))
    .color(whiteColor)
    .pos(26, 0.5)
    .scale(0.65)
    .draw(ctx);

    Text.create("firstColText1")
    .font("nanbin:harmonyos_sanssc_medium")
    .text(TextUtil.cycleString(firstColText1))
    .color(whiteColor)
    .pos(101, 18)
    .scale(0.5)
    .draw(ctx);

    Text.create("secondColText1")
    .font("nanbin:harmonyos_sanssc_medium")
    .text(TextUtil.cycleString(secondColText1 + pids.arrivals().get(0).destination()))
    .color(whiteColor)
    .pos(101, 24)
    .size(35, 18)
    .scale(0.4)
    .draw(ctx);

    Text.create("ftime")
    .font("nanbin:harmonyos_sanssc_medium")
    .text(TextUtil.cycleString(reftime))
    .color(whiteColor)
    .pos(101, 33)
    .size(35, 18)
    .scale(0.8)
    .draw(ctx);

    Text.create("thirdColText1")
    .font("nanbin:harmonyos_sanssc_medium")
    .text(TextUtil.cycleString(thirdColText1))
    .color(whiteColor)
    .pos(101, 47)
    .scale(0.5)
    .draw(ctx);

    Text.create("fourthColText1")
    .font("nanbin:harmonyos_sanssc_medium")
    .text(TextUtil.cycleString(secondColText1 + pids.arrivals().get(1).destination()))
    .color(whiteColor)
    .pos(101, 53)
    .size(35, 18)
    .scale(0.4)
    .draw(ctx);

    Text.create("stime")
    .font("nanbin:harmonyos_sanssc_medium")
    .text(TextUtil.cycleString(restime))
    .color(whiteColor)
    .pos(101, 58)
    .size(35, 18)
    .scale(0.8)
    .draw(ctx);

    Text.create("customMsg")
    .font("nanbin:harmonyos_sanssc_medium")
    .text(TextUtil.cycleString(pids.getCustomMessage(0)))
    .color(whiteColor)
    .scale(0.8)
    .size(pids.width - 38.5, 18)
    .marquee()
    .pos(13, 69)
    .draw(ctx);
}
function dispose(ctx, state, pids) {
    print("Goodbye World ^^;");
}