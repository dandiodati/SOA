package com.nightfire.comms.servicemgr;

import com.nightfire.framework.util.Debug;

/**
 * 
 * @author hpirosha
 *
 */
public class Notification 
{

    private String event;
    private Throwable throwable;
    
    /**
     * Instantiate a notification with string containing details about the event. 
     * @param event
     */
    public Notification(String event)
    {
        this.event = event;    
    }
    
    /**
     * Instantiate a notification with string containing details about the event
     * and the error/exception that is associated with the event.
     * @param event
     * @param throwable
     */
    public Notification(String event,Throwable throwable)
    {
        this.event = event;
        this.throwable = throwable;
    }

    public String getEvent() {
        return event;
    }

    public Throwable getThrowable() {
        return this.throwable;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(event);

        if(throwable!=null){
            sb.append("  ");
            sb.append(throwable.getMessage());
            sb.append("  ");
            sb.append(Debug.getStackTrace(throwable));
        }
        
        return sb.toString();
    }
}
