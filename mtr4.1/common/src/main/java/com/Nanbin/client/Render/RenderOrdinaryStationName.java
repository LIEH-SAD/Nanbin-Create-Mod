package com.Nanbin.client.Render;

import com.Nanbin.Registry.RegBlock.BlockOrdinaryStationName;

/**
 * 普通站名牌的渲染器（布局常量见 {@link RenderCRTStationNameBase#STYLE_ORDINARY}）。
 */
public class RenderOrdinaryStationName extends RenderCRTStationNameBase<BlockOrdinaryStationName.BlockEntity> {

	public RenderOrdinaryStationName() {
		super(STYLE_ORDINARY);
	}
}
