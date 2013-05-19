/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //webgui/core/com/nightfire/webgui/manager/beans/ServiceComponentBean.java#23 $
 */

package com.nightfire.webgui.manager.beans;


import com.nightfire.webgui.core.beans.*;
import com.nightfire.webgui.core.*;

import com.nightfire.webgui.core.xml.*;
import com.nightfire.webgui.manager.*;

import  javax.servlet.*;
import javax.servlet.http.*;


import  org.w3c.dom.*;

import  com.nightfire.framework.util.*;
import  com.nightfire.framework.message.common.xml.*;
import  com.nightfire.framework.message.transformer.*;

import  com.nightfire.framework.constants.PlatformConstants;
import  com.nightfire.framework.message.*;




import java.util.*;
import org.w3c.dom.Node;

/**
 * A base class that know how to decompose/compose and info header section and a body section.
 *
 *
 */

public abstract class InfoBodyBase extends XMLBean implements ManagerServletConstants, PlatformConstants
{

    public static final String ACTIONS_ROOT = "Actions";
    public static final String ACTION_NODE = "Action";

    private Set actionSet = null;
    private String serviceType;

    public InfoBodyBase()  throws ServletException
    {
        super();

    }
    /**
     * Constructor.
     *
     * @param  headerData  Map of header fields.
     * @param  bodyData    Map of body fields.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public InfoBodyBase(Map headerData, Map bodyData) throws ServletException
    {
       super(headerData, bodyData);
    }

    /**
     * Constructor.
     *
     * @param  headerData  XML header document.
     * @param  bodyData    XML body document.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public InfoBodyBase(String headerData, String bodyData) throws ServletException
    {
        super(headerData, bodyData);

    }

    /**
     * Constructor.
     *
     * @param  headerData  XMLGenerator object for the header data.
     * @param  bodyData    XMLGenerator object for the body data.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public  InfoBodyBase(XMLGenerator header, XMLGenerator body) throws ServletException
    {
       super(header, body);
    }

    /**
     * Constructor.
     *
     * @param  headerData  XMLGenerator object for the header data.
     * @param  bodyData    XMLGenerator object for the body data.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public  InfoBodyBase(Document header, Document body) throws ServletException
    {
       super(header, body);
    }

     /**
     * Sets the service type for this component.
     */
    public void setServiceType(String serviceType) throws ServletException
    {
       setHeaderValue(ServletConstants.SERVICE_TYPE, serviceType);
       this.serviceType = serviceType;
    }
    /**
     * returns the service type for this component.
     */
    public String getServiceType()
    {
        return serviceType;
    }


    /**
     * Copies this bean.
     */
    public NFBean getFinalCopy() throws MessageException
    {
       InfoBodyBase bean = (InfoBodyBase) super.getFinalCopy();
       bean.serviceType = this.serviceType;

       if (actionSet != null) {
           bean.actionSet = new HashSet();
           bean.actionSet.addAll(actionSet);
       }


       return bean;
    }


    /**
     * Gets the boid header field
     */
    public String getBOID()
    {
        return getHeaderValue(ManagerServletConstants.BOID_FIELD);

    }



    /**
     * Gets the meta data name header field
     */
    public String getMetaDataName()
    {
        return getHeaderValue(ManagerServletConstants.META_DATA_NAME);

    }



   /**
    * Composes the header and body to form the info node and request of a service component.
    *
    * NOTE: Any transforms that need to be done should be performed
    * BEFORE this method is called.
    *
    * This method makes a copy of this bean's data.
    *
    * @return A service component as one xml document without id attributes on each request node.
    * @exception MessageException if the xml can not be created.
    */
   public Document compose()  throws MessageException
   {
      return compose(false);
   }


    /**
    * Composes the header and body to form the info node and request of a service component.
    *
    * NOTE: Any transforms that need to be done should be performed
    * BEFORE this method is called.
    *
    * This method makes a copy of this bean's data.
    *
    * @return A service component as one xml document with id attributes on each request node.
    * @exception MessageException if the xml can not be created.
    */
   public Document composeWithIds()  throws MessageException
   {
      return compose(true);
   }



   private Document compose(boolean keepIds)  throws MessageException
   {

      XMLPlainGenerator sc = new XMLPlainGenerator(BODY_NODE);

      String serviceType = getServiceType();


      // append any bundle header nodes to the root info node
      Node rootInfo = sc.create(serviceType +"." + INFO_NODE);

      log.debug("ServiceComponentBean:Creating service component[" + serviceType +"] info node");


      XMLGenerator header = (XMLGenerator) this.getHeaderDataSource();

      sc.copyChildren(rootInfo, header.getDocument().getDocumentElement());


      XMLGenerator body = (XMLGenerator) this.getBodyDataSource();


      // add the request data for this service component
      // if keepids is true copy all nodes with the id attributes
      // otherwise copy all nodes with out id attributes.
      //
      if ( keepIds)
         sc.copy(sc.getDocument().getDocumentElement().getFirstChild(),body.getDocument().getDocumentElement());
      else
        ServletUtils.copyNoIds(sc.getDocument().getDocumentElement().getFirstChild(), body.getDocument().getDocumentElement());


      return sc.getDocument();

   }

   /**
    * Decomposes a service component.
    * Any data in the info node gets placed in the header of this bean and the request
    * gets placed into the body of this bean.
    *
    * NOTE: Any transforms that need to be done on the body data can be done
    * after this method is called.
    *
    * @param sc - The XMLGenerator holding the service component xml.
    * @param scNode - The service component node.
    *
    * @exception MessageException if the xml can not be created.
    * @exception ServletException If the inner beans could not be created.
    */
   public void decompose(XMLGenerator sc, Node scNode) throws MessageException, ServletException
   {

      Node hFields[] = null;

      // set the node of this service component as the the service type for this component

      String stype = scNode.getNodeName();



      XMLGenerator header = (XMLGenerator) getHeaderDataSource();


      log.debug("decompose(): Decomposing node [" + scNode.getNodeName() + "] ...");




      Node[] children = sc.getChildren ( scNode ) ;

      for ( int i=0; i<children.length; i++ )
      {
          String nodeName = children[i].getNodeName();

          if (nodeName.equals(INFO_NODE)) //copy header
          {
              if( sc.exists(children[i], ACTIONS_ROOT)) {
                  Node actionRoot = sc.getNode(children[i], ACTIONS_ROOT);
                  Node [] actions = sc.getChildren(actionRoot, ACTION_NODE);
                  actionSet = new HashSet();

                  for(int j=0; j < actions.length; j++) {
                      actionSet.add(header.getNodeValue(actions[j]));
                  }

                  if (log.isDebugEnabled())
                      log.debug("Added the following actions [" + actionSet +"]");

                  sc.remove(children[i], ACTIONS_ROOT);
              }

              header.copyChildren(header.getDocument().getDocumentElement(), children[i] );
          }
          else  // copy into body
          {


              //log.debug("ServiceComponent: Obtaining service component request data node : " + nodeName );

              //create an new document with the same root node as the source
              Document doc = XMLLibraryPortabilityLayer.getNewDocument( nodeName , null);

              XMLGenerator body = new XMLPlainGenerator(doc);

              body.copyChildren(body.getDocument().getDocumentElement(), children[i]);

              setBodyDataSource(body);
          }
      }

      // set service type after copying nodes incase it got wiped out
      setServiceType(stype);


   }


   /**
    * Decomposes a service component.
    * Any data in the info node gets placed in the header of this bean and the request
    * gets placed into the body of this bean.
    *
    * NOTE: Any transforms that need to be done on the body data can be done
    * after this method is called.
    *
    * @param sc - The XMLPlainGenerator holding the service component xml.
    *
    * @exception MessageException if the xml can not be created.
    * @exception ServletException If the inner beans could not be created.
    */
   public void decompose(XMLPlainGenerator sc) throws MessageException, ServletException
   {
      decompose(sc, sc.getDocument().getDocumentElement());
   }

    public Set getAllowableActions()
    {
        return actionSet;
    }

    public void setAllowableActions(Set newActions)
    {
        actionSet = newActions;
    }

    /**
     * Convenient method for displaying the string representation of the header data.
     *
     * @return  Header data string.
     */
    public String describeHeaderData()
    {
        StringBuffer descr = new StringBuffer("\nServiceType : ").append(getServiceType()).append("\n");
        descr.append("Actions: " + actionSet).append("\n");

        descr.append(super.describeHeaderData());


        return descr.toString();

    }

    /**
     * Returns grandparent BundleBeanBag.  Note that this method needs to be redefined
     * by the child class.
     *
     * @return a <code>BundleBeanBag</code> value
     */
    public BundleBeanBag getParentBag()
    {
        log.error("getParentBag(): This method needs to be redefined by this child class.");

        return null;
    }

}
