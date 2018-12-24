package com.github.kuro46.embedscript.script;

import com.github.kuro46.embedscript.Prefix;
import com.github.kuro46.embedscript.script.command.Command;
import com.github.kuro46.embedscript.script.command.data.BypassPermCommandData;
import com.github.kuro46.embedscript.script.command.data.CommandData;
import com.github.kuro46.embedscript.util.MojangUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author shirokuro
 */
public class ScriptManager {
    private final Map<EventType, Map<ScriptPosition, Script>> scripts = new EnumMap<>(EventType.class);
    private final Map<EventType, Path> paths = new EnumMap<>(EventType.class);
    private final Plugin plugin;

    public ScriptManager(Plugin plugin) throws IOException {
        this.plugin = plugin;

        Path dataFolder = plugin.getDataFolder().toPath();
        Files.createDirectories(dataFolder);
        for (EventType eventType : EventType.values()) {
            Path path = Paths.get(dataFolder.toString(), eventType.getFileName());
            paths.put(eventType, path);
            scripts.put(eventType, ScriptSerializer.deserialize(path));
        }
    }

    public void embed(CommandSender sender,
                      EventType type,
                      ScriptPosition position,
                      Script script) {
        Map<ScriptPosition, Script> scripts = getScripts(type);
        if (scripts.putIfAbsent(position, script) != null) {
            sender.sendMessage(Prefix.ERROR_PREFIX + "Script already exists in that place.");
            return;
        }
        ScriptSerializer.serializeLaterAsync(getPath(type), scripts);

        sender.sendMessage(Prefix.SUCCESS_PREFIX + "Script was successfully embedded.");
    }

    public void add(CommandSender sender,
                    EventType type,
                    ScriptPosition position,
                    Script script) {
        Map<ScriptPosition, Script> scripts = getScripts(type);
        Script baseScript = getScript(type, position);
        if (baseScript == null) {
            sender.sendMessage(Prefix.ERROR_PREFIX + "Script not exists in that place.");
            return;
        }
        baseScript.getCommands().addAll(script.getCommands());
        ScriptSerializer.serializeLaterAsync(getPath(type), scripts);

        sender.sendMessage(Prefix.SUCCESS_PREFIX + "Script was successfully added.");
    }

    public void remove(CommandSender sender, EventType type, ScriptPosition position) {
        Map<ScriptPosition, Script> scripts = getScripts(type);
        if (scripts.remove(position) == null) {
            sender.sendMessage(Prefix.ERROR_PREFIX + "Script not exists in that place.");
            return;
        }
        ScriptSerializer.serializeLaterAsync(getPath(type), scripts);

        sender.sendMessage(Prefix.SUCCESS_PREFIX + "Script was successfully removed.");
    }

    public void view(CommandSender sender, EventType type, ScriptPosition position) {
        Script script = getScript(type, position);
        if (script == null) {
            sender.sendMessage(Prefix.ERROR_PREFIX + "Script not exists in that place.");
            return;
        }
        ArrayList<Command> commands = new ArrayList<>(script.getCommands());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            sender.sendMessage("Script information: ------------------------------");
            for (Command command : commands) {
                sender.sendMessage("===============================================");
                UUID author = command.getAuthor();
                Player player = Bukkit.getPlayer(author);
                String stringAuthor = player == null
                    ? MojangUtil.getName(author)
                    : player.getName();
                sender.sendMessage("author: " + stringAuthor);
                sender.sendMessage("command: " + command.getCommand());
                CommandData data = command.getData();
                sender.sendMessage("type: " + data.getType().name());
                if (data.getType() == ScriptType.BYPASS_PERMISSION) {
                    sender.sendMessage("permission: " + ((BypassPermCommandData) data).getPermission());
                }
                sender.sendMessage("===============================================");
            }
            sender.sendMessage("Script information: ------------------------------");
        });
    }

    /**
     * Send list of scripts to player
     *
     * @param player Player
     * @param type   Event type
     * @param world  World (Nullable)
     */
    public void list(Player player, EventType type, String world) {
        Predicate<ScriptPosition> predicate;
        if (world == null) {
            predicate = location -> true;
        } else {
            predicate = location -> world.equals(location.getWorld());
        }
        BaseComponent[] prefixComponent = TextComponent.fromLegacyText(Prefix.PREFIX);
        Set<ScriptPosition> positions = getScripts(type).keySet().stream()
            .filter(predicate)
            .collect(Collectors.toSet());
        int i = 0;
        for (ScriptPosition position : positions) {
            ++i;
            BaseComponent[] baseComponents = new ComponentBuilder("")
                .append(prefixComponent)
                .append("[" + i + "] ")
                .create();
            sendScriptInfo(player, baseComponents, position);
        }
        if (i == 0) {
            player.sendMessage(Prefix.ERROR_PREFIX + "Script not exists.");
        }
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

    public boolean hasScript(EventType eventType, ScriptPosition position) {
        return getScripts(eventType).containsKey(position);
    }

    public Script getScript(EventType eventType, ScriptPosition position) {
        return getScripts(eventType).get(position);
    }

    private Map<ScriptPosition, Script> getScripts(EventType eventType) {
        return scripts.get(eventType);
    }

    private Path getPath(EventType eventType) {
        return paths.get(eventType);
    }
}
