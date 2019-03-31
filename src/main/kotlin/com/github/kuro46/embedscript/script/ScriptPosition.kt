package com.github.kuro46.embedscript.script

import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.bukkit.Location
import org.bukkit.block.Block
import java.util.*

/**
 * @author shirokuro
 */
@JsonAdapter(ScriptPosition.Adapter::class)
class ScriptPosition(val world: String, val x: Int, val y: Int, val z: Int) : Comparable<ScriptPosition> {

    constructor(block: Block) : this(block.world.name, block.x, block.y, block.z)

    constructor(location: Location) : this(location.world.name, location.blockX, location.blockY, location.blockZ)

    override fun compareTo(other: ScriptPosition): Int {
        val xCompareTo = Integer.compare(x, other.x)
        if (xCompareTo != 0) {
            return xCompareTo
        }
        val yCompareTo = Integer.compare(y, other.y)
        if (yCompareTo != 0) {
            return yCompareTo
        }
        val zCompareTo = Integer.compare(z, other.z)
        return if (zCompareTo != 0) {
            zCompareTo
        } else world.compareTo(other.world)

    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScriptPosition

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false
        if (world != other.world) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + z
        result = 31 * result + world.hashCode()
        return result
    }

    override fun toString(): String {
        return "ScriptPosition(world='$world', x=$x, y=$y, z=$z)"
    }

    class Adapter : TypeAdapter<ScriptPosition>() {
        override fun write(writer: JsonWriter, value: ScriptPosition) {
            writer.beginObject()
            writer.name("world").value(value.world)
            writer.name("x").value(value.x.toLong())
            writer.name("y").value(value.y.toLong())
            writer.name("z").value(value.z.toLong())
            writer.endObject()
        }

        override fun read(reader: JsonReader): ScriptPosition {
            var world: String? = null
            var x: Int? = null
            var y: Int? = null
            var z: Int? = null

            reader.beginObject()
            while (reader.hasNext()) {
                val nextName = reader.nextName()
                when (nextName) {
                    "world" -> world = reader.nextString()
                    "x" -> x = reader.nextInt()
                    "y" -> y = reader.nextInt()
                    "z" -> z = reader.nextInt()
                    else -> throw JsonParseException("'$nextName' is unknown value!")
                }
            }
            reader.endObject()

            if (world == null || x == null || y == null || z == null) {
                throw JsonParseException("'world' or 'x' or 'z' not exists!")
            }
            return ScriptPosition(world, x, y, z)
        }
    }
}
