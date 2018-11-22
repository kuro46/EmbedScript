package shirokuro.embedscript.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import shirokuro.embedscript.CommandPerformer;
import shirokuro.embedscript.script.EventType;
import shirokuro.embedscript.script.Script;
import shirokuro.embedscript.script.ScriptBlock;
import shirokuro.embedscript.script.ScriptManager;

import java.util.HashMap;
import java.util.Map;

/**
 * @author shirokuro
 */
public class MoveListener extends AbstractListener {
    private final Map<Player, ScriptBlock> beforeWalked = new HashMap<>();
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
        ScriptBlock scriptBlock = new ScriptBlock(to.getWorld().getName(),
            to.getBlockX(), to.getBlockY() - 1, to.getBlockZ());

        Player player = event.getPlayer();

        ScriptBlock before = beforeWalked.get(player);
        if (before != null && before.equals(scriptBlock))
            return;
        beforeWalked.put(player, scriptBlock);

        Script script = scriptManager.getScript(EventType.WALK, scriptBlock);
        if (script == null)
            return;
        performer.perform(player, script.getCommands());
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        beforeWalked.remove(event.getPlayer());
    }
}
