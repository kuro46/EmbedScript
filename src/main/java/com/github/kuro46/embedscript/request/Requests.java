package com.github.kuro46.embedscript.request;

import com.github.kuro46.embedscript.script.EventType;
import com.github.kuro46.embedscript.script.ScriptPosition;
import com.github.kuro46.embedscript.script.ScriptUI;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author shirokuro
 */
public class Requests implements Listener {
    private final Map<Player, Request> requests = new WeakHashMap<>(2);
    private final ScriptUI scriptUI;

    public Requests(ScriptUI scriptUI) {
        this.scriptUI = scriptUI;
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
                scriptUI.view(player, eventType, position);
                break;
            }
            case EMBED: {
                scriptUI.embed(player, eventType,
                    position, ((RequestWithScript) request).getScript());
                break;
            }
            case ADD: {
                scriptUI.add(player, eventType,
                    position, ((RequestWithScript) request).getScript());
                break;
            }
            case REMOVE: {
                scriptUI.remove(player, eventType, position);
                break;
            }
            default: {
                throw new UnsupportedOperationException();
            }
        }
        return true;
    }
}
