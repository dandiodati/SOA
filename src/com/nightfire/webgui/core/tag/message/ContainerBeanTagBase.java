/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.message;

import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.beans.NFBean;
import com.nightfire.webgui.core.tag.*;




/**
 * Container tag for BeanTagBase tags. This class handles any created beans, by inner classes.
 * It then sets a new container bean into the message cache.
 */
public abstract class ContainerBeanTagBase extends BeanTagBase
{

    
    /**
     * This method will be called via a inner BeanTagBase tag after creating a bean.
     *
     *
     * @param beanName a <code>String</code> value
     * @param bean a <code>NFBean</code> value
     */
    public abstract void addBean(String beanName, NFBean bean);

}


