package com.Nanbin.client.Screen;

import com.Nanbin.mapping.Registry;
import com.Nanbin.mapping.TicketMachineHelper;
import com.Nanbin.packet.PacketAddCustomBalance;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.mapper.PlayerHelper;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.data.IGui;
import org.mtr.mod.generated.lang.TranslationProvider;
import org.mtr.mod.screen.MTRScreenBase;

import static com.Nanbin.Init.MOD_ID;

public class TicketMachineScreen extends MTRScreenBase implements IGui {
    private static final Identifier WINDOW_TEXTURE = new Identifier("minecraft", "textures/gui/advancements/window.png");
    private static final Identifier ROUTE_MAP_TEXTURE = new Identifier("mtr", "textures/texture/route_map.png");
    private static final Identifier TICKET_RESIDENT_BAR_TEXTURE = new Identifier(MOD_ID, "textures/gui/ticket_resident_bar.png");
    private static final int WINDOW_SIZE = 340;
    private static final float RELATIVE_X = (float) WINDOW_SIZE / 256;
    private static final int BUTTON_COUNT = 8;
    private static final int BUTTON_WIDTH = 46;
    private static final int BUTTON_HEIGHT = 20;
    private static final int COLUMN_GAP = 6;
    private static final int ROW_PITCH = 34;
    private static final int BAR_LEFT = (int) (10 * RELATIVE_X);
    private static final int BAR_TOP = (int) (19 * RELATIVE_X);
    private static final int START_X = 6;
    private static final int START_Y = 13;
    private static final int[] ADD_AMOUNTS = {10, 20, 30, 50, 100, 150, 200, 300};
    private static final int[] EMERALD_COSTS = {1, 2, 3, 5, 10, 15, 20, 30};

    private final ButtonWidgetExtension[] buttons = new ButtonWidgetExtension[BUTTON_COUNT];
    private MutableText balanceText;
    private int balance;

    public TicketMachineScreen(int balance) {
        this.balance = balance;
        this.balanceText = TranslationProvider.GUI_MTR_BALANCE.getMutableText(new Object[]{balance});

        for (int i = 0; i < BUTTON_COUNT; ++i) {
            final int index = i;
            this.buttons[i] = new ButtonWidgetExtension(0, 0, 0, BUTTON_HEIGHT, TextHelper.literal("$" + ADD_AMOUNTS[i]), (button) -> {
                Registry.sendPacketToServer(new PacketAddCustomBalance(index));
                this.balance += ADD_AMOUNTS[index];
                this.balanceText = TranslationProvider.GUI_MTR_BALANCE.getMutableText(new Object[]{this.balance});
            });
        }
    }

    public void updateBalance(int balance) {
        this.balance = balance;
        this.balanceText = TranslationProvider.GUI_MTR_BALANCE.getMutableText(new Object[]{balance});
    }

    protected void init2() {
        super.init2();

        for (int i = 0; i < BUTTON_COUNT; ++i) {
            IDrawing.setPositionAndWidth(this.buttons[i], getButtonX(i), getButtonY(i), BUTTON_WIDTH);
        }

        for (ButtonWidgetExtension button : this.buttons) {
            this.addChild(new ClickableWidget(button));
        }
    }

    public void tick2() {
        int emeraldCount = this.getEmeraldCount();

        for (int i = 0; i < BUTTON_COUNT; ++i) {
            this.buttons[i].active = emeraldCount >= EMERALD_COSTS[i];
        }
    }

    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphicsHolder);
        int windowX = (this.width - WINDOW_SIZE) / 2;
        int windowY = (this.height - WINDOW_SIZE / 2) / 2;
        GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
        // 当前所处车站名
        final String currentStation = TicketMachineHelper.getCurrentStationName();
        final MutableText stationText = TextHelper.literal(currentStation);

        guiDrawing.beginDrawingTexture(WINDOW_TEXTURE);
        guiDrawing.drawTexture(windowX, windowY, windowX + WINDOW_SIZE, windowY + WINDOW_SIZE, 0.0F, 0.0F, 1.0F, 1.0F);
        guiDrawing.finishDrawingTexture();

        guiDrawing.beginDrawingTexture(ROUTE_MAP_TEXTURE);
        guiDrawing.drawTexture(windowX + 94*RELATIVE_X, windowY + 19*RELATIVE_X, windowX + 243*RELATIVE_X, windowY + 131*RELATIVE_X, 0.0F, 0.0F, 1.0F, 1.0F);
        guiDrawing.finishDrawingTexture();

        guiDrawing.beginDrawingTexture(TICKET_RESIDENT_BAR_TEXTURE);
        guiDrawing.drawTexture(windowX + 10*RELATIVE_X, windowY + 19*RELATIVE_X, windowX + 93*RELATIVE_X, windowY + 131*RELATIVE_X, 0.0F, 0.0F, 1.0F, 1.0F);
        guiDrawing.finishDrawingTexture();

        MutableText emeraldsText = TranslationProvider.GUI_MTR_EMERALDS.getMutableText(new Object[]{this.getEmeraldCount()});
        //下面的位置转换可能引人不适，但是作者也不想优化了
        graphicsHolder.drawText(this.balanceText, (int) (windowX + 241*RELATIVE_X - GraphicsHolder.getTextWidth(emeraldsText) - 6 - GraphicsHolder.getTextWidth(this.balanceText)), (int) (windowY + 8*RELATIVE_X), 5592405, false, GraphicsHolder.getDefaultLight());
        graphicsHolder.drawText(emeraldsText, (int) (windowX + 241*RELATIVE_X - GraphicsHolder.getTextWidth(emeraldsText)), (int) (windowY + 8*RELATIVE_X), 5592405, false, GraphicsHolder.getDefaultLight());
        graphicsHolder.drawText(stationText, (int) (windowX + 9*RELATIVE_X), (int) (windowY + 8*RELATIVE_X), 5592405, false, GraphicsHolder.getDefaultLight());

        for (int i = 0; i < BUTTON_COUNT; ++i) {
            MutableText emeraldCostText = TextHelper.translatable("gui.nanbin.emerald", EMERALD_COSTS[i]);
            graphicsHolder.drawText(emeraldCostText, getButtonX(i) + (BUTTON_WIDTH - GraphicsHolder.getTextWidth(emeraldCostText)) / 2, getButtonY(i) + BUTTON_HEIGHT + 2, -1, false, GraphicsHolder.getDefaultLight());
        }

        super.render(graphicsHolder, mouseX, mouseY, delta);
    }

    public boolean isPauseScreen2() {
        return false;
    }

    private int getButtonX(int index) {
        return (this.width - WINDOW_SIZE) / 2 + BAR_LEFT + START_X + (index % 2) * (BUTTON_WIDTH + COLUMN_GAP);
    }

    private int getButtonY(int index) {
        return (this.height - WINDOW_SIZE / 2) / 2 + BAR_TOP + START_Y + (index / 2) * ROW_PITCH;
    }

    private int getEmeraldCount() {
        ClientPlayerEntity clientPlayerEntity = MinecraftClient.getInstance().getPlayerMapped();
        PlayerInventory playerInventory = clientPlayerEntity == null ? null : PlayerHelper.getPlayerInventory(PlayerEntity.cast(clientPlayerEntity));
        return playerInventory == null ? 0 : playerInventory.count(Items.getEmeraldMapped());
    }
}