package com.Nanbin.client.Render;

import com.Nanbin.Registry.RegBlock.BlockCRTStationInfoBase;
import com.Nanbin.Registry.RegBlock.BlockCRTStationInfo1;

/**
 * 站台信息屏（单面）的渲染器：只画正面条带。
 * <p>
 * 单面板的 BE 只存了一份数据（{@code front = true}），所以这里固定传正面。
 */
public class RenderCRTStationInfo1 extends RenderCRTStationInfoBase {

	@Override
	protected boolean onlyFront() {
		return true;
	}
}
