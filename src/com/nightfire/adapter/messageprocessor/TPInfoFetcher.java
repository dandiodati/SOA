package com.nightfire.adapter.messageprocessor;

import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.message.MessageException;
import com.nightfire.security.tpr.TradingPartnerRelationship;
import com.nightfire.security.tpr.TPRException;

/**
 * This message-processor fetches Tradingpartner information from TRADINGPARTNER and TRADINGPARTNERRELATIONSHIP table
 * using TradingPartner name and other related attributes.
 *
 * @author Narendra Verma
 */
public class TPInfoFetcher extends MessageProcessorBase {                   

    // Public Static Strings related to Conifigurable properties
    public static final String TRADING_PARTNER_NAME_IN_LOC_PROP = "TRADING_PARTNER_NAME_IN_LOC";
    public static final String INTERFACE_VERSION_IN_LOC_PROP = "INTERFACE_VERSION_IN_LOC";
    public static final String DEFAULT_INTERFACE_VERSION_PROP = "DEFAULT_INTERFACE_VERSION";
    public static final String TRANSACTION_IN_LOC_PROP = "TRANSACTION_IN_LOC";
    public static final String DEFAULT_TRANSACTION_PROP = "DEFAULT_TRANSACTION";
    public static final String TRADINGPARTNER_TYPE_OUT_LOC_PROP = "TRADINGPARTNER_TYPE_OUT_LOC";
    public static final String CUSTOMER_SOAPURL_OUT_LOC_PROP = "CUSTOMER_SOAPURL_OUT_LOC";
    public static final String GATEWAY_SUPPLIER_OUT_LOC_PROP = "GATEWAY_SUPPLIER_OUT_LOC";
    public static final String IS_NONBONDED_TP_PROP = "IS_NONBONDED_TP";


    private String tradingPartnerIDLocation;
    private String isNonBondedLoc;
    private String tradingPartnerTypeLoc;
    private String interfaceVersionLoc;
    private String transactionLoc;
    private String defaultTransaction;
    private String defaultInterfaceVersion;
    private String customerSoapURLLoc;
    private String gatewaySupplierLoc;
    private String cid;

    /**
     * Initializes this object via its persistent properties.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception com.nightfire.common.ProcessingException when initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException {

        // Call base class method to load the properties.
        super.initialize( key, type );

        if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "TPInfoFetcher: Initializing..." );

        /* Initialize a string buffer to contain errors while initializing properties */

        StringBuffer errorMsg = new StringBuffer();

        // Getting value of configured message processor's properties.
        tradingPartnerIDLocation = getRequiredPropertyValue(TRADING_PARTNER_NAME_IN_LOC_PROP, errorMsg );
        tradingPartnerTypeLoc =getPropertyValue(TRADINGPARTNER_TYPE_OUT_LOC_PROP);
        interfaceVersionLoc =getPropertyValue(INTERFACE_VERSION_IN_LOC_PROP);
        defaultInterfaceVersion =getPropertyValue(DEFAULT_INTERFACE_VERSION_PROP);
        transactionLoc =getPropertyValue(TRANSACTION_IN_LOC_PROP);
        defaultTransaction =getPropertyValue(DEFAULT_TRANSACTION_PROP);
        customerSoapURLLoc =getPropertyValue(CUSTOMER_SOAPURL_OUT_LOC_PROP);
        isNonBondedLoc =getPropertyValue(IS_NONBONDED_TP_PROP);
        gatewaySupplierLoc =getPropertyValue(GATEWAY_SUPPLIER_OUT_LOC_PROP);

        if ( errorMsg.length() > 0 )
           {
                String errMsg = errorMsg.toString( );

                Debug.log( Debug.ALL_ERRORS, errMsg );
    
                throw new ProcessingException( errMsg );
           }

        if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "TPInfoFetcher: Initialized.");
    }


    /**
     * Extract date values from the context/input and compare them.
     *
     * @param  mpContext The context
     * @param  inputObject  Input message to process.
     *
     * @return  The given input, or null.
     *
     * @exception  com.nightfire.common.ProcessingException  Thrown if processing fails.
     * @exception com.nightfire.framework.message.MessageException  Thrown if message is bad.
     */
    public NVPair[] process ( MessageProcessorContext mpContext, MessageObject inputObject )
                        throws MessageException, ProcessingException
    {
        if ( inputObject == null )
            return null;


        try
        {
            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log( Debug.MSG_STATUS, "TPInfoFetcher: processing ... " );

            // Get CustomerID from customer context location.
            CustomerContext context = CustomerContext.getInstance();
            cid = context.getCustomerID();

            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log( Debug.MSG_STATUS, "TPInfoFetcher: Got CustomerID : "+cid );

            // Get Trading Partner name from context location.
            String tradingPartnerName = getString( tradingPartnerIDLocation, mpContext, inputObject );

            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log( Debug.MSG_STATUS, "TPInfoFetcher: Got tradingPartnerName : " + tradingPartnerName );

            if(!StringUtils.hasValue(cid) || !StringUtils.hasValue(tradingPartnerName))
                throw new ProcessingException("Invalid TradingPartnerId: ["+tradingPartnerName+"] or CustomerID: ["+cid+"]");

            // Get instance of TradingPartnerRelationship API using customer id.
            TradingPartnerRelationship tpr = TradingPartnerRelationship.getInstance(cid);

            String tradingPartnerType;
            String isNonBonded;
            String transaction= null;

            if (StringUtils.hasValue(customerSoapURLLoc) || StringUtils.hasValue(gatewaySupplierLoc)){
               if (StringUtils.hasValue(transactionLoc))
                    transaction = getString( transactionLoc, mpContext, inputObject );
               else{
                    transaction = defaultTransaction;
                    if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log( Debug.MSG_STATUS, "TPInfoFetcher: Using default Transaction : "+defaultTransaction);
              }
            }

            // Fetch Customer Soap URL and set into context at given location.
            if (StringUtils.hasValue(customerSoapURLLoc))
                setCustomerSoapURL(mpContext, inputObject, tpr , tradingPartnerName, transaction);

            // Fetch Gateway Supplier and set into context at given location.
            if (StringUtils.hasValue(gatewaySupplierLoc))
                 setGatewaySupplier(mpContext, inputObject, tpr , tradingPartnerName, transaction);

            // Fetch TradingPartnerType and set into context at given location.
            if(StringUtils.hasValue(tradingPartnerTypeLoc)){
                tradingPartnerType  = TradingPartnerRelationship.getTradingPartnerType(tradingPartnerName);
                set( tradingPartnerTypeLoc, mpContext, inputObject, tradingPartnerType);
            }

            // Set true or false based on NonBonded TP or Bonded TP respectively at given context location.
            if(StringUtils.hasValue(isNonBondedLoc)){
               isNonBonded = TradingPartnerRelationship.isNonBonded(tradingPartnerName) + "";
               set( isNonBondedLoc, mpContext, inputObject, isNonBonded );

            }

        }
        catch ( Exception e )
        {
            Debug.log( Debug.ALL_ERRORS, "ERROR in TPInfoFetcher: \n " + e.toString() );
            throw new ProcessingException( e.toString() );
        }

        // Always return input value to provide pass-through semantics.
        return( formatNVPair( inputObject ) );
    }

    private void setCustomerSoapURL( MessageProcessorContext mpContext, MessageObject inputObject, TradingPartnerRelationship tpr ,
                                     String tradingPartnerName, String transaction) throws MessageException , ProcessingException{

       String interfaceVersion;

       if (StringUtils.hasValue(interfaceVersionLoc))
                 interfaceVersion = getString( interfaceVersionLoc, mpContext, inputObject );
       else{
               interfaceVersion = defaultInterfaceVersion;
                if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log( Debug.MSG_STATUS, "TPInfoFetcher: Using default InterfaceVersion : "+defaultInterfaceVersion);
       }

       if ( !StringUtils.hasValue(transaction) && !StringUtils.hasValue(interfaceVersion) )
       {
          throw new MessageException (" ERROR:Invalid Trasaction [ " + transaction + "] or InterfaceVersion ["+ interfaceVersion + "], while getting Customer Soap URL");
       }

       String customerSoapURL = tpr.getCustomerSoapUrl(tradingPartnerName , transaction , interfaceVersion);

        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log( Debug.MSG_STATUS, "TPInfoFetcher: CustomerSoap URL found ["+customerSoapURL+"]");

       if(customerSoapURL != null)
         set( customerSoapURLLoc, mpContext, inputObject, customerSoapURL);

    }

    private void setGatewaySupplier( MessageProcessorContext mpContext, MessageObject inputObject, TradingPartnerRelationship tpr ,
                                     String tradingPartnerName , String transaction) throws MessageException , ProcessingException{

         String  gatewaySupplier;

         if ( !StringUtils.hasValue(transaction))
                  throw new MessageException (" ERROR:Invalid Trasaction [ " + transaction + "],  while getting GatewaySupplier");
         try
         {

            gatewaySupplier = tpr.getGatewaySupplier(tradingPartnerName , transaction );

         }
         catch ( TPRException tpre){

             throw new ProcessingException ( tpre.toString() );
         }

         set( gatewaySupplierLoc, mpContext, inputObject, gatewaySupplier);
    }

 }
