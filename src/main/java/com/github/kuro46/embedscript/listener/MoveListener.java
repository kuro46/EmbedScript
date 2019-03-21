package com.github.kuro46.embedscript.listener;

import com.github.kuro46.embedscript.EmbedScript;
import com.github.kuro46.embedscript.script.Script;
import com.github.kuro46.embedscript.script.ScriptManager;
import com.github.kuro46.embedscript.script.ScriptPosition;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Set;

/**
 * @author shirokuro
 */
public class MoveListener implements Listener {
    private final ScriptManager scriptManager;
    private final Plugin plugin;

    public MoveListener(EmbedScript embedScript) {
        this.plugin = embedScript.getPlugin();
        this.scriptManager = embedScript.getScriptManager();
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (equalsBlock(event.getFrom(), to)) {
            return;
        }

        Player player = event.getPlayer();
        List<Script> scripts = scriptManager.get(
            new ScriptPosition(to.getWorld().getName(),
                to.getBlockX(),
                to.getBlockY() - 1,
                to.getBlockZ()));
        if (scripts.isEmpty()) {
            return;
        }

        scripts.forEach(script -> {
            if (validateMoveType(script, event)) {
                script.perform(plugin, player);
            }
        });
    }

    private boolean validateMoveType(Script script, PlayerMoveEvent event) {
        Set<Script.MoveType> moveTypes = script.getMoveTypes();
        if (moveTypes.isEmpty()) {
            return false;
        }

        for (Script.MoveType moveType : moveTypes) {
            if (moveType == Script.MoveType.ALL) {
                return true;
            } else if (moveType == Script.MoveType.GROUND && validateGroundMoveType(event)) {
                return true;
            }
        }

        return false;
    }

    private boolean validateGroundMoveType(PlayerMoveEvent event) {
        Location to = event.getTo();
        //Expects air
        Block upperSurface = to.getBlock();
        //Expects non-air
        Block downerSurface = upperSurface.getRelative(BlockFace.DOWN);

        return (upperSurface.getType() == Material.AIR && downerSurface.getType() != Material.AIR)
            && !(to.getY() - upperSurface.getY() > 0.2);
    }

    private boolean equalsBlock(Location location, Location location1) {
        return location.getBlockX() == location1.getBlockX() &&
            location.getBlockY() == location1.getBlockY() &&
            location.getBlockZ() == location1.getBlockZ();
    }
}
