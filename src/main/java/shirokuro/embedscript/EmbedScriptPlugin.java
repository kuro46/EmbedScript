package shirokuro.embedscript;

import org.bukkit.plugin.java.JavaPlugin;
import shirokuro.embedscript.command.EventCommandExecutor;
import shirokuro.embedscript.listener.InteractListener;
import shirokuro.embedscript.listener.MoveListener;
import shirokuro.embedscript.request.Requests;
import shirokuro.embedscript.script.EventType;
import shirokuro.embedscript.script.ScriptManager;

import java.io.IOException;

/**
 * @author shirokuro
 */
public class EmbedScriptPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        ScriptManager scriptManager;
        try {
            scriptManager = new ScriptManager(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Requests requests = new Requests(this, scriptManager);
        for (EventType eventType : EventType.values()) {
            getCommand(eventType.getCommandName())
                .setExecutor(new EventCommandExecutor(eventType, requests, scriptManager));
        }
        CommandPerformer commandPerformer = new CommandPerformer(this);
        new InteractListener(this, scriptManager, requests, commandPerformer);
        new MoveListener(this, scriptManager, commandPerformer);
    }
}
