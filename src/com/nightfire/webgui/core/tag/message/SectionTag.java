/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.message;

import  com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.tag.util.RuleGenerator;

import  com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.beans.XMLBean;
import com.nightfire.webgui.core.ServletConstants;

import com.nightfire.framework.util.*;


import javax.servlet.jsp.*;
import javax.servlet.ServletException;

/**
 * The Section tag represents a section in an XML message.
 * It must have a ancestor of type MessageTag.
 */
public class SectionTag extends FormSectionBaseTag
{


    /**
     * Creates html for the start of a Section.
     * @return {@link javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE} to evaluate the body
     * of this tag.
     */
    public int doStartTag() throws JspException
    {
        super.doStartTag();

        if ( log.isDebugEnabled() )
           log.debug("Creating Section [" + curPart.getID() +"]");

        StringBuffer buf = new StringBuffer();

        String rulesFileName = ((Section) curPart).getCustomValue(RULES_FILE_NAME);
        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log( Debug.MSG_STATUS, "SectionTag: Name of rules file: " + rulesFileName);
        }


        String rulesJS = "";

        if (StringUtils.hasValue (rulesFileName))
        {
            XMLBean requestBean = (XMLBean) pageContext.findAttribute(ServletConstants.REQUEST_BEAN);
            String  metaResourceName = requestBean.getHeaderValue("metaResource");
            String rulesResourceName = metaResourceName.substring(0, metaResourceName.lastIndexOf('/')+1) + rulesFileName + ".xml";
            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            {
                Debug.log( Debug.MSG_STATUS, "SectionTag: Name of rulesResource: " + rulesResourceName);
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

            buf.append("<Table width='100%' border='0' cellpadding='0' cellspacing='0'")

               .append(" class='" + TagConstants.CLASS_SECTION_TABLE + "'>").append(NL)
               .append("<tr>").append(NL)
               .append("<td>")
               .append("<Table border='0' cellpadding='0' cellspacing='0' width=\"100%\"><tr>").append(NL)
               .append("<td class=\"" + TagConstants.CLASS_SECTION_HEADING +"\">").append(NL)
               .append(curPart.getFullName())
               .append("</td>").append(NL);

          buf.append("<td align='right'><Table border='0' cellpadding='0' cellspacing='0'><tr>").append(NL);


            if (curPart.getRepeatable() && !isReadOnly() )
                createAddDelete(buf, "Section", TagConstants.IMG_ADD_SECTION, TagConstants.IMG_DEL_SECTION);

            // if the section is repeatable create a goto box
            if ( curPart.getRepeatable() )
               createGotoMenu(buf,TagConstants.CLASS_GOTO_SECTION,"Section");
            buf.append("</tr></table>").append(NL);
            
            buf.append("</tr></Table></td></tr>");


            buf.append("<tr><td width=\"100%\">").append(NL);


            pageContext.getOut().write(buf.toString());

        }
        catch (Exception ex)
        {
            String err = "SectionTag Failed to write start of Section tag: " + ex.getMessage();
            log.error(err);
            log.error("",ex);
            throw new JspException(err);
        }
        return EVAL_BODY_INCLUDE;
    }

   /**
     * Called when body evaluation is complete  
     * Finishes building the rest of the html for this Section.
     */
    public int doAfterBody() throws JspException
    {

         if ( log.isDebugEnabled() )
           log.debug("doAfterBody Finished html.");

        try
        {
           StringBuffer buf = new StringBuffer();
           //close off body
           buf.append("</td></tr>").append(NL);

           buf.append("</Table>").append(NL);
           pageContext.getOut().write(buf.toString() );

        }
        catch (Exception ex)
        {
            String err = "SectionTag Failed to write end of Section tag: " + ex.getMessage();
            log.error(err);
            log.error("",ex);
            throw new JspException(err);
        }

        return SKIP_BODY;
    }
}
