package com.Nanbin.mapping;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * 取代 MTR 4.0 的 {@code org.mtr.mapping.mapper.TextHelper} + {@code GraphicsHolder.getTextWidth}。
 */
public interface TranslationProvider {

	TranslationHolder BLOCK_CAB_DOOR = new TranslationHolder("tooltip.nanbin.block.cab_door");
	TranslationHolder BLOCK_TEMP_FENCE = new TranslationHolder("tooltip.nanbin.block.crt_temp_fence_1");
	TranslationHolder BLOCK_TALL_FENCE = new TranslationHolder("tooltip.nanbin.block.tall_fence");
	TranslationHolder TOOLTIP_STATION_COLOR = new TranslationHolder("tooltip.mtr.station_color");
	TranslationHolder BRUSH_USE = new TranslationHolder("tooltip.nanbin.brush_use");
	TranslationHolder APG_DOOR_SORT_FROM_LEFT = new TranslationHolder("tooltip.nanbin.apg_door_sort_from_left");
	TranslationHolder APG_DOOR_SORT_FROM_RIGHT = new TranslationHolder("tooltip.nanbin.apg_door_sort_from_right");
	TranslationHolder TICKET_FAULT_IN_USE = new TranslationHolder("tips.nanbin.ticket_fault.in_use");
	TranslationHolder TICKET_FAULT_ON = new TranslationHolder("tips.nanbin.ticket_fault.on");
	TranslationHolder TICKET_FAULT_OFF = new TranslationHolder("tips.nanbin.ticket_fault.off");

	final class TranslationHolder {

		public final String key;

		TranslationHolder(String key) {
			this.key = key;
		}

		public MutableText getMutableText(Object... arguments) {
			return Text.translatable(key, arguments);
		}

		public Text getText(Object... arguments) {
			return getMutableText(arguments);
		}

		public String getString(Object... arguments) {
			return getMutableText(arguments).getString();
		}

		public int width(Object... arguments) {
			return MinecraftClient.getInstance().textRenderer.getWidth(getMutableText(arguments));
		}
	}
}
