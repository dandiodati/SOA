package com.nightfire.spi.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import javax.jms.Message;

import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.jms.JmsMsgStoreDAO;
import com.nightfire.framework.jms.JmsMsgStoreDataObject;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.repository.RepositoryManager;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.ThrottlingDataCache.Outage;
import com.nightfire.spi.common.ThrottlingDataCache.Quota;

/**
 * ThrottlingAware DAO that uses outage and quota for
 * determining whether a message needs to be inserted into
 * message store table or not.
 * 
 * It uses throttling query configured in repository.
 * @author hpirosha
 *
 */
public class ThrottlingAwareJmsMsgStoreDAO extends JmsMsgStoreDAO {

    @Override
    public void init(String queueNm,String consumerNm,String messageStoreTableNm)
    {
    	super.init(queueNm, consumerNm, messageStoreTableNm);
    	initSelf(consumerNm);
    	
    }

    @Override
    public void init(String queueNm,String consumerNm)
    {
    	super.init(queueNm, consumerNm);
    	initSelf(consumerNm);
    }
	

	public boolean useCachedData() {
		return useCachedData;
	}


	private void initSelf(String consumerNm) {

		if(Debug.isLevelEnabled(Debug.DB_DATA))
		{
			Debug.log(Debug.DB_DATA,"DAO key :"+getConfigKey());
			Debug.log(Debug.DB_DATA,"DAO type :"+getConfigType());
		}
		
		try
		{
			String val = PersistentProperty.get(getConfigKey(), getConfigType(), USE_CACHED_DATA);
			if (StringUtils.hasValue(val))
				useCachedData = Boolean.parseBoolean(val);
			
		}
		catch(Exception e)
		{
			//use default i.e. use cached data
		}

		try 
		{

			queryFileNm = PersistentProperty.get(getConfigKey(), getConfigType(), QUERY_FILE_NM);

			loadQueryFile();
		} catch (Exception exp) {
			throw new RuntimeException(
					"Could not locate and parse throttling query file from repository :"
							+ queryFileNm
							+ " check value of persistent property KEY->" +getConfigKey()
							+ " PROPERTYTYPE->" + getConfigType()
							+ " PROPERTYNAME->" + QUERY_FILE_NM);
		}

	}
	
	@Override
    public boolean insert(String customerId,Message jmsMsg, Connection dbConn)
    {
    	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS,"inside ThrottlingAwareJmsMsgStoreDAO#insert(customerId, jmsMsg, dbConn)");

    	try 
    	{
			String transaction = (String)CustomerContext.getInstance().getOtherItems().get(TRANSACTION_FOR_OUTAGE);
			if(!StringUtils.hasValue(transaction))
				transaction = (String)CustomerContext.getInstance().getOtherItems().get(TRANSACTION_PROP);
	    	
			String supplier =  (String)CustomerContext.getInstance().getOtherItems().get(SUPPLIER_FOR_OUTAGE);
	    	if(!StringUtils.hasValue(supplier))
	    		supplier = (String)CustomerContext.getInstance().getOtherItems().get(ILEC_HDR_PROP);

			if (Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS,
						"ThrottlingAwareJmsMsgStoreDAO : transaction,supplier,customerId. . .["
								+ transaction + "] [" + supplier + "]["
								+ customerId + "]");

	    	
	    	Outage outage = ThrottlingDataCache.getInstance().getOutage(transaction, supplier, !useCachedData);

	    	if (outage != null)
	    	{
		    	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
		            Debug.log(Debug.MSG_STATUS,"Got outage value. . ."+outage);
	    	}
	    	else
		    	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
		            Debug.log(Debug.MSG_STATUS,"outage is null ");

	    	if((outage!=null) && supplierHasOutage(outage))
	    	{
            	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
	                Debug.log(Debug.MSG_STATUS,"Sleeping until the outage ends, would return without consuming message");

	    		Thread.sleep(outage.getOutageEndDt().getTime() - (new Date()).getTime());
	    		
            	return false;
	    	}

			transaction = (String)CustomerContext.getInstance().getOtherItems().get(TRANSACTION_FOR_QUOTA);
			if(!StringUtils.hasValue(transaction))
				transaction = (String)CustomerContext.getInstance().getOtherItems().get(TRANSACTION_PROP);
			
			supplier =  (String)CustomerContext.getInstance().getOtherItems().get(SUPPLIER_FOR_QUOTA);
	    	if(!StringUtils.hasValue(supplier))
	    		supplier = (String)CustomerContext.getInstance().getOtherItems().get(ILEC_HDR_PROP);

	    	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
	            Debug.log(Debug.MSG_STATUS,"Getting Quota value. . .["+transaction+"] ["+supplier+"]");

	    	Quota quota = ThrottlingDataCache.getInstance().getQuota(transaction, supplier, customerId, !useCachedData);
			if (quota == null)
				return super.insert(customerId, jmsMsg, dbConn);
			else if (isQuotaRemaining(quota, customerId))
				return super.insert(customerId, jmsMsg, dbConn);

	    	Debug.warning("Quota for customer :"+customerId+" has ended");
	    	
	    	return false;
		}
    	catch (FrameworkException e) 
    	{
    		Debug.warning("ThrottlingAwareJmsMsgStoreDAO : exception occured :"+e.getMessage());
		}
    	catch (InterruptedException e) 
    	{
    		Debug.warning("ThrottlingAwareJmsMsgStoreDAO : Interrupted from sleep");
		}
    	return false;

    }

    @Override
    public boolean insert(String customerId, String header, Message jmsMsg, Connection dbConn) 
    {
    	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS,"inside ThrottlingAwareJmsMsgStoreDAO#insert(customerId,header,jmsMsg,dbConn)");

       	try 
    	{
			String transaction = (String)CustomerContext.getInstance().getOtherItems().get(TRANSACTION_FOR_OUTAGE);
			if(!StringUtils.hasValue(transaction))
				transaction = (String)CustomerContext.getInstance().getOtherItems().get(TRANSACTION_PROP);
	    	
			String supplier =  (String)CustomerContext.getInstance().getOtherItems().get(SUPPLIER_FOR_OUTAGE);
	    	if(!StringUtils.hasValue(supplier))
	    		supplier = (String)CustomerContext.getInstance().getOtherItems().get(ILEC_HDR_PROP);

			if (Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS,
						"ThrottlingAwareJmsMsgStoreDAO : transaction,supplier,customerId. . .["
								+ transaction + "] [" + supplier + "]["
								+ customerId + "]");

	    	Outage outage = ThrottlingDataCache.getInstance().getOutage(transaction, supplier, !useCachedData);
	    	
	    	if (outage != null)
	    	{
		    	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
		            Debug.log(Debug.MSG_STATUS,"Got outage value. . ."+outage);
	    	}
	    	else
		    	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
		            Debug.log(Debug.MSG_STATUS,"outage is null ");

	    	if((outage!=null) && supplierHasOutage(outage))
	    	{
            	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
	                Debug.log(Debug.MSG_STATUS,"Sleeping until the outage ends, would return without consuming message");

	    		Thread.sleep(outage.getOutageEndDt().getTime() - (new Date()).getTime());
	    		
            	return false;
	    	}

			transaction = (String)CustomerContext.getInstance().getOtherItems().get(TRANSACTION_FOR_QUOTA);
			if(!StringUtils.hasValue(transaction))
				transaction = (String)CustomerContext.getInstance().getOtherItems().get(TRANSACTION_PROP);
			
			supplier =  (String)CustomerContext.getInstance().getOtherItems().get(SUPPLIER_FOR_QUOTA);
	    	if(!StringUtils.hasValue(supplier))
	    		supplier = (String)CustomerContext.getInstance().getOtherItems().get(ILEC_HDR_PROP);

	    	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
	            Debug.log(Debug.MSG_STATUS,"Getting Quota value. . .["+transaction+"] ["+supplier+"]");
	    	
	    	Quota quota = ThrottlingDataCache.getInstance().getQuota(transaction, supplier, customerId, !useCachedData);
			if (quota == null)
				return super.insert(customerId, jmsMsg, dbConn);
			else if (isQuotaRemaining(quota, customerId))
				return super.insert(customerId, jmsMsg, dbConn);

	    	Debug.warning("Quota for customer :"+customerId+" has ended");
	    	
	    	return false;
		}
    	catch (FrameworkException e) 
    	{
    		Debug.warning("ThrottlingAwareJmsMsgStoreDAO : exception occured :"+e.getMessage());
		}
    	catch (InterruptedException e) 
    	{
    		Debug.warning("ThrottlingAwareJmsMsgStoreDAO : Interrupted from sleep");
		}
    	return false;
    }

    @Override
    public boolean insert(JmsMsgStoreDataObject dataObject,Connection dbConn)
    {
    	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS,"inside ThrottlingAwareJmsMsgStoreDAO#insert(dataObject,dbConn)");

       	try 
    	{
			String transaction = (String)CustomerContext.getInstance().getOtherItems().get(TRANSACTION_FOR_OUTAGE);
			if(!StringUtils.hasValue(transaction))
				transaction = (String)CustomerContext.getInstance().getOtherItems().get(TRANSACTION_PROP);
	    	
			String supplier =  (String)CustomerContext.getInstance().getOtherItems().get(SUPPLIER_FOR_OUTAGE);
	    	if(!StringUtils.hasValue(supplier))
	    		supplier = (String)CustomerContext.getInstance().getOtherItems().get(ILEC_HDR_PROP);

	    	String customerId = (String)CustomerContext.getInstance().getOtherItems().get(CustomerContext.CUSTOMER_ID_NODE);

			if (Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS,
						"ThrottlingAwareJmsMsgStoreDAO : transaction,supplier,customerId. . .["
								+ transaction + "] [" + supplier + "]["
								+ customerId + "]");

	    	
			Outage outage = ThrottlingDataCache.getInstance().getOutage(transaction, supplier, !useCachedData);
	    	if (outage != null)
	    	{
		    	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
		            Debug.log(Debug.MSG_STATUS,"Got outage value. . ."+outage);
	    	}
	    	else
		    	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
		            Debug.log(Debug.MSG_STATUS,"outage is null ");

	    	if((outage!=null) && supplierHasOutage(outage))
	    	{
	    		long timeToSleep = outage.getOutageEndDt().getTime() - (new Date()).getTime();
            	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
	                Debug.log(Debug.MSG_STATUS,"Sleeping["+timeToSleep+" ms] until the outage ends, would return without consuming message");

	    		Thread.sleep(timeToSleep);
	    		
            	return false;
	    	}

			transaction = (String)CustomerContext.getInstance().getOtherItems().get(TRANSACTION_FOR_QUOTA);
			if(!StringUtils.hasValue(transaction))
				transaction = (String)CustomerContext.getInstance().getOtherItems().get(TRANSACTION_PROP);
			
			supplier =  (String)CustomerContext.getInstance().getOtherItems().get(SUPPLIER_FOR_QUOTA);
	    	if(!StringUtils.hasValue(supplier))
	    		supplier = (String)CustomerContext.getInstance().getOtherItems().get(ILEC_HDR_PROP);

	    	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
	            Debug.log(Debug.MSG_STATUS,"Getting Quota value. . .["+transaction+"] ["+supplier+"] ["+customerId+"]");

	    	Quota quota = ThrottlingDataCache.getInstance().getQuota(transaction, supplier, customerId, !useCachedData);
	    	if (quota != null)
	    	{
		    	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
		            Debug.log(Debug.MSG_STATUS,"Got Quota value. . ."+quota);
	    	}
	    	else
		    	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
		            Debug.log(Debug.MSG_STATUS,"quota is null ");
	    		
			
	    	if (quota == null)
				return super.insert(dataObject, dbConn);
			else if (isQuotaRemaining(quota, customerId))
				return super.insert(dataObject, dbConn);

	    	Debug.warning("Quota for customer :"+customerId+" has ended");
	    	
	    	return false;
		}
    	catch (FrameworkException e) 
    	{
    		Debug.warning("ThrottlingAwareJmsMsgStoreDAO : exception occured :"+e.getMessage());
		}
    	catch (InterruptedException e) 
    	{
    		Debug.warning("ThrottlingAwareJmsMsgStoreDAO : Interrupted from sleep");
		}
    	return false;
    }

    private List<String> tokens = new ArrayList<String>();
    private String[] NAMED_TOKENS = {"${CID}","${TIME_INTERVAL}"};
    
    private void loadQueryFile()
    {
        String xml;
        try {
            xml = RepositoryManager.getInstance().getMetaData( CATEGORY, queryFileNm );
            XMLMessageParser fileParser = new XMLMessageParser( xml );

            if(Debug.isLevelEnabled(Debug.XML_BASE))
            	Debug.log(Debug.XML_BASE,"loaded query file ->\n"+xml);
            
            configuredQuery = fileParser.getTextValue("query");

			query = configuredQuery.replaceAll(PATTERN, "?");
    		
			if(Debug.isLevelEnabled(Debug.MSG_BASE))
    			Debug.log(Debug.MSG_BASE,"query after replacing tokens->"+query);

			TreeMap<Integer, String> map = new TreeMap<Integer, String>();
			for (int i = 0; i < NAMED_TOKENS.length; i++) {
				int index = -1;
				while (index < configuredQuery.length()) {
					index = configuredQuery.indexOf(NAMED_TOKENS[i], index + 1);
					if (index < 0)
						index = configuredQuery.length();
					else
						map.put(index, NAMED_TOKENS[i]);
				}

			}

			while (map.size() > 0) {
				String val = map.remove(map.firstKey());

				tokens.add(val);
			}
         }
        catch (Exception e) 
        {
            Debug.error("Unable to load and parse file from  repository : " + CATEGORY + ":" + queryFileNm);
        }
    }

	/**
	 * 
	 * @param outage
	 * @return
	 */
	private boolean supplierHasOutage(Outage outage) {

		if (outage.getOutageStartDt() != null
				&& outage.getOutageEndDt() != null) {
			Date currentDate = new Date();
			return currentDate.after(outage.getOutageStartDt())
					&& currentDate.before(outage.getOutageEndDt());
		}
		return false;
	}
    
    /**
     * 
     * @param quota
     * @param customerId
     * @return
     * @throws FrameworkException
     */
    private boolean isQuotaRemaining(Quota quota,String customerId) throws FrameworkException {
    	Connection conn = null;
    	PreparedStatement pstm = null;
    	ResultSet rs = null;
    	
    	try
    	{
    		long limit = quota.getQuotaLimit();
    		conn = DBConnectionPool.getInstance().acquireConnection();
    		pstm = conn.prepareStatement(query);
    		
    		
    		int idx=0;
    		for(String token :tokens)
    		{
    			token = tokens.get(idx);
    			if(token.equals("${CID}"))
    				pstm.setString(idx+1, customerId);
    			
    			else if(token.equals("${TIME_INTERVAL}"))
    				pstm.setLong(idx+1, quota.getTimeInterval());
    			
    			idx++;
    		}

    		if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
    			Debug.log(Debug.NORMAL_STATUS,"ThrottlingAwareJmsMsgStoreDAO : Executing SQL..\n "+ query +" Checking quota for customer ->"+customerId);
    		
    		
    		rs = pstm.executeQuery();
    		
    		if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
    			Debug.log(Debug.NORMAL_STATUS,"ThrottlingAwareJmsMsgStoreDAO : Finished Executing SQL.. Finished Checking QUOTA..");

    		if(rs.next())
    		{
    			long processed = rs.getLong(1);
    			
    			boolean result =  limit > processed;
    			
    			if(Debug.isLevelEnabled(Debug.MSG_STATUS))
    				Debug.log(Debug.MSG_STATUS,"ThrottlingAwareJmsMsgStoreDAO : Quota Remaining :"+result+" limit->"+limit+" processed->"+processed);
    			
    			return result;
    		}
    		return true;
    	}
    	catch(ResourceException re)
    	{
    		throw new FrameworkException(re);
    	}
    	catch(SQLException sqle)
    	{
    		throw new FrameworkException(sqle);
    	}
    	finally
    	{
			if (conn != null) {
				try {
					DBConnectionPool.getInstance().releaseConnection(conn);
				} catch (Exception ignore) {
				}
			}
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ignore) {
				}
			}
			if (pstm != null) {
				try {
					pstm.close();
				} catch (Exception ignore) {
				}
			}

    	}
    	
    }
    
	private String configuredQuery = null;
	private String query = null;
	
	private static final String CATEGORY = "throttling";
	private static final String USE_CACHED_DATA = "USE_CACHED_DATA";
	private static final String QUERY_FILE_NM = "QUERY_FILE_NM";
	private String queryFileNm = null;
	private boolean useCachedData = true;
	private static String PATTERN = "\\p{Punct}\\{CID\\}|\\p{Punct}\\{TIME_INTERVAL\\}";

	private static String TRANSACTION_PROP = "Transaction";
	private static String TRANSACTION_FOR_QUOTA = "TransactionForQuota";
	private static String TRANSACTION_FOR_OUTAGE = "TransactionForOutage";

	private static String SUPPLIER_FOR_OUTAGE = "SupplierForOutage";
	private static String SUPPLIER_FOR_QUOTA = "SupplierForQuota";


	private static String ILEC_HDR_PROP = "Supplier";

	
}
