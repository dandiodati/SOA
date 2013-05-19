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

public class Splitter extends MessageProcessorBase
{
    /**
     * Initializes the Splitter.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     * @exception ProcessingException when initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {

        // Call the abstract super class's initialize method. This initializes
        // the adapterProperties hashtable defined in the super class and
        // retrieves the name and toProcessorNames values from the properties.
        super.initialize(key, type);

        if ( Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE) )
            Debug.log(Debug.OBJECT_LIFECYCLE, "Splitter: Initializing.....");

        StringBuffer errorBuffer = new StringBuffer( );
        truncHeaderFooter = getRequiredPropertyValue(TRUNCATE_HEADER_FOOTER_PROP, errorBuffer);

        if ( Debug.isLevelEnabled(Debug.MSG_DATA) )
            Debug.log(Debug.MSG_DATA, "Splitter: truncHeaderFooter? ---->"+truncHeaderFooter);


        String temp = getPropertyValue(FILE_SEPARATOR_PROP);
        if (StringUtils.hasValue(temp))
        {
            fileSeparator = StringUtils.replaceSubstrings(temp,"\\r","\r");
            fileSeparator = StringUtils.replaceSubstrings(fileSeparator,"\\n","\n");
            if ( Debug.isLevelEnabled(Debug.MSG_DATA) )
                Debug.log(Debug.MSG_DATA, "Splitter: fileSeparator---->"+fileSeparator);
        }

        try
        {
            splitLength = Integer.parseInt(getRequiredPropertyValue(SPLIT_LENGTH_PROP, errorBuffer));

            if ( Debug.isLevelEnabled(Debug.MSG_DATA) )
                Debug.log(Debug.MSG_DATA, "Splitter: splitLength---->"+splitLength);
        }

        catch(NumberFormatException nx)
        {
            throw new ProcessingException("ERROR: Splitter: The SPLIT_LENGTH " +
                                "must be a number." );
        }

        if (splitLength <=0)
        {
            throw new ProcessingException("ERROR: Splitter: The SPLIT_LENGTH " +
                                            "must be greater than zero.");
        }

        if ( errorBuffer.length() > 0 )
        {
            String errMsg = errorBuffer.toString( );

            Debug.log( Debug.ALL_ERRORS, errMsg );

            throw new ProcessingException( errMsg );
        }


    }

 /**
   * This gets a String to be split in the message object.It splits the string into substrings
   * and then stores them in the message object to be apssed to the next processors individually.
   * This class expects at least a single next processor hence it return a null if there are no next
   * processors.
   *
   * @param msgObj The MesageObject containg the string.
   *
   * @return NVPair[] Name-Value pair array of next processor name and the MessageObject
   *
   * @exception ProcessingException Thrown if processing fails
   */
    public NVPair[] process ( MessageProcessorContext context, MessageObject msgObj )
        throws MessageException, ProcessingException
    {

        if ( Debug.isLevelEnabled(Debug.BENCHMARK) )
            Debug.log(Debug.BENCHMARK,"Reached Splitter process method..");

        //Debug.log(null, Debug.UNIT_TEST,"MESSAGE OBJECT RECEIVED:--->"+msgObj.describe());

        if(msgObj == null)
        {
            return null;
        }

        else if(toProcessorNames == null)
        {
           Debug.log(Debug.ALL_WARNINGS,"Splitter: No next processors available. Hence exiting.");
        }
        else if ( !(msgObj.get() instanceof String) )
        {
            if ( Debug.isLevelEnabled(Debug.UNIT_TEST) )
                Debug.log(Debug.UNIT_TEST,"SPLITTER : UNIT_TEST: "+((msgObj.get()).getClass()));

            throw new ProcessingException("ERROR: Splitter: " +
                                       "The msg Object must be a String." );
        }

        if ( Debug.isLevelEnabled(Debug.UNIT_TEST) )
            Debug.log( Debug.UNIT_TEST,"MESSAGE OBJECT RECEIVED:--->"+msgObj.describe());

         String batchedRecords =   msgObj.getString();

         List singleRecord ;
         String subStr ="";
         // to take care empty batch files that are valid files in some cases
         if( !StringUtils.hasValue(batchedRecords))
         {
             Debug.log(Debug.ALL_WARNINGS,"MESSAGE CONTENT IS NULL");
             return null;
         }

         if( !StringUtils.hasValue(fileSeparator))   //if fileseparator is false...
         {
            if (batchedRecords.length() < splitLength)
            {
                throw new MessageException("ERROR: Splitter: " +
                                       "Batched String length is less than " +
                                       splitLength + "bytes." );
            }

            float tempSize = (float)( batchedRecords.length() ) / splitLength;
            int size =  ( batchedRecords.length() ) / splitLength;
            
            //check for length of string
            if(size != tempSize)
            {
                throw new MessageException("ERROR: Splitter: " +
                                    "Record received is not of correct length");

            }
            singleRecord = new Vector(size);
            int i = 0;
            for ( i = 0 ;i<size ;i++)
            {

                subStr = batchedRecords.substring(i*splitLength, (i+1)*splitLength) ;
                singleRecord.add(i, subStr);
            }
         }
         else   //if fileseparator is true...
         {
            singleRecord = new Vector();
            StringTokenizer st = new StringTokenizer(batchedRecords, fileSeparator );
            while(st.hasMoreTokens())
            {
                subStr = st.nextToken();
                if (subStr.length() != splitLength)
                {
                    throw new MessageException("ERROR: Splitter: Record "+
                    "received after removing the fileseparator is not of correct length .");
                }
                singleRecord.add(subStr);

                if ( Debug.isLevelEnabled(Debug.UNIT_TEST) )
                    Debug.log( Debug.UNIT_TEST,"\nDATA\n" +
                        "********************************************"+subStr);

            }

         }

         if(getBoolean(truncHeaderFooter))  //if truncHeaderFooter is true...
         {
            singleRecord.remove( (singleRecord.size()- 1) );
            singleRecord.remove(0);

         }
            //else continue...
         int batchedSize = singleRecord.size();

        if ( Debug.isLevelEnabled(Debug.UNIT_TEST) )
            Debug.log( Debug.UNIT_TEST,"BATCHED SIZE--->"+  batchedSize);

         int processorsNo = toProcessorNames.length;
         NVPair[] contents = new NVPair [ batchedSize * processorsNo];
         int i=0;

         for(i=0; i<batchedSize; i++)
         {
            MessageObject mObj = new MessageObject();
            mObj.set(singleRecord.get(i));

            // Sending the message objects to the next processors.
            for (int j = 0; j < processorsNo; j++)
            {

                contents[(i*(processorsNo))+j] = new NVPair ( toProcessorNames[j], mObj );

                if ( Debug.isLevelEnabled(Debug.UNIT_TEST) )
                    Debug.log(Debug.UNIT_TEST,"NEXT PROCESSOR-->"+toProcessorNames[j]+"\n"+
                          "MESSAGE OBJECT CONTENT------------>"+mObj.describe());
            }

         }

         if ( Debug.isLevelEnabled(Debug.UNIT_TEST) )
            Debug.log( Debug.UNIT_TEST, "SPLITTER : CONTEXT---->" + context.describe());

        return contents;

    }


    private static final String TRUNCATE_HEADER_FOOTER_PROP = "TRUNCATE_HEADER_FOOTER";
    private static final String SPLIT_LENGTH_PROP = "SPLIT_LENGTH";
    private static final String FILE_SEPARATOR_PROP = "FILE_SEPARATOR";

    private String truncHeaderFooter;
    private int splitLength;
    private String fileSeparator;

    //--------------------------For Testing---------------------------------//

    public static void main(String[] args)
    {

        Debug.enable(Debug.UNIT_TEST);
        Debug.enable(Debug.BENCHMARK);
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
             Debug.log(Debug.MAPPING_ERROR, "UnBatcher: " +
                      "Database initialization failure: " + e.getMessage());
        }

        Splitter spl = new Splitter();

        try
        {
            MessageProcessorContext mpx = new MessageProcessorContext();
            mpx.set("FileName","MANS9999");

            String s = FileUtils.readFile("D:\\Response\\DECCCORR.txt") ;
            System.out.println("length----------->>>>>"+ s.length());
            MessageObject msob = new MessageObject();
            msob.set(s);
            spl.initialize("E911BATCHER","FAX_MESSAGE_SPLITTER");
            spl.process(mpx,msob);


        }
        catch(Exception ex)
        {
            System.out.println(ex.getMessage());
        }

    }


}

