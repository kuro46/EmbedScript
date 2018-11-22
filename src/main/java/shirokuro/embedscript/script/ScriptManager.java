package shirokuro.embedscript.script;

import com.google.gson.reflect.TypeToken;
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
import shirokuro.embedscript.GsonHolder;
import shirokuro.embedscript.Prefix;
import shirokuro.embedscript.script.command.Command;
import shirokuro.embedscript.script.command.data.BypassPermCommandData;
import shirokuro.embedscript.script.command.data.CommandData;
import shirokuro.embedscript.util.MojangUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author shirokuro
 */
public class ScriptManager {
    private final Map<EventType, Map<ScriptBlock, Script>> scripts = new EnumMap<>(EventType.class);
    private final Map<EventType, Path> paths = new EnumMap<>(EventType.class);
    private final Plugin plugin;

    public ScriptManager(Plugin plugin) throws IOException {
        this.plugin = plugin;

        Path dataFolder = plugin.getDataFolder().toPath();
        Files.createDirectories(dataFolder);
        for (EventType eventType : EventType.values()) {
            Path path = Paths.get(dataFolder.toString(), eventType.getFileName());
            paths.put(eventType, path);
            scripts.put(eventType, readScripts(path));
        }
    }

    public void embed(CommandSender sender,
                      EventType type,
                      ScriptBlock location,
                      Script script) {
        Map<ScriptBlock, Script> scripts = getScripts(type);
        if (scripts.putIfAbsent(location, script) != null) {
            sender.sendMessage(Prefix.ERROR_PREFIX + "Script already exists in that place.");
            return;
        }
        writeScriptsAsync(getPath(type), scripts);

        sender.sendMessage(Prefix.SUCCESS_PREFIX + "Script was successfully embedded.");
    }

    public void add(CommandSender sender,
                    EventType type,
                    ScriptBlock block,
                    Script script) {
        Map<ScriptBlock, Script> scripts = getScripts(type);
        Script baseScript = getScript(type, block);
        if (baseScript == null) {
            sender.sendMessage(Prefix.ERROR_PREFIX + "Script not exists in that place.");
            return;
        }
        baseScript.getCommands().addAll(script.getCommands());
        writeScriptsAsync(getPath(type), scripts);

        sender.sendMessage(Prefix.SUCCESS_PREFIX + "Script was successfully added.");
    }

    public void remove(CommandSender sender, EventType type, ScriptBlock location) {
        Map<ScriptBlock, Script> scripts = getScripts(type);
        if (scripts.remove(location) == null) {
            sender.sendMessage(Prefix.ERROR_PREFIX + "Script not exists in that place.");
            return;
        }
        writeScriptsAsync(getPath(type), scripts);

        sender.sendMessage(Prefix.SUCCESS_PREFIX + "Script was successfully removed.");
    }

    public void view(CommandSender sender, EventType type, ScriptBlock block) {
        Script script = getScript(type, block);
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
    public void list(Player player, EventType type, World world) {
        Predicate<ScriptBlock> predicate;
        if (world == null) {
            predicate = location -> true;
        } else {
            predicate = location -> {
                World locationWorld = Bukkit.getWorld(location.getWorld());
                return world.equals(locationWorld);
            };
        }
        BaseComponent[] prefixComponent = TextComponent.fromLegacyText(Prefix.PREFIX);
        Set<ScriptBlock> blocks = getScripts(type).keySet().stream()
            .filter(predicate)
            .collect(Collectors.toSet());
        int i = 0;
        for (ScriptBlock block : blocks) {
            ++i;
            BaseComponent[] baseComponents = new ComponentBuilder("")
                .append(prefixComponent)
                .append("[" + i + "] ")
                .create();
            sendScriptInfo(player, baseComponents, block);
        }
        if (i == 0) {
            player.sendMessage(Prefix.ERROR_PREFIX + "Script not exists.");
        }
    }

    //TODO: EDIT OPERATION

    private void sendScriptInfo(Player player, BaseComponent[] prefix, ScriptBlock location) {
        World world = Bukkit.getWorld(location.getWorld());
        String worldName = world != null
            ? world.getName()
            : location.getWorld().toString();
        String tpCommand = "/tp " + player.getName() + " " + location.getX() + " "
            + location.getY() + " " + location.getZ();
        BaseComponent[] baseComponents = new ComponentBuilder("")
            .append(prefix)
            .append("World: " + worldName + " X: " + location.getX()
                + " Y: " + location.getY() + " Z: " + location.getZ() + " (click to teleport)")
            .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(tpCommand)))
            .create();
        player.spigot().sendMessage(baseComponents);
    }

    public boolean hasScript(EventType eventType, ScriptBlock block) {
        return getScripts(eventType).containsKey(block);
    }

    public Script getScript(EventType eventType, ScriptBlock block) {
        return getScripts(eventType).get(block);
    }

    private Map<ScriptBlock, Script> getScripts(EventType eventType) {
        return scripts.get(eventType);
    }

    public Map<ScriptBlock, Script> getScriptsView(EventType eventType) {
        return Collections.unmodifiableMap(scripts.get(eventType));
    }

    private Path getPath(EventType eventType) {
        return paths.get(eventType);
    }

    private Map<ScriptBlock, Script> readScripts(Path path) throws IOException {
        if (Files.notExists(path)) {
            return new HashMap<>();
        }
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            return GsonHolder.get().fromJson(reader, new TypeToken<Map<ScriptBlock, Script>>() {
            }.getType());
        }
    }

    private void writeScripts(Path path, Map<ScriptBlock, Script> scripts) throws IOException {
        if (Files.notExists(path)) {
            Files.createFile(path);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            GsonHolder.get().toJson(scripts, writer);
        }
    }

    private void writeScriptsAsync(Path path, Map<ScriptBlock, Script> scripts) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                writeScripts(path, scripts);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

}
