package com.nightfire.framework.xrq;

import java.util.*;
import com.nightfire.framework.resource.*;
import com.nightfire.framework.util.*;


import com.nightfire.framework.xrq.utils.*;

/**
 * Defines a class whom can establish a connection and obtain the results of
 * a query.
 * <B>IMPORTANT: That this class maintains state which makes it not thread safe if there is only a single
 * instance. Each thread needs a new instance of this class. </B>
 *
 * NOTE2: releaseResources must be called after the results of the executeQuery(...) method
 * are dealt with.
 *
 *
 */
public abstract class QueryExecutor
{

  /**
   * executes the query at the destination storage device. If there is an Exception
   * any acquired resources are freed.
   *
   * @param query - destination query which would usually be formatted by the QueryBuilder.
   *
   */
  public abstract void executeQuery(String query) throws FrameworkException, UnavailableResourceException;

  /**
   * returns the RecordSerializer containing the results of the query.
   *
   * @returns The results object which can be converted into an acceptable format.
   */
  public abstract RecordSerializer getResults();


  /**
   * releases any resources obtained by this class.
   * IMPORTANT: Since the results of the executor can be extracted in a separate thread, this method
   * MUST NOT be called by the creating class. The PageListCache and PageList classes handle calling this
   * method at the correct time.
   * NOTE: This also calls cleanup on the RecordSerializer.
   */

  protected abstract void releaseResources() throws FrameworkException;

}