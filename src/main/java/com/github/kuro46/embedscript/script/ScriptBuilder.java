package com.github.kuro46.embedscript.script;

import org.apache.commons.lang.ArrayUtils;

import java.util.UUID;

public class ScriptBuilder {
    private final UUID author;
    private Script.MoveType[] moveTypes = new Script.MoveType[0];
    private Script.ClickType[] clickTypes = new Script.ClickType[0];
    private Script.PushType[] pushTypes = new Script.PushType[0];
    private String[] permissionsToGive = new String[0];
    private String[] permissionsToNeeded = new String[0];
    private String[] permissionsToNotNeeded = new String[0];
    private Script.ActionType[] actionTypes = new Script.ActionType[0];
    private String[] actions = new String[0];

    public ScriptBuilder(UUID author) {
        this.author = author;
    }

    public ScriptBuilder withMoveTypes(Script.MoveType[] moveTypes) {
        this.moveTypes = moveTypes;
        return this;
    }

    public ScriptBuilder withClickTypes(Script.ClickType[] clickTypes) {
        this.clickTypes = clickTypes;
        return this;
    }

    public ScriptBuilder withPushTypes(Script.PushType[] pushTypes) {
        this.pushTypes = pushTypes;
        return this;
    }

    public ScriptBuilder withPermissionsToGive(String[] permissionsToGive) {
        this.permissionsToGive = permissionsToGive;
        return this;
    }

    public ScriptBuilder withPermissionsToNeeded(String[] permissionsToNeeded) {
        this.permissionsToNeeded = permissionsToNeeded;
        return this;
    }

    public ScriptBuilder withPermissionsToNotNeeded(String[] permissionsToNotNeeded) {
        this.permissionsToNotNeeded = permissionsToNotNeeded;
        return this;
    }

    public ScriptBuilder withActionTypes(Script.ActionType[] actionTypes) {
        this.actionTypes = actionTypes;
        return this;
    }

    public ScriptBuilder withActions(String[] actions) {
        this.actions = actions;
        return this;
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
            permissionsToNeeded,
            permissionsToNotNeeded,
            actionTypes,
            actions);
    }
}
