package com.Nanbin.client.Screen;

import com.Nanbin.Init;
import com.Nanbin.mapping.GraphicsHolder;
import com.Nanbin.mapping.GuiDrawing;
import com.Nanbin.mapping.Registry;
import com.Nanbin.mapping.ScreenBase;
import com.Nanbin.mapping.TicketMachineHelper;
import com.Nanbin.packet.PacketAddCustomBalance;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.mtr.data.IGui;

public class TicketMachineScreen extends ScreenBase implements IGui {

	private static final Identifier WINDOW_TEXTURE = Identifier.of("minecraft", "textures/gui/advancements/window.png");
	private static final Identifier ROUTE_MAP_TEXTURE = Identifier.of("mtr", "textures/texture/route_map.png");
	private static final Identifier TICKET_RESIDENT_BAR_TEXTURE = Identifier.of(Init.MOD_ID, "textures/gui/ticket_resident_bar.png");
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

	private final ButtonWidget[] buttons = new ButtonWidget[BUTTON_COUNT];
	private MutableText balanceText;
	private int balance;

	public TicketMachineScreen(int balance) {
		super(Text.translatable("gui.nanbin.ticket_machine.title"));
		this.balance = balance;
		this.balanceText = org.mtr.generated.lang.TranslationProvider.GUI_MTR_BALANCE.getMutableText(String.valueOf(balance));

		for (int i = 0; i < BUTTON_COUNT; ++i) {
			final int index = i;
			this.buttons[i] = ButtonWidget.builder(Text.literal("$" + ADD_AMOUNTS[i]), button -> {
				Registry.sendPacketToServer(new PacketAddCustomBalance(index));
				this.balance += ADD_AMOUNTS[index];
				this.balanceText = org.mtr.generated.lang.TranslationProvider.GUI_MTR_BALANCE.getMutableText(String.valueOf(this.balance));
			}).dimensions(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT).build();
		}
	}

	public void updateBalance(int balance) {
		this.balance = balance;
		this.balanceText = org.mtr.generated.lang.TranslationProvider.GUI_MTR_BALANCE.getMutableText(String.valueOf(balance));
	}

	@Override
	protected void init2() {
		super.init2();

		for (int i = 0; i < BUTTON_COUNT; ++i) {
			buttons[i].setPosition(getButtonX(i), getButtonY(i));
			buttons[i].setWidth(BUTTON_WIDTH);
		}
		for (final ButtonWidget button : buttons) {
			addChild(button);
		}
	}

	@Override
	public void tick2() {
		final int emeraldCount = getEmeraldCount();
		for (int i = 0; i < BUTTON_COUNT; ++i) {
			buttons[i].active = emeraldCount >= EMERALD_COSTS[i];
		}
	}

	@Override
	public void render2(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
		final int windowX = (width - WINDOW_SIZE) / 2;
		final int windowY = (height - WINDOW_SIZE / 2) / 2;

		final String currentStation = TicketMachineHelper.getCurrentStationName();
		final MutableText stationText = Text.literal(currentStation);

		final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
		guiDrawing.beginDrawingTexture(WINDOW_TEXTURE);
		guiDrawing.drawTexture(windowX, windowY, windowX + WINDOW_SIZE, windowY + WINDOW_SIZE, 0.0F, 0.0F, 1.0F, 1.0F);
		guiDrawing.finishDrawingTexture();

		guiDrawing.beginDrawingTexture(ROUTE_MAP_TEXTURE);
		guiDrawing.drawTexture(windowX + 94 * RELATIVE_X, windowY + 19 * RELATIVE_X, windowX + 243 * RELATIVE_X, windowY + 131 * RELATIVE_X, 0.0F, 0.0F, 1.0F, 1.0F);
		guiDrawing.finishDrawingTexture();

		guiDrawing.beginDrawingTexture(TICKET_RESIDENT_BAR_TEXTURE);
		guiDrawing.drawTexture(windowX + 10 * RELATIVE_X, windowY + 19 * RELATIVE_X, windowX + 93 * RELATIVE_X, windowY + 131 * RELATIVE_X, 0.0F, 0.0F, 1.0F, 1.0F);
		guiDrawing.finishDrawingTexture();

		final MutableText emeraldsText = org.mtr.generated.lang.TranslationProvider.GUI_MTR_EMERALDS.getMutableText(String.valueOf(getEmeraldCount()));
		graphicsHolder.drawText(balanceText,
				(int) (windowX + 241 * RELATIVE_X - GraphicsHolder.getTextWidth(emeraldsText) - 6 - GraphicsHolder.getTextWidth(balanceText)),
				(int) (windowY + 8 * RELATIVE_X), 5592405, false, GraphicsHolder.getDefaultLight());
		graphicsHolder.drawText(emeraldsText, (int) (windowX + 241 * RELATIVE_X - GraphicsHolder.getTextWidth(emeraldsText)),
				(int) (windowY + 8 * RELATIVE_X), 5592405, false, GraphicsHolder.getDefaultLight());
		graphicsHolder.drawText(stationText, (int) (windowX + 9 * RELATIVE_X),
				(int) (windowY + 8 * RELATIVE_X), 5592405, false, GraphicsHolder.getDefaultLight());

		for (int i = 0; i < BUTTON_COUNT; ++i) {
			final MutableText emeraldCostText = Text.translatable("gui.nanbin.emerald", EMERALD_COSTS[i]);
			graphicsHolder.drawText(emeraldCostText,
					getButtonX(i) + (BUTTON_WIDTH - GraphicsHolder.getTextWidth(emeraldCostText)) / 2,
					getButtonY(i) + BUTTON_HEIGHT + 2, -1, false, GraphicsHolder.getDefaultLight());
		}
	}

	@Override
	public boolean isPauseScreen2() {
		return false;
	}

	private int getButtonX(int index) {
		return (width - WINDOW_SIZE) / 2 + BAR_LEFT + START_X + (index % 2) * (BUTTON_WIDTH + COLUMN_GAP);
	}

	private int getButtonY(int index) {
		return (height - WINDOW_SIZE / 2) / 2 + BAR_TOP + START_Y + (index / 2) * ROW_PITCH;
	}

	private int getEmeraldCount() {
		final var clientPlayer = MinecraftClient.getInstance().player;
		if (clientPlayer == null) {
			return 0;
		}
		final PlayerInventory inventory = clientPlayer.getInventory();
		return inventory == null ? 0 : inventory.count(Items.EMERALD);
	}
}
