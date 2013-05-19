package com.nightfire.webgui.gateway.servicemgr;

import java.io.PrintWriter;
import java.util.HashMap;

import com.nightfire.common.ProcessingException;
import com.nightfire.comms.servicemgr.Notification;
import com.nightfire.comms.servicemgr.ServerManagerNotificationHandler;
import com.nightfire.framework.util.Debug;
import com.nightfire.webgui.gateway.servicemgr.CommandProcessor.PARAMS;

/**
 * Application Controller for Service Manager Admin
 */
public class ServiceMangerAppController 
{
    private ServerManagerNotificationHandler handler 
                    = new ServerManagerNotificationHandler ();
    
    /**
     * processes the command and renders the view on out stream.
     * @param initParams command to execute.
     * @param out out stream on which view needs to be rendered
     * @throws ProcessingException on error
     */
    public void process (HashMap initParams, PrintWriter out) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "Inside application controller process ... params "+initParams);
        
        // create a new commandprocessor
        CommandProcessor commandProcessor = new CommandProcessor(initParams);
        
        Notification notification = null;

        StringBuilder msg = new StringBuilder();
        msg.append("\nCommand Executed :").append(commandProcessor.getCommand());
        if(commandProcessor.getId()!=null)
            msg.append("\n\nID :").append(commandProcessor.getId());
        if(commandProcessor.getServiceType()!=null)
            msg.append("\n\nServiceType :").append(commandProcessor.getServiceType());

        String errorMessage = null;
        try 
        {
            // process the command processor.
            commandProcessor.process();
            notification = new Notification(msg.toString());
        } 
        catch (ProcessingException e) 
        {
            // eat the exception and display it on the admin UI.
            errorMessage = e.getMessage();
            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                Debug.log(Debug.NORMAL_STATUS, "errorMessage :"+errorMessage);

            initParams.put(PARAMS.errormessage.toString(),new String[]{ errorMessage});
            notification = new Notification(msg.toString(),e);
        }

        // handle notifications for start/stop events only.
        if("start".equalsIgnoreCase(commandProcessor.getCommand()) ||
                "stop".equalsIgnoreCase(commandProcessor.getCommand()) )
        {
            handler.handle(notification);
        }

        //render the view.
        String nextView = commandProcessor.getNextView();
        ViewDispatcher dispatcher = new ViewDispatcher(nextView);
        dispatcher.dispatch(out,initParams);
        
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "Done with application controller process...");

    }
    
}
