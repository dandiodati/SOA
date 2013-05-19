/**
 * Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //comms/R4.4/com/nightfire/comms/ejb/GatewayBeanClient.java#1 $
 */
package com.nightfire.comms.ejb;

import java.util.*;
import java.rmi.*;

import javax.ejb.*;
import javax.rmi.*;
import javax.naming.*;

import com.nightfire.rmi.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.repository.*;
import com.nightfire.framework.environment.*;
import com.nightfire.framework.message.parser.xml.*;


/**
 * Utility class for interacting with the EJB that 
 * implements the GatewayBean interface.
 */
public class GatewayBeanClient
{
    /**
     * Name of the repository category containing gateway bean configurations.
     */
    public static final String GATEWAY_CONFIG_REPOSITORY_CATEGORY = "GatewayConfiguration";
    
    /**
     * Name of the node in the gateway configuration XML giving the gateway bean home name in the JNDI.
     */
    public static final String GATEWAY_HOME_NAME = "GatewayBeanHomeName";
    
    
    /**
     * Name of the node in the gateway configuration XML giving the URL in the JNDI.
     */
    public static final String URL = "Url";


    /**
     * Name of the node in the gateway configuration XML giving the user name in the JNDI.
     */
    public static final String USER = "User";


    /**
     * Name of the node in the gateway configuration XML giving the password in the JNDI.
     */
    public static final String PASSWORD = "Password";


    /**
     * Name of the node in the gateway configuration XML giving the initial context factory in the JNDI.
     */
    public static final String INITIAL_CONTEXT_FACTORY = "InitialContextFactory";

    /**
     * Unit-test.
     *
     * @param  args  Command-line arguments: meta-data-name, app-server-url, 
     *                method-name, header-file, message-file.
     */
    public static final void main ( String[] args )
    {
        if ( args.length < 4 )
        {
            System.err.println( "\n\nUSAGE: GatewayBeanClient <meta-data-name> " 
                                + "<method-name> <header-file-name> <request-file-name> [count].\n\n" );
            
            System.exit( -1 );
        }
        
        Debug.enableAll( );
        Debug.showLevels( );
        Debug.configureFromProperties( );

        String metaDataName    = args[0];
        String methodName      = args[1];
        String headerFileName  = args[2];
        String requestFileName = args[3];
        
        try
        {
            int count = 1;
            
            if ( args.length > 4 )
                count = Integer.parseInt( args[4] );
            
            String header = FileUtils.readFile( headerFileName );

            Debug.log( Debug.UNIT_TEST, "Request header:\n" + header );

            String request = FileUtils.readFile( requestFileName );

            Debug.log( Debug.UNIT_TEST, "Request body:\n" + request );


            Debug.log( Debug.UNIT_TEST, "Creating gateway bean client ..." );

            GatewayBeanClient client = new GatewayBeanClient( metaDataName );


            for ( int Ix = 0;  Ix < count;  Ix ++ )
            {
                Debug.log( Debug.UNIT_TEST, "Invoking method [" + methodName + "()], iteration [" + (Ix + 1) + "] ..." );
                
                if ( methodName.equals( "processAsync" ) )
                {
                    client.processAsync( header, request );
                }
                else
                if ( methodName.equals( "processSync" ) )
                {
                    String response = client.processSync( header, request );
                    
                    Debug.log( Debug.UNIT_TEST, "Response:\n" + response );
                }
                else
                if ( methodName.equals( "processSynchronous" ) )
                {
                    RMIRequestHandler.ResponsePair rp = client.processSynchronous( header, request );
                    
                    Debug.log( Debug.UNIT_TEST, "Response header :\n" + rp.header + "\nResponse message:\n" + rp.message );
                }
                else
                    Debug.error( "Invalid method name [" + methodName + "()]." );
            }
        }
        catch ( Exception e )
        {
            System.err.println( "\n\nERROR: " + e.toString() );

            e.printStackTrace( );
        }
    }

    
    /**
     * Constructor.
     *
     * @param  metaDataName  Name used to locate gateway bean's configuration
     *                       information in the Repository Manager.
     * @param  url  The URL identifying the JNDI context.
     * @param  user  The user name to use in access control.
     * @param  password  The user password to use in access control.
     *
     * @exception  FrameworkException  Thrown on gateway bean errors.
     */
    public GatewayBeanClient ( String metaDataName ) throws FrameworkException
    {
        try
        {
            // Get the meta-data describing the gateway from the repository.
            String xmlDescription 
                = RepositoryManager.getInstance().getMetaData( GATEWAY_CONFIG_REPOSITORY_CATEGORY, 
                                                               metaDataName );
            
            XMLMessageParser p = new XMLMessageParser( xmlDescription );
            
            gatewayBeanHomeName = p.getValue( GATEWAY_HOME_NAME );

            url = p.getValue( URL );

            initialContextFactory = p.getValue( INITIAL_CONTEXT_FACTORY );

            if( p.exists( USER ) )
                user = p.getValue( USER );

            if( p.exists( PASSWORD ) )
                user = p.getValue( PASSWORD );

        }
        catch ( Exception e )
        {
            if ( e instanceof FrameworkException )
                throw (FrameworkException)e;
            else
                throw new FrameworkException( e );
        }
    }
    
    
    /**
     * Method used to get all interfaces supported by a given gateway.
     *
     * @return An array of UsageDescription objects.
     *
     * @exception 
     */
    public RMIRequestHandler.UsageDescription[] getUsageDescriptions ( ) throws java.rmi.RemoteException
    {
        return( getBean().getUsageDescriptions() );
    }
    
    
    /**
     * Test to see if a particular usage is supported.
     *
     * @param  usage  UsageDescription object containing usage in question.
     *
     * @return 'true' if usage is supported, otherwise false.
     *
     * @exception RemoteException  Thrown on communications errors.
     */
    public boolean supportsUsage ( RMIRequestHandler.UsageDescription usage ) throws java.rmi.RemoteException
    {
        return( getBean().supportsUsage( usage ) );
    }
    
    
    /**
     * Method providing asynchronous processing.
     *
     * @param  header  Message header.
     * @param  request Message body.
     *
     * @exception RemoteException  Thrown on communications errors.
     * @exception RMIInvalidDataException  Thrown if request data is bad.
     * @exception RMIServerException  Thrown if server can't process request due to system errors.
     */
    public void processAsync ( String header, String request )
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException
    {
        getBean().processAsync( header, request );
    }
    
    
    /**
     * Method providing synchronous processing.
     *
     * @param  header  Message header.
     * @param  request Message body.
     *
     * @return The synchronous response.
     *
     * @exception RemoteException  Thrown on communications errors.
     * @exception RMIInvalidDataException  Thrown if request data is bad.
     * @exception RMIServerException  Thrown if server can't process request due to system errors.
     * @exception RMINullResultException  Thrown if server can't process request due to system errors.
     */
    public String processSync ( String header, String request )
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException, RMINullResultException
    {
        return( getBean().processSync( header, request ) );
    }
    
    
    /**
     * Method providing synchronous processing, with headers in and out.
     *
     * @param  header  Message header.
     * @param  request Message body.
     *
     * @return A response-pair object containing the response header and body.
     *
     * @exception RemoteException  Thrown on communications errors.
     * @exception RMIInvalidDataException  Thrown if request data is bad.
     * @exception RMIServerException  Thrown if server can't process request due to system errors.
     * @exception RMINullResultException  Thrown if server can't process request due to system errors.
     */
    public RMIRequestHandler.ResponsePair processSynchronous ( String header, String request )
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException, RMINullResultException
    {
        return( getBean().processSynchronous( header, request ) );
    }
    
    
    /**
     * Get the gateway bean instance.
     *
     * @return  A reference to the gateway bean.
     *
     * @exception  RMIServerException  Thrown on gateway bean errors.
     */
    public GatewayBean getBean ( ) throws RMIServerException
    {
        if ( gatewayBean == null )
        {
            synchronized( this )
            {
                if ( gatewayBean == null )
                {
                    try 
                    {
                        gatewayBean = create( );
                    } 
                    catch ( Exception e ) 
                    {
                        throw new RMIServerException( "ERROR: Could not create gateway bean:\n" + e.toString() );
                    }
                }
            }
        }

        return gatewayBean;
    }
    
    
    /**
     * Lookup the gateway bean's home and return a reference to it.
     *
     * @return  The Home object
     *
     * @exception  FrameworkException  Thrown on gateway bean errors.
     */
    public GatewayBeanHome getHome ( ) throws FrameworkException
    {
        if ( gatewayBeanHome == null )
        {
            synchronized( this )
            {
                if ( gatewayBeanHome == null )
                {
                    Context ctx = getInitialContext( );
                    
                    try 
                    {
                        Object obj = ctx.lookup( gatewayBeanHomeName );
                        
                        gatewayBeanHome = (GatewayBeanHome)PortableRemoteObject.narrow( obj, GatewayBeanHome.class );
                    } 
                    catch ( Exception e ) 
                    {
                        throw new FrameworkException( "ERROR: Could not locate gateway bean home named [" 
                                                      + gatewayBeanHomeName + "]:\n" + e.toString() );
                    }
                }
            }
        }
        
        return gatewayBeanHome;
    }
    

    /**
     * Create a gateway bean instance.
     *
     * @return  A reference to the newly-created gateway bean.
     *
     * @exception  FrameworkException  Thrown on gateway bean errors.
     */
    public GatewayBean create ( ) throws FrameworkException
    {
        try 
        {
            GatewayBeanHome h = getHome( );
            
            return( h.create( ) );
        } 
        catch ( Exception e ) 
        {
            throw new FrameworkException( "ERROR: Could not create gateway bean:\n" + e.toString() );
        }
    }


    /**
     * Narrows the object argument to its remote interface.
     *
     * @param  obj  The object to narrow.
     *
     * @return  The corresponding gateway bean remote object.
     *
     * @exception  FrameworkException  Thrown on gateway bean errors.
     */
    public GatewayBean toGatewayBean ( Object obj ) throws FrameworkException
    {
        try
        {
            return( (GatewayBean)PortableRemoteObject.narrow( obj, GatewayBean.class ) );
        }
        catch ( Exception e ) 
        {
            throw new FrameworkException( "ERROR: Object couldn't be converted to gateway bean:\n" 
                                          + e.toString() );
        }
    }
    

    /**
     * Get the JNDI context.
     *
     * @return  The initial context.
     *
     * @exception  FrameworkException  Thrown on gateway bean errors.
     */
    private Context getInitialContext ( ) throws FrameworkException 
    {
        try 
        {
            Properties props = new Properties( );
            
            // TODO: Externalize weblogic name from here.
            props.put( Context.INITIAL_CONTEXT_FACTORY, initialContextFactory );

            props.put( Context.PROVIDER_URL, url );
            
            if ( user != null ) 
            {
                if ( Debug.isLevelEnabled( Debug.IO_DATA ) )
                    Debug.log( Debug.IO_DATA, "User [" + user + "]." );
                
                props.put( Context.SECURITY_PRINCIPAL, user );
                
                if ( password == null )
                    throw new FrameworkException ( "User [" + user + "] must have a valid non-null password" );
                
                props.put( Context.SECURITY_CREDENTIALS, password );
                
                if ( Debug.isLevelEnabled( Debug.IO_DATA ) )
                	Debug.log( Debug.IO_DATA, "Password Set." );
            }
            else
            {
                //This is an internal system call, so we use the default user and password configured
                //for this system.
                user = SystemEnvironment.getDefaultUser( );

                password = SystemEnvironment.getDefaultPassword( );

                if ( Debug.isLevelEnabled( Debug.IO_DATA ) )
                	Debug.log( Debug.IO_DATA, "Using default user [" + user + "]." );

                props.put( Context.SECURITY_PRINCIPAL, user );

                props.put( Context.SECURITY_CREDENTIALS, password );
            }

            return( new InitialContext( props ) );
        }
        catch ( Exception e )
        {
            throw new FrameworkException( "ERROR: Could not locate context at URL ["
                                          + url + "]:\n" + e.toString() );
        }
    }


    // App server and client access information.
    private String url;
    private String user;
    private String password;
    private String initialContextFactory;

    // Repository configuration items.
    private String gatewayBeanHomeName;

    // Cached home object.
    private GatewayBeanHome gatewayBeanHome;
    // Cached gateway bean object.
    private GatewayBean gatewayBean;
}
