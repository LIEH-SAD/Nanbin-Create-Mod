package com.Nanbin.mapping;

import org.mtr.mapping.annotation.MappedMethod;
import org.mtr.mapping.holder.BooleanBiFunction;
import org.mtr.mapping.holder.Box;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.VoxelShape;
import org.mtr.mapping.tool.HolderBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class VoxelShapes extends HolderBase<net.minecraft.world.phys.shapes.Shapes> {
    public VoxelShapes(net.minecraft.world.phys.shapes.Shapes data) {
        super(data);
    }

    @MappedMethod
    public static VoxelShapes cast(HolderBase<?> data) {
        return new VoxelShapes((net.minecraft.world.phys.shapes.Shapes)data.data);
    }

    @MappedMethod
    public static boolean isInstance(@Nullable HolderBase<?> data) {
        return data != null && data.data instanceof net.minecraft.world.phys.shapes.Shapes;
    }

    @MappedMethod
    public boolean equals(@Nullable Object data) {
        return data instanceof HolderBase && ((net.minecraft.world.phys.shapes.Shapes)this.data).equals(((HolderBase)data).data);
    }

    //修改union的组合数量，保证多方块组合
    @Nonnull
    @MappedMethod
    public static VoxelShape union(VoxelShape... shapes) {
        if (shapes.length == 0) {
            return new VoxelShape(net.minecraft.world.phys.shapes.Shapes.empty());
        }
        net.minecraft.world.phys.shapes.VoxelShape result = shapes[0].data;
        for (int i = 1; i < shapes.length; i++) {
            result = net.minecraft.world.phys.shapes.Shapes.or(result, shapes[i].data);
        }
        return new VoxelShape(result);
    }

    @Nonnull
    @MappedMethod
    public static VoxelShape fullCube() {
        return new VoxelShape(net.minecraft.world.phys.shapes.Shapes.block());
    }

    @Nonnull
    @MappedMethod
    public static VoxelShape combineAndSimplify(VoxelShape first, VoxelShape second, BooleanBiFunction function) {
        return new VoxelShape(net.minecraft.world.phys.shapes.Shapes.join((net.minecraft.world.phys.shapes.VoxelShape)first.data, (net.minecraft.world.phys.shapes.VoxelShape)second.data, (net.minecraft.world.phys.shapes.BooleanOp)function.data));
    }

    @MappedMethod
    public static boolean adjacentSidesCoverSquare(VoxelShape one, VoxelShape two, Direction direction) {
        return net.minecraft.world.phys.shapes.Shapes.mergedFaceOccludes((net.minecraft.world.phys.shapes.VoxelShape)one.data, (net.minecraft.world.phys.shapes.VoxelShape)two.data, direction.data);
    }

    @MappedMethod
    public static boolean matchesAnywhere(VoxelShape shape1, VoxelShape shape2, BooleanBiFunction predicate) {
        return net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty((net.minecraft.world.phys.shapes.VoxelShape)shape1.data, (net.minecraft.world.phys.shapes.VoxelShape)shape2.data, (net.minecraft.world.phys.shapes.BooleanOp)predicate.data);
    }

    @MappedMethod
    public static boolean isSideCovered(VoxelShape shape, VoxelShape neighbor, Direction direction) {
        return net.minecraft.world.phys.shapes.Shapes.blockOccudes((net.minecraft.world.phys.shapes.VoxelShape)shape.data, (net.minecraft.world.phys.shapes.VoxelShape)neighbor.data, direction.data);
    }

    @Nonnull
    @MappedMethod
    public static VoxelShape empty() {
        return new VoxelShape(net.minecraft.world.phys.shapes.Shapes.empty());
    }

    @MappedMethod
    public static boolean unionCoversFullCube(VoxelShape one, VoxelShape two) {
        return net.minecraft.world.phys.shapes.Shapes.faceShapeOccludes((net.minecraft.world.phys.shapes.VoxelShape)one.data, (net.minecraft.world.phys.shapes.VoxelShape)two.data);
    }

    @Nonnull
    @MappedMethod
    public static VoxelShape extrudeFace(VoxelShape shape, Direction direction) {
        return new VoxelShape(net.minecraft.world.phys.shapes.Shapes.getFaceShape((net.minecraft.world.phys.shapes.VoxelShape)shape.data, direction.data));
    }

    @Nonnull
    @MappedMethod
    public static VoxelShape combine(VoxelShape one, VoxelShape two, BooleanBiFunction function) {
        return new VoxelShape(net.minecraft.world.phys.shapes.Shapes.joinUnoptimized((net.minecraft.world.phys.shapes.VoxelShape)one.data, (net.minecraft.world.phys.shapes.VoxelShape)two.data, (net.minecraft.world.phys.shapes.BooleanOp)function.data));
    }

    @Nonnull
    @MappedMethod
    public static VoxelShape cuboid(Box box) {
        return new VoxelShape(net.minecraft.world.phys.shapes.Shapes.create((net.minecraft.world.phys.AABB)box.data));
    }

    @Nonnull
    @MappedMethod
    public static VoxelShape cuboid(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return new VoxelShape(net.minecraft.world.phys.shapes.Shapes.box(minX, minY, minZ, maxX, maxY, maxZ));
    }

    @Nonnull
    @MappedMethod
    public static VoxelShape getUnboundedMapped() {
        return new VoxelShape(net.minecraft.world.phys.shapes.Shapes.INFINITY);
    }
}
