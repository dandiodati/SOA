package com.nightfire.framework.test;

import java.util.*;
import com.nightfire.framework.locale.*;

public class TestResource extends NFResource
{
    protected static final String HELLO_WORLD = "Hello World!";
    protected static final String OK = "OK";
    protected static final String TIME = "The Time is now: ";
    protected static final String FORMAT = "{2} first " +
                                           "{0} second " +
                                           "{1} third."; 


    public TestResource() {

      Properties myContents = new Properties();

      myContents.put(HELLO_WORLD  , HELLO_WORLD);
      myContents.put(OK           , OK);
      myContents.put(TIME         , TIME );
      myContents.put(FORMAT       , FORMAT);

      setContents(myContents);

   }

    /**
     * Implemtation of Get Base Name 
     * is required by the ResourceBundle 
     * for Resourcing to function properly.
     *
     * @return the fully qualified class name of the 
     *         base resource class.
     */
    public static String getBaseName(){

        return TestResource.class.getName();   

   }
} 


