/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //webgui/R4.4/com/nightfire/webgui/manager/beans/BundleBeanBag.java#1 $
 */

package com.nightfire.webgui.manager.beans;

import com.nightfire.webgui.core.beans.*;

import  javax.servlet.*;
import javax.servlet.http.*;

import com.nightfire.framework.message.MessageException;

import com.nightfire.webgui.core.xml.*;

import com.nightfire.framework.constants.PlatformConstants;

import com.nightfire.framework.util.*;

import com.nightfire.framework.message.common.xml.*;

import java.util.*;

import org.w3c.dom.*;



/**
 * A Bundle implementation of a NFBeanBag
 * It handles the coverting a set of beans into a
 * bundle xml with all components. And it handles spliting apart a bundle xml
 * into separate beans.
 */

public class BundleBeanBag extends MapBeanBag  implements PlatformConstants
{

    private int uniqueSequence = 0;

    private HashMap groups = new HashMap();

    private XMLGeneratorGroup genGroup = new XMLGeneratorGroup();

    public static final String ACTIONS_ROOT = "Actions";
    public static final String ACTION_NODE = "Action";

    private Set actionSet = null;

    /**
     * The prefix appended to the name of a header generator in
     * a generator group.
     *
     */
    public static final String GROUP_HEADER_PREFIX  = "header/";


   public BundleBeanBag(String name)
   {
      super(name);
   }

   public BundleBeanBag()
   {
      this("BundleBeanBag");
   }

    public Set getAllowableActions()
    {
        return actionSet;
    }

    /**
     * The method makes a deep copy of this bean bag and all of its component beans.
     *
     */
    public NFBean getFinalCopy() throws MessageException

    {

      BundleBeanBag copy = new BundleBeanBag(getName());

      copy.setHeaderDataSource(((XMLGenerator)getHeaderDataSource()).getOutputCopy() );

      // copy all beans in the bean bag and maintain the same order amount groups.
      Iterator grpIter = groups.values().iterator();
      while ( grpIter.hasNext() ) {

         List grpList = (List) grpIter.next();
         Iterator grpListIter = grpList.iterator();
         while (grpListIter.hasNext() ) {
            XMLBean bean = (XMLBean) grpListIter.next();
            log.debug("Copying bean instance [" + bean.getId() +"]" );
            copy.addBean( bean.getId(), bean.getFinalCopy() );
         }
       }

      return copy;

    }

    /**
     * Adds a ServiceComponentBean or ModifierBean to this BeanBag with the following id.
     * Each id takes the form of  'serviceType' or 'serviceType(index)'.
     * For modifier bean, can also take an id in the form of 'serviceType.LatestModifier' or
     * serviceType(index).LatestModifier'. Note for ModifierBeans, the parent ServiceComponentBean 
     * must exists first, or it will not be added.
     * If index is not provided then the bean will be added as the last index for this service type.
     *
     * @param key The key to add the bean as.
     * @param bean The NFBean to add to this BeanBag.
     * @return Returns the bean which previously existed at this id, or null if none existed.
     */
    public NFBean addBean(String key, NFBean bean)
    {
        log.debug("addBean(): Adding a bean with key [" + key + "] to the bundle ...");
        
        
        
        if ( bean instanceof ModifierBean) {
            int i = key.indexOf(".");
            if (i > -1) 
                key = key.substring(0, i );

            return addBean(key,(ModifierBean)bean);
        }
        else if (bean instanceof ServiceComponentBean) {
            return addBean(key, (ServiceComponentBean)bean);
        }
        
        else
        {
            log.error("addBean(): The bean being added is not an instance of ServiceComponentBean or ModifierBean.  It will be skipped.");

            return null;
        }
            
    }
    
    private NFBean addBean(String key, ModifierBean bean)
    {
        ServiceComponentBean scBean = (ServiceComponentBean)getBean(key);
        NFBean oldBean = null;
        
        if (scBean != null) {
            oldBean = scBean.getModifierBean();
            try {
                scBean.setModifierBean(bean);
            }
            catch (ServletException e) {
                log.error("Failed to add modifier bean [" + key +"]");
            }
            
        } else
            log.warn("addBean(): Parent ServiceComponent Bean for modifier bean ["+key+ "], does not exist. Skipping the add.");

        return oldBean;
    }
    
            
            
    /**
     * Adds a ServiceComponentBean to this BeanBag with the following id.
     * Each id takes the form of  'serviceType' or 'serviceType(index)'.
     * If index is not provided then the bean will be added as the last index for this service type.
     *
     * @param key The key to add the bean as.
     * @param bean The NFBean to add to this BeanBag.
     * @return Returns the bean which previously existed at this id, or null if none existed.
     */
    private NFBean addBean(String key, ServiceComponentBean bean)
    {


        KeySet keySet  = getKeySet(key);

        NFBean oldBean = null;

        if (keySet.bean != null)
        {
            if (super.exists(keySet.bean))
            {
                log.warn("addBean(): ServiceComponent bean [" + keySet.bean + "] already exists.  It is being removed from the bundle.");

                oldBean = removeBean(keySet.bean);
            }
        }
        else
        {
            int componentBeanCount = getBeanCount(keySet.group);

            keySet.bean            = keySet.group + "(" + String.valueOf(componentBeanCount) + ")";

            log.debug("addBean(): There are currently [" + componentBeanCount + "] component bean of type [" + keySet.group + "] prior to adding.");
        }

        List componentBeans = (List)groups.get(keySet.group);

        if (componentBeans == null)
        {
            componentBeans = new LinkedList();

            groups.put(keySet.group, componentBeans);
        }

        componentBeans.add(bean);

        ((ServiceComponentBean)bean).setId(keySet.bean);

        try
        {
            ((ServiceComponentBean)bean).setServiceType(keySet.group);
        }
        catch (ServletException ex)
        {
            log.error( ex );

            String msg = "addBean(): ServiceComponent bean [" + ((ServiceComponentBean)bean).getId() +
                         "] couldn't set its service type to [" + keySet.group + "]";

            log.error( msg );
            throw new IllegalStateException( msg );
        }

        ((ServiceComponentBean)bean).setParentBag(this);

        // Add the body generator of the bean to a generator group.  This allows
        // templates to access data from other service components.

        try
        {
            int count = genGroup.getGeneratorCount(keySet.group);

            genGroup.setGenerator(keySet.group + "(" + count + ")", (XMLGenerator)bean.getBodyDataSource());
            genGroup.setGenerator(GROUP_HEADER_PREFIX + keySet.group + "(" + count + ")", (XMLGenerator)bean.getHeaderDataSource());
        }
        catch (MessageException e)
        {
            log.error("addBean(): Failed to add body generator for service component bean [" +  keySet.bean + "] to the generator group: " + e.getMessage());
        }

        // Add the bean to the parent map.

        super.addBean(keySet.bean, bean);

        return oldBean;
    }

    /**
     * Obtain the service-component type and id from passed-in string.
     *
     * @param  string  String to parse for type and id.  The id has a format of
     *                 xxx(i) where xxx is the type and i is an integer.
     *
     * @return  KeySet object, wrapper of the type and id.
     */
    private KeySet getKeySet(String string)
    {
        //log.debug("getKeySet(): Parsing for service-component type and id from string [" + string + "] ...");

        String type          = string;

        String id            = null;

        int    indexLocation = string.indexOf("(");

        if (indexLocation  > -1)
        {
            type = string.substring(0, indexLocation);

            id   = type + "(" + string.substring(indexLocation + 1, string.length() - 1) + ")";
        }

        //log.debug("getKeySet(): Creating KeySet object with service-component type [" + type + "] and id [" + id + "] ...");

        return new KeySet(type, id);
    }

    /**
     * Tests for exist of a ServiceComponentBean or a child ModifierBean 
     * with the id specified.
     *
     * Each id must be in the form of 'id(index)' or 'id(index).modifierid'
     */
    public boolean exists(String id)
    {
        return (getBean(id) != null);   
    }


    /**
     * Obtains the ServiceComponentBean or ModifierBean with the id specified.
     *
     * Each id must be in the form of 'id(index)' or 'id(index).modifierid'
     */
    public NFBean getBean(String id)
    {

       int i = id.indexOf(".");
       
       NFBean found = null;
       
       if (i > -1) { // modifier case
            String key = id.substring(0, i);
            KeySet keySet = getKeySet(key);
            ServiceComponentBean bean = (ServiceComponentBean) super.getBean(keySet.bean);
            if (bean != null)
                found = bean.getModifierBean();
            
        }
       else {
            KeySet keySet = getKeySet(id);
            found = (ServiceComponentBean) super.getBean(keySet.bean);
        }
  

       return found;
       
    }


    /**
     * Removes the service component bean and any associated modifer bean 
     * with the id specified.
     *
     * Each id must be in the form of 'id(index)'.
     * 
     */
    public NFBean removeBean(String id)
    {
        KeySet keySet         = getKeySet(id);

        if ( !super.exists(keySet.bean) )  {
           log.warn("removeBean(): Component bean [" + keySet.bean + "] does not exist, skipping remove.");
           return null;
        }

        ServiceComponentBean removedBean = (ServiceComponentBean) super.removeBean(keySet.bean);

        List grpList = (List)groups.get(keySet.group);

        if (grpList != null) {
           int index = grpList.indexOf(removedBean);

           if ( index >= 0 )
               grpList.remove(index);
        }

        if (log.isDebugEnabled())
            log.debug("removeBean(): Component bean [" + keySet.bean + "] was removed from the bean bag.");
       
 
        try {
          // remove the body generator of the bean from the generator group
          XMLGenerator gen = (XMLGenerator)removedBean.getBodyDataSource();
          genGroup.removeGenerator(gen);

          // remove the header generator of the bean from the group
          XMLGenerator hdr = (XMLGenerator)removedBean.getHeaderDataSource();
          genGroup.removeGenerator(hdr);


       } catch (MessageException e) {
          log.error("Failed to remove body generator for service component bean [" +  keySet.bean + "] from generator group: " + e.getMessage());
       }
        return removedBean;
    }



  /**
   * Returns all the groups within this bundle bean bag.
   *
   * @return a <code>Set</code> with all current groups ( each object is a String)
   */
  public Set getBeanGroups()
  {
    return groups.keySet();
  }

    /**
     * Returns the number of service components with the specified id.
     * Each id takes the form of 'serviceType'.
     */
    public int getBeanCount(String id)
    {
        KeySet keySet  = getKeySet(id);

        int    count   = 0;

        List   grpList = (List)groups.get(keySet.group);

        if (grpList != null)
        {
            count = grpList.size();
        }

        return count;
    }


    /**
     * Obtains a list of service components of the same service type.
     *
     * @param  serviceType  The id of the bean with the index.
     *
     * @return  A list of service components beans.
     * If the serviceType is not valid returns null.
     */
    public List getBeans(String serviceType)
    {

       KeySet keySet         = getKeySet(serviceType);

       int count = 0;

       List grpList = (List)groups.get(keySet.group);

        if (grpList != null) {
           count = grpList.size();

           if (log.isDebugEnabled() )
              log.debug("getBeans(): Returning [" + count + "] service-component beans of type [" + keySet.group+ "] ...");
           return grpList;
        } else
           return null;

    }

    /**
    * Uses the current beans in the bag to form a bundle.
    *
    * NOTE: Any transforms that need to be done on the ServiceComponent beans need to be done
    * BEFORE this method is called.
    *
    * @return A bundle as one xml document without id attributes on each service component xml data.
    * @exception MessageException if the xml can not be created.
    */
   public Document compose()  throws MessageException
   {
      return compose(false);
   }


    /**
    * Uses the current beans in the bag to form a bundle.
    *
    * NOTE: Any transforms that need to be done on the ServiceComponent beans need to be done
    * BEFORE this method is called.
    *
    * @return A bundle as one xml document with ids on each service component xml data.
    * @exception MessageException if the xml can not be created.
    */
   public Document composeWithIds()  throws MessageException
   {
      return compose(true);
   }


   /**
    * Uses the current beans in the bag to form a bundle. Not the bundle xml only contains service component
    * beans data and no modifier bean info.
    *
    * NOTE: Any transforms that need to be done on the ServiceComponent beans need to be done
    * BEFORE this method is called.
    *
    * @return A bundle as one xml document.
    * @exception MessageException if the xml can not be created.
    */
   private Document compose(boolean keepIds)  throws MessageException
   {

      XMLPlainGenerator bundle = new XMLPlainGenerator(BODY_NODE);

      // append any bundle header nodes to the root info node
      log.debug("BundleBeanBag:Creating bundle info node");
      Node rootInfo = bundle.create(INFO_NODE);
      XMLGenerator header = (XMLGenerator) this.getHeaderDataSource();
      bundle.copyChildren(rootInfo, header.getDocument().getDocumentElement());

      Iterator grpIter = groups.entrySet().iterator();
      while ( grpIter.hasNext() ) {

         Map.Entry entry = (Map.Entry) grpIter.next();
         String group = (String) entry.getKey();
         List grpList = (List) entry.getValue();
         int count = 0;


         Iterator beanIter = grpList.iterator();

         while (beanIter.hasNext() ) {

            String id = group +"(" + count++ +")";

            log.debug("BundleBeanBag: Creating service component node :" + id);
            // create service component node and its info node
            Node scNode = bundle.create(id);

            // get the bean holding this service component
            ServiceComponentBean bean = (ServiceComponentBean) beanIter.next();

            // add the request data for this service component
            // if keepids is true copy all nodes with the id attributes
            // otherwise copy all nodes without id attributes.
            //
            Document doc = null;

            if ( keepIds)
               doc = bean.composeWithIds();
            else
               doc = bean.compose();

            bundle.copyChildren(scNode, doc.getDocumentElement().getFirstChild());
         }

      }

      return bundle.getDocument();

   }



   /**
    * Decomposes a bundle into individual service component beans
    * and places them in this bean bag.
    *
    * NOTE: Any transforms that need to be done on the ServiceComponent beans can be done
    * after this method is called.
    *
    * @param bundleXml -The bundle xml.
    * @exception MessageException if the xml can not be created.
    * @exception ServletException If the inner beans could not be created.
    */
   public void decompose(String bundleXml) throws MessageException, ServletException
   {
      XMLPlainGenerator bundle = new XMLPlainGenerator(bundleXml);
      decompose(bundle);
   }

   /**
    * Decomposes a bundle into individual service component beans
    * and places them in this bean bag.
    *
    * NOTE: Any transforms that need to be done on the ServiceComponent beans can be done
    * after this method is called.
    *
    * @param bundleXml -The bundle xml.
    * @exception MessageException if the xml can not be created.
    * @exception ServletException If the inner beans could not be created.
    */
   public void decompose(Document bundleXml) throws MessageException, ServletException
   {
      XMLPlainGenerator bundle = new XMLPlainGenerator(bundleXml);
      decompose(bundle);
   }

   /**
    * Decomposes a bundle into individual service component beans
    * and places them in this bean bag. This method does not handle any modifier beans.
    *
    * NOTE: Any transforms that need to be done on the ServiceComponent beans can be done
    * after this method is called.
    *
    * @param bundle - The XMLGenerator holding the bundle xml.
    * @exception MessageException if the xml can not be created.
    * @exception ServletException If the inner beans could not be created.
    */
   public void decompose(XMLGenerator bundle) throws MessageException, ServletException
   {
      Node [] hFields = null;

      XMLGenerator header = (XMLGenerator) getHeaderDataSource();

      if ( bundle.exists(INFO_NODE) ) {
          Node info = bundle.getNode(INFO_NODE);
                    
          if( bundle.exists(info, ACTIONS_ROOT)) {
              Node actionRoot = bundle.getNode(info, ACTIONS_ROOT);
              Node [] actions = bundle.getChildren(actionRoot, ACTION_NODE);
              actionSet = new HashSet();

              for(int j=0; j < actions.length; j++) {
                  actionSet.add(bundle.getNodeValue(actions[j]));
              }

              if (log.isDebugEnabled())
                  log.debug("Added the following bundle actions [" + actionSet +"]");

              bundle.remove(info, ACTIONS_ROOT);
          }

          header.copyChildren(header.getDocument().getDocumentElement(), info );
       
      }


      Node [] scChildren = bundle.getChildren(bundle.getDocument().getDocumentElement());

      for (int i =0; i < scChildren.length; i++ ) {

	   String child = scChildren[i].getNodeName();

	   if ( ! child.equals(INFO_NODE) ) {

        if (Debug.isLevelEnabled(Debug.MSG_DATA))
        {
	        log.debug("decompose(): Inspecting component child [" + child + "] ...");
	    }

		ServiceComponentBean bean = new ServiceComponentBean((String) null, (String)null);

	      bean.decompose(bundle, scChildren[i]);

            addBean(scChildren[i].getNodeName(), bean);
	   }
      }
   }

    public void clear()
    {
       groups.clear();
       super.clear();
    }




   public static final void main(String[] args)
   {

      Debug.enableAll();
      Debug.showLevels();
      Properties props = new Properties();
     // props.setProperty(Debug.LOG_FILE_NAME_PROP,"unittest.log");

      //Debug.configureFromProperties(props);
      StringBuffer bundle = new StringBuffer("<?xml version=\"1.0\"?>");
      bundle.append("<Body>\n<Info>\n");
      bundle.append("<MetaDataName value=\"blah\"/>\n");
      bundle.append("<Action value=\"Submit\"/>\n");
      bundle.append("</Info>\n");
      bundle.append("<Loop>\n");
      bundle.append("<Info><Supplier value=\"acme\"/></Info>");
      bundle.append(" <Request><lsr_order><Admin><Pon value=\"bdff\"/>");
      bundle.append(" <CCNA value=\"ccna\"/></Admin><contact><name value=\"dan\"/></contact></lsr_order></Request>");
      bundle.append("</Loop>");
      bundle.append("<E911>\n");
      bundle.append("  <Info><Supplier value=\"ilum\"/></Info>");
      bundle.append("  <Root><e911_info><b value=\"b\"/></e911_info></Root>\n");
      bundle.append("</E911>\n");
      bundle.append("<E911>\n");
      bundle.append("  <Info><Supplier value=\"ilum2\"/></Info>");
      bundle.append("  <Root><e911_info><b value=\"b2\"/></e911_info></Root>\n");
      bundle.append("</E911>\n");
      bundle.append("</Body>\n");

      BundleBeanBag bag = new BundleBeanBag("bag");

      try {
      bag.decompose(bundle.toString());

      String initBagDescr = bag.describeBodyData();

      System.out.println("Decomposed this xml:\n" + bundle.toString() + "\n");
      System.out.println("To the following bean bag:\n" + initBagDescr + "\n");

      XMLPlainGenerator gen = new XMLPlainGenerator(bag.compose() );

      System.out.println("Composed separated beans back into bundle xml:\n" + gen.getOutput());

      bag.clear();
      bag.decompose(gen);

      String secondBagDescr = bag.describeBodyData();

      //System.out.println(" Test compare initial\n" + initBagDescr+"\n second \n" + secondBagDescr);

      if ( initBagDescr.equals(secondBagDescr) )
         System.out.println(" Test passed decomposition --> composition --> decomposition created the same results");
      else
         System.out.println(" Test failed decomposition --> composition --> decomposition created the different results");

      } catch (MessageException e ) {
         System.err.println("failed to create bundle bag " + e.getMessage());

      } catch (ServletException e ) {
         System.err.println("failed to create bundle bag " + e.getMessage());
      }
   }


   public String describeBeans(boolean includeBeanData)
   {
      StringBuffer summary = new StringBuffer("BundleBeanBag: ServiceComponent Beans : \n");
      return summary.toString() + super.describeBeans(includeBeanData);

   }


    /**
     * Convenient method for displaying the string representation of the header data.
     *
     * @return  Header data string.
     */
    public String describeHeaderData()
    {
        StringBuffer descr = new StringBuffer("\nActions: " + actionSet).append("\n");
        
        descr.append(super.describeHeaderData());

        
        return descr.toString();
        
    }

   private class KeySet
   {
      public String group;
      public String bean;

      public KeySet(String grp, String bKey)
      {
         group = grp;
         bean = bKey;
      }

   }

}
