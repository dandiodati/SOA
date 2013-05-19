package com.nightfire.framework.util;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;

/**
 * Utility class for Exception
 */
public class ExceptionUtils {

    /**
     * Checks if passed exception message is Business Rule Error
     *
     * @param e Exception object passed
     * @return true if the exception contains a valid BR error message.
     */
    public static boolean isBRError(Exception e)
    {
        if (e == null)
            return false;

        return isBRError(e.toString());
    }

    /**
     * Checks if passed message is Business Rule Error
     *
     * @param errorMsg String error message passed
     * @return true if the error message contains a valid BR error message.
     */
    public static boolean isBRError(String errorMsg)
    {
        if (!StringUtils.hasValue(errorMsg))
            return false;

        boolean retValue = false;
        String brStr     = null;

        try
        {
            int startBRIdx = errorMsg.indexOf("<Errors>");
            // Not taking lastIndexOf BR error end tag '</Errors>', as the same exception message can contain repetion or BR error message
            int endBRIdx = errorMsg.indexOf("</Errors>");

            if (endBRIdx != -1)
                 endBRIdx = endBRIdx + "</Errors>".length();

            if (startBRIdx != -1 && endBRIdx != -1)
                brStr = errorMsg.substring(startBRIdx, endBRIdx);

            XMLLibraryPortabilityLayer.convertStringToDom(brStr);

            retValue = true;
        }
        catch(Exception exp)
        {
            //ignore any exception
        }

        return retValue;
    }

    public static void main (String[] args)
    {
        String msg1 = "Hello There!!! this is not an business rule error.";
        String msg2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Errors>\n" +
                "<ruleerrorcontainer>\n" +
                "<ruleerror>\n" +
                "<RULE_ID value=\"LSR_ADMIN_AN_2 \"/>\n" +
                "<MESSAGE value=\"AN is required when the first position of the REQTYP field is &quot;C&quot;, otherwise prohibited.   \"/>\n" +
                "<CONTEXT value=\"/Request/lsr_order/lsr/lsr_adminsection/AN\"/>\n" +
                "<CONTEXT_VALUE value=\"5105555555\"/>\n" +
                "</ruleerror>" +
                "</ruleerrorcontainer>\n" +
                "</Errors>";
        System.out.println("msg1 isBRError: " + isBRError(new MessageException(msg1)));
        System.out.println("msg2 isBRError: " + isBRError(new MessageException(msg2)));
    }
}
