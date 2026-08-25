package com.Nanbin.mixin;

import com.Nanbin.client.Drawing.SignTextStyleConfig;
import com.Nanbin.client.Drawing.SignTextStyleConfig.SignTextStyle;
import com.Nanbin.client.JavaScriptSupport.JSSignLoader;
import org.mtr.core.serializer.JsonReader;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.ResourceManagerHelper;
import org.mtr.mod.Init;
import org.mtr.mod.client.CustomResourceLoader;
import org.mtr.mod.config.Config;
import org.mtr.mod.resource.SignResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

/**
 * 为 MTR 原版铁路告示牌（RAILWAY_SIGN）注入更多可选标志。
 */
@Mixin(value = CustomResourceLoader.class, remap = false)
public class CustomResourceLoaderMixin {

	private static final Identifier CUSTOM_SIGNS_ID = new Identifier("nanbin", "signs.json");

	@Inject(method = "reload", at = @At("RETURN"))
	private static void nanbin$addCustomSigns(CallbackInfo ci) {
		try {
			final Field signsField = CustomResourceLoader.class.getDeclaredField("SIGNS");
			final Field signsCacheField = CustomResourceLoader.class.getDeclaredField("SIGNS_CACHE");
			signsField.setAccessible(true);
			signsCacheField.setAccessible(true);

			@SuppressWarnings("unchecked")
			final java.util.List<SignResource> signs = (java.util.List<SignResource>) signsField.get(null);
			@SuppressWarnings("unchecked")
			final java.util.Map<String, SignResource> signsCache = (java.util.Map<String, SignResource>) signsCacheField.get(null);

			SignTextStyleConfig.clear();
			ResourceManagerHelper.readAllResources(CUSTOM_SIGNS_ID, inputStream -> {
				try {
					final JsonReader reader = new JsonReader(Config.readResource(inputStream));
					reader.iterateReaderArray("signs", () -> {
					}, signReader -> {
						final SignResource sign = new SignResource(signReader);
						signs.add(sign);
						signsCache.put(sign.signId, sign);
						// 自定义文字样式（MTR 不识别这些键，只由本 mod 使用）
						SignTextStyleConfig.put(sign.signId, new SignTextStyle(
								SignTextStyleConfig.parseColor(signReader.getString("textColor", "")),
								(float) signReader.getDouble("textSize", 1.0),
								signReader.getBoolean("textBold", false)
						));
					});
				} catch (Exception e) {
					Init.LOGGER.error("Failed to load custom railway signs from {}", CUSTOM_SIGNS_ID, e);
				}
			});

			// 加载 JS 指示牌配置
			JSSignLoader.reload();
		} catch (ReflectiveOperationException | RuntimeException e) {
			throw new RuntimeException("Failed to register Nanbin custom railway signs", e);
		}
	}
}