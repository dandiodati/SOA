package com.nightfire.framework.util;

import org.w3c.dom.Document;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.util.xml.ParsedXPath;

/**
 * Utility class to obtain the Unique Identifier for Gateway driver chain.
 * The Unique Idenfier could be PON/TXNUM/REQUESTNO/... or any other desired field
 *
 * The class works on following input parameters
 * Key - the KEY for driver chain
 * PropertyType - the PROPERTYTYPE identify starting subflow for the message processing
 * PropertyName - this PROPERTYTYPE is an optional parameter. by default "UNIQUE_IDENTIFIER_XPATH" shall be used
 *                  to pick unique identifier XPath value from PP table
 * Message - the input message body XML document where the Unique Identifier can be found
 * XPath - obtained using key, propertyType and PropertyName, the XPath for the Unique Identifier in the passed Message.
 *         This XPath can be combined one for multiple possible locations of unique identifier. In case of combined XPaths the
 *         first valid value (including empty/white-space) obtained shall be returned. Null shall be returned if all XPaths have no values.
 *
 */
public class DriverUniqueIdentifierReader {

    /**
     * Driver chain key from PERSISTENTPROPERTY table which represents the Gateway (e.g. UOM_ICP_Send/UOM_ICP_Receive)
     */
    private String key;

    /**
     * Subflow name of the Gateway driver chain (e.g. RequestDriver/ResponseDriver/ReflowDriver)
     */
    private String propertyType;

    /**
     * The xpath which is obtained from PersistenProperty table for given <key>, <propertytye>
     */
    private String xpath;

    /**
     * ParsedXPath object for <xpath>
     */
    private ParsedXPath parsedXPath;

    /**
     * Default property name to look for xpath in PersistentProperty table
     */
    private String propertyName = "UNIQUE_IDENTIFIER_XPATHS";

    public DriverUniqueIdentifierReader(String key, String propertyType)
    {
        this(key, propertyType, null);
    }

    public DriverUniqueIdentifierReader(String key, String propertyType, String propertyName)
    {
        this.key = key;
        this.propertyType = propertyType;

        if (StringUtils.hasValue(propertyName))
            this.propertyName = propertyName;
        initializeParsedXPath();
    }

    private void initializeParsedXPath()
    {
        try
        {
            xpath = PersistentProperty.get(key, propertyType, propertyName);

            if (StringUtils.hasValue(xpath))
            {
                if (Debug.isLevelEnabled(Debug.MSG_DATA))
                    Debug.log(Debug.MSG_DATA, "DriverUniqueIdentifierReader(): obtained XPath for key-type-propertyname ["+
                        key + "-" + propertyType + "-" + propertyName + "] for customer [" + CustomerContext.getInstance().getCustomerID() + "] " +
                            "as [" + xpath + "]");

                parsedXPath = new ParsedXPath(xpath);
            }
        }
        catch (Exception pe)
        {
            String customerId = CustomerContext.DEFAULT_CUSTOMER_ID;

            try{customerId = CustomerContext.getInstance().getCustomerID();}
            catch(FrameworkException fe) { /*ignore if customerid is not obtained*/ }

            Debug.log(Debug.ALL_WARNINGS, "DriverUniqueIdentifierReader(): Could not fetch XPath for key-type-propertyname ["+
                key + "-" + propertyType + "-" + propertyName + "] for customer [" + customerId + "]");
        }

    }

    private void checkForLastesXPath()
    {
        String xpathLatest = null;
        try
        {
            xpathLatest = PersistentProperty.get(key, propertyType, propertyName);

            if ( (StringUtils.hasValue(xpath) && !xpath.equals(xpathLatest))
                    || (StringUtils.hasValue(xpathLatest) && !xpathLatest.equals(xpath)))
            {
                if (Debug.isLevelEnabled(Debug.MSG_DATA))
                    Debug.log(Debug.MSG_DATA, "DriverUniqueIdentifierReader(): obtained updated XPath for key-type-propertyname ["+
                        key + "-" + propertyType + "-" + propertyName + "] for customer [" + CustomerContext.getInstance().getCustomerID() + "] " +
                            "as [" + xpathLatest + "]");

                synchronized (parsedXPath)
                {
                    parsedXPath = null;
                    if (StringUtils.hasValue(xpathLatest))
                        parsedXPath = new ParsedXPath(xpathLatest);
                }
            }
        }
        catch (FrameworkException pe)
        {
            String customerId = CustomerContext.DEFAULT_CUSTOMER_ID;

            try{customerId = CustomerContext.getInstance().getCustomerID();}
            catch(FrameworkException fe) { /*ignore if customerid is not obtained*/ }

            Debug.log(Debug.ALL_WARNINGS, "DriverUniqueIdentifierReader(): Could not fetch XPath for key-type-propertyname ["+
                key + "-" + propertyType + "-" + propertyName + "] for customer [" + customerId + "]");
        }
    }

    public String getUniqueIdentifier(String message)
    {
        Document messageDocument = null;

        try
        {
            messageDocument = XMLLibraryPortabilityLayer.convertStringToDom(message);
        }
        catch (MessageException e)
        {
            Debug.log(Debug.ALL_WARNINGS,"DriverUniqueIdentifierReader(): could not convert message to Document, " +
                    "hence returning null as unique identifier. "+e.getMessage());
            return null;
        }

        return getUniqueIdentifier(messageDocument);
    }

    public String getUniqueIdentifier(Document message)
    {
        String customerId = CustomerContext.DEFAULT_CUSTOMER_ID;
        String uniqueIdentifier = null;

        try
        {
            customerId = CustomerContext.getInstance().getCustomerID();
        }
        catch(FrameworkException fe) { /*ignore if customerid is not obtained*/ }

        try
        {
            checkForLastesXPath();
            if (parsedXPath != null)
            {
                String[] values = parsedXPath.getValues(message.getDocumentElement());

                if (values != null && values.length > 0)
                {
                    uniqueIdentifier = values[0];
                }
            }
        }
        catch (FrameworkException pe)
        {
            Debug.log(Debug.ALL_WARNINGS, "DriverUniqueIdentifierReader(): Returning null as unique identifier as XPath could not be fetched for key-type-propertyname ["+
                key + "-" + propertyType + "-" + propertyName + "] for customer [" + customerId + "]"
                    + pe.getMessage());
            return null;
        }

        if (Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log(Debug.MSG_DATA, "DriverUniqueIdentifierReader(): returning unique identifier for key-type-propertyname ["+
                key + "-" + propertyType + "-" + propertyName + "] for customer [" + customerId + "] " +
                    "as [" + uniqueIdentifier + "]");

        return uniqueIdentifier;
    }

    public static void main (String[] args)
    {
        String message = "<?xml version=\"1.0\"?>\n" +
                "<Request>\n" +
                "  <lsr_order>\n" +
                "    <eu>\n" +
                "      <locationaccesscontainer type=\"container\">\n" +
                "          <locationaccess>\n" +
                "              <NAME value=\"Koo Koo&apos;s\" />\n" +
                "          </locationaccess>\n" +
                "      </locationaccesscontainer>\n" +
                "    </eu>\n" +
                "    <ls>\n" +
                "      <servicedetailscontainer type=\"container\">\n" +
                "          <servicedetails>\n" +
                "              <LNUM value=\"00001\" />\n" +
                "              <ECCKT value=\"38.LYFU.418519..SB\" />\n" +
                "              <LNA value=\"D\" />\n" +
                "          </servicedetails>\n" +
                "      </servicedetailscontainer>\n" +
                "      <ls_adminsection>\n" +
                "          <LQTY value=\"00001\" />\n" +
                "      </ls_adminsection>\n" +
                "    </ls>\n" +
                "    <lsr>\n" +
                "      <lsr_adminsection>\n" +
                "          <DTSENT value=\"06-12-2008-1057AM\" />\n" +
                "          <REQTYP value=\"AB\" />\n" +
                "          <PON value1=\"BS220XXADDA48\" />\n" +
                " <PON11>AB</PON11>" +
                "          <LS1O value=\"770943\" />\n" +
                "          <ACT value=\"D\" />\n" +
                "          <AN value=\"404N070042042\" />\n" +
                "          <CCNA value=\"ZXL\" />\n" +
                "          <SC value=\"LCSC\" />\n" +
                "          <NC value=\"LY--\" />\n" +
                "          <NCI value=\"04QC5.OOJ\" />\n" +
                "          <DDD value=\"12-12-2008\" />\n" +
                "          <ACTL value=\"PWSPGAAS94A\" />\n" +
                "          <SECNCI value=\"04DU5.24\" />\n" +
                "          <PROJECT value=\"CAVENOBILL\" />\n" +
                "          <PROJINDR value=\"A\"/>   \n" +
                "          <CC value=\"9999\" />\n" +
                "          <TOS value=\"1B--\" />\n" +
                "      </lsr_adminsection>\n" +
                "      <billsection>\n" +
                "          <ACNA value=\"ZXL\" />\n" +
                "          <BAN1 value=\"404N070042042\" />\n" +
                "      </billsection>\n" +
                "      <contactsection>\n" +
                "          <INIT value=\"Bojangles\" />\n" +
                "          <FAXNO value=\"444-888-4444\" />\n" +
                "          <TELNO value=\"888-444-8888\" />\n" +
                "          <IMPCON>\n" +
                "              <NAME value=\"Wyatt Earp\" />\n" +
                "              <TELNO value=\"555-000-5555\" />\n" +
                "          </IMPCON>\n" +
                "      </contactsection>\n" +
                "    </lsr>\n" +
                "  </lsr_order>\n" +
                "  <SupplierLSROrderRequest>\n" +
                "    <ls>\n" +
                "      <servicedetailscontainer type=\"container\">\n" +
                "          <servicedetails>\n" +
                "              <LNUM value=\"00001\" />\n" +
                "          </servicedetails>\n" +
                "      </servicedetailscontainer>\n" +
                "    </ls>\n" +
                "  </SupplierLSROrderRequest>\n" +
                "</Request>";
        try
        {
            System.setProperty("INSTALLROOT", "e:\\sm_gw");
            DBInterface.initialize("jdbc:oracle:thin:@impetus-786:1521:ORCL786", "sm_icp30", "sm_icp30");
//            CustomerContext.getInstance().setCustomerID("ACME");
//            DriverUniqueIdentifierReader dur = new DriverUniqueIdentifierReader("UOM_ICP_Send", "ValidateBR");
            DriverUniqueIdentifierReader dur = new DriverUniqueIdentifierReader("UOM_ICP_Send", "ValidateBR", "MY_UNIQUE_IDENTIFIER_XPATH");
//            System . out.println("dur = " + dur.getUniqueIdentifier(message));
        }
        catch (Exception e)
        {
//            System . out.println("e.getMessage() = " + e.getMessage());
            e.printStackTrace();
        }
    }
}
