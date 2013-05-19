/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.meta;

// jdk imports
import java.util.Arrays;
import java.util.List;

// third-party imports
import org.w3c.dom.*;

// nightfire imports
import com.nightfire.framework.message.util.xml.ParsedXPath;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.debug.*;


/**
 * DataType provides data validation and formatting information for a
 * {@link Field} object.
 */
public class DataType
{
    /*
     * Values in XML document
     */
    protected static final String[] USAGE_VALUES =
    {
        "CONDITIONAL",
        "OPTIONAL",
        "PROHIBITED",
        "REQUIRED"
    };
    protected static final List USAGE_LIST = Arrays.asList(USAGE_VALUES);
    protected static final String[] TYPE_VALUES =
    {
        "DATE TIME",
        "DATE",
        "DECIMAL",
        "ENUMERATED",
        "TEXT",
        "TEXT AREA",
        "TIME",
        "TELEPHONE NUMBER",
        "DATE OPTIONAL TIME",
        "REL DATE OPTIONAL TIME",
        "RADIO / OPTION BUTTON",
        "CHECK BOX",
        "SPID"
    };
    protected static final List TYPE_LIST = Arrays.asList(TYPE_VALUES);
    protected static final String TEXT_AREA_NAME = "TEXT_AREA";

    /**
     * Used to indicate a Field is conditionally required
     */
    public static final int CONDITIONAL = 0;

    /**
     * Used to indicate a Field is optional
     */
    public static final int OPTIONAL = 1;

    /**
     * Used to indicate a Field is prohibited
     */
    public static final int PROHIBITED = 2;

    /**
     * Used to indicate a Field is required
     */
    public static final int REQUIRED = 3;

    /**
     * Used to indicate a date and time data type
     */
    public static final int TYPE_DATE_TIME = 0;

    /**
     * Used to indicate a date without time data type
     */
    public static final int TYPE_DATE = 1;

    /**
     * Used to indicate a number data type
     */
    public static final int TYPE_DECIMAL = 2;

    /**
     * Used to indicate an enumerated data type
     */
    public static final int TYPE_ENUMERATED = 3;

    /**
     * Used to indicate a text data type
     */
    public static final int TYPE_TEXT = 4;

    /**
     * Used to indicate a text field data type
     */
    public static final int TYPE_TEXT_AREA = 5;

    /**
     * Used to indicate a text data type
     */
    public static final int TYPE_TIME = 6;

    /**
     * Used to indicate a Telephone number data type
     */
    public static final int TYPE_TELEPHONE_NUMBER = 7;

    /**
	* Used to indicate a Telephone number data type
	*/
    public static final int TYPE_DATE_OPTIONAL_TIME = 8;


    /**
	* Used to indicate a Telephone number data type
	*/
    public static final int TYPE_REL_DATE_OPTIONAL_TIME = 9;

    /**
	* Used to indicate a Radio button controls
	*/
    public static final int TYPE_RADIO_BUTTON = 10;

    /**
	* Used to indicate a CheckBox button controls
	*/
    public static final int TYPE_CHECK_BOX = 11;

    /**
	* Used to indicate a Telephone number data type
	*/
    public static final int TYPE_SPID = 12;

    /**
     * Used to indicate a value is unspecified
     */
    public static final int UNSPECIFIED = -1;

    /**
     * Minimum allowable length
     */
    protected int minLen = UNSPECIFIED;

    /**
     * Maximum allowable length
     */
    protected int maxLen = UNSPECIFIED;

    /**
     * Indicates whether or not this field is required.  May be REQUIRED,
     * OPTIONAL, PROHIBITED, or CONDITIONAL.
     */
    protected int usage = UNSPECIFIED;

    /**
     * Format string
     */
    protected String format = null;

    /**
     * Underlying data type.  May be TYPE_DECIMAL, TYPE_DATE, TYPE_ENUMERATED,
     * TYPE_TEXT, TYPE_TELEPHONE_NUMBER or TYPE_TEXT_AREA.
     */
    protected int type = UNSPECIFIED;

    /**
     * Source for enumerated values
     */
    protected OptionSource optionSource = null;

    /**
     * Examples
     */
    protected String[] examples = null;

    /**
     * Data type name
     */
    protected String name = null;

    /**
     * Base data type
     */
    protected DataType baseType = null;

    protected DebugLogger log;



    /**
     * Constructor
     */
    public DataType()
    {
      log = DebugLogger.getLoggerLastApp(DataType.class);

    }

    /**
     * Returns the minimum allowable length
     */
    public int getMinLen()
    {
        if ( (minLen == UNSPECIFIED) && (baseType != null) )
            return baseType.getMinLen();
        else
            return minLen;
    }

    /**
     * Returns the maximum allowable length
     */
    public int getMaxLen()
    {
        if ( (maxLen == UNSPECIFIED) && (baseType != null) )
            return baseType.getMaxLen();
        else
            return maxLen;
    }

   /**
    * Sets the max length of this data type.
    */
    public void setMaxLen(int len)
    {
       maxLen = len;
    }

    /**
     * Returns a value indicating the usage of the field. May be REQUIRED,
     * OPTIONAL, PROHIBITED, or CONDITIONAL.
     */
    public int getUsage()
    {
        if ( (usage == UNSPECIFIED) && (baseType != null) )
            return baseType.getUsage();
        else
            return usage;
    }

    /**
     * Returns a human-readable string describing the value returned by
     * getUsage()
     */
    public String getUsageName()
    {
        int idx = getUsage();

        if (idx == UNSPECIFIED)
            return null;
        else
            return (String)USAGE_LIST.get(idx);
    }


    /**
     * Returns the format string.
     */
    public String getFormat()
    {
        if ( (format == null) && (baseType != null) )
            return baseType.getFormat();
        else
            return format;
    }

     /**
     * sets the format string for this datatype.
     */
    public void setFormat(String format)
    {
       this.format = format;
    }

    /**
     * Returns a value indicating the underlying data type.  May be
     * TYPE_DECIMAL, TYPE_DATE, TYPE_ENUMERATED, TYPE_TEXT, TYPE_TELEPHONE_NUMBER
     * or TYPE_TEXT_AREA.
     */
    public int getType()
    {
        if ( (type == UNSPECIFIED) && (baseType != null) )
            return baseType.getType();
        else
            return type;
    }

    /**
     * Returns a human-readable string describing the value returned by
     * getType()
     */
    public String getTypeName()
    {
        int idx = getType();

        if (idx == UNSPECIFIED)
            return null;
        else
            return (String)TYPE_LIST.get(idx);
    }

    /**
     * Returns an object which may be used to obtain a list of enumerated
     * value for the DataType and information about the values.
     */
    public OptionSource getOptionSource()
    {
        if ( (optionSource == null) && (baseType != null) )
            return baseType.getOptionSource();
        else
            return optionSource;
    }

    /**
     * Sets the object which may be used to obtain a list of enumerated
     * value for the DataType and information about the values.
     *
     * @param optSrc  The new OptionSource to use
     */
    public void setOptionSource(OptionSource optSrc)
    {
        optionSource = optSrc;
    }

    /**
     * Returns a list of example values.
     */
    public String[] getExamples()
    {
        if ( (examples == null) && (baseType != null) )
            return baseType.getExamples();
        else
            return examples;
    }

    /**
     * Returns the name of this DataType.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Checks this DataType and all of its base DataTypes to see if any match
     * the provided name.  This allows a single check to determine if this
     * is a particular DataType or a derivative of it.
     *
     * @param name The name to check
     *
     * @return true if this DataType or one of its ancestors matches
     *         <code>name</code>
     */
    public boolean isInstance(String name)
    {
        // check our name first
        if (this.name != null)
        {
            if (stripIdSuffix(this.name).equals(name))
                return true;
        }

        // now check ancestors
        if (baseType != null)
            return baseType.isInstance(name);

        return false;
    }

    /**
     * Returns this DataType's base DataType.
     */
    public DataType getBaseType()
    {
        return baseType;
    }

    /**
     * Sets this DataType's base DataType.
     *
     * @param baseType  The new base DataType to use
     */
    public void setBaseType(DataType baseType)
    {
        this.baseType = baseType;
    }

    /**
     * Read this object from an XML document
     *
     * @param ctx    The context node (Form element) in the XML document
     * @param msg    The containing message object
     *
     * @exception FrameworkException Thrown if the XML document is invalid
     *                               or there is an error accessing it
     */
    public void readFromXML(Node ctx, Message msg) throws FrameworkException
    {
        MessagePaths xpaths = msg.getXPaths();

        // minLen
        minLen = getInt(xpaths.minLengthPath, ctx);

        // maxLen
        maxLen = getInt(xpaths.maxLengthPath, ctx);

        // usage
        String strUsage = getString(xpaths.usagePath, ctx);
        if (strUsage != null)
            usage = USAGE_LIST.indexOf(strUsage);

        // format
        format = getString(xpaths.formatPath, ctx);

        // optionSource
        List optNodes = xpaths.enumPath.getNodeList(ctx);
        if (optNodes.size() > 0)
        {
            Node newCtx = (Node)optNodes.get(0);
            String cName = xpaths.enumClassPath.getValue(newCtx);

            // create a new instance of the class
            try
            {
                optionSource =
                    (OptionSource)Class.forName(cName).newInstance();
            }
            catch (Exception ex)
            {
                throw new FrameworkException(ex);
            }

            // prep it
            optionSource.readFromXML(newCtx, msg);
        }

        // examples
        examples = xpaths.examplesPath.getValues(ctx);

        // name
        name = getString(xpaths.idPath, ctx);
        if (name != null)
            msg.registerDataType(this);

        // type & baseType
        String strType = getString(xpaths.baseTypePath, ctx);
        if (strType != null)
        {
            // see if this is a fundamental type
            int idxType = TYPE_LIST.indexOf(strType);
            if (idxType != -1)
            {
                type = idxType;
                if (type == TYPE_TEXT)
                {
                    // see if it's a text area
                    String areaFlag = getString(xpaths.textAreaPath, ctx);
                    if (areaFlag != null)
                    {
                        if (areaFlag.equalsIgnoreCase("true"))
                            type = TYPE_TEXT_AREA;
                    }
                }
            }
            else
            {
                // this should be the name of a base type
                baseType = msg.getDataType(strType);

                if (baseType == null)
                    log.warn(
                              "No match for base data type \"" + strType
                              + "\".");
            }
        }
    }

    /**
     * Gets the String value from a ParsedXPath, returning null if no value
     * is present
     */
    protected String getString(ParsedXPath xpath, Node ctx)
        throws FrameworkException
    {
        List nodes = xpath.getNodeList(ctx);
        if (nodes.size() < 1)
            return null;
        else
            return ((Node)nodes.get(0)).getNodeValue();
    }

    /**
     * Gets the integer value from a ParsedXPath, returning UNSPECIFIED if no
     * value is present
     */
    protected int getInt(ParsedXPath xpath, Node ctx) throws FrameworkException
    {
        String strVal = getString(xpath, ctx);
        if (strVal == null)
            return UNSPECIFIED;
        return StringUtils.getInteger(strVal);
    }

    /**
	 * Strips an ID string of any # character and anything that follows.  To
	 * allow unique ids to be used for parts with the same XML element name,
	 * # is allowed in an id, but not retained as part of the XML path
	 */
	protected String stripIdSuffix(String id)
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

}
