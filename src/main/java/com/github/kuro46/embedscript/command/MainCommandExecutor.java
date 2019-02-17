package com.github.kuro46.embedscript.command;

import com.github.kuro46.embedscript.Prefix;
import com.github.kuro46.embedscript.request.Request;
import com.github.kuro46.embedscript.request.RequestType;
import com.github.kuro46.embedscript.request.Requests;
import com.github.kuro46.embedscript.script.ParseException;
import com.github.kuro46.embedscript.script.Script;
import com.github.kuro46.embedscript.script.ScriptUI;
import com.github.kuro46.embedscript.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * @author shirokuro
 */
public class MainCommandExecutor implements CommandExecutor {
    private final ScriptUI scriptUI;
    private final Requests requests;

    public MainCommandExecutor(ScriptUI scriptUI, Requests requests) {
        this.scriptUI = scriptUI;
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
            case "teleport":
                if (args.length < 5) {
                    return false;
                }
                World world = Bukkit.getWorld(args[1]);
                if (world == null) {
                    player.sendMessage(Prefix.ERROR_PREFIX + "World: " + args[1] + " not exist.");
                    return true;
                }
                try {
                    Location playerLocation = player.getLocation();
                    player.teleport(new Location(world,
                        Integer.parseInt(args[2]) + 0.5,
                        Integer.parseInt(args[3]),
                        Integer.parseInt(args[4]) + 0.5,
                        playerLocation.getYaw(),
                        playerLocation.getPitch()));
                } catch (NumberFormatException e) {
                    player.sendMessage("X or Y or Z is not valid number.");
                }
                player.sendMessage(Prefix.SUCCESS_PREFIX + "Teleported.");
                return true;
            case "help":
                player.sendMessage(new String[]{
                    "/embedscript teleport <world> <x> <y> <z> - Teleport to specific location.",
                    "/embedscript list [world] - Displays list of scripts in specific world or the server.",
                    "/embedscript view - Displays list of scripts in the clicked block.",
                    "/embedscript remove - Removes scripts in the clicked block.",
                    "/embedscript embed - Embeds scripts to the clicked block.",
                    "/",
                    "/embedscript help - Display this message."
                });
                return true;
            //Script operations
            case "list":
                list(player, args);
                return true;
            case "view":
                view(player);
                return true;
            case "remove":
                remove(player);
                return true;
            case "embed":
                return modifyAction(player, args, RequestType.EMBED);
            case "add":
                return modifyAction(player, args, RequestType.ADD);
            default:
                return false;
        }
    }

    private void list(Player player, String[] args) {
        String world = args.length < 2
            ? null
            : args[1];
        scriptUI.list(player, world, null);
    }

    private void view(Player player) {
        player.sendMessage(Prefix.PREFIX + "Click the block to view the script.");
        requests.putRequest(player, new Request(RequestType.VIEW));
    }

    private void remove(Player player) {
        player.sendMessage(Prefix.PREFIX + "Click the block to remove the script.");
        requests.putRequest(player, new Request(RequestType.REMOVE));
    }

    private boolean modifyAction(Player player, String[] args, RequestType requestType) {
        if (args.length < 2) {
            return false;
        }
        String stringScript = Util.joinStringSpaceDelimiter(1, args);
        Script script;
        try {
            script = Script.parse(player.getUniqueId(), stringScript);
        } catch (ParseException e) {
            player.sendMessage(Prefix.ERROR_PREFIX +
                String.format("Failed to parse script. (error: %s)", e.getMessage()));
            return true;
        }
        player.sendMessage(Prefix.PREFIX + "Click the block to add a script.");
        requests.putRequest(player, new Request(requestType, script));

        return true;
    }
}
