package com.Nanbin.signeditor;

import com.Nanbin.client.Render.RenderCRTRailwaySign;
import com.Nanbin.client.Render.SignDrawing;

import gg.essential.universal.UMatrixStack;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;

import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.registry.UConverters;
import org.mtr.tool.GuiAnimation;
import org.mtr.tool.ReleasedDynamicTextureRegistry;
import org.mtr.widget.ImageComponentBase;
import org.mtr.widget.SlotBackgroundComponent;

import java.util.function.IntConsumer;


public class CrtSignPreviewComponent extends SlotBackgroundComponent {

	private static final int ARROW_SPRITE_SIZE = 18;
	private static final int ARROW_OFFSET = 14;
	private static final int ANIMATION_DURATION = 200;

	private final BlockPos signPos;
	private final String[] signIds;
	private final LongAVLTreeSet[] selectedIds;
	private final String[] routeNumbers;
	private final GuiAnimation guiAnimation = new GuiAnimation();

	private int editingIndex = -1;
	private IntConsumer editCallback;

	public CrtSignPreviewComponent(BlockPos signPos, String[] signIds, LongAVLTreeSet[] selectedIds, String[] routeNumbers) {
		this.signPos = signPos;
		this.signIds = signIds;
		this.selectedIds = selectedIds;
		this.routeNumbers = routeNumbers;
		onMouseClickConsumer(event -> {
			if (editCallback == null || signIds == null || signIds.length == 0) {
				return;
			}
			final float innerWidth = getWidth() - 2.0F;
			if (innerWidth <= 0) {
				return;
			}
			final float cellSize = innerWidth / signIds.length;
			final int cell = (int) Math.floor((event.getAbsoluteX() - getLeft() - 1.0F) / cellSize);
			if (cell >= 0 && cell < signIds.length) {
				editCallback.accept(cell);
			}
		});
	}

	public void onEdit(IntConsumer editCallback) {
		this.editCallback = editCallback;
	}

	public void setEditingIndex(int index) {
		if (index == editingIndex) {
			return;
		}
		editingIndex = index;
		if (index >= 0) {
			guiAnimation.animate(index, ANIMATION_DURATION);
		}
	}

	@Override
	public void draw(UMatrixStack uMatrixStack) {
		super.draw(uMatrixStack);

		guiAnimation.tick();

		final float innerWidth = getWidth() - 2.0F;
		final float innerHeight = getHeight() - 2.0F;
		if (innerWidth <= 0 || innerHeight <= 0 || signIds == null || signIds.length == 0) {
			return;
		}

		// 选中箭头：贴在预览框上方，跟着当前编辑的格子左右滑动（与 MTR 4.1 同款）
		if (editingIndex >= 0) {
			final float cellSize = innerWidth / signIds.length;
			final double arrowX = getLeft() + 1.0 + guiAnimation.getCurrentValue() * cellSize + (cellSize - ARROW_SPRITE_SIZE) / 2.0;
			final double arrowY = getTop() + 1.0 - ARROW_OFFSET;
			ImageComponentBase.drawTexture(ReleasedDynamicTextureRegistry.ARROW_DOWN_TEXTURE.get(), vertexConsumer ->
					ImageComponentBase.drawTexturedQuad(uMatrixStack, vertexConsumer, (float) arrowX, (float) arrowY,
							(float) arrowX + ARROW_SPRITE_SIZE, (float) arrowY + ARROW_SPRITE_SIZE, 0, 0, 1, 1));
		}

		final MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			return;
		}
		final VertexConsumerProvider.Immediate bufferSource = client.getBufferBuilders().getEntityVertexConsumers();
		final MatrixStack matrixStack = UConverters.convert(uMatrixStack);

		matrixStack.push();
		matrixStack.translate(getLeft() + 1.0F, getTop() + 1.0F, 0);
		try {
			// 格子边长：预览宽度被等比约束锁死成「格数 × 高度」，所以横向刚好铺满
			RenderCRTRailwaySign.drawSignRow(matrixStack, bufferSource, SignDrawing.immediate(matrixStack, bufferSource),
					signPos, signIds, selectedIds, routeNumbers, innerWidth / signIds.length, false, true);
		} finally {
			matrixStack.pop();
		}
		bufferSource.draw();
	}
}
