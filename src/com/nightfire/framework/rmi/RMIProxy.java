package com.nightfire.framework.rmi;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteStub;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.nightfire.framework.util.CommonConfigUtils;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;


/**
 * Proxy to RMI Registry running in Clearing House.
 * This class should be used to interact with RMI Registry
 * running in Clearing House. It encapsulates details of rmi host
 * and port information from clients and keeps track of objects that are
 * exported to JRMP.
 */
public class RMIProxy {

    /* host ip/name where rmi is running */
    static String rmiHost;
    
    /* port where rmi is running */
    static String rmiPort;

    /* list containing references of remote objects that have been exported to */
    private static List<Remote> remoteObjLst;
    
    private RMIProxy() throws FrameworkException 
    {
        rmiHost = CommonConfigUtils.getRMIServerAddr();
        rmiPort = CommonConfigUtils.getRMIServerPort();
        remoteObjLst = new ArrayList<Remote>();
    }
    
    private static RMIProxy _singleton ;
    
    public static RMIProxy getInstance() throws FrameworkException {
        if(_singleton==null)
        {
            synchronized (RMIProxy.class) {
                if(_singleton!=null)
                    return _singleton;
                
                return _singleton = new RMIProxy();
            }
        }
        
        return _singleton;
    }
    
    /**
     * Bind an object to RMI Registry running in Clearing House Platform 
     * @param name
     * @param obj
     * @throws RemoteException
     * @throws AlreadyBoundException
     * @throws AccessException
     */
    public void bind(String name, Remote obj)  throws RemoteException, AlreadyBoundException, AccessException
    {
       Registry reg = LocateRegistry.getRegistry(rmiHost, Integer.parseInt(rmiPort));
       reg.bind(name, obj);
    }

    /**
     * List all names bound in RMI Registry running in Clearing House Platform 
     * @return String array of names
     * @throws RemoteException
     * @throws AccessException
     */
    public String[] list() throws RemoteException,AccessException
    {
        Registry reg = LocateRegistry.getRegistry(rmiHost, Integer.parseInt(rmiPort));
        return reg.list();
    }
    
    /**
     * Look up an object in RMI Registry running in Clearing House Platform  
     * @param name
     * @return
     * @throws RemoteException
     * @throws AccessException
     * @throws NotBoundException
     */
    public Remote lookup(String name) throws RemoteException,AccessException,NotBoundException 
    {
        Registry reg = LocateRegistry.getRegistry(rmiHost, Integer.parseInt(rmiPort));
        return reg.lookup(name);
    }
    
    /**
     * Rebind an object in RMI Registry running in Clearing House Platform
     * @param name
     * @param obj
     * @throws RemoteException
     * @throws AccessException
     */
    public void rebind(String name,Remote obj) throws RemoteException,AccessException
    {
        Registry reg = LocateRegistry.getRegistry(rmiHost, Integer.parseInt(rmiPort));
        reg.rebind(name, obj);
    }
    
    /**
     * Unbind an object in RMI Registry running in Clearing House Platform 
     * @param name
     * @throws RemoteException
     * @throws AccessException
     * @throws NotBoundException
     */
    public void unbind(String name)throws RemoteException,AccessException,NotBoundException
    {
        Registry reg = LocateRegistry.getRegistry(rmiHost, Integer.parseInt(rmiPort));
        reg.unbind(name);
    }
    
    /**
     * Export an object into JRMP running in current VM. Also it would
     * add a reference of the object being exported to this Proxy. 
     * @param obj
     * @return
     * @throws RemoteException
     */
    public RemoteStub exportObject(Remote obj) throws RemoteException 
    {
        remoteObjLst.add(obj);
        return UnicastRemoteObject.exportObject(obj);    
    }

    /**
     * Export an object into JRMP running in current VM. Also it would
     * add a reference of the object being exported to this Proxy. 
     * @param obj
     * @param port
     * @return
     * @throws RemoteException
     */
    public Remote exportObject(Remote obj, int port) throws RemoteException 
    {
        
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, "RMIProxy: Exporting object :"+obj.getClass().getName());
        
        remoteObjLst.add(obj);
        return UnicastRemoteObject.exportObject(obj,port);    
    }

    /**
     * Unexport an object from JRMP 
     * @param obj
     * @param force
     * @return
     * @throws NoSuchObjectException
     */
    public boolean unexportObject(Remote obj, boolean force) throws NoSuchObjectException 
    {
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, "RMIProxy: UnExporting object :"+obj.getClass().getName());

        remoteObjLst.remove(obj);
        return UnicastRemoteObject.unexportObject(obj, force);
    }
    
    /**
     * This method would unexport all objects that have been through this Proxy. 
     */
    public void unexportAll()
    {
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, "RMIProxy: Unexporting all objects.");

        Iterator iter = remoteObjLst.iterator();
        
        while(iter.hasNext())
        {
            Remote remoteObj = (Remote)iter.next();
            try
            {
                UnicastRemoteObject.unexportObject(remoteObj, true);
                iter.remove();
            }
            catch(Exception e)
            {
                Debug.warning("Exception occured while unexporting object ["+remoteObj.getClass().getName()+"] :"+e.getMessage());
            }
        }
    }
}
