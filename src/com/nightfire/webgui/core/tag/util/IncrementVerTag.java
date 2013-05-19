/**
 * Copyright (c) 2004 NeuStar, Inc.  All rights reserved.
 *
 * $Header: $
 */

package com.nightfire.webgui.core.tag.util;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;

import com.nightfire.framework.message.common.xml.XMLGenerator;
import com.nightfire.framework.message.common.xml.XMLPlainGenerator;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.security.domain.DomainProperties;
import com.nightfire.security.domain.DomainPropException;
import com.nightfire.webgui.core.DataHolder;
import com.nightfire.webgui.core.tag.TagUtils;
import com.nightfire.webgui.core.tag.VariableTagBase;
import com.nightfire.webgui.gateway.GatewayConstants;

/**
 * This tag gets the current value of the VER field for given PON.
 * and increments the current VER value by one.
 * The incremented value is set to the variable var
 * This VALUE will be accessible via the value specified by "var" in the jsp tag.
 */
public class IncrementVerTag extends VariableTagBase
{
    /**
     * Starts procesing of this tag.
     *
     * Here we first determine the current value of VER attribute and
     * based on it we would make call to calculate the new
     * incremented value of the series.
     *
     * Currently this Tag only implements this utility
     * for domain property: AUTOINCREMENTVERLSR.
     *
     * @ throws JspException on processing error.
     */

    private String PON = "";
    private String ILEC = "";
    private String version = "";
    private String serviceType = null;
    private String serviceGroupType = null;
    private String incrementUnconditionally = "false";
    private static String INITIAL_VER_VALUE = "00";

    /**
     * Sets PON to be used in accessing the current version value
     *
     * @param pon value to passed
     * @throws JspException
     */

    public void setPON(String pon)  throws JspException
    {
       if ( StringUtils.hasValue(pon) )
             this.PON = (String)TagUtils.getDynamicValue("pon", pon, String.class, this, pageContext);
    }

    /**
     * Sets ILEC to be used in accessing the current version value
     *
     * @param ILEC value to passed
     * @throws JspException
     */
    public void setILEC(String ILEC)  throws JspException
    {
       if ( StringUtils.hasValue(ILEC) )
             this.ILEC = (String)TagUtils.getDynamicValue("ILEC", ILEC, String.class, this, pageContext);
    }

    /**
     * Sets serviceType to be used in accessing the current version value
     *
     * @param serviceType value to passed
     * @throws JspException
     */
    public void setServiceType (String serviceType) throws JspException {
        this.serviceType = (String) TagUtils.getDynamicValue("serviceType", serviceType, String.class, this, pageContext);
    }

    /**
     * Sets serviceGroupType to be used in accessing the current version value
     *
     * @param serviceGroupType value to passed
     * @throws JspException
     */
    public void setServiceGroupType (String serviceGroupType) throws JspException {
        this.serviceGroupType = (String) TagUtils.getDynamicValue("serviceGroupType", serviceGroupType, String.class, this, pageContext);
    }

     /**
     * Sets if VER can be incremented unconditionally
     *
     * @param incrementUnconditionally value to passed
     * @throws JspException
     */
    public void setIncrementUnconditionally (String incrUnconditionally) throws JspException {
        this.incrementUnconditionally = (String) TagUtils.getDynamicValue("incrementUnconditionally", incrUnconditionally, String.class, this, pageContext);
    }

    public int doStartTag() throws JspException
    {
        super.doStartTag();

        String newVersionVal = null;

        try
        {
            boolean obtainNextVer = false;
            if (serviceGroupType.equals(GatewayConstants.ASR_ORDER))
            {
                if (isAutoIncrementVerASR())
                {
                    obtainNextVer = true;
                }
            }
            else if (serviceGroupType.equals(GatewayConstants.LSR_ORDER)) {
                if (isAutoIncrementVerLSR())
                {
                   obtainNextVer = true;
                }
            }
            if(incrementUnconditionally.equalsIgnoreCase("true"))
            {
               obtainNextVer = true;
            } 

            if (obtainNextVer)
            {
                String maxVerErrMsg = "The VER field has already reached the maximum value (99, ZZ, zz).";
                version = getCurrentVersion();
                if (version.equalsIgnoreCase("zz") || version.equalsIgnoreCase("99"))
                {
                    log.error("doStartTag: Error: " + maxVerErrMsg);
                    throw new JspTagException(maxVerErrMsg);
                }
                // In case  unconditional increment first version value will be "00".
                if(!(StringUtils.hasValue(version)) && incrementUnconditionally.equals("true"))
                {
                    newVersionVal = INITIAL_VER_VALUE;
                }
                else
                {
                    newVersionVal= getNextVersionValue (version);
                }

            }

            if (log.isDebugEnabled())
                log.debug("doStartTag: setting [var] attribute as [" + newVersionVal + "].");

            //Now set the Version value on the location specified by var and scope variables.
            setVarAttribute(newVersionVal);
        }
        catch (Exception e )
        {
            String err = "Unable to increment the VER field value. Error: " + e.getMessage();
            log.error("doStartTag: " + err);
            throw new JspTagException(err);
        }

        return SKIP_BODY;
    }

    public void release()
    {
       super.release();
    }

    /**
     * This method detect whether the current customerId
     * present inside the context has the AutoIncrementVerLSR or not
     * @return - result whether the field for the customerId is AutoIncrementVerLSR
     */
    private boolean isAutoIncrementVerLSR ()
    {
        return isAutoIncrement ( DomainProperties.AUTOINCREMENTVERLSR );
    }

	/**
     * This method detect whether the current customerId
     * present inside the context has the AutoIncrementVerASR or not
     * @return - result whether the field for the customerId is AutoIncrementVerASR
     */
	private boolean isAutoIncrementVerASR()
	{
        return isAutoIncrement ( DomainProperties.AUTOINCREMENTVERASR );
	}

    /**
     * This method takes help of DomainProperties class and detect whether the current customerId
     * present inside the context has the AutoIncrement or not for the input field
     * @return - result whether the field for the customerId is AutoIncrement
     */
    private boolean isAutoIncrement(String fieldName)
    {
        boolean returnVal = false;
        try
        {
            DomainProperties domain = DomainProperties.getInstance();
            if (domain.isAutoIncrementField(fieldName))
                returnVal = true;
            if ( log.isDebugEnabled() )
                log.debug("isAutoIncrement: returning AUTOINCREMENT property as [" + returnVal + "] for field ["+fieldName+"]." );
        }
        catch (DomainPropException e)
        {
            log.error("isAutoIncrement: Unable to retrive the value of AutoIncrement for field ["+fieldName+"]");
            log.error("isAutoIncrement: Error: " + e.toString());
        }
        catch (Exception e)
        {
            log.error("isAutoIncrement: Unable to retrive the value of AutoIncrement for field ["+fieldName+"]");
            log.error("isAutoIncrement: Error: " + e.toString());
        }
        return returnVal;
    }

    /**
     * This method returns the "00" string the case if version is null
     * else the incremented next version value would be returned
     *
     * @param version - the value whose next incremented version to be generated
     * @return String - containing the new Incremented Version value
     */
    private String getNextVersionValue(String version)
    {
        /**
         * setting version to default value "<empty String>" in the case
         * if version is found null
         */
        if (version == null)
            version = "";

        final int numChars = 2;
        char fillChar = '0';
        final boolean leftPad = true;
        /**
         * This call to padString method make the version string to be of
         * <numChar> char long if version has length less than <numChars> chars
         * or if version is <empty String> else version remain unchanged.
         *
         * If the version is of lenth one and its first character is digit
         * then '0' would be attach as prefix else 'a'/'A' conditionally
         *
         */

         if (version.length()==1 && !Character.isDigit(version.charAt(0)))
         {
             fillChar = Character.isLowerCase(version.charAt(0)) ? 'a' : 'A';
         }

        version = StringUtils.padString(version, numChars, leftPad, fillChar);
        String newVersion = "";

        char ch1 = version.charAt(0);
        char ch2 = version.charAt(1);

        switch (ch2)
        {
          case 'z':
          case 'Z':
          case '9':  ch1=incrementChar(ch1); break;
        }
        ch2= incrementChar(ch2);
        // The empty code "" converts the characters in to the String format
        newVersion=ch1+""+ch2;
        if (log.isDebugEnabled())
            log.debug("getNextVersionValue(): returning new version as [" + newVersion + "]");
        return newVersion;
	}

	/**
     * Intermediate method to update the character based on cyclic case
     * as "BZ" version value would have "CA" as next Incremented Version value
     * @param ch character to be changed
     * @return changed characted
     */
	private char incrementChar(char ch)
    {
        switch (ch)
        {
          case 'z':  ch='a'; break;
          case 'Z':  ch='A'; break;
          case '9':  ch='0'; break;
          default : ch++;
        }
        return ch;
    }

    /**
     * This method makes a DB call to bring the currentVersion in case
     * of autoIncrement in respect to customerId, PON, Supplier and ServiceType
     * and on the base of result obtain the value of version would be returned
     *
     * @return version
     */
    private String getCurrentVersion ()
    {
        String currentVER = "";
        try
        {
            DataHolder dataHolder = new DataHolder();
            XMLGenerator header = new XMLPlainGenerator("Header");
            XMLGenerator body = new XMLPlainGenerator("Body");
            body.create("Info");
            body.setValue(body.getNode("Info"), "ServiceType", serviceType);
            body.setValue(body.getNode("Info"), "PON", PON);
            body.setValue(body.getNode("Info"), "Supplier", ILEC);
            header.setValue("Action", "query-ver-value");
            dataHolder = TagUtils.sendRequest(header, body, pageContext);

            DataHolder responseObj = dataHolder;
            XMLPlainGenerator parser = new XMLPlainGenerator(responseObj.getBodyStr());
            currentVER = parser.getAttribute("DataContainer.Data(0).VER", "value");

            if (log.isDebugEnabled())
                log.debug("getCurrentVersion: returning currentVER as ["+currentVER+"].");
        }
        catch (Exception e)
        {
            log.error("getCurrentVersion: Error: " + e.toString());
        }
        return currentVER;
    }

}
