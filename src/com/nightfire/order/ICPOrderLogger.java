package com.nightfire.order;

import com.nightfire.framework.order.CHOrderEvalContext;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.order.cfg.OrderLoggerCfg;
import com.nightfire.order.common.CHOrderContext;
import com.nightfire.order.common.CHOrderStatusTransition;
import com.nightfire.order.pojo.CHOrderPojo;
import com.nightfire.order.pojo.CHTxEventHistory;
import com.nightfire.order.pojo.ICPTransPojo;
import com.nightfire.order.pojo.ICPWirelessPortedTn;
import com.nightfire.order.utils.CHOrderConstants;
import com.nightfire.order.utils.CHOrderException;
import biz.neustar.nsplatform.db.pojo.POJOBase;

/**
 * OrderLogger for ICP.
 * 
 * @author Abhishek Jain
 */
public class ICPOrderLogger extends CHOrderLoggerBase 
{
    /**
     * WirelessNportTnPojo identifier.  
     */
    private static final String PORTED_TN = "PortedTN";

    public ICPOrderLogger(OrderLoggerCfg config, CHOrderContext context) throws CHOrderException
    {
        super(config, context);
    }
    /**
     * Get new pojo instance for given pojo type.
     */
    public POJOBase getNewPOJO(CHOrderContext context, String pojoType) throws CHOrderException{

        POJOBase pojoObj = null;

        try{

                // Get new object of ICP trans pojo
                if(POJOTYPE.Trans.toString().equals(pojoType)){
                     pojoObj =  new ICPTransPojo();
                }

                // Get new object of ICP order pojo
                if(POJOTYPE.Event.toString().equals(pojoType)){
                     pojoObj = new CHTxEventHistory();
                    ((CHTxEventHistory)pojoObj).xml_data = context.getInputMessage();
                }

                // Get new object of ICP event history pojo
                if(POJOTYPE.Order.toString().equals(pojoType)){
                     pojoObj = new CHOrderPojo();
                     ((CHOrderPojo)pojoObj).product =  context.getProduct();
                }

                // Check whether sysdate needs to be logged in UTC timezone.
                if(pojoObj != null && context.logSysdateInUTC())
                {
                    if (Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
                        Debug.log(Debug.MSG_LIFECYCLE, "Setting constant on timezone to log sysdate in UTC");

                     pojoObj.useTimeZone(CHOrderConstants.UTC_TIMEZONE);
                }


      }catch(Exception e)
       {
         Debug.error("Failed while creating new pojo object of given pojotype ["+ pojoType +"]  "+ e.getMessage());
         throw new  CHOrderException(e);
       }

       return  pojoObj;
    }

    /**
     * initialize POJOMap with ICP specific POJO classes.
     */
    public void initializePOJOMap(CHOrderContext context) throws CHOrderException
    {
        // sets the POJOMap with appropriate pojo objects.
        try 
        {
            
            CHOrderPojo chOrderPojo = new CHOrderPojo();
            chOrderPojo.product =  context.getProduct();

            CHTxEventHistory eventHistoryPojo = new CHTxEventHistory();
            eventHistoryPojo.xml_data = context.getInputMessage();

            ICPTransPojo icpTransPojo =    new ICPTransPojo();

            if(context.logSysdateInUTC())
            {
                if (Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
                    Debug.log(Debug.MSG_LIFECYCLE, "Setting constant on timezone to log sysdate in UTC");

                chOrderPojo.useTimeZone(CHOrderConstants.UTC_TIMEZONE);
                eventHistoryPojo.useTimeZone(CHOrderConstants.UTC_TIMEZONE);
                icpTransPojo.useTimeZone(CHOrderConstants.UTC_TIMEZONE);
            }
            
            pojoMap.put(POJOTYPE.Order.toString(),chOrderPojo );
            pojoMap.put(POJOTYPE.Event.toString(), eventHistoryPojo );
            pojoMap.put(POJOTYPE.Trans.toString(), icpTransPojo );
            pojoMap.put(PORTED_TN, new ICPWirelessPortedTn());
        } 
        catch (Exception e) 
        {
            throw new CHOrderException("Unable to initialize POJOs,"+e.getMessage());
        }
    }

    @Override
    public void log(CHOrderContext context) throws CHOrderException
    {

        // 1) Fetch the old status
        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "ICPOrderLogger: inside log()");

        CHOrderEvalContext evalContext = new CHOrderEvalContext();
        String[] queryParams = new String[]{CHOrderConstants.ICP_TRANS_REQUESTNUMBER_COL,CHOrderConstants.ICP_TRANS_CUSTOMERID_COL};
        String requestNumber = (String) context.getAttribute(CHOrderConstants.MP_CONTEXT_REQUESTNUMBER);
        String custID = context.getCustomerId();

        if (Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log(Debug.MSG_DATA, "ICPOrderLogger: getting request no ["+requestNumber+"] and customerID ["+custID+"]");

        String[] queryParamsValue = new String[]{requestNumber,custID};

        if (Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log(Debug.MSG_DATA, "ICPOrderLogger: getting old status with paramsNames["+queryParams+"] and paramvalues["+queryParamsValue+"]");

        String oldStatus = null;
        try
        {
            evalContext.initialize(context.getDbConnection(),CHOrderConstants.ICP_TRANS_TABLE_NAME,queryParams,queryParamsValue);
            oldStatus = evalContext.getAttribute(CHOrderConstants.CH_ORDER_STATUS);

            if (Debug.isLevelEnabled(Debug.MSG_DATA))
                Debug.log(Debug.MSG_DATA, "ICPOrderLogger: fetched oldStatus ["+oldStatus+"]");


            // if old status returned is null assign a value none.
            if( !StringUtils.hasValue(oldStatus))
            {
                String txStatus = (String) context.getAttribute("INCOMPLETE_TRANSACTION_STATUS");
                if(!StringUtils.hasValue(txStatus))
                    oldStatus = "none";
                else
                    oldStatus = txStatus;
            }
        }
        catch(FrameworkException fe)
        {
            Debug.error("Failed while orderEvalContext initialization "+fe.getMessage());
            throw new CHOrderException(fe);
        }

        // 2) get the Event Code from the context.
        String eventCode = context.getEventCode();

        if (Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log(Debug.MSG_DATA, "ICPOrderLogger: fetched eventCode["+eventCode+"]");

        // 3) get the Next Status.
        CHOrderStatusTransition statusTransition = CHOrderStatusTransition.getInstance();

        if( !statusTransition.isAlreadyLoaded(context.getProduct()) )
        {
            try
            {
                if (Debug.isLevelEnabled(Debug.MSG_DATA))
                    Debug.log(Debug.MSG_DATA, "ICPOrderLogger: going to load status transition matrix for product["+context.getProduct()+"]");

                statusTransition.load(context.getDbConnection(),context.getProduct());
            }
            catch(Exception fe)
            {
                Debug.error("Failed while loading status transition "+fe.getMessage());
                throw new CHOrderException(fe);
            }
        }

        // 3.1) if next status is invalid

        String newStatus = statusTransition.getNewStatus(oldStatus,eventCode,context.getProduct());

        if (Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log(Debug.MSG_DATA, "ICPOrderLogger: Fetched newStatus ["+newStatus+"]");

        // set IS_VALID_EVENT attribute with respective values.
        boolean isValidEvent = StringUtils.hasValue(newStatus);

        if (Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log(Debug.MSG_DATA, "ICPOrderLogger: isValidEvent["+isValidEvent+"], If valid event occurs store the newStatus otherwise," +
                                        " store the event[i.e. "+eventCode+"] which occured on the Order.");

        context.setAttribute(CHOrderConstants.IS_VALID_EVENT,isValidEvent+"");

        // in case invalid event and it is 'save' then set order status as MessageSubType value.
        if(!isValidEvent && eventCode.equals(CHOrderConstants.SAVE_EVENT)){
            String messageSubType =  (String) context.getAttribute(CHOrderConstants.MP_CONTEXT_MESSAGESUBTYPE);
            context.setAttribute(CHOrderConstants.CH_ORDER_STATUS, messageSubType );
            if (Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
                Debug.log(Debug.MSG_LIFECYCLE, "ICPOrderLogger: finally setting "+CHOrderConstants.CH_ORDER_STATUS + " ["+messageSubType+"]");
        }

        else  // Also, set the Status attribute in the context;
              // here the assumption is if valid event occured store the newStatus otherwise,
              // store the event occured on the Order.
        {
            context.setAttribute(CHOrderConstants.CH_ORDER_STATUS, isValidEvent ? newStatus:eventCode );
            if (Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
                Debug.log(Debug.MSG_LIFECYCLE, "ICPOrderLogger: finally setting "+CHOrderConstants.CH_ORDER_STATUS+" ["+(isValidEvent?newStatus:eventCode)+"]");
        }

        // log the records to order base schema.
        super.log(context);

        // since logging is DONE we need to maintain this incompleted transaction.
        // we are logging the newstatus as oldstatus so that while in case of Sync Response
        // the new status is rememberd as old one.
        if(isValidEvent)
        {
            context.setAttributeInMPC("INCOMPLETE_TRANSACTION_STATUS",newStatus);
        }

    }

}
