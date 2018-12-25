package com.github.kuro46.embedscript.listener;

import com.github.kuro46.embedscript.script.EventType;
import com.github.kuro46.embedscript.script.Script;
import com.github.kuro46.embedscript.script.ScriptManager;
import com.github.kuro46.embedscript.script.ScriptPosition;
import com.github.kuro46.embedscript.script.command.CommandPerformer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author shirokuro
 */
public class MoveListener extends AbstractListener {
    private final Map<Player, ScriptPosition> beforeWalked = new WeakHashMap<>();
    private final ScriptManager scriptManager;
    private final CommandPerformer performer;

    public MoveListener(Plugin plugin, ScriptManager scriptManager, CommandPerformer performer) {
        super(plugin);
        this.scriptManager = scriptManager;
        this.performer = performer;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        beforeWalked.put(player, new ScriptPosition(player.getLocation()));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        //Expects air
        Block upperSurface = to.getBlock();
        //Expects non-air
        Block downerSurface = upperSurface.getRelative(BlockFace.DOWN);

        if (!(upperSurface.getType() == Material.AIR && downerSurface.getType() != Material.AIR)
            || to.getY() - upperSurface.getY() > 0.2) {
            return;
        }

        Player player = event.getPlayer();
        ScriptPosition position = new ScriptPosition(downerSurface);
        ScriptPosition beforePos = beforeWalked.get(player);
        if (equal2D(position, beforePos)) {
            return;
        }
        beforeWalked.put(player, position);

        Script script = scriptManager.getScript(EventType.WALK, position);
        if (script == null) {
            return;
        }
        performer.perform(player, script.getCommands());
    }

    private boolean equal2D(ScriptPosition position, ScriptPosition position1) {
        return position.getX() == position1.getX()
            && position.getZ() == position1.getZ();
    }
}
