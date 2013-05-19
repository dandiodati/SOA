package com.nightfire.comms.jms.failover;

/**
 * Interface holding constants for failover implementation.
 * 
 * @author hpirosha
 * 
 */
public interface LeaseConstants {

	public static final String UPDATE_OWNER_SQL = "UPDATE consumer_lease_conf SET consumer_has_restarted = ?  "
			+ " WHERE key = ? and consumer = ? ";

	/*
	 * either lease has expired or previously the lease was acquired by this
	 * consumer
	 */
	public static final String ACQUIRE_KEY_SQL = "UPDATE consumer_lease_conf SET acquired_by = ? , "
			+ "acquired_timestamp = ? , lease_expires_at = ? , consumer_has_restarted = ?"
			+ " WHERE key = ? AND consumer = ? AND (lease_expires_at < ? OR lease_expires_at IS NULL OR (consumer = acquired_by AND acquired_by = ?))";

	public static final String UPDATE_LEASE_SQL = "UPDATE consumer_lease_conf SET lease_expires_at = ?  WHERE key = ? and consumer = ?";

	public static final String OWNER_UP_SQL = "SELECT consumer_has_restarted FROM consumer_lease_conf WHERE key = ? and consumer = ? ";

	public static final String SELECT_LEASE_SQL = "SELECT lease_expires_at FROM consumer_lease_conf  WHERE key = ? and consumer = ?";

	public static final String UPDATE_KEY_OWNER_SQL = "UPDATE consumer_lease_conf SET acquired_by = ? ,"
			+ " acquired_timestamp = ? , lease_expires_at = ? , consumer_has_restarted = null WHERE key = ? "
			+ "AND alternate_consumer = ? AND ( lease_expires_at < ? OR lease_expires_at IS NULL )"; /*
											 * to make sure there isn't a race
											 * condition
											 */
}
