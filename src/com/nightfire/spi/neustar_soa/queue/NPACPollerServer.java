package com.nightfire.spi.neustar_soa.queue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.communications.ComServerBase;
import com.nightfire.spi.neustar_soa.adapter.NPACComServer;
import com.nightfire.spi.neustar_soa.adapter.NPACConstants;

public class NPACPollerServer extends ComServerBase{

   /**
    * Property indicating the maximum number of workers in the queue worker
    * thread pool (optional, default is 1).
    */
   public static final String MAX_QUEUE_WORKER_THREADS_PROP =
                                 "MAX_QUEUE_WORKER_THREADS";

   public static final int DEFAULT_MAX_QUEUE_WORKER_THREADS = 1;

   public static final String WHERE_CONDITION_PROP = "WHERE_CONDITION";

   public static final String TIMER_PROP = "TIMER";

   public static final String NPAC_COM_SERVER_KEY_PROP = "NPAC_COM_SERVER_KEY";

   public static final String NPAC_COM_SERVER_TYPE_PROP = "NPAC_COM_SERVER_TYPE";
   
   public static final String MESSAGE_AGE = "MESSAGE_AGE";
   
   public static final int DEFAULT_MESSAGE_AGE_IN_DAYS = 3;

   /**
    * The array of pollers created by this com server.
    */
   private Poller[] pollers;

   public NPACPollerServer(String key, String type)
                           throws ProcessingException{

       super(key, type);

       // get optional where condition
       String whereCondition = getPropertyValue( WHERE_CONDITION_PROP );
       
       // get optional where condition
       String messageAge = getPropertyValue( MESSAGE_AGE );
       
       String npacComServerType =
          getRequiredPropertyValue( NPAC_COM_SERVER_TYPE_PROP );

       List npacComServerProps = new ArrayList();
       
       Debug.log(Debug.SYSTEM_CONFIG, "where condition: ["+whereCondition+"]");
       
       if(StringUtils.hasValue(messageAge)){    	   
    	   whereCondition = whereCondition + " AND ARRIVALTIME > SYSDATE-"+messageAge.toString();
    	   Debug.log(Debug.SYSTEM_CONFIG, "where condition with preconfigured time: ["+whereCondition+"]");
       }else{
    	   whereCondition = whereCondition + " AND ARRIVALTIME > SYSDATE-"+DEFAULT_MESSAGE_AGE_IN_DAYS;
    	   Debug.log(Debug.SYSTEM_CONFIG, "where condition with default preconfigured time: ["+whereCondition+"]");
       }
       
       for (int Ix = 0; true; Ix++) { 
    	       	   
    	   String npacComServerKey = getPropertyValue(PersistentProperty.
    			           getPropNameIteration(NPAC_COM_SERVER_KEY_PROP, Ix));
    	   
    	   Debug.log(Debug.SYSTEM_CONFIG, "NPAC_COM_SERVER_KEY_PROP : "+npacComServerKey); 
	      //stop when no more properties are specified
           if (!StringUtils.hasValue(npacComServerKey))
               break;
            try{
            	Map npacComServerPropsLocal = PersistentProperty.getProperties(npacComServerKey,
                                                  npacComServerType);
            	npacComServerProps.add(npacComServerPropsLocal);
            	
               }
            catch(Exception ex){
              throw new ProcessingException("Could not located properties with key ["+
                                            npacComServerKey+"] and type ["+
                                            npacComServerType+"]: "+ex);
             }
       }

       // create poller instances
       pollers = initPollers(whereCondition,
                             getMaxWorkerThreads(),
                             getTimerInterval(),
                             npacComServerProps );

   }

   protected Poller[] initPollers(String whereCondition,
                                  int maxWorkerThreads,
                                  long timer,
                                  List npacComServerProperties)
                                  throws ProcessingException{

      SPID[] spids = getSPIDs(npacComServerProperties);

      List newPollers = new ArrayList(spids.length+1);

      for(int i = 0; i < spids.length; i++){

         try{

            NPACQueuePoller poller = new NPACQueuePoller(whereCondition,
                                                         spids[i].toString(),
                                                         maxWorkerThreads,
                                                         timer);

            newPollers.add(poller);

         }
         catch(Exception ex){
            Debug.logStackTrace(ex);
            throw new ProcessingException("Could not create poller for SPID ["+
                                          spids[i]+"]: "+ex);
         }

      }

      // add catch-all poller to process any messages that were not
      // otherwise processed by the other SPID-specific pollers
      // Add catch-all poller to process any messages that were not
      // otherwise processed by the other SPID-specific pollers. This
      // provides backwards compatibility.
//      String catchAllCondition = SOAMessageType.SPID_COL+
//                                 " is null";
//
//      if( StringUtils.hasValue( whereCondition ) ){
//         catchAllCondition = whereCondition + " and "+catchAllCondition;
//      }
//
//      try{
//
//         NPACQueuePoller poller = new NPACQueuePoller(catchAllCondition,
//                                                      null,
//                                                      maxWorkerThreads,
//                                                      timer);
//
//         newPollers.add(poller);
//
//      }
//      catch(Exception ex){
//         Debug.error("Could not create poller with condition ["+
//                     catchAllCondition+"]: "+ex);
//      }


      Poller[] pollerArray = new Poller[ newPollers.size() ];
      newPollers.toArray(pollerArray);

      return pollerArray;

   }


   /**
    * This gets the timer interval from properties.
    * This is how long the pollers will sleep
    * when there are no longer any messages to process.
    */
   private long getTimerInterval() throws ProcessingException{

       String timerValue = getRequiredPropertyValue( TIMER_PROP );

       long timer;

       try{
          timer = Long.parseLong( timerValue );
       }
       catch(NumberFormatException nfex){
          throw new ProcessingException("The value ["+timerValue+
                                        "] for property ["+TIMER_PROP+
                                        "] is not a valid integer.");
       }

       // convert seconds to milliseconds
       timer *= 1000;

       if( Debug.isLevelEnabled(Debug.SECURITY_CONFIG) ){
          Debug.log(Debug.SYSTEM_CONFIG, "Polling timer set to ["+timer+"] ms");
       }

       return timer;

   }

   /**
    * This gets the max number of worker threads that each poller should
    * use when processing consumed messages.
    *
    * @throws ProcessingException if the value found for the max worker threads
    *                             property is not an integer.
    * @return int the max number of worker threads. The default value is one
    *             if the property is not present.
    */
   private int getMaxWorkerThreads() throws ProcessingException{

      int maxWorkerThreads = DEFAULT_MAX_QUEUE_WORKER_THREADS;

      String maxWorkerThreadValue =
          getPropertyValue(MAX_QUEUE_WORKER_THREADS_PROP);

      if( maxWorkerThreadValue != null ){

         try{
            maxWorkerThreads = Integer.parseInt(maxWorkerThreadValue);
         }
         catch(NumberFormatException nfex){
            throw new ProcessingException("The value ["+maxWorkerThreadValue+
                                          "] given for property ["+
                                          MAX_QUEUE_WORKER_THREADS_PROP+
                                          "] is not a valid integer.");
         }

      }

      return maxWorkerThreads;

   }

   protected SPID[] getSPIDs(Map npacComServerProperties){

      List spids = new ArrayList();

      int index = 0;
      String propName = getSpidPropertyName(index);
      String spidValue = (String) npacComServerProperties.get( propName );

      while(spidValue != null){

         SPID spid = new SPID( spidValue );

         for(int region = 0; region < NPACConstants.REGION_COUNT; region++){

            String regionProp = NPACComServer.REGION_PROP_PREFIX+
                                region+"_"+index;

            String regionValue =
               (String) npacComServerProperties.get( regionProp );

            if( StringUtils.hasValue(regionValue) ){

               if( StringUtils.getBoolean(regionValue, false) ){
                  spid.setActiveRegion(region);
               }

            }

         }

         spids.add( spid );

         index++;
         propName = getSpidPropertyName(index);
         spidValue = (String) npacComServerProperties.get( propName );

      }

      // copy the spid list into an array and return it
      SPID[] results = new SPID[ spids.size() ];
      spids.toArray(results);

      return results;

   }

   protected SPID[] getSPIDs(List npacComServerProperties) {
		List spidList = new ArrayList();
		for (Iterator iterator = npacComServerProperties.iterator(); iterator.hasNext();) {
			try {
				SPID[] spid = getSPIDs((Map) iterator.next());
				
				spidList.addAll(Arrays.asList(spid));
			} catch (Exception ex) {
				Debug.error("Could not initialize list : " + ex);
			}
		}
		
		SPID spids[] = new SPID[spidList.size()];
		spidList.toArray(spids);
		return spids;
	}

   /**
    * Convenience method for getting an indexed SPID property name.
    *
    * @param index int
    * @return String
    */
   private static String getSpidPropertyName(int index){

      return PersistentProperty.getPropNameIteration(
                NPACComServer.SECONDARY_SPID_PROP,
                index);

   }

   /**
    * This starts all of the pollers.
    */
   public void run(){

      // start all pollers
      for(int i = 0; i < pollers.length; i++){

         pollers[i].start();

      }

   }

   /**
    * Shutdown all of the poller threads.
    */
   public void shutdown(){

      // shutdown all pollers
      for(int i = 0; i < pollers.length; i++){

         pollers[i].shutdown();

      }

   }

   protected class SPID{

      private String spid;

      private boolean[] activeRegions;

      public SPID(String spid){

         this.spid = spid;
         activeRegions = new boolean[NPACConstants.REGION_COUNT];

      }

      public void setActiveRegion(int region){

         activeRegions[region] = true;

      }

      public boolean isActiveRegion(int region){

         return activeRegions[region];

      }

      public String toString(){

         return spid;

      }

   }

}