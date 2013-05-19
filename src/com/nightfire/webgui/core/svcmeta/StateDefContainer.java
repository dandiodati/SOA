/**
 * Copyright (c) 2003 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */
package com.nightfire.webgui.core.svcmeta;

import org.w3c.dom.*;

import java.util.*;
import java.rmi.*;
import java.sql.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.repository.*;

import com.nightfire.mgrcore.common.*;
import com.nightfire.webgui.manager.ManagerServletConstants;
import com.nightfire.framework.message.util.xml.*;

import com.nightfire.webgui.manager.beans.*;
/**
 * The class represents allowable actions for each message types for each state for
 *  a service type. The class contains a list of states, a list message types for each
 *  state, and a list of allowable actions for each message type.
 * It also contains utility methods to load, cache or access these definitions.
 *
 * The configuration for this class resides under repostitory STATE_DEF_CONFIG.
 * Change (1/11): Make value of stateDefMap to be a list of StateDef instead
 * of a instance of StateDef because we need to handle:
 * <StateDef name="Started">
 *                 <ExternalStateDefRef type="csr" value="PendingOrders"/>
 *                 <ExternalStateDefRef type="Loop" value="WaitForResponse|PendingOrders"/>
 *
 *                <MessageType name="new">
 *                        <Actions>
 *                                <Name value="send-order"/>
 *                        </Actions>
 *                        <Actions>
 *                                <Name value="cancel-order"/>
 *                        </Actions>
 *                </MessageType>
 *</StateDef>
 *
 * <StateDef name="Started">
 *                 <MessageType name="new">
 *                         <Actions>
 *                                 <Name value="send-order"/>
 *                         </Actions>
 *                 </MessageType>
 * </StateDef>
 */
public class StateDefContainer
{
    /**
     * Repository category for the state definitions.
     */
    public static final String STATE_DEF_CONFIG = "stateDef";

    /**
     * Node name used to indicate default behaviour.
     */
    public static final String DEFAULT_ITEM = "Default";

    /**
     * Variable containing all loaded stateDefContainers.
     *
     */
    private static Map stateDefContainerMap;

    /**
     * The name of the stateDefContainer.
     *
     */
    private final String stateDefContainerName;

    /**
     * Map of all state defs defined, where key=stateName and value=a list of stateDef.
     */
     private final Map stateDefMap;

     /**
      * Default allowable actions based solely on message type. This is used only
      * if there is no configuration specified for the state. If default
      * StateDef exist, it will be in first item in defaultStateDef list.
      */
     protected List defaultStateDef = null;

    /**
     * Constructs a state def container object from a state definition file.
     *
     * @param stateConfig An XML file describing a set of allowable actions that are based on
     *                  state name and message type.
     *
     * @throws FrameworkException If configuration is invalid.
     */
    protected StateDefContainer( String stateConfig ) throws FrameworkException
    {
      if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
      {
        Debug.log(Debug.SYSTEM_CONFIG,
                  "Loading state def configurations from [" + stateConfig +
                  "]...");
      }

        // Create a map to contain the states and state defs.
        this.stateDefMap = new HashMap();

        try
        {
            XMLPlainGenerator parser = new XMLPlainGenerator ( stateConfig );

            Node stateDefContainerNode = StateDefXPath.STATE_DEF_CONTAINER.getNode(parser.getDocument());

            this.stateDefContainerName = getString(StateDefXPath.NAME, stateDefContainerNode);

            if (this.stateDefContainerName == null)
            {
                throw new FrameworkException("ERROR: Required name attribute must be specified for state def container.");
            }

            List stateDefNodes = StateDefXPath.STATE_DEF.getNodeList(stateDefContainerNode);

            Iterator iter = stateDefNodes.iterator();
            while (iter.hasNext())
            {
                Node stateDefNode = (Node)iter.next();

                StateDef stateDef = new StateDef( stateDefNode );

                String curState = stateDef.stateName;

                if ( curState.equals( DEFAULT_ITEM ) )
                {
                    if ( defaultStateDef == null )
                    {
                      defaultStateDef = new ArrayList();
                      defaultStateDef.add(stateDef);

                      if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
                        Debug.log(Debug.SYSTEM_CONFIG,
                                  "Default STATE configured.");
                      }
                    }
                    else
                    {
                      defaultStateDef.add(stateDef);
                      if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
                        Debug.log(Debug.SYSTEM_CONFIG,
                            "More than one configuration for state [Default]. state container name: [" +
                                  this.stateDefContainerName + "]");
                    }
                }
                else
                {
                    // store stateDef into StateDefMap in a list format
                    List stateDefList = null;
                    if (stateDefMap.containsKey(curState)) {
                        stateDefList = (List) stateDefMap.get(curState);
                    }
                    else {
                      stateDefList = new ArrayList();
                    }
                    // add stateDef to the list
                    stateDefList.add(stateDef);
                    // add list to StateDefMap.
                    // key = stateName; value = list of StateDef
                    stateDefMap.put( curState, stateDefList );
                }

            }

            if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            {
                Debug.log(Debug.SYSTEM_CONFIG, "Loaded state def container " +
                          describe() );

            }

        }
        catch ( MessageException e )
        {
            throw new FrameworkException ( "Failed to initialize state configuration: " + e.getMessage() );
        }

    }

    /**
     * Get allowable actions for the specified state and message type. Since
     * this signature doesn't have ServiceComponentBean or ModifierBean, obtain
     * the StateDef with no ExternalStateDef
     *
     * @param stateName The name of state
     * @param messageType The message type
     *
     * @return A list of allowable actions for the given state and message type.
     *
     * @throws FrameworkException If processing error occurs.
     */
    public List getAllowableActions( String stateName, String messageType ) throws FrameworkException
    {
        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log ( Debug.MSG_STATUS, "Getting allowable actions for state [" + stateName + "], messageType [" +
            messageType + "]..." );

        List actions = null;

        List stateList = getStateDef(stateName);
        if (stateList != null && stateList.size() > 0) {
          for(int i = 0; i < stateList.size(); i++) {

            if (((StateDef)stateList.get(i)).externalRefExists())
              actions = ((StateDef)stateList.get(i)).getAllowableActions(messageType);
              break;
          }
        }

        if ( actions == null && defaultStateDef != null && defaultStateDef.size() > 0)
        {
            // try the default state definition
                actions = ((StateDef)defaultStateDef.get(0)).getAllowableActions( messageType );
        }


        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log ( Debug.MSG_STATUS, "Got allowable actions [" + actions + "] for state [" +
            stateName + "], message type [" + messageType + "]." );

        return actions;

    }

    public String describe()
    {
        StringBuffer sb =new StringBuffer();

        sb.append("State Def configuration [" + stateDefContainerName + "]: " );

        if ( defaultStateDef != null && defaultStateDef.size() > 0)
        {
            sb.append("Default state configuration [" +
                      ((StateDef)defaultStateDef.get(0)).describe() + "]\n");
        }

        Iterator iter = stateDefMap.values().iterator();

        while ( iter.hasNext ( ) )
        {
            List items = (List) iter.next( );
            for(int i = 0; i < items.size(); i++) {
              sb.append(((StateDef)items.get(i)).describe());
            }

        }

        return sb.toString();

    }

    /**
     * Get a list of StateDef
     *
     * @param stateName The name of state
     *
     * @return A list of StateDef objects.
     *
     */
    public List getStateDef(String stateName)
    {
      if (stateName == null)
        return defaultStateDef;
      List aStateDef = (List) stateDefMap.get(stateName);
      if (aStateDef == null)
        return defaultStateDef;
      return aStateDef;
    }

    /**
     * Class to maintain a StateDef' configuration information.
     * It contains a single state's allowable actions for each message type.
     */
    private static class StateDef
    {
      public StateDef(Node stateNode) throws FrameworkException
      {
        this.stateName = getString(StateDefXPath.NAME, stateNode);

        // add external statedef ref support
        if (StateDefXPath.REF_TYPE.nodeExists(stateNode)) {
          addRef(stateNode);
        }

        // Create a map to contain the message type to actions map.
        this.messageTypeActionsMap = new HashMap();
        List messageTypeNodes = StateDefXPath.MESSAGE_TYPE.getNodeList(stateNode);

        Iterator iter = messageTypeNodes.iterator();
        while (iter.hasNext())
        {
          Node messageTypeNode = (Node) iter.next();

          addMessageType(messageTypeNode);

        }
      }

      // Add external statedef ref to externalRefMap.
      private void addRef(Node stateNode) throws FrameworkException {

		this.externalRefMap = new HashMap();
        Node[] refs = StateDefXPath.REF_TYPE.getNodes(stateNode);
        for (int i = 0; i < refs.length; i++) {
          externalRefMap.put(getString(StateDefXPath.TYPE,refs[i]),
			  getString(StateDefXPath.VALUE, refs[i]));
        }
      }

      // Add configuration for a message type.
      private void addMessageType ( Node messageTypeNode ) throws FrameworkException
      {
          String messageType = getString(StateDefXPath.NAME, messageTypeNode);

          List actions = new ArrayList();
          List actionsNodes = StateDefXPath.ACTION_NAME.getNodeList(messageTypeNode);

          Iterator iter = actionsNodes.iterator();
          while (iter.hasNext())
          {
              Node actionNode = (Node)iter.next();

              actions.add(actionNode.getNodeValue());

          }

          // if the message type is "Default", then we want to keep it separate
          // as a fallback configuration.
          if ( messageType.equals( DEFAULT_ITEM ) )
          {
              defaultActions = actions;
          }
          else
          {
              messageTypeActionsMap.put( messageType, actions );
          }
      }

      public List getAllowableActions( String messageType )
      {
          List actions = null;
          if (messageType != null)
          {
              // Try the give message type first.
              actions = (List)messageTypeActionsMap.get( messageType );
          }

          if ( actions == null )
          {
              // Try the default if not defined.
              actions = defaultActions;
          }

          return actions;
      }

      public String describe ( )
      {
          StringBuffer sb = new StringBuffer( );

          sb.append( "State [" );
          sb.append( stateName );

          sb.append( "], default actions is [" );
          sb.append( defaultActions );

          if ( externalRefMap != null )
          {
              sb.append( "], External Ref map \n[" );
              sb.append( externalRefMap.toString ( ) );
          }

          if ( messageTypeActionsMap != null )
          {
              sb.append( "], message type to actions map \n[" );
              sb.append( messageTypeActionsMap.toString ( ) );
          }
          sb.append( "].\n" );

          return( sb.toString() );

      }//describe
      public Map getExternalRefs ()
      {
        return externalRefMap;
      }

      public boolean externalRefExists ()
      {
        return externalRefMap != null && externalRefMap.size() > 0;
      }

      public boolean isExternalValid(InfoBodyBase bean, boolean checkParent) {
        // check if externalStateRef exists
        if (externalRefExists()) {
          // loop through external stateDefRef
          for (Iterator it = externalRefMap.keySet().iterator(); it.hasNext();) {
            // get service type
            String serviceType = (String) it.next();
            // get state. Note that state can be x|y
            String state = (String) externalRefMap.get(serviceType);

            if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                Debug.log ( Debug.MSG_STATUS, "externalRef exists serviceType ["
                            +  serviceType
                            + "] state [" + state + "]"
                            + " bean service type ["
                            + bean.getServiceType()
                            + "] Bean StateName ["
                            + stateName + "]");

            // if external ref criteria = current bean's state and service type
            if (serviceType.equalsIgnoreCase(bean.getServiceType())
                && state.indexOf(stateName) != -1)
              continue;

            // check if parent bundle has any order in the right state as ExternalStateDef
            // specified. This is used for ServiceComponentBean only, not for ModifierBean
            if (checkParent && bean instanceof ServiceComponentBean) {
              // get parent bundle
              BundleBeanBag parent = ((ServiceComponentBean)bean).getParentBag();
              if (parent != null) {
                // get all order lists with the service type
                List orderList = parent.getBeans(serviceType);
                if (orderList == null)
                  return false;
                boolean found = false;
                // loop through all orders
                for (Iterator orderIt = orderList.iterator(); orderIt.hasNext(); ) {
                  ServiceComponentBean serviceComponent = (ServiceComponentBean)
                      orderIt.next();
                  // compare current state
                  if (state.indexOf( (String) serviceComponent.
                                             getHeaderValue(
                      ManagerServletConstants.
                      STATUS_FIELD)) != -1) {
                    found = true;
                    break;
                  }
                }
                if (!found)
                  return false;
              }
              else {
                return false;
              }
            }

          }
        }
        return true;
      }

      public final String stateName;
      public List defaultActions;
      public final Map messageTypeActionsMap;
      // support external statedef ref. key=service type value=state(s)
      public Map externalRefMap = null;

    }

    /**
     * Retreives a state def container with the given name.
     * Loads the state def containers if necessary.
     *
     * @param stateDefContainerName The name of the stateDefContainer.
     *
     * @return The StateDefContainer with the given name.
     *
     * @exception FrameworkException in the case where the
     *            configuration for the StateDefContainer is bad.
     */
    public static StateDefContainer getStateDefContainer(String stateDefContainerName) throws FrameworkException
    {
        // Check for null
        if (stateDefContainerName == null)
        {
            throw new FrameworkException("Cannot obtain state defintion with null name.");
        }

        // Loading state def containers if not yet initialized.
        if (null == stateDefContainerMap)
        {
          synchronized (StateDefContainer.class)
          {
            if (null == stateDefContainerMap)
            {
              initialize();
            }
          }
        }

        StateDefContainer stateDefContainer = (StateDefContainer) stateDefContainerMap.get(stateDefContainerName);

        // Check for null
        if (stateDefContainer == null)
        {
            throw new FrameworkException("Cannot obtain state defintion with name [" + stateDefContainerName + "].");
        }

        return stateDefContainer;
    }

    /**
     * Get allowable actions for the specified state and message type.
     *
     * @param stateDefContainerName The name of the stateDefContainer.
     * @param stateName The name of state
     * @param messageType The message type
     *
     * @return A list of allowable actions for the given state and message type.
     *
     * @throws FrameworkException If processing error occurs.
     */
    public static List getAllowableActions( String stateDefContainerName, String stateName, String messageType ) throws FrameworkException
    {
        StateDefContainer stateDefContainer = getStateDefContainer(stateDefContainerName);

        if (stateDefContainer == null)
        {
            throw new FrameworkException("Cannot obtain state defintion with [" + stateDefContainerName + "].");
        }

        return stateDefContainer.getAllowableActions(stateName, messageType);

    }

    /**
     * Get allowable actions for the specified ServiceComponentBean and message type.
     * If externalStateDefRef exists, need to verify if the parent bundle has
     * an order which matches service type and state specified in external StateDefRef.
     *
     * @param stateDefContainerName The name of the stateDefContainer.
     * @param bean The ServiceComponentBean instance
     * @param messageType The message type
     *
     * @return A list of allowable actions for the given ServiceComponentBean and message type.
     *
     * @throws FrameworkException If processing error occurs.
     */
    public List getAllowableActions( ServiceComponentBean bean, String messageType )
    throws FrameworkException
    {

      String stateName = (String)bean.getHeaderValue(ManagerServletConstants.STATUS_FIELD);
      List stateDefList = getStateDef(stateName);

      List actions = null;
      
      if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
          Debug.log ( Debug.MSG_STATUS, "getAllowableActions(ServiceComponentBean, String) stateName ["
                      +  stateName
                      + "] messageType ["
                      + messageType + "]");

      if (stateDefList != null && stateDefList.size() > 0) {
        for(int i = 0; i < stateDefList.size(); i++) {
            StateDef sd = (StateDef) stateDefList.get(i);

          if (sd.isExternalValid(bean, true)) {
               actions = sd.getAllowableActions(messageType);
               break;
          }
        }
      }

      if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log ( Debug.MSG_STATUS, "Got allowable actions [" + actions + "] for state [" +
            stateName + "], message type [" + messageType + "]." );

        return actions;
        
    }

    /**
     * Get allowable actions for the specified ServiceComponentBean and message type.
     * If externalStateDefRef exists, need to verify if the parent ServiceCompoent
     * or Modifier Bean matches service type with state specified in external statedef.
     *
     * @param stateDefContainerName The name of the stateDefContainer.
     * @param bean The ServiceComponentBean instance
     * @param messageType The message type
     *
     * @return A list of allowable actions for the given ServiceComponentBean and message type.
     *
     * @throws FrameworkException If processing error occurs.
     */
    public List getAllowableActions(  ModifierBean bean,String messageType )
    throws FrameworkException
    {
      String stateName = (String)bean.getHeaderValue(ManagerServletConstants.STATUS_FIELD);
      List stateDefList = getStateDef(stateName);
      String modifyServiceType = bean.getServiceType();
      List actions = null;
      

      if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
          Debug.log ( Debug.MSG_STATUS, "getAllowableActions(ModifierBean, String) stateName ["
                      +  stateName
                      + "] messageType ["
                      + messageType + "]");

      if (stateDefList != null && stateDefList.size() > 0) {
       for(int i = 0; i < stateDefList.size(); i++) {
         if (((StateDef)stateDefList.get(i)).isExternalValid(bean, false)) {
              actions =  ((StateDef)stateDefList.get(i)).getAllowableActions(messageType);
              break;
         }
         else {
           ServiceComponentBean parent = bean.getParent();
           if (parent != null &&((StateDef)stateDefList.get(i)).isExternalValid(parent, false)) {
             actions =  ((StateDef)stateDefList.get(i)).getAllowableActions(messageType);
             break;
           }
         }
       }
     }

      if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log ( Debug.MSG_STATUS, "Got allowable actions [" + actions + "] for state [" +
            stateName + "], message type [" + messageType + "]." );

        return actions;
     
    }
    /**
     * Loads all StateDefContainer definitions as specified
     * under the state def repository category. There can be multiple xml files
     * under the category.
     *
     * @throws FrameworkException If configuration is invalid.
     */
    protected static void initialize () throws FrameworkException
    {
        if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
        {
            Debug.log(Debug.SYSTEM_CONFIG,
                      "Initializing state def configurations...");
        }

        NVPair[] stateDefConfigs = null;
        try
        {
            //Get the state def configuration from the repository.
            //There can be mutiple configuration files in the directory repository/stateDef.
            RepositoryManager repositoryManager = RepositoryManager.getInstance();
            stateDefConfigs = repositoryManager.getAllMetaData ( STATE_DEF_CONFIG, true );
        }
        catch ( NotFoundException nfe )
        {
            String errMsg = "No meta-data configured for state definition in the Repository.";
            Debug.error(errMsg);
            throw new FrameworkException ( errMsg );
        }
        catch ( RepositoryException e )
        {
            throw new FrameworkException ( "Unable to get meta-data for state definition from the Repository: " + e.toString() );
        }

        stateDefContainerMap = new HashMap();

        // Construct the state defintions using the meta-data from all the files.
        for ( int Ix=0; Ix<stateDefConfigs.length; Ix++ )
        {
            StateDefContainer stateDefContainer = new StateDefContainer( (String)(stateDefConfigs[Ix].value) );

            String stateDefContainerName = stateDefContainer.stateDefContainerName;

            if (stateDefContainerMap.containsKey(stateDefContainerName))
            {
                Debug.warning( "Overwriting configuration for state def container [" + stateDefContainerName + "]." );
            }
            stateDefContainerMap.put(stateDefContainer.stateDefContainerName, stateDefContainer);

        }

        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
        {
            Debug.log(Debug.SYSTEM_CONFIG,
                      "Finished initializing state def configuration.");
        }

    }//initialize

    /**
     * Gets the String value from a ParsedXPath, returning null if no value is
     * present
     *
     * @param xpath The xpath
     * @param node The given node
     *
     * @throws FrameworkException If fails.
     *
     * @return The string value of the (first) node specified by the xpath.
     * Or null if no nodes matching the xpath.
     */
    public static String getString(ParsedXPath xpath, Node node)
        throws FrameworkException
    {
        List nodes = xpath.getNodeList(node);
        if (nodes.size() < 1)
        {
            return null;
        }
        else
        {
            return ( (Node) nodes.get(0)).getNodeValue();
        }
      }

      public static void main(String[] args) throws Exception
      {

          //For Testing
          /*
          args = new String[] {
              "TestMeta/DL.xml",
              "TestMeta" // This is the folder containing service definitions
          };
           */
          Properties debugProps = new Properties();
          debugProps.put(Debug.DEBUG_LOG_LEVELS_PROP, "all");
          //debugProps.put(Debug.LOG_FILE_NAME_PROP, "ServiceDefTest.log");

          //DebugConfigurator.configure(debugProps, null);
          Debug.configureFromProperties(debugProps);
          Debug.showLevels();

          Debug.log(Debug.UNIT_TEST, "StateDefContainer: Start testing...");

          String serviceType = "TestStateDef";

          doTest(serviceType, null, null);

          doTest(serviceType, "Done", "request");

          doTest(serviceType, "WaitForSOC", "supplement");

          doTest(serviceType, "Intial", "Blah");

          doTest(serviceType, "Blah", "foc");

          doTest(serviceType, "Blah", "Blah");

          doTest("Blah", "Blah", "Blah");

          Debug.log(Debug.UNIT_TEST, "Testing getStateDefContainer...");

          StateDefContainer stateDefContainer = StateDefContainer.getStateDefContainer(serviceType);

          doTest(stateDefContainer, null, null);

          Debug.log(Debug.UNIT_TEST, "StateDefContainer: Done.");
  }

    private static void doTest(String stateDefContainerName, String stateName, String messageType)
    {
        try
        {

            StateDefContainer stateDefContainer = StateDefContainer.getStateDefContainer(stateDefContainerName);

            doTest(stateDefContainer, stateName, messageType);
        }
        catch (FrameworkException fe)
        {
            Debug.error("Failed to retrieve state def.");
            Debug.logStackTrace(fe);
        }
    }

    private static void doTest(StateDefContainer stateDefContainer, String stateName, String messageType)
    {
        try
        {

            Debug.log(Debug.UNIT_TEST, "GetAllowableActions for state [" + stateName +
                    "], messageType [" + messageType + "]...");

            List actions = stateDefContainer.getAllowableActions(stateName, messageType);

            Debug.log(Debug.UNIT_TEST, "Returned the following actions: [" +
                    actions + "].");
        }
        catch (FrameworkException fe)
        {
            Debug.error("Test failed.");
            Debug.logStackTrace(fe);
        }
    }

    /**
     * Collection of xpaths to build state defs
     */
    private static class StateDefXPath
    {
        private static final
            ParsedXPath STATE_DEF_CONTAINER = getParsedXPath("/StateDefContainer");
        private static final
            ParsedXPath NAME = getParsedXPath("@name");
        private static final
            ParsedXPath STATE_DEF = getParsedXPath("StateDef");
        private static final
            ParsedXPath MESSAGE_TYPE = getParsedXPath("MessageType");
        private static final
            ParsedXPath ACTION_NAME = getParsedXPath("Actions/Name/@value");

        private static final
            ParsedXPath REF_TYPE = getParsedXPath("ExternalStateDefRef");
        private static final
            ParsedXPath VALUE = getParsedXPath("@value");
        private static final
            ParsedXPath TYPE = getParsedXPath("@type");

        // Do not initiate this class.
        private StateDefXPath()
        {
        }

        /**
         * This method returns the correspondign xpath for a string.
         * Since the xpaths are known at design time, the exception shouldn't
         * happen during runtime.
         * @param xpath String
         * @return ParsedXPath
         */
        private static ParsedXPath getParsedXPath(String xpath)
        {
            try
            {
                return new ParsedXPath(xpath);
            }
            catch (FrameworkException ex)
            {
                // Since the xpaths are known at design time, it's safe to swallow
                // the exception, which shouldn't happen during runtime.
                Debug.error("Invalid xpath given.");
                return null;
            }
        }
    }

}
