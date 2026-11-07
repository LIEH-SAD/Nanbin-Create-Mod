package com.Nanbin.client.Screen;

import com.Nanbin.Init;
import com.Nanbin.client.ClientData.NanbinClientConfig;
import com.Nanbin.client.Drawing.RouteMapOverride;
import com.Nanbin.uiHelper.NanbinUIScreen;
import com.Nanbin.uiHelper.NanbinUISelectButtonItem;
import com.Nanbin.uiHelper.NanbinUITextFieldItem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.joml.Matrix4f;

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

        // 外观：深色模式开关（使用 uiHelper 的贴图按钮）
        addComponent(new NanbinUISelectButtonItem(Text.translatable("gui.nanbin.config.dark_mode"),
                NanbinClientConfig::isDarkMode, this::toggleDarkMode));

        // 贡献者
        addTitle(Text.translatable("gui.nanbin.config.contributors"));
        addConfig(Text.literal("LIEH-SAD"), () -> openUrl("https://www.mcmod.cn/author/38799.html"))
                .setIcon(new Identifier("nanbin:contributors/ls.png"));
        addConfig(Text.literal("jh1145"), () -> openUrl("https://space.bilibili.com/1397141452?"))
                .setIcon(new Identifier("nanbin:contributors/jh.png"));
        addConfig(Text.literal("Jacob"), () -> openUrl("https://afdian.com/a/0101P"))
                .setIcon(new Identifier("nanbin:contributors/jb.png"));
        addConfig(Text.literal("2伍溜柒"), () -> openUrl("https://space.bilibili.com/3546582999894124"))
                .setIcon(new Identifier("nanbin:contributors/2wu.png"));
        addConfig(Text.literal("厦航在拍车的无名机长"), () -> openUrl("https://space.bilibili.com/3546725140663173"))
                .setIcon(new Identifier("nanbin:contributors/hz.png"));
    }

    private void toggleDarkMode() {
        NanbinClientConfig.setDarkMode(!NanbinClientConfig.isDarkMode());
        // 立即应用，下次打开编辑器就是切换后的主题
        com.Nanbin.client.CRTRailwaySignEditor.editor.CRTEditorTheme.applyDarkMode(NanbinClientConfig.isDarkMode());
    }

    @Override
    public void renderBackground(DrawContext context) {
        drawTexture(context.getMatrices(), BACKGROUND_TEXTURE, 0, 0, this.width, this.height);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        try {
            super.render(context, mouseX, mouseY, delta);
            this.drawLogoAndVersion(context);
            this.drawPoem(context, this.width / 2, getPanelY() + getPanelHeight() + BLOCK_GAP);
        } catch (Exception e) {
            Init.LOGGER.error("", e);
        }
    }

    @Override
    public void close() {
        this.saveRouteMapUrl();
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

    /** 顶部区域：LOGO + 加大的模组名（1.5×）+ 版本号；整组在面板上方水平居中，文字块与 LOGO 垂直居中。 */
    private void drawLogoAndVersion(DrawContext context) {
        final MutableText nameText = Text.literal(Init.MOD_NAME);
        final MutableText versionText = Text.literal(Init.FINAL_VERSION);
        final int nameWidth = Math.round(this.textRenderer.getWidth(nameText) * NAME_SCALE);
        final int nameHeight = Math.round(8 * NAME_SCALE);
        final int versionWidth = this.textRenderer.getWidth(versionText);
        final int textWidth = Math.max(nameWidth, versionWidth);
        final float groupWidth = LOGO_SIZE + LOGO_GAP + textWidth;
        final int topY = Math.max(2, (getPanelY() - LOGO_SIZE) / 2);

        final MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate((this.width - groupWidth) / 2.0F, (float) topY, 0.0F);

        drawTexture(matrices, HEADER_LOGO, 0, 0, LOGO_SIZE, LOGO_SIZE);

        matrices.translate(LOGO_SIZE + LOGO_GAP, 0.0F, 0.0F);

        final int textHeight = nameHeight + NAME_GAP + 8;
        final int textTop = (LOGO_SIZE - textHeight) / 2;

        matrices.push();
        matrices.translate(0.0F, (float) textTop, 0.0F);
        matrices.scale(NAME_SCALE, NAME_SCALE, 1.0F);
        context.drawTextWithShadow(this.textRenderer, nameText, 0, 0, -1);
        matrices.pop();

        context.drawTextWithShadow(this.textRenderer, versionText, 0, textTop + nameHeight + NAME_GAP, -1);

        matrices.pop();
    }

    private void drawPoem(DrawContext context, int centerX, int startY) {
        final MutableText line1 = Text.literal("谁家今夜扁舟子?");
        final MutableText line2 = Text.literal("何处相思明月楼?");
        final MutableText line0 = Text.literal("This is a test version, please do not use it in production environment.");
        final int lineSpacing = 18;

        final MatrixStack matrices = context.getMatrices();
        if (Init.IS_TEST_VERSION) {
            matrices.push();
            final int line0Width = this.textRenderer.getWidth(line0);
            matrices.translate((float) (centerX - line0Width / 2), (float) (startY + lineSpacing), 0.0);
            context.drawTextWithShadow(this.textRenderer, line0, 0, 0, 154);
            matrices.pop();
        } else {
            matrices.push();
            final int line1Width = this.textRenderer.getWidth(line1);
            matrices.translate((float) (centerX - line1Width / 2), (float) startY, 0.0);
            context.drawTextWithShadow(this.textRenderer, line1, 0, 0, -1);
            matrices.pop();

            matrices.push();
            final int line2Width = this.textRenderer.getWidth(line2);
            matrices.translate((float) (centerX - line2Width / 2), (float) (startY + lineSpacing), 0.0);
            context.drawTextWithShadow(this.textRenderer, line2, 0, 0, -1);
            matrices.pop();
        }
    }

    private void drawTexture(MatrixStack matrices, Identifier texture, float x, float y, float width, float height) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        bufferBuilder.vertex(matrix, x, y + height, 0.0F).texture(0.0F, 1.0F).next();
        bufferBuilder.vertex(matrix, x + width, y + height, 0.0F).texture(1.0F, 1.0F).next();
        bufferBuilder.vertex(matrix, x + width, y, 0.0F).texture(1.0F, 0.0F).next();
        bufferBuilder.vertex(matrix, x, y, 0.0F).texture(0.0F, 0.0F).next();

        tessellator.draw();
    }
}
