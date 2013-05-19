package com.nightfire.comms.servicemgr;

/**
 * Constants used by Service Management.
 * 
 * @author hpirosha
 */
public interface ServiceMgrConsts {

	/**
	 * Different constants for identifying type of PollCommServers
	 */
	public static String FTP_POLLER = "ftp-poller";
	public static String FILE_SERVER = "file-server";
	public static String ASYNC_TIMER_SERVER = "timer-server";
	public static String SRM_JMS_SERVER = "multi-customer-jms-server";
	public static String SRM_CONFIGURED_QUEUES_JMS_SERVER = "configured-queues-jms-server";
	public static String SRM_EVENT_SERVER = "multi-customer-event-server";
	public static String IA_SERVER = "ia-server";
	public static String POLL_COMM_SERVER_CONFIG = "poll-comm-server-config";

	/**
	 * Constant to identify a particular AsyncEmailServer.
	 */
	public static final String ASYNC_EMAIL_SERVER = "async-email-server";

	public static final String POLL_COMM_SERVER = "poll-comm-server";

	public static final String JMS_CONSUMER = "jms-consumer";

	/*	 
	 * Constants for supportted and unsupported services
	 */

	public static final String SUPPORTED_SERVICES = "SUPPORTED_SERVICES";

	public static final String UNSUPPORTED_SERVICES = "UNSUPPORTED_SERVICES";

	public static final String ID_CONSTANT = "id";

	public static final String GROUP_TAG_CONSTANT = "group";

}
