package com.nightfire.spi.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.nightfire.framework.cache.CacheManager;
import com.nightfire.framework.cache.CachingObject;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;

/**
 * This class provides API to access Outage and Quota
 * information from database or cache.
 * @author hpirosha
 * 
 */
public class ThrottlingDataCache implements CachingObject {

	private Map<String, Outage> outage = null;
	private Map<String, Quota> quotas = null;

	private static final String SEP = "|";

	private static final String OUTAGE_SQL = 
			"SELECT transaction,supplier,outage_start_datetime," +
			"outage_end_datetime FROM outage";
	
	private static final String SELECT_OUTAGE_SQL = 
			"SELECT outage_start_datetime,outage_end_datetime FROM" +
			" outage WHERE transaction =? AND  supplier = ?";

	private static final String QUOTA_SQL =
			"SELECT transaction,customerid,supplier," +
			"time_interval,quota_limit FROM quotas";
	
	private static final String SELECT_QUOTA_SQL = 
			"SELECT time_interval,quota_limit FROM quotas WHERE" +
			" transaction =? AND customerid =? AND supplier = ? ";

	private static ThrottlingDataCache singleton = null;

	/**
	 * private constructor
	 */
	private ThrottlingDataCache() {
	}

	public static ThrottlingDataCache getInstance() throws FrameworkException {

		if (singleton != null)
			return singleton;

		synchronized (ThrottlingDataCache.class) {
			if (singleton != null)
				return singleton;

			singleton = new ThrottlingDataCache();
			singleton.initialise();
			CacheManager.getRegistrar().register(singleton);			
		}
		return singleton;
	}

	/**
	 * Initialise 
	 * 
	 * @throws FrameworkException
	 */
	private void initialise() throws FrameworkException {

		outage = new HashMap<String, Outage>();
		quotas = new HashMap<String, Quota>();

		Connection conn = null;
		PreparedStatement pstm = null;
		ResultSet rs = null;

		try {
			conn = DBConnectionPool.getInstance().acquireConnection();
			pstm = conn.prepareStatement(QUOTA_SQL);
			rs = pstm.executeQuery();

			while (rs.next()) {
				Quota quota = new Quota(rs.getString("transaction"), rs
						.getString("customerid"), rs.getString("supplier"), rs
						.getLong("time_interval"), rs.getLong("quota_limit"));

				quotas.put(quota.getSupplier() + SEP + quota.getCustomerId()
						+ SEP + quota.getTransaction(), quota);
			}

			pstm = conn.prepareStatement(OUTAGE_SQL);
			rs = pstm.executeQuery();

			while (rs.next()) {
				Outage out = new Outage(rs.getString("transaction"), rs
						.getString("supplier"), rs
						.getTimestamp("outage_start_datetime"), rs
						.getTimestamp("outage_end_datetime"));

				outage.put(out.getSupplier() + SEP + out.getTransaction(), out);
			}

			
			
			if (Debug.isLevelEnabled(Debug.DB_DATA))
				Debug.log(Debug.DB_DATA,
						"Initialized throttling data cache with Outage -> "
								+ outage + "\nQuotas ->" + quotas);
			
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

	/**
	 * reload cache
	 */
	public void flushCache() throws FrameworkException {
		if (singleton != null) {
			if (outage != null)
				outage.clear();
			if (quotas != null)
				quotas.clear();
			outage = null;
			quotas = null;
			singleton = null;
		}
	}

	/**
	 * Get outage data 
	 * @param transaction
	 * @param supplier
	 * @param fetchLatest if data needs to fetched from database
	 * @return
	 */
	public Outage getOutage(String transaction, String supplier, boolean fetchLatest)
			throws FrameworkException {

		if (!fetchLatest) {

			return outage.get(supplier + SEP + transaction);
		}
		return getOutageFromDB(transaction, supplier);
	}

	/**
	 * Get quota
	 * @param transaction
	 * @param supplier
	 * @param customerId
	 * @param fetchLatest if data needs to fetched from database
	 * @return
	 */
	public Quota getQuota(String transaction, String supplier,
			String customerId, boolean fetchLatest) throws FrameworkException {

		if (!fetchLatest) {
			
			return quotas.get(supplier + SEP + customerId + SEP + transaction);
		}

		return getQuotaFromDB(transaction, supplier, customerId);
	}

	/**
	 * Get quota from database
	 * @param transaction
	 * @param supplier
	 * @param customerId
	 * @return
	 * @throws FrameworkException
	 */
	private Quota getQuotaFromDB(String transaction, String supplier,
			String customerId) throws FrameworkException {

		Connection conn = null;
		PreparedStatement pstm = null;
		ResultSet rs = null;

		try {
			conn = DBConnectionPool.getInstance().acquireConnection();
			pstm = conn.prepareStatement(SELECT_QUOTA_SQL);
			pstm.setString(1, transaction);
			pstm.setString(2, customerId);
			pstm.setString(3, supplier);
			rs = pstm.executeQuery();

			if (rs.next()) {
				Quota quota = new Quota(transaction, customerId, supplier, rs
						.getLong("time_interval"), rs.getLong("quota_limit"));

				return quota;
			}
			return null;
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

	/**
	 * Get outage from database
	 * @param transaction
	 * @param supplier
	 * @return
	 * @throws FrameworkException
	 */
	private Outage getOutageFromDB(String transaction, String supplier)
			throws FrameworkException {
		Connection conn = null;
		PreparedStatement pstm = null;
		ResultSet rs = null;

		try {
			conn = DBConnectionPool.getInstance().acquireConnection();
			pstm = conn.prepareStatement(SELECT_OUTAGE_SQL);
			pstm.setString(1, transaction);
			pstm.setString(2, supplier);
			rs = pstm.executeQuery();

			if (rs.next()) {
				Outage out = new Outage(transaction, supplier, rs
						.getDate("outage_start_datetime"), rs
						.getDate("outage_end_datetime"));

				return out;
			}
			return null;
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

	class Outage {

		public String getTransaction() {
			return transaction;
		}

		public String getSupplier() {
			return supplier;
		}

		public Date getOutageStartDt() {
			return outageStartDt;
		}

		public Date getOutageEndDt() {
			return outageEndDt;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("transaction :"+transaction).
			append("\tsupplier :"+supplier).
			append("\toutageStartDt :"+outageStartDt).
			append("\toutageEndDt :"+outageEndDt);
			
			return sb.toString();
		}

		private String transaction, supplier;
		private Date outageStartDt, outageEndDt;

		Outage(String transaction, String supplier, Date outageStartDt,
				Date outageEndDt) {
			this.transaction = transaction;
			this.supplier = supplier;
			this.outageStartDt = outageStartDt;
			this.outageEndDt = outageEndDt;
		}
	}

	class Quota {

		public String getTransaction() {
			return transaction;
		}

		public String getCustomerId() {
			return customerId;
		}

		public String getSupplier() {
			return supplier;
		}

		public long getTimeInterval() {
			return timeInterval;
		}

		public long getQuotaLimit() {
			return quotaLimit;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("transaction :"+transaction).
			append("\tsupplier :"+supplier).
			append("\tcustomerId :"+customerId).
			append("\ttimeInterval :"+timeInterval).
			append("\tquotaLimit :"+quotaLimit);
			
			return sb.toString();
		}

		private String transaction, customerId, supplier;
		private long timeInterval, quotaLimit;

		Quota(String transaction, String customerId, String supplier,
				long timeInterval, long quotaLimit) {
			this.transaction = transaction;
			this.customerId = customerId;
			this.supplier = supplier;
			this.timeInterval = timeInterval;
			this.quotaLimit = quotaLimit;
		}
	}
}
