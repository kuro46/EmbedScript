package shirokuro.embedscript.script;

import com.google.gson.annotations.JsonAdapter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import shirokuro.embedscript.script.adapters.ScriptBlockAdapter;

import java.util.Objects;

/**
 * @author shirokuro
 */
@JsonAdapter(ScriptBlockAdapter.class)
public class ScriptBlock {
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
    public String toString() {
        return "ScriptBlock{" +
            "world=" + world +
            ", x=" + x +
            ", y=" + y +
            ", z=" + z +
            '}';
    }
}
