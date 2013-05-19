package com.nightfire.framework.xrq.utils;

import java.util.*;
import com.nightfire.framework.util.*;


/**
 * This class is an access point into the swap file.
 */
public class SwapIOAccess
{

  private String swapDir;
  private String fileName;

  private List swapRecords;
  private int maxRecs;

  /**
   * constructor
   * @param swapDir the directory where the swap file should exist.
   * @param fileName the name of the swap file.
   * @param records - A list of records to add before buildSwap is called.
   *   This allows for some previously processed records to be added before
   *   additional records are processed.
   * @param maxNumRecords - the max number of records to write to swap.
   *
   */
  public SwapIOAccess(String swapDir, String fileName, List records, int maxNumRecords)
  {
     this.fileName = fileName;
     this.swapDir = swapDir;
     swapRecords = new ArrayList(records);
     maxRecs = maxNumRecords;
  }

  /**
   * open the swap file.
   */
  public void open() throws FrameworkException
  {}
  /**
   * close the swap file.
   */
  public void close() throws FrameworkException
  {}

  /**
   * returns the number of records in the swap
   */
  public int size()
  {
     return swapRecords.size();
  }


  /**
   * build the swap file.
   * first adds any serialized records that were pushed in front
   * of this RecordSerializer via the pushSerializedRecords(...) method.
   * Then it iterates through the rest of records in the RecordSerializer
   */
  public void buildSwap(RecordSerializer rs)
  {
     int count = swapRecords.size();

     
     while (rs.hasNext() && count < maxRecs) {
        swapRecords.add(rs.next() );
        count++;
     }

  }


  /**
   * retrieves the single record at specified location
   * @param index the index of the record.
   * @returns The serialized record.
   * @exception FrameworkException thrown if the record is not found.
   *
   */
  public String getRecord(int index) throws FrameworkException
  {
    return (String) swapRecords.get(index);

  }

  /**
   * retrieves a list of records.
   * @param index the starting index of the records.
   * @param count The number of records to retrieve.
   *
   * @returns Iterator contains the records 
   * @exception FrameworkException thrown if the record is not found.
   *
   */
  public final Iterator getRecords(int index, int count) throws FrameworkException
  {

     int end = index + count;


     if ( end >= size() )
        end = size();

     if (Debug.isLevelEnabled(Debug.IO_STATUS) )
        Debug.log(Debug.IO_STATUS, "SwapIOAccess[" + fileName + "] : Retrieving records from start index [" + index + "] to end index [" + end +"]");


     List subList;


     try {
        subList = swapRecords.subList(index, end);
     } catch (Exception e) {
        Debug.error("Invalid swap access: " + e.getMessage() );
        throw new FrameworkException("SwapIOAccess[" + fileName + "] : Invalid swap access: " + e.getMessage());
     }
     return subList.iterator();

  }

  /**
   * does any clean up
   */
  public void cleanup() throws FrameworkException
  {
  }

} 