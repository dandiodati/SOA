package com.nightfire.comms.util ;

import java.io.* ;
import java.util.* ;
import java.rmi.* ;
import java.rmi.registry.* ;

import com.nightfire.idl.* ;
import com.nightfire.rmi.* ;

import com.nightfire.framework.util.* ;
import com.nightfire.framework.corba.* ;


public class RequestHandlerClient {

    private boolean DEBUG = false ;		// set to true for debug messages

    private int protocol ; 			// one of either USE_CORBA or USE_RMI

    private Properties properties ; 		// contains configuration parameters, 
    						// and is passed into constructor

    private RMIRequestHandler cachedRMIHandler; // only one of these will be used
    private RequestHandler cachedCORBAHandler ; // (depends on config properties)

    public static final int USE_CORBA = 0 ; 	// possible values for protocol
    public static final int USE_RMI   = 1 ;	// can add more later if necessary

    /* static property names */
    public static final String USE_RMI_PROP           = "USE_RMI" ;
    public static final String RMI_REGISTRY_HOST_PROP = "RMI_REGISTRY_HOST" ;
    public static final String RMI_REGISTRY_PORT_PROP = "RMI_REGISTRY_PORT" ;
    public static final String SERVER_OBJECT_PROP     = "SERVER_OBJECT" ;
    public static final String IOR_ACCESS_PROP        = "IOR_ACCESS" ;
    private CorbaPortabilityLayer cpl = null;

    /**
     *  constructor
     *  @param properties Properties object containint N/V pairs
     *  
     *  The following properties are of interest. Others are ignored.
     *  USE_RMI	(true/false) if true, RMI is used; if absent CORBA is used
     *  RMI_REGISTRY_HOST
     *  RMI_REGISTRY_PORT
     *  SERVER_OBJECT
     *  IOR_ACCESS
     */
    public RequestHandlerClient (Properties properties) throws FrameworkException {

	if (properties == null)
	    throw new FrameworkException ("RequestHandlerClient: ERROR: Properties were null.") ;

        this.properties = properties ;

	if (!properties.containsKey (SERVER_OBJECT_PROP))
	    throw new FrameworkException ("Required property [" + SERVER_OBJECT_PROP + "] is missing.") ;

	String useRMI = "false" ;
	if (properties.containsKey (USE_RMI_PROP))
	    useRMI = (String) properties.getProperty (USE_RMI_PROP) ;

	if (useRMI.equalsIgnoreCase ("true")) {
            this.protocol = USE_RMI ;

	    if (!properties.containsKey (RMI_REGISTRY_PORT_PROP))
	        throw new FrameworkException ("Required property [" + RMI_REGISTRY_PORT_PROP + "] is missing.") ;
	    if (!properties.containsKey (RMI_REGISTRY_HOST_PROP))
	        throw new FrameworkException ("Required property [" + RMI_REGISTRY_HOST_PROP + "] is missing.") ;
	}
	else {
            this.protocol = USE_CORBA ;

	    if (!properties.containsKey (IOR_ACCESS_PROP))
	        throw new FrameworkException ("Required property [" + IOR_ACCESS_PROP + "] is missing.") ;
	}
    }

    /**
     *  @param header  XML from request header
     *  @param request XML request body
     *  @return The XML header and request packaged in a ResponsePair
     *  @exception FrameworkException a variety of Exception types
     *  may be thrown, depending on whether CORBA or RMI is used. Any 
     *  Exception that is caught is re-thrown as a FrameworkException.
     */
    public ResponsePair processSynchronous (String header, String request) 
        throws FrameworkException {

	ResponsePair response = new ResponsePair() ;

	if (protocol == USE_CORBA) {
            try {


                if (cpl == null)  {
                    String args[] = null;
                    cpl = new CorbaPortabilityLayer(args, System.getProperties(), null);

                    if(Debug.isLevelEnabled(Debug.MSG_DATA))
                        Debug.log(Debug.MSG_DATA, "Creating CORBA orb");
               }

                // Check to see if the peer object is alive.
                if ( peerDisconnected( cachedCORBAHandler ) )
                    cachedCORBAHandler = null;

	        if (cachedCORBAHandler == null) {
                    ObjectLocator locator = new ObjectLocator (cpl.getORB());
                    org.omg.CORBA.Object corbaObject = null;
                
                    String serverName      = (String) properties.getProperty (SERVER_OBJECT_PROP) ;
                    String iorAccessMethod = (String) properties.getProperty (IOR_ACCESS_PROP);
            
                    if (iorAccessMethod == null)
                        iorAccessMethod = ObjectLocator.defaultIORAccessMethod;

                    // Make sure that the reference isn't cached by the object-locator infrastructure.
                    locator.removeFromCache( serverName );
            
                    corbaObject = locator.find (serverName, iorAccessMethod);
            
                    cachedCORBAHandler = (RequestHandler) RequestHandlerHelper.narrow (corbaObject);

                    if (cachedCORBAHandler == null)
                        throw new FrameworkException ("Could not create CORBA RequestHandler object." );
	        }

	        org.omg.CORBA.StringHolder responseBody   = new org.omg.CORBA.StringHolder();
	        org.omg.CORBA.StringHolder responseHeader = new org.omg.CORBA.StringHolder();

            cachedCORBAHandler.processSynchronous (header, request, responseHeader, responseBody);
            
	        response.header  = responseHeader.value ;
	        response.message = responseBody.value ;
	    }
        catch (Exception e) 
        {
            // If we catch an exception, we assume that a communications failure occurred - most likely
            // due to a stale cached CORBA reference.  We recover from this by throwing out the reference 
            // so that the next invocation will get a new one.  (i.e., Recovery on next attempt.)
            cachedCORBAHandler = null;
            
            String errMsg = "RequestHandlerClient.processSynchronous(): Could not process CORBA request.\n" 
                + e.toString();
            
            Debug.error( errMsg );
            Debug.error( Debug.getStackTrace( e ) );
            
	        // catching generic exception as there are several that may arise
	        throw new FrameworkException ( errMsg );
	    }
	}
	else if (protocol == USE_RMI) {
	 
	    if (cachedRMIHandler == null) {

		String serverName = (String) properties.get (SERVER_OBJECT_PROP) ;
		String hostName   = (String) properties.get (RMI_REGISTRY_HOST_PROP) ;
		String portNumber = (String) properties.get (RMI_REGISTRY_PORT_PROP) ;

                int portNum ;
		try {
		    portNum = Integer.parseInt (portNumber) ;
		}
		catch (NumberFormatException nfe) {
	            throw new FrameworkException ("Required property [" + RMI_REGISTRY_PORT_PROP + 
		                                  "] exists, but does not contain a valid port number.") ;
		}

                if (DEBUG) {
                    try {
	                Registry r = LocateRegistry.getRegistry (hostName, portNum) ;
		        String[] list = r.list() ;
		        if (list.length == 0)
		            System.out.println ("There are no Objects in the Registry") ;
			else {
		            System.out.println ("THERE ARE " 
			                        + list.length + " REGISTRY ITEMS:") ;
		            for (int i = 0 ; i < list.length ; i++)
		                System.out.println ("ITEM:\t" + (i+1) + "\t" + list[i]) ;
			}
		    }
		    catch (Exception e) {
	                throw new FrameworkException ("Could not locate registry.\n" + e.getMessage()) ;
		    }
		}

                try {
		    String serviceName = "//" + hostName + ":" + portNumber + "/" + serverName ;
		    cachedRMIHandler = (RMIRequestHandler) Naming.lookup (serviceName) ;
		}
		catch (Exception e) {
	            e.printStackTrace() ;
	            throw new FrameworkException ("RequestHandlerClient.processSynchronous(): an error occurred while trying to create the RMIRequestHandler.\n" + e.getMessage()) ;
		}
                if (cachedRMIHandler == null)
                    throw new FrameworkException ("Could not create RMI RequestHandler object." );
	    }

            try {
	        RMIRequestHandler.ResponsePair pair = cachedRMIHandler.processSynchronous (header, request) ;
	        response.header  = pair.header ;
	        response.message = pair.message ;
	    }
	    catch (Exception e) {
	        throw new FrameworkException ("RequestHandlerClient.processSynchronous(): could not process RMI request.\n" + e.getMessage()) ;
	    }
	}

        return response ;
    }


    /**
     * Test to see if given object is 'dead' with respect to CORBA access.
     * 
     * @param  candidate  Candidate CORBA object to check.
     *
     * @return  'true' if object is null, or CORBA non-existence
     *          tests indicate that it is dead.
     */
    private boolean peerDisconnected ( org.omg.CORBA.Object candidate )
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "Testing CORBA object for live-ness ..." );
        
        if ( candidate == null )
        {
            Debug.log( Debug.OBJECT_LIFECYCLE, "Null CORBA object is not live." );
            
            return true;
        }
        
        try
        {
            boolean dead = candidate._non_existent( );
            
            Debug.log( Debug.OBJECT_LIFECYCLE, "CORBA object is dead? [" + dead + "]." );
            
            return dead;
        }
        catch ( Exception e )
        {
            Debug.warning( "Non-existence call against CORBA object failed, so it's assumed dead:\n" 
                           + e.toString() );
            
            return true;
        }
    }


    /**
     * Class encapsulating the synchronous response header and message.
     */
    public static class ResponsePair implements Serializable {
        public String header;
        public String message;
    }

    /**
     *  main method for unit testing...
     */ 
    public static void main (String[] args) {
	Properties props = new Properties() ;
	props.setProperty (RequestHandlerClient.USE_RMI_PROP, "true") ;
	props.setProperty (RequestHandlerClient.RMI_REGISTRY_PORT_PROP, "1099") ;
	props.setProperty (RequestHandlerClient.RMI_REGISTRY_HOST_PROP, "192.168.8.22") ;
	props.setProperty (RequestHandlerClient.SERVER_OBJECT_PROP, "ProcessManager") ;

        RequestHandlerClient client = null ;
	try {
            client = new RequestHandlerClient (props) ;
	}
	catch (FrameworkException e) {
	    e.printStackTrace() ;
	}

	try {
	    client.processSynchronous ("header", "request") ;
	}
	catch (Exception e) {
	    e.printStackTrace() ;
	}
    }
}
