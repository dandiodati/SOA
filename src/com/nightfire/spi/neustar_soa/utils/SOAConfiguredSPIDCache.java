
package com.nightfire.spi.neustar_soa.utils;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import com.nightfire.framework.cache.CacheManager;
import com.nightfire.framework.cache.CachingObject;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.db.SQLUtil;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;


public class SOAConfiguredSPIDCache implements CachingObject
{

    private static HashMap<String,Object> soaSpidProp = new HashMap<String,Object>();
    
    private static String className = "SOAConfiguredSPIDCache";    
        
    private String spid;
    private String status;     
    
    
    public static final String STATUS = "STATUS";    
      
    
    private static SOAConfiguredSPIDCache createInstance (String spid)
    {
        Connection dbConn = null;
        SOAConfiguredSPIDCache spidCache = null;
        Vector<String> vColumns = new Vector<String> ();
        Vector vAllRows = new Vector();
        Vector vRow = new Vector();
        Hashtable<String, String> htWhere = new Hashtable<String, String>();

        try
        {            
        	//Getting status value just for checking whether record exist or not 
            vColumns.add(STATUS);           
            
            htWhere.put("SPID", spid);
            htWhere.put("STATUS", "ok");
            dbConn = DBInterface.acquireConnection();
            /**
             * The fetchRows function of SQLUtil return the vector containing
             * vectors as a row containing the column data in it
             */
            vAllRows = SQLUtil.fetchRows(dbConn, "SOA_SPID", vColumns, htWhere);

            spidCache = new SOAConfiguredSPIDCache();
            
            if (vAllRows != null && vAllRows.size() > 0)
            {
                // Obtaining first row of the resultset
                vRow = (Vector) vAllRows.get(0);                                
                
                if (vRow.get(0) != null) {
                	spidCache.setSpid(spid);
				}				
             }
            else {
            	
				spidCache.setSpid("0");
				if (Debug.isLevelEnabled(Debug.SECURITY_CONFIG)) {
	                Debug.log(Debug.SECURITY_CONFIG,
	                        "[In case SPID not configured in SOA_SPID table then SPID value 0 will be set in cache " + "].");
	            }
			}
            

            if (Debug.isLevelEnabled(Debug.SECURITY_CONFIG)) {
                Debug.log(Debug.SECURITY_CONFIG,
                        "createInstance(): Instance Created for [" + spid
                            + "] with values SPID as [" + spidCache.getSpid()                                                   
                            + "].");
            }
        } catch (DatabaseException e ) {
            Debug.error("createInstance(): Unable to acquire Database connection. Error: " + e.toString());
        } catch (Exception e) {        	
            Debug.error("createInstance(): Error: " + e.toString());
        } finally {
            /** Here try is required to release the DB Connection Pool Instance */
            try {
                DBInterface.releaseConnection(dbConn);
            }
            catch (Exception e) {
                Debug.error("createInstance(): Error: " + e.toString());
            }
        }
        return spidCache;
    }
    
    public static SOAConfiguredSPIDCache getInstance (String spid) throws ConfiguredSOASPIDCacheException
    {

        if (!StringUtils.hasValue(spid)) {
            throw new ConfiguredSOASPIDCacheException("getInstance(spid): spid is null or empty String.");
        }

        SOAConfiguredSPIDCache spidCache = null;
        
        spidCache = (SOAConfiguredSPIDCache) soaSpidProp.get(spid);

        if (spidCache == null)
        {
            synchronized (soaSpidProp) {
            	spidCache = createInstance(spid);
                if (spidCache!=null) {
                	soaSpidProp.put(spidCache.getSpid(), spidCache);
                }
                if (Debug.isLevelEnabled(Debug.SECURITY_CONFIG)) {
                    Debug.log(Debug.SECURITY_CONFIG,
                            "getInstance(spid): Total instance in pool for SOAConfiguredSPIDCache are [" + soaSpidProp.size() + "]");
                }
            }
        }
        else
        {
            if (Debug.isLevelEnabled(Debug.SECURITY_CONFIG)) {
                Debug.log(Debug.SECURITY_CONFIG,
                        "getInstance(spid): Instance found in pool for spid [" + spid + "]");
            }
        }

        if (Debug.isLevelEnabled(Debug.SECURITY_CONFIG)) {
            Debug.log(Debug.SECURITY_CONFIG,
                    "getInstance(spid): Returning instance for spid [" + spid + "]");
        }

        return spidCache;
    }

   
    public void flushCache() throws FrameworkException 
    {
        if (soaSpidProp!=null)
        {
            synchronized(soaSpidProp)
            {
            	soaSpidProp.clear();
                if (Debug.isLevelEnabled(Debug.SECURITY_CONFIG)) {
                    Debug.log(Debug.SECURITY_CONFIG,
                            "flushSOAConfiguredSPID(): Flushing the SOA_SPID Properties...");
                }
            }
        } else {
            if (Debug.isLevelEnabled(Debug.SECURITY_CONFIG)) {
                Debug.log(Debug.SECURITY_CONFIG,
                        "flushSOAConfiguredSPID(): Being null no need to flush SOA_SPID Properties...");
            }
        }
    }
	
	
	/*
	 * A single private instance of this class will be created to assist in cache flushing.
 	 */
	private SOAConfiguredSPIDCache(){
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG )){
			
			Debug.log(Debug.SYSTEM_CONFIG, className + " : Initializing...");
		}
			
		try
        {
            CacheManager.getRegistrar().register( this );
                       
        }
        catch ( Exception e )
        {
            Debug.warning( e.toString() );
        }
	}
	
    //Used exclusively for cache flushing.
    private static SOAConfiguredSPIDCache flush = new SOAConfiguredSPIDCache( );


	public String getSpid() {
		return spid;
	}

	public void setSpid(String spid) {
		this.spid = spid;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}    
	

   
}
