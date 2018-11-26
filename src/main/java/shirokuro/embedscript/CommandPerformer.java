package shirokuro.embedscript;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;
import shirokuro.embedscript.script.command.Command;
import shirokuro.embedscript.script.command.data.BypassPermCommandData;
import shirokuro.embedscript.script.command.data.CommandData;

import java.util.List;
import java.util.regex.Pattern;

/**
 * @author shirokuro
 */
public class CommandPerformer {
    private static final Pattern PLAYER_PATTERN = Pattern.compile("<player>", Pattern.LITERAL);
    private final Plugin plugin;

    public CommandPerformer(Plugin plugin) {
        this.plugin = plugin;
    }

    public void perform(Player trigger, Command command) {
        CommandData data = command.getData();
        String commandLine = PLAYER_PATTERN.matcher(command.getCommand()).replaceAll(trigger.getName());
        switch (data.getType()) {
            case BYPASS_PERMISSION: {
                String permission = ((BypassPermCommandData) data).getPermission();
                if (trigger.hasPermission(permission)) {
                    trigger.performCommand(commandLine);
                } else {
                    PermissionAttachment attachment = trigger.addAttachment(plugin, permission, true);
                    try {
                        trigger.performCommand(commandLine);
                    } finally {
                        trigger.removeAttachment(attachment);
                    }
                }
                break;
            }
            case PLAYER:
                trigger.sendMessage(commandLine);
                break;
            case COMMAND:
                trigger.performCommand(commandLine);
                break;
            case CONSOLE:
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandLine);
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    public void perform(Player trigger, List<Command> commands) {
        //TODO: BYPASSとかBYPASS_PERMとか連続してたらパーミッションの状態を維持しておくようにする
        commands.forEach(command -> perform(trigger, command));
    }
}
