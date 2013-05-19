package com.nightfire.framework.test;

import junit.framework.*;
import com.nightfire.framework.util.*;
import java.util.*;


public class TestPropUtils extends TestCase
{
  private HashMap props;

  public TestPropUtils(String name)
  {
     super(name);
  }

  public void setUp()
  {
     props = new HashMap();

     props.put("PROPERTY", "blah");
     props.put("REQ_PROPERTY", "blah");

  }
  public void testGetPropertyValue()
  {
     String temp = (String) PropUtils.getPropertyValue(props, "PROPERTY");
     assertTrue("Property should not be null", temp != null);

  }
  public void testGetMissingPropertyValue()
  {
     String temp = (String) PropUtils.getPropertyValue(props, "NO_SUCH_PROPERTY");
     assertTrue("Property should be null", temp == null);
  }

  public void testGetReqProperty()
  {
     String exceptionThrown = new String();
     String temp;

     try {
       temp = PropUtils.getRequiredPropertyValue(props,"REQ_PROPERTY");
     } catch (FrameworkException pe) {
        exceptionThrown = pe.getMessage();
     }
     assertTrue("An exception should have been thrown with a message containing the words 'Required property'", exceptionThrown.indexOf("Required property") == -1);


  }

  public void testGetReqMissingProperty()
  {
     String exceptionThrown = new String();

     try {
       PropUtils.getRequiredPropertyValue(props,"NO_SUCH_REQ_PROPERTY");
     } catch (FrameworkException pe) {
        exceptionThrown = pe.getMessage();
     }

     assertTrue("An exception should have been thrown with a message containing the words 'Required property'", exceptionThrown.indexOf("Required property") > -1);


  }

  public void testGetReqmissingPropertyWithErroBuf()
  {
     StringBuffer errorBuf = new StringBuffer();


       PropUtils.getRequiredPropertyValue(props,"REQ_PROPERTY", errorBuf);
       PropUtils.getRequiredPropertyValue(props,"BLAH_REQ_PROPERTY", errorBuf);
       PropUtils.getRequiredPropertyValue(props,"NO_REQ_PROPERTY", errorBuf);
       PropUtils.getRequiredPropertyValue(props,"BLAH_BLAH_REQ_PROPERTY", errorBuf);
       System.out.println("Got a message : " + errorBuf.toString());
     assertTrue("An error buffer should have a message containing the words 'Required property'", errorBuf.toString().indexOf("Required property") > -1);



  }
}
