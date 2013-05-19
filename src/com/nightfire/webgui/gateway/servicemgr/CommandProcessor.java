package com.nightfire.webgui.gateway.servicemgr;

import java.util.HashMap;

import com.nightfire.common.ProcessingException;
import com.nightfire.comms.servicemgr.ServerManagerBase;
import com.nightfire.comms.servicemgr.ServerManagerFactory;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;

public class CommandProcessor 
{
    /**
     * enum to support various types.
     */
    private enum TYPES {manager,service};
    /**
     * enum to support various commands.
     */
    private enum COMMAND {start,stop,status};
    /**
     * enums to support different views.
     */
    public enum NEXTVIEW {managerview, serviceview};
    /**
     * enums to support various parameters.
     */
    public enum PARAMS {command,type,id,servicetype,errormessage};
    
    private String command;
    private String type;
    private String id;
    private String serviceType;
    private String nextView;
    
    
    /**
     * @return view information that is to be displayed after executing the command.
     */
    public String getNextView() 
    {
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "Returing nextView["+nextView+"].");

        return nextView;
    }

    /**
     * creates new instance of type CommandProcessor.
     * @param params parameters 
     */
    public CommandProcessor( HashMap params)
    {
        command = getParameter(params,PARAMS.command.toString());
        
        if(!StringUtils.hasValue(command))
        {
            // defaulting the command to status.
            Debug.log(Debug.NORMAL_STATUS, "Defaulting command to ["+COMMAND.status.toString()+"]");
            command = COMMAND.status.toString();
        }
        
        type = (String) getParameter(params, PARAMS.type.toString());
        
        if(!StringUtils.hasValue(type))
        {
            // defaulting the type as manger.
            Debug.log(Debug.NORMAL_STATUS, "Defaulting TYPE to ["+TYPES.manager.toString()+"]");
            type = TYPES.manager.toString();
        }
        
        id = getParameter(params, PARAMS.id.toString());
        
        serviceType = getParameter(params,PARAMS.servicetype.toString());
    }
    
    /**
     * gets the parameter value from the Map
     * @param params Map containing <name,String[]value> pair 
     * @param paramName parameter name.
     * @return parameter value as String
     */
    public static String getParameter(HashMap params, String paramName) 
    {
        String[] values = (String[])params.get(paramName);
        if(values==null || values.length==0)
        {
            return null;
        }
        return values[0];
    }

    // proccess called for manager.
    private void managerProcess(String command, String id) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "Inside Manager Process command ["+command+"] and id ["+id+"]");

        ServerManagerFactory serverFactory = ServerManagerFactory.getInstance();

        setNextView(NEXTVIEW.managerview.toString());
        
        if(COMMAND.start.toString().equalsIgnoreCase(command))
        {
            serverFactory.start(id,null);
        }
        else if(COMMAND.stop.toString().equalsIgnoreCase(command))
        {
            serverFactory.stop(id);
        }
        else if(COMMAND.status.toString().equalsIgnoreCase(command))
        {
            // do nothing.
        }
        else
        {
            if(Debug.isLevelEnabled(Debug.MSG_ERROR))
                Debug.log(Debug.MSG_ERROR, "Command ["+command+"] Not Supported.");

            throw new ProcessingException(command +" command Not Supported.");
        }
        
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "Exiting manager process ...");
        
    }
    
    // process called for service.
    private void serviceProcess(String command,String serviceType, String id) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "Inside Service Process command["+command+"] , serviceType["+serviceType+"] and id["+id+"]");

        ServerManagerFactory serverFactory = ServerManagerFactory.getInstance();
        ServerManagerBase serverManager = serverFactory.getServerManager(serviceType);
        
        HashMap params = new HashMap();
        params.put(PARAMS.id.toString(),id);
        
        setNextView(NEXTVIEW.serviceview.toString());
        
        if(COMMAND.start.toString().equalsIgnoreCase(command))
        {
            serverManager.start(params);
        }
        else if(COMMAND.stop.toString().equalsIgnoreCase(command))
        {
            serverManager.stop(params);
        }
        else if(COMMAND.status.toString().equalsIgnoreCase(command))
        {
            // do nothing.
        }
        else
        {
            if(Debug.isLevelEnabled(Debug.MSG_ERROR))
                Debug.log(Debug.MSG_ERROR, "Command ["+command+"] Not Supported");

            throw new ProcessingException(command +" command Not Supported.");
        }
        
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "Exiting Service Process...");

    }
    
    /**
     * process the command.
     * @throws ProcessingException on error
     */
    public void process()throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "Inside Command Processor process..");

        if(StringUtils.hasValue(command) && StringUtils.hasValue(type))
        {
            if(type.equalsIgnoreCase(TYPES.manager.toString()))
            {
                managerProcess(command,id);
            }
            else if(type.equalsIgnoreCase(TYPES.service.toString()))
            {
                serviceProcess(command,serviceType,id);
            }
            else
            {
                if(Debug.isLevelEnabled(Debug.MSG_ERROR))
                    Debug.log(Debug.MSG_ERROR, "Invalid Type type:"+type);

                throw new ProcessingException("Invalid type param :type["+type+"]");
            }
        }
        else
        {
            Debug.log(Debug.MSG_ERROR, "Invalid Params: command ["+command+"],type ["+type+"],id["+id+"]");
            throw new ProcessingException("Could not process, since one or more parameter is invalid");
        }
        
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "Exiting Command Processor process ...");

    }

    /**
     * sets the next view information.
     * @param nextView as String
     */
    public void setNextView(String nextView) 
    {
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "Setting NextView["+nextView+"]");

        this.nextView = nextView;
    }

    /**
     * get the current command
     * @return command as String
     */
    public String getCommand() {
        return command;
    }

    /**
     * get the service id associated
     * of the current command
     * @return id as String
     */
    public String getId() {
        return id;
    }

    /**
     * get the service type associated
     * with the current command
     * @return serviceType as String
     */
    public String getServiceType() {
        return serviceType;
    }

    /**
     * get the server type associated
     * with the current command
     * @return type as String
     */
    public String getType() {
        return type;
    }
}
