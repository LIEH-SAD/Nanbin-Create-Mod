package com.Nanbin.mixin;

import com.Nanbin.Init;
import com.Nanbin.client.Drawing.SignTextStyleConfig;
import com.Nanbin.client.JavaScriptSupport.JSSignLoader;
import net.minecraft.util.Identifier;
import org.mtr.core.serializer.JsonReader;
import org.mtr.config.Config;
import org.mtr.resource.ResourceManagerHelper;
import org.mtr.resource.SignResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 给 MTR 原版铁路告示牌（RAILWAY_SIGN）注入更多可选标志，并加载 JS 站牌脚本配置。
 * <p>
 * 挂在 MTR 的 {@code CustomResourceLoader.reload()} 末尾（每次资源重载都会跑），做两件事：
 * <ol>
 *     <li>读 {@code assets/nanbin/signs.json}，把自定义标志塞进 MTR 的 {@code SIGNS} / {@code SIGNS_CACHE}，
 *     并登记自定义文字样式（MTR 不认识那些键，只有本模组用）；</li>
 *     <li>调 {@link JSSignLoader#reload()} 重新载入 JS 站牌脚本清单。</li>
 * </ol>
 * <p>
 * 4.1 适配：{@code org.mtr.mod.client.CustomResourceLoader} → {@code org.mtr.client.CustomResourceLoader}；
 * {@code org.mtr.mod.config.Config} → {@code org.mtr.config.Config}（返回 gson 的 JsonElement）；
 * MTR 4.0 的映射层 {@code ResourceManagerHelper} → {@code org.mtr.resource.ResourceManagerHelper}。
 * <p>
 * 反射取那两个缓存字段时<b>按泛型参数 {@code SignResource} 定位，不写死字段名</b>
 * —— 字段名会随映射变化，写死字符串在生产环境会失效。
 */
@Mixin(value = org.mtr.client.CustomResourceLoader.class, remap = false)
public class CustomResourceLoaderMixin {

	private static final Identifier CUSTOM_SIGNS_ID = Identifier.of(Init.MOD_ID, "signs.json");

	@Inject(method = "reload", at = @At("RETURN"), remap = false)
	private static void nanbin$addCustomSigns(CallbackInfo ci) {
		try {
			final List<SignResource> signs = findSignResourceField(Collection.class);
			final Map<String, SignResource> signsCache = findSignResourceField(Map.class);
			if (signs == null || signsCache == null) {
				Init.LOGGER.error("CustomResourceLoaderMixin: 找不到 MTR 的告示牌缓存字段，自定义标志不会加载");
				return;
			}

			SignTextStyleConfig.clear();
			ResourceManagerHelper.readAllResources(CUSTOM_SIGNS_ID, inputStream -> {
				try {
					final JsonReader reader = new JsonReader(Config.readResource(inputStream));
					reader.iterateReaderArray("signs", () -> {
					}, signReader -> {
						final SignResource sign = new SignResource(signReader);
						signs.add(sign);
						signsCache.put(sign.signId, sign);
						// 自定义文字样式（MTR 不识别这些键，只由本模组使用）
						SignTextStyleConfig.put(sign.signId, new SignTextStyleConfig.SignTextStyle(
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

	/**
	 * 在 MTR 的缓存字段里找「元素类型是 {@link SignResource}」的那个：
	 * {@code Collection} 子类是标志列表，{@code Map} 子类是 id → 标志 的缓存。
	 */
	@SuppressWarnings("unchecked")
	private static <T> T findSignResourceField(Class<?> expectedKind) throws ReflectiveOperationException {
		for (final Field field : org.mtr.client.CustomResourceLoader.class.getDeclaredFields()) {
			if (!Modifier.isStatic(field.getModifiers()) || !expectedKind.isAssignableFrom(field.getType())) {
				continue;
			}
			if (!(field.getGenericType() instanceof ParameterizedType parameterizedType)) {
				continue;
			}
			final Type[] arguments = parameterizedType.getActualTypeArguments();
			// Map 的泛型参数是 <String, SignResource>，Collection 是 <SignResource>
			if (arguments.length > 0 && arguments[arguments.length - 1].getTypeName().equals(SignResource.class.getName())) {
				field.setAccessible(true);
				return (T) field.get(null);
			}
		}
		return null;
	}
}
