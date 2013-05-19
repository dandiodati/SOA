/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.message;

import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.tag.util.RuleGenerator;
import com.nightfire.webgui.core.meta.*;

import com.nightfire.framework.util.*;

import javax.servlet.jsp.*;
import javax.servlet.ServletException;


/**
 * Generates a simple blank form based on a MessageContainer meta object.
 * This class can be used to generate a backing for a fields that do not need
 * a form/section heading.
 *
 * It must have a ancestor of type MessageTag.
 * Used with an inner body tag.
 */
public class SimpleFormTag extends MessageContainerBaseTag
{

    /**
    * This custom field indicates file name of the GUI Rules file,
    * for the section. This is an optional field
    */
   public static final String RULES_FILE_NAME ="rules-file";


    /**
     * Creates html for the start of a simple form.
     * @return {@link javax.servlet.jsp.tagext.EVAL_BODY_INCLUDE} to evaluate the body
     * of this tag.
     */
    public int doStartTag() throws JspException
    {

        super.doStartTag();

        if ( log.isDebugEnabled() )
           log.debug("Creating Simple Form [" + curPart.getID() +"]");


        StringBuffer buf = new StringBuffer();

        // Rules
        String rulesFileName = ((Section) curPart).getCustomValue(RULES_FILE_NAME);
        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log( Debug.MSG_STATUS, "SimpleFormTag: Name of rules file: " + rulesFileName);
        }

        String rulesJS = "";

        if (StringUtils.hasValue (rulesFileName))
        {
            String rulesResourceName = rulesFileName + ".xml";
            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            {
                Debug.log( Debug.MSG_STATUS, "SimpleFormTag: Name of rulesResource: " + rulesResourceName);
            }


            try
            {
                if (StringUtils.hasValue (rulesResourceName))
                {
                    RuleGenerator ruleGen = new RuleGenerator ();
                    rulesJS = ruleGen.createRulesScript ( pageContext, rulesResourceName );
                }
            }
            catch (ServletException e)
            {
                log.error(e);
            }
            catch (Exception e)
            {
                log.error(e);
            }
        }

        if ( ! StringUtils.hasValue (rulesJS))
        {
            rulesJS = "<Script language=\"JavaScript\"> function callAllRules(obj){};</Script>";
        }

        buf.append(rulesJS);

        try
        {
           buf.append("<table border='0' cellpadding='0' cellspacing='0' width=\"100%\"");
           buf.append(" class='" + TagConstants.CLASS_SIMPLEFORM_TABLE + "'>").append(NL);
           buf.append("<tr><td width=\"100%\">");

           pageContext.getOut().write(buf.toString());
        }
        catch (Exception ex)
        {
            String err = "SimpleFormTag Failed to write start of tag: " + ex.getMessage();
            log.error(err);
            log.error("",ex);
            throw new JspException(err);
        }
        return EVAL_BODY_INCLUDE;
    }

    /**
     * Called when body evaluation is complete  \
     * Finishes building the rest of the html for this form.
     */
    public int doAfterBody() throws JspException
    {
         if ( log.isDebugEnabled() )
           log.debug("doAfterBody Finishing html.");

        try
        {
           pageContext.getOut().write("</td></tr></Table>");

        }
        catch (Exception ex)
        {
            String err = "SimpleFormTag Failed to write end of tag: " + ex.getMessage();
            log.error(err);
            log.error("",ex);
            throw new JspException(err);
        }

        return SKIP_BODY;
    }
}
