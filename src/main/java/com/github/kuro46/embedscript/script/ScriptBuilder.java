package com.github.kuro46.embedscript.script;

import org.apache.commons.lang.ArrayUtils;

import java.util.Arrays;
import java.util.UUID;

public class ScriptBuilder {
    private final UUID author;
    private Script.MoveType[] moveTypes;
    private Script.ClickType[] clickTypes;
    private Script.PushType[] pushTypes;
    private String[] permissionsToGive;
    private String[] neededPermissions;
    private String[] unneededPermissions;
    private Script.ActionType[] actionTypes;
    private String[] actions;

    public ScriptBuilder(UUID author) {
        this.author = author;
        this.moveTypes = new Script.MoveType[0];
        this.clickTypes = new Script.ClickType[0];
        this.pushTypes = new Script.PushType[0];
        String[] emptyStringArray = new String[0];
        this.permissionsToGive = emptyStringArray;
        this.neededPermissions = emptyStringArray;
        this.unneededPermissions = emptyStringArray;
        this.actionTypes = new Script.ActionType[0];
        this.actions = emptyStringArray;
    }

    public ScriptBuilder(Script script) {
        this.author = script.getAuthor();
        this.moveTypes = script.getMoveTypes().toArray(new Script.MoveType[0]);
        this.clickTypes = script.getClickTypes().toArray(new Script.ClickType[0]);
        this.pushTypes = script.getPushTypes().toArray(new Script.PushType[0]);
        this.permissionsToGive = script.getPermissionsToGive().toArray(new String[0]);
        this.neededPermissions = script.getNeededPermissions().toArray(new String[0]);
        this.unneededPermissions = script.getUnneededPermissions().toArray(new String[0]);
        this.actionTypes = script.getActionTypes().toArray(new Script.ActionType[0]);
        this.actions = script.getActions().toArray(new String[0]);
    }

    public ScriptBuilder withMoveTypes(Script.MoveType[] moveTypes) {
        this.moveTypes = Arrays.copyOf(moveTypes, moveTypes.length);
        return this;
    }

    public ScriptBuilder withClickTypes(Script.ClickType[] clickTypes) {
        this.clickTypes = Arrays.copyOf(clickTypes, clickTypes.length);
        return this;
    }

    public ScriptBuilder withPushTypes(Script.PushType[] pushTypes) {
        this.pushTypes = Arrays.copyOf(pushTypes, pushTypes.length);
        return this;
    }

    public ScriptBuilder withPermissionsToGive(String[] permissionsToGive) {
        this.permissionsToGive = copyStringArray(permissionsToGive);
        return this;
    }

    public ScriptBuilder withNeededPermissions(String[] permissionsToNeeded) {
        this.neededPermissions = copyStringArray(permissionsToNeeded);
        return this;
    }

    public ScriptBuilder withUnneededPermissions(String[] permissionsToNotNeeded) {
        this.unneededPermissions = copyStringArray(permissionsToNotNeeded);
        return this;
    }

    public ScriptBuilder withActionTypes(Script.ActionType[] actionTypes) {
        this.actionTypes = Arrays.copyOf(actionTypes, actionTypes.length);
        return this;
    }

    public ScriptBuilder withActions(String[] actions) {
        this.actions = copyStringArray(actions);
        return this;
    }

    public UUID getAuthor() {
        return author;
    }

    public Script.MoveType[] getMoveTypes() {
        return Arrays.copyOf(moveTypes, moveTypes.length);
    }

    public Script.ClickType[] getClickTypes() {
        return Arrays.copyOf(clickTypes, clickTypes.length);
    }

    public Script.PushType[] getPushTypes() {
        return Arrays.copyOf(pushTypes, pushTypes.length);
    }

    public String[] getPermissionsToGive() {
        return copyStringArray(permissionsToGive);
    }

    public String[] getNeededPermissions() {
        return copyStringArray(neededPermissions);
    }

    public String[] getUnneededPermissions() {
        return copyStringArray(unneededPermissions);
    }

    public Script.ActionType[] getActionTypes() {
        return Arrays.copyOf(actionTypes, actionTypes.length);
    }

    public String[] getActions() {
        return copyStringArray(actions);
    }

    private String[] copyStringArray(String[] array) {
        return Arrays.copyOf(array, array.length);
    }

    public Script build() throws ParseException {
        if (ArrayUtils.isEmpty(actionTypes) || ArrayUtils.isEmpty(actions)) {
            throw new ParseException("ActionType or Action is empty!");
        }

        if (ArrayUtils.isEmpty(moveTypes) && ArrayUtils.isEmpty(clickTypes) && ArrayUtils.isEmpty(pushTypes)) {
            throw new ParseException("MoveType, ClickType or PushType is must be specified");
        }

        return new Script(author,
            moveTypes,
            clickTypes,
            pushTypes,
            permissionsToGive,
            neededPermissions,
            unneededPermissions,
            actionTypes,
            actions);
    }
}
