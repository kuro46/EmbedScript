package shirokuro.embedscript.script.command.data;

import com.google.gson.annotations.JsonAdapter;
import shirokuro.embedscript.script.ScriptType;
import shirokuro.embedscript.script.adapters.CommandDataAdapter;

/**
 * @author shirokuro
 */
@JsonAdapter(CommandDataAdapter.class)
public class ConsoleCommandData extends CommandData {
    public ConsoleCommandData() {
        super(ScriptType.CONSOLE);
    }
}
