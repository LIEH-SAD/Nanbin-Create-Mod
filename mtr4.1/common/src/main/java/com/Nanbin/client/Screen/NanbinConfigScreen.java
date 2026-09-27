package com.Nanbin.client.Screen;

import com.Nanbin.Init;
import com.Nanbin.client.ClientData.NanbinClientConfig;
import com.Nanbin.client.Drawing.RouteMapOverride;
import com.Nanbin.mapping.GraphicsHolder;
import com.Nanbin.mapping.GuiDrawing;
import com.Nanbin.uiHelper.NanbinUIScreen;
import com.Nanbin.uiHelper.NanbinUITextFieldItem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.net.URI;

public class NanbinConfigScreen extends NanbinUIScreen {

	private static final Identifier HEADER_LOGO = Identifier.of("nanbin", "logo.png");
	private static final Identifier BACKGROUND_TEXTURE = Identifier.of("nanbin", "textures/gui/background.png");
	private static final int LOGO_SIZE = 40;
	private static final int LOGO_GAP = 6;
	private static final int BLOCK_GAP = 6;
	private static final int ROUTE_MAP_URL_MAX_LENGTH = 512;
	private static final String ROUTE_MAP_URL_SUGGESTION = "https://";

	private NanbinUITextFieldItem routeMapUrlField;

	public NanbinConfigScreen() {
		this(null);
	}

	public NanbinConfigScreen(Screen parent) {
		super(Text.literal("Nanbin Create Mod"), parent);
	}

	@Override
	protected void buildComponents() {
		addTitle(Text.translatable("gui.nanbin.config.title"));
		// 左侧名称、右侧文本框
		routeMapUrlField = addTextField(Text.translatable("gui.nanbin.config.route_map"),
				NanbinClientConfig.getRouteMapUrl(), ROUTE_MAP_URL_MAX_LENGTH)
				.setSuggestion(ROUTE_MAP_URL_SUGGESTION);

		// 贡献者
		addTitle(Text.translatable("gui.nanbin.config.contributors"));
		addConfig(Text.literal("LIEH-SAD"), () -> openUrl("https://www.mcmod.cn/author/38799.html"))
				.setIcon(Identifier.of("nanbin", "contributors/ls.png"));
		addConfig(Text.literal("jh1145"), () -> openUrl("https://space.bilibili.com/1397141452?"))
				.setIcon(Identifier.of("nanbin", "contributors/jh.png"));
		addConfig(Text.literal("Jacob"), () -> openUrl("https://afdian.com/a/0101P"))
				.setIcon(Identifier.of("nanbin", "contributors/jb.png"));
	}

	@Override
	protected void drawBackground(GraphicsHolder graphicsHolder) {
		drawTexture(graphicsHolder, BACKGROUND_TEXTURE, 0, 0, width, height);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		try {
			super.render(context, mouseX, mouseY, delta);
			final GraphicsHolder graphicsHolder = new GraphicsHolder(context);
			drawLogoAndVersion(graphicsHolder);
			drawPoem(graphicsHolder, width / 2, getPanelY() + getPanelHeight() + BLOCK_GAP);
		} catch (Exception e) {
			Init.LOGGER.error("NanbinConfigScreen: render failed", e);
		}
	}

	@Override
	public void close() {
		saveRouteMapUrl();
		super.close();
	}

	private void saveRouteMapUrl() {
		if (routeMapUrlField == null) {
			return;
		}
		final String url = routeMapUrlField.getText().trim();
		if (url.equals(NanbinClientConfig.getRouteMapUrl())) {
			return;
		}
		NanbinClientConfig.setRouteMapUrl(url);
		RouteMapOverride.apply(url);
	}

	/** 用系统默认浏览器打开网址；只允许 http/https，避免 file:、jar: 等协议被系统执行。 */
	private static void openUrl(String url) {
		try {
			final URI uri = new URI(url);
			final String scheme = uri.getScheme();
			if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
				Init.LOGGER.error("NanbinConfigScreen: refused to open non-http url {}", url);
				return;
			}
			Util.getOperatingSystem().open(uri);
		} catch (Exception e) {
			Init.LOGGER.error("NanbinConfigScreen: failed to open url {}", url, e);
		}
	}

	private void drawLogoAndVersion(GraphicsHolder graphicsHolder) {
		final MutableText versionText = Text.literal(Init.FINAL_VERSION);
		final float groupWidth = LOGO_SIZE + LOGO_GAP + GraphicsHolder.getTextWidth(versionText);
		final int topY = Math.max(2, (getPanelY() - LOGO_SIZE) / 2);

		graphicsHolder.push();
		graphicsHolder.translate((width - groupWidth) / 2.0F, topY, 0.0F);

		drawTexture(graphicsHolder, HEADER_LOGO, 0, 0, LOGO_SIZE, LOGO_SIZE);

		graphicsHolder.translate(LOGO_SIZE + LOGO_GAP, 0.0F, 0.0F);
		graphicsHolder.drawText(versionText, 0, (LOGO_SIZE - 8) / 2, -1, true, GraphicsHolder.getDefaultLight());

		graphicsHolder.pop();
	}

	private void drawPoem(GraphicsHolder graphicsHolder, int centerX, int startY) {
		final MutableText line0 = Text.literal("This is a test version, please do not use it in production environment.");
		final MutableText line1 = Text.literal("谁家今夜扁舟子？");
		final MutableText line2 = Text.literal("何处相思明月楼？");
		final int lineSpacing = 18;

		if (Init.IS_TEST_VERSION) {
			graphicsHolder.push();
			final int line0Width = GraphicsHolder.getTextWidth(line0);
			graphicsHolder.translate(centerX - line0Width / 2.0F, startY + lineSpacing, 0.0F);
			graphicsHolder.drawText(line0, 0, 0, 154, true, GraphicsHolder.getDefaultLight());
			graphicsHolder.pop();
		} else {
			graphicsHolder.push();
			final int line1Width = GraphicsHolder.getTextWidth(line1);
			graphicsHolder.translate(centerX - line1Width / 2.0F, startY, 0.0F);
			graphicsHolder.drawText(line1, 0, 0, -1, true, GraphicsHolder.getDefaultLight());
			graphicsHolder.pop();

			graphicsHolder.push();
			final int line2Width = GraphicsHolder.getTextWidth(line2);
			graphicsHolder.translate(centerX - line2Width / 2.0F, startY + lineSpacing, 0.0F);
			graphicsHolder.drawText(line2, 0, 0, -1, true, GraphicsHolder.getDefaultLight());
			graphicsHolder.pop();
		}
	}

	private static void drawTexture(GraphicsHolder graphicsHolder, Identifier texture, float x, float y, float width, float height) {
		// 先把着色器颜色重置为白色，避免贴图被上一步的着色器颜色染色
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
		guiDrawing.beginDrawingTexture(texture);
		guiDrawing.drawTexture(x, y, x + width, y + height, 0.0F, 0.0F, 1.0F, 1.0F);
		guiDrawing.finishDrawingTexture();
	}
}
