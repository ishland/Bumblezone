package com.telepathicgrunt.the_bumblezone.utils;


import com.telepathicgrunt.the_bumblezone.Bumblezone;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class BoxOctree {

    private static final int subdivideThreshold = 10;
    private static final int maximumDepth = 3;

    private final AABB boundary;
    private final Vec3i size;
    private final int depth;
    private final List<AABB> innerBoxes = new ArrayList<>();
    private final List<BoxOctree> childrenOctants = new ArrayList<>();

    public BoxOctree(AABB axisAlignedBB) {
        this(axisAlignedBB, 0);
    }

    private BoxOctree(AABB axisAlignedBB, int parentDepth) {
        boundary = axisAlignedBB.move(0, 0, 0); // deep copy
        size = new Vec3i(roundAwayFromZero(boundary.getXsize()), roundAwayFromZero(boundary.getYsize()), roundAwayFromZero(boundary.getZsize()));
        depth = parentDepth + 1;
    }

    private int roundAwayFromZero(double value) {
        return (value >= 0) ? (int)Math.ceil(value) : (int)Math.floor(value);
    }

    private void subdivide() {
        if(!childrenOctants.isEmpty()) {
            throw new UnsupportedOperationException(Bumblezone.MODID + " - Tried to subdivide when there are already children octants.");
        }

        int halfXSize = size.getX()/2;
        int halfYSize = size.getY()/2;
        int halfZSize = size.getZ()/2;

        // Lower Left Back Corner
        childrenOctants.add(new BoxOctree(new AABB(
                boundary.minX, boundary.minY, boundary.minZ,
                boundary.minX + halfXSize, boundary.minY + halfYSize, boundary.minZ + halfZSize),
                depth));

        // Lower Left Front Corner
        childrenOctants.add(new BoxOctree(new AABB(
                boundary.minX, boundary.minY, boundary.minZ + halfZSize,
                boundary.minX + halfXSize, boundary.minY + halfYSize, boundary.maxZ),
                depth));

        // Lower Right Back Corner
        childrenOctants.add(new BoxOctree(new AABB(
                boundary.minX + halfXSize, boundary.minY, boundary.minZ,
                boundary.maxX, boundary.minY + halfYSize, boundary.minZ + halfZSize),
                depth));

        // Lower Right Front Corner
        childrenOctants.add(new BoxOctree(new AABB(
                boundary.minX + halfXSize, boundary.minY, boundary.minZ + halfZSize,
                boundary.maxX, boundary.minY + halfYSize, boundary.maxZ),
                depth));

        // Upper Left Back Corner
        childrenOctants.add(new BoxOctree(new AABB(
                boundary.minX, boundary.minY + halfYSize, boundary.minZ,
                boundary.minX + halfXSize, boundary.maxY, boundary.minZ + halfZSize),
                depth));

        // Upper Left Front Corner
        childrenOctants.add(new BoxOctree(new AABB(
                boundary.minX, boundary.minY + halfYSize, boundary.minZ + halfZSize,
                boundary.minX + halfXSize, boundary.maxY, boundary.maxZ),
                depth));

        // Upper Right Back Corner
        childrenOctants.add(new BoxOctree(new AABB(
                boundary.minX + halfXSize, boundary.minY + halfYSize, boundary.minZ,
                boundary.maxX, boundary.maxY, boundary.minZ + halfZSize),
                depth));

        // Upper Right Front Corner
        childrenOctants.add(new BoxOctree(new AABB(
                boundary.minX + halfXSize, boundary.minY + halfYSize, boundary.minZ + halfZSize,
                boundary.maxX, boundary.maxY, boundary.maxZ),
                depth));

        for (AABB parentInnerBox : innerBoxes) {
            for (BoxOctree octree : childrenOctants) {
                if (octree.boundaryIntersects(parentInnerBox)) {
                    octree.addBox(parentInnerBox);
                }
            }
        }

        innerBoxes.clear();
    }

    public void addBox(AABB axisAlignedBB) {
        if (depth < maximumDepth && innerBoxes.size() > subdivideThreshold) {
            subdivide();
        }

        if (!childrenOctants.isEmpty()) {
            for (BoxOctree octree : childrenOctants) {
                if (octree.boundaryIntersects(axisAlignedBB)) {
                    octree.addBox(axisAlignedBB);
                }
            }
        }
        else {
            // Prevent re-adding the same box if it already exists
            for (AABB parentInnerBox : innerBoxes) {
                if (parentInnerBox.equals(axisAlignedBB)) {
                    return;
                }
            }

            innerBoxes.add(axisAlignedBB);
        }
    }

    public boolean boundaryEntirelyContains(AABB axisAlignedBB) {
        return boundary.contains(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ) &&
                boundary.contains(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ);
    }

    public boolean boundaryIntersects(AABB axisAlignedBB) {
        return boundary.intersects(axisAlignedBB);
    }

    public boolean withinBoundsButNotIntersectingChildren(AABB axisAlignedBB) {
        return this.boundaryEntirelyContains(axisAlignedBB) && !this.intersectsAnyBox(axisAlignedBB);
    }

    public boolean intersectsAnyBox(AABB axisAlignedBB) {
        if (!childrenOctants.isEmpty()) {
            for (BoxOctree octree : childrenOctants) {
                if (octree.boundaryIntersects(axisAlignedBB) && octree.intersectsAnyBox(axisAlignedBB)) {
                    return true;
                }
            }
        }
        else {
            for (AABB innerBox : innerBoxes) {
                if (innerBox.intersects(axisAlignedBB)) {
                    return true;
                }
            }
        }

        return false;
    }
}
