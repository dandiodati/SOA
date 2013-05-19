package com.nightfire.webgui.core.tag.util;

import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.resource.ResourceException;

import java.util.Map;
import java.util.HashMap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

/**
 * Utility class to fetch Servername to WSP mapping from WSP_INFO table.
 * This requirement came from production environment where Common Branded GUI box need to be accessed using different
 * DNS names in the GUI HTTP URL. Based on the URL the proper WSP logo/title are to be displayed for the GUI User.
 *
 * ServerName: is expected to be a DNS name from GUI URL, like ht.neustar.com or wow.neustar.com or mytest.impetus.com
 *              If this serverName happens to be static IP address or machine name then it can only be mapped to one WSP
 *              This
 * WSP: This serverName is mapped to a WSP like HAWAIITEL, WOW, TESTWSP
 */
public class WSPInfoUtil {

    // to store servername to wsp mapping
    private static Map<String,String> server2WSPMap = new HashMap<String,String>();
    // to be used in logging statement
    private static final String className = WSPInfoUtil.class.getSimpleName() + ": ";
    private static final String SELECT_SERVER_WSP_MAPPING_SQL = "SELECT * FROM WSP_INFO";
    private static final String SERVERNAME_COL = "SERVERNAME";
    private static final String WSP_COL = "WSP";

    /**
     * Static method to get the WSP corresponding to the servername obtained from HTTP URL.
     * These mappings are stored in the WSP_INFO table.
     * WSP_INFO
     *
     * SERVERNAME        |   WSP
     * -------------------------
     * ht.neustar.com    |   HAWAIITEL
     * wow.neustar.com   |   WOW
     * .......
     * .......
     *
     *
     * @param serverName HTTP request servername used in initial request
     * @return WSP string WSP for the given server name
     * @throws FrameworkException
     */
    public static String getWSPForServer(String serverName) throws FrameworkException {

        String key = serverName;
        String retValue = null;

        if (key != null)
            key = key.trim();

        // return null, if invalid serverName is passed
        if (!StringUtils.hasValue(key)) {
            Debug.log(Debug.ALL_WARNINGS, className + "Invalid serverName [" + serverName + "] passed. Returning null.");
            return null;
        }

        // initialize the servername to wsp mapping if not already initialized
        if (server2WSPMap.isEmpty())
            initialize();

        retValue = server2WSPMap.get(key);

        if (Debug.isLevelEnabled(Debug.MAPPING_DATA))
            Debug.log(Debug.MAPPING_DATA, className + "Obtained [" + retValue + "] as WSP for server-name [" + serverName + "]");

        return retValue;
    }

    public static void initialize() throws FrameworkException {

        Connection dbConn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String serverName = null;
        String wsp = null;

        try {
            dbConn = DBConnectionPool.getInstance().acquireConnection();
            pstmt = dbConn.prepareStatement(SELECT_SERVER_WSP_MAPPING_SQL);
            
            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            	Debug.log(Debug.NORMAL_STATUS, "WSPInfoUtil: Executing SQL: \n"+SELECT_SERVER_WSP_MAPPING_SQL);
            
            rs = pstmt.executeQuery();

            
            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            	Debug.log(Debug.NORMAL_STATUS, "WSPInfoUtil: Finished Executing SQL Select Query..");

            while (rs.next()) {
                serverName = rs.getString(SERVERNAME_COL);
                wsp = rs.getString(WSP_COL);

                if (Debug.isLevelEnabled(Debug.DB_DATA))
                    Debug.log(Debug.DB_DATA, className + "Adding [" + wsp + "] as WSP for server-name [" + serverName + "]");

                server2WSPMap.put(serverName, wsp);
            }
        }
        catch (ResourceException re) {
            Debug.log(Debug.ALL_ERRORS, className + "Could not obtain database connection for fetching WSP_INFO mappings. " + re.getMessage());
            Debug.logStackTrace(re);

            throw new FrameworkException("Could not fetch mappings from WSP_INFO table.");
        }
        catch (SQLException sqle) {
            Debug.log(Debug.ALL_ERRORS,
                    className + "Could not create prepared statement for SQL [" +SELECT_SERVER_WSP_MAPPING_SQL+"]. "+ sqle.getMessage());
            Debug.logStackTrace(sqle);

            throw new FrameworkException("Could not fetch mappings from WSP_INFO table.");
        }
        finally {
            // Clean up all database-related resources.
            if ( rs != null ) {
                try {
                    rs.close( );
                    rs = null;
                }
                catch ( Exception e ){
                    Debug.error( e.toString() );
                }
            }

            if ( pstmt != null ){
                try{
                    pstmt.close( );
                    pstmt = null;
                }
                catch ( Exception e ){
                    Debug.error( e.toString() );
                }
            }

            if ( dbConn != null ){
                try{
                    DBConnectionPool.getInstance().releaseConnection( dbConn );
                }
                catch ( Exception e ){
                    Debug.error( e.toString() );
                }
            }
        }
    }

    public static void main(String[] args)
    {
        try
        {
            DBInterface.initialize(args[0], args[1], args[2]);
            System.out.println("WSP for ht.neustar.com: " + WSPInfoUtil.getWSPForServer("ht.neustar.com"));
            System.out.println("WSP for wow.neustar.com: " + WSPInfoUtil.getWSPForServer("wow.neustar.com"));
            System.out.println("WSP for 192.168.64.102: " + WSPInfoUtil.getWSPForServer("192.168.64.102"));
        }
        catch (DatabaseException e)
        {
            e.printStackTrace();
        } catch (FrameworkException e) {
            e.printStackTrace();
        }
    }
}
