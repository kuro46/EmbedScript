package com.github.kuro46.embedscript.script;

/**
 * @author shirokuro
 */
public enum EventType {
    WALK("WalkScripts.json", "eswalk", "walk"),
    INTERACT("InteractScripts.json", "esinteract", "interact");

    private final String fileName;
    private final String commandName;
    private final String presetName;

    EventType(String fileName, String commandName, String presetName) {
        this.fileName = fileName;
        this.commandName = commandName;
        this.presetName = presetName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getCommandName() {
        return commandName;
    }

    public String getPresetName() {
        return presetName;
    }
}
