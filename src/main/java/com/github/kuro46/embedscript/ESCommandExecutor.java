package com.github.kuro46.embedscript;

import com.github.kuro46.embedscript.migrator.ScriptBlockMigrator;
import com.github.kuro46.embedscript.request.Request;
import com.github.kuro46.embedscript.request.RequestType;
import com.github.kuro46.embedscript.request.Requests;
import com.github.kuro46.embedscript.script.ParseException;
import com.github.kuro46.embedscript.script.Script;
import com.github.kuro46.embedscript.script.ScriptManager;
import com.github.kuro46.embedscript.script.ScriptUI;
import com.github.kuro46.embedscript.script.parser.ScriptParser;
import com.github.kuro46.embedscript.util.Scheduler;
import com.github.kuro46.embedscript.util.Util;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/**
 * @author shirokuro
 */
public class ESCommandExecutor implements CommandExecutor {
    private final Configuration configuration;
    private final ScriptParser scriptParser;
    private final String presetName;
    private final ScriptUI scriptUI;
    private final Requests requests;
    private final ScriptManager scriptManager;
    private final EmbedScript embedScript;

    public ESCommandExecutor(EmbedScript embedScript) {
        this(embedScript, null);
    }

    public ESCommandExecutor(EmbedScript embedScript, String presetName) {
        this.embedScript = embedScript;
        this.scriptManager = embedScript.getScriptManager();
        this.configuration = embedScript.getConfiguration();
        this.scriptParser = embedScript.getScriptParser();
        this.presetName = presetName;
        this.scriptUI = embedScript.getScriptUI();
        this.requests = embedScript.getRequests();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            return false;
        }

        HandleResult consoleHandleResult = handleConsoleCommand(sender, args);
        if (consoleHandleResult != HandleResult.UNMATCH) {
            return consoleHandleResult.toBoolean();
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Cannot perform this command from the console.");
            return true;
        }

        Player player = (Player) sender;

        return handlePlayerCommand(player, args).toBoolean();
    }

    private HandleResult handleConsoleCommand(CommandSender sender, String[] args) {
        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "help":
                help(sender);
                return HandleResult.COLLECT_USE;
            case "reload":
                reload(sender);
                return HandleResult.COLLECT_USE;
            case "migrate":
                migrate(sender);
                return HandleResult.COLLECT_USE;
            default:
                return HandleResult.UNMATCH;
        }
    }

    private HandleResult handlePlayerCommand(Player player, String[] args) {
        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "teleport":
                return HandleResult.getByBoolean(teleport(player, args));
            case "page":
                return HandleResult.getByBoolean(page(player, args));
            //Script operations
            case "list":
                list(player, args);
                return HandleResult.COLLECT_USE;
            case "view":
                view(player);
                return HandleResult.COLLECT_USE;
            case "remove":
                remove(player);
                return HandleResult.COLLECT_USE;
            case "embed":
                return HandleResult.getByBoolean(modifyAction(player, args, RequestType.EMBED));
            case "add":
                return HandleResult.getByBoolean(modifyAction(player, args, RequestType.ADD));
            default:
                return HandleResult.UNMATCH;
        }
    }

    private void help(CommandSender sender) {
        sender.sendMessage(new String[]{
            "/es help - displays this message",
            "/es reload - reloads configuration and scripts",
            "/es migrate - migrates from ScriptBlock",
            "/es list [world] [page] - displays list of scripts",
            "/es view - displays information of the script in the clicked block",
            "/es remove - removes the script in the clicked block",
            "/es embed <script> - embeds a script to the clicked block",
            "/es add <script> - adds a script to the clicked block"
        });
    }

    private void reload(CommandSender sender) {
        Scheduler.execute(() -> {
            sender.sendMessage(Prefix.PREFIX + "Reloading configuration and scripts...");
            try {
                configuration.load();
            } catch (IOException | InvalidConfigurationException e) {
                sender.sendMessage(Prefix.ERROR_PREFIX + "Failed to reload configuration! (error: " + e.getMessage() + ")");
                e.printStackTrace();
                return;
            }

            try {
                scriptManager.reload();
            } catch (IOException e) {
                sender.sendMessage(Prefix.ERROR_PREFIX + "Failed to reload scripts! (error: " + e.getMessage() + ")");
                e.printStackTrace();
            }

            sender.sendMessage(Prefix.SUCCESS_PREFIX + "Successfully reloaded!");
        });
    }

    private void migrate(CommandSender sender) {
        Scheduler.execute(() -> {
            sender.sendMessage("Migrating data of ScriptBlock...");
            try {
                ScriptBlockMigrator.migrate(embedScript);
            } catch (InvalidConfigurationException | ParseException | IOException e) {
                sender.sendMessage(Prefix.ERROR_PREFIX + "Failed to migrate data of ScriptBlock!");
                System.err.println("Failed to migrate data of ScriptBlock!");
                e.printStackTrace();
                return;
            }
            sender.sendMessage(Prefix.SUCCESS_PREFIX + "Successfully migrated!");
        });
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
            return true;
        }
        player.sendMessage(Prefix.SUCCESS_PREFIX + "Teleported.");
        return true;
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
            script = scriptParser.parse(player.getUniqueId(), preset + stringScript);
        } catch (ParseException e) {
            player.sendMessage(Prefix.ERROR_PREFIX +
                String.format("Failed to parse script. (error: %s)", e.getMessage()));
            return true;
        }
        player.sendMessage(Prefix.PREFIX + "Click the block to add a script.");
        requests.putRequest(player, new Request(requestType, script));

        return true;
    }

    private enum HandleResult {
        COLLECT_USE,
        INCORRECT_USE,
        UNMATCH;

        public static HandleResult getByBoolean(boolean bool) {
            return bool ? COLLECT_USE : INCORRECT_USE;
        }

        public boolean toBoolean() {
            switch (this) {
                case UNMATCH:
                case INCORRECT_USE:
                    return false;
                case COLLECT_USE:
                    return true;
                default:
                    throw new IllegalStateException();
            }
        }
    }
}
