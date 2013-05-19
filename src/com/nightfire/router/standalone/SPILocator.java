package com.nightfire.router.standalone;

import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.omg.CORBA.ORB;

import com.nightfire.framework.corba.CorbaException;
import com.nightfire.framework.corba.CorbaPortabilityLayer;
import com.nightfire.framework.corba.ObjectLocator;
import com.nightfire.framework.util.CommonConfigUtils;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;

/**
 * Locator class to be used by message routing component.
 * It provides wrapper methods over ObjectLocator; it handles the exception thrown
 * by CORBA layer.
 *
 */
public class SPILocator extends ObjectLocator {
    
    CorbaPortabilityLayer cpl  = null;
    private static Map<String,ORB> orbStore = new Hashtable<String,ORB>();
    private static String DEFAULT_ORB = "DEFAULT_ORB";
    
    /**
     * Instantiates a locator object with default ORB addr and port.
     * The one defined in commonConfig.xml. 
     * 
     * @throws FrameworkException
     */
    public SPILocator() throws FrameworkException 
    {
        ORB orb = orbStore.get(DEFAULT_ORB);
        if(isValidORB(orb))
        {
            setORB(orb);
        }
        else
        {
            Properties orbProperties = new Properties();
            orbProperties.put(CorbaPortabilityLayer.ORB_AGENT_ADDR_PROP,
                    CommonConfigUtils.getCORBAServerAddr());
            orbProperties.put(CorbaPortabilityLayer.ORB_AGENT_PORT_PROP, 
                    CommonConfigUtils.getCORBAServerPort());

            cpl  = new CorbaPortabilityLayer(null, orbProperties);
            orb = cpl.getORB();
            setORB(orb);

            orbStore.put(DEFAULT_ORB,orb);
        }
    }
    

    /**
     * Instantiates a locator object with an ORB with specified addr and port.
     *
     * @param addr String
     * @param port String
     * @throws FrameworkException
     */
    public SPILocator(String addr, String port) throws FrameworkException 
    {
        String key = addr+"#"+port;
       
        ORB orb = orbStore.get(key);
        if(isValidORB(orb))
        {
            setORB(orb);
        }
        else
        {
            createNewORB(addr, port);
        }
    }

    /**
     * Creates a new ORB and sets that on parent class. 
     * @param addr
     * @param port
     * @throws FrameworkException
     */
    private void createNewORB(String addr, String port) throws FrameworkException {

        Properties orbProperties = new Properties();
        orbProperties.put(CorbaPortabilityLayer.ORB_AGENT_ADDR_PROP, addr);
        orbProperties.put(CorbaPortabilityLayer.ORB_AGENT_PORT_PROP, port);
        CorbaPortabilityLayer cpl = new CorbaPortabilityLayer(null, orbProperties);

        ORB orb = cpl.getORB();
        setORB(orb);
        
        orbStore.put(addr+"#"+port, orb);
    }

    /**
     * This method tries to find the object in default ORB space.
     * If it finds an object then it checks for its existentence i.e. 
     * whether it is valid or not.If the object found does not exists/invalid,
     * then it removes from its cache and retries once.
     * 
     * @param objName String name of the object
     * @return org.omg.CORBA.Object
     */
    public org.omg.CORBA.Object find(String objName) 
    {
          return find(objName, false);
    }

    /**
     * This method tries to find the object in supplied ORBAddr and ORBPort.
     * If it finds an object then it checks for its existentence i.e. 
     * whether it is valid or not.If the object found does not exists/invalid,
     * then it removes from its cache and retries once.
     * 
     * @param objName String name of the object
     * @param addr String ip address of ORB 
     * @param port String port of ORB
     * @return org.omg.CORBA.Object
     */
    public org.omg.CORBA.Object find(String objName,String addr,String port) 
    {
          return find(objName,addr,port,false);
    }

    /**
     * This method ensures that the find operation is only re-tried once. 
     * @param objName
     * @param secondTry
     * @return
     */
    protected org.omg.CORBA.Object find(String objName, boolean secondTry) 
    {
        try
        {
            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                Debug.log(Debug.NORMAL_STATUS,"trying to locate gateway with name ["+objName+"]");
            
            org.omg.CORBA.Object obj = super.find(objName);
            if(!secondTry && obj!=null && isDead(obj))
            {
                super.removeFromCache(objName);
                return find(objName,true);
            }
            
            return obj;
        }
        catch(CorbaException ce)
        {
            Debug.warning("Could not locate gateway with name ["+objName+"]");
            return null;
        }
        catch(org.omg.CORBA.SystemException se)
        {
            // in case super.find (above) throws an exception
            if(!secondTry)
            {
                Debug.warning("Gateway Object not reachable, removing from cache");
                super.removeFromCache(objName);
                return find(objName, true);
            }
            else
                return null;
        }
    }

    /**
     * This method ensures that the find operation is only re-tried once. 
     * @param objName
     * @param addr
     * @param port
     * @param secondTry
     * @return org.omg.CORBA.Object 
     */
    protected org.omg.CORBA.Object find(String objName,String addr,String port,boolean secondTry) 
    {
        try
        {
            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                Debug.log(Debug.NORMAL_STATUS,"trying to locate gateway with name ["+objName+"]");

            org.omg.CORBA.Object obj = super.find(objName, addr, port);
            if(!secondTry && obj!=null && isDead(obj))
            {
                super.removeFromCache(objName,addr,port);
                updateOrbStore(addr,port);
                return find(objName,addr,port,true);
            }
  
            return obj;
        }
        catch(CorbaException ce)
        {
            Debug.warning("Could not locate gateway with name ["+objName+"]");
            return null;
        }
        catch(org.omg.CORBA.SystemException se)
        {
            // in case super.find (above) throws an exception            
            if(!secondTry)
            {
                Debug.warning("Gateway Object not reachable, removing from cache");
                super.removeFromCache(objName,addr,port);
                try
                {
                    updateOrbStore(addr,port);
                }   
                catch(CorbaException ce)
                {
                    /* in case we get an exception here, we stop */
                    return null;
                }
                
                return find(objName,addr,port,true);
            }
            else
                return null;
        }
    }

    /**
     * Remove the existing ORB, if any and create a new one
     * @param addr String
     * @param port String
     * @throws CorbaException 
     */
    private void updateOrbStore(String addr, String port) throws CorbaException {
       
        String key = addr+"#"+port;
        ORB orb = orbStore.remove(key);
        try
        {
            if(orb!=null)
                orb.destroy();
        }
        catch(org.omg.CORBA.SystemException ignore)
        {
            //do nothing
        }
        
        try
        {
            createNewORB(addr, port);
        }
        catch(FrameworkException fe)
        {
            Debug.error("Could not create orb for addr["+addr+"] and port["+port+"]..\n"+Debug.getStackTrace(fe));
            throw new CorbaException(fe); 
        }
    }
    
    /**
     * Method to check whether a CORBA object is dead or alive.
     * 
     * @param obj
     * @return boolean
     */
    private boolean isDead(org.omg.CORBA.Object obj)
    {
        boolean non_exist = true;
        try
        {
            if(obj==null)
                return true;
            
            non_exist = obj._non_existent();
        } 
        catch (org.omg.CORBA.SystemException e)
        {
            non_exist = true;
        }
        return non_exist;
    }
    
    /**
     * Method to check whether the ORB still exists or not.
     * 
     * @param orb ORB
     * @return boolean 
     */
    private boolean isValidORB(ORB orb) 
    {
        try
        {
            if(orb==null)
                return false;
            
            /* check if it can create an ANY type, 
             * this would try to ensure that the ORB is valid */
            orb.create_any();
            return true;
        }
        catch(org.omg.CORBA.SystemException se)
        {
            return false;
        }
    }

}
