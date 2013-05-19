/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.message;

import com.nightfire.webgui.core.tag.*;
import com.nightfire.framework.util.StringUtils;

import javax.servlet.jsp.*;



/**
 * The Section tag represents a Form in an XML message.
 * It must have a ancestor of type MessageTag.
 */
public class FormTag extends FormSectionBaseTag
{


    /**
     * Creates html for the start of a form.
     * @return {@link javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE} to evaluate the body
     * of this tag.
     */
    public int doStartTag() throws JspException
    {

        super.doStartTag();

        if ( log.isDebugEnabled() )
           log.debug("Creating Form [" + curPart.getID() +"]");

        if ( log.isDebugEnabled() )
           log.debug("doStartTag Got message part " + curPart.getID());

        StringBuffer buf = new StringBuffer();

        try
        {
           buf.append("<table border='0' cellpadding='0' cellspacing='0' width=\"100%\"")
              .append(" class='" + TagConstants.CLASS_FORM_TABLE + "'>").append(NL)
              .append("<tr><td>")
              .append("<Table border='0' cellpadding='0' cellspacing='0' width=\"100%\"><tr>");

            // displaying full name only if it is present in the meta file
            if (StringUtils.hasValue(curPart.getFullName()))
            {
               buf.append("<td class='" + TagConstants.CLASS_FORM_HEADING + "'")
                  .append(" align=\"left\" valign=\"top\">")
                  .append(curPart.getFullName())
                  .append("</td>").append(NL);
            }

          buf.append("<td align='right'><Table border='0' cellpadding='0' cellspacing='0'><tr>").append(NL);

 
           if (curPart.getRepeatable() && !isReadOnly())
               createAddDelete(buf, "Form", TagConstants.IMG_ADD_FORM, TagConstants.IMG_DEL_FORM);


           if ( curPart.getRepeatable() )
               createGotoMenu(buf,TagConstants.CLASS_GOTO_FORM,"Form");

           buf.append("</tr></table>").append(NL);

            buf.append("</tr></Table></td></tr>");


            buf.append("<tr><td width=\"100%\">").append(NL);
            

            pageContext.getOut().write(buf.toString());

        }
        catch (Exception ex)
        {
            String err = "FormTag Failed to write start of tag: " + ex.getMessage();
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
           StringBuffer buf = new StringBuffer();

           //close off body
           buf.append("</td></tr>").append(NL);


        
           buf.append("</Table>").append(NL);
           pageContext.getOut().write(buf.toString() );

        }
        catch (Exception ex)
        {
            String err = "FormTag Failed to write end of tag: " + ex.getMessage();
            log.error(err);
            log.error("",ex);
            throw new JspException(err);
        }

        return SKIP_BODY;

    }

    



}
