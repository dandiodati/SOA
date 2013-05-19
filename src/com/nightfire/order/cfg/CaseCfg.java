package com.nightfire.order.cfg;


import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Class containing configuration of a case.
 * A case is selected for execution if its associated
 * condition evaulates to <code>true</code>. 
 * @author hpirosha
 */
public class CaseCfg 
{
    private Condition condition;
    private List<CommandCfg> cmdCfgLst;
    private String desc;

    public CaseCfg(String desc,Condition condition)
    {
        cmdCfgLst = new LinkedList<CommandCfg>();
        this.condition = condition;
        this.desc = desc;
    }

    /**
     * Add a command cfg to this case. 
     * @param cmd
     */
    public void addCommand(CommandCfg cmd)
    {
        cmdCfgLst.add(cmd);
    }

    /**
     * Get the associated description with this cfg object.
     * @return String
     */public String getDesc()
     {
         return desc;
     }

     /**
      * Get the associated condition config with this object.  
      * @return Condition
      */
     public Condition getCondition()
     {
         return condition;
     }

     /**
      * Get the list of commands; these are in the same order 
      * as they are specified in configuration file.  
      * @return
      */
     public List<CommandCfg> getCommandCfgLst()
     {
         return Collections.unmodifiableList(cmdCfgLst);
     }

     public String toString()
     {
         StringBuffer sb = new StringBuffer();
         sb.append("[ Case desc=:").append(desc).append("]");

         return sb.toString();
     }
}

