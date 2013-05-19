/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.tag.util;

import  javax.servlet.jsp.*;
import  java.util.*;

import  com.nightfire.framework.util.*;
import  com.nightfire.webgui.core.meta.*;
import  com.nightfire.webgui.core.tag.*;
import  com.nightfire.webgui.manager.beans.*;
import  com.nightfire.webgui.manager.svcmeta.*;


/**
 * GetOrderListTag is responsible for building a list of NVPair/NVPairGroup objects
 * representing all existing orders.
 */

public class GetOrderListTag extends VariableTagBase
{
	private BundleBeanBag bean;
	private BundleDef     bundleDef;

    /**
     * Set an attribute "bean".
     *
     * @ param BundleBeanBag Object
     */
    public void setBean(Object beanObj) throws JspException
    {
        setDynAttribute("bean", beanObj, BundleBeanBag.class);
    }

    /**
     * Set an attribute "bundleDef".
     *
     * @ param BundleDef Object
     */
    public void setBundleDef(Object bundleObj) throws JspException
    {
        setDynAttribute("bundleDef", bundleObj, BundleDef.class);
    }


    /**
     * Redefinition of doStartTag() in VariableTagBase.  This method processes the
     * start tag for this instance.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     *
     * @return  SKIP_BODY if there is no exception.
     */
    public int doStartTag() throws JspException
    {
      super.doStartTag();

      bean = (BundleBeanBag) getDynAttribute("bean");
      bundleDef = (BundleDef) getDynAttribute("bundleDef");

      ArrayList orderList = new ArrayList();

      for (Iterator group = bean.getBeanGroups().iterator(); group.hasNext(); ) {
        String svcType = (String) group.next();
        List svcBeans = bean.getBeans(svcType);
        ComponentDef svcDef = bundleDef.getComponent(svcType);
        String svcName = bundleDef.getComponent(svcType).getDisplayName();

        if (svcBeans.size() >= 1) { // create NVPairGroup
          ArrayList nvPairs = new ArrayList();

          for (Iterator svcComps = svcBeans.iterator(); svcComps.hasNext(); ) {
            ServiceComponentBean svcBean = (ServiceComponentBean) svcComps.next();
            String               beanId  = svcBean.getId();
            //Get a position of multiple service components.
            int	begin = beanId.indexOf ('(') + 1;
            int end   = beanId.indexOf (')');
            int idPos = 1;
            try {
				idPos = Integer.parseInt(beanId.substring(begin, end));
				idPos++;
			}
			catch(NumberFormatException e) {
				log.warn("GetOrderListTag: Service Component Id "+ beanId +" doesn't contain a number.");
			}

            NVPair order = new NVPair(svcName + " " + idPos, beanId);

            if (log.isDebugEnabled()) {
              log.debug("Name is " + order.getName());
              log.debug("Value is " + order.getValue());
            }

            nvPairs.add(order);
          }

          NVPairGroup orders = new NVPairGroup(svcType, svcName, nvPairs);
          orderList.add(orders);

        }


      }

      setVarAttribute(orderList);

      return SKIP_BODY;
    }

    /**
     * Redefinition of release() in VariableTagBase.  This method is invoked after
     * doEndTag(), allowing any state maintenance to be performed.
     */
    public void release()
    {
        super.release();

        bean      = null;
        bundleDef = null;
    }
}
