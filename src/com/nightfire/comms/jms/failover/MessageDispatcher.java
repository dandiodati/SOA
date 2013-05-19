package com.nightfire.comms.jms.failover;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.nightfire.framework.jms.JMSConsumerCallBack;

import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.jms.AbstractJmsMsgStoreDAO;

/**
 * Dispatches a Message to a communication server for processing.
 * This class is backed-up by a thread pool for processing.
 * 
 * @author hpirosha
 * 
 */
public class MessageDispatcher {

	/**
	 * Thread Pool Executor.
	 */
	protected ThreadPoolExecutor executor = null;

	private JMSConsumerCallBack server = null;
	protected int corePoolSize;
	protected int maxPoolSize;

	

	/**
	 * 
	 * @param server implements the JMSConsumerCallBack interface
	 * @param corePoolSize
	 * @param maxPoolSize
	 */
	public MessageDispatcher(JMSConsumerCallBack server, int corePoolSize,
			int maxPoolSize) {
		this.corePoolSize = corePoolSize;
		this.maxPoolSize = maxPoolSize;
		this.server = server;
		executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 0,
				TimeUnit.SECONDS, new LinkedBlockingQueue(maxPoolSize));
	}

	/**
	 * Dispatch message for processing
	 * @param header String
	 * @param body String
	 * @param msgId String
	 */
	public void dispatch(String header, String body, String msgId) {

		final String aHeader = header;
		final String aBody = body;
		final String aMsgId = msgId;

		boolean notProcessed = true;
		while (notProcessed) {
			try {
				executor.execute(new Runnable() {

					public void run() {

						try {
							if (Debug.isLevelEnabled(Debug.MSG_STATUS))
								Debug
										.log(Debug.MSG_STATUS,
												"Started processing dequeued message in new thread...");

							try {
								CustomerContext
										.getInstance()
										.set(
												AbstractJmsMsgStoreDAO.PROP_JMSMESSAGEID,
												aMsgId);
							} catch (Exception ignore) {
							}

							server.processMessage(aHeader, aBody);

							if (Debug.isLevelEnabled(Debug.MSG_STATUS))
								Debug.log(Debug.MSG_STATUS,
										"Done processing dequeued message.");
						} catch (Exception ex) {
							Debug.log(Debug.ALL_ERRORS,
									"ERROR: MessageDispatcher.run() - Processing failed due to ["
											+ ex.getMessage() + "]");
							Debug
									.log(Debug.ALL_ERRORS, Debug
											.getStackTrace(ex));
						} finally {
							try {
								CustomerContext.getInstance().cleanup();
							} catch (FrameworkException e) {
								Debug
										.warning("An exception occurred while cleaning up CustomerContext :"
												+ Debug.getStackTrace(e));
							}
						}

					}

				});
				notProcessed = false;
			} catch (RejectedExecutionException ree) {
				/*
				 * if executor rejects this message wait for a free thread in
				 * the pool.
				 */
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					Debug
							.log(Debug.ALL_ERRORS,
									"Interrupted sleeping thread..");
				}
			} catch (RuntimeException re) {
				notProcessed = false;
				Debug.log(Debug.ALL_ERRORS,
						"Runtime Exception while executing the request");
				Debug.log(Debug.ALL_ERRORS, Debug.getStackTrace(re));
				Debug.error("Skipping this request .. ");
			}
		}

	}

	public void shutdown()
	{
		if(executor!=null)
			executor.shutdown();
	}
}
