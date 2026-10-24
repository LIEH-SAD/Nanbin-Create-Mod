package com.Nanbin.client.Render;

import com.Nanbin.InitClient;
import com.Nanbin.Registry.RegBlock.BlockCRTStationNameBase;
import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FontType;
import com.Nanbin.client.RouteMap.RouteMapGenerator;
import com.Nanbin.client.RouteMap.RouteMapGenerator.ResolvedRouteData;
import com.Nanbin.mapping.DirectionHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.mtr.MTRClient;
import org.mtr.block.IBlock;
import org.mtr.client.MinecraftClientData;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Station;
import org.mtr.render.BlockEntityRendererExtension;
import org.mtr.render.MainRenderer;
import org.mtr.render.QueuedRenderLayer;
import org.mtr.render.RenderRouteBase;
import org.mtr.render.StoredMatrixTransformations;
import org.mtr.tool.Drawing;

/**
 * CRT 站名牌渲染器的公共基类。
 * <p>
 * 源项目里 {@code RenderCRTStationName1}、{@code RenderCRTStationName2}、{@code RenderOrdinaryStationName}
 * 三个类逐行相同、只有一批布局常量不同（字号、字体、横条位置、圆参数等），
 * 所以这里抽成「一份逻辑 + 一份 {@link Style} 常量」，三个子类只提供各自的 Style。
 * <p>
 * 4.1 适配：{@code BlockEntityRenderer}（4.0 映射层）→ {@link BlockEntityRendererExtension}；
 * render 签名换成 {@code (T, MatrixStack, VertexConsumerProvider, ClientWorld, ClientPlayerEntity, float, int, int)}；
 * {@code GraphicsHolder} 的 rotate/translate/pop → {@code MatrixStack} + {@link Drawing}；
 * {@code entity.getWorld2()/getPos2()} → 原版 {@code getWorld()/getPos()}；
 * {@code BlockStationNameBase.FACING} → {@code Properties.HORIZONTAL_FACING}；
 * {@code InitClient.findStation} → {@link MTRClient#findStation}；
 * {@code DynamicTextureCache} 换到 {@code org.mtr.client.DynamicTextureCache}。
 * <p>
 * 顺带删掉了 4.0 里那段「调试：解析状态变化时输出一次日志」的死代码 ——
 * 它只算了几个局部变量、连 {@code lastDebugStates} 都没写进去就结束了。
 */
public abstract class RenderCRTStationNameBase<T extends BlockCRTStationNameBase.StationNameBlockEntity> extends BlockEntityRendererExtension<T> implements IBlock {

	private static final float Z_FROM_CENTER = 0.459375F;
	private static final float TEXT_MARGIN = 0.05F;
	private static final float TEXT_SCALE = 0.6F;
	private static final boolean USE_CUSTOM_FONT = true;

	/** MTR 的 Render 加载早于 BlockEntity，且字体纹理有缓存，需定时强制刷新 */
	private static final long REFRESH_INTERVAL_MS = 1000L;

	private long lastRefreshTime = 0;

	/** 本子类的布局参数。 */
	protected final Style style;

	protected RenderCRTStationNameBase(Style style) {
		this.style = style;
	}

	@Override
	public void render(T entity, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, float tickDelta, int light, int overlay) {
		// MTR 的 Render 加载早于 BlockEntity，且字体纹理被缓存不会随数据/布局变化自动失效，
		// 因此每秒强制刷新一次缓存，确保站名、间距等显示最新效果。
		final long now = System.currentTimeMillis();
		if (now - lastRefreshTime >= REFRESH_INTERVAL_MS) {
			lastRefreshTime = now;
			CustomFontTextureCache.instance.clearFittedTextureCache();
		}

		final BlockPos pos = entity.getPos();
		final BlockState state = clientWorld.getBlockState(pos);

		final Direction facing = IBlock.getStatePropertySafe(state, Properties.HORIZONTAL_FACING);
		final int shadingColor = RenderRouteBase.getShadingColor(facing, entity.getColor(state));

		final StoredMatrixTransformations baseMatrix = new StoredMatrixTransformations(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
		baseMatrix.add(matrices -> {
			Drawing.rotateYDegrees(matrices, -DirectionHelper.asRotation(facing));
			Drawing.rotateZDegrees(matrices, 180);
		});

		// 解析站名与站色
		final Station station = MTRClient.findStation(pos);
		final String stationName = station != null ? station.getName() : org.mtr.generated.lang.TranslationProvider.GUI_MTR_UNTITLED.getString();
		final int stationColor = station != null ? station.getColor() : 0;

		// CRT 站名牌是单面的，只渲染一次
		renderSide(baseMatrix, pos, facing, stationName, stationColor, shadingColor, light, entity);
	}

	private void renderSide(StoredMatrixTransformations base, BlockPos pos, Direction facing, String stationName, int stationColor, int shadingColor, int light, T entity) {
		final StoredMatrixTransformations matrix = base.copy();
		matrix.add(matrices -> matrices.translate(0, 0, Z_FROM_CENTER));

		String platformNumber = "";
		final long savedPlatformId = entity.getPlatformId();
		// 优先使用服务端同步到方块实体上的真实线路编号（MTR 客户端数据不含 routeNumber）
		final String syncedRouteNumber = entity.getRouteNumber() == null ? "" : entity.getRouteNumber();
		final ResolvedRouteData resolved = RouteMapGenerator.resolveRouteData(savedPlatformId, stationColor, syncedRouteNumber);
		final int themeColor = resolved.themeColor();
		final int routeColor = resolved.routeColor();
		final String routeNumber = resolved.routeNumber();

		try {
			final Platform platform = savedPlatformId == 0 ? null : MinecraftClientData.getInstance().platformIdMap.get(savedPlatformId);
			if (platform != null) {
				platformNumber = platform.getName();
			}
		} catch (Exception e) {
			InitClient.LOGGER.error("RenderCRTStationName: Error resolving platform name at {}", pos.toShortString(), e);
		}

		final float bgWidth = style.bgWidth();
		final float bgHeight = style.bgHeight();
		final float drawAspect = (bgWidth - TEXT_MARGIN * 2) / (bgHeight - TEXT_MARGIN * 2);
		final float textX = (-bgWidth / 2.0F + TEXT_MARGIN) * TEXT_SCALE;
		final float textY = (-bgHeight / 2.0F + TEXT_MARGIN) * TEXT_SCALE;
		final float textWidth = (bgWidth - TEXT_MARGIN * 2) * TEXT_SCALE;
		final float textHeight = (bgHeight - TEXT_MARGIN * 2) * TEXT_SCALE;

		final Identifier textureId;
		// 数据暂缺时仍渲染（回退到站名/站色），不再跳过渲染
		if (USE_CUSTOM_FONT) {
			textureId = CustomFontTextureCache.instance.getSignTexture(stationName, themeColor, routeColor, routeNumber, platformNumber, drawAspect,
					style.fontType(), style.fontSize(), style.circleFontType(),
					style.topBarEnd(), style.textEnd(), style.middleBarStart(), style.middleBarEnd(), style.middleBarWidth(), style.bottomBarStart(),
					style.latinFontRatio(), style.gapRatio(), style.verticalBias(), style.extraOffsetRatio(),
					style.textMarginDefault(), style.textMarginMedium(), style.textMarginShort(),
					style.circleCenterYOffset(), style.circleRadiusHRatio(), style.circleRadiusWRatio(), style.strokeHRatio(), style.circleFontRatio(),
					style.invertedCircle(), style.whiteBackground());
		} else {
			textureId = org.mtr.client.DynamicTextureCache.instance.getStationName(stationName, bgWidth).identifier;
		}

		MainRenderer.scheduleRender(textureId, false, QueuedRenderLayer.EXTERIOR, (matrices, vertexConsumer, offset) -> {
			matrix.transform(matrices, offset);
			new Drawing(matrices, vertexConsumer)
					.setVerticesWH(textX, textY, textWidth, textHeight)
					.setUv(0, 0, 1, 1)
					.setColor(shadingColor)
					.setLight(light)
					.draw();
			matrices.pop();
		});
	}

	/**
	 * 三个站名牌版本的差异全部集中在这里（源项目里散落成 20 多个 private static final 常量）。
	 */
	public record Style(
			float bgWidth,
			float bgHeight,
			FontType fontType,
			FontType circleFontType,
			int fontSize,
			boolean whiteBackground,
			float topBarEnd,
			float textEnd,
			float middleBarStart,
			float middleBarEnd,
			float middleBarWidth,
			float bottomBarStart,
			float latinFontRatio,
			float gapRatio,
			float verticalBias,
			float extraOffsetRatio,
			float textMarginDefault,
			float textMarginMedium,
			float textMarginShort,
			float circleCenterYOffset,
			float circleRadiusHRatio,
			float circleRadiusWRatio,
			float strokeHRatio,
			float circleFontRatio,
			boolean invertedCircle
	) {
	}

	/** 站名牌 1：汉文主字体 + 顶/底条 + 非反色圆。 */
	public static final Style STYLE_NAME_1 = new Style(
			1.4F, 1.6F, FontType.SOURCE_HAN, FontType.SOURCE_SANS_3, 85, false,
			0.10F, 0.55F, 0F, 0F, 0F, 0.70F,
			0.5F, 0.02F, 0.65F, 0F, 0.04F, 0.12F, 0.20F,
			0F, 0.10F, 0.14F, 0.006F, 0.35F, false
	);

	/** 站名牌 2：ALIBABA 字体 + 无顶/底条 + 中间主题色横条 + 反色圆。 */
	public static final Style STYLE_NAME_2 = new Style(
			1.5F, 1.7F, FontType.ALIBABA, FontType.SOURCE_SANS_3, 81, false,
			0.0F, 0.30F, 0.40F, 0.50F, 0F, 1F,
			0.5F, 0.012F, 0.7F, 0.03F, 0.04F, 0.12F, 0.20F,
			0.30F, 0.1F, 0.14F, 0.006F, 0.35F, true
	);

	/** 普通站名牌：白底黑字、ALIBABA、无顶/底条、非反色圆。 */
	public static final Style STYLE_ORDINARY = new Style(
			1.5F, 1.7F, FontType.ALIBABA, FontType.SOURCE_SANS_3, 81, true,
			0F, 0.62F, 0F, 0F, 0F, 1F,
			0.5F, 0.02F, 0.55F, 0F, 0.10F, 0.20F, 0.30F,
			0.36F, 0.09F, 0.13F, 0.006F, 0.32F, false
	);
}
