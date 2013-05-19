/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.beans;

import  javax.servlet.ServletException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.*;
import com.nightfire.webgui.core.xml.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.debug.*;


import java.util.*;

import java.lang.UnsupportedOperationException;



/**
 * A Map implementation of a NFBeanBag that holds XMLBeans
 * and supports header fields.
 */
 
public class MapBeanBag implements NFBeanBag
{
    private HashMap beans;
    private String name ="";

    private XMLBean headerBean;
    
  protected DebugLogger log;
  

    public MapBeanBag()
    {
       log = DebugLogger.getLoggerLastApp(getClass());            
       beans = new HashMap();

       try {
          headerBean = new XMLBean((String)null, (String)null);
       } catch (ServletException e ) {
          log.error("Failed to create bean bag: " + e.getMessage()  );
       }
    }

    public MapBeanBag(String name)
    {
       this();
       setName(name);
    }


    public boolean exists(String id)
    {
       return beans.containsKey(id);
    }


    public void setName(String name )
    {
      this.name = name;
    }

    public String getName()
    {
       return name;

    }


    public NFBean getBean(String id)
    {
       return (NFBean)beans.get(id);
    }




    public NFBean addBean(String id, NFBean bean)
    {
       return (NFBean) beans.put(id, bean);
    }



    public NFBean removeBean(String id)
    {

       return (NFBean) beans.remove(id);
    }

    /**
     * Returns a map of the beans in this bean bag.
     */
    public Map getBodyAsMap()
    {
       return beans;
    }


    public String describeBeans(boolean includeBeanData)
    {

       StringBuffer descr = new StringBuffer("\nMapBeanBag: ").append(name).append(" Description\n");

       descr.append("\n\n Body Beans:\n");
       Iterator iter = beans.entrySet().iterator();

       while (iter.hasNext() ) {
          Map.Entry entry = (Map.Entry) iter.next();
          String name = (String) entry.getKey();
          NFBean bean = (NFBean) entry.getValue();

          descr.append("------------  ").append(name).append(" Bean ------------\n");

          if (includeBeanData) {
             descr.append("     Header:\n");
             descr.append(bean.describeHeaderData());
             descr.append("\n     Body:\n");
             descr.append(bean.describeBodyData() );

          }
       }

       return descr.toString();

    }


    /**
     * Set a header field value.
     * If the key started with ServletConstants.NF_FIELD_HEADER_PREFIX it is stripped off.
     */
    public void setHeaderValue(String key, String value) throws ServletException
    {
      headerBean.setHeaderValue(key,value);
    }

    /**
     * Not Supported
     */
    public void setBodyValue(String key, String value) throws ServletException
    {
       throw new UnsupportedOperationException("SetBodyValue not supported on a BeanBag");
    }

     /**
     * Obtain the header data structure object.
     *
     *
     * @return  XMLGenerator object.
     */
    public Object getHeaderDataSource()
    {
        return headerBean.getHeaderDataSource();
    }


    public void setHeaderDataSource(Object headerDataSource)
    {
      headerBean.setHeaderDataSource(headerDataSource);
    }

    
    /**
     * Not Supported
     */
    public void setBodyDataSource(Object bodyDataSource)
    {
      throw new UnsupportedOperationException("getBodyDataSource not supported on a BeanBag");
    }


    
    /**
     * Not Supported
     */
    public Object getBodyDataSource()
    {
       throw new UnsupportedOperationException("getBodyDataSource not supported on a BeanBag");
    }


    /**
     * Get a header field value.
     * If the key started with ServletConstants.NF_FIELD_HEADER_PREFIX it is stripped off.
     */
    public String getHeaderValue(String key)
    {
       return headerBean.getHeaderValue(key);
    }


    /**
     * Not Supported
     */
    public String getBodyValue(String key)
    {
       throw new UnsupportedOperationException("getBodyValue not supported on a BeanBag");
    }


    
    /**
     * Add header data to this bean. All existing keys in the bean are replaced
     * by the same key from the map.
     * All fields starting with ServletConstants.NF_FIELD_HEADER_PREFIX are added as header fields.
     * All fields starting with ServletConstants.NF_FIELD_PREFIX are added as body fields.
     * All other fields are ignored.
     *
     * @param  Data -  Map of data
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public void addToHeaderData(Object data) throws ServletException
    {
       headerBean.addToHeaderData(data);
    }

    /**
     * Not Supported
     */
    public void addToBodyData(Object data) throws ServletException
    {
        throw new UnsupportedOperationException("addToBodyData not supported on a BeanBag");
    }


    /**
     * Obtains the response code given in the header data.  This is applicable only
     * in the case where the Bean encapsulates the response data.
     *
     * @return  Response code.
     */
    public String getResponseCode()
    {
       return headerBean.getResponseCode();
    }
    
    /**
     * Convenient method for displaying the string representation of the header data.
     *
     * @return  Header data string.
     */
    public String describeHeaderData()
    {
       return headerBean.describeHeaderData();
    }
    
    /**
     * Convenient method for displaying the string representation of the body data.
     * Calls {link @describeBeans}(true).
     *
     * @return  Body data string.
     */
    public String describeBodyData()
    {
       return describeBeans(true);
    }



    
    /**
     * returns the header data as a map object
     */
    public Map getHeaderAsMap()
    {
       return headerBean.getHeaderAsMap();
    }


    /**
     * Makes a final deep copy of this bean and all of the beans contained within.
     * @return A new MapBeanBag of all beans with finalized data.
     */
    public NFBean getFinalCopy() throws MessageException

    {

      MapBeanBag copy = new MapBeanBag(getName());

      copy.headerBean  = (XMLBean) this.headerBean.getFinalCopy();

      Iterator iter = beans.entrySet().iterator();

      while ( iter.hasNext() ) {
         Map.Entry entry = (Map.Entry) iter.next();
         String id = (String) entry.getKey();
         NFBean beanCopy = ((NFBean) entry.getValue()).getFinalCopy();
         copy.addBean(id, beanCopy);
      }

      return copy;

    }


    public boolean removeHeaderField(String field)
    {
      return headerBean.removeHeaderField(field);
    }


    public void clear()
    {
       beans.clear();
    }

    public void setHeaderTransform(Object transform) throws ServletException
    {
        headerBean.setHeaderTransform(transform);
     }

     /**
     * Not Supported
     */
    public void setBodyTransform(Object transform) throws ServletException
    {
         throw new UnsupportedOperationException("setBodyTransform not supported on a BeanBag");
    }

    public Object getHeaderTransform()
    {
       return headerBean.getHeaderTransform();
    }

     /**
     * Not Supported
     */
    public Object getBodyTransform()
    {
         throw new UnsupportedOperationException("getBodyTransform not supported on a BeanBag");
    }



    public void transform() throws MessageException
    {
       headerBean.transform();

       Iterator beanIter = beans.values().iterator();
       while (beanIter.hasNext() )
       {
          NFBean bean = (NFBean) beanIter.next();
          bean.transform();
       }

    }


    /**
     * Clears all transform objects set on this bean and all inner beans.
     */
    public void clearTransforms()
    {
       headerBean.clearTransforms();

       Iterator beanIter = beans.values().iterator();
       while (beanIter.hasNext() )
       {
          NFBean bean = (NFBean) beanIter.next();
          bean.clearTransforms();
       }
    }


}
