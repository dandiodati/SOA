/**
 * Copyright(c) 2000 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.adapter.messageprocessor.queue;

// jdk imports

// third party imports
import org.w3c.dom.Document;

// NightFire imports
import com.nightfire.comms.queue.PCQueueManager;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;

/**
 * XMLQueueProducer is a MessageProtocolAdapter.  It accepts an XML message
 * and uses a value from that message as a key for a PCQueue on which to
 * place the message.
 */
public class XMLQueueProducer extends MessageProcessorBase
{
    /**
     * Name of the property which contains the location of the key value
     * in the XML document.
     */
    private static final String KEY_LOCATION_PROP_NAME = "KEY_LOCATION";

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
     * Default constructor.
     */
    public XMLQueueProducer()
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

        // get our key location in the XML document
        // NOTE: this is the only required property, so an exception is ok
        keyLocation = getRequiredPropertyValue(KEY_LOCATION_PROP_NAME);

        // get our optional input name
        inputName = getPropertyValue(INPUT_NAME_PROP_NAME);
        if (inputName != null)
            inputName = inputName.trim();
    }

    /**
     * Called when the processor should execute a message.  When processing a
     * message, XMLQueueProducer uses a configurable value to retrieve a value
     * from the XML document.  That value is then used as the key it gives to
     * PCQueueManager.
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
             "XMLQueueProducer is attempting to retrieve a queue key from [" +
                  keyLocation + "].");

        // check for existence first, so we can throw a meaningful exception
        // if the value isn't there
        if (!parser.exists(keyLocation))
            throw new MessageException(
                   "XMLQueueProducer failed to find the required location [" +
                   keyLocation + "] in the input message.");

        // now get our value
        String keyVal = parser.getValue(keyLocation);

        Debug.log(this, Debug.MSG_STATUS,
                  "XMLQueueProducer is posting 1 message with the key [" +
                  keyVal + "].");

        // post our message
        PCQueueManager.getInst().enqueue(keyVal, input);

        // pass on the message to the next processor(s)
        return formatNVPair(input);
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

        // in my humble opinion, this MessageObject stuff is way out of control
        if (inputName == null)
            doc = input.getDOM();
        else
            doc = input.getDOM(inputName);

        // now put it in a parser
        return new XMLMessageParser(doc);
    }
}
