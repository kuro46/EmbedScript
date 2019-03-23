package com.github.kuro46.embedscript.script;

import com.github.kuro46.embedscript.Configuration;
import com.github.kuro46.embedscript.api.EmbedScriptAPI;
import com.github.kuro46.embedscript.api.PerformListener;
import com.github.kuro46.embedscript.util.Scheduler;
import com.github.kuro46.embedscript.util.Util;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public class ScriptPerformer {
    private final Plugin plugin;
    private final Configuration configuration;
    private final Logger logger;

    public ScriptPerformer(Logger logger, Plugin plugin, Configuration configuration) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.logger = logger;
    }

    public void perform(ScriptPosition position, Script script, Player trigger) {
        for (String permission : script.getNeededPermissions()) {
            if (!hasPermissionOrOP(trigger, permission)) {
                return;
            }
        }

        for (String permission : script.getUnneededPermissions()) {
            if (hasPermissionOrOP(trigger, permission)) {
                return;
            }
        }

        List<String> permissionsToGive = script.getPermissionsToGive();
        PermissionAttachment attachment = permissionsToGive.isEmpty() ? null : trigger.addAttachment(plugin);
        for (String permission : permissionsToGive) {
            if (trigger.hasPermission(permission)) {
                continue;
            }
            attachment.setPermission(permission, true);
        }

        List<Script.ActionType> actionTypes = script.getActionTypes();
        for (String action : script.getActions()) {
            action = Util.replaceAndUnescape(action, "<player>", trigger.getName());
            action = Util.replaceAndUnescape(action, "<world>", trigger.getWorld().getName());

            for (Script.ActionType actionType : actionTypes) {
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
                    case BROADCAST_RAW:
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            player.spigot().sendMessage(ComponentSerializer.parse(action));
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException(
                            String.format("Cannot perform '%s': '%s' is unsupported type!",
                                action,
                                actionType.name()));
                }

                if (configuration.isLogEnabled()) {
                    final String finalAction = action;
                    Scheduler.execute(() -> {
                        String message = configuration.getLogFormat();
                        message = Util.replaceAndUnescape(message, "<trigger>", trigger.getName());
                        message = Util.replaceAndUnescape(message, "<action>", finalAction);
                        message = Util.replaceAndUnescape(message, "<action-type>", actionType.name().toLowerCase(Locale.ENGLISH));
                        Location location = trigger.getLocation();
                        String worldName = location.getWorld().getName();
                        message = Util.replaceAndUnescape(message, "<trigger_world>", worldName);
                        message = Util.replaceAndUnescape(message, "<trigger_x>", toString(location.getBlockX()));
                        message = Util.replaceAndUnescape(message, "<trigger_y>", toString(location.getBlockY()));
                        message = Util.replaceAndUnescape(message, "<trigger_z>", toString(location.getBlockZ()));
                        message = Util.replaceAndUnescape(message, "<script_world>", worldName);
                        message = Util.replaceAndUnescape(message, "<script_x>", toString(position.getX()));
                        message = Util.replaceAndUnescape(message, "<script_y>", toString(position.getY()));
                        message = Util.replaceAndUnescape(message, "<script_z>", toString(position.getZ()));

                        logger.info(message);
                    });
                }
            }
        }

        if (attachment != null) {
            trigger.removeAttachment(attachment);
        }
    }

    private boolean hasPermissionOrOP(Player player, String string) {
        return string.equals("op") ? player.isOp() : player.hasPermission(string);
    }

    private String toString(Object o) {
        return o.toString();
    }
}
