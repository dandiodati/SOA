package com.nightfire.adapter.messageprocessor;

import java.util.*;

import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.framework.message.MessageException;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.db.DBInterface;

/**
  * This Class collects all the records and passes on the list to the next processor.
  */
public class Batcher extends MessageProcessorBase
{

    /**
     * Initializes the Batcher.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     * @exception ProcessingException when initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {

       if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log(Debug.SYSTEM_CONFIG,"Batcher initializing...");

        // Call the abstract super class's initialize method. This initializes
        // the adapterProperties hashtable defined in the super class and
        // retrieves the name and toProcessorNames values from the properties.

        super.initialize(key, type);
        // initialize the List.
        batchedData = new Vector();

    } // end initialize method


  /**
   * This method gets a series of Message objects ,it then populates the List in
   * the Message Object ,after doing that it returns the list to the next
   * processor.
   *
   * @param msgObj The MesageObject containing a Map
   *
   * @return NVPair[] Name-Value pair array of next processor name and the MessageObject
   *
   * @exception ProcessingException Thrown if processing fails
   */
    public NVPair[] process ( MessageProcessorContext context, MessageObject msgObj )
        throws MessageException, ProcessingException
    {

        if(Debug.isLevelEnabled(Debug.BENCHMARK))
            Debug.log(Debug.BENCHMARK,"Reached Batcher process method...");

        // will come here if the driver is just looping
        //in this case just exit from the component
        if(msgObj == null && msgAvailable == false)
        {
            if(Debug.isLevelEnabled(Debug.UNIT_TEST))
                Debug.log(Debug.UNIT_TEST,"Msg Object is null &  flag is false.");
            
            return null;
        }
        //will come here after all the messages have been collected in the list
        //in this case reset the flag and return the list to the next component
        if(msgObj == null)
        {
            if(Debug.isLevelEnabled(Debug.UNIT_TEST))
                Debug.log(Debug.UNIT_TEST,"Collected all object now sending the list.");
            
            msgAvailable = false;
            MessageObject msob = new MessageObject(batchedData);
            
            if(Debug.isLevelEnabled(Debug.UNIT_TEST))
                Debug.log(Debug.UNIT_TEST,"MESSAGE OBJECT SENT TO THE NEXT PROCESSOR CONTAINS --->\n\n\n" + msob.describe());
            
            return formatNVPair(msob);
        }

        // set the flag to true...which means that there are more
        //messages to come , so all we do is populate the list and return a null
        //to the next processor.
        
        if(Debug.isLevelEnabled(Debug.UNIT_TEST))
            Debug.log(Debug.UNIT_TEST,"Collecting the objects...\n" +
                            "Object received :"+msgObj.describe()+"\n\n");
        msgAvailable = true;
        batchedData.add(msgObj.get());
        
        if(Debug.isLevelEnabled(Debug.UNIT_TEST))
            Debug.log(Debug.UNIT_TEST,"VECTOR SIZE ------>"+  batchedData.size());

        return null;

    }// end process method


    private boolean msgAvailable = false;
    private List batchedData;

    //--------------------------For Testing---------------------------------//

    public static void main(String[] args)
    {

        Debug.enableAll();
        Debug.showLevels();
        if (args.length != 3)
        {
          Debug.log (Debug.ALL_ERRORS, "Batcher: USAGE:  "+
          " jdbc:oracle:thin:@192.168.164.238:1521:e911 e911 e911 ");
          return;
        }
        try
        {

            DBInterface.initialize(args[0], args[1], args[2]);
        }
        catch (DatabaseException e)
        {
             Debug.log(null, Debug.MAPPING_ERROR, "Batcher: " +
                      "Database initialization failure: " + e.getMessage());
        }

        Map h1 = new HashMap();
        h1.put("METTA","LICA");
        h1.put("RECORD","HELLO");
        h1.put("JETHRO","TULL");

        HashMap h2 = new HashMap();
        h2.put("GARFIELD","JON");
        h2.put("ASTERIX","OBELIX");
        h2.put("ARCHIE","JUGHEAD");

        try
        {
            Batcher btch = new  Batcher();
            MessageProcessorContext mpx = new MessageProcessorContext();
            mpx.set("FileName","MANS9999");

            MessageObject msob1 = new MessageObject();
            msob1.set(h1);
            btch.initialize("BATCHER","BATCHER_ADAPTER");
            btch.process(mpx,msob1);

            MessageObject msob2 = new MessageObject();
            msob2.set(h2);
            btch.process(mpx,msob2);

             btch.process(mpx,null);

        }
        catch(Exception ex)
        {
            System.out.println(ex.getMessage());
        }

    }//end main method

}//end class

