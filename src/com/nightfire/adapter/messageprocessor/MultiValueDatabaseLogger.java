
package com.nightfire.adapter.messageprocessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.w3c.dom.Node;

import com.nightfire.adapter.util.DBMetaData;
import com.nightfire.adapter.util.TableMetaData;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.XMLMessageBase;
import com.nightfire.framework.message.util.xml.CachingXPathAccessor;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;


/**
 * This is a message-processor for logging multiple values of a column to the
 * database. It inserts a new row for each value of the multi valued column.
 * The columns that are to be inserted are specified in the persistent
 * properties configuration.
 *
 * Location for a multi valued field is specified using the existing
 * convention(separate multiple paths using "|" and separate each
 * path element using ".")
 *
 * Each location would be converted to the XPath expression and these
 * expressions are used to extract values from the input message.
 *
 *
 */
public class MultiValueDatabaseLogger extends DatabaseLogger
{

    /**
     * Property prefix indicating whether column value is multivalued or not.
     */
    public static final String MULTIVALUED_COLUMN_NAME_PROP  = "MULTIVALUED_COLUMN";

    /**
     * Constructor.
     */
    public MultiValueDatabaseLogger ( )
    {
        super();
        Debug.log( Debug.OBJECT_LIFECYCLE, "Creating MultiValued database-logger message-processor." );
            multiValuedColumns = new LinkedList();
    }

    /**
     * Initializes this object via its persistent properties.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        // Call base class method to load the properties.
        super.initialize( key, type );

        multiValuedField = getRequiredPropertyValue( MULTIVALUED_COLUMN_NAME_PROP );

        if (isvalidMultiValuedField())
        {
            Debug.log( Debug.SYSTEM_CONFIG, "Multivalued Column [" + multiValuedField + "]." );
        } else {
            throw new ProcessingException( "ERROR: MultiValued Column Name " +
                "[" + multiValuedField + "] should match one of the columns" );
        }

        Debug.log( Debug.SYSTEM_CONFIG, "MultiValueDatabaseLogger: Initialization done." );
    }

    /**
     * Check whether the given MULTIVALUED_COLUMN name is valid or not.
     *
     * @return	false if MULTIVALUED_COLUMN name doesnt match to any column
     * 			othrerwise return true.
     */
    private boolean isvalidMultiValuedField()
    {
        Iterator iter = columns.iterator();
        boolean valid = false;

        while (iter.hasNext())
        {
            ColumnData cd = (ColumnData) iter.next();
            if (cd.columnName.equals(multiValuedField))
            {
                multiValuedFieldIndex = columns.indexOf(cd);
                valid = true;
                break;
            }
        }
        return valid;
    }

    /**
     * Insert a single row into the database table using the given connection.
     *
     * @param  dbConn  The database connection to perform the SQL INSERT operation against.
     *
     * @exception  MessageException  Thrown on data errors.
     * @exception  ProcessingException  Thrown if processing fails.
    */
    protected void insert ( Connection dbConn ) throws MessageException, ProcessingException
    {
        // Make sure that at least one column value will be inserted.
        validate( );

        PreparedStatement pstmt = null;

        try
        {
            if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                Debug.log( Debug.MSG_STATUS, CustomerContext.getInstance().describe() );

            //Check if userid, interface version need to be logged or not to the table.
            currTableInfo = DBMetaData.getInstance().getTableMetaData( dbConn, tableName );

            if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                Debug.log( Debug.MSG_STATUS, currTableInfo.describe() );

            //We log to the table only if the CustomerContext has the data and the
            //table has the column.
            //InterfaceVersion check
            if ( (currTableInfo.existsInterfaceVersion()) && (CustomerContext.getInstance().getInterfaceVersion() != null ) )
                useInterfaceVersion = true;

            //UserId check.
            if ( (currTableInfo.existsUserId()) && (CustomerContext.getInstance().getUserID() != null ) )
                useUserId = true;

            //Create the SQL statement using the column data objects.
            String sqlStmt = constructSqlStatement( );


            // Get a prepared statement for the SQL statement.
            pstmt = dbConn.prepareStatement( sqlStmt );

            // Above code fragment for this method is copied from the
            // super class. The new code starts here.  This code would insert
            // multiple values for a multi valued field .

            int count = 0;

            // get the multivalued field.
            ColumnData mvField = (ColumnData)columns.get(multiValuedFieldIndex);

            Iterator iter = multiValuedColumns.iterator();

            while ( iter.hasNext() )
            {
                String str = (String)iter.next();

                if ( str == null )
                {
                    continue;
                }
                mvField.value = str;

                // Populate the SQL statement using values obtained from the column data objects.
                populateSqlStatement( pstmt );

                if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                	Debug.log( Debug.NORMAL_STATUS, "MultiValueDatabaseLogger: Executing SQL:\n" + sqlStmt );
                
                // Execute the SQL INSERT operation.
                count += pstmt.executeUpdate( );
                
                if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                	Debug.log( Debug.NORMAL_STATUS, "MultiValueDatabaseLogger: Finished Executing SQL...");

            }

            if ( count > 0 )
            {
            	if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            		Debug.log( Debug.NORMAL_STATUS, "MultiValueDatabaseLogger: Successfully inserted [" + count + "] row(s) into table ["
                    + tableName + "]" );
            }
        }
        catch ( SQLException sqle )
        {
            throw new ProcessingException( "ERROR: Could not insert row into database table [" + tableName
                                            + "]:\n" + DBInterface.getSQLErrorMessage(sqle) );
        }
        catch ( Exception e )
        {
            throw new ProcessingException( "ERROR: Could not insert row into database table [" + tableName
                                            + "]:\n" + e.toString() );
        }
        finally
        {
            if ( pstmt != null )
            {
                try
                {
                    pstmt.close( );
                }
                catch ( SQLException sqle )
                {
                    Debug.log( Debug.ALL_ERRORS, DBInterface.getSQLErrorMessage(sqle) );
                }
            }
        }
    }



    /**
     * Extract data for each column from the input message/context.
     *
     * @param context The message context.
     * @param inputObject The input object.
     *
     * @exception MessageException  thrown if required value can't be found.
     * @exception  ProcessingException  Thrown if processing fails.
     */
    protected void extractMessageData ( MessageProcessorContext context, MessageObject inputObject )
        throws MessageException, ProcessingException
    {
        super.extractMessageData(context, inputObject);

        ColumnData multiValueColumn = (ColumnData)columns.get(multiValuedFieldIndex);
        // If location was given, try to get a value from it.

        if ( StringUtils.hasValue( multiValueColumn.location ) )
        {
            // Location contains one or more alternative locations that could
            // contain the column's value.
            StringTokenizer st = new StringTokenizer( multiValueColumn.location, separator );

            // While location alternatives are available ...
            while ( st.hasMoreTokens() )
            {
                // Extract a location alternative.
                String loc = st.nextToken().trim( );

                // Replace "." with "/" to make it XPATH expresison.
                loc = "//" + loc.replaceAll("[.]", "/");

                // Attempt to get the value from the context or input object.
                // get all the matching nodes of the given input location.
                Node matchingNodes[] = getAllMatchingNodes(inputObject, loc);

                for (int i = 0; i < matchingNodes.length; i++)
                {
                    // get child of the base node.
                    Node childNode = matchingNodes[i];
                    String value = XMLMessageBase.getNodeAttributeValue(childNode,XMLMessageBase.VALUE_ATTR_NAME);
                    if (StringUtils.hasValue(value))
                    {
                        multiValuedColumns.add(value);
                    }
                }
            }

            // get the multivalued field.
            ColumnData mvField = (ColumnData)columns.get(multiValuedFieldIndex);

            if( multiValuedColumns.size() == 0 )
            {
                mvField.value = mvField.defaultValue;
                if ( mvField.value != null )
                {
                    Debug.log( Debug.MSG_DATA, "Using default value for column [" + mvField.columnName + "]." );
                        multiValuedColumns.add( mvField.defaultValue );
                }
            } else {
                // in some special cases where the "get()" method of
                // MessageObject class doesnt returns a  value for
                // multivalued field it is required to initialize some
                // non null value to multivalued field value otherwise
                // "constructSQLStatement" method would ignore the
                // multivalued field.
                // Here "multivalued" is just going to cause the code to
                // include the column.
                mvField.value = "multivalued";
            }

            // If no value can be obtained ...
            if ( mvField.value == null )
            {
                // If the value is required ...
                if ( mvField.optional == false )
                {
                    // Signal error to caller.
                    throw new MessageException( "ERROR: Could not locate required value for column [" + mvField.columnName
                                                + "], database table [" + tableName + "]." );
                }
                else  // No optional value is available, so just continue on.
                    Debug.log( Debug.MSG_DATA, "Skipping optional column [" + mvField.columnName + "] since no data is available." );
            }
        }
    }


    /**
     * Reset the column values in the list.
     */
    protected void resetColumnValues ( )
    {
        // reset all the db logger components.
        super.resetColumnValues();

        Debug.log (Debug.MSG_STATUS, "Removing multiple values for a column, if exists. " );

        Iterator iter = multiValuedColumns.iterator( );

        // While values are available for a multi valued column,
        // remove from beginning of the list...
        while ( iter.hasNext() )
        {
            multiValuedColumns.remove(0);
        }
    }


    /**
     * Returns all the child nodes of a node, represented by
     * the given inputLocation.
     *
     * @param inputObject The input object
     * @param inputLocation Input location for the values of multi valued field.
     *
     * @return return all the children of the node represented by input
     * 		   location or return an empty array of Node.
     *
     */
    private Node[] getAllMatchingNodes ( MessageObject inputObject, String inputLocation )
    {
        if(inputObject == null)
        {
            return null;
        }

        try
        {
            CachingXPathAccessor cc = new CachingXPathAccessor(inputObject.getDOM());
            return cc.getNodes(inputLocation);
        }
        catch (Exception e)
        {
            /* if nothing available on the given location,
             * just return an empty array of Node. */
            return new Node[0];
        }
    }

    private List multiValuedColumns;
    private String multiValuedField;
    private int multiValuedFieldIndex = -1;
}
