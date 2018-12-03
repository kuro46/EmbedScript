package com.github.kuro46.embedscript.request;

import com.github.kuro46.embedscript.script.EventType;
import com.github.kuro46.embedscript.script.ScriptManager;
import com.github.kuro46.embedscript.script.ScriptPosition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * @author shirokuro
 */
public class Requests implements Listener {
    private final Map<Player, Request> requests = new HashMap<>(2);
    private final ScriptManager scriptManager;

    public Requests(Plugin plugin, ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public Request putRequest(Player player, Request request) {
        return requests.put(player, request);
    }

    public Request removeRequest(Player player) {
        return requests.remove(player);
    }

    public boolean hasRequest(Player player) {
        return requests.containsKey(player);
    }

    public boolean executeRequest(Player player, ScriptPosition position) {
        Request request = removeRequest(player);
        if (request == null)
            return false;

        EventType eventType = request.getEventType();
        switch (request.getRequestType()) {
            case VIEW: {
                scriptManager.view(player, eventType, position);
                break;
            }
            case EMBED: {
                scriptManager.embed(player, eventType,
                    position, ((RequestWithScript) request).getScript());
                break;
            }
            case ADD: {
                scriptManager.add(player, eventType,
                    position, ((RequestWithScript) request).getScript());
                break;
            }
            case REMOVE: {
                scriptManager.remove(player, eventType, position);
                break;
            }
            default: {
                throw new UnsupportedOperationException();
            }
        }
        return true;
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        requests.remove(event.getPlayer());
    }
}
