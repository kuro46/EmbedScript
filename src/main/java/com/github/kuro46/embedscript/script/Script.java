package com.github.kuro46.embedscript.script;

import com.github.kuro46.embedscript.api.EmbedScriptAPI;
import com.github.kuro46.embedscript.api.PerformListener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * /es
 * @listen-move(lm) all|ground
 * @listen-click(lc) all|right|left
 * @listen-push(lp) all|button|plate
 * @give-permission(gp) permission
 * @enough-permission(ep) permission|op
 * @not-enough-permission(nep) permission|op
 * @action-type(at) command|say|plugin|console
 * @action(a) string
 *
 * able to set multiple value: "@key [value,value1]"
 *
 * "\@" is replace to "@"
 * "\[" is replace to "["
 * "\]" is replace to "]"
 * "\," is replace to ","
 */

/**
 * unmodifiable class
 */
public class Script {
    private static final Pattern PLAYER_PATTERN = Pattern.compile("<player>", Pattern.LITERAL);
    private static final String PATTERN_AT = "[^\\\\]??@";
    private static final String PATTERN_LEFT_SQUARE_BRACKET = "[^\\\\]??\\[";
    private static final String PATTERN_RIGHT_SQUARE_BRACKET = "[^\\\\]??]";
    private static final String PATTERN_COMMA = "[^\\\\]??,";

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

    private <E extends Enum<E>> Set<E> unmodifiableEnumSet(E[] elements){
        return elements.length == 0
            ? Collections.emptySet()
            : Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(elements)));
    }

    private <E> List<E> unmodifiableList(E[] elements){
        return elements.length == 0
            ? Collections.emptyList()
            : Collections.unmodifiableList(Arrays.asList(elements));
    }

    public static Script parse(UUID author,String string) throws ParseException {
        Pattern leftSquareBracketStartsWithPattern = Pattern.compile('^' + PATTERN_LEFT_SQUARE_BRACKET);
        Pattern rightSquareBracketEndsWithPattern = Pattern.compile(PATTERN_RIGHT_SQUARE_BRACKET + '$');
        Pattern atPattern = Pattern.compile(PATTERN_AT);
        Pattern spacePattern = Pattern.compile(" ");

        List<MoveType> moveTypes = new ArrayList<>(1);
        List<ClickType> clickTypes = new ArrayList<>(1);
        List<PushType> pushTypes = new ArrayList<>(1);
        List<String> permissionsToGive = new ArrayList<>(1);
        List<String> permissionsToNeeded = new ArrayList<>(1);
        List<String> permissionsToNotNeeded = new ArrayList<>(1);
        List<ActionType> actionTypes = new ArrayList<>(1);
        List<String> actions = new ArrayList<>(1);

        String[] split = atPattern.split(string);
        for (String s : split) {
            if (s.isEmpty()){
                continue;
            }
            s = spacePattern.matcher(s).replaceFirst(" @");
            String[] keyValue = atPattern.split(s);
            String key = keyValue[0];
            String v = keyValue[1];

            List<String> values = new ArrayList<>(1);
            if (leftSquareBracketStartsWithPattern.matcher(v).find() &&
                rightSquareBracketEndsWithPattern.matcher(v).find()) {
                // trim "[" and "]"
                v = v.substring(1, v.length() - 1);
                Collections.addAll(values, Pattern.compile(PATTERN_COMMA).split(v));
            } else {
                values.add(v);
            }

            ValueConsumer<String> valueConsumer;

            switch (key) {
                case "listen-move":
                case "lm":
                    moveTypes.clear();
                    valueConsumer = value -> {
                        try {
                            moveTypes.add(MoveType.valueOf(value.toUpperCase(Locale.ENGLISH)));
                        } catch (IllegalArgumentException e) {
                            throw new ParseException(String.format("'%s' is unknown value. (key: '%s')", value, key), e);
                        }
                    };
                    break;
                case "listen-click":
                case "lc":
                    clickTypes.clear();
                    valueConsumer = value -> {
                        try {
                            clickTypes.add(ClickType.valueOf(value.toUpperCase(Locale.ENGLISH)));
                        } catch (IllegalArgumentException e) {
                            throw new ParseException(String.format("'%s' is unknown value. (key: '%s')", value, key), e);
                        }
                    };
                    break;
                case "listen-push":
                case "lp":
                    pushTypes.clear();
                    valueConsumer = value -> {
                        try {
                            pushTypes.add(PushType.valueOf(value.toUpperCase(Locale.ENGLISH)));
                        } catch (IllegalArgumentException e) {
                            throw new ParseException(String.format("'%s' is unknown value. (key: '%s')", value, key), e);
                        }
                    };
                    break;
                case "give-permission":
                case "gp":
                    pushTypes.clear();
                    valueConsumer = permissionsToGive::add;
                    break;
                case "enough-permission":
                case "ep":
                    permissionsToNeeded.clear();
                    valueConsumer = permissionsToNeeded::add;
                    break;
                case "not-enough-permission":
                case "nep":
                    permissionsToNotNeeded.clear();
                    valueConsumer = permissionsToNotNeeded::add;
                    break;
                case "action-type":
                case "at":
                    actionTypes.clear();
                    valueConsumer = value -> {
                        try {
                            actionTypes.add(ActionType.valueOf(value.toUpperCase(Locale.ENGLISH)));
                        } catch (IllegalArgumentException e) {
                            throw new ParseException(String.format("'%s' is unknown value. (key: '%s')", value, key), e);
                        }
                    };
                    break;
                case "action":
                case "a":
                    actions.clear();
                    valueConsumer = actions::add;
                    break;
                default:
                    throw new ParseException(String.format("'%s' is unknown key.", key));
            }

            for (String value : values) {
                valueConsumer.accept(value);
            }
        }

        return new Script(author,
            moveTypes.toArray(new MoveType[]{}),
            clickTypes.toArray(new ClickType[]{}),
            pushTypes.toArray(new PushType[]{}),
            permissionsToGive.toArray(new String[]{}),
            permissionsToNeeded.toArray(new String[]{}),
            permissionsToNotNeeded.toArray(new String[]{}),
            actionTypes.toArray(new ActionType[]{}),
            actions.toArray(new String[]{}));
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

    private boolean hasPermissionOrOP(Player player,String string){
        return string.equals("op") ? player.isOp() : player.hasPermission(string);
    }

    public void perform(Plugin plugin, Player trigger) {
        for (String permission : permissionsToNeeded) {
            if (!hasPermissionOrOP(trigger,permission)) {
                return;
            }
        }

        for (String permission : permissionsToNotNeeded) {
            if (hasPermissionOrOP(trigger,permission)) {
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
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),action);
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

        public static ClickType getByAction(Action action){
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK){
                return RIGHT;
            }else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK){
                return LEFT;
            }else {
                return null;
            }
        }
    }

    public enum PushType {
        ALL,
        BUTTON,
        PLATE;

        public static PushType getByEvent(PlayerInteractEvent event){
            if (event.getAction() != Action.PHYSICAL){
                return null;
            }

            Material clickedBlockType = event.getClickedBlock().getType();
            if (clickedBlockType == Material.STONE_BUTTON ||
                clickedBlockType == Material.WOOD_BUTTON){
                return BUTTON;
            }else if (clickedBlockType == Material.GOLD_PLATE ||
                clickedBlockType == Material.IRON_PLATE ||
                clickedBlockType == Material.STONE_PLATE ||
                clickedBlockType == Material.WOOD_PLATE){
                return PLATE;
            }else {
                return null;
            }
        }
    }

    public enum ActionType {
        COMMAND,
        SAY,
        PLUGIN,
        CONSOLE
    }

    @FunctionalInterface
    private interface ValueConsumer<V> {
        void accept(V value) throws ParseException;
    }
}
