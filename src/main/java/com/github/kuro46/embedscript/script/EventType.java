package com.github.kuro46.embedscript.script;

/**
 * @author shirokuro
 */
public enum EventType {
    WALK("WalkScripts.json", "eswalk"),
    INTERACT("InteractScripts.json", "esinteract");

    private final String fileName;
    private final String commandName;

    EventType(String fileName, String commandName) {
        this.fileName = fileName;
        this.commandName = commandName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getCommandName() {
        return commandName;
    }
}
