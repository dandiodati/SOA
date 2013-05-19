/**
 * Copyright (c) 2003 Neustar, Inc. All rights reserved.
 *
 * $Header: //nfcommon/main/com/nightfire/framework/util/CustomerContext.java#1 $
 */

package com.nightfire.framework.util;


import org.w3c.dom.*;
import java.util.*;

import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.xml.*;


/**
 * Utility providing customer-specific contextual information to be used
 * in constraining access to data relevant to the current customer.
 */
public class CustomerContext
{
    /**
     * Default customer identifier if no customer-specific identifier has
     * been given (same as system).
     */
    public static final String DEFAULT_CUSTOMER_ID = "DEFAULT";

    /**
     * Default user identifier if one has not already been set at the
     * time it is referenced.
     */
    public static final String DEFAULT_USER_ID = "nfsystem";

    /*
     * Constant for the value of the Customer ID which represents the absence of a Customer.
     */
    public static final String NO_CUSTOMER_ID = "NONE";

    /**
     * Maximum length of customer identifier.
     */
    public static final int CUSTOMER_ID_LENGTH = 10;

    /**
     * Name of node whose value attribute contains the customer identifier.
     */
    public static final String CUSTOMER_ID_NODE = "CustomerIdentifier";

    /**
     * Name of property whose value contains the customer identifier.
     */
    public static final String CUSTOMER_ID_PROP = "CustomerId";

    /**
     * Name of database table column containing the customer identifier.
     */
    public static final String CUSTOMER_ID_COL_NAME = "CustomerId";

    /**
     * Default SubDomain identifier if no SubDomain-specific identifier has
     * been given.
     */
    public static final String DEFAULT_SUBDOMAIN_ID = "";

    /*
     * Constant for the value of the SubDomain ID which represents the absence of a Customer.
     */
    public static final String NO_SUBDOMAIN_ID = "NONE";

    /**
     * Maximum length of SubDomain identifier.
     */
    public static final int SUBDOMAIN_ID_LENGTH = 20;

    /**
     * Name of property whose value contains the SubDomain identifier.
     */
    public static final String SUBDOMAIN_ID_PROP = "SubDomainId";

    /**
     * Name of node whose value attribute contains the user identifier.
     */
    public static final String USER_ID_NODE = "UserIdentifier";

    /**
     * Name of database table column containing the user identifier.
     */
    public static final String USER_ID_COL_NAME = "UserId";

    /**
     * Name of node whose value attribute contains the user password.
     */
    public static final String USER_PASSWORD_NODE = "UserPassword";

    /**
     * Name of node whose value attribute contains the user password.
     */
    public static final String USER_PASSWORD_ENCODING_NODE = "IsUserPasswordEncoded";

    /**
     * Name of node whose value attribute contains the interface version.
     */
    public static final String INTERFACE_VERSION_NODE = "InterfaceVersion";

    /**
     * Name of database table column containing the interface version.
     */
    public static final String INTERFACE_VERSION_COL_NAME = "InterfaceVersion";

    /**
     * Name of node whose value attribute contains the interface version.
     */
    public static final String SUBDOMAIN_ID_NODE = "SubDomainId";

    /**
     * Name of database table column containing the SubDomainId.
     */
    public static final String SUBDOMAIN_ID_COL_NAME = "SubDomainId";

    /**
     * Name of node whose value attribute contains the unique message id.
     */
    public static final String MESSAGE_ID = "UniqueMsgId";

    /**
     * Name of node whose value attribute contains the unique identifier of the message.
     */
    public static final String UNIQUE_IDENTIFIER = "UniqueIdentifier";


    /**
     * Get the customer context object associated with the current thread of execution.
     *
     * @return  The current customer context object.
     *
     * @exception  FrameworkException  Thrown on errors.
     */
    public static CustomerContext getInstance ( ) throws FrameworkException
    {
        CustomerContext currentContext = (CustomerContext)contexts.get( );

        if ( currentContext == null )
        {
            currentContext = new CustomerContext( );

            contexts.set( currentContext );
        }

        return currentContext;
    }


    /**
     * Test to see if the context is currently referring to the default system customer.
     *
     * @return  'true' if the current customer id value is "DEFAULT", otherwise 'false'.
     */
    public boolean isDefaultCustomer ( )
    {
        return( DEFAULT_CUSTOMER_ID.equals( customerId ) );
    }


    /**
     * Test to see if the context is currently referring to the NONE customer.
     *
     * @return  'true' if the current customer id value is "NONE", otherwise 'false'.
     */
    public boolean isNoCustomerID ( )
    {
        return(NO_CUSTOMER_ID.equals(customerId));
    }

    /**
     * Propagate customer-specific information from header to context, or visa-versa
     * (depending upon where the data currently resides).
     *
     * @param  header  XML text containing the header to either populate or
     *                 extract customer-specific information from, or null if
     *                 unavailable.
     *
     * @return  The (possibly-modified) header in text form.
     *
     * @exception  FrameworkException  Thrown on errors.
     */
    public String propagate ( String header ) throws FrameworkException
    {
        Document headerDoc = null;

        if ( header != null )
        {
            XMLMessageParser p = new XMLMessageParser( header );

            headerDoc = p.getDocument( );
        }

        XMLMessageGenerator g = new XMLMessageGenerator( propagate( headerDoc ) );

        return( g.generate() );
    }


    /**
     * Propagate customer-specific information from header to context, or visa-versa
     * (depending upon where the data currently resides).
     *
     * @param  header  XML Document containing the header to either populate or
     *                 extract customer-specific information from, or null if
     *                 unavailable.
     *
     * @return  The (possibly-modified) header in DOM form.
     *
     * @exception  FrameworkException  Thrown on errors.
     */
    public Document propagate ( Document header ) throws FrameworkException
    {
        XMLMessageGenerator gen = null;

        if ( header == null )
        {
            gen = new XMLMessageGenerator( "header" );

            header = gen.getDocument( );
        }

        XMLMessageParser parser = new XMLMessageParser( header );

        // If customer-id is in header, use header contents to populate context.
        if ( parser.valueExists( CUSTOMER_ID_NODE ) )
        {
            setCustomerID( parser.getValue( CUSTOMER_ID_NODE ) );

            if ( parser.valueExists( USER_ID_NODE ) )
                setUserID( parser.getValue( USER_ID_NODE ) );

            if ( parser.valueExists( USER_PASSWORD_NODE ) )
                setUserPassword( parser.getValue( USER_PASSWORD_NODE ) );

            if ( parser.valueExists( INTERFACE_VERSION_NODE ) )
                setInterfaceVersion( parser.getValue( INTERFACE_VERSION_NODE ) );

            if ( parser.valueExists( SUBDOMAIN_ID_NODE ) )
                setSubDomainId ( parser.getValue( SUBDOMAIN_ID_NODE ) );

            if ( parser.valueExists( MESSAGE_ID ) )
                setMessageId ( parser.getValue( MESSAGE_ID ) );

            if ( parser.valueExists( UNIQUE_IDENTIFIER ) )
                setUniqueIdentifier ( parser.getValue( UNIQUE_IDENTIFIER ) );

            if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
                Debug.log( Debug.SYSTEM_CONFIG, describe() );

            return( parser.getDocument() );
        }
        else
        {
            // No customer-id in header, so populate it from context.
            gen = new XMLMessageGenerator( header );

            gen.setValue( CUSTOMER_ID_NODE, customerId );

            if ( StringUtils.hasValue( userId ) )
                gen.setValue( USER_ID_NODE, userId );

            if ( StringUtils.hasValue( userPassword ) )
                gen.setValue( USER_PASSWORD_NODE, userPassword );

            if ( StringUtils.hasValue( interfaceVersion ) )
                gen.setValue( INTERFACE_VERSION_NODE, interfaceVersion );

            String sdId = subDomainId;
            
            if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
                Debug.log(Debug.SYSTEM_CONFIG, "subDomainId [" + sdId + "]");

            if(StringUtils.hasValue(sdId))
                gen.setValue( SUBDOMAIN_ID_NODE, sdId);
            else
                gen.setValue( SUBDOMAIN_ID_NODE, "");

            if(StringUtils.hasValue(messageId))
                gen.setValue( MESSAGE_ID, messageId);

            if(StringUtils.hasValue(uniqueIdentifier))
                gen.setValue( UNIQUE_IDENTIFIER, uniqueIdentifier);

            if(Debug.isLevelEnabled (Debug.SYSTEM_CONFIG))
                Debug.log(Debug.SYSTEM_CONFIG, describe ());

            return( gen.getDocument() );
        }
    }


    /**
     * Get the customer ID from the currently-active context object.
     *
     * @return  The customer identifier.
     *
     * @exception  FrameworkException  Thrown on errors.
     */
    public String getCustomerID ( ) throws FrameworkException
    {
        return customerId;
    }


    /**
     * Set the customer ID on the currently-active context object.
     *
     * @param cid  The customer identifier.
     *
     * @return  The previous customer identifier value.
     *
     * @exception  FrameworkException  Thrown on errors.
     */
    public String setCustomerID ( String cid ) throws FrameworkException
    {
        String temp = customerId;

        if ( cid == null )
            customerId = DEFAULT_CUSTOMER_ID;
        else
            customerId = cid;

        return temp;
    }



    /**
     * Set the Customer to be the NONE Customer.
     *
     */
    public void setNoCustomer ( ) throws FrameworkException
    {
        setCustomerID(NO_CUSTOMER_ID);
    }


    /**
     * Get the user ID from the currently-active context object.
     *
     * @return  The user identifier.
     *
     * @exception  FrameworkException  Thrown on errors.
     */
    public String getUserID ( ) throws FrameworkException
    {
        if (!StringUtils.hasValue(userId))
        {
            userId = DEFAULT_USER_ID;
        }

        return userId;
    }


    /**
     * Set the user ID on the currently-active context object.
     *
     * @param uid  The user identifier.
     *
     * @return  The previous user identifier value.
     *
     * @exception  FrameworkException  Thrown on errors.
     */
    public String setUserID ( String uid ) throws FrameworkException
    {
        String temp = userId;

        userId = uid;

        return temp;
    }


    /**
     * Get the SubDomain Id from the currently-active context object.
     *
     * @return  The SubDomainId value.
     */
    public String getSubDomainId ()
    {
        return subDomainId;
    }

    /**
     * Set the SubDomain ID on the currently-active context object.
     *
     * @param subDomainId The SubDomain name
     */
    public void setSubDomainId (String subDomainId)
    {
        this.subDomainId = subDomainId;
    }

    /**
     * Get the user password from the currently-active context object.
     *
     * @return  The user password.
     *
     * @exception  FrameworkException  Thrown on errors.
     */
    public String getUserPassword ( ) throws FrameworkException
    {
        return userPassword;
    }

    /**
     * Set the user password on the currently-active context object.
     *
     * @param up  The user password.
     *
     * @return  The previous user password value.
     *
     * @exception  FrameworkException  Thrown on errors.
     */
    public String setUserPassword ( String up ) throws FrameworkException
    {
        String temp = userPassword;

        userPassword = up;

        return temp;
    }

    /**
     * Get the interface version from the currently-active context object.
     *
     * @return  The interface version.
     *
     * @exception  FrameworkException  Thrown on errors.
     */
    public String getInterfaceVersion ( ) throws FrameworkException
    {
        return interfaceVersion;
    }


    /**
     * Set the interface version on the currently-active context object.
     *
     * @param intfver  The interface version.
     *
     * @return  The previous interface version value.
     *
     * @exception  FrameworkException  Thrown on errors.
     */
    public String setInterfaceVersion ( String intfver ) throws FrameworkException
    {
        String temp = interfaceVersion;

        interfaceVersion = intfver;

        return temp;
    }


    /**
     * Reset the currently-active context object
     * to the default system values.
     *
     * @exception  FrameworkException  Thrown on errors.
     */
    public void cleanup ( ) throws FrameworkException
    {
        setCustomerID( DEFAULT_CUSTOMER_ID );
        setUserID( null );
        setUserPassword( null );
        setInterfaceVersion( null );
        setSubDomainId ( null );
        // clean up the dataContainer
        if(dataContainer!=null)
             dataContainer.clear();
        dataContainer=null;
        setMessageId(null);
        setUniqueIdentifier(null);
    }


    /**
     * Returns a human-readable description of the current customer context.
     *
     * @return  Context description in text form.
     */
    public String describe ( )
    {
        StringBuffer sb = new StringBuffer( );

        sb.append( "Customer-context: customer-id [" );
        sb.append( customerId );
        sb.append( "], user-id [" );
        sb.append( userId );
        sb.append( "], interface-version [" );
        sb.append( interfaceVersion );
        sb.append("], subDomainId [");
        sb.append( subDomainId );
        sb.append( "]" );

        return( sb.toString() );
    }


    private CustomerContext ( )
    {
        // Not to be instantiated by external clients.
        customerId = "DEFAULT";
        subDomainId = "";
    }

    /**
     * Add customer-id to the given name. The resultant name will be
     * <CustomerId_name>
     *
     * @param  name to be modified.
     *
     * @return  The modified name with the CustomerId prepended to it.
     *
     * @exception  FrameworkException  Thrown on errors.
     */
    public String addCustomerId ( String name )
        throws FrameworkException
    {
        return( getCustomerID() + "_" + name );
    }


    /**
     * Replace all instances of <CustomerId_name> in the target string with <name>.
     *
     * @param  target  Target text to modify.
     * @param  name  Name text used to construct the customer-specific name.
     *
     * @return  The modified text with the customer identifier removed.
     *
     * @exception  FrameworkException  Thrown on errors.
     */
    public String removeCustomerId ( String target, String name )
        throws FrameworkException
    {
        return( StringUtils.replaceSubstrings( target, addCustomerId( name ), name ) );
    }

    /**
     * Set the unique message id.
     * @param id  String
     */
    public void setMessageId(String id) throws FrameworkException
    {
            messageId = id;
    }

    /**
     * Get the unique request-id if it is set, else will return null
     * @return String
     */
    public String getMessageId()
    {
        return messageId;
    }

    /**
     * Set the unique identifer (e.g. PON/TXNUM/REQNO).
     * @param uniqueIdentifier  String
     */
    public void setUniqueIdentifier(String uniqueIdentifier) throws FrameworkException
    {
        this.uniqueIdentifier = uniqueIdentifier;
    }

    /**
     * Get the unique identifer (e.g. PON/TXNUM/REQNO) if it is set, else will return null
     * @return String uniqueIdentifier
     */
    public String getUniqueIdentifier()
    {
        return this.uniqueIdentifier;
    }

    /**
    * This is used in accessing the thread-specific CustomerContext instance.
    */
    private static InheritableThreadLocal contexts = new InheritableThreadLocal(){

       /**
       * This makes a copy of the parent thread's CustomerContext instead
       * of referencing the parent thread's instance directly, which is the
       * default behavior.
       */
       protected Object childValue(Object parentValue){

           CustomerContext parentContext = (CustomerContext) parentValue;
           CustomerContext childContext = new CustomerContext();

           if(parentContext != null){

              try{
                 childContext.setCustomerID( parentContext.getCustomerID() );
                 childContext.setUserID( parentContext.getUserID() );
                 childContext.setUserPassword( parentContext.getUserPassword() );
                 childContext.setInterfaceVersion( parentContext.getInterfaceVersion() );
                 childContext.setSubDomainId ( parentContext.getSubDomainId () );
                 childContext.setOtherItems( parentContext.getOtherItems() );
              }
              catch(FrameworkException fex){

                 // the way things are currently implemented, this will
                 // never ever happen, because even though these methods'
                 // signatures claim to throw FrameworkExceptions, their
                 // implementations really don't. So, with that said...
                 Debug.error("An error occured while trying to copy the parent"+
                             " customer context into a new child thread: "+
                             fex);

              }

           }

           return childContext;

       }

    };

    public Object get ( String name )
    {
        if (dataContainer == null)
            return null;
        else
            return dataContainer.get( name );
    }

    /**
     * Method to obtain the entire content of the customer context in a document.
     * The root will be <header>
     * @return Document containing context content.
     * @throws FrameworkException if something goes wrong.
     */
    public Document getAll() throws FrameworkException
    {
        Document out = null;
        out = propagate( out );
        if( dataContainer != null )
        {
            XMLMessageGenerator gen = new XMLMessageGenerator(out);

            for( Iterator iter = dataContainer.entrySet().iterator(); iter.hasNext(); )
            {
                Map.Entry e = (Map.Entry) iter.next();
                gen.setValue( e.getKey().toString(), e.getValue().toString() );

            }
        }

        return out;
    }

    public void set ( String name, Object value )
    {
        if ( dataContainer == null )
            dataContainer = new HashMap();
        dataContainer.put( name, value );
    }

    /**
     * Return miscellaneous items contains in the map data container.
     */
    public Map getOtherItems ( )
    {
        return dataContainer;
    }

    /**
     * Copy the contents of map into the current CustomerContext's other
     * item container.
     */
    public void setOtherItems ( Map map )
    {
        if ( map == null )
            return;
        if ( dataContainer == null )
            dataContainer = new HashMap();
        dataContainer.putAll( map );
    }

    // The customer id value should never be null.
    private String customerId = DEFAULT_CUSTOMER_ID;
    private String userId;
    private String userPassword;
	private String subDomainId;
    private String interfaceVersion;
    // Unique message id assigned for tracking requests
    private String messageId;
    // Unique identifier of the message (e.g. PON/TXNUM/REQNO)
    private String uniqueIdentifier;


    //Container for any other thread specific items.
    private Map dataContainer;
}
