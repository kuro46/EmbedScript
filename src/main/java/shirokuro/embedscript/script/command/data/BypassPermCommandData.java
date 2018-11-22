package shirokuro.embedscript.script.command.data;

import com.google.gson.annotations.JsonAdapter;
import shirokuro.embedscript.script.ScriptType;
import shirokuro.embedscript.script.adapters.CommandDataAdapter;

/**
 * @author shirokuro
 */
@JsonAdapter(CommandDataAdapter.class)
public class BypassPermCommandData extends CommandData {
    private final String permission;

    public BypassPermCommandData(String permission) {
        super(ScriptType.BYPASS_PERMISSION);
        this.permission = permission;
    }

    public String getPermission() {
        return permission;
    }

    @Override
    public String toString() {
        return "BypassPermCommandData{" +
            "permission='" + permission + '\'' +
            ", type=" + type +
            '}';
    }
}
