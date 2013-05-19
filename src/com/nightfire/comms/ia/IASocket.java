/**
 * Copyright (c) 2002 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: $
 *
 */

package com.nightfire.comms.ia;

// JDK import
import javax.net.*;
import javax.net.ssl.*;
import com.nightfire.framework.util.Debug;


public class IASocket
{
    
    private static final long MSEC_PER_SEC = 1000;

    private static final long MSEC_PER_MINUTE = 60 * MSEC_PER_SEC;	
	
    public SSLSocket socket;
    public long createTime;
    public long lastUsedTime;
    public boolean valid = true;
    
    public IASocket( SSLSocket socket )
    {
	    this.socket = socket;
	    this.createTime = System.currentTimeMillis();
	    this.lastUsedTime = System.currentTimeMillis();
    }
	    
    
    /*
     * Helper method to determine if this socket has been unused for more than 5 minutes
     *
     * @return - whether this socket has been unused for more than 5 minutes
     */
    public boolean inactiveTimerExpired()
    {
	    
	    long delta 
		    = System.currentTimeMillis() - lastUsedTime;
	    
	    Debug.log ( Debug.MSG_STATUS, "inactiveTimerExpired delta ["
			    + delta +"] ms." );
	    
	    if ( delta > MSEC_PER_MINUTE * 5  )
	    {
		return true;
	    }
	    else
	    {
		return false;
	    }
    }

    /*
     * Helper method to determine whether this socket is older 
     * than 3 hours
     *
     * @return boolean -whether this socket is older than 3 hours.
     */
    public boolean activeTimerExpired()
    {	 
	    long delta 
		    = System.currentTimeMillis() - createTime;

	    Debug.log ( Debug.MSG_STATUS, "activeTimerExpired delta ["
			    + delta +"] ms." );
	    
	    
	    if ( delta >  MSEC_PER_MINUTE * 180  )
	    {
		return true;
	    }
	    else
	    {
		return false;
	    }
    }

    /*
     * Helper method to update the last time this socket was used
     */
    public void setLastUsedTime()
    {
	    lastUsedTime = System.currentTimeMillis();
    }

    
    /*
     * Helper method that marks this socket as invalid
     */
    public void invalidate()
    {
	    this.valid = false;
    }

    /*
     * Helper method to determine whether this socket is valid or invalid
     *
     * @return boolean - whether this socket is valid or not.
     */
    public boolean isValid()
    {
	    return valid;
    }
			
}

