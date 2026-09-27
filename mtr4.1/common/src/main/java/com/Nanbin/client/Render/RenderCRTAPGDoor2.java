package com.Nanbin.client.Render;

import com.Nanbin.Registry.RegBlock.BlockCRTAPGDoor2;
import com.Nanbin.Registry.RegBlock.BlockCRTAPGGlass2;
import com.Nanbin.Registry.RegBlock.BlockCRTAPGGlassEnd2;
import com.Nanbin.Registry.RegBlock.BlockCRTPlatform;
import net.minecraft.block.Block;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.mtr.block.BlockAPGGlass;
import org.mtr.block.BlockAPGGlassEnd;
import org.mtr.render.MainRenderer;
import org.mtr.render.QueuedRenderLayer;

/**
 * CRT 屏蔽门（二代）渲染器。
 * <p>
 * 二代的特点：门体拆成上下两块模型，配 {@code crt_apg_door_2_top/bottom_left|right} 四张贴图；
 * 指示灯与地面灯都用 {@code crt_apg_door_light_*}/{@code crt_ground_light_off*} 这套新贴图，
 * 地面灯常亮不闪（源项目里就是这样：二代的地面灯永远只画 off 那张）。
 * <p>
 * 源项目里这个渲染器注册时传的 {@code type} 是 <b>2</b>（APG 门），本移植沿用。
 */
public class RenderCRTAPGDoor2 extends RenderCRTAPGDoorBase<BlockCRTAPGDoor2.BlockEntity> {

	/** 二代门体上半格模型：一块 16×8 的平板。 */
	private static final ModelPart CRT_APG_TOP_2 = DoorModels.createSingleCube(34, 9, 0, 8, 1, 16, 8, 1);

	/** 二代门体下半格模型（34×27 贴图）。 */
	private static final ModelPart CRT_APG_BOTTOM_2 = DoorModels.createModelPart(root -> {
		DoorModels.createModelPartData(root, 0, 0, -8, -16, -7, 16, 16, 1, 0, 0, 0, 0, 0, 0, 0);
		DoorModels.createModelPartData(root, 0, 17, -8, -6, -8, 16, 6, 1, 0, 0, 0, 0, 0, 0, 0);
		DoorModels.createModelPartData(root, 0, 24, -8, -2, 0, 16, 2, 1, 0, 0, -6, -8, -0.7854F, 0, 0);
	}, 34, 27);

	/** 二代指示灯（8×8）：两块 1×1×3 的灯板，靠门框一侧 + 斜角一块。 */
	private static final ModelPart CRT_APG_LIGHT_2 = DoorModels.createModelPart(root -> {
		DoorModels.createModelPartData(root, 0, 4, -0.5F, -9, -7, 1, 1, 3, 0.05F, 0, 0, 0, 0, 0, 0);
		DoorModels.createModelPartData(root, 0, 0, -0.5F, 0.05F, -3.05F, 1, 1, 3, 0.05F, 0, -9.05F, -4.95F, 0.3927F, 0, 0);
	}, 8, 8);

	/** 二代地面灯（16×16）：比一代厚一格。 */
	private static final ModelPart CRT_APG_GROUND_LIGHT_2 = DoorModels.createModelPart(root ->
			DoorModels.createModelPartData(root, 0, 0, -8, 0, -8, 16, 1, 16, 0, 0, 0, 0, 0, 0, 0), 16, 16);

	/** 门未解锁时贴的「停用」告示牌（6×6）。 */
	private static final ModelPart CRT_APG_DOOR_LOCKED_2 = DoorModels.createSingleCube(6, 6, 5, 10, 1, 6, 6, 0);

	public RenderCRTAPGDoor2() {
		super(2);
	}

	@Override
	protected void renderLighting(DoorState state) {
		// 门上方的指示灯：两页都画，旁边的玻璃按门页方向取；MTR 自己的 APG 玻璃也算
		if (state.half) {
			final Block block = state.world.getBlockState(state.pos.offset(state.side ? state.facing.rotateYClockwise() : state.facing.rotateYCounterclockwise())).getBlock();
			if (block instanceof BlockCRTAPGGlass2 || block instanceof BlockCRTAPGGlassEnd2 || block instanceof BlockAPGGlass || block instanceof BlockAPGGlassEnd) {
				final String lightTexture;
				final QueuedRenderLayer renderLayer;
				if (state.doorFullyOpen) {
					lightTexture = "green";
					renderLayer = QueuedRenderLayer.LIGHT_TRANSLUCENT;
				} else if (state.doorMoving) {
					lightTexture = (state.world.getTime() % (long) DOOR_MOVING_BLINK_PERIOD) < (DOOR_MOVING_BLINK_PERIOD / 2.0) ? "off" : "green";
					renderLayer = QueuedRenderLayer.LIGHT_TRANSLUCENT;
				} else if (state.trainApproaching) {
					lightTexture = (state.world.getTime() % (long) ARRIVAL_WARNING_BLINK_PERIOD) < (ARRIVAL_WARNING_BLINK_PERIOD / 2.0) ? "off" : "yellow";
					renderLayer = QueuedRenderLayer.LIGHT_TRANSLUCENT;
				} else {
					lightTexture = "off";
					renderLayer = QueuedRenderLayer.EXTERIOR;
				}

				MainRenderer.scheduleRender(Identifier.of("nanbin", "textures/block/crt_apg_door_light_" + lightTexture + ".png"), false, renderLayer, (matrices, vertexConsumer, offset) -> {
					state.storedLight.transform(matrices, offset);
					matrices.translate(state.side ? -0.50F : 0.50F, 0, 0);
					matrices.scale(0.5F, 1, 1);
					CRT_APG_LIGHT_2.render(matrices, vertexConsumer, state.light, OverlayTexture.DEFAULT_UV);
					matrices.pop();
				});
			}
		}

		// 地面灯：下半格、且正下方是 CRT 站台时才有；二代不闪烁，永远画 off 那张
		if (!state.half) {
			final Block block = state.world.getBlockState(state.pos.offset(Direction.DOWN)).getBlock();
			if (block instanceof BlockCRTPlatform) {
				final String groundTexture = state.side ? "crt_ground_light_off.png" : "crt_ground_light_off_flipped.png";
				MainRenderer.scheduleRender(Identifier.of("nanbin", "textures/block/" + groundTexture), false, QueuedRenderLayer.EXTERIOR, (matrices, vertexConsumer, offset) -> {
					state.storedGround.transform(matrices, offset);
					CRT_APG_GROUND_LIGHT_2.render(matrices, vertexConsumer, state.light, OverlayTexture.DEFAULT_UV);
					matrices.pop();
				});
			}
		}
	}

	@Override
	protected void renderDoorBody(DoorState state) {
		MainRenderer.scheduleRender(Identifier.of("nanbin", "textures/block/crt_apg_door_2_" + (state.half ? "top" : "bottom") + "_" + (state.side ? "right" : "left") + ".png"), false, QueuedRenderLayer.EXTERIOR, (matrices, vertexConsumer, offset) -> {
			state.storedBody.transform(matrices, offset);
			(state.half ? CRT_APG_TOP_2 : CRT_APG_BOTTOM_2).render(matrices, vertexConsumer, state.light, OverlayTexture.DEFAULT_UV);
			matrices.pop();
		});
		if (!state.half && state.side) {
			renderDoorNumberPlate(state);
		}
		if (state.half && !state.unlocked) {
			MainRenderer.scheduleRender(DOOR_NOT_IN_USE_TEXTURE, false, QueuedRenderLayer.EXTERIOR, (matrices, vertexConsumer, offset) -> {
				state.storedBody.transform(matrices, offset);
				CRT_APG_DOOR_LOCKED_2.render(matrices, vertexConsumer, state.light, OverlayTexture.DEFAULT_UV);
				matrices.pop();
			});
		}
	}
}
