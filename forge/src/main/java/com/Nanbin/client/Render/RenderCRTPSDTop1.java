package com.Nanbin.client.Render;

import com.Nanbin.Registry.RegBlock.BlockCRTPSDTop1;
import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FontType;
import com.Nanbin.client.RouteMap.LineMapData;
import com.Nanbin.client.RouteMap.LineMapDataResolver;
import com.Nanbin.client.RouteMap.RouteMapGenerator;
import com.Nanbin.client.RouteMap.RouteMapGenerator.PSDStationData;
import org.mtr.mapping.holder.Block;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.BlockEntityRenderer;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.block.BlockPSDAPGDoorBase;
import org.mtr.mod.block.BlockPSDAPGGlassEndBase;
import org.mtr.mod.block.BlockPSDTop;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.RenderRouteBase;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.util.HashMap;
import java.util.Map;

/**
 * CRT 高屏蔽门吊板渲染器（自绘，不再走 MTR 的 {@code RenderPSDTop}）。
 *
 * <p>绘制空间与 {@code RenderCRTStationName1} 相同：以方块中心为原点，先 {@code rotateY(-facing.asRotation())}、再 {@code rotateZ(180)}，
 * 于是 x_draw 增大对应乘客视角的从左到右、y_draw 增大对应从上到下，平面落在吊板正面（与 MTR psd_top 的 1.95/16 深度一致）。
 *
 * <p>按吊板下方部件分面：门上方＝大站牌（站名 + 线路编号／车站编号圆徽 + 上一站／下一站）；玻璃上方＝整条线路图；端玻璃上方＝不显示。
 * {@link BlockPSDTop#ARROW_DIRECTION} 由刷子右键循环并沿站台传播，这里只取它的奇偶，奇数时把整张图水平镜像。
 *
 * <p>吊板沿站台按下方门／玻璃的分组（LEFT / MIDDLE / RIGHT）成组，两类内容都按整组生成再按「本格下标 i / 组内总格数 N」切片：
 * 一扇门占 2 格，两块吊板各画一半才拼成一块完整站牌；玻璃连成几格，线路图就铺几格。
 */
public class RenderCRTPSDTop1 extends BlockEntityRenderer<BlockCRTPSDTop1.BlockEntity> implements IBlock {

    /** 吊板正面绘制深度（相对方块中心），与 MTR psd_top 的 1.95/16 一致。 */
    private static final float Z_FACE = 1.95F / 16F;
    /**
     * 绘制区上下缘（y_draw：负 = 上，正 = 下）。实拍导视板只占吊板约 0.6 格高（不是整格）、且紧贴下部：
     * 下缘对齐主面板下沿 0.4375（= 7/16，其下 1/16 是吊板底口收边），上缘 = 下缘 − 0.6。
     */
    private static final float CONTENT_BOTTOM = 0.4375F;
    private static final float CONTENT_TOP = CONTENT_BOTTOM - 0.6F;
    /** persistent 面板只占绘制区上半部分。 */
    private static final float PERSISTENT_BOTTOM = (CONTENT_TOP + CONTENT_BOTTOM) / 2F;

    /** 与 MTR 相同的站台搜索参数：吊板向下 3 格、半径 5 格内找站台。 */
    private static final int PLATFORM_SEARCH_Y_OFFSET = 3;
    private static final int PLATFORM_SEARCH_DISTANCE = 5;
    /** 兜底搜索：向下到屏蔽门下格所在楼层、放宽半径再找一次。 */
    private static final int VERTICAL_OFFSET_TO_PSD = 2;
    private static final int PLATFORM_SEARCH_FALLBACK_DISTANCE = 16;

    // 站牌（门上方）的字号与配色；字号以 512px 纹理高为基准，实拍站名约占吊板高 1/5
    // 整块牌（站名／上一站／下一站／圆内文字）统一用常规体，不用加粗
    private static final FontType SIGN_FONT_TYPE = FontType.ALIBABA;
    private static final FontType SIGN_CIRCLE_FONT_TYPE = FontType.SOURCE_SANS_3;
    private static final int SIGN_STATION_FONT_SIZE = 105;
    private static final int SIGN_SIDE_FONT_SIZE = 70;
    private static final int SIGN_BADGE_FONT_SIZE = 46;
    /** 拼音／英文相对中文的字号比（实拍约 0.55）。 */
    private static final float SIGN_LATIN_FONT_RATIO = 0.58F;
    private static final float SIGN_GAP_RATIO = 0.05F;
    private static final int SIGN_TEXT_COLOR = 0xFF000000;
    // 站牌宽度按下方门／玻璃那一组的格数计算，不再固定按单格 1F 生成
    // （否则每格各画一张完整站牌，一扇门上方会出现两块重复的牌子）。

    // 线路图（玻璃上方）的字号；字号以 512px 纹理高为基准，实拍站名约占吊板高 1/6
    // 线路图的元素尺寸由渲染器按「主线厚度」的规格比例推导，这里的字号只作为上限。
    private static final FontType MAP_NAME_FONT_TYPE = FontType.SOURCE_HAN;
    private static final FontType MAP_NUMBER_FONT_TYPE = FontType.SOURCE_SANS_3;
    private static final int MAP_NAME_FONT_SIZE = 114;
    private static final int MAP_NUMBER_FONT_SIZE = 60;
    /** 规格：中文 24 : 拼音 13。 */
    private static final float MAP_LATIN_FONT_RATIO = 13F / 24F;
    /** 规格：中文与拼音间距 = 中文字号 × ratio（规格值为 20/24，这里按版面适当收紧）。 */
    private static final float MAP_GAP_RATIO = 0.45F;
    /** 规格文字色 #111111。 */
    private static final int MAP_TEXT_COLOR = 0xFF111111;

    public RenderCRTPSDTop1(Argument dispatcher) {
        super(dispatcher);
    }

    @Override
    public void render(BlockCRTPSDTop1.BlockEntity entity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay) {
        final World world = entity.getWorld2();
        if (world == null) {
            return;
        }
        final BlockPos pos = entity.getPos2();
        final BlockState state = world.getBlockState(pos);
        final Direction facing = IBlock.getStatePropertySafe(state, DirectionHelper.FACING);
        final BlockPSDTop.EnumPersistent persistent = IBlock.getStatePropertySafe(state, BlockPSDTop.PERSISTENT);
        // 前进方向与 MTR psd_top 一致：刷子右键在 4 个方向间循环，并由 BlockPSDTop.onUse2 沿站台向左右各传播一格。
        // 这里只取奇偶：偶数按正向画，奇数整张水平镜像（站名倾斜、换乘支线、灰色延伸段都换到另一侧）。
        final boolean reverse = IBlock.getStatePropertySafe(state, BlockPSDTop.ARROW_DIRECTION) % 2 != 0;

        // 按 MTR 的规则向下 3 格、半径 5 格内找最近的站台，找不到再放宽一次；仍失败则不绘制
        final long[] platformId = {0};
        org.mtr.mod.InitClient.findClosePlatform(pos.down(PLATFORM_SEARCH_Y_OFFSET), PLATFORM_SEARCH_DISTANCE, platform -> platformId[0] = platform.getId());
        if (platformId[0] == 0) {
            org.mtr.mod.InitClient.findClosePlatform(pos.down(VERTICAL_OFFSET_TO_PSD), PLATFORM_SEARCH_FALLBACK_DISTANCE, platform -> platformId[0] = platform.getId());
        }
        if (platformId[0] == 0) {
            debug(pos, "no platform nearby");
            return;
        }

        final Block blockBelow = world.getBlockState(pos.down()).getBlock();
        final boolean door = blockBelow.data instanceof BlockPSDAPGDoorBase;
        if (!door && blockBelow.data instanceof BlockPSDAPGGlassEndBase) {
            // 端玻璃上方不显示（与 MTR 相同）
            debug(pos, "glass end below -> nothing");
            return;
        }
        if (persistent == BlockPSDTop.EnumPersistent.BLANK) {
            // 刷子把吊板切成 blank 时不显示内容（与 MTR 相同）
            debug(pos, "persistent=blank -> nothing");
            return;
        }

        final Group group = findGroup(world, pos, facing);
        final boolean persistentNone = persistent == BlockPSDTop.EnumPersistent.NONE;
        final float contentBottom = persistentNone ? CONTENT_BOTTOM : PERSISTENT_BOTTOM;
        final float bandHeight = contentBottom - CONTENT_TOP;
        // 本组总宽度（格）：门一组 2 格、玻璃刷成几格就是几格。站牌／线路图都按整组生成再切片。
        final float groupBlocks = group.count();

        final Identifier texture;
        if (door) {
            final PSDStationData data = RouteMapGenerator.resolvePSDStationData(platformId[0]);
            if (data == null || data.stationName().isEmpty()) {
                debug(pos, "door sign: no station data for platform " + platformId[0]);
                return;
            }
            texture = CustomFontTextureCache.instance.getPSDStationSignTexture(
                    data.stationName(), data.previousStationName(), data.nextStationName(), data.routeNumber(), data.stationNumber(),
                    data.lineColor(), SIGN_TEXT_COLOR, groupBlocks, bandHeight,
                    SIGN_FONT_TYPE, SIGN_CIRCLE_FONT_TYPE, SIGN_STATION_FONT_SIZE, SIGN_SIDE_FONT_SIZE, SIGN_BADGE_FONT_SIZE,
                    SIGN_LATIN_FONT_RATIO, SIGN_GAP_RATIO, reverse);
            debug(pos, "door sign drawn: platform=" + platformId[0] + " facing=" + facing + " side=" + sideOf(state) + " group=" + group.index() + "/" + group.count() + " reverse=" + reverse);
        } else {
            final LineMapData data = LineMapDataResolver.resolve(platformId[0]);
            if (data == null || data.stations().isEmpty()) {
                debug(pos, "line map: no route data for platform " + platformId[0]);
                return;
            }
            texture = CustomFontTextureCache.instance.getFasciaLineMapTexture(data, group.count(), bandHeight,
                    MAP_NAME_FONT_TYPE, MAP_NUMBER_FONT_TYPE, MAP_NAME_FONT_SIZE, MAP_NUMBER_FONT_SIZE,
                    MAP_LATIN_FONT_RATIO, MAP_GAP_RATIO, MAP_TEXT_COLOR, reverse);
            debug(pos, "line map drawn: platform=" + platformId[0] + " line=" + data.lineNumber() + " stations=" + data.stations().size() + " current=" + data.currentIndex() + " branches=" + data.branches().size() + " facing=" + facing + " side=" + sideOf(state) + " group=" + group.index() + "/" + group.count() + " reverse=" + reverse);
        }

        final int shadingColor = RenderRouteBase.getShadingColor(facing, 0xFFFFFFFF);
        // 按「本格下标 i / 组内总格数 N」切纹理：一扇门（2 格）上方的两块吊板各画一半，
        // 合起来才是横跨整扇门的大站牌。
        final float u1 = (float) group.index() / group.count();
        final float u2 = (float) (group.index() + 1) / group.count();

        final StoredMatrixTransformations matrix = new StoredMatrixTransformations(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        matrix.add(graphics -> {
            graphics.rotateYDegrees(-facing.asRotation());
            graphics.rotateZDegrees(180);
            graphics.translate(0, 0, Z_FACE);
        });

        // 主内容（站牌／线路图的自己那一段）。注意 IDrawing.drawTexture 末尾两个 int 的参数顺序是 (color, light)，
        // 传反的话颜色会拿到打包光照值（alpha=0），画出来是全透明的 —— 即「什么都没有渲染」。
        MainRenderer.scheduleRender(texture, false, QueuedRenderLayer.EXTERIOR, (graphicsHolderNew, offset) -> {
            matrix.transform(graphicsHolderNew, offset);
            IDrawing.drawTexture(graphicsHolderNew, -0.5F, CONTENT_TOP, 0F, 0.5F, contentBottom, 0F, u1, 0F, u2, 1F, facing, shadingColor, light);
            graphicsHolderNew.pop();
        });
    }

    /** 吊板分组：本格在组内的下标（从乘客左手边数起，0 起）与组内总格数。 */
    private record Group(int index, int count) {
    }

    /**
     * 扫描本行吊板得到分组。分组以下方门／玻璃的分组属性为准（乘客视角最左端 LEFT、中间 MIDDLE、最右端 RIGHT，
     * 与 MTR 的刷子分组一致），吊板自身无需同步该属性。
     */
    private static Group findGroup(World world, BlockPos pos, Direction facing) {
        final Object kind = world.getBlockState(pos.down()).getBlock().data;
        final int left = countTowards(world, pos, facing.rotateYCounterclockwise(), kind, EnumSide.LEFT);
        final int right = countTowards(world, pos, facing.rotateYClockwise(), kind, EnumSide.RIGHT);
        return new Group(left, left + 1 + right);
    }

    /**
     * 从 pos 出发沿 direction 数出本组的格数：遇到 MIDDLE 计入并继续，遇到 endSide（分组端点）计入并停止，
     * 其它情况（非同类吊板、非同类下方方块、另一分组端点）立即停止。
     */
    private static int countTowards(World world, BlockPos pos, Direction direction, Object kind, EnumSide endSide) {
        int count = 0;
        BlockPos current = pos.offset(direction);
        while (world.getBlockState(current).getBlock().data instanceof BlockCRTPSDTop1) {
            final BlockState below = world.getBlockState(current.down());
            if (below.getBlock().data != kind) {
                break;
            }
            final EnumSide side = sideOf(below);
            if (side == EnumSide.MIDDLE) {
                count++;
            } else {
                if (side == endSide) {
                    count++;
                }
                break;
            }
            current = current.offset(direction);
        }
        return count;
    }

    /** 取分组属性：门用 SIDE，玻璃／端玻璃用 SIDE_EXTENDED。 */
    private static EnumSide sideOf(BlockState state) {
        return state.getBlock().data instanceof BlockPSDAPGDoorBase
                ? IBlock.getStatePropertySafe(state, IBlock.SIDE)
                : IBlock.getStatePropertySafe(state, IBlock.SIDE_EXTENDED);
    }

    /** 调试用状态去重表：每个位置只在「状态关键字」变化时更新一次（与 RenderCRTStationName1 的调试做法一致）。 */
    private static final Map<Long, String> DEBUG_STATES = new HashMap<>();

    private static void debug(BlockPos pos, String message) {
        final long key = pos.asLong();
        if (message.equals(DEBUG_STATES.get(key))) {
            return;
        }
        if (DEBUG_STATES.size() > 512) {
            DEBUG_STATES.clear();
        }
        DEBUG_STATES.put(key, message);
    }
}
