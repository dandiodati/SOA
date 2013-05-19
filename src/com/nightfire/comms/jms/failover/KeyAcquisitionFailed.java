package com.nightfire.comms.jms.failover;

import com.nightfire.common.ProcessingException;

/**
 * This exception is thrown if KEY from consumer_lease_conf table cannot be
 * acquired after fixed number of attempts.
 * 
 * @author hpirosha
 * 
 */
public class KeyAcquisitionFailed extends ProcessingException {

	/**
	 * This is initialized with the underlying cause
	 * 
	 * @param cause
	 */
	public KeyAcquisitionFailed(Exception cause) {
		super(cause);
	}

	/**
	 * This is intialized with the error message
	 * 
	 * @param message
	 */
	public KeyAcquisitionFailed(String message) {
		super(message);
	}

	private static final long serialVersionUID = 10999L;

}
