package com.nightfire.framework.test;

import java.io.*;
import java.util.*;
import java.sql.*;

import org.omg.CORBA.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.resource.*;


/*
 * Class for testing 
 */
class TestDatabasePools2
{
    //Test
    public static void main(String[] args){

        Debug.showLevels();
        Debug.enableAll();

        if( args.length != 6 ){

            Debug.error("Usage: java com.nightfire.framework.db.DBConnectionPool "
                        + "<database1> <user1> <password1>"
                        + "<database2> <user2> <password2>" );
            System.exit(-1);
        }

        String db1   = args[0];
        String user1 = args[1];
        String pwd1  = args[2];

        String db2   = args[3];
        String user2 = args[4];
        String pwd2  = args[5];

        try{

            DriverManager.registerDriver((Driver)(Class.forName("oracle.jdbc.driver.OracleDriver").newInstance()) );

            Debug.log(Debug.UNIT_TEST, "Creating pool1");
            DBConnectionPool.addPoolConfiguration( DBConnectionPool.getThreadSpecificPoolKey(), db1, user1, pwd1 );

            Debug.log(Debug.UNIT_TEST, "get instance pool1" );
            DBConnectionPool dcp = DBConnectionPool.getInstance();
            Debug.log(Debug.UNIT_TEST, "Pool 1 of type " + StringUtils.getClassName(dcp) );

            Debug.log(Debug.UNIT_TEST, "acquire conn1 on pool 1" );
            Connection conn = dcp.acquireConnection();

            Debug.log(Debug.UNIT_TEST, "Creating pool2");
            DBConnectionPool.addPoolConfiguration( "pool2", db2, user2, pwd2);

            Debug.log(Debug.UNIT_TEST, "get instance pool2" );
            DBConnectionPool dcp2 = DBConnectionPool.getInstance("pool2");
            Debug.log(Debug.UNIT_TEST, "Pool 2 of type " + StringUtils.getClassName(dcp2) );

            Debug.log(Debug.UNIT_TEST, "Pool 2 key: " + dcp2.getThreadSpecificPoolKey() );

            Debug.log(Debug.UNIT_TEST, "acquire conn2 on pool 2" );
            Connection conn2 = dcp2.acquireConnection();
            Debug.log(Debug.UNIT_TEST, "acquire conn3 on pool 2" );
            Connection conn3 = dcp2.acquireConnection();

            Debug.log(Debug.UNIT_TEST, "commit conn3" );
            dcp2.commit(conn3);

            Debug.log(Debug.UNIT_TEST, "get instance pool 3 using key of pool 2 but forcing NF pool" );
            DBConnectionPool dcp3 = DBConnectionPool.getInstance("pool2", true );
            Debug.log(Debug.UNIT_TEST, "Pool 3 of type " + StringUtils.getClassName(dcp3) );

            Debug.log(Debug.UNIT_TEST, "acquire conn4 on pool 3" );
            Connection conn4 = dcp3.acquireConnection();


            try{

                Debug.log(Debug.UNIT_TEST, "commit conn1 on pool 2" );
                dcp2.commit(conn);
                Debug.log(Debug.UNIT_TEST, "This shouldn't be reached. The connection didn't belong to the pool" );
            }
            catch(ResourceException re){

                Debug.log(Debug.ALL_ERRORS, "The following message is normal:\n" + re.getMessage() );
            }

            Debug.log(Debug.UNIT_TEST, "release conn2" );
            dcp2.releaseConnection(conn2);

            Debug.log(Debug.UNIT_TEST, "release conn3" );
            dcp2.releaseConnection(conn3);

            Debug.log(Debug.UNIT_TEST, "release conn" );
            dcp.releaseConnection(conn);

            Debug.log(Debug.UNIT_TEST, "release conn4" );
            dcp3.releaseConnection(conn4);

        }
        catch(Exception e){

            Debug.logStackTrace(e);
            System.err.println( e.toString() );
        }
    }
}
