package com.Nanbin.client.JavaScriptSupport;

import com.Nanbin.client.Render.RenderCRTRailwaySign;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mod.render.StoredMatrixTransformations;

/**
 * JS 指示牌每帧绘制的上下文（ctx）。
 * 坐标系与世界坐标一致：x ∈ [0, getWidth()]，y ∈ [0, getHeight()]，均为世界单位；
 * getWidth()/getHeight() 是实际渲染宽高（如 RailwaySign 高 0.5、宽 = 格数 × 0.5），
 * 格数与单格尺寸见 getCellCount()/getCellSize()。
 */
public final class JSDrawContext {

	private final StoredMatrixTransformations storedMatrixTransformations;
	private final Direction facing;
	private final float startX;
	private final float startY;
	private final float cellSize;
	private final int cellCount;
	private final int backgroundColor;
	// 非 0 时覆盖 getWidth()：墙挂牌每格宽 1.0 但内容基于 0.5 高度绘制，需要显式总宽。
	private final float widthOverride;

	public JSDrawContext(StoredMatrixTransformations storedMatrixTransformations, Direction facing, float startX, float startY, float cellSize, int cellCount, boolean transparentBlack, int backgroundColor) {
		this(storedMatrixTransformations, facing, startX, startY, cellSize, cellCount, transparentBlack, backgroundColor, 0.0F);
	}

	public JSDrawContext(StoredMatrixTransformations storedMatrixTransformations, Direction facing, float startX, float startY, float cellSize, int cellCount, boolean transparentBlack, int backgroundColor, float widthOverride) {
		this.storedMatrixTransformations = storedMatrixTransformations;
		this.facing = facing;
		this.startX = startX;
		this.startY = startY;
		this.cellSize = cellSize;
		this.cellCount = cellCount;
		this.backgroundColor = backgroundColor;
		this.widthOverride = widthOverride;
	}

	public int getCellCount() {
		return cellCount;
	}

	public float getWidth() {
		return widthOverride > 0.0F ? widthOverride : cellCount * cellSize;
	}

	/** 指示牌渲染高度（世界单位）= 单格尺寸（与 render 中原版渲染高度一致）。 */
	public float getHeight() {
		return cellSize;
	}

	/** 单格的世界尺寸。 */
	public float getCellSize() {
		return cellSize;
	}

	public void drawText(String text, double x, double y, double w, double h, double color, double scale, boolean bold, boolean centered) {
		RenderCRTRailwaySign.renderJSTextInBox(text, storedMatrixTransformations, facing, startX + (float) x, startY + (float) y, (float) w, (float) h, normalizeColor(toArgb(color)), (float) scale, bold, centered);
	}

	public void drawTexture(String identifier, double x, double y, double w, double h, double color) {
		RenderCRTRailwaySign.renderJSTexture(new Identifier(identifier), storedMatrixTransformations, facing, startX + (float) x, startY + (float) y, (float) w, (float) h, toArgb(color));
	}

	public void drawRect(double x, double y, double w, double h, double color) {
		RenderCRTRailwaySign.renderJSRect(storedMatrixTransformations, facing, startX + (float) x, startY + (float) y, (float) w, (float) h, normalizeColor(toArgb(color)));
	}

	public void drawLine(double x1, double y1, double x2, double y2, double thickness, double color) {
		RenderCRTRailwaySign.renderJSLine(storedMatrixTransformations, facing, startX + (float) x1, startY + (float) y1, startX + (float) x2, startY + (float) y2, (float) thickness, normalizeColor(toArgb(color)));
	}

	/**
	 * 把脚本传入的颜色统一解释为 32 位 ARGB。
	 * 脚本里的颜色来自 Java long/int，Nashorn 会转成 double 传进来；直接 (int) 强转会让 ≥ 0x80000000 的 ARGB
	 * 按浮点→整型饱和规则变成 0x7FFFFFFF（颜色发白），故先转 long 再转 int 取回低 32 位。
	 */
	private static int toArgb(double color) {
		return (int) (long) color;
	}

	/** 旧接口兼容：把 execute 返回的结果（单个 {@link JSSignResult} 或数组）逐格绘制到本上下文。 */
	public void drawResults(Object result) {
		final JSSignEngine.JSSignResult[] results = JSSignEngine.toCellResults(result, cellCount);
		for (int i = 0; i < cellCount; i++) {
			final JSSignEngine.JSSignResult cell = results[Math.min(i, results.length - 1)];
			final String text = cell.getText();
			if (text == null || text.isEmpty()) {
				continue;
			}
			final int color = cell.getTextColor() != 0 ? cell.getTextColor() : 0xFF000000;
			final int bg = cell.getBackgroundColor() != 0 ? cell.getBackgroundColor() : backgroundColor;
			if (bg != 0) {
				drawRect(i * cellSize, 0, cellSize, cellSize, bg);
			}
			drawText(text, i * cellSize, 0, cellSize, cellSize, color, cell.getTextSize(), cell.isTextBold(), true);
		}
	}

	/** 若颜色没有 alpha（高位为 0），自动补为不透明。 */
	public static int normalizeColor(int color) {
		return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
	}
}
