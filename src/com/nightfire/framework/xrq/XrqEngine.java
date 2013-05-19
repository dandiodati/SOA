package com.nightfire.framework.xrq;

import java.util.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;


import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.*;

import com.nightfire.framework.locale.*;

import com.nightfire.framework.xrq.utils.*;
import com.nightfire.framework.xrq.sql.*;


import org.w3c.dom.*;


/**
 * This is the main class that should be used for all access to the xrq infrastructure.
 * It simplifies usage by creating all major components. In general there should only be one type of
 * xrq engine running at a time.
 */
public class XrqEngine
{

  public static final String DEFAULT_XRQ_KEY = "XRQ";
  public static final String DEFAULT_XRQ_TYPE = "ENGINE";

  public static final String QUERY_EXECUTOR_TYPE = "QUERY_EXECUTOR_TYPE";
  public static final String CLAUSE_FACTORY_TYPE = "CLAUSE_FACTORY";
  public static final String PAGE_CACHE_TYPE = "PAGE_LIST_CACHE";

  public static final String MAX_WAIT_TIME_PROP = "MAX_WAIT_TIME";

  private QueryBuilder qBuilder;
  private PageListCache plCache;

  private int maxWaitTime;

  private static Map engineMap = Collections.synchronizedMap(new HashMap() );
  private static boolean localeInitialized = false;


  /**
   * acquire a xrq engine object.
   * @param key The property key for the xrq engine.
   * @param type The property type for the xrq engine.
   */
  public static final XrqEngine acquireXrqEngine(String key, String type) throws FrameworkException
  {
      String engkey = key + "_" + type;

     XrqEngine engine = (XrqEngine)engineMap.get(engkey);


     synchronized (engineMap) {
        if ( engine == null ) {
           engine = new XrqEngine(key,type);
           engineMap.put(engkey, engine);
        }
     }


     return engine;
  }

  public static final void releaseXrqEngine(XrqEngine engine)
  {
     // since current implementation uses a singleton objects, do nothing here.
     // this could be implemented in the future to do pooling.
  }



  /**
   * initializes the single XrqEngine object.
   * This class is thread safe.
   */
   protected XrqEngine (String key, String type) throws FrameworkException
  {
     // no synchronization is needed since the acquire method is handling it.
     if (!localeInitialized ) {
         
         if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, "XrqEngine: Initializing Resource Bundle.");
        
        if(!NFLocale.init(XrqConstants.XRQ_RESOURCE_CATALOG, XrqLanguageResource.RESOURCE_TYPE )) {
           Debug.warning("XrqEngine: Resource Bundle cannot initialize, using defaults.");
        }
        localeInitialized = true;
     }

     if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
        Debug.log(Debug.OBJECT_LIFECYCLE, "XrqEngine: Initializing XrqEngine.");
      
     Map props;

     try {
       props = PersistentProperty.getProperties(key, type);
     } catch (PropertyException pe) {
        String err = "XrqEngine: Failed to get properties key [" + key + "] type [" + type +"] : " + pe.getMessage();
        Debug.error(err);
        throw new FrameworkException(err);
     }


     String  maxWaitTimeStr = PropUtils.getRequiredPropertyValue(props, MAX_WAIT_TIME_PROP);

     try {
        maxWaitTime = StringUtils.getInteger(maxWaitTimeStr) * XrqConstants.MSEC_PER_SECOND;
     } catch (PropertyException pe) {
        String err = "XrqEngine: Failed to get property ["+ MAX_WAIT_TIME_PROP +"] : " + pe.getMessage();
        Debug.error(err);
        throw new FrameworkException(err);
     }


     ClauseFactory factory = new ClauseFactory(key, CLAUSE_FACTORY_TYPE);

     qBuilder = new QueryBuilder();

     // set up ordered nodes
     qBuilder.initialize("Query", props);

     // set the configured factory.
     qBuilder.setClauseFactory(factory);


     Map plcProps;

     try {
        plcProps =  PersistentProperty.getProperties(key, PAGE_CACHE_TYPE);
     } catch (PropertyException pe) {
        String err = "XrqEngine: Failed to get properties key [" + key + "] type [" + type +"] : " + pe.getMessage();
        Debug.error(err);
        throw new FrameworkException(err);
     }
     plCache = new PageListCache(plcProps, maxWaitTime);
     

  }

  /**
   * Used to execute an XRQ query request.
   * There are two types of requests, one to perform a new query, and another to
   * perform the retrieval of another page of a past query.
   * @exception FrameworkException is thrown if a system error occurs.
   * @exception MessageException is thrown if xml data is invalid.
   * @exception UnavailableResourceException thrown if resources are currently busy.
   * @exception PageExpiredException thrown if the page being request has expired.
   * @returns The page results from the request. Or null of no records were found.
   */
  public Page execute(String xml) throws FrameworkException, MessageException, UnavailableResourceException, PageExpiredException
  {
     return execute(new XMLMessageParser( xml) );
  }

  /**
   * Used to execute an XRQ query request.
   * There are two types of requests, one to perform a new query, and another to
   * perform the retrieval of another page of a past query.
   * @param parser The parser which contains the xrq message.
   * NOTE: The xml in the parser will be modified, so if the xml is needed, the caller will
   * be responsible for copying it. Or the caller can used the execute(String) method.
   * @exception FrameworkException is thrown if a system error occurs.
   * @exception MessageException is thrown if xml data is invalid.
   * @exception UnavailableResourceException thrown if resources are currently busy.
   * @exception PageExpiredException thrown if the page being request has expired.
   * @returns The page results from the request. Or null of no records were found.
   */
  public Page execute(XMLMessageParser parser) throws FrameworkException, MessageException, UnavailableResourceException, PageExpiredException
  {

     String key = null;
     int pageIndex = -1;

     PageList pList = null;

     Page page = null;


     long start = 0;

      if ( Debug.isLevelEnabled(Debug.BENCHMARK) )
        start = System.currentTimeMillis();


     String keyNode = XrqConstants.HEADER_NODE + "." + XrqConstants.PAGE_KEY_NODE;

     if ( parser.exists(keyNode ) && StringUtils.hasValue(key = parser.getValue(keyNode)) ) {
           
         if(Debug.isLevelEnabled(Debug.MSG_STATUS))
           Debug.log(Debug.MSG_STATUS, "XrqEngine : Trying to retrieve another page." 
                            +"\n: Page list key : "+ key );

           page = getPastPage(parser, key);

           if ( Debug.isLevelEnabled(Debug.BENCHMARK) )
              Debug.log(Debug.BENCHMARK,"PAGE RETRIVAL TIME: [" + ((double)(System.currentTimeMillis() - start))/ (double)XrqConstants.MSEC_PER_SECOND + "] seconds.");
     } else {
        Debug.log(Debug.NORMAL_STATUS, "XrqEngine : Performing new xrq search." );
        page = sendNewRequest(parser);
        if ( Debug.isLevelEnabled(Debug.BENCHMARK) )
              Debug.log(Debug.BENCHMARK,"XrqEngine: NEW QUERY TIME: [" + ((double)(System.currentTimeMillis() - start))/ (double)XrqConstants.MSEC_PER_SECOND + "] seconds.");
     }

     return page;

  }

  /**
   * obtains the next requested page from a past page list.
   *
   */
  private Page getPastPage(XMLMessageParser parser, String pageListKey) throws FrameworkException, MessageException, UnavailableResourceException, PageExpiredException
  {
     int pageIndex;
     
     try {
        pageIndex = StringUtils.getInteger( parser.getValue(XrqConstants.HEADER_NODE +"." + XrqConstants.NEXTPAGE_NODE) );
     } catch (FrameworkException e) {
        String err = "XrqEngine : Failed to convert int : " + e.getMessage();
        Debug.error(err);
        throw new FrameworkException(err);
     }

     if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        Debug.log(Debug.MSG_STATUS, "XrqEngine : Trying to obtain page list ["+ pageListKey + "] and page [" + pageIndex +"]" );
     
     PageList pList = plCache.getPageList(pageListKey);


     Page page = pList.getPage(pageIndex);


     if(Debug.isLevelEnabled(Debug.MSG_STATUS))
         Debug.log(Debug.MSG_STATUS, "XrqEngine: Got Page [" + pageIndex +"]");

     if (Debug.isLevelEnabled(Debug.MSG_STATUS) )
        Debug.log(Debug.MSG_STATUS, page.describe() );

     return page;
  }


  /**
   * sends a new request, and returns the first page of records.
   */
  private Page sendNewRequest(XMLMessageParser parser) throws FrameworkException, MessageException, UnavailableResourceException, PageExpiredException
  {
     Document dom = parser.getDocument();

     // remove header and build query string
     qBuilder.extractHeader(dom);
     String query = qBuilder.eval(dom);

     if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        Debug.log(Debug.MSG_STATUS, "XrqEngine : Created query [" + query  +"]");
     
     QueryExecutor qe = new SQLExecutor(maxWaitTime);
     // execute query string
     qe.executeQuery(query);

     // create a page list of results
     PageList pList = plCache.createPageList(qe);

     // if the page list is null, then there were no records. return null.
     if (pList == null ) {
        Debug.log(Debug.NORMAL_STATUS, "XrqEngine : No Records found.");
        return null;
     }

     if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        Debug.log(Debug.MSG_STATUS, "XrqEngine : Created PageList [" + pList.getIdentifier()  +"]");

     Page page = pList.getPage(0);

     if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        Debug.log(Debug.MSG_STATUS, "XrqEngine: Returning first page of data." );

     if (Debug.isLevelEnabled(Debug.MSG_STATUS) )
        Debug.log(Debug.MSG_STATUS, page.describe() );

     return page;
  }



}