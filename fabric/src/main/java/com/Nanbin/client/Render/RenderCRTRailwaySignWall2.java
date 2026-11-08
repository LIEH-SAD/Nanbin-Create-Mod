package com.Nanbin.client.Render;

import com.Nanbin.Registry.RegBlock.BlockCRTRailwaySignWall2;
import org.mtr.mapping.mapper.BlockEntityRenderer;
import org.mtr.mapping.mapper.GraphicsHolder;

// CRT 墙挂铁路指示牌2 渲染器：完全复用1的绘制流程，只有模型几何和发光独立可调。
public class RenderCRTRailwaySignWall2 extends RenderCRTRailwaySignWall1<BlockCRTRailwaySignWall2.BlockEntity> {

	public RenderCRTRailwaySignWall2(BlockEntityRenderer.Argument dispatcher) {
		super(dispatcher);
		// 2 的模型是 z=0~0.5px 的薄板，内容贴在牌面南面（0.5px）外侧 0.1px，即 z=0.6px。
		// contentOffsetZ = 0.5 - 0.6 / 16，比1的旧模型（内容 z=2.1px）向内收了 1.5px。
		contentOffsetZ = 0.4625F;
	}

	// 2 的发光等级为 0：牌面内容跟随方块所在位置的世界光照，不再全亮。
	@Override
	protected int contentLight(int worldLight) {
		return worldLight;
	}
}
