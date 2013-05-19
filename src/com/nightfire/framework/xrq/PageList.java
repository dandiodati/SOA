package com.nightfire.framework.xrq;

import java.util.*;
import com.nightfire.framework.resource.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.xrq.utils.*;
import com.nightfire.framework.locale.*;


import com.nightfire.framework.message.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.parser.xml.*;





/**
 * This class is used to hold a list of Pages. It loads pages into an in memory buffer as needed, contains
 * multiple access methods to obtain pages, and tracks access time.
 * It contains an in memory page buffer the size of bufferSize, and a swap which contains
 * all other records. The page buffer acts as a small window to all the available pages. When
 * different pages are obtained (via getPage() ) the page buffer window slides.
 * As the page buffer slides new pages are dynamically loaded from the swap file.
 * NOTE: Only a single request thread at a time can access this class,
 * so each thread must have its own instance of this class.
 * This class is created only by the PageListCache class.
 *
 */
 //For unit tests to work you need to give them access to this method.
 public class PageList
//class PageList implements XrqConstants
{


  private static final String RECORD_NODE_PATH_START= XrqConstants.RECORD_CONTAINER_NODE +"." + XrqConstants.RECORD_NODE+ "(";


  private String pageKey;
  private int maxPageWaitTime; // number of milliseconds to wait to obtain a page.
  private int pageExpiredTime; // amount of idle time for a pagelist to expire
  private int bufferSize;      // the number of pages in the main page buffer
  private int recordsPerPage;  // the numbe of records per page of data.

  private int maxSwapSize;     // max number of records obtained from the db
  private String swapFileDir;  // the swap file directory

  private HashMap pages;
  private HashMap swapBuffer;
  private QueryExecutor qExec;
  private RecordSerializer rs;

  private String SwapFileDir;

  private SwapIOAccess swapIOAccess;  // provides access to the swap file

  // needs to be be volatile to prevent multiple threads from corrupting the data.
  private volatile long accessTime;

  private Lock pageLock;  // page buffer lock
  private Lock swapLock;   // swap lock

  private PageBufUpdater bufUpdater;       // thread responsible for transfers pages from
                                           // swap to page buffer

  private  SwapCreator sc;                // thread responsible for creating swap file if needed.

  private boolean isDestroyed = false;    // indicates if this page list has been destroyed

  //used to indicate if a swap file is being build and its size.
  private int swapSize  = -1;
  private boolean buildingSwap = false;


   int curPageIndex = 0;  // keeps track of the current page selected

  /**
   * package protected Constructor, only a pageListCache can create this object.
   * @param pageKey The unique key used to identify this PageList.
   * @param pageExpiredTime The amount of idle time( in milliseconds) before  this page list expires.
   * @param maxPageWaitTime The amount of time (milliseconds) to wait for a response from this page list.
   * @param bufferSize The number of pages to hold in the buffer cache at a time.
   * @param recordsPerPage The number of records within each page.
   * @param qExec The query executor used to execute obtain the pages.
   * @param maxSwapSize The maximum number of pages That will be retrieved for any query.
   * @param swapFileDir The directory where the swap file should be written to.
   */
  public PageList(String pageKey, int pageExpiredTime, int maxPageWaitTime, int bufferSize,
                   int recordsPerPage, QueryExecutor qExec, int maxSwapSize, String swapFileDir) throws FrameworkException
  {
     //this.parent = parent;
     this.pageKey = pageKey;
     this.maxPageWaitTime = maxPageWaitTime;
     this.pageExpiredTime = pageExpiredTime;
     this.swapFileDir = swapFileDir;
     this.recordsPerPage = recordsPerPage;
     this.qExec = qExec;
     rs = qExec.getResults();

     this.maxSwapSize = maxSwapSize;


     this.bufferSize = bufferSize;

     this.swapFileDir = swapFileDir;

     if ( bufferSize % 2 != 1 ) {
        throw new FrameworkException("PageList creation failed: PageList buffer size must be an odd number");
     }
     pageLock = new SyncLock("Page");
     swapLock = new SyncLock("Swap");

     if (maxSwapSize < bufferSize ) {
        throw new FrameworkException("PageList creation failed: maxSwapSize must be larger than bufferSize");
     }

     initializePages();

  }

  /**
   * sets up the pages, swap file, etc.
   * This method must be called after creation.
   *
   */
  private void initializePages() throws MessageException, FrameworkException
  {

     pages = new HashMap(bufferSize);



     List prevRecords = new ArrayList(recordsPerPage * bufferSize);
     Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "]: Initializing pages" );
     // first fill up buffer as much as possible


     for (int i = 0; i < bufferSize &&  rs.hasNext(); i++ ) {
        Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "]: Creating Page count " + i );
        XMLMessageGenerator gen = new XMLMessageGenerator("Page");
        for ( int j = 0; j < recordsPerPage && rs.hasNext(); j++) {
           String loc = RECORD_NODE_PATH_START + j + ")";

           String rec = (String) rs.next();

           Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "]: Adding record : " + rec );
           prevRecords.add(rec);

           rs.toXML(gen.getDocument(), loc, rec );

        }

        pages.put(new Integer(i), new Page( gen.getDocument() ) );

     }




     // if there are more records than the buffer size
     // create a swapIoAcess object
     // and then start the swap creator thread.
     // Then start the page buf updater thread.
     //
     // if there are no more records don't bother creating a swap file, or starting any
     // page/swap loading threads.
     if ( rs.hasNext() )  {
       swapIOAccess = new SwapIOAccess(swapFileDir, pageKey, prevRecords, maxSwapSize);

       // next add remaining records into swap in a separate thread.


       bufUpdater = new PageBufUpdater(maxPageWaitTime);


        sc = new SwapCreator();

        // indicates that a swap needs to be built
        swapSize = 0;
        sc.setPriority(Thread.MAX_PRIORITY);
        // tell the VM to start the thread
         Debug.log(Debug.THREAD_LIFECYCLE, "PageList[" + pageKey+ "]: Starting swap thread.");
        sc.start();

        // waiting for swap creator to start
        // can't use the Thread's isAlive method because it never seems to return
        // true when the thread is started, therefore this thread has its own test method.
        while( !sc.isStarted() ) {}


        // check if the swap creator failed to obtain the swap lock
        if (sc.failedToLock() ) {
           String err =  "PageList[" + pageKey+ "]: Swap Creator thread, Failed to obtain swap lock";
           Debug.error(err);
           throw new FrameworkException(err);
        }

        // start the buffer updater thread
        Debug.log(Debug.THREAD_LIFECYCLE, "PageList[" + pageKey+ "]: Starting swap to Buffer Thread.");
        bufUpdater.start();
     }  else {
        // release query executor resources since no swap creator thread was needed.
        try  {

          qExec.releaseResources();

        } catch (FrameworkException e) {
           Debug.error("Could Not release resource: " + e.getMessage());
        }
     }


     accessTime = System.currentTimeMillis( );
     Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "]: Done Initializing pages" );

  }


  /**
   *  creates a pages and adds it to the page buffer
   * @param index index of the page.
   * @param gen The xml generator used to build the page in.
   * @param recIter an Iterator which returns each record for this page.
   */
  private Page createAddPage(int index, XMLMessageGenerator gen, Iterator recIter) throws MessageException, FrameworkException
  {
     Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "].createAddPage - building page[" + index +"]" );

     Page aPage;
     for ( int j = 0; j < recordsPerPage && recIter.hasNext(); j++) {
           String loc = RECORD_NODE_PATH_START + j + ")";
           gen.create(loc);
           rs.toXML(gen.getDocument(), loc, (String)recIter.next() );
     }
     aPage = new Page( gen.getDocument() );

     pages.put(new Integer(index), aPage);

     return aPage;
  }


  /**
   * indicates if this pageList has expired.
   * @returns true if it has expired otherwise false.
   */
  public boolean isExpired()
  {
     long delta = calculateIdleTime();

     boolean old = false;

     if (delta > pageExpiredTime )
        old =  true;
     else
        old =  false;
        
     Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "].isExpired: idle time = " + delta + ", returning [" + old + "]");
     return old;

  }

  // calculates the idle time of this Page list.
  private long calculateIdleTime() {
     long now = System.currentTimeMillis( );
     long delta;

     delta = now - accessTime;


     return delta;
  }




  /**
   * Indicates if the page at this index exists.
   * @param index - The index of the page to test for.
   *
   * @exception UnavailableResourceException - thrown if a resource is current unavailable.
   * @exception PageExpiredException - This page has expired an is no longer valid.
   * @exception FrameworkException - Some other exception occured.
   * @returns true if the Page exists otherwise false.
   */
  public boolean exists(int index) throws UnavailableResourceException, PageExpiredException
  {
      boolean results = false;

      if (Debug.isLevelEnabled(Debug.MSG_STATUS) ) {
           Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "].exists(): Testing for existence of index [" + index + "]");
        }

      try {
        pageLock.acquire(maxPageWaitTime);

          if (isDestroyed) {
             Debug.log(Debug.MSG_WARNING, "PageList[" + pageKey+ "]: was destroyed");
             String msg = NFLocale.getString(XrqConstants.XRQ_RESOURCE_CATALOG, XrqLanguageResource.PAGE_EXPIRED);
             throw new PageExpiredException(msg);
          }

          accessTime = System.currentTimeMillis( );



        int pageSize = getNumOfPages();

        // check if the page is in the page buffer
        // else check if the a swap file was built and if it is in the swap file.
        // else if a swap file is currently being built throw an unavailable exception so the
        // the client tried again later.
        // otherwise the page does not exist
        if (pages.containsKey(new Integer(index)) )
           results = true;
        else if ( pageSize != -1 && index >= 0  && index < pageSize )
           results = true;
        else if (pageSize == -1 ) {
            Debug.log(Debug.THREAD_WARNING, "PageList[" + pageKey+ "]: exists() - Swap file is currently busy." );
           //pull out of NFLocale resource
           String msg = NFLocale.getString(XrqConstants.XRQ_RESOURCE_CATALOG, XrqLanguageResource.RESOURCE_BUSY);
           throw new UnavailableResourceException(msg);
        }

        if (Debug.isLevelEnabled(Debug.MSG_STATUS) ) {
           Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "].exists(): Testing for existence of index [" + index + "], " + results);
           Debug.log(Debug.MSG_STATUS, describe());
        }

     } catch (InterruptedException ie ) {
        Debug.log(Debug.THREAD_WARNING, "PageList[" + pageKey+ "]: exists() - Could not obtain page lock: " + ie.getMessage() );
        //pull out into NFLocale resource
        String msg = NFLocale.getString(XrqConstants.XRQ_RESOURCE_CATALOG, XrqLanguageResource.RESOURCE_BUSY);
        throw new UnavailableResourceException(msg);
     } finally {
        pageLock.release();
     }


     return results;
  }


  /**
   * returns the number of pages that are currently available in memory.
   *
   * These are the number of pages that are available and can be used immediately.
   * This does not include the number of pages in the swap file, since the number
   * may not be know at this time.
   *
   */
  public int getNumOfAvailPages()
  {
     return pages.size();
  }



  /**
   * returns the size of the total number of pages that exist.
   *
   * If the number of pages, fit in the page buffer, and the swap file is not needed
   * then the size of the page buffer is returned ( same as calling getNumOfAvailPages).
   * If a swap file is needed, and the swap file is currently being build then
   * a -1 is returned.
   * If the swap file is needed, and the swap file is already built then
   * the size is returned, this includes the pages in the page buffer.
   *
   */
  public int getNumOfPages()
  {

     int tempSize;


     tempSize = swapSize;

     // if the size of the swap is -1, then no swap was needed so
     // return the num of available pages
     // else if the size is 0, then the swap is currently being built
     // so return -1.
     // else the swap has been built, so calculate the number of pages available.
     // the swap file always contains the records from the page buffer too.
     //
     if (tempSize < 0 )
        return getNumOfAvailPages();
     else if ( tempSize == 0 )
        return -1;
     else {
        double temp = Math.ceil((double)tempSize/(double)recordsPerPage);
       // Debug.error("here " + tempSize +" recperpage " + recordsPerPage + " = " + temp);
        return ( (int) temp);
     }
  }

  /**
   * Returns the page at the specified index.
   * @exception UnavailableResourceException - thrown if a resource is current unavailable.
   * @exception PageExpiredException - This page has expired an is no longer valid.
   * @exception FrameworkException - Some other exception occured.
   * @returns true if the Page exists otherwise false.
   */
  public Page getPage(int index) throws UnavailableResourceException, PageExpiredException, FrameworkException
  {
     Page page = null;
     try {
        pageLock.acquire(maxPageWaitTime);

         if (isDestroyed) {
             Debug.log(Debug.MSG_WARNING, "PageList[" + pageKey+ "].getPage(): was destroyed");
             String msg = NFLocale.getString(XrqConstants.XRQ_RESOURCE_CATALOG, XrqLanguageResource.PAGE_EXPIRED);
             throw new PageExpiredException(msg);
          }


          accessTime = System.currentTimeMillis( );



        if (Debug.isLevelEnabled(Debug.MSG_STATUS) ) {
         Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "].getPage(): Trying to get Page [" + index + "]" );
         Debug.log(Debug.MSG_STATUS, describe());
        }

        page = (Page) pages.get(new Integer(index));




        // if the page is null go retrieve it directly from the swap
        if (page == null) {
           try {
             page = retrieveFromSwap(index);
           } catch (UnavailableResourceException e) {
              throw e;
           } catch (FrameworkException e) {
              String err = "PageList[" + pageKey+ "].getPage(): Failed to retrieve page from swap: " + e.getMessage();
              Debug.error(err);
              throw new FrameworkException(err);
           }
        }
        // if the page is still null then this was an invalid index return null.
        if (page == null) {
           String e = "PageList[" + pageKey+ "].getPage(): Page with index [" + index + "] doesn't exist";
           Debug.error(e);
           throw new FrameworkException(e);
        }

        page.setHeaderField(XrqConstants.PAGE_KEY_NODE, pageKey);
        page.setHeaderField(XrqConstants.CURRENT_PAGE_NODE, String.valueOf(index) );

        page.setHeaderField(XrqConstants.IN_MEM_PAGES_NODE, String.valueOf(getNumOfAvailPages()) );

        page.setHeaderField(XrqConstants.TOTAL_PAGES_NODE, String.valueOf(getNumOfPages()) );


        if (Debug.isLevelEnabled(Debug.XML_STATUS) ) {
         Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "].getPage(): Obtained page [" + index + "]" );
         Debug.log(Debug.MSG_STATUS, page.describe());
         
        }


        curPageIndex = index;

     } catch (InterruptedException ie ) {
        Debug.log(Debug.THREAD_WARNING, "PageList[" + pageKey+ "].getPage():  Could not obtain page lock: " + ie.getMessage() );
        
        String msg = NFLocale.getString(XrqConstants.XRQ_RESOURCE_CATALOG, XrqLanguageResource.RESOURCE_BUSY);
        throw new UnavailableResourceException(msg);
      } catch (MessageException ie ) {
        String err = "PageList[" + pageKey+ "].getPage(): Failed to get Page : " + ie.getMessage();
        Debug.error(err);
        throw new FrameworkException(err);
     } finally {
         // update the buffer in a background thread
         // and continue.
        updateBufferFromSwap();
        pageLock.release();
     }



     return page;
  }

  /*
   * updates the buffer( window) with pages from the swap file
   * This method only retrieves new pages that are not already in the
   * page buffer.
   */
  public void updateBufferFromSwap()
  {
     // The following variables are used as temporary space
     // for updating the page buffer

     Set currentKeys = new HashSet();
     Set dirtyKeys = new HashSet();
     Set newKeys = new HashSet();


     // calulate the distance from the midpoint to the beginning or end of the window
     int split = bufferSize/2;

     int midIndex = curPageIndex;
     Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "].updateBufferFromSwap - Current page index [" + midIndex +"]");

     int totalPageCount =  getNumOfPages();


     // if the number of total pages is less than the buffer size, all pages
     // are in the buffer and therefore never needs updating
     // we return to avoid the unneeded calculations
     //
     if ( totalPageCount <= bufferSize ) {
         Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "].updateBufferFromSwap - Total Page count less than buffer size, update not needed." );
        return;
     }


     try {

        if (totalPageCount == -1 )  {
          Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "].updateBufferFromSwap - Total Page count not yet available, skipping update." );
          return;
        }
     }  catch (Exception e ) {
        Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "].updateBufferFromSwap - Total Page count not yet available, skipping update: " + e.getMessage() );
        return;
     }


     // if the midIndex is within split distance of the beginning or end of the list of pages
     // then shift enough to prevent a buffer under run or over run.
     if ( midIndex - split < 0 )
        midIndex = 0 + split;
     else if ( midIndex + split >= totalPageCount )
        midIndex = (totalPageCount -1) - split;

     Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "].updateBufferFromSwap - Calculated mid index [" + midIndex +"]");

     // using the new calculated midIndex find the new beginning and end of the window
     // then create a mirror set of what the new window should look like
     int start = midIndex - split;
     int end = midIndex + split;
     newKeys.add(new Integer(midIndex) );

     for (int i = start; i <= end; i++ ) {
        newKeys.add(new Integer(i) );
     }

     // add all pages to a current set of pages.
     currentKeys.addAll( pages.keySet() );

     // find all the dirty pages in the page buffer
     // These are all pages which are no longer in the current window.
     // take the difference of the current keys - the new needed keys
     dirtyKeys.addAll(currentKeys);
     dirtyKeys.removeAll(newKeys);

     Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "].updateBufferFromSwap - Dirty buffer page count [" + dirtyKeys.size() +"], " + describeSet(dirtyKeys));

     // find the pages we will need to obtain from the swap;
     // take the difference of the new needed pages - current set of pages.
     newKeys.removeAll(currentKeys);

     Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "].updateBufferFromSwap - New page buffer count [" + newKeys.size() +"], " + describeSet(newKeys));


     // it there are no new pages to retrieve from the swap then return
     if (newKeys.isEmpty() )  {
        Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "].updateBufferFromSwap - Page Buffer needs no change, skipping update." );
        return;
     }
     // request a swap to buffer update.
     Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "].updateBufferFromSwap - Page Buffer needs update." );
     bufUpdater.update(newKeys, dirtyKeys);

  }

  /**
   * Retrieves the specified Page from the swap file.
   * @param index - The index of the page to retrieve.
   * @exception UnavailableResourceException thrown if resources are busy.
   * @param FrameworkException -thrown if swap file access fails.
   *
   */
  private Page retrieveFromSwap(int index) throws UnavailableResourceException, FrameworkException
  {
     Page aPage = null;

     int swapSize = getNumOfPages();
     int pageBufSize = getNumOfAvailPages();

     Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "].retrieveFromSwap - Trying to retrieve page [" + index + "] from swap." );


     // if the swap size is equal to the page buffer size
     // then return null since this indicates that
     // there is no swap file
     if (swapSize == pageBufSize)
        return null;

     try {
        swapLock.acquire(maxPageWaitTime);

           swapIOAccess.open();
           Iterator iter = swapIOAccess.getRecords(index * recordsPerPage, recordsPerPage);

           // create the page and add it to the page buffer
           XMLMessageGenerator gen = new XMLMessageGenerator("Page");
           aPage = createAddPage(index, gen, iter);
     } catch (InterruptedException ie ) {
        Debug.log(Debug.THREAD_WARNING, "PageList[" + pageKey+ "]: retrieveFromSwap() -Could not obtain swap lock: " + ie.getMessage() );
        String msg = NFLocale.getString(XrqConstants.XRQ_RESOURCE_CATALOG, XrqLanguageResource.RESOURCE_BUSY);
        throw new UnavailableResourceException(msg);
     } finally {
        swapLock.release();
     }

     return aPage;

  }

  /**
   * cleanup or destroy this PageList.
   * @param msecs - waits this number of msecs to obtain a lock on this pageList.
   * If a lock can not be obtained then cleanup on this PageList is skipped and should be tried again at a later time.
   */
  public void cleanup(long msecs) throws UnavailableResourceException, FrameworkException
  {
     try {
        pageLock.acquire(msecs);
        swapLock.acquire(msecs);

        if (isDestroyed) {
             Debug.log(Debug.MSG_WARNING, "PageList[" + pageKey+ "].cleanup(): was destroyed, skipping");
             return;
        }



        pages.clear();
        pages = null;

        // if the buffer updater thread is running
        // send it an interrupt
        // and tell it to stop.
        if ( bufUpdater != null ) {
           bufUpdater.interrupt();
           bufUpdater.killThread();
        }


        //since the swap creator thread maintain a lock on the swap we will not
        // be able to kill it, so let it die naturally.


        // clean up any file resources held by the swap object.
        if (swapIOAccess != null )
           swapIOAccess.cleanup();

        isDestroyed = true;

     } catch (InterruptedException ie ) {
        Debug.log(Debug.THREAD_WARNING, "PageList[" + pageKey+ "]: cleanUp() - Could not obtain page lock: " + ie.getMessage() );
        String msg = NFLocale.getString(XrqConstants.XRQ_RESOURCE_CATALOG, XrqLanguageResource.RESOURCE_BUSY);
        throw new UnavailableResourceException(msg);
     } catch (FrameworkException ie ) {
        String err = "PageList[" + pageKey + "]: cleanUp() - Could not clean up swap: " + ie.getMessage();
        Debug.log(Debug.IO_ERROR, err );
        //pull out into NFLocale resource
        throw new FrameworkException(err);
     } finally {
        pageLock.release();
        swapLock.release();
        Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "].cleanup(): was successfully destroyed.");
     }

  }

   /**
    * Describes the contents of the page list.
    * NOTE: This is is an expensive operation since it must obtain a lock, and do lots
    * of formatting. Use only as needed.
    */
   public String describe() {

      StringBuffer pageStr;

      int temp = getNumOfPages();

      String size;

      // if the number of pages is unknown set size to a question mark
      // otherwise get the numeric value.
      if ( temp == -1 )
         size = "?";
      else
         size = String.valueOf(temp);




      long delta = calculateIdleTime();
      float idle = (float)delta/(float)XrqConstants.MSEC_PER_SECOND;

      pageStr = new StringBuffer("PageList [" + pageKey+ "] Idle [" + idle + "] Pages(" + size +  "): [... ");

      try {
        pageLock.acquire();

            if (isDestroyed) {
             pageStr.append(" DESTROYED ");
           } else {

              Iterator iter = new TreeSet(pages.keySet()).iterator();

              while (iter.hasNext() ) {
                 int page = ((Integer)iter.next()).intValue();
                 if (page == curPageIndex )
                   pageStr.append("->");

                 pageStr.append("(");
                 pageStr.append(page);
                 pageStr.append(") ");
              }
           }

           pageStr.append("...]");


      } catch (InterruptedException ie ) {
        Debug.log(Debug.THREAD_ERROR, "PageList[" + pageKey+ "]: describe() - Could not obtain page lock: " + ie.getMessage() );
     } finally {
        pageLock.release();
     }

     return pageStr.toString();
  }

  /**
   * describes a set of Integers
   *
   */
  private String describeSet(Set aSet)
  {
     Iterator iter = aSet.iterator();
     StringBuffer buf = new StringBuffer();

        while (iter.hasNext() ) {
           int page = ((Integer)iter.next()).intValue();
              buf.append("(");
              buf.append(page);
              buf.append(") ");
        }

      return (buf.toString() );
   }

  /**
   * returns the key used to identify this PageList.
   */
  public String getIdentifier()
  {
     return pageKey;
  }





  //---------------------------- inner thread classes --------------------------------


  /**
   * Builds the swap file in a separate thread and then exits.
   * This allows records to be retrieved from the database in the background
   * while the client can look at the first set of pages.
   */
  private class SwapCreator extends Thread
  {

     private boolean failedToLock = false;
     private boolean started = false;

     public SwapCreator()
     {
        setName("SwapCreator_" + Thread.currentThread().getName() );
     }

     /**
      * Indicates if this thread failed to obtain a lock on the swap file.
      */
     public synchronized boolean failedToLock()
     {
        return failedToLock;
     }

     /**
      * indicates if this thread has started.
      */
     public synchronized boolean isStarted()
     {
        return started;
     }

     // main thread method
     public void run()
     {

        started = true;


        try {
           swapLock.acquire();

           if (Debug.isLevelEnabled(Debug.BENCHMARK) )
              Performance.startBenchmarkLog(getName() + "Swap Creator");

           Debug.log(Debug.THREAD_STATUS, "PageList[" + pageKey+ "] -Creating swap file.");
           swapIOAccess.open();
         

           swapIOAccess.buildSwap(rs);


        } catch (InterruptedException ie ) {
           Debug.error("PageList[" + pageKey+ "] -Could not obtain swap lock: " + ie.getMessage() );

           failedToLock = true;

        } catch (FrameworkException e) {
           Debug.error("PageList[" + pageKey+ "] : failed creating swap file: " +  e.getMessage() );
        } finally {


           //close file resources
           try {
             swapIOAccess.close();
           } catch (FrameworkException e) {
              Debug.error("PageList[" + pageKey+ "] : could not close swap resource: " + e.getMessage() );
           }

           // close db resources
           try {
             qExec.releaseResources();
           } catch (FrameworkException e) {
              Debug.error("PageList[" + pageKey+ "] : could not close QueryExecutor resource: " + e.getMessage() );
           }

           if (Debug.isLevelEnabled(Debug.BENCHMARK) )
              Performance.finishBenchmarkLog(getName() + "Swap Creator");

           swapLock.release();
        }


        swapSize = swapIOAccess.size();

     }
  }


  /**
    * This is a single thread which has the job of transfering
    * pages from swap to the page buffer. It is kicked off whenever a client
    * retrieves another page. It removes pages
    * that are no longer needed and adds new pages from the swap to the page buffer.
    */
  private class PageBufUpdater extends Thread
  {
     private boolean update = false;
     private SortedSet bufPagesToGet = new TreeSet();
     private Set bufDirtyKeys = new HashSet();

     private int waitTime;

     private boolean stopped = false;

     public PageBufUpdater(int waitTime)
     {
        setDaemon(true);
        setName("SwapToPageBuffer_" + Thread.currentThread().getName() );
        this.waitTime = waitTime;
     }

     /**
      * stops this thread.
      */
     public synchronized void killThread()
     {
        stopped = true;
        notify();
     }

     // main method
     public void run()
     {
           Debug.log(Debug.THREAD_LIFECYCLE, "PageList[" + pageKey+ "] -PageBufUpdater starting up.");

           // if this thread has been stopped
           // exit.
           while (!stopped) {
              if (update)
                 updateBuffer();
              try {
               synchronized(this) {
                   // waits for a specfied time and then loops.
                   // this is only done so that if there was a request for
                   // this thread to stop and it missed it, then it will exit after this time out.
                   //
                   // This will also catch any missed updates.
                   wait((long)pageExpiredTime);
                }
              } catch (InterruptedException i ) {
                 Debug.log(Debug.THREAD_WARNING, "PageBufUpdater interrupted, ignoring: " + i.getMessage() );
              }
           }

     }

     /**
      * indicates that an update to the page buffer should occur.

      * @param pagesToGet indicates the pages that need to be retrieved from the swap.
      *  They should be a sorted set of Integers.
      * @param dirtyKeys - A set of invalid pages in the page buffer.
      * NOTE: During the operation, the page buffer is first cleared of pages which exist in dirtyKeys.
      * Then pagesToGet are retrieved from the swap and added to the page buffer.
      *
      * This method returns right away, and the update occurs some time in the future.
      * If a update can not obtain a lock on the swap file or page buffer, then
      * the update is skipped (best effort).
      */
     public synchronized void update(Set pagesToGet, Set dirtyKeys )
     {

        // make a temporary class level copy of the pages to get and dirty pages
        // These pages are used at a later time to update the buffer

        bufPagesToGet.addAll(pagesToGet);
        bufDirtyKeys.addAll(dirtyKeys);

        update = true;
        //Debug.error("IN UPDATE: pagesToGet " + describeSet(this.pagesToGet) + ", dirty " + describeSet(this.dirtyKeys) );
        notify();
       
     }

     /**
      * At a later time this method is called (after update has been called).
      * It removes dirty pages from the page buffer ,and then loads needed
      * pages from the swap file into the page buffer.
      */
     private void updateBuffer()
     {

         long start = -1;

         if (Debug.isLevelEnabled(Debug.BENCHMARK) )  {
            start = System.currentTimeMillis();
         }

         // make a local copy
         Set tPagesToGet = new HashSet();
         Set tDirtyKeys = new HashSet();

         try {
           swapLock.acquire(waitTime/2);

           swapIOAccess.open();

           try {
              pageLock.acquire(waitTime/2);

              // copy the Sets so that if there is a update call in the middle
             // of this thread execution, it won't have a problem with the iterators.
             // since these sets are local to this method we only need synchronization
             // while copying the sets
             synchronized(this) {
                tPagesToGet.addAll(bufPagesToGet);
                tDirtyKeys.addAll(bufDirtyKeys);
                 //Debug.error("DURING UPDATE: pagesToGet " + describeSet(tPagesToGet) + ", dirty " + describeSet(tDirtyKeys) );
                bufDirtyKeys.clear();
                bufPagesToGet.clear();
                update = false;
                //Debug.error("DURING UPDATE2: pagesToGet " + describeSet(tPagesToGet) + ", dirty " + describeSet(tDirtyKeys) );
              }

              Iterator pagesIter = tPagesToGet.iterator();

               if (Debug.isLevelEnabled(Debug.MSG_STATUS) ) {
                  Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "]: Updating Page buffer before:" );
                  Debug.log(Debug.MSG_STATUS, describe());
               }

               // remove dirty pages from the page buffer
               if (pages.size() == tDirtyKeys.size() ) {
                  pages.clear();
                  Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "]: All pages Dirty, clearing.");
               }
               else {
                  Iterator it = tDirtyKeys.iterator();
                  while (it.hasNext() ) {
                     Integer key = (Integer)it.next();
                     pages.remove(key);
                     Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "]: Removing dirty page: " + key );
                  }
               }

               // loop over needed pages, retrieve the associated
               // records, and add the pages to the page buffer
               while ( pagesIter.hasNext() ) {
                  int pageIndex = ((Integer)pagesIter.next()).intValue();

                  Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "]: Adding new page: " + pageIndex );

                  Iterator recIter = swapIOAccess.getRecords(pageIndex * recordsPerPage, recordsPerPage);

                  XMLMessageGenerator gen = new XMLMessageGenerator("Page");
                   createAddPage(pageIndex, gen, recIter);
              }

              if (Debug.isLevelEnabled(Debug.MSG_STATUS) ) {
                  Debug.log(Debug.MSG_STATUS, "PageList[" + pageKey+ "]: Updating Page buffer after:" );
                  Debug.log(Debug.MSG_STATUS, describe());
               }

           } catch (InterruptedException e) {
              Debug.error(Thread.currentThread() +": Could not obtain page lock, skipping update: " +  e.getMessage() );
           } finally {
              pageLock.release();
           }


        } catch (InterruptedException ie ) {
           Debug.log(Debug.THREAD_WARNING, "PageList[" + pageKey+ "] -Could not obtain swap lock, skipping update: " + ie.getMessage() );
        } catch (FrameworkException e) {
           Debug.error("PageList[" + pageKey+ "]: - failed creating swap file: " +  e.getMessage() );
        } finally {
           try {
             swapIOAccess.close();
           } catch (FrameworkException e) {
              Debug.warning("PageList[" + pageKey+ "]: could not close swap file: " + e.getMessage() );
           }
           swapLock.release();
        }



         if (Debug.isLevelEnabled(Debug.BENCHMARK) ) {
            Debug.log(Debug.BENCHMARK, getName() + " Swap to page buffer Update.");
            Debug.log(Debug.BENCHMARK, getName() + " ELAPSED TIME: " +  ((double)(System.currentTimeMillis() - start))/ (double)XrqConstants.MSEC_PER_SECOND + "] seconds." );

         }
     }
  }




}