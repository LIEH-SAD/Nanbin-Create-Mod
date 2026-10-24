package com.Nanbin.client.Screen;

import com.Nanbin.Registry.RegBlock.BlockCRTStationInfo1;
import com.Nanbin.Registry.RegBlock.BlockCRTStationInfo1Double;
import com.Nanbin.packet.ClientPacketHelper;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import mtr.mappings.ScreenMapper;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * 站台信息屏配置界面：输入网络图片 URL、选择站台、编辑指示牌格子内容并保存。
 */
public class StationInfoScreen extends ScreenMapper {

	private static final int BUTTON_WIDTH = 150;
	private static final int BUTTON_HEIGHT = 20;
	private static final int TEXT_FIELD_WIDTH = 200;
	private static final int TEXT_FIELD_HEIGHT = 20;
	private static final int SPACING = 10;
	private static final int FIELD_MAX_LENGTH = 256;
	private static final int COLOR_TITLE = 0xFFFFFF;
	private static final int COLOR_HINT = 0xFFAAAAAA;

	private final BlockPos blockPos;
	private final boolean isFront;
	private final LongAVLTreeSet selectedIds = new LongAVLTreeSet();

	private String currentUrl;
	private TextFieldWidget urlField;

	public StationInfoScreen(BlockPos blockPos, String initialUrl, boolean isFront) {
		super(Text.translatable("gui.nanbin.station_info.title"));
		this.blockPos = blockPos;
		this.isFront = isFront;

		String url = initialUrl != null ? initialUrl : "";
		final World world = MinecraftClient.getInstance().world;
		if (world != null) {
			final BlockEntity blockEntity = world.getBlockEntity(blockPos);
			if (blockEntity instanceof BlockCRTStationInfo1.BlockEntity entity) {
				if (!entity.getUrl().isEmpty()) {
					url = entity.getUrl();
				}
				final List<LongAVLTreeSet> savedSelectedIds = entity.getSelectedIds();
				if (!savedSelectedIds.isEmpty()) {
					selectedIds.addAll(savedSelectedIds.get(0));
				}
			} else if (blockEntity instanceof BlockCRTStationInfo1Double.BlockEntity entity) {
				if (!entity.getUrl(isFront).isEmpty()) {
					url = entity.getUrl(isFront);
				}
				final List<LongAVLTreeSet> savedSelectedIds = entity.getSelectedIds(isFront);
				if (!savedSelectedIds.isEmpty()) {
					selectedIds.addAll(savedSelectedIds.get(0));
				}
			}
		}
		this.currentUrl = url;
	}

	@Override
	protected void init() {
		super.init();
		final int centerX = width / 2;
		int currentY = 50;

		urlField = new TextFieldWidget(textRenderer, centerX - TEXT_FIELD_WIDTH / 2, currentY, TEXT_FIELD_WIDTH, TEXT_FIELD_HEIGHT, Text.translatable("gui.nanbin.station_info.url_hint"));
		urlField.setMaxLength(FIELD_MAX_LENGTH);
		urlField.setText(currentUrl);
		urlField.setChangedListener(text -> currentUrl = text);
		addDrawableChild(urlField);
		currentY += TEXT_FIELD_HEIGHT + SPACING * 2;

		addDrawableChild(ButtonWidget.builder(
				Text.translatable("gui.nanbin.station_info.select_station"),
				button -> ClientPacketHelper.openPlatformSelectionScreen(blockPos, selectedIds, this)).dimensions(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		currentY += BUTTON_HEIGHT + SPACING;

		addDrawableChild(ButtonWidget.builder(
				Text.translatable("gui.nanbin.station_info.select_sign"),
				button -> ClientPacketHelper.openRailwaySignDoubleScreen(blockPos, isFront)).dimensions(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		currentY += BUTTON_HEIGHT + SPACING * 2;

		addDrawableChild(ButtonWidget.builder(
				Text.translatable("gui.nanbin.save"),
				button -> {
					ClientPacketHelper.saveStationInfoScreen(blockPos, getUrl(), selectedIds, isFront);
					close();
				}).dimensions(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context);
		final int centerX = width / 2;
		context.drawCenteredTextWithShadow(textRenderer, title, centerX, 10, COLOR_TITLE);
		context.drawCenteredTextWithShadow(textRenderer, Text.translatable("gui.nanbin.station_info.url_hint"), centerX, 28, COLOR_HINT);
		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	public String getUrl() {
		return urlField != null ? urlField.getText() : "";
	}

	/** 是否配置正面（双面信息屏据此区分正反面数据）。 */
	public boolean isFront() {
		return isFront;
	}
}
