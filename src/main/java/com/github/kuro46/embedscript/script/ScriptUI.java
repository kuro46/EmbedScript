package com.github.kuro46.embedscript.script;

import com.github.kuro46.embedscript.Prefix;
import com.github.kuro46.embedscript.util.MojangUtil;
import com.github.kuro46.embedscript.util.Scheduler;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * @author shirokuro
 */
public class ScriptUI {
    private final ScriptManager scriptManager;

    public ScriptUI(ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
    }

    public void embed(CommandSender sender,
                      ScriptPosition position,
                      Script script) {
        Objects.requireNonNull(script);

        if (!scriptManager.get(position).isEmpty()) {
            sender.sendMessage(Prefix.ERROR_PREFIX + "Script already exists in that place.");
            return;
        }

        scriptManager.put(position, script);

        sender.sendMessage(Prefix.SUCCESS_PREFIX + "Script was successfully embedded.");
    }

    public void add(CommandSender sender,
                    ScriptPosition position,
                    Script script) {
        Objects.requireNonNull(script);

        if (scriptManager.get(position).isEmpty()) {
            sender.sendMessage(Prefix.ERROR_PREFIX + "Script not exists in that place.");
            return;
        }
        scriptManager.put(position, script);

        sender.sendMessage(Prefix.SUCCESS_PREFIX + "Script was successfully added.");
    }

    public void remove(CommandSender sender, ScriptPosition position) {
        if (scriptManager.remove(position) == null) {
            sender.sendMessage(Prefix.ERROR_PREFIX + "Script not exists in that place.");
            return;
        }

        sender.sendMessage(Prefix.SUCCESS_PREFIX + "Script was successfully removed.");
    }

    public void view(CommandSender sender, ScriptPosition position) {
        List<Script> scripts = scriptManager.get(position);
        if (scripts.isEmpty()) {
            sender.sendMessage(Prefix.ERROR_PREFIX + "Script not exists in that place.");
            return;
        }
        Scheduler.execute(() -> {
            sender.sendMessage("Script information: ------------------------------");
            for (Script script : scripts) {
                sender.sendMessage("===============================================");
                UUID author = script.getAuthor();
                Player player = Bukkit.getPlayer(author);
                String stringAuthor = player == null
                    ? MojangUtil.getName(author)
                    : player.getName();
                sender.sendMessage("author " + stringAuthor);
                sender.sendMessage("@listen-move " + collectionToString(script.getMoveTypes()));
                sender.sendMessage("@listen-click " + collectionToString(script.getClickTypes()));
                sender.sendMessage("@listen-push " + collectionToString(script.getPushTypes()));
                sender.sendMessage("@give-permission " + collectionToString(script.getPermissionsToGive()));
                sender.sendMessage("@enough-permission " + collectionToString(script.getPermissionsToNeeded()));
                sender.sendMessage("@not-enough-permission " +collectionToString(script.getPermissionsToNotNeeded()));
                sender.sendMessage("@action-type " + collectionToString(script.getActionTypes()));
                sender.sendMessage("@action " + collectionToString(script.getActions()));
                sender.sendMessage("===============================================");
            }
            sender.sendMessage("Script information: ------------------------------");
        });
    }

    private String collectionToString(Collection<?> collection){
        if (collection.isEmpty()){
            return "NONE";
        }else if (collection.size() == 1){
            return collection.iterator().next().toString();
        }else {
            return collection.stream()
                .map(Object::toString)
                .collect(Collectors.joining(",","[","]"));
        }
    }

    /**
     * Send list of scripts to player
     *
     * @param player Player
     * @param world  World (Nullable)
     */
    public void list(Player player, String world, Script filter) {
        Map<ScriptPosition, List<Script>> snapshot = scriptManager.snapshot();
        Scheduler.execute(() -> {
            BaseComponent[] prefixComponent = TextComponent.fromLegacyText(Prefix.PREFIX);
            int printCount = 0;
            for (Map.Entry<ScriptPosition, List<Script>> entry : snapshot.entrySet()) {
                ScriptPosition position = entry.getKey();
                List<Script> scripts = entry.getValue();

                for (Script script : scripts) {
                    if ((filter != null && isFilterable(script, filter)) ||
                        (world != null && !world.equalsIgnoreCase(position.getWorld()))) {
                        continue;
                    }

                    ++printCount;
                    BaseComponent[] baseComponents = new ComponentBuilder("")
                        .append(prefixComponent)
                        .append("[" + printCount + "] ")
                        .create();
                    sendScriptInfo(player, baseComponents, position);
                }
            }
            if (printCount == 0) {
                player.sendMessage(Prefix.ERROR_PREFIX + "Script not exists.");
            }
        });
    }

    //TODO: EDIT OPERATION

    private void sendScriptInfo(Player player, BaseComponent[] prefix, ScriptPosition position) {
        World world = Bukkit.getWorld(position.getWorld());
        String worldName = world != null
            ? world.getName()
            : position.getWorld();
        String tpCommand = "/embedscript teleport " + position.getWorld() + " " + position.getX() + " "
            + position.getY() + " " + position.getZ();
        BaseComponent[] baseComponents = new ComponentBuilder("")
            .append(prefix)
            .append("World: " + worldName + " X: " + position.getX()
                + " Y: " + position.getY() + " Z: " + position.getZ() + " (click to teleport)")
            .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(tpCommand)))
            .create();
        player.spigot().sendMessage(baseComponents);
    }

    public boolean hasScript(ScriptPosition position) {
        return scriptManager.contains(position);
    }

    private boolean isFilterable(Script script, Script filter) {
        if (isFilterable(script.getMoveTypes(), filter.getMoveTypes())) {
            return true;
        }
        if (isFilterable(script.getClickTypes(), filter.getClickTypes())) {
            return true;
        }
        if (isFilterable(script.getPushTypes(), filter.getPushTypes())) {
            return true;
        }
        if (isFilterable(script.getActionTypes(), filter.getActionTypes())) {
            return true;
        }

        BiPredicate<String, String> stringPredicate = String::equalsIgnoreCase;
        if (isFilterable(script.getActions(), filter.getActions(), stringPredicate)) {
            return true;
        }
        if (isFilterable(script.getPermissionsToGive(), filter.getPermissionsToGive(), stringPredicate)) {
            return true;
        }
        if (isFilterable(script.getPermissionsToNeeded(), filter.getPermissionsToNeeded(), stringPredicate)) {
            return true;
        }

        return isFilterable(script.getPermissionsToNotNeeded(), filter.getPermissionsToNotNeeded(), stringPredicate);
    }

    private <E> boolean isFilterable(Collection<E> target, Collection<E> filter, BiPredicate<E, E> equals) {
        for (E f : filter) {
            if (target.isEmpty()) {
                return true;
            }
            for (E t : target) {
                if (!equals.test(f, t)) {
                    return true;
                }
            }
        }
        return false;
    }

    private <E> boolean isFilterable(Collection<E> target, Collection<E> filter) {
        return isFilterable(target, filter, Object::equals);
    }
}
