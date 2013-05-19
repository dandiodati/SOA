package com.nightfire.order.common;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.nightfire.order.cfg.CommandCfg;
import com.nightfire.framework.util.Debug;
import com.nightfire.order.utils.CHOrderException;

import biz.neustar.nsplatform.db.pojo.POJOBase;

/**
 * Models a chain of command and process all command sequentially.
 * @author Abhishek Jain
 */
public class CommandChain 
{
    /**
     * List of commands stored as linked list, since order needs to be
     * preserved for sequential execution of these commands. 
     */
    private LinkedList<Command> commandList = new LinkedList<Command>();
    /**
     * configure the command chain using CommandListConfig type.
     * @param commandListConf
     */
    public CommandChain (List<CommandCfg> commandListConf)
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "CommandChain: Constructor commandListConf["+commandListConf+"]");
        // process all the different command configuration one by one
        for (Iterator iterator = commandListConf.iterator(); iterator.hasNext();) 
        {
            CommandCfg commandCfg = (CommandCfg) iterator.next();
            // create a new command object with appropriate pojotype and method.
            Command command = new Command(commandCfg);
            
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))            
                Debug.log(Debug.MSG_STATUS, "CommandChain -> Adding Command with CommandCfg["+commandCfg+"].");
            // add the command to command chain;
            addCommand(command);
        }
        
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "CommandChain: Done Constructing CommandChain");
    }
    /**
     * execute commands sequentially and gets the input from context and sets output.
     * @param context CommandContext 
     * @param pojoMap Map containing pojos for different types.
     */
    public void process (CHOrderContext context, HashMap<String, POJOBase> pojoMap)
    throws CHOrderException
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "CommandChain: Inside process ...");

        // execute each command sequentially.
        for (Iterator iterator = commandList.iterator(); iterator.hasNext();) 
        {
            Command command = (Command) iterator.next();
            
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, "CommandChain -> Executing Command["+command+"].");
            
            command.execute(context, pojoMap);
        }

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "CommandChain: Done Processing CommandChain.");
    }

    /**
     * adds a new command to command list.
     * @param command new command to be added to chain.
     */
    public void addCommand(Command command)
    {
        commandList.add(command);
    }
}
