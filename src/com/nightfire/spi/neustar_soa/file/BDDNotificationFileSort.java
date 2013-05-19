/*
 * The purpose of this BDDNotificationFileSort is to read the data from given
 * BDD file and sorts that data based on the NotificationID and CreationTimeStamp 
 * elements before importing the data into the respective SOA Database tables.
 *   
 *
 * @author D.Subbarao
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 *
 */
/**
	Revision History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			D.Subbarao		10/27/2005			Created
	2			D.Subbarao		11/24/2005			Modified.
	3			D.Subbarao		12/01/2005			Comments added for 
													readDataOnSort()
	4			D.Subbarao		12/07/2005			Eleminated unused objects
													and Added comments.
    5			D.Subbarao		12/16/2005			Eleminated file extension.		
    6			Jigar			08/28/2006			Updated to fix the TD.											
*/
package com.nightfire.spi.neustar_soa.file;

import java.io.FileWriter;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.LineNumberReader;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.ArrayList;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;


public class BDDNotificationFileSort {

    // This is used to extract the elements from BBD file.    
    private static final String DEFAULT_DELIMITER = "|";
    

    // This holds the name of its class.
    private String className="BDDNotificationFileSort";
    
    // This holds the name of current method.
    private String methodName=null;
    
    /**
    * This will be used to read the data from a BDDfile and it sorts the raw 
    * data based on notificationID and creationTimeStamp elements wherein BDD 
    * file. After sorting, the sorted data will be assigned into the original
    * list.
    * 
    * @param bddFile contains a BDD file.
    * @return sortFile contains the sorted data.
    * @throws FrameworkException  will be thrown if any application related 
    * 							  errors occurred.
	*             		Ex: while converting from a string to integer format.
	* @throws IOException is thrown when I/O operation error is occurred.
     */
    public String readDataOnSort(String bddFile )
    		throws IOException,FrameworkException{
     
     methodName="readDataOnSort";
      
     // This is used to provide the raw data and collect the sorted data.
     ArrayList mainDataList=null;
        
     // This is used to write the sorted data into opened file.
     PrintWriter fileOutput=null;
     
     // This is used to open a BDD file to write the sorted data into it.
     FileWriter writer=null;
     
     // This is used to read the data from a BDD file. 
     FileReader reader = null;
     
     // This will contain the sorted file.
     String sortFile=null;
     
     //This is used to read one line of data from a BDD file at each time.
     
     LineNumberReader buffer = null;
        
     try{

        sortFile="Sort." + bddFile;
         
        
                              
        // A new instance will be created with reader by FileReader class.
        
        reader = new FileReader(bddFile);

        // A new instance will be created with writer by FileWriter class.
        
        writer=new FileWriter(sortFile);
        
        //  A new instance will be created with fileOutput by PrintWriter class.
        
        fileOutput=new PrintWriter(writer);
        
        // A new instance will be created with buffer by LineNumberReader
        // class.
        
        buffer = new LineNumberReader(reader);
        
        // This reads and assign a line of data into line object.
        
        String line = buffer.readLine();
		
        // This is used to hold the notification key for identifying the data.
        
        String notifyKey=null;
        String notifDate=null;
		
        // A new instance is being created by ArrayList to keep the raw 
        // data before sorting and sorting data after sort.
        
		mainDataList=new ArrayList();

		// This is used to iterate the outer loop.
		
		int pos=0;

		// This will be encountred when line is null which is read from BDD.
		
		while (line != null) 
		{
		    StringTokenizer tokens = new StringTokenizer(line,DEFAULT_DELIMITER, true);

		    //  This is used to make the position in the mainDataList.
		    int pos1=0;
		    
		    //	 This is used to make the position in the mainDataList.
		    ArrayList dataList=new ArrayList();
		   
		    String data=null;
		    
		    if(tokens.hasMoreElements()){
		        
			     String last = DEFAULT_DELIMITER;
			     
			        while(tokens.hasMoreElements()){
			                
			            data=(String)tokens.nextElement();
			            
			            // Check to see if next token is a delimiter.
			         	if( data.equals(DEFAULT_DELIMITER) ){
			         		
			            	// We ignore delimiters, unless the last token was a delimiter too.
			            	if( last.equals(DEFAULT_DELIMITER) ) {
			            		
			               	// We found two consecutive delimiters. This indicates an
			               	// empty field, so we will add an empty value to the results.
			            		dataList.add("");
			            	}

			         	}
			         	else{
			            	// add the token
			         		dataList.add( data );
			         	}
			         	last = data;
	         	
			         	if(pos1==0)
			                notifDate=data;
			         	if(pos1==6)
			         		notifyKey=data;
			            pos1++;
			      	}

			      	if ( last.equals(DEFAULT_DELIMITER) ) {
			      		
			         	// If the last character on a line is the delimiter,
			         	// this means that there was one last empty field.
			      		dataList.add("");

			      	}

		    }
		    ArrayList rowData=new ArrayList();
		    
		    rowData.add(notifyKey);
		    
		    rowData.add(dataList);
		    
		    rowData.add(notifDate);
		    
		    mainDataList.add(pos++,rowData);
		    
			if (line.length() == 0)
			    break;
			line = buffer.readLine();
		}
		Debug.log(Debug.MAPPING_STATUS, className + "[" + methodName +
			    "] The BDDFile sorting process will be " +
				"started on creationTimestamp");
		
		sortBySelectedItem(mainDataList,mainDataList.size());
		
		Debug.log(Debug.MAPPING_STATUS,className + "[" + methodName + 
		        "] The BDDFile sorting process has been " +
				"done on creationTimestamp ");

		Debug.log(Debug.MAPPING_STATUS,className + "[" + methodName +
			     "] The data of BDD file which has been" +
		 "sorted will be written into a new file:"+sortFile);
		
		for(pos=0;pos<mainDataList.size();pos++) {
		    
		   ArrayList sortData=(ArrayList) mainDataList.get(pos);
		   
		   ArrayList sortList=(ArrayList)sortData.get(1);
		   
		   StringBuffer sortBuffer=new StringBuffer();

		   for(int pos1=0;pos1<sortList.size();pos1++){
		       
		       sortBuffer.append((String)sortList.get(pos1));
		       
		       if(pos1!=sortList.size()-1)
		           
		           sortBuffer.append(DEFAULT_DELIMITER);
		   }

		 fileOutput.println(sortBuffer.toString());
		}

		Debug.log(Debug.MAPPING_STATUS,className + "[" + methodName +
			     "]" + sortFile + " has been" +
				 "been created with the sorted data of BDD file");
      }
	  catch(IOException ex){
	      
	      Debug.log(Debug.ALL_ERRORS,ex.getMessage());
	      
	      throw new FrameworkException(ex.getMessage());    
	  }
	  catch(Exception fe){
	      
	      Debug.log(Debug.ALL_ERRORS,fe.getMessage());
	      
	      throw new FrameworkException(fe.getMessage());    
	  }
	  finally {
	      		if(writer!=null){
	      			try{
	      		  		writer.close();
	      			}catch(Exception e){
	      				if (Debug.isLevelEnabled(Debug.ALL_ERRORS)) {
							Debug.log(Debug.ALL_ERRORS, className + " : Exception occures at the time to close the " +
									"stream..." +e.getMessage());
	      				}
	      			}
	      		}
	      		if(fileOutput != null){
	      			try{
	      				fileOutput.close();
	      			}catch(Exception e){
	      				if (Debug.isLevelEnabled(Debug.ALL_ERRORS)) {
							Debug.log(Debug.ALL_ERRORS, className + " : Exception occures at the time to close the " +
									"stream..." +e.getMessage());
	      				}
	      			}
	      		} 
	      		if(buffer != null){
	      			try{
	      				buffer.close();
	      			}catch(Exception e){
	      				if (Debug.isLevelEnabled(Debug.ALL_ERRORS)) {
							Debug.log(Debug.ALL_ERRORS, className + " : Exception occures at the time to close the " +
									"stream..." +e.getMessage());
	      				}
	      			}
	      		}
	      		
	     Debug.log(Debug.MAPPING_STATUS,
	     className + "[" + methodName + "] The sorting process has been done");
	  }
	 
     return sortFile;
     
    }
   /**
    * This will be used to make use the data which is read from a file and it
    * sorts that data on a selected item either notificationid or 
    * creationtimestamp.
    *   
    * 
    * @param mainList contains the BDD data.
    * @param listSize contains the size of BDD data.
    * @throws FrameworkException  will be thrown if any application related 
    * 							  errors occurred.
	*             			Ex: while converting from a string to integer format.
    */
    private void sortBySelectedItem(ArrayList mainList, int listSize)
    					throws FrameworkException
    {
      methodName="sortBySelectedItem";
      
      int pos, pos1;
      
      int min;
      
      ArrayList temp=null;

      Debug.log(Debug.MAPPING_STATUS," The sorting will be started" +
		 " creation Time Stamp.");
      
      try{
      for (pos = 0; pos < listSize-1; pos++)
      {
        min = pos;
        
        for (pos1 = pos+1; pos1 < listSize; pos1++)
        {
          ArrayList firstList=(ArrayList)mainList.get(pos1);
          ArrayList secondList=(ArrayList)mainList.get(min);
  
          if (Long.parseLong((String)firstList.get(2)) < 
                  Long.parseLong((String)secondList.get(2)))       
        	  
              min = pos1;
        }
        
          	temp =(ArrayList)mainList.get(pos);
        	
        	mainList.set(pos,mainList.get(min));
        	
        	mainList.set(min,temp);
      }
      Debug.log(Debug.MAPPING_STATUS,className + "[" + methodName + "] " +
      		"The sorting has been" +
		 " done based on creation Time Stamp.");
      }
      catch(Exception ex){
	      
          Debug.log(Debug.ALL_ERRORS,ex.getMessage());
          
	      throw new FrameworkException(ex.getMessage());    
	  }
   } 
}