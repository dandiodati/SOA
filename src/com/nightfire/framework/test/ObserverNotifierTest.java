package com.nightfire.framework.test;

import com.nightfire.framework.util.*;

public class ObserverNotifierTest 
{
    public static void main (String[] args)
    {
        if (args.length != 2)
        {
            System.err.println("\nUsage: java ObserverNotifierTest <Observer Time Out Period in seconds> <Notifier sleep Time in seconds>");
            System.exit(-1);
        }
        String timeOutPeriodString = args[0];
        String notifierSleepTimeString = args[1];
        
        int timeOutPeriod = 10;
        
        try
        {
            timeOutPeriod = Integer.parseInt ( timeOutPeriodString ) ;
        }
        catch (NumberFormatException e)
        {
            System.err.println("Should supply an integer value ");
            System.exit(-1);
        }
        Debug.enableAll();
        ObserverNotifierTest wnt = new ObserverNotifierTest();
        wnt.test( timeOutPeriod, notifierSleepTimeString );
    }

    public void test(int timeOut, String notifierSleepTime )
    {
        Debug.log(Debug.IO_STATUS, "Creating NFNotifier ...");
        TestNotifier notifier = new TestNotifier();
        Debug.log(Debug.IO_STATUS, "Creating NFObserver with timeout period [" + timeOut +"] seconds");
        NFObserver observer = new NFObserver( notifier, timeOut * 1000 );

        //Set the context which can be used by the notifier
        observer.setContext( notifierSleepTime );
        observer.executeNotifier();

        if (observer.isNotified())
        {
            Debug.log(Debug.IO_STATUS, "Observer notified by notifier before timing out ");
        }
        else
        {
            Debug.log(Debug.IO_STATUS, "Observer timed out before getting notified ");            
        }
        
    }

    /**
     * Test Notifier
     */
    private class TestNotifier extends NFNotifier
    {

        public TestNotifier()
        {

        }
        
        public void executeNotifier(NFObserver observer)
        {
            String notifierSleepTime = (String) observer.getContext();
            int sleepTime = Integer.parseInt(notifierSleepTime) ;
            Debug.log(Debug.IO_STATUS, "Test Notifier sleeping for [" + sleepTime +"] seconds");
            try
            {
                Thread.sleep ( sleepTime * 1000 ) ;
            }
            catch (InterruptedException e)
            {
                Debug.log(Debug.ALL_ERRORS, "Notifier Thread interrupted.");
            }
        }
    
    }
}
