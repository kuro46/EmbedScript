package com.github.kuro46.embedscript.request;

import com.github.kuro46.embedscript.script.Script;

/**
 * @author shirokuro
 */
public class Request {
    private final RequestType requestType;
    private final Script script;

    public Request(RequestType requestType) {
        this(requestType, null);
    }

    public Request(RequestType requestType, Script script) {
        this.requestType = requestType;
        this.script = script;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public Script getScript() {
        return script;
    }
}
