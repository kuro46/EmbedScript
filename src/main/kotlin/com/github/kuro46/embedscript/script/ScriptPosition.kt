package com.github.kuro46.embedscript.script

import org.bukkit.Location
import org.bukkit.block.Block

/**
 * @author shirokuro
 */
class ScriptPosition(val world: String, val x: Int, val y: Int, val z: Int) : Comparable<ScriptPosition> {

    constructor(block: Block) : this(block.world.name, block.x, block.y, block.z)

    constructor(location: Location) : this(location.world.name, location.blockX, location.blockY, location.blockZ)

    override fun compareTo(other: ScriptPosition): Int {
        x.compareTo(other.x).let { if (it != 0) return it }
        y.compareTo(other.y).let { if (it != 0) return it }
        z.compareTo(other.z).let { if (it != 0) return it }
        return world.compareTo(other.world)
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
}
