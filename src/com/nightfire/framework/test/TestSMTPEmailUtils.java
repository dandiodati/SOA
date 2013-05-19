package com.nightfire.framework.test;

import java.io.*;
import java.util.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.email.*;
import com.nightfire.unittest.*;
import com.nightfire.framework.db.*;


/*
 * Example unit test driver.
 */
public class TestSMTPEmailUtils extends UnitTestBase
{
    private static String fileName [] = new String [5];
    private static String userEmailAddress = null;
    private static String smtpServerAddress = "192.168.10.6";
    private static boolean debugFlag = false;

    /**
     * @param  args  Array of strings containing command-line arguments.
     */
    public static void main ( String[] args )
    {
        // Configure which debug log levels are displayed.
        Debug.enableAll( );
        //Debug.disableAll( );
        //Debug.enable( Debug.UNIT_TEST );
        //Debug.enable( Debug.ASSERT );
        // Show or hide logging-level as a part of each log message.
        Debug.showLevels( );
        //Debug.hideLevels( );
        // Hide the time-stamping of each log message.
        //Debug.disableTimeStamping( );

        //FrameworkException.showStackTrace( );

        // Write results to file with appropriate name.
        // redirectLogs( "example_unit_test.out" );

        if ( args.length != 5 )
        {
          log ( "Usage is : java -classpath %CLASSPATH% com.nightfire.spi.common.test.communications.TestSMTPEmailUtils " +
          "<userEmailAddr> rtfFileName emailFileName textFileName wordFileName");
          log ( "Example usage is : java -classpath %CLASSPATH% com.nightfire.spi.common.test.communications.TestSMTPEmailUtils " +
          "sonali@nightfire.com rtfFile.rtf email.txt textFile.txt wordFile.doc");
          System.exit( -1 );
        }
        else
        {
          userEmailAddress = args [0];
          fileName [0] = args [1];//rtfFileName
          fileName [1] = args [2];//emailFileName
          fileName [2] = args [3];//textFileName
          fileName [3] = args [4];//wordFileName
        }

        String myTests[] = {
            "First unit test-Inline + Attachment in rtf format",
            "Second unit test-Inline only with email composed externally",
            "Third unit test-Inline only text",
            "Fourth unit test-Attachment only - word format",
            "Fifth unit test-Inline + Attachment in text format with many headers and method calls in random order",
            "Sixth unit test-Inline only text with only basic method calls used",
            "Seventh unit test-Inline text with 2 attachments-word and text",
            "Eighth unit test-No message with only basic method calls to compose email-invalid case",
            "Ninth unit test-Inline only with incorrect email composed externally",
            "Tenth unit test-Inline correct but null attachments used to compose email"
        };

        testNames = myTests;

        // Concrete child class should provide names of unit tests.
        assertTrue( testNames != null, "'testNames' variable is not null." );

        testCounter = 0;

        try
        {
            // PUT YOUR UNIT TESTS HERE!!!
            logTestStart( );
            unit_test_one( );
            logTestEnd( );

            logTestStart( );
            unit_test_two( );
            logTestEnd( );

            logTestStart( );
            unit_test_three( );
            logTestEnd( );

            logTestStart( );
            unit_test_four( );
            logTestEnd( );

            logTestStart( );
            unit_test_five( );
            logTestEnd( );

            logTestStart( );
            unit_test_six( );
            logTestEnd( );

            logTestStart( );
            unit_test_seven( );
            logTestEnd( );

            logTestStart( );
            try
            {
              unit_test_eight( );
            }
            catch ( Exception e )
            {
        	    log( "Unit test \"" + testNames[testCounter] +
                 "\" failed with the following exception:\n" + e.toString() );

              log( getStackTrace( e ) );
            }
            logTestEnd( );

            logTestStart( );
            try
            {
              unit_test_nine( );
            }
            catch ( Exception e )
            {
        	    log( "Unit test \"" + testNames[testCounter] +
                 "\" failed with the following exception:\n" + e.toString() );

              log( getStackTrace( e ) );
            }
            logTestEnd( );

            logTestStart( );
            try
            {
              unit_test_ten( );
            }
            catch ( Exception e )
            {
        	    log( "Unit test \"" + testNames[testCounter] +
                 "\" failed with the following exception:\n" + e.toString() );

              log( getStackTrace( e ) );
            }
            logTestEnd( );

        }
        catch ( Exception e )
        {
        	log( "Unit test \"" + testNames[testCounter] +
                 "\" failed with the following exception:\n" + e.toString() );

            log( getStackTrace( e ) );
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


    /*************************************************************************/

    private static void unit_test_one ( ) throws Exception
    {
      String message = FileUtils.readFile ( fileName[0] );
      SMTPEmailUtils newSMTPEmailUtils = new SMTPEmailUtils ( );
      newSMTPEmailUtils.initServer ( smtpServerAddress , debugFlag );
      newSMTPEmailUtils.setSender ( userEmailAddress );
      newSMTPEmailUtils.addRecipient ( userEmailAddress );
      newSMTPEmailUtils.addCC ( userEmailAddress );
      newSMTPEmailUtils.addBCC ( userEmailAddress );
      newSMTPEmailUtils.addHeader ( "Importance", "Normal" );
      newSMTPEmailUtils.setSubject ( "Testing this SMTP EmailUtils class-Test1, " +
      testNames[0] );

      String inputMessageArray [] = new String [1];
      inputMessageArray [0] = new String ( message );
      String attachmentTypeArray [] = new String [1];
      attachmentTypeArray [0] = new String ( "application/rtf" );
      newSMTPEmailUtils.setMessageContent ( message,
          inputMessageArray, attachmentTypeArray );

      //Send message using SMTPEmailUtils's sendEmail method as follows:
      newSMTPEmailUtils.sendEmail ();

    }


    /*************************************************************************/

    private static void unit_test_two ( )  throws Exception
    {
      String message = FileUtils.readFile ( fileName [1] );
      SMTPEmailUtils newSMTPEmailUtils = new SMTPEmailUtils ( );
      newSMTPEmailUtils.sendEmail ( smtpServerAddress, message, debugFlag );
    }


    /*************************************************************************/

    private static void unit_test_three ( ) throws Exception
    {
      String message = FileUtils.readFile ( fileName[2] );
      SMTPEmailUtils newSMTPEmailUtils = new SMTPEmailUtils ( );
      newSMTPEmailUtils.initServer ( smtpServerAddress , debugFlag );
      newSMTPEmailUtils.setSender ( userEmailAddress );
      newSMTPEmailUtils.addBCC ( userEmailAddress );
      newSMTPEmailUtils.addHeader ( "Importance", "High" );
      newSMTPEmailUtils.setSubject ( "Testing this SMTP EmailUtils class-Test3, " +
      testNames[2]);

      newSMTPEmailUtils.setMessageContent ( message );

      //Send message using SMTPEmailUtils's sendEmail method as follows:
      newSMTPEmailUtils.sendEmail ();


    }

    /*************************************************************************/

    private static void unit_test_four ( ) throws Exception
    {
      //check if only attachment??
      String message = FileUtils.readFile ( fileName[3] );
      SMTPEmailUtils newSMTPEmailUtils = new SMTPEmailUtils ( );
      newSMTPEmailUtils.initServer ( smtpServerAddress, debugFlag );
      newSMTPEmailUtils.setSender ( userEmailAddress );
      newSMTPEmailUtils.addRecipient ( userEmailAddress );
      newSMTPEmailUtils.addCC ( userEmailAddress );
      newSMTPEmailUtils.addBCC ( userEmailAddress );
      newSMTPEmailUtils.addHeader ( "Importance", "Normal" );
      newSMTPEmailUtils.setSubject ( "Testing this SMTP EmailUtils class-Test4, " +
      testNames[3]);

      String inputMessageArray [] = new String [1];
      inputMessageArray [0] = new String ( message );
      String attachmentTypeArray [] = new String [1];
      attachmentTypeArray [0] = new String ( "application/msword" );
      newSMTPEmailUtils.setMessageContent ( inputMessageArray, attachmentTypeArray );

      //Send message using SMTPEmailUtils's sendEmail method as follows:
      newSMTPEmailUtils.sendEmail ();
    }

    /*************************************************************************/

    private static void unit_test_five ( ) throws Exception
    {
      String message = FileUtils.readFile ( fileName[2] );
      SMTPEmailUtils newSMTPEmailUtils = new SMTPEmailUtils ( );

      newSMTPEmailUtils.initServer ( smtpServerAddress , debugFlag );
      String inputMessageArray [] = new String [1];
      inputMessageArray [0] = new String ( message );
      String attachmentTypeArray [] = new String [1];
      attachmentTypeArray [0] = new String ( "text/plain" );
      newSMTPEmailUtils.setMessageContent ( message,
          inputMessageArray, attachmentTypeArray );

      newSMTPEmailUtils.addHeader ( "Importance", "High" );
      newSMTPEmailUtils.addHeader ( "X-Return-Path", "sonali@nightfire.com");
      newSMTPEmailUtils.addCC ( userEmailAddress );
      newSMTPEmailUtils.addHeader ( "In-Reply-To", "dummy_user@abc.com");
      newSMTPEmailUtils.setSender ( "sonals@rocketmail.com" );
      newSMTPEmailUtils.addRecipient ( userEmailAddress );
      newSMTPEmailUtils.setSender ( userEmailAddress );
      newSMTPEmailUtils.setSubject ( "Testing this SMTP EmailUtils class-Test5, " +
      testNames[4]);
      newSMTPEmailUtils.setSubject ( "Testing SMTP EmailUtils class-Test5, " +
      testNames[4] + "-new heading");

      //Send message using SMTPEmailUtils's sendEmail method as follows:
      newSMTPEmailUtils.sendEmail ( );

      newSMTPEmailUtils.printMessage ( );
    }

    private static void unit_test_six ( ) throws Exception
    {
      String message = FileUtils.readFile ( fileName[2] );
      SMTPEmailUtils newSMTPEmailUtils = new SMTPEmailUtils ( );
      newSMTPEmailUtils.initServer ( smtpServerAddress , debugFlag );
      newSMTPEmailUtils.setSender ( userEmailAddress );
      newSMTPEmailUtils.addRecipient ( userEmailAddress );
      newSMTPEmailUtils.setMessageContent ( message );

      //Send message using SMTPEmailUtils's sendEmail method as follows:
      newSMTPEmailUtils.sendEmail ();
    }

    private static void unit_test_seven ( ) throws Exception
    {
      String messageTxt = FileUtils.readFile ( fileName[2] );
      String messageWord = FileUtils.readFile ( fileName[3] );

      SMTPEmailUtils newSMTPEmailUtils = new SMTPEmailUtils ( );
      newSMTPEmailUtils.initServer ( smtpServerAddress , true );
      newSMTPEmailUtils.setSender ( userEmailAddress );
      newSMTPEmailUtils.addCC ( userEmailAddress );
      newSMTPEmailUtils.addBCC ( userEmailAddress );
      newSMTPEmailUtils.addHeader ( "Importance", "Normal" );
      newSMTPEmailUtils.setSubject ( "Testing this SMTP EmailUtils class-Test7, " +
      testNames[0] );

      String inputMessageArray [] = new String [2];
      inputMessageArray [0] = new String ( messageWord );
      inputMessageArray [1] = new String ( messageTxt );

      String attachmentTypeArray [] = new String [2];
      attachmentTypeArray [0] = new String ( "application/msword" );
      attachmentTypeArray [1] = new String ( "text/plain" );

      newSMTPEmailUtils.setMessageContent ( messageTxt,
          inputMessageArray, attachmentTypeArray );

      //Send message using SMTPEmailUtils's sendEmail method as follows:
      newSMTPEmailUtils.sendEmail ();
    }

    /*************************************************************************/

    private static void unit_test_eight ( ) throws Exception
    {
      SMTPEmailUtils newSMTPEmailUtils = new SMTPEmailUtils ( );
      newSMTPEmailUtils.initServer ( smtpServerAddress , debugFlag );
      newSMTPEmailUtils.setSender ( userEmailAddress );
      newSMTPEmailUtils.addRecipient ( userEmailAddress );

      //Send message using SMTPEmailUtils's sendEmail method as follows:
      newSMTPEmailUtils.sendEmail ();
    }

    /*************************************************************************/

    private static void unit_test_nine ( )  throws Exception
    {
      String message = FileUtils.readFile ( fileName [2] );
      SMTPEmailUtils newSMTPEmailUtils = new SMTPEmailUtils ( );
      newSMTPEmailUtils.sendEmail ( smtpServerAddress, message, debugFlag );
    }

    /*************************************************************************/

    private static void unit_test_ten ( )  throws Exception
    {
      String messageTxt = FileUtils.readFile ( fileName[2] );
      SMTPEmailUtils newSMTPEmailUtils = new SMTPEmailUtils ( );
      newSMTPEmailUtils.initServer ( smtpServerAddress , true );
      newSMTPEmailUtils.setSender ( userEmailAddress );
      newSMTPEmailUtils.addCC ( userEmailAddress );
      newSMTPEmailUtils.setSubject ( "Testing this SMTP EmailUtils class-Test10, " +
      testNames[0] );

      String inputMessageArray [] = new String [2];
      String attachmentTypeArray [] = new String [2];
      newSMTPEmailUtils.setMessageContent ( messageTxt,
          inputMessageArray, attachmentTypeArray );

      //Send message using SMTPEmailUtils's sendEmail method as follows:
      newSMTPEmailUtils.sendEmail ();
    }

}

