package com.Nanbin.client.Screen;

import com.Nanbin.Registry.RegBlock.BlockBusTicketProcessor;
import com.Nanbin.mapping.Registry;
import com.Nanbin.packet.PacketHandler;
import mtr.mappings.ScreenMapper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * 公交计费器设置界面：切换「两次刷卡 / 一次售票」模式，并在一次售票模式下设置固定金额。
 */
public class BusTicketProcessorScreen extends ScreenMapper {

	private static final int GUI_WIDTH = 200;
	private static final int GUI_HEIGHT = 100;
	private static final int MARGIN = 12;
	private static final int FIELD_HEIGHT = 18;
	private static final int BUTTON_HEIGHT = 20;
	private static final int FIELD_MAX_LENGTH = 6;
	private static final int MIN_AMOUNT = 1;
	private static final int MAX_AMOUNT = 999999;
	private static final int COLOR_TITLE = 0xFFFFFF;
	private static final int COLOR_LABEL = 0xA0A0A0;
	private static final int COLOR_DISABLED_HINT = 0xFF808080;
	private static final String[] MODE_NAMES = {"gui.nanbin.ticket_processer.two_tap", "gui.nanbin.ticket_processer.fixed"};

	private final BlockPos blockPos;
	private int mode;
	private int amount;

	private ButtonWidget modeButton;
	private TextFieldWidget amountField;

	public BusTicketProcessorScreen(BlockPos blockPos, int mode, int amount) {
		super(Text.translatable("gui.nanbin.ticket_processer"));
		this.blockPos = blockPos;
		this.mode = mode;
		this.amount = amount;
	}

	@Override
	protected void init() {
		super.init();

		final int guiLeft = (width - GUI_WIDTH) / 2;
		final int guiTop = (height - GUI_HEIGHT) / 2;
		final int componentWidth = GUI_WIDTH - MARGIN * 2;

		modeButton = ButtonWidget.builder(Text.translatable(MODE_NAMES[mode]), button -> {
			mode = (mode + 1) % MODE_NAMES.length;
			modeButton.setMessage(Text.translatable(MODE_NAMES[mode]));
			amountField.setEditable(mode == BlockBusTicketProcessor.MODE_FIXED_AMOUNT);
		}).dimensions(guiLeft + MARGIN, guiTop + 34, componentWidth, BUTTON_HEIGHT).build();
		addDrawableChild(modeButton);

		amountField = new TextFieldWidget(textRenderer, guiLeft + MARGIN, guiTop + 64, componentWidth, FIELD_HEIGHT, Text.translatable("gui.nanbin.ticket_processer"));
		amountField.setMaxLength(FIELD_MAX_LENGTH);
		amountField.setText(String.valueOf(amount));
		amountField.setEditable(mode == BlockBusTicketProcessor.MODE_FIXED_AMOUNT);
		addDrawableChild(amountField);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context);

		final int guiLeft = (width - GUI_WIDTH) / 2;
		final int guiTop = (height - GUI_HEIGHT) / 2;

		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, guiTop + 12, COLOR_TITLE);

		if (mode != BlockBusTicketProcessor.MODE_FIXED_AMOUNT) {
			context.drawTextWithShadow(textRenderer, Text.translatable("gui.nanbin.ticket_processer.two_tap.disabled"), guiLeft + MARGIN, guiTop + 88, COLOR_DISABLED_HINT);
		}

		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public void close() {
		int parsedAmount = BlockBusTicketProcessor.DEFAULT_AMOUNT;
		try {
			parsedAmount = Math.max(MIN_AMOUNT, Math.min(MAX_AMOUNT, Integer.parseInt(amountField.getText().trim())));
		} catch (NumberFormatException ignored) {
		}
		amount = parsedAmount;
		Registry.sendPacketToServer(PacketHandler.PACKET_UPDATE_BUS_TICKET_PROCESSOR, buf -> {
			buf.writeBlockPos(blockPos);
			buf.writeInt(mode);
			buf.writeInt(amount);
		});
		super.close();
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
