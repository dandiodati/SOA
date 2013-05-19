package com.nightfire.comms.jms.failover;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.Properties;

import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.jms.JMSConsumerCallBack;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;

/**
 * 
 * This thread tracks lease of the key owned and alternate key. It generates
 * KeyLeaseEvents to notify a consumer.
 * 
 * @author hpirosha
 * 
 */
public class KeyLeaseTracker extends Thread {

	private KeyLeaseEventListener listener = null;
	private String myKey = null;
	private String altKey = null;
	private String myName = null;
	private String altConsumerName = null;

	private long extendLeaseByInterval = 0;
	private int updateLeaseEveryInterval = 0;
	private boolean hasAlternateKey = false;
	/**
	 * 
	 * @param myKey
	 * @param myName name of the consumer for whom i am tracking
	 * @param listener
	 * @param altConsumerName name of the alternate consumer whose key i am watching on
	 * @param altKey
	 * @param updateLeaseEveryMinutes periodic interval at which the lease is updated, specified in minutes  
	 * @param extendLeaseByMinutes the lease is extended by these number of minutes
	 */
	public KeyLeaseTracker(String myKey,String myName,
			KeyLeaseEventListener listener, String altConsumerName,String altKey,int updateLeaseEveryMinutes, int extendLeaseByMinutes) {
		this.myKey = myKey;
		this.myName = myName;
		this.listener = listener;
		this.altKey = altKey;
		this.altConsumerName = altConsumerName;
		this.updateLeaseEveryInterval = updateLeaseEveryMinutes * 60 * 1000;
		this.extendLeaseByInterval = extendLeaseByMinutes * 60 * 1000;
		setDaemon(true);
	}

	@Override
	/**
	 * 
	 */
	public void run() {

		waitBeforeUpdatingLease();

		while (true) {

			if (listener != null && listener.isAlive()) {

                if(forceShutdown)
					return;

				if (!updateLease(myKey,myName))  // failure
				{
					// be optimistic and continue
				}

				if (hasAlternateKey()) {

					if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
						Debug.log(Debug.NORMAL_STATUS,"Consumer ["+myName+"] -> has alternate key ["+altKey+"]");
					
					try {
						if (hasOwnerRestarted(altKey,altConsumerName)) {
							
							if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
								Debug.log(Debug.NORMAL_STATUS,"Consumer ["+myName+"] detected -> Alternate Consumer has started ");

							
							KeyLeaseEvent event = KeyLeaseEvent
									.getReleaseAltKeyEvent();

							/* close alternate consumer */
							listener.handleEvent(event);

							hasAlternateKey = false;
						} else
						{
							updateLease(altKey,altConsumerName);
							
							if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
								Debug.log(Debug.NORMAL_STATUS,"Consumer ["+myName+"] -> updated lease for alternate key ["+altKey+"]");
						}
					} catch (Exception e) {

						Debug.warning("Could not check alternate owner "+e.getMessage());
						/* what to do now !! */
					}
				} else {
					if (canAcquireKey(altKey,altConsumerName)) {
						
						if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
							Debug.log(Debug.NORMAL_STATUS, "Consumer ["
									+ myName + "] -> lease for alternate key ["
									+ altKey
									+ "] has expired and it is available ");

						if (updateKeyOwner(altKey,myName)) {


							KeyLeaseEvent event = KeyLeaseEvent
									.getLeaseForAltKeyAvailable();

							try {
								/* start alternate consumer */
								listener.handleEvent(event);

								hasAlternateKey = true;
								
								if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
									Debug
											.log(Debug.NORMAL_STATUS, "Consumer ["
													+ myName
													+ "] ->  alternate key ["
													+ altKey
													+ "] has been acquired by me ");

							} catch (FrameworkException fe) {
								Debug.warning("Consumer ["+myName+"] could not start alternate consumer:"+fe.getMessage());
								/* it shall be tried again */
							}

						}
						/* if update fails then retry after sleep */
					}
				}

				if(forceShutdown)
					return; /* exit */
				
				waitBeforeUpdatingLease();
			} else {
				// warn and exit 
				Debug.warning("Consumer["+myName+"] has shutdown, terminating it's lease tracking thread ! ");
				return;
			}
		}
	}

	/**
	 * Sleeps for updateLeaseEveryInterval milli seconds
	 */
	private void waitBeforeUpdatingLease() {
		try {
			sleep(updateLeaseEveryInterval);
		} catch (InterruptedException e) {
			Debug.warning("Thread interrupted from sleep.");
		}
	}

	private boolean forceShutdown = false;
	/**
	 * 
	 */
	public void shutdown() {
		forceShutdown = true;
	}

	// -------------------------------------------------------------------------------------------------------------------
	// table : consumer_lease_conf
	// ------------------------------------------------------------------------------------------------------------------
	// key | consumer | acquired_by | acquired_timestamp | lease_expires_at |
	// consumer_has_restarted | alternate_consumer
	// -------------------------------------------------------------------------------------------------------------------

	/**
	 * 
	 * @param key
	 * @throws Exception
	 */
	private boolean updateLease(String key, String name) {

		Connection conn = null;
		PreparedStatement pstm = null;

		try {
			conn = DBConnectionPool.getInstance().acquireConnection();
			pstm = conn.prepareStatement(LeaseConstants.UPDATE_LEASE_SQL);
			java.sql.Timestamp expTime = new java.sql.Timestamp(System
					.currentTimeMillis()
					+ extendLeaseByInterval);
			pstm.setTimestamp(1, expTime);
			pstm.setString(2, key);
			pstm.setString(3, name);

			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            	Debug.log(Debug.NORMAL_STATUS, "KeyLeaseTracker: Executing SQL: \n"+LeaseConstants.UPDATE_LEASE_SQL);

			int result = pstm.executeUpdate();
			if (result == 1) {
				if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
					Debug.log(Debug.NORMAL_STATUS,
							"KeyLeaseTracker: Finished Executing SQL.. Successfully updated lease for key -> " + key
									+ " by -> " + myName);
				return true;
			}

			return false;
		} catch (Exception exp) {
			// log exception
			return false;
		} finally {
			closeDBResources(null, pstm,conn);
		}
	}

	/**
	 * Method to check for owner of a key has restarted.
	 * @param otherKey
	 * @return
	 * @throws Exception
	 */
	private boolean hasOwnerRestarted(String otherKey,String name) throws Exception {

		Connection conn = null;
		PreparedStatement pstm = null;
		ResultSet rs = null;

		try {
			conn = DBConnectionPool.getInstance().acquireConnection();
			pstm = conn.prepareStatement(LeaseConstants.OWNER_UP_SQL);
			pstm.setString(1, otherKey);
			pstm.setString(2, name);

			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            	Debug.log(Debug.NORMAL_STATUS, "KeyLeaseTracker: Executing SQL: \n"+LeaseConstants.OWNER_UP_SQL);

			rs = pstm.executeQuery();

			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            	Debug.log(Debug.NORMAL_STATUS, "KeyLeaseTracker: Finished Executing SQL...");


			if (rs.next()) {
				String str = rs.getString(1);
				return "YES".equals(str);
			}
			return false;
		}
		finally {
			closeDBResources(rs, pstm,conn);
		}
	}

	/**
	 * 
	 * @param otherKey
	 * @return
	 * @throws Exception
	 */
	private boolean canAcquireKey(String otherKey,String name)  {

		Connection conn = null;
		PreparedStatement pstm = null;
		ResultSet rs = null;

		if(Debug.isLevelEnabled(Debug.MSG_BASE))
			Debug.log(Debug.MSG_BASE,"Consumer["+myName+"] checking for alternate key's lease_expiration");
		
		try {
			conn = DBConnectionPool.getInstance().acquireConnection();
			pstm = conn.prepareStatement(LeaseConstants.SELECT_LEASE_SQL);
			pstm.setString(1, otherKey);
			pstm.setString(2, name);

			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            	Debug.log(Debug.NORMAL_STATUS, "KeyLeaseTracker: Executing SQL: \n"+LeaseConstants.SELECT_LEASE_SQL);

			rs = pstm.executeQuery();

			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            	Debug.log(Debug.NORMAL_STATUS, "KeyLeaseTracker: Finished Executing SQL...");

			if (rs.next()) {
				java.sql.Timestamp expTs = rs.getTimestamp(1);
				if(Debug.isLevelEnabled(Debug.DB_DATA))
					Debug.log(Debug.DB_DATA,"lease_expires_at for key["+otherKey+"]->"+expTs);
				
				if(expTs==null)
					return true;
				
				return (new Date()).after(expTs);
			}
			return false;
		}
		catch(Exception exp)
		{
			Debug.warning("Exception occured while acquiring key->" + otherKey
					+ ":" + exp.getMessage());
			return false;
		}
		finally {
			closeDBResources(rs, pstm,conn);
		}
	}

	/**
	 * 
	 * @param otherKey
	 * @throws Exception
	 */
	private boolean updateKeyOwner(String otherKey,String name)  {

		Connection conn = null;
		PreparedStatement pstm = null;

		try {
			conn = DBConnectionPool.getInstance().acquireConnection();
			pstm = conn.prepareStatement(LeaseConstants.UPDATE_KEY_OWNER_SQL);
			pstm.setString(1, myName);
			pstm.setTimestamp(2, new java.sql.Timestamp(System
					.currentTimeMillis()));
			pstm.setTimestamp(3, new java.sql.Timestamp(System
					.currentTimeMillis()
					+ extendLeaseByInterval));
			pstm.setString(4, otherKey);
			pstm.setString(5, name);
			pstm.setTimestamp(6, new java.sql.Timestamp(System
					.currentTimeMillis()));

			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            	Debug.log(Debug.NORMAL_STATUS, "KeyLeaseTracker: Executing SQL: \n"+LeaseConstants.UPDATE_KEY_OWNER_SQL);

			int result = pstm.executeUpdate();
			
			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            	Debug.log(Debug.NORMAL_STATUS, "KeyLeaseTracker: Finished Executing SQL...");

			if(result==1)
				return true;
			
			return false;
		}
		catch(Exception exp){
			Debug.warning("Failed to updated key owner :"+exp.getMessage());
			return false;
		}
		finally {
			closeDBResources(null, pstm,conn);
		}
	}

	/**
	 * 
	 * @return
	 */
	private boolean hasAlternateKey() {
		return hasAlternateKey;
	}

	/**
	 * Utility method to close database related resources
	 * 
	 * @param rs
	 *            ResultSet
	 * @param pstm
	 *            Statement
	 */
	private void closeDBResources(ResultSet rs, PreparedStatement pstm,Connection conn) {
		try {
			if (rs != null)
				rs.close();
		} catch (Exception ignore) {
		}

		try {
			if (pstm != null)
				pstm.close();
		} catch (Exception ignore) {
		}
		
		try {
			if (conn != null) {
				conn.commit();
				DBConnectionPool.getInstance().releaseConnection(conn);
			}
		} catch (Exception ignore) {
		}

	}
	
	public static void main(String[] args) throws Exception {

		System.setProperty("INSTALLROOT", "d:/GW");
		Properties props = new Properties();
		props.put(Debug.DEBUG_LOG_LEVELS_PROP, "ALL");
		props.put(Debug.LOG_FILE_NAME_PROP, "e:/test.log");
		props.put(Debug.MAX_DEBUG_WRITES_PROP, "10000");

		Debug.configureFromProperties(props);

		DBInterface.initialize("jdbc:oracle:thin:@impetus-786:1521:ORCL786",
				"suninstall", "suninstall");

		MultipleKeyConsumer consumer1 = new MultipleKeyConsumer("Instance-1",
				"Instance-2", "lsr_request_1","lsr_request_2", true, "LSR_REQUEST_Q",
				new JMSConsumerCallBack() {

					public void processMessage(String arg0, String arg1) {

						System.out
								.println("message was passed on to consumer1");
					}
				}, 3, 5, 3, 5,null,null);

		MultipleKeyConsumer consumer2 = new MultipleKeyConsumer("Instance-2",
				"Instance-1", "lsr_request_2", "lsr_request_1",true, "LSR_REQUEST_Q",
				new JMSConsumerCallBack() {

					public void processMessage(String arg0, String arg1) {

						System.out
								.println("message was passed on to consumer2");
					}
				}, 3, 5, 3, 5,null,null);

		consumer1.start();

		/* sleep for 5 min */
		Thread.sleep(5 * 60 * 1000);

		System.out.println("Starting consumer2");
		consumer2.start();

		/* sleep for 2 min */
		Thread.sleep(2 * 60 * 1000);

		System.out.println("Shutting down consumer1");
		/* stop consumer 1 */
		consumer1.shutdown();

		/* sleep for 5 min */
		Thread.sleep(5 * 60 * 1000);

		System.out.println("Starting consumer1");
		consumer1.start();

		/* sleep for 5 min */
		Thread.sleep(5 * 60 * 1000);
		System.out.println("Shutting down consumer2");
		consumer2.shutdown();

		/* again sleep for 5 min */
		Thread.sleep(5 * 60 * 1000);

	}
}
