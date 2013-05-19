package com.nightfire.comms.jms.failover;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;

import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.util.Debug;

/**
 * Class to acquire key from consumer_lease_conf table on start-up. It shall
 * attempt to acquire key for configured number of times with an interval
 * between two consecutive attempts.
 * 
 * @author hpirosha
 * 
 */
public class AcquireKeyOnStartup {

	private String keyToAcquire = null;
	private String acquireeNm = null;
	public final static int DEFAULT_MAX_ATTEMPTS = 5;  
	private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
	
	/* retry interval in minutes */
	public final static int RETRY_INTERVAL = 60 * 1000 * 3;
	private long retryInterval = RETRY_INTERVAL;

	/**
	 * This constructor shall initialize it to default, maximum of five
	 * attempts. Interval between retries shall be 3 minutes.
	 * 
	 * @param key
	 *            the key to acquire
	 * @param acquireeNm
	 *            name of the acquiree, this shall be used to update
	 *            consumer_lease_conf table
	 */
	public AcquireKeyOnStartup(String key,String acquireeNm) {
		this.keyToAcquire = key;
		this.acquireeNm = acquireeNm;
	}

	/**
	 * 
	 * @param key
	 * @param acquireeNm
	 * @param maxAttempts
	 * @param retryInterval in minutes
	 */
	public AcquireKeyOnStartup(String key,String acquireeNm,int maxAttempts, int retryInterval) {
		this(key,acquireeNm);
		this.maxAttempts = maxAttempts;
		this.retryInterval = retryInterval * 1000 * 60;
	}

	/**
	 * Attempts to acquire the key.
	 * If it fails then throws an exception 
	 */
	public void acquire() throws KeyAcquisitionFailed {
		boolean executeOnce = false;
		int attempts = 0;

		while (true) {

			attempts++;

			if (acquireKey(keyToAcquire))
				break;
			else {
				if (!executeOnce) {
					try {
						updateOwnerHasRestartedColumn();
						executeOnce = true;
					
						Date date = getLeaseExpiration();
						if (date != null) {
							if (date.after(new Date())) {

								/*
								 * calculate the sleep time, time remaining for
								 * the current lease in database to expire
								 */
								retryInterval = date.getTime()
										- System.currentTimeMillis();
							} else {
								retryInterval = 10; /* 10 ms */
							}
						}
					} catch (Exception e) {
						Debug.warning("Could not update owner column for key :"
								+ keyToAcquire + "reason :" + e.getMessage());
					}

				}

				if (attempts == maxAttempts) {
					throw new KeyAcquisitionFailed(
							" Could not acquire key after maximum attempts : "
									+ maxAttempts);
				}
			}

			try {
				Debug.log(Debug.NORMAL_STATUS,
						"Attempt to acquire key failed.  key [" + keyToAcquire
								+ "] would retry in (ms) ->" + retryInterval);

				Thread.currentThread().sleep(retryInterval);
			} catch (InterruptedException ie) {
				/* if shutdown requested then exit ! */
				if(forceShutdown)
					return ;
			}

		}
	}

	/**
	 * Internal method
	 * 
	 * @param key
	 * @return boolean <code>true</code> if it successfully acquires else
	 *         <code>false</code> if it fails or an exception occurs.
	 */
	private boolean acquireKey(String key)  {

		Connection conn = null;
		PreparedStatement pstm = null;

		try {
			conn = DBConnectionPool.getInstance().acquireConnection();
			pstm = conn.prepareStatement(LeaseConstants.ACQUIRE_KEY_SQL);
			pstm.setString(1, acquireeNm);
			pstm.setTimestamp(2, new java.sql.Timestamp(System
					.currentTimeMillis()));
			pstm.setTimestamp(3, new java.sql.Timestamp(System
					.currentTimeMillis()
					+ (5 * 60 * 1000)));
			pstm.setString(4, null);
			pstm.setString(5, key);
			pstm.setString(6, acquireeNm);
			pstm.setTimestamp(7, new java.sql.Timestamp(System
					.currentTimeMillis()));
			pstm.setString(8, acquireeNm);

            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            	Debug.log(Debug.NORMAL_STATUS, "AcquireKeyOnStartup: Executing SQL: \n"+LeaseConstants.ACQUIRE_KEY_SQL );

			int count = pstm.executeUpdate();
			if (count == 1) {

				if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
					Debug.log(Debug.NORMAL_STATUS,
							"AcquireKeyOnStartup: Finished Executing SQL... Acquired lease at startup for key -> " + key
									+ "acquireeNm -> " + acquireeNm);
				return true;
			}
			return false;
		}
		catch(Exception e)
		{
			Debug.warning("An exception occured while acquiringKey #"+e.getMessage());
			return false;
		}
		finally {
			closeDBResources(null, pstm,conn);
		}
	}


	/**
	 * Update 
	 * @return boolean <code>true</code> if consumer_has_resumed column is
	 *         successfully updated to "YES" value, else <code>false</code>
	 * @throws Exception
	 */
	private boolean updateOwnerHasRestartedColumn() throws Exception {

		Connection conn = null;
		PreparedStatement pstm = null;

		try {
			conn = DBConnectionPool.getInstance().acquireConnection();
			pstm = conn.prepareStatement(LeaseConstants.UPDATE_OWNER_SQL);
			pstm.setString(1, "YES");
			pstm.setString(2, keyToAcquire);
			pstm.setString(3, acquireeNm);

            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            	Debug.log(Debug.NORMAL_STATUS, "AcquiredKeyOnStartup: Executing SQL: \n"+LeaseConstants.UPDATE_OWNER_SQL);

			int count = pstm.executeUpdate();
			
            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            	Debug.log(Debug.NORMAL_STATUS, "AcquiredKeyOnStartup: Finished Executing SQL...");


			if (count == 1) {
				return true;
				
			}
			return false;
		} finally {
			closeDBResources(null, pstm,conn);
		}
	}

	private Date getLeaseExpiration() {
		Connection conn = null;
		PreparedStatement pstm = null;
		ResultSet rs = null;

		try {
			conn = DBConnectionPool.getInstance().acquireConnection();
			pstm = conn.prepareStatement(LeaseConstants.SELECT_LEASE_SQL);
			pstm.setString(1, keyToAcquire);
			pstm.setString(2, acquireeNm);

			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            	Debug.log(Debug.NORMAL_STATUS, "AcquiredKeyOnStartup: Executing SQL: \n"+LeaseConstants.SELECT_LEASE_SQL);

			rs = pstm.executeQuery();
			
            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            	Debug.log(Debug.NORMAL_STATUS, "AcquiredKeyOnStartup: Finished Executing SQL...");

			if (rs.next()) {
				java.sql.Timestamp expTs = rs.getTimestamp(1);
				if(Debug.isLevelEnabled(Debug.DB_DATA))
					Debug.log(Debug.DB_DATA,"lease_expires_at for key["+keyToAcquire+"]->"+expTs);
				
				if(expTs!=null)
					return new Date(expTs.getTime());
			}
			return null;
		}
		catch(Exception exp) {
			Debug.warning("Could not get lease_expiration_ts for key["+keyToAcquire+"] reason -> ["+exp.getMessage());
			return null;
		}
		finally {
			closeDBResources(rs, pstm,conn);
		}

	}
	
	/**
	 * Close database related resources
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

	private boolean forceShutdown = false;
	public void forceShutdown() {
		forceShutdown = true;
	}
}
