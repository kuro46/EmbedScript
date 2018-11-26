package shirokuro.embedscript.script;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import shirokuro.embedscript.Prefix;
import shirokuro.embedscript.script.command.Command;
import shirokuro.embedscript.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author shirokuro
 */
public final class ScriptGenerator {
    private ScriptGenerator() {
        throw new UnsupportedOperationException("Class for static method");
    }

    public static Script generateFromString(CommandSender sender, UUID author, String stringScript) {
        String[] stringCommands;
        if (stringScript.startsWith("[") && stringScript.endsWith("]")) {
            stringScript = stringScript.substring(1, stringScript.length() - 1);
            stringCommands = stringScript.split("]\\[");
        } else {
            stringCommands = new String[]{stringScript};
        }
        List<Command> commands = new ArrayList<>(stringCommands.length);
        for (String stringCommand : stringCommands) {
            if (stringCommand.startsWith("@bypass")) {
                sender.sendMessage(Prefix.ERROR_PREFIX + "@bypass is not implemented in EmbedScript," +
                    " Because it is have a vulnerability.");
                return null;
            }
            Command command = generateCommandFromString(author, stringCommand);
            if (command == null) {
                sender.sendMessage(Prefix.ERROR_PREFIX + "Bad script!");
                return null;
            }
            commands.add(command);
        }
        return new Script(commands);
    }

    private static Command generateCommandFromString(UUID author, String string) {
        String[] strings = string.split(" ");
        if (strings.length < 2)
            return null;
        String stringScriptType = strings[0];
        ScriptType scriptType = ScriptType.getByString(stringScriptType);
        if (scriptType == null)
            return null;
        String command = Util.joinStringSpaceDelimiter(1, strings);
        command = ChatColor.translateAlternateColorCodes('&', command);
        if (scriptType != ScriptType.PLAYER && command.startsWith("/")) {
            command = command.substring(1);
        }
        return new Command(author, scriptType.newDataFromString(stringScriptType), command);
    }
}
