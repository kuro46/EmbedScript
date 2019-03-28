package com.github.kuro46.embedscript.script

import com.github.kuro46.embedscript.GsonHolder
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Multimap
import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.bukkit.Material
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import java.io.IOException
import java.util.*

@JsonAdapter(Script.ScriptAdapter::class)
class Script(val author: UUID,
             val moveTypes: ImmutableSet<MoveType>,
             val clickTypes: ImmutableSet<ClickType>,
             val pushTypes: ImmutableSet<PushType>,
             val script: ImmutableListMultimap<String, String>) {

    enum class MoveType {
        ALL,
        GROUND
    }

    enum class ClickType {
        ALL,
        RIGHT,
        LEFT;


        companion object {

            fun getByAction(action: Action): ClickType? {
                return if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                    RIGHT
                } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                    LEFT
                } else {
                    null
                }
            }
        }
    }

    enum class PushType {
        ALL,
        BUTTON,
        PLATE;


        companion object {

            fun getByEvent(event: PlayerInteractEvent): PushType? {
                if (event.action != Action.PHYSICAL) {
                    return null
                }

                val clickedBlockType = event.clickedBlock.type
                return if (clickedBlockType == Material.STONE_BUTTON || clickedBlockType == Material.WOOD_BUTTON) {
                    BUTTON
                } else if (clickedBlockType == Material.GOLD_PLATE ||
                    clickedBlockType == Material.IRON_PLATE ||
                    clickedBlockType == Material.STONE_PLATE ||
                    clickedBlockType == Material.WOOD_PLATE) {
                    PLATE
                } else {
                    null
                }
            }
        }
    }

    class ScriptAdapter : TypeAdapter<Script>() {
        override fun write(out: JsonWriter, value: Script) {
            val gson = GsonHolder.get()
            out.beginObject()
            out.name("author").jsonValue(gson.toJson(value.author))
            out.name("moveTypes").jsonValue(gson.toJson(value.moveTypes))
            out.name("clickTypes").jsonValue(gson.toJson(value.clickTypes))
            out.name("pushTypes").jsonValue(gson.toJson(value.pushTypes))
            out.name("script")
            writeScript(out, value)
            out.endObject()
        }

        private fun writeScript(out: JsonWriter, value: Script) {
            out.beginObject()
            val gson = GsonHolder.get()
            for (key in value.script.keySet()) {
                out.name(key).jsonValue(gson.toJson(value.script.get(key)))
            }
            out.endObject()
        }

        override fun read(reader: JsonReader): Script {
            var author: UUID? = null
            var moveTypes: Set<MoveType>? = null
            var clickTypes: Set<ClickType>? = null
            var pushTypes: Set<PushType>? = null
            var script: Multimap<String, String>? = null

            val gson = GsonHolder.get()
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "author" -> author = gson.fromJson<UUID>(reader, object : TypeToken<UUID>() {

                    }.type)
                    "moveTypes" -> moveTypes = gson.fromJson<Set<MoveType>>(reader, object : TypeToken<Set<MoveType>>() {

                    }.type)
                    "clickTypes" -> clickTypes = gson.fromJson<Set<ClickType>>(reader, object : TypeToken<Set<ClickType>>() {

                    }.type)
                    "pushTypes" -> pushTypes = gson.fromJson<Set<PushType>>(reader, object : TypeToken<Set<PushType>>() {

                    }.type)
                    "script" -> {
                        script = ArrayListMultimap.create()
                        reader.beginObject()
                        while (reader.hasNext()) {
                            script!!.putAll(reader.nextName(), gson.fromJson(reader, object : TypeToken<List<String>>() {
                            }.type))
                        }
                        reader.endObject()
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()

            if (author == null || moveTypes == null || clickTypes == null || pushTypes == null || script == null) {
                throw JsonParseException("")
            }

            return Script(author,
                ImmutableSet.copyOf(moveTypes),
                ImmutableSet.copyOf(clickTypes),
                ImmutableSet.copyOf(pushTypes),
                ImmutableListMultimap.copyOf(script))
        }
    }
}