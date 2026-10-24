package com.Nanbin.client.Render;

import com.Nanbin.Registry.RegBlock.BlockCRTAPGDoor1;
import com.Nanbin.Registry.RegBlock.BlockCRTAPGGlass1;
import com.Nanbin.Registry.RegBlock.BlockCRTAPGGlassEnd1;
import com.Nanbin.Registry.RegBlock.BlockCRTPlatform;
import net.minecraft.block.Block;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.mtr.render.MainRenderer;
import org.mtr.render.QueuedRenderLayer;

/**
 * CRT 屏蔽门（一代）渲染器。
 * <p>
 * 一代的特点：整扇门（上下面板 + 下缘挡板）都压在下半格的模型里，
 * 所以只有 {@code !half} 时才画门体；提示灯贴图用 {@code *_old} 后缀的那一套，
 * 地面灯在列车接近时会黄灯闪烁。
 * <p>
 * 源项目里这个渲染器注册时传的 {@code type} 是 <b>2</b>（APG 门），本移植沿用。
 */
public class RenderCRTAPGDoor1 extends RenderCRTAPGDoorBase<BlockCRTAPGDoor1.BlockEntity> {

	/** 一代门体下半格模型（34×27 贴图）。 */
	private static final ModelPart CRT_APG_BOTTOM_1 = DoorModels.createModelPart(root -> {
		DoorModels.createModelPartData(root, 0, 0, -8, -17, -7, 16, 16, 1, 0, 0, 0, 0, 0, 0, 0);
		DoorModels.createModelPartData(root, 0, 17, -8, -7, -8, 16, 6, 1, 0, 0, 0, 0, 0, 0, 0);
		// 下缘向外翻的挡板：轴心在门页底缘，绕 X 轴转 -45°
		DoorModels.createModelPartData(root, 0, 24, -8, -3, -2, 16, 2, 1, 0, 0, -5, -8, -0.7854F, 0, 0);
	}, 34, 27);

	/** 门上方的指示灯（8×8）。 */
	private static final ModelPart CRT_APG_LIGHT_1 = DoorModels.createModelPart(root ->
			DoorModels.createModelPartData(root, 0, 4, -8, -1.5F, -5.2F, 4, 1, 1, 0.05F, 0, 0, 0, 0, 0, 0), 8, 8);

	/** 地面灯（16×16），只有一层平面。 */
	private static final ModelPart CRT_APG_GROUND_LIGHT_1 = DoorModels.createModelPart(root ->
			DoorModels.createModelPartData(root, 0, 0, -8, 0, -8, 16, 0, 16, 0, 0, 0, 0, 0, 0, 0), 16, 16);

	/** 门未解锁时贴的「停用」告示牌（6×6）。 */
	private static final ModelPart CRT_APG_DOOR_LOCKED_1 = DoorModels.createSingleCube(6, 6, 5, 20, 1, 3, 3, 0);

	public RenderCRTAPGDoor1() {
		super(2);
	}

	@Override
	protected void renderLighting(DoorState state) {
		// 门上方的指示灯：仅右门页、且旁边是 CRT 一代 APG 玻璃时才有
		if (state.half && state.side) {
			final Block block = state.world.getBlockState(state.pos.offset(state.facing.rotateYClockwise())).getBlock();
			if (block instanceof BlockCRTAPGGlass1 || block instanceof BlockCRTAPGGlassEnd1) {
				final String lightTexture;
				final QueuedRenderLayer renderLayer;
				if (state.doorFullyOpen) {
					lightTexture = "green";
					renderLayer = QueuedRenderLayer.LIGHT_TRANSLUCENT;
				} else if (state.doorMoving) {
					lightTexture = (state.world.getTime() % (long) DOOR_MOVING_BLINK_PERIOD) < (DOOR_MOVING_BLINK_PERIOD / 2.0) ? "off" : "green";
					renderLayer = QueuedRenderLayer.LIGHT_TRANSLUCENT;
				} else {
					// 未开门时不提示「列车接近」：一代这套贴图只有 off/green
					lightTexture = "off";
					renderLayer = QueuedRenderLayer.EXTERIOR;
				}

				MainRenderer.scheduleRender(Identifier.of("nanbin", "textures/block/crt_apg_door_light_" + lightTexture + "_old.png"), false, renderLayer, (matrices, vertexConsumer, offset) -> {
					state.storedLight.transform(matrices, offset);
					matrices.translate(-0.50F, 0, 0);
					matrices.scale(0.5F, 1, 1);
					CRT_APG_LIGHT_1.render(matrices, vertexConsumer, state.light, OverlayTexture.DEFAULT_UV);
					matrices.pop();
				});
			}
		}

		// 地面灯：下半格、且正下方是 CRT 站台时才有
		if (!state.half) {
			final Block block = state.world.getBlockState(state.pos.offset(Direction.DOWN)).getBlock();
			if (block instanceof BlockCRTPlatform) {
				final String groundLightTexture;
				final QueuedRenderLayer groundRenderLayer;
				if (state.doorFullyOpen) {
					groundLightTexture = "green";
					groundRenderLayer = QueuedRenderLayer.LIGHT_TRANSLUCENT;
				} else if (state.doorMoving) {
					groundLightTexture = "off";
					groundRenderLayer = QueuedRenderLayer.EXTERIOR;
				} else if (state.trainApproaching) {
					groundLightTexture = (state.world.getTime() % (long) ARRIVAL_WARNING_BLINK_PERIOD) < (ARRIVAL_WARNING_BLINK_PERIOD / 2.0) ? "off" : "yellow";
					groundRenderLayer = QueuedRenderLayer.LIGHT_TRANSLUCENT;
				} else {
					groundLightTexture = "off";
					groundRenderLayer = QueuedRenderLayer.EXTERIOR;
				}

				final String groundTexture = state.side ? "crt_ground_light_" + groundLightTexture + ".png" : "crt_ground_light_" + groundLightTexture + "_flipped.png";
				MainRenderer.scheduleRender(Identifier.of("nanbin", "textures/block/" + groundTexture), false, groundRenderLayer, (matrices, vertexConsumer, offset) -> {
					state.storedGround.transform(matrices, offset);
					CRT_APG_GROUND_LIGHT_1.render(matrices, vertexConsumer, state.light, OverlayTexture.DEFAULT_UV);
					matrices.pop();
				});
			}
		}
	}

	@Override
	protected void renderDoorBody(DoorState state) {
		if (!state.half) {
			MainRenderer.scheduleRender(Identifier.of("nanbin", "textures/block/crt_apg_door_1_bottom_" + (state.side ? "right" : "left") + ".png"), false, QueuedRenderLayer.EXTERIOR, (matrices, vertexConsumer, offset) -> {
				state.storedBody.transform(matrices, offset);
				CRT_APG_BOTTOM_1.render(matrices, vertexConsumer, state.light, OverlayTexture.DEFAULT_UV);
				matrices.pop();
			});
			if (state.side) {
				renderDoorNumberPlate(state);
			}
		}
		if (state.half && !state.unlocked) {
			MainRenderer.scheduleRender(DOOR_NOT_IN_USE_TEXTURE, false, QueuedRenderLayer.EXTERIOR, (matrices, vertexConsumer, offset) -> {
				state.storedBody.transform(matrices, offset);
				CRT_APG_DOOR_LOCKED_1.render(matrices, vertexConsumer, state.light, OverlayTexture.DEFAULT_UV);
				matrices.pop();
			});
		}
	}
}
