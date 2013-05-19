/**
 * Copyright (c) 2000-2006 Neustar, Inc. All rights reserved.
 *
 * $Header$  //webgui/core/com/nightfire/webgui/core/tag/util/TagFunctions.java#1$
 */


package com.nightfire.webgui.core.tag.util;

// JDK import
import java.util.Collection;
import javax.servlet.jsp.PageContext;

// Nightfire import
import com.nightfire.webgui.core.tag.TagConstants;
import com.nightfire.webgui.core.tag.TagUtils;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.security.tpr.TradingPartnerRelationship;


/**
 * This class provides JSP pages with the utility functions that can be used in
 * the expression language.
 */
public class TagFunctions
{
    public PageContext pageContext;
    /**
     * Constructor.
     * */
    public TagFunctions(){}

    /**
     * @param  s1  The origin string that may contain s2.
     * @param  s2  The string to look for in s1.
     * @return  int  if the string argument occurs as a substring within this object,
     *               then the index of the first character of the first such substring
     *               is returned; if it does not occur as a substring, -1 is returned.
     */
    public int indexOf(String s1, String s2)
    {
        return s1.indexOf(s2);
    }

    /**
     * This method uses TradingPartnerRelationship.getTradingPartnerNames method which returns
     * a list of all the enabled trading partners for a particular customerId and Transaction type.
     *
     * @param  queryMethodParams - String representation of comma separated method parameters.
     *
     * @return  String[][] representation of the option list of option values and option display names
    */
     public String[][] getSupplierNames(String queryMethodParams)
     {
        /**
        * Transaction type.
        */
        String transaction = null;

         if (StringUtils.hasValue(queryMethodParams))
         {
            //Splitting the comma seperated parameters into an array of String.

            String[]  methodParams = queryMethodParams.split(",");

            transaction = methodParams[0].trim();
         }

         String[][] optionList =null;

         Debug.log(Debug.MSG_STATUS,"getSupplierNames(): Getting option values using query method for transaction  ["+ transaction +"]...");

         try
         {
             if (StringUtils.hasValue(transaction))
             {
                   //Extracting the customer Id.

                   String customerId = CustomerContext.getInstance().getCustomerID();

                   Collection enabledSuppliers = TradingPartnerRelationship.getInstance(customerId).getTPNamesForTransaction(transaction);

                   String[] enabledSupplierNames = (String[]) enabledSuppliers.toArray(new String[enabledSuppliers.size()]);

                   // This piece of code has been introduced to limit the branded customer see the supplier
                   // of the domain using which he has logged in, if he is configured for more than one branded supplier.

                String wholesaleProvider = TagUtils.getWSPInSession (pageContext);

                 for (Object enabledSupplier : enabledSuppliers)
                 {
                     String supplier = (String) enabledSupplier;
                     if (StringUtils.hasValue(wholesaleProvider) && wholesaleProvider.equals(supplier))
                     {
                         enabledSupplierNames = new String[1];
                         enabledSupplierNames[0] = supplier;
                     }
                 }


                   optionList = new String [2][enabledSupplierNames.length+1];

                   //optionList[0] refers to the option-list/Enumerated list values.

                   optionList[0] =  enabledSupplierNames;

                   //optionList[1] refers to the option-list/Enumerated list Display values.

                   optionList[1] =  enabledSupplierNames;
              }

              //To support displaying 'All' option, except when only one option is available
              if( optionList == null || optionList[0].length > 1 )
                optionList= addOptionAllToList(optionList);

              Debug.log(Debug.MSG_STATUS,"getSupplierNames(): Option list obtained using the query method\n");

             return  optionList;
          }
          catch (Exception e)
          {
              Debug.error("getSupplierNames(): Failed to obtain the Option list using the query method\n" + e.getMessage());
          }
          return null;
     }


      /**
      * Can be used to add 'All' option value as the first option to enumerated fields.
      * @param optionList two dimensional string representation of option values and option display names
      * @return String[][] updated optionList with first option display name as "All"  and option value as blank.
      */
       public String[][] addOptionAllToList(String[][] optionList)
       {
           String[][] newOptionList;

           String ALL_OPTION = "All";

           Debug.log(Debug.MSG_STATUS,"addOptionAllToList(): adding 'All' as the first option value to the list...");
           try
           {
               if(optionList != null)
               {

                       //To support displaying 'All' option.

                      newOptionList = new String [2][optionList[0].length+1];

                      //Assigning empty string to the first option-list value

                      newOptionList[0][0]=TagConstants.NL;

                      System.arraycopy(optionList[0],0,newOptionList[0],1,optionList[0].length);

                      //Assigning 'All' value to the first option-list display value

                      newOptionList[1][0]= ALL_OPTION;

                      System.arraycopy(optionList[1],0,newOptionList[1],1,optionList[1].length);
              }
              else //If optionList is null, then add "All" as the display value and blank value as option value to the List.
              {
                      newOptionList = new String [2][1];

                      newOptionList[0][0]=TagConstants.NL;

                      newOptionList[1][0]=ALL_OPTION;
              }
              return newOptionList;
           }
           catch (Exception e)
           {
              Debug.error("addOptionAllToList(): Failed to add option 'All' to the Option list\n" + e.getMessage());
           }
           return null;
       }

    public PageContext getPageContext()
    {
        return pageContext;
    }

    public void setPageContext(PageContext pageContext)
    {
        this.pageContext = pageContext;
    }

}//End of TagFunctions.
