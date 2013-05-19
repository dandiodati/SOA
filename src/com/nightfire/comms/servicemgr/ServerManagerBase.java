package com.nightfire.comms.servicemgr;

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;


/**
 * Abstract class for all the server managers.
 */
public abstract class ServerManagerBase 
{
    private String type;
    private String gwsServerName = null; 
    protected XMLMessageParser parser;

    /**
     * ErrorHandler that sends notifications about error/exceptions 
     */
    
    protected ServerManagerNotificationHandler notificationHandler;

    public ServerManagerBase(String type) 
    {
        this.type=type;
        notificationHandler = new ServerManagerNotificationHandler();
    }

    /**
     * This is the parameterized constructor for ServerManagerBase class. 
     * Takes GWS ServerName as a parameter.
     * 
     * @param type
     * @param gwsServerName
     */
    public ServerManagerBase(String type, String gwsServerName) 
    {
    	this.gwsServerName = gwsServerName;
    	this.type=type;
    	notificationHandler = new ServerManagerNotificationHandler();
    }
    

    /**
     * Returns GWS ServerName to the Caller.
     * @return gwsServerName
     */
    public String getGwsServerName(){
    	return this.gwsServerName;
    }
    
    /**
     * @return type of ServerManager.
     */
    public String getType()
    {
        return this.type;
    }
    
    
    
    /**
     * Returns the document representation of Server Manager configuration.
     * @return 
     */
    protected Document getDocument()
    {
        return parser.getDocument();
    }
    
    public abstract void initialize() throws ProcessingException;

    public void initialize(String gwsServerName) throws ProcessingException {
    	this.gwsServerName = gwsServerName;
    	initialize();
    }
    
    /**
     * starts all the configured services.
     * @throws Exception
     */
    public abstract void startAll()throws ProcessingException;

    /**
     * stops all the running services for this server.
     * @throws ProcessingException
     */
    public abstract void stopAll() throws ProcessingException;
    
    /**
     * starts the service identified by parameters.
     * @param parameters
     * @throws ProcessingException
     */
    public abstract void start(Map parameters)throws ProcessingException;
    
    /**
     * stops the service identified by parameters.
     * @param parameters
     * @throws ProcessingException
     */
    public abstract void stop(Map parameters) ;
    
    /**
     * Adds a new service to be managed by server manager. 
     * @param parameters parameters to configure the service.
     * @throws ProcessingException
     */
    public abstract void add(Map parameters) throws ProcessingException;
    
    /**
     * Removes the service managed by this server manager.
     * @param parameters parameters to identify the service
     * @throws ProcessingException
     */
    public abstract void remove(Map parameters);
    
    /**
     * update the properties status with the latest status value.
     * @param serverType to identify the ServerManager
     * @param status sets status value to true/false.
     */
    protected void updateXML(String serviceElement,String idElement, String id, String startAttributeName, String status) 
    {
        if ( Debug.isLevelEnabled ( Debug.XML_DATA ) )
            Debug.log(Debug.XML_DATA, "updateXML[Service:"+serviceElement+" ID:"+id+" status:"+status+"]...");

        Document doc = getParser().getDocument();

        // modify the respective servers start attribute to its status. 
        NodeList serviceList = doc.getElementsByTagName(serviceElement);

        for(int i=0;i<serviceList.getLength();i++){
            Element service =(Element) serviceList.item(i);
            if(id.equals(service.getAttribute(idElement)))
            {
                service.setAttribute(startAttributeName, status);
            }
        }

        if ( Debug.isLevelEnabled ( Debug.XML_DATA ) )
            Debug.log(Debug.XML_DATA, "exiting updateXML[Service:"+serviceElement+" ID:"+id+" status:"+status+"]...");
    } 

    public XMLMessageParser getParser() 
    {
        return parser;
    }
    
    /**
     * Enumeration representing configuration types.
     */
    enum ConfigType {ATTRIBUTE, ELEMENT};
    
    /**
     * get the configuration value for property specified. Returns null if not configured.
     * @param consumerElement JMS Consumer properties.
     * @param type type of property in XML can be one of {Attribute, Element}
     * @param name name of the property value.
     * @return property value.
     */
    protected String getConfigurationValue(Element consumerElement, ConfigType type, String name, String defaultValue)
    {
        String result = null;
        try
        {
            if("ATTRIBUTE".equals(type.toString()))
            {
                result = consumerElement.getAttribute(name);
            }
            else if ("ELEMENT".equals(type.toString()))
            {
                result = consumerElement.getElementsByTagName(name).item(0).getFirstChild().getNodeValue();
            }
        }
        catch(Exception e)
        {
            Debug.log(Debug.DB_WARNING, "Configuration read problem for property "+name);
            return defaultValue;
        }
        return result;
    }

}
