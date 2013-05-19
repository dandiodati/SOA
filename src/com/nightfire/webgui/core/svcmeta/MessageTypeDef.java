/*
 * Copyright(c) 2002 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.svcmeta;

// jdk imports
import java.util.*;

// third-party imports
import org.w3c.dom.*;

// nightfire imports
import com.nightfire.framework.message.util.xml.DOMWriter;
import com.nightfire.framework.message.util.xml.ParsedXPath;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.webgui.core.svcmeta.*;
import com.nightfire.framework.debug.*;

/**
 * Represents the message type associated with one or more xml documents.
 * This class is able to determine if a document is an instance of 
 * a specific message type.
 */
public class MessageTypeDef
{
    /** List of parsedXPath expressions that return a boolean */
    private List conditions;

    /** message type associated with this action */
    protected String name;

    /** display name associated with this action */
    protected String displayName;

    private DebugLogger log;
  

 
  
    /**
     * Constructor
     */
    public MessageTypeDef()
    {
      log = DebugLogger.getLoggerLastApp(getClass() );
      conditions = new ArrayList();
    }

    /**
     * Returns the Message Type name (matches the name used by the manager)
     */
    public String getName()
    {
      return name;
    }

    /**
     * Returns the display name for the component
     */
    public String getDisplayName()
    {
        return displayName;
    }

 
  /**
   * Determines if a document matches the message type associated 
   * with this instance.
   *
   * @param doc The xml document to test
   * @return Returns true if the document is an instance of this
   * specific message type, else false.
   */
  public boolean isCurrentType(Document doc)
    {
     
      Iterator iter = conditions.iterator();
     
      while (iter.hasNext()) {
        ParsedXPath xPath = (ParsedXPath) iter.next();
        try {            
            if (xPath.getBooleanValue(doc) == true)
                {
                    log.debug("MessageTypeDef.isCurrentType : Found an xpath match for the message type");
                    return true;
                }
            else
                log.debug("MessageTypeDef.isCurrentType : Didn't find a match");
        }
        catch (FrameworkException e) {
          log.warn("MessageTypeDef [" + name + "] Failed to evaluate xpath boolean expression, returning false : " + e.getMessage() );
        }
        
      }
      return false;
    }

    /**
     * Reads this message type definition from a node in an XML document
     *
     * @param ctx      The node to read from
     * @param buildCtx The BuildContext
     *
     * @exception FrameworkException Thrown if the definition cannot be
     *                               loaded.
     */
    public void readFromXML(Node ctx, BuildContext buildCtx)
        throws FrameworkException
    {        
        // get the name
        name = buildCtx.getString(buildCtx.xpaths.idPath, ctx);

	  // display name
        displayName = buildCtx.getString(buildCtx.xpaths.displayNamePath, ctx);

        if (log.isDebugEnabled())
        {
            log.debug("readFromXML(): The XPath test conditions for [" + name + "] MessageTypeDef object are:");    
        }


        //test conditions
        List condNodes = buildCtx.xpaths.testConditionPath.getNodeList(ctx);

        if (condNodes != null && condNodes.size() > 0)
        {
            Iterator iter = condNodes.iterator();
            Node condNode = null;
            String condStr = null;
            
            while (iter.hasNext() ) {
               condNode = (Node)iter.next();              
               condStr = condNode.getNodeValue();
               conditions.add(new ParsedXPath(condStr));
               
                if (log.isDebugEnabled())
                {
                    log.debug("[" + condStr + "]");    
                }
            }
                                          
        }
      
    }

    /**
     * Static passthough message type def object.
     * Can be used when a message type just needs to be 
     * a passthough
     */
    public static final class PassThrough extends MessageTypeDef
    {
        public boolean isCurrentType(Document doc) 
        {
            return true;
        }
            
        public PassThrough() 
        {
            super();
            
            this.name ="PASSTHROUGH";
            this.displayName ="Pass Through";
                
        }
    }
    
            
}
