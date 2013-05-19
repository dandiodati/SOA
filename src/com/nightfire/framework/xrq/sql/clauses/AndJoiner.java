package com.nightfire.framework.xrq.sql.clauses;


import org.w3c.dom.*;

import com.nightfire.framework.xrq.*;
import com.nightfire.framework.message.MessageException;
import java.util.*;


/**
 * joins sub clauses to perform the logical AND of clauses.
 */
public class AndJoiner extends ClauseJoinerBase
{


  /**
   * returns a type of join.
   */
   protected String getClauseSeparator()
   {
     return ("and");
   }

}
