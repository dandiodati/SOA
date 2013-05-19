package com.nightfire.framework.test;

import java.util.*;

public class TestResource_fr_CA extends TestResource
{
    protected static final String L_HELLO_WORLD = "Bonjour!";
    protected static final String L_OK = "local";
    protected static final String L_FORMAT = "{1} second " +
                                           "{0} third " +
                                           "{2} first."; 


    public TestResource_fr_CA() {
      Properties myContents = new Properties();

      myContents.put(HELLO_WORLD  , L_HELLO_WORLD);
      myContents.put(OK           , L_OK);
      myContents.put(FORMAT       , L_FORMAT);

      setContents(myContents);

   }
} 

