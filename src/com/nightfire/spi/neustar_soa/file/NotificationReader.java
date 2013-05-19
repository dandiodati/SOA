/**
 * This class used to read the contents from a BDD file for inserting
 * that data into respective SOA database tables
 *    
 * @author D.Subbarao
 * @version 3.3
 * 
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * 
 * @see com.nightfire.framework.util.Debug
 * @see com.nightfire.spi.neustar_soa.utils.SOAConstants
 * @see com.nightfire.spi.neustar_soa.utils.SOAUtility
 * @see com.nightfire.framework.util.FrameworkException
 */

/** 
 History
 ---------------------
 Rev#		Modified By 	Date				Reason
 -----       -----------     ----------			--------------------------
 1			Subbarao		08/03/2005			Created
 2			Subbarao		09/02/2005			Removed unnecessary comments.
 3			Subbarao		09/12/2005			Removed the spid element from 
 												parameters list at functions
 												and also properties file.
 4			Subbarao		09/28/2005			Replaced a line in main().
 5			Subbarao		10/10/2005			Added initSpid element in read().
 6			Subbarao		10/28/2005			Modified.	
 7			Subbarao		11/24/2005			Modified.
 8			Subbarao		11/30/2005			Modified read(). 
 */

package com.nightfire.spi.neustar_soa.file;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import com.nightfire.spi.neustar_soa.file.DelimitedFileReader;
import com.nightfire.spi.neustar_soa.utils.SOAUtility;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.spi.neustar_soa.file.BDDNotificationFileSort;


public class NotificationReader extends DelimitedFileReader {

    // This holds class name.
     private static String className="NotificationReader";
    
     // This holds name of the current method.
     private static String methodName=null;
     
     // This holds the status of BDD process. 
     private static boolean success = true;
     
     // This holds the message. 
     private static String status=null;

     // This holds a file.
     private String sortFile=null;
     
     private static String logFile=null;
     
    /**
     * This reads the named file and insert the record(s) into the respective
     * SOA Database table based on its contents.
     * 
     * @param bddFile       String an Notification Bulk Load file
     * @param customerID contains the customer id.			
     * @param reportFile contains the name of report.
     * @throws IOException
     *             the named file was not found or was inaccessable.
     * @throws FrameworkException is thrown when any application level error is 
     * 							occurred.
     */
    public void read(String bddFile,
            String customerID,String reportFile) throws IOException, FrameworkException{
        
        methodName="read";
        
        String notifitype = null, logname = null;
        
        NotificationTokenConsumer notificationTknConsumer = null;
        
        // This is used to sort the data in BDD file.
        BDDNotificationFileSort bddNotifyFileSort=null;

        try {
            
            notificationTknConsumer = new NotificationTokenConsumer();
                      
            notificationTknConsumer.customerID = customerID;
            
            bddNotifyFileSort=new BDDNotificationFileSort();
            
            // It reads the contents in BDDFile for sorting and writes 
            // the sorting data into a new file.
            sortFile=bddNotifyFileSort.readDataOnSort(bddFile);
       
            // It starts initialization to make use the sorted file.
       
            super.init(sortFile);
            
            Debug.log(Debug.MAPPING_STATUS,
                    className + "["+ methodName + "] Reading process is " +
                    "started for the contents " +
                    "of a BDD file:[" + sortFile + "]");
            
            // It reads the contents from a sorted file.
          
            super.read(notificationTknConsumer,reportFile);
                               
            Debug.log(Debug.MAPPING_STATUS, className + "["+ methodName + "]  " +
            		"Data has been " +
            		"successfully read and inserted into SOA " +
            		"database tables...");
            
        } catch (NumberFormatException fex) {
            
            success = false;
            
            // we don't want to lose any valuable stack trace
            // info about where this exception came from
            Debug.logStackTrace(fex);
            
        } catch (IOException fnfex) {
            
            success = false;
            
            Debug.log(Debug.ALL_ERRORS,  className + "["+ methodName + "]"
                    + " Could not read file [" + bddFile + "]: " 
                    + fnfex.getMessage());
            
        } catch (FrameworkException fex) {
            
            success = false;
            
            Debug.log(Debug.ALL_ERRORS,  className + "["+ methodName + "] "
                    + fex.getMessage());
            
        }
        catch(Exception ex){
         
            success = false;
            
            Debug.log(Debug.ALL_ERRORS,  className + "["+ methodName + "] "
                    + ex.getMessage());
        }
        finally {
            
            if (notificationTknConsumer != null) {
                
                if(super.bddStatus==false)
                         
                    	success=false;
                
                notificationTknConsumer.cleanup(success);
                
            }
            if(super.bddStatus==false) {
                
            System.out.println("***** FAILED:The BDD data insertion has been "+
               "failed! check the report to see the failed data in "
                        + reportFile +  " ****");
            System.exit(1);
            }
            
            status=success==false?"***** UNSUCCESS:The Bdd data insertion has" +
            " been failed! Check " + logFile + " file to know the more details.":
             "***** SUCCESS:The BDD data insertion has " +
             "been done!";
             
             System.out.println(status);
             
             System.exit(1);
             
        }
        
    }
    /**
     * This is the command-line interface for processing an Notification BL file
     * and insert into the DB with its contents.
     * 
     * @param args
     *            String[] the command line arguments. See the usage string for
     *            description.
     */
    public static void main(String[] args) {
        
        String bddFile = null;
                      
        String custID = null;
        
        String dbname = null;
        
        String dbuser = null;
        
        String dbpass = null;
                      
        String reportFile=null;
        
        methodName="main";
        
        try {
        
            HashMap hashMap = null;
            
            if (args.length == 0) {
                
                System.out.println("Property file is missing !!");
                
                System.exit(-1);
            }
            
            hashMap = SOAUtility.validateProperties(args[0]);
            
	                  
            if (hashMap != null) {
            
                bddFile = (String) hashMap.get(SOAConstants.SOURCE_FILE_NAME);
                
                custID = (String) hashMap.get(SOAConstants.DOMAIN_NAME);
                
                dbname = (String) hashMap.get(SOAConstants.DATABASE_NAME);
                
                dbuser = (String) hashMap.get(SOAConstants.DATABASE_USER_NAME);
                
                dbpass = (String) hashMap.get(SOAConstants.DATABASE_PASSWORD);

                logFile = (String) hashMap.get(SOAConstants.LOG_FILE_NAME);
                
                reportFile = (String) hashMap.get(SOAConstants.REPORT_FILE_NAME);
                    
                // To keep the log information into the specifed file.
    	        Properties props = new Properties();
    	        
   	            props.put("DEBUG_LOG_LEVELS", "ALL");
    	        
   	            props.put("LOG_FILE",logFile);
    	        
   	            Debug.showLevels();
    	        
   	            Debug.configureFromProperties(props);
                                               
   	            Debug.log(Debug.MAPPING_STATUS,
                    "start loading the properties from a properties file:["
                    + args[0] + "]");

                // This checks the given parameters may contain the data 
                //  or not.
                if (bddFile!=null && custID!=null
                     && dbname!=null && dbuser!=null && dbpass!=null) {
                    
                    DBInterface.initialize(dbname, dbuser, dbpass);
                    
                    NotificationReader reader = new NotificationReader();
                    
                    reader.read(bddFile,custID,reportFile);
                    
                } else {
                    
                    Debug.log(Debug.ALL_ERRORS,
                    className + "["+ methodName + "] Required properties"+ 
                    "might contain null data those are in properties file.!!");
                }
            } else {

               Debug.log(Debug.ALL_ERRORS,className + "["+ methodName + 
                    "] Retreiving the properties process is failed!");
            }
        } catch (IOException fnfex) {
          
          success=false;
          
          Debug.log(Debug.ALL_ERRORS,className + "["+ methodName + 
                    "] Could not read file" +
            		" [" + bddFile + "]:" + fnfex.getMessage());
            
        } catch (FrameworkException fex) {
            
            success=false; 
           
             Debug.log(Debug.ALL_ERRORS,className + "["+ methodName + 
                 "]"+ fex.getMessage());
        }
        catch(Exception ex){
            
            success=false;
            
            Debug.log(Debug.ALL_ERRORS,className + "["+ methodName + 
                 "]"+  ex.getMessage());
         	
        }
        finally{

             if(status==null) {

                status=success==false?"***** UNSUCCESS:The Bdd data insertion has" +
               	" been failed! Check " + logFile + " file to know the more details.":
                "***** SUCCESS:The BDD data insertion has " +
             	"been done!";
             
                 System.out.println(status);
             }
            
        }
    }
    
}