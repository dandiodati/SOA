///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import com.nightfire.common.ProcessingException;
import com.nightfire.spi.common.communications.*;

import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;

import com.nightfire.spi.neustar_soa.adapter.smartsockets.Receiver;
import com.nightfire.spi.neustar_soa.adapter.smartsockets.NPACReceiver;
import com.nightfire.spi.neustar_soa.queue.NPACQueueUtils;
import com.nightfire.spi.neustar_soa.utils.SOAQueryConstants;
import com.nightfire.spi.neustar_soa.utils.TimeZoneUtil;


/**
* This is the Com Server used to receive messages from the NPAC's OSS Gateway.
* This server publishes an RMI interface which it registers with our
* SOAP receiver service. The SOAP service then forwards incoming XML
* messages to this server via RMI for processing.
*
* This server hides all of the necessary "bookkeeping" requests and responses
* from the driver chain. This gateway's driver chains only need to worry about
* messages that would be sent or received by the customer. The comm server
* automatically takes care of creating new sessions, associations, sending
* reply notifications, etc. that are required by the NPAC OSS Gateway.
*/
public class NPACComServer extends ComServerBase
                           implements Receiver{
  
   /**
   * The name of the SmartSockets' project in which the NPAC OSS Gateway
   * will be running.
   */
   public static final String NPAC_PROJECT_PROP = "NPAC_PROJECT";

   /**
   * The name of the host where the NPAC's SmartSockets' RTServer is running.
   */
   public static final String NPAC_RTSERVER_PROP = "NPAC_RTSERVER";

   /**
   * The prefix for the SmartSockets subjects to which requests
   * will be sent. The prefix property value plus a router ID
   * makes up the entire subject.
   */
   public static final String NPAC_SUBJECT_PROP = "NPAC_SUBJECT";

   /**
    * This is an iterating property (SOA_SUBJECT_0, SOA_SUBJECT_1,
    * SOA_SUBJECT_2, etc.)
    * used to indicate all of the subjects to which the comm server
    * should listen for notifications from the OSS NPAC Gateway.
    */
   public static final String SOA_SUBJECT_PROP_PREFIX = "SOA_SUBJECT";

   /**
   * This property contains the value of the replyTo field in all messages
   * we send to the NPAC. This is where the NPAC will send reply messages.
   */
   public static final String SOA_UNIQUE_SUBJECT_PROP = "SOA_UNIQUE_SUBJECT";

   /**
   * The property containing the username for logging into the NPAC Gateway.
   */
   public static final String NPAC_USER_PROP = "NPAC_USER";

   /**
   * The property containing the password for logging into the NPAC Gateway.
   */
   public static final String NPAC_PASSWORD_PROP = "NPAC_PASSWORD";

   /**
   * The name of the database sequence from which the next invoke ID will
   * be retrieved for a send message.
   */
   public static final String INVOKE_ID_SEQUENCE_PROP = "INVOKE_ID_SEQUENCE";

   /**
   * The default invoke ID value that will be used if the INVOKE_ID_SEQUENCE
   * property is not populated.
   */
   public static final String DEFAULT_INVOKE_ID_SEQUENCE = "NeustarSOAMsgKey";

   /**
   * This is the name of the table in which the last notification time
   * for a particular secondary SPID and region will get logged.
   */
   public static final String LAST_NOTIFICATION_TIME_TABLE_PROP =
                                   "LAST_NOTIFICATION_TIME_TABLE";

   /**
   * This is the default value for the LAST_NOTIFICATION_TIME_TABLE table
   * if this property is not populated.
   */
   public static final String DEFAULT_LAST_NOTIFICATION_TIME_TABLE =
                                   "LAST_NPAC_RESPONSE_TIME";

   /**
   * This property contains the name of the default primary SPID that
   * will get associated with a secondary SPID if it does not have
   * an explicitly configured primary SPID.
   */
   public static final String DEFAULT_PRIMARY_SPID_PROP = "DEFAULT_PRIMARY_SPID";

   /**
   * This is an iterating property used to supply a customer's SPID.
   */
   public static final String SECONDARY_SPID_PROP = "SECONDARY_SPID";

   /**
   * This is actually an iterating property that will get matched up with
   * the SECONDARY_SPID with the same iterative index. (e.g. PRIMARY_SPID_2
   * is the primary SPID for SECONDARY_SPID_2). Up to 39 secondary SPIDs
   * may be associated with the same primary SPID.
   */
   public static final String PRIMARY_SPID_PROP = "PRIMARY_SPID";

   /**
   * This iterating boolean property indicates whether the corresponding
   * SECONDARY_SPID supports NPAC region 0. So, for example,
   * if the value of REGION0_4 is "true", then the SPID defined in
   * SECONDARY_SPID_4 is supported in region 0. The default value for this
   * property, if not present, will be false.
   */
   public static final String REGION_PROP_PREFIX = "REGION";

   /**
   * This is the interval (in seconds) that the keep alive thread should
   * sleep between sending keep alive requests for active sessions.
   */
   public static final String KEEP_ALIVE_INTERVAL_PROP = "KEEP_ALIVE_INTERVAL";

   /**
   * This is the interval, in seconds, that the adapter will wait
   * when it needs to resend a request (such as NewSession).
   */
   public static final String RETRY_INTERVAL_PROP = "RETRY_INTERVAL";

   /**
   * A default retry interval of 5 minutes.
   */
   public static final long DEFAULT_RETRY_INTERVAL = 300000;

   /**
    * This property, in seconds, tells how long we should wait before
    * resending a request to the NPAC which received no reply.
    */
   public static final String RESEND_INTERVAL_PROP = "RESEND_INTERVAL";

   /**
   * A default resend interval (in seconds) of 15 minutes.
   */
   public static final long DEFAULT_RESEND_INTERVAL = 900000;

   /**
   * This is the window of time (in minutes) that will be used in calculating
   * the start/stop times for download requests.
   */
   public static final String RECOVERY_INTERVAL_PROP = "RECOVERY_INTERVAL";

   /**
    * The default recovery interval (one hour) represented in
    * milliseconds.
    */
   public static final long DEFAULT_RECOVERY_INTERVAL = 3600000;

   /**
   * An optional property that indicates a buffer (in minutes) that
   * should be subtracted from the last known notification time when
   * performing recovery. This property is added at the suggestion of the
   * LSMS developers who have discovered that some notifications can
   * be missed during recovery without this additional padding. I would
   * recommend not setting this property unless it is dicovered to be
   * necessary in production.
   */
   public static final String RECOVERY_PADDING_PROP = "RECOVERY_PADDING";

   /**
    * By default, do not push back the recovery time. If it is discovered
    * that notifications are somehow being missed during recovery, then the
    * RECOVERY_PADDING property can be set to try to catch these missing
    * notifications.
    */
   public static final int DEFAULT_RECOVERY_PADDING = 0;

   /**
   * This property indicates the default time (in ms) to wait before attempting
   * to cleanup unused threads in the worker thread queue. The
   * default value should usually be sufficient, but if tuning
   * is necessary, the change is easier to make in properties than in the
   * code.
   */
   public static final String THREAD_WAIT_TIME_PROP = "THREAD_WAIT_TIME";

   /**
   * This is the default time to wait before attempting to cleanup unused
   * threads in the worker thread queue. This value is
   * equal to 10 minutes in ms.
   */
   private static final long DEFAULT_THREAD_WAIT_TIME = 36000000;

   /**
   * This property indicates the maximum number of waiting threads that should
   * be kept ready and waiting in the worker thread queue when
   * there is no work to be done. This is an optional property,
   * and the default value should be sufficient, but the property
   * allows for more flexible tuning if necessary.
   */
   public static final String WAITING_THREAD_COUNT_PROP
                                 = "WAITING_THREAD_COUNT";

   /**
   * This is default the maximum number of waiting threads that should
   * be kept ready and waiting in the worker thread queue when
   * there is no work to be done.
   */
   private static final int DEFAULT_WAITING_THREAD_COUNT = 20;

   /**
    * property based on which records will be deleted from npac_queue table
    */
   public static final String MESSAGE_RESENT_TIME = "MESSAGE_RESENT_TIME";
   /**
   * This must be static, because it must be accessible by processors in the
   * driver chain that need to access values such as the Session ID.
   */
   private static NPACAdapter adapter = null;

   /**
   * This thread is responsible for timing the periodic keep alive requests
   * that get sent to the NPAC Gateway.
   */
   private KeepAliveThread keepAliveThread;
   
   /**
   * This provides utility methods for accessing and updated the
   * last notification time table in the database.
   */
   private LastNotificationTime lastNotificationTime;

   /**
    * The collection of receivers listening for notifications from
    * the NPAC Gateway.
    */
   private NPACReceiver[] receivers;

   /**
   * Runnable tasks are added to this queue in order
   * to perform work in separate worker threads.
   */
   private WorkQueue queue;

   /**
   * Incoming smart sockets messages are added to this queue in order
   * to perform work in separate worker threads.
   */
   private WorkQueue smartSocketsThreads;

   /**
   * To store Swim based or Time based data for each region.
   */
   HashMap recoveryMap = new HashMap();

   /**
	 * Default time to check before converting the status of records from retry
	 * to sent.
	 */
   private static String msgResentDefaultTime = "2880";
   
   /**
    * This represents the connectivity key
    */
    private String connectivityKey;
   
   /**
   *
   * @throws ProcessingException if any required properties are missing
   *                             of have invalid values.
   */
   public NPACComServer(String key, String type)
                           throws ProcessingException{
	   
      super(key, type);
      
      connectivityKey = key;
      
      if( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){
          Debug.log(Debug.MSG_STATUS, " ConnectivityKey = [" + connectivityKey + "]");
      }
      //Move and delete the old messages from npac queue table
      String msgPickTime = getPropertyValue(MESSAGE_RESENT_TIME);
      
      if(StringUtils.hasValue(msgPickTime)){
    	  
    	  resetSentMessages(msgPickTime);
      
      }else{
    	  
    	  resetSentMessages(msgResentDefaultTime);
    	  
    	  Debug.log(Debug.MSG_STATUS, "The [ OLDMESSAGE_DELETION_TIME ] property is set to empty. " +
    	  		"So using bydefault time of [" + msgResentDefaultTime + "]");
      }
           //configure LAST_NPAC_RESPONSE_TIME table
      initLastNpacResponseTime();
           // make sure that the adapter instance is initialized
      initNPACAdapter();
           // configured the secondary SPIDs and sessions from properties
      initSPIDs();
		   // get data from RECOVERY_PROCESS table.
	  getTimeBasedRecovery();
   }

   /**
   * Utility method for getting the boolean value of the property with the
   * given name. By default, if the property is missing or invalid, this
   * will return false.
   */
   private boolean getBooleanPropertyValue(String propertyName){

      boolean result = false;

      String propValue = getPropertyValue(propertyName);

      if( StringUtils.hasValue( propValue ) ){

         result = StringUtils.getBoolean( propValue, false );

      }

      return result;

   }

   /**
   * This creates the NPACAdapter instance and initializes
   * it using Peristent Property values.
   */
   private void initNPACAdapter() throws ProcessingException{

      if(adapter == null){

         synchronized(NPACComServer.class){

            if(adapter == null){

               StringBuffer errorBuffer = new StringBuffer();

               String npacProject = getRequiredPropertyValue( NPAC_PROJECT_PROP,
                                                              errorBuffer );
               
               String npacSubject = getRequiredPropertyValue( NPAC_SUBJECT_PROP,
                                                              errorBuffer );
               
               String npacServer = getRequiredPropertyValue( NPAC_RTSERVER_PROP,
                                                             errorBuffer );
                              
               String user = getRequiredPropertyValue( NPAC_USER_PROP,
                                                       errorBuffer );
               

               String password = getRequiredPropertyValue( NPAC_PASSWORD_PROP,
                                                           errorBuffer );
               
               String soaReplyToSubject =
                         getRequiredPropertyValue( SOA_UNIQUE_SUBJECT_PROP,
                                                   errorBuffer );
               

               // this is the interval of time that the adapter will wait
               // before sending retry requests
               long retryInterval = getPropertyAsMs(RETRY_INTERVAL_PROP,
                                                    DEFAULT_RETRY_INTERVAL,
                                                    errorBuffer);


               // this is the interval of time that the adapter will wait
               // for a reply from the NPAC before resending a request
               long resendInterval = getPropertyAsMs(RESEND_INTERVAL_PROP,
                                                     DEFAULT_RESEND_INTERVAL,
                                                     errorBuffer);

               long recoveryInterval =
                  getMinutesPropertyAsMs(RECOVERY_INTERVAL_PROP,
                                         DEFAULT_RECOVERY_INTERVAL,
                                         errorBuffer);
               // subtract one second (the gateway sends back a
               // criteria-too-large error for a window of exactly one
               // hour, so instead, we'll use 59 minutes 59 seconds.
               recoveryInterval -= 1000;

               long recoveryPadding =
                  getMinutesPropertyAsMs( RECOVERY_PADDING_PROP,
                                          DEFAULT_RECOVERY_PADDING,
                                          errorBuffer );

               if( errorBuffer.length() > 0 ){

                  throw new ProcessingException( errorBuffer.toString() );

               }

               String sequenceID = getPropertyValue( INVOKE_ID_SEQUENCE_PROP );
               // set the default value if necessary
               if( sequenceID == null ) sequenceID = DEFAULT_INVOKE_ID_SEQUENCE;

               // initialize access to the last notification time table
               String tableName =
                  getPropertyValue( LAST_NOTIFICATION_TIME_TABLE_PROP );
               if( tableName == null ){
                   tableName = DEFAULT_LAST_NOTIFICATION_TIME_TABLE;
               }
               lastNotificationTime = new LastNotificationTime( tableName );

               // check for the optional work queue properties
               long threadWait = DEFAULT_THREAD_WAIT_TIME;
               String prop = getPropertyValue(THREAD_WAIT_TIME_PROP);
               if( prop != null ){

                  try{
                     threadWait = Long.parseLong(prop);
                  }
                  catch(NumberFormatException nfex){
                     Debug.error("The value ["+prop+"] for property ["+
                                 THREAD_WAIT_TIME_PROP+
                                 "] is not a valid numeric value.");
                  }

               }

               int idleThreadCount = DEFAULT_WAITING_THREAD_COUNT;
               prop = getPropertyValue(WAITING_THREAD_COUNT_PROP);
               if( prop != null ){

                  try{
                     idleThreadCount = Integer.parseInt(prop);
                  }
                  catch(NumberFormatException nfex){
                     Debug.error("The value ["+prop+"] for property ["+
                                 WAITING_THREAD_COUNT_PROP+
                                 "] is not a valid numeric value.");
                  }

               }

               if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){

                  Debug.log(Debug.SYSTEM_CONFIG,
                            "Initializing the NPAC Adapter:\n\tNPAC Project ["+
                            npacProject+"]\n\tNPAC Subject ["+npacSubject+
                            "]\n\tUser ID ["+user+
                            "]\n\tSOA Unique Subject ["+soaReplyToSubject+
                            "]\n\tNPAC RTServer location ["+npacServer+
                            "]\n\tInvoke ID Sequence ["+sequenceID+
                            "]\n\tLast Notification Time Table ["+tableName+
                            "]\n\tRetry Interval ["+retryInterval+
                            "] ms\n\tResend Timeout Interval ["+resendInterval+
                            "] ms\n\tRecovery Window ["+recoveryInterval+
                            "] ms\n\tRecovery Padding ["+recoveryPadding+
                            "] ms\n");

               }


               smartSocketsThreads = new WorkQueue(threadWait,
                                                   idleThreadCount,
                                                   idleThreadCount*2);

               // initialize receiver(s) to listen for
               // messages from the NPAC
               initSmartSocketsReceivers(npacProject,
                                         soaReplyToSubject,
                                         user,
                                         password,
                                         npacServer,
                                         smartSocketsThreads);

               queue = new WorkQueue(threadWait,
                                     idleThreadCount,
                                     idleThreadCount);

               adapter = new NPACAdapter(sequenceID,
                                         npacProject,
                                         npacSubject,
                                         user,
                                         password,
                                         npacServer,
                                         soaReplyToSubject,
                                         retryInterval,
                                         resendInterval,
                                         lastNotificationTime,
                                         recoveryInterval,
                                         recoveryPadding,
                                         queue,
										 recoveryMap,
                                         this);

            }

         }

      }

   }
   
   /**
    * This will configured LAST_NPAC_RESPONSE_TIME for all configured Secondary SPID in All 
    * configured region
    * @throws ProcessingException .
    */
   private void initLastNpacResponseTime() throws ProcessingException{
	   
	   try{		   
		   // initialize access to the last notification time table
           String tableName =
              getPropertyValue( LAST_NOTIFICATION_TIME_TABLE_PROP );
           if( tableName == null ){
               tableName = DEFAULT_LAST_NOTIFICATION_TIME_TABLE;
           }               
	      int spidIndex = 0;
	      String propName =
	         PersistentProperty.getPropNameIteration(SECONDARY_SPID_PROP,
	                                                 spidIndex);
	      String spid = getPropertyValue(propName);      
	      
	      while(spid != null){

	         String regionProperty = null;

	         Connection conn = null;
	         
	         PreparedStatement ps = null;
	         
	         boolean configured = getRecoveryFlag(spid);
	         if(configured){
		         // check to see which regions are allowed for this SPID
		         for(int region = 0; region < NPACConstants.REGION_COUNT; region++){
	
		            regionProperty = PersistentProperty.getPropNameIteration(
		                                                            REGION_PROP_PREFIX+
		                                                            region,
		                                                            spidIndex);
		            boolean supported = getBooleanPropertyValue( regionProperty );
		            	         
		            if( supported){
		            	for(int type = 0; type < 3; type++){
		   	            	if( notPresent(spid ,region,type)){
					            try
					            {
					            	conn = DBInterface.acquireConnection();
						            // create the insert statement
						            StringBuffer buffer = new StringBuffer("INSERT INTO ");
						            buffer.append(tableName);
						            buffer.append(" ( ");
						            buffer.append("SPID");
						            buffer.append(", ");
						            buffer.append("REGIONID");
						            buffer.append(", ");
						            buffer.append("DATETIME");
						            buffer.append(", ");
						            buffer.append("TYPE");
						            buffer.append(") VALUES ( ?, ?, ?, ?)");
						            
						            ps = conn.prepareStatement(buffer.toString());
						            ps.setString(1, spid);
						            ps.setInt(2,region);
						            
						            String systemDate = TimeZoneUtil.convert("GMT", "MM-dd-yyyy-hhmmssa", new Date());
						            java.util.Date newTime = TimeZoneUtil.parse("GMT", "MM-dd-yyyy", systemDate);
						            Timestamp sqlTime = new Timestamp( newTime.getTime() ); 
						            
						            ps.setTimestamp(3, sqlTime);
						            ps.setInt(4, type);
						            					            
						            // Insert into LAST_NPAC_RESPONSE_TIME table.
						            Debug.log(Debug.NORMAL_STATUS, "Insert LastNpacResponseTime for SPID["+ spid+ "] and " +
					    					"REGIONID ["+ region+ "] and TYPE ["+ type+ "] and DATETIME ["+ sqlTime+ "]:" );
						            ps.execute();	
						            conn.commit();			            
					            }
					            catch(Exception ex)
					            {
									try
									{
				
										conn.rollback();
									}
									catch(Exception e)
									{
										Debug.log(Debug.SYSTEM_CONFIG,e.toString());
									}
				
					            	Debug.log(Debug.SYSTEM_CONFIG,ex.toString());
					            }
					            finally
					            {
					                try
					                {
					                	if (ps !=null)
					                	ps.close();
					                }catch(Exception dbEx)
						             {
						               	Debug.log(Debug.SYSTEM_CONFIG,dbEx.toString());
						             }
					                try{
										if( conn!= null)
										DBInterface.releaseConnection( conn );
					                }
					                catch(Exception dbEx)
					                {
					                	Debug.log(Debug.SYSTEM_CONFIG,dbEx.toString());
					                } 
					            }
			            	}
		            	}
		            }
	
		         }
	         }
	         spidIndex++;
	         propName = PersistentProperty.getPropNameIteration(SECONDARY_SPID_PROP,
	                                                            spidIndex);
	         spid = getPropertyValue(propName);
	      }
	   }
	   catch(Exception Ex)
       {
       	Debug.log(Debug.SYSTEM_CONFIG,Ex.toString());
       } 
    }

   /**
    * This will check last npac response time if already exist for given spid,region and type 
    * @param spid Secondary SPID.
    * @param region.
    * @param type 0,1 or 2
    * @throws ProcessingException .
    */
	private boolean notPresent(String spid, int region, int type) throws ProcessingException{
	
		 Connection conn = null;
	
		 PreparedStatement ps=null;
	
		 ResultSet rs=null;
	
		 boolean LastNpacResponseTime = true;
	
		 try
		 {
			conn = DBInterface.acquireConnection();
	
			ps = conn.prepareStatement(SOAQueryConstants.SOA_CONNECTIVITY_LAST_NPAC_RESPONSE);
			ps.setString(1, spid);
			ps.setInt(2, region);
			ps.setInt(3, type);
			rs = ps.executeQuery();
			if( rs.next())
			{
				LastNpacResponseTime =false;
				
			}
			Debug.log(Debug.NORMAL_STATUS, "LastNpacResponseTime for spid["+ spid+ "] and " +
					"region ["+ region+ "] and Type ["+ type+ "]:"+ LastNpacResponseTime );
		 }
		 catch(Exception DBEx)
		 {
			 Debug.log(Debug.SYSTEM_CONFIG,DBEx.toString());
		 }
		 finally
		 {
			 	 if (rs != null)
					try {
						rs.close();
					} catch (Exception clEx) {
						
						Debug.log(Debug.SYSTEM_CONFIG,clEx.toString());
					}
	
				 if (ps != null)
					try {
						ps.close();
					} catch (Exception clEx) {
						
						Debug.log(Debug.SYSTEM_CONFIG,clEx.toString());
					}			 
				 
				 if( conn!= null)
					try {
						DBInterface.releaseConnection( conn );
					} catch (Exception clEx) {
						
						Debug.log(Debug.SYSTEM_CONFIG,clEx.toString());
					}
			
		 }
	
		return LastNpacResponseTime;
	}
	
	/**
     * This will check configuration from SOA_RECOVERY_FLAGS Table 
     * @param spid Secondary SPID.
     * @throws ProcessingException .
     */
	private boolean getRecoveryFlag(String spid) throws ProcessingException{
	
		 Connection conn = null;
	
		 PreparedStatement ps=null;
	
		 ResultSet rs=null;
	
		 boolean npacResponseTimeConnectivityFalg = false;
	
		 try
		 {
			conn = DBInterface.acquireConnection();
	
			ps = conn.prepareStatement(SOAQueryConstants.SOA_RECOVERY_FLAGS);
			ps.setString(1, spid);
			rs = ps.executeQuery();
			if( rs.next())
			{   
				int flag = rs.getInt("NPACRESPONSETIMEFLAG");
				if (flag == 1) 
				npacResponseTimeConnectivityFalg = true;
			}
			Debug.log(Debug.NORMAL_STATUS, "npacResponseTimeConnectivityFalg for spid["+ spid+ "]:"+ npacResponseTimeConnectivityFalg);
		 }
		 catch(Exception DBEx)
		 {
			 Debug.log(Debug.SYSTEM_CONFIG,DBEx.toString());
		 }
		 finally
		 {
				 if (rs != null)
					try {
						rs.close();
					} catch (Exception clEx) {
						
						Debug.log(Debug.SYSTEM_CONFIG,clEx.toString());
					}
	
				 if (ps != null)
					try {
						ps.close();
					} catch (Exception clEx) {
						
						Debug.log(Debug.SYSTEM_CONFIG,clEx.toString());
					}			 
				 
				 if( conn!= null)
					try {
						DBInterface.releaseConnection( conn );
					} catch (Exception clEx) {
						
						Debug.log(Debug.SYSTEM_CONFIG,clEx.toString());
					}
				 
				 

		 }
	
		return npacResponseTimeConnectivityFalg;
	}

   /**
    * This gets the configured subjects from properties and creates
    * SmartSockets receivers to listen to these subjects.
    *
    * @param project String the NPAC's SmartSockets project name.
    * @param soaUniqueSubject String the unique subject to which reply
    *                                messages will be sent.
    * @param user String the user ID with which to login to the NPAC's
    *                    SmartSockets server.
    * @param password String the password with which to login to the NPAC's
    *                        SmartSockets server.
    * @param rtserver String the host (or comma-separated list of hosts)
    *                        where the SmartSockets RTServer is running.
    * @param maxIdleWorkerThreads int the maximum number of idle worker threads
    *                                 used in the work queues that process
    *                                 incoming messages.
    * @throws ProcessingException thrown if no SOA subjects were found
    *                             in properties.
    */
   private void initSmartSocketsReceivers(String project,
                                          String soaUniqueSubject,
                                          String user,
                                          String password,
                                          String rtserver,
                                          WorkQueue threads )
                                          throws ProcessingException{
	   

      // get list of SOA_SUBJECT properties
      List subjects = new ArrayList();

      int index = 0;
      String subject = getPropertyValue(SOA_SUBJECT_PROP_PREFIX+"_"+index);
      while( StringUtils.hasValue(subject) ){
		 
         subjects.add(subject);

         index++;
         subject = getPropertyValue(SOA_SUBJECT_PROP_PREFIX+"_"+index);

                
      }
      

      if( subjects.size() == 0 ){

         throw new ProcessingException( "No ["+
                                        SOA_SUBJECT_PROP_PREFIX+
                                       "] properties were specified." );

      }

      // make sure to listen for replies to the unique subject
      subjects.add( soaUniqueSubject );

      int subjectCount = subjects.size();

      // create array of receivers to listen to subjects
      receivers = new NPACReceiver[ subjectCount ];

      for(int i = 0; i < subjectCount; i++){

         try{

            subject = subjects.get(i).toString();

            Debug.log(Debug.SYSTEM_CONFIG,
                      "Creating listener for subject ["+
                      subject+"]" );

            NPACReceiver receiver = new NPACReceiver(project,
               subject,
               soaUniqueSubject+"_00"+i,
               user,
               password,
               rtserver,
               this,
               threads);

            receivers[i] = receiver;

         }
         catch(FrameworkException fex){

            throw new ProcessingException( fex.getMessage() );

         }

      }

   }

    /**
   * This uses persistent property values to create data objects
   * in the NPAC adapter to represent sessions/primary SPIDs,
   * secondary SPIDs, and whether secondary SPIDs are enabled for
   * particular regions. This method should only be called once by the
   * constructor.
   */
   private void getTimeBasedRecovery() throws ProcessingException{

	 Connection conn = null;

	 PreparedStatement ps=null;

	 Statement st=null;

	 ResultSet rs=null;

	 int regionID = 0;

	 int recoveryFalg = 0;

	 try
	 {

		conn = DBInterface.acquireConnection();

		st=conn.createStatement();				

		// get data from RECOVERY_PROCESS table.
		rs = st.executeQuery(SOAQueryConstants.SOA_RECOVERY_PROCESS);

		while( rs.next())
		{
			regionID = rs.getInt(1);

			recoveryFalg = rs.getInt(2);

			recoveryMap.put(Integer.valueOf(regionID), Integer.valueOf(recoveryFalg));
		}

		Debug.log(Debug.NORMAL_STATUS, "recoveryMap value in getTimeBasedRecovery():"+ recoveryMap );
		
	 }
	 catch(Exception DBEx)
	 {
		 Debug.log(Debug.SYSTEM_CONFIG,DBEx.toString());
	 }
	 finally
	 {
		 	 if (rs != null)
				try {
					rs.close();
				} catch (Exception clEx) {
					Debug.log(Debug.SYSTEM_CONFIG,clEx.toString());
				}

			 if (st != null)
				try {
					st.close();
				} catch (Exception clEx) {
					Debug.log(Debug.SYSTEM_CONFIG,clEx.toString());
				}			 
			 
			 if( conn!= null)
				try {
					DBInterface.releaseConnection( conn );
				} catch (Exception clEx) {
					Debug.log(Debug.SYSTEM_CONFIG,clEx.toString());
				}
		
		
	 }

	}

   /**
   * This uses persistent property values to create data objects
   * in the NPAC adapter to represent sessions/primary SPIDs,
   * secondary SPIDs, and whether secondary SPIDs are enabled for
   * particular regions. This method should only be called once by the
   * constructor.
   */
   private void initSPIDs() throws ProcessingException{

      int spidIndex = 0;
      String propName =
         PersistentProperty.getPropNameIteration(SECONDARY_SPID_PROP,
                                                 spidIndex);
      String spid = getPropertyValue(propName);      
      
      if(spid != null)
      {
    	  Connection conn = null;
    		
          PreparedStatement ps=null;

          try
	         {
	            conn = DBInterface.acquireConnection();	
	            // Delete Records from SOA_REGION_RECOVERY table before connecting to NPAC.
	            ps=conn.prepareStatement(SOAQueryConstants.REGION_RECOVERY_DELETE);
	            
	            ps.setString(1,connectivityKey);
	            if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
					Debug.log(Debug.MSG_STATUS, "Executing delete query ["
							+ SOAQueryConstants.REGION_RECOVERY_DELETE
							+ "] for Connectivity : [" + connectivityKey + "]");
				}
	            ps.execute();
	            
	            conn.commit();	
	         }
	         catch(Exception DBEx)
	         {
	        	 Debug.log(Debug.SYSTEM_CONFIG,DBEx.toString());
	         }
	         finally
	         {
	        	 try
	        	 {
		        	 if (ps != null)
		        		 ps.close();
		        	 
					 if( conn!= null)
						DBInterface.releaseConnection( conn );
	        	 }
	        	 catch(Exception clEx)
	        	 {
	        		 Debug.log(Debug.SYSTEM_CONFIG,clEx.toString());
	        	 }
	         }
    	  
	       while(spid != null){
	    	 
	    	 Debug.log(Debug.MSG_STATUS, "SPID for configuration: [" + spid + "]");
	
	         SecondarySPID secondarySPID = new SecondarySPID( spid );
	
	         String regionProperty = null;	              	         
	         
	         // check to see which regions are allowed for this SPID
	         for(int region = 0; region < NPACConstants.REGION_COUNT; region++){
	
	            regionProperty = PersistentProperty.getPropNameIteration(
	                                                            REGION_PROP_PREFIX+
	                                                            region,
	                                                            spidIndex);
	            boolean supported = getBooleanPropertyValue( regionProperty );
	            if(spidIndex == 0)
            	{
		            try
		            {
			            conn = DBInterface.acquireConnection();
		
			            ps=conn.prepareStatement(SOAQueryConstants.REGION_RECOVERY_INSERT);
		
			            ps.setInt(1,region);
		
			            ps.setString(2,String.valueOf(supported));
		
			            ps.setString(3,SOAQueryConstants.NPACAWAITING_STATUS);
			            
			            ps.setString(4,connectivityKey);
						
			            if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
								Debug
										.log(
												Debug.MSG_STATUS,
												"Executing Insert query ["
														+ SOAQueryConstants.REGION_RECOVERY_INSERT
														+ "] for SPID : ["
														+ spid
														+ "]"
														+ " and region ["
														+ region
														+ "] and connectivity :["
														+ connectivityKey + "]");
							}
						// Insert into SOA_REGION_RECOVERY table with all SPID's and regions.
			            ps.execute();
		
			            conn.commit();
			            
		            }
		            catch(Exception ex)
		            {
						try
						{
		
							conn.rollback();
						}
						catch(Exception e)
						{
							Debug.log(Debug.SYSTEM_CONFIG,e.toString());
						}
		
		            	Debug.log(Debug.SYSTEM_CONFIG,ex.toString());
		            }
		            finally
		            {
		                try
		                {
		                	if (ps !=null)
		                	ps.close();
		                	
							if( conn!= null)
							DBInterface.releaseConnection( conn );
		                }
		                catch(Exception dbEx)
		                {
		                	Debug.log(Debug.SYSTEM_CONFIG,dbEx.toString());
		                } 
		            }
            	}else if(spidIndex > 0 && supported){
            		 try
 		             {
 			            conn = DBInterface.acquireConnection();
 		
 			            ps=conn.prepareStatement(SOAQueryConstants.REGION_RECOVERY_UPDATE_FULL);
 		
 			            ps.setString(1,String.valueOf(supported));
 		
 			            ps.setInt(2,region);
 			            
 			            ps.setString(3,connectivityKey);
 						
 						// Insert into SOA_REGION_RECOVERY table with all SPID's and regions.
							if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
								Debug
										.log(
												Debug.MSG_STATUS,
												"Executing update query ["
														+ SOAQueryConstants.REGION_RECOVERY_UPDATE_FULL
														+ "]for SPID : ["
														+ spid
														+ "]"
														+ " and region ["
														+ region
														+ "] and connectivity :["
														+ connectivityKey + "]");
							}
 			            ps.execute();
 		
 			            conn.commit();
 			            
 		            }
 		            catch(Exception ex)
 		            {
 						try
 						{
 		
 							conn.rollback();
 						}
 						catch(Exception e)
 						{
 							Debug.log(Debug.SYSTEM_CONFIG,e.toString());
 						}
 		
 		            	Debug.log(Debug.SYSTEM_CONFIG,ex.toString());
 		            }
 		            finally
 		            {
 		                try
 		                {
 		                	if (ps !=null)
 		                	ps.close();
 		                	
 							if( conn!= null)
 							DBInterface.releaseConnection( conn );
 		                }
 		                catch(Exception dbEx)
 		                {
 		                	Debug.log(Debug.SYSTEM_CONFIG,dbEx.toString());
 		                } 
 		            }
            	}
				secondarySPID.setRegionSupported(region, supported);
	         }
	
	         // get the primary SPID for this seconday SPID
	         String primarySPIDName =
	            PersistentProperty.getPropNameIteration(PRIMARY_SPID_PROP,
	                                                    spidIndex);
	         String primarySPID = getPropertyValue( primarySPIDName );
	         
	
	         // if the primary SPID is not defined, then use the default
	         // primary SPID
	         if( primarySPID == null ){
	        	 
	
	            primarySPID = getPropertyValue( DEFAULT_PRIMARY_SPID_PROP );
	
	            if(primarySPID == null){
	
	               // there was no primary SPID set for this secondary SPID,
	               // and there is no default primary SPID set for
	               // this comm server
	               throw new ProcessingException("["+primarySPIDName+
	                                             "] is not set for ["+
	                                             propName+
	                                             "], and no ["+
	                                             DEFAULT_PRIMARY_SPID_PROP+
	                                             "] could be found.");
	
	            }
	
	         }
	
	         // add the secondary SPID to the primary SPID
	
	         adapter.add(primarySPID, secondarySPID);
	         // check for the next secondary SPID
	         spidIndex++;
	         propName = PersistentProperty.getPropNameIteration(SECONDARY_SPID_PROP,
	                                                            spidIndex);
	         spid = getPropertyValue(propName);
	
	      }
      }

   }

   /**
   * This reads the keep alive sleep interval from properties and
   * starts the thread that will periodically tell the NPAC adapter to
   * send client keep-alive messages for any active sessions.
   */
   private void startKeepAliveThread() throws ProcessingException{

      StringBuffer errors = new StringBuffer();

      // get the keep alive sleep interval
      String keepAliveInterval =
         getRequiredPropertyValue(KEEP_ALIVE_INTERVAL_PROP,
                                  errors);

      if( errors.length() > 0 ){
         throw new ProcessingException( errors.toString() );
      }

      int interval;

      try{

         interval = Integer.parseInt(keepAliveInterval);
         // convert seconds to milliseconds
         interval *= 1000;

      }
      catch(NumberFormatException nfex){

         throw new ProcessingException("The value of the ["+
                                       KEEP_ALIVE_INTERVAL_PROP+
                                       "] property is not a valid integer value: "+
                                       nfex.getMessage() );

      }

      // kick off the keep-alive thread
      keepAliveThread = new KeepAliveThread(interval);
      keepAliveThread.start();

   }


   /**
   * This publishes the receive interface for this com server.
   * This method defines the Runnable interface from ComServer.
   */
   public void run(){

      try{    	      	 

         // kick off keep alive thread
         startKeepAliveThread();
    	  
         // send the NewSession request(s)
         adapter.initialize();
         
         

      }
      catch(ProcessingException pex){

         Debug.error( pex.getMessage() );

      }

   }

   /**
   * This method defines the remote interface by which this server
   * receives incoming notifications. The only client that should
   * be calling this method is the SOAP service which is receiving
   * response notifications from the NPAC Gateway and forwarding them
   * to this method.
   *
   * @param notification the XML notification forwarded to this
   *                     method by the SOAP service via RMI.
   * @returns 0 for ACK and -1 for NACK.
   */
   public int process(String notification){
	   

      if( Debug.isLevelEnabled(Debug.IO_STATUS) ){

         Debug.log( Debug.IO_STATUS,
                    "Received notification:\n"+
                    notification );

      }

      // This should only possibly happen when the gateway is shutting down,
      // since the RMI receive interface should
      // not be receiving any requests until after the adapter is initialized.
      if( adapter == null ){

         // log error

         return NPACConstants.NACK_RESPONSE;

      }

      XMLMessageParser parsedNotification;
      String notificationType;

      try{

         parsedNotification = new XMLMessageParser(notification);
         
         // get the notification type
         try{
            notificationType =
               parsedNotification.getNode(NPACConstants.NOTIFICATION_TYPE_NODE).getNodeName();
         }
         catch(MessageException mex){

            Debug.error("Could not retrieve notification type: "+
                        mex.toString());

            return NPACConstants.NACK_RESPONSE;

         }

      }
      catch(MessageException mex){

         Debug.error("Could not parse notification:\n"+notification+
                     "\n"+mex.toString());

         return NPACConstants.NACK_RESPONSE;

      }

      // get the session ID and invoke ID
      String sessionID;
      String invokeID;
       try{
         sessionID = parsedNotification.getTextValue(NPACConstants.SESSION_ID);
         invokeID = parsedNotification.getTextValue(NPACConstants.INVOKE_ID);
       
      }
      catch(MessageException mex){

         Debug.error("Could not get session ID or invoke ID: "+mex.toString());

         return NPACConstants.NACK_RESPONSE;

      }

      // the customer ID field is not required
      String customerID = getCustomerID(parsedNotification);

      if( customerID == null && Debug.isLevelEnabled(Debug.ALL_WARNINGS) ){

         Debug.warning("The optional customer ID field is missing for "+
                       "notification with invoke ID ["+invokeID+"]");

      }

      if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){

         Debug.log(Debug.MSG_STATUS,
                   "Received notification:"+
                   "\n\tType        ["+notificationType+
                   "]\n\tInvoke ID   ["+invokeID+
                   "]\n\tSession ID  ["+sessionID+
                   "]\n\tCustomer ID ["+customerID+"]\n");

      }

      int ack = NPACConstants.NACK_RESPONSE;

      // check for "bookkeeping" responses that are handled
      // by the adapter
      if( notificationType.equals( NPACConstants.NEW_SESSION_REPLY_TYPE ) ||
          notificationType.equals( NPACConstants.ASSOCIATION_REPLY_TYPE ) ||
          notificationType.equals( NPACConstants.RECOVERY_COMPLETE_TYPE ) ||
          notificationType.equals( NPACConstants.RECOVERY_COMPLETE_WITH_ERROR_CODE_TYPE ) ||
          notificationType.equals( NPACConstants.RECOVERY_REPLY_TYPE )    ||
          notificationType.equals( NPACConstants.DOWNLOAD_RECOVERY_REPLY_TYPE) ||
          notificationType.equals( NPACConstants.SWIM_PROCESSING_RECOVERY_RESULT_REPLY))
        {
          ack = adapter.handleNotification(invokeID, parsedNotification);

        }
      else if( notificationType.equals(NPACConstants.GATEWAY_KEEP_ALIVE_TYPE) ){

         ack = adapter.receiveKeepAlive(sessionID, parsedNotification);

      }
      else if( notificationType.equals(
                  NPACConstants.ASSOCIATION_STATUS_NOTIFICATION_TYPE ) ){

         ack = adapter.receiveAssociationStatusNotification(invokeID,
                                                            sessionID,
                                                            customerID,
                                                            parsedNotification);

      }
      else if( notificationType.equals(NPACConstants.GATEWAY_RELEASE_TYPE) ){

         // the NPAC gateway wants us to release a particular session
         ack = adapter.receiveReleaseSession(sessionID);

      }
      else if( notificationType.equals(NPACConstants.GATEWAY_ERROR_TYPE) ){
    	  
    	  
         // have the adapter handle the error
         ack = adapter.handleError(invokeID, parsedNotification);

      }
      else {

         boolean networkNotification = isNetworkNotification(notificationType);

         // if the customer ID is null, then there is no way to know who
         // this message is for
         if (customerID != null) {

            try {

               // First log the time that the notification was received.
               // This assumes that the driver chain, even if it fails,
               // will log the incoming notification XML somewhere,
               // and will be able to retry the notification at a later time.
               lastNotificationTime.setLastNotificationTime(
                  parsedNotification,
                  networkNotification);

            }
            catch (Exception ex) {

               Debug.error("Could not log last notification time for " +
                           "notification with invoke ID [" + invokeID +
                           "]: " +
                           ex.toString() +
                           ". Notification will be rejected.");

            }

         }

         // We were expecting a reply message with this invoke ID.
         // The message will be processed by the handler object
         // waiting for that invoke ID
         if( adapter.notificationHandlerExists(invokeID) ){
            adapter.handleNotification(invokeID, parsedNotification);
         }
         else{

            // pass all other notification types to the driver chain
            // for processing
            ack = processNotification(invokeID,
                                      sessionID,
                                      customerID,
                                      notificationType,
                                      notification,
                                      networkNotification);

         }

      }

      if( Debug.isLevelEnabled(Debug.IO_STATUS) ){

         String ackString = (ack == NPACConstants.ACK_RESPONSE) ?
                             "an ACK" : "a NACK";

         Debug.log(Debug.IO_STATUS,
                   "Returning "+ackString+
                   " for notification with invoke ID ["+invokeID+"]");

      }

      return ack;

   }

   /**
   * This passes a notification off to the receive driver chain for
   * processing. If the notification type requires, then this will also
   * send an asynchronous NotificationReply back to the NPAC OSS Gateway.
   */
   private int processNotification(String invokeID,
                                   String sessionID,
                                   String customerID,
                                   String notificationType,
                                   String notification,
                                   boolean networkNotification){
	   
      int ack = NPACConstants.NACK_RESPONSE;

      String errorCode = null;
      String errorInfo = null;

      try{

         // pass notification to driver chain
         super.process("", notification);

         ack = NPACConstants.ACK_RESPONSE;

      }
      catch(Exception ex){

         Debug.error("Errors occurred while processing the "+
                     "notification with invoke ID ["+invokeID+"]: "+
                     ex.toString());

         errorCode = NPACConstants.PROCESSING_FAILURE_ERROR_CODE;
         errorInfo = ex.getMessage();

      }

      // check to see if this message type is a notification and requires
      // an asynchronous reply to be sent
      if( networkNotification ){

         // DownloadReplies are sent in reponse to "network" notifications
         if (ack == NPACConstants.ACK_RESPONSE) {
            adapter.sendSuccessDownloadReply(invokeID,
                                             sessionID,
                                             customerID);
         }
         else {
            adapter.sendFailureDownloadReply(invokeID,
                                             sessionID,
                                             customerID,
                                             errorCode,
                                             errorInfo);
         }

      }
      else if( notificationType.endsWith(NPACConstants.NOTIFICATION_SUFFIX) ){

         // Send an async NotificationReply for any notifications
         if (ack == NPACConstants.ACK_RESPONSE) {
            adapter.sendSuccessNotificationReply(invokeID,
                                                 sessionID,
                                                 customerID);
         }
         else {
            adapter.sendFailureNotificationReply(invokeID,
                                                 sessionID,
                                                 customerID,
                                                 errorCode,
                                                 errorInfo);
         }

      }

      return ack;

   }

   public void processMessage(String message){
      try{
         super.process("", message);
      }
      catch(Exception ex){

         Debug.error("Could not process response message:\n"+
                     message);
         Debug.logStackTrace(ex);

      }
   }

   /**
    * This checks the given notification type to determine if it is a
    * network notification. These types of notifications require an
    * automated DownloadReply.
    *
    * @param notification String notification the notification type.
    * @return boolean
    */
   private static boolean isNetworkNotification(String notification){
	   

      return notification.endsWith(NPACConstants.NPA_NXX_CREATE_TYPE) ||
             notification.endsWith(NPACConstants.NPA_NXX_DELETE_TYPE) ||
             notification.endsWith(NPACConstants.LRN_CREATE_TYPE) ||
             notification.endsWith(NPACConstants.LRN_DELETE_TYPE) ||
             notification.endsWith(NPACConstants.SP_CREATE_TYPE) ||
             notification.endsWith(NPACConstants.SP_MODIFY_TYPE) ||
             notification.endsWith(NPACConstants.SP_DELETE_TYPE) ||
             notification.endsWith(NPACConstants.NPA_NXX_X_CREATE_TYPE) ||
             notification.endsWith(NPACConstants.NPA_NXX_X_MODIFY_TYPE) ||
             notification.endsWith(NPACConstants.NPA_NXX_X_DELETE_TYPE);

   }

   /**
   * This is a utility method for extracting the customer ID (a SPID)
   * from the given nitification.
   */
   private static String getCustomerID(XMLMessageParser parsedNotification){

      String customerID = null;

      try{

         if( parsedNotification.exists(NPACConstants.CUSTOMER_ID) ){

            customerID =
               parsedNotification.getTextValue(NPACConstants.CUSTOMER_ID);

         }

      }
      catch(MessageException mex){

         Debug.warning("Could not get customer ID from notification: "+
                       mex.getMessage());

      }

      return customerID;

   }

   /**
   * Shuts down this comm server and cleans up.
   */
   public void shutdown(){

      Debug.log(Debug.NORMAL_STATUS,
                "Shutting down NPAC Com Server" );

      // kill the worker threads
      queue.kill();
      smartSocketsThreads.kill();

      // shut down SmartSocket receivers
      for(int i = 0; i < receivers.length; i++){

         receivers[i].cleanup();

      }

      if( keepAliveThread != null ){
         keepAliveThread.kill();
      }

      synchronized(NPACComServer.class){

         if(adapter != null){

            Debug.log(Debug.NORMAL_STATUS, "Shutting down NPAC Adapter" );

            adapter.shutdown();
            adapter = null;

         }

      }

   }

   /**
   * This method allows the SessionLookup message processor to access
   * the NPAC Adapter instance in order to lookup the session ID
   * for a particular customer.
   */
   public static NPACAdapter getAdapter(){

      return adapter;

   }

   /**
    * This is called when this Com Server is initialized in order to reset
    * all of the 'sent' messages in the NPAC Message Queue to
    * 'retry' status. This handles the case where the Com Server was brought
    * down before receiving a response for a sent message.
    */
   public void resetSentMessages(String pickTime){

      NPACQueueUtils.retryAllSentMessages(pickTime);
      Debug.log(Debug.SYSTEM_CONFIG,"After executing resent sent Messages");

   }

   /**
    * This gets the value of a property and converts it from seconds to
    * milliseconds. If the property value cannout be found the given
    * default value will be used.
    *
    * @param propertyName String
    * @param defaultValue long
    * @return long
    */
   private long getPropertyAsMs(String propertyName,
                                long defaultValue,
                                StringBuffer errorBuffer){

      long result = defaultValue;
      String propertyValue = null;

      try{

         propertyValue = getPropertyValue(propertyName);

         if(propertyValue != null){

            result = Long.parseLong( propertyValue );
            // convert seconds to ms
            result *= 1000;

         }

      }
      catch(NumberFormatException nfex){

         errorBuffer.append( "The value ["+propertyValue+
                             "] for property ["+propertyName+
                             "] is not a valid integer value: "+
                             nfex.toString() );

      }

      return result;

   }

   /**
    * This gets the value of a property and converts that numeric value
    * from minutes to milliseconds.
    *
    * @param propertyName String the name of the property whose value should
    *                            be retrieved.
    * @param defaultValue long the default value to be returned if the property
    *                          is not present.
    * @param errorBuffer String the buffer to which any errors will be appended.
    *
    * @return long the value of the property in millisends.
    */
   private long getMinutesPropertyAsMs(String propertyName,
                                       long defaultValue,
                                       StringBuffer errorBuffer){

      long result = defaultValue;

      String propertyValue = getPropertyValue(propertyName);

      try {

         if (propertyValue != null) {

            result = Long.parseLong(propertyValue);

            // convert minutes to millisesconds
            result *= 60000;

         }

      }
      catch (NumberFormatException nfex) {

         errorBuffer.append("The value [" + propertyValue +
                            "] for property [" + propertyName +
                            "] is not a valid integer value: " +
                            nfex.toString());

      }

      return result;

   }

   /**
   * This thread periodically tells the NPAC adapter to send client keep-alive
   * messages for any active sessions.
   */
   private class KeepAliveThread extends Thread{

      /**
      * The sleep interval in ms.
      */
      private int sleep;

      /**
      * This flag is used to keep the run() method looping. When set to
      * false (by the kill() method), the run() method will exit.
      */
      private boolean alive = true;

      /**
      *
      * @param the keep alive sleep interval in milliseconds.
      */
      public KeepAliveThread(int sleepIntervalInMS){

         sleep = sleepIntervalInMS;

      }

      /**
      * This will cause the run method to exit.
      */
      public void kill(){

         Debug.log(Debug.NORMAL_STATUS, "Killing keep alive thread.");

         alive = false;

         // disturb the thread if it is sleeping
         try{
            interrupt();
         }
         catch(Exception ex){

            Debug.warning("Could not interrupt the keep alive thread.");

         }

      }

      /**
      * This method loops, sleeping for the configured interval, and
      * then wakes up to tell the adapter to send any necessary client keep
      * alive messages.
      */
      public void run(){

         Debug.log(Debug.NORMAL_STATUS,
                   "Keep alive thread started. Sleep interval is ["+
                   sleep+"] ms.");

         while( alive ){

            try{
            	

               Thread.sleep(sleep);

               Debug.log(Debug.NORMAL_STATUS,
                         "Keep alive thread just woke up.");

               if( alive && adapter != null ){

                  adapter.sendKeepAlive();
               }
               

            }
            catch(InterruptedException ex){

               Debug.warning("Keep alive thread was interrupted.");

            }

         }

         Debug.log(Debug.NORMAL_STATUS, "Keep alive thread exiting.");

      }

   }

}
