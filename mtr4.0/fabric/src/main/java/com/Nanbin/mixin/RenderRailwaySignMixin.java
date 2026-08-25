package com.Nanbin.mixin;

import com.Nanbin.client.JavaScriptSupport.JSSignConfig;
import com.Nanbin.client.Render.RenderCRTRailwaySign;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.block.BlockRailwaySign;
import org.mtr.mod.block.BlockStationNameBase;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.render.RenderRailwaySign;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.mtr.mod.resource.SignResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = RenderRailwaySign.class, remap = false)
public abstract class RenderRailwaySignMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void nanbin$renderJSStyle(BlockRailwaySign.BlockEntity entity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay, CallbackInfo ci) {
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

        World world = entity.getWorld2();
        if (world == null) {
            return;
        }
        BlockPos pos = entity.getPos2();
        BlockState blockState = world.getBlockState(pos);
        if (!(blockState.getBlock().data instanceof BlockRailwaySign)) {
            return;
        }
        BlockRailwaySign railwaySignBlock = (BlockRailwaySign) blockState.getBlock().data;

        if (signIds.length != railwaySignBlock.length) {
            return;
        }

        Direction facing = IBlock.getStatePropertySafe(blockState, BlockStationNameBase.FACING);

        int backgroundColor = 0;
        for (String signId : signIds) {
            if (signId != null && !signId.isEmpty()) {
                SignResource sign = RenderRailwaySign.getSign(signId);
                if (sign != null && sign.getBackgroundColor() != 0) {
                    backgroundColor = sign.getBackgroundColor();
                    break;
                }
            }
        }

        StoredMatrixTransformations storedMatrixTransformations = new StoredMatrixTransformations(
                0.5 + pos.getX(),
                0.53125 + pos.getY(),
                0.5 + pos.getZ()
        );
        storedMatrixTransformations.add((graphicsHolderNew) -> {
            graphicsHolderNew.rotateYDegrees(-facing.asRotation());
            graphicsHolderNew.rotateZDegrees(180.0F);
            graphicsHolderNew.translate(
                    (float) railwaySignBlock.getXStart() / 16.0F - 0.5F,
                    0.0,
                    -0.06875000009313226
            );
        });

        Map<Long, String> routeNumberMap = new HashMap<>();
        for (SimplifiedRoute route : MinecraftClientData.getInstance().simplifiedRoutes) {
            String name = route.getName();
            if (name != null && !name.isEmpty()) {
                routeNumberMap.put(route.getColor() | 0xFF000000L, name);
            }
        }

        graphicsHolder.push();
        graphicsHolder.translate(0.5, 0.53125, 0.5);
        graphicsHolder.rotateYDegrees(-facing.asRotation());
        graphicsHolder.rotateZDegrees(180.0F);
        graphicsHolder.translate(
                (float) railwaySignBlock.getXStart() / 16.0F - 0.5F,
                0.0,
                -0.06875000009313226
        );

        RenderCRTRailwaySign.renderJSStyleLine(styleScriptId, storedMatrixTransformations, pos, signIds, entity.getSelectedIds(), new String[0], facing, backgroundColor, 0.5F, 0.0F, 0.0F, false, routeNumberMap);

        graphicsHolder.pop();
        ci.cancel();
    }
}