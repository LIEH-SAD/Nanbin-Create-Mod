package com.Nanbin.client.Screen;

import com.Nanbin.Registry.RegBlock.BlockBusTicketProcessor;
import com.Nanbin.mapping.Registry;
import com.Nanbin.packet.PacketUpdateBusTicketProcessor;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mapping.tool.TextCase;
import org.mtr.mod.data.IGui;
import org.mtr.mod.screen.MTRScreenBase;

public class BusTicketProcessorScreen extends MTRScreenBase implements IGui {
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 100;
    private static final int MARGIN = 12;
    private static final int FIELD_HEIGHT = 18;
    private static final int BUTTON_HEIGHT = 20;
    private static final int FIELD_MAX_LENGTH = 6;
    private static final int MIN_AMOUNT = 1;
    private static final int MAX_AMOUNT = 999999;
    private static final String[] MODE_NAMES = {"gui.nanbin.ticket_processer.two_tap", "gui.nanbin.ticket_processer.fixed"};

    private final BlockPos blockPos;
    private int mode;
    private int amount;

    private ButtonWidgetExtension modeButton;
    private TextFieldWidgetExtension amountField;

    public BusTicketProcessorScreen(BlockPos blockPos, int mode, int amount) {
        this.blockPos = blockPos;
        this.mode = mode;
        this.amount = amount;
    }

    protected void init2() {
        super.init2();

        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;
        int fieldWidth = GUI_WIDTH - MARGIN * 2;
        int componentX = guiLeft + MARGIN;

        modeButton = new ButtonWidgetExtension(componentX, guiTop + 34, fieldWidth, BUTTON_HEIGHT,
                TextHelper.translatable(MODE_NAMES[mode]), (button) -> {
                    mode = (mode + 1) % MODE_NAMES.length;
                    modeButton.setMessage2(Text.cast(TextHelper.translatable(MODE_NAMES[mode])));
                    amountField.setEditable2(mode == BlockBusTicketProcessor.MODE_FIXED_AMOUNT);
                });

        amountField = new TextFieldWidgetExtension(componentX, guiTop + 64, fieldWidth, FIELD_HEIGHT,
                FIELD_MAX_LENGTH, TextCase.DEFAULT, null, null);
        amountField.setText2(String.valueOf(amount));
        amountField.setEditable2(mode == BlockBusTicketProcessor.MODE_FIXED_AMOUNT);

        addChild(new ClickableWidget(modeButton));
        addChild(new ClickableWidget(amountField));
    }

    public void onClose2() {
        int parsedAmount = BlockBusTicketProcessor.DEFAULT_AMOUNT;
        try {
            parsedAmount = Math.max(MIN_AMOUNT, Math.min(MAX_AMOUNT, Integer.parseInt(amountField.getText2().trim())));
        } catch (NumberFormatException ignored) {
        }
        amount = parsedAmount;
        Registry.sendPacketToServer(new PacketUpdateBusTicketProcessor(blockPos, mode, amount));
        super.onClose2();
    }

    public void tick2() {
        amountField.tick2();
        if (mode == BlockBusTicketProcessor.MODE_FIXED_AMOUNT) {
            String text = amountField.getText2();
            if (!text.isEmpty()) {
                try {
                    int val = Integer.parseInt(text);
                    amount = Math.max(MIN_AMOUNT, Math.min(MAX_AMOUNT, val));
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphicsHolder);

        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;

        graphicsHolder.drawCenteredText(TextHelper.translatable("gui.nanbin.ticket_processer"), this.width / 2, guiTop + 12, ARGB_WHITE);

        if (mode != BlockBusTicketProcessor.MODE_FIXED_AMOUNT) {
            graphicsHolder.drawText(TextHelper.translatable("gui.nanbin.ticket_processer.two_tap.disabled"),
                    guiLeft + MARGIN, guiTop + 88, 0x808080, false, GraphicsHolder.getDefaultLight());
        }

        super.render(graphicsHolder, mouseX, mouseY, delta);
    }

    public boolean isPauseScreen2() {
        return false;
    }
}