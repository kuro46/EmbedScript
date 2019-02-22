package com.github.kuro46.embedscript.script;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.io.IOException;
import java.util.Objects;

/**
 * @author shirokuro
 */
@JsonAdapter(ScriptPosition.Adapter.class)
public class ScriptPosition implements Comparable<ScriptPosition> {
    private final String world;
    private final int x, y, z;

    public ScriptPosition(Block block) {
        this(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public ScriptPosition(Location location) {
        this(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public ScriptPosition(String world, int x, int y, int z) {
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
        ScriptPosition that = (ScriptPosition) o;
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
    public int compareTo(ScriptPosition o) {
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

    public static class Adapter extends TypeAdapter<ScriptPosition> {
        @Override
        public void write(JsonWriter out, ScriptPosition value) throws IOException {
            out.beginObject();
            out.name("world").value(value.world);
            out.name("x").value(value.x);
            out.name("y").value(value.y);
            out.name("z").value(value.z);
            out.endObject();
        }

        @Override
        public ScriptPosition read(JsonReader in) throws IOException {
            String world = null;
            Integer x = null;
            Integer y = null;
            Integer z = null;

            in.beginObject();
            while (in.hasNext()) {
                String nextName = in.nextName();
                switch (nextName) {
                    case "world":
                        world = in.nextString();
                        break;
                    case "x":
                        x = in.nextInt();
                        break;
                    case "y":
                        y = in.nextInt();
                        break;
                    case "z":
                        z = in.nextInt();
                        break;
                    default:
                        throw new JsonParseException(String.format("'%s' is unknown value!", nextName));
                }
            }
            in.endObject();

            if (world == null || x == null || y == null || z == null) {
                throw new JsonParseException("'world' or 'x' or 'z' not exists!");
            }
            return new ScriptPosition(world, x, y, z);
        }
    }
}
