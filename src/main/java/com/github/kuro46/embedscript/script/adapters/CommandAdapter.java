package com.github.kuro46.embedscript.script.adapters;

import com.github.kuro46.embedscript.GsonHolder;
import com.github.kuro46.embedscript.script.command.Command;
import com.github.kuro46.embedscript.script.command.data.CommandData;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.UUID;

/**
 * @author shirokuro
 */
public class CommandAdapter extends TypeAdapter<Command> {
    @Override
    public void write(JsonWriter out, Command value) throws IOException {
        out.beginObject();
        out.name("author").value(value.getAuthor().toString());
        out.name("command").value(value.getCommand());
        out.name("data").jsonValue(GsonHolder.get().toJson(value.getData()));
        out.endObject();
    }

    @Override
    public Command read(JsonReader in) throws IOException {
        Type dataType = new TypeToken<CommandData>() {
        }.getType();

        UUID author = null;
        String command = null;
        CommandData data = null;

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "author":
                    author = UUID.fromString(in.nextString());
                    break;
                case "command":
                    command = in.nextString();
                    break;
                case "data":
                    data = GsonHolder.get().fromJson(in, dataType);
                    break;
                default:
                    in.skipValue();
            }
        }
        in.endObject();

        if (author == null || command == null || data == null) {
            throw new JsonSyntaxException("Illegal syntax");
        }
        return new Command(author, data, command);
    }
}
