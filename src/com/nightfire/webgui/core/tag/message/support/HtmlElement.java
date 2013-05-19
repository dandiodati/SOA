/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag.message.support;

import com.nightfire.framework.util.*;
import java.io.*;

/**
 * Represents a single html component on a webpage.
 * Such as a field (text input, select, etc), label, heading, etc.
 * Each HtmlElement should be a complete snipet of html code that
 * can exist as is, and should not need joining with another HtmlElement.
 * In most cases the html element should return a
 * HTML wrapped within and HTML table: <TABLE>...</TABLE>
 */
public class HtmlElement
{


    /**
     * used to create as set of indents for when describe is called.
     */
   protected static final String indents[] = {
   "#",
   "##",
   "###",
   "####",
   "#####",
   "######",
   "#######",
   "########",
   "#########",
   "##########" };


   /**
    * A StringBuffer which hold html content.
    */
   protected StringBuffer html;

   protected StringBuffer jscript;

   private int lineCount = 1;
   private boolean skip;
   private boolean spanning = false;

   private boolean columnBreak = false;

   private boolean fixedLayout;

   private HtmlContainer parent = null;
   

   /**
    * Creates a HtmlElement
    * Line Count defaults to one.
    */
   public HtmlElement()
   {
      html = new StringBuffer();
      jscript = new StringBuffer();
   }

   /**
    * creates a HtmlElement with the specified values.
    * @param lineCount The total number of lines that this component
    * takes up.
    * @param skip true indicates that this element should be
    * skipped during layout. Useful for creating padding to the linecount.
    * @param spanning indicates if this element should span across the html page.
    * The layout can make use of this if neccessary.
    */
   public HtmlElement(int lineCount, boolean skip, boolean spanning)
   {
      this();
      this.lineCount = lineCount;
      this.skip = skip;
      this.spanning = spanning;
   }

   /**
    * prepends a html string to the current
    * htmlBuffer. Only use as neccessary since
    * this requires copying the buffer..
    */
   public HtmlElement prepend(String htmlStr)
   {
      StringBuffer temp = new StringBuffer(htmlStr);

      html = temp.append(html.toString() );
      return this;
   }


   /**
    * Sets the parent container class which holds this element.
    * This method is called when an HtmlElement is added to a HtmlContainer.
    * @param par The parent class.
    *
    */
   protected void setParent(HtmlContainer par)
   {
      parent = par;
   }


   /**
    * Returns the parent container which holds this element.
    * @return The parent HtmlContainer, may be null if there is no parent.
    */
   public HtmlContainer getParent()
   {
      return parent;
   }


   /**
    * Appends jscript code to this object.
    * @param code - The javascript code to append.
    * @return This current HtmlElement with the appended jscript.
    *
    */
   public HtmlElement appendJScript(String code)
   {
      this.jscript.append(code);
      return this;
   }

   /**
    * Appends the html code to this object.
    * @param html - The html code to append.
    * @return This current HtmlElement with the append html.
    *
    */
   public HtmlElement append(String html)
   {
      this.html.append(html);
      return this;
   }

   /**
    * Appends the html code to this object.
    * @param html - The html code (char) to append.
    * @return This current HtmlElement with the append html.
    */
   public HtmlElement append(char html)
   {
      this.html.append(html);
      return this;
   }

   /**
    * Appends the html code to this object.
    * @param html - The html code (int) to append.
    * @return This current HtmlElement with the append html.
    */
   public HtmlElement append(int num)
   {
      this.html.append(String.valueOf(num));
      return this;
   }

   /**
    *
    */
   public HtmlElement append(String html, int lineCount)
   {
      this.html.append(html);
      this.lineCount = lineCount;
      return this;
   }


   /**
    * Returns the html code for this object.
    * @return HTML code.
    */
   public String getHTML()
   {
      return html.toString();

   }

   /**
    * Returns the jscript code for this object if any exists
    * @return HTML code.
    */
   public String getJscript()
   {
      return jscript.toString();
   }

   /**
    * Returns the number of lines that this HtmlElement takes up on the screen
    * For example a text box would take i line.
    * @return The number of lines.
    */
   public int getLineCount()
   {
      return lineCount;
   }


   /**
    * Sets the number of lines that this HtmlELement occupies on the screen.
    * @param count The number of lines.
    */
   public void setLineCount(int count)
   {
      lineCount = count;
   }

   /**
    * Sets spanning to true or false.
    * Spanning indicates if this component should span across a hold section, or
    * multiple boundaries. For example a sub section can be spanning, to indicate
    * that it should take up a whole row of the html page.
    * @param b true it this component is spanning otherwise false.
    */
   public void setSpanning(boolean b)
   {
     spanning = b;
   }

   /**
    * Indicates if this object spans across the section.
    * @return true if this object is spanning otherwise false.
    */
   public boolean isSpanning()
   {
      return spanning;
   }

   /**
    * Sets column break property to true or false.
    * @param b true it columnbreak property is configured as true otherwise false.
    */
   public void setColumnBreak(boolean b)
   {
     columnBreak = b;
   }

   /**
    * Indicates if this object has columnBreak property configured as true or false.
    * @return true if this object has column break otherwise false.
    */
   public boolean hasColumnBreak()
   {
      return columnBreak;
   }


   /**
    * Sets this HtmlElement's fixed layout value.
    * Indicates that an HtmlElement( usually a HtmlContainer) handles
    * its only layout so its html should be used as is.
    * @param b true indicates that this HtmlElement has a fixed layout, otherwise it can be
    * laid out.
    */
   public void setFixedLayout(boolean b)
   {
     fixedLayout = b;
   }

   /**
    * Indicates  if this element has a fixed layout and can not be changed.
    * So the only method that should be called is getHtml();
    */
   public boolean isFixedLayout()
   {
      return fixedLayout;
   }

   /**
    * Set skip mode to true or false.
    * @param b true indicates that this component should be skipped.
    */
   public void setSkip(boolean b)
   {
     skip = b;
   }

   /**
    * Indicates if this element should be skipped.
    * @return True Indicates that this HtmlELement is used for padding and therefore should skipped
    * by a HtmlLayout otherwise false.
    */
   public boolean isSkipped()
   {
      return skip;
   }

   /**
    * Returns a string description of the structure of this HtmlElement (HtmlContainer only)
    * and the html contained within.
    *
    * @return A string description.
    */
   public String describe()
   {
      StringBuffer htmlClasses = new StringBuffer("\nHtmlElement Tree:\n");
      StringBuffer htmlCode    = new StringBuffer("\nHtmlElement Generated Html:\n");

      describe(htmlClasses, htmlCode, 0);

      return (new StringBuffer(htmlClasses.toString()).append(htmlCode.toString()).toString() );
   }

   /**
    * Helper method that gets called by the main describe() method.
    * Can be overwritten by sub classses to create specific implementations.
    * @param elemBuf The String buffer to append a description of the element's structure.
    * @param htmlBuf The string buffer to append a description of the element's html code.
    * @param level The level to start at if multiple levels exist within this element(HtmlContainer)
    */
    protected void describe(StringBuffer elemBuf, StringBuffer htmlBuf, int level) {
      // get the current indent
      String indent = getDescrIndent(level);

      String classStr = " Class [" + StringUtils.getClassName(this) + "]\n";

      elemBuf.append(indent).append(classStr);

      htmlBuf.append("\n").append(indent).append(classStr);
      htmlBuf.append(indent).append(" Html [" + html.toString() + "]\n");

   }

   /**
    * Util for describe which builds a String with the correct number of indents.
    * @param level The level that the current element is at.
    * @return String with the correct padding.
    */
   protected String getDescrIndent(int level)
   {
      int indentMaxSize = indents.length -1;
      StringBuffer buffer = new StringBuffer();

      String indent;
      // if there are not enough indent levels
      // then add enough of the max indents until we get the
      // level of indent we need.
      if (level > indentMaxSize) {
         int count = level/ indentMaxSize;
         for (int i = 0; i < count; i++ ) {
            buffer.append(indents[indentMaxSize]);
         }

         int leftOver = level - (count * indentMaxSize);
         buffer.append(indents[leftOver]);
      } else
         buffer.append(indents[level]);

      // get the current indent
      return buffer.toString();
   }



}
