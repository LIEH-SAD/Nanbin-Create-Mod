package com.Nanbin.client.Screen;

import com.Nanbin.client.ClientData.SignClipboard;
import org.mtr.core.data.Station;
import org.mtr.libraries.it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.*;
import org.mtr.mod.InitClient;
import org.mtr.mod.client.CustomResourceLoader;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.IGui;
import org.mtr.mod.render.RenderRailwaySign;
import org.mtr.mod.resource.SignResource;
import org.mtr.mod.screen.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 多行指示牌编辑器（N 行 × M 格，小图标），供各 CRT 方块共用。
 * 修改自天津地铁的 RailwaySignDoubleScreen，去掉了对 ziyue 模块的依赖。
 * 行数/列数/初始数据/保存动作均由调用方传入。
 */

public class RailwaySignDoubleScreen extends ScreenExtension implements IGui
{
    protected final int lines;
    protected final int length;

    protected int line;
    protected int editingIndex;
    protected int page;
    protected int totalPages;
    protected int columns;
    protected int rows;

    protected final BlockPos signPos;
    protected final boolean isRailwaySign;
    protected final String[][] signIds;
    protected final List<LongAVLTreeSet> selectedIds;
    protected final Runnable onSave;
    protected final ObjectImmutableList<DashboardListItem> exitsForList;
    protected final ObjectImmutableList<DashboardListItem> platformsForList;
    protected final ObjectArraySet<DashboardListItem> routesForList;
    protected final ObjectArraySet<DashboardListItem> stationsForList;
    protected final ObjectArrayList<String> allSignIds = new ObjectArrayList<>();

    protected final ButtonWidgetExtension[][] buttonsEdit;
    protected final ButtonWidgetExtension[] buttonsSelection;
    protected final ButtonWidgetExtension buttonClear;
    protected final TexturedButtonWidgetExtension buttonPrevPage;
    protected final TexturedButtonWidgetExtension buttonNextPage;

    protected static final int SIGN_SIZE = 32;
    protected static final int SIGN_BUTTON_SIZE = 16;

    protected final int buttonYStart;

    public RailwaySignDoubleScreen(BlockPos signPos, int lines, int length, String[][] signIds, List<LongAVLTreeSet> selectedIds, boolean isRailwaySign, Runnable onSave) {
        super();
        this.signPos = signPos;
        this.lines = Math.max(lines, 1);
        this.length = Math.max(length, 1);
        this.signIds = signIds != null ? signIds : new String[this.lines][this.length];
        this.selectedIds = selectedIds != null ? selectedIds : new ArrayList<>();
        while (this.selectedIds.size() < this.lines) {
            this.selectedIds.add(new LongAVLTreeSet());
        }
        this.isRailwaySign = isRailwaySign;
        this.onSave = onSave;
        this.buttonYStart = (SQUARE_SIZE + SIGN_SIZE) * this.lines + SIGN_BUTTON_SIZE / 2;
        editingIndex = -1;
        final ClientWorld world = MinecraftClient.getInstance().getWorldMapped();

        allSignIds.addAll(CustomResourceLoader.getSortedSignIds());

        final Station station = InitClient.findStation(signPos);
        if (station == null) {
            exitsForList = ObjectImmutableList.of();
            platformsForList = ObjectImmutableList.of();
            stationsForList = new ObjectArraySet<>();
            routesForList = new ObjectArraySet<>();
        } else {
            exitsForList = new ObjectImmutableList<>(EditStationScreen.getExitsForDashboardList(EditStationScreen.getStationExits(station, true)));
            platformsForList = PIDSConfigScreen.getPlatformsForList(new ObjectArrayList<>(station.savedRails));

            final ObjectArraySet<Station> connectingStationsIncludingThisOne = new ObjectArraySet<>(station.connectedStations);
            connectingStationsIncludingThisOne.add(station);
            stationsForList = MinecraftClientData.convertDataSet(connectingStationsIncludingThisOne);

            final LongAVLTreeSet platformIds = new LongAVLTreeSet();
            connectingStationsIncludingThisOne.forEach(connectingStation -> connectingStation.savedRails.forEach(platform -> platformIds.add(platform.getId())));
            routesForList = new ObjectArraySet<>();
            final IntAVLTreeSet addedColors = new IntAVLTreeSet();
            MinecraftClientData.getInstance().simplifiedRoutes.forEach(simplifiedRoute -> {
                final int color = simplifiedRoute.getColor();
                if (!addedColors.contains(color) && simplifiedRoute.getPlatforms().stream().anyMatch(simplifiedRoutePlatform -> platformIds.contains(simplifiedRoutePlatform.getPlatformId()))) {
                    routesForList.add(new DashboardListItem(color, simplifiedRoute.getName().split("\\|\\|")[0], color));
                    addedColors.add(color);
                }
            });
        }

        buttonsEdit = new ButtonWidgetExtension[this.lines][length];
        for (int i = 0; i < this.lines; i++) {
            for (int j = 0; j < buttonsEdit[i].length; j++) {
                final int line = i;
                final int index = j;
                buttonsEdit[i][j] = new ButtonWidgetExtension(0, 0, 0, SQUARE_SIZE, TextHelper.translatable("selectWorld.edit"), button -> edit(line, index));
            }
        }

        buttonsSelection = new ButtonWidgetExtension[allSignIds.size()];
        for (int i = 0; i < allSignIds.size(); i++) {
            final int index = i;
            buttonsSelection[i] = new ButtonWidgetExtension(0, 0, 0, SIGN_BUTTON_SIZE, button -> setNewSignId(allSignIds.get(index)));
        }


        buttonClear = new ButtonWidgetExtension(0, 0, 0, SQUARE_SIZE, TextHelper.translatable("gui.mtr.reset_sign"), button -> setNewSignId(null));
        buttonPrevPage = new TexturedButtonWidgetExtension(0, 0, 0, SQUARE_SIZE, new Identifier("textures/gui/sprites/mtr/icon_left.png"), new Identifier("textures/gui/sprites/mtr/icon_left_highlighted.png"), new Identifier("textures/gui/sprites/mtr/icon_left.png"), button -> setPage(page - 1));
        buttonNextPage = new TexturedButtonWidgetExtension(0, 0, 0, SQUARE_SIZE, new Identifier("textures/gui/sprites/mtr/icon_right.png"), new Identifier("textures/gui/sprites/mtr/icon_right_highlighted.png"), new Identifier("textures/gui/sprites/mtr/icon_right.png"), button -> setPage(page + 1));
    }

    @Override
    protected void init2() {
        super.init2();

        for (int i = 0; i < lines; i++) {
            for (int j = 0; j < buttonsEdit[i].length; j++) {
                IDrawing.setPositionAndWidth(buttonsEdit[i][j], (width - SIGN_SIZE * length) / 2 + j * SIGN_SIZE, i * (SIGN_SIZE + SQUARE_SIZE) + SIGN_SIZE, SIGN_SIZE);
                addChild(new ClickableWidget(buttonsEdit[i][j]));
            }
        }

        columns = Math.max((width - SIGN_BUTTON_SIZE * 3) / (SIGN_BUTTON_SIZE * 8) * 2, 1);
        rows = Math.max((height - SIGN_SIZE * lines - SQUARE_SIZE * 4 - SQUARE_SIZE / 2) / SIGN_BUTTON_SIZE, 1);

        final int xOffsetSmall = (width - SIGN_BUTTON_SIZE * (columns * 4 + 3)) / 2 + SIGN_BUTTON_SIZE;
        final int xOffsetBig = xOffsetSmall + SIGN_BUTTON_SIZE * (columns + 1);

        totalPages = loopSigns((index, x, y, isBig) -> {
            IDrawing.setPositionAndWidth(buttonsSelection[index], (isBig ? xOffsetBig : xOffsetSmall) + x, buttonYStart + y, isBig ? SIGN_BUTTON_SIZE * 3 : SIGN_BUTTON_SIZE);
            buttonsSelection[index].visible = false;
            addChild(new ClickableWidget(buttonsSelection[index]));
        }, true);

        final int buttonClearX = (width - PANEL_WIDTH - SQUARE_SIZE * 4) / 2;
        final int buttonY = height - SQUARE_SIZE * 2;

        IDrawing.setPositionAndWidth(buttonClear, buttonClearX, buttonY, PANEL_WIDTH);
        buttonClear.visible = false;
        addChild(new ClickableWidget(buttonClear));

        IDrawing.setPositionAndWidth(buttonPrevPage, buttonClearX + PANEL_WIDTH, buttonY, SQUARE_SIZE);
        buttonPrevPage.visible = false;
        addChild(new ClickableWidget(buttonPrevPage));
        IDrawing.setPositionAndWidth(buttonNextPage, buttonClearX + PANEL_WIDTH + SQUARE_SIZE * 3, buttonY, SQUARE_SIZE);
        buttonNextPage.visible = false;
        addChild(new ClickableWidget(buttonNextPage));

        addCopyPasteButtons();

        if (!isRailwaySign) {
            MinecraftClient.getInstance().openScreen(new Screen(new DashboardListSelectorScreen(this::onClose2, platformsForList, selectedIds.get(line), true, false, null)));
        }
    }

    /** 复制/粘贴/清空/撤销四个经典工具按钮（与 RailwaySignScreenMixin 相同）。 */
    private void addCopyPasteButtons() {
        final int y = 60;
        final int x = width - 35;

        final ButtonWidgetExtension undoButton = new ButtonWidgetExtension(x, y + 90, 30, 20, TextHelper.translatable("gui.nanbin.undo"), button -> {
            SignClipboard.undo(signPos, lines, length, signIds, selectedIds);
            button.setActiveMapped(SignClipboard.canUndo(signPos));
        });
        undoButton.active = SignClipboard.canUndo(signPos);

        final ButtonWidgetExtension copyButton = new ButtonWidgetExtension(x, y, 30, 20, TextHelper.translatable("gui.nanbin.copy"), button -> SignClipboard.copy(lines, length, signIds, selectedIds));

        final ButtonWidgetExtension pasteButton = new ButtonWidgetExtension(x, y + 30, 30, 20, TextHelper.translatable("gui.nanbin.paste"), button -> {
            SignClipboard.paste(signPos, lines, length, signIds, selectedIds);
            undoButton.setActiveMapped(SignClipboard.canUndo(signPos));
        });
        pasteButton.active = SignClipboard.canPaste(lines, length);

        final ButtonWidgetExtension clearButton = new ButtonWidgetExtension(x, y + 60, 30, 20, TextHelper.translatable("gui.nanbin.clear"), button -> MinecraftClient.getInstance().openScreen(new Screen(new RailwaySignClearConfirmScreen((ScreenExtension) (Object) this, TextHelper.translatable("gui.nanbin.clear.question"), () -> {
            SignClipboard.clear(signPos, lines, length, signIds, selectedIds);
            undoButton.setActiveMapped(SignClipboard.canUndo(signPos));
        }))));

        addChild(new ClickableWidget(copyButton));
        addChild(new ClickableWidget(pasteButton));
        addChild(new ClickableWidget(clearButton));
        addChild(new ClickableWidget(undoButton));
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        renderBackground(graphicsHolder);
        super.render(graphicsHolder, mouseX, mouseY, delta);

        for (int i = 0; i < lines; i++) {
            for (int j = 0; j < signIds[i].length; j++) {
                if (signIds[i][j] != null) {
                    RenderRailwaySign.drawSign(graphicsHolder, null, signPos, signIds[i][j], (width - SIGN_SIZE * length) / 2F + j * SIGN_SIZE, i * (SQUARE_SIZE + SIGN_SIZE), SIGN_SIZE, RenderRailwaySign.getMaxWidth(signIds[i], j, false), RenderRailwaySign.getMaxWidth(signIds[i], j, true), selectedIds.get(i), Direction.UP, 0, (textureId, x, y, size, flipTexture) -> {
                        final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
                        guiDrawing.beginDrawingTexture(textureId);
                        guiDrawing.drawTexture(x, y, x + size, y + size, flipTexture ? 1 : 0, 0, flipTexture ? 0 : 1, 1);
                        guiDrawing.finishDrawingTexture();
                    });
                }
            }
        }

        if (editingIndex >= 0) {
            final int xOffsetSmall = (width - SIGN_BUTTON_SIZE * (columns * 4 + 3)) / 2 + SIGN_BUTTON_SIZE;
            final int xOffsetBig = xOffsetSmall + SIGN_BUTTON_SIZE * (columns + 1);

            loopSigns((index, x, y, isBig) -> {
                final String signId = allSignIds.get(index);
                final SignResource sign = RenderRailwaySign.getSign(signId);
                if (sign != null) {
                    final boolean moveRight = sign.hasCustomText && sign.getFlipCustomText();
                    RenderRailwaySign.drawSign(graphicsHolder, null, signPos, signId, (isBig ? xOffsetBig : xOffsetSmall) + x + (moveRight ? SIGN_BUTTON_SIZE * 2 : 0), buttonYStart + y, SIGN_BUTTON_SIZE, 2, 2, selectedIds.get(line), Direction.UP, 0, (textureId, x1, y1, size, flipTexture) -> {
                        final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
                        guiDrawing.beginDrawingTexture(sign.getTexture());
                        guiDrawing.drawTexture(x1, y1, x1 + size, y1 + size, flipTexture ? 1 : 0, 0, flipTexture ? 0 : 1, 1);
                        guiDrawing.finishDrawingTexture();
                    });
                }
            }, false);

            graphicsHolder.drawCenteredText(String.format("%s/%s", page + 1, totalPages), (width - PANEL_WIDTH - SQUARE_SIZE * 4) / 2 + PANEL_WIDTH + SQUARE_SIZE * 2, height - SQUARE_SIZE * 2 + TEXT_PADDING, ARGB_WHITE);
        }
    }

    @Override
    public boolean mouseScrolled2(double mouseX, double mouseY, double amount) {
        setPage(page + (int) Math.signum(-amount));
        return super.mouseScrolled2(mouseX, mouseY, amount);
    }

    @Override
    public void onClose2() {
        if (onSave != null) {
            onSave.run();
        }
        super.onClose2();
    }

    @Override
    public boolean isPauseScreen2() {
        return false;
    }

    @Override
    public void resize2(MinecraftClient client, int width, int height) {
        super.resize2(client, width, height);
        for (int i = 0; i < lines; i++) {
            for (ButtonWidgetExtension button : buttonsEdit[i]) {
                button.active = true;
            }
        }
        for (ButtonWidgetExtension button : buttonsSelection) {
            button.visible = false;
        }
        editingIndex = -1;
    }

    protected int loopSigns(LoopSignsCallback loopSignsCallback, boolean ignorePage) {
        int pageCount = rows * columns;
        int indexSmall = 0;
        int indexBig = 0;
        int columnSmall = 0;
        int columnBig = 0;
        int rowSmall = 0;
        int rowBig = 0;
        int totalPagesSmallCount = 1;
        int totalPagesBigCount = 1;
        for (int i = 0; i < allSignIds.size(); i++) {
            final SignResource sign = RenderRailwaySign.getSign(allSignIds.get(i));
            final boolean isBig = sign != null && sign.hasCustomText;

            final boolean onPage = (isBig ? indexBig : indexSmall) / pageCount == page;
            buttonsSelection[i].visible = onPage;
            if (ignorePage || onPage) {
                loopSignsCallback.loopSignsCallback(i, (isBig ? columnBig * 3 : columnSmall) * SIGN_BUTTON_SIZE, (isBig ? rowBig : rowSmall) * SIGN_BUTTON_SIZE, isBig);
            }

            if (isBig) {
                columnBig++;
                if (totalPagesBigCount < 0) {
                    totalPagesBigCount = -totalPagesBigCount + 1;
                }
                if (columnBig >= columns) {
                    columnBig = 0;
                    rowBig++;
                    if (rowBig >= rows) {
                        rowBig = 0;
                        totalPagesBigCount = -totalPagesBigCount;
                    }
                }
                indexBig++;
            } else {
                columnSmall++;
                if (totalPagesSmallCount < 0) {
                    totalPagesSmallCount = -totalPagesSmallCount + 1;
                }
                if (columnSmall >= columns) {
                    columnSmall = 0;
                    rowSmall++;
                    if (rowSmall >= rows) {
                        rowSmall = 0;
                        totalPagesSmallCount = -totalPagesSmallCount;
                    }
                }
                indexSmall++;
            }
        }
        return Math.max(Math.abs(totalPagesBigCount), Math.abs(totalPagesSmallCount));
    }

    protected void edit(int line, int editingIndex) {
        this.line = line;
        this.editingIndex = editingIndex;
        for (int i = 0; i < lines; i++) {
            for (ButtonWidgetExtension button : buttonsEdit[i]) {
                button.active = true;
            }
        }
        buttonClear.visible = true;
        setPage(page);
        buttonsEdit[line][editingIndex].active = false;
    }

    protected void setNewSignId(@Nullable String newSignId) {
        if (editingIndex >= 0 && editingIndex < signIds[0].length) {
            signIds[line][editingIndex] = newSignId;
            final boolean isExitLetter = signIsExit(newSignId);
            final boolean isPlatform = signIsPlatform(newSignId);
            final boolean isLine = signIsLine(newSignId);
            final boolean isStation = signIsStation(newSignId);
            if ((isExitLetter || isPlatform || isLine || isStation)) {
                MinecraftClient.getInstance().openScreen(new Screen(new DashboardListSelectorScreen(this::onClose2, new ObjectImmutableList<>(isExitLetter ? exitsForList : (isPlatform ? platformsForList : (isLine ? routesForList : stationsForList))), selectedIds.get(line), false, false, null)));
            }
        }
    }

    protected void setPage(int newPage) {
        page = MathHelper.clamp(newPage, 0, totalPages - 1);
        buttonPrevPage.visible = editingIndex >= 0 && page > 0;
        buttonNextPage.visible = editingIndex >= 0 && page < totalPages - 1;
    }

    @FunctionalInterface
    protected interface LoopSignsCallback {
        void loopSignsCallback(int index, int x, int y, boolean isBig);
    }

    private static boolean signIsExit(@Nullable String signId) {
        return "exit_letter".equals(signId) || "exit_letter_flipped".equals(signId);
    }

    private static boolean signIsPlatform(@Nullable String signId) {
        return "platform".equals(signId) || "platform_flipped".equals(signId);
    }

    private static boolean signIsLine(@Nullable String signId) {
        return "line".equals(signId) || "line_flipped".equals(signId)
                || "crt_route_name".equals(signId) || "crt_route_name_flipped".equals(signId) || "crt_route_number".equals(signId);
    }

    private static boolean signIsStation(@Nullable String signId) {
        return "station".equals(signId) || "station_flipped".equals(signId);
    }
}
