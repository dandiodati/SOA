package com.nightfire.framework.xrq.sql;

import com.nightfire.framework.xrq.*;
import java.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.locale.*;
import com.nightfire.framework.resource.*;

import com.nightfire.framework.xrq.utils.*;
import com.nightfire.framework.util.*;

import java.sql.*;


/**
 * This class is an sql implementation of a QueryExcecutor. Its Job is to take a sql query string
 * , obtain a connection to the DB, and return the results as a RecordSerializer.
 *
 *
 * IMPORTANT: This class maintains state which makes it not thread safe if multiple threads access the same
 * class instance. Each thread needs its own instance of this class.
 *
 *
 *
 */

public class SQLExecutor extends QueryExecutor
{


  private Connection conn;

  private int maxdbWaitTime;

  private boolean released = false;

  private SQLResultSetSerializer serializer;
  private PreparedStatement preparedStat;

  /*
   * initializes this QueryExecutor. 
   */
  public SQLExecutor(int maxdbWaitTime) throws FrameworkException
  {
     this.maxdbWaitTime = maxdbWaitTime;
  }


  public void executeQuery(String query) throws UnavailableResourceException, FrameworkException
  {


    long start =0;
    
      if ( Debug.isLevelEnabled(Debug.BENCHMARK) )
         start = System.currentTimeMillis();

    try {
      conn = DBConnectionPool.getInstance().acquireConnection(maxdbWaitTime);
      if (conn == null)
         throw new ResourceException("Got a null DB connection");

      preparedStat = conn.prepareStatement(query );
      ResultSet set = preparedStat.executeQuery();

      serializer = new SQLResultSetSerializer(set, NFLocale.getDateTimeFormat());

    } catch (ResourceException re) {
       Debug.log(Debug.MSG_STATUS,"SQLExecutor: " + re.getMessage() );
        String msg = NFLocale.getString(XrqConstants.XRQ_RESOURCE_CATALOG, XrqLanguageResource.RESOURCE_BUSY);
        throw new UnavailableResourceException(msg);
    } catch (FrameworkException re) {
       String error ="SQLExecutor: Failed to init serializer " + re.getMessage();
       Debug.log(Debug.MSG_STATUS,error );
       try {
          releaseResources();
       } catch (FrameworkException e) {
          Debug.warning("SQLExecutor: Could not release resource:" + e.getMessage() );
       }
       throw new FrameworkException(error);
    } catch (SQLException se) {
       String error = "SQLExecutor: DB error occurred: " + DBInterface.getSQLErrorMessage(se);
       Debug.error(error);
       try {
          releaseResources();
       } catch (FrameworkException e) {
          Debug.warning("SQLExecutor: Could not release resource:" + e.getMessage() );
       }

       throw new FrameworkException(error);
    }

    if ( Debug.isLevelEnabled(Debug.BENCHMARK) )
              Debug.log(Debug.BENCHMARK,"SQL EXECUTION TIME: [" + ((double)(System.currentTimeMillis() - start))/ (double)XrqConstants.MSEC_PER_SECOND + "] seconds.");


  }

  /**
   * returns the results of a query.
   * NOTE: The execute method has to be called first, or a null object will be returned.
   * @return SQLResultSetSerializer The class the handles serializing the records.
   */
  public RecordSerializer getResults()
  {
     return serializer;
  }

  /**
   * frees up an resources acquired by this class. This should be called after all records
   * are retrieved from the SQLResultSetSerializer.
   */
  protected void releaseResources() throws FrameworkException
  {

      if (Debug.isLevelEnabled(Debug.MSG_STATUS) )
         Debug.log(Debug.MSG_STATUS,"SQLExecutor: Trying to free up db resources.");
         
      try {
        synchronized (this) {
          if ( conn != null ) {

            if ( serializer != null )
               serializer.cleanup();

            if (preparedStat != null)
               preparedStat.close();

            DBConnectionPool.getInstance().releaseConnection(conn);
            conn = null;
          } else
             Debug.log(Debug.MSG_STATUS,"SQLExecutor: Connection never established, so there are no resources to free");
        }
      } catch (SQLException se) {
         String err = "SQLExecutor: Failed to close sql statement : " + DBInterface.getSQLErrorMessage(se);
         Debug.error(err);
         throw new FrameworkException(err);
      }  catch (Exception se) {
         String err = "SQLExecutor: Failed to close db connection : " + se.toString();
         Debug.error(err);
         throw new FrameworkException(err);
      }


  }


}