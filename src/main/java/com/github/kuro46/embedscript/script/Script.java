package com.github.kuro46.embedscript.script;

import com.github.kuro46.embedscript.GsonHolder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@JsonAdapter(Script.ScriptAdapter.class)
public class Script {
    private final UUID author;
    private final ImmutableSet<MoveType> moveTypes;
    private final ImmutableSet<ClickType> clickTypes;
    private final ImmutableSet<PushType> pushTypes;
    private final ImmutableListMultimap<String, String> script;

    public Script(UUID author,
                  ImmutableSet<MoveType> moveTypes,
                  ImmutableSet<ClickType> clickTypes,
                  ImmutableSet<PushType> pushTypes,
                  ImmutableListMultimap<String, String> script) {
        this.author = author;
        this.moveTypes = moveTypes;
        this.clickTypes = clickTypes;
        this.pushTypes = pushTypes;
        this.script = script;
    }

    public UUID getAuthor() {
        return author;
    }

    public ImmutableSet<MoveType> getMoveTypes() {
        return moveTypes;
    }

    public ImmutableSet<ClickType> getClickTypes() {
        return clickTypes;
    }

    public ImmutableSet<PushType> getPushTypes() {
        return pushTypes;
    }

    public ImmutableListMultimap<String, String> getScript() {
        return script;
    }

    public enum MoveType {
        ALL,
        GROUND
    }

    public enum ClickType {
        ALL,
        RIGHT,
        LEFT;

        public static ClickType getByAction(Action action) {
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                return RIGHT;
            } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                return LEFT;
            } else {
                return null;
            }
        }
    }

    public enum PushType {
        ALL,
        BUTTON,
        PLATE;

        public static PushType getByEvent(PlayerInteractEvent event) {
            if (event.getAction() != Action.PHYSICAL) {
                return null;
            }

            Material clickedBlockType = event.getClickedBlock().getType();
            if (clickedBlockType == Material.STONE_BUTTON ||
                clickedBlockType == Material.WOOD_BUTTON) {
                return BUTTON;
            } else if (clickedBlockType == Material.GOLD_PLATE ||
                clickedBlockType == Material.IRON_PLATE ||
                clickedBlockType == Material.STONE_PLATE ||
                clickedBlockType == Material.WOOD_PLATE) {
                return PLATE;
            } else {
                return null;
            }
        }
    }

    public static class ScriptAdapter extends TypeAdapter<Script> {
        @Override
        public void write(JsonWriter out, Script value) throws IOException {
            Gson gson = GsonHolder.get();
            out.beginObject();
            out.name("author").jsonValue(gson.toJson(value.author));
            out.name("moveTypes").jsonValue(gson.toJson(value.moveTypes));
            out.name("clickTypes").jsonValue(gson.toJson(value.clickTypes));
            out.name("pushTypes").jsonValue(gson.toJson(value.pushTypes));
            out.name("script");
            writeScript(out, value);
            out.endObject();
        }

        private void writeScript(JsonWriter out, Script value) throws IOException {
            out.beginObject();
            Gson gson = GsonHolder.get();
            for (String key : value.script.keySet()) {
                out.name(key).jsonValue(gson.toJson(value.script.get(key)));
            }
            out.endObject();
        }

        @Override
        public Script read(JsonReader in) throws IOException {
            UUID author = null;
            Set<MoveType> moveTypes = null;
            Set<ClickType> clickTypes = null;
            Set<PushType> pushTypes = null;
            Multimap<String, String> script = null;

            Gson gson = GsonHolder.get();
            in.beginObject();
            while (in.hasNext()) {
                switch (in.nextName()) {
                    case "author":
                        author = gson.fromJson(in, new TypeToken<UUID>() {
                        }.getType());
                        break;
                    case "moveTypes":
                        moveTypes = gson.fromJson(in, new TypeToken<Set<MoveType>>() {
                        }.getType());
                        break;
                    case "clickTypes":
                        clickTypes = gson.fromJson(in, new TypeToken<Set<ClickType>>() {
                        }.getType());
                        break;
                    case "pushTypes":
                        pushTypes = gson.fromJson(in, new TypeToken<Set<PushType>>() {
                        }.getType());
                        break;
                    case "script":
                        script = ArrayListMultimap.create();
                        in.beginObject();
                        while (in.hasNext()) {
                            script.putAll(in.nextName(), gson.fromJson(in, new TypeToken<List<String>>() {
                            }.getType()));
                        }
                        in.endObject();
                        break;
                    default:
                        in.skipValue();
                }
            }
            in.endObject();

            if (author == null || moveTypes == null || clickTypes == null || pushTypes == null || script == null) {
                throw new JsonParseException("");
            }

            return new Script(author,
                ImmutableSet.copyOf(moveTypes),
                ImmutableSet.copyOf(clickTypes),
                ImmutableSet.copyOf(pushTypes),
                ImmutableListMultimap.copyOf(script));
        }
    }
}
