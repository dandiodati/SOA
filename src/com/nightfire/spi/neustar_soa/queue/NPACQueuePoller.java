package com.nightfire.spi.neustar_soa.queue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.HashMap;

import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.util.Debug;

public class NPACQueuePoller implements Poller {

   private NPACQueuePollerThread poller;

   public NPACQueuePoller(String whereCondition,
                          String spid,
                          int maxThreadCount,
                          long pollingInterval)
                          throws Exception{	  

      NPACMessageConsumer consumer = new NPACMessageConsumer();

      Map values = new HashMap();
      if( spid != null ){
         values.put(NPACMessageType.SPID_COL, spid);
      }

      poller = new NPACQueuePollerThread(consumer,
                                     NPACMessageType.NPAC_MESSAGE_TYPE,
                                     whereCondition,
                                     values,
                                     maxThreadCount,
                                     pollingInterval);

   }

   public void start(){
	   	   
      poller.start();

   }

   public void shutdown(){

      poller.shutdown();

   }

}
