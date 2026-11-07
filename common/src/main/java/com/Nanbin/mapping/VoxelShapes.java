package com.Nanbin.mapping;

import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

/**
 * MTR 4.0 {@code org.mtr.mapping.holder.VoxelShapes} 的等价物。
 *
 * <p>MTR 4.0 把原版 {@code net.minecraft.util.shape.VoxelShapes} 包了一层 holder；MTR 3.6.3 直接用
 * 原版静态方法。为了不改写移植文件的调用形状，这里提供同名静态方法的薄封装（参数和返回值都是原版类型）。
 * 与 MTR 4.0 的区别仅在于：{@link BooleanBiFunction} 直接用原版枚举常量（{@link BooleanBiFunction#OR} 等），
 * 不再有 {@code BooleanBiFunction.getOrMapped()}。</p>
 */
public final class VoxelShapes {

	private VoxelShapes() {
	}

	/** 原版 {@code VoxelShapes.union} 只接受两个形状；这里把任意个形状依次合并（0 个返回空形状）。 */
	public static VoxelShape union(VoxelShape... shapes) {
		if (shapes.length == 0) {
			return net.minecraft.util.shape.VoxelShapes.empty();
		}
		VoxelShape result = shapes[0];
		for (int i = 1; i < shapes.length; i++) {
			result = net.minecraft.util.shape.VoxelShapes.union(result, shapes[i]);
		}
		return result;
	}

	public static VoxelShape fullCube() {
		return net.minecraft.util.shape.VoxelShapes.fullCube();
	}

	public static VoxelShape combineAndSimplify(VoxelShape first, VoxelShape second, BooleanBiFunction function) {
		return net.minecraft.util.shape.VoxelShapes.combineAndSimplify(first, second, function);
	}

	public static boolean adjacentSidesCoverSquare(VoxelShape one, VoxelShape two, Direction direction) {
		return net.minecraft.util.shape.VoxelShapes.adjacentSidesCoverSquare(one, two, direction);
	}

	public static boolean matchesAnywhere(VoxelShape shape1, VoxelShape shape2, BooleanBiFunction predicate) {
		return net.minecraft.util.shape.VoxelShapes.matchesAnywhere(shape1, shape2, predicate);
	}

	public static boolean isSideCovered(VoxelShape shape, VoxelShape neighbor, Direction direction) {
		return net.minecraft.util.shape.VoxelShapes.isSideCovered(shape, neighbor, direction);
	}

	public static VoxelShape empty() {
		return net.minecraft.util.shape.VoxelShapes.empty();
	}

	public static boolean unionCoversFullCube(VoxelShape one, VoxelShape two) {
		return net.minecraft.util.shape.VoxelShapes.unionCoversFullCube(one, two);
	}

	public static VoxelShape extrudeFace(VoxelShape shape, Direction direction) {
		return net.minecraft.util.shape.VoxelShapes.extrudeFace(shape, direction);
	}

	public static VoxelShape combine(VoxelShape one, VoxelShape two, BooleanBiFunction function) {
		return net.minecraft.util.shape.VoxelShapes.combine(one, two, function);
	}

	public static VoxelShape cuboid(Box box) {
		return net.minecraft.util.shape.VoxelShapes.cuboid(box);
	}

	public static VoxelShape cuboid(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		return net.minecraft.util.shape.VoxelShapes.cuboid(minX, minY, minZ, maxX, maxY, maxZ);
	}

	/** MTR 4.0 的 {@code getUnboundedMapped()}；原版是常量 {@code VoxelShapes.UNBOUNDED}。 */
	public static VoxelShape getUnbounded() {
		return net.minecraft.util.shape.VoxelShapes.UNBOUNDED;
	}
}
