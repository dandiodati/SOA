package com.nightfire.order.common;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import com.nightfire.order.cfg.CommandCfg;
import com.nightfire.framework.util.Debug;
import com.nightfire.order.utils.CHOrderException;
import com.nightfire.order.utils.CHOrderUtils;

import biz.neustar.nsplatform.db.pojo.POJOBase;

/**
 * This class models a command object. Command object is responsible for
 * executing the action i.e. one of [CRUD] operation on the associated POJO. 
 * 
 * @author Abhishek Jain
 */
public class Command 
{
    /**
     * Type of pojo to which this command is associated.
     */
    private String pojoType;
    /**
     * Method that needs to be executed on the respective pojo type.
     */
    private String pojoMethod;
    /**
     * createNewPojo flag having value either true or false.
     * If true: create new pojo object.
     * If false: Use pojo object from pojo list.
     */
    private Boolean createNewPojo;
    /**
     * conditions on which this command will be executed.
     */

    private LinkedList<CommandCondition> commandConditions = new LinkedList<CommandCondition>();
    /**
     * Input parameter that needs to be fetched from the context and 
     * that needs to be set on the business objects i.e. on respective POJOs.
     * NOTE: key = from+"|"+to
     */
    private HashMap<String, Param> inputParams = new HashMap<String, Param>();
    /**
     * Output parameters that are generated after business execution.
     * NOTE: key = from+"|"+to
     */
    private HashMap<String, Param> outputParams = new HashMap<String, Param>();
    /**
     * Name of the command specified in command configuration.  
     */
    private String name;
    /**
     * constructs a command object using CommandConfig type. 
     * @param config command configuration of type CommandConfig 
     */
    public Command(CommandCfg config)
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "Command: Inside Constructor with CommandCfg["+config.toString()+"]");
    
        this.name = config.getName();
        this.setCommandConditions(config.getCmdConditions());
        // create input parameters and attach it to command.
        this.setInParams(config.getInParams());
        // create output parameters and attach it to command.
        this.setOutParams(config.getOutParams());
        // sets the pojo type and method information.
        this.pojoMethod = config.getMethod();
        // sets createNewPojo whether new pojo needs to be created or use existing pojo object from pojo list.
        this.createNewPojo = config.getCreateNewPojo();

        this.pojoType = config.getPojoType();
        
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))        
            Debug.log(Debug.MSG_STATUS, "Command: Constructor Done .. ");
    }

    private void setCommandConditions(Map<String, String> cmdConditions)
    {
        if(cmdConditions == null)
            return;
        
        Set<String> keys = cmdConditions.keySet();
        
        for (Iterator iterator = keys.iterator(); iterator.hasNext();)
        {
            String name = (String) iterator.next();
            String value = (String) cmdConditions.get(name);
            commandConditions.add(new CommandCondition(name,value));
        }
        
    }

    /**
     * executes the command on the respective POJOBase and return the out params
     * in form of HashMap.
     * @param context of type CHOrderContext. Placeholder for different input and out params.
     * @return out values in form of HashMap.
     */
    public void execute (CHOrderContext context, HashMap<String, POJOBase> pojoMap) 
    throws CHOrderException	
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "Command: Inside execute.");

        // check if command has to be executed.
        if( ! testCommandCondition(context) )
        {
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, "Command: Command Condition failed. returning ...");
            return;
        }
        // get the correct pojo from the Pojo List.
        POJOBase pojoObject = pojoMap.get(pojoType);

       // Check whether new pojo object needs to be initilaized.
       // If true: Override pojoObject variable by newly created pojo corresponding to pojoType.
       // If false: Use pojo object from pojoMap itself.
        if(createNewPojo){

            Debug.log(Debug.MSG_STATUS, "Command: Command attribute 'create_new_pojo' value is ["+ createNewPojo +"], " +
                    "hence create new pojo before executing ["+ pojoMethod +"] method,");
            pojoObject = context.getOrderLogger().getNewPOJO(context, pojoType);
        }

        // and then perform the preExecution operations.
        preExecute(context, pojoObject);
        // perform the business method on the respective pojo.
        CHOrderUtils.performOperation(pojoObject,pojoMethod,context);
        // finally perform the postExecution operations.
        postExecute(context, pojoObject);
        
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))        
            Debug.log(Debug.MSG_STATUS, "Command: Done Execution .. ");
    }

    /**
     * checks the condition whether or not to execute this command.
     * @param context context containing all the parameters
     * @return true if all the condition cases validates to true else return false.
     */
    private boolean testCommandCondition(CHOrderContext context)
    {
        if(commandConditions.size() == 0)
            return true;
        
        boolean result = true;
        for (Iterator iterator = commandConditions.iterator(); iterator.hasNext();)
        {
            CommandCondition cmdCondition = (CommandCondition) iterator.next();
            // if any of the condition fails return false
            if(!cmdCondition.evaluate(context))
                return false;
        }
        // since all condition has validated successfully return true.. 
        return result;
    }

    /**
     * perform all the post operation on the business object. Copies all the 
     * out parameters list and sets the value and return in form of Map.
     * @param businessPOJO type POJOBase on which operation is to be performed.
     * 
     */
    protected void postExecute(CHOrderContext context,POJOBase businessPOJO) 
    throws CHOrderException
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "Command: Inside postExecute ..");

        // populates all the input parameters from the context.
        for (Iterator iterator = this.outputParams.values().iterator(); iterator.hasNext();) 
        {
            // get out parameter 
            Param param = (Param) iterator.next();
            // get the value of parameter from the respective POJO
            String value = CHOrderUtils.getPOJOAttribute(businessPOJO,param.getFrom());

            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, "Command: postExecute -> setting POJO["+businessPOJO.getClass().getSimpleName()+"] " +
                    "attr ["+param.getTo()+"=>"+value+"].");

            // set the value to context.
            context.setAttribute(param.getTo(), value);
        }
        
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "Command: Done postExecute ..");
    }

    /**
     * perform all the pre execution operation on the business objects. It copies all the 
     * input parameters in the business objects.
     * @param businessPOJO type POJOBase on which operation is to be performed.
     */
    protected void preExecute(CHOrderContext context,POJOBase businessPOJO) 
    throws CHOrderException
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "Command: preExecute ..");
        // populates all the input parameters from the context.
        for (Iterator iterator = this.inputParams.values().iterator(); iterator.hasNext();) 
        {
            Param param = (Param) iterator.next();
            // get the value from the context
            //String value = (String) context.getAttribute(param.getFrom());
            Object value = context.getAttribute(param.getFrom());
            if(value !=null)
            {
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log(Debug.MSG_STATUS, "Command: preExecute ->" );

                // set the value to the business pojo
                CHOrderUtils.setPOJOAttribute(businessPOJO,param.getTo(),value);
            }
        }
        
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "Command: Done preExecute ..");
    }

    /**
     * sets the out parameters using the map
     * @param inParams
     */
    public void setInParams(Map inParams) 
    {
        if(inParams == null)
            return;

        HashSet keys = new HashSet(inParams.keySet());
        for (Iterator iterator = keys.iterator(); iterator.hasNext();) 
        {
            String from = (String) iterator.next();
            String to = (String)inParams.get(from);
            Param param = new Param(from,to);
            this.inputParams.put(from+"|"+to, param);
        }
    }

    /**
     * sets the out parameters using the map
     * @param outParams
     */
    public void setOutParams(Map outParams) 
    {
        if(outParams == null)
            return ;

        HashSet keys = new HashSet(outParams.keySet());
        for (Iterator iterator = keys.iterator(); iterator.hasNext();) 
        {
            String from = (String) iterator.next();
            String to = (String)outParams.get(from);
            Param param = new Param(from,to);
            this.outputParams.put(from+"|"+to, param);
        }	
    }
    
    /**
     * @override 
     */
    public String toString()
    {
        return name;
    }
}
