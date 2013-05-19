package com.nightfire.adapter.messageprocessor;

import java.util.*;
import java.sql.*;
import java.text.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;

/**
 * This is a generic message-processor for deleting the records in the database. All the
 * columns that are to be deleted are specified in the SQL_SET_STATEMENT in persistent property
 * column to eb used in the where clause are specified in the SQL_WHERE_STATEMENT in persistent property
 */

public class DBDelete
    extends DBMessageProcessorBase {
  /**
   * Property indicating name of the database table to delete row into.
   */
  public static final String TABLE_NAME_PROP = "TABLE_NAME";
  public static final String SQL_WHERE_STATEMENT_PROP = "SQL_WHERE_STATEMENT";
  /**
   * Property indicating whether SQL operation should be part of overall driver transaction.
   */
  public static final String TRANSACTIONAL_LOGGING_PROP =
      "TRANSACTIONAL_LOGGING";
  /**
   * Property indicating separator token used to separate individual location alternatives.
   */
  public static final String LOCATION_SEPARATOR_PROP = "SEPARATOR";

  /**
   * Property prefix giving column data type.
   */
  public static final String WHERE_COLUMN_TYPE_PREFIX_PROP =
      "WHERE_COLUMN_TYPE";

  /**
   * Property prefix giving date format for date types.
   */
  public static final String WHERE_DATE_FORMAT_PREFIX_PROP =
      "WHERE_DATE_FORMAT";

  /**
   * Property prefix giving location of column value.
   */
  public static final String WHERE_LOCATION_PREFIX_PROP = "WHERE_LOCATION";

  /**
   * Property prefix giving default value for column.
   */
  public static final String WHERE_DEFAULT_PREFIX_PROP = "WHERE_DEFAULT";

  // Types of supported columns that require special processing.
  public static final String COLUMN_TYPE_STRING = "STRING";
  public static final String COLUMN_TYPE_DATE = "DATE";

  // Token indicating that current date/time should be used for date field values.
  public static final String SYSDATE = "SYSDATE";

  // Token value for LOCATION indicating that the entire contents of the message-
  // processor's input message-object should be used as the column value.
  public static final String PROCESSOR_INPUT = "PROCESSOR_INPUT";

  public static final String SEPARATOR = "|";

  /**
   * Property indicating whether customer criteria should be added or not to the SQL statement.
   * If not set, default value is true.
   */
  public static final String USE_CUSTOMER_ID_PROP = "USE_CUSTOMER_ID";

  /**
     * Property indicating whether SubDomain criteria should be added or not to the SQL statement.
     * If not set, default value is false.
     */
    public static final String USE_SUBDOMAIN_ID_PROP = "USE_SUBDOMAIN_ID";

  /**
   * Constructor.
   */
  public DBDelete() {
    Debug.log(Debug.OBJECT_LIFECYCLE, "Creating DBDelete message-processor.");

    columns = new LinkedList();
  }

  /**
   * Initializes this object via its persistent properties.
   *
   * @param  key   Property-key to use for locating initialization properties.
   * @param  type  Property-type to use for locating initialization properties.
   *
   * @exception ProcessingException when initialization fails
   */
  public void initialize(String key, String type) throws ProcessingException {
    // Call base class method to load the properties.
    super.initialize(key, type);

    // Get configuration properties specific to this processor.
    
    if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
        Debug.log(Debug.SYSTEM_CONFIG, "DBDelete: Initializing...");

    errorBuffer = new StringBuffer();

    tableName = getRequiredPropertyValue(TABLE_NAME_PROP, errorBuffer);

    if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
      Debug.log(Debug.SYSTEM_CONFIG,
                "Database table to delete rows from is [" + tableName + "].");
    }

    String strTemp = getPropertyValue(TRANSACTIONAL_LOGGING_PROP);

    if (StringUtils.hasValue(strTemp)) {
      try {
        usingContextConnection = getBoolean(strTemp);
      }
      catch (FrameworkException e) {
        errorBuffer.append("Property value for " + TRANSACTIONAL_LOGGING_PROP +
                           " is invalid. " + e.getMessage() + "\n");
      }

    } //if

    if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
      Debug.log(Debug.SYSTEM_CONFIG,
                "Logger will participate in overall driver transaction? ["
                + usingContextConnection + "].");
    }

    separator = getPropertyValue(LOCATION_SEPARATOR_PROP);

    if (!StringUtils.hasValue(separator)) {
      separator = SEPARATOR;

    }
    if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
      Debug.log(Debug.SYSTEM_CONFIG,
                "Location separator token [" + separator + "].");
    }

    sqlWhereStmt = getPropertyValue(SQL_WHERE_STATEMENT_PROP);
    // save copy of original SQL "where" stmt
    originalSQLWhereStmt = sqlWhereStmt;

    if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
      Debug.log(Debug.SYSTEM_CONFIG,
                "'where part of' SQL query is [" + sqlWhereStmt + "].");
    }
    populateColumnData();

    strTemp = getPropertyValue(USE_CUSTOMER_ID_PROP);

    if (StringUtils.hasValue(strTemp)) {
      try {
        useCustomerId = getBoolean(strTemp);
      }
      catch (FrameworkException e) {
        errorBuffer.append("Property value for " + USE_CUSTOMER_ID_PROP +
                           " is invalid. " + e.getMessage() + "\n");
      }
    }

    if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
      Debug.log(Debug.SYSTEM_CONFIG,
                "To use customer id in SQL statement?[" + useCustomerId + "].");
    }

      strTemp = getPropertyValue( USE_SUBDOMAIN_ID_PROP );

      if (StringUtils.hasValue(strTemp)) {
        try {
          useSubDomainId = getBoolean(strTemp);
        }
        catch (FrameworkException e) {
          errorBuffer.append("Property value for " + USE_SUBDOMAIN_ID_PROP +
                             " is invalid. " + e.getMessage() + "\n");
        }
      }

      if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
        Debug.log(Debug.SYSTEM_CONFIG,
                  "To use SubDomain id in SQL statement?[" + useSubDomainId + "].");
      }

      if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
        Debug.log(Debug.SYSTEM_CONFIG, "DBDelete: Initialization done.");
  }

  /**
   * This method populates the list fo columndata
   *
   * @exception ProcessingException when initialization fails
   **/

  private void populateColumnData() throws ProcessingException {
    // Loop until all SetColumn configuration properties have been read ...
    int count = -1;
    int index = 0;

    if (sqlWhereStmt == null) { // no WHERE clause to be appended
      // If any of the required properties are absent, indicate error to caller.
      if (errorBuffer.length() > 0) {
        String errMsg = errorBuffer.toString();
        Debug.log(Debug.ALL_ERRORS, errMsg);
        throw new ProcessingException(errMsg);
      }
      return;
    }
    // Loop until all WhereColumn configuration properties have been read ...
    count = -1;
    index = 0;
    // to find the number of Column values to be filled in WHERE clause...this is counted by number of ? in the string
    while (index != -1) {
      count++;
      index = sqlWhereStmt.indexOf("?", index + 1);
    }

    for (int Ix = 0; true; Ix++) {
      String location = getPropertyValue(PersistentProperty.
                                         getPropNameIteration(
          WHERE_LOCATION_PREFIX_PROP, Ix));
      String defaultValue = getPropertyValue(PersistentProperty.
                                             getPropNameIteration(
          WHERE_DEFAULT_PREFIX_PROP, Ix));

      //String optional = getPropertyValue( PersistentProperty.getPropNameIteration( WHERE_OPTIONAL_PREFIX_PROP, Ix ));
      // If we can't find default or location then , we're done.
      if (!StringUtils.hasValue(location) && !StringUtils.hasValue(defaultValue)) {
        break;
      }

      String colType = getPropertyValue(PersistentProperty.getPropNameIteration(
          WHERE_COLUMN_TYPE_PREFIX_PROP, Ix));
      String dateFormat = getPropertyValue(PersistentProperty.
                                           getPropNameIteration(
          WHERE_DATE_FORMAT_PREFIX_PROP, Ix));
      try {
        // Create a new column data object and add it to the list.
        ColumnData cd = new ColumnData(colType, dateFormat, location,
                                       defaultValue, "FALSE");
        
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log(Debug.SYSTEM_CONFIG, cd.describe());
        
        columns.add(cd);
      }
      catch (Exception e) {
        throw new ProcessingException(
            "ERROR: Could not create column data description:\n"
            + e.toString());
      }
    }
    // If any of the required properties are absent, indicate error to caller.
    if (errorBuffer.length() > 0) {
      String errMsg = errorBuffer.toString();
      Debug.log(Debug.ALL_ERRORS, errMsg);
      throw new ProcessingException(errMsg);
    }
  }

  /**
   * Extract data values from the context/input, and use them to
   * delete a row into the configured database table.
   *
   * @param  mpContext The context
   * @param  inputObject  Input message to process.
   *
   * @return  The given input, or null.
   *
   * @exception  ProcessingException  Thrown if processing fails.
   * @exception  MessageException  Thrown if message is bad.
   */
  public NVPair[] process(MessageProcessorContext mpContext,
                          MessageObject inputObject) throws MessageException,
      ProcessingException {
    if (inputObject == null) {
      return null;
    }

    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        Debug.log(Debug.MSG_STATUS, "DBDelete: processing ... ");

    // Reset the values in cases where we're logging again.
    resetColumnValues();

    // Reset where clause in case we're logging again.
    sqlWhereStmt = originalSQLWhereStmt;

    // Extract the column values from the arguments.
    extractMessageData(mpContext, inputObject);

    Connection dbConn = null;

    try {
      // Get a database connection from the appropriate location - based on transaction characteristics.
      if (usingContextConnection) {
          
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))  
            Debug.log(Debug.MSG_STATUS,
                "Database delete is transactional, so getting connection from context.");

        dbConn = mpContext.getDBConnection();
      }
      else {
          
       if(Debug.isLevelEnabled(Debug.MSG_STATUS))  
        Debug.log(Debug.MSG_STATUS,
            "Database delete is not transactional, so getting connection from pool.");

        dbConn = DBConnectionPool.getInstance().acquireConnection();
      }

      // delete the data to the database.
      delete(dbConn);

      // If the configuration indicates that this SQL operation isn't part of the overall driver
      // transaction, commit the changes now.
      if (!usingContextConnection) {
          
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))  
            Debug.log(Debug.MSG_STATUS, "Committing data deleted in database.");

        DBConnectionPool.getInstance().commit(dbConn);
      }
    }
    catch (Exception e) {
      String errMsg =
          "ERROR: DBDelete: Attempt to delete database rows failed with error: "
          + e.getMessage();

      Debug.log(Debug.ALL_ERRORS, errMsg);

      // If the configuration indicates that this SQL operation isn't part of the overall driver
      // transaction, roll back any changes now.
      if (!usingContextConnection) {
        Debug.log(Debug.MSG_STATUS,
                  "Rolling-back any database changes due to database-delete.");

        try {
          DBConnectionPool.getInstance().rollback(dbConn);
        }
        catch (ResourceException re) {
          Debug.log(Debug.ALL_ERRORS, re.getMessage());
        }
      }

      // Re-throw the exception to the driver.
      throw new ProcessingException(errMsg);
    }
    finally {
      // If the configuration indicates that this SQL operation isn't part of the overall driver
      // transaction, return the connection previously acquired back to the resource pool.
      if (!usingContextConnection && (dbConn != null)) {
        try {
          DBConnectionPool.getInstance().releaseConnection(dbConn);
        }
        catch (Exception e) {
          Debug.log(Debug.ALL_ERRORS, e.toString());
        }
      }
    }

    // Always return input value to provide pass-through semantics.
    return (formatNVPair(inputObject));
  }

  /**
   * delete into the database table using the given connection.
   *
   * @param  dbConn  The database connection to perform the SQL delete operation against.
   *
   * @exception  ProcessingException  Thrown if processing fails.
   */
  private void delete(Connection dbConn) throws ProcessingException {
    //validate();
    PreparedStatement pstmt = null;

    try {
      // Create the SQL statement using the column data objects.
      String sqlStmt = "DELETE FROM " + tableName + " ";
      if (sqlWhereStmt != null) {
        sqlStmt = sqlStmt + " WHERE " + sqlWhereStmt;
      }
      if (useCustomerId) {
        //Add customer information
        sqlStmt = SQLBuilder.addCustomerIDCondition(sqlStmt);
      }

      if (useSubDomainId) {
        //Add SubDomain information
        sqlStmt = SQLBuilder.addSubDomainIDCondition (sqlStmt);
      }

      if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
        Debug.log(Debug.NORMAL_STATUS, "DBDelete : Executing SQL:\n" + sqlStmt);

      // Get a prepared statement for the SQL statement.
      pstmt = dbConn.prepareStatement(sqlStmt);

      // Populate the SQL statement using values obtained from the column data objects.
      populateSqlStatement(pstmt);

      // Execute the SQL delete operation.
      int count = pstmt.executeUpdate();

      if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
        Debug.log(Debug.NORMAL_STATUS,
                "DBDelete : Finished Executing SQL.. Successfully deleted [" + count + "] row(s) into table ["
                + tableName + "]");

      // Check that the number of database rows deleted was acceptable, based on configuration.
      checkSQLOperationResultCount(count);
    }
    catch (SQLException sqle) {
      throw new ProcessingException(
          "ERROR: Could not delete row from database table [" + tableName
          + "]:\n" + DBInterface.getSQLErrorMessage(sqle));
    }
    catch (Exception e) {
      throw new ProcessingException(
          "ERROR: Could not delete rows from database table [" + tableName
          + "]:\n" + e.toString());
    }
    finally {
      if (pstmt != null) {
        try {
          pstmt.close();
        }
        catch (SQLException sqle) {
          Debug.log(Debug.ALL_ERRORS, DBInterface.getSQLErrorMessage(sqle));
        }
      }
    }
  }

  /**
   * Populate the SQL delete statement from the column data.
   *
   * @param  pstmt  The prepared statement object to populate.
   *
   * @exception Exception  thrown if population fails.
   */
  private void populateSqlStatement(PreparedStatement pstmt) throws Exception {
      
    if(Debug.isLevelEnabled(Debug.DB_DATA))   
        Debug.log(Debug.DB_DATA, "Populating SQL delete statement ...");

    int Ix = 1; // First value slot in prepared statement.

    Iterator iter = columns.iterator();

    for (Ix =1;iter.hasNext();Ix++) {
      ColumnData cd = (ColumnData) iter.next();
      
      if(Debug.isLevelEnabled(Debug.DB_DATA))
      Debug.log(Debug.DB_DATA, "Populating prepared-statement slot ["+Ix+"] with column data ." +
                cd.describe());
      if (cd.value == null) {
        // will this work for all data types??
        pstmt.setNull(Ix, java.sql.Types.VARCHAR);
        continue;
      }

      // Default is no column type specified.
      if (!StringUtils.hasValue(cd.columnType)) {
        pstmt.setObject(Ix, cd.value);
      }
      else if (cd.columnType.equalsIgnoreCase("String")) {
        pstmt.setString(Ix, cd.value.toString());
      }
      else if (cd.columnType.equalsIgnoreCase(COLUMN_TYPE_DATE)) {
        String val = (String) (cd.value);
        if (val.equalsIgnoreCase(SYSDATE)) {
          //get current date to set a value for this column
          pstmt.setTimestamp(Ix,
                             new java.sql.Timestamp(DateUtils.
              getCurrentTimeValue()));
          continue;
        }
        // If we're not deleteing the current system date, the caller must
        // have provided an actual date value, which we must now parse.
        if (!StringUtils.hasValue(cd.dateFormat)) {
          throw new FrameworkException(
              "ERROR: Configuration for date column does not specify date format.");
        }

        SimpleDateFormat sdf = new SimpleDateFormat(cd.dateFormat);

        java.util.Date d = sdf.parse(val);

        pstmt.setTimestamp(Ix, new java.sql.Timestamp(d.getTime()));
      }
      else {
        throw new FrameworkException(
            "ERROR: Invalid column-type property value ["
            + cd.columnType + "] given in configuration.");
      }

      // Advance to next value slot in prepared statement.
    }

    if (useCustomerId) {
      //Use Ix slot number as-is, and incrementing to be used in next loop.
      SQLBuilder.populateCustomerID(pstmt, Ix++);
    }

      if (useSubDomainId) {
        //Use Ix slot number as-is, because it was already incremented before exiting above loop.
        SQLBuilder.populateSubDomainID (pstmt, Ix);
    }

    Debug.log(Debug.DB_DATA, "Done populating SQL DELETE statement.");

  }

  /**
   * Extract data for each column from the input message/context.
   *
   * @param context The message context.
   * @param inputObject The input object.
   *
   * @exception MessageException  thrown if required value can't be found.
       * @exception ProcessingException thrown if any other processing error occurs.
   */
  private void extractMessageData(MessageProcessorContext context,
                                  MessageObject inputObject) throws
      ProcessingException, MessageException {
      
    if(Debug.isLevelEnabled(Debug.MSG_STATUS))  
        Debug.log(Debug.MSG_STATUS, "Extracting message data to delete ...");

    Iterator iter = columns.iterator();

    // While columns are available ...
    while (iter.hasNext()) {
      ColumnData cd = (ColumnData) iter.next();

      // If location was given, try to get a value from it.
      if (StringUtils.hasValue(cd.location)) {
        // Location contains one or more alternative locations that could
        // contain the column's value.
        StringTokenizer st = new StringTokenizer(cd.location, separator);

        // While location alternatives are available ...
        while (st.hasMoreTokens()) {
          // Extract a location alternative.
          String loc = st.nextToken().trim();

          // If the value of location indicates that the input message-object's
          // entire value should be used as the column's value, extract it.
          if (loc.equalsIgnoreCase(PROCESSOR_INPUT)) {
              
           if(Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log(Debug.MSG_DATA,
                      "Using message-object's contents as the column's value.");

            cd.value = inputObject.get();

            break;
          }

          // Attempt to get the value from the context or input object.
          if (exists(loc, context, inputObject)) {
            cd.value = get(loc, context, inputObject);

            // If we found a value, we're done with this column.
          }
          if (cd.value != null) {
              
           if(Debug.isLevelEnabled(Debug.MSG_DATA))  
            Debug.log(Debug.MSG_DATA,
                      "Found value for column [" + cd.describe() +
                      "] at location [" +
                      loc + "].");

            break;
          }
        }
      }

      // If no value was obtained from location in context/input, try to set it from default value (if available).
      if (cd.value == null) {
        cd.value = cd.defaultValue;

        // If no value can be obtained ...
      }
      if (cd.value == null) {
        // If the value is required ...
        if (cd.optional == false) {
          // Signal error to caller.
          throw new ProcessingException(
              "ERROR: Could not locate required value for column [" +
              cd.describe()
              + "], database table [" + tableName + "].");
        }
        else { // No optional value is available, so putting 'NULL'.
          cd.value = null;
        }
      }
    }

    Debug.log(Debug.MSG_STATUS, "Done extracting message data to delete.");
  }

  /**
   * Reset the column values in the list.
   */
  private void resetColumnValues() {
      
    if(Debug.isLevelEnabled(Debug.MSG_STATUS))  
        Debug.log(Debug.MSG_STATUS, "Resetting column values ...");

    Iterator iter = columns.iterator();

    // While columns are available ...
    while (iter.hasNext()) {
      ColumnData cd = (ColumnData) iter.next();

      cd.value = null;
    }
  }

  /**
   * Check that columns were configured and at least one
   * field has a value to delete.
   *
   * @exception ProcessingException  thrown if invalid
   */
  private void validate() throws ProcessingException {
    Debug.log(Debug.MSG_STATUS, "Validating column data ...");

    if (columns.size() < 1) {
      throw new ProcessingException(
          "ERROR: No database column values are available to write to ["
          + tableName + "].");
    }

    Debug.log(Debug.DB_DATA, "Done validating column data.");
  }

  private String tableName = null;
  private String separator = null;
  private boolean usingContextConnection = true;
  private List columns;
  private String sqlSetStmt = null;
  private String sqlWhereStmt = null;
  private String originalSQLWhereStmt = null;
  StringBuffer errorBuffer = null;
  private boolean useCustomerId = true;
  private boolean useSubDomainId = false;

  /**
   * Class ColumnData is used to encapsulate a description of a single column
   * and its associated value.
   */
  private static class ColumnData {
    public final String columnType;
    public final String dateFormat;
    public final String location;
    public final String defaultValue;
    public final boolean optional;

    public Object value = null;

    public ColumnData(String columnType, String dateFormat,
                      String location, String defaultValue, String optional) throws
        FrameworkException {
      this.columnType = columnType;
      this.dateFormat = dateFormat;
      this.location = location;
      this.defaultValue = defaultValue;
      this.optional = StringUtils.getBoolean(optional);
    }

    public String describe() {
      StringBuffer sb = new StringBuffer();

      sb.append("Column description: ");

      if (StringUtils.hasValue(columnType)) {
        sb.append("Type [");
        sb.append(columnType);
      }

      if (StringUtils.hasValue(dateFormat)) {
        sb.append("], date-format [");
        sb.append(dateFormat);
      }

      if (StringUtils.hasValue(location)) {
        sb.append("], location [");
        sb.append(location);
      }

      if (StringUtils.hasValue(defaultValue)) {
        sb.append("], default [");
        sb.append(defaultValue);
      }

      sb.append("], optional [");
      sb.append(optional);

      if (value != null) {
        sb.append("], value [");
        sb.append(value);
      }

      sb.append("].");

      return (sb.toString());
    }
  }

  public static void main(String[] args) {
    Debug.enableAll();
    Debug.showLevels();
    if (args.length != 5) {
      Debug.log(Debug.ALL_ERRORS, "DBDelete: USAGE:  " +
                " jdbc:oracle:thin:@host:1521:sid user password key type");
      return;
    }
    try {

      DBInterface.initialize(args[0], args[1], args[2]);
    }
    catch (DatabaseException e) {
      Debug.log(null, Debug.MAPPING_ERROR, "DBDelete: " +
                "Database initialization failure: " + e.getMessage());
    }

    DBDelete bl = new DBDelete();
    try {
      bl.initialize(args[3], args[4]);

      HashMap m = new HashMap();
      //m.put("recordtype", "SOIFile");
      MessageObject msg = new MessageObject(m);
      MessageProcessorContext mpc = new MessageProcessorContext();
      mpc.set("lockExpiryPeriod", "30");
      //mpc.set("Status", "Faxed");
      bl.process(mpc, msg);

    }
    catch (Exception pe) {
      System.out.println("FAILED IN MAIN OF DBDelete:" + pe.getClass());
    }
  } //end of main*/

//*************************************************

}
