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
  * This Class splits the list it receives into individual objects and returns
  * them one at a time to the next processor
  */
public class UnBatcher extends MessageProcessorBase
{
    /**
     * Initializes the UnBatcher.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     * @exception ProcessingException when initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        if ( Debug.isLevelEnabled ( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "UnBatcher: initializing...");

        // Call the abstract super class's initialize method. This initializes
        // the adapterProperties hashtable defined in the super class and
        // retrieves the name and toProcessorNames values from the properties.
        super.initialize(key, type);

    } // end initialize method


     /**
   * This gets a list of maps , it then  passes these maps individually to the next
   * processor.
   * This class expects at least a single next processor hence it return a null if there are no next
   * processors.
   * @param msgObj The MesageObject containg the list of maps
   *
   * @return NVPair[] Name-Value pair array of next processor name and the MessageObject
   *
   * @exception ProcessingException Thrown if processing fails
   */
    public NVPair[] process ( MessageProcessorContext context, MessageObject msgObj )
        throws MessageException, ProcessingException
    {
         if ( Debug.isLevelEnabled ( Debug.BENCHMARK ) )
            Debug.log( Debug.BENCHMARK,"Reached UnBatcher process method...");

        if(msgObj == null)
        {
            return null;
        }
        else if(toProcessorNames == null)
        {
           Debug.log(Debug.ALL_WARNINGS,"UnBatcher: No next processors available. Hence exiting.");
        }
        //Check if message object contains a list
        else if ( !(msgObj.get() instanceof List) )
        {

            throw new ProcessingException("ERROR: UnBatcher: The msg Object must be a List." );
        }

        List RecordList = (List)(msgObj.get());
        int batchedSize = RecordList.size();
        NVPair[] content = new NVPair [ batchedSize * toProcessorNames.length ];
        int processorsNo = toProcessorNames.length;
        // seperate the list into individual maps
        for(int i=0; i < batchedSize; i++)
        {

            Object record = RecordList.get(i);

            //create a new message object for each object.
            MessageObject mObj = new MessageObject(record);
            
            // Sending the message objects to the next processors.
            for (int j = 0; j < processorsNo; j++)
            {

                content[(i*processorsNo)+j] = new NVPair ( toProcessorNames[j], mObj );
                if ( Debug.isLevelEnabled ( Debug.UNIT_TEST ) )
                    Debug.log( Debug.UNIT_TEST,"NEXT PROCESSOR-->"+toProcessorNames[j]+"\n"+
                          "MESSAGE OBJECT CONTENT------------>"+mObj.describe());
            }

        }
        return(content);

    } // end process method


    //--------------------------For Testing---------------------------------//

    public static void main(String[] args)
    {

        Debug.enableAll();
        Debug.showLevels();
        if (args.length != 3)
        {
          Debug.log (Debug.ALL_ERRORS, "UnBatcher: USAGE:  "+
          " jdbc:oracle:thin:@192.168.164.238:1521:e911 e911 e911 ");
          return;
        }
        try
        {

            DBInterface.initialize(args[0], args[1], args[2]);
        }
        catch (DatabaseException e)
        {
             Debug.log( Debug.ALL_ERRORS, "UnBatcher: " +
                      "Database initialization failure: " + e.getMessage());
        }

        UnBatcher unb = new UnBatcher();
        HashMap h1 = new HashMap();
        h1.put("DAMAN","SONI");
        h1.put("RECORD","HELLO");
        h1.put("BYE","TATA");
        HashMap h2 = new HashMap();
        h2.put("GARFIELD","JON");
        h2.put("ASTERIX","OBELIX");
        h2.put("ARCHIE","JUGHEAD");
        HashMap h3 = new HashMap(h1);
        List v1 = new Vector();
        v1.add(h1);
        v1.add(h2);
        v1.add(h3);

        try
        {
            MessageProcessorContext mpx = new MessageProcessorContext();
            mpx.set("FileName","MANS9999");
            MessageObject msob = new MessageObject();
            msob.set(v1);
            unb.initialize("E911BATCHER","E911_REQUEST_UNBATCHER");
            unb.process(mpx,msob);

        }
        catch(Exception ex)
        {
            System.out.println(ex.getMessage());
        }

    } // end main method


}// end class