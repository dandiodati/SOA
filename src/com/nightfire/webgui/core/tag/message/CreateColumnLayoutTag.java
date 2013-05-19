/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag.message;

import com.nightfire.webgui.core.tag.message.support.*;
import com.nightfire.webgui.core.tag.*;

import java.util.*;

import com.nightfire.framework.util.*;

/**
 * Creates a ColumLayout class that can layout the
 * html body in columns.
 */
public class CreateColumnLayoutTag extends VariableTagBase
{

    /**
     * The number of columns
     */
    private int cols = 1;

    // number of sub sections
    private int subSecCols = -1;

    private static final String NL = TagConstants.NL;

    private ColumnLayout layout = null;

    private int percentOver = 130;

    /**
     * Set the number of columns
     */
    public void setCols(String cols)
    {
        this.cols = Integer.parseInt(cols);

    }

    /**
     * Set the number of columns in a sub section
     */
    public void setSubSectionCols(String cols)
    {
        subSecCols = Integer.parseInt(cols);
    }

    /**
     *
     * Set the percentage that lines in a column can be over the average number
     * of lines per column. The default is 140. So if there is a field group at
     * the end of the column that pushes the column over 40 percent then that group is
     * moved to the next column. This provides a basic leveling of the columns
     * NOTE: Takes a positive integer greater than 100.
     * If a number less than 100 is given then 100 is added to it.
     *
     * @param precent The percentage that a column can go over the average size for the column.
     */
    public void setColAdjustment(String percent)
    {
        percentOver = Integer.parseInt(percent);

        if (percentOver < 100 )
           percentOver = 100 + percentOver;
           
    }


    /**
     * creates a HtmlElementLayout class that generates a Column style layout.
     * of fields.
     * Sets this class at the provided var location.
     */
    public int doStartTag()
    {
      setup();
      

       this.setVarAttribute(this);
       
       if (subSecCols < 1 )
          subSecCols = cols;

       // if this tag is getting pooled then the column layout class can be
       // reused

       // if the layout is null then create a new one
       // if it is not null then check if the cols and subsecCols match
       // if matches then return the current layout otherwise create a new layout
       if ( layout != null)  {
          if ( layout.getCols() != cols  || layout.getSubSecCols() != subSecCols) {
             layout = new ColumnLayout(cols, subSecCols);
             if (log.isDebugEnabled() )
               log.debug("Creating a new column layout");
          } else if (log.isDebugEnabled() )
             log.debug("Using current column layout.");
       } else {
         layout = new ColumnLayout(cols, subSecCols);
         if (log.isDebugEnabled() )
               log.debug("Creating a new column layout");
       }

       this.setVarAttribute(layout);


       return SKIP_BODY;
    }




    /**
     * Free resources after the tag evaluation is complete
     */
    public void release()
    {
       super.release();
       cols = 1;
       subSecCols = -1;

    }

    // this class is thread safe
    private class ColumnLayout implements HtmlElementLayout
    {
        private int cols = 1;

       // number of sub sections
       private int subSecCols = -1;

       public ColumnLayout(int cols, int subSecCols) {
          this.cols = cols;
          this.subSecCols = subSecCols;
       }


       public int getCols()
       {
          return cols;
       }

       public int getSubSecCols()
       {
          return subSecCols;
       }

       /**
        * Creates a column layout when called by the body tag.
        */
       public String doLayout(HtmlContainer container)
       {
           //if (log.isDebugEnabled() )
           //   log.debug("ColumnLayout: Created HtmlContainer : " + container.describe() );
   


           StringBuffer buf = new StringBuffer();
           List children = container.getChildElements();
           Iterator iter = children.iterator();

           buf.append("<table cellpadding=\"0\" cellspacing=\"0\" border= \"0\" class=\"" + TagConstants.CLASS_FIELD_TABLE + "\" width=\"100%\">").append(NL);

           while (iter.hasNext())
           {
               HtmlContainer con = (HtmlContainer)iter.next();


               buf.append(NL).append("<tr>").append(NL);
               // determine the number of lines to display per column

               if (!con.isSpanning()) {
                  createCols(buf, con, cols);
               } else if ( con instanceof HtmlSubSecContainer ) {
                 createSubSection(buf, (HtmlSubSecContainer)con, subSecCols);
               } else if (con.isSpanning()) {
                   // if the container is spanning just add it in.
                   //buf.append("<td colspan=\"").append(cols).append("\">").append(con.getHTML()).append("</td>").append(NL);
                   buf.append("<td valign=\"top\"")
                      .append(" width=\"100%\" colspan=\"").append(cols).append("\">").append(NL)
   
                       .append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" class=\"")
                       .append(TagConstants.CLASS_COLUMN_TABLE + "\">").append(NL)

                       .append("<tr><td width=\"100%\">").append(con.getHTML()).append("</td></tr>").append(NL)
                       .append("</table></td>");
               }

               buf.append("</tr>").append(NL).append(NL);
           }

            buf.append("</Table>").append(NL);

            return buf.toString();
       }


       /**
        * creates a sub section
        */
       private void createSubSection(StringBuffer buf, HtmlSubSecContainer con, int cols)
       {
          Iterator subSections = con.getChildElements().iterator();

          buf.append("<td valign=\"top\"")
             .append(" width=\"100%\" colspan=\"").append(cols).append("\">").append(NL)
             .append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" class=\"").append(TagConstants.CLASS_SUBSEC_TABLE).append("\">").append(NL)
             .append(con.getHtmlHeader());
           int count = 0;



           while (subSections.hasNext()) {
   
              // there may be elements that are added as spacers
              HtmlElement elem = (HtmlElement) subSections.next();
              if (elem.isSkipped() )
                 continue;
   
              buf.append("<tr><td width=\"100%\">").append(NL);
              buf.append("<table cellpadding=\"0\" cellspacing=\"0\" class=\"").append(TagConstants.CLASS_SUBSEC).append("\">").append(NL);
   

              HtmlContainer subSec = (HtmlContainer) elem;

              buf.append("<tr>").append(subSec.getHtmlHeader()).append("</tr>").append(NL)
                 .append("<tr>").append(NL);
   
              if ( log.isDebugEnabled() )
                 buf.append(NL).append("<!-- Creating sub section [" + count++ + "] -->").append(NL);

              createCols(buf, subSec, cols);

              buf.append("</tr>").append(NL);
              buf.append("<tr>").append(subSec.getHtmlFooter()).append("</tr>").append(NL);
              buf.append("</table>");

              buf.append("</td></tr>").append(NL);
           }


           buf.append(con.getHtmlFooter());
           buf.append(NL).append("</Table>").append(NL);
           buf.append(NL).append("</td>").append(NL);

       }



       /**
        * handles the creation of cols within a HtmlContainer.
        */
        private void createCols(StringBuffer buf, HtmlContainer con, int cols)
        {

          Performance.startBenchmarkLog("CreateColumnLayout: Creating cols");
          // get all first level children which may be fields, or fieldgroups
          List colChildren = con.getChildElements();

          // indicates that this element has been reordered and should be skipped
          //
          HashSet reorderedElement = new HashSet();


          double dWidth = 1.0 / ((double)cols) * 100.0;
          int width = (int)dWidth;

          int totalLines = con.getLineCount();

          //Check if column break property is  configured for this section.
          boolean hasColumnBreak=con.hasColumnBreak();
          int linesPerCol = (int) Math.ceil((double)totalLines / (double)cols);

          if (log.isDebugEnabled() )
            log.debug("The line count is " + totalLines);

          if (log.isDebugEnabled() )
            log.debug("The lines per column is " + linesPerCol);

                   // represents the number of lines
                   int curLineCount = 0;

                   // represents the index of the current child
                   int childIndex = 0;

                   // create each column
                   for (int i = 0; i < cols; i++)
                   {
                       boolean insertColumnBreak=false;
                       // inner table cell for column
                       if ( log.isDebugEnabled() )
                          buf.append(NL).append("<!-- column " + i + " -->").append(NL);

                       buf.append("<td valign=\"top\"");
                       //if this is a middle column add the column line separator
                       if ((i != 0) && (curLineCount < totalLines))
                           buf.append(" class=\"" + TagConstants.CLASS_BORDER_CELL + "\"");

                       buf.append(" width=\"")
                          .append(width)
                          .append("%\"><table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" class=\"")
                          .append(TagConstants.CLASS_COLUMN_TABLE + "\">").append(NL);

                       // find the max number of lines to add in this column
                       int maxPerCol = curLineCount + linesPerCol;



                       // generate each html element
                       // while there are more children elements
                       // loop over the fields in this column while
                       // they can fit.
                       // If this is the last column and the current column is already too large
                       // keep adding any left over children so that fields are not lost.

                       // It is possible
                       // for a field group to push past the number of lines in
                       // the last column, so we continue adding all elements in the last
                       // column even if we push past the max number of lines.
                       // If the last column is too large then field groups in the meta file will
                       // have to be reordered.
                       // 
                       while(childIndex < colChildren.size() && (!insertColumnBreak  || i + 1 == cols) ) {
                           HtmlElement elem = (HtmlElement) colChildren.get(childIndex);

                           int elemLineCount = elem.getLineCount();

                           // In some cases field groups end getting to large
                           // so we push a group over to the next column if 
                           // the following conditions are met:
                           // 1. No columnbreak property is configured
                           // 2. This next element is not the first child.
                           // 3. This next elemen pushes the line count over the
                           // the maximum lines per column more that a specified 
                           // percentage.
                           // 4. This is not the last column.
                           // If this is met then the field/field group is moved
                           // into the next column.
                           if (!hasColumnBreak && childIndex != 0 && i + 1 < cols && curLineCount + elemLineCount > maxPerCol ) {
                              int amountOver = (int)((double)(curLineCount + elemLineCount) / (double)maxPerCol * 100.0);
                               log.debug("Current element index [" + childIndex +"], curLineCount [" + curLineCount +"] makes the column " + amountOver + "% over the max per cols.");
                              // if this last column field is over the percent allowed and this
                              // is not the last column then break and it will
                              // get added in the next column.
                              if ( amountOver > percentOver )
                                 break;
                           }

                           if ( log.isDebugEnabled() )
                              buf.append(NL).append("<!-- Creating field, index [" + childIndex+ "], curLineCount [" + curLineCount +"], maxPerCol [" + maxPerCol + "] -->").append(NL);
   
                           buf.append("<tr><td>").append(NL);
                           if (!elem.isSkipped())
                               buf.append(elem.getHTML());

                           curLineCount += elemLineCount;
                           childIndex++;
                           // if columnbreak property is configured, apply column breaks after that field, else apply column breaks when
                           // no. of lines per column exceeds.
                           if(hasColumnBreak)
                           {
                               insertColumnBreak=elem.hasColumnBreak();
                           }
                           else
                           {
                               insertColumnBreak=curLineCount >= maxPerCol;
                           }
                           buf.append("</td></tr>").append(NL);
                       }

                
                       buf.append("</table></td>");

                       if ( log.isDebugEnabled() )
                          buf.append(NL).append("<!-- End column " + i + " -->").append(NL);

                   }

                    Performance.finishBenchmarkLog("CreateColumnLayout: Creating cols");

        }

    }
}
