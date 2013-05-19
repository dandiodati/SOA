package com.nightfire.webgui.gateway.tag.util;

import com.nightfire.webgui.core.tag.VariableTagBase;
import com.nightfire.webgui.core.tag.TagUtils;
import com.nightfire.webgui.core.tag.TagConstants;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.db.SQLUtil;
import com.nightfire.framework.db.DBInterface;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import java.io.IOException;
import java.sql.Connection;
import java.util.Vector;
import java.util.Hashtable;
import java.util.StringTokenizer;

/**
 * Copyright (c) 2004 NeuStar, Inc.  All rights reserved.
 *
 * $Header: $
 */

/**
 *  This tag perform two different actions.
 *  1. This is used in listing all the users present in the domain
 *     customer wise, so that user can assign any template to that particular user.
 *  2. This tag is also able to save the changes made by
 *     any user to assign or unassign any template.
 */

public class TemplateUsersTag extends VariableTagBase
{
    private String assignedUserNames = "";
    private String operation = "";
    private String customerId = "";
    private String templateKey = "";

    /**
     * Sets customer id
     *
     * @param customerId
     * @throws JspException
     */
    public void setCustomerId ( String customerId ) throws JspException
    {
        if ( StringUtils.hasValue(customerId) )
              this.customerId = (String)TagUtils.getDynamicValue("customerId", customerId, String.class, this, pageContext);
    }

    /**
     * Sets Template Key
     *
     * @param templateKey
     * @throws JspException
     */
    public void setTemplateKey ( String templateKey ) throws JspException
    {
        if ( StringUtils.hasValue(templateKey) )
              this.templateKey = (String)TagUtils.getDynamicValue("templateKey", templateKey, String.class, this, pageContext);
    }

    /**
     * Sets operation
     *
     * @param operation
     * @throws JspException
     */
    public void setOperation ( String operation ) throws JspException
    {
        if ( StringUtils.hasValue(operation) )
              this.operation = (String)TagUtils.getDynamicValue("operation", operation, String.class, this, pageContext);
    }

    /**
     * Sets assigned user names
     *
     * @param assignedUserNames
     * @throws JspException
     */
    public void setAssignedUserNames ( String assignedUserNames ) throws JspException
    {
        if ( StringUtils.hasValue(assignedUserNames) )
              this.assignedUserNames = (String)TagUtils.getDynamicValue("assignedUserNames", assignedUserNames, String.class, this, pageContext);
    }

    public int doStartTag() throws JspException
    {
        super.doStartTag();

        StringBuffer html = new StringBuffer ( "" );
        Connection dbConn = null;
        Vector result = null;

        try
        {
            /* Not using the constants for the table and column names here*/
            dbConn = DBInterface.acquireConnection();
            if (log.isDebugEnabled())
                log.debug("TemplateUsersTag.doStartTag: Connection acquired" );

            if (operation.equalsIgnoreCase ( "list" ))
            {

                Vector colNames = new Vector();
                colNames.add ( "Assign" );
                colNames.add ( "Last Name" );
                colNames.add ( "First Name" );

                Vector cols = new Vector();
                cols.addElement("TEMPLATE_KEY");
                cols.addElement("USERNAME");
                cols.addElement("LASTNAME");
                cols.addElement("FIRSTNAME");

                Hashtable where = new Hashtable();

                Vector orderby = new Vector();
                orderby.addElement("LASTNAME");
                orderby.addElement("FIRSTNAME");

                if (log.isDebugEnabled())
                    log.debug("TemplateUsersTag.doStartTag: Retrieving results..." );

                String str = "CLEARINGHOUSEUSER u, TEMPLATE_USERS t where t.USERID (+)=u.USERNAME and t.CUSTOMERID (+)=u.CUSTOMERID and t.TEMPLATE_KEY (+)='"+templateKey+"' and u.CUSTOMERID='"+customerId+"'";
                result = SQLUtil.fetchRows(dbConn, str, cols, where, orderby);

                if (log.isDebugEnabled())
                    log.debug("TemplateUsersTag.doStartTag: Retrieving results...resultsize = " + result.size () );

                if (result != null || result.size() > 0)
                {
                    html.append("<table class=\"" + TagConstants.CLASS_SEARCHRESULTS_TABLE +"\" WIDTH=\"100%\" align=\"center\" CELLPADDING=\"0\" CELLSPACING=\"0\" border=\"0\">");
                    html.append ( prepareHeader (colNames) );
                    html.append ( prepareRows (result) );
                    html.append("</table>");
                    if (log.isDebugEnabled())
                        log.debug("TemplateUsersTag.doStartTag: Total records processed are " + result.size () );
                }
                else
                {
                    if (log.isDebugEnabled())
                        log.debug("TemplateUsersTag.doStartTag: No matching records found.");
                }

                pageContext.getOut().println(html.toString());
            }
            else if (operation.equalsIgnoreCase ( "assign" ))
            {
                Hashtable columnValues = new Hashtable ();
                columnValues.put ("TEMPLATE_KEY", templateKey);
                if (log.isDebugEnabled())
                    log.debug("TemplateUsersTag.doStartTag: Deleting record with TEMPLATE_KEY: ["+templateKey+"]");
                int n = SQLUtil.deleteRows ( dbConn, "TEMPLATE_USERS", columnValues);

                if (assignedUserNames.trim().length() > 0)
                {
                    Vector userNames = parseUserNames (assignedUserNames);
                    final int USERNAMESSIZE = userNames.size ();
                    for ( int elem = 0; elem < USERNAMESSIZE; elem++ )
                    {
                        columnValues = new Hashtable ();
                        columnValues.put ("TEMPLATE_KEY", templateKey);
                        columnValues.put ("CUSTOMERID", customerId);
                        columnValues.put ("USERID", userNames.get ( elem ).toString ());
                        if (log.isDebugEnabled())
                            log.debug("TemplateUsersTag.doStartTag: Inserting record with values: " +
                                "TEMPLATE_KEY: ["+templateKey+"], CustomerId: ["+customerId+"] and UserId: ["+userNames.get ( elem ).toString ()+"]");
                        SQLUtil.insertRow ( dbConn, "TEMPLATE_USERS", columnValues);
                    }
                }
            }
            else
            {
                String errMsg = "Value of operation node is required. "
                    + "\n Usage:"
                    + "\n 1. list: To display the user list"
                    + "\n 2. assign: To assign templates to the user list. Comma separated user names string is mandatory.";

                throw new JspException(errMsg);
            }
        }
        catch (IOException e)
        {
            String errorMessage = "Failed to write the result to the output stream:\n" + e.getMessage();
            log.error("TemplateUsersTag.doStartTag(): " + errorMessage);
            throw new JspException(errorMessage);
        }
        catch (Exception err )
        {
            log.error("TemplateUsersTag.doStartTag: " + err);
            throw new JspTagException (err.getMessage ());
        }
        finally
        {
            /** Here try is required to release the DB Connection Pool Instance */
            try {
                dbConn.commit ();
                DBInterface.releaseConnection(dbConn);
            }
            catch (Exception e) {
                Debug.error("TemplateUsersTag.doStartTag: Error: " + e.toString());
            }
        }
        return SKIP_BODY;
    }

    /**
     * This function parses the string and returns vector
     * containing the user names.
     *
     * @param assignedUserNames
     * @return
     */
    private Vector parseUserNames ( String assignedUserNames )
    {
        Vector userNames = new Vector ();
        StringTokenizer token = new StringTokenizer (assignedUserNames, ",");
        int numTokens = token.countTokens ();

        for ( int elem = 0; elem < numTokens; elem++ )
        {
            userNames.add ( token.nextToken ().trim () );
        }

        return userNames;
    }

    /**
     * This function prepare the HTML for rows
     * to be display on GUI
     *
     * @param result
     * @return
     */
    private StringBuffer prepareRows ( Vector result )
    {
        StringBuffer innerHTML = new StringBuffer ( "" );
        final int RESULTSIZE = result.size ();
        for ( int elem = 0; elem < RESULTSIZE; elem++ )
        {
            Vector tempRecord = (Vector) result.get ( elem );
            int tempRecordSize = tempRecord.size ();

            innerHTML.append("<tr ");
            if (elem % 2 == 1)
               innerHTML.append("class=\"oddRow\" ");

            innerHTML.append("onMouseOver=\"highLightRow(this);\" onMouseOut=\"dehighLightRow(this);\">");

            for ( int colNums = 0; colNums < tempRecordSize; colNums++ )
            {
                /*
                *   We will not display the User Name, hence we are continuing at value '1'
                *   because the column at position '1' is user name.
                */
                if (colNums==1) continue;
                innerHTML.append ( "<td>" );
                String colValue = "";
                if (tempRecord.get ( colNums ) !=null)
                    colValue = tempRecord.get ( colNums ).toString ();
                /*As our first column would contain checkbox always*/
                if (colNums==0)
                {
                    String checkboxStatus = "";
                    if ( StringUtils.hasValue(colValue) )
                        checkboxStatus = "CHECKED";
                    innerHTML.append("<input type=\"checkbox\" name=\""+tempRecord.get ( colNums+1 ).toString ()+"\""+ checkboxStatus + ">");
                }
                else
                {
                    innerHTML.append(tempRecord.get ( colNums ).toString ());
                }
                innerHTML.append ( "</td>" );
            }
            innerHTML.append ( "</tr>" );
        }
        return innerHTML;
    }

    /**
     * This function prepare the HTML for header row
     * to be display on GUI
     *
     * @param colNames
     * @return
     */
    private StringBuffer prepareHeader ( Vector colNames )
    {
        StringBuffer innerHTML = new StringBuffer ( "" );
        final int colNamesSize = colNames.size ();
        for ( int elem = 0; elem < colNamesSize; elem++ )
        {
            innerHTML.append( "<th align=\"left\" class=\"").append(TagConstants.CLASS_SORTABLE).append("\">" );
            innerHTML.append( colNames.get ( elem ) + "</th>" );
        }
        return innerHTML;
    }

    public void release()
    {
       super.release();
    }
}
