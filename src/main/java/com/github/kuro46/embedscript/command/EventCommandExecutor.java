package com.github.kuro46.embedscript.command;

import com.github.kuro46.embedscript.Prefix;
import com.github.kuro46.embedscript.request.Request;
import com.github.kuro46.embedscript.request.RequestType;
import com.github.kuro46.embedscript.request.Requests;
import com.github.kuro46.embedscript.script.EventType;
import com.github.kuro46.embedscript.script.ParseException;
import com.github.kuro46.embedscript.script.Script;
import com.github.kuro46.embedscript.script.ScriptUI;
import com.github.kuro46.embedscript.util.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * @author shirokuro
 */
public class EventCommandExecutor implements CommandExecutor {
    private final ScriptUI scriptUI;
    private final EventType eventType;
    private final Requests requests;

    public EventCommandExecutor(EventType eventType, Requests requests, ScriptUI scriptUI) {
        this.scriptUI = scriptUI;
        this.eventType = eventType;
        this.requests = requests;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Can't perform this command from the console.");
            return true;
        }
        if (args.length < 1) {
            return false;
        }
        Player player = (Player) sender;
        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "view":
                player.sendMessage(Prefix.PREFIX + "Click the block to view the script.");
                requests.putRequest(player, new Request(RequestType.VIEW));
                return true;
            case "embed": {
                if (args.length < 2) {
                    return false;
                }
                Script script = parseScript(player, args);
                if (script == null) {
                    return true;
                }
                player.sendMessage(Prefix.PREFIX + "Click the block to embed a script.");
                requests.putRequest(player, new Request(RequestType.EMBED, script));
                return true;
            }
            case "add": {
                if (args.length < 2) {
                    return false;
                }
                Script script = parseScript(player, args);
                if (script == null) {
                    return true;
                }
                player.sendMessage(Prefix.PREFIX + "Click the block to add a script.");
                requests.putRequest(player, new Request(RequestType.ADD, script));

                return true;
            }
            case "remove":
                player.sendMessage(Prefix.PREFIX + "Click the block to remove the script.");
                requests.putRequest(player, new Request(RequestType.REMOVE));
                return true;
            case "list":
                String world = args.length < 2
                    ? null
                    : args[1];
                Script filter;
                try {
                    filter = Script.parse(null, eventType.getPreset().trim());
                } catch (ParseException e) {
                    e.printStackTrace();
                    return true;
                }
                scriptUI.list(player, world, filter);
                return true;
            case "help":
                player.sendMessage(new String[]{
                    "/" + eventType.getCommandName() + " view - Show commands in the script.",
                    "/" + eventType.getCommandName() + " embed <script> - Embed script to the block.",
                    "/" + eventType.getCommandName() + " add <script> - Add script to the block.",
                    "/" + eventType.getCommandName() + " remove - Remove the script from the block.",
                    "/" + eventType.getCommandName() + " list [world] - Show list of location of script."
                });
                return true;
            default:
                return false;
        }
    }

    private Script parseScript(Player player, String[] args) {
        String stringScript = eventType.getPreset() + Util.joinStringSpaceDelimiter(1, args);
        try {
            return Script.parse(player.getUniqueId(), stringScript);
        } catch (ParseException e) {
            player.sendMessage(Prefix.ERROR_PREFIX +
                String.format("Failed to parse script. (error: %s)", e.getMessage()));
            return null;
        }
    }
}
