package com.github.kuro46.embedscript;

import com.github.kuro46.embedscript.request.Request;
import com.github.kuro46.embedscript.request.RequestType;
import com.github.kuro46.embedscript.request.Requests;
import com.github.kuro46.embedscript.script.ParseException;
import com.github.kuro46.embedscript.script.Script;
import com.github.kuro46.embedscript.script.ScriptUI;
import com.github.kuro46.embedscript.script.parser.ScriptParser;
import com.github.kuro46.embedscript.util.Util;
import org.apache.commons.lang.math.NumberUtils;
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
public class ESCommandExecutor implements CommandExecutor {
    private final ScriptParser scriptParser;
    private final String presetName;
    private final ScriptUI scriptUI;
    private final Requests requests;

    public ESCommandExecutor(ScriptParser scriptParser, ScriptUI scriptUI, Requests requests) {
        this(scriptParser, null, scriptUI, requests);
    }

    public ESCommandExecutor(ScriptParser scriptParser, String presetName, ScriptUI scriptUI, Requests requests) {
        this.scriptParser = scriptParser;
        this.presetName = presetName;
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
                return teleport(player, args);
            case "help":
                help(player);
                return true;
            case "page":
                return page(player, args);
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

    private boolean teleport(Player player, String[] args) {
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
    }

    private void help(Player player) {
        player.sendMessage(new String[]{
            "/embedscript teleport <world> <x> <y> <z> - Teleport to specific location.",
            "/embedscript list [world] - Displays list of scripts in specific world or the server.",
            "/embedscript view - Displays list of scripts in the clicked block.",
            "/embedscript remove - Removes scripts in the clicked block.",
            "/embedscript embed - Embeds scripts to the clicked block.",
            "/",
            "/embedscript help - Display this message."
        });
    }

    private boolean page(Player player, String[] args) {
        if (args.length < 2) {
            return false;
        }

        int parsed;
        try {
            parsed = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Prefix.ERROR_PREFIX + args[1] + " is not a valid number!");
            return true;
        }

        scriptUI.changePage(player, parsed);
        return true;
    }

    private void list(Player player, String[] args) {
        String world = args.length < 2
            ? player.getWorld().getName()
            : args[1];
        int pageIndex = !(args.length < 3) && NumberUtils.isNumber(args[2])
            ? Integer.parseInt(args[2]) - 1
            : 0;
        Script filter;
        if (presetName == null) {
            filter = null;
        } else {
            try {
                filter = scriptParser.parse(player.getUniqueId(), "@preset " + presetName);
            } catch (ParseException e) {
                player.sendMessage(Prefix.ERROR_PREFIX +
                    String.format("Failed to filter the scripts. (error: %s)", e.getMessage()));
                return;
            }
        }
        scriptUI.list(player, world, filter, pageIndex);
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
            String preset = presetName == null
                ? ""
                : "@preset " + presetName + " ";
            script = scriptParser.parse(player.getUniqueId(), preset + " " + stringScript);
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
