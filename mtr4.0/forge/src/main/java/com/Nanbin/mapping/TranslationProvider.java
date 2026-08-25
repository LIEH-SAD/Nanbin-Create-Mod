package com.Nanbin.mapping;

import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.TextHelper;

public interface TranslationProvider {
    TranslationHolder BLOCK_CAB_DOOR = new TranslationHolder("tooltip.nanbin.block.cab_door");
    TranslationHolder BLOCK_TEMP_FENCE = new TranslationHolder("tooltip.nanbin.block.crt_temp_fence_1");
    TranslationHolder BLOCK_TALL_FENCE = new TranslationHolder("tooltip.nanbin.block.tall_fence");
    TranslationHolder TOOLTIP_STATION_COLOR = new TranslationHolder("tooltip.mtr.station_color");
    TranslationHolder BRUSH_USE = new TranslationHolder("tooltip.nanbin.brush_use");
    TranslationHolder APG_DOOR_SORT_FROM_LEFT = new TranslationHolder("tooltip.nanbin.apg_door_sort_from_left");
    TranslationHolder APG_DOOR_SORT_FROM_RIGHT = new TranslationHolder("tooltip.nanbin.apg_door_sort_from_right");

    public static class TranslationHolder {
        public final String key;

        private TranslationHolder(String key) {
            this.key = key;
        }

        public MutableText getMutableText(Object... arguments) {
            return TextHelper.translatable(this.key, arguments);
        }

        public Text getText(Object... arguments) {
            return Text.cast(TextHelper.translatable(this.key, arguments));
        }

        public String getString(Object... arguments) {
            return this.getMutableText(arguments).getString();
        }

        public int width(Object... arguments) {
            return GraphicsHolder.getTextWidth(this.getMutableText(arguments));
        }
    }
}