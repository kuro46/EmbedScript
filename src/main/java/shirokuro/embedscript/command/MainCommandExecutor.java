package shirokuro.embedscript.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import shirokuro.embedscript.Prefix;

import java.util.Locale;

/**
 * @author shirokuro
 */
public class MainCommandExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Can't perform this command from the console.");
            return true;
        }
        if (args.length < 1) {
            return false;
        }
        Player player = (Player) sender;
        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "teleport": {
                if (args.length < 5) {
                    return false;
                }
                World world = Bukkit.getWorld(args[1]);
                if (world == null) {
                    player.sendMessage(Prefix.ERROR_PREFIX + "World: " + args[1] + " not exist.");
                    return true;
                }
                try {
                    player.teleport(new Location(world,
                        Double.parseDouble(args[2]),
                        Double.parseDouble(args[3]),
                        Double.parseDouble(args[4])));
                } catch (NumberFormatException e) {
                    player.sendMessage("X or Y or Z is not valid number.");
                }
                player.sendMessage(Prefix.SUCCESS_PREFIX + "Teleported.");
                return true;
            }
            case "migrate": {
                return true;
            }
            default:
                return false;
        }
    }
}
