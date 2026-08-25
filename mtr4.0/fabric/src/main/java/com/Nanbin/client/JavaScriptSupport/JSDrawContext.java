package com.Nanbin.client.JavaScriptSupport;

import com.Nanbin.client.Render.RenderCRTRailwaySign;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mod.render.StoredMatrixTransformations;

/**
 * JS 指示牌每帧绘制的上下文（ctx）。
 * <p>
 * 坐标系与世界坐标一致：x 在 [0, getWidth()]，y 在 [0, getHeight()]，
 * getWidth()/getHeight() 返回指示牌实际渲染的宽高（世界单位），
 * 如 RailwaySign 高 0.5、宽 格数 × 0.5；格数与单格尺寸可用 getCellCount()/getCellSize() 获取。
 */
public final class JSDrawContext {

	private final StoredMatrixTransformations storedMatrixTransformations;
	private final Direction facing;
	private final float startX;
	private final float startY;
	private final float cellSize;
	private final int cellCount;
	private final int backgroundColor;

	public JSDrawContext(StoredMatrixTransformations storedMatrixTransformations, Direction facing, float startX, float startY, float cellSize, int cellCount, boolean transparentBlack, int backgroundColor) {
		this.storedMatrixTransformations = storedMatrixTransformations;
		this.facing = facing;
		this.startX = startX;
		this.startY = startY;
		this.cellSize = cellSize;
		this.cellCount = cellCount;
		this.backgroundColor = backgroundColor;
	}

	/** 格数（整块指示牌的格子数量）。 */
	public int getCellCount() {
		return cellCount;
	}

	/** 指示牌渲染宽度（世界单位）= 格数 × 单格尺寸。 */
	public float getWidth() {
		return cellCount * cellSize;
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
		RenderCRTRailwaySign.renderJSTextInBox(text, storedMatrixTransformations, facing, startX + (float) x, startY + (float) y, (float) w, (float) h, normalizeColor((int) color), (float) scale, bold, centered);
	}

	public void drawTexture(String identifier, double x, double y, double w, double h, double color) {
		RenderCRTRailwaySign.renderJSTexture(new Identifier(identifier), storedMatrixTransformations, facing, startX + (float) x, startY + (float) y, (float) w, (float) h, (int) color);
	}

	public void drawRect(double x, double y, double w, double h, double color) {
		RenderCRTRailwaySign.renderJSRect(storedMatrixTransformations, facing, startX + (float) x, startY + (float) y, (float) w, (float) h, normalizeColor((int) color));
	}

	public void drawLine(double x1, double y1, double x2, double y2, double thickness, double color) {
		RenderCRTRailwaySign.renderJSLine(storedMatrixTransformations, facing, startX + (float) x1, startY + (float) y1, startX + (float) x2, startY + (float) y2, (float) thickness, normalizeColor((int) color));
	}

	/**
	 * 旧接口兼容：把 execute 返回的结果（单个 {@link JSSignResult} 或数组）逐格绘制到本上下文。
	 */
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
