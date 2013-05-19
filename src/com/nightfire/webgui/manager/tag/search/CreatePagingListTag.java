/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.tag.search;


import com.nightfire.webgui.core.tag.*;

import  java.util.*;

import  javax.servlet.*;
import  javax.servlet.jsp.*;
import  javax.servlet.jsp.tagext.*;

import  com.nightfire.framework.util.*;



/**
 * This tag is responsible for building a list of pages to display. Each Object in the list is
 * an instance of a PageInfo Object.
 */

public class CreatePagingListTag extends VariableTagBase
{
    /**
     * Total records to show per page
     */
    private int recsPerPage = 10;
   
    /**
     * Total number of rows/records returned from the query.
     *
     */
    private int totalNumRecs = -1;
    
    /**
     * The selected page in the page list.
     */
    private int currentPage = -1;

    /**
     * The max number of pages to be displayed at any time.
     */
    private int maxDisplayed = 21;
    

    private String varTotalPages;
    private String varFirstPage;
    private String varLastPage;
    private String varPrevPage;
    private String varNextPage;
    

    
    /**
     * Used to hold information about each page.
     * Contains the page index (zero based), the startOffset for the first row in this page, and
     * the endOffset for the last row in this page.
     *
     */
    public class PageInfo 
    {
        private int index;
        private int recStart;
        private int recEnd;
        
        PageInfo(int index, int recStart, int recEnd)
        {
            this.index = index;
            this.recStart = recStart;
            this.recEnd = recEnd;
        }
        
        public int getPageIndex()
        {
            return index;
        }
        

        public int getRecStart()
        {
            return recStart;
        }
        

        public int getRecEnd() 
        {
            return recEnd;
        }
    }
    
    /**
     * Sets the name of the variable which will get set with
     * the total number of pages.
     * @param varTotalPages Name of the variable.
     */
    public void setVarTotalPages(String varTotalPages)
    {
        this.varTotalPages = varTotalPages;
    }

    /**
     * Sets the name of the variable to hold a PageInfo
     * object about the first page.
     * Note this is null when the first page is not available for
     * display.
     * 
     * @param varFirstPage Name of the variable.
     */
    public void setVarFirstPage(String varFirstPage)
    {
        this.varFirstPage = varFirstPage;
    }

    /**
     * Sets the name of the variable to hold a PageInfo
     * object about the last page.
     * Note this is null when the last page is not available for
     * display.
     * 
     * @param varLastPage Name of the variable.
     */
    public void setVarLastPage(String varLastPage)
    {
        this.varLastPage = varLastPage;
    }

    /**
     * Sets the name of the variable to hold a PageInfo
     * object about the previous page.
     * Note this is null when the previous page is not available for
     * display.
     * 
     * @param varPrevPage Name of the variable.
     */
    public void setVarPrevPage(String varPrevPage)
    {
        this.varPrevPage = varPrevPage;
    }

    /**
     * Sets the name of the variable to hold a PageInfo
     * object about the next page.
     * Note this is null when the next page is not available for
     * display.
     * 
     * @param varNextPage Name of the variable.
     */
    public void setVarNextPage(String varNextPage)
    {
        this.varNextPage = varNextPage;
    }


    /**
     * Sets the total number of records in the query results
     * Optional, Defaults to 15
     * @param total total number of pages
     */
    public void setRecsPerPage(String total) throws JspException
    {

       String temp = (String) TagUtils.getDynamicValue("recsPerPage",total, String.class,this,pageContext);
  
       
       if(StringUtils.hasValue(temp))
          recsPerPage = Integer.parseInt(temp);

    }

    /**
     * Sets the total number of records in the query results
     * @param total total number of pages
     */
    public void setTotalRecs(String total) throws JspException
    {
       String temp = (String) TagUtils.getDynamicValue("totalRecs",total, String.class,this,pageContext);
     
       if(StringUtils.hasValue(temp));
          totalNumRecs = Integer.parseInt(temp);
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
     * Sets the max number of pages that should be displayed.
     * Even numbers are rounded up to the next odd number.
     * Optional, Defaults to 20.
     * @param displayCount Number of pages displayed.
     */
    public void setMaxDisplayPages(String displayCount) throws JspException
    {
       String temp = (String) TagUtils.getDynamicValue("maxDisplayed",displayCount, String.class,this,pageContext);
 
    
       if( StringUtils.hasValue(temp) )
           maxDisplayed = Integer.parseInt(temp);

      // if maxDisplayed is even that add one to get a midpoint to shift off of
       if (maxDisplayed % 2 == 0)
           maxDisplayed += 1;
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
      
      if (totalNumRecs < 0) {
          String err = "Error property totalRecs must have a valid value.";
          log.error(err);
          throw new JspTagException(err);
      }
   
        List pages = new LinkedList();
        
        log.debug("Creating page list, total number of recs[" + totalNumRecs
                  +"], records per page [" + recsPerPage +"]");
        

        try
        {
            int totalPages = (int)Math.ceil((double)totalNumRecs/(double)recsPerPage);
            
            if(StringUtils.hasValue(varTotalPages) ) {
                VariableSupportUtil.setVarObj(varTotalPages, new Integer(totalPages), scope, pageContext);  
           
                log.debug("Calculated total number of pages [" + totalPages +"]");
            }
            

           int startIndex, endIndex;
           
           // figure out the mid point
           int splitDisplayed = (int)Math.floor( ((double)maxDisplayed/2.0) );

           //if the total displayed is odd than the splitDisplayed will be rounded down, so
           // place results back in maxDisplayed. Even numbers will end up
           // behaving like maxDisplayed + 1;
           //maxDisplayed = splitDisplayed * 2;

           // if there are less pages than the total to display
           // just display all of them
           if ( totalPages <= maxDisplayed) {
              startIndex = 0;
              endIndex = totalPages - 1;
           } else {
              // try to center the current page and
              // have half of maxDisplayed pages before and after.
              startIndex = currentPage - splitDisplayed;

              if ( startIndex < 0 ) {
                 startIndex = 0;
              }

              endIndex = startIndex + maxDisplayed - 1;

              // if the end index goes past the end
              // then shift the window back
              if (endIndex >= totalPages) {    
                 endIndex = totalPages - 1;
                 startIndex = endIndex - maxDisplayed + 1;
              }
              

           }

           if(log.isDebugEnabled() )
               log.debug("Calulated page indices to display, start index [" + startIndex 
                         + "] and end index [" + endIndex +"]");
         
           // add the pages to a list
    
           for (int i = startIndex; i <= endIndex; i++ ) {
              pages.add(createPageInfo(i));
           }

           if ( varPrevPage != null && currentPage > 0 ) {
                  VariableSupportUtil.setVarObj(varPrevPage, 
                                                createPageInfo(currentPage -1),
                                                scope, pageContext); 
           }

           if ( varNextPage != null && currentPage < totalPages - 1  ) {
                  VariableSupportUtil.setVarObj(varNextPage, 
                                                createPageInfo(currentPage +1),
                                                scope, pageContext); 
           }

           if ( varFirstPage != null && startIndex > 0  ) {
                  VariableSupportUtil.setVarObj(varFirstPage, 
                                                createPageInfo(0),
                                                scope, pageContext); 
           }
           if ( varLastPage != null && endIndex < totalPages - 1  ) {
                  VariableSupportUtil.setVarObj(varLastPage, 
                                                createPageInfo(totalPages - 1),
                                                scope, pageContext); 
           }
           
                                            
           // if there is an output variable (var) defined place the pages there
           // otherwise write a comma separated list to out.
           if (!varExists()  )
              pageContext.getOut().print(printList(pages));
           else
              setVarAttribute(pages);

           if(log.isDebugEnabled() )
               log.debug("Build the following page list [page index: row start offset:row end offset]= " + printList(pages));
           
           return SKIP_BODY;

        }
        catch (Exception e)
        {
            String errorMessage = "ERROR: Failed to create page list.\n" + e.getMessage();

            log.error(errorMessage,e);
            throw new JspException(errorMessage);
        }
    }


    private PageInfo createPageInfo(int index) 
    {
        int startOffset = index * recsPerPage;
        int endOffset = startOffset + recsPerPage -1;

        return ( new PageInfo(index,startOffset, endOffset));
    }
    
 

    /**
     * returns a list of pages as a comma separated string.
     */
    private String printList(List list)
    {
       Iterator iter = list.iterator();
       StringBuffer str = new StringBuffer();

       while (iter.hasNext() ) {
           PageInfo pi = (PageInfo) iter.next();

          str.append("[");
          str.append(pi.index).append(":");
          str.append(pi.recStart).append(":");
          str.append(pi.recEnd);
          str.append("]");

          if ( iter.hasNext() )
             str.append(", ");

       }
       return str.toString();
    }



    public void release()
    {
        super.release();
    
        recsPerPage = 10;
        totalNumRecs = -1;
        currentPage = -1;
        maxDisplayed = 21;
        varTotalPages = null;
        varFirstPage = null;
        varLastPage = null;
        varPrevPage = null;
        varNextPage = null;

    }
}
