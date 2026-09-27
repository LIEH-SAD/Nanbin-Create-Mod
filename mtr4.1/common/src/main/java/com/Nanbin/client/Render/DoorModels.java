package com.Nanbin.client.Render;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 方块实体渲染用的「单个立方体模型」工具。
 * <p>
 * 为什么需要自己写：MTR 4.0 有一套模型包装层
 * （{@code org.mtr.mapping.mapper.EntityModelExtension} / {@code ModelPartExtension}），
 * 4.0 的屏蔽门渲染器就是建在它上面的；<b>MTR 4.1 把这层整个删掉了</b>
 * （jar 里已经不存在这些类），改成直接用原版 {@code ModelPart}。
 * 所以移植 CRT 屏蔽门渲染器时，原来那些嵌套模型类必须重写。
 * <p>
 * 这里的三个方法照搬 MTR 4.1 自己 {@code org.mtr.render.RenderPSDAPGDoor} 里的私有静态工具
 * （那边是 Mojang 映射，这里换成了 Yarn 名）：
 * <ul>
 *     <li>{@code MeshDefinition} → {@link ModelData}，{@code PartDefinition} → {@link ModelPartData}</li>
 *     <li>{@code CubeListBuilder} → {@link ModelPartBuilder}（{@code texOffs} → {@code uv}，{@code addBox} → {@code cuboid}）</li>
 *     <li>{@code PartPose.offsetAndRotation} → {@link ModelTransform#of}</li>
 *     <li>{@code CubeDeformation} → {@link Dilation}</li>
 *     <li>{@code LayerDefinition.create(...).bakeRoot()} → {@link TexturedModelData#of} → {@link TexturedModelData#createModel()}</li>
 * </ul>
 * 画的时候（跟 MTR 一样）在 {@code MainRenderer.scheduleRender} 回调里调
 * {@code model.render(matrixStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY)}。
 */
public final class DoorModels {

	private static final AtomicInteger PART_COUNTER = new AtomicInteger();

	private DoorModels() {
	}

	/** 单个立方体：以方块中心为原点的像素坐标（x/z 相对中心 −8，y 相对方块底面 −16 再往上）。 */
	public static ModelPart createSingleCube(int textureWidth, int textureHeight, int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
		return createModelPart(root -> createModelPartData(root, 0, 0, x - 8, y - 16, z - 8, sizeX, sizeY, sizeZ, 0, 0, 0, 0, 0, 0, 0), textureWidth, textureHeight);
	}

	public static ModelPart createModelPart(Consumer<ModelPartData> builder, int textureWidth, int textureHeight) {
		final ModelData modelData = new ModelData();
		builder.accept(modelData.getRoot());
		return TexturedModelData.of(modelData, textureWidth, textureHeight).createModel();
	}

	/**
	 * 在父节点下挂一个带纹理偏移、可选位移/旋转的立方体。
	 *
	 * @param offsetX/Y/Z 立方体相对轴心的偏移（像素）
	 * @param dilation    外扩量（比原模型大一圈用）
	 * @param pivotX/Y/Z  轴心
	 * @param rotateX/Y/Z 轴心旋转（弧度）
	 */
	public static void createModelPartData(ModelPartData parent, int textureX, int textureY, float offsetX, float offsetY, float offsetZ, float sizeX, float sizeY, float sizeZ, float dilation, float pivotX, float pivotY, float pivotZ, float rotateX, float rotateY, float rotateZ) {
		parent.addChild(
				"nanbin_part_" + PART_COUNTER.incrementAndGet(),
				ModelPartBuilder.create().uv(textureX, textureY).cuboid(offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ, new Dilation(dilation)),
				ModelTransform.of(pivotX, pivotY, pivotZ, rotateX, rotateY, rotateZ)
		);
	}
}
