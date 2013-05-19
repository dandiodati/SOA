package com.nightfire.comms.servicemgr;

/**
 * Class to process error/exception conditions while an operation 
 * is being performed by a manager component.   
 * 
 * The idea is not to break any execution flow while an operation 
 * is being performed but take necessary action for the error/exception 
 * to get noticed.  
 * 
 * @author hpirosha
 */
public class ServerManagerNotificationHandler 
{

    public ServerManagerNotificationHandler()
    {
        
    }
    
    /**
     * This method sends an email using service manager
     * email notifier. 
     * 
     * @param notification
     */
    public void handle(Notification notification)
    {
        if(notification!=null)
            ServerManagerEmailNotifier.getInstance().sendNotification(notification.toString());
    }
}
