package com.Nanbin.client.Render;

import com.Nanbin.Init;
import com.Nanbin.Registry.RegBlock.BlockCRTRailwaySign;
import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.Drawing.FontRenderUtils;
import com.Nanbin.client.Drawing.SignTextStyleConfig;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FontType;
import com.Nanbin.client.JavaScriptSupport.JSDrawContext;
import com.Nanbin.client.JavaScriptSupport.JSSignConfig;
import com.Nanbin.client.JavaScriptSupport.JSSignEngine;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import mtr.block.BlockRailwaySign;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.client.CustomResources;
import mtr.client.IDrawing;
import mtr.data.Route;
import mtr.data.Station;
import mtr.render.RenderRailwaySign;
import mtr.render.MoreRenderLayers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.awt.Color;
import java.lang.Character.UnicodeScript;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RenderCRTRailwaySign extends RenderRailwaySign<BlockCRTRailwaySign.BlockEntityCRTRailwaySign> {
	private static final FontType FONT_TYPE;
	private static final int FONT_SIZE = 88;
	private static final float NUMBER_SCALE_BOOST = 1.35F;
	private static final float CJK_SCALE_BOOST = 1.0F;
	private static final float MODULE3_CJK_SCALE = 0.8F;
	private static final Color TEXT_COLOR;
	private static final long REFRESH_INTERVAL_MS = 1000L;
	private static final float LATIN_SCALE = 0.5F;
	private static final float LINE_GAP_RATIO = 0.0F;
	private static final float LATIN_RAISE_RATIO = 0.7F;
	private static final Identifier WHITE_TEXTURE;
	/** 渲染内容相对告示牌面板的 z 前移量：模型面板 north 面在 z=7/16=0.4375，内容需明显在其前方避免 z-fighting。 */
	private static final float SIGN_Z_OFFSET = -0.075F;

	private static final String ROUTE_NUMBER_V2 = "crt_route_name";
	private static final String ROUTE_NUMBER_V2_FLIPPED = "crt_route_name_flipped";
	private long lastRefreshTime = 0L;
	private static long lastJSRefreshTime;
	private static final long JS_REFRESH_INTERVAL_MS = 500L;

	public RenderCRTRailwaySign(BlockEntityRendererFactory.Context context) {
		// MTR 3.2.2 的 RenderRailwaySign 构造不真正使用 dispatcher（仅传给空的 super），传 null 即可。
		super(null);
	}

	public void render(BlockCRTRailwaySign.BlockEntityCRTRailwaySign entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
		try {
			renderInner(entity, tickDelta, matrices, vertexConsumers, light, overlay);
		} catch (Exception e) {
			Init.LOGGER.error("RenderCRTRailwaySign: render failed at {}", entity.getPos(), e);
		}
	}

	private void renderInner(BlockCRTRailwaySign.BlockEntityCRTRailwaySign entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {

		World world = entity.getWorld();
		if (world != null) {
			BlockPos pos = entity.getPos();
			BlockState state = world.getBlockState(pos);
			if (state.getBlock() instanceof BlockRailwaySign) {
				BlockRailwaySign block = (BlockRailwaySign) state.getBlock();
				String[] signIds = entity.getSignIds();
				if (signIds != null && signIds.length == block.length) {
					boolean allEmpty = true;
					for (String id : signIds) {
						if (id != null && !id.isEmpty()) {
							allEmpty = false;
							break;
						}
					}
					if (allEmpty) {
						return;
					}

					Direction facing = IBlock.getStatePropertySafe(state, net.minecraft.block.HorizontalFacingBlock.FACING);
					int[] cellBackgroundColors = new int[signIds.length];

					for (int i = 0; i < signIds.length; ++i) {
						String signId = signIds[i];
						if (signId != null) {
							CustomResources.CustomSign sign = RenderRailwaySign.getSign(signId);
							if (sign != null && sign.backgroundColor != 0) {
								cellBackgroundColors[i] = sign.backgroundColor;
							}
						}
					}

					final String styleScriptId = JSSignConfig.getStyleScriptId(signIds);

					Map<Long, String> routeNameByColor = new HashMap<>();
					for (final Route route : ClientData.ROUTES) {
						routeNameByColor.put((long) route.color, route.name);
					}

					// 仅非 JS 样式模式绘制整幅统一背景：JS 样式的背景由脚本通过 ctx 自行绘制
					// （与 MTR mixin 路径一致，避免背景矩形与 JS 半透明内容同层冲突闪烁）。
					// 非 JS 牌始终给整幅背景（无格子背景色则默认白底）。
					if (styleScriptId == null) {
						int unifiedBg = 0;
						for (int i = 0; i < signIds.length; ++i) {
							if (signIds[i] != null && !signIds[i].isEmpty() && cellBackgroundColors[i] != 0) {
								unifiedBg = cellBackgroundColors[i];
								break;
							}
						}

						int bgColor = unifiedBg != 0 ? unifiedBg : 16777215;
						int newBackgroundColor = bgColor | -16777216;
						// 复制 RenderCRTStationName1 的工作模式：用 render 传入的 matrices + relative 变换 + 局部 vertexConsumers
						matrices.push();
						matrices.translate(0.5, 0.53125, 0.5);
						matrices.multiply(new Quaternionf().rotationY(MathHelper.RADIANS_PER_DEGREE * -facing.asRotation()));
						matrices.multiply(new Quaternionf().rotationZ(MathHelper.RADIANS_PER_DEGREE * 180.0F));
						matrices.translate((float) block.getXStart() / 16.0F - 0.5F, 0.0F, SIGN_Z_OFFSET);
						// 背景：与 MTR 原版一致使用 getLight(WHITE,false) 不透明混合层，并沿 z 后移 0.003125，
						// 使背景平面与格子内容平面存在稳定深度差，内容画在背景前，避免半透明层内互相闪烁。
						matrices.push();
						matrices.translate(0.0F, 0.0F, 0.003125F);
						final RenderLayer bgLayer = MoreRenderLayers.getLight(WHITE_TEXTURE, false);
						final VertexConsumer bgConsumer = vertexConsumers.getBuffer(bgLayer);
						IDrawing.drawTexture(matrices, bgConsumer, 0.0F, 0.0F, 0.5F * (float) signIds.length, 0.5F, 0.0F, 0.0F, 1.0F, 1.0F, facing, newBackgroundColor, 15728880);
						matrices.pop();
						matrices.pop();
					}

					if (styleScriptId != null) {
						Map<Long, String> routeNumberMap = new HashMap<>();
						for (long color : entity.getSelectedIds()) {
							String number = entity.getRouteNumber(color);
							if (number != null && !number.isEmpty()) {
								routeNumberMap.put(color, number);
							}
						}

						int jsBackgroundColor = cellBackgroundColors[0] != 0 ? cellBackgroundColors[0] : 0;
						// JS 样式渲染与格子绘制使用同一相对坐标矩阵（背景所在坐标系）
						matrices.push();
						matrices.translate(0.5, 0.53125, 0.5);
						matrices.multiply(new Quaternionf().rotationY(MathHelper.RADIANS_PER_DEGREE * -facing.asRotation()));
						matrices.multiply(new Quaternionf().rotationZ(MathHelper.RADIANS_PER_DEGREE * 180.0F));
						matrices.translate((float) block.getXStart() / 16.0F - 0.5F, 0.0F, SIGN_Z_OFFSET);
						renderJSStyleSign(styleScriptId, matrices, vertexConsumers, pos, signIds, new LongAVLTreeSet(entity.getSelectedIds()), entity.getRouteNumbers(), facing, jsBackgroundColor, 0.5F, routeNumberMap);
						matrices.pop();
					} else {
						int unifiedBg = 0;
						for (int i = 0; i < signIds.length; ++i) {
							if (signIds[i] != null && !signIds[i].isEmpty() && cellBackgroundColors[i] != 0) {
								unifiedBg = cellBackgroundColors[i];
								break;
							}
						}
						final int globalBg = unifiedBg != 0 ? unifiedBg : 16777215;
						// 与 MTR 原版 render 一致：对 render 传入的 matrices 做本地坐标变换后再绘制格子
						matrices.push();
						matrices.translate(0.5, 0.53125, 0.5);
						matrices.multiply(new Quaternionf().rotationY(MathHelper.RADIANS_PER_DEGREE * -facing.asRotation()));
						matrices.multiply(new Quaternionf().rotationZ(MathHelper.RADIANS_PER_DEGREE * 180.0F));
						matrices.translate((float) block.getXStart() / 16.0F - 0.5F, 0.0F, SIGN_Z_OFFSET);
						for (int i = 0; i < signIds.length; ++i) {
							if (signIds[i] != null && !isRouteNumberV2(signIds[i])) {
								int cellBg = cellBackgroundColors[i] != 0 ? cellBackgroundColors[i] : 16777215;
								drawSignCRT(matrices, vertexConsumers, pos, signIds[i], 0.5F * (float) i, 0.0F, 0.5F, RenderRailwaySign.getMaxWidth(signIds, i, false), RenderRailwaySign.getMaxWidth(signIds, i, true), new LongAVLTreeSet(entity.getSelectedIds()), entity.getRouteNumbers(), facing, cellBg, (textureId, x, y, size, flipTexture) -> drawTexturedRect(matrices, vertexConsumers, textureId, x, y, size, size, flipTexture, facing), false, signIds, globalBg);
							}
						}
						renderRouteNumberV2Regions(matrices, vertexConsumers, entity, signIds, facing, 0.5F, routeNameByColor, cellBackgroundColors);
						matrices.pop();
					}
				} else {
					Init.LOGGER.warn("CRT Railway Sign render skipped: signIds.length={}, block.length={}", signIds == null ? "null" : signIds.length, block.length);
				}
			}
		}
	}

	/** 用传入的 matrices（render 传入，已相对变换）和 vertexConsumers 绘制矩形。调用方负责 push/pop。 */
	private static void drawTexturedRect(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Identifier textureId, float x, float y, float w, float h, boolean flip, Direction facing) {
		final RenderLayer renderLayer = MoreRenderLayers.getLight(textureId, true);
		final VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);
		IDrawing.drawTexture(matrices, vertexConsumer, x, y, w, h, flip ? 1.0F : 0.0F, 0.0F, flip ? 0.0F : 1.0F, 1.0F, facing, -1, 15728880);
	}

	private static void drawTexturedRect(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Identifier textureId, float x, float y, float w, float h, Direction facing, int color) {
		final RenderLayer renderLayer = MoreRenderLayers.getLight(textureId, true);
		final VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);
		IDrawing.drawTexture(matrices, vertexConsumer, x, y, w, h, 0.0F, 0.0F, 1.0F, 1.0F, facing, color, 15728880);
	}

	private static void drawTexturedRect(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Identifier textureId, float x, float y, float w, float h, float u1, float v1, float u2, float v2, Direction facing, int color) {
		final RenderLayer renderLayer = MoreRenderLayers.getLight(textureId, true);
		final VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);
		IDrawing.drawTexture(matrices, vertexConsumer, x, y, w, h, u1, v1, u2, v2, facing, color, 15728880);
	}

	public static void drawSignCRT(MatrixStack matrices, VertexConsumerProvider vertexConsumers, BlockPos pos, String signId, float x, float y, float size, float maxWidthLeft, float maxWidthRight, LongAVLTreeSet selectedIds, String[] routeNumbers, Direction facing, int backgroundColor, RenderRailwaySign.DrawTexture drawTexture, boolean transparentBlack, String[] signIds, int globalBgColor) {
		CustomResources.CustomSign sign = RenderRailwaySign.getSign(signId);
		if (sign != null) {
			boolean isExit = signId.equals("exit_letter") || signId.equals("exit_letter_flipped");
			boolean isLine = signId.equals("line") || signId.equals("line_flipped");
			boolean isPlatform = signId.equals("platform") || signId.equals("platform_flipped");
			// 统一使用 render 传入的相对坐标矩阵 + vertexConsumers 同步绘制，避免绝对坐标 SMT 在 Immediate 下错位。
			if (!isExit && !isLine && !isPlatform) {
				if (!sign.hasCustomText()) {
					RenderRailwaySign.drawSign(matrices, vertexConsumers, null, MinecraftClient.getInstance().textRenderer, pos, signId, x, y, size, maxWidthLeft, maxWidthRight, selectedIds, facing, transparentBlack ? -16777216 : backgroundColor | -16777216, drawTexture);
				} else {
					boolean flipCustomText = sign.flipCustomText;
					boolean isSmall = sign.small;
					boolean isStation = signId.equals("station") || signId.equals("station_flipped");
					boolean isStationNameCRT = signId.equals("crt_station_name");
					boolean isRouteNumber = signId.equals("crt_route_number") || signId.equals("crt_route_number_flipped");
					boolean isJSSign = signId.startsWith("crt_js_");
					if (isJSSign) {
						renderJSSign(signId, matrices, vertexConsumers, pos, signIds, selectedIds, routeNumbers, facing, backgroundColor, x, size, flipCustomText, transparentBlack, y);
					} else {
						SignTextStyleConfig.SignTextStyle style = SignTextStyleConfig.get(signId);
						if (style == null && signId.endsWith("_flipped")) {
							style = SignTextStyleConfig.get(signId.substring(0, signId.length() - "_flipped".length()));
						}

						String signText = resolveSignText(signId, sign, selectedIds, routeNumbers, isStation || isStationNameCRT, isRouteNumber);
						if (transparentBlack) {
							renderCustomTextCRT(signText, matrices, vertexConsumers, facing, size, x, false, size, 0, isRouteNumber ? getNumberFontType(signText) : FONT_TYPE, isRouteNumber, true, false, (SignTextStyleConfig.SignTextStyle) null, false, 1.0F, TEXT_COLOR, y, 0.0F);
						} else {
							if (!isStation && !isRouteNumber && !isStationNameCRT) {
								float signSize = (sign.small ? 0.75F : 1.0F) * size;
								float margin = (size - signSize) / 2.0F;
								drawTexture.drawTexture(sign.textureId, x + margin, y + margin, signSize, sign.flipTexture);
							}

							float fixedMargin = size * 0.25F / 2.0F;
							float start;
							float maxWidth;
							if (!isRouteNumber && !isStationNameCRT) {
								maxWidth = Math.max(0.0F, (flipCustomText ? maxWidthLeft : maxWidthRight) * size - fixedMargin * (float) (isSmall ? 1 : 2));
								start = flipCustomText ? x - (isSmall ? 0.0F : fixedMargin) : x + size + (isSmall ? 0.0F : fixedMargin);
							} else {
								maxWidth = size;
								start = x;
							}

							renderCustomTextCRT(signText, matrices, vertexConsumers, facing, size, start, flipCustomText, maxWidth, globalBgColor, isRouteNumber ? getNumberFontType(signText) : FONT_TYPE, isRouteNumber, isRouteNumber || isStationNameCRT, isRouteNumber, style, false, 1.0F, (Color) null);
						}
					}
				}
			} else {
				RenderRailwaySign.drawSign(matrices, vertexConsumers, null, MinecraftClient.getInstance().textRenderer, pos, signId, x, y, size, maxWidthLeft, maxWidthRight, selectedIds, facing, transparentBlack ? -16777216 : backgroundColor | -16777216, drawTexture);
			}
		}
	}

	private static String resolveSignText(String signId, CustomResources.CustomSign sign, LongAVLTreeSet selectedIds, String[] routeNumbers, boolean isStation, boolean isRouteNumber) {
		if (signId != null && (signId.equals("nanbin_custom_text") || signId.equals("nanbin_custom_text_flipped"))) {
			String customText = JSSignEngine.readCustomText(selectedIds);
			if (!customText.isEmpty()) {
				return customText;
			}
		}

		if (isStation) {
			return selectedIds.longStream().filter(ClientData.DATA_CACHE.stationIdMap::containsKey).sorted().mapToObj(stationId -> {
				final Station station = ClientData.DATA_CACHE.stationIdMap.get(stationId);
				return station == null ? "" : station.name;
			}).collect(Collectors.joining("|"));
		} else {
			return isRouteNumber ? String.join("/", routeNumbers) : (sign.customText == null ? "" : sign.customText);
		}
	}

	private static void renderCustomTextCRT(String signText, MatrixStack matrices, VertexConsumerProvider vertexConsumers, Direction facing, float size, float start, boolean flipCustomText, float maxWidth, int backgroundColor, FontType fontType, boolean forceFullSizeLines, boolean centerInCell, boolean forceWhiteBackground, @Nullable SignTextStyleConfig.SignTextStyle style, boolean drawBackground, float cjkScale, @Nullable Color forcedTextColor) {
		renderCustomTextCRT(signText, matrices, vertexConsumers, facing, size, start, flipCustomText, maxWidth, backgroundColor, fontType, forceFullSizeLines, centerInCell, forceWhiteBackground, style, drawBackground, cjkScale, forcedTextColor, 0.0F, 0.0F);
	}

	private static void renderCustomTextCRT(String signText, MatrixStack matrices, VertexConsumerProvider vertexConsumers, Direction facing, float size, float start, boolean flipCustomText, float maxWidth, int backgroundColor, FontType fontType, boolean forceFullSizeLines, boolean centerInCell, boolean forceWhiteBackground, @Nullable SignTextStyleConfig.SignTextStyle style, boolean drawBackground, float cjkScale, @Nullable Color forcedTextColor, float yOffset, float centerWidth) {
		if (!(maxWidth <= 0.0F)) {
			int textBackgroundColor = !forceWhiteBackground ? backgroundColor | -16777216 : -1;
			Color textColor = forcedTextColor != null ? forcedTextColor : (style != null && style.hasColor() ? new Color(style.textColor) : (isLightBackground(textBackgroundColor) ? TEXT_COLOR : new Color(255, 255, 255)));
			float sizeMult = style != null ? style.textSize : 1.0F;
			boolean bold = style != null && style.textBold;
			FontType renderFontType = bold ? FontType.SOURCE_HAN : fontType;
			List<CustomFontTextureCache.FittedTextTexture> fittedLines = new ArrayList<>();
			List<Integer> linePaddings = new ArrayList<>();
			List<Integer> lineRaises = new ArrayList<>();
			int totalGlyphHeight = 0;
			int maxPxWidth = 0;
			int maxPadding = 0;

			for (String line : signText.split("\\|")) {
				if (!line.isEmpty()) {
					boolean cjk = containsCJK(line);
					int lineFontSize = !cjk && !forceFullSizeLines ? Math.round(44.0F) : Math.round(88.0F * cjkScale);
					CustomFontTextureCache.FittedTextTexture fitted = CustomFontTextureCache.instance.getFittedTextTexture(line, renderFontType, lineFontSize, textColor, bold);
					if (fitted.width <= 0 || fitted.height <= 0) {
						return;
					}

					int padding = Math.max(2, Math.round((float) lineFontSize * 0.08F));
					fittedLines.add(fitted);
					linePaddings.add(padding);
					lineRaises.add(!cjk && !forceFullSizeLines ? Math.round((float) lineFontSize * 0.7F) : 0);
					totalGlyphHeight += Math.max(1, fitted.height - padding * 2);
					maxPxWidth = Math.max(maxPxWidth, fitted.width);
					maxPadding = Math.max(maxPadding, padding);
				}
			}

			if (!fittedLines.isEmpty()) {
				int lineGapPx = Math.round(0.0F);
				totalGlyphHeight += lineGapPx * (fittedLines.size() - 1);
				float pixelToWorld = size / (float) totalGlyphHeight;
				float blockWidth = (float) maxPxWidth * pixelToWorld;
				float blockHeight = (float) totalGlyphHeight * pixelToWorld;
				float fitScale = Math.min(1.0F, maxWidth / blockWidth);
				float scaleY;
				float scaleX;
				if (centerInCell) {
					scaleY = sizeMult;
					scaleX = Math.min(sizeMult, maxWidth / blockWidth);
				} else {
					scaleY = fitScale * sizeMult;
					scaleX = fitScale * sizeMult;
				}

				float width = blockWidth * scaleX;
				float height = blockHeight * scaleY;
				float x1;
				if (centerInCell) {
					float glyphWidth = (float) Math.max(1, maxPxWidth - maxPadding * 2) * pixelToWorld * scaleX;
					x1 = start + (centerWidth > 0.0F ? (centerWidth - glyphWidth) / 2.0F : (size - glyphWidth) / 2.0F);
				} else {
					x1 = flipCustomText ? start - width : start;
				}

				float firstRaise = lineRaises.isEmpty() ? 0.0F : (float) (Integer) lineRaises.get(0) * pixelToWorld * scaleY;
				float y1 = yOffset + (size - height) / 2.0F + firstRaise / 2.0F;
				if (drawBackground) {
					float backgroundX = centerInCell ? start : x1;
					float backgroundY = centerInCell ? yOffset : y1;
					float backgroundW = centerInCell ? (centerWidth > 0.0F ? centerWidth : size) : width;
					float backgroundH = centerInCell ? size : height;
					drawTexturedRect(matrices, vertexConsumers, WHITE_TEXTURE, backgroundX, backgroundY, backgroundW, backgroundH, facing, textBackgroundColor);
				}

				float lineY = y1;

				for (int i = 0; i < fittedLines.size(); ++i) {
					CustomFontTextureCache.FittedTextTexture lineTexture = fittedLines.get(i);
					int padding = linePaddings.get(i);
					int raise = lineRaises.get(i);
					float lineW = (float) lineTexture.width * pixelToWorld * scaleX;
					float lineH = (float) lineTexture.height * pixelToWorld * scaleY;
					float lineX;
					if (centerInCell) {
						float lineGlyphWidth = (float) Math.max(1, lineTexture.width - padding * 2) * pixelToWorld * scaleX;
						float lineCenterX = start + (centerWidth > 0.0F ? (centerWidth - lineGlyphWidth) / 2.0F : (size - lineGlyphWidth) / 2.0F);
						lineX = lineCenterX - (float) padding * pixelToWorld * scaleX;
					} else {
						lineX = flipCustomText ? start - lineW + (float) padding * pixelToWorld * scaleX : x1 - (float) padding * pixelToWorld * scaleX;
					}

					float finalLineY = lineY - (float) (padding + raise) * pixelToWorld * scaleY;
					float glyphLineHeight = (float) Math.max(1, lineTexture.height - padding * 2) * pixelToWorld * scaleY;
					drawTexturedRect(matrices, vertexConsumers, lineTexture.identifier, lineX, finalLineY, lineW, lineH, facing, -1);
					lineY += glyphLineHeight + (float) lineGapPx * pixelToWorld * scaleY;
				}

			}
		}
	}
	private static boolean containsCJK(String text) {
		for (int i = 0; i < text.length(); ++i) {
			UnicodeScript script = UnicodeScript.of(text.charAt(i));
			if (script == UnicodeScript.HAN) {
				return true;
			}
		}

		return false;
	}

	private static boolean isLightBackground(int argbColor) {
		int r = argbColor >> 16 & 255;
		int g = argbColor >> 8 & 255;
		int b = argbColor & 255;
		return (r * 299 + g * 587 + b * 114) / 1000 > 128;
	}

	public static float measureTextWidth(String text, float boxHeight) {
		if (text != null && !text.isEmpty() && !(boxHeight <= 0.0F)) {
			int totalGlyphHeight = 0;
			int maxPxWidth = 0;
			int lineCount = 0;

			for (String line : text.split("\\|")) {
				if (!line.isEmpty()) {
					boolean cjk = containsCJK(line);
					int lineFontSize = cjk ? Math.round(88.0F) : Math.round(44.0F);
					CustomFontTextureCache.FittedTextTexture fitted = CustomFontTextureCache.instance.getFittedTextTexture(line, FONT_TYPE, lineFontSize, TEXT_COLOR);
					if (fitted.width > 0 && fitted.height > 0) {
						int padding = Math.max(2, Math.round((float) lineFontSize * 0.08F));
						totalGlyphHeight += Math.max(1, fitted.height - padding * 2);
						maxPxWidth = Math.max(maxPxWidth, fitted.width);
						++lineCount;
					}
				}
			}

			if (lineCount != 0 && maxPxWidth > 0) {
				int lineGapPx = Math.round(0.0F);
				totalGlyphHeight += lineGapPx * (lineCount - 1);
				float pixelToWorld = boxHeight / (float) totalGlyphHeight;
				return (float) maxPxWidth * pixelToWorld;
			} else {
				return 0.0F;
			}
		} else {
			return 0.0F;
		}
	}

	private static boolean isRouteNumberV2(String signId) {
		return "crt_route_name".equals(signId) || "crt_route_name_flipped".equals(signId);
	}

	private static String deriveNumberFromName(@Nullable String rawName) {
		if (rawName != null && !rawName.isEmpty()) {
			int hideIdx = rawName.indexOf("||");
			String visible = hideIdx >= 0 ? rawName.substring(0, hideIdx) : rawName;
			int sepIdx = visible.indexOf(124);
			String primary = sepIdx >= 0 ? visible.substring(0, sepIdx) : visible;
			return primary.trim();
		} else {
			return "";
		}
	}

	private static String stripNumberFromName(@Nullable String rawName, String number) {
		if (rawName != null && !rawName.isEmpty()) {
			int hideIdx = rawName.indexOf("||");
			String visible = hideIdx >= 0 ? rawName.substring(0, hideIdx) : rawName;
			String[] numberSegments = number != null && !number.isEmpty() ? number.split("\\|") : new String[0];
			String[] nameSegments = visible.split("\\|");
			StringBuilder sb = new StringBuilder();
			for (String nameSegment : nameSegments) {
				String trimmed = nameSegment.trim();
				boolean matches = false;
				for (String numberSegment : numberSegments) {
					if (!numberSegment.isEmpty() && trimmed.contains(numberSegment)) {
						matches = true;
						break;
					}
				}
				if (!matches) {
					if (sb.length() > 0) {
						sb.append("|");
					}
					sb.append(trimmed);
				}
			}
			return sb.toString();
		} else {
			return "";
		}
	}

	private static void renderRouteNumberV2Regions(MatrixStack matrices, VertexConsumerProvider vertexConsumers, BlockCRTRailwaySign.BlockEntityCRTRailwaySign entity, String[] signIds, Direction facing, float size, Map<Long, String> routeNameByColor, int[] cellBackgroundColors) {
		for (int i = 0; i < signIds.length; ++i) {
			if (signIds[i] != null && isRouteNumberV2(signIds[i])) {
				int start;
				for (start = i; start > 0 && signIds[start - 1] != null && signIds[start - 1].equals(signIds[i]); --start) {
				}

				int v2End;
				for (v2End = i; v2End < signIds.length - 1 && signIds[v2End + 1] != null && signIds[v2End + 1].equals(signIds[i]); ++v2End) {
				}

				boolean regionFlipped = "crt_route_name_flipped".equals(signIds[start]);
				long[] colors = null;

				for (int c = start; c <= v2End && colors == null; ++c) {
					long[] cellColors = entity.getCellColors(c);
					if (cellColors != null && cellColors.length > 0) {
						colors = cellColors;
					}
				}

				if (colors == null) {
					colors = entity.getSelectedIds().stream().filter((color) -> color > 0L && routeNameByColor.containsKey(color)).mapToLong(Long::longValue).toArray();
				}

				if (colors.length == 0) {
					i = v2End;
				} else {
					int extraBlanks = Math.max(0, colors.length - (v2End - start + 1));
					int regionStart = start;
					int end = v2End;

					for (int c = v2End + 1; c < signIds.length && c <= v2End + extraBlanks; end = c++) {
						String nextSignId = signIds[c];
						if (nextSignId != null && !nextSignId.isEmpty()) {
							break;
						}
					}

					if (regionFlipped) {
						int rightAbsorbed = end - v2End;

						for (int c = start - 1; c >= 0 && c >= start - (extraBlanks - rightAbsorbed); regionStart = c--) {
							String nextSignId = signIds[c];
							if (nextSignId != null && !nextSignId.isEmpty()) {
								break;
							}
						}
					}

					for (int cell = regionStart; cell <= end; ++cell) {
						int cellBg = cellBackgroundColors[cell] != 0 ? cellBackgroundColors[cell] : 16777215;
						renderRouteNumberV2Cell(matrices, vertexConsumers, signIds, cell, regionStart, end, colors, routeNameByColor, entity, facing, size, cellBg, regionFlipped);
					}

					i = end;
				}
			}
		}

	}

	private static void renderRouteNumberV2Cell(MatrixStack matrices, VertexConsumerProvider vertexConsumers, String[] signIds, int cellIndex, int regionStart, int regionEnd, long[] colors, Map<Long, String> routeNameByColor, BlockCRTRailwaySign.BlockEntityCRTRailwaySign entity, Direction facing, float size, int backgroundColor, boolean regionFlipped) {
		String cellSignId = signIds[cellIndex];
		boolean flipped = "crt_route_name_flipped".equals(cellSignId) || (cellSignId == null || cellSignId.isEmpty()) && regionFlipped;
		int shownRoutes = Math.min(regionEnd - regionStart + 1, colors.length);
		int routeIndex = flipped ? regionEnd - cellIndex : cellIndex - regionStart;
		if (routeIndex < shownRoutes) {
			long routeColorLong = colors[routeIndex];
			int routeColor = (int) routeColorLong | -16777216;
			String routeName = routeNameByColor.get(routeColorLong);
			String number = entity.getRouteNumber(routeColorLong);
			if (number.isEmpty()) {
				number = deriveNumberFromName(routeName);
			}

			String text = stripNumberFromName(routeName, number);
			boolean isTextCell = flipped ? cellIndex == regionEnd - shownRoutes + 1 : cellIndex == regionStart + shownRoutes - 1;
			float cellX = (float) cellIndex * size;
			if (flipped) {
				renderRouteNumberV2CellFlip(matrices, vertexConsumers, signIds, cellIndex, facing, size, cellX, routeColor, number, text, isTextCell, backgroundColor);
			} else {
				renderRouteNumberV2CellNormal(matrices, vertexConsumers, signIds, cellIndex, facing, size, cellX, routeColor, number, text, isTextCell, backgroundColor);
			}

		}
	}

	private static void renderRouteNumberV2CellNormal(MatrixStack matrices, VertexConsumerProvider vertexConsumers, String[] signIds, int cellIndex, Direction facing, float size, float cellX, int routeColor, String number, String text, boolean isLast, int backgroundColor) {
		float m1Y = 0.2F * size;
		float m1W = 0.3F * size;
		float m1H = 0.8F * size;
		drawSolidRect(matrices, vertexConsumers, facing, cellX, m1Y, m1W, m1H, routeColor);
		boolean hasNumber = !number.isEmpty();
		if (hasNumber) {
			drawNumberBlock(matrices, vertexConsumers, facing, cellX + 0.3F * size, 0.0F, 0.7F * size, 1.0F * size, number);
		}

		if (isLast && !text.isEmpty()) {
			float textStart = cellX + (hasNumber ? 1.0F : 0.4F) * size;
			SignTextStyleConfig.SignTextStyle style = SignTextStyleConfig.get(signIds[cellIndex]);
			renderCustomTextCRT(text, matrices, vertexConsumers, facing, size, textStart, false, 100.0F, backgroundColor, FontType.ALIBABA, false, false, false, style, false, 0.8F, (Color) null);
		}

	}

	private static void renderRouteNumberV2CellFlip(MatrixStack matrices, VertexConsumerProvider vertexConsumers, String[] signIds, int cellIndex, Direction facing, float size, float cellX, int routeColor, String number, String text, boolean isTextCell, int backgroundColor) {
		float m1X = cellX + 0.7F * size;
		float m1Y = 0.2F * size;
		float m1W = 0.3F * size;
		float m1H = 0.8F * size;
		drawSolidRect(matrices, vertexConsumers, facing, m1X, m1Y, m1W, m1H, routeColor);
		boolean hasNumber = !number.isEmpty();
		if (hasNumber) {
			drawNumberBlock(matrices, vertexConsumers, facing, cellX, 0.0F, 0.7F * size, size, number);
		}

		if (isTextCell && !text.isEmpty()) {
			float textStart = cellX + (hasNumber ? 0.0F : 0.7F) * size;
			SignTextStyleConfig.SignTextStyle style = SignTextStyleConfig.get(signIds[cellIndex]);
			if (style == null) {
				style = SignTextStyleConfig.get("crt_route_name");
			}

			renderCustomTextCRT(text, matrices, vertexConsumers, facing, size, textStart, true, 100.0F, backgroundColor, FontType.ALIBABA, false, false, false, style, false, 0.8F, (Color) null);
		}

	}

	private static void drawNumberBlock(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Direction facing, float x, float y, float w, float h, String number) {
		drawSolidRect(matrices, vertexConsumers, facing, x, y, w, h, -1);
		drawNumberInBox(matrices, vertexConsumers, facing, x, y, w, h, number);
	}

	private static void drawSolidRect(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Direction facing, float x, float y, float width, float height, int color) {
		drawTexturedRect(matrices, vertexConsumers, WHITE_TEXTURE, x, y, width, height, facing, color);
	}

	private static FontType getNumberFontType(String text) {
		return FontRenderUtils.isNumeric(text) ? FontType.SOURCE_SANS_3 : FONT_TYPE;
	}

	private static void drawNumberInBox(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Direction facing, float boxX, float boxY, float boxW, float boxH, String number) {
		if (!number.isEmpty()) {
			CustomFontTextureCache.FittedTextTexture fitted = CustomFontTextureCache.instance.getFittedTextTexture(number, getNumberFontType(number), 88, new Color(0, 0, 0));
			if (fitted.width > 0 && fitted.height > 0) {
				int padding = Math.max(1, Math.round(1.76F));
				float glyphW = (float) Math.max(1, fitted.width - padding * 2);
				float glyphH = (float) Math.max(1, fitted.height - padding * 2);
				float boost = FontRenderUtils.isNumeric(number) ? 1.35F : 1.0F;
				float scale = Math.min(boxH / glyphH, boxW / glyphW) * boost;
				float textW = (float) fitted.width * scale;
				float textH = (float) fitted.height * scale;
				float textX = boxX + (boxW - textW) / 2.0F;
				float textY = boxY + (boxH - textH) / 2.0F;
				drawTexturedRect(matrices, vertexConsumers, fitted.identifier, textX, textY, textW, textH, facing, -1);
			}
		}
	}

	private static void renderJSStyleSign(String scriptId, MatrixStack matrices, VertexConsumerProvider vertexConsumers, BlockPos pos, String[] signIds, LongAVLTreeSet selectedIds, String[] routeNumbers, Direction facing, int backgroundColor, float cellSize, Map<Long, String> routeNumberMap) {
		renderJSStyleLine(scriptId, matrices, vertexConsumers, pos, signIds, selectedIds, routeNumbers, facing, backgroundColor, cellSize, 0.0F, 0.0F, false, routeNumberMap);
	}

	public static void renderJSStyleLine(String scriptId, MatrixStack matrices, VertexConsumerProvider vertexConsumers, BlockPos pos, String[] lineIds, LongAVLTreeSet selectedIds, String[] routeNumbers, Direction facing, int backgroundColor, float cellSize, float startX, float startY, boolean transparentBlack, Map<Long, String> routeNumberMap) {
		refreshJSCache();
		String signKey = scriptId + "_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + "_" + startX + "_" + startY + "_" + facing;
		JSSignEngine.JSSignContext context = new JSSignEngine.JSSignContext(lineIds, 0, selectedIds, routeNumbers, backgroundColor, cellSize, signKey, pos, routeNumberMap);
		JSDrawContext drawContext = new JSDrawContext(matrices, vertexConsumers, facing, startX, startY, cellSize, lineIds.length, transparentBlack, backgroundColor);
		JSSignEngine.render(scriptId, signKey, drawContext, new JSSignEngine.JSSignAPI(context, scriptId));
	}

	private static void refreshJSCache() {
		long now = System.currentTimeMillis();
		if (now - lastJSRefreshTime >= 500L) {
			lastJSRefreshTime = now;
			JSSignEngine.clearLegacyResultCache();
		}

	}

	private static void renderJSSign(String signId, MatrixStack matrices, VertexConsumerProvider vertexConsumers, BlockPos pos, String[] signIds, LongAVLTreeSet selectedIds, String[] routeNumbers, Direction facing, int backgroundColor, float x, float size, boolean flipCustomText, boolean transparentBlack, float y) {
		refreshJSCache();
		String scriptId = signId.startsWith("crt_js_") ? signId.substring("crt_js_".length()).replace("_flipped", "") : "";
		if (!scriptId.isEmpty()) {
			JSSignEngine.JSSignContext context = new JSSignEngine.JSSignContext(signIds, 0, selectedIds, routeNumbers, backgroundColor, size);
			Object result = JSSignEngine.execute(scriptId, context);
			JSSignEngine.JSSignResult cellResult = result instanceof JSSignEngine.JSSignResult ? (JSSignEngine.JSSignResult) result : JSSignEngine.toCellResults(result, 1)[0];
			renderJSSignCell(cellResult, matrices, vertexConsumers, facing, size, x, flipCustomText, transparentBlack, backgroundColor, y);
		}
	}

	private static void renderJSSignCell(JSSignEngine.JSSignResult result, MatrixStack matrices, VertexConsumerProvider vertexConsumers, Direction facing, float size, float x, boolean flipCustomText, boolean transparentBlack, int backgroundColor, float y) {
		if (result.isError()) {
			renderCustomTextCRT(result.getText(), matrices, vertexConsumers, facing, size, x, flipCustomText, size, backgroundColor, FontType.ALIBABA, false, true, false, (SignTextStyleConfig.SignTextStyle) null, true, 1.0F, new Color(16711680));
		} else {
			int textColor = result.getTextColor() != 0 ? result.getTextColor() : (transparentBlack ? 0 : (isLightBackground(backgroundColor == 0 ? -1 : backgroundColor | -16777216) ? 0 : -1));
			Color finalTextColor = transparentBlack ? TEXT_COLOR : new Color(textColor, true);
			if (transparentBlack) {
				renderCustomTextCRT(result.getText(), matrices, vertexConsumers, facing, size, x, false, size, 0, FONT_TYPE, false, true, false, (SignTextStyleConfig.SignTextStyle) null, false, result.getTextSize(), TEXT_COLOR, y, 0.0F);
			} else {
				SignTextStyleConfig.SignTextStyle style = new SignTextStyleConfig.SignTextStyle(textColor, result.getTextSize(), result.isTextBold());
				int bg = result.getBackgroundColor() != 0 ? result.getBackgroundColor() : backgroundColor;
				renderCustomTextCRT(result.getText(), matrices, vertexConsumers, facing, size, x, flipCustomText, size, bg, FONT_TYPE, false, true, false, style, true, 1.0F, finalTextColor);
			}
		}
	}

	public static boolean isJSSign(String signId) {
		return signId != null && signId.startsWith("crt_js_");
	}

	public static void renderJSTextInBox(String text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, Direction facing, float boxX, float boxY, float boxW, float boxH, int color, float scale, boolean bold, boolean centered) {
		if (text != null && !text.isEmpty() && !(boxW <= 0.0F) && !(boxH <= 0.0F)) {
			SignTextStyleConfig.SignTextStyle style = new SignTextStyleConfig.SignTextStyle(color, scale, bold);
			renderCustomTextCRT(text, matrices, vertexConsumers, facing, boxH, boxX, false, boxW, 0, FONT_TYPE, false, centered, false, style, false, 1.0F, new Color(color, true), boxY, boxW);
		}
	}

	public static void renderJSTexture(Identifier textureId, MatrixStack matrices, VertexConsumerProvider vertexConsumers, Direction facing, float x, float y, float w, float h, int color) {
		if (!(w <= 0.0F) && !(h <= 0.0F)) {
			drawTexturedRect(matrices, vertexConsumers, textureId, x, y, w, h, 0.0F, 0.0F, 1.0F, 1.0F, facing, color);
		}
	}

	public static void renderJSRect(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Direction facing, float x, float y, float w, float h, int color) {
		if (!(w <= 0.0F) && !(h <= 0.0F)) {
			drawTexturedRect(matrices, vertexConsumers, WHITE_TEXTURE, x, y, w, h, facing, color);
		}
	}

	public static void renderJSLine(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Direction facing, float x1, float y1, float x2, float y2, float thickness, int color) {
		float dx = x2 - x1;
		float dy = y2 - y1;
		float length = (float) Math.sqrt((double) (dx * dx + dy * dy));
		if (!(length <= 1.0E-4F) && !(thickness <= 0.0F)) {
			float angle = (float) Math.atan2((double) dy, (double) dx);
			float midX = (x1 + x2) / 2.0F;
			float midY = (y1 + y2) / 2.0F;
			matrices.push();
			final RenderLayer renderLayer = MoreRenderLayers.getLight(WHITE_TEXTURE, true);
			final VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);
			matrices.translate(midX, midY, 0.0F);
			matrices.multiply(new Quaternionf().rotationZ(angle));
			IDrawing.drawTexture(matrices, vertexConsumer, -length / 2.0F, -thickness / 2.0F, length, thickness, 0.0F, 0.0F, 1.0F, 1.0F, facing, color, 15728880);
			matrices.pop();
		}
	}

	static {
		FONT_TYPE = FontType.ALIBABA;
		TEXT_COLOR = new Color(0, 0, 0);
		WHITE_TEXTURE = new Identifier("mtr", "textures/block/white.png");
		lastJSRefreshTime = 0L;
	}
}