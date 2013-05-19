/*
 * Copyright(c) 2002 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.manager.svcmeta;

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
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.svcmeta.*;
import com.nightfire.webgui.core.xml.*;
import com.nightfire.webgui.core.*;
import com.nightfire.framework.debug.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.webgui.core.svcmeta.StateDefContainer;
import com.nightfire.webgui.manager.beans.*;
import com.nightfire.webgui.core.beans.*;




/**
 * ComponentDef represents the definition of a service component inside
 * a bundle.  It contains information about how many of the service components
 * are allowed and what information is associated with a type of component.
 */
public class ComponentDef
{
    /** Used to indicate an unlimited number of maxOccurs */
    private static final String UNBOUNDED_VALUE = "unbounded";

    /** The bundle definition we are a part of */
    private BundleDef bundle;

    /** Identifies this component */
    private String id;

    /** Display name for the component */
    private String displayName;

    /** Full name for the component */
    private String fullName;

    /** Link to a state def that defines allowable actions for a given state, message type */
    private String stateDefName;

    /** Help text for the component */
    private String helpText;

    /** Minimum number of times the component may occur in the bundle */
    private int minOccurs;

    /** Maximum number of times the component may occur in the bundle */
    private int maxOccurs;

    /** default message type **/
    private MessageTypeDef defaultMsgType;

	/** This specifies whether 'display of outbound 997' feature is available to this service type or not.*/
    private boolean isDisplay997Supported;

    /** Fields representing additional information needed to create
        an instance of this component */
    private ArrayList addlInfoFields = new ArrayList();

    /** Read-only view of addlInfoFields */
    private List ro_addlInfoFields;

    /** Fields to display in the summary line for the component */
    private ArrayList summaryFields = new ArrayList();

    /** Read-only view of summaryFields */
    private List ro_summaryFields;

    /** Fields to display in the confirmation line for the component */
    private ArrayList confirmationFields = new ArrayList();

    /** Read-only view of confirmationFields */
    private List ro_confirmationFields;

    /** Fields to display in the detail line for the component */
    private ArrayList detailFields = new ArrayList();

   /** Fields to display in the detail line for the component with outbound ack/nack */
    private ArrayList ackDetailFields = new ArrayList();

    /** Read-only view of detailFields */
    private List ro_detailFields;

    /** Read-only view of ack detailFields */
    private List ro_ackDetailFields;

    /** Action definitions available on this component */
    private ArrayList actionDefs = new ArrayList();

    /** Read-only view of actionDefs */
    private List ro_actionDefs;

    /** Message Type definitions available on this component */
    private ArrayList msgTypeDefs = new ArrayList();

    /** Read-only view of msg type Defs */
    private List ro_msgTypeDefs;

    /** Fields to display in the history detail line for the component */
    private ArrayList historyDetailFields = new ArrayList();

    /** Read-only view of historyDetailFields */
    private List ro_historyDetailFields;

    private DebugLogger log;

    private ModifierInfo modifierInfo;

    private HistoryInfo historyInfo;

    /**  Variable Used to get TemplateSupport XML tag */
    private boolean templateSupport;
    
    /**
     * Get allowable actions for the service component bean and message type.
     *
     * @param bean ServiceComponentBean
     * @param messageType The message type
     *
     * @return A list of allowable actions for the given state and message type.
     *
     * @throws FrameworkException If processing error occurs.
     */

    public List getActions( ServiceComponentBean bean, String messageType )
    throws FrameworkException
    {
      StateDefContainer container = getStateDefContainer();
       if (container != null)
         return container.getAllowableActions(bean, messageType);
       else
         return null;
    }
    /**
     * Get allowable actions for the modifierBean and message type.
     *
     * @param bean The modifier bean
     * @param messageType The message type
     *
     * @return A list of allowable actions for the given state and message type.
     *
     * @throws FrameworkException If processing error occurs.
     */

    public List getActions(  ModifierBean bean,String messageType )
    throws FrameworkException
    {
      StateDefContainer container = getStateDefContainer();
      if (container != null)
        return container.getAllowableActions(bean, messageType);
      else
        return null;
    }


    /**
     * Constructor
     */
    public ComponentDef()
    {
        log = DebugLogger.getLoggerLastApp(getClass() );

        ro_addlInfoFields      = Collections.unmodifiableList(addlInfoFields);
        ro_summaryFields       = Collections.unmodifiableList(summaryFields);
        ro_confirmationFields  = Collections.unmodifiableList(confirmationFields);
        ro_detailFields        = Collections.unmodifiableList(detailFields);
		ro_ackDetailFields     = Collections.unmodifiableList(ackDetailFields);
        ro_actionDefs          = Collections.unmodifiableList(actionDefs);
        ro_historyDetailFields = Collections.unmodifiableList(historyDetailFields);
        ro_msgTypeDefs     = Collections.unmodifiableList(msgTypeDefs);
    }

    /**
     * Returns the bundle definition this component definition is a part of
     */
    public BundleDef getBundleDef()
    {
        return bundle;
    }

    /**
     * Sets the bundle definition this component definition is a part of
     */
    public void setBundleDef(BundleDef bundle)
    {
        this.bundle = bundle;
    }

    /**
     * Returns this component definition's ID.
     * @deprecated  This method does not conform to the Java Bean naming standard.  Use getId() instead.
     */
    public String getID()
    {
        return id;
    }


    /**
     * Returns this component definition's id.
     *
     * @return a <code>String</code> value
     */
    public String getId()
    {
        return id;
    }


    /**
     * Returns the display name for the component
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Returns the full name for the component
     */
    public String getFullName()
    {
        return fullName;
    }
    
    /**
     * Returns the templateSupport for the service group
     */
    public boolean getTemplateSupport()
    {
        return templateSupport;
    }
    
    /**
     * Returns the help text for the component
     */
    public String getHelpText()
    {
        return helpText;
    }

    /**
     * Returns the minimum number of times the component may occur in
     * a bundle.
     */
    public int getMinOccurs()
    {
        return minOccurs;
    }

    /**
     * Returns the maximum number of times the component may occur in
     * a bundle.
     */
    public int getMaxOccurs()
    {
        return maxOccurs;
    }

    /**
     * Returns a list of the fields representing additional information
     * needed to create an instance of this component.  The items in
     * the list are instances of {@link Field}.
     */
    public List getAddlInfoFields()
    {
        return ro_addlInfoFields;
    }

    /**
     * Returns a list of the fields to display in the summary line for
     * the component.  The items in the list are instances of {@link Field}.
     */
    public List getSummaryFields()
    {
        return ro_summaryFields;
    }

    /**
     * Returns a list of the fields to display in the confirmation line for
     * the component.  The items in the list are instances of {@link Field}.
     */
    public List getConfirmationFields()
    {
        return ro_confirmationFields;
    }

    /**
     * Returns a list of the fields to display in the detail line for
     * the component.  The items in the list are instances of {@link Field}.
     *
     * @deprecated replace by getHistoryInfo.
     */
    public List getDetailFields()
    {
        return ro_detailFields;
    }

    /**
     * Returns a list of the fields to display in the detail line for
     * the component with outbound ack/nack.  The items in the list are instances of {@link Field}.
     *
     */
    public List getAckDetailFields()
    {
        return ro_ackDetailFields;
    }

	/**
     * Returns the permission of import for the service
     *
     * @return boolean
     */
    public boolean getIsDisplay997Supported()
    {
        return isDisplay997Supported;
    }

    /**
     * Returns a list of the actions available on components of this type.
     * The items in the list are instances of {@link ActionDef}.
     */
    public List getActionDefs()
    {
        return ro_actionDefs;
    }

    /**
     * Returns a list of the message types available on
     * components of this type.
     * The items in the list are instances of {@link MessageTypeDef}.
     */
    public List getMessageTypeDefs()
    {
        return ro_msgTypeDefs;
    }

  /**
   * Returns the default message type.
   *
   * @return The default message type.
   */
  public MessageTypeDef getDefaultMessageType()
  {
    return defaultMsgType;

  }

  /**
   * Find the message type associated with this xml document.
   *
   * @param doc The xml document to test
   * @return The message type id.
   */
  public MessageTypeDef getMessageType(Document doc)
  {
    if (log.isDebugDataEnabled())
    {
        try
        {
            log.debugData("getMessageType(): Checking against the following Document object to obtain a matched MessageTypeDef object:\n" + (new XMLPlainGenerator(doc)).getOutput());
        }
        catch (Exception e)
        {
            log.error("getMessageType(): Failed to output the Document object:\n" + e.getMessage());
        }
    }

    MessageTypeDef result = null;

    Iterator iter = msgTypeDefs.iterator();
    while ( iter.hasNext() ) {
      MessageTypeDef def = (MessageTypeDef) iter.next();

        if (log.isDebugEnabled())
        {
            log.debug("getMessageType(): Checking [" + def.getName() + "] MessageTypeDef object as a possible match ...");
        }

      if (def.isCurrentType(doc) ) {
        result = def;
        break;
      }
    }

    if (result != null){
        if (log.isDebugEnabled() )
        {
            log.debug("ComponentDef [" + getId() +"] returning message type [" + result.getName() +"] for document.");
        }
    }
    else{
        log.debug("ComponentDef [" + getId() +"] returning a null MessageTypeDef object ...");
    }

    return result;
  }

    /**
     * Returns a list of the history-detail fields to be used in obtaining
     * the detail of an order transaction from the backend.  The items in
     * the list are instances of {@link Field}.
     *
     * @deprecated replace by getHistoryInfo.
     */
    public List getHistoryDetailFields()
    {
        return ro_historyDetailFields;
    }

    /**
     * Returns a particular action from the actions available on components of this type.
     * @param String id
     * @return ActionDef
     */
    public ActionDef getActionDef( String id )
    {
        Iterator iter = ro_actionDefs.iterator();

        ActionDef action = null;

        while ( iter.hasNext() )
        {
            ActionDef temp = (ActionDef) iter.next() ;
            String actionName = temp.getActionName();
            int idSuffix = actionName.indexOf("#");
            
            if (idSuffix > -1)
                actionName = actionName.substring(0, idSuffix);
            
            if ( actionName.equals( id ) )
            {
                action = temp;
                break;
            }
        }
        if ( action == null )
        {
            log.warn("getActionDef(String): No ActionDef found for id [" + id + "].");
        }

        return action;
    }


    /**
     * Returns information about modifiers for this component.
     *
     * @return a <code>ModifierInfo</code> value or null if there
     * in no information.
     */
    public ModifierInfo getModifierInfo()
    {
        return modifierInfo;
    }

    /**
     * Returns information about history for this component.
     *
     * @return a <code>History</code> value or null if there
     * in no information.
     */
    public HistoryInfo getHistoryInfo()
    {
        return historyInfo;
    }


    /**
     * Reads this component definition from a node in an XML document
     *
     * @param ctx      The node to read from
     * @param buildCtx The BuildContext that contains predefined data types
     *
     * @exception FrameworkException Thrown if the definition cannot be
     *                               loaded.
     */
    public void readFromXML(Node ctx, BundleDefBuildContext buildCtx)
        throws FrameworkException
    {
        BuildPaths xpaths = buildCtx.xpaths;

        // get the id
        id = buildCtx.getString(xpaths.idPath, ctx);

        // display name
        displayName = buildCtx.getString(xpaths.displayNamePath, ctx);

        // full name
        fullName = buildCtx.getString(xpaths.fullNamePath, ctx);
        
        // flag indicating whether to display this group in dropdown on GUI.
		templateSupport = StringUtils.getBoolean(buildCtx.getString(xpaths.templateSupportPath, ctx), false);
		
        // stateDefName

        if (xpaths.stateDefPath.nodeExists(ctx))
          stateDefName = buildCtx.getString(xpaths.stateDefPath, ctx);

        // help text
        List helpNodes = xpaths.helpPath.getNodeList(ctx);
        if (helpNodes.size() > 0)
        {
            Node helpNode = (Node)helpNodes.get(0);

            helpText = DOMWriter.toString(helpNode.getFirstChild(), true);
        }

        // minimum number of occurences
        minOccurs = buildCtx.getInt(xpaths.minOccursPath, ctx);

        // maximum number of occurences
        String max = buildCtx.getString(xpaths.maxOccursPath, ctx);
        if (!StringUtils.hasValue(max))
            maxOccurs = -1;
        else if (max.equalsIgnoreCase(UNBOUNDED_VALUE))
            maxOccurs = Integer.MAX_VALUE;
        else
            maxOccurs = StringUtils.getInteger(max);

	    // isDisplay997Supported
        isDisplay997Supported = Boolean.valueOf(buildCtx.getString(xpaths.display997SupportPath	, ctx)).booleanValue();

        // addlInfoFields
        loadFields(xpaths.infoFieldsPath, ctx, buildCtx, addlInfoFields);

        // summaryFields
        loadFields(xpaths.summaryFieldsPath, ctx, buildCtx, summaryFields);

        // confirmationFields
        loadFields(xpaths.confirmationFieldsPath, ctx, buildCtx, confirmationFields);

        // detailFields
        loadFields(xpaths.detailFieldsPath, ctx, buildCtx, detailFields);

        // ackDetailFields
        loadFields(xpaths.ackDetailFieldsPath, ctx, buildCtx, ackDetailFields);

        // historyDetailFields
        loadFields(xpaths.historyDetailFieldsPath, ctx, buildCtx, historyDetailFields);

        // actionDefs
        Iterator iter = xpaths.allowableActionsPath.getNodeList(ctx)
            .iterator();
        while (iter.hasNext())
        {
            Node n = (Node)iter.next();
            String name = n.getNodeValue();
            ActionDef def = buildCtx.getAction(name);
            if (def == null)
                log.error(
                          "Could not locate action with name [" + name
                          + "], referenced at [" + buildCtx.toXPath(ctx)
                          + "].");
            else
                actionDefs.add(def);
        }

          // message type defs
        iter = xpaths.allowableMsgTypesPath.getNodeList(ctx)
            .iterator();

        while (iter.hasNext())
        {
            Node n = (Node)iter.next();

            String name = buildCtx.getString(xpaths.valuePath, n);

            MessageTypeDef def = buildCtx.getMessageType(name);

            String defaultStr = buildCtx.getString(xpaths.defaultPath, n);

            if ( StringUtils.hasValue(defaultStr) && ServletUtils.getBoolean(defaultStr) )
              defaultMsgType = def;

            if (def == null)
                log.error(
                          "Could not locate MessageTypeDef with name [" + name
                          + "], referenced at [" + buildCtx.toXPath(ctx)
                          + "].");
            else
                msgTypeDefs.add(def);
        }

        if ( defaultMsgType == null) {
          String err ="ComponentDef [" + id +"] : Must have a default message type set.";
          log.error(err);
          throw new FrameworkException(err);
        }

        // load history
        loadHistoryInfo(ctx, buildCtx);
        if (historyInfo != null && historyInfo.getTypeField() == null) {
          throw new FrameworkException("ComponentDef [" + id +"] have a default typeField attribute set for history info.");
        }

        loadModifierInfo(ctx, buildCtx);


    }

    /**
     * Reads in history info from an XML document
     *
     * @param ctx       The service component node
     * @param buildCtx  The BuildContext to use
     *
     * @exception FrameworkException Thrown if history cannot be loaded
     *                               from doc
     */
    private void loadHistoryInfo(Node ctx, BuildContext buildCtx)
        throws FrameworkException
    {
        if (buildCtx.xpaths.historyInfoPath.nodeExists(ctx)) {

            Node historyNode = buildCtx.xpaths.historyInfoPath.getNode(ctx);

            historyInfo = new HistoryInfo(this, buildCtx);
            historyInfo.readFromXML(historyNode, buildCtx);
        }


    }

    /**
     * Reads in action definitions from an XML document
     *
     * @param ctx       The service component node
     * @param buildCtx  The BuildContext to use
     *
     * @exception FrameworkException Thrown if a actions cannot be loaded
     *                               from doc
     */
    private void loadModifierInfo(Node ctx, BuildContext buildCtx)
        throws FrameworkException
    {

        if (buildCtx.xpaths.modifiersInfoPath.nodeExists(ctx)) {

            Node modNode = buildCtx.xpaths.modifiersInfoPath.getNode(ctx);

            modifierInfo = new ModifierInfo(this, buildCtx);
            modifierInfo.readFromXML(modNode, buildCtx);
        }


    }


    /**
     * Loads a set of field definitions
     *
     * @param path     The path to load from
     * @param ctx      The Node path is relative to
     * @param buildCtx The build context
     * @param dest     The list to place loaded fields in
     */
    private void loadFields(ParsedXPath path, Node ctx,
                            BuildContext buildCtx, ArrayList dest)
        throws FrameworkException
    {
        Iterator iter = path.getNodeList(ctx).iterator();
        while (iter.hasNext())
        {
            Node n = (Node)iter.next();
            String name = n.getNodeValue();
            MessagePart fld = buildCtx.getMessagePart(name);
            if (fld == null)
                log.error(
                          "Could not locate field with name [" + name
                          + "], referenced at [" + buildCtx.toXPath(ctx)
                          + "].");
            else {
              log.debug("ComponentDef [" + id +"] : Loading field [" + fld.getId() +"]");
              dest.add(fld);
            }

        }
    }
    /**
     * getStateDefContainer
     *
     * @return StateDefContainer
     * @throws FrameworkException
     */
    private StateDefContainer getStateDefContainer() throws FrameworkException
    {
        if (stateDefName != null)
        {
            // Use state def name if specified.
            return StateDefContainer.getStateDefContainer(stateDefName);
        }
        else
        {
            // Use the service name if no state def is specified.
            return StateDefContainer.getStateDefContainer(id);
        }
    }

}
