/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.message;

import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.xml.*;

import com.nightfire.webgui.core.*;
import com.nightfire.framework.constants.PlatformConstants;

import java.io.*;
import javax.servlet.jsp.*;

import javax.servlet.jsp.tagext.*;
import javax.servlet.http.*;
import javax.servlet.*;
import java.util.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.util.xml.*;
import com.nightfire.framework.message.common.xml.*;


import org.w3c.dom.*;

import com.nightfire.framework.rules.*;
import com.nightfire.framework.debug.Benchmark;

/**
 * Builds a validation error based on the a business error or invalid input.
 *
 */
public class ValidationErrorTag extends VariableTagBase
{

    /**
     *  The Message meta object to find fields in.
     */
    private Message meta;

    /**
     * A String or XMLGenerator of xml validation errors.
     * XML created by com.nightfire.framework.rules.ErrorCollection
     */
    private Object errors;

    /**
     * A set of mappings of fields from
     * the dtd to the gui xml. It is an xml message
     * conforming to the dtd with id attributes
     * indicating the gui xml path.
     */
    private BizRuleMapper xmlMapping;


    /**
     * The request message that produced this set of biz errors
     */
    private XMLGenerator message;



    private boolean displayUnknown = true;

    String fieldLabelMode;
    


     /**
     * Set a unique id for this tag.
     * @param message The expression which evaluates to a Message object.
     */
    public void setId(String id)
    {
       try {
           super.setId( (String)TagUtils.getDynamicValue("id", id, String.class, this, pageContext) );
       } catch(JspException e) {
          log.error("Failed to set id : " + e.getMessage());
       }
    }


    /**
     * Set the meta data Message object
     * @param message The expression which evaluates to a Message object.
     */
    public void setMeta(Object meta) throws JspException
    {
       this.meta = (Message) TagUtils.getDynamicValue("meta", meta, Message.class, this, pageContext);
    }


    /**
     * Sets the validation error xml message
     * @param errors The Object that holds the validation errors
     */
    public void setErrors(Object errors)  throws JspException
    {
       this.errors = TagUtils.getDynamicValue("errors", errors, Object.class, this, pageContext);

    }

    /**
     * Sets the validation mapping. This a xml the contains mappings from
     * dtd paths to the gui xml.
     * @param error The XMLGenerator that holds the validation mapping
     */
    public void setXmlMapping(Object mapping)  throws JspException
    {
       xmlMapping = (BizRuleMapper) TagUtils.getDynamicValue("xmlMapping",mapping, BizRuleMapper.class,this,pageContext);

    }

     /**
     * Set the message data object for this request.
     * @param message The XMLGenerator  object.
     */
    public void setMessage(Object message)  throws JspException
    {
       this.message = (XMLGenerator) TagUtils.getDynamicValue("message",message, XMLGenerator.class,this,pageContext);
    }





    /**
     * Indicates to display unknown fields that could not be mapped.
     *
     */
     public void setDisplayUnknown(String bool)
     {
        try {
           displayUnknown = StringUtils.getBoolean(bool);
        } catch (FrameworkException e) {
           log.warn("Invalid value for displayUnknown attribute, defaulting to true: " + e.getMessage());
        }
     }


    /**
     *  Generate biz rule error message
     *
     */
    public int doStartTag() throws JspException
    {
      super.doStartTag();
      
      fieldLabelMode = PropUtils.getPropertyValue(props, TagConstants.DISPLAY_FIELD_MODE_PROP, TagConstants.DISPLAYNAME_LABEL);

        StringBuffer html = new StringBuffer();
        try {

           long start = Benchmark.startTiming(log);

           if (xmlMapping != null && log.isDebugDataEnabled() )
              log.debugData("Got field mappings: " + xmlMapping.describe());

           html.append("<Table class=\"").append(TagConstants.CLASS_ERROR_SET).append("\" cellspacing=\"0\" cellpadding=\"0\" width=\"100%\">");

           ErrorCollection collection = null;

           if (errors instanceof XMLGenerator )
              collection = ErrorCollection.toErrorCollection(((XMLGenerator)errors).getOutput());
           else if (errors instanceof String )
              collection = ErrorCollection.toErrorCollection((String) errors);
           else
             throw new JspTagException("ValidationErrorTag: Unknown type for errors attribute:"+ errors.getClass());

           RuleError[] errors = collection.getErrors();


           for(int i =0; i < errors.length; i++ ) {
              String context = errors[i].getContext();
              String message = errors[i].getMessage();
              String id		 = errors[i].getID();
              
              if(StringUtils.hasValue(id)){
                  message = "[" + id + "]: " + message;                  
              }

              // get a list of possible destinations

              List nfPaths =  null;

              if ( xmlMapping != null && StringUtils.hasValue(context))
                 nfPaths = xmlMapping.getMappingPaths(context);

              Field f = null;
              String nfPath = null;
              
              // loop over destintations and return the first meta field that
              // matches
              for( int j = 0; nfPaths!=null && j < nfPaths.size(); j++ ) {
                 nfPath =  (String)nfPaths.get(j);
                 String strippedPath = stripIndices(nfPath);
                 if ( log.isDebugEnabled() )
                    log.debug("Looking for field [" + strippedPath +"]" );
                 f = (Field)meta.getMessagePart(strippedPath);

                 if ( f != null) {
                    if(log.isDebugEnabled())
                       log.debug("Found matching field with path [" + f.getFullXMLPath() +"]" );
                    break;
                 }
              }

              html.append("<TR><TD>");
              modifyErrMessage( f, message, html, nfPath);

              html.append("</TD></TR>");


           }

           html.append("</TR></Table>");

           Benchmark.stopTiming(log, start, "Created validation errors");

        } catch (FrameworkException e) {
            
           String err= "Failed to parse error xml: " + e.getMessage();
           log.error(err,e);
           
           throw new JspTagException(err);
        }


        try
        {
            if (varExists() ) {
              setVarAttribute(html.toString());
           } else {
              pageContext.getOut().println(html.toString());
           }
        }
        catch (Exception ex)
        {
            String err = "Failed to write tag: " + ex.getMessage();
            log.error(err,ex);
            throw new JspException(err);
        }

        return SKIP_BODY;
    }

    /**
     * Modifies the error messag that comes back with each biz rule error.
     * Looks for the field name from the dtd in the message and replaces it
     * with the display name used in the gui.
     * If there is no mapping and the displayUnknown is true, then the original message is returned.
     *
     * @param f The field that is related to this error message.
     *  This could be null if no mapping was found.
     * @param message The error message returned by a biz rule.
     * @param html The buffer holding the html code.
     * @param the full xml nf path with indexes
     *
     */
    private void modifyErrMessage(Field f, String message, StringBuffer html, String nfPath)
    {

       String newMessage = message;

       String fieldLink = new String("#null");

       html.append("<table class=\"").append(TagConstants.CLASS_ERROR_RULE).append("\" cellspacing=\"0\" cellpadding=\"0\" width=\"100%\">");
       String levels="&nbsp;&nbsp;";

       if ( f  != null) {

           String localPath = f.getXMLPath();
           if ( log.isDebugEnabled() )
              log.debug("Looking in error message for string [" + localPath +"]" );


            int loc = -1;
            // if there is other xml nodes in this path just grab the last part (field name).
            // NOTE: The field ids in the gui must equal the dtd field names.
            if ((loc = localPath.lastIndexOf(".")) > -1 )
              localPath = localPath.substring(loc + 1);

            // Displaying the field names in the message as
            // "Field-Full-Name (Abbreviation)"
            newMessage = StringUtils.replaceWord(message, localPath, getFieldName(f));

           LinkedList stack = new LinkedList();

           MessagePart part = f.getParent();


           try {
              fieldLink = createErrorHeadings(f, nfPath, stack);
              if ( log.isDebugEnabled() )
                 log.debug("Built javascript code [" + fieldLink +"]" );

           } catch (MessageException e ) {
              log.warn("Failed to create error headings: " + e.getMessage());
           }

            // add error headings top down to the message
            while (stack != null && stack.size() > 0 ) {
               html.append("<tr><td class=\"").append(TagConstants.CLASS_ERROR_HEADING).append("\">").append(levels);
               //html.append("<A href=\"").append(link).append("\">");
               html.append((String)stack.removeFirst() );
               //html.append("</A>");
               html.append("</td></tr>");
               levels += "&nbsp;&nbsp;";
            }

        }
        // add one more level of indent
        levels += "&nbsp;&nbsp;";
        
        // if parent locations can't be found just add the message
        html.append("<TR><td class=\"").append(TagConstants.CLASS_ERROR_MSG).append("\">").append(levels);

        if (  f != null || displayUnknown) {
           html.append("<A href=\"").append(fieldLink).append("\">");
           html.append(newMessage).append("</A>");
        }

        html.append("</td></tr>");

        html.append("</table>");

    }

    /**
     * Returns String " <Field Name> (Abbrevaition)" or " <Field Name>" where
     * the <Field Name> is full name of the field. If full name is not defined
     * it's display name is used instead.
     * 
     * @param field -
     *            The field that is related to the error message.
     * @return The display text for the field in the rule description
     */
    private String getFieldName(Field field)
    {
        String fieldName = field.getFullName();

        if (!StringUtils.hasValue(fieldName))
            fieldName = field.getDisplayName();

        String abbr = field.getAbbreviation();

        if (StringUtils.hasValue(abbr))
        {
            // append field abbreviation to field name for help.
            if (!fieldName.equalsIgnoreCase(abbr))
            {
                fieldName = new StringBuffer(fieldName).append(" (").append(abbr).append(")").toString();
            }
        }

        return fieldName;
    }


    /**
     * This method creates the error headings and the error link javascript call.
     * @param field The field obtained from the nfpath.
     * @param nfPath The xml path to the field.
     * @param stack A list which will contain each heading for a fiel.
     */
    private String createErrorHeadings(Field field, String nfPath, LinkedList stack)
      throws MessageException
    {

       StringBuffer path = new StringBuffer();

       StringBuffer fieldLink = new StringBuffer();

       int formIndex = 0, secIndex = 0;
       String secId = "";

       PathElement pathe = new PathElement(nfPath);
 

       // build a stack of names that can be displayed
       // and build the javscript function with the indexs of the containers.
       // the format will be gotoErrorField('field xml path', 'section id', section index, form index, other indexes)
       // Note the field xml path will have an index appended to it for repeating sub sections.
       //

       // loop over each parent part and find the xml node
       for ( MessagePart part = field; part != null; part = part.getParent() ) {

          // for each part find the PathElement
          // We only care about finding the index of sections, forms and RepeatingSubSections
          PathElement current = getPathElement(pathe, part);

          //This case should occur only when the parent ( form  or section )
          //is not output as an xml element.
          if (current == null) {
              current = new PathElement();
              current.name="";
              current.idx = -1;
          }    


          if ( part instanceof Form ) {

             if ( current.idx == -1 ) {
                formIndex = 0;
                current.idx = 0;
             } else
                formIndex = current.idx;

             if (((MessageContainer)part).getRepeatable() )
                stack.addFirst(part.getAbbreviation() +" # " + (formIndex + 1) );
             else
                stack.addFirst(part.getAbbreviation() );
          } else if (part instanceof Section ) {
             if ( current.idx == -1 ) {
                secIndex = 0;
                current.idx = 0;
             } else
                secIndex = current.idx;

             secId = part.getID();

             if (((MessageContainer)part).getRepeatable() )
                stack.addFirst(part.getDisplayName() +" # " + (secIndex + 1) );
             else
                stack.addFirst(part.getDisplayName() );

          } else if (part instanceof RepeatingSubSection ) {
             if ( current.idx == -1 )
                current.idx = 0;
             stack.addFirst(part.getDisplayName() +" # " + (current.idx + 1) );
          } else if (part instanceof FieldGroup )  {
             stack.addFirst(part.getDisplayName() );
          }

       }

       // rebuild the xml path so that forms, sections, and repeatingsubsections
       // always have indexes
       for (PathElement e = pathe; e != null; e = e.next ) {
          path.append(e.name);
          if (e.idx > -1 )
            path.append("(" + e.idx + ")");
          if ( e.next != null)
          path.append(PathElement.NAME_DELIMITER);
       }


       fieldLink.append("javascript:gotoErrorField('" + getId() + "','");
       fieldLink.append(path.toString()).append("','").append(secId).append("',");
       fieldLink.append(secIndex).append(",").append(formIndex);
       fieldLink.append(")");


       return fieldLink.toString();
    }

    // find the PathElement which matches this part.
    private PathElement getPathElement(PathElement elem, MessagePart part )
    {       
      
       String partName = part.getXMLPath();
      
       // if there are other nodes in the xml path just get the last one. 
       if (StringUtils.hasValue (partName) && partName.indexOf('.') > -1 )
          partName = partName.substring(partName.lastIndexOf('.') + 1);

       for (PathElement current = elem; current != null; current = current.next ) {
             if (current.name.equals(partName) )
                return current;
       }

       return null;
    }


    /**
     * Strips off any indexes from a nf xml path.
     * This is done to fine meta data fields.
     */
    private String stripIndices(String path)
    {
       int start = -1;

       while ((start = path.indexOf("(")) > -1 ) {
          int end = path.indexOf(")", start +1);
          String prefix = path.substring(0, start);
          String suffix = path.substring(end+1);
          path = prefix + suffix;
       }

       return path;
    }


    public void release()
    {
       super.release();

       errors = null;
       xmlMapping = null;
       message = null;
    }



}
