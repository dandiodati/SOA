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
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.debug.*;
import java.awt.Dimension;



/**
 * ActionDef represents an action that may be performed on a service
 * component.  Whether or not the action may be performed for an instance
 * of a component is determined by the manager.
 */
public class ActionDef
{
    /**
     * indicates that the message type result does not change
     * can be used for actions that need to message type to pass through.
     * This allows a single action def to be used in multiple cases
     * instead of defining different ones.
     */
    public static final String PASS_MSG_TYPE_NAME = "PASSTHROUGH";


    public static final MessageTypeDef PASS_MSG_TYPE_DEF = new MessageTypeDef.PassThrough();






    /** Action name (matches the name used by the manager) */
    private String actionName;

    /* This is the action key that gets sent to the backend */
    private String key;

    /** Display name for the action */
    private String displayName;

    /** Full name for the action */
    private String fullName;

    /** Append data for action*/
    private String appendQueryData;

    /** Controlling Permission associated with the action. It controls the availability of the action on the GUI.*/
    private String controllingPermission;

    /** Conversion Required associated with the action. It is used to support Multi Version.*/
    private String conversionRequired;

    /** This specifies whether 'Locking' feature is available to the Action or not.*/
    private boolean isLockingSupported;

    /** Controlling Query associated with the action. It controls the availability of the action on the GUI.*/
    private String controllingQuery;

    /** On convert-order action the Service Type to be converted to */
    private String serviceTypeToConvert;

    /** Help text for the action */
    private String helpText;

    /** message type associated with this action */
    private MessageTypeDef  messageType;

    private String redirectPage;

    private Dimension newWin;

    /** Node that specifies whether to present a form to edit the request */
    private boolean editRequired = true;

    /** Node that specifies whether to check this action against state configuration */
    private boolean checkState = false;

    /** Node that specifies whether this action is disabled when the order is locked by
     * another user.  It is true by default since almost all actions will fall into this
     * category.
     */
    private boolean disableWithoutLock = true;

    // indicates if this action is allowed when there is no state associated with
    // the request.
    private boolean allowedInEmptyState = true;
    

    /**
     * Constructor
     */
    public ActionDef()
    {
    }

    /**
     * Returns the id to indentify this action def object.
     * @return String
     */
    public String getActionName()
    {
        return actionName;
    }

    /**
     * Returns the action key for this action def.
     * @return String
     */
    public String getActionKey()
    {
        return key;

    }


    /**
     * Returns the display name for the action
     * @return String
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Returns the full name for the action
     * @return String
     */
    public String getFullName()
    {
        return fullName;
    }

    /**
     * Returns the controlling permission associated with the action
     * @return String
     */
    public String getControllingPermission()
    {
        return controllingPermission;
    }

    /**
     * Returns the controlling query associated with the action
     * @return String
     */
    public String getControllingQuery()
    {
        return controllingQuery;
    }

    /**
     * Returns the conversion Required associated with the action.
     * It is used to support Multi Version.
     * @return String
     */
    public String getConversionRequired()
    {
        return conversionRequired;
    }

    /**
     * Returns whether 'Locking' feature is supported for the Action
     *
     * @return boolean
     */
    public boolean getIsLockingSupported()
    {
        return isLockingSupported;
    }

    /**
     * Returns the Service Type of the order to be converted to on convert-order action.
     * @return String
     */
    public String getServiceTypeToConvert()
    {
        return serviceTypeToConvert;
    }


    /**
     * Returns the message type name associated with this action
     * @return String
     */
    public String getMessageType()
    {
        return messageType.getName();
    }

    /**
     * Help text for the action
     * @return String
     */
    public String getHelpText()
    {
        return helpText;
    }

    /**
     * Returns the custom jsp page to handle this action.
     * May return null.
     *
     * @return a <code>String</code> value
     */
    public String getRedirectPage()
    {
        return redirectPage;
    }

  /**
     * Returns the append query data value associated with this action.
     * May return null.
     *
     * @return String value
     */
    public String getAppendQueryData()
    {
        return appendQueryData;
    }
  /**
     * Indicates if this action should open in a new window
     * May return null.
     *
     * @return a <code>String</code> value
     */
    public Dimension getNewWindow()
    {
        return newWin;
    }


    /**
     * Indicates whether an action needs to be validated against the state.
     *
     * @return  true if validation is required, false otherwise.
     */
    public boolean getCheckState()
    {
        return this.checkState;
    }

    /**
     * Indicates whether this action is disabled when the order is locked by another user.
     *
     * @return  true or false.
     */
    public boolean getDisableWithoutLock()
    {
        return this.disableWithoutLock;
    }

    /**
     * Indicates if this action is allowed when the state associated with the request
     * is empty. This occurs for managers when creating a new bundle.
     * In other cases it my not be used.
     *
     * @return  true or false.
     */
    public boolean getAllowedInEmptyState()
    {
        return allowedInEmptyState;
    }

    /**
     * Indicates whether an action can be auto-submitted without intermediary-edit session by the user.
     *
     * @return  true if intermediatry-edit session is required, false otherwise.
     */
    public boolean getEditRequired()
    {
        return this.editRequired;
    }

    /**
     * Reads this action definition from a node in an XML document
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
        DebugLogger log = DebugLogger.getLoggerLastApp(getClass() );

        // get the name
        actionName = buildCtx.getString(buildCtx.xpaths.idPath, ctx);

        key = buildCtx.getString(buildCtx.xpaths.keyPath, ctx);

        if (!StringUtils.hasValue(key))
            key = actionName;
       
        // display name
        displayName = buildCtx.getString(buildCtx.xpaths.displayNamePath, ctx);

        // full name
        fullName = buildCtx.getString(buildCtx.xpaths.fullNamePath, ctx);

        // help text
        List helpNodes = buildCtx.xpaths.helpPath.getNodeList(ctx);
        if (helpNodes.size() > 0)
        {
            Node helpNode = (Node)helpNodes.get(0);

            helpText = DOMWriter.toString(helpNode.getFirstChild(), true);
        }

		// Controlling Permission associated with the action
		controllingPermission = buildCtx.getString(buildCtx.xpaths.controllingPermissionPath, ctx);

        // Controlling Query associated with the action
        controllingQuery = buildCtx.getString(buildCtx.xpaths.controllingQueryPath, ctx);

        // conversion Required associated with the action
        conversionRequired = buildCtx.getString(buildCtx.xpaths.conversionRequiredPath, ctx);

        //appendQueryData associated with action
        appendQueryData = buildCtx.getString(buildCtx.xpaths.appendQueryDataPath, ctx);

        // isLockingSupported
        String islockingSupportedStr = buildCtx.getString(buildCtx.xpaths.lockingPath, ctx);

        if (StringUtils.hasValue(islockingSupportedStr))
        {
            isLockingSupported = StringUtils.getBoolean(islockingSupportedStr);
        }

	    //On convert-order action the Service Type to be converted to
		serviceTypeToConvert = buildCtx.getString(buildCtx.xpaths.serviceTypeToConvertPath, ctx);

        // Get a custom redirect page if specified
        redirectPage = buildCtx.getString(buildCtx.xpaths.redirectPagePath, ctx);
        String newWinWStr = buildCtx.getString(buildCtx.xpaths.newWinWidthPath, ctx);

        String newWinHStr = buildCtx.getString(buildCtx.xpaths.newWinHeightPath, ctx);


        if (StringUtils.hasValue(newWinHStr) && StringUtils.hasValue(newWinWStr) )     {
            int newWinW = Integer.parseInt(newWinWStr);
            int newWinH = Integer.parseInt(newWinHStr);
            newWin = new Dimension(newWinW, newWinH);
        }


        String checkStateStr = buildCtx.getString(buildCtx.xpaths.checkStatePath, ctx);

        if ( StringUtils.hasValue(checkStateStr) )
        {
            checkState = StringUtils.getBoolean(checkStateStr);
        }

        String disableWithoutLockStr = buildCtx.getString(buildCtx.xpaths.disableWithoutLockPath, ctx);

        if (StringUtils.hasValue(disableWithoutLockStr))
        {
            disableWithoutLock = StringUtils.getBoolean(disableWithoutLockStr);
        }

        String allowedInEmptyStateStr = buildCtx.getString(buildCtx.xpaths.allowInEmptyStatePath, ctx);

        allowedInEmptyState = StringUtils.getBoolean(allowedInEmptyStateStr, allowedInEmptyState);
        

        if (StringUtils.hasValue(disableWithoutLockStr))
        {
            disableWithoutLock = StringUtils.getBoolean(disableWithoutLockStr);
        }

        String editRequiredStr = buildCtx.getString(buildCtx.xpaths.editRequiredPath, ctx);

        if ( StringUtils.hasValue(editRequiredStr) )
        {
            editRequired = StringUtils.getBoolean(editRequiredStr);
        }


        // message type
        String messageTypeStr = buildCtx.getString(buildCtx.xpaths.messageTypeResultPath, ctx);
        if ( StringUtils.hasValue( messageTypeStr) ) {

            // if the message type result is passthough then use the
            // static passthough message type def object
            if (messageTypeStr.equals(PASS_MSG_TYPE_NAME)) {
                messageType = PASS_MSG_TYPE_DEF;
            }
            else {
                messageType = buildCtx.getMessageType(messageTypeStr);
            }

            if ( messageType == null) {
                log.error("ActionDef [" + actionName + "] at location [" + buildCtx.toXPath(ctx) +"] contains a bad message type reference [" + messageTypeStr +"]");
            }
        }

        if ( messageType == null) {
          log.error("ActionDef [" + actionName +"] must have a message type defined.");
          throw new FrameworkException("ActionDef [" + actionName +"] must have a message type defined.");
        }

    }
}
