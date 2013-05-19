package com.nightfire.comms.soap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import com.nightfire.framework.cache.CacheManager;
import com.nightfire.framework.cache.CachingObject;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;


public class GWSRequestControlDataCache implements CachingObject {
	
	private Map<String,DownTimeInfo>  downTimeInfo= null;
	
	private static final String SEP = "|";
	private static final String NULL_CONSTANT = "null";
	
	private static final String DOWN_TIME_SQL = 
			"SELECT gateway_driver_key,gateway_driver_type,downTime_starts_at," +
			"downTime_ends_at,downTime_message FROM GWS_REQUEST_CONTROL";
	
	private static final String SELECT_DOWN_TIME_SQL = 
			"SELECT gateway_driver_type,downTime_starts_at,downTime_ends_at," +
			"downTime_message FROM GWS_REQUEST_CONTROL WHERE gateway_driver_key =?";

	private static GWSRequestControlDataCache singleton = null;
	
	/**
	 * private constructor
	 */
	private GWSRequestControlDataCache() {
	}
	
	
	public static GWSRequestControlDataCache getInstance() throws FrameworkException {

		if (singleton != null)
			return singleton;

		synchronized (GWSRequestControlDataCache.class) {
			if (singleton != null)
				return singleton;

			singleton = new GWSRequestControlDataCache();
			singleton.initialise();
			CacheManager.getRegistrar().register(singleton);			
		}
		return singleton;
	}


	/**
	 * Initialise and register with CacheManager
	 * 
	 * @throws FrameworkException
	 */
	private void initialise() throws FrameworkException {

		downTimeInfo = new HashMap<String, DownTimeInfo>();
		Connection conn = null;
		PreparedStatement pstm = null;
		ResultSet rs = null;

		try {
			conn = DBConnectionPool.getInstance().acquireConnection();
			pstm = conn.prepareStatement(DOWN_TIME_SQL);
			rs = pstm.executeQuery();

			while (rs.next()) {
				DownTimeInfo out = new DownTimeInfo(rs.getString("gateway_driver_key"),
						 rs.getString("gateway_driver_type"), 
						 rs.getTimestamp("downTime_starts_at"), 
						 rs.getTimestamp("downTime_ends_at"),
						 rs.getString("downTime_message"));
				
				if(out.getDriverType() == null)
				    downTimeInfo.put(out.getDriverKey().trim()+SEP+out.getDriverType(), out);
				else
					downTimeInfo.put(out.getDriverKey().trim()+SEP+out.getDriverType().trim(), out);	
			}
			
			
			if (Debug.isLevelEnabled(Debug.DB_DATA))
				Debug.log(Debug.DB_DATA,
						"Initialized GWSRequestControlDataCache with DownTimeInfo -> "
								+ downTimeInfo );
			
		} catch (ResourceException re) {
			throw new FrameworkException(
					"Could not acquire a database connection "
							+ re.getMessage());
		} catch (SQLException sqle) {

			throw new FrameworkException("Could not execute sql "
					+ sqle.getMessage());
		} catch (Exception e) {
			throw new FrameworkException(
					"Exception is occured during the processing of data set "
							+ e.getMessage());
		}
		finally {
			if (conn != null) {
				try {
					DBConnectionPool.getInstance().releaseConnection(conn);
				} catch (Exception ignore) {
				}
			}
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ignore) {
				}
			}
			if (pstm != null) {
				try {
					pstm.close();
				} catch (Exception ignore) {
				}
			}
		}
	}
	
	/**
	 * reload cache
	 */
	public void flushCache() throws FrameworkException {
		if (singleton != null) {
			if (downTimeInfo != null) {
				downTimeInfo.clear();
				downTimeInfo = null;
			}
			singleton = null;
		}
	}

	/**
	 * Get down time data 
	 * @param gatewayKey
	 * 
	 * @return
	 */
	public DownTimeInfo getDownTimeInfo(String gatewayKey,String gatewayType,boolean fetchLatest)
			throws FrameworkException {
       try{
    	   String gatewayTypeLocal = null; 
    	   
    	   if(gatewayType !=null)
    	    gatewayTypeLocal = gatewayType.trim();    	   
    		   
		if (!fetchLatest) {
			
			DownTimeInfo downTimeLocal =  downTimeInfo.get(gatewayKey.trim()+SEP+gatewayTypeLocal);
			if(downTimeLocal == null)
			{
				downTimeLocal = downTimeInfo.get(gatewayKey.trim()+SEP+NULL_CONSTANT);
			}
			return downTimeLocal;
			
		}
		return getDownTimeFromDB(gatewayKey,gatewayTypeLocal);
       }
       catch(Exception e)
       {
    	   Debug.log(Debug.NORMAL_STATUS, "Returning null value since exception occured while fetching DownTimeInfo" );
    	   return null;
       }
	}

	/**
	 * Get DownTime from database
	 * @param gatewayKey
	 * @return
	 * @throws FrameworkException
	 */
	private DownTimeInfo getDownTimeFromDB(String gatewayKey,String gatewayType)
	throws FrameworkException {
		Connection conn = null;
		PreparedStatement pstm = null;
		ResultSet rs = null;
		HashMap<String, DownTimeInfo> downTimeMap = new HashMap<String, DownTimeInfo>();
		try {
			conn = DBConnectionPool.getInstance().acquireConnection();
			pstm = conn.prepareStatement(SELECT_DOWN_TIME_SQL);
			pstm.setString(1, gatewayKey);
			//pstm.setString(2,gatewayType );
			rs = pstm.executeQuery();

			if (rs.next()) {
				DownTimeInfo out = new DownTimeInfo(gatewayKey,rs.getString("gateway_driver_type"),rs
						.getTimestamp("downTime_starts_at"), rs
						.getTimestamp("downTime_ends_at"),rs
						.getString("downTime_message"));
				downTimeMap.put(gatewayKey+SEP+rs.getString("gateway_driver_type"), out);				
			}		
			DownTimeInfo downTimeLocal =  downTimeMap.get(gatewayKey+SEP+gatewayType);
			if(downTimeLocal == null)
			{
				downTimeLocal = downTimeInfo.get(gatewayKey+SEP+NULL_CONSTANT);
			}
			
			return downTimeLocal;
			
		} catch (ResourceException re) {
			throw new FrameworkException(
					"Could not acquire a database connection "
							+ re.getMessage());
		} catch (SQLException sqle) {

			throw new FrameworkException("Could not execute sql "
					+ sqle.getMessage());
		} finally {
			if (conn != null) {
				try {
					DBConnectionPool.getInstance().releaseConnection(conn);
				} catch (Exception ignore) {
				}
			}
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ignore) {
				}
			}
			if (pstm != null) {
				try {
					pstm.close();
				} catch (Exception ignore) {
				}
			}
		}

	}
	
	
	class DownTimeInfo {
		
		public String getDriverKey() {
			return gatewayDriverKey;
		}

		public String getDriverType() {
			return gatewayDriverType;
		}
		
		public Timestamp getDownTimeStartsAt() {
			return  downTimeStartsAt;
		}
		
		public Timestamp getDownTimeEndsAt(){
			return downTimeEndsAt;
		}
		
		public String getDownTimeMessage() {
			return downTimeMessage;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();			
			sb.append("\tgatewayDriverKey :"+gatewayDriverKey).
			append("\tgatewayDriverType:"+gatewayDriverType).
			append("\tdownTimeMessage :"+downTimeMessage).
			append("\tdownTimeStartsAt :"+downTimeStartsAt).
			append("\tdownTimeEndsAt :"+downTimeEndsAt);
			
			return sb.toString();
		}

		private String  gatewayDriverKey,gatewayDriverType,downTimeMessage;
		private Timestamp downTimeStartsAt, downTimeEndsAt;

		DownTimeInfo(String gatewayDriverKey,String gatewayDriverType, Timestamp downTimeStartsAt,
				Timestamp downTimeEndsAt ,String downTimeMessage) {			
			this.gatewayDriverKey = gatewayDriverKey;
			this.gatewayDriverType = gatewayDriverType;			
			this.downTimeStartsAt = downTimeStartsAt;
			this.downTimeEndsAt = downTimeEndsAt;			
			this.downTimeMessage = downTimeMessage;
		}
	
	}

}

