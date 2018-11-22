package shirokuro.embedscript.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Locale;

/**
 * @author shirokuro
 */
public class MainCommandExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String args0 = args[0].toLowerCase(Locale.ENGLISH);
        switch (args0) {
            case "teleport": {
                //TODO:
                /*
                args1 = player
                args2 = world
                args3 = x
                args4 = y
                args5 = z
                 */
                break;
            }
        }
        sender.sendMessage("Nothing to here :)");
        return true;
    }
}
