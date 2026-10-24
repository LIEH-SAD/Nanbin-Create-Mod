package com.Nanbin.mixin;

import com.Nanbin.client.JavaScriptSupport.JSSignConfig;
import com.Nanbin.client.Render.RenderCRTRailwaySign;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import mtr.block.BlockRailwaySign;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.client.CustomResources;
import mtr.data.Route;
import mtr.render.RenderRailwaySign;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * 为原版 MTR 铁路指示牌注入 JS 样式渲染：当告示牌第 0 格为 crt_js_style_ 标记时，
 * 拦截原版渲染并调用 CRT 的 JS 样式渲染器。
 */
@Mixin(value = RenderRailwaySign.class, remap = false)
public abstract class RenderRailwaySignMixin {

	// allow=2：RenderRailwaySign 可能有实际 render 方法与接口桥方法（重映射后同名），
	// 允许匹配多个避免 ambiguous 崩溃；handler 幂等（非 JS 样式直接 return）。
	@Inject(method = "render", at = @At("HEAD"), cancellable = true, allow = 2)
	private void nanbin$renderJSStyle(BlockRailwaySign.TileEntityRailwaySign entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, CallbackInfo ci) {
		String[] signIds = entity.getSignIds();
		String styleScriptId = JSSignConfig.getStyleScriptId(signIds);
		if (styleScriptId == null) {
			return;
		}

		// Skip rendering when all cells are empty
		boolean allEmpty = true;
		for (String id : signIds) {
			if (id != null && !id.isEmpty()) {
				allEmpty = false;
				break;
			}
		}
		if (allEmpty) {
			return;
		}

		World world = entity.getWorld();
		if (world == null) {
			return;
		}
		BlockPos pos = entity.getPos();
		BlockState blockState = world.getBlockState(pos);
		if (!(blockState.getBlock() instanceof BlockRailwaySign)) {
			return;
		}
		BlockRailwaySign railwaySignBlock = (BlockRailwaySign) blockState.getBlock();

		if (signIds.length != railwaySignBlock.length) {
			return;
		}

		Direction facing = IBlock.getStatePropertySafe(blockState, HorizontalFacingBlock.FACING);

		int backgroundColor = 0;
		for (String signId : signIds) {
			if (signId != null && !signId.isEmpty()) {
				CustomResources.CustomSign sign = RenderRailwaySign.getSign(signId);
				if (sign != null && sign.backgroundColor != 0) {
					backgroundColor = sign.backgroundColor;
					break;
				}
			}
		}

		Map<Long, String> routeNumberMap = new HashMap<>();
		for (Route route : ClientData.ROUTES) {
			String name = route.name;
			if (name != null && !name.isEmpty()) {
				// 键必须是 route.color 本身（RGB）：脚本 getRouteNumber(color) 内部按 (key & 0xFFFFFF) 查表
				routeNumberMap.put((long) route.color, name);
			}
		}

		// 与 CRT 渲染器一致：在 render 传入的相对坐标矩阵上做本地变换后绘制
		matrices.push();
		matrices.translate(0.5, 0.53125, 0.5);
		matrices.multiply(new Quaternionf().rotationY(MathHelper.RADIANS_PER_DEGREE * -facing.asRotation()));
		matrices.multiply(new Quaternionf().rotationZ(MathHelper.RADIANS_PER_DEGREE * 180.0F));
		matrices.translate((float) railwaySignBlock.getXStart() / 16.0F - 0.5F, 0.0, -0.075);

		RenderCRTRailwaySign.renderJSStyleLine(styleScriptId, matrices, vertexConsumers, pos, signIds, new LongAVLTreeSet(entity.getSelectedIds()), new String[0], facing, backgroundColor, 0.5F, 0.0F, 0.0F, false, routeNumberMap);

		matrices.pop();

		ci.cancel();
	}
}
