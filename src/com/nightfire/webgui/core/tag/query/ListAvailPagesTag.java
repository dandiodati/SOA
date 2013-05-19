/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag.query;

import com.nightfire.webgui.core.tag.*;

import  java.util.*;

import  javax.servlet.*;
import  javax.servlet.jsp.*;
import  javax.servlet.jsp.tagext.*;

import  com.nightfire.framework.util.*;



/**
 * This tag is responsible for building a list of available pages that can be currently selected.
 * Based on the selected page of data, it returns a controlled subset of all available pages.
 */

public class ListAvailPagesTag extends VariableTagBase
{
    /**
     * Total Pages available in a page list
     */
    private int totalPages = 0;

    /**
     * Number of Pages currently available in memory
     */
    private int inMemPages = 0;

    /**
     * The selected page in the page list.
     */
    private int currentPage = -1;

    /**
     * The max number of pages to be displayed at any time.
     */
    private int maxDisplayed = -1;

    private String varTotalAvail;

    /**
     * Sets the name of the variable which holds the total number of pages.
     * @param varTotalPages Name of the variable.
     */
    public void setVarTotalAvail(String varTotalPages)
    {
       this.varTotalAvail = varTotalPages;
    }

    /**
     * Sets the number of in memory pages that will be available immediately to the user.
     * @param memPages Number of in memory pages
     */
    public void setInMemPages(String memPages) throws JspException
    {
       String temp = (String) TagUtils.getDynamicValue("inMemPages",memPages, String.class,this,pageContext);
       inMemPages = Integer.parseInt(temp);
    }

    /**
     * Sets the total number of available pages that can be displayed or a ? character if
     * the total number is not known at this time.
     * @param total total number of pages
     */
    public void setTotalPages(String total) throws JspException
    {
       String temp = (String) TagUtils.getDynamicValue("totalPages",total, String.class,this,pageContext);
       totalPages = Integer.parseInt(temp);
    }

    /**
     * Sets the current page that was selected.
     * @param current The current page.
     */
    public void setCurrentPage(String current) throws JspException
    {
       String temp = (String) TagUtils.getDynamicValue("currentPage",current, String.class,this,pageContext);
       currentPage = Integer.parseInt(temp);
    }


    /**
     * Sets the max number of pages that should be returned at a time.
     * Even numbers are rounded up to the next odd number.
     * @param displayCount Number of pages displayed.
     */
    public void setMaxDisplayed(String displayCount) throws JspException
    {
       String temp = (String) TagUtils.getDynamicValue("maxDisplayed",displayCount, String.class,this,pageContext);
       maxDisplayed = Integer.parseInt(temp);
    }



    /**
     * Creates a list of pages to be displayed.
     * The pages will be added as a list of strings to the output variable
     * defined by var. If var is not defined then the list of pages are written
     * to the html page as a comma separated String.
     *
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     *
     * @return  SKIP_BODY if there is no exception.
     */
    public int doStartTag() throws JspException
    {
      super.doStartTag();
      

        List pages = new LinkedList();


        try
        {

           // if the total pages is unknown at this time use the value of ? for display purposes
           // otherwise  set the total number of pages
           if ( totalPages == -1 ) {
              totalPages = inMemPages;
              VariableSupportUtil.setVarObj(varTotalAvail, inMemPages +" + ?", scope, pageContext);
           } else {
              VariableSupportUtil.setVarObj(varTotalAvail, String.valueOf(totalPages), scope, pageContext);
           }

           int startIndex, endIndex;


           // figure out the mid point
           int splitDisplayed = (int)Math.floor( ((double)maxDisplayed/2.0) );

           //if the total displayed is odd than the splitDisplayed will be rounded down, so
           // place results back in maxDisplayed. Even numbers will end up
           // have behave like maxDisplayed + 1;
           maxDisplayed = splitDisplayed * 2;

           // if there are less pages than the total to display
           // just display all of them
           if ( totalPages > -1 && totalPages <= maxDisplayed) {
              startIndex = 0;
              endIndex = totalPages - 1;
           } else {
              // try to center the current page and
              // have half of maxDisplayed pages before and after.
              startIndex = currentPage - splitDisplayed;

              if ( startIndex < 0 ) {
                 startIndex = 0;
              }

              endIndex = startIndex + maxDisplayed;

              if (endIndex >= totalPages)
                 endIndex = totalPages - 1;

           }
           // add the pages to a list
           String item;
           for (int i = startIndex; i <= endIndex; i++ ) {
              item = String.valueOf(i);
              pages.add(item);
              if (log.isDebugEnabled() )
                 log.debug("Adding page [" + item + "]");
           }

           // if there is an output variable (var) defined place the pages there
           // otherwise write a comma separated list to out.
           if (!varExists()  )
              pageContext.getOut().print(printList(pages));
           else
              setVarAttribute(pages);

           return SKIP_BODY;

        }
        catch (Exception e)
        {
            String errorMessage = "ERROR: listAvailPagesTag.doAfterBody(): Failed to create page list.\n" + e.getMessage();

            log.error(errorMessage);
            log.error("",e);
            throw new JspException(errorMessage);
        }
    }


    /**
     * returns a list of pages as a comma separated string.
     */
    private String printList(List list)
    {
       Iterator iter = list.iterator();
       StringBuffer str = new StringBuffer();

       while (iter.hasNext() ) {
          str.append((String) iter.next());

          if ( iter.hasNext() )
             str.append(", ");

       }
       return str.toString();
    }



    public void release()
    {
        super.release();

        totalPages = 0;
        inMemPages = 0;
        maxDisplayed = -1;
        currentPage  = -1;
        varTotalAvail = null;

    }
}
