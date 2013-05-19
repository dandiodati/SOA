/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag.message.support;

import com.nightfire.framework.util.*;
import java.util.*;
import java.io.*;

/**
 * A container which holds HtmlElements. It also tracks the total
 * number of lines in all fragments contained with in this object.
 */
public class HtmlContainer extends HtmlElement
{


   private List elems;

   private StringBuffer htmlFooter;

   /**
    * Creates a Html Container
    */
   public HtmlContainer()
   {
      super();
      elems = new ArrayList();
      htmlFooter = new StringBuffer();
   }

   /**
    * adds html code to the footer of the container.
    * @param html The html code to append.
    * @return Returns the current object.
    */
   public HtmlContainer appendFooter(String html)
   {
      htmlFooter.append(html);

      return this;
   }

   /**
    * adds a HtmlElement to this container.
    * Each element can be evalulated indiviually.
    * @param frag A HtmlElement to add to this container.
    * @return The current HtmlElement that got added to this container.
    */
   public HtmlElement add(HtmlElement frag)
   {

      elems.add(frag);
      frag.setParent(this);
      return frag;

   }

   /**
    * returns the total number of lines for all HtmlElements
    * within this container. This calles getLineCount on each HtmlElement
    * within so it is an expensive operation that should only be called
    * as needed.
    * @return The total number of lines.
    */
   public int getLineCount()
   {


      Iterator childIter = elems.iterator();
      int count = 0;

      while (childIter.hasNext()) {
         HtmlElement elem = (HtmlElement) childIter.next();
         count += elem.getLineCount();
      }

      return count;
      
   }

   /**
    * Returns a list of all HtmlElements within this container.
    * @return a List of HtmlElements.
    */
   public List getChildElements()
   {
      return elems;
   }

   /**
    * Returns the html code code which exists before all HtmlElements.
    * @return The html header code for this container.
    */
   public String getHtmlHeader()
   {
      return html.toString();
   }

   /**
    * Returns the html code code which exists after all HtmlElements.
    * @return The html footer code for this container.
    */
   public String getHtmlFooter()
   {
      return htmlFooter.toString();
   }

   

   /**
    * Combines the container's header html,
    * all children html code(calls getHtml on each HtmlElement within this container)
    * , and the container's footer html.
    *
    * @return  Html code for this container and all its children.
    * Used when the content of the container and its children can be used
    * as is and does not need to be laid out.
    */
   public String getHTML()
   {
      Iterator iter = elems.iterator();

      while (iter.hasNext() ) {
         HtmlElement frag = (HtmlElement) iter.next();
         html.append(frag.getHTML());
      }
      html.append(htmlFooter.toString());
      return html.toString();

   }


   /**
    * Returns String description of the structure of this container
    * and all inner HtmlElements (which may also be HtmlContainers),
    * followed by the html contained with each HtmlElement.
    * The header and footer for this HtmlContainer and any inner HtmlContainers
    * are printed after describing the children.
    * Very useful for trying to figure out out a HtmlElementLayout can interact
    * with an HtmlElement hierarchy.
    * NOTE: The main describe method in the HtmlElement class should be called.
    * @see HtmlElement#describe()
    */
   protected void describe(StringBuffer elemBuf, StringBuffer htmlBuf, int level) {


      // get the current indent
      String indent = getDescrIndent(level);

      String classStr = " Class [" + StringUtils.getClassName(this) + "]\n";

      elemBuf.append(indent).append(classStr);

      htmlBuf.append("\n").append(indent).append(classStr);
      htmlBuf.append(indent).append(" Html Header [" + html.toString() + "]\n");
      htmlBuf.append(indent).append(" Html Footer [" + htmlFooter.toString() + "]\n");
      
      Iterator iter = elems.iterator();

      level += 1;

      while (iter.hasNext() ) {
         HtmlElement elem = (HtmlElement) iter.next();
         elem.describe(elemBuf,htmlBuf, level);
      }



   }


}
