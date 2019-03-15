package com.github.kuro46.embedscript.script;

import com.github.kuro46.embedscript.GsonHolder;
import com.github.kuro46.embedscript.api.EmbedScriptAPI;
import com.github.kuro46.embedscript.api.PerformListener;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * unmodifiable class
 */
@JsonAdapter(Script.Adapter.class)
public class Script {
    private static final Pattern PLAYER_PATTERN = Pattern.compile("<player>", Pattern.LITERAL);

    private final UUID author;
    private final Set<MoveType> moveTypes;
    private final Set<ClickType> clickTypes;
    private final Set<PushType> pushTypes;
    private final List<String> permissionsToGive;
    private final List<String> permissionsToNeeded;
    private final List<String> permissionsToNotNeeded;
    private final List<ActionType> actionTypes;
    private final List<String> actions;

    public Script(UUID author,
                  MoveType[] moveTypes,
                  ClickType[] clickTypes,
                  PushType[] pushTypes,
                  String[] permissionsToGive,
                  String[] permissionsToNeeded,
                  String[] permissionsToNotNeeded,
                  ActionType[] actionTypes,
                  String[] actions) {
        this.author = author;
        this.moveTypes = unmodifiableEnumSet(moveTypes);
        this.clickTypes = unmodifiableEnumSet(clickTypes);
        this.pushTypes = unmodifiableEnumSet(pushTypes);
        this.permissionsToGive = unmodifiableList(permissionsToGive);
        this.permissionsToNeeded = unmodifiableList(permissionsToNeeded);
        this.permissionsToNotNeeded = unmodifiableList(permissionsToNotNeeded);
        this.actionTypes = unmodifiableList(actionTypes);
        this.actions = unmodifiableList(actions);
    }

    private <E extends Enum<E>> Set<E> unmodifiableEnumSet(E[] elements) {
        return elements.length == 0
            ? Collections.emptySet()
            : Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(elements)));
    }

    private <E> List<E> unmodifiableList(E[] elements) {
        return elements.length == 0
            ? Collections.emptyList()
            : Collections.unmodifiableList(Arrays.asList(elements));
    }

    public UUID getAuthor() {
        return author;
    }

    public Set<MoveType> getMoveTypes() {
        return moveTypes;
    }

    public Set<ClickType> getClickTypes() {
        return clickTypes;
    }

    public Set<PushType> getPushTypes() {
        return pushTypes;
    }

    public List<String> getPermissionsToGive() {
        return permissionsToGive;
    }

    public List<String> getPermissionsToNeeded() {
        return permissionsToNeeded;
    }

    public List<String> getPermissionsToNotNeeded() {
        return permissionsToNotNeeded;
    }

    public List<ActionType> getActionTypes() {
        return actionTypes;
    }

    public List<String> getActions() {
        return actions;
    }

    private boolean hasPermissionOrOP(Player player, String string) {
        return string.equals("op") ? player.isOp() : player.hasPermission(string);
    }

    public void perform(Plugin plugin, Player trigger) {
        for (String permission : permissionsToNeeded) {
            if (!hasPermissionOrOP(trigger, permission)) {
                return;
            }
        }

        for (String permission : permissionsToNotNeeded) {
            if (hasPermissionOrOP(trigger, permission)) {
                return;
            }
        }

        PermissionAttachment attachment = permissionsToGive.isEmpty() ? null : trigger.addAttachment(plugin);
        for (String permission : permissionsToGive) {
            if (trigger.hasPermission(permission)) {
                continue;
            }
            attachment.setPermission(permission, true);
        }

        for (String action : actions) {
            action = PLAYER_PATTERN.matcher(action).replaceAll(trigger.getName());
            for (ActionType actionType : actionTypes) {
                switch (actionType) {
                    case SAY:
                        trigger.sendMessage(action);
                        break;
                    case SAY_RAW:
                        trigger.spigot().sendMessage(ComponentSerializer.parse(action));
                        break;
                    case PLUGIN:
                        PerformListener listener = EmbedScriptAPI.getListener(action);
                        if (listener != null) {
                            listener.onPerformed(trigger);
                        }
                        break;
                    case COMMAND:
                        trigger.performCommand(action);
                        break;
                    case CONSOLE:
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), action);
                        break;
                    case BROADCAST:
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            player.sendMessage(action);
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException(
                            String.format("Cannot perform '%s': '%s' is unsupported type!",
                                action,
                                actionType.name()));
                }
            }
        }

        if (attachment != null) {
            trigger.removeAttachment(attachment);
        }
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

    public enum ActionType {
        COMMAND,
        SAY,
        SAY_RAW,
        BROADCAST,
        PLUGIN,
        CONSOLE
    }

    public static class Adapter extends TypeAdapter<Script> {
        @Override
        public void write(JsonWriter out, Script value) throws IOException {
            out.beginObject();
            Gson gson = GsonHolder.get();
            out.name("author").jsonValue(gson.toJson(value.author));
            out.name("moveTypes").jsonValue(gson.toJson(value.moveTypes));
            out.name("clickTypes").jsonValue(gson.toJson(value.clickTypes));
            out.name("pushTypes").jsonValue(gson.toJson(value.pushTypes));
            out.name("permissionsToGive").jsonValue(gson.toJson(value.permissionsToGive));
            out.name("permissionsToNeeded").jsonValue(gson.toJson(value.permissionsToNeeded));
            out.name("permissionsToNotNeeded").jsonValue(gson.toJson(value.permissionsToNotNeeded));
            out.name("actionTypes").jsonValue(gson.toJson(value.actionTypes));
            out.name("actions").jsonValue(gson.toJson(value.actions));
            out.endObject();
        }

        @Override
        public Script read(JsonReader in) throws IOException {
            UUID author = null;
            MoveType[] moveTypes = null;
            ClickType[] clickTypes = null;
            PushType[] pushTypes = null;
            String[] permissionsToGive = null;
            String[] permissionsToNeeded = null;
            String[] permissionsToNotNeeded = null;
            ActionType[] actionTypes = null;
            String[] actions = null;

            in.beginObject();
            Gson gson = GsonHolder.get();
            Type stringArrayType = new TypeToken<String[]>() {
            }.getType();
            while (in.hasNext()) {
                String nextName = in.nextName();
                switch (nextName) {
                    case "author":
                        author = gson.fromJson(in, TypeToken.get(UUID.class).getType());
                        break;
                    case "moveTypes":
                        moveTypes = gson.fromJson(in, new TypeToken<MoveType[]>() {
                        }.getType());
                        break;
                    case "clickTypes":
                        clickTypes = gson.fromJson(in, new TypeToken<ClickType[]>() {
                        }.getType());
                        break;
                    case "pushTypes":
                        pushTypes = gson.fromJson(in, new TypeToken<PushType[]>() {
                        }.getType());
                        break;
                    case "permissionsToGive":
                        permissionsToGive = gson.fromJson(in, stringArrayType);
                        break;
                    case "permissionsToNeeded":
                        permissionsToNeeded = gson.fromJson(in, stringArrayType);
                        break;
                    case "permissionsToNotNeeded":
                        permissionsToNotNeeded = gson.fromJson(in, stringArrayType);
                        break;
                    case "actionTypes":
                        actionTypes = gson.fromJson(in, new TypeToken<ActionType[]>() {
                        }.getType());
                        break;
                    case "actions":
                        actions = gson.fromJson(in, stringArrayType);
                        break;
                    default:
                        throw new JsonParseException(String.format("'%s' is unknown value!", nextName));
                }
            }
            in.endObject();

            if (moveTypes == null
                || clickTypes == null
                || pushTypes == null
                || permissionsToGive == null
                || permissionsToNeeded == null
                || permissionsToNotNeeded == null
                || actionTypes == null
                || actions == null) {
                throw new JsonParseException("'moveTypes' or" +
                    " 'clickTypes' or" +
                    " 'pushTypes' or" +
                    " 'permissionsToGive' or" +
                    " 'permissionsToNeeded' or" +
                    " 'permissionsToNotNeeded' or" +
                    " 'actionTypes' or" +
                    " 'actions' not exists!");
            }
            return new Script(author,
                moveTypes,
                clickTypes,
                pushTypes,
                permissionsToGive,
                permissionsToNeeded,
                permissionsToNotNeeded,
                actionTypes,
                actions);
        }
    }

}
