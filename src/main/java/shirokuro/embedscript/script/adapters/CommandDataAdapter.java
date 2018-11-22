package shirokuro.embedscript.script.adapters;

import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import shirokuro.embedscript.script.ScriptType;
import shirokuro.embedscript.script.command.data.BypassPermCommandData;
import shirokuro.embedscript.script.command.data.CommandData;

import java.io.IOException;

/**
 * @author shirokuro
 */
public class CommandDataAdapter extends TypeAdapter<CommandData> {
    @Override
    public void write(JsonWriter out, CommandData value) throws IOException {
        out.beginObject();
        ScriptType type = value.getType();
        out.name("type").value(type.name());
        if (type == ScriptType.BYPASS_PERMISSION) {
            out.name("permission").value(((BypassPermCommandData) value).getPermission());
        }
        out.endObject();
    }

    @Override
    public CommandData read(JsonReader in) throws IOException {
        ScriptType type = null;
        String permission = null;

        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case "type":
                    type = ScriptType.valueOf(in.nextString());
                    break;
                case "permission":
                    permission = in.nextString();
                    break;
                default:
                    in.skipValue();
            }
        }
        in.endObject();

        if (type == null)
            throw new JsonSyntaxException("Key: \"type\" not found.");
        return type.newDataFromString(type.getString() + (permission == null ? "" : permission));
    }
}
