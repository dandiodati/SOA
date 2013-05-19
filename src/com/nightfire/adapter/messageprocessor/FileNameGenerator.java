package com.nightfire.adapter.messageprocessor;

import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.framework.message.MessageException;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.PersistentSequence;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.util.*;

/**
  * This Class is used to generate the file name using a prefix from the
  * properties and a sequence no. from the specified sequence table.
  */
public class FileNameGenerator extends MessageProcessorBase
{

    /**
     * Initializes the FileNameGenerator.
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

        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, "FileNameGenerator: Initializing...");

        namePrefix = StringUtils.padString(getRequiredPropertyValue(NAME_PREFIX_PROP),4,true,' ');
        if(Debug.isLevelEnabled(Debug.BENCHMARK))
            Debug.log(Debug.BENCHMARK, "FileNameGenerator: Prefix---->"+namePrefix);

        sequenceName = getRequiredPropertyValue(SEQUENCE_NAME_PROP);
        if(Debug.isLevelEnabled(Debug.BENCHMARK))
            Debug.log(Debug.BENCHMARK, "FileNameGenerator: sequenceName---->"+sequenceName);

        filenameLoc = getRequiredPropertyValue(FILENAME_LOCATION_PROP);
        if(Debug.isLevelEnabled(Debug.BENCHMARK))
            Debug.log(Debug.BENCHMARK, "FileNameGenerator: filenameLoc---->"+filenameLoc);

        try
        {
            sequenceLength = Integer.parseInt(getRequiredPropertyValue(SEQUENCE_LENGTH_PROP));
            if(Debug.isLevelEnabled(Debug.BENCHMARK))
                Debug.log(Debug.BENCHMARK, "FileNameGenerator: sequenceLength---->"+sequenceLength);
        }
        catch(NumberFormatException nx)
        {
            throw new ProcessingException("ERROR: FileNameGenerator: The SEQUENCE_LENGTH " +
                                "must be a number." );
        }

        if (sequenceLength <=0)
        {
            throw new ProcessingException("ERROR: FileNameGenerator: The SEQUENCE_LENGTH " +
                                            "must be greater than zero.");
        }



    }//end of initialize method


  /**
   * This method generates the filename and sets it in the context.
   * @param msgObj The MesageObject to be sent to the next processor
   *
   * @return NVPair[] Name-Value pair array of next processor name and the MessageObject passed in
   *
   * @exception ProcessingException Thrown if processing fails
   */
    public NVPair[] process ( MessageProcessorContext context, MessageObject msgObj )
        throws MessageException, ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.BENCHMARK))
            Debug.log(Debug.BENCHMARK, "FileNameGenerator: Reached process method...");

        if(msgObj == null)
        {
            return null;
        }
        String fileName = "";
        String sequence = "";
        try
        {
            //get the next sequence
            int tempInt = PersistentSequence.getNextSequenceValue(sequenceName);
            sequence = StringUtils.padNumber(tempInt,sequenceLength,true,'0');

            if(Debug.isLevelEnabled(Debug.BENCHMARK))
                Debug.log(Debug.BENCHMARK, "FileNameGenerator: Sequence----->"+sequence);

        }
        catch(DatabaseException dbe)
        {
            throw new ProcessingException("ERROR: FileNameGenerator: Unable to get sequence number");

        }
        fileName =  namePrefix + sequence;

        if(Debug.isLevelEnabled(Debug.BENCHMARK))
            Debug.log(Debug.BENCHMARK, "FileNameGenerator: File Name----->"+fileName);
        
        //set the filename in the context or the message object, as mentioned in the properties
        this.set(filenameLoc, context, msgObj, fileName);

      if(Debug.isLevelEnabled(Debug.UNIT_TEST))
      {
          Debug.log(Debug.UNIT_TEST, "FileNameGenerator: CONTEXT CONTENT----->"+context.describe());
          Debug.log(Debug.UNIT_TEST, "FileNameGenerator: MESSAGE OBJECT CONTENT----->"+msgObj.describe());
      }

        //Return the message Object as it is
        return( formatNVPair ( msgObj ) );


    }//end of process method

    private static final String SEQUENCE_NAME_PROP = "SEQUENCE_NAME";
    private static final String NAME_PREFIX_PROP = "NAME_PREFIX";
    private static final String FILENAME_LOCATION_PROP = "FILENAME_LOCATION";
    private static final String SEQUENCE_LENGTH_PROP = "SEQUENCE_LENGTH";

    private String namePrefix;
    private String sequenceName;
    private String filenameLoc;
    private int sequenceLength;


    //--------------------------For Testing---------------------------------//

    public static void main(String[] args)
    {

        Debug.enableAll();
//        Debug.showLevels();
        if (args.length != 3)
        {
          Debug.log (Debug.ALL_ERRORS, "FileNameGenerator: USAGE:  "+
          " jdbc:oracle:thin:@192.168.164.238:1521:e911 e911 e911 ");
          return;
        }
        try
        {

            DBInterface.initialize(args[0], args[1], args[2]);
        }
        catch (DatabaseException e)
        {
             Debug.log(Debug.ALL_ERRORS, "FileNameGenerator: " +
                      "Database initialization failure: " + e.getMessage());
        }


        FileNameGenerator fgn = new FileNameGenerator();

        try
        {
            fgn.initialize("E911BATCHER","E911_REQUEST_FILE_NAME_GENERATOR");
            MessageProcessorContext mpx = new MessageProcessorContext();
            MessageObject mob = new MessageObject();
            mob.set("AAAA","Data");
            fgn.process(mpx,mob);
            //Debug.log(Debug.BENCHMARK, "FileName from Context--->" + mpx.get("FileName"));

        }
        catch(ProcessingException pex)
        {
            System.out.println(pex.getMessage());
        }
        catch(MessageException mex)
        {
            System.out.println(mex.getMessage());
        }
    } //end of main method


} //end of class