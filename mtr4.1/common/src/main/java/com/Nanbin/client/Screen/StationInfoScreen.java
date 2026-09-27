package com.Nanbin.client.Screen;

import com.Nanbin.InitClient;
import com.Nanbin.Registry.RegBlock.BlockCRTStationInfoBase;
import com.Nanbin.mapping.GraphicsHolder;
import com.Nanbin.mapping.ScreenBase;
import com.Nanbin.packet.ClientPacketHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.mtr.data.IGui;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;

import java.util.List;


public class StationInfoScreen extends ScreenBase implements IGui {

	private static final int BUTTON_WIDTH = 150;
	private static final int BUTTON_HEIGHT = 20;
	private static final int TEXT_FIELD_WIDTH = 200;
	private static final int TEXT_FIELD_HEIGHT = 20;
	private static final int SPACING = 10;
	private static final int FIELD_MAX_LENGTH = 256;

	private final BlockPos blockPos;
	private final boolean isFront;
	private String currentUrl;
	private final LongAVLTreeSet selectedIds = new LongAVLTreeSet();
	private TextFieldWidget urlTextField;

	public StationInfoScreen(BlockPos blockPos, String initialUrl, boolean isFront) {
		super(Text.translatable("gui.nanbin.station_info.title"));
		this.blockPos = blockPos;
		this.isFront = isFront;

		String url = initialUrl != null ? initialUrl : "";
		final World world = MinecraftClient.getInstance().world;
		if (world != null && world.getBlockEntity(blockPos) instanceof BlockCRTStationInfoBase.BlockEntityBase entity) {
			final String savedUrl = entity.getUrl(isFront);
			if (!savedUrl.isEmpty()) {
				url = savedUrl;
			}
			final List<LongAVLTreeSet> savedSelectedIds = entity.getSelectedIds(isFront);
			if (!savedSelectedIds.isEmpty()) {
				selectedIds.addAll(savedSelectedIds.get(0));
			}
		}
		this.currentUrl = url;
	}

	@Override
	protected void init2() {
		super.init2();
		final int centerX = width / 2;
		int currentY = 50;

		urlTextField = new TextFieldWidget(textRenderer, centerX - TEXT_FIELD_WIDTH / 2, currentY, TEXT_FIELD_WIDTH, TEXT_FIELD_HEIGHT, Text.empty());
		urlTextField.setMaxLength(FIELD_MAX_LENGTH);
		urlTextField.setChangedListener(text -> currentUrl = text);
		urlTextField.setText(currentUrl);
		addChild(urlTextField);
		currentY += TEXT_FIELD_HEIGHT + SPACING * 2;

		addChild(ButtonWidget.builder(Text.translatable("gui.nanbin.station_info.select_station"),
						button -> ClientPacketHelper.openPlatformSelectionScreen(blockPos, selectedIds, this))
				.dimensions(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		currentY += BUTTON_HEIGHT + SPACING;

		addChild(ButtonWidget.builder(Text.translatable("gui.nanbin.station_info.select_sign"),
						button -> ClientPacketHelper.openRailwaySignDoubleScreen(blockPos, isFront))
				.dimensions(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		currentY += BUTTON_HEIGHT + SPACING * 2;

		addChild(ButtonWidget.builder(Text.translatable("gui.nanbin.save"), button -> {
					ClientPacketHelper.saveStationInfoScreen(blockPos, getUrl(), selectedIds, isFront);
					close();
				})
				.dimensions(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
	}

	@Override
	public void render2(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
		try {
			final int centerX = width / 2;
			graphicsHolder.drawCenteredText(Text.translatable("gui.nanbin.station_info.title"), centerX, 10, ARGB_WHITE, true, GraphicsHolder.getDefaultLight());
			graphicsHolder.drawCenteredText(Text.translatable("gui.nanbin.station_info.url_hint"), centerX, 28, 0xFFAAAAAA, true, GraphicsHolder.getDefaultLight());
		} catch (Exception e) {
			InitClient.LOGGER.error("Failed to render StationInfoScreen", e);
		}
	}

	@Override
	public boolean isPauseScreen2() {
		return false;
	}

	public String getUrl() {
		return urlTextField != null ? urlTextField.getText() : currentUrl;
	}
}
