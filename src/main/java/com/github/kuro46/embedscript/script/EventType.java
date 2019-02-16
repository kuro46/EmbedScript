package com.github.kuro46.embedscript.script;

/**
 * @author shirokuro
 */
public enum EventType {
    WALK("WalkScripts.json", "eswalk", "@listen-move GROUND "),
    INTERACT("InteractScripts.json", "esinteract", "@listen-click ALL @listen-push ALL ");

    private final String fileName;
    private final String commandName;
    private final String preset;

    EventType(String fileName, String commandName, String preset) {
        this.fileName = fileName;
        this.commandName = commandName;
        this.preset = preset;
    }

    public String getFileName() {
        return fileName;
    }

    public String getCommandName() {
        return commandName;
    }

    public String getPreset() {
        return preset;
    }
}
