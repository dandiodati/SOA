package com.nightfire.framework.xrq;

import java.util.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.cache.*;
import com.nightfire.framework.xrq.utils.*;
import com.nightfire.framework.locale.*;


/**
 * This is the main paging class, which is the access point for all client code.
 * Its job is to
 * 1. maintain a map of all PageList objects.
 * 2. handle creation, expiration, and destruction of PageLists.
 * 3. Handle synchronization to the page lists.
 *
 *All PageLists and retrieved from this
 * class.
 */
public class PageListCache implements CachingObject
{

  private Map props;

  /**
   * property which indicates the amount of idle time(seconds) before trying to remove page lists.
   * if set to 0, then the time is unlimited.
   */
  public static final String CLEANUP_IDLE_TIME_PROP = "CLEANUP_IDLE_TIME";

  /**
   * property which indicates the amount of idle time(seconds) before a page list expires.( Based on last access to the page list).
   * if set to 0, then the time is unlimited.
   */
  public static final String PAGELIST_EXPIRED_PROP = "PAGELIST_EXPIRED_TIME";

  /**
   * property which indicates the maximum number of pagelists that this cache can maintain
   */
  public static final String MAX_NUM_PAGELISTS_PROP = "MAX_NUM_PAGELISTS";

  /**
   * property which indicates the number of records in each page.
   */
  public static final String RECORDS_PER_PAGE_PROP = "RECORDS_PER_PAGE";

  /**
   * property which indicates the number of pages in each page list memory buffer
   */
  public static final String PAGES_PER_PAGELIST_PROP = "PAGELIST_BUFFER_SIZE";

  /**
   * property which indicates the max number of records that can be retrieved from the db
   */
  public static final String MAX_PAGE_SWAP_SIZE_PROP = "MAX_SWAP_SIZE";

  /**
   * property which indicates the directory where swap files are written to.
   * NOT IMPLEMENTED
   */
  public static final String SWAP_DIR_PROP = "SWAP_FILE_DIR";


  private String lastKey = "";

  private int cleanUpIdleTime;
  private int cleanUpWaitTime;
  private int maxNumPageLists;
  private int maxPageWaitTime;
  private int recordsPerPage;
  private int pagesPerList;
  private int pageListExpiredTime;


  private Map pageLists;
  private Map badLists;

  private String swapDir = null;

  private int maxSwapSize;

  private Lock cacheLock = new SyncLock("PageListCache");
  private CleanerThread cleaner;

  /**
   * The amount of time (milliseconds) that the cleanup thread will wait to try to destroy a page list object.
   */
  private static final int CLEANUP_PAGE_DESTROY_TIME = 100;

  /**
   * initializes this class.
   * @param props The properties for this class.
   * @param max wait time to obtain a resource(in milliseconds)
   */
  public PageListCache (Map props, int maxWaitTime) throws FrameworkException
  {

     //Register for clearing.
     CacheManager.getRegistrar().register ( this );

     if(Debug.isLevelEnabled(Debug.MSG_DATA))
        Debug.log(Debug.MSG_DATA, "Initializing PageListCache");
     
     this.props = props;
     StringBuffer errors = new StringBuffer();

     String cleanUpIdleTimeStr = PropUtils.getRequiredPropertyValue(props,CLEANUP_IDLE_TIME_PROP,errors);

     String maxNumPageListsStr = PropUtils.getRequiredPropertyValue(props,MAX_NUM_PAGELISTS_PROP,errors);

     String recordsPerPageStr = PropUtils.getRequiredPropertyValue(props,RECORDS_PER_PAGE_PROP,errors);
     String pagesPerListStr = PropUtils.getRequiredPropertyValue(props,PAGES_PER_PAGELIST_PROP,errors);
     String pageListExpiredTimeStr = PropUtils.getRequiredPropertyValue(props,PAGELIST_EXPIRED_PROP,errors);

     String maxSwapSizeStr         = PropUtils.getRequiredPropertyValue(props,MAX_PAGE_SWAP_SIZE_PROP,errors);


     // nod used
     //swapDir   =   PropUtils.getRequiredPropertyValue(props, SWAP_DIR_PROP,errors);

     if ( errors.length()  > 0 ) {
       Debug.error("PageListCache: " + errors.toString() );
       throw new FrameworkException("PageListCache: " + errors.toString() );
     }

     try {
        cleanUpIdleTime = StringUtils.getInteger(cleanUpIdleTimeStr) * XrqConstants.MSEC_PER_SECOND;

        maxNumPageLists = StringUtils.getInteger(maxNumPageListsStr);
        maxPageWaitTime = maxWaitTime;
        recordsPerPage = StringUtils.getInteger(recordsPerPageStr);
        pagesPerList = StringUtils.getInteger(pagesPerListStr);
        pageListExpiredTime = StringUtils.getInteger(pageListExpiredTimeStr) * XrqConstants.MSEC_PER_SECOND;

        maxSwapSize = StringUtils.getInteger(maxSwapSizeStr);
     } catch (FrameworkException e) {
        Debug.error("PageListCache: failed to convert to int: " + e.getMessage() );
        throw new FrameworkException("PageListCache: failed to convert to int: " + e.getMessage() );
     }

     if ( pagesPerList % 2 != 1 ) {
        Debug.warning("Property " + PAGES_PER_PAGELIST_PROP + " must be an odd integer, adding 1");
        pagesPerList += 1;
     }

     if ( maxSwapSize < pagesPerList) {
        Debug.warning("Property "+ MAX_PAGE_SWAP_SIZE_PROP + " must be greater than " + PAGES_PER_PAGELIST_PROP + ", setting to " + PAGES_PER_PAGELIST_PROP + " + 1.");
        maxSwapSize = pagesPerList + 1;
     }

     // access to the main pages list is protected by pageListCache lock
     pageLists = new HashMap();
     //pageLists = Collections.synchronizedMap(new HashMap() );

     // synchronize access to this bad map
     //badLists  = Collections.synchronizedMap(new HashMap() );
     badLists = new HashMap();

     cleaner = new CleanerThread();
     cleaner.start();

     if(Debug.isLevelEnabled(Debug.MSG_DATA))
        Debug.log(Debug.MSG_DATA, "Done Initializing PageListCache");
  }

  /**
   * creates a page List using a queryExecutor.
   * If there are no records then a null PageList is returned.
   * NOTE: The QueryExceutor should have already been executed, and should be associated with a single
   * request (thread).
   * If something goes wrong, this method handles calling executor.releaseResources().
   *
   */
  public final PageList createPageList(QueryExecutor executor) throws UnavailableResourceException, FrameworkException
  {
     // make sure to syncronize when obtaining or adding a page.
     PageList pageList = null;
     String key = null;

     long start =0, objCreate =0;

     if ( Debug.isLevelEnabled(Debug.BENCHMARK) )
             start = System.currentTimeMillis();

     boolean destroyList = false;
     try {


        key = genKey();
        
        if(Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log(Debug.MSG_DATA, "PageListCache: createPageList() - Trying to create pagelist " + key );

        if ( Debug.isLevelEnabled(Debug.BENCHMARK) )
             objCreate = System.currentTimeMillis();

        // the page list handles releasing resources on the executor.
        pageList = new PageList(key, pageListExpiredTime, maxPageWaitTime, pagesPerList,
                                       recordsPerPage, executor, maxSwapSize,swapDir);

        if ( Debug.isLevelEnabled(Debug.BENCHMARK) )
              Debug.log(Debug.BENCHMARK,"PageListCache: creating pagelist object time : [" + ((double)(System.currentTimeMillis() - objCreate))/ (double)XrqConstants.MSEC_PER_SECOND + "] seconds.");


        int availPages = pageList.getNumOfAvailPages();

        cacheLock.acquire();

        // check if there were any records found.
        // if there are no availiable pages then return null, indicating
        // that there were records found.
        // if there is only one page it does not have to be placed into the page list cache.
        //
        if (availPages == 0 ) {
           return null;
        } else if (availPages > 1 ) {
           // else if there is at least one page found
           // check if there is enough room in this page list cache.
           // if the page list cache is full, then throw an unavailable exception
           // otherwise add the page list to the cache.
           if ( pageLists.size() >= maxNumPageLists ) {
             destroyList = true;
             String msg = NFLocale.getString(XrqConstants.XRQ_RESOURCE_CATALOG, XrqLanguageResource.CACHE_FULL);

             //try to clean up the cache, for next time
             // then thrown a unavailable resource exception to try again later.
             cleanup();
             throw new UnavailableResourceException(msg);
           } else
              pageLists.put(key, pageList);
        }

     } catch (InterruptedException ie ) {
        Debug.log(Debug.THREAD_WARNING, "PageListCache: createPageList() - Could not obtain cache lock: " + ie.getMessage() );
        destroyList = true;
        String msg = NFLocale.getString(XrqConstants.XRQ_RESOURCE_CATALOG, XrqLanguageResource.RESOURCE_BUSY);
        throw new UnavailableResourceException(msg);
     } catch (UnavailableResourceException e ) {
        Debug.log(Debug.MSG_STATUS, "PageListCache: " + e.getMessage() );
        throw (e);
     } catch (FrameworkException e ) {
        String err = "PageListCache: Failed to create page list: " + e.getMessage();
        Debug.error(err);
        throw new FrameworkException(err);
     } finally {
        // if there was no room in the cache then destroy the pagelist
        if (destroyList ) {
           try {
              pageList.cleanup(CLEANUP_PAGE_DESTROY_TIME);
           } catch (FrameworkException e) {
              // adding to cache to let Clean up thread handle it later
              badLists.put("(INVALID)" + key, pageList);
              String err = "PageListCache: Could not clean up Page List:, adding to invalid map, for later cleanup." + e.getMessage();
              Debug.warning(err);
           }
        }

         cacheLock.release();

     }

     if ( Debug.isLevelEnabled(Debug.BENCHMARK) )
              Debug.log(Debug.BENCHMARK,"PageListCache: PAGE LIST TOTAL CREATION TIME: [" + ((double)(System.currentTimeMillis() - start))/ (double)XrqConstants.MSEC_PER_SECOND + "] seconds.");

     return pageList;

  }

  /**
   * Tells the cleanup thread to try to remove expired pagelists.
   * NOTE: This is a best effort call, so some expired pagelists
   * may not be removed.
   * This is an asyncronous call that returns immediately.
   */
  public final void cleanup()
  {
     cleaner.cleanup();
  }

  /**
   * Guarranted to clear all pagelists in this cache
   * On return of calling clear() all pages will be gone.
   * NOTE: Normally cleanup should be used, because this call be a larger performance hit.
   * NOTE 2 : Until this call returns all access to the PageListCache will be locked.
   */
  public final void clear()
  {
      try {
         cacheLock.acquire();
         Iterator iter = pageLists.values().iterator();

          // destroy all pagelists

          while (iter.hasNext() ) {
             PageList pagelist = (PageList) iter.next();
             destroyPageList(pagelist, 0);
             iter.remove();
          }
      } catch (Exception ie ) {
         Debug.log(Debug.THREAD_WARNING, "PageListCache: - Could not obtain cache lock: " + ie.getMessage() );
         Debug.logStackTrace(ie);
      } finally {
         Debug.log(Debug.THREAD_STATUS, "PageListCache: Cleared PageListCache. Page Cache size [" + pageLists.size() + "]");
         cacheLock.release();
      }
  }


  /**
   * Does a best effort approach to destroy a page list.
   *
   * @param pageList The page list to destroy.
   * waits for the maximum time specified by CLEANUP_PAGE_DESTROY_TIME.
   */
  public final void destroyPageList(PageList pageList)
  {
     destroyPageList(pageList, CLEANUP_PAGE_DESTROY_TIME);
  }

  /**
   * Does a best effort approach to destroy a page list.
   * @param pageList The page list to destroy.
   * @param waitTime The max wait time to wait for a page to be destroyed.
   * If the page is not destroyed with in the wait time specified, it is logged.
   */
  public final void destroyPageList(PageList pageList, int waitTime)
  {
     PageList temp;

        try {
            pageList.cleanup(waitTime);

        } catch (FrameworkException e) {
           String err = "PageListCache: Could not clean up Page List: " + pageList.describe() + ": " + e.getMessage();
           Debug.warning(err);
        }
  }


  /**
   * Returns the specified page list.
   * @param key The key used to identify a single page list.
   * @returns the Page list associtated with the key.
   * @exception UnavailableResourceException is thrown if resources are currently unavailable.
   * @exception PageExpiredException is thrown if this page no longer exists.
   */
  public final PageList getPageList(String key) throws UnavailableResourceException, PageExpiredException
  {
    PageList pages;

    try {
       cacheLock.acquire(maxPageWaitTime);
       pages = (PageList) pageLists.get(key);

    } catch (InterruptedException ie ) {
       Debug.log(Debug.THREAD_WARNING, "PageListCache: -Could not obtain cache lock: " + ie.getMessage() );
       String msg = NFLocale.getString(XrqConstants.XRQ_RESOURCE_CATALOG, XrqLanguageResource.RESOURCE_BUSY);
       throw new UnavailableResourceException(msg);
    } finally {
       cacheLock.release();
    }

    if (pages == null) {
        Debug.log(Debug.MSG_WARNING, "PageList[" + key+ "]: No longer exists");
        String msg = NFLocale.getString(XrqConstants.XRQ_RESOURCE_CATALOG, XrqLanguageResource.PAGE_EXPIRED);
        throw new PageExpiredException(msg);
    }

    return pages;

  }



  /**
   * generates a unique key using the system time.
   * This methods keeps track of the last key generated so the keys are guarrantted to be unique
   * within this JVM session.
   *
   */
  private synchronized String genKey()
  {
     String newKey =  Long.toHexString(DateUtils.getCurrentTimeValue() );

     while (newKey.equals(lastKey) ) {
        Debug.warning("PageListCache : Duplicate key produced, regenerating key, " + newKey);
        try {
           Thread.sleep(1);
        } catch (InterruptedException e) {}
        newKey =  Long.toHexString(DateUtils.getCurrentTimeValue() );
     }
     Debug.log(Debug.MSG_STATUS, "PageListCache : Produced key, " + newKey);
     lastKey = newKey;

     return (newKey);
     //return (String.valueOf(DateUtils.getCurrentTimeValue() ) );
  }


  /**
   * Describes the contents of this page list cache.
   * (Lists all page lists contained within).
   * Useful for logging, but is an expensive operation.
   */
  public String describe() {
     StringBuffer desc = new StringBuffer("\nPAGE LIST CACHE CONTENTS: \n");

     try {
        cacheLock.acquire();
        Iterator iter = pageLists.values().iterator();

        while (iter.hasNext() ) {
           desc.append( ((PageList)iter.next()).describe() +"\n");
        }
     } catch (InterruptedException ie ) {
       Debug.log(Debug.THREAD_WARNING, "PageListCache: -Could not obtain cache lock: " + ie.getMessage() );
    } finally {
       cacheLock.release();
    }


     return desc.toString();
  }


  // ------------------------------------- inner classes ---------------------------------------

  /**
   * This is a single thread which removes expired page lists from
   * the cache. It is called an specified timed intervals, and when the cache gets
   * full.
   */
   private class CleanerThread extends Thread
  {

     public CleanerThread()
     {
        setDaemon(true);
        setName("PageListCache_Cleaner_" + Thread.currentThread().getName());
     }

     /**
      * indicates that cleanup should occur.
      * This method returns immediately and cleanup will occur at some later point in time.
      * Not guarranted to execute, since this thread may not be able to obtain a lock.
      *
      */
     public void cleanup()
     {
        synchronized (this) {
           notify();
        }
     }

     // main method
     public void run()
     {

           while (true) {
              try {
                 try {

                    // trys to obtain a lock on the cache.
                    cacheLock.acquire(cleanUpIdleTime);



                    Debug.log(Debug.THREAD_STATUS, "PageListCache: Performing cleanup. Page Cache size [" + pageLists.size() + "]");



                    if (Debug.isLevelEnabled(Debug.BENCHMARK) )
                       Performance.startBenchmarkLog(getName() + " PageListCache cleanup");

                    if (Debug.isLevelEnabled(Debug.MSG_STATUS) )
                       Debug.log(Debug.MSG_STATUS, "PageListCache: Before Cleanup : " + describe() );

                    // obtain a list of the current page lists
                    // and of page lists that did not make it.
                    Iterator iter = pageLists.values().iterator();
                    Iterator badIter = badLists.values().iterator();

                    // destroy any bad page lists that were created
                    Debug.log(Debug.THREAD_STATUS, "PageListCache: Destroying invalid page lists. Size [" + badLists.size() + "]");
                    while (badIter.hasNext() ) {
                       PageList pagelist = (PageList) badIter.next();
                       destroyPageList(pagelist);
                       badIter.remove();
                    }

                    // destroy any pagelists that expired
                    Debug.log(Debug.THREAD_STATUS, "PageListCache: Destroying expired page lists.");
                    while (iter.hasNext() ) {
                       PageList pagelist = (PageList) iter.next();
                       if (pagelist.isExpired() ) {
                          destroyPageList(pagelist);
                          iter.remove();
                       }
                    }
                 } catch (Exception ie ) {
                    Debug.log(Debug.THREAD_WARNING, "PageListCache: - Could not obtain cache lock: " + ie.getMessage() );
                    Debug.logStackTrace(ie);
                 } finally {
                    Debug.log(Debug.THREAD_STATUS, "PageListCache: Finished cleanup. Page Cache size [" + pageLists.size() + "]");

                    if (Debug.isLevelEnabled(Debug.MSG_STATUS) )
                       Debug.log(Debug.MSG_STATUS, "PageListCache : After Cleanup : " + describe() );

                    if (Debug.isLevelEnabled(Debug.BENCHMARK) )
                       Performance.finishBenchmarkLog(getName() + " PageListCache cleanup"  );


                    cacheLock.release();
                 }

                 // sleep for the specified time or until a notify wakes this thread up.
                 synchronized (this ) {
                    wait(cleanUpIdleTime);
                 }

              } catch (InterruptedException i ) {
                 Debug.log(Debug.THREAD_WARNING, "CleanerThread interrupted, ignoring: " + i.getMessage() );
              }
           }
     }


  }

    /**
     * Method invoked by the cache-flushing infrastructure
     * to indicate that the cache should be emptied.
     *
     * @exception FrameworkException if cache cannot be cleared.
     */
    public void flushCache ( ) throws FrameworkException
    {
        //Since access to this pageLists is synchronized, no locking is required.
        clear();
    }

}