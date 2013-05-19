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
import com.nightfire.security.tpr.UOMTradingPartnerInfo;

/**
 * This message-processor fetches TP information from UOM_TRADINGPARTNER_INFO table.
 * using TradingPartner name and customer spid.
 * (Where in UOM_TRADINGPARTNER_INFO TradingPartner name refers to TradingPartnerSPID )
 *
 * @author Narendra Verma
 */
public class UOMTPInfoFetcher extends MessageProcessorBase {

    // Public Static Strings related to Conifigurable properties
    public static final String CUSTOMER_SPID_IN_LOC_PROP = "CUSTOMER_SPID_IN_LOC";
    public static final String TRADING_PARTNER_NAME_IN_LOC_PROP = "TRADING_PARTNER_NAME_IN_LOC";
    public static final String TP_UOM_SOAP_URL_OUT_LOC_PROP = "TP_UOM_SOAP_URL_OUT_LOC";
    public static final String NONBONDED_TP_FAX_NO_OUT_LOC_PROP = "NONBONDED_TP_FAX_NO_OUT_LOC";


    private String tpUOMSOAPURLOutLocation;
    private String tradingPartnerIDLocation;
    private String nonBondedTPFaxNoLocation;
    private String customerSpidLoc;

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

        Debug.log( Debug.SYSTEM_CONFIG, "UOMTPInfoFetcher: Initializing..." );

        /* Initialize a string buffer to contain errors while initializing properties */

        StringBuffer errorMsg = new StringBuffer();
        tradingPartnerIDLocation =getRequiredPropertyValue(TRADING_PARTNER_NAME_IN_LOC_PROP,errorMsg );

        tpUOMSOAPURLOutLocation = getPropertyValue(TP_UOM_SOAP_URL_OUT_LOC_PROP);
        nonBondedTPFaxNoLocation =getPropertyValue(NONBONDED_TP_FAX_NO_OUT_LOC_PROP);
        customerSpidLoc =getPropertyValue(CUSTOMER_SPID_IN_LOC_PROP);


        if ( errorMsg.length() > 0 )
               {
                    String errMsg = errorMsg.toString( );

                    Debug.log( Debug.ALL_ERRORS, errMsg );

                    throw new ProcessingException( errMsg );
               }

        Debug.log( Debug.SYSTEM_CONFIG, "UOMTPInfoFetcher: Initialized");
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
            Debug.log( Debug.MSG_STATUS, "UOMTPInfoFetcher: processing ... " );

            // Get CustomerID from customer context location.
            String customerSpid = getString(customerSpidLoc , mpContext, inputObject );

            //CustomerContext context = CustomerContext.getInstance();

            Debug.log( Debug.MSG_STATUS, "UOMTPInfoFetcher: Got CustomerSPID : "+customerSpid );

            // Get Trading Partner name from context location.
            String tradingPartnerName = getString( tradingPartnerIDLocation, mpContext, inputObject );
            Debug.log( Debug.MSG_STATUS, "UOMTPInfoFetcher: Got tradingPartner ID : "+tradingPartnerName );

            if( !StringUtils.hasValue(customerSpid) || !StringUtils.hasValue(tradingPartnerName))
                throw new ProcessingException("Invalid TradingPartnerId: ["+tradingPartnerName+"] or CustomerSPID: ["+customerSpid+"]");

            UOMTradingPartnerInfo uomTPInfo = UOMTradingPartnerInfo.getInstance();

            String faxNo;
            String uomTpURL;

            if(StringUtils.hasValue(tpUOMSOAPURLOutLocation)) {
                uomTpURL= uomTPInfo.getTpUOMSoapUrl(tradingPartnerName,customerSpid);

                if(StringUtils.hasValue(uomTpURL)){
                    set( tpUOMSOAPURLOutLocation, mpContext, inputObject, uomTpURL );
                    Debug.log( Debug.MSG_STATUS, "UOMTPInfoFetcher: Set TP UOMSoapURL  [" + uomTpURL + "] at location [" + tpUOMSOAPURLOutLocation + "] ");
                }                                                             

            }
            
            if(StringUtils.hasValue(nonBondedTPFaxNoLocation) ){
                faxNo = uomTPInfo.getFaxNumber(tradingPartnerName,customerSpid);
                if(StringUtils.hasValue(faxNo)){
                    set( nonBondedTPFaxNoLocation, mpContext, inputObject, faxNo );
                    Debug.log( Debug.MSG_STATUS, "UOMTPInfoFetcher: Set NonBonded FaxNO  [" + faxNo + "] at location [" + nonBondedTPFaxNoLocation + "] ");
                }

            }
        }
        catch ( Exception e )
        {
            Debug.log( Debug.ALL_ERRORS, "ERROR: Trading Partner UOM SOAP URL could not be obtained:\n" + e.toString() );

            throw new MessageException( e.toString() );
        }

        // Always return input value to provide pass-through semantics.
        return( formatNVPair( inputObject ) );
    }
 }
