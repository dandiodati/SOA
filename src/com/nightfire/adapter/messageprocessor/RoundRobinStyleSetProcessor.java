package com.nightfire.adapter.messageprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;

/**
*
*/
public class RoundRobinStyleSetProcessor extends MessageProcessorBase
{
   /* Property prefix giving location of values. */
   public static final String VAL_PREFIX_PROP = "VALUE";

   /* Property prefix giving location where fetched data is to be set. */
   public static final String OUTPUT_LOC_PREFIX_PROP = "OUTPUT_LOC";

   private String outputLoc = null;
   private String key , type;

   /**
    * Initializes this object via its persistent properties.
    *
    * @param  key   Property-key to use for locating initialization properties.
    * @param  type  Property-type to use for locating initialization properties.
    *
    * @exception ProcessingException when initialization fails
    */
   public void initialize ( String key, String type ) throws ProcessingException
   {
       
       super.initialize( key, type );
       
       this.key = key;
       this.type = type;

       if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
           Debug.log(Debug.SYSTEM_CONFIG, ": Initializing..." );

       StringBuffer errorBuffer = new StringBuffer( );
       
       outputLoc = getPropertyValue(OUTPUT_LOC_PREFIX_PROP);
       

       List<String> values = new ArrayList<String>();
       
       for ( int Ix = 0;  true;  Ix ++ )
       {
           String val = getPropertyValue( PersistentProperty.getPropNameIteration( VAL_PREFIX_PROP, Ix ) );
           
           
           if ( !StringUtils.hasValue( val ) )
               break;
           
           values.add(val);
           
       }

       
       put(key+"#"+type,values);
       
       if ( errorBuffer.length() > 0 )
       {
           String errMsg = errorBuffer.toString( );

           Debug.log( Debug.ALL_ERRORS, errMsg );

           throw new ProcessingException( errMsg );
       }

       if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
           Debug.log( Debug.SYSTEM_CONFIG, ": Initialization done." );
   }


   /**
    *
    * @param  mpContext The context
    * @param  inputObject  Input message to process.
    *
    * @return  The given input, or null.
    *
    * @exception  ProcessingException  Thrown if processing fails.
    * @exception  MessageException  Thrown if message is bad.
    */
   public NVPair[] process ( MessageProcessorContext mpContext, MessageObject inputObject )
                       throws MessageException, ProcessingException 
   {
   	/* if input is null or output location is not configured */
       if ( inputObject == null || outputLoc == null)
           return null;

       /* Put the result in its configured location. */
       set( outputLoc, mpContext, inputObject, getNext(key+"#"+type) );
       
       
       /* Always return input value to provide pass-through semantics. */
       return( formatNVPair( inputObject ) );
   }

	private static HashMap<String, Integer> idIndex = new HashMap<String, Integer>();
	private static HashMap<String, List<String>> idValue = new HashMap<String, List<String>>();
	
	private void put(String id, List<String> values)
	{
		if(idValue.get(id)==null)
			idValue.put(id, values);
	}
	
	private String getNext(String id)
	{
		List<String> list = idValue.get(id);
		if(list==null)
			return null;
		
		synchronized (RoundRobinStyleSetProcessor.class) {
			Integer index = idIndex.get(id);
			if(index==null || (index+1)==list.size())
			{
				index = new Integer(-1);
			}
			idIndex.put(id, ++index);
			
			return list.get(index);
		}
	}
	
}

