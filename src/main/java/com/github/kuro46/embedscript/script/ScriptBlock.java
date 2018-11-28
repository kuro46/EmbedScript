package com.github.kuro46.embedscript.script;

import com.github.kuro46.embedscript.script.adapters.ScriptBlockAdapter;
import com.google.gson.annotations.JsonAdapter;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.Objects;

/**
 * @author shirokuro
 */
@JsonAdapter(ScriptBlockAdapter.class)
public class ScriptBlock implements Comparable<ScriptBlock> {
    private final String world;
    private final int x, y, z;

    public ScriptBlock(Block block) {
        this(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public ScriptBlock(Location location) {
        this(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public ScriptBlock(String world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScriptBlock that = (ScriptBlock) o;
        return x == that.x &&
            y == that.y &&
            z == that.z &&
            Objects.equals(world, that.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, y, z);
    }

    @Override
    public int compareTo(ScriptBlock o) {
        int xCompareTo = Integer.compare(x, o.x);
        if (xCompareTo != 0) {
            return xCompareTo;
        }
        int yCompareTo = Integer.compare(y, o.y);
        if (yCompareTo != 0) {
            return yCompareTo;
        }
        int zCompareTo = Integer.compare(z, o.z);
        if (zCompareTo != 0) {
            return zCompareTo;
        }
        int worldCompareTo = world.compareTo(o.world);
        if (worldCompareTo != 0) {
            return worldCompareTo;
        }
        return 0;
    }

    @Override
    public String toString() {
        return "ScriptBlock{" +
            "world=" + world +
            ", x=" + x +
            ", y=" + y +
            ", z=" + z +
            '}';
    }
}
