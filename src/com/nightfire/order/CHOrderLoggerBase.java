package com.nightfire.order;

import java.util.HashMap;
import java.util.LinkedList;

import biz.neustar.nsplatform.db.pojo.POJOBase;

import com.nightfire.framework.util.Debug;
import com.nightfire.order.cfg.CommandCfg;
import com.nightfire.order.cfg.OrderLoggerCfg;
import com.nightfire.order.common.CHOrderContext;
import com.nightfire.order.common.CommandChain;
import com.nightfire.order.utils.CHOrderException;

/**
 * Abstract Base class for different CHOrderLoggers. Provides basic functionality
 * for all the different product specific loggers.
 * 
 * @author Abhishek Jain
 */
public abstract class CHOrderLoggerBase 
{
    /**
     * stores all the different POJOs associated to this logger.  
     */
    protected HashMap<String, POJOBase> pojoMap = new HashMap<String, POJOBase>();
    /**
     * chain of commands to be executed. 
     */
    private CommandChain commandChain ;

    public static enum POJOTYPE {Order,Event,Trans};

    public CHOrderLoggerBase(OrderLoggerCfg config, CHOrderContext context) 
    throws CHOrderException
    {
        Debug.log(Debug.MSG_STATUS, "CHOrderLoggerBase: Constructing Base with config ["+config.toString()+"].");
        initialize(config,context);
        Debug.log(Debug.MSG_STATUS, "CHOrderLoggerBase: Done Construction.");
    }
    /**
     * abstract method to initialize POJO Map with respective types for Order, 
     * Event & Trans order tables. 
     */
    protected abstract void initializePOJOMap(CHOrderContext context) throws CHOrderException;

    /**
     * abstract method to get new pojo object of given type,
     */
    public abstract POJOBase getNewPOJO(CHOrderContext context, String pojoType ) throws CHOrderException;

    /**
     * initialize the order logger based on configuration and context information.
     * @param config OrderLoggerCfg type
     * @param context CHOrderContext type
     */
    protected void initialize(OrderLoggerCfg config, CHOrderContext context) 
    throws CHOrderException
    {
        Debug.log(Debug.MSG_STATUS, "CHOrderLoggerBase: Inside Initialize ->["+config.toString()+"].");
        // sets the respective POJO types.
        initializePOJOMap(context);
        // sets the respective command chain based on the context params.
        LinkedList<CommandCfg> commandCfgList = new LinkedList<CommandCfg>(config.getCommandList(context));
        commandChain = new CommandChain(commandCfgList);

        Debug.log(Debug.MSG_STATUS, "CHOrderLoggerBase: Done with Initialization");
    }
    /**
     * Logs the order details as per order configurations.
     * @param context context information of type CHOrderContext.
     */
    public void log(CHOrderContext context) throws CHOrderException
    {
        // process the command chain
        this.commandChain.process(context, this.pojoMap);
    }
}
