/**
 * Copyright (c) 2004 NeuStar, Inc.  All rights reserved.
 *
 * $Header: $
 */

package com.nightfire.webgui.core.resource;

import java.net.URL;
import java.util.*;
import javax.servlet.*;
import org.w3c.dom.*;

import java.io.*;

import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.util.xml.*;
import com.nightfire.framework.util.*;

import com.nightfire.framework.debug.*;
import com.nightfire.webgui.core.*;

/**
 * Transformer class for transforming a URL into an Map containing aliases.
 */
public class AliasDataResourceTransformer implements ResourceTransformer
{
    /**
     * Attribute name that contains the group id.
     */
    public static final String ID = "id";

    private ServletContext context = null;
    private DebugLogger log;

    /**
     * Constructor where initialization happens.
     *
     * @param context ServletContext
     */
    public AliasDataResourceTransformer ( ServletContext context )
    {
        this.context = context;

        String webAppName = ServletUtils.getWebAppContextPath(context);
        log = DebugLogger.getLogger(webAppName, getClass());

    }

   /**
    * Transforms a URL of the data into the needed object type.
    * @param enc The character encoding to use while loading the url.
    * @param  url  The url to the data to transform.
    *
    * @return Object of Map type. The map structure is as follows:
    *    key:   group id.
    *    value: Map of (orignalValue, aliasValue)
    * If no mappings are found, an empty Map will be returned. 
    *
    * @throws FrameworkException when processing fails.
    */
    public Object transformData(URL url, String enc) throws FrameworkException
    {
        log.debug("transformData(): Returning a new Map object for url [" + url.toString() + "]");

        Map aliasGroups = new HashMap();
        /* Loading product/Customer specific aliases */
        try
        {
            XMLPlainGenerator orig = new XMLPlainGenerator(url.openStream());
            Document doc = orig.getDocument();
            String ALIASES = "aliases";
            String XML = ".xml";
            if (url.toString().endsWith(ALIASES + XML))
            {
                String urlStr = url.toString();
                String webinfStr = "/WEB-INF/";
                if (urlStr.indexOf(webinfStr) != -1)
                {
                    try
                    {
                        String path = urlStr.substring(urlStr.indexOf(webinfStr), urlStr.indexOf(ALIASES) + ALIASES.length());
                        File[] files = FileUtils.getAvailableFileList(context.getRealPath(path));
                        for (File file : files)
                        {
                            try
                            {
                                XMLPlainGenerator xtempdoc = new XMLPlainGenerator(file.toURL().openStream());
                                mergeGroups (orig, xtempdoc);
                            }
                            catch (Exception e)
                            {
                                log.warn("transformData(): Unable to load file [" + file.toURL().toString() + "]. Continuing with next file.");
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        log.warn("transformData(): Unable to load files from aliases folder due to Exception: " + e.getMessage());
                    }
                }
            }

            // resolve id links within the xml document file
            XMLRefLinker linker = new XMLRefLinker(doc);
            doc = linker.resolveRefs();

            createAliasGroups( doc, aliasGroups );

        }
        catch (IOException ex)
        {
            log.error ("Could not open URL [" + url + "]: ", ex);
        }

        return aliasGroups;

    }//transformData

    private void mergeGroups(XMLPlainGenerator alias, XMLPlainGenerator productAlias) throws MessageException
    {
        Node [] productGrpNodes = productAlias.getChildren(productAlias.getNode(productAlias.getDocument(), "Aliases"), "Group");
        Node aliasParent = alias.getNode(alias.getDocument(), "Aliases");
        Node [] aliasGrpNodes = alias.getChildren(aliasParent, "Group");
        for (int i = 0; i < productGrpNodes.length; i++)
        {
            boolean matchFind = false;
            Node productGrpNode = productGrpNodes[i];
            Node idAttrbProd = productGrpNode.getAttributes().getNamedItem("id");

            for (int k = 0; k < aliasGrpNodes.length; k++)
            {
                Node aliasGrpNode = aliasGrpNodes[k];
                Node idAttrbAlias = aliasGrpNode.getAttributes().getNamedItem("id");
                if (idAttrbProd.getNodeValue().equals(idAttrbAlias.getNodeValue()))
                {
                    XMLGeneratorUtils.moveChildren(aliasGrpNode, productGrpNode);
                    matchFind = true;
                    break;
                }
            }
            if (!matchFind)
            {
                XMLGeneratorUtils.copy(aliasParent, productGrpNode);
            }
        }
        
    }

    public List getReferredResources(URL url, String enc)
    {
        return null;

    }//getReferredResources

    /**
     * Populate the aliasGroups map with the key being group ids and the values
     * being the map of orignalValue and aliased value.
     *
     * @param doc Document
     * @param aliasGroups map
     * @throws MessageException on error
     */
    private void createAliasGroups(Document doc, Map aliasGroups) throws MessageException
    {
        XMLPlainGenerator aliasGen = new XMLPlainGenerator(doc);

        Node[] aliasGroupNodes = aliasGen.getChildren(doc.getDocumentElement());
        for ( int i =0; i < aliasGroupNodes.length; i++ )
        {
            String groupId = aliasGen.getAttribute(aliasGroupNodes[i], ID );
            if (StringUtils.hasValue(groupId))
            {
                Map aliasMappings = new HashMap();
                aliasGroups.put(groupId, aliasMappings);
                setAliases(aliasGen, aliasMappings, groupId, aliasGroupNodes[i]);
            }
        }

    }//createaliasGroups

   /**
    * Loops over all aliases in this alias group and adds all orig values
    * as a mapping to the alias value.
    *
    * @param aliasGen The generator
    * @param aliasMappings The alias mappings for this specific group.
    * @param groupId The id of the alias group
    * @param aliasGroup The node for this specific alias group
    * @exception MessageException if an error occurs
    */
    private void setAliases(XMLPlainGenerator aliasGen, Map aliasMappings, String groupId, Node aliasGroup) throws MessageException
    {

        Node [] aliasNodes = aliasGen.getChildren(aliasGroup);
        for(int i = 0; i < aliasNodes.length; i++ )
        {
            String alias = aliasGen.getNodeValue(aliasNodes[i]);
            Node[] origNodes = aliasGen.getChildren(aliasNodes[i]);

            for(int j = 0; j < origNodes.length; j++ )
            {
                String orig = aliasGen.getNodeValue(origNodes[j]);
                aliasMappings.put(orig, alias);
                if( log.isDebugEnabled() )
                    log.debug("Adding alias mapping value [" +
                                orig +"], alias [" + alias +"], group [" + groupId + "]");
            }//for

        }//for

    }//setAliases

}//AliasDataResourceTransformer
