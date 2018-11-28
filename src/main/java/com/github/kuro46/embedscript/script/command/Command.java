package com.github.kuro46.embedscript.script.command;

import com.github.kuro46.embedscript.script.adapters.CommandAdapter;
import com.github.kuro46.embedscript.script.command.data.CommandData;
import com.google.gson.annotations.JsonAdapter;

import java.util.UUID;

/**
 * @author shirokuro
 */
@JsonAdapter(CommandAdapter.class)
public class Command {
    private final UUID author;
    private final CommandData data;
    private final String command;

    public Command(UUID author, CommandData data, String command) {
        this.author = author;
        this.data = data;
        this.command = command;
    }

    public UUID getAuthor() {
        return author;
    }

    public CommandData getData() {
        return data;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return "Command{" +
            "author=" + author +
            ", data=" + data +
            ", command='" + command + '\'' +
            '}';
    }
}
