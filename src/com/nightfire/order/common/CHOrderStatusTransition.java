package com.nightfire.order.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.nightfire.framework.cache.CacheManager;
import com.nightfire.framework.cache.CachingObject;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.order.utils.CHOrderException;


/**
 * This implements singleton implementation of CHOrder Status Transition Matrix cache. 
 * This matrix is used to determine the following:
 * 	1) (Old Status,Event Code, New Status) triplet is valid or not.
 *  2) (OLD STATUS, EVENT CODE) -> NEW STATUS
 *  	i.e. give OLD STATUS and EVENT CODE determines the NEW STATUS.
 *  
 * @author Abhishek Jain
 */
public class CHOrderStatusTransition 
{

    // Register CHOrderStatusTransitionCache with the cache manager.
    static
    {
        if (Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE,"Registring CHOrderStatusTransitionCache with Cache Manager");

        CacheManager.getRegistrar().register( new CHOrderStatusTransitionCache() );
    }

    private static class CHOrderStatusTransitionCache implements CachingObject
    {
        /**
         * Method invoked by the cache-flushing infrastructure
         * to indicate that the cache should be emptied.
         *
         * @exception FrameworkException if cache cannot be cleared.
         */
        public void flushCache ( ) throws FrameworkException
        {
            CHOrderStatusTransition.getInstance().clearCache( );
        }
    }
    /**
     * static singleton instance of this class. 
     */
    private static CHOrderStatusTransition singleton;

    /**
     * private constructor to support singleton implementation. 
     */
    private CHOrderStatusTransition()
    {
    }

    /**
     * @return returns a singleton instance of type CHOrderStatusTransitionCache
     */
    public static CHOrderStatusTransition getInstance()
    {
        if(singleton != null)
            return singleton;
            
        synchronized(CHOrderStatusTransition.class)
        {
            if(singleton==null)
                singleton = new CHOrderStatusTransition();  
        } 

        return singleton;
    }

    /** 
     * flush all the cached data.
     */
    private synchronized void  clearCache() //throws FrameworkException
    {
        // for all product flush the cache;
        productTransitionMap.clear();
    }

    /**
     * returns true if transition records are already loaded for given product name otherwise
     * returns false. To reload the records load() method can be used.
     * @param productName name of product.
     * 
     * @return 
     */
    public boolean isAlreadyLoaded(String productName)
    {
        if(productName == null)
            return false;

        return productTransitionMap.get(productName) == null ?  false : true ;
    }

    private synchronized void populate(Connection connection, String product) throws SQLException
    {
        // check whether it is already loaded
        if(isAlreadyLoaded(product))
            return;
        
        String sqlQuery;
        // Create a local map is copy of global map which should be updated in one go, otherwise there will
        // dirty reading if the global map is updated with single record at one time.
        Map<String, Collection<OrderStatusTransitionRecord>> localProductTransitionMap =
        new ConcurrentHashMap<String, Collection<OrderStatusTransitionRecord>>();

        // get the correct sql query for getting transition records.
        if(!StringUtils.hasValue(product))
            sqlQuery = ALL_TRANSITION_QUERY;
        else
            sqlQuery = PRODUCT_TRANSITION_QUERY;

        PreparedStatement pstmt = null;
        ResultSet results = null;

        try 
        {
    
            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                Debug.log(Debug.NORMAL_STATUS,"CHOrderStatusTransition : Executing SQL Query \n["+sqlQuery+"]");
            
            pstmt = connection.prepareStatement(sqlQuery);
    
            // if product is passed than set product in the prepared statement.
            if(StringUtils.hasValue(product))
                pstmt.setString(1, product);
    
            results = pstmt.executeQuery();

            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                Debug.log(Debug.NORMAL_STATUS,"CHOrderStatusTransition : Finished Executing SQL Query...");

            while(results.next())
            {
                String oldStatus = results.getString(OLD_STATUS);
                String newStatus = results.getString(NEW_STATUS);
                String eventCode = results.getString(EVENT_CODE);
                String prod = results.getString(PRODUCT);
                OrderStatusTransitionRecord record = new OrderStatusTransitionRecord(oldStatus,newStatus,eventCode);
                // update local map one by one for each record coming in random sequence of product value
                addRecord(prod,record, localProductTransitionMap);
            }
            // update global map to insert records in one go for each product.
            // This shall avoid dirty reading in multi-threaded request processing
            addRecords(localProductTransitionMap);
        }
        finally
        {
            try {
            if(pstmt!=null)
                pstmt.close();
            }catch(SQLException ignore){}

            try {
                if(results!=null)
                    results.close();
            }catch(SQLException ignore){}
        }
    }

    /**
     * add the record to proper product bucket in a given map.
     * @param product
     * @param record
     * @param localProductTransitionMap
     */
    private void addRecord(String product, OrderStatusTransitionRecord record, Map<String, Collection<OrderStatusTransitionRecord>> localProductTransitionMap)
    {
        // get the collection of records for given product.
        Collection<OrderStatusTransitionRecord> records = localProductTransitionMap.get(product);
        if(records == null)
        {
            // since collection is null create a new collection type add add it to map.
            records = new ConcurrentLinkedQueue<OrderStatusTransitionRecord>();
            localProductTransitionMap.put(product, records);
        }
        // finally put the record in the collection.
        records.add(record);
    }

    /**
     * add the record to proper product bucket in global map.
     * @param localProductTransitionMap
     */
    private void addRecords(Map<String, Collection<OrderStatusTransitionRecord>> localProductTransitionMap)
    {
        Set keySet = localProductTransitionMap.keySet();
        Iterator keys = keySet.iterator();

        while (keys.hasNext())
        {
            String product = (String) keys.next();
            // get the collection of records for given product.
            Collection<OrderStatusTransitionRecord> records = productTransitionMap.get(product);
            if(records == null)
            {
                // since collection is null create a new collection type add add it to map.
                records = new ConcurrentLinkedQueue<OrderStatusTransitionRecord>();
                productTransitionMap.put(product, records);
            }
            // finally put the record in the collection.
            records.addAll(localProductTransitionMap.get(product));
        }
    }

    /**
     * loads all records in CH_ORDER_STATUS_TRANSITION table to the cache. This could be used for
     * reloading all product transition records at runtime.   
     */
    public void load(Connection connection) throws Exception
    {
        if(Debug.isLevelEnabled(Debug.DB_STATUS))
            Debug.log(Debug.DB_STATUS,"inside load..");
        
        populate(connection, null );
        
        if(Debug.isLevelEnabled(Debug.DB_STATUS))
            Debug.log(Debug.DB_STATUS,"load done..");
    }

    /**
     * loads product specific records in CH_ORDER_STATUS_TRANSITION table.
     * @param productName name of the product
     */
    public void load(Connection connection, String productName) throws CHOrderException
    {
        if(Debug.isLevelEnabled(Debug.DB_STATUS))
            Debug.log(Debug.DB_STATUS,"inside load productName["+productName+"]...");
        
        try 
        {
            populate(connection, productName );
        } catch (SQLException e) 
        {
            throw new CHOrderException(e);
        }
        
        if(Debug.isLevelEnabled(Debug.DB_STATUS))
            Debug.log(Debug.DB_STATUS,"done loading..");
    }

    /**
     * validate the triplet (OLD_STATE,NEW_STATE,EVENT_CODE) with respect to
     * the details in CH_ORDER_STATUS_TRANSITION table for a particular product.
     * i.e. Validates whether order in some status can transit to a new status on
     * some event condition.
     * 
     * @param oldStatus old status of the order.
     * @param newStatus new status of the order.
     * @param eventCode event code to specify a particular event.
     * @param product product name.
     * @return true if the triplet is valid otherwise returns false.
     */
    public boolean validate(String oldStatus, String newStatus, String eventCode, String product)
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS,"validating oldStatus["+oldStatus+"], newStatus["+newStatus+"]," +
                " eventCode["+eventCode+"], product["+product+"]");

        // if product is null delegate to overloaded method.
        if(product == null)
            validate(oldStatus, newStatus, eventCode);

        Collection<OrderStatusTransitionRecord> records = productTransitionMap.get(product);

        return validate(oldStatus, newStatus, eventCode, records);
    }

    /**
     * validate the triplet (OLD_STATE,NEW_STATE,EVENT_CODE) with respect to
     * the details in CH_ORDER_STATUS_TRANSITION table.
     * i.e. Validates whether order in some status can transit to a new status on
     * some event condition.
     * 
     * @param oldStatus old status of the order.
     * @param newStatus new status of the order.
     * @param eventCode event code to specify a particular event.
     * @return true if the triplet is valid otherwise returns false.
     */
    public boolean validate(String oldStatus, String newStatus, String eventCode)
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS,"validating oldStatus["+oldStatus+"], " +
                    "newStatus["+newStatus+"], eventCode["+eventCode+"]");
        
        Iterator<String> iterator = productTransitionMap.keySet().iterator();
        while(iterator.hasNext())
        {
            String productName = (String) iterator.next();
            // if any of the product validate this to true return
            // else check for a valid state in all the product.
            // I am assuming finding true is better approach, since most of the
            // product will have the same rules is more probable.
            if(validate(oldStatus, newStatus, eventCode, productName))
                return true;
        }
        // finally return false.
        return false;
    }

    private boolean validate(String oldStatus, String newStatus, String eventCode, Collection<OrderStatusTransitionRecord> records) 
    {
        if(records == null)
            return false;

        for (Iterator<OrderStatusTransitionRecord> iterator = records.iterator(); iterator.hasNext();) 
        {
            OrderStatusTransitionRecord record =  iterator.next();
            // compare all the three properties and decide to return true or false
            if(
                    compare(record.getOldStatus(), oldStatus)
                    && compare(record.getNewStatus(), newStatus)
                    && compare(record.getEventCode(), eventCode)
            )
                return true;
        }
        return false;
    }

    /**
     * utility method that compares two strings and return the comparison result in boolean.
     * @param value1
     * @param value2
     * @return
     */
    private boolean compare(String value1, String value2) 
    {
        if(value1 == value2)
            return true;
        else if(value1 != null && value2 != null && value1.trim().equalsIgnoreCase(value2.trim()))
            return true;
        else
            return false;
    }

    /**
     * returns the new status of order given its old status and current event code.
     * 
     * @param oldStatus old status of the order.
     * @param eventCode event code.
     * @param product name of the product.
     * @return the new status for the order pair, returns null if no new status is found.
     */
    public String getNewStatus (String oldStatus, String eventCode, String product)
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS,"GetNewStatus for oldStatus["+oldStatus+"]," +
                    "eventCode["+eventCode+"] and product["+product+"]");

        // if product is null delegate to overloaded method.
        if(product == null)
            return getNewStatus(oldStatus,eventCode);

        Collection<OrderStatusTransitionRecord> records = productTransitionMap.get(product);

        return getNewStatus(oldStatus, eventCode, product,records);
    }

    /**
     * returns the new status of order given its old status and current event code.
     * @param oldStatus old status of the order.
     * @param eventCode event code.
     * @return the new status for the order pair, returns null if no new status is found.
     */
    public String getNewStatus (String oldStatus, String eventCode)
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS,"GetNewStatus for oldStatus["+oldStatus+"] and eventCode["+eventCode+"]");
        
        //HashSet<String> productNames = new HashSet<String>(productTransitionMap.keySet());
        //for (Iterator<String> iterator = productNames.iterator(); iterator.hasNext();)
        Iterator<String> iterator = productTransitionMap.keySet().iterator();
        while(iterator.hasNext())
        {
            String productName = (String) iterator.next();
            String result =getNewStatus(oldStatus, eventCode, productName);

            if(StringUtils.hasValue(result))
                return result;
        }
        return null;
    }

    private String getNewStatus(String oldStatus, String eventCode, String product, Collection<OrderStatusTransitionRecord> records) 
    {
        if(records == null)
            return null;

        for (Iterator<OrderStatusTransitionRecord> iterator = records.iterator(); iterator.hasNext();) 
        {
            OrderStatusTransitionRecord record = (OrderStatusTransitionRecord) iterator.next();
            if(compare(record.getOldStatus(),oldStatus)&& compare(record.getEventCode(),eventCode))
            {
                return record.getNewStatus();
            }
        }
        return null;
    }
    /**
     * Map of order status transition records containing product specific transition details.
     * <PRODUCT, COLLECTION OF OrderStatusTransitionRecord> 
     */
    private Map<String, Collection<OrderStatusTransitionRecord>> productTransitionMap = 
        new ConcurrentHashMap<String, Collection<OrderStatusTransitionRecord>>();

    /**
     * place holder class for storing Order Status Transition record.
     */
    public class OrderStatusTransitionRecord
    {
        /**
         * old status of order
         */
        private String old_status;
        /**
         * new status of order
         */
        private String new_status;
        /**
         * event code.
         */
        private String event_code;
        /**
         * construct a new record of type OrderStatusTransitionRecord
         * @param old_status
         * @param new_status
         * @param event_code
         */
        public OrderStatusTransitionRecord(String old_status,
                String new_status, String event_code) 
        {
            super();
            this.old_status = old_status;
            this.new_status = new_status;
            this.event_code = event_code;
        }
        /**
         * @return returns old status of order.
         */
        public String getOldStatus() 
        {
            return old_status;
        }
        /**
         * @return new status of order.
         */
        public String getNewStatus() 
        {
            return new_status;
        }
        /**
         * @return returns event code.
         */
        public String getEventCode() 
        {
            return event_code;
        }
    }

    /**
     * Query to fetch transition records for all the products. 
     */
    private static String ALL_TRANSITION_QUERY = "SELECT OLD_STATUS,NEW_STATUS,EVENT_CODE,PRODUCT FROM CH_ORDER_STATUS_TRANSITION";
    /**
     * Query to fetch transition records for a specific product. 
     */
    private static String PRODUCT_TRANSITION_QUERY = "SELECT OLD_STATUS,NEW_STATUS,EVENT_CODE,PRODUCT FROM CH_ORDER_STATUS_TRANSITION WHERE PRODUCT LIKE ?";

    // query results column names.
    private static final String OLD_STATUS = "OLD_STATUS";
    private static final String NEW_STATUS = "NEW_STATUS";
    private static final String EVENT_CODE = "EVENT_CODE";
    private static final String PRODUCT = "PRODUCT";

    // unit test cases.
    public static void main(String[] args) throws Exception
    {
        DBInterface.initialize("jdbc:oracle:thin:@192.168.96.192:1521:ORCL132", "hpirosha", "hpirosha");
        final Connection conn = DBInterface.getConnection();

        Thread t1 = new Thread() {
             public void run()
            {
                 try
                 {
                 CHOrderStatusTransition.getInstance().load(conn,"ICP");
                 }catch(Exception e) {
                     System.out.println("Exception occured :" + e.getMessage());
                     System.exit(1);
                 }
            }};
            t1.start();

            Thread t2 = new Thread() {
                public void run()
               {
                    CHOrderStatusTransition.getInstance().getNewStatus("save", "create", "ICP");                
               }};
            t2.start();
    
            Thread.sleep(10*60*1000);
        
    }
}
