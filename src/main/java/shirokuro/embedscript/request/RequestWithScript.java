package shirokuro.embedscript.request;

import shirokuro.embedscript.script.EventType;
import shirokuro.embedscript.script.Script;

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
