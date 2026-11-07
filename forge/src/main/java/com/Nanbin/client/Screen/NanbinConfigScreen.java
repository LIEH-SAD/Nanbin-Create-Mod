package com.Nanbin.client.Screen;

import com.Nanbin.Init;
import com.Nanbin.client.ClientData.NanbinClientConfig;
import com.Nanbin.client.Drawing.RouteMapOverride;
import com.Nanbin.uiHelper.NanbinUIScreen;
import com.Nanbin.uiHelper.NanbinUITextFieldItem;
import com.mojang.blaze3d.systems.RenderSystem;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.holder.Util;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.mapper.TextHelper;

import java.net.URI;

/**
 * Nanbin 配置界面（纯客户端）。
 *
 * <p>沿用 NanbinUI 样式：大标题在面板顶部居中，配置项是文本框类型（不是二级菜单）。
 * 背景图与 LOGO 保留：LOGO + 版本号显示在面板上方，诗句显示在面板下方。
 */
public class NanbinConfigScreen extends NanbinUIScreen {

    private static final Identifier HEADER_LOGO = new Identifier("nanbin:logo.png");
    private static final Identifier BACKGROUND_TEXTURE = new Identifier("nanbin:textures/gui/background.png");
    private static final int LOGO_SIZE = 40;
    private static final int LOGO_GAP = 6;
    private static final float NAME_SCALE = 1.5F;
    private static final int NAME_GAP = 2;
    private static final int BLOCK_GAP = 6;
    private static final int ROUTE_MAP_URL_MAX_LENGTH = 512;
    private static final String ROUTE_MAP_URL_SUGGESTION = "https://";

    private NanbinUITextFieldItem routeMapUrlField;

    public NanbinConfigScreen() {
        this((Screen) null);
    }

    public NanbinConfigScreen(Screen parent) {
        super(TextHelper.literal("Nanbin Create Mod"), parent);
    }

    @Override
    protected void buildComponents() {
        addTitle(TextHelper.translatable("gui.nanbin.config.title"));
        // 左侧名称、右侧文本框
        routeMapUrlField = addTextField(TextHelper.translatable("gui.nanbin.config.route_map"),
                        NanbinClientConfig.getRouteMapUrl(), ROUTE_MAP_URL_MAX_LENGTH)
                .setSuggestion(ROUTE_MAP_URL_SUGGESTION);

        // 外观：深色模式开关（使用 uiHelper 的贴图按钮）
        addSelectButton(TextHelper.translatable("gui.nanbin.config.dark_mode"),
                NanbinClientConfig::isDarkMode, this::toggleDarkMode);

        // 贡献者
        addTitle(TextHelper.translatable("gui.nanbin.config.contributors"));
        addConfig(TextHelper.literal("LIEH-SAD"), () -> openUrl("https://www.mcmod.cn/author/38799.html"))
                .setIcon(new Identifier("nanbin:contributors/ls.png"));
        addConfig(TextHelper.literal("jh1145"), () -> openUrl("https://space.bilibili.com/1397141452?"))
                .setIcon(new Identifier("nanbin:contributors/jh.png"));
        addConfig(TextHelper.literal("Jacob"), () -> openUrl("https://afdian.com/a/0101P"))
                .setIcon(new Identifier("nanbin:contributors/jb.png"));
        addConfig(TextHelper.literal("2伍溜柒"), () -> openUrl("https://space.bilibili.com/3546582999894124"))
                .setIcon(new Identifier("nanbin:contributors/2wu.png"));
    }

    @Override
    protected void drawBackground(GraphicsHolder graphicsHolder) {
        drawTexture(graphicsHolder, BACKGROUND_TEXTURE, 0, 0, getWidthMapped(), getHeightMapped());
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        try {
            super.render(graphicsHolder, mouseX, mouseY, delta);
            this.drawLogoAndVersion(graphicsHolder);
            this.drawPoem(graphicsHolder, getWidthMapped() / 2, getPanelY() + getPanelHeight() + BLOCK_GAP);
        } catch (Exception e) {
            Init.LOGGER.error("", e);
        }
    }

    @Override
    public void onClose2() {
        this.saveRouteMapUrl();
        super.onClose2();
    }

    private void toggleDarkMode() {
        NanbinClientConfig.setDarkMode(!NanbinClientConfig.isDarkMode());
        // 立即应用，下次打开编辑器就是切换后的主题
        com.Nanbin.client.CRTRailwaySignEditor.editor.CRTEditorTheme.applyDarkMode(NanbinClientConfig.isDarkMode());
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

    /** 顶部区域：LOGO + 加大的模组名（1.5×）+ 版本号；整组在面板上方水平居中，文字块与 LOGO 垂直居中。 */
    private void drawLogoAndVersion(GraphicsHolder graphicsHolder) {
        final MutableText nameText = TextHelper.literal(Init.MOD_NAME);
        final MutableText versionText = TextHelper.literal(Init.FINAL_VERSION);
        final int nameWidth = Math.round(GraphicsHolder.getTextWidth(nameText) * NAME_SCALE);
        final int nameHeight = Math.round(8 * NAME_SCALE);
        final int versionWidth = GraphicsHolder.getTextWidth(versionText);
        final int textWidth = Math.max(nameWidth, versionWidth);
        final float groupWidth = LOGO_SIZE + LOGO_GAP + textWidth;
        final int topY = Math.max(2, (getPanelY() - LOGO_SIZE) / 2);

        graphicsHolder.push();
        graphicsHolder.translate((getWidthMapped() - groupWidth) / 2.0F, topY, 0.0F);

        drawTexture(graphicsHolder, HEADER_LOGO, 0, 0, LOGO_SIZE, LOGO_SIZE);

        graphicsHolder.translate(LOGO_SIZE + LOGO_GAP, 0.0F, 0.0F);

        final int textHeight = nameHeight + NAME_GAP + 8;
        final int textTop = (LOGO_SIZE - textHeight) / 2;

        graphicsHolder.push();
        graphicsHolder.translate(0.0F, textTop, 0.0F);
        graphicsHolder.scale(NAME_SCALE, NAME_SCALE, 1.0F);
        graphicsHolder.drawText(nameText, 0, 0, -1, true, GraphicsHolder.getDefaultLight());
        graphicsHolder.pop();

        graphicsHolder.drawText(versionText, 0, textTop + nameHeight + NAME_GAP, -1, true, GraphicsHolder.getDefaultLight());

        graphicsHolder.pop();
    }

    private void drawPoem(GraphicsHolder graphicsHolder, int centerX, int startY) {
        final MutableText line1 = TextHelper.literal("谁家今夜扁舟子?");
        final MutableText line2 = TextHelper.literal("何处相思明月楼?");
        final MutableText line0 = TextHelper.literal("This is a test version, please do not use it in production environment.");
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
        // 参考实现会先把着色器颜色重置为白色，避免贴图被上一步的着色器颜色染色
        //? if <1.17 {
        /*RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        *///?} else {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        //?}
        final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
        guiDrawing.beginDrawingTexture(texture);
        guiDrawing.drawTexture(x, y, x + width, y + height, 0.0F, 0.0F, 1.0F, 1.0F);
        guiDrawing.finishDrawingTexture();
    }
}
