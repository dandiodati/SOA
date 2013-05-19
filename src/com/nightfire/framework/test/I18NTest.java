package com.nightfire.framework.test;

import java.util.*;
import com.nightfire.unittest.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.locale.*;

public class I18NTest extends UnitTestBase
{
    private static final String LANG_PROP  = "CURRENT_LANGUAGE";
    private static final String COUNTRY_PROP  = "CURRENT_COUNTRY";
    private static final String ENGLISH = "EN";
    private static final String US = "US";
    private static final String FRENCH = "FR";
    private static final String CANADA = "CA";
    private static final String FOO = null;
    private static final String BAR = null;
    private static final String HELLO_KEY = TestResource.HELLO_WORLD ;
    private static final String TIME_KEY = TestResource.TIME;
    private static final String CURRENCY_KEY = "CURRENCY";
    private static final String GOOD_PACKAGE = "com.nightfire.framework.test.Test";
    private static final String BAD_PACKAGE = "com.nightfire.foo";
    private static final String EXPECT_1 =  "Hello World!";
    private static final String EXPECT_2 = "Bonjour!";
    private static final String EXPECT_3 = "The Time is now: ";
    private static final String EXPECT_4 = CURRENCY_KEY;
    private static final String EXPECT_5 = HELLO_KEY;
    private static final String EXPECT_6 = HELLO_KEY;

    private static final String ZERO = "ZERO";
    private static final String ONE = "ONE";
    private static final String TWO = "TWO";
    private static final String GOOD_FORMAT_KEY = TestResource.FORMAT;
    private static final String BAD_FORMAT_KEY = "TEST_FORMAT";
    private static final String BAD_RESOURCE_TYPE  = "TEST_RESOURCE";
    private static final String FORMAT_EXPECT_1 = 
            "TWO first ZERO second ONE third.";
    private static final String FORMAT_EXPECT_2 = 
            "ONE second ZERO third TWO first.";
    private static final String FORMAT_EXPECT_3 = 
            "TEST_FORMAT";
    private static final String FORMAT_EXPECT_4 = 
            "{2} first {0} second {1} third.";

    private static final String TEST_CATALOG_NAME = "TEST_CATALOG";
    private static final String BAD_CATALOG_NAME = "BAD_CATALOG";

    private static final String myTests[] = {"Get Base Defined Resource",
            "Get Defined Resource",
            "Get Only Base Defined Resource",
            "Get Undefined Resource",
            "Test no NFResource Defined",
            "Test Locale init failure",
            "Format a Message with Placeholders at the front and back",
            "Format a Message with Placeholders changing order",
            "Format a message with literal portions changing order",
            "Format a message where the NFResource has not been defined",
            "Format a message Where the Format has not been defined",
            "Format a message with a null format",
            "Format a message with null args",
            "Format a message with too few args",
            "Format a message with a null in the args"};


    public static void main(String [] args)
    {
        testNames = myTests; 
        testCounter = 0;
        boolean initialized = false;
        Debug.enableAll();
        try
	{
            logTestStart();
            initialized = init(TEST_CATALOG_NAME, GOOD_PACKAGE , ENGLISH , US);
            Debug.log(null, Debug.ALL_ERRORS, 
                          "Initialization success:" + initialized); 
            if(test(TEST_CATALOG_NAME, HELLO_KEY , EXPECT_1) && initialized)
                logTestEnd();             
            else
	    {
                Debug.log(null, Debug.ALL_ERRORS, 
                          "Failed Test 1:" + testNames[0]); 
	    }
       }
        catch(Exception e)
	{
            Debug.log(null, Debug.ALL_ERRORS, 
                      "Failed Test 1:" + testNames[0] +
                      ":" + e); 
        }
        initialized = false;
        try
	{
            logTestStart();
            initialized = init(TEST_CATALOG_NAME, GOOD_PACKAGE , FRENCH , CANADA);
            Debug.log(null, Debug.ALL_ERRORS, 
                          "Initialization success:" + initialized); 
            if(test(TEST_CATALOG_NAME, HELLO_KEY ,EXPECT_2 ) && initialized)
                logTestEnd();
            else
	    {
                Debug.log(null, Debug.ALL_ERRORS, 
                          "Failed Test 2:" + testNames[1]); 
	    }
        }
        catch(Exception e)
	{
            Debug.log(null, Debug.ALL_ERRORS, 
                      "Failed Test 2:" + testNames[1] + 
                      ":" + e); 
        }
        initialized = false;
        try
	{
            logTestStart();
            initialized = init(TEST_CATALOG_NAME, GOOD_PACKAGE  ,  FRENCH, CANADA);
            Debug.log(null, Debug.ALL_ERRORS, 
                          "Initialization success:" + initialized); 
            if(test(TEST_CATALOG_NAME, TIME_KEY ,EXPECT_3) && initialized)
                logTestEnd( );
            else
	    {
                Debug.log(null, Debug.ALL_ERRORS, 
                          "Failed Test 3:" + testNames[2]); 
	    }
        }
        catch(Exception e)
	{
            Debug.log(null, Debug.ALL_ERRORS, 
                      "Failed Test 3:" + testNames[2] + 
                      ":" + e); 
        }
        initialized = false;
        try
	{
            logTestStart();
            initialized = init(TEST_CATALOG_NAME,  GOOD_PACKAGE , FRENCH , CANADA);
            Debug.log(null, Debug.ALL_ERRORS, 
                          "Initialization success:" + initialized); 
            if(test(TEST_CATALOG_NAME, CURRENCY_KEY ,EXPECT_4 ) && initialized)
                logTestEnd();
            else
	    {
                Debug.log(null, Debug.ALL_ERRORS, 
                          "Failed Test 4:" + testNames[3]); 
	    }
        }
        catch(Exception e)
	{
            Debug.log(null, Debug.ALL_ERRORS, 
                      "Failed Test 4:" + testNames[3] +  
                      ":" + e); 
        }
        initialized = false;
        try
	{
            logTestStart();
            initialized = init(BAD_CATALOG_NAME,  BAD_PACKAGE ,  FRENCH, CANADA);
            Debug.log(null, Debug.ALL_ERRORS, 
                          "Initialization success:" + initialized); 
            if(test(BAD_CATALOG_NAME,  HELLO_KEY, EXPECT_5 ) && !initialized)
                logTestEnd();
            else
	    {
                Debug.log(null, Debug.ALL_ERRORS, 
                          "Failed Test 5:" + testNames[4]); 
	    }
        }
        catch(Exception e)
	{
            Debug.log(null, Debug.ALL_ERRORS, 
                      "Failed Test 5:" + testNames[4] + 
                      ":" + e); 
        }

            logTestStart();
        /*
          // This test case is no longer valid, as the catalog objects
          // are cached internally by the locale object.
        initialized = false;
        try
	{
            logTestStart();
            initialized = init(TEST_CATALOG_NAME,  GOOD_PACKAGE , FOO , BAR);
            Debug.log(null, Debug.ALL_ERRORS, 
                          "Initialization success:" + initialized); 
            if(test(TEST_CATALOG_NAME, HELLO_KEY ,EXPECT_6 ) && !initialized)
                logTestEnd();
            else
	    {
                Debug.log(null, Debug.ALL_ERRORS, 
                          "Failed Test 6:" + testNames[5]); 
	    }
        }
        catch(Exception e)
	{
            Debug.log(null, Debug.ALL_ERRORS, 
                      "Failed Test 6:" + testNames[5] + 
                      ":" + e); 
        }
        */
            logTestEnd();

        try
	{
            logTestStart();
            initialized = init(TEST_CATALOG_NAME,  GOOD_PACKAGE ,ENGLISH, US);
            Debug.log(null, Debug.ALL_ERRORS, 
                          "Initialization success:" + initialized); 
            if(testFormat(GOOD_FORMAT_KEY , FORMAT_EXPECT_1) && initialized)
                logTestEnd();
            else
	    {
                Debug.log(null, Debug.ALL_ERRORS, 
                          "Failed Test 7:" + testNames[6]); 
	    }
        }
        catch(Exception e)
	{
            Debug.log(null, Debug.ALL_ERRORS, 
                      "Failed Test 7:" + testNames[6] + 
                      ":" + e); 
        }
        try
	{
            logTestStart();
            initialized = init(TEST_CATALOG_NAME,  GOOD_PACKAGE ,FRENCH, CANADA);
            Debug.log(null, Debug.ALL_ERRORS, 
                          "Initialization success:" + initialized); 
            if(testFormat(GOOD_FORMAT_KEY , FORMAT_EXPECT_2) && initialized)
                logTestEnd();
            else
	    {
                Debug.log(null, Debug.ALL_ERRORS, 
                          "Failed Test 8:" + testNames[7]); 
	    }
        }
        catch(Exception e)
	{
            Debug.log(null, Debug.ALL_ERRORS, 
                      "Failed Test 8:" + testNames[7] + 
                      ":" + e); 
        }
        try
	{
            logTestStart();
            initialized = init(TEST_CATALOG_NAME,  GOOD_PACKAGE ,FRENCH ,CANADA);
            Debug.log(null, Debug.ALL_ERRORS, 
                          "Initialization success:" + initialized); 
            if(testFormat(GOOD_FORMAT_KEY , FORMAT_EXPECT_2) && initialized)
                logTestEnd();
            else
	    {
                Debug.log(null, Debug.ALL_ERRORS, 
                          "Failed Test 9:" + testNames[8]); 
	    }
        }
        catch(Exception e)
	{
            Debug.log(null, Debug.ALL_ERRORS, 
                      "Failed Test 9:" + testNames[8] + 
                      ":" + e); 
        }
        try
	{
            logTestStart();
            initialized = init(TEST_CATALOG_NAME,  BAD_RESOURCE_TYPE ,ENGLISH, US);
            Debug.log(null, Debug.ALL_ERRORS, 
                          "Initialization success:" + initialized); 
            if(testFormat(BAD_FORMAT_KEY , FORMAT_EXPECT_3) && !initialized)
                logTestEnd();
            else
	    {
                Debug.log(null, Debug.ALL_ERRORS, 
                          "Failed Test 10:" + testNames[9]); 
	    }
        }
        catch(Exception e)
	{
            Debug.log(null, Debug.ALL_ERRORS, 
                      "Failed Test 10:" + testNames[9] + 
                      ":" + e); 
        }
        try
	{
            logTestStart();
            initialized = init(TEST_CATALOG_NAME,  GOOD_PACKAGE ,ENGLISH, US);
            Debug.log(null, Debug.ALL_ERRORS, 
                          "Initialization success:" + initialized); 
            if(testFormat(BAD_FORMAT_KEY , FORMAT_EXPECT_3) && initialized)
                logTestEnd();
            else
	    {
                Debug.log(null, Debug.ALL_ERRORS, 
                          "Failed Test 11:" + testNames[10]); 
	    }
        }
        catch(Exception e)
	{
            Debug.log(null, Debug.ALL_ERRORS, 
                      "Failed Test 11:" + testNames[10] + 
                      ":" + e); 
        }
        try
	{
            logTestStart();
            initialized = init(TEST_CATALOG_NAME,  GOOD_PACKAGE ,ENGLISH, US);
            Debug.log(null, Debug.ALL_ERRORS, 
                          "Initialization success:" + initialized); 
            if(testNullFormat() && initialized)
                logTestEnd();
            else
	    {
                Debug.log(null, Debug.ALL_ERRORS, 
                          "Failed Test 12:" + testNames[11]); 
	    }
        }
        catch(Exception e)
	{
            Debug.log(null, Debug.ALL_ERRORS, 
                      "Failed Test 12:" + testNames[11] + 
                      ":" + e); 
        }
        try
	{
            logTestStart();
            initialized = init(TEST_CATALOG_NAME,  GOOD_PACKAGE ,ENGLISH, US);
            Debug.log(null, Debug.ALL_ERRORS, 
                          "Initialization success:" + initialized); 
            if(testFormatNullArgs(GOOD_FORMAT_KEY, FORMAT_EXPECT_4) && initialized)
                logTestEnd();
            else
	    {
                Debug.log(null, Debug.ALL_ERRORS, 
                          "Failed Test 13:" + testNames[12]); 
	    }
        }
        catch(Exception e)
	{
            Debug.log(null, Debug.ALL_ERRORS, 
                      "Failed Test 13:" + testNames[12] + 
                      ":" + e); 
        }
        try
	{
            logTestStart();
            initialized = init(TEST_CATALOG_NAME,  GOOD_PACKAGE ,ENGLISH, US);
            Debug.log(null, Debug.ALL_ERRORS, 
                          "Initialization success:" + initialized); 
            if(testFormatTooFewArgs(GOOD_FORMAT_KEY, 
                                    FORMAT_EXPECT_4) && initialized)
                logTestEnd();
            else
	    {
                Debug.log(null, Debug.ALL_ERRORS, 
                          "Failed Test 14:" + testNames[13]); 
	    }
        }
        catch(Exception e)
	{
            Debug.log(null, Debug.ALL_ERRORS, 
                      "Failed Test 14:" + testNames[13] + 
                      ":" + e); 
        }
        try
	{
            logTestStart();
            initialized = init(TEST_CATALOG_NAME,  GOOD_PACKAGE ,ENGLISH, US);
            Debug.log(null, Debug.ALL_ERRORS, 
                          "Initialization success:" + initialized); 
            if(testFormatNullInArgs(GOOD_FORMAT_KEY) && initialized)
                logTestEnd();
            else
	    {
                Debug.log(null, Debug.ALL_ERRORS, 
                          "Failed Test 15:" + testNames[14]); 
	    }
        }
        catch(Exception e)
	{
            Debug.log(null, Debug.ALL_ERRORS, 
                      "Failed Test 15:" + testNames[14] + 
                      ":" + e); 
        }

        log( "\t\t\tTEST SUMMARY");
        log( "\tTotal test cases: " + testNames.length );
        log( "\tTotal passed:     " + testCounter );
        if ( testCounter == testNames.length ) 
        {
    	    log( "All tests finished successfully." );
        }
        else
        {
    	    log( "At least one test failed." );
        }    
    }    

    private static boolean init(String catalog, String type, String lang, String country)
    {
        boolean success = false;
        log("initializing with type " + "[" + type + "]" + 
            " lang " + "["  + lang + "]" + 
            " and country " + "[" + country + "]"); 
        try
        {
            Properties props = System.getProperties();
            props.put(LANG_PROP, lang);
            props.put(COUNTRY_PROP, country);
            System.setProperties(props);
            success = NFLocale.init( catalog, type);
        }
        catch(Exception exp)
        {
            Debug.log(null, Debug.ALL_ERRORS,
                      "Failed initialzing from [" + lang + "]" + 
                      "[" + country + "]");
        }
        return success;
    }

    private static boolean test(String catalog, String key, String expected)
    {    
        log("Looking for: " + key + " expecting: " + expected);
        String got = NFLocale.getString(catalog, key);
        log("got: " + got);
        return got.equals(expected);
    }

    private static boolean testFormat(String key, String expected)
    {    
        log("Formatting: " + key + " expecting: " + expected);
        String got = NFLocale.format(TEST_CATALOG_NAME, key, ZERO, ONE, TWO);
        log("got: " + got);
        return got.equals(expected);
    }

    private static boolean testNullFormat()
    {    
        log("Formatting: " + null + " expecting: " + null);
        String got = NFLocale.format(TEST_CATALOG_NAME, null, ZERO, ONE, TWO);
        log("got: " + got);
        return null == got;
    }

    private static boolean testFormatTooFewArgs(String key, String expected)
    {    
        log("Formatting: " + key + " expecting: " + expected);
        String got = NFLocale.format(TEST_CATALOG_NAME, key, ONE, TWO);
        log("got: " + got);
        return !got.equals(expected);
    }

    private static boolean testFormatNullArgs(String key, String expected)
    {    
        log("Formatting: " + key + " expecting: " + expected);
        Object [] args = null;
        String got = NFLocale.format(TEST_CATALOG_NAME, key, args);
        log("got: " + got);
        return got.equals(expected);
    }

    private static boolean testFormatNullInArgs(String key)
    {    
        log("Formatting: " + key + " expecting a string return value." );
        String got = NFLocale.format(TEST_CATALOG_NAME, key, ZERO, null, TWO);
        log("got: " + got);
        if(null != got && got instanceof String)
            return true;
        else
            return false;
    }
}




