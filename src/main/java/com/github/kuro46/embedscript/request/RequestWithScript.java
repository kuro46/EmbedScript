package com.github.kuro46.embedscript.request;

import com.github.kuro46.embedscript.script.EventType;
import com.github.kuro46.embedscript.script.Script;

/**
 * @author shirokuro
 */
public class RequestWithScript extends Request {
    private final Script script;

    public RequestWithScript(RequestType requestType, EventType eventType, Script script) {
        super(requestType, eventType);
        this.script = script;
    }

    public Script getScript() {
        return script;
    }
}
