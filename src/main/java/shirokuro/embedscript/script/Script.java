package shirokuro.embedscript.script;

import com.google.gson.annotations.JsonAdapter;
import shirokuro.embedscript.script.adapters.ScriptAdapter;
import shirokuro.embedscript.script.command.Command;

import java.util.List;

/**
 * @author shirokuro
 */
@JsonAdapter(ScriptAdapter.class)
public class Script {
    private final List<Command> commands;

    public Script(List<Command> commands) {
        this.commands = commands;
    }

    public List<Command> getCommands() {
        return commands;
    }

    @Override
    public String toString() {
        return "Script{" +
            "commands=" + commands +
            '}';
    }
}
