
package com.Nanbin.client.Menu;

import com.Nanbin.entrypoint;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.*;
import org.mtr.mod.Init;
import org.mtr.mod.Patreon;
import org.mtr.mod.client.DynamicTextureCache;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.config.Client;
import org.mtr.mod.config.Config;
import org.mtr.mod.data.IGui;
import org.mtr.mod.generated.lang.TranslationProvider;
import org.mtr.mod.screen.MTRScreenBase;
import org.mtr.mod.screen.WidgetShorterSlider;

import java.util.function.IntConsumer;

public class NanbinConfigScreen extends MTRScreenBase implements IGui {
    private final Client client = Config.getClient();
    private final ButtonWidgetExtension buttonShowAnnouncementMessages = new ButtonWidgetExtension(0, 0, 0, 16, TextHelper.literal("Menu.nanbin.sam"), (button) -> {
        this.client.toggleChatAnnouncements();
        setButtonText(button, this.client.getChatAnnouncements());
    });
    private final ButtonWidgetExtension buttonUseTTSAnnouncements = new ButtonWidgetExtension(0, 0, 0, 16, TextHelper.literal("Menu.nanbin.uta"), (button) -> {
        this.client.toggleTextToSpeechAnnouncements();
        setButtonText(button, this.client.getTextToSpeechAnnouncements());
    });
    private final ButtonWidgetExtension buttonHideTranslucentParts = new ButtonWidgetExtension(0, 0, 0, 16, TextHelper.literal("Menu.nanbin.htp"), (button) -> {
        this.client.toggleHideTranslucentParts();
        setButtonText(button, this.client.getHideTranslucentParts());
    });
    private final ButtonWidgetExtension buttonLanguageOptions = new ButtonWidgetExtension(0, 0, 0, 16, TextHelper.literal("Menu.nanbin.lang"), (button) -> {
        this.client.cycleLanguageDisplay();
        button.setMessage(this.client.getLanguageDisplay().translationKey.getText(new Object[0]));
    });
    private final WidgetShorterSlider sliderDynamicTextureResolution = new WidgetShorterSlider(0, 0, 7, String::valueOf, (IntConsumer)null);
    private final WidgetShorterSlider sliderTrainOscillationMultiplier = new WidgetShorterSlider(0, 0, 15, (i) -> i * 10 + "%", (IntConsumer)null);
    private final ButtonWidgetExtension buttonDefaultRail3D = new ButtonWidgetExtension(0, 0, 0, 16, TextHelper.literal("Menu.nanbin.3drail"), (button) -> {
        this.client.toggleDefaultRail3D();
        setButtonText(button, this.client.getDefaultRail3D());
    });
    private final ButtonWidgetExtension buttonUseMTRFont = new ButtonWidgetExtension(0, 0, 0, 16, TextHelper.literal("Menu.nanbin.font"), (button) -> {
        this.client.toggleUseMTRFont();
        setButtonText(button, this.client.getUseMTRFont());
    });
    private final ButtonWidgetExtension buttonDisableShadowsForShaders = new ButtonWidgetExtension(0, 0, 0, 16, TextHelper.literal("Menu.nanbin.dss"), (button) -> {
        this.client.toggleDisableShadowsForShaders();
        setButtonText(button, this.client.getDisableShadowsForShaders());
    });
    private final ButtonWidgetExtension buttonSupportPatreon = new ButtonWidgetExtension(0, 0, 0, 16, TextHelper.literal("Menu.nanbin.support"), (button) -> Util.getOperatingSystem().open("https://afdian.com/a/sadliehbilibili"));
    private static final Identifier HEADER_LOGO = new Identifier("nanbin:logo.png");
    private static final int HEADER_LOGO_SIZE = 40;
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 16;

    public NanbinConfigScreen() {
        super();
    }

    protected void init2() {
        super.init2();
        int startY = 6;
        int i = 1;
        IDrawing.setPositionAndWidth(this.buttonShowAnnouncementMessages, this.width - 20 - 60, startY + 16 * i++ + 20, 60);
        IDrawing.setPositionAndWidth(this.buttonUseTTSAnnouncements, this.width - 20 - 60, startY + 16 * i++ + 20, 60);
        IDrawing.setPositionAndWidth(this.buttonHideTranslucentParts, this.width - 20 - 60, startY + 16 * i++ + 20, 60);
        IDrawing.setPositionAndWidth(this.buttonLanguageOptions, this.width - 20 - 60, startY + 16 * i++ + 20, 60);
        IDrawing.setPositionAndWidth(this.sliderDynamicTextureResolution, this.width - 20 - 60, startY + 16 * i++ + 20, 54 - GraphicsHolder.getTextWidth("100%"));
        IDrawing.setPositionAndWidth(this.sliderTrainOscillationMultiplier, this.width - 20 - 60, startY + 16 * i++ + 20, 54 - GraphicsHolder.getTextWidth("100%"));
        IDrawing.setPositionAndWidth(this.buttonDefaultRail3D, this.width - 20 - 60, startY + 16 * i++ + 20, 60);
        IDrawing.setPositionAndWidth(this.buttonUseMTRFont, this.width - 20 - 60, startY + 16 * i++ + 20, 60);
        IDrawing.setPositionAndWidth(this.buttonDisableShadowsForShaders, this.width - 20 - 60, startY + 16 * i++ + 20, 60);
        IDrawing.setPositionAndWidth(this.buttonSupportPatreon, this.width - 20 - 60, startY + 16 * i + 20, 60);
        setButtonText(new ButtonWidget(this.buttonShowAnnouncementMessages), this.client.getChatAnnouncements());
        setButtonText(new ButtonWidget(this.buttonUseTTSAnnouncements), this.client.getTextToSpeechAnnouncements());
        setButtonText(new ButtonWidget(this.buttonHideTranslucentParts), this.client.getHideTranslucentParts());
        setButtonText(new ButtonWidget(this.buttonDefaultRail3D), this.client.getDefaultRail3D());
        setButtonText(new ButtonWidget(this.buttonUseMTRFont), this.client.getUseMTRFont());
        setButtonText(new ButtonWidget(this.buttonDisableShadowsForShaders), this.client.getDisableShadowsForShaders());
        this.buttonLanguageOptions.setMessage2(this.client.getLanguageDisplay().translationKey.getText(new Object[0]));
        this.sliderDynamicTextureResolution.setHeight(16);
        this.sliderDynamicTextureResolution.setValue(this.client.getDynamicTextureResolution());
        this.sliderTrainOscillationMultiplier.setHeight(16);
        this.sliderTrainOscillationMultiplier.setValue((int)(this.client.getVehicleOscillationMultiplier() * (double)10.0F));
        this.buttonDefaultRail3D.active = OptimizedRenderer.hasOptimizedRendering();
        this.buttonSupportPatreon.setMessage2(TranslationProvider.GUI_MTR_SUPPORT.getText(new Object[0]));
        this.addChild(new ClickableWidget(this.buttonShowAnnouncementMessages));
        this.addChild(new ClickableWidget(this.buttonUseTTSAnnouncements));
        this.addChild(new ClickableWidget(this.buttonHideTranslucentParts));
        this.addChild(new ClickableWidget(this.buttonLanguageOptions));
        this.addChild(new ClickableWidget(this.sliderDynamicTextureResolution));
        this.addChild(new ClickableWidget(this.sliderTrainOscillationMultiplier));
        this.addChild(new ClickableWidget(this.buttonDefaultRail3D));
        this.addChild(new ClickableWidget(this.buttonUseMTRFont));
        this.addChild(new ClickableWidget(this.buttonDisableShadowsForShaders));
        this.addChild(new ClickableWidget(this.buttonSupportPatreon));
    }

    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        try {
            this.renderBackground(graphicsHolder);
            this.drawHeader(graphicsHolder);
            int yStart1 = 29;
            int i = 1;
            graphicsHolder.drawText(TextHelper.translatable("Menu.nanbin.sam"), 20, 16 * i++ + 29, -1, false, GraphicsHolder.getDefaultLight());
            graphicsHolder.drawText(TextHelper.translatable("Menu.nanbin.uta"), 20, 16 * i++ + 29, -1, false, GraphicsHolder.getDefaultLight());
            graphicsHolder.drawText(TextHelper.translatable("Menu.nanbin.htp"), 20, 16 * i++ + 29, -1, false, GraphicsHolder.getDefaultLight());
            graphicsHolder.drawText(TextHelper.translatable("Menu.nanbin.lang"), 20, 16 * i++ + 29, -1, false, GraphicsHolder.getDefaultLight());
            graphicsHolder.drawText(TextHelper.translatable("Menu.nanbin.dtr"), 20, 16 * i++ + 29, -1, false, GraphicsHolder.getDefaultLight());
            graphicsHolder.drawText(TextHelper.translatable("Menu.nanbin.tom"), 20, 16 * i++ + 29, -1, false, GraphicsHolder.getDefaultLight());
            graphicsHolder.drawText(TextHelper.translatable("Menu.nanbin.3drail"), 20, 16 * i++ + 29, -1, false, GraphicsHolder.getDefaultLight());
            graphicsHolder.drawText(TextHelper.translatable("Menu.nanbin.font"), 20, 16 * i++ + 29, -1, false, GraphicsHolder.getDefaultLight());
            graphicsHolder.drawText(TextHelper.translatable("Menu.nanbin.dss"), 20, 16 * i++ + 29, -1, false, GraphicsHolder.getDefaultLight());
            graphicsHolder.drawText(TextHelper.translatable("Menu.nanbin.support"), 20, 16 * i++ + 29, -154, false, GraphicsHolder.getDefaultLight());
            int yStart2 = 16 * (i + 1) + 29;
            String tierTitle = "";
            int y = 0;
            int x = 0;
            int maxWidth = 0;

            for(Patreon patreon : Patreon.PATREON_LIST) {
                if (!patreon.tierTitle.equals(tierTitle)) {
                    x += maxWidth + 6;
                    y = 0;
                    MutableText text = TextHelper.literal(patreon.tierTitle);
                    maxWidth = GraphicsHolder.getTextWidth(text);
                    graphicsHolder.drawText(text, 14 + x, yStart2, patreon.tierColor, false, GraphicsHolder.getDefaultLight());
                } else if (y + yStart2 + 8 + 20 > this.height) {
                    x += maxWidth + 6;
                    y = 0;
                    maxWidth = 0;
                }

                tierTitle = patreon.tierTitle;
                MutableText text = patreon.tierAmount < 1000 ? TranslationProvider.OPTIONS_MTR_ANONYMOUS.getMutableText(new Object[0]) : TextHelper.literal(patreon.name);
                maxWidth = Math.max(maxWidth, GraphicsHolder.getTextWidth(text));
                graphicsHolder.drawText(text, 14 + x, yStart2 + y + 8 + 6, -5592406, false, GraphicsHolder.getDefaultLight());
                y += 10;
            }

            super.render(graphicsHolder, mouseX, mouseY, delta);
        } catch (Exception e) {
            Init.LOGGER.error("", e);
        }

    }

    public void onClose2() {
        super.onClose2();
        this.client.setDynamicTextureResolution(this.sliderDynamicTextureResolution.getIntValue());
        this.client.setVehicleOscillationMultiplier((double)this.sliderTrainOscillationMultiplier.getIntValue() / (double)10.0F);
        DynamicTextureCache.instance.reload();
        Config.save();
    }

    private void drawHeader(GraphicsHolder graphicsHolder) {
        MutableText titleText = TextHelper.literal("Nanbin Create Mod");
        graphicsHolder.push();
        graphicsHolder.translate((double)(((float)this.getWidthMapped() - (float)GraphicsHolder.getTextWidth(titleText) * 1.5F - 40.0F - 6.0F) / 2.0F), (double)0.0F, (double)0.0F);
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
        graphicsHolder.drawText(entrypoint.FINAL_VERSION, 0, 24, -1, true, GraphicsHolder.getDefaultLight());
        graphicsHolder.pop();
    }

    private static void setButtonText(ButtonWidget button, boolean state) {
        button.setMessage((state ? TranslationProvider.OPTIONS_MTR_ON : TranslationProvider.OPTIONS_MTR_OFF).getText(new Object[0]));
    }
}
