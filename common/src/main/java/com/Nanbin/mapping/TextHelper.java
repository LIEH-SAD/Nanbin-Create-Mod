package com.Nanbin.mapping;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

/**
 * MTR 4.0 {@code org.mtr.mapping.mapper.TextHelper} 的等价物。
 *
 * <p>{@code org.mtr.libraries} 之外，MTR 4.0 这层只做了一件事：把文本构造统一成
 * {@code MutableText}。MTR 3.6.3 直接用原版 {@link Text}，所以这里是最薄的转发。</p>
 */
public final class TextHelper {

	private TextHelper() {
	}

	public static MutableText translatable(String key, Object... args) {
		return Text.translatable(key, args);
	}

	public static MutableText literal(String text) {
		return Text.literal(text);
	}

	public static MutableText setStyle(MutableText text, Style style) {
		return text.setStyle(style);
	}

	/** MTR 4.0 的 OrderedText 包装；原版用 {@link Text#asOrderedText()} 等价转换。 */
	public static net.minecraft.text.OrderedText mutableTextToOrderedText(MutableText text) {
		return text.asOrderedText();
	}

	public static MutableText append(MutableText base, MutableText... extras) {
		for (final MutableText extra : extras) {
			base.append(extra);
		}
		return base;
	}
}
