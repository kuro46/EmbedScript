package com.github.kuro46.embedscript.listener;

import com.github.kuro46.embedscript.EmbedScript;
import com.github.kuro46.embedscript.request.Requests;
import com.github.kuro46.embedscript.script.Script;
import com.github.kuro46.embedscript.script.ScriptManager;
import com.github.kuro46.embedscript.script.ScriptPosition;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author shirokuro
 */
public class InteractListener implements Listener {
    private final Cache<UUID, Boolean> coolTime = CacheBuilder.newBuilder()
        .expireAfterWrite(300, TimeUnit.MILLISECONDS)
        .build();
    private final Plugin plugin;
    private final ScriptManager scriptManager;
    private final Requests requests;

    public InteractListener(EmbedScript embedScript) {
        this.plugin = embedScript.getPlugin();
        this.scriptManager = embedScript.getScriptManager();
        this.requests = embedScript.getRequests();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!event.hasBlock() || isInCoolTime(player)) {
            return;
        }
        updateCoolTime(player);

        ScriptPosition position = new ScriptPosition(event.getClickedBlock());
        if (requests.executeRequest(player, position)) {
            return;
        }

        List<Script> scripts = scriptManager.get(position);
        if (scripts.isEmpty()) {
            return;
        }

        for (Script script : scripts) {
            if (validateClickType(script, event.getAction()) || validatePushType(script, event)) {
                script.perform(plugin, player);
                event.setCancelled(true);
            }
        }
    }

    private boolean validateClickType(Script script, Action action) {
        Set<Script.ClickType> clickTypes = script.getClickTypes();
        if (clickTypes.isEmpty()) {
            return false;
        }

        Script.ClickType clickTypeOfEvent = Script.ClickType.getByAction(action);
        // PHYSICAL action
        if (clickTypeOfEvent == null) {
            return false;
        }

        for (Script.ClickType clickType : clickTypes) {
            if (clickType == Script.ClickType.ALL || clickType == clickTypeOfEvent) {
                return true;
            }
        }

        return false;
    }

    private boolean validatePushType(Script script, PlayerInteractEvent event) {
        Set<Script.PushType> pushTypes = script.getPushTypes();
        if (pushTypes.isEmpty()) {
            return false;
        }

        Script.PushType pushTypeOfEvent = Script.PushType.getByEvent(event);
        //Not PHYSICAL or Unknown material
        if (pushTypeOfEvent == null) {
            return false;
        }

        for (Script.PushType pushType : pushTypes) {
            if (pushType == Script.PushType.ALL || pushType == pushTypeOfEvent) {
                return true;
            }
        }

        return false;
    }

    private boolean isInCoolTime(Player player) {
        return coolTime.getIfPresent(player.getUniqueId()) != null;
    }

    private void updateCoolTime(Player player) {
        coolTime.put(player.getUniqueId(), Boolean.TRUE);
    }
}
