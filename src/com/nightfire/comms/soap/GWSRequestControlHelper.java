package com.nightfire.comms.soap;

import com.nightfire.comms.soap.GWSRequestControlDataCache.DownTimeInfo;
import com.nightfire.framework.util.Debug;

import java.util.Date;
import java.sql.Timestamp;
/**
 * Class contains the methods to find current status of gateway
 * 
 * @author vishalb.gupta
 *
 */

public class GWSRequestControlHelper {
	private String driverKey;
	private String driverType;
	private String message;

	public GWSRequestControlHelper(String driverKey, String driverType) {
		this.driverKey = driverKey;
		this.driverType = driverType;
		Debug.log(Debug.NORMAL_STATUS,
				"GWSRequestControlHelper is inalized with DriverKey ["
						+ driverKey + "] and DriverType [" + driverType + "]");

	}

	public String getMessage() {
		return message;
	}
	
	/**
	 * Returns true if current timestamp falls between startdowntime and enddowntime 
	 * mentioned in table GWS_REQUEST_CONTROL for particular driverKey or driverKey/driverType
	 * 
	 * @return type boolean
	 */

	public boolean isGatewayDown() {
		try {
			DownTimeInfo downTimeInfo = GWSRequestControlDataCache.getInstance()
					.getDownTimeInfo(driverKey, driverType, false);

			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, "DownTimeInfo is [" + downTimeInfo
					+ "]");

			if (downTimeInfo == null) {
				return false;
			} else {
				Timestamp startDownTime = downTimeInfo.getDownTimeStartsAt();
				Timestamp endDownTime = downTimeInfo.getDownTimeEndsAt();

				Date currentDateTime = new Date();

				Timestamp currentTimestamp = new Timestamp(currentDateTime
						.getTime());

				if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
				Debug.log(Debug.NORMAL_STATUS, "Current Timestamp is ["
						+ currentTimestamp + "]");

				if (startDownTime == null) {
					if (endDownTime == null)
						return false;
					 
					if (endDownTime.after(currentTimestamp)) {
							message = downTimeInfo.getDownTimeMessage();
						return true;
						} 
						return false;
					}
				
				else {
					if (endDownTime == null) {
						if (currentTimestamp.before(startDownTime))
							return false;						
							message = downTimeInfo.getDownTimeMessage();
							return true;
						
					}

					
					if (currentTimestamp.before(endDownTime)&& currentTimestamp.after(startDownTime)) {
							message = downTimeInfo.getDownTimeMessage();
							return true;
						}
					return false;
				}

			}

		} catch (Exception e) {
			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS,
							"An Exception is occured in isGatewayDown method so assuming gateway is up");
			return false;

		}

	}

}
