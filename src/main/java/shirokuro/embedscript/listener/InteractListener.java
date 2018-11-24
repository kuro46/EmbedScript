package shirokuro.embedscript.listener;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import shirokuro.embedscript.CommandPerformer;
import shirokuro.embedscript.request.Requests;
import shirokuro.embedscript.script.EventType;
import shirokuro.embedscript.script.Script;
import shirokuro.embedscript.script.ScriptBlock;
import shirokuro.embedscript.script.ScriptManager;

import java.util.concurrent.TimeUnit;

/**
 * @author shirokuro
 */
@SuppressWarnings("unused")
public class InteractListener extends AbstractListener {
    private final Cache<Player, Boolean> interval = CacheBuilder.newBuilder()
        .weakKeys()
        .expireAfterWrite(300, TimeUnit.MILLISECONDS)
        .build();
    private final ScriptManager scriptManager;
    private final Requests requests;
    private final CommandPerformer performer;

    public InteractListener(Plugin plugin, ScriptManager scriptManager, Requests requests, CommandPerformer performer) {
        super(plugin);
        this.scriptManager = scriptManager;
        this.requests = requests;
        this.performer = performer;
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!event.hasBlock() || interval.getIfPresent(player) != null)
            return;
        interval.put(player, Boolean.TRUE);
        ScriptBlock scriptBlock = new ScriptBlock(event.getClickedBlock());
        if (requests.executeRequest(player, scriptBlock))
            return;
        Script script = scriptManager.getScript(EventType.INTERACT, scriptBlock);
        if (script == null)
            return;

        performer.perform(player, script.getCommands());
    }
}
