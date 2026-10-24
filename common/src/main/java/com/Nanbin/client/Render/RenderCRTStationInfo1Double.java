package com.Nanbin.client.Render;

import com.Nanbin.Registry.RegBlock.BlockCRTStationInfoBase;

/**
 * 站台信息屏（双面）的渲染器：正面 + 背面各画一次，两面数据各自独立。
 */
public class RenderCRTStationInfo1Double extends RenderCRTStationInfoBase {

	@Override
	protected boolean onlyFront() {
		return false;
	}
}
