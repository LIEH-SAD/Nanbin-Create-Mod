package com.Nanbin.Registry.RegMean;

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
public final class VoxelShapes extends HolderBase<net.minecraft.util.shape.VoxelShapes> {
    public VoxelShapes(net.minecraft.util.shape.VoxelShapes data) {
        super(data);
    }

    @MappedMethod
    public static VoxelShapes cast(HolderBase<?> data) {
        return new VoxelShapes((net.minecraft.util.shape.VoxelShapes)data.data);
    }

    @MappedMethod
    public static boolean isInstance(@Nullable HolderBase<?> data) {
        return data != null && data.data instanceof net.minecraft.util.shape.VoxelShapes;
    }

    @MappedMethod
    public boolean equals(@Nullable Object data) {
        return data instanceof HolderBase && ((net.minecraft.util.shape.VoxelShapes)this.data).equals(((HolderBase)data).data);
    }

    //修改union的组合数量，保证多方块组合
    @Nonnull
    @MappedMethod
    public static VoxelShape union(VoxelShape... shapes) {
        if (shapes.length == 0) {
            return new VoxelShape(net.minecraft.util.shape.VoxelShapes.empty());
        }
        net.minecraft.util.shape.VoxelShape result = shapes[0].data;
        for (int i = 1; i < shapes.length; i++) {
            result = net.minecraft.util.shape.VoxelShapes.union(result, shapes[i].data);
        }
        return new VoxelShape(result);
    }

    @Nonnull
    @MappedMethod
    public static VoxelShape fullCube() {
        return new VoxelShape(net.minecraft.util.shape.VoxelShapes.fullCube());
    }

    @Nonnull
    @MappedMethod
    public static VoxelShape combineAndSimplify(VoxelShape first, VoxelShape second, BooleanBiFunction function) {
        return new VoxelShape(net.minecraft.util.shape.VoxelShapes.combineAndSimplify((net.minecraft.util.shape.VoxelShape)first.data, (net.minecraft.util.shape.VoxelShape)second.data, (net.minecraft.util.function.BooleanBiFunction)function.data));
    }

    @MappedMethod
    public static boolean adjacentSidesCoverSquare(VoxelShape one, VoxelShape two, Direction direction) {
        return net.minecraft.util.shape.VoxelShapes.adjacentSidesCoverSquare((net.minecraft.util.shape.VoxelShape)one.data, (net.minecraft.util.shape.VoxelShape)two.data, direction.data);
    }

    @MappedMethod
    public static boolean matchesAnywhere(VoxelShape shape1, VoxelShape shape2, BooleanBiFunction predicate) {
        return net.minecraft.util.shape.VoxelShapes.matchesAnywhere((net.minecraft.util.shape.VoxelShape)shape1.data, (net.minecraft.util.shape.VoxelShape)shape2.data, (net.minecraft.util.function.BooleanBiFunction)predicate.data);
    }

    @MappedMethod
    public static boolean isSideCovered(VoxelShape shape, VoxelShape neighbor, Direction direction) {
        return net.minecraft.util.shape.VoxelShapes.isSideCovered((net.minecraft.util.shape.VoxelShape)shape.data, (net.minecraft.util.shape.VoxelShape)neighbor.data, direction.data);
    }

    @Nonnull
    @MappedMethod
    public static VoxelShape empty() {
        return new VoxelShape(net.minecraft.util.shape.VoxelShapes.empty());
    }

    @MappedMethod
    public static boolean unionCoversFullCube(VoxelShape one, VoxelShape two) {
        return net.minecraft.util.shape.VoxelShapes.unionCoversFullCube((net.minecraft.util.shape.VoxelShape)one.data, (net.minecraft.util.shape.VoxelShape)two.data);
    }

    @Nonnull
    @MappedMethod
    public static VoxelShape extrudeFace(VoxelShape shape, Direction direction) {
        return new VoxelShape(net.minecraft.util.shape.VoxelShapes.extrudeFace((net.minecraft.util.shape.VoxelShape)shape.data, direction.data));
    }

    @Nonnull
    @MappedMethod
    public static VoxelShape combine(VoxelShape one, VoxelShape two, BooleanBiFunction function) {
        return new VoxelShape(net.minecraft.util.shape.VoxelShapes.combine((net.minecraft.util.shape.VoxelShape)one.data, (net.minecraft.util.shape.VoxelShape)two.data, (net.minecraft.util.function.BooleanBiFunction)function.data));
    }

    @Nonnull
    @MappedMethod
    public static VoxelShape cuboid(Box box) {
        return new VoxelShape(net.minecraft.util.shape.VoxelShapes.cuboid((net.minecraft.util.math.Box)box.data));
    }

    @Nonnull
    @MappedMethod
    public static VoxelShape cuboid(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return new VoxelShape(net.minecraft.util.shape.VoxelShapes.cuboid(minX, minY, minZ, maxX, maxY, maxZ));
    }

    @Nonnull
    @MappedMethod
    public static VoxelShape getUnboundedMapped() {
        return new VoxelShape(net.minecraft.util.shape.VoxelShapes.UNBOUNDED);
    }
}
