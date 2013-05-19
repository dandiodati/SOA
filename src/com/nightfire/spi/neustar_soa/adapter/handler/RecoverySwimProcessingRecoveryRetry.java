/**
 * The purpose of this class Resends Swim processing recovery request 
 * 
 * @author Sreedhar K
 * @version 3.3
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * 
 * @see java.util.Date;
 * @see java.util.List;
 * 
 * @see com.nightfire.spi.neustar_soa.adapter.NPACAdapter;
 * @see com.nightfire.spi.neustar_soa.adapter.Session;
 */

/**
	Revision History
	---------------------
	Rev#        Modified By     Date                Reason
	-----       -----------     ----------          --------------------------
	1           Sreedhar         09/04/2005         Created
	2           Sreedhar         10/10/2005         Modified to implement 
													swim_more_data functionality
*/
package com.nightfire.spi.neustar_soa.adapter.handler;

import java.util.Date;
import java.util.List;

import com.nightfire.spi.neustar_soa.adapter.NPACAdapter;
import com.nightfire.spi.neustar_soa.adapter.Session;

public class RecoverySwimProcessingRecoveryRetry extends Retry {
    
    /*
     * A reference to the adapter, used to resend the request.
     */
    private NPACAdapter adapter;
    
    /*
     * The region/association for which we are recovering.
     */
    private int region;
    
    /*
     *  Revcovery complete time 
     */
    
    private Date recoveryCompleteTime;
    
    /*
     *  LasNotification time we got from NPAC
     */
    private Date lastNotificationTime;
    
    /*
     * action Id we populated in the reply.
     */
    private String swimActionID;
    
    /*
     * Spid involved in the request    
     */ 
    
    private String spid;
    
    /*
     * Recovery interval to send the recovery again.
     */  
    private long recoveryInterval;
    
    /*
     * List of spids associated with current region
     */ 
    private List spids;
    
    /*
     * This is the index of the customer SPID in the spids list for
     * which we are currently performing recovery.
     */ 
    private int spidIndex;

   /**
    * Constructor
    * 
    * @param retryWait waiting period to retry
    * @param adapter  NPACAdapter class
    * @param session session involved
    * @param region  region associated with the request
    * @param spids avilable spids for the region
    * @param spidIndex index of the spid in the list of spids
    * @param recoveryCompleteTime Date
    * @param lastNotificationTime Date
    * @param swimActionID Swim action Id involved in the request
    * @param spid spid associated in the request.
    * @param recoveryInterval retry interval
    */

    public RecoverySwimProcessingRecoveryRetry(long retryWait,
                                                NPACAdapter adapter, 
                                                Session session, 
                                                int region, 
                                                List spids, 
                                                int spidIndex,
                                                Date recoveryCompleteTime, 
                                                Date lastNotificationTime,
                                                String swimActionID, 
                                                String spid,
                                                long recoveryInterval) {
        super(retryWait, session);
        this.spids= spids;
        this.spidIndex = spidIndex;
        this.adapter = adapter;
        this.recoveryCompleteTime = recoveryCompleteTime;
        this.lastNotificationTime = lastNotificationTime;
        this.region = region;
        this.swimActionID = swimActionID;
        this.spid = spid;
        this.recoveryInterval=recoveryInterval;
    }

    protected void retry() {
        adapter.sendRecoverySwimRequest(session, 
                                        region,
                                        spids,
                                        spidIndex,
                                        recoveryCompleteTime, 
                                        lastNotificationTime, 
                                        swimActionID, 
                                        spid,
                                        recoveryInterval);


    }

}
