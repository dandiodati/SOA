/*
 * Copyright(c) 2002 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.gateway.svcmeta;

// jdk imports
import java.util.*;

// third-party imports
import org.w3c.dom.*;

// nightfire imports
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.util.xml.*;
import com.nightfire.framework.util.*;
import com.nightfire.webgui.core.svcmeta.*;
import com.nightfire.webgui.core.*;
import com.nightfire.framework.debug.*;
import com.nightfire.webgui.core.meta.*;




/**
 * ServiceDef represents the definition of a service inside
 * a service group.  It contains information about how many of the services
 * are allowed and what information is associated with a type of service.
 */
public class ServiceDef
{
    public static final String TXN_LEVEL   = "txn-level";

    public static final String ORDER_LEVEL = "order-level";

    /** Identifies this service */
    private String id;

    /** Display name for the service */
    private String displayName;

    /** History Title to be displayed for the service on order-history page */
    private String historyTitle;

    /** Full name for the service */
    private String fullName;

    /** Link to a state def that defines allowable actions for a given state, message type */
    private String stateDefName;

    /**
     * Link to a Sub Type definition associated with service,
     * this is used to create the radio control for the services with same sub type
     */
    private String subType;

    /**
     * Link to a Sub Type Order definition associated with service,
     * this is used to keep the order while creating the radio control as per sub type 
     */
    private String subTypeOrder;

    /** Link to a supplier that over rides the Supplier value if service type is in-flight */
    private String inflightSupplier;

    /** Link to a inflight Version that over rides the Interface Version value if service type is in-flight */
    private String inflightVersion;

    /** Help text for the service */
    private String helpText;

    /** This specifies whether import facility is available to this service type or not.*/
    private boolean isImportAllowed;

    /** This specifies whether upload excel facility is available to this service type or not.*/
    private boolean isUploadExcelSupported;

    /** This specifies whether 'display of outbound 997' feature is available to this service type or not.*/
    private boolean isDisplay997Supported;

    /** This specifies whether 'Abandon & Cancel Order' feature is available to this service type or not.*/
    private boolean isAbandonActionSupported;

    /** This specifies whether 'Suspend & Resume Order' feature is available to this service type or not.*/
    private boolean isSuspendResumeSupported;

    /** This specifies whether 'auto increment VER' feature is available to this service type or not.*/
    private boolean isAutoIncrementVer;

    /** This specifies whether 'auto increment VER WithOut Supplement' feature is available to this service type or not.*/

    private boolean isAutoIncrementVerOnNewOrder;

    /** This specifies whether the request type should be displayed or not */
     private String displaySuffix;

    /** List of Nodes to be removed*/
    private ArrayList<String> removeNodes;

    /** List of Message Types for whom Save has to be disabled*/
    private ArrayList<String> hideSaveForMsgType;

    /** List of Message Types for whom Validate has to be disabled*/
    private ArrayList<String> hideValidateForMsgType;

    /** This specifies whether 'Import Overwrite' is supported or not.*/
    private boolean isImportOverwriteSupported;

    /** List of dateTime to be converted*/
    private ArrayList<String> convertToUTC_TZ_TSFs;

    /** List of dateTime to be converted in Ack*/
    private ArrayList<String> convertToUTC_TZ_AckTSFs;

    /** List of dateTime to be converted*/
    private ArrayList<String> convert_TZ_TSFs;

    private String timezone_To_Convert_TZ_TSFs;


    /** This specifies which Supplier value should be displayed*/
    private String aliasMethod;

    /** default message type **/
    private MessageTypeDef defaultMsgType;

    /** Type of actions.  This specifies whether the actions are applicable to each of  **/
    /** the history transactions or to the overall order.  By default, it is applicable **/
    /** to the each of the transactions.                                                **/
    private String actionsType;

    /** Indicates whether this service type can be created via the GUI. **/
    private boolean userCreatable = true;

    /** Fields representing additional information needed to create
        an instance of this service */
    private ArrayList<MessagePart> addlInfoFields = new ArrayList<MessagePart>();

    /** Read-only view of addlInfoFields */
    private List ro_addlInfoFields;

    /** Fields to display in the summary line for the service */
    private ArrayList<MessagePart> summaryFields = new ArrayList<MessagePart>();

    /** Read-only view of summaryFields */
    private List ro_summaryFields;

    /** Fields to display in the confirmation page for the service */
    private ArrayList<MessagePart> confirmationFields = new ArrayList<MessagePart>();

    /** Read-only view of confirmationFields */
    private List ro_confirmationFields;

    /** Fields to display in the transaction line for the service */
    private ArrayList<MessagePart> transactionSummaryFields = new ArrayList<MessagePart>();

    /** Read-only view of summaryFields */
    private List ro_txSummaryFields;

    /** Fields to display in the outbound 997 transaction line for the service */
    private ArrayList<MessagePart> ackTransactionSummaryFields = new ArrayList<MessagePart>();

    /** Read-only view of outbound 997 Tx summaryFields */
    private List ro_ackTxSummaryFields;

    /** Fields to display in the title */
    private ArrayList<MessagePart> titleFields = new ArrayList<MessagePart>();
    
    /** Read-only view of titleFields */
    private List ro_txTitleFields;
    
    /** Action definitions available on this service */
    private ArrayList<ActionDef> actionDefs = new ArrayList<ActionDef>();

    /** Read-only view of actionDefs */
    private List ro_actionDefs;

    /** Message Type definitions available on this service */
    private ArrayList<MessageTypeDef> msgTypeDefs = new ArrayList<MessageTypeDef>();

    /** Read-only view of msg type Defs */
    private List ro_msgTypeDefs;

    /** Fields to display in the history detail line for the service */
    private ArrayList<MessagePart> historyDetailFields = new ArrayList<MessagePart>();

    /** Read-only view of historyDetailFields */
    private List ro_historyDetailFields;

    private DebugLogger log;
    private ServiceGroupDef serviceGroupDef;

    /**
     * Constructor
     */
    protected ServiceDef()
    {
        log = DebugLogger.getLoggerLastApp(getClass() );

        ro_addlInfoFields      = Collections.unmodifiableList(addlInfoFields);
        ro_summaryFields       = Collections.unmodifiableList(summaryFields);
        ro_confirmationFields       = Collections.unmodifiableList(confirmationFields);
        ro_txSummaryFields       = Collections.unmodifiableList(transactionSummaryFields);
        ro_ackTxSummaryFields       = Collections.unmodifiableList(ackTransactionSummaryFields);
        ro_txTitleFields       = Collections.unmodifiableList(titleFields);
        ro_actionDefs          = Collections.unmodifiableList(actionDefs);
        ro_historyDetailFields = Collections.unmodifiableList(historyDetailFields);
        ro_msgTypeDefs     = Collections.unmodifiableList(msgTypeDefs);
    }

    /**
     * Returns this service definition's ID.
     *
     * @return String
     */
    public String getID()
    {
        return id;
    }

    /**
     * Returns the display name for the service
     *
     * @return String
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Returns the history Title for the service
     *
     * @return String
     */
    public String getHistoryTitle()
    {
        return historyTitle;
    }

    /**
     * Returns the full name for the service
     *
     * @return String
     */
    public String getFullName()
    {
        return fullName;
    }

    /**
     *
     * @return the value of sub type 
     */
    public String getSubType ()
    {
        return subType;
    }

    /**
     *
     * @return the value of sub type order
     */
    public String getSubTypeOrder ()
    {
        return subTypeOrder;
    }

    /**
     *
     * @return the value of in-flight supplier
     */
    public String getInflightSupplier ()
    {
        return inflightSupplier;
    }

    /**
     *
     * @return the value of in-flight version
     */
    public String getInflightVersion ()
    {
        return inflightVersion;
    }

    /**
     * Returns the help text for the service
     *
     * @return String
     */
    public String getHelpText()
    {
        return helpText;
    }

    /**
     * Returns the permission of import for the service
     *
     * @return boolean
     */
    public boolean getIsImportAllowed()
    {
        return isImportAllowed;
    }

    /**
     * Returns true if upload excel for the service
     * is supported else false.
     *
     * @return boolean
     */
    public boolean getIsUploadExcelSupported()
    {
        return isUploadExcelSupported;
    }

    /**
     * Returns whether 'display of outbound 997s' feature is supported for the service
     *
     * @return boolean
     */
    public boolean getIsDisplay997Supported()
    {
        return isDisplay997Supported;
    }

    /**
     * Returns whether 'Abandon & Cancel Order' feature is supported for the service
     *
     * @return boolean
     */
    public boolean getIsAbandonActionSupported ()
    {
        return isAbandonActionSupported;
    }

    /**
     * Returns whether 'Suspend & Resume Order' feature is supported for the service
     *
     * @return boolean
     */
    public boolean getIsSuspendResumeSupported ()
    {
        return isSuspendResumeSupported;
    }

    /**
     * Returns whether 'Auto Increment VER' feature is supported for the service
     *
     * @return boolean
     */
    public boolean getIsAutoIncrementVer()
    {
        return isAutoIncrementVer;
    }

    /**
     * Returns whether 'Auto Increment VER WithOut Supplement' feature is supported for the service
     *
     * @return boolean
     */
    public boolean getIsAutoIncrementVerOnNewOrder()
    {
        return isAutoIncrementVerOnNewOrder;
    }
     /**
     * Returns whether 'Import Overwrite' feature is supported for the service
     *
     * @return boolean
     */
    public boolean getIsImportOverwriteSupported()
    {
        return isImportOverwriteSupported;
    }
    /**
     * Returns the type of allowable actions.
     *
     * @return String
     */
    public String getActionsType()
    {
        return actionsType;
    }

    /**
     * Returns a true or false indicating whether this service type is creatable via GUI.
     *
     * @return boolean
     */
    public boolean getUserCreatable()
    {
        return userCreatable;
    }

    /**
     * Returns the service group that this service belongs to.
     *
     * @return The owner service group.
     */
    public ServiceGroupDef getServiceGroupDef()
    {
        return serviceGroupDef;
    }

    /**
     * Returns a list of the fields representing additional information needed
     * to create an instance of this service. The items in the list are
     * instances of {@link Field}.
     *
     * @return List
     */
    public List getAddlInfoFields()
    {
        return ro_addlInfoFields;
    }


    /**
     * Returns the display suffix for the service
     *
     * @return String
     */
    public String getDisplaySuffix()
    {
        return displaySuffix;
    }

    /**
     * Returns the List of values of remove nodes associated with this action.
     *
     * @return ArrayList<String> value
     */
    public ArrayList<String> getRemoveNodes()
    {
        return removeNodes;
    }

    /**
     * Returns the List of values of hideSaveForMsgType associated with this action.
     *
     * @return ArrayList<String> value
     */
    public ArrayList<String> getHideSaveForMsgType()
    {
        return hideSaveForMsgType;
    }

     /**
     * Returns the List of values of hideValidateForMsgType associated with this action.
     *
     * @return ArrayList<String> value
     */
    public ArrayList<String> getHideValidateForMsgType()
    {
        return hideValidateForMsgType;
    }

    /**
     * Returns the List of TransactionSummaryFields nodes whose timezone
     * is required to convert
     *
     * @return ArrayList<String> value
     */
    public ArrayList getConvertToUTC_TZ_TSFs()
    {
        return convertToUTC_TZ_TSFs;
    }

    /**
     * Returns the List of AckTransactionSummaryFields nodes whose timezone
     * is required to convert
     *
     * @return ArrayList<String> value
     */
    public ArrayList getConvertToUTC_TZ_AckTSFs()
    {
        return convertToUTC_TZ_AckTSFs;
    }

        /**
     * Returns the List of TransactionSummaryFields nodes whose timezone
     * is required to convert
     *
     * @return ArrayList<String> value
     */
    public ArrayList getConvert_TZ_TSFs()
    {
        return convert_TZ_TSFs;
    }

    /**
     * Returns the TP Alias Method for the service
     *
     * @return String
     */
    public String getAliasMethod()
    {
        return aliasMethod;
    }

    /**
     * Returns the Timezone to convert TSFs for the service
     *
     * @return String
     */
    public String getTimezone_To_Convert_TZ_TSFs()
    {
        return timezone_To_Convert_TZ_TSFs;
    }



    /**
     * Returns a Collection of the services with the same sub type.
     * The Collection would be ordered as per sub type order.
     *
     * @return Collection
     */
    public Collection getSubTypeServices ()
    {
        TreeMap<String, ServiceDef> tempMap = new TreeMap<String, ServiceDef> ();
        if (StringUtils.hasValue (subType))
        {
            List allServices = getServiceGroupDef ().getServices ();
            for (Object allService : allServices)
            {
                ServiceDef tempSvc = (ServiceDef) allService;
                if (StringUtils.hasValue(tempSvc.getSubType()) && subType.equals(tempSvc.getSubType()))
                {
                    tempMap.put(tempSvc.getSubTypeOrder(), tempSvc);
                }
            }
        }

        return tempMap.values ();
    }

    /**
     * Returns the named {@link Field} from the additional info fields
     * or null if field not defined.
     *
     * @param localXmlPath The local xml path of a file
     *
     * @return The named field from the additional info fields or null if field not defined.
     *
     */
    public Field getAddlInfoField(String localXmlPath)
    {
        for (Object addlInfoField : addlInfoFields)
        {
            Field field = (Field) addlInfoField;
            if (field.getXMLPath().equals(localXmlPath))
            {
                return field;
            }
        }
        // No field found.
        return null;
    }

    /**
     * Returns a list of the actions available on services of this type. The
     * items in the list are instances of {@link ActionDef}.
     *
     * @return List
     */
    public List getActionDefs()
    {
        return ro_actionDefs;
    }

    /**
     * Returns a list of the fields to display in the summary line for
     * the component.  The items in the list are instances of {@link Field}.
     *
     * @return List
     */
    public List getSummaryFields()
    {
        return ro_summaryFields;
    }

    /**
     * Returns a list of the fields to display in the confirmation page for
     * the component.  The items in the list are instances of {@link Field}.
     *
     * @return List
     */
    public List getConfirmationFields()
    {
        return ro_confirmationFields;
    }

    /**
     * Returns a list of the fields to display in the transaction line for
     * the component.  The items in the list are instances of {@link Field}.
     *
     * @return List
     */
    public List getTransactionSummaryFields()
    {
        return ro_txSummaryFields;
    }

    /**
     * Returns a list of the fields to display in the outbound-ack transaction line for
     * the component.  The items in the list are instances of {@link Field}.
     *
     * @return List
     */
    public List getAckTransactionSummaryFields()
    {
        return ro_ackTxSummaryFields;
    }
    
    /**
     * Returns a list of the fields to display in the title of display windows.
     *
     * @return List
     */
    public List getTitleFields()
    {
        return ro_txTitleFields;
    }

    /**
     * Returns a list of the message types available on services of this type.
     * The items in the list are instances of {@link MessageTypeDef}.
     *
     * @return List
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
    if (log.isDebugEnabled())
    {
        try
        {
            log.debug("getMessageType(): Checking against the following Document object to obtain a matched MessageTypeDef object:\n" + (new XMLPlainGenerator(doc)).getOutput());
        }
        catch (Exception e)
        {
            log.error("getMessageType(): Failed to output the Document object:\n" + e.getMessage());
        }
    }

    MessageTypeDef result = null;

      for (Object msgTypeDef : msgTypeDefs)
      {
          MessageTypeDef def = (MessageTypeDef) msgTypeDef;

          if (log.isDebugEnabled())
          {
              log.debug("getMessageType(): Checking [" + def.getName() + "] MessageTypeDef object as a possible match ...");
          }

          if (def.isCurrentType(doc))
          {
              result = def;
              break;
          }
      }

    if (result != null){
        if (log.isDebugEnabled() )
        {
            log.debug("ServiceDef [" + getID() +"] returning message type [" + result.getName() +"] for document.");
        }
    }
    else{
        if (log.isDebugEnabled() )
        {
            log.debug("ServiceDef [" + getID() +
                      "] returning a null MessageTypeDef object ...");
        }
    }

    return result;
  }

    /**
     * Returns a list of the history-detail fields to be used in obtaining the
     * detail of an service transaction from the backend. The items in the list
     * are instances of {@link Field}.
     *
     * @return List
     */
    public List getHistoryDetailFields()
    {
        return ro_historyDetailFields;
    }

    /**
     * Returns a particular action from the actions available on services of
     * this type.
     *
     * @param id id
     *
     * @return ActionDef
     */
    public ActionDef getActionDef( String id )
    {
        Iterator iter = ro_actionDefs.iterator();

        ActionDef action = null;

        while ( iter.hasNext() )
        {
            ActionDef temp = (ActionDef) iter.next() ;
            if ( temp.getActionName().equals( id ) )
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
     * Returns a list of Action objects valid for the indicated state of this
     * service.
     *
     * @param state State of the order.
     * @param messageType Type of message, such as createsv, cancelsv
     *
     * @return List of Action objects.
     *
     * @throws FrameworkException on error
     */
    public List getActions(String state, String messageType) throws FrameworkException
  {

      if (log.isInfoEnabled())
      {
          log.info(
              "ServiceDef.getActions: Getting allowable actions for state [" +
              state + "], messageType [" +
              messageType + "]...");
      }

      if (actionDefs == null)
      {
          log.warn("ServiceDef.getActions: No actions defined for service [" + id + "]");
          return null;
      }

      // Lazy loading state actions.
      List stateActions = null;
      // Used for lazy loading
      boolean stateActionsLoaded = false;

      List<ActionDef> allowedActions = new ArrayList<ActionDef>();

      for (Object actionDef : actionDefs)
      {
          ActionDef action = (ActionDef) actionDef;
          String actionName = action.getActionName();
          if (action.getCheckState())
          {
              if (log.isDebugEnabled())
              {
                  log.debug("Checking action [" + actionName +
                      "] against state...");
              }

              // Load state actions if state def container not loaded.
              if (!stateActionsLoaded)
              {
                  StateDefContainer stateDefContainer = getStateDefContainer();

                  if (log.isDebugEnabled())
                  {
                      log.debug(
                          "Retrieving state actions for current state, message type...");
                  }

                  if (stateDefContainer != null)
                  {
                      // Retrieve the allowable actions.
                      stateActions = stateDefContainer.getAllowableActions(state, messageType);
                  }

                  stateActionsLoaded = true;
                  if (log.isDebugEnabled())
                  {
                      log.debug("Got state actions [" + stateActions +
                          "] for current state, message type.");
                  }
              }

              // Check against state actions.
              if (stateActions != null)
              {
                  if (stateActions.contains(actionName))
                  {
                      if (log.isDebugEnabled())
                      {
                          log.debug("Adding allowed action [" + actionName +
                              "] for current state, message type...");
                      }
                      allowedActions.add(action);
                  }
                  else
                  {
                      if (log.isDebugEnabled())
                      {
                          log.debug("Skipping action [" + actionName +
                              "] not allowed for current state, message type...");
                      }
                  }
              }
              else
              {
                  if (log.isDebugEnabled())
                  {
                      log.debug(
                          "No actions allowed for current state, message type, skipping action [" +
                              actionName + "]...");
                  }
              }
          }
          else
          {
              if (log.isDebugEnabled())
              {
                  log.debug("No need to check against state for action [" +
                      actionName + "], adding it...");
              }
              allowedActions.add(action);
          }
      }

      if (log.isInfoEnabled())
      {
          log.info("ServiceDef.getActions: Obtained allowed actions [" +
                   allowedActions + "]for [" + state + "], messageType [" +
                   messageType + "]...");
      }

      return allowedActions;

  }


    /**
     * Reads this service definition from a node in an XML document
     *
     * @param ctx      The node to read from
     * @param buildCtx The BuildContext that contains predefined data types
     *
     * @exception FrameworkException Thrown if the definition cannot be
     *                               loaded.
     */
    protected void readFromXML(Node ctx, ServiceGroupBuildContext buildCtx)
        throws FrameworkException
    {
        BuildPaths xpaths = buildCtx.xpaths;

        // get the id
        id = buildCtx.getString(xpaths.idPath, ctx);

        // display name
        displayName = buildCtx.getString(xpaths.displayNamePath, ctx);

        // history Title
        historyTitle = buildCtx.getString(xpaths.historyTitlePath, ctx);

        // full name
        fullName = buildCtx.getString(xpaths.fullNamePath, ctx);

        // stateDefName
        stateDefName = buildCtx.getString(xpaths.stateDefPath, ctx);

        // subType
        subType = buildCtx.getString(xpaths.subTypePath, ctx);

        // subTypeOrder
        subTypeOrder = buildCtx.getString(xpaths.subTypeOrderPath, ctx);

        // inflightSupplier
        inflightSupplier = buildCtx.getString(xpaths.inflightSupplierPath, ctx);

        // inflightVersion
        inflightVersion = buildCtx.getString(xpaths.inflightVersionPath, ctx);

        //display suffix
        displaySuffix = buildCtx.getString(xpaths.displaySuffixPath, ctx);

        //tpAliasMethod
        aliasMethod = buildCtx.getString(xpaths.tpAliasMethodPath, ctx);

        // help text
        List helpNodes = xpaths.helpPath.getNodeList(ctx);
        if (helpNodes.size() > 0)
        {
            Node helpNode = (Node)helpNodes.get(0);

            helpText = DOMWriter.toString(helpNode.getFirstChild(), true);
        }

        removeNodes = populateNodeList (removeNodes, buildCtx.xpaths.removeNodesPath, ctx);

        hideSaveForMsgType = populateNodeList (hideSaveForMsgType, buildCtx.xpaths.hideSaveForMsgTypePath, ctx);

        hideValidateForMsgType = populateNodeList (hideValidateForMsgType, buildCtx.xpaths.hideValidateForMsgTypePath, ctx);

        convertToUTC_TZ_TSFs = populateNodeList (convertToUTC_TZ_TSFs, buildCtx.xpaths.convertToUTC_TZ_TSFsPath, ctx);
        convertToUTC_TZ_AckTSFs = populateNodeList (convertToUTC_TZ_AckTSFs, buildCtx.xpaths.convertToUTC_TZ_AckTSFsPath, ctx);

        convert_TZ_TSFs = populateNodeList (convert_TZ_TSFs, buildCtx.xpaths.convert_TZ_TSFsPath, ctx);

        timezone_To_Convert_TZ_TSFs = buildCtx.getString(xpaths.timezone_To_Convert_TZ_TSFsPath, ctx);
        // isImportAllowed
        isImportAllowed = Boolean.valueOf(buildCtx.getString(xpaths.allowImportPath, ctx));

        //isUploadExcelSupported
        isUploadExcelSupported = Boolean.valueOf(buildCtx.getString(xpaths.uploadExcelPath, ctx));

        // isDisplay997Supported
        isDisplay997Supported = Boolean.valueOf(buildCtx.getString(xpaths.display997SupportPath, ctx));

        // isAbandonActionSupported
        isAbandonActionSupported = Boolean.valueOf(buildCtx.getString(xpaths.abandonActionSupportPath, ctx));

        // isSuspendResumeSupported
        isSuspendResumeSupported = Boolean.valueOf(buildCtx.getString(xpaths.suspendResumeSupportPath, ctx));

        // isAutoIncrementVer
        isAutoIncrementVer = Boolean.valueOf(buildCtx.getString(xpaths.autoIncrementVerPath, ctx));

        // isAutoIncrementVer without supplement
        isAutoIncrementVerOnNewOrder = Boolean.valueOf(buildCtx.getString(xpaths.autoIncrementVerOnNewOrderPath, ctx));

        //isImportOverwriteSupported
        isImportOverwriteSupported = Boolean.valueOf(buildCtx.getString(xpaths.importOverwriteSupportedPath, ctx));

        // addlInfoFields
        loadFields(xpaths.infoFieldsPath, ctx, buildCtx, addlInfoFields);

        // summaryFields
        loadFields(xpaths.summaryFieldsPath, ctx, buildCtx, summaryFields);

        // confirmationFields
        loadFields(xpaths.confirmationFieldsPath, ctx, buildCtx, confirmationFields);

        // transactionSummaryFields
        loadFields(xpaths.txSummaryFieldsPath, ctx, buildCtx, transactionSummaryFields);

        // AckTransactionSummaryFields
        loadFields(xpaths.ackTxSummaryFieldsPath, ctx, buildCtx, ackTransactionSummaryFields);

        // titleFields
        loadFields(xpaths.titleFieldsPath, ctx, buildCtx, titleFields);

        // historyDetailFields
        loadFields(xpaths.historyDetailFieldsPath, ctx, buildCtx, historyDetailFields);

        // type of allowable actions
        actionsType = buildCtx.getString(xpaths.allowableActionsTypePath, ctx);

        // flag indicating whether this service type is creatable via GUI.
        userCreatable = StringUtils.getBoolean(buildCtx.getString(xpaths.userCreatablePath, ctx), true);

        if (!StringUtils.hasValue(actionsType))
        {
            actionsType = TXN_LEVEL;
        }

        // actionDefs
        for (Object allowableAction : xpaths.allowableActionsPath.getNodeList(ctx))
        {
            Node n = (Node) allowableAction;
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
        for (Object allowableMsgType : xpaths.allowableMsgTypesPath.getNodeList(ctx))
        {
            Node n = (Node) allowableMsgType;

            String name = buildCtx.getString(xpaths.valuePath, n);

            MessageTypeDef def = buildCtx.getMessageType(name);

            String defaultStr = buildCtx.getString(xpaths.defaultPath, n);

            if (StringUtils.hasValue(defaultStr) && ServletUtils.getBoolean(defaultStr))
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
          String err ="ServiceDef [" + id +"] : Must have a default message type set.";
          log.error(err);
          throw new FrameworkException(err);
        }


    }

    /**
     * Populates the arraylist object passed with the nodes under given node
     * as ctx with the given xpath
     * @param list   ArrayList object
     * @param xPath  ParsedXPath to obtain nodelist
     * @param ctx    Nodes to retrieve under given node 
     * @return ArrayList<String> 
     * @throws FrameworkException on error
     */
    private ArrayList<String> populateNodeList(ArrayList<String> list, ParsedXPath xPath, Node ctx) throws FrameworkException
    {
        list = new ArrayList<String>();
        List nodesList = xPath.getNodeList(ctx);

        if (nodesList.size() > 0)
        {
            Node rootNode = (Node)nodesList.get(0);
            NodeList chileNodes = rootNode.getChildNodes();
            for (int num = 0; num < chileNodes.getLength(); num++)
            {
                Node node = chileNodes.item(num);
                if( XMLMessageBase.isValueNode(node))
                {
                    list.add(XMLMessageBase.getNodeValue(node));
                }
            }
        }
        return list;
    }

    /**
     * Loads a set of field definitions.
     *
     * @param path The path to load from
     * @param ctx The Node path is relative to
     * @param buildCtx The build context
     * @param dest The list to place loaded fields in
     * @throws FrameworkException on error
     *
     * Note: variable dest has made of type ArrayList<MessagePart>
     * to loadFields only of <MessagePart> type.  
     * This can be changed if required.
     */
    private void loadFields(ParsedXPath path, Node ctx,
                            ServiceGroupBuildContext buildCtx, ArrayList<MessagePart> dest)
        throws FrameworkException
    {
        for (Object nodeChild : path.getNodeList(ctx))
        {
            Node n = (Node) nodeChild;
            String name = n.getNodeValue();
            MessagePart fld = buildCtx.getMessagePart(name);
            if (fld == null)
                log.error(
                    "Could not locate field with name [" + name
                        + "], referenced at [" + buildCtx.toXPath(ctx)
                        + "].");
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("ServiceDef [" + id + "] : Loading field [" +
                        fld.getId() + "]");
                }
                dest.add(fld);
            }

        }
    }

    /**
     * setServiceGroup
     *
     * @param serviceGroupDef ServiceGroupDef
     */
    protected void setServiceGroup(ServiceGroupDef serviceGroupDef)
    {
        if (this.serviceGroupDef != null)
        {
            log.warn("Overwriting previously assigned service group [" +
                     this.serviceGroupDef.getID() +
                     "] with new service group [" + serviceGroupDef.getID() + "].");
        }

        this.serviceGroupDef = serviceGroupDef;
    }

    /**
     * getStateDefContainer
     *
     * @return StateDefContainer object
     * @throws FrameworkException on error
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
