package shirokuro.embedscript.request;

import shirokuro.embedscript.script.EventType;

/**
 * @author shirokuro
 */
public class Request {
    private final RequestType requestType;
    private final EventType eventType;

    public Request(RequestType requestType, EventType eventType) {
        this.requestType = requestType;
        this.eventType = eventType;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public EventType getEventType() {
        return eventType;
    }
}
