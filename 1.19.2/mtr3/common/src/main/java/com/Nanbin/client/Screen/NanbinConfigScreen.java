package com.Nanbin.client.Screen;

import com.Nanbin.Init;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;

public class NanbinConfigScreen extends Screen {

    private static final Identifier HEADER_LOGO = new Identifier("nanbin:logo.png");
    private static final Identifier BACKGROUND_TEXTURE = new Identifier("nanbin:textures/gui/background.png");

    private final Screen parent;

    public NanbinConfigScreen() {
        super(Text.literal("Nanbin Create Mod"));
        this.parent = null;
    }

    public NanbinConfigScreen(Screen parent) {
        super(Text.literal("Nanbin Create Mod"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        try {
            this.drawBackground(matrices);
            this.drawHeader(matrices);
            super.render(matrices, mouseX, mouseY, delta);
        } catch (Exception e) {
            Init.LOGGER.error("", e);
        }
    }

    @Override
    public void close() {
        if (this.client != null && this.parent != null) {
            this.client.setScreen(this.parent);
        } else {
            super.close();
        }
    }

    private void drawBackground(MatrixStack matrices) {
        drawTexture(matrices, BACKGROUND_TEXTURE,
                0, 0, this.width, this.height,
                0.0F, 0.0F, 1.0F, 1.0F);
    }

    private void drawHeader(MatrixStack matrices) {
        final MutableText titleText = Text.literal("Nanbin Create Mod");
        final int centerX = this.width / 2;
        final int headerTopY = (this.height - 40) / 2;

        matrices.push();
        matrices.translate(
                ((float) this.width - (float) this.textRenderer.getWidth(titleText) * 1.5F - 40.0F - 6.0F) / 2.0F,
                (float) headerTopY,
                0.0F
        );

        drawTexture(matrices, HEADER_LOGO,
                0, 0, 40, 40,
                0.0F, 0.0F, 1.0F, 1.0F);

        matrices.translate(40.0F, 0.0F, 0.0F);
        matrices.translate(6.0F, 0.0F, 0.0F);

        matrices.push();
        matrices.scale(1.5F, 1.5F, 1.5F);
        drawTextWithShadow(matrices, this.textRenderer, titleText, 0, 6, -1);
        matrices.pop();

        drawTextWithShadow(matrices, this.textRenderer, Text.literal(Init.FINAL_VERSION), 0, 24, -1);

        matrices.pop();

        this.drawPoem(matrices, centerX, headerTopY + 40 + 24);
    }

    private void drawPoem(MatrixStack matrices, int centerX, int startY) {
        final MutableText line1 = Text.literal("谁家今夜扁舟子?");
        final MutableText line2 = Text.literal("何处相思明月楼?");
        final int lineSpacing = 18;

        matrices.push();
        final int line1Width = this.textRenderer.getWidth(line1);
        matrices.translate((float) (centerX - line1Width / 2), (float) startY, 0.0);
        drawTextWithShadow(matrices, this.textRenderer, line1, 0, 0, -1);
        matrices.pop();

        matrices.push();
        final int line2Width = this.textRenderer.getWidth(line2);
        matrices.translate((float) (centerX - line2Width / 2), (float) (startY + lineSpacing), 0.0);
        drawTextWithShadow(matrices, this.textRenderer, line2, 0, 0, -1);
        matrices.pop();
    }

    private void drawTexture(MatrixStack matrices, Identifier texture,
                             float x, float y, float width, float height,
                             float u, float v, float uWidth, float vHeight) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        bufferBuilder.vertex(matrix, x, y + height, 0.0F).texture(u, v + vHeight).next();
        bufferBuilder.vertex(matrix, x + width, y + height, 0.0F).texture(u + uWidth, v + vHeight).next();
        bufferBuilder.vertex(matrix, x + width, y, 0.0F).texture(u + uWidth, v).next();
        bufferBuilder.vertex(matrix, x, y, 0.0F).texture(u, v).next();

        tessellator.draw();
    }
}