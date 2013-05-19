package com.nightfire.order.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.nightfire.framework.util.Debug;
import com.nightfire.order.common.CHOrderContext;

import biz.neustar.nsplatform.db.pojo.POJOBase;

/**
 * Utility Class to provide various utility methods to support Order Base framework.
 * @author Abhishek Jain
 */
public class CHOrderUtils 
{

    /**
     * Sets the value for POJO attribute identified by specified name.
     * @param pojoObject Pojo business object of type POJOBase.
     * @param to attribute name in Pojo
     * @param value 
     */
    public static void setPOJOAttribute(POJOBase pojoObject, String to, Object value)
    throws CHOrderException
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "CHOrderUtils: setAttribute pojo["+pojoObject.getClass().getSimpleName()+"]" +
                    " attr["+to+"=>"+value+"]");
        if(value!=null)
            try
        {
                Class pojoClass = pojoObject.getClass();
                Field toField = pojoClass.getField(to);
                Class fieldType = toField.getType();
                Object fieldCastValue = cast(value, fieldType);
                toField.set(pojoObject, fieldCastValue);
        }
        catch (Exception e) 
        {
            Debug.log(Debug.ALL_WARNINGS, "Unable to set field name["+to+"] value["+value+"] PojoType["+pojoObject.getClass()+"]. "+e.getMessage());
            throw new CHOrderException("Unable to set field name["+to+"] value["+value+"] PojoType["+pojoObject.getClass()+"]. "+e.getMessage());
        }
    }

    /**
     * cast a give value as String to respective type defined by fieldType.
     * @param value
     * @param fieldType
     * @return
     * @throws ParseException 
     */
    private static Object cast(Object value, Class fieldType)
    throws ParseException 
    {  	    
        if(String.class.equals(fieldType))
            return value.toString();
        else if(Integer.class.equals(fieldType))
            return Integer.parseInt(value.toString());
        else if(Float.class.equals(fieldType))
            return Float.parseFloat(value.toString());
        else if(Long.class.equals(fieldType))
            return Long.parseLong(value.toString());
        else if(Date.class.equals(fieldType))
        {
            return new SimpleDateFormat("MMddyyyyHHmm").parse(value.toString());

        }


        return null;
    }

    /**
     * gets the value associated to POJO's attribute.
     * @param pojoObject
     * @param from
     * @return
     * @throws Exception
     */
    public static String getPOJOAttribute(POJOBase pojoObject, String from) 
    throws CHOrderException
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "CHOrderUtils: getAttribute pojo["+pojoObject.getClass().getSimpleName()+"] attr["+from+"].");
        Class pojoClass = pojoObject.getClass();
        Field fromField;
        try 
        {
            fromField = pojoClass.getField(from);
            Class fieldType = fromField.getType();
            Object retValue = fromField.get(pojoObject);
            
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))            
                Debug.log(Debug.MSG_STATUS,"CHOrderUtils: val=>"+retValue.toString());
            
            return retValue.toString();
        } 
        catch (NullPointerException ex)
        {
            return null;
        }
        catch (Exception e) 
        {
            Debug.log(Debug.ALL_WARNINGS, "Unable to get field name["+from+"]. "+e.getMessage());
            throw new CHOrderException("Unable to get field name["+from+"]. "+e.getMessage());
        }
    }

    /**
     * perform the specified operation on the POJO. 
     * @param pojoObject
     * @param pojoMethod
     */
    public static void performOperation(POJOBase pojoObject, String pojoMethod,CHOrderContext context)
    throws CHOrderException 
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "CHOrderUtils: performOperation pojo["+pojoObject.getClass().getSimpleName()+"],pojoMethod["+pojoMethod+"].");
        
        Connection conn;
        try 
        {
            conn = context.getDbConnection();
        } 
        catch (Exception e) 
        {
            Debug.log(Debug.ALL_WARNINGS, "performOperation() : Unable to get a DBConnection "+e.getMessage());
            throw new CHOrderException("Could not get a DBConnection, details["+e.getMessage()+"]");
        }

        try 
        {
            Method method = pojoObject.getClass().getMethod(pojoMethod, new Class[]{Connection.class});
            method.invoke(pojoObject, new Object[]{conn});
        }
        catch (Exception e) 
        {
            Debug.log(Debug.ALL_WARNINGS, "Unable to execute method "+Debug.getStackTrace(e));
            Throwable cause = null;
            if(e instanceof InvocationTargetException)
            {
                cause = e.getCause();
                Debug.log(Debug.ALL_WARNINGS, "Cause of Exception : "+Debug.getStackTrace(cause));
            }
            else if(e.getCause()!=null)
            {
                cause = e.getCause();
                Debug.log(Debug.ALL_WARNINGS, "Cause of Exception : "+Debug.getStackTrace(cause));
            }
            if(cause!=null)
                    throw new CHOrderException("Could not execute method["+pojoMethod+"] " +
                            "on pojo["+pojoObject.getClass()+"], details["+Debug.getStackTrace(cause)+"]");
            else
                    throw new CHOrderException("Could not execute method["+pojoMethod+"] " +
                            "on pojo["+pojoObject.getClass()+"], details["+Debug.getStackTrace(e)+"]");
        }
    }
}

