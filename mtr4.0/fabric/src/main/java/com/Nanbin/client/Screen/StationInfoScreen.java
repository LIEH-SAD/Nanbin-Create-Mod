package com.Nanbin.client.Screen;

import com.Nanbin.InitClient;
import com.Nanbin.Registry.RegBlock.BlockCRTStationInfo1;
import com.Nanbin.packet.ClientPacketHelper;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.*;
import org.mtr.mapping.tool.TextCase;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.data.IGui;
import org.mtr.mod.screen.MTRScreenBase;

import java.util.List;

public class StationInfoScreen extends MTRScreenBase implements IGui {

    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int TEXT_FIELD_WIDTH = 200;
    private static final int TEXT_FIELD_HEIGHT = 20;
    private static final int SPACING = 10;
    private static final int FIELD_MAX_LENGTH = 256;

    private final BlockPos blockPos;
    /** 屏幕当前显示的 URL，随编辑更新，屏幕重新初始化时据此恢复。 */
    private String currentUrl;
    private final LongAVLTreeSet selectedIds = new LongAVLTreeSet();
    private TextFieldWidgetExtension urlTextField;

    public StationInfoScreen(BlockPos blockPos, String initialUrl) {
        super();
        this.blockPos = blockPos;
        String url = initialUrl != null ? initialUrl : "";
        // 以客户端方块实体中已保存的 URL / 站台选择为准（服务端包先到达时显示旧值的问题由此兜底）。
        final ClientWorld world = MinecraftClient.getInstance().getWorldMapped();
        if (world != null) {
            final BlockEntity blockEntity = world.getBlockEntity(blockPos);
            if (blockEntity != null && blockEntity.data instanceof BlockCRTStationInfo1.BlockEntity entity) {
                final String savedUrl = entity.getUrl();
                if (!savedUrl.isEmpty()) {
                    url = savedUrl;
                }
                final List<LongAVLTreeSet> savedSelectedIds = entity.getSelectedIds();
                if (!savedSelectedIds.isEmpty()) {
                    selectedIds.addAll(savedSelectedIds.get(0));
                }
            }
        }
        this.currentUrl = url;
    }

    @Override
    protected void init2() {
        super.init2();
        final int centerX = getWidthMapped() / 2;
        int currentY = 50;

        urlTextField = new TextFieldWidgetExtension(0, 0, 0, TEXT_FIELD_HEIGHT, FIELD_MAX_LENGTH, TextCase.DEFAULT, null, null);
        urlTextField.setChangedListener2(text -> currentUrl = text);
        IDrawing.setPositionAndWidth(urlTextField, centerX - TEXT_FIELD_WIDTH / 2, currentY, TEXT_FIELD_WIDTH);
        urlTextField.setText2(currentUrl);
        addChild(new ClickableWidget(urlTextField));
        currentY += TEXT_FIELD_HEIGHT + SPACING * 2;

        final ButtonWidgetExtension stationButton = new ButtonWidgetExtension(
                centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                TextHelper.translatable("gui.nanbin.station_info.select_station"),
                button -> ClientPacketHelper.openPlatformSelectionScreen(blockPos, selectedIds, this)
        );
        addChild(new ClickableWidget(stationButton));
        currentY += BUTTON_HEIGHT + SPACING;

        final ButtonWidgetExtension signButton = new ButtonWidgetExtension(
                centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                TextHelper.translatable("gui.nanbin.station_info.select_sign"),
                button -> ClientPacketHelper.openRailwaySignDoubleScreen(blockPos)
        );
        addChild(new ClickableWidget(signButton));
        currentY += BUTTON_HEIGHT + SPACING * 2;

        final ButtonWidgetExtension saveButton = new ButtonWidgetExtension(
                centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                TextHelper.translatable("gui.nanbin.save"),
                button -> {
                    ClientPacketHelper.saveStationInfoScreen(blockPos, getUrl(), selectedIds);
                    onClose2();
                }
        );
        addChild(new ClickableWidget(saveButton));
    }

    @Override
    public void tick2() {
        if (urlTextField != null) {
            urlTextField.tick2();
        }
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        try {
            renderBackground(graphicsHolder);
            final int centerX = getWidthMapped() / 2;
            graphicsHolder.drawCenteredText(TextHelper.translatable("gui.nanbin.station_info.title"), centerX, 10, ARGB_WHITE);
            graphicsHolder.drawCenteredText(TextHelper.translatable("gui.nanbin.station_info.url_hint"), centerX, 28, 0xFFAAAAAA);
            super.render(graphicsHolder, mouseX, mouseY, delta);
        } catch (Exception e) {
            InitClient.LOGGER.error("Failed to render StationInfoScreen", e);
        }
    }

    @Override
    public boolean isPauseScreen2() {
        return false;
    }

    public String getUrl() {
        return urlTextField != null ? urlTextField.getText2() : "";
    }
}