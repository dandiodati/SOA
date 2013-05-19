/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.framework.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.nightfire.framework.db.*;

/**
 * This class gives a general way of retrieving required and optional properties
 * 
 */

public class PropUtils
{

  private static String RETURN = System.getProperty("line.separator");

  private static final String[] PASSWORD_PROP_ARRAY = {"PASSWORD", "Password", "password", "PASSPHRASE", "PASSWD", "Passwd", "passwd"};
  private static final String EQUAL_PROP = "=";
  private static final String COMMA_PROP = ",";
  private static final String PROTECTED_PASSWORD_VALUE = "****";

  private PropUtils()
  {
  }

  /**
   * Retrieves a required property from a property map.
   *
   * @param properties The properties map to use.
   * @param propName The property whose value is to be returned
   * @return String Value of propName
   *
   * @exception FrameworkException Thrown if property does not exist
   */
  public static String getRequiredPropertyValue ( Map properties,String propName ) throws FrameworkException
  {
    String propValue = getPropertyValue ( properties, propName );
    if ( ! ( StringUtils.hasValue ( propValue ) ) )
    {
      throw new FrameworkException ("Required property \'" + propName + "\' is missing or has no value " + RETURN);
    }
    else
    {
      return ( String ) propValue;
    }
  }

  /**
   * Retrieves a required property from a property map.
   *
   * @param properties The properties map to use.
   * @param propName The property whose value is to be returned
   *
   * @param errorMsg Container for any errors that occur during proessing. Error
   *  messages are appended to this container instead of throwing exceptions
   *
   * @return String Value of propName
   */
  public static String getRequiredPropertyValue ( Map properties, String propName, StringBuffer errorMsg )
  {
    String propValue = null;
    try
    {
      propValue = getRequiredPropertyValue ( properties, propName );
    }
    catch ( FrameworkException pe)
    {
      errorMsg.append (pe.getMessage());
    }
    return propValue;
  }

  /** 
   * Return the value for the property propName from the properties map.
   *
   * @param properties The properties map to use.
   * @param propName The property whose value is to be returned
   *
   * @return String Value of propName
   */
  public static String getPropertyValue ( Map properties, String propName )
  {
    return ( ( String ) properties.get ( propName ) );
  }


  /** 
   * Return the value for the property propName from the properties map.
   * If the value is null or is an empty string, returns defaultValue.
   *
   * @param properties The properties map to use.
   * @param propName The property whose value is to be returned
   * @param defaultValue default string to return if the property is empty or null.
   *
   * @return String Value of propName or defaultValue if propName is null or has an empty value.
   */
  public static String getPropertyValue ( Map properties, String propName, String defaultValue )
  {
     String  temp = ( String ) properties.get ( propName );

     if (!StringUtils.hasValue(temp) )
       temp = defaultValue;

     return ( temp);
  }


    /**
     * Get the list of name-value properties associated with 'key' and 'type' value with the values from the propType 
     * overwriting the the defaultPropType.
     *
     * @param  propKey   The property-key name.
     * @param  propType  The property-type name.
     * @param  defaultPropType  The deafult properties type that will be overwritten.
     *
     * @return  List of name-value properties.
     *
     * @exception  FrameworkException  Thrown if requested item can't be found.
     */
    public static Properties getProperties ( String propKey, String propType, String defaultPropType ) throws FrameworkException
    {
        if (Debug.isLevelEnabled(Debug.DB_DATA))
        {
            Debug.log(Debug.DB_DATA, "PropUtils.getProperties(" + propKey +
                ", " + propType + ", " + defaultPropType + ") called.");
        }
        
        // Get the property-keys item from default property type.
        Map defaultProps = PersistentProperty.getProperties( propKey, defaultPropType );

        // Get the property-types item from primary property type.
        Map properties = PersistentProperty.getProperties( propKey, propType );

        return mergeMap(properties, defaultProps);

    }
    
    
    /**
     * Create a list of Properties containing all keys from two lists of given Propertiess. 
     * For keys existing in both lists,
     * the values in the first list overwrites the second list.
     *
     * @param  primaryValues   The Properties containing primary values.
     * @param  defaultValues  The Properties containing default values.
     *
     * @return  List of properties.
     *
     */
    public static Properties mergeMap ( Map primaryValues, Map defaultValues )
    {
        Properties result = new Properties();
        
        // Copy all default values
        if (defaultValues != null)
        {
            result.putAll(defaultValues);
        }

        // Overwrite it with the primary values.
        if (primaryValues != null)
        {
            result.putAll(primaryValues);
        }

        return result;
    }
    
    
    /**
     * Append given value with the current cid.
     *
     * @param  value  The value to be appended.
     *
     * @return  The property value, or null if absent.
     *
     * @exception FrameworkException Thrown if property does not exist
     */
    public static String appendCustomerId ( String value ) throws FrameworkException
    {
        return appendCustomerId ( value, CustomerContext.getInstance().getCustomerID() );
    }


    /**
     * Append given value with the given cid.
     *
     * @param  value  The value to be appended.
     * @param  cid  The iteration count.
     *
     * @return  The property value, or null if absent.
     */
    public static String appendCustomerId ( String value, String cid )
    {
        if (!StringUtils.hasValue(cid) || cid.equals(CustomerContext.DEFAULT_CUSTOMER_ID))
        {
            return value;
        }
        
        String suffix = "_" + cid;
        
        if (value != null && !value.endsWith(suffix))
        {
            return value + suffix;
        }
        else
        {
            return value;
        }
        
    }

    /**
    * Suppress passwords from properties
    */
    public static String suppressPasswords(String input) {
        if (!StringUtils.hasValue(input))
            return input;

        StringBuilder sbInput = new StringBuilder(input);

        int sbLength = sbInput.length();

        for (String key : PASSWORD_PROP_ARRAY) {
            if (sbInput.indexOf(key) == -1)
                continue;

            for (int keyIndex = 0; (keyIndex >= 0 && keyIndex < sbLength); keyIndex++) {

                keyIndex = sbInput.indexOf(key, keyIndex);

                if (keyIndex == -1)
                    break;

                int equalIndex = sbInput.indexOf(EQUAL_PROP, keyIndex);
                int commaIndex = sbInput.indexOf(COMMA_PROP, keyIndex);

                if (equalIndex == -1)
                    continue;

                if (equalIndex < commaIndex || (commaIndex == -1 && equalIndex < sbLength - 1)) {

                    int keyLength = key.length();

                    String test = sbInput.substring(keyIndex, equalIndex);
                    int pwStringLength = sbInput.substring(keyIndex, equalIndex).trim().length();

                    if (keyLength == pwStringLength) {
                        if (commaIndex == -1)
                            sbInput.delete(equalIndex + 1, sbLength - 1);
                        else
                            sbInput.delete(equalIndex + 1, commaIndex);

                        sbInput.insert(equalIndex + 1, PROTECTED_PASSWORD_VALUE);
                    }
                }
            }
        }

        return sbInput.toString();
    }


    /**
     * If the propertyname is password related then return protected password value
     */
    public static String suppressPasswordValue( String propName, String value )
    {
        if ( ! StringUtils.hasValue(propName) )
            return value;

        for ( String passName: PASSWORD_PROP_ARRAY )
        {
            if ( propName.indexOf(passName) != -1 )
                return PROTECTED_PASSWORD_VALUE;
        }

        return value;
    }

    public static void main (String [] args)
    {
        String myStr = "{COMMON_PROPERTIES={wireless_nport_save={DRIVER_TYPE=UOM_Save_Chain, DRIVER_KEY=UOM_ICP_Common_OBF50}, vze_lsog9_lsr_order={PASSWD=ABCDe, LOG_FILE=vze_lsog9_lsr_order.log, MAX_DEBUG_WRITES=100000, MAX_LOG_FILE_COUNT=10}, clearing_house_validate={KEY_PASSPHRASE=ieieie, LOG_FILE=clearing_house_validate.log, KEYSTORE_PASSWORD=mxmxmxm, MAX_LOG_FILE_COUNT=10}, wireless_nport_validate={PASSPHRASE=oeoeoeoe, DRIVER_KEY=UOM_ICP_Common_OBF50}, rim_lsr_request={DEBUG_LOG_LEVELS=ALL, MAX_LOG_FILE_COUNT=5}, att_lsr_order={LOG_FILE=att_lsr_order.log}, COMMON_PROPERTIES={ORBagentPort=30005, MAX_DBCONNECTION_WAIT_TIME=60, REQUEST_ID_SEQUENCE_NM=CH_MSG_SEQ, PROCESS_INFO_CLASS_NAME=com.nightfire.framework.util.OSProcessInfo, GWS_URL_WIRELESS_NPORT=http://192.168.101.16:18004/axis/services/WSRequestHandlerPort, DEBUG_LOG_LEVELS=ALL, GWS_URL_LSR_ORDER=http://192.168.99.29:5555/axis/services/WSRequestHandlerPort, ORBagentAddr=192.168.101.17, REQUEST_ID_INCR_COUNT=100, VALIDATE_CONNECTION_USING_SQL=FALSE, MAX_DBCONNECTION_SIZE=100, SMTP_SERVER=192.168.101.17, PEP_END_POINT_URL=http://192.168.64.229:4081/axis/services/RequestHandlerPort, GWS_URL=http://192.168.99.29:1090/axis/services/WSRequestHandlerPort, GWS_URL_RECEIVE_WIRELESS_NPORT=http://192.168.101.16:18004/axis/services/WSRequestHandlerPort, MAX_DEBUG_WRITES=50000}, clearing_house_save={LOG_FILE=clearing_house_save.log, DEBUG_LOG_LEVELS=ALL, MAX_DEBUG_WRITES=100000, MAX_LOG_FILE_COUNT=10}, vze_lsr_order_connectivity={DEBUG_LOG_LEVELS=ALL, LOG_FILE=vze_lsr_order_connectivity.log, MAX_DEBUG_WRITES=100000, MAX_LOG_FILE_COUNT=10}, rim_lsr_response={DEBUG_LOG_LEVELS=ALL, MAX_LOG_FILE_COUNT=5}, wireless_nport_send={DRIVER_TYPE=UOM_Send_Chain, DRIVER_KEY=UOM_ICP_Send_OBF50}, receive_wireless_nport_save={DRIVER_TYPE=UOM_Save_Chain, DRIVER_KEY=UOM_ICP_Common_OBF50}, receive_wireless_nport_receive={DRIVER_TYPE=UOM_Receive_Chain, DRIVER_KEY=UOM_ICP_Receive_OBF50}, receive_wireless_nport_validate={DRIVER_TYPE=UOM_Validate_Chain, DRIVER_KEY=UOM_ICP_Common_OBF50}}, ASRPreOrder={IsBrandedSupplier={DEFAULT_NEXT_PROCESSOR=PopulateSupplier, NEXT_PROCESSOR_0=GetCCNA, NEXT_PROCESSOR_NAME=GetCCNA|PopulateSupplier, NAME=IsBrandedSupplier, TEST_0=@context.BRANDED_TYPE=TRUE}, ResponseNFLogger={OPTIONAL_2=FALSE, LOCATION_11=ResponseHeader.IRM, OPTIONAL_1=FALSE, LOCATION_10=ResponseHeader.IRI, LOCATION_7=INPUT_MESSAGE, OPTIONAL_0=FALSE, LOCATION_6=ResponseHeader.CC, LOCATION_5=ResponseHeader.CCNA, LOCATION_4=ResponseHeader.MESSAGE_ID, TRANSACTIONAL_LOGGING=TRUE, LOCATION_3=ResponseHeader.ResponseType, LOCATION_2=@context.REQUEST_HEADER.Supplier, LOCATION_1=@context.PREORDER_SEQUENCE, COLUMN_11=IRM, COLUMN_10=IRI, COLUMN_9=ACTION, SEPARATOR=|, COLUMN_8=APPLYBUSINESSRULES, COLUMN_7=MESSAGE, COLUMN_6=CC, COLUMN_5=CCNA, NEXT_PROCESSOR_NAME=NOBODY, COLUMN_4=MESSAGEID, COLUMN_3=MESSAGETYPE, COLUMN_2=SUPPLIER, COLUMN_1=ASRMESSAGEKEY, COLUMN_0=CREATED, DATE_FORMAT_0=MM-dd-yyyy, NAME=ResponseNFLogger, DEFAULT_9=receive, DEFAULT_8=1, TABLE_NAME=SEND_ASR_PREORDER_MESSAGE, DEFAULT_0=SYSDATE, COLUMN_TYPE_7=TEXT_BLOB, COLUMN_TYPE_0=DATE, OPTIONAL_9=FALSE, OPTIONAL_8=FALSE, OPTIONAL_11=TRUE, OPTIONAL_7=TRUE, OPTIONAL_10=TRUE, OPTIONAL_6=TRUE, OPTIONAL_5=FALSE, OPTIONAL_4=FALSE, OPTIONAL_3=FALSE}, SetSupplierType={NEXT_PROCESSOR_NAME=IsBrandedSupplier, NAME=SetSupplierType, TEST_8=@context.REQUEST_HEADER.Supplier=Frontier, TEST_7=@context.REQUEST_HEADER.Supplier=VZNC, TEST_6=@context.REQUEST_HEADER.Supplier=ATT, TEST_5=@context.REQUEST_HEADER.Supplier=FPC, RESULT_VALUE_8=FALSE, TEST_4=@context.REQUEST_HEADER.Supplier=HAWAIITEL, RESULT_VALUE_7=FALSE, TEST_3=@context.REQUEST_HEADER.Supplier=VZW, RESULT_VALUE_6=FALSE, TEST_2=@context.REQUEST_HEADER.Supplier=BAS, RESULT_VALUE_5=FALSE, TEST_1=@context.REQUEST_HEADER.Supplier=BAN, RESULT_LOCATION=@context.BRANDED_TYPE, RESULT_VALUE_4=TRUE, TEST_0=@context.REQUEST_HEADER.Supplier=QWEST, RESULT_VALUE_3=FALSE, RESULT_VALUE_2=FALSE, RESULT_VALUE_1=FALSE, RESULT_VALUE_0=FALSE}, GetReqID={SEQ_NAME=ASRPREORDERMSGKEY, NEXT_PROCESSOR_NAME=ReqNFLogger, SEQ_VAL_LOCATION=@context.PREORDER_SEQUENCE, MIN_SEQ_VAL_LENGTH=3, NAME=GetReqID}, SetATTApplicationType={RESULT_VALUE_2=SA, RESULT_VALUE_1=CFA, RESULT_VALUE_0=LOC, TEST_2=@context.RequestType=SvcAvailInquiry, TEST_1=@context.RequestType=CFAInquiry, NEXT_PROCESSOR_NAME=SetEndPointUrlLoc, TEST_0=@context.RequestType=LocationInquiry, NAME=SetATTApplicationType, RESULT_LOCATION=@context.APPLICATION_TYPE}, SPISERVER={SMTP_SERVER=192.168.101.17, MAX_DBCONNECTION_SIZE=100, MAX_DBCONNECTION_WAIT_TIME=60, PROCESS_INFO_CLASS_NAME=com.nightfire.framework.util.OSProcessInfo, GWS_URL_LSR_ORDER=http://192.168.99.29:5555/axis/services/WSRequestHandlerPort, GWS_URL=http://192.168.99.29:1090/axis/services/WSRequestHandlerPort, LOG_FILE=/export/home/qa_comm/nfi/logs/asrpreorder.log, NAME=Nightfire.SPIServer.ASRPreOrder, DEBUG_LOG_LEVELS=All, GWS_URL_RECEIVE_WIRELESS_NPORT=http://192.168.101.16:18004/axis/services/WSRequestHandlerPort, ORBagentAddr=192.168.101.17, VALIDATE_CONNECTION_USING_SQL=FALSE, ORBagentPort=30005, SUPERVISOR_KEY=ASRPreOrder, MAX_DEBUG_WRITES=50000, REQUEST_ID_SEQUENCE_NM=CH_MSG_SEQ, PEP_END_POINT_URL=http://192.168.64.229:4081/axis/services/RequestHandlerPort, SUPERVISOR_TYPE=SUPERVISOR, REQUEST_ID_INCR_COUNT=100, GWS_URL_WIRELESS_NPORT=http://192.168.101.16:18004/axis/services/WSRequestHandlerPort}, PopulateSupplier={TARGET_NODE_NAME_0=Supplier, NEXT_PROCESSOR_NAME=RuleProcessor, VALUE_SOURCE_LOCATION_0=@context.REQUEST_HEADER.Supplier, TARGET_NODE_ATTRIBUTE_NAME_0=value, OPTIONAL_0=false, NAME=PopulateSupplier}, IsBranded={DEFAULT_NEXT_PROCESSOR=NFtoUOMMapper, NEXT_PROCESSOR_0=SetCIDAsILEC, NEXT_PROCESSOR_NAME=NFtoUOMMapper|SetCIDAsILEC, NAME=IsBranded, TEST_0=@context.BRANDED_TYPE=TRUE}, RemoveNewLineChar={STYLE_SHEET_IS_COMPILED=FALSE, NEXT_PROCESSOR_NAME=UOMtoNFMapper, INPUT_LOCATION=INPUT_MESSAGE, NAME=RemoveNewLineChar, OUTPUT_LOCATION=INPUT_MESSAGE, STYLE_SHEET=./gateways/asr-send-preorder/runtime/xsl/RemoveNewLineChar.xsl}, VZSOAPClient={END_POINT_URL_LOC=@context.END_POINT_URL_LOC, KEYSTORE_ALIAS=srtest, CRYPTO_ALIAS_PASSWORD=security, NEXT_PROCESSOR_NAME=SOAPRequestFailed, KEYSTORE_FILE=/export/home/qa_comm/nfi/Security/keystore, SOAP_RESPONSE_LOC=INPUT_MESSAGE, NAME=VZSOAPClient, TML_APPLICATIONTYPE=@context.RequestType, KEYSTORE_PASSWORD=security, TML_TO=Verizon, TML_RETRYCOUNT=0, TOKEN_PASSWORD=NEUSTAR_T1, CRYPTO_KEYSTORE_TYPE=jks, CRYPTO_PROVIDER=org.apache.ws.security.components.crypto.Merlin, TOKEN_USERNAME=NEUSTARUOM, TML_FROM=NUS, SOAP_REQUEST_LOC=INPUT_MESSAGE, ASR_MSGKEY=@context.PREORDER_SEQUENCE}, FPCSoapClient={SSL_PROVIDER=com.sun.net.ssl.internal.ssl.Provider(), END_POINT_URL=http://192.168.101.15:8272/axis/services/asrport, NEXT_PROCESSOR_NAME=FPCNSRemover, KEYSTORE_TYPE=jks, SOAP_RESPONSE_LOC=INPUT_MESSAGE, KEYSTORE_PASSWORD=security, SOAP_REQUEST_LOC=INPUT_MESSAGE, KEYSTORE_FILE=/export/home/qa_comm/nfi/Security/keystore, NAME=FPCSoapClient}, CommonExceptionGenerator={XSL_JAR_FILE=/export/home/qa_comm/nfi/gateways/asr-send-preorder/lib/maps.jar, NEXT_PROCESSOR_NAME=ExceptionGenerator, STYLE_SHEET_IS_COMPILED=TRUE, STYLE_SHEET_LOCATION=@context.ExceptionMapClass, INPUT_LOCATION=INPUT_MESSAGE, NAME=CommonExceptionGenerator, OUTPUT_LOCATION=INPUT_MESSAGE, TRANSFORM_AS_STRING=TRUE}, GetCCNA={SQL_QUERY_STATEMENT=SELECT COUNT(0) FROM DOMAIN_CCNA  WHERE CCNA =?, INPUT_DATE_FORMAT_0=MM-dd-yyyy, LOCATION_0=RequestHeader.CCNA, OUTPUT_DATE_FORMAT=MM-dd-yyyy, NEXT_PROCESSOR_NAME=ValidateCCNA, USE_CUSTOMER_ID=true, TABLE_NAME=DOMAIN_CCNA, NAME=GetCCNA, RESULT_LOCATION=@context.hasCCNA}, RespNFLogger={LOCATION_11=ResponseHeader.IRM, OPTIONAL_2=FALSE, LOCATION_10=ResponseHeader.IRI, OPTIONAL_1=FALSE, LOCATION_7=INPUT_MESSAGE, LOCATION_6=ResponseHeader.CC, OPTIONAL_0=FALSE, LOCATION_5=ResponseHeader.CCNA, LOCATION_4=ResponseHeader.MESSAGE_ID, TRANSACTIONAL_LOGGING=TRUE, LOCATION_3=ResponseHeader.ResponseType, LOCATION_2=@context.REQUEST_HEADER.Supplier, LOCATION_1=@context.PREORDER_SEQUENCE, COLUMN_12=REGION, COLUMN_11=IRM, COLUMN_10=IRI, SEPARATOR=|, COLUMN_9=ACTION, COLUMN_8=APPLYBUSINESSRULES, COLUMN_7=MESSAGE, COLUMN_6=CC, COLUMN_5=CCNA, NEXT_PROCESSOR_NAME=COMM_SERVER, COLUMN_4=MESSAGEID, COLUMN_3=MESSAGETYPE, COLUMN_2=SUPPLIER, COLUMN_1=ASRMESSAGEKEY, COLUMN_0=CREATED, DATE_FORMAT_0=MM-dd-yyyy, NAME=RespNFLogger, DEFAULT_9=receive, DEFAULT_8=1, TABLE_NAME=SEND_ASR_PREORDER_MESSAGE, DEFAULT_0=SYSDATE, COLUMN_TYPE_7=TEXT_BLOB, COLUMN_TYPE_0=DATE, OPTIONAL_9=FALSE, OPTIONAL_12=TRUE, OPTIONAL_8=FALSE, OPTIONAL_11=TRUE, OPTIONAL_7=TRUE, OPTIONAL_10=TRUE, OPTIONAL_6=TRUE, OPTIONAL_5=FALSE, OPTIONAL_4=FALSE, LOCATION_12=@context.REGION.REGION, OPTIONAL_3=FALSE}, SetEndPointURLLocation={RESULT_VALUE_0=http://192.168.97.5:8080/axis/services/CfaInquiryServicePort, RESULT_LOCATION=@context.END_POINT_URL_LOC, NEXT_PROCESSOR_NAME=VZNSGenerator, TEST_1=@context.RequestType=LocationInquiry, NAME=SetEndPointURLLocation, TEST_0=@context.RequestType=CFAInquiry, RESULT_VALUE_1=http://192.168.97.5:8080/axis/services/LInquiryServicePort}, ReqSupplierRemover={XSL_JAR_FILE=/export/home/qa_comm/nfi/gateways/asr-send-preorder/lib/maps.jar, STYLE_SHEET_IS_COMPILED=TRUE, NEXT_PROCESSOR_NAME=SetSupplierAlias, INPUT_LOCATION=INPUT_MESSAGE, NAME=ReqSupplierRemover, OUTPUT_LOCATION=INPUT_MESSAGE, STYLE_SHEET=com.nightfire.asr_preorder.RemoveSupplierNode}, SyncRequestDriver={CLASS_58=com.nightfire.adapter.messageprocessor.ConditionTester, TYPE_17=NFtoUOMMapper, CLASS_57=com.nightfire.adapter.messageprocessor.XSLTMessageProtocolAdapter, TYPE_16=IsRegionExist, CLASS_56=com.nightfire.asrpreorder.comms.soap.FrontierSoapClient, TYPE_15=IsBrandedSupplier, CLASS_55=com.nightfire.adapter.messageprocessor.XSLTMessageProtocolAdapter, TYPE_14=IsBranded, CLASS_54=com.nightfire.asrpreorder.comms.soap.VZSOAPMessagingClient, TYPE_13=IsATT, CLASS_53=com.nightfire.adapter.messageprocessor.XSLTMessageProtocolAdapter, TYPE_12=GetWSP, CLASS_52=com.nightfire.adapter.messageprocessor.ConditionTester, TYPE_11=GetRespID, CLASS_51=com.nightfire.adapter.messageprocessor.ConditionTester, TYPE_10=GetRequestInfo, CLASS_50=com.nightfire.adapter.messageprocessor.XSLTMessageProtocolAdapter, ASYNC_FLAG=false, CLASS_49=com.nightfire.adapter.messageprocessor.TestAndRoute, CLASS_48=com.nightfire.asrpreorder.comms.soap.VZSOAPMessagingClient, CLASS_47=com.nightfire.adapter.messageprocessor.XSLTMessageProtocolAdapter, CLASS_46=com.nightfire.adapter.messageprocessor.XSLTMessageProtocolAdapter, CLASS_45=com.nightfire.adapter.messageprocessor.MessageExceptionThrower, CLASS_44=com.nightfire.adapter.messageprocessor.TestAndRoute, CLASS_43=com.nightfire.adapter.messageprocessor.TestAndRoute, CLASS_42=com.nightfire.adapter.messageprocessor.ConditionTester, CLASS_41=com.nightfire.adapter.messageprocessor.ConditionTester, CLASS_40=com.nightfire.adapter.messageprocessor.StringConcatenator, CLASS_39=com.nightfire.adapter.messageprocessor.XSLTMessageProtocolAdapter, CLASS_38=com.nightfire.adapter.messageprocessor.ConditionTester, CLASS_37=com.nightfire.adapter.messageprocessor.StringConcatenator, CLASS_36=com.nightfire.adapter.messageprocessor.StringConcatenator, CLASS_35=com.nightfire.adapter.messageprocessor.GetAndSet, CLASS_34=com.nightfire.adapter.messageprocessor.ConditionTester, CLASS_33=com.nightfire.adapter.messageprocessor.CustomerSetter, CLASS_32=com.nightfire.adapter.messageprocessor.GetAndSet, CLASS_31=com.nightfire.adapter.messageprocessor.XSLTMessageProtocolAdapter, CLASS_30=com.nightfire.adapter.messageprocessor.ConditionTester, CLASS_29=com.nightfire.adapter.messageprocessor.TestAndRoute, CLASS_28=com.nightfire.adapter.messageprocessor.RuleProcessor, CLASS_27=com.nightfire.adapter.messageprocessor.DatabaseLogger, CLASS_26=com.nightfire.adapter.messageprocessor.DatabaseLogger, CLASS_25=com.nightfire.adapter.messageprocessor.DatabaseLogger, CLASS_24=com.nightfire.adapter.messageprocessor.XSLTMessageProtocolAdapter, CLASS_23=com.nightfire.adapter.messageprocessor.DatabaseLogger, CLASS_22=com.nightfire.adapter.messageprocessor.ConditionTester, CLASS_21=com.nightfire.adapter.messageprocessor.DriverMessageProcessor, CLASS_20=com.nightfire.asrpreorder.comms.soap.QWESTSOAPMessagingClient, CLASS_19=com.nightfire.adapter.messageprocessor.XMLPopulator, KEY_58=ASRPreOrder, CLASS_18=com.nightfire.adapter.messageprocessor.XSLTMessageProtocolAdapter, KEY_57=ASRPreOrder, CLASS_17=com.nightfire.adapter.messageprocessor.XSLTMessageProtocolAdapter, KEY_56=ASRPreOrder, CLASS_16=com.nightfire.adapter.messageprocessor.TestAndRoute, KEY_55=ASRPreOrder, CLASS_15=com.nightfire.adapter.messageprocessor.TestAndRoute, KEY_54=ASRPreOrder, CLASS_14=com.nightfire.adapter.messageprocessor.TestAndRoute, KEY_53=ASRPreOrder, CLASS_13=com.nightfire.adapter.messageprocessor.TestAndRoute, KEY_52=ASRPreOrder, CLASS_12=com.nightfire.adapter.messageprocessor.DBSelectQuery, CLASS_11=com.nightfire.adapter.messageprocessor.GetNextValOfSequence, KEY_51=ASRPreOrder, CLASS_10=com.nightfire.adapter.messageprocessor.GetAndSet, KEY_50=ASRPreOrder, KEY_49=ASRPreOrder, KEY_48=ASRPreOrder, KEY_47=ASRPreOrder, KEY_46=ASRPreOrder, KEY_45=ASRPreOrder, KEY_44=ASRPreOrder, KEY_43=ASRPreOrder, KEY_42=ASRPreOrder, KEY_41=ASRPreOrder, KEY_40=ASRPreOrder, KEY_39=ASRPreOrder, KEY_38=ASRPreOrder, KEY_37=ASRPreOrder, KEY_36=ASRPreOrder, KEY_35=ASRPreOrder, KEY_34=ASRPreOrder, KEY_33=ASRPreOrder, KEY_32=ASRPreOrder, KEY_31=ASRPreOrder, KEY_30=ASRPreOrder, KEY_29=ASRPreOrder, KEY_28=ASRPreOrder, KEY_27=ASRPreOrder, KEY_26=ASRPreOrder, KEY_25=ASRPreOrder, KEY_24=ASRPreOrder, KEY_23=ASRPreOrder, KEY_22=ASRPreOrder, KEY_21=ASRPreOrder, KEY_20=ASRPreOrder, KEY_19=ASRPreOrder, KEY_18=ASRPreOrder, KEY_17=ASRPreOrder, KEY_9=ASRPreOrder, KEY_16=ASRPreOrder, KEY_8=ASRPreOrder, KEY_15=ASRPreOrder, KEY_7=ASRPreOrder, KEY_14=ASRPreOrder, KEY_6=ASRPreOrder, KEY_13=ASRPreOrder, KEY_5=ASRPreOrder, KEY_12=ASRPreOrder, KEY_4=ASRPreOrder, KEY_11=ASRPreOrder, CLASS_9=com.nightfire.adapter.messageprocessor.GetNextValOfSequence, KEY_3=ASRPreOrder, KEY_10=ASRPreOrder, CLASS_8=com.nightfire.adapter.messageprocessor.DBSelectQuery, KEY_2=ASRPreOrder, CLASS_7=com.nightfire.asrpreorder.comms.soap.FPCSoapClient, KEY_1=ASRPreOrder, TYPE_58=SetFrontierURLandAppType, CLASS_6=com.nightfire.adapter.messageprocessor.XSLTMessageProtocolAdapter, KEY_0=ASRPreOrder, TYPE_57=FrontierNSRemover, CLASS_5=com.nightfire.adapter.messageprocessor.XSLTMessageProtocolAdapter, TYPE_56=FrontierSOAPClient, CLASS_4=com.nightfire.adapter.messageprocessor.MessageExceptionThrower, TYPE_55=FrontierNSGenerator, CLASS_3=com.nightfire.adapter.messageprocessor.StringConcatenator, TYPE_54=VZNCSOAPClient, CLASS_2=com.nightfire.adapter.messageprocessor.XSLTMessageProtocolAdapter, TYPE_53=VZNCNSGenerator, CLASS_1=com.nightfire.adapter.messageprocessor.XSLTMessageProtocolAdapter, TYPE_52=SetVZNCEndPointURL, CLASS_0=com.nightfire.asrpreorder.comms.soap.ATTSOAPMessagingClient, TYPE_51=SetEndPointURLLocation, TYPE_50=RemoveNewLineChar, TYPE_49=ValidateCCNA, TYPE_48=VZSOAPClient, TYPE_47=VZNSGenerator, TYPE_46=UOMtoNFMapper, TYPE_45=ThrowMessageException, TYPE_44=TestForATT, TYPE_43=SupplierRouter, TYPE_42=SetSupplierType, TYPE_41=SetSupplierAlias, TYPE_40=SetSOAPAction, DEFER_EXCEPTIONS_FLAG=true, TYPE_39=SetRegion, TYPE_38=SetQwestApplicationType, TYPE_37=SetICSCErrorMsg, TYPE_36=SetErrorMessage, TYPE_35=SetError, TYPE_34=SetEndPointUrlLoc, TYPE_33=SetCustomerID, TYPE_32=SetCIDAsILEC, TYPE_31=SetATTMsgTimeStamp, TYPE_30=SetATTApplicationType, TYPE_9=GetReqID, TYPE_8=GetCCNA, TYPE_7=FPCSoapClient, TYPE_6=FPCNSRemover, TYPE_5=FPCNSGenerator, TYPE_4=ExceptionThrower, TYPE_3=ExceptionMapNameGenerator, TYPE_2=ExceptionGenerator, TYPE_29=SOAPRequestFailed, TYPE_1=CommonExceptionGenerator, TYPE_28=RuleProcessor, TYPE_0=ATTSOAPClient, TYPE_27=RespUOMLogger, TYPE_26=RespNFLogger, TYPE_25=ReqUOMLogger, TYPE_24=ReqSupplierRemover, TYPE_23=ReqNFLogger, TYPE_22=ReqGetApplyRules, TYPE_21=ReceivePORequestProcessor, TYPE_20=QWESTSOAPClient, TYPE_19=PopulateSupplier, TYPE_18=NamespaceRemover}, ReqNFLogger={OPTIONAL_2=FALSE, LOCATION_8=@context.STATE, OPTIONAL_1=FALSE, LOCATION_10=@context.ApplyRules, LOCATION_7=@context.CC, OPTIONAL_0=FALSE, LOCATION_6=@context.CCNA, LOCATION_5=@context.ICSC, LOCATION_4=@context.MESSAGE_ID, TRANSACTIONAL_LOGGING=TRUE, LOCATION_3=@context.RequestType, LOCATION_2=@context.REQUEST_HEADER.Supplier, LOCATION_1=@context.PREORDER_SEQUENCE, DEFAULT_11=submit, DEFAULT_10=1, COLUMN_12=REGION, COLUMN_11=ACTION, COLUMN_10=APPLYBUSINESSRULES, COLUMN_9=MESSAGE, SEPARATOR=|, COLUMN_8=STATE, COLUMN_7=CC, COLUMN_6=CCNA, COLUMN_5=ICSC, NEXT_PROCESSOR_NAME=IsBranded, COLUMN_4=MESSAGEID, COLUMN_3=MESSAGETYPE, COLUMN_2=SUPPLIER, COLUMN_1=ASRMESSAGEKEY, COLUMN_0=CREATED, DATE_FORMAT_0=MM-dd-yyyy, NAME=ReqNFLogger, TABLE_NAME=SEND_ASR_PREORDER_MESSAGE, COLUMN_TYPE_9=TEXT_BLOB, DEFAULT_0=SYSDATE, COLUMN_TYPE_0=DATE, OPTIONAL_9=TRUE, OPTIONAL_12=TRUE, OPTIONAL_8=TRUE, OPTIONAL_11=FALSE, OPTIONAL_7=TRUE, OPTIONAL_10=FALSE, OPTIONAL_6=FALSE, OPTIONAL_5=TRUE, OPTIONAL_4=FALSE, LOCATION_12=@context.REGION.REGION, OPTIONAL_3=FALSE, LOCATION_9=INPUT_MESSAGE}, QWESTSOAPClient={USER_PASSWORD=COLORADO, SOAP_ACTION=@context.SOAP_ACTION, NEXT_PROCESSOR_NAME=SOAPRequestFailed, END_POINT_URL=http://192.168.97.5:8080/axis/services/ILECRequestHandler, TML_TO=Qwest, USER_ID=LVC, TML_FROM=Neustar, TML_RETRYCOUNT=0, SOAP_RESPONSE_LOC=INPUT_MESSAGE, SOAP_REQUEST_LOC=INPUT_MESSAGE, ASR_MSGKEY=@context.PREORDER_SEQUENCE, TML_APPLICATIONTYPE=@context.APPLICATION_TYPE, NAME=QWESTSOAPClient}, SetEndPointUrlLoc={RESULT_VALUE_2=http://192.168.97.5:8080/axis/services/SvcAvailInquiry, RESULT_VALUE_1=http://192.168.97.5:8080/axis/services/LocationInquiry, RESULT_VALUE_0=http://192.168.97.5:8080/axis/services/CFAInquiry, TEST_2=@context.RequestType=SvcAvailInquiry, TEST_1=@context.RequestType=LocationInquiry, NEXT_PROCESSOR_NAME=ATTSOAPClient, TEST_0=@context.RequestType=CFAInquiry, NAME=SetEndPointUrlLoc, RESULT_LOCATION=@context.END_POINT_URL_LOC}, SetQwestApplicationType={RESULT_VALUE_0=LOCATION_INQUIRY, RESULT_LOCATION=@context.APPLICATION_TYPE, NEXT_PROCESSOR_NAME=SetSOAPAction, NAME=SetQwestApplicationType, TEST_1=@context.RequestType=CFAInquiry, TEST_0=@context.RequestType=LocationInquiry, RESULT_VALUE_1=CFA_INQUIRY}, SetCIDAsILEC={OUTPUT_LOC_0=@context.ILEC, NEXT_PROCESSOR_NAME=GetWSP, INPUT_LOC_0=@context.REQUEST_HEADER.CustomerIdentifier, NAME=SetCIDAsILEC}, FrontierSOAPClient={NEXT_PROCESSOR_NAME=FrontierNSRemover, END_POINT_URL=http://192.168.98.16:26989/axis/services/asrport, KEYSTORE_TYPE=jks, TML_TO=NTIR, TML_FROM=FRPO, TML_RETRYCOUNT=3, SOAP_RESPONSE_LOC=INPUT_MESSAGE, KEYSTORE_PASSWORD=security, SOAP_REQUEST_LOC=INPUT_MESSAGE, ASR_MSGKEY=@context.PREORDER_SEQUENCE, KEYSTORE_FILE=/export/home/qa_comm/nfi/Security/keystore, TML_APPLICATIONTYPE=@context.APPLICATION_TYPE, NAME=FrontierSOAPClient}, VZNSGenerator={XSL_JAR_FILE=/export/home/qa_comm/nfi/gateways/asr-send-preorder/lib/maps.jar, STYLE_SHEET_IS_COMPILED=TRUE, NEXT_PROCESSOR_NAME=VZSOAPClient, INPUT_LOCATION=INPUT_MESSAGE, NAME=VZNSGenerator, OUTPUT_LOCATION=INPUT_MESSAGE, STYLE_SHEET=com.nightfire.asr_preorder.VZNamespaceGenerator}, SUPERVISOR={COMM_SERVER_CLASS_0=com.nightfire.comms.corba.SyncCorbaServer, COMM_SERVER_TYPE_0=SyncCORBAServer, COMM_SERVER_KEY_0=ASRPreOrder}, ReqUOMLogger={LOCATION_2=INPUT_MESSAGE, SEPARATOR=|, LOCATION_0=@context.PREORDER_SEQUENCE, OPTIONAL_2=FALSE, NEXT_PROCESSOR_NAME=SupplierRouter, OPTIONAL_1=FALSE, OPTIONAL_0=FALSE, TRANSACTIONAL_LOGGING=TRUE, COLUMN_TYPE_2=TEXT_BLOB, COLUMN_2=MESSAGE, COLUMN_1=CREATED, COLUMN_TYPE_1=DATE, COLUMN_0=ASRMESSAGEKEY, DEFAULT_1=SYSDATE, TABLE_NAME=SEND_ASR_PREORDER_UOM, DATE_FORMAT_1=MM-dd-yyyy, NAME=ReqUOMLogger}, SetSupplierAlias={NEXT_PROCESSOR_NAME=GetRequestInfo, NAME=SetSupplierAlias, TEST_7=@context.REQUEST_HEADER.Supplier=Frontier, TEST_6=@context.REQUEST_HEADER.Supplier=VZNC, TEST_5=@context.REQUEST_HEADER.Supplier=ATT, TEST_4=@context.REQUEST_HEADER.Supplier=FPC, RESULT_VALUE_7=Frontier, TEST_3=@context.REQUEST_HEADER.Supplier=VZW, RESULT_VALUE_6=VZNC, TEST_2=@context.REQUEST_HEADER.Supplier=BAS, RESULT_VALUE_5=ATT, TEST_1=@context.REQUEST_HEADER.Supplier=BAN, RESULT_LOCATION=@context.SUPPLIER_ALIAS, RESULT_VALUE_4=FPC, TEST_0=@context.REQUEST_HEADER.Supplier=QWEST, RESULT_VALUE_3=VZ, RESULT_VALUE_2=VZ, RESULT_VALUE_1=VZ, RESULT_VALUE_0=QWEST}, IsATT={DEFAULT_NEXT_PROCESSOR=GetReqID, NEXT_PROCESSOR_0=SetRegion, NEXT_PROCESSOR_NAME=GetReqID|SetRegion, THROW_EXCEPTION_FOR_NO_ROUTE_FLAG=true, NAME=IsATT, TEST_0=@context.SUPPLIER_ALIAS=ATT}, UOMtoNFMapper={XSL_JAR_FILE=/export/home/qa_comm/nfi/gateways/asr-send-preorder/lib/maps.jar, STYLE_SHEET_IS_COMPILED=TRUE, NEXT_PROCESSOR_NAME=RespNFLogger, INPUT_LOCATION=INPUT_MESSAGE, NAME=UOMtoNFMapper, STYLE_SHEET=com.nightfire.asr_preorder.RespMain, TRANSFORM_AS_STRING=TRUE}, ReqGetApplyRules={RESULT_VALUE_0=0, RESULT_LOCATION=@context.ApplyRules, NEXT_PROCESSOR_NAME=SetSupplierType, NAME=ROOT, TEST_0=@context.REQUEST_HEADER.ApplyBusinessRules=N}, SetCustomerID={NEXT_PROCESSOR_NAME=ReceivePORequestProcessor, NAME=SetCustomerID, CUSTOMER_ID_LOCATION=@context.WSP.PROVIDER}, FrontierNSRemover={STYLE_SHEET_IS_COMPILED=FALSE, NEXT_PROCESSOR_NAME=SOAPRequestFailed, INPUT_LOCATION=INPUT_MESSAGE, NAME=FrontierNSRemover, OUTPUT_LOCATION=INPUT_MESSAGE, STYLE_SHEET=./gateways/asr-send-preorder/runtime/xsl/Remove_Frontier_ResNode.xsl}, TestForATT={DEFAULT_NEXT_PROCESSOR=ReqUOMLogger, NEXT_PROCESSOR_0=SetATTMsgTimeStamp, NEXT_PROCESSOR_NAME=ReqUOMLogger|SetATTMsgTimeStamp, NAME=TestForATT, TEST_0=@context.SUPPLIER_ALIAS=ATT}, SOAPRequestFailed={TEST_5=soap-env:Fault, TEST_4=Fault, NEXT_PROCESSOR_5=ExceptionMapNameGenerator, NEXT_PROCESSOR_4=ExceptionMapNameGenerator, TEST_3=soapenv:Fault, THROW_EXCEPTION_FOR_NO_ROUTE_FLAG=true, NEXT_PROCESSOR_3=ExceptionMapNameGenerator, TEST_2=faultcode, TEST_1=EXCEPTION_DETAILS, NEXT_PROCESSOR_NAME=ExceptionMapNameGenerator|NamespaceRemover, NEXT_PROCESSOR_2=ExceptionMapNameGenerator, NEXT_PROCESSOR_1=ExceptionMapNameGenerator, TEST_0=soap-env:faultcode, NEXT_PROCESSOR_0=ExceptionMapNameGenerator, DEFAULT_NEXT_PROCESSOR=NamespaceRemover, NAME=SOAPRequestFailed}, ExceptionMapNameGenerator={CONCATENATED_OUTPUT_LOCATION=@context.ExceptionMapClass, REQUIRED_0=TRUE, NEXT_PROCESSOR_NAME=CommonExceptionGenerator, STRING_VALUE_LOCATION_1=@context.SUPPLIER_ALIAS, STATIC_STRING_VALUE_2=ExceptionMap, STATIC_STRING_VALUE_0=com.nightfire.asr_preorder., REQUIRED_2=TRUE, NAME=ExceptionMapNameGenerator, REQUIRED_1=TRUE}, IsRegionExist={DEFAULT_NEXT_PROCESSOR=GetReqID, NEXT_PROCESSOR_0=SetICSCErrorMsg, NEXT_PROCESSOR_NAME=GetReqID|SetICSCErrorMsg, THROW_EXCEPTION_FOR_NO_ROUTE_FLAG=true, NAME=IsRegionExist, TEST_0=@context.REGION.REGION=NOREGION}, SetICSCErrorMsg={NEXT_PROCESSOR_NAME=ThrowMessageException, REQUIRED_0=TRUE, STATIC_STRING_VALUE_0=<?xml version=\"1.0\"?><Errors><ruleerrorcontainer><ruleerror><RULE_ID value=\"\" /><MESSAGE value=\"Invalid ICSC. Please try with a valid ICSC.\" /><CONTEXT value=\"asr_preorder/RequestHeader/ICSC\" /><CONTEXT_VALUE value=\"err\" /></ruleerror></ruleerrorcontainer></Errors>, CONCATENATED_OUTPUT_LOCATION=INPUT_MESSAGE, NAME=SetICSCErrorMsg}, SupplierRouter={TEST_5=@context.SUPPLIER_ALIAS=Frontier, NEXT_PROCESSOR_5=SetFrontierURLandAppType, TEST_4=@context.SUPPLIER_ALIAS=VZNC, NEXT_PROCESSOR_4=SetVZNCEndPointURL, TEST_3=@context.SUPPLIER_ALIAS=ATT, NEXT_PROCESSOR_3=SetATTApplicationType, TEST_2=@context.SUPPLIER_ALIAS=FPC, TEST_1=@context.SUPPLIER_ALIAS=VZ, NEXT_PROCESSOR_NAME=FPCNSGenerator|SetATTApplicationType|SetEndPointURLLocation|SetQwestApplicationType|SetVZNCEndPointURL|SetFrontierURLandAppType, NEXT_PROCESSOR_2=FPCNSGenerator, TEST_0=@context.SUPPLIER_ALIAS=QWEST, NEXT_PROCESSOR_1=SetEndPointURLLocation, NEXT_PROCESSOR_0=SetQwestApplicationType, NAME=SupplierRouter}, FPCNSRemover={STYLE_SHEET_IS_COMPILED=FALSE, NEXT_PROCESSOR_NAME=SOAPRequestFailed, INPUT_LOCATION=INPUT_MESSAGE, NAME=FPCNSRemover, OUTPUT_LOCATION=INPUT_MESSAGE, STYLE_SHEET=./gateways/asr-send-preorder/runtime/xsl/Remove_FPC_ResNode.xsl}, SyncCORBAServer={SERVICE_PROVIDER_0=ASRPREORDER, OPERATION_0=asr_preorder, ASYNCHRONOUS_0=FALSE, EVENT_CHANNEL_NAME=NightFire.SPI.StatusChannel, INTERFACE_VERSION_0=ASOG, DRIVER_KEY=ASRPreOrder, DRIVER_TYPE=SyncRequestDriver, SERVER_NAME=Nightfire.SPI.ASRPREORDER.ASOG.asr_preorder, NAME=SyncCORBAServer}, VZNCSOAPClient={END_POINT_URL_LOC=@context.END_POINT_URL_LOC, KEYSTORE_ALIAS=srtest, CRYPTO_ALIAS_PASSWORD=security, NEXT_PROCESSOR_NAME=SOAPRequestFailed, KEYSTORE_FILE=/export/home/qa_comm/nfi/Security/keystore, SOAP_RESPONSE_LOC=INPUT_MESSAGE, NAME=VZNCSOAPClient, TML_APPLICATIONTYPE=@context.RequestType, KEYSTORE_PASSWORD=security, TML_TO=VZNC, TML_RETRYCOUNT=3, TOKEN_PASSWORD=VZNC_T1, CRYPTO_KEYSTORE_TYPE=jks, CRYPTO_PROVIDER=org.apache.ws.security.components.crypto.Merlin, TOKEN_USERNAME=VZNCUOM, TML_FROM=NUS, SOAP_REQUEST_LOC=INPUT_MESSAGE, ASR_MSGKEY=@context.PREORDER_SEQUENCE}, GetWSP={INPUT_DATE_FORMAT_0=MM-dd-yyyy, SQL_QUERY_STATEMENT=SELECT PROVIDER FROM DOMAIN_PROVIDER WHERE DOMAIN=?, LOCATION_0=@context.REQUEST_HEADER.CustomerIdentifier, OUTPUT_DATE_FORMAT=MM-dd-yyyy, NEXT_PROCESSOR_NAME=SetCustomerID, USE_CUSTOMER_ID=FALSE, THROW_EXCEPTION_WHEN_NOT_FOUND_FLAG=FALSE, SINGLE_RESULT_FLAG=TRUE, TABLE_NAME=DOMAIN_PROVIDER, NAME=GetWSP, RESULT_LOCATION=@context.WSP}, SetFrontierURLandAppType={RESULT_VALUE_0=FRONT-CFA, RESULT_LOCATION=@context.APPLICATION_TYPE, NEXT_PROCESSOR_NAME=FrontierNSGenerator, TEST_1=@context.RequestType=LocationInquiry, NAME=SetFrontierURLandAppType, TEST_0=@context.RequestType=CFAInquiry, RESULT_VALUE_1=FRONT-LI}, VZNCNSGenerator={XSL_JAR_FILE=./gateways/asr-send-preorder/lib/maps.jar, STYLE_SHEET_IS_COMPILED=TRUE, NEXT_PROCESSOR_NAME=VZNCSOAPClient, INPUT_LOCATION=INPUT_MESSAGE, NAME=VZNCNSGenerator, OUTPUT_LOCATION=INPUT_MESSAGE, STYLE_SHEET=com.nightfire.asr_preorder.VZNamespaceGenerator}, ExceptionThrower={NEXT_PROCESSOR_NAME=NOBODY, NAME=ExceptionThrower}, NFtoUOMMapper={XSL_JAR_FILE=/export/home/qa_comm/nfi/gateways/asr-send-preorder/lib/maps.jar, NEXT_PROCESSOR_NAME=TestForATT, STYLE_SHEET_IS_COMPILED=TRUE, INPUT_LOCATION=INPUT_MESSAGE, NAME=NFtoUOMMapper, STYLE_SHEET=com.nightfire.asr_preorder.ReqMain}, SetErrorMessage={REQUIRED_0=TRUE, CONCATENATED_OUTPUT_LOCATION=INPUT_MESSAGE, NEXT_PROCESSOR_NAME=ExceptionThrower, STRING_VALUE_LOCATION_4=., STRING_VALUE_LOCATION_2=@context.REQUEST_HEADER.Supplier, STATIC_STRING_VALUE_3=]   , STATIC_STRING_VALUE_1=SUPPLIER: [, STATIC_STRING_VALUE_0=RESPONSE SUMMARY:                              , REQUIRED_4=TRUE, REQUIRED_3=TRUE, REQUIRED_2=TRUE, NAME=SetErrorMessage, REQUIRED_1=TRUE}, SetATTMsgTimeStamp={XSL_JAR_FILE=./gateways/asr-send-preorder/lib/maps.jar, STYLE_SHEET_IS_COMPILED=TRUE, NEXT_PROCESSOR_NAME=ReqUOMLogger, INPUT_LOCATION=INPUT_MESSAGE, NAME=SetATTMsgTimeStamp, OUTPUT_LOCATION=INPUT_MESSAGE, STYLE_SHEET=com.nightfire.asr_preorder.ATTMessageTimestamp}, ValidateCCNA={DEFAULT_NEXT_PROCESSOR=GetRequestInfo, NEXT_PROCESSOR_NAME=GetRequestInfo|SetError, NEXT_PROCESSOR_0=SetError, THROW_EXCEPTION_FOR_NO_ROUTE_FLAG=TRUE, NAME=ValidateCCNA, TEST_0=@context.hasCCNA.COUNT=0}, ASRSendPOResponseProcessing={TYPE_0=GetResponeID, ASYNC_FLAG=false, KEY_1=ASRPreOrder, KEY_0=ASRPreOrder, CLASS_1=com.nightfire.adapter.messageprocessor.DatabaseLogger, CLASS_0=com.nightfire.adapter.messageprocessor.GetNextValOfSequence, TYPE_1=ResponseNFLogger}, GetRequestInfo={NEXT_PROCESSOR_NAME=IsATT, OUTPUT_LOC_6=@context.STATE, OUTPUT_LOC_5=@context.CC, OUTPUT_LOC_4=@context.CCNA, OPTIONAL_6=FALSE, OUTPUT_LOC_3=@context.ICSC, OPTIONAL_5=FALSE, OUTPUT_LOC_2=@context.MESSAGE_ID, OPTIONAL_4=FALSE, OUTPUT_LOC_1=@context.RequestType, OPTIONAL_3=FALSE, OPTIONAL_2=FALSE, OUTPUT_LOC_0=@context.Supplier, INPUT_LOC_6=RequestHeader.STATE, OPTIONAL_1=FALSE, INPUT_LOC_5=RequestHeader.CC, OPTIONAL_0=FALSE, INPUT_LOC_4=RequestHeader.CCNA, INPUT_LOC_3=RequestHeader.ICSC, NAME=GetRequestInfo, INPUT_LOC_2=RequestHeader.MESSAGE_ID, INPUT_LOC_1=RequestHeader.RequestType, INPUT_LOC_0=@context.REQUEST_HEADER.Supplier}, ReceivePORequestProcessor={NEXT_PROCESSOR_NAME=COMM_SERVER, DEFAULT_DRIVER_KEY=ASRReceivePreOrder, DEFAULT_DRIVER_TYPE=ASRReceivePORequestProcessing, NAME=ReceivePORequestProcessor}, GetRespID={SEQ_NAME=ASRPREORDERMSGKEY, NEXT_PROCESSOR_NAME=RespUOMLogger, SEQ_VAL_LOCATION=@context.PREORDER_SEQUENCE, MIN_SEQ_VAL_LENGTH=3, NAME=GetRespID}, FPCNSGenerator={STYLE_SHEET_IS_COMPILED=FALSE, NEXT_PROCESSOR_NAME=FPCSoapClient, INPUT_LOCATION=INPUT_MESSAGE, NAME=FPCNSGenerator, OUTPUT_LOCATION=INPUT_MESSAGE, STYLE_SHEET=./gateways/asr-send-preorder/runtime/xsl/Add_FPC_ReqNode.xsl}, RuleProcessor={EVALUATOR_CLASS_7=asr_preorder.FPC.FPC, NEXT_PROCESSOR_NAME=ReqSupplierRemover, EVALUATOR_CLASS_6=asr_preorder.SVC_AVAIL_INQUIRY.SVC_AVAIL_INQUIRY, EVALUATOR_CLASS_5=asr_preorder.LOCATION_INQUIRY.LOCATION_INQUIRY, EVALUATOR_CLASS_4=asr_preorder.CFA_INQUIRY.CFA_INQUIRY, EVALUATOR_CLASS_3=asr_preorder.REQUEST_HEADER.REQUEST_HEADER, EVALUATOR_CLASS_2=asr_preorder.VZ.VZ, RETURN_XML_ERRORS=TRUE, EVALUATOR_CLASS_1=asr_preorder.ATT.ATT, EVALUATOR_CLASS_0=asr_preorder.QWEST.QWEST, INPUT_MESSAGE_LOCATION=INPUT_MESSAGE, NF_RULE_CLASSPATH=./gateways/bellsouth-asr-preorder/rules:./gateways/qwest-asr-preorder/rules:./gateways/sbc-asr-preorder/rules:./gateways/verizon-asr-preorder/rules:./gateways/fpc-asr-preorder/rules:./gateways/asr-send-preorder/lib/ILECSpecificrules.jar:./gateways/asr-send-preorder/lib/rules.jar, ENABLED=TRUE, NAME=RuleProcessor}, SetVZNCEndPointURL={RESULT_VALUE_0=http://192.168.97.5:8080/axis/services/CfaInquiryServicePort, RESULT_LOCATION=@context.END_POINT_URL_LOC, NEXT_PROCESSOR_NAME=VZNCNSGenerator, TEST_1=@context.RequestType=LocationInquiry, NAME=SetVZNCEndPointURL, TEST_0=@context.RequestType=CFAInquiry, RESULT_VALUE_1=http://192.168.97.5:8080/axis/services/LInquiryServicePort}, FrontierNSGenerator={STYLE_SHEET_IS_COMPILED=FALSE, NEXT_PROCESSOR_NAME=FrontierSOAPClient, INPUT_LOCATION=INPUT_MESSAGE, NAME=FrontierNSGenerator, OUTPUT_LOCATION=INPUT_MESSAGE, STYLE_SHEET=./gateways/asr-send-preorder/runtime/xsl/Add_Frontier_ReqNode.xsl}, SetError={OUTPUT_LOC_0=INPUT_MESSAGE, NEXT_PROCESSOR_NAME=ThrowMessageException, DEFAULT_0=<?xml version=\"1.0\"?><Errors><ruleerrorcontainer><ruleerror><RULE_ID value=\"\" /><MESSAGE value=\"This CCNA is not assigned to you. Please try with a valid CCNA.\" /><CONTEXT value=\"asr_preorder/RequestHeader/CCNA\" /><CONTEXT_VALUE value=\"err\" /></ruleerror></ruleerrorcontainer></Errors>, OPTIONAL_0=true, NAME=SetError}, NamespaceRemover={XSL_JAR_FILE=/export/home/qa_comm/nfi/gateways/asr-send-preorder/lib/maps.jar, STYLE_SHEET_IS_COMPILED=TRUE, NEXT_PROCESSOR_NAME=GetRespID, INPUT_LOCATION=INPUT_MESSAGE, NAME=NamespaceRemover, STYLE_SHEET=com.nightfire.asr_preorder.Translate, TRANSFORM_AS_STRING=TRUE}, ThrowMessageException={NEXT_PROCESSOR_NAME=NOBODY, NAME=ThrowMessageException}, GetResponeID={SEQ_NAME=ASRPREORDERMSGKEY, NEXT_PROCESSOR_NAME=ResponseNFLogger, SEQ_VAL_LOCATION=@context.PREORDER_SEQUENCE, MIN_SEQ_VAL_LENGTH=3, NAME=ROOT}, ExceptionGenerator={NEXT_PROCESSOR_NAME=SetErrorMessage, INPUT_LOCATION=INPUT_MESSAGE, NAME=ExceptionGenerator, STYLE_SHEET=/export/home/qa_comm/nfi/gateways/asr-send-preorder/runtime/xsl/ExceptionGenerator.xsl, OUTPUT_LOCATION=INPUT_MESSAGE}, SetSOAPAction={CONCATENATED_OUTPUT_LOCATION=@context.SOAP_ACTION, REQUIRED_0=TRUE, NEXT_PROCESSOR_NAME=QWESTSOAPClient, STRING_VALUE_LOCATION_1=@context.APPLICATION_TYPE, STATIC_STRING_VALUE_2=Request, STATIC_STRING_VALUE_0=http://www.qwest.com/, REQUIRED_2=TRUE, NAME=SetSOAPAction, REQUIRED_1=TRUE}, ATTSOAPClient={END_POINT_URL_LOC=@context.END_POINT_URL_LOC, KEYSTORE_ALIAS=srtest, CRYPTO_ALIAS_PASSWORD=asd, NEXT_PROCESSOR_NAME=SOAPRequestFailed, KEYSTORE_FILE=/export/home/qa_comm/nfi/Security/srtest.keystore, SOAP_RESPONSE_LOC=INPUT_MESSAGE, NAME=ATTSOAPClient, TML_APPLICATIONTYPE=@context.APPLICATION_TYPE, KEYSTORE_PASSWORD=srtest, TML_TO=ATT, NAMESPACE_VERSION=ASOG39, TML_RETRYCOUNT=0, CRYPTO_KEYSTORE_TYPE=jks, CRYPTO_PROVIDER=org.apache.ws.security.components.crypto.Merlin, TML_CORRELATIONID=78786767, TML_FROM=Neustar, SOAP_REQUEST_LOC=INPUT_MESSAGE, TML_TRACKID=Trackid, ASR_MSGKEY=@context.PREORDER_SEQUENCE}, SetRegion={STYLE_SHEET_IS_COMPILED=FALSE, NEXT_PROCESSOR_NAME=IsRegionExist, INPUT_LOCATION=INPUT_MESSAGE, NAME=SetRegion, OUTPUT_LOCATION=@context.REGION, STYLE_SHEET=./gateways/asr-send-preorder/runtime/xsl/ICSC_REGION.xsl}, RespUOMLogger={SEPARATOR=|, LOCATION_2=INPUT_MESSAGE, LOCATION_0=@context.PREORDER_SEQUENCE, OPTIONAL_2=FALSE, NEXT_PROCESSOR_NAME=RemoveNewLineChar, OPTIONAL_1=FALSE, OPTIONAL_0=FALSE, TRANSACTIONAL_LOGGING=TRUE, COLUMN_2=MESSAGE, COLUMN_TYPE_2=TEXT_BLOB, COLUMN_TYPE_1=DATE, COLUMN_1=CREATED, COLUMN_0=ASRMESSAGEKEY, DEFAULT_1=SYSDATE, TABLE_NAME=SEND_ASR_PREORDER_UOM, DATE_FORMAT_1=MM-dd-yyyy, NAME=RespUOMLogger}}, IOR_ACCESS={}}";
//        System.out.println("myStr before = " + myStr);
        PropUtils.suppressPasswords(myStr);

//        System.out.println("myStr after = " + PropUtils.suppressPasswords(myStr));
//        System.out.println("myStr after = " + PropUtils.suppressPasswords(null));
//        System.out.println("myStr after = " + PropUtils.suppressPasswords(""));
//        System.out.println("myStr after = " + PropUtils.suppressPasswords("akfdajsdl;PASSWORD,fkajsfl;kasjflksa;djfalsk;kfjasdlkfjsdlkfj"));
//        System.out.println("myStr after = " + PropUtils.suppressPasswords("akfdajsdl;PASSWORD=,fkajsfl;kasjflksa;djfalsk;kfjasdlkfjsdlkfj"));
//        System.out.println("myStr after = " + PropUtils.suppressPasswords("akfdajsdl;PASSWORDasdfajsfdl,fkajsfl;kasjflksa;djfalsk;kfjasdlkfjsdlkfj"));
//        System.out.println("myStr after = " + PropUtils.suppressPasswords("akfdajsdl;PASSWORD=PASSWD,=abcde,fkajsfl;kasjflksa;djfalsk;kfjasdlkfjsdlkfj"));
//        System.out.println("myStr after = " + PropUtils.suppressPasswords("akfdajsdl;PASSWORD=PASSWD,=abcde,fkajsfl;kasjflksa;djfalsk;kfjasdlkfjsdlkfjPASSWORD="));
//        System.out.println("myStr after = " + PropUtils.suppressPasswords("akfdajsdl;PASSWORD=PASSWD,=abcde,fkajsfl;kasjflksa;djfalsk;kfjasdlkfjsdlkfjPASSWORD=,"));
//        System.out.println("myStr after = " + PropUtils.suppressPasswords("akfdajsdl;PASSWORD=PASSWD,=abcde,fkajsfl;kasjflksa;djfalsk;kfjasdlkfjsdlkfjPASSWORD=6kdiekdiekdikeidkei"));
//        System.out.println("myStr after = " + PropUtils.suppressPasswords("akfdajsdl;PASSWORD=PASSWD,=abcde,fkajUserID=ABCDED,sfl;kasjflksa;djfalsk;kfjasdlkfjsdlkfjPASSWORD=6kdiekdiekdikeidkei"));
        System.out.println("myStr after = " + PropUtils.suppressPasswords("{java.vendor=Sun Microsystems Inc., sun.java.launcher={CC=1111, Passwd=mgrqa, IntfVersion=LSOG10, TransactionNo=0001, MM=10, DBRESULTS=[#document: null], MAX_MSG_KEY=1727389, FILENAME=TO1111100001., RequestKey=1727390, OrderType=TO, UserName=mgrqa, RAO=444}, catalina.base=/export/home/mgrqa/basicsendsrm/nfi/tomcat, javax.net.ssl.trustStorePassword=keystore, sun.management.compiler=HotSpot Server Compiler, catalina.useNaming=true, os.name=SunOS, sun.boot.class.path=/export/home/mgrqa/basicsendsrm/nfi/tomcat/common/endorsed/xercesImpl.jar:/export/home/mgrqa/basicsendsrm/nfi/tomcat/common/endorsed/xml-apis.jar:/export/home/mgrqa/basicsendsrm/nfi/tomcat/common/endorsed/xalan.jar:/export/home/mgrqa/basicsendsrm/nfi/tomcat/common/endorsed/serializer.jar:/export/home/mgrqa/install/j2sdk1.5.0_12/jre/lib/rt.jar:/export/home/mgrqa/install/j2sdk1.5.0_12/jre/lib/i18n.jar:/export/home/mgrqa/install/j2sdk1.5.0_12/jre/lib/sunrsasign.jar:/export/home/mgrqa/install/j2sdk1.5.0_12/jre/lib/jsse.jar:/export/home/mgrqa/install/j2sdk1.5.0_12/jre/lib/jce.jar:/export/home/mgrqa/install/j2sdk1.5.0_12/jre/lib/charsets.jar:/export/home/mgrqa/install/j2sdk1.5.0_12/jre/classes, java.util.logging.config.file=/export/home/mgrqa/basicsendsrm/nfi/tomcat/conf/logging.properties, javax.net.debug=ssl:handshake:verbose:session , java.vm.specification.vendor=Sun Microsystems Inc., java.runtime.version=1.5.0_12-b04, javax.net.ssl.keyStore=/export/home/mgrqa/basicsendsrm/nfi/security/keystore, user.name=mgrqa, shared.loader=${catalina.base}/shared/classes,${catalina.base}/shared/lib/*.jar, tomcat.util.buf.StringCache.byte.enabled=true, javax.net.ssl.trustStoreType=jks, java.naming.factory.initial=org.apache.naming.java.javaURLContextFactory, user.language=en, NF_REPOSITORY_ROOT=/export/home/mgrqa/basicsendsrm/nfi/repository, sun.boot.library.path=/export/home/mgrqa/install/j2sdk1.5.0_12/jre/lib/sparc, java.version=1.5.0_12, java.util.logging.manager=org.apache.juli.ClassLoaderLogManager, user.timezone=GMT, sun.arch.data.model=32, java.endorsed.dirs=/export/home/mgrqa/basicsendsrm/nfi/tomcat/common/endorsed, sun.cpu.isalist=sparcv9+vis sparcv9 sparcv8plus+vis sparcv8plus sparcv8 sparcv8-fsmuld sparcv7 sparc, sun.jnu.encoding=ISO646-US, file.encoding.pkg=sun.io, package.access=sun.,org.apache.catalina.,org.apache.coyote.,org.apache.tomcat.,org.apache.jasper.,sun.beans., NF_ENDORSED=/export/home/mgrqa/basicsendsrm/nfi/lib/endorsed, file.separator=/, java.specification.name=Java Platform API Specification, java.class.version=49.0, java.home=/export/home/mgrqa/install/j2sdk1.5.0_12/jre, javax.xml.parsers.DocumentBuilderFactory=org.apache.xerces.jaxp.DocumentBuilderFactoryImpl, java.vm.info=mixed mode, os.version=5.8, WHOLESALE_PROVIDER=, org.omg.CORBA.ORBSingletonClass=org.jacorb.orb.ORBSingleton, ORBagentPort=30005, path.separator=:, java.vm.version=1.5.0_12-b04, ORB.Provider=JacORB, java.protocol.handler.pkgs=com.sun.net.ssl.internal.www.protocol|org.apache.axis.transport|, SVCnameroot=Nightfire, java.awt.printerjob=sun.print.PSPrinterJob, sun.io.unicode.encoding=UnicodeBig, package.definition=sun.,java.,org.apache.catalina.,org.apache.coyote.,org.apache.tomcat.,org.apache.jasper., java.naming.factory.url.pkgs=org.apache.naming, user.home=/export/home/mgrqa/, java.specification.vendor=Sun Microsystems Inc., java.library.path=/export/home/mgrqa/install/j2sdk1.5.0_12/jre/lib/sparc/server:/export/home/mgrqa/install/j2sdk1.5.0_12/jre/lib/sparc:/export/home/mgrqa/install/j2sdk1.5.0_12/jre/../lib/sparc:./lib::/usr/lib, java.vendor.url=http://java.sun.com/, javax.net.ssl.keyStoreType=jks, java.vm.vendor=Sun Microsystems Inc., common.loader=${catalina.home}/common/classes,${catalina.home}/common/i18n/*.jar,${catalina.home}/common/endorsed/*.jar,${catalina.home}/common/lib/*.jar, java.runtime.name=Java(TM) 2 Runtime Environment, Standard Edition, java.class.path=:/export/home/mgrqa/basicsendsrm/nfi/tomcat/bin/bootstrap.jar:/export/home/mgrqa/basicsendsrm/nfi/tomcat/bin/commons-logging-api.jar, DBDRIVER=oracle.jdbc.OracleDriver, java.vm.specification.name=Java Virtual Machine Specification, java.vm.specification.version=1.0, catalina.home=/export/home/mgrqa/basicsendsrm/nfi/tomcat, sun.cpu.endian=big, sun.os.patch.level=unknown, java.io.tmpdir=/export/home/mgrqa/basicsendsrm/nfi/tomcat/temp, java.vendor.url.bug=http://java.sun.com/cgi-bin/bugreport.cgi, server.loader=${catalina.home}/server/classes,${catalina.home}/server/lib/*.jar, os.arch=sparc, java.awt.graphicsenv=sun.awt.X11GraphicsEnvironment, java.ext.dirs=/export/home/mgrqa/install/j2sdk1.5.0_12/jre/lib/ext, user.dir=/export/home/mgrqa/basicsendsrm/nfi, CURRENT_COUNTRY=US, CURRENT_LANGUAGE=EN, line.separator=\n" +
                ", java.vm.name=Java HotSpot(TM) Server VM, javax.xml.soap.MessageFactory=org.apache.axis.soap.MessageFactoryImpl, file.encoding=ISO646-US, org.omg.CORBA.ORBClass=org.jacorb.orb.ORB, java.specification.version=1.5, javax.net.ssl.trustStore=/export/home/mgrqa/basicsendsrm/nfi/security/keystore, javax.net.ssl.keyStorePassword=keystore}"));

    }
}                                                      