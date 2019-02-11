package com.github.kuro46.embedscript.script.adapters;

import com.github.kuro46.embedscript.script.ScriptPosition;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * @author shirokuro
 */
public class ScriptBlockAdapter extends TypeAdapter<ScriptPosition> {
    @Override
    public void write(JsonWriter out, ScriptPosition value) throws IOException {
        out.beginObject();
        out.name("world").value(value.getWorld());
        out.name("x").value(value.getX());
        out.name("y").value(value.getY());
        out.name("z").value(value.getZ());
        out.endObject();
    }

    @Override
    public ScriptPosition read(JsonReader in) throws IOException {
        String world = null;
        Integer x = null;
        Integer y = null;
        Integer z = null;

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "world":
                    world = in.nextString();
                    break;
                case "x":
                    x = in.nextInt();
                    break;
                case "y":
                    y = in.nextInt();
                    break;
                case "z":
                    z = in.nextInt();
                    break;
                default:
                    in.skipValue();
            }
        }
        in.endObject();

        if (world == null || x == null || y == null || z == null) {
            throw new JsonSyntaxException("Illegal syntax");
        }
        return new ScriptPosition(world, x, y, z);
    }
}
