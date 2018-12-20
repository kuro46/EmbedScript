package com.github.kuro46.embedscript.listener;

import com.github.kuro46.embedscript.CommandPerformer;
import com.github.kuro46.embedscript.script.EventType;
import com.github.kuro46.embedscript.script.Script;
import com.github.kuro46.embedscript.script.ScriptManager;
import com.github.kuro46.embedscript.script.ScriptPosition;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author shirokuro
 */
public class MoveListener extends AbstractListener {
    private final Map<UUID, ScriptPosition> beforeWalked = new HashMap<>();
    private final ScriptManager scriptManager;
    private final CommandPerformer performer;

    public MoveListener(Plugin plugin, ScriptManager scriptManager, CommandPerformer performer) {
        super(plugin);
        this.scriptManager = scriptManager;
        this.performer = performer;
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        ScriptPosition scriptPosition = new ScriptPosition(to.getWorld().getName(),
            to.getBlockX(), to.getBlockY() - 1, to.getBlockZ());

        Player player = event.getPlayer();

        ScriptPosition before = beforeWalked.get(player.getUniqueId());
        if (before != null && before.equals(scriptPosition))
            return;
        beforeWalked.put(player.getUniqueId(), scriptPosition);

        Script script = scriptManager.getScript(EventType.WALK, scriptPosition);
        if (script == null)
            return;
        Block blockAt = to.getWorld().getBlockAt(
            scriptPosition.getX(),
            scriptPosition.getY(),
            scriptPosition.getZ());
        double y = to.getY();
        if (blockAt.getType() != Material.AIR && y != (int) y)
            return;
        performer.perform(player, script.getCommands());
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        beforeWalked.remove(event.getPlayer().getUniqueId());
    }
}
