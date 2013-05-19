package com.nightfire.spi.neustar_soa.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.nightfire.framework.util.Debug;

/** 
 * DynamicComparator - This class that can be 
 * used with DynamicComparator.sort(list, fieldName, sortorder). 
 * It will create a Comparator 
 * that will compare two objects based on the field  
 * passed into the sort method.   
 *  
 * Usefull if you have a Collection of Beans that you 
 * want to sort based on a specific field. 
 * 
 * 
 * 
 */

public final class DynamicComparator implements Comparator{
	
	//Declare fields
	private String 	   field;
	private boolean    sortAsc;

	private DynamicComparator(String field, boolean sortAsc)
	{
		this.field     = field;
		this.sortAsc    = sortAsc;
	}

	/** This method is used to sort collection of beans depend on passed arguments.
	 * 
	 * @param list, list of beans
	 * @param field, name of field on which sorting need to perform.
	 * @param sortAsc, boolean value, true for ascending.
	 */
	public static void sort(List list, String field, boolean sortAsc)
	{
		Collections.sort(list, new DynamicComparator(field,	sortAsc));
	}
	
	/** This method is called by Collections sort method.
	 *  
	 * @param o1
	 * @param o2
	 * @return
	 */
	public int compare(Object first, Object second) {
		Comparable cFirst = null, cSecond = null;
        try {
			cFirst = (Comparable) first.getClass().getField(field).get(first);
			cSecond = (Comparable) second.getClass().getField(field).get(second);
			return (sortAsc)?cFirst.compareTo(cSecond):cSecond.compareTo(cFirst);
		} catch (IllegalArgumentException e) {
			if ( Debug.isLevelEnabled( Debug.MSG_ERROR ))
			{
				Debug.log(Debug.MSG_ERROR ,"DynamicComparator, Error : "+ e.getMessage());
			}
		} catch (SecurityException e) {
			if ( Debug.isLevelEnabled( Debug.MSG_ERROR ))
			{
				Debug.log(Debug.MSG_ERROR ,"DynamicComparator, Error : "+ e.getMessage());
			}
		} catch (IllegalAccessException e) {
			if ( Debug.isLevelEnabled( Debug.MSG_ERROR ))
			{
				Debug.log(Debug.MSG_ERROR ,"DynamicComparator, Error : "+ e.getMessage());
			}
		} catch (NoSuchFieldException e) {
			if ( Debug.isLevelEnabled( Debug.MSG_ERROR ))
			{
				Debug.log(Debug.MSG_ERROR ,"DynamicComparator, Error : "+ e.getMessage());
			}
		}
        return 0;
        
	}

}
