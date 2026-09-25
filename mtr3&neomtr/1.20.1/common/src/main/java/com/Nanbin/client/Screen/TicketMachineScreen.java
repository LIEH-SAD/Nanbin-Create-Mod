package com.Nanbin.client.Screen;

import com.Nanbin.mapping.Registry;
import com.Nanbin.packet.PacketHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import mtr.client.ClientData;
import mtr.data.RailwayData;
import mtr.data.Station;
import mtr.mappings.ScreenMapper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import static com.Nanbin.Init.MOD_ID;

public class TicketMachineScreen extends ScreenMapper {
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
	private static final int COLOR_TEXT = 0xFF555555; // 5592405

	private final ButtonWidget[] buttons = new ButtonWidget[BUTTON_COUNT];
	private Text balanceText;
	private int balance;

	public TicketMachineScreen(int balance) {
		super(Text.literal(""));
		this.balance = balance;
		this.balanceText = Text.translatable("gui.mtr.balance", balance);

		for (int i = 0; i < BUTTON_COUNT; ++i) {
			final int index = i;
			this.buttons[i] = ButtonWidget.builder(Text.literal("$" + PacketHandler.ADD_AMOUNTS[i]), (button) -> {
				Registry.sendPacketToServer(PacketHandler.PACKET_UPDATE_TICKET_MENU, buf -> buf.writeInt(index));
				this.balance += PacketHandler.ADD_AMOUNTS[index];
				this.balanceText = Text.translatable("gui.mtr.balance", this.balance);
			}).dimensions(0, 0, 0, BUTTON_HEIGHT).build();
		}
	}

	public void updateBalance(int balance) {
		this.balance = balance;
		this.balanceText = Text.translatable("gui.mtr.balance", balance);
	}

	@Override
	protected void init() {
		super.init();

		for (int i = 0; i < BUTTON_COUNT; ++i) {
			final ButtonWidget button = this.buttons[i];
			button.setX(getButtonX(i));
			button.setY(getButtonY(i));
			button.setWidth(BUTTON_WIDTH);
			this.addDrawableChild(button);
		}
	}

	@Override
	public void tick() {
		final int emeraldCount = this.getEmeraldCount();

		for (int i = 0; i < BUTTON_COUNT; ++i) {
			this.buttons[i].active = emeraldCount >= PacketHandler.EMERALD_COSTS[i];
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context);
		final int windowX = (this.width - WINDOW_SIZE) / 2;
		final int windowY = (this.height - WINDOW_SIZE / 2) / 2;
		// 当前所处车站名
		final String currentStation = this.getCurrentStationName();
		final Text stationText = Text.literal(currentStation);

		final MatrixStack matrices = context.getMatrices();

		drawTexture(matrices, WINDOW_TEXTURE, windowX, windowY, windowX + WINDOW_SIZE, windowY + WINDOW_SIZE);

		drawTexture(matrices, ROUTE_MAP_TEXTURE, windowX + 94 * RELATIVE_X, windowY + 19 * RELATIVE_X, windowX + 243 * RELATIVE_X, windowY + 131 * RELATIVE_X);

		drawTexture(matrices, TICKET_RESIDENT_BAR_TEXTURE, windowX + 10 * RELATIVE_X, windowY + 19 * RELATIVE_X, windowX + 93 * RELATIVE_X, windowY + 131 * RELATIVE_X);

		final Text emeraldsText = Text.translatable("gui.mtr.emeralds", this.getEmeraldCount());
		//下面的位置转换可能引人不适，但是作者也不想优化了
		context.drawText(this.textRenderer, this.balanceText, (int) (windowX + 241 * RELATIVE_X - this.textRenderer.getWidth(emeraldsText) - 6 - this.textRenderer.getWidth(this.balanceText)), (int) (windowY + 8 * RELATIVE_X), COLOR_TEXT, false);
		context.drawText(this.textRenderer, emeraldsText, (int) (windowX + 241 * RELATIVE_X - this.textRenderer.getWidth(emeraldsText)), (int) (windowY + 8 * RELATIVE_X), COLOR_TEXT, false);
		context.drawText(this.textRenderer, stationText, (int) (windowX + 9 * RELATIVE_X), (int) (windowY + 8 * RELATIVE_X), COLOR_TEXT, false);

		for (int i = 0; i < BUTTON_COUNT; ++i) {
			final Text emeraldCostText = Text.translatable("gui.nanbin.emerald", PacketHandler.EMERALD_COSTS[i]);
			context.drawText(this.textRenderer, emeraldCostText, getButtonX(i) + (BUTTON_WIDTH - this.textRenderer.getWidth(emeraldCostText)) / 2, getButtonY(i) + BUTTON_HEIGHT + 2, -1, false);
		}

		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private String getCurrentStationName() {
		final MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) {
			return "";
		}
		final Station station = RailwayData.getStation(ClientData.STATIONS, ClientData.DATA_CACHE, client.player.getBlockPos());
		return station == null ? "" : station.name;
	}

	private int getButtonX(int index) {
		return (this.width - WINDOW_SIZE) / 2 + BAR_LEFT + START_X + (index % 2) * (BUTTON_WIDTH + COLUMN_GAP);
	}

	private int getButtonY(int index) {
		return (this.height - WINDOW_SIZE / 2) / 2 + BAR_TOP + START_Y + (index / 2) * ROW_PITCH;
	}

	private int getEmeraldCount() {
		final MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) {
			return 0;
		}
		return client.player.getInventory().count(Items.EMERALD);
	}

	private void drawTexture(MatrixStack matrices, Identifier texture, float x1, float y1, float x2, float y2) {
		RenderSystem.setShader(GameRenderer::getPositionTexProgram);
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		final Tessellator tessellator = Tessellator.getInstance();
		final BufferBuilder bufferBuilder = tessellator.getBuffer();
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

		final Matrix4f matrix = matrices.peek().getPositionMatrix();

		bufferBuilder.vertex(matrix, x1, y2, 0.0F).texture(0.0F, 1.0F).next();
		bufferBuilder.vertex(matrix, x2, y2, 0.0F).texture(1.0F, 1.0F).next();
		bufferBuilder.vertex(matrix, x2, y1, 0.0F).texture(1.0F, 0.0F).next();
		bufferBuilder.vertex(matrix, x1, y1, 0.0F).texture(0.0F, 0.0F).next();

		tessellator.draw();
	}
}
