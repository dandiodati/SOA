package com.nightfire.comms.jms.failover;

import com.nightfire.framework.util.FrameworkException;

/**
 * Interface to be implemented for listening to key events.
 * 
 * @author hpirosha
 * 
 */
public interface KeyLeaseEventListener {

	/**
	 * Method to handle KeyLeaseEvent
	 * 
	 * @param event
	 * @throws FrameworkException
	 */
	public void handleEvent(KeyLeaseEvent event) throws FrameworkException;

	/**
	 * Status of the Key Event Listener
	 * 
	 * @return
	 */
	public boolean isAlive();
}
