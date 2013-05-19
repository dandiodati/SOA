package com.nightfire.comms.jms.failover;

/**
 * Class to represent different conditions while accessing the conf table as
 * events. This also serves as a factory for creating events.
 * 
 * Events are predefined i.e. new events can't be created.
 * 
 * @author hpirosha
 * 
 */
public class KeyLeaseEvent {

	public enum EVENT {
		LEASE_FOR_OWN_KEY_AVAILABLE(0), LEASE_FOR_ALTERNATE_KEY_AVAILABLE(1), RELEASE_ALTERNATE_KEY(
				2), OWN_KEY_ALREADY_ACQUIRED(3);

		private int eventType;

		EVENT(int eventType) {
			this.eventType = eventType;
		}

		public int valueOf() {
			return this.eventType;
		}
	};
	
	private EVENT eventType; 
	private KeyLeaseEvent(EVENT eventType) {
		this.eventType = eventType;
	}
	
	/**
	 * Event for own key available
	 * @return KeyLeaseEvent
	 */
	public static KeyLeaseEvent getLeaseForOwnKeyAvailableEvent() {
		return new KeyLeaseEvent(EVENT.LEASE_FOR_OWN_KEY_AVAILABLE);
	}
	
	/**
	 * Event for alternate key available
	 * @return KeyLeaseEvent
	 */
	public static KeyLeaseEvent getLeaseForAltKeyAvailable() {
		return new KeyLeaseEvent(EVENT.LEASE_FOR_ALTERNATE_KEY_AVAILABLE);
	}

	/**
	 * Event for alternate key released
	 * @return KeyLeaseEvent
	 */
	public static KeyLeaseEvent getReleaseAltKeyEvent() {
		return new KeyLeaseEvent(EVENT.RELEASE_ALTERNATE_KEY);
	}

	/**
	 * Event for own key already acquired
	 * @return KeyLeaseEvent
	 */
	public static KeyLeaseEvent getOwnKeyAlreadyAcquiredEvent() {
		return new KeyLeaseEvent(EVENT.OWN_KEY_ALREADY_ACQUIRED);
	}

	/**
	 * Gives value of the Event as one of the elements of an enum which
	 * contains the predefined events.
	 * 
	 * @return EVENT
	 */
	public EVENT eventType() {
		return this.eventType;
	}
}
