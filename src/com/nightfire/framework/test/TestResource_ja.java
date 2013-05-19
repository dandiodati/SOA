package com.nightfire.framework.test;

import java.util.*;

public class TestResource_ja extends TestResource
{
    protected static final String L_HELLO_WORLD = "local";
    protected static final String L_OK = "local";
    protected static final String L_TIME = "local";

   public TestResource_ja() {

      super();
      Properties myContents = new Properties();

      myContents.put(HELLO_WORLD  , L_HELLO_WORLD);
      myContents.put(OK           , L_OK);
      myContents.put(TIME         , L_TIME );

      setContents(myContents);

   }
} 

