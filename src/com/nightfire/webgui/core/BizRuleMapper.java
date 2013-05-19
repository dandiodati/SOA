/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core;

import  javax.servlet.*;
import  javax.servlet.http.*;

import com.nightfire.webgui.core.xml.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.debug.*;


import com.nightfire.framework.util.*;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.transformer.*;
import com.nightfire.framework.message.util.xml.*;

import java.util.*;

import org.w3c.dom.*;

/**
 * This class handles mappings a business rule error to the
 * the related gui meta field.
 * It uses the mappings generators to look up the error field,
 * and then finds the id attribute which should indicate the
 * related gui meta field.
 *
 */

public class BizRuleMapper
{
   private XMLGenerator curMapping, defaultMapping, transformDefaultMapping;
   private boolean finalState = false;
   private boolean newInstance = false;
 
   private DebugLogger log;
  

  
   /**
    * Create an instance with the specfied mappings.
    * @param latestMapping The most current set of mappings formed from
    * the request message.
    * @param defaultMapping The default set of mappings formed from the meta
    * file.
    * @param transformedDefaultMapping The default mappings transformed
    * into their final state.
    *
    * All mapping files must be in the final xml format that is to be sent to a
    * back end process with one exception. Each field node must contain an id
    * attribute which contains the path to the associated gui xml field.
    * 
    */
   private BizRuleMapper(DebugLogger log, XMLGenerator latestMapping, XMLGenerator defaultMapping, XMLGenerator transformDefaultMapping)
   {
      curMapping = latestMapping;
      this.defaultMapping = defaultMapping;
      this.transformDefaultMapping = transformDefaultMapping;
      this.log = log;
      
   }

   protected BizRuleMapper(ServletContext context, XMLGenerator defaultMapping)
   {
      String webAppName = ServletUtils.getWebAppContextPath(context); 
      log = DebugLogger.getLogger(webAppName, this.getClass());

      this.defaultMapping = defaultMapping;
   }



   /**
    * Updates the current biz ruler mapper's default mappings
    * This method only updates this instance once.
    */
   public synchronized void updateDefault(Object defaultTransformer) throws MessageException
   {
       if ( !finalState && defaultTransformer != null) {
          transform(defaultTransformer);
          finalState = true;
       }
    }

    /**
     * Each request needs to call this method to get a a new instance.
     *
     */
    public synchronized BizRuleMapper getNewInstance(XMLGenerator latestMappings)
    {
       BizRuleMapper newMapper = new BizRuleMapper(log, latestMappings, defaultMapping, transformDefaultMapping);
       
       newMapper.finalState = true;
       newMapper.newInstance = true;

       return newMapper;
    }

    /**
    * Sets the final state of the BizRuleMapper.
    * Indicates that mappings in this BizRuleMapper are in same form as final
    * output request.
    * @return bool true indicates that the mappings contained within this mapper
    * are in the final output state.
    *   false indicates that the mappings are in the form of the gui xml.
    */
   public boolean isFinalState()
   {
      return finalState;
   }



    private void transform(Object defaultTransform) throws MessageException
    {

       if ( defaultTransform instanceof XMLFilter)
          transformDefaultMapping = ((XMLFilter)defaultTransform).filter(defaultMapping);
       else if (defaultTransform instanceof XSLMessageTransformer)
         transformDefaultMapping =
            new XMLPlainGenerator(((XSLMessageTransformer)defaultTransform).transform(defaultMapping.getOutputDOM() ) );

    }

     /**
     * Returns a list of possible destination mappings.
     * This translates a biz rule xpath context to
     * the related field(s) in the gui.
     *
     * If no mapping can be resolved then an empty list will be returned.
     *
     */
    public  synchronized List getMappingPaths(String xpath) throws FrameworkException
    {
         if (newInstance != true )
            throw new FrameworkException("BizRuleMapper: getNewInstance must be called before using this method.");

         List dests = new ArrayList();


         // if there is validation mapping try top find the nf path.

         if ( log.isDebugEnabled() )
            log.debug("Evaluating xpath expression [" + xpath+ "]");

         // this is faster than using the XPathAccessor
         ParsedXPath parsedXPath = new ParsedXPath(xpath);
         List matches = null;

         
         // if there are mappings then try to find all nodes

         // first we try the latest set of mappings which may not exist
         // (outgoing xml format)
         // this case occurs for nodes in the outgoing xml request
         if ( curMapping != null) {
            matches = parsedXPath.getNodeList(curMapping.getDocument());
            dests = findMappings(matches);
         }

         
         // if no mappings are found then we try again with the transformed default set of
         // mappings (outgoing xml format)
         // this occurs for fields that are not yet outgoing xml request
         if (dests.size() == 0 &&  transformDefaultMapping != null)  {
            matches = parsedXPath.getNodeList(transformDefaultMapping.getDocument());
            dests = findMappings(matches);
         }

         // if still no mappings are found then we try again with the default set of
         // mappings ( gui xml format )
         // this case occurs for fields that are gui specific and will not be in the
         // outgoing xml request.
         if (dests.size() == 0 &&  defaultMapping != null)  {
            matches = parsedXPath.getNodeList(defaultMapping.getDocument());
            dests = useNodesAsDests(defaultMapping, matches);
         }

         if ( log.isDebugEnabled() )
            log.debug("Found " + dests.size() + " mappings.");


        return dests;
    }

    private List findMappings(  List matches )
    {
         List dests = new ArrayList();


         if (matches == null || matches.size() == 0 )
            return dests;


         // for each node find the id which maps back to the gui meta xml.
         // there could be multiple matches in the case of a xpath with an or expression
         // so return back all of them
         for (int i= 0; i < matches.size();i++ ) {
            Node n = (Node)matches.get(i);

               NamedNodeMap map  = n.getAttributes();

               Node dest = null;
               if ( map != null )
                   dest = map.getNamedItem("id");

               if (dest != null )
                  dests.add(dest.getNodeValue());
         }

         
         return dests;

    }

    // adds the current nodes as paths
    // to the gui fields.
    //
    private List useNodesAsDests( XMLGenerator defaultMapping,List matches )
    {
         List dests = new ArrayList();

         if (matches == null || matches.size() == 0 )
            return dests;


         for (int i= 0; i < matches.size();i++ ) {
            Node n = (Node)matches.get(i);

            String path = defaultMapping.getXMLPath(n);
            if ( StringUtils.hasValue(path))
                dests.add(path);
         }


         
         return dests;

    }

    public String describe()
    {
       StringBuffer buffer = new StringBuffer().append("BizRuleMapper : Current transformed outgoing Mappings: \n");
       if (curMapping != null)
          buffer.append(curMapping.describe() );
          
       buffer.append("\n\nDefault Mappings: \n");
       if ( defaultMapping != null)
          buffer.append(defaultMapping.describe() );
       buffer.append("\n\nDefault transformed Mappings: \n");
       if ( transformDefaultMapping != null)
          buffer.append(transformDefaultMapping.describe() );
          
       return buffer.toString();

    }







}
