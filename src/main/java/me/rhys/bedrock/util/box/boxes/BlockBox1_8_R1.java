package me.rhys.bedrock.util.box.boxes;

import me.rhys.bedrock.util.box.BlockBox;
import me.rhys.bedrock.util.box.BoundingBox;
import me.rhys.bedrock.util.BlockUtil;
import me.rhys.bedrock.util.MathUtil;
import net.minecraft.server.v1_8_R1.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlockBox1_8_R1 implements BlockBox {

    @Override
    public List<BoundingBox> getCollidingBoxes(org.bukkit.World world, BoundingBox box) {
        int minX = MathUtil.floor(box.minX);
        int maxX = MathUtil.floor(box.maxX + 1);
        int minY = MathUtil.floor(box.minY);
        int maxY = MathUtil.floor(box.maxY + 1);
        int minZ = MathUtil.floor(box.minZ);
        int maxZ = MathUtil.floor(box.maxZ + 1);

        List<Location> locations = new ArrayList<>();

        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                for (int y = minY - 1; y < maxY; y++) {
                    Location loc = new Location(world, x, y, z);
                    locations.add(loc);
                }
            }
        }

        List<BoundingBox> boxes = Collections.synchronizedList(new ArrayList<>());

        locations.parallelStream().forEach(loc -> {
            org.bukkit.block.Block block = BlockUtil.getBlock(loc);
            if (block != null && !block.getType().equals(Material.AIR)) {
                if (BlockUtil.collisionBoundingBoxes.containsKey(block.getType())) {
                    BoundingBox box2 = BlockUtil.collisionBoundingBoxes.get(block.getType()).add(block.getLocation().toVector());
                    boxes.add(box2);
                } else {
                    int x = block.getX(), y = block.getY(), z = block.getZ();

                    BlockPosition pos = new BlockPosition(x, y, z);
                    World nmsWorld = ((CraftWorld) world).getHandle();
                    IBlockData nmsiBlockData = ((CraftWorld) world).getHandle().getType(pos);
                    Block nmsBlock = nmsiBlockData.getBlock();
                    List<AxisAlignedBB> preBoxes = new ArrayList<>();

                    nmsBlock.updateShape(nmsWorld, pos);
                    nmsBlock.a(nmsWorld,
                            pos,
                            nmsiBlockData,
                            (AxisAlignedBB) box.toAxisAlignedBB(),
                            preBoxes,
                            null);

                    if (preBoxes.size() > 0) {
                        for (AxisAlignedBB aabb : preBoxes) {
                            BoundingBox bb = new BoundingBox(
                                    (float) aabb.a,
                                    (float) aabb.b,
                                    (float) aabb.c,
                                    (float) aabb.d,
                                    (float) aabb.e,
                                    (float) aabb.f);

                            if (bb.collides(box)) {
                                boxes.add(bb);
                            }
                        }
                    } else {
                        BoundingBox bb = new BoundingBox(
                                (float) nmsBlock.z(),
                                (float) nmsBlock.B(),
                                (float) nmsBlock.D(),
                                (float) nmsBlock.A(),
                                (float) nmsBlock.C(),
                                (float) nmsBlock.E()).add(x, y, z, x, y, z);
                        if (bb.collides(box)) {
                            boxes.add(bb);
                        }
                    }
                }
            }
        });

        return boxes;
    }

    @Override
    public List<BoundingBox> getSpecificBox(Location loc) {
        return Collections.synchronizedList(getCollidingBoxes(loc.getWorld(), new BoundingBox(loc.toVector(), loc.toVector())));
    }

    @Override
    public boolean isChunkLoaded(Location loc) {
        return BlockUtil.isChunkLoaded(loc);
    }

    @Override
    public boolean isRiptiding(LivingEntity entity) {
        return false;
    }

    @Override
    public boolean isUsingItem(Player player) {
        EntityHuman entity = ((org.bukkit.craftbukkit.v1_8_R1.entity.CraftHumanEntity) player).getHandle();
        return entity.bY() != null && entity.bY().getItem().e(entity.bY()) != EnumAnimation.NONE;
    }

    @Override
    public float getMovementFactor(Player player) {
        return (float) ((CraftPlayer) player).getHandle().getAttributeInstance(GenericAttributes.d).getValue();
    }

    @Override
    public int getTrackerId(Player player) {
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        EntityTrackerEntry entry = (EntityTrackerEntry) ((WorldServer) entityPlayer.getWorld()).tracker.trackedEntities.get(entityPlayer.getId());
        return entry.tracker.getId();
    }

    @Override
    public float getAiSpeed(Player player) {
        return ((CraftPlayer) player).getHandle().bH();
    }
}