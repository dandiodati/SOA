/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag;

import com.nightfire.webgui.core.beans.*;
import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.tag.message.BodyTag;
import com.nightfire.webgui.core.tag.util.WSPInfoUtil;
import com.nightfire.framework.debug.*;

import  javax.servlet.*;
import  javax.servlet.http.*;
import  javax.servlet.jsp.*;
import  javax.servlet.jsp.tagext.*;
import  javax.servlet.jsp.PageContext;

import  com.nightfire.framework.util.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.xml.*;
import com.nightfire.webgui.gateway.svcmeta.ServiceDef;
import com.nightfire.framework.message.common.xml.*;

import org.w3c.dom.Document;


import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.File;

// uses specific apache implementation for Expression Language(EL) evalutation.
// When this becomes a standard this class will disapear.
import org.apache.taglibs.standard.lang.support.ExpressionEvaluatorManager;
import com.nightfire.framework.message.util.xml.ParsedXPath;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.message.parser.MessageParserException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.db.*;
import com.nightfire.framework.locale.NFLocale;
import com.nightfire.framework.repository.RepositoryManager;
import com.nightfire.security.domain.*;
import com.nightfire.security.tpr.TradingPartnerRelationship;
import com.nightfire.security.tpr.TPRException;
import com.nightfire.security.*;
import com.nightfire.security.SecurityException;
import com.nightfire.security.store.StoreProviderBase;
import com.nightfire.mgrcore.im.query.QueryEngine;
import com.nightfire.mgrcore.utils.EnvironmentUtils;
import com.nightfire.mgrcore.repository.RepositoryCategories;


/**
 * common utilties for tags
 */

public class TagUtils implements ServletConstants
{

    /**
     * special chars that need to be esaped
     */
    protected static final char[] SPECIAL_ATTR_CHARS =
        { '\n', '\r', '\"', '&', '\'', '<', '>'}; // This is sorted!

    /**
     * The escape codes for each special char.
     */
    protected static final String[] ATTR_ESCAPES = { "&#13;",
                                                  "&#10;",
                                                  "&quot;",
                                                  "&amp;",
                                                  "&apos;",
                                                  "&lt;",
                                                  "&gt;"};

    /**
     * variables for Service Provider Caching
     */
    public static final String SORTED_BY_SPID      = "SrtSPID";
    public static final String SORTED_BY_SPNAME    = "SrtSPName";
    public static final String SPDATA_CLEANUP_TIME = "SPDATA_CLEANUP_TIME";
    public static final String SPDATA_FIELD_ID     = "FIELD_ID";

    public static final String QUERY_RESULT_COUNT     = "QueryResultCount";

    // to store <query-criteria[-CUSTOMERID[-SUBDOMAINID]]>, <SPID JS data> mapping
    protected static HashMap spidData = new HashMap();

    // to store <query-criteria[-CUSTOMERID[-SUBDOMAINID]]>, <lastUpdatedTime> mapping
    protected static HashMap spidLastUpdateTime = new HashMap();


    public static final int CONVERT_TO_SERVER_TZ      = 1;
    public static final int CONVERT_TO_CLIENT_TZ   = 2;

    public static final String IMAGES_STR           = "/images/";
    public static final String IMG_NFLOGO_GIF_STR   = "/images/NFLogo.gif";
    public static final String NFLOGO_GIF_STR       = "/NFLogo.gif";
    public static final String NEUSTAR_STR          = "NeuStar";


     /**
     * Escapes special characters for use in an HTML attribute
     *
     * @param val  The value to escape
     * @return String The string value with special characters escaped.
     *
     */
    public static final String escapeAttr(String val)
    {
        if (!StringUtils.hasValue(val))
            return val;

        StringBuffer elem = new StringBuffer();

        int len = val.length();
        for (int i = 0; i < len; i++)
        {
            char c = val.charAt(i);
            // we do a binary search only because they don't provide any
            // other kind of array search.  There's no real performance
            // gain to optimizing the search since we only have seven elements
            int pos;
            if ( (pos = Arrays.binarySearch(SPECIAL_ATTR_CHARS, c)) >= 0 )
                // escape the char
                elem.append(ATTR_ESCAPES[pos]);
            else
                // OK as-is
                elem.append(c);
        }

        return elem.toString();
    }


    /**
    * This function keeps only numbers and characters
    * and spaces. All the characters other this will be
    * removed.
    *
    * @param val  The value to filter
    * @return String Filtered string
    *
    */
   public static final String filterSpecialChars(String val)
   {
       // returning empty string in case of null
       if (!StringUtils.hasValue(val))
           return "";

       // excluding double quote (") char as
       // javascript error will occur if it present.
       val = StringUtils.replaceSubstrings(val, "\"", "");
       val = StringUtils.replaceSubstrings(val, "\\", "\\\\");
       val = StringUtils.replaceSubstrings(val, "'", "\\'");
       val = StringUtils.replaceSubstrings(val, "\\n", "n");

       return val;
   }



    /**
     * Returns the context path of the current webapp.
     * If the context path can not be obtained then an empty string is
     * returned.
     */
    public static final String getWebAppContextPath(PageContext pc)
    {
      return ServletUtils.getWebAppContextPath(pc.getRequest() );
    }


    /**
     * <b>This method should never be called directly by any tag.</b>
     * The {@link NFTagSupport NFTagSupport} and {@link NFBodyTagSupport NFBodyTagSupport}  base tags use these methods.
     *
     * Sets an attribute and value for dynamic evaluation.
     * This must be called in setter methods, and later the getDynAttribute must
     * be called to obtain the value.
     *
     *
     * @param attrMap Map to hold attribute expression for later evaulation.
     * @param attrName The name of the attribute.
     * @param object The expression or could be the actual value.
     * @param type The expected type for the attribute value after evaluation.
     * @exception JspException if an error occurs
     */
    protected static final void setDynAttribute(Map attrMap, String attrName, Object object, Class type) throws JspException
    {

       if (attrMap == null)
            throw new JspException("Null attribute map passed in");

       if (!StringUtils.hasValue(attrName)|| type == null)
           throw new JspException("Attribute name or expected type is null when setting dynamic attribute.");

        DynAttrEntry entry = new DynAttrEntry(attrName, type, object);
        attrMap.put(attrName, entry);


    }


    /**
     * <b>This method should never be called directly by any tag.</b>
     * The {@link NFTagSupport NFTagSupport} and {@link NFBodyTagSupport NFBodyTagSupport}  base tags use these methods.
     *
     * Returns a dynamic value for a attribute. A attribute may evaluate to a runtime statement
     * or expression language statement.
     *
     * <b>NOTE: Any values that are strings, get evaulated as expression language.
     * So if a runtime expression evalulates to a String which contains a '${' it
     * will get evaluated again as an expression language statement. This rare case should never occur
     * and will not be an issue once JSP supports both runtime expressions and expresion language.</b>
     *
     * @param attrMap Map to holding the attribute expression for evaulation.
     * @param attrName The name of the attribute.
     * @param tag The tag which owns this attribute setter method.
     * @param context a <code>PageContext</code> value
     * @return Returns the object that is evaluated via expression language.
     * The object will always be of type expectedType and can be null.
     *
     * @exception JspException if the expectedType does not match or if an value contains an invalid expression language statement.
     */
    public static final Object getDynAttribute(Map attrMap, String attrName, TagSupport tag, PageContext context) throws JspException
    {

        String webAppName = TagUtils.getWebAppContextPath(context);

        DebugLogger log = DebugLogger.getLogger(webAppName, TagUtils.class);

        if (attrMap == null)
            throw new JspException("Null attribute map passed in");

        DynAttrEntry entry = (DynAttrEntry)attrMap.get(attrName);

        // if entry or value does not exist this may have been an optional attribute
        // or it was not set.

       if ( entry == null || entry.value == null)
            return null;


        String elExpression = "";

        Object result = entry.value;


       if (log.isDebugEnabled() )
            log.debug("Getting dynamic value for:\n  Tag [" + StringUtils.getClassName(tag)+ "]\n  Attribute ["+ attrName +"]\n  Value [" + entry.value +"]\n  Value Type[" + StringUtils.getClassName(entry.value) +"]\n  Expected Value Type [" + entry.type.getName() +"]");


       try {
           // if this value is a String then check for a possible el expression
           if (entry.value instanceof String) {

              elExpression = (String) entry.value;
              if ( !StringUtils.hasValue(elExpression) )
                 return null;

              // if there is an el expression evaluate it
              // otherwise return the string as is
              int start =  elExpression.indexOf("${");
              if ( start > -1 && elExpression.indexOf("}",start +1) > -1 )
                 result =  ExpressionEvaluatorManager.evaluate(entry.name, elExpression, entry.type, tag, context );
              else
                 result = elExpression;

           }


           if ( result != null && !entry.type.isInstance(result) ) {
               throw new JspException("Resulting class ["+ result.getClass().getName() + "] does not match expected type [" + entry.type.getName() +"]");
           }



       } catch ( Exception e) {
          String error =  "Failed to get dynamic value for tag [" + StringUtils.getClassName(tag)+ "]\n  attribute ["+ entry.name +"]\n  value [" + entry.value +"]\n  value type[" + StringUtils.getClassName(entry.value) +"]\n  expected object type [" + entry.type.getName() +"]\n: " + e.getMessage();
          log.error(error, e);
          throw new JspException(error);
       }

       return result;
    }

    /**
     *
     * Returns a dynamic value for a attribute. A attribute may evaluate to a runtime statement
     * or expression language statement.
     *
     * <b>NOTE: Any values that are strings, get evaulated as expression language.
     * So if a runtime expression evalulates to a String which contains a '${' it
     * will get evaluated again as an expression language statement. This rare case should never occur
     * and will not be an issue once JSP supports both runtime expressions and expresion language.</b>
     *
     * @deprecated This method should not longer be used.
     * @param attributeName The name of the attribute for this value.
     * @param value The value that was passed into this attribute's setter method.
     * @param expectedType - The expected class type that this value should be.
     * @param tag The tag which owns this attribute setter method.
     * @param tagPageContext The page context for the tag passed in.
     * @return Returns the object obtained by an attribute's setter method on a tag.
     * The object will always be of type expectedType and can be null.
     *
     * @exception JspException throws if the expectedType does not match or if an value contains an invalid expression language statement.
     */
    public static final Object getDynamicValue(String attributeName, Object value, Class expectedType, TagSupport tag, PageContext tagPageContext) throws JspException
    {
      String webAppName = TagUtils.getWebAppContextPath(tagPageContext);

      DebugLogger log = DebugLogger.getLogger(webAppName, TagUtils.class);


       String elExpression = "";

       Object result = value;

       //if (log.isDebugEnabled() )
       //   log.debug("Getting dynamic value for:\n  Tag [" + StringUtils.getClassName(tag)+ "]\n  Attribute ["+ attributeName +"]\n  Value [" + value +"]\n  Value Type[" + value.getClass().getName() +"]\n  Expected Value Type [" + expectedType.getName() +"]");

       if ( value == null)
          return null;

       try {
           // if this value is a String then check for a possible el expression
           if (value instanceof String) {

              elExpression = (String) value;
              if ( !StringUtils.hasValue(elExpression) )
                 return null;

              // if there is an el expression evaluate it
              // otherwise return the string as is
              int start =  elExpression.indexOf("${");
              if ( start > -1 && elExpression.indexOf("}",start +1) > -1 )
                 result =  ExpressionEvaluatorManager.evaluate(attributeName, elExpression, expectedType, tag, tagPageContext );
              else
                 result = elExpression;

           }


           if ( result != null && !expectedType.isInstance(result) ) {
              throw new JspException("Resulting class ["+ result.getClass().getName() + "] does not match expected type");
           }



       } catch ( Exception e) {
          String error =  "Failed to get dynamic value for tag [" + StringUtils.getClassName(tag)+ "]\n  attribute ["+ attributeName +"]\n  value [" + value +"]\n  value type[" + StringUtils.getClassName(value) +"]\n  expected object type [" + expectedType.getName() +"]\n: " + e.getMessage();
          log.error(error);
          log.error("",e);
          throw new JspException(error);
       }

       return result;
    }

    /**
     * Ensures IDs are suitable for html link purposes (valid URL)
     */
    public static final String normalizeID(String id)
    {
        return id.replace('#', '_');
    }


    /**
     * Sets a bean in the message data cache associated with messageId.
     * It also places a reference to the bean in the current request.
     * @param pcxt The page context to of the current tag.
     * @param messageId The message id to place the bean in the message cache. Used to
     * group a set of beans associated with a specific request.
     *  If this value is null or an empty string then a new unique message id is created.
     *  This is the same sequence of generated numbers that the ControllerServlet uses
     *  so there will not be a conflict.
     * @param beanName The name of the bean which must be unique for a given messageId.
     * @param bean The bean that should be placed in the message cache.This can be an instance of
     * NFBean, or any other type of object.
     * @return The message id that was used to place this bean in the message cache.
     * Can be used to send to the ControllerServlet for future processing.
     *
     */
    public static final String setBeanInMsgCache(PageContext pcxt, String messageId, String beanName, Object bean) throws JspException
    {
       String webAppName = TagUtils.getWebAppContextPath(pcxt);
       DebugLogger log = DebugLogger.getLogger(webAppName, TagUtils.class);



       KeyTypeMap dataCache = (KeyTypeMap)pcxt.getSession().getAttribute(ServletConstants.MESSAGE_DATA_CACHE);

       if (dataCache == null) {
          log.error("Failed to get " + ServletConstants.MESSAGE_DATA_CACHE);
          throw new JspException("Failed to get " + ServletConstants.MESSAGE_DATA_CACHE);
       }


       if (!StringUtils.hasValue(messageId) ) {
          messageId = String.valueOf(ServletUtils.generateMessageID(pcxt.getSession()));

          if (log.isDebugEnabled() )
             log.debug("Creating a new message id [" + messageId + "].");
       }


       Object oldBean = dataCache.put(messageId, beanName, bean);

       pcxt.getRequest().setAttribute(beanName, bean);

       if ( oldBean != null)
          log.warn("Bean with message id [" + messageId + "] is getting replaced by a new object.");


       // sets the message id at the defined var variable.
      return messageId;

   }


    /**
     * Returns a bean object from the message cache.
     * @param pcxt The page context to of the current tag.
     * @param messageId The message id to lookup the corresponding request data in the message cache.
     * @param beanName The name of the bean which must be unique for the  given messageId.
     * @return The bean object in the message cache or null if a bean is not found.
     */
    public static final Object getBeanInMsgCache(PageContext pcxt, String messageId, String beanName) throws JspException
    {

       String webAppName = TagUtils.getWebAppContextPath(pcxt);
       DebugLogger log = DebugLogger.getLogger(webAppName, TagUtils.class);

       if ( !StringUtils.hasValue(messageId) ) {
           log.error(ServletConstants.MESSAGE_ID + " must be no be null.");
           throw new JspException(ServletConstants.MESSAGE_ID +" must not be null.");
        }


       KeyTypeMap dataCache = (KeyTypeMap)pcxt.getSession().getAttribute(ServletConstants.MESSAGE_DATA_CACHE);

       if (dataCache == null) {
          log.error("Failed to get " + ServletConstants.MESSAGE_DATA_CACHE);
          throw new JspException("Failed to get " + ServletConstants.MESSAGE_DATA_CACHE);
       }

      return dataCache.get(messageId, beanName);

    }


   /**
    * Replaces any spaces in the string with html &nbsp; entities.
    * Used for displaying on html pages.
    * @param str The string to escape spaces in.
    * @return The string with replaced spaces.
    */
   public static final String escapeHtmlSpaces(String str)
   {
       // replace any spaces with html entities so that no line wrapping
       // happens
       if (StringUtils.hasValue(str) )
          str = StringUtils.replaceSubstrings(str, " ", "&nbsp;");
      return str;
   }


 /**
     * Indicates if this field is a hidden type of field.
     * A field is considered to be hidden if it has a custom value
     * with a value of {@link TagConstants.DATA_TYPE_HIDDEN} or
     * if it has a base data type that is of
     * type {@link TagConstants.DATA_TYPE_HIDDEN}.
     *
     * @param field a <code>Field</code> value
     * @return true if this is a hidden field, otherwise false.
     */
    public static final boolean isHidden(Field field)
    {
        boolean results = false;

        if ( field != null) {
            if (ServletUtils.getBoolean(field.getCustomValue(TagConstants.DATA_TYPE_HIDDEN), false) ||
                field.getDataType().isInstance(TagConstants.DATA_TYPE_HIDDEN))
                results = true;
        }

        return results;

    }

 /**
     * Indicates if this field is a hidden type of field.
     * A field is considered to be hidden if it has a custom value
     * with a value of {@link BodyTag.USE_CLIENT_TIMEZONE}.
     *
     * @param field a <code>Field</code> value
     * @return true if this is a field, has custom value as specified above.
     */
    public static final boolean isDateConversionReq (Field field)
    {
        String serverTZ = field.getCustomValue(BodyTag.USE_CLIENT_TIMEZONE);
        if ((field != null) && StringUtils.hasValue(serverTZ) && !(serverTZ.equalsIgnoreCase("false")))
            return true;
        return false;
    }


 /**
     * Indicates if this field needs to be sorted.
     * A field will be sorted if it has a custom value
     * with a value of {@link BodyTag.SORTED_CUSTOM_VALUE}.
     *
     * @param field a <code>Field</code> value
     * @return true if this is a sorted field, otherwise false.
     */
    public static final boolean isSorted (Field field)
    {
        if (field != null)
        {
            return StringUtils.getBoolean (field.getCustomValue(BodyTag.SORTED_CUSTOM_VALUE), false);
        }
        return false;
    }

    /**
     * This method returns a boolean based on the value of
     * custom key {@link BodyTag.GUI_DISPLAY_CONTROL_KEY_CUSTOM_VALUE}
     * and the corresponding value in DomainProperties.
     * @param field Field
     * @param pageContext PageContext obj
     * @param domainProperties DomainProperties obj
     * @return true if custom value and domain property (1 for true) are true
     */
    public static final boolean isDisplayField(Field field, PageContext pageContext, DomainProperties domainProperties)
    {
        if (field != null)
        {
            return shouldDisplayField (field.getCustomValue(BodyTag.GUI_DISPLAY_CONTROL_KEY_CUSTOM_VALUE), pageContext, domainProperties);
        }

        return true;
    }

    /**
     * This method returns a boolean based on the value of
     * custom key {@link BodyTag.GUI_DISPLAY_CONTROL_KEY_CUSTOM_VALUE}
     * and the corresponding value in DomainProperties.
     * @param fieldGrp FieldGroup
     * @param pageContext PageContext obj
     * @param domainProperties DomainProperties obj
     * @return true if custom value and domain property (1 for true) are true
     */
    public static final boolean isDisplayFieldGroup(FieldGroup fieldGrp, PageContext pageContext, DomainProperties domainProperties)
    {
        if (fieldGrp != null)
        {
            return shouldDisplayField (fieldGrp.getCustomValue(BodyTag.GUI_DISPLAY_CONTROL_KEY_CUSTOM_VALUE), pageContext, domainProperties);
        }

        return true;
    }

    public static final boolean isClientTimezoneSupported(PageContext pageContext)
    {
        String cid = getCustomerId(pageContext);
        String boxIdentifier = pageContext.getServletContext().getInitParameter(ServletConstants.BOX_IDENTIFIER);
        if (ServletConstants.SOA_BOX_IDENTIFIER.equalsIgnoreCase(boxIdentifier))
        {
            try
            {
                DomainProperties dp = DomainProperties.getInstance(cid);
                if (StringUtils.hasValue(dp.getSupportClientTimezone()));
                    return (StringUtils.getBoolean(dp.getSupportClientTimezone()));
            }
            catch(DomainPropException de)
            {
                return false;
            }
            catch(FrameworkException fe)
            {
                return false;
            }
        }
        return true;
    }

    public static final boolean shouldDisplayField (String key, PageContext pageContext, DomainProperties domainProperties)
    {
        boolean returnVal = true;

        /** compare the custom value configured in the meta file
         * with the domain configuration in the Domain Properties table.
         */
        if (StringUtils.hasValue(key))
        {
            String webAppName = getWebAppContextPath(pageContext);
            DebugLogger log = DebugLogger.getLogger(webAppName, TagUtils.class);

            ArrayList<String> domainGuiDisplayKey = domainProperties.getGuiDisplayControlKeys();
            returnVal = domainGuiDisplayKey.contains(key);

            if (log.isDebugEnabled())
            {
                log.debug("shouldDisplayField: The guiDisplayKey value in the Domain Property is :[" +
                    key + "]; returning value as [" + returnVal + "]");
            }
        }
        return returnVal;
    }

    /**
     * This method returns a boolean based on the custom values of
     * {@link BodyTag.AUTO_INCREMENT_FIELD_VALUE} and the corressponding value in
     *  DomainProperties.
     * @param field Field
     * @param pageContext PageContext obj
     * @param domainProperties DomainProperties obj
     * @return true if custom value and domain property (1 for true) are true
     */
    public static final boolean isAutoIncremented(Field field, PageContext pageContext, DomainProperties domainProperties)
    {
        boolean returnVal = false;

        if (field != null)
        {
            String webAppName = getWebAppContextPath(pageContext);
            DebugLogger log = DebugLogger.getLogger(webAppName, TagUtils.class);
            String autoIncFieldVal = field.getCustomValue(BodyTag.AUTO_INCREMENT_FIELD_VALUE);
            /**
             * Comapring the value of autoIncrementField and on the basis of its value the
             * corressponding value of DomainProperties would be called.
             * We also need to check its presenece, nullability and emptyness.
             */
            if (autoIncFieldVal != null && (!autoIncFieldVal.equalsIgnoreCase("")))
            {
                returnVal = domainProperties.isAutoIncrementField(autoIncFieldVal);

                if (log.isDebugEnabled())
                {
                    log.debug("isAutoIncremented: Checking the AutoIncrement flag for the Domain Property [" +
                        autoIncFieldVal + "] and returning value as ["+returnVal+"]");
                }
            }
        }

        return returnVal;
    }


    /**
     * Utility method for sending a request to the processing layer.
     *
     * @param  headerData   Header portion of the request data.
     * @param  bodyData     Body portion of the request data.
     * @param  pageContext  PageContext object.
     *
     * @return  DataHolder response data object.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public static final DataHolder sendRequest(Object headerData, Object bodyData, PageContext pageContext) throws JspException
    {
       String webAppName = getWebAppContextPath(pageContext);
       DebugLogger log = DebugLogger.getLogger(webAppName, TagUtils.class);
       ServletContext sContext = pageContext.getServletContext();


        XMLBean dataBean;
        try {

            if ((headerData instanceof XMLGenerator) && (bodyData instanceof XMLGenerator))
                {
                    dataBean = new XMLBean((XMLGenerator)headerData, (XMLGenerator)bodyData);
                }
            else if ((headerData instanceof Document) && (bodyData instanceof Document))
                {
                    dataBean = new XMLBean((Document)headerData, (Document)bodyData);
                }
            else if ((headerData instanceof String) && (bodyData instanceof String))
                {
                    dataBean = new XMLBean((String)headerData, (String)bodyData);
                }
            else
                {
                    String errorMessage = "sendRequest(): Unsupported header and body data-type [" + headerData.getClass().getName() + ", " + bodyData.getClass().getName() + "].  Only XMLGenerator, Document, and String are supported.";

                    log.error(errorMessage);

                    throw new JspException(errorMessage);
                }


            DataAdapter     dataAdapter             = (DataAdapter)sContext.getAttribute(DATA_ADAPTER);

            ProtocolAdapter protocolAdapter         = (ProtocolAdapter)sContext.getAttribute(PROTOCOL_ADAPTER);

            HttpSession     session                 = pageContext.getSession();

            DataHolder      transformedRequestData  = dataAdapter.transformRequest(dataBean, (HttpServletRequest)pageContext.getRequest());

            DataHolder      responseData            = protocolAdapter.sendRequest(transformedRequestData, session);

            DataHolder      transformedResponseData = dataAdapter.transformResponse(responseData, session);

            return transformedResponseData;
        }
        catch (ServletException e) {
            log.error("Failed to send request: " + e.getMessage());
            throw new JspException("Failed to send request",e);
        }

    }

    /**
     * Returns the label to be used for a field's display name.
     *
     * @param field a <code>Field</code> value
     * @param mode a <code>String</code> value
     * @return a <code>String</code> value
     */
    public static String getFieldLabel(Field field, String mode)
    {

        String abbrev = field.getAbbreviation();
        String fieldName = field.getDisplayName();

        if (StringUtils.hasValue(abbrev) ) {
            // if true, display field abbreviations instead of display names
            if (mode.equals(TagConstants.ONLY_ABBREVS_LABEL) ) {
                fieldName = abbrev;
            } else if (mode.equals(TagConstants.BOTH_LABEL)) {
                if (!abbrev.equalsIgnoreCase(fieldName) )
                    fieldName = new StringBuffer(fieldName).append(" (").append(abbrev).append(")").toString();
            }
            // else just use default display name only

        }
        return fieldName;
    }

    /**
     * Returns the label to be used for a field group's display name.
     *
     * @param grp a <code>FieldGroup</code> value
     * @param mode a <code>String</code> value
     * @return a <code>String</code> value
     */
    public static String getFieldGroupLabel(FieldGroup grp, String mode)
    {
        return grp.getDisplayName();
    }

    /**
     * Method to get CustomerId from the Page Context
     *
     * @param page PageContext obj
     * @return string as customer id
     */
    public static String getCustomerId(PageContext page)
    {
         HttpSession session = page.getSession();

         ServletContext context = page.getServletContext();
         if( session != null)
         {

            SessionInfoBean sBean = (SessionInfoBean) session.getAttribute(ServletConstants.SESSION_BEAN);
            return sBean.getCustomerId();

        }

        return null;
    }

    /**
     * A tag utility method that allows an XMLTemplateGenerator instance to be
     * created from an XMLPlainGenerator instance.
     *
     * @param  pageContext   PageContext object.
     * @param  resource  Source XMLPlainGenerator instance.
     *
     * @return  A new XMLTemplateGenerator instance.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public static XMLTemplateGenerator getTemplateGenerator(PageContext pageContext, XMLPlainGenerator resource) throws JspException
    {
       String      webAppName = getWebAppContextPath(pageContext);

       DebugLogger log        = DebugLogger.getLogger(webAppName, TagUtils.class);

        try
        {
            log.debug("getTemplateGenerator(): Creating an XMLTemplateGenerator object from an XMLPlainGenerator object ...");

            XMLTemplateGenerator templateGenerator = new XMLTemplateGenerator(resource.getOutputCopy().getDocument());

            log.debug("getTemplateGenerator(): Returning a newly created XMLTemplateGenerator object ...");

            return templateGenerator;
        }
        catch (Exception e)
        {
            String errorMessage = "Failed to create an XMLTemplateGenerator object from an XMLPlainGenerator object:\n" + e.getMessage();

            log.error("getTemplateGenerator(): " + errorMessage);

            throw new JspTagException(errorMessage);
        }
    }

    /**
     * Initialize the SP Data if required.
     *
     * @param queryCriteria name of query
     * @param pageContext page context object
     */
    public static void initSPData(String queryCriteria, String latestUpdatedTimeQueryCriteria, PageContext pageContext)
    {
        String      webAppName = getWebAppContextPath(pageContext);
        DebugLogger log        = DebugLogger.getLogger(webAppName, TagUtils.class);

        if ( getSPDataFromCache(queryCriteria, latestUpdatedTimeQueryCriteria, pageContext) == null )
        {
            synchronized(spidData)
            {
                if ( getSPDataFromCache(queryCriteria, latestUpdatedTimeQueryCriteria, pageContext) == null )
                {
                    // Cache the SPID data before meta file parsing and HTML code generation
                    XMLMessageParser xmp = null;
                    try
                    {
                        xmp = new XMLMessageParser("<Body></Body>");
                    }
                    catch (MessageParserException e)
                    {
                        log.error("Unable to create XMLMessageParser.", e);
                    }

                    if (xmp != null)
                    {
                        String[][] strResultsArr = queryOptionList( queryCriteria, xmp.getDocument(), pageContext);
                        // by default consider SP data is enabled
                        setSPDataInCache( queryCriteria, strResultsArr);
                    }
                }
            }
        }
    }

    /**
     * It is expected that query should return only single row & single column
     * as a result. If it contains multiple row and multiple columns then
     * the value from first row and first column will be returned.
     *
     * Also the result is supposed to be under
     * DataContainer.Data(N) node and no other hierarchy
     *
     * @param queryCriteria query name
     * @param context page context object
     * @return string
     */
    public static String getQuerySingleRowVal(String queryCriteria, PageContext context)
    {
        return getQuerySingleRowVal(queryCriteria, context, null);
    }
    
    /**
     * It is expected that query should return only single row & single column
     * as a result. If it contains multiple row and multiple columns then
     * the value from first row and first column will be returned.
     *
     * Also the result is supposed to be under
     * DataContainer.Data(N) node and no other hierarchy
     *
     * @param queryCriteria query name
     * @param context page context object
     * @param queryCriteriaTokens
     * @return string
     */
    public static String getQuerySingleRowVal(String queryCriteria, PageContext context, String queryCriteriaTokens)
    {
        String      webAppName = getWebAppContextPath(context);
        DebugLogger log        = DebugLogger.getLogger(webAppName, TagUtils.class);
        XMLGenerator queryResult = getQueryResult(queryCriteria, null, context, queryCriteriaTokens);
        String optionValue = "";
        try
        {
            if (queryResult.exists("DataContainer.Data(0).0"))
                optionValue = queryResult.getValue("DataContainer.Data(0).0");
        }
        catch (MessageException e)
        {
            log.warn("TagUtils.getQuerySingleRowVal(): Could not obtain data from db, making empty. Error message: " + e.getMessage());
            optionValue = "";
        }
        catch (Exception e)
        {
            log.warn("TagUtils.getQuerySingleRowVal(): Could not obtain data from db, making empty. Error message: " + e.getMessage());
            optionValue = "";
        }

        return optionValue;
    }

    public static class DynAttrEntry
    {
        public String name;
        public Class  type;
        public Object value;

        public DynAttrEntry(String name, Class type, Object value) {
            this.name = name;
            this.type = type;
            this.value = value;
        }
    }


    /**
     * Returns the field value from an xpath in the body or from a header field.
     * A field may have a custom field value of 'XPath' which indicates the body
     * xml value to use.
     *
     * @param  pageContext   PageContext object.
     * @param bean a <code>XMLBean</code> value
     * @param field a <code>Field</code> value
     * @return a <code>String</code> value
     * @exception JspException if an error occurs
     */
    public static final String getSummaryFieldValue(PageContext pageContext, XMLBean bean, Field field) throws JspException
    {

       String      webAppName = getWebAppContextPath(pageContext);

       DebugLogger log        = DebugLogger.getLogger(webAppName, TagUtils.class);


        String      path         = field.getCustomValue("XPath");
        Document    xmlDoc       = null;
        String      value        = null;

        if (!StringUtils.hasValue (path)) {
            value  = bean.getHeaderValue(field.getXMLPath());
        }
        else {
            XMLGenerator gen = (XMLGenerator)bean.getBodyDataSource();

            xmlDoc = gen.getDocument();

            try {
                ParsedXPath xpath           = new WebParsedXPath(path, gen.getGroup());

                String[]    candidateValues = xpath.getValues(xmlDoc);

                if (candidateValues.length > 0) {
                    value = candidateValues[0];

                    if (candidateValues.length > 1) {
                        log.warn("Matching " + candidateValues.length + " XPath values for [" + path + "].");
                    }

                }

                if (!StringUtils.hasValue(value)) {
                    value  = bean.getHeaderValue(field.getXMLPath());
                }
            }
            catch (Exception e) {
                String errorMessage = "Failed to create an ParsedXPath instance from the specified 'path' value:\n" + e.getMessage();

                log.error(errorMessage);

                throw new JspException(errorMessage);

            }

        }

        return value;


    }

    /**
     * Checks if displayReplicate is not null && not empty, if empty it
     * returns false else returns true
     *
     *@param basic String : String to check
     * @return boolean: returns true if string not null && equals to basic
    **/
    public static boolean isReplicateDisplayTrue(String basic) {
        boolean isBasic = false;
        if(StringUtils.hasValue(basic)){
            if(basic.trim().equals("basic"))
                isBasic = true;
        }
        else
            isBasic = false;
        return isBasic;
    }


        /**
         * Checks if the display if for local manager , if empty it
         * returns false else returns true
         *
         *@param basic String : String to check
         * @return boolean: returns true if string not null && equals to localmgr
        **/
        public static boolean isLocalMgrTrue(String basic) {
            boolean isLocalMgr = false;
            if(StringUtils.hasValue(basic)){
                if(basic.trim().equals("localmgr"))
                    isLocalMgr = true;
            }
            else
                isLocalMgr = false;
            return isLocalMgr;
    }
    /**
     * Encode some special characters found in the input string
     * to be displayed on the GUI, as they are not directly handled by browser.
     * This is for HTML purpose only.
     *
     * Currently it handles only double quote (")
     *
     * @param value the string
     * @return string with encoded characters
     */

    public static String performHTMLEncoding(String value)
    {
        String newValue = value;

        if (value.indexOf("\"") != -1)
        {
            newValue = StringUtils.replaceSubstrings ( value, "\"", "&quot;");
        }

        return newValue;
    }

    /**
     * Strips an ID string of any # character and anything that follows.  To
     * allow unique ids to be used for parts with the same XML element name,
     * # is allowed in an id, but not retained as part of the XML path
     *
     * @param id as string
     * @return string without hash etc
     */
    public static String stripIdSuffix(String id)
    {
        if (id == null)
            return id;

        int invalidIdx = id.indexOf('#');


        if (invalidIdx == 0)
            return "";

        if (invalidIdx > -1)
            return id.substring(0, invalidIdx);

        return id;
    }

    /**
     * Returns the WSP from session bean.
     *
     * @param pageContext PageContext object 
     * @return String - provider of the domain
     */
    public static String getWSPInSession (PageContext pageContext)
    {
        return getWSPInSession(pageContext.getSession());
    }

    /**
     * Returns the WSP from session bean.
     *
     * @param session HttpSession
     * @return String - provider of the domain
     */
    public static String getWSPInSession (HttpSession session)
    {
        if (session != null)
        {
            SessionInfoBean sBean = (SessionInfoBean)session.getAttribute(ServletConstants.SESSION_BEAN);
            if (sBean != null && StringUtils.hasValue(sBean.getWsp()) && !sBean.getWsp().equals(DomainType.NOBODY))
                return sBean.getWsp();
        }

        return "";
    }

    /**
     * Returns the WSP from session bean.
     *
     * @param session HttpSession
     * @return String - provider of the domain
     */
    public static String getNFLogoPath (HttpSession session)
    {
        String wspLogoPath = null;
        String wsp = getWSPInSession (session);
        if (StringUtils.hasValue(wsp))
        {
            String wspNameSP = System.getProperty(SecurityFilter.WHOLESALE_PROVIDER);
            if (StringUtils.hasValue(wsp) && StringUtils.hasValue(wspNameSP) && wspNameSP.equals(StoreProviderBase.ANY_VALUE))
                wspLogoPath = IMAGES_STR + wsp + NFLOGO_GIF_STR;

            if (StringUtils.hasValue(wspLogoPath) && isValidLogoFile(session.getServletContext(), wspLogoPath))
                return wspLogoPath;
        }
        return IMG_NFLOGO_GIF_STR;
    }

    /**
     * Returns the CID's name
     *
     * @param cid customer id
     * @return String - name of the domain
     */
    public static String getDomainName (String cid)
    {
        String name = NEUSTAR_STR;

        if (StringUtils.hasValue(cid) && !cid.equals(DomainType.NOBODY))
        {
            try
            {
                DomainProperties dp = DomainProperties.getInstance(cid);
                name=dp.getDomainName();
            }
            catch (Exception e)
            {
                // For any exception returning NeuStar
                name = NEUSTAR_STR;
            }
        }

        return name;
    }

    /**
     * Returns the provider of the present domain, if any,
     * in case there is no provider NOBODY will be returned.
     *
     * @param session HttpSession
     * @return String - provider of the domain
     * @throws DomainTypeException on error
     */
    public static String getDomainProvider (HttpSession session) throws DomainTypeException
    {
        return getDomainProvider (getCustomerIdFromSessionBean (session));
    }

    /**
     * Returns the provider of the present domain, if any,
     * in case there is no provider NOBODY will be returned.
     *
     * @param cid customer id
     * @return String - provider of the domain
     * @throws DomainTypeException on error
     */
    public static String getDomainProvider (String cid) throws DomainTypeException
    {
        return DomainType.getInstance (cid).getDomainProvider();
    }

    /**
     * Returns the providers of the present domain, if any,
     * in case there is no provider NOBODY will be returned as a List element.
     *
     * @param session HttpSession
     * @return String - provider of the domain
     * @throws DomainTypeException on error
     */
    public static Set<String> getDomainProviders (HttpSession session) throws DomainTypeException
    {
        return getDomainProviders (getCustomerIdFromSessionBean (session));
    }

    /**
     * Returns the providers of the present domain, if any,
     * in case there is no provider NOBODY will be returned as a List element.
     *
     * @param cid Cid
     * @return String - provider of the domain
     * @throws DomainTypeException on error
     */
    public static Set<String> getDomainProviders (String cid) throws DomainTypeException
    {
        return DomainType.getInstance (cid).getDomainProviders ();
    }

    /**
     * Returns true if the nature of the present domain
     * is of type Basic.
     *
     * @param session HttpSession
     * @return boolean
     * @throws DomainTypeException on error
     */
    public static boolean isBasic (HttpSession session) throws DomainTypeException
    {
        return DomainType.getInstance (getCustomerIdFromSessionBean (session)).getDomainNature ().equals ( DomainType.BASIC );
    }

    /**
     * Returns true if the nature of the present domain
     * is of type Branded.
     *
     * @param session HttpSession
     * @return boolean
     * @throws DomainTypeException on error
     */
    public static boolean isBranded (HttpSession session) throws DomainTypeException
    {
        return DomainType.getInstance (getCustomerIdFromSessionBean (session)).getDomainNature ().equals ( DomainType.BRANDED );
    }

    /**
     * Returning customer Id from sessionBean of session.
     * And if sessionBean or session is null then DEFAULT_CUSTOMER_ID
     * will be returned.
     *
     * @param session HttpSession
     * @return String
     */
    public static String getCustomerIdFromSessionBean (HttpSession session)
    {
        String customerId = CustomerContext.DEFAULT_CUSTOMER_ID;
        if (session != null)
        {
            SessionInfoBean sessionBean = (SessionInfoBean) session.getAttribute(ServletConstants.SESSION_BEAN);
            if (sessionBean != null)
            {
                customerId = sessionBean.getCustomerId ();
                if (!StringUtils.hasValue ( customerId ))
                    customerId = CustomerContext.DEFAULT_CUSTOMER_ID;
            }
        }
        return customerId;
    }

    /**
     * Returning subdomain Id from SecurityService after
     * obtaining CID and UID from sessionBean of session.
     * And if sessionBean or session is null then DEFAULT_SUBDOMAIN_ID
     * will be returned.
     *
     * @param session HttpSession
     * @return String
     */
    public static String getSubDomain(HttpSession session)
    {
        String customerId;
        String subdomainId = CustomerContext.DEFAULT_SUBDOMAIN_ID;

        if (session != null)
        {
            SessionInfoBean sessionBean = (SessionInfoBean) session.getAttribute(ServletConstants.SESSION_BEAN);
            if (sessionBean != null)
            {
                customerId = sessionBean.getCustomerId ();
                if (!StringUtils.hasValue ( customerId ))
                    customerId = CustomerContext.DEFAULT_CUSTOMER_ID;

                String userId = sessionBean.getUserId ();
                if (!StringUtils.hasValue ( userId ))
                    userId = CustomerContext.DEFAULT_USER_ID;

                try
                {
                    subdomainId = SecurityService.getInstance(customerId).getSubDomainId ( userId );
                }
                catch (SecurityException e)
                {
                    subdomainId = CustomerContext.DEFAULT_SUBDOMAIN_ID;
                }

            }
        }
        return subdomainId;
    }

    /**
     * This method checks if the SP Data cache last updated time has changed
     * if changed, it clears the cache for the given lookup key
     * if not changed then return the cached SP lookup Data for given lookup key
     *
     * @param spidDataKey key to find the SPID last updated time and data from corresponding maps
     * @param latestUpdatedTimeQueryCriteria query to find the latest udpated time of the SPID data
     * @param pageContext pageContext used to fetch single row query.
     *
     * @return String the cached SP data if valid and found.
     */
    public static String getSPDataFromCache ( String spidDataKey, String latestUpdatedTimeQueryCriteria, PageContext pageContext )
    {
        return getSPDataFromCache (spidDataKey, latestUpdatedTimeQueryCriteria, pageContext, null);
    }

    /**
     * This method checks if the SP Data cache last updated time has changed
     * if changed, it clears the cache for the given lookup key
     * if not changed then return the cached SP lookup Data for given lookup key
     *
     * @param spidDataKey key to find the SPID last updated time and data from corresponding maps
     * @param latestUpdatedTimeQueryCriteria query to find the latest udpated time of the SPID data
     * @param pageContext pageContext used to fetch single row query.
     * @param queryCriteriaTokens 
     *
     * @return String the cached SP data if valid and found.
     */
    public static String getSPDataFromCache ( String spidDataKey, String latestUpdatedTimeQueryCriteria, PageContext pageContext, String queryCriteriaTokens )
    {
        DebugLogger log = DebugLogger.getLogger(TagUtils.getWebAppContextPath(pageContext), TagUtils.class);

        String jsCode = null;
        long latestUpdatedTimeInMills = 0;
        Long lastUpdatedTimeOfSPData = null;

        String strLatestUpdatedTime = TagUtils.getQuerySingleRowVal(latestUpdatedTimeQueryCriteria, pageContext, queryCriteriaTokens);

        try {
            if ( log.isDebugEnabled() )
                log.debug("for key [" + spidDataKey + "], obtained latestUpdated Time [" + strLatestUpdatedTime + "] using query file [" + latestUpdatedTimeQueryCriteria + "].");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            latestUpdatedTimeInMills  = sdf.parse(strLatestUpdatedTime).getTime();

            if ( log.isDebugEnabled() )
                log.debug("for key [" + spidDataKey + "], latest updated time in milliseconds [" + latestUpdatedTimeInMills + "] using query file [" + latestUpdatedTimeQueryCriteria + "].");

        } catch (ParseException e) {

            log.error( "Could not parse date/timestamp obtained from query-criteria " +
                    "[" + latestUpdatedTimeQueryCriteria + "]" );
            return null;
        }

        // Obtain last updated time from cache
        lastUpdatedTimeOfSPData = (Long) spidLastUpdateTime.get(spidDataKey);

        if ( log.isDebugEnabled() )
            log.debug("For key [" + spidDataKey + "] Last updated time in milliseconds, from cache is [" + lastUpdatedTimeOfSPData + "]");

        // compare if latest updated time is NOT the same as last updated time then
        // clear the data from cache for this lookup key
        if ( lastUpdatedTimeOfSPData != null && latestUpdatedTimeInMills != lastUpdatedTimeOfSPData.longValue() )
        {
            if ( log.isDebugEnabled() )
                log.debug("For key [" + spidDataKey + "], last updated time did not match with latest " +
                        "updated time, hence clearing the cache.");
            synchronized(spidData)
            {
                if ( spidData != null )
                    spidData.remove(spidDataKey);
            }
        }

        // update the "last updated time" in the cache as it would be re-loaded
        spidLastUpdateTime.put(spidDataKey, new Long(latestUpdatedTimeInMills));

        if ( spidData != null && spidData.containsKey( spidDataKey ) )
        {
            jsCode = (String) spidData.get(spidDataKey);
            if ( log.isDebugEnabled() )
                log.debug("For key [" + spidDataKey + "], found cached JS Code [" + jsCode + "]");
        }

        return jsCode;
    }

    /**
     *  Converts the value to the required TimeZone
     * if convertToServerTZ = 2 ; would convert the value to Client TimeZone
     * if convertToServerTZ = 1 ; would convert the value to UTC TimeZone
     *
     * @param pageContext PageContext object
     * @param value Date as string
     * @param convertToServerTZ as int
     * @param serverTZ as String
     * @return date as string in UTC
     */
    public static String convertTimeZone (PageContext pageContext, String value, int convertToServerTZ, String serverTZ)
    {
        DebugLogger log = DebugLogger.getLogger(TagUtils.getWebAppContextPath(pageContext), TagUtils.class);
        TimeZone t = TimeZone.getTimeZone(serverTZ);
        Calendar calendar = Calendar.getInstance(t);
        int clientOffset = 0;
        TimeZone timezone = TimeZone.getTimeZone("EST5EDT");
                            
        if (isClientTimezoneSupported(pageContext))
        {
            if ((StringUtils.hasValue(value)))
            {
                try
                {
                    if (pageContext.getSession().getAttribute(SecurityFilter.CLIENT_TZ_OFFSET) != null)
                        clientOffset = Integer.parseInt((String)pageContext.getSession().getAttribute(SecurityFilter.CLIENT_TZ_OFFSET));
                    Date currentDate = new Date((DateUtils.formatTime(value, NFLocale.getDateTimeFormat())));
                    calendar.setTime(currentDate);
                    // clientOffset is in respect to UTC, converting it to the offset between serverTZ and clientTZ
                    clientOffset = getAdjustedOffset(clientOffset, serverTZ, currentDate);

                    boolean origDateDS = timezone.inDaylightTime(currentDate);

                    //this function checks for the boundary cases i.e. when timezone changes from EST to EDT and vice versa
                    clientOffset = handleBoundaryDST(serverTZ, clientOffset, t, origDateDS, calendar, timezone);

                    //the actual conversion happens here.
                    if(clientOffset != 0)
                    {
                        if (convertToServerTZ == CONVERT_TO_SERVER_TZ)
                            calendar.add(Calendar.MINUTE, clientOffset);
                        else if (convertToServerTZ == CONVERT_TO_CLIENT_TZ)
                            calendar.add(Calendar.MINUTE, -clientOffset);
                        value = DateUtils.formatTime(calendar.getTime().getTime(),NFLocale.getDateTimeFormat());
                    }
                   
                    //the following function negates the changes factored in by the Java Calendar API
                    // when timezone changes from EDT to EST and vice versa while actual conversion.

                    if ("EST".equalsIgnoreCase(serverTZ))
                    {
                        value = adjustAPIChanges(timezone, calendar, origDateDS, NFLocale.getDateTimeFormat());
                    }
                }
                catch(ParseException e) // no need to throw error, just logging warning
                {
                    log.warn("Unable to parse the value [" + value + "] to obtain date in server TimeZone. Trying with seconds format.");
                    
                    try
                    {
                        //repeating the code from above to handle date in a different format
                        if (pageContext.getSession().getAttribute(SecurityFilter.CLIENT_TZ_OFFSET) != null)
                            clientOffset = Integer.parseInt((String)pageContext.getSession().getAttribute(SecurityFilter.CLIENT_TZ_OFFSET));

                        Date currentDate = new Date((DateUtils.formatTime(value, "MM-dd-yyyy-hhmmssa")));
                        calendar.setTime(currentDate);

                        // clientOffset is in respect to UTC, converting it to the offset between serverTZ and clientTZ
                        clientOffset = getAdjustedOffset(clientOffset, serverTZ, currentDate);
                        boolean origDateDS = timezone.inDaylightTime(currentDate);

                        clientOffset = handleBoundaryDST(serverTZ, clientOffset, t, origDateDS, calendar, timezone);

                        if(clientOffset != 0)
                        {
                            if (convertToServerTZ == CONVERT_TO_SERVER_TZ)
                                calendar.add(Calendar.MINUTE, clientOffset);
                            else if (convertToServerTZ == CONVERT_TO_CLIENT_TZ)
                                calendar.add(Calendar.MINUTE, -clientOffset);
                            value = DateUtils.formatTime(calendar.getTime().getTime(),"MM-dd-yyyy-hhmmssa");
                        }
                        if ("EST".equalsIgnoreCase(serverTZ))
                        {
                            value = adjustAPIChanges(timezone, calendar, origDateDS, "MM-dd-yyyy-hhmmssa");
                        }
                        
                    }
                    catch(ParseException pe)
                    {
                        log.warn("Unable to parse the value [" + value + "] to obtain date in server TimeZone. Returning same value.");
                    }
                 }
                 catch(Exception e)
                 {
                     log.warn("Unable to parse the value [" + value + "] to obtain date in server TimeZone. Returning same value.");
                 }
             }
        }
        return value;
    }

    public static int handleBoundaryDST(String serverTZ, int clientOffset, TimeZone t, boolean origDateDS, Calendar calendar, TimeZone timezone)
    {
        if ("EST".equalsIgnoreCase(serverTZ) && (calendar.get(Calendar.HOUR_OF_DAY) == 1))
        {
            Calendar tempCal = Calendar.getInstance(t);
            tempCal.setTime(calendar.getTime());
            tempCal.add(Calendar.MINUTE, -60);
            boolean tempDateDS = timezone.inDaylightTime(tempCal.getTime());
            if (!origDateDS && tempDateDS)
                clientOffset += 60;
            else
            {
                tempCal.setTime(calendar.getTime());
                tempCal.add(Calendar.MINUTE, 60);
                tempDateDS = timezone.inDaylightTime(tempCal.getTime());
                if (origDateDS && !tempDateDS)
                    clientOffset -= 60;
            }
        }
        return clientOffset;
    }

    public static String adjustAPIChanges(TimeZone timezone, Calendar calendar, boolean origDateDS, String dateFormat) throws ParseException
    {
        boolean newDateDS = timezone.inDaylightTime(calendar.getTime());
        boolean adjustAPI = false;
        String dateValue;
        if (!origDateDS && newDateDS)
        {
            if (calendar.get(Calendar.HOUR_OF_DAY) == 2)
                adjustAPI = true;
            else
                calendar.add(Calendar.MINUTE, -60);
        }
        else if (origDateDS && !newDateDS)
            calendar.add(Calendar.MINUTE, 60);

        dateValue = DateUtils.formatTime(calendar.getTime().getTime(), dateFormat);

        if (adjustAPI)
        {
            String newVal = dateValue;
            int hyphen = newVal.lastIndexOf("-");
            dateValue = newVal.substring(0,hyphen+2) + "2" + newVal.substring(hyphen+3, newVal.length());
        }
        return dateValue;
    }
    
    public static int getAdjustedOffset(int offset, String serverTZ)
    {
        return getAdjustedOffset(offset, serverTZ, null);
    }
    public static int getAdjustedOffset(int offset, String serverTZ, Date currentDate)
    {

        if (StringUtils.hasValue(serverTZ) && "EST".equalsIgnoreCase(serverTZ))
        {
            TimeZone timezone = TimeZone.getTimeZone("EST5EDT");
            if (currentDate == null)
            {
                offset = offset + timezone.getOffset(new Date().getTime())/60000;
            }
            else
            {
                offset = offset + timezone.getOffset(currentDate.getTime())/60000;
            }
        }
        return offset;
    }

    /**
     * This method uses QueryEngine to query the database.
     *
     * @param  queryCriteria     The criteria portion of the RepositoryManager's query category-criteria.
     * @param  message           XMLGenerator instance of the message data.
     * @param  context           page context object
     *
     * @return  XMLGenerator contains query result in XML Dom format.
     */
    public static XMLGenerator getQueryResult (String queryCriteria, Document message, PageContext context)
    {
        return getQueryResult(queryCriteria, message, context, null);
    }
    /**
     * This method uses QueryEngine to query the database.
     *
     * @param  queryCriteria     The criteria portion of the RepositoryManager's query category-criteria.
     * @param  message           XMLGenerator instance of the message data.
     * @param  context           page context object
     * @param  queryCriteriaTokens
     *
     * @return  XMLGenerator contains query result in XML Dom format.
     */
    public static XMLGenerator getQueryResult (String queryCriteria, Document message, PageContext context, String queryCriteriaTokens)
    {
        String webAppName = TagUtils.getWebAppContextPath(context);
        DebugLogger log = DebugLogger.getLogger(webAppName, TagUtils.class);
        XMLGenerator resultGenerator = null;
        HashMap queryTokensMap = null;

        try
        {
            if (message == null)
            {
                XMLMessageParser xmp = null;
                try
                {
                    xmp = new XMLMessageParser("<Body></Body>");
                }
                catch (MessageParserException e)
                {
                    log.error("Unable to create XMLMessageParser.", e);
                }
                if (xmp != null)
                    message = xmp.getDocument();
            }

            if (log.isDebugEnabled())
            {
                log.debug("TagUtils.getQueryResult(): Querying db for option list values using: queryCriteria: " + queryCriteria + "\n  message:\n" + message);
            }

            queryTokensMap = (HashMap) createQueryTokensMap(queryCriteriaTokens);

            QueryEngine.QueryResults queryResult = QueryEngine.executeQuery(queryCriteria, message, 0, -1, null, queryTokensMap);

            if (queryResult != null)
            {
                resultGenerator = new XMLPlainGenerator(queryResult.getResultsAsDOM());
                resultGenerator.create(QUERY_RESULT_COUNT);
                resultGenerator.setValue(QUERY_RESULT_COUNT, queryResult.getResultCount()+"");
                if (log.isDebugEnabled())
                {
                    log.debug("TagUtils.getQueryResult(): Total rows obtained [" + queryResult.getResultCount() + "] and bean has contents:\n" + resultGenerator.describe());
                }
            }
        }
        catch (Exception e)
        {
            log.error("TagUtils.getQueryResult(): Failed to obtain the query result from the database via QueryEngine:" + e.getMessage(), e);
        }

        return resultGenerator;
    }

    /**
     * to create Map out of query criteria tokens.
     * expected token value example: SUPPLIER=ATT;PON=ABC;MY_TOKEN=XYZ
     * Map shall have Key,Value like below
     *          Key             Value
     *         ----------------------
     *         SUPPLIER         ATT
     *         PON              ABC
     *         MY_TOKEN         XYZ
     *
     * No exceptions are thrown and all wrong entries shall be ignored
     * @param queryCriteriaTokens String containing comma separated token=value pairs.
     *
     * @returns Map containing key value pairs
     *
     */
    private static Map createQueryTokensMap(String queryCriteriaTokens)
    {
        if ( !StringUtils.hasValue(queryCriteriaTokens) )
            return null;

        Map tokenValueMap = new HashMap();

        String[] tokens = queryCriteriaTokens.split(";");

        if (tokens != null)
        {
            for (String str: tokens)
            {
                try
                {
                    if (StringUtils.hasValue(str))
                    {
                        String[] keyValuePair = str.split("=");
                        String key = keyValuePair[0];
                        String value = keyValuePair[1];

                        if (StringUtils.hasValue(key) && StringUtils.hasValue(value))
                            tokenValueMap.put(key, value);
                    }
                }
                catch(Exception e) {/*ignore any specific token exception*/}
            }
        }
        return tokenValueMap;
    }


    /**
     * This method uses QueryEngine to query the database for the options list values.
     *
     * @param  queryCriteria  The criteria portion of the RepositoryManager's query category-criteria.
     * @param  message        XMLGenerator instance of the message data.
     * @param  context        page context object
     *
     * @return  String[][] representation of the queried option list from the database.
     *          The first index [0] contains option key values.
     *          The first index [1] contains option display values.
     *          If the display value doesn't exist, the key value is used as the default.
     */
    public static String[][] queryOptionList(String queryCriteria, Document message, PageContext context)
    {
        return queryOptionList(queryCriteria, message, context, null);
    }

    /**
     * This method uses QueryEngine to query the database for the options list values.
     *
     * @param  queryCriteria        The criteria portion of the RepositoryManager's query category-criteria.
     * @param  message              XMLGenerator instance of the message data.
     * @param  context              page context object
     * @param  queryCriteriaTokens  
     *
     * @return  String[][] representation of the queried option list from the database.
     *          The first index [0] contains option key values.
     *          The first index [1] contains option display values.
     *          If the display value doesn't exist, the key value is used as the default.
     */
    public static String[][] queryOptionList(String queryCriteria, Document message, PageContext context, String queryCriteriaTokens)
    {
        String webAppName = TagUtils.getWebAppContextPath(context);

        DebugLogger log = DebugLogger.getLogger(webAppName, TagUtils.class);

        try
        {
            XMLGenerator queryResult = getQueryResult(queryCriteria, message, context, queryCriteriaTokens);

            int          optionCount     = Integer.parseInt(queryResult.getValue(QUERY_RESULT_COUNT));

            if (optionCount > 0)
            {
                String[][]     optionList      = new String [2][optionCount];

                for (int i = 0; i < optionCount; i++)
                {
                    String optionValue = queryResult.getValue("DataContainer.Data(" + i + ").0");

                    if (log.isDebugEnabled())
                    {
                        log.debug("TagUtils.queryOptionList(): Resulting option value at index (" + i + ") is: " + optionValue);
                    }

                    optionList[0][i] = optionValue;

                    optionList[1][i] = optionValue;

                    if (queryResult.getChildCount("DataContainer.Data(" + i + ")") > 1)
                    {
                        String optionName = queryResult.getValue("DataContainer.Data(" + i + ").1");

                        if (StringUtils.hasValue(optionName))
                        {
                            optionList[1][i] = optionName;
                        }
                    }

                    if (log.isDebugEnabled())
                    {
                        log.debug("TagUtils.queryOptionList(): Resulting option name at index (" + i + ") is: " + optionList[1][i]);
                    }
                }

                return optionList;
            }

            log.debug("TagUtils.queryOptionList(): Query result is empty.  Resorting to other options ...");
        }
        catch (Exception e)
        {
            log.error("TagUtils.queryOptionList(): Failed to obtain the option list from the database via QueryEngine:\n" + e.getMessage());
        }

        return null;
    }

    /**  This method returns the display name of the supplier checks for the
     * tpAliasMethod for the service and returns the appropriate display name
     *
     * @param pageContext  the PageContext associated with the page
     * @param value        the supplier id to be used to find the name
     * @param svcObj       the ServiceDef associated with the page
     * @return output      the display name for the supplier
     */
    public static String getSupplierDisplayName (PageContext pageContext, String value, ServiceDef svcObj)
    {
        String output = value;
        DebugLogger log = DebugLogger.getLogger(TagUtils.getWebAppContextPath(pageContext), TagUtils.class);


        if (svcObj != null)
        {
            String svcAliasMethod = StringUtils.hasValue(svcObj.getAliasMethod()) ? svcObj.getAliasMethod() : "";
            if (StringUtils.hasValue(svcAliasMethod) && svcAliasMethod.equals(ServletConstants.TP_ALIAS_METHOD_DB) || svcAliasMethod.equals(ServletConstants.TP_ALIAS_METHOD_FS))
            {
                // Alwasys showing in format <NAME> instead of <NAME>(<SPID>)
                output = getSupplierDBValue (value, false);
            }
        }
        if (!StringUtils.hasValue(output))
            output = value;
        log.debug("getSupplierDisplayName(): Returning alias [ " + output + " ] for value [ " + value + " ].");
        return output;
    }

    /**
     * This methode returns the value of the supplier present in the
     * database. If any error occurs then it returns the value passed in.
     * @param value name of the supplier
     * @param longName return "<NAME> (<TPNAME>)" format
     * @return db value of the supplier
     */
    public static String getSupplierDBValue(String value, boolean longName)
    {
        if (!StringUtils.hasValue(value))
            return "";

        String retVal;

        try
        {
            retVal = TradingPartnerRelationship.getSupplierName(value);
            if (longName)
                retVal += " (" + value + ")";
        }
        catch (TPRException e)
        {
            retVal = value; // returning same value in case of TPRException
        }
        catch (Exception e)
        {
            retVal = value; // returning same value in case of any Exception
        }

        return retVal;
    }


    /**
     * Prepares javascript code out of queryResult and sets it in the SP data cache for given queryCriteria.
     * if pre-populated SPID is found in the results then sets the SP Name for it in the passed variable "prePopulateSPName".
     *
     * @param lookupKey lookup data key (QueryfileName [+ DOMAIN [+ SUBDOMAIN]])
     * @param queryResult 2-d string array of results
     *
     *          if true then only formated Javascript SPID data would be cached
     *          otherwise Javascript SPID data would NOT be cached.
     * @return String  the newly added javascript code
     */
    public static String setSPDataInCache ( String lookupKey, String[][] queryResult )
    {
        String[] spIDs        = null;
        String[] spNames        = null;
        String[] spidNamePairSortedBySPID = null;
        String[] nameSPIDPairSortedBySPName = null;
        StringBuffer javaScriptForLookup = new StringBuffer();

        if (queryResult != null)
        {
            spIDs   = queryResult[0];
            spNames = queryResult[1];

            // Return null, if the result set has immature data
            if (spIDs == null || spIDs.length < 1
                    || spNames == null || spIDs.length != spNames.length )
                return null;

            spidNamePairSortedBySPID = new String[spIDs.length];
            nameSPIDPairSortedBySPName = new String[spIDs.length];

            for(int i = 0; i < spIDs.length; i++)
            {
                //escape the \ char with \\ in the display name.
                spNames[i] = StringUtils.replaceSubstrings(spNames[i], "\\","\\\\") ;

                spidNamePairSortedBySPID[i] = spIDs[i] + "#" + spNames[i];
                nameSPIDPairSortedBySPName[i] = spNames[i] + "#" + spIDs[i];
            }

            // id-name mapping sorted by ID
            Arrays.sort(spidNamePairSortedBySPID);
            // name-id mapping sorted by Name
            Arrays.sort(nameSPIDPairSortedBySPName);

            String lookupDataSortedBySPIDWithNoFltr = "";
            String lookupDataSortedBySPNameWithNoFltr = "";
            String lookupDataSortedBySPIDDescWithNoFltr = "";
            String lookupDataSortedBySPNameDescWithNoFltr = "";

            String tempSPID = null;
            String tempSPName = null;
            String tempRow = null;

            javaScriptForLookup.append(" <script language=\"javascript\">");

            javaScriptForLookup.append(" var ").append(SORTED_BY_SPID).append("= new Array(").append(nameSPIDPairSortedBySPName.length).append(");");
            javaScriptForLookup.append(" var ").append(SORTED_BY_SPNAME).append("= new Array(").append(nameSPIDPairSortedBySPName.length).append(");");

            for ( int i = 0; i < spidNamePairSortedBySPID.length; i++ )
            {
                javaScriptForLookup.append(SORTED_BY_SPID).append("[").append(i).append("] = '").append(spidNamePairSortedBySPID[i].replaceAll("'","\\\\'")).append("';");
                javaScriptForLookup.append(SORTED_BY_SPNAME).append("[").append(i).append("] = '").append(nameSPIDPairSortedBySPName[i].replaceAll("'","\\\\'")).append("';");

                //Start: HTML table row contents sorted by SPID Asc and Desc
                tempSPID = spidNamePairSortedBySPID[i].substring( 0, spidNamePairSortedBySPID[i].indexOf("#") );
                tempSPName = spidNamePairSortedBySPID[i].substring( spidNamePairSortedBySPID[i].indexOf("#") + 1 );

                tempSPName = TagUtils.performHTMLEncoding(tempSPName);

                tempRow = "<tr><td>" + tempSPID
                        + "<td>" + StringUtils.replaceSubstrings( tempSPName, "'","\\'" );

                // Sorting on SPID: Ascending
                lookupDataSortedBySPIDWithNoFltr = lookupDataSortedBySPIDWithNoFltr + tempRow;

                // Sorting on SPID: Descending
                lookupDataSortedBySPIDDescWithNoFltr = tempRow + lookupDataSortedBySPIDDescWithNoFltr;
                //End: HTML table row contents sorted by SPID Asc and Desc

                //Start: HTML table row contents sorted by Description Asc and Desc
                tempSPID = nameSPIDPairSortedBySPName[i].substring( nameSPIDPairSortedBySPName[i].lastIndexOf("#") + 1 );
                tempSPName = nameSPIDPairSortedBySPName[i].substring( 0, nameSPIDPairSortedBySPName[i].lastIndexOf("#") );

                tempSPName = TagUtils.performHTMLEncoding(tempSPName);

                tempRow = "<tr><td>" + tempSPID
                        + "<td>" + StringUtils.replaceSubstrings( tempSPName, "'","\\'" );

                // Sorting on description: Ascending
                lookupDataSortedBySPNameWithNoFltr = lookupDataSortedBySPNameWithNoFltr + tempRow;

                // Sorting on description: Descending
                lookupDataSortedBySPNameDescWithNoFltr = tempRow + lookupDataSortedBySPNameDescWithNoFltr;
                //End: HTML table row contents sorted by Description Asc and Desc
            }

            lookupDataSortedBySPIDWithNoFltr = "<table id=\"myTable\" onMouseover=\"H(event)\" onMouseout=\"D(event)\" onclick=\"S(event);\" class=\"T\" border=\"1\" CELLPADDING=\"0\" CELLSPACING=\"0\">" + lookupDataSortedBySPIDWithNoFltr + "</table>";
            lookupDataSortedBySPIDDescWithNoFltr = "<table id=\"myTable\" onMouseover=\"H(event)\" onMouseout=\"D(event)\" onclick=\"S(event);\" class=\"T\" border=\"1\" CELLPADDING=\"0\" CELLSPACING=\"0\">" + lookupDataSortedBySPIDDescWithNoFltr + "</table>";

            lookupDataSortedBySPNameWithNoFltr = "<table id=\"myTable\" onMouseover=\"H(event)\" onMouseout=\"D(event)\" onclick=\"S(event);\" class=\"T\" border=\"1\" CELLPADDING=\"0\" CELLSPACING=\"0\">" + lookupDataSortedBySPNameWithNoFltr + "</table>";
            lookupDataSortedBySPNameDescWithNoFltr = "<table id=\"myTable\" onMouseover=\"H(event)\" onMouseout=\"D(event)\" onclick=\"S(event);\" class=\"T\" border=\"1\" CELLPADDING=\"0\" CELLSPACING=\"0\">" + lookupDataSortedBySPNameDescWithNoFltr + "</table>";

            javaScriptForLookup.append("var noFilter").append(SORTED_BY_SPID).append(" = '").append(lookupDataSortedBySPIDWithNoFltr).append("';");
            javaScriptForLookup.append("var noFilter").append(SORTED_BY_SPID).append("Desc = '").append(lookupDataSortedBySPIDDescWithNoFltr).append("';");

            javaScriptForLookup.append("var noFilter").append(SORTED_BY_SPNAME).append(" = '").append(lookupDataSortedBySPNameWithNoFltr).append("';");
            javaScriptForLookup.append("var noFilter").append(SORTED_BY_SPNAME).append("Desc = '").append(lookupDataSortedBySPNameDescWithNoFltr).append("';");

            javaScriptForLookup.append("</script>");

            synchronized(spidData)
            {
                spidData.put(lookupKey, javaScriptForLookup.toString());
            }
        }

        return javaScriptForLookup.toString();
    }

    public static String getLookupFieldDataKey(String queryCriteria, String lastUpdatedTimeQueryCriteria, DebugLogger log)
    {
        /**
         * key format: QueryfileName [+ DOMAIN [+ SUBDOMAIN]]
         * e.g. if query criteria to fetch SPID data = gw-soa-lrn-texttag, 'ACME' is DOMAIN & 'SUB1' is SUBDOMAIN then key = gw-soa-lrn-texttag-ACME-SUB1
         * lastUpdatedTimeQueryCriteria query file would be searched for ${CID} & ${SDID} tokens, if present, would be added
         * in the key.
        **/
        String spidDataKey = queryCriteria;

        String lastUpdateQueryXMLString = null;

        try {
            lastUpdateQueryXMLString = FileUtils.readFile(RepositoryManager.getInstance().getRepositoryRoot() + File.separator +
                    CustomerContext.DEFAULT_CUSTOMER_ID + File.separator +
                    RepositoryCategories.QUERY_CONFIG + File.separator +
                    lastUpdatedTimeQueryCriteria + ".xml");
        } catch (FrameworkException e) {
            log.error("Could not read [" + lastUpdatedTimeQueryCriteria + "] from repository.");
            return spidDataKey;
        }

        // Append CustomerID in key if last updated time query has CID token in it
        if (lastUpdateQueryXMLString.indexOf(QueryEngine.CID_TOKEN) != -1)
        {
            spidDataKey = spidDataKey + RESOURCE_ELEMENT_SEPERATOR + EnvironmentUtils.getCustomerID();

            // Append SubDomain ID in key if last updated time query has SDID token in it
            if (lastUpdateQueryXMLString.indexOf(QueryEngine.SDID_TOKEN) != -1 && StringUtils.hasValue(EnvironmentUtils.getSubDomainID()))
                spidDataKey = spidDataKey + RESOURCE_ELEMENT_SEPERATOR + EnvironmentUtils.getSubDomainID();
        }

        return spidDataKey;
    }

    /**
     * This method is used at login and invalid-login JSP
     * files to display NFLogoLogin.gif on header, if exists.
     *
     * If the image path passed has value then it will return
     * the same path else create and set the path and returns.
     * This is to avoid calculating file statistics on every call.
     *
     * @param pageContext PageContext object
     * @param imgPath     present image path, if populated
     * @return            image path to use
     */
    public static String getLoginImgPath (PageContext pageContext, String imgPath)
    {
        return getLoginImgPath (pageContext, imgPath, null);
    }
    
    /**
     * This method is used at login and invalid-login JSP
     * files to display NFLogoLogin.gif on header, if exists.
     *
     * If the image path passed has value then it will return
     * the same path else create and set the path and returns.
     * This is to avoid calculating file statistics on every call.
     *
     * @param pageContext PageContext object
     * @param imgPath     present image path, if populated
     * @param wspName     name of whole sale provider
     * @return            image path to use
     */
    public static String getLoginImgPath (PageContext pageContext, String imgPath, String wspName)
    {
        DebugLogger log = DebugLogger.getLogger(TagUtils.getWebAppContextPath(pageContext), TagUtils.class);

        String nfLogoPath = IMG_NFLOGO_GIF_STR;
        String nfLoginLogoPath = IMAGES_STR + "NFLogoLogin.gif";

        // to store logo paths in common branded gui environment
        String wspLogoPathInCBG = null;
        String wspLoginLogoPathInCBG = null;

        try
        {
            // cache image path result
            if (!StringUtils.hasValue(imgPath))
            {
                if (StringUtils.hasValue(wspName) && !wspName.equals(DomainType.NOBODY))
                {
                    String wspNameSP = System.getProperty(SecurityFilter.WHOLESALE_PROVIDER);
                    if (StringUtils.hasValue(wspNameSP) && wspNameSP.equals(StoreProviderBase.ANY_VALUE))
                    {
                        wspLogoPathInCBG = IMAGES_STR + wspName + NFLOGO_GIF_STR;
                        wspLoginLogoPathInCBG = IMAGES_STR + wspName + "/NFLogoLogin.gif";
                    }
                }

                // check if Login Logo is different and if it is a valid existing file
                // else check if the plain logo is different for this WSP and if it is a valid exisiting file
                // otherwise return Neustar default login logo/ plain logo
                if (StringUtils.hasValue(wspLoginLogoPathInCBG) && isValidLogoFile(pageContext.getServletContext(), wspLoginLogoPathInCBG))
                    imgPath = wspLoginLogoPathInCBG;
                else if (StringUtils.hasValue(wspLogoPathInCBG) && isValidLogoFile(pageContext.getServletContext(), wspLogoPathInCBG))
                    imgPath = wspLogoPathInCBG;
                else if (isValidLogoFile(pageContext.getServletContext(), nfLoginLogoPath))
                    imgPath = nfLoginLogoPath;
                else
                    imgPath = nfLogoPath;

                if (log.isDebugEnabled())
                    log.debug("Image path calculated via FileUtils.FileStatistics is [" + imgPath + "]");
            }
        }
        catch (Exception e)
        {
            // setting default image path as nfLogoPath in case of any exception
            imgPath = nfLogoPath;
        }

        if (log.isDebugEnabled())
            log.debug("Returning image path calculated/cached as [" + imgPath + "]");

        return imgPath;
    }

    private static boolean isValidLogoFile(ServletContext servletContext, String logoPath)
    {
        String logoRealPath = servletContext.getRealPath(logoPath);
        FileUtils.FileStatistics fileStatLogo = null;

        try
        {
            fileStatLogo = FileUtils.getFileStatistics(logoRealPath);
        }
        catch (FrameworkException fe)
        {
            // return false in case of exception
            return false;
        }

        return (fileStatLogo.exists && fileStatLogo.isFile);
    }

    /**
     * Populate Julian date, if the value of the field is empty.
     *
     * @param pageContext      PageContext object
     * @param field html field
     * @param value present value of the field
     * @param isCopyAction true if present action on GUI is clone
     * @return same if already populated else the generated one
     */
    public static String populateJulianDate (PageContext pageContext, Field field, String value, boolean isCopyAction)
    {
        DebugLogger log = DebugLogger.getLogger(TagUtils.getWebAppContextPath(pageContext), TagUtils.class);
        String customVal = field.getCustomValue(BodyTag.JULIAN_DATE_CUSTOM_VALUE);
        boolean isRegenerateRequired = StringUtils.getBoolean(field.getCustomValue(BodyTag.REGENERATE_FOR_COPY_CUSTOM_VALUE), false);

         // If this field is already populated then we will not change it unless action is copy-order
         if (StringUtils.getBoolean(customVal, false) && (!StringUtils.hasValue(value) || (isRegenerateRequired && isCopyAction)))
         {
            Calendar currentCalendar = Calendar.getInstance();
            int dayOfYear = currentCalendar.get(Calendar.DAY_OF_YEAR);
            String julianDay = Integer.toString(dayOfYear);

            if (dayOfYear < 10)
                julianDay = "00"+ julianDay;
            else if (dayOfYear < 100)
                julianDay = "0"+ julianDay;

            value = Integer.toString(currentCalendar.get(Calendar.YEAR)).substring(2, 4) + julianDay;

             if (log.isDebugEnabled()) {
                 log.debug("populateJulianDate: Returnig value [" + value + "]");
             }
         }
         return value;
    }

    /**
     * Populate Host Id, if the value of the field is empty.
     *
     * @param pageContext      PageContext object
     * @param domainProperties Domain Properties object
     * @param field html field
     * @param value present value of the field
     * @param isCopyAction true if present action on GUI is clone
     * @return same if already populated else the generated one
     */
    public static String populateHostId (PageContext pageContext, DomainProperties domainProperties, Field field, String value, boolean isCopyAction)
    {
        DebugLogger log = DebugLogger.getLogger(TagUtils.getWebAppContextPath(pageContext), TagUtils.class);
        String customVal = field.getCustomValue(BodyTag.HOSTID_CUSTOM_VALUE);
        boolean isRegenerateRequired = StringUtils.getBoolean(field.getCustomValue(BodyTag.REGENERATE_FOR_COPY_CUSTOM_VALUE), false);

         // If this field is already populated then we will not change it unless action is copy-order
         if (StringUtils.getBoolean(customVal, false) && (!StringUtils.hasValue(value) || (isRegenerateRequired && isCopyAction)))
         {
            if (domainProperties != null)
                value = domainProperties.getHostId();

             if (log.isDebugEnabled()) {
                 log.debug("populateHostId: Returnig value [" + value + "]");
             }
         }
         return value;
    }

    /**
     * Populate Sequence Value, if the value of the field is empty.
     *
     * @param pageContext      PageContext object
     * @param field html field
     * @param value present value of the field
     * @param isCopyAction true if present action on GUI is clone
     * @return same if already populated else the generated one
     */
    public static String populateSequenceValue (PageContext pageContext, Field field, String value, boolean isCopyAction)
     {
        DebugLogger log = DebugLogger.getLogger(TagUtils.getWebAppContextPath(pageContext), TagUtils.class);
        String customVal = field.getCustomValue(BodyTag.SEQUENCE_VALUE_CUSTOM_VALUE);
        boolean isRegenerateRequired = StringUtils.getBoolean(field.getCustomValue(BodyTag.REGENERATE_FOR_COPY_CUSTOM_VALUE), false);

         // If this field is already populated then we will not change it unless action is copy-order
         if (StringUtils.hasValue(customVal) && (!StringUtils.hasValue(value) || (isRegenerateRequired && isCopyAction)))
         {
            try
            {
                value = SQLUtil.getSequenceValueAsStr(customVal);
            }
            catch (Exception e)
            {
                log.warn("populateSequenceValue: Unable to obtain value for sequence [" + customVal + "]. Setting it to 0.");
                value = "0";
            }

             if (log.isDebugEnabled()) {
                 log.debug("populateSequenceValue: Returnig value [" + value + "]");
             }
         }
         return value;
    }

    /**
     * Obtains SSO URL from database and if not found returns from web-context.
     *
     * @param application      ServletContext object
     * @param ssoFrom String SSO from product (among CH, SOA & ICP)
     * @param ssoTo String SSO to product (among CH, SOA & ICP)
     * @return SSOURLInitial
     */
    public static String getSSOURL (ServletContext application, String ssoFrom, String ssoTo)
     {
        DebugLogger log = DebugLogger.getLogger(ServletUtils.getWebAppContextPath(application), TagUtils.class);
        String ssoURLProperty = null;
        try
        {
            boolean reverseProxyCustomer = false;
            DomainProperties dProp = DomainProperties.getInstance();

            if (dProp != null)
            {
                reverseProxyCustomer = dProp.isReverseProxyCustomer();
            }

            String ssoURLPropName = null;

            // Domain is reverse proxy domain
            if ( reverseProxyCustomer )
                ssoURLPropName = "SSO_URL_RP_" + ssoFrom + "_TO_" + ssoTo;
            // Domain is NOT reverse proxy
            else
                ssoURLPropName = "SSO_URL_" + ssoFrom + "_TO_" + ssoTo;

            try
            {
                ssoURLProperty = PersistentProperty.getProperty("WEBAPPS","COMMON",ssoURLPropName);
            }
            catch(Exception e)
            {
                // Neither Customer specific nor default SSO url property is configured in Persistent Properties (WEBAPPS,COMMON).
            }

            // if SSO url is not configured in Persistent Property table then obtain it from web.xml
            if ( !StringUtils.hasValue(ssoURLProperty) )
            {
                if (reverseProxyCustomer)
                    ssoURLProperty = application.getInitParameter("REVERSE_PROXY_URL");
                else
                    ssoURLProperty = application.getInitParameter("SSO_URL");
            }            }
        catch (Exception e)
        {
            log.error("Could not obtain SSO URL from [" + ssoFrom + "] to [" + ssoTo +"]");
        }

         if (log.isDebugEnabled()) {
             log.debug("Returnig SSO URL from [" + ssoFrom + "] to [" + ssoTo +"]: [" + ssoURLProperty + "]");
         }
         return ssoURLProperty;
    }

    /**
     * Obtains WSP name for given server name.
     * returns null if invalid servername is passed or no mapping is found for it.
     *
     * @param application      ServletContext object
     * @param serverName String server name
     * @return WSP string WSP for server name
     */
    public static String getWSPforServerName (ServletContext application, String serverName) throws JspException
     {
        DebugLogger log = DebugLogger.getLogger(ServletUtils.getWebAppContextPath(application), TagUtils.class);
        String wsp = null;
        try
        {
            wsp = WSPInfoUtil.getWSPForServer(serverName);
        }
        catch(FrameworkException fe)
        {
            log.error("Could not obtain WSP for server name [" + serverName + "]. "+ fe.getMessage());
            throw new JspException(fe.getMessage());
        }

        if (log.isDebugEnabled()) {
            log.debug("Returnig WSP [" + wsp + "] for server name [" + serverName +"]");
        }

        return wsp;
    }
}
