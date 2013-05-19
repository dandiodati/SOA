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
import com.nightfire.framework.message.util.xml.DOMWriter;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.webgui.core.svcmeta.*;
import com.nightfire.framework.debug.*;


/**
 * ServieGroupDef represents the definition of a service group.  It contains information
 * about services in the group.
 */
public class ServiceGroupDef
{
    /** Identifies this service group */
    private String id;

    /** Display name for the service group */
    private String displayName;

    /** Full name for the service group */
    private String fullName;

    /**  Variable Used to get ValidateRequired XML tag */
    private boolean validateRequired;

    /**  Variable Used to get UpdateRemark XML tag */
    private boolean updateRequestFieldSupport;

    /**  Variable Used to check whether UOM supported */
    private boolean uomSupport;

    /**  Variable Used to get SaveRequired XML tag */
    private boolean saveRequired;

    /**  Variable Used to get TemplateSupport XML tag */
    private boolean templateSupport;

    /** Modify permission for the service group */
    private String modifyPermission;

    /** Fallout only permission for the service group */
    private String falloutPermission;

    /** Help text for the service group */
    private String helpText;

    /** List of services that may occur in the service group */
    private ArrayList serviceList = new ArrayList();

   /** List of suppliers which do not want 997 support */
    private ArrayList hide997List = new ArrayList();

     /** List of suppliers which want 997 support */
    private ArrayList show997List = new ArrayList();

       /** List of suppliers which  want Total Row Count support */
    private ArrayList totalRowCountList = new ArrayList();

    /** map to track ids to services **/
    private HashMap serviceMap = new HashMap();

    /** Unmodifiable view of services */
    private List ro_serviceList = null;

    /** Unmodifiable view of supplier which want to hide 997 */
    private List ro_hide997List = null;

    /** Unmodifiable view of supplier which want to hide 997 */
    private List ro_show997List = null;

    /** Unmodifiable view of suppliers which want to support Total Row Count */
    private List ro_totalRowCountList = null;


     /** Indicates whether this service type can be created via the GUI. **/
    private boolean userCreatable = true;

    /** This specifies whether 'Customer Use' feature is available to service group or not.*/
    private boolean isCustomerUseSupported;

    /** This specifies whether 'Customer Use' feature is read-only for service group or not.*/
    private boolean isCustomerUseReadOnly;

    /** This specifies whether 'Locking' feature is available to this service group or not.*/
    private boolean isLockingSupported;

    /** This specifies whether 'Printing' feature is available to this service group or not.*/
    private boolean isPrintingSupported;

    /** This specifies whether 'Pagination on Order-History page' feature is available to this service group or not.*/
    private boolean isHistoryPagingSupported;

    /** Used to access the list of services */
    private ServiceDefContainer serviceDefContainer = null;

    private DebugLogger log;


    /**
     * Constructor for a URL.  This constructor loads a document from the
     * URL using the system default character encoding.
     *
     * @param FrameworkException Thrown if the document cannot be loaded
     */
    protected ServiceGroupDef(ServiceDefContainer serviceDefContainer) throws FrameworkException
    {
      log = DebugLogger.getLoggerLastApp(getClass() );

      this.serviceDefContainer = serviceDefContainer;

      ro_serviceList      = Collections.unmodifiableList(serviceList);
      ro_hide997List      = Collections.unmodifiableList(hide997List);
      ro_show997List      = Collections.unmodifiableList(show997List);
      ro_totalRowCountList      = Collections.unmodifiableList(totalRowCountList);

    }

    /**
     * Returns this service group definition's ID.
     */
    public String getID()
    {
        return id;
    }

    /**
     * Returns the display name for the service group
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Returns the full name for the service group
     */
    public String getFullName()
    {
        return fullName;
    }

    /**
     * Returns the help text for the service group
     */
    public String getHelpText()
    {
        return helpText;
    }

    /**
     * Returns the validateRequired for the service group
     */
    public boolean getValidateRequired()
    {
        return validateRequired;
    }

    /**
     * Returns the validateRequired for the service group
     */
    public boolean getUpdateRequestFieldSupport()
    {
        return updateRequestFieldSupport;
    }

    /**
     * Returns the saveRequired for the service group
     */
    public boolean getSaveRequired()
    {
        return saveRequired;
    }

    /**
     * Returns the uomSupport for the service group
     */
    public boolean getUomSupport()
    {
        return uomSupport;
    }

    /**
     * Returns the templateSupport for the service group
     */
    public boolean getTemplateSupport()
    {
        return templateSupport;
    }


    /**
     * Returns the modify permission for the service group
     */
    public String getModifyPermission()
    {
        return modifyPermission;
    }

    /**
     * Returns the fallout permission for the service group
     */
    public String getFalloutPermission()
    {
        return falloutPermission;
    }


    /**
     * Obtains a particular service by id
     *
     * @param id  The id of the service to return
     */

   public boolean getUserCreatable()
    {
        return userCreatable;
    }

    public ServiceDef getService(String id)
    {
        return (ServiceDef)serviceMap.get(id);
    }

    /**
     * Obtains a list of all the services
     */
    public List getServices()
    {
        return ro_serviceList;
    }

    /**
     * Obtains a list of all the suppliers supporting Total Row Count Flag
     */
    public List getTotalRowCountList()
    {
        return ro_totalRowCountList;
    }

    /**
     * Obtains a list of all the suppliers not supporting 997
     */
    public List getAnti997Supporter()
    {
        return ro_hide997List;
    }

    /**
     * Obtains a list of all the suppliers supporting 997
     */
    public List getShow997Supporter()
    {
        return ro_show997List;
    }
    /**
     * Returns whether 'Customer Use' feature is supported for the service group
     *
     * @return boolean
     */
    public boolean getIsCustomerUseSupported ()
    {
        return isCustomerUseSupported;
    }

    /**
     * Returns whether 'Customer Use' is read only for the service group
     *
     * @return boolean
     */
    public boolean getIsCustomerUseReadOnly ()
    {
        return isCustomerUseReadOnly;
    }

    /**
     * Returns whether 'Locking' feature is supported for the service group
     *
     * @return boolean
     */
    public boolean getIsLockingSupported ()
    {
        return isLockingSupported;
    }

    /**
     * Returns whether 'Pagination for order-history page' feature is supported for the service group
     *
     * @return boolean
     */
    public boolean getIsHistoryPagingSupported ()
    {
        return isHistoryPagingSupported;
    }

    /**
     * Returns whether 'Printing' feature is supported for the service group
     *
     * @return boolean
     */
    public boolean getIsPrintingSupported ()
    {
        return isPrintingSupported;
    }

    /**
     * Reads this service group definition from a node in an XML document
     *
     * @param ctx      The node to read from
     * @param buildCtx The BuildContext that contains predefined data types
     *
     * @exception FrameworkException Thrown if the definition cannot be
     *                               loaded.
     */
    public void readFromXML(Node ctx, ServiceGroupBuildContext buildCtx)
        throws FrameworkException
    {
        BuildPaths xpaths = buildCtx.xpaths;

        // get the id
        id = buildCtx.getString(xpaths.idPath, ctx);

        // display name
        displayName = buildCtx.getString(xpaths.displayNamePath, ctx);

        // full name
        fullName = buildCtx.getString(xpaths.fullNamePath, ctx);

        // modify permission
        modifyPermission = buildCtx.getString(xpaths.modifyPermissionPath, ctx);

        // fallout only permission
        falloutPermission = buildCtx.getString(xpaths.falloutPermissionPath, ctx);

        // flag indicating whether validate button to display in GUI.
        validateRequired = StringUtils.getBoolean(buildCtx.getString(xpaths.validateRequiredPath, ctx), false);

        // flag indicating whether Update remarks feature is supported. This fraeture is initially supported in ESR.
        updateRequestFieldSupport = StringUtils.getBoolean(buildCtx.getString(xpaths.updateRequestFieldSupportPath, ctx), false);

        // flag indicating whether save button to display in GUI.
        saveRequired = StringUtils.getBoolean(buildCtx.getString(xpaths.saveRequiredPath, ctx), false);

        // flag indicating whether service is UOMSupported.
        uomSupport = StringUtils.getBoolean(buildCtx.getString(xpaths.uomSupportPath, ctx), false);

        // flag indicating whether to display this group in dropdown on GUI.
        templateSupport = StringUtils.getBoolean(buildCtx.getString(xpaths.templateSupportPath, ctx), false);

          // flag indicating whether this service type is creatable via GUI.
        userCreatable = StringUtils.getBoolean(buildCtx.getString(xpaths.userCreatablePath, ctx), true);

        // flag indicating whether this field is supported for service group or not.
        isCustomerUseSupported = Boolean.valueOf(buildCtx.getString(xpaths.customerUseSupportPath, ctx)).booleanValue();

        // flag indicating whether this field is read-only for service group or not.
        isCustomerUseReadOnly = Boolean.valueOf(buildCtx.getString(xpaths.customerUseReadOnlyPath, ctx)).booleanValue();

        // isLockingSupported
        isLockingSupported = Boolean.valueOf(buildCtx.getString(xpaths.lockingPath, ctx)).booleanValue();

        // isPrintingSupported
        isPrintingSupported = Boolean.valueOf(buildCtx.getString(xpaths.printingPath, ctx)).booleanValue();

        // isHistoryPagingSupported
        isHistoryPagingSupported = Boolean.valueOf(buildCtx.getString(xpaths.historyPagingPath, ctx)).booleanValue();

        // help text
        List helpNodes = xpaths.helpPath.getNodeList(ctx);
        if (helpNodes.size() > 0)
        {
            Node helpNode = (Node)helpNodes.get(0);

            helpText = DOMWriter.toString(helpNode.getFirstChild(), true);
        }

        //anti 997 Suppliers
        List anti997Nodes = xpaths.anti997SuppliersPath.getNodeList(ctx);
        Iterator iter997 = anti997Nodes.iterator();

        while (iter997.hasNext())
        {
            Node n = (Node)iter997.next();
            String supplierName = n.getNodeValue();
            hide997List.add(supplierName);
        }

        //show 997 Suppliers
        List show997Nodes = xpaths.show997SuppliersPath.getNodeList(ctx);
        Iterator iterShow997 = show997Nodes.iterator();

        while (iterShow997.hasNext())
        {
            Node n = (Node)iterShow997.next();
            String supplierName = n.getNodeValue();
            show997List.add(supplierName);
        }

         //Total Row Count Flag Supporter Suppliers
        List totRowCntNodes = xpaths.totalRowCountSupporterSuppliersPath.getNodeList(ctx);
        Iterator iterTRC = totRowCntNodes.iterator();

        while (iterTRC.hasNext())
        {
            Node n = (Node)iterTRC.next();
            String supplierName = n.getNodeValue();
            totalRowCountList.add(supplierName);
        }

        // Load Service into List
        List serviceNodes = xpaths.servicesPath.getNodeList(ctx);
        Iterator iter = serviceNodes.iterator();

        while (iter.hasNext())
        {
            Node n = (Node)iter.next();
            String serviceId = n.getNodeValue();

            // Need to get the service
            ServiceDef service = serviceDefContainer.getService(serviceId);

            if (service == null)
            {
                //throw new InvalidServiceGroupDefException(
                //    "Could not locate service with name [" + serviceId
                //    + "] for service group [" + id + "], referenced at [" + buildCtx.toXPath(ctx)
                //    + "].");
                log.fatal(
                    "Could not locate service with name [" + serviceId
                    + "] for service group [" + id + "], referenced at [" + buildCtx.toXPath(ctx)
                    + "].");
            }
            else
            {
                service.setServiceGroup(this);

                serviceList.add(service);
                serviceMap.put(serviceId, service);
            }
        }

    }

    public static final class InvalidServiceGroupDefException extends FrameworkException
    {
        public InvalidServiceGroupDefException(String msg)
        {
            super(msg);
        }
    }


}
