package shirokuro.embedscript.script.command.data;

import com.google.gson.annotations.JsonAdapter;
import shirokuro.embedscript.script.ScriptType;
import shirokuro.embedscript.script.adapters.CommandDataAdapter;

/**
 * @author shirokuro
 */
@JsonAdapter(CommandDataAdapter.class)
public abstract class CommandData {
    protected final ScriptType type;

    public CommandData(ScriptType type) {
        this.type = type;
    }

    public ScriptType getType() {
        return type;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "type=" + type +
            '}';
    }
}
