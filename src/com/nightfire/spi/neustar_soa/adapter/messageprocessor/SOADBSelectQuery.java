/**
 * A generic message-processor for extracting row values from the database
 * via the execution of a configurable SQL SELECT statmement.
 * 
 * @author Ashok Kumar
 * @version 1.1
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see			com.nightfire.adapter.messageprocessor.DBMessageProcessorBase
 * @see			com.nightfire.framework.util.Debug
 * @see			com.nightfire.framework.util.NVPair
 * @see			com.nightfire.framework.db.DBConnectionPool
 * @see			com.nightfire.framework.db.SQLBuilder
 * @see			com.nightfire.framework.db.DBInterface
 * @see			com.nightfire.framework.util.StringUtils
 * @see			com.nightfire.spi.common.driver.MessageObject
 * @see			com.nightfire.framework.message.generator.xml.XMLMessageGenerator
 * @see			com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer
 * 
 */

/** 
	Revision History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Ashok			10/05/2004			Created	

 */

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;


import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;


import org.w3c.dom.Document;

import com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.db.SQLBuilder;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.monitor.ThreadMonitor;


public class SOADBSelectQuery extends DBMessageProcessorBase
{
	
	/**
	 * Variable contains value of Table name
	 */	
	private String tableName = null;
	
	/**
	 * Variable contains sql where clause
	 */
	private String sqlStmt = null;
	
	/**
	 * Variable contains original sql where clause
	 */
	private String originalSQLStmt = null;
	
	/**
	 * Variable contains single result flag value
	 */
	private boolean singleResultFlag = true;
	
	/**
	 * Variable contains value of transaction logging
	 */
	private boolean usingContextConnection = true;
	
	/**
	 * Variable contains output date format
	 */
	private String outputDateFormat;
	/**
	 * Variable contains value of Seperator
	 */
	private String separator = null;
	
	/**
	 * Variable contains location of result
	 */
	private String resultLocation;
	
	/**
	 * Variable contains value of useCustomerID flag
	 */
	private boolean useCustomerId = true;
	
	/**
	 * Variable contains value of customerIdNamespace
	 */
	private String customerIdNamespace = null;
    
	/**
	 * Variable contains columndata objects
	 */
	private List columns;	
    
	/**
     * Constructor.
     */
    public SOADBSelectQuery ( )
    {
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) ){
			Debug.log( Debug.OBJECT_LIFECYCLE, "Creating soa database-select-" +
												"query message-processor." );
		}

        columns = new ArrayList( );
        
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
        
        // Get configuration properties specific to this processor.
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
          Debug.log( Debug.SYSTEM_CONFIG, "SOADBSelectQuery: Initializing..." );
		}

        StringBuffer errorBuffer = new StringBuffer( );
        
        tableName = getRequiredPropertyValue( SOAConstants.TABLE_NAME_PROP, 
        									  errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, 
					"Database table to query against is [" + tableName + "]." );
		}

        sqlStmt = getRequiredPropertyValue( 
        				SOAConstants.SQL_QUERY_STATEMENT_PROP, errorBuffer );

        // keep a copy of the unadulterated (no CustomerId clause)
        // SQL statement
        originalSQLStmt = sqlStmt;
        if( Debug.isLevelEnabled( Debug.NORMAL_STATUS ) ){
          Debug.log( Debug.NORMAL_STATUS, "SQL query is [" + sqlStmt + "]." );
		}

        resultLocation = getRequiredPropertyValue( 
        					SOAConstants.RESULT_LOCATION_PROP, errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, 
					"Location to store result into [" + resultLocation + "]." );
		}

        String strTemp = getPropertyValue( 
        							SOAConstants.SINGLE_RESULT_FLAG_PROP );

        if ( StringUtils.hasValue( strTemp ) )
        {
            
            try 
            {
                
                singleResultFlag = getBoolean( strTemp );
                
            } catch ( FrameworkException e ) {
                
                errorBuffer.append ( "Property value for " + 
                SOAConstants.SINGLE_RESULT_FLAG_PROP +
                " is invalid. " + e.getMessage ( ) + "\n" );
                
            }
            
        }
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Single query result expected? [" 
												+ singleResultFlag + "]." );
		}

        strTemp = getPropertyValue( SOAConstants.TRANSACTIONAL_LOGGING_PROP );

        if ( StringUtils.hasValue( strTemp ) )
        {
            
            try 
            {
            
                usingContextConnection = getBoolean( strTemp );
                
            } catch ( FrameworkException e ) {
                
                errorBuffer.append ( "Property value for " + 
                SOAConstants.TRANSACTIONAL_LOGGING_PROP +
                " is invalid. " + e.getMessage ( ) + "\n" );
                
            }
        }
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Database-select-query will " +
							"participate in overall driver transaction? [" 
							+ usingContextConnection + "]." );
		}

        outputDateFormat = getPropertyValue( 
        							SOAConstants.OUTPUT_DATE_FORMAT_LOC_PROP );

        separator = getPropertyValue( SOAConstants.LOCATION_SEPARATOR_PROP );
        
        if ( !StringUtils.hasValue( separator ) )
        {
        
            separator = SOAConstants.DEFAULT_LOCATION_SEPARATOR;
            
        }
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, 
							"Location separator token [" + separator + "]." );
		}


        // Loop until all column configuration properties have been read ...
        for ( int Ix = 0;  true;  Ix ++ )
        {
            String colType = getPropertyValue( 
            						PersistentProperty.getPropNameIteration( 
            						SOAConstants.COLUMN_TYPE_PREFIX_PROP, Ix ) );

            String dateFormat = getPropertyValue( 
            				PersistentProperty.getPropNameIteration( 
            				SOAConstants.INPUT_DATE_FORMAT_PREFIX_PROP, Ix ) );

            String location = getPropertyValue( 
            						PersistentProperty.getPropNameIteration( 
            						SOAConstants.LOCATION_PREFIX_PROP, Ix ) );

            String defaultValue = getPropertyValue( 
            						PersistentProperty.getPropNameIteration( 
            						SOAConstants.DEFAULT_PREFIX_PROP, Ix ) );

            if ( !StringUtils.hasValue( colType ) 
            	&& !StringUtils.hasValue( dateFormat ) 
                && !StringUtils.hasValue(location) 
                && !StringUtils.hasValue(defaultValue) )
            {
            
                break;
                
            }

            try
            {
                // Create a new column data object and add it to the list.
                ColumnData cd = new ColumnData( colType, dateFormat, 
                								location, defaultValue );

                if ( Debug.isLevelEnabled(Debug.SYSTEM_CONFIG) )
                {                
                    Debug.log( Debug.SYSTEM_CONFIG, cd.describe() );
                }
                
                columns.add( cd );
                
            }
            catch ( FrameworkException e )
            {
                throw new ProcessingException( "ERROR: Could not create" +
                				" column data description:\n" + e.toString() );
            }
            
        }
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Number of columns participating" +
						" in dynamic query criteria [" + columns.size() + "]." );
		}

        strTemp = getPropertyValue( SOAConstants.USE_CUSTOMER_ID_PROP );

        if ( StringUtils.hasValue( strTemp ) )
        {
            try 
            {
                
                useCustomerId = getBoolean( strTemp );
                
            }
            catch ( FrameworkException e )
            {
                
                errorBuffer.append ( "Property value for " + 
                SOAConstants.USE_CUSTOMER_ID_PROP +
                " is invalid. " + e.getMessage ( ) + "\n" );
                
            }
            
        }
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, 
				"To use customer id in SQL statement?[" + useCustomerId + "]." );
		}

        customerIdNamespace = getPropertyValue( 
        						SOAConstants.CUSTOMER_ID_NAMESPACE_PROP );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, 
						"Customer ID namespace [" + customerIdNamespace + "]." );
		}

        // If any of the required properties are absent, 
        // indicate error to caller.
        if ( errorBuffer.length() > 0 )
        {
            String errMsg = errorBuffer.toString( );

            Debug.log( Debug.ALL_ERRORS, errMsg );

            throw new ProcessingException( errMsg );
        }
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
          Debug.log( Debug.SYSTEM_CONFIG, "SOADBSelectQuery: Initialization done." );
		}
        
    }


    /**
     * Extract data values from the context/input, and use them to
     * query for rows from a database table.
     *
     * @param  mpContext The context
     * @param  inputObject  Input message to process.
     *
     * @return  The given input, or null.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    public NVPair[] process ( MessageProcessorContext mpContext, 
    						  MessageObject inputObject )
                        	  throws MessageException, 
                        	  		 ProcessingException 
    {
    	ThreadMonitor.ThreadInfo tmti = null;
        
        if ( inputObject == null )
        {
        
        	return null;
        	
        }
        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
          Debug.log( Debug.MSG_STATUS, "SOADBSelectQuery: processing ... " );
		}
        
        Document result = null;
        
        try{
	        tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
	        
	        // Reset the values in cases where we're querying again.
	        resetColumnValues( );
	
	        // Reset the query back to its original value in case we're querying
	        // again.
	        sqlStmt = originalSQLStmt;
	
	        // Extract the column values from the arguments.
	        extractQueryCriteriaValues( mpContext, inputObject );
	        
	        Connection dbConn = null;
	
	        try
	        {
	            
	            // Get a database connection from the appropriate location - 
	            // based on transaction characteristics.
	            if ( usingContextConnection )
	            {
	                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						Debug.log( Debug.MSG_STATUS, "Database logging is " +
								"transactional, so getting connection from context." );
					}
	
	                dbConn = mpContext.getDBConnection( );
	                
	            }
	            else
	            {
	                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						Debug.log( Debug.MSG_STATUS, "Database logging is not " +
									"transactional, so getting connection from pool." );
					}
	
	                dbConn = DBConnectionPool.getInstance().acquireConnection( );
	                
	            }
	
	            // Get the data from the database.
	            result = query( dbConn );
	            
	        }
	        catch ( FrameworkException e )
	        {
	            String errMsg = "ERROR: SOADBSelectQuery: Attempt to query " +
	            			"database failed with error: " + e.getMessage();
	
	            Debug.log( Debug.ALL_ERRORS, errMsg );
	
	            // Re-throw the exception to the driver.
	            throw new ProcessingException( errMsg );
	            
	        }
	        finally
	        {
	            // If the configuration indicates that this SQL operation isn't 
	            // part of the overall driver transaction, return the connection 
	            // previously acquired back to the resource pool.
	            if ( !usingContextConnection && (dbConn != null) )
	            {
	                try
	                {
	                	
	                    DBConnectionPool.getInstance().releaseConnection( dbConn );
	                    
	                }
	                catch ( ResourceException e )
	                {
	                	
	                    Debug.log( Debug.ALL_ERRORS, e.toString() );
	                    
	                }
	            }
	            
	        }
	
	        if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
	        {
	            
	            Debug.log( Debug.MSG_DATA, "Result:\n" 
	                      + XMLLibraryPortabilityLayer.convertDomToString( result ) );
	                      
	        }
	
	        // Place the result in the configured location.
	        set( resultLocation, mpContext, inputObject, result );
        
        }finally{
        	ThreadMonitor.stop(tmti);	
        }
        // Pass the input on to the output.
        return( formatNVPair( inputObject ) );
        
    }

    
    /**
     * Query for rows from the database table using the given connection.
     *
     * @param  dbConn  The database connection to perform the SQL SELECT operation against.
     *
     * @return  Document containing query results.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    private Document query ( Connection dbConn ) throws ProcessingException
    {
        
        PreparedStatement pstmt = null;
        
		ResultSet rs = null ;

        try
        {
            
            SimpleDateFormat sdf = null;

            if ( StringUtils.hasValue( outputDateFormat ) )
            {
            
                sdf = new SimpleDateFormat( outputDateFormat );
                
            }

            if ( useCustomerId )
            {
                // Add customer information
                sqlStmt = SQLBuilder.addCustomerIDCondition( sqlStmt, 
                										customerIdNamespace );
                
            }
            if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ) ){
              Debug.log( Debug.NORMAL_STATUS, "Executing SQL:\n" + sqlStmt );
			}

			XMLMessageGenerator gen = new XMLMessageGenerator( tableName );

            // Get a prepared statement for the SQL statement.
            pstmt = dbConn.prepareStatement( sqlStmt );

            // Populate the SQL statement using values obtained from the 
            // column data objects.
            if ( !populateSqlStatement( pstmt ) )
            {
            	
				return( gen.getDocument() );
				
            }			

            // Execute the SQL SELECT operation.
            rs = pstmt.executeQuery( );

            ResultSetMetaData rsmd = rs.getMetaData( );

            int numCols = rsmd.getColumnCount( );           

            int rowCount = 0;

            // Loop over rows returned by query ...
            for ( int Ix = 0;  rs.next();  Ix ++ )
            {
                
                rowCount ++;

                String iterationPrefix = SOAConstants.ROW_DELIMITER + "(" + Ix + ").";

                // Loop over columns that are available for each returned row ...
                for ( int Jx = 1;  Jx <= numCols;  Jx ++ )
                {
                    String colValue = null;

                    String colName = rsmd.getColumnName( Jx );

                    int colType = rsmd.getColumnType( Jx );
                    if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
						Debug.log( Debug.DB_DATA, "Extracting result-set data for column [" 
								   + colName + "] of type [" + colType + "] ..." );
					}

                    if ( ( colType == Types.DATE ) 
                    	|| ( colType == Types.TIME ) 
                    	|| ( colType == Types.TIMESTAMP ) )
                    {
                        if ( sdf == null )
                        {
                        	
                            throw new MessageException( "ERROR: Missing " +
                            	"required date-format for extracting date " +
                            	"query results from column [" + colName + "]." );
                            	
                        }

                        java.sql.Timestamp d = rs.getTimestamp( colName );

                        if ( rs.wasNull() )
                        {
							if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
								Debug.log( Debug.DB_DATA, "Skipping column [" + 
								colName + "] with null value ..." );
							}

                            continue;
                        }

                        colValue = sdf.format( d );
                        
                    }
                    else  // It's not a date, so extract as string.
                    {
                        
                        colValue = rs.getString( colName );

                        if ( rs.wasNull() )
                        {
                           if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){ 
								Debug.log( Debug.DB_DATA, "Skipping column [" + 
								colName + "] with null value ..." );
						   }

                            continue;
                            
                        }
                        
                    }

                    if ( singleResultFlag )
                    {
                        if ( Ix > 0 )
                        {
                            
                            throw new MessageException( "ERROR: " +
                            	"Single-result flag was set to 'true'" +
                            	", but SQL query returned multiple rows." );
                            	
                        }

                        gen.setValue( colName, colValue );
                        
                    }
                    else
                    {
                    
                        gen.setValue( iterationPrefix + colName, colValue );
                        
                    }
                    
                }
            }            

            // Check that the number of database rows selected was 
            // acceptable, based on configuration.
            checkSQLOperationResultCount( rowCount );

            return( gen.getDocument() );
            
        }
        catch ( SQLException sqle )
        {
            throw new ProcessingException( "ERROR: Could not select row " +
            				"from database table [" + tableName 
                            + "]:\n" + DBInterface.getSQLErrorMessage(sqle) );
        }
        catch ( Exception e )
        {
            throw new ProcessingException( "ERROR: Could not select row " +
            							"from database table [" + tableName 
                                        + "]:\n" + e.toString() );
        }
        finally
        {
            if ( pstmt != null )
            {
                try
                {
                	if( rs != null)
                	{
                		
                		rs.close();
                		
                		rs = null;
                		
                	}
                    
                    pstmt.close( );
                    
                    pstmt = null;
                    
                }
                catch ( SQLException sqle )
                {
                	
                    Debug.log( Debug.ALL_ERRORS, 
                    				DBInterface.getSQLErrorMessage(sqle) );
                    
                }
            }
        }
    }


    /**
     * Populate the SQL SELECT statement from the column data.
     *
     * @param  pstmt  The prepared statement object to populate.
     * 
     * @return boolean value 
     *
     * @exception Exception  thrown if population fails.
     */
    private boolean populateSqlStatement ( PreparedStatement pstmt ) 
    												throws Exception
    {
        if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
         Debug.log( Debug.DB_DATA, "Populating SQL SELECT statement ..." );
		}

        Iterator iter = columns.iterator( );

        int Ix = 0;
        
        for ( Ix = 1;  iter.hasNext();  Ix ++ )
        {
            ColumnData cd = (ColumnData)iter.next( );
            if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
				Debug.log( Debug.DB_DATA, 
							"Populating prepared-statement slot [" + Ix + "]." );
			}

            // Default is no column type specified.
            if ( !StringUtils.hasValue(cd.columnType) )
            {
                if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
					Debug.log( Debug.MSG_DATA, 
							"Value for column is [" + cd.value.toString() + "]." );
				}
                
                pstmt.setObject( Ix, cd.value );
                
            }
            else if ( cd.columnType.equalsIgnoreCase( 
            								SOAConstants.COLUMN_TYPE_DATE) )
            {
               
                String val = (String)(cd.value);
                if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
					Debug.log( Debug.MSG_DATA, 
										"Value for date column is [" + val + "]." );
				}

                if ( !StringUtils.hasValue( cd.dateFormat ) )
                {
                	
                    throw new ProcessingException( "ERROR: Configuration " +
                    					"for date column [" + cd.describe() + 
										"] does not specify date format." );
										
                }

                SimpleDateFormat sdf = new SimpleDateFormat( cd.dateFormat );

                java.util.Date d = sdf.parse( val );

                pstmt.setTimestamp( Ix, new java.sql.Timestamp(d.getTime()) );
                
            }
			else if ( cd.columnType.equalsIgnoreCase( 
											SOAConstants.COLUMN_TYPE_NUMBER ) )
            {
                
                String val = ( ( String )( cd.value ) ).trim();
                if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
					Debug.log( Debug.MSG_DATA, 
										"Value for number column is [" + val + "]." );
				}

                try
                {
                	
                	Long.parseLong( val );
                	

                }
                catch ( NumberFormatException nfe )
                {
                	if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
						Debug.log( Debug.MSG_DATA, 
										"Could not parse the value [" + val + "]." );
					}

					return false;

                }

                pstmt.setObject( Ix, val );

            }
            else
            {
                throw new ProcessingException( "ERROR: Invalid column-type " +
                				"property value given in configuration for [" 
                                              		+ cd.describe() + "] ." );
            }
        }

        if ( useCustomerId )
        {
        	
            //Increment Ix slot number.
            SQLBuilder.populateCustomerID( pstmt, Ix++ );
            
        }
        if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
         Debug.log( Debug.MSG_DATA, "Done populating SQL SELECT statement." );
		}

		return true;
		
    }


    /**
     * Extract data for each column from the input message/context.
     *
     * @param mpcontext The message context.
     * @param intputObject The input object.
     *
     * @exception MessageException thrown if required value can't be found.
     * @exception ProcessingException thrown if any other processing error occurs.
     */
    private void extractQueryCriteriaValues ( MessageProcessorContext context , 
    										  MessageObject inputObject )
    										  throws ProcessingException, 
    										  		 MessageException
    {
        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			Debug.log (Debug.MSG_STATUS, 
						"Extracting message data to use in query criteria ..." );
		}
        
        Iterator iter = columns.iterator( );

        // While columns are available ...
        while ( iter.hasNext() )
        {
            
            ColumnData cd = (ColumnData)iter.next( );

            // If location was given, try to get a value from it.
            if ( StringUtils.hasValue( cd.location ) )
            {
                // Location contains one or more alternative locations that could
                // contain the column's value.
                StringTokenizer st = new StringTokenizer( cd.location, separator );
                
                // While location alternatives are available ...
                while ( st.hasMoreTokens() )
                {
                    // Extract a location alternative.
                    String loc = st.nextToken().trim( );
                    
                    // If the value of location indicates that the input 
                    // message-object's entire value should be used as 
                    // the column's value, extract it.
                    if ( loc.equalsIgnoreCase( SOAConstants.PROCESSOR_INPUT ) )
                    {
						if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
							Debug.log( Debug.MSG_DATA, "Using message-object's " +
											"contents as the column's value." );
						}
                        
                        cd.value = inputObject.get( );
                        
                        break;
                        
                    }
                    
                    // Attempt to get the value from the context or input object.
                    if ( exists( loc, context, inputObject ) )
                    {
                        cd.value = get( loc, context, inputObject );
                        
                        // If we found a value, we're done with this column.
                        if ( cd.value != null )
                        {
                            if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
								Debug.log( Debug.MSG_DATA, "Found value for " +
											"column at location [" + loc + "]." );
							}
                            
                            break;
                            
                        }
                    }
                }
            }
            
            // If no value was obtained from location in context/input, 
            // try to set it from default value (if available).
            if ( cd.value == null )
            {
            	
                cd.value = cd.defaultValue;

                if ( cd.value != null )
                {
                    if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
						Debug.log( Debug.MSG_DATA, 
										"Using default value for column." );
					}
                    
                }else
                {
                    // Signal error to caller.
                    throw new ProcessingException( "ERROR: Could not " +
                    	"locate required value for column [" + cd.describe() 
                         + "] used in query against database table [" + 
                         tableName + "]." );
                         
                }
            }
        }
        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			Debug.log (Debug.MSG_STATUS, 
						"Done extracting message data to use in query criteria." );
		}
					
    }


    /**
     * Reset the column values in the list.
     */
    private void resetColumnValues ( )
    {
        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
          Debug.log (Debug.MSG_STATUS, "Resetting column values ..." );
		}
        
        Iterator iter = columns.iterator( );
        
        // While columns are available ...
        while ( iter.hasNext() )
        {
            
            ColumnData cd = (ColumnData)iter.next( );
            
            cd.value = null;
            
        }
    }  
    
    


    /**
     * Class ColumnData is used to encapsulate a description of a single column
     * and its associated value.
     */
    private static class ColumnData
    {
		
		//	Property used to store Column Name for a column
        public final String columnType;
        
		//	Property used to store date format 
        public final String dateFormat;
        
		//	Property used to store location for a column's value
        public final String location;
        
		//	Property used to store default value
        public final String defaultValue;

		//	Property used to store value for a column
        public Object value = null;


        public ColumnData ( String columnType, String dateFormat, 
                            String location, String defaultValue ) 
                            throws FrameworkException
        {
            this.columnType   = columnType;
            
            this.dateFormat   = dateFormat;
            
            this.location     = location;
            
            this.defaultValue = defaultValue;
            
        }

		/**
		 * This method used to describe a column
		 */
        public String describe ( )
        {
            
            StringBuffer sb = new StringBuffer( );

            sb.append( "Column description: " );

            if ( StringUtils.hasValue( columnType ) )
            {
                sb.append( "type [" );
                sb.append( columnType );
            }

            if ( StringUtils.hasValue( dateFormat ) )
            {
                sb.append( "], date-format [" );
                sb.append( dateFormat );
            }

            if ( StringUtils.hasValue( location ) )
            {
                sb.append( "], location [" );
                sb.append( location );
            }

            if ( StringUtils.hasValue( defaultValue ) )
            {
                sb.append( "], default [" );
                sb.append( defaultValue );
            }

            if ( value != null )
            {
                sb.append( "], value [" );
                sb.append( value.toString() );
            }

            sb.append( "]." );
            
            return( sb.toString() );
            
        }
        
    }
}
