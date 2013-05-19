/**
 * Copyright(c) 2000 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.adapter.messageprocessor.queue;

import org.w3c.dom.Document;

// NightFire imports
import com.nightfire.comms.queue.PCQueueManager;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;

/**
 * XMLQueueConsumer is a MessageProtocolAdapter.  It accepts an XML message
 * and uses a value from that message as a key for a PCQueue on which to wait
 * for a response.
 */
public class XMLQueueConsumer extends MessageProcessorBase
{    
    /**
     * Name of the property which contains the location of the key value
     * in the XML document.
     */
    private static final String KEY_LOCATION_PROP_NAME = "KEY_LOCATION";

    /**
     * Name of the property which indicates the maximum number of SECONDS
     * to wait on the queue.
     */
    private static final String TIMEOUT_PROP_NAME = "QUEUE_TIMEOUT";

    /**
     * Name of the property which specifies a name to use with the
     * message object.
     */
    private static final String INPUT_NAME_PROP_NAME = "INPUT_NAME";

    /**
     * The location of the key value in the XML document.
     */
    private String keyLocation = null;

    /**
     * The name to use for retrieving input.  This can be null.
     */
    private String inputName = null;

    /**
     * The maximum nuber of MILLISECONDS to wait on the queue.
     */
    private long timeout = 0;

    /**
     * Default constructor.
     */
    public XMLQueueConsumer()
    {
        super();
    }

    /**
     * Called to initialize the message processor.
     *
     * @param key    Property key to use for locating initialization
     *               properties.
     * @param type   Property type to use for locating initialization
     *               properties.
     *
     * @exception    ProcessingException  Thrown if initialization fails
     */
    public void initialize(String key, String type) throws ProcessingException
    {
        // have the super class do its thing
        super.initialize(key, type);

        // allocate a buffer to store any error messages
        StringBuffer errorBuff = new StringBuffer();

        // get our key location in the XML document
        keyLocation = getRequiredPropertyValue(KEY_LOCATION_PROP_NAME,
                                               errorBuff);

        // get our timeout for the queue in seconds
        timeout = getRequiredIntProperty(TIMEOUT_PROP_NAME, errorBuff);
        // convert it to milliseconds
        timeout *= 1000;
        
        // get our optional input name
        inputName = getPropertyValue(INPUT_NAME_PROP_NAME);
        if (inputName != null)
            inputName = inputName.trim();

        // check to see if we had any errors
        if (errorBuff.length() > 0)
            throw new ProcessingException(errorBuff.insert(0,
                    "XMLQueueConsumer had the following property errors:\n")
                                          .toString());
    }

    /**
     * Called when the processor should execute a message.  When processing a
     * message, XMLQueueConsumer uses a configurable value to retrieve a value
     * from the XML document.  That value is then used as the key it gives to
     * PCQueueManager to wait for a message.
     *
     * @param context  The message context.
     * @param input    The message to process
     * 
     * @return         A list of name and value pairs, or null if none.
     *
     * @exception      MessageException    Thrown if there is an error with
     *                                     the message data.
     * @exception      ProcessingException Thrown if an error occurs while
     *                                     processing the message which is
     *                                     unrelated to the message data
     */
    public NVPair[] process(MessageProcessorContext context,
                            MessageObject input)
        throws MessageException, ProcessingException
    {
        // check for a null input
        if (input == null)
            return null;

        // get a parser to work with
        XMLMessageParser parser = getParser(input);

        Debug.log(this, Debug.MSG_STATUS,
             "XMLQueueConsumer is attempting to retrieve a queue key from [" +
                  keyLocation + "].");

        // check for existence first, so we can throw a meaningful exception
        // if the value isn't there
        if (!parser.exists(keyLocation))
            throw new MessageException(
                   "XMLQueueConsumer failed to find the required location [" +
                   keyLocation + "] in the input message.");

        // now get our value
        String keyVal = parser.getValue(keyLocation);

        Debug.log(this, Debug.MSG_STATUS,
                  "XMLQueueConsumer is waiting [" + (timeout / 1000) +
                  "] seconds for a message matching the key [" +
                  keyVal + "].");

        // wait for a message
        MessageObject output =
            (MessageObject)PCQueueManager.getInst().dequeue(keyVal, timeout);

        // if we didn't get anything, we timed out
        if (output == null)
        {
            Debug.log(this, Debug.ALL_ERRORS,
                      "XMLQueueConsumer timed out waiting for a response.");

            throw new ProcessingException(
                      "XMLQueueConsumer timed out waiting for a response.");
        }
        else
            Debug.log(this, Debug.MSG_STATUS,
                      "XMLQueueConsumer retrieved 1 response.");

        // pass on the message to the next processor(s)
        return formatNVPair(output);
    }

    /**
     * Obtains an XMLMessageParser from our message input.
     *
     * @param input    The message that should be in an XMLMessageParser
     *
     * @return         An XMLMessageParser with the input
     *
     * @exception      MessageException    Thrown if there is an error with
     *                                     the message data.
     * @exception      ProcessingException Thrown if an error occurs while
     *                                     processing the message which is
     *                                     unrelated to the message data
     */
    private XMLMessageParser getParser(MessageObject input)
        throws MessageException, ProcessingException
    {
        // first, get an XML document
        Document doc;
        if (inputName == null)
            doc = input.getDOM();
        else
            doc = input.getDOM(inputName);

        // now put it in a parser
        return new XMLMessageParser(doc);
    }

    /**
     * Retrieves a required property as an integer.
     *
     * @param propName  The name of the property to retrieve
     * @param errorBuff The buffer where a message is written if a required
     *                  property is missing
     *
     * @return          The value of the property as an integer.
     */
    private int getRequiredIntProperty(String propName, StringBuffer errorBuff)
    {
        int intVal = 0;

        // get the property
        String val = getRequiredPropertyValue(propName, errorBuff);

        // if the property is missing, just return 0 as there is already an
        // error message in the buffer
        if (val == null)
            return intVal;

        // convert the property to an integer
        try
        {
            intVal = Integer.parseInt(val);
        }
        catch(NumberFormatException e)
        {
            // indicate the error if the number is formatted incorrectly
            errorBuff.append("The value [").append(val)
                     .append("] for property [").append(propName)
                     .append("] cannot be converted to an integer.");
        }

        return intVal;
    }
}
