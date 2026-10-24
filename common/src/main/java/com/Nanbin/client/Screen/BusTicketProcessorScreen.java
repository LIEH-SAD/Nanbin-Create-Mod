package com.Nanbin.client.Screen;

import com.Nanbin.Registry.RegBlock.BlockBusTicketProcessor;
import com.Nanbin.mapping.GraphicsHolder;
import com.Nanbin.mapping.Registry;
import com.Nanbin.mapping.ScreenBase;
import com.Nanbin.packet.PacketUpdateBusTicketProcessor;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.mtr.data.IGui;

/**
 * 公交售票机配置界面：模式按钮 + 金额输入框。
 * <p>
 * 4.1 适配：基类换成 {@link ScreenBase}；{@code ButtonWidgetExtension}/{@code TextFieldWidgetExtension}
 * 包装层 → 原版 {@link ButtonWidget}/{@link TextFieldWidget}（{@code TextCase} 过滤去掉，保持默认）。
 */
public class BusTicketProcessorScreen extends ScreenBase implements IGui {

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

	private ButtonWidget modeButton;
	private TextFieldWidget amountField;

	public BusTicketProcessorScreen(BlockPos blockPos, int mode, int amount) {
		super(Text.translatable("gui.nanbin.ticket_processer"));
		this.blockPos = blockPos;
		this.mode = mode;
		this.amount = amount;
	}

	@Override
	protected void init2() {
		super.init2();

		final int guiLeft = (width - GUI_WIDTH) / 2;
		final int guiTop = (height - GUI_HEIGHT) / 2;
		final int fieldWidth = GUI_WIDTH - MARGIN * 2;
		final int componentX = guiLeft + MARGIN;

		modeButton = ButtonWidget.builder(Text.translatable(MODE_NAMES[mode]), button -> {
			mode = (mode + 1) % MODE_NAMES.length;
			modeButton.setMessage(Text.translatable(MODE_NAMES[mode]));
			amountField.setEditable(mode == BlockBusTicketProcessor.MODE_FIXED_AMOUNT);
		}).dimensions(componentX, guiTop + 34, fieldWidth, BUTTON_HEIGHT).build();

		amountField = new TextFieldWidget(textRenderer, componentX, guiTop + 64, fieldWidth, FIELD_HEIGHT, Text.empty());
		amountField.setMaxLength(FIELD_MAX_LENGTH);
		amountField.setText(String.valueOf(amount));
		amountField.setEditable(mode == BlockBusTicketProcessor.MODE_FIXED_AMOUNT);

		addChild(modeButton);
		addChild(amountField);
	}

	@Override
	public void onClose2() {
		int parsedAmount = BlockBusTicketProcessor.DEFAULT_AMOUNT;
		try {
			parsedAmount = Math.max(MIN_AMOUNT, Math.min(MAX_AMOUNT, Integer.parseInt(amountField.getText().trim())));
		} catch (NumberFormatException ignored) {
		}
		amount = parsedAmount;
		Registry.sendPacketToServer(new PacketUpdateBusTicketProcessor(blockPos, mode, amount));
		super.onClose2();
	}

	@Override
	public void tick2() {
		// 输入框本身已作为子控件登记，无需手动 tick；这里只做金额实时钳制
		if (mode == BlockBusTicketProcessor.MODE_FIXED_AMOUNT) {
			final String text = amountField.getText();
			if (!text.isEmpty()) {
				try {
					amount = Math.max(MIN_AMOUNT, Math.min(MAX_AMOUNT, Integer.parseInt(text)));
				} catch (NumberFormatException ignored) {
				}
			}
		}
	}

	@Override
	public void render2(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
		final int guiLeft = (width - GUI_WIDTH) / 2;
		final int guiTop = (height - GUI_HEIGHT) / 2;

		graphicsHolder.drawCenteredText(Text.translatable("gui.nanbin.ticket_processer"), width / 2, guiTop + 12, ARGB_WHITE, true, GraphicsHolder.getDefaultLight());

		if (mode != BlockBusTicketProcessor.MODE_FIXED_AMOUNT) {
			graphicsHolder.drawText(Text.translatable("gui.nanbin.ticket_processer.two_tap.disabled"),
					guiLeft + MARGIN, guiTop + 88, 0x808080, false, GraphicsHolder.getDefaultLight());
		}
	}

	@Override
	public boolean isPauseScreen2() {
		return false;
	}
}
