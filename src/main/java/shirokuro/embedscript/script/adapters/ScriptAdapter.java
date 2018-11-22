package shirokuro.embedscript.script.adapters;

import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import shirokuro.embedscript.GsonHolder;
import shirokuro.embedscript.script.Script;
import shirokuro.embedscript.script.command.Command;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @author shirokuro
 */
public class ScriptAdapter extends TypeAdapter<Script> {
    @Override
    public void write(JsonWriter out, Script value) throws IOException {
        out.beginArray();
        for (Command command : value.getCommands()) {
            out.jsonValue(GsonHolder.get().toJson(command));
        }
        out.endArray();
    }

    @Override
    public Script read(JsonReader in) throws IOException {
        Type commandType = new TypeToken<Command>() {
        }.getType();

        List<Command> commands = new ArrayList<>();

        in.beginArray();
        while (in.hasNext()) {
            commands.add(GsonHolder.get().fromJson(in, commandType));
        }
        in.endArray();

        return new Script(commands);
    }
}
