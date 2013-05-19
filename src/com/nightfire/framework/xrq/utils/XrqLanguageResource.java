package com.nightfire.framework.xrq.utils;


import com.nightfire.framework.locale.*;
import java.util.*;


/**
 * language resources used by XRQEngine.
 */
public class XrqLanguageResource extends NFResource
{
   public static final String RESOURCE_TYPE =  "com.nightfire.framework.xrq.utils.XrqLanguage";

   public static final String PAGE_EXPIRED = "The search results have expired. You will need to perform a new search.";
   public static final String RESOURCE_BUSY = "System resources are busy, please wait a minute and try again.";

   
   public static final String CACHE_FULL = "Page Cache is currently full.";



   public XrqLanguageResource()
   {
	   	Properties textResource = new Properties();

	   	textResource.put(PAGE_EXPIRED, PAGE_EXPIRED);
      textResource.put(RESOURCE_BUSY, RESOURCE_BUSY);
      textResource.put(CACHE_FULL, CACHE_FULL);

	   	setContents(textResource);
   }


} 