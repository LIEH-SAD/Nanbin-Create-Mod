
package com.Nanbin.client.Screen;

import com.Nanbin.Init;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.*;
import org.mtr.mod.data.IGui;
import org.mtr.mod.screen.MTRScreenBase;

public class NanbinConfigScreen extends MTRScreenBase implements IGui {
    private static final Identifier HEADER_LOGO = new Identifier("nanbin:logo.png");
    private static final Identifier BACKGROUND_TEXTURE = new Identifier("nanbin:textures/gui/background.png");

    public NanbinConfigScreen() {
        super();
    }

    protected void init2() {
        super.init2();
    }

    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        try {
            this.drawBackground(graphicsHolder);
            this.drawHeader(graphicsHolder);
            super.render(graphicsHolder, mouseX, mouseY, delta);
        } catch (Exception e) {
            Init.LOGGER.error("", e);
        }
    }

    public void onClose2() {
        super.onClose2();
    }

    private void drawBackground(GraphicsHolder graphicsHolder) {
        GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
        guiDrawing.beginDrawingTexture(BACKGROUND_TEXTURE);
        guiDrawing.drawTexture(0.0F, 0.0F, (double)this.getWidthMapped(), (double)this.getHeightMapped(), 0.0F, 0.0F, 1.0F, 1.0F);
        guiDrawing.finishDrawingTexture();
    }

    private void drawHeader(GraphicsHolder graphicsHolder) {
        final MutableText titleText = TextHelper.literal("Nanbin Create Mod");
        final int centerX = this.getWidthMapped() / 2;
        final int headerTopY = (this.getHeightMapped() - 40) / 2;
        graphicsHolder.push();
        graphicsHolder.translate((double)(((float)this.getWidthMapped() - (float)GraphicsHolder.getTextWidth(titleText) * 1.5F - 40.0F - 6.0F) / 2.0F), (double)headerTopY, (double)0.0F);
        GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
        guiDrawing.beginDrawingTexture(HEADER_LOGO);
        guiDrawing.drawTexture((double)0.0F, (double)0.0F, (double)40.0F, (double)40.0F, 0.0F, 0.0F, 1.0F, 1.0F);
        guiDrawing.finishDrawingTexture();
        graphicsHolder.translate((double)40.0F, (double)0.0F, (double)0.0F);
        graphicsHolder.translate((double)6.0F, (double)0.0F, (double)0.0F);
        graphicsHolder.push();
        graphicsHolder.scale(1.5F, 1.5F, 1.5F);
        graphicsHolder.drawText(titleText, 0, 6, -1, true, GraphicsHolder.getDefaultLight());
        graphicsHolder.pop();
        graphicsHolder.drawText(Init.FINAL_VERSION, 0, 24, -1, true, GraphicsHolder.getDefaultLight());
        graphicsHolder.pop();

        this.drawPoem(graphicsHolder, centerX, headerTopY + 40 + 24);
    }

    private void drawPoem(GraphicsHolder graphicsHolder, int centerX, int startY) {
        final MutableText line1 = TextHelper.literal("谁家今夜扁舟子?");
        final MutableText line2 = TextHelper.literal("何处相思明月楼?");
        final int lineSpacing = 18;

        graphicsHolder.push();
        final int line1Width = GraphicsHolder.getTextWidth(line1);
        graphicsHolder.translate((double)(centerX - line1Width / 2), (double)startY, 0.0);
        graphicsHolder.drawText(line1, 0, 0, -1, true, GraphicsHolder.getDefaultLight());
        graphicsHolder.pop();

        graphicsHolder.push();
        final int line2Width = GraphicsHolder.getTextWidth(line2);
        graphicsHolder.translate((double)(centerX - line2Width / 2), (double)(startY + lineSpacing), 0.0);
        graphicsHolder.drawText(line2, 0, 0, -1, true, GraphicsHolder.getDefaultLight());
        graphicsHolder.pop();
    }
}
