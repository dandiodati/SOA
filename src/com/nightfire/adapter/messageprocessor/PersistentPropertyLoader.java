package com.nightfire.adapter.messageprocessor;

import java.util.ArrayList;
import java.util.List;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;

/**
 * Message Processor that loads configured properties from
 * persistent property and sets into message processor context.
 * It uses persistent property api to set the property values
 * in configured output locations.
 * 
 * Properties that needs to be loaded are defined as iterative. 
 * Following are the properties for this message processor :
 * KEY_{IDX} : The Key value which should be looked-up from PersistentProperty.
 * PROPERTY_TYPE_{IDX} : The PropertyType value which should be looked-up from PersistentProperty.
 * PROPERTY_NAME_{IDX} : The PropertyName value which should be looked-up from PersistentProperty.
 * OUTPUT_LOCATION_{IDX} : The output context location for obtained PropertyValue from PersistentProperty.
 * USE_CUSTOMER_ID_{IDX} : Flag indicating whether the CustomerId set in CustomerContext should be used implicity by 
 * 						   PersistentProperty API, or the DEFAULT cutomer should be set in CustomerContext before accessing 
 * 						   PersistentProperty API. TRUE= internally use the set customerid,
 * 						   FALSE = set DEFAULT customerid and then access API.
 * 
 * 
 * @author hpirosha
 *
 */
public class PersistentPropertyLoader extends MessageProcessorBase {

	private static final String KEY_PROP = "KEY";
	private static final String PROPERTY_TYPE_PROP = "PROPERTY_TYPE";
	private static final String PROPERTY_NAME_PROP = "PROPERTY_NAME";
	private static final String OUTPUT_LOCATION_PROP = "OUTPUT_LOCATION";
	private static final String USE_CUSTOMER_ID_PROP = "USE_CUSTOMER_ID";
	private List<PPDataHolder> ppData = new ArrayList<PPDataHolder>();
	
	
	@Override  
	public void initialize(String key, String type) throws ProcessingException {
		
		super.initialize( key, type );
		
		for ( int Ix = 0;  true;  Ix ++ )
        {
			String propKey = getPropertyValue(PersistentProperty.getPropNameIteration(KEY_PROP, Ix));
			
			if(!StringUtils.hasValue(propKey))
				break;
					
			PPDataHolder holder = new PPDataHolder(
					propKey,
					getPropertyValue(PersistentProperty.getPropNameIteration(
							PROPERTY_TYPE_PROP, Ix)),
					getPropertyValue(PersistentProperty.getPropNameIteration(
							PROPERTY_NAME_PROP, Ix)),
					getPropertyValue(PersistentProperty.getPropNameIteration(
							OUTPUT_LOCATION_PROP, Ix)),
					getPropertyValue(PersistentProperty.getPropNameIteration(
							USE_CUSTOMER_ID_PROP, Ix)));
			
			ppData.add(holder);
			
			if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
				Debug.log(Debug.SYSTEM_CONFIG," Found configured property :"+holder);
           
        }
		
	}
	
	
	@Override
	public NVPair[] process ( MessageProcessorContext mpContext, MessageObject inputObject )
    throws MessageException, ProcessingException 
    {
		if(inputObject ==null)
			return null;
		/*
		 * Load the value of configured properties
		 */
		for(PPDataHolder holder : ppData)
		{
			String propertyValue = null;
			try
			{
				boolean useCustomerId = StringUtils.getBoolean(holder.getUseCustomerId());
				
				if(useCustomerId)
					propertyValue = PersistentProperty.get(holder.getKey(), holder.getPropertyType(), holder.getPropertyName());
				else
				{
					String customerId = CustomerContext.getInstance().getCustomerID();
					CustomerContext.getInstance().setCustomerID(CustomerContext.DEFAULT_CUSTOMER_ID);
					propertyValue = PersistentProperty.get(holder.getKey(), holder.getPropertyType(), holder.getPropertyName());
					CustomerContext.getInstance().setCustomerID(customerId);
				}
				
			}
			catch(Exception ex)
			{
				Debug.warning("Unable to get configured property from persistent property :"+holder.toString());
			}
			
			/*
			 * set the value into message processor context
			 */
			if(StringUtils.hasValue(propertyValue))
			{
				if(Debug.isLevelEnabled(Debug.DB_DATA))
					Debug.log(Debug.DB_DATA," setting value :"+propertyValue +" on location :"+holder.getOutputLoc());
				
				mpContext.set(holder.getOutputLoc(), propertyValue);
			}
		}
		
		
		/* Always return input value to provide pass-through semantics. */
	     return( formatNVPair( inputObject ) ); 
    }
	
	private class PPDataHolder {
		private String key, propertyType, propertyName, outputLoc, useCustomerId;

		PPDataHolder(String key, String propertyType, String propertyName, String outputLoc, String useCustomerId)
		{
			this.key = key;
			this.propertyType = propertyType;
			this.propertyName = propertyName;
			this.outputLoc = outputLoc;
			this.useCustomerId = useCustomerId;
		}
		
		public String getKey() {
			return key;
		}

		public String getPropertyType() {
			return propertyType;
		}

		public String getPropertyName() {
			return propertyName;
		}

		public String getOutputLoc() {
			return outputLoc;
		}

		public String getUseCustomerId() {
			return useCustomerId;
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(" key =").append(key)
			.append(" propertytype =").append(propertyType)
			.append(" propertyName =").append(propertyName)
			.append(" outputLoc =").append(outputLoc)
			.append(" useCustomerId =").append(useCustomerId);
			
			return sb.toString();
		}
	}
}
