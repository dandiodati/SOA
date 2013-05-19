/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.message;


import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import org.w3c.dom.*;

import com.nightfire.framework.constants.*;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.util.*;
import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.tag.message.support.*;
import com.nightfire.webgui.core.xml.*;

/**
 * Tags used within a ServiceBeanTagBase class and modify a service related order bean.
 * Child classes have access to the parentTag object to obtain service information.
 */
public abstract class ModifyServiceBeanTagBase extends VariableTagBase
{

    protected ServiceBeanTagBase parentTag = null;
    
    public int doStartTag() throws JspException
    {
        super.doStartTag();


        parentTag = (ServiceBeanTagBase) findAncestorWithClass(this, ServiceBeanTagBase.class);
        
        if (parentTag == null) {
            String err ="This tag [" + StringUtils.getClassName(this) +"] must be using within a ServiceBeanTagBase class";
            log.error(err);
            throw new JspTagException(err);
        }
    
        return SKIP_BODY;
            
    }

}       
