package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import com.nightfire.common.ProcessingException;

import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.common.driver.MessageObject;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.Debug;


import com.nightfire.spi.neustar_soa.adapter.smartsockets.SOAClient;

public class SOASmartSocketsClient extends MessageProcessorBase {

  public static final String SOA_PROJECT_PROP = "SOA_INTERNAL_PROJECT";

  public static final String SOA_SUBJECT_PROP = "SOA_INTERNAL_SUBJECT";

  public static final String SOA_RTSERVER_PROP = "SOA_RTSERVER";

  public static final String MESSAGE_LOC_PROP = "MESSAGE_LOCATION";

  private String soaProject;

  private String soaSubject;

  private String soaRtServer;

  private String messageLoc;

  public void initialize(String key, String type)
                         throws ProcessingException {

     super.initialize(key, type);

     soaProject  = getRequiredPropertyValue(SOA_PROJECT_PROP);
     soaSubject  = getRequiredPropertyValue(SOA_SUBJECT_PROP);
     soaRtServer = getRequiredPropertyValue(SOA_RTSERVER_PROP);
     messageLoc  = getRequiredPropertyValue(MESSAGE_LOC_PROP);

  }

   public NVPair[] process(MessageProcessorContext context,
                           MessageObject message)
                           throws MessageException,
                                  ProcessingException
   {
	  ThreadMonitor.ThreadInfo tmti = null;
      // Required knee-jerk reaction to a null input.
      if( message == null ){
         return null;
      }

      String npacMessage = getString(messageLoc, context, message);

      SOAClient client = null;

      try{
        client = new SOAClient(soaProject, soaSubject, soaRtServer);
      }
      catch(FrameworkException fwex){
         throw new ProcessingException("SOA Client cannot be created: " + fwex);
      }

      try{
    	tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] sending request XML" + npacMessage );
        client.sendXML( npacMessage );
        client.cleanup();
      }
      catch(FrameworkException fwex){
         throw new ProcessingException("Could not send message: " + fwex );
      }
      finally
      {
    	  ThreadMonitor.stop(tmti);
      }

      // returns the original message object.
      return formatNVPair(message);

   }

}
