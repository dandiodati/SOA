package com.nightfire.adapter.messageprocessor;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.order.CHOrderEvalContext;
import com.nightfire.framework.util.*;
import com.nightfire.framework.rules.ErrorCollection;
import com.nightfire.framework.rules.RuleError;
import com.nightfire.order.common.CHOrderContext;
import com.nightfire.order.common.CHOrderStatusTransition;
import com.nightfire.order.utils.CHOrderConstants;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;

import java.util.StringTokenizer;

/**
 * This message processor is responsible for the determining whether the Order
 * can transit to a new valid status or not with a given event code.
 *
 * @author Abhishek Jain
 */
public class StatusTransitionValidator extends MessageProcessorBase
{
    /**
      * Property indicating the location of event occured.
     */
    public static final String EVENT_CODE_INPUT_LOC_PROP = "EVENT_CODE_INPUT_LOC";
    /**
     * Property indicating the location of Old status.
     */
    public static final String OLD_STATUS_INPUT_LOC_PROP = "OLD_STATUS_INPUT_LOC";
    /**
      * Property indicating the out put location of new status.
     */
    public static final String NEW_STATUS_OUTPUT_LOC_PROP = "NEW_STATUS_OUTPUT_LOC";
   /**
     * Request number location.
     */
    public static final String REQUEST_NUMBER_INPUT_LOC_PROP = "REQUEST_NUMBER_INPUT_LOC";
    /**
     * Output locaion for status transition validation.
     */
    public static final String ERRORS_CONTEXT_LOCATION_PROP = "ERRORS_CONTEXT_LOCATION";
    /**
     * Property indicating product value.
     */
    public static final String PRODUCT_NAME_DEFAULT_VALUE_PROP = "PRODUCT_NAME_DEFAULT_VALUE";
    /**
      * Property indicating whether status transition rule needs to be applied or skipped.
     */
    public static final String ENABLED_PROP = "ENABLED";

    /**
     * If set to true then errors generated will have a HALT_MESSAGE_EXECUTION node whose
     * value would be true. This signifies that 'Bypass Validation' link would not be shown
     * on Validation Error page in case of Status Transition failure error.
     */

    public static final String HALT_MESSAGE_EXECUTION_PROP = "HALT_MESSAGE_EXECUTION";


    /**
     * initialize this message processor.
     * @param key key to identify the message processor.
     * @param type Driver type to identify the message processor type.
     * @throws ProcessingException on some initialization error.
     */
    public void initialize(String key, String type) throws ProcessingException
    {
        super.initialize(key, type);

        if(Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log(Debug.MSG_DATA, "StatusTransitionValidator: inside initialize with parameters... " +
                 "key["+key+"] & type["+type+"]");

        StringBuffer errorBuffer = new StringBuffer( );

        // Required properties
        eventCodeLoc = getRequiredPropertyValue( EVENT_CODE_INPUT_LOC_PROP, errorBuffer );
        errorContextLocation =  getRequiredPropertyValue( ERRORS_CONTEXT_LOCATION_PROP, errorBuffer );
        productDefaultVal =  getRequiredPropertyValue( PRODUCT_NAME_DEFAULT_VALUE_PROP, errorBuffer );

        newStatusOutLoc = getPropertyValue(NEW_STATUS_OUTPUT_LOC_PROP);
        setHaltMsgeExecNode = getPropertyValue(HALT_MESSAGE_EXECUTION_PROP);

        if ( StringUtils.hasValue( setHaltMsgeExecNode ) ){

               try {

                   setHaltMessageExecNode = getBoolean( setHaltMsgeExecNode );
              }
               catch ( FrameworkException e )
               {
                   errorBuffer.append ( "Property value for " + HALT_MESSAGE_EXECUTION_PROP +
                     " is invalid. " + e.getMessage ( ) + "\n" );
               }
         }

        // OLD_STATUS_INPUT_LOC staus is optional property.
        // In case OLD_STATUS_INPUT_LOC presents , REQUEST_NUMBER_INPUT_LOC property would be considered as optional otherwise its required.

        oldStatusLoc = getPropertyValue( OLD_STATUS_INPUT_LOC_PROP);

        String enableValue = getPropertyValue( ENABLED_PROP);

        if ( StringUtils.hasValue( enableValue ) ){

                   try {

                       enable = getBoolean( enableValue );
                  }
                   catch ( FrameworkException e )
                   {
                       errorBuffer.append ( "Property value for " + ENABLED_PROP +
                         " is invalid. " + e.getMessage ( ) + "\n" );
                   }
         }


        if(StringUtils.hasValue(oldStatusLoc))
            requestNumberLoc = getPropertyValue( REQUEST_NUMBER_INPUT_LOC_PROP );
        else
            requestNumberLoc  = getRequiredPropertyValue( REQUEST_NUMBER_INPUT_LOC_PROP, errorBuffer);

        if ( errorBuffer.length() > 0 )
        {
            String errMsg = errorBuffer.toString( );
            Debug.log( Debug.ALL_ERRORS, errMsg );
            throw new ProcessingException( errMsg );
        }

        if(Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log(Debug.MSG_DATA, "StatusTransitionValidator: done with initialization...");

    }

    /**
     * process the message processor, request number and event code are expected in message
     * processor context.
     * This process determines whether the given order can transit to new
     * valid status on the given event, and sets the validation results back in the context.
     *
     * @param mpcontext message processor context that holds are the parameters.
     * @param input message object.
     * @return
     * @throws MessageException
     * @throws ProcessingException
     */
    public NVPair[] process(MessageProcessorContext mpcontext, MessageObject input)
    throws MessageException, ProcessingException
    {
        if(input == null){
            return null;
        }

        if(Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log(Debug.MSG_DATA, "StatusTransitionValidator: Apply Status Transtion validation [ "+ enable+ "]");

        // If ENABLED property is 'false' then state tranistion validation is skipped.     
        if(! enable) {
            return ( formatNVPair( input ) );
        }


        String  oldStatus = null;
        String  eventCode = getString (eventCodeLoc, mpcontext, input);

        if( !StringUtils.hasValue(eventCode))
             throw new ProcessingException("Invalid EventCode ["+eventCode+"], could not process StatusTransitionValidator message processor.");

		oldStatus = getOldStatus(mpcontext, input);
                                  

        // get the Next Status.
        CHOrderContext context = new CHOrderContext(mpcontext, true);
        CHOrderStatusTransition statusTransition = CHOrderStatusTransition.getInstance();
        if( !statusTransition.isAlreadyLoaded(productDefaultVal) )
        {
            try
            {
                if(Debug.isLevelEnabled(Debug.MSG_DATA))
                    Debug.log(Debug.MSG_DATA, "StatusTransitionValidator: going to load status transition matrix for product["+productDefaultVal+"]");
                statusTransition.load(context.getDbConnection(),productDefaultVal);
            }
            catch(Exception fe)
            {
                Debug.error("Failed while loading status transition "+fe.getMessage());
                Debug.error(Debug.getStackTrace(fe));
                throw new ProcessingException(fe);
            }
        }

        String newStatus = statusTransition.getNewStatus(oldStatus, eventCode, productDefaultVal);

        if(Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log(Debug.MSG_DATA, "StatusTransitionValidator: Fetched newStatus ["+newStatus+"]");

        if(StringUtils.hasValue(newStatus))
        {
            if(Debug.isLevelEnabled(Debug.MSG_DATA))
                Debug.log(Debug.MSG_DATA, "StatusTransitionValidator: Status transition rule validated successfully");
        }

        else {

            ErrorCollection errors = null;

            if(!setHaltMessageExecNode)
                errors = new ErrorCollection();
            else
                errors = new ErrorCollection(true);

            RuleError ruleError = new RuleError(RULE_ID, getErrorMsg(eventCode, oldStatus));

            errors.addError(ruleError);

            if(Debug.isLevelEnabled(Debug.MSG_DATA))
                Debug.log(Debug.MSG_DATA, "StatusTransitionValidator: Status transition validation failed because of invalid event");

            mpcontext.set(errorContextLocation, errors.toXML() );
            
        }

        if(StringUtils.hasValue(newStatusOutLoc)  && StringUtils.hasValue(newStatus))
             set(newStatusOutLoc, mpcontext, input, newStatus );

        return ( formatNVPair( input ) );

    }


	private String getOldStatus(MessageProcessorContext mpcontext, MessageObject input)
		throws ProcessingException, MessageException
	{

		String oldStatus = null;
		if(StringUtils.hasValue(oldStatusLoc))
		{
            // get the oldStatus from the context if already present.
            oldStatus = getString (oldStatusLoc, mpcontext, input);
        }

        // if oldStatus is not found find it with help of EvalContext.
        if(!StringUtils.hasValue(oldStatus))
		{
            String requestNumber = null;
            //get request number either from context or from specified path locations, to fetch old status.
            if(requestNumberLoc.indexOf("@") != -1)
                requestNumber = getString (requestNumberLoc, mpcontext, input);
            else
                requestNumber= (String) getValue( requestNumberLoc, mpcontext, input);

             if(!StringUtils.hasValue(requestNumber))
                    throw new ProcessingException("Invalid RequestNumber ["+requestNumber+"], could not process StatusTransitionValidator message processor.") ;

			CHOrderContext context = new CHOrderContext(mpcontext, true);

            if(Debug.isLevelEnabled(Debug.MSG_DATA))
			     Debug.log(Debug.MSG_DATA, "In case oldStatus couldn't be fetched from context, fetch it from SEA schema");

            try
	        {
                String custID =  CustomerContext.getInstance().getCustomerID();

                // get the current status of the order identified by the request number.
                String[] queryParams = new String[]{CHOrderConstants.ICP_TRANS_REQUESTNUMBER_COL,CHOrderConstants.ICP_TRANS_CUSTOMERID_COL};
                String[] queryParamsValue = new String[]{requestNumber,custID};

                CHOrderEvalContext evalContext = new CHOrderEvalContext();
                oldStatus = null;

			    if(Debug.isLevelEnabled(Debug.MSG_DATA))
				    Debug.log(Debug.MSG_DATA, "StatusTransitionValidator: getting current status with parameters request no ["+requestNumber+"] and customerID ["+custID+"]");

	            evalContext.initialize(context.getDbConnection(),CHOrderConstants.ICP_TRANS_TABLE_NAME,queryParams,queryParamsValue);
	            oldStatus = evalContext.getAttribute(CHOrderConstants.CH_ORDER_STATUS);

                // if old status returned is null assign a value none.
	            if( !StringUtils.hasValue(oldStatus))
				    oldStatus = "none";

	            if(Debug.isLevelEnabled(Debug.MSG_DATA))
				    Debug.log(Debug.MSG_DATA, "StatusTransitionValidator: fetched current status ["+oldStatus+"]");

	        }
	        catch(FrameworkException fe)
	        {
			    Debug.error("Failed while orderEvalContext initialization "+fe.getMessage());
	            Debug.error(Debug.getStackTrace(fe));
	            throw new ProcessingException(fe);
	        }

		}

        return oldStatus;
	}
    /**
      * Extract first text value available from the given locations.
      *
      * @param  locations  A set of '|' delimited XML locations to check.
      * @param  mpContext The context
      * @param  inputObject  Input message to process.
      *
      * @return  The requested value.
      *
      * @exception  MessageException  Thrown on non-processing errors.
      * @exception  ProcessingException  Thrown if processing fails.
      */
     protected Object getValue ( String locations,
                                 MessageProcessorContext mpContext,
                                 MessageObject inputObject )
                                 throws MessageException,
                                        ProcessingException
     {
         // check for the possibility of multiple paths
         StringTokenizer st = new StringTokenizer( locations,
                                                   MessageProcessorBase.SEPARATOR );

         Object result = null;

         // for each path and while we have not yet found a result
         while ( st.hasMoreTokens() && result == null)
         {

             String path = st.nextToken( );

             if(Debug.isLevelEnabled(Debug.MSG_DATA))
                Debug.log( Debug.MSG_DATA, "Checking location [" + path + "] for value ..." );

             // this first checks to see if the value exists if the
             // input message or context
             if ( exists( path, mpContext, inputObject, false) ){

                // try to get the text value from the input message
                if (!path.startsWith(MessageProcessorBase.CONTEXT_START)) {

                    if(Debug.isLevelEnabled(Debug.MSG_DATA))
                       Debug.log(Debug.MSG_DATA,  "Checking location [" + path + "] for text value ...");

                   XMLMessageParser inputMessage =
                      new XMLMessageParser(inputObject.getDOM());

                   if (inputMessage.textValueExists(path)) {
                      result = inputMessage.getTextValue(path);
                      
                      if(Debug.isLevelEnabled(Debug.MSG_DATA))
                          Debug.log(Debug.MSG_DATA,  "Found text value [" + result + "]");
                   }
                   // checking if value attribute exists or not
                   else if (! inputMessage.valueExists(path))
                   {
                        if(Debug.isLevelEnabled(Debug.MSG_DATA))
                            Debug.log(Debug.MSG_DATA,
                                "Node does not have  textvalue and 'value' attribute. [" + result + "]");

                        return null;
                   }

                }

             }

         }

         return result;

     }

    private String getErrorMsg(String eventCode, String oldStatus)
    {
        return   "[ " + eventCode + " ] is not allowed on top of [ "+ oldStatus + " ].";

    }

    private String eventCodeLoc;
    private String oldStatusLoc;
    private String requestNumberLoc;
    private String errorContextLocation;
    private String productDefaultVal;
    private String newStatusOutLoc;
    private Boolean enable = true;
    private String setHaltMsgeExecNode = null;
    private boolean setHaltMessageExecNode = false;

    private static final String RULE_ID = "InputDataValidationError";
}
