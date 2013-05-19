///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter.smartsockets;

import com.smartsockets.TipcDefs;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FileUtils;
import com.nightfire.framework.util.FrameworkException;

public class SOAClient extends SmartSocketsClient{

   public static final String SOA_MESSAGE_TYPE = "SOA_MESSAGE";
   private static final int SOA_MESSAGE_TYPE_NUMBER = 25000;
   private static final String SOA_MESSAGE_TYPE_PARAMS = "xml";
   private static final int SOA_LOAD_BALANCE_MODE = TipcDefs.LB_ROUND_ROBIN;

   public SOAClient(String project,
                    String subject,
                    String server)
                    throws FrameworkException{

      super( project,
             subject,
             null,
             null,
             server,
             SOA_MESSAGE_TYPE,
             SOA_MESSAGE_TYPE_NUMBER,
             SOA_MESSAGE_TYPE_PARAMS,
             SOA_LOAD_BALANCE_MODE,
             null );

   }

   // For testing purposes only
   public static void main(String[] argv){

      String usage = "USAGE: java "+SOAClient.class.getName()+
                     " <project> <subject> <server> <XML filename>";

      if( argv.length != 4 ){

         System.err.println(usage);
         System.exit(-1);

      }

      SOAClient client = null;

      // project name, e.g. SOA_DEV1
      String project = null;

      // subject name, e.g. SOA_0001
      String subject = null;

      // the host (or list of hosts) where an RTServer
      // instance is running
      String server = null;

      // the name of the file containing the input XML to send
      String filename = null;



      for( int i = 0; i < argv.length; i++ ){

         if( project == null ){
            project = argv[i];
         }
         else if( subject == null ){
            subject = argv[i];
         }
         else if( server == null ){
            server = argv[i];
         }
         else if( filename == null ){
            filename = argv[i];
         }

      }

      Debug.setThreadLogFileName("soaclientLog.txt");
      Debug.enableAll();

      try{

         String message = FileUtils.readFile( filename );

         client = new SOAClient(project, subject, server);

         boolean quit = false;
         java.io.BufferedReader input =
            new java.io.BufferedReader(
               new java.io.InputStreamReader( System.in ) );

         while( !quit ){

            try{
               System.out.println("Sending message:\n"+message);
               client.sendXML(message);
               System.out.println("Message delivered successfully to RTServer.");
            }
            catch(FrameworkException ex){
               System.out.println("Error sending message: "+ex);
               Debug.error("Error sending message: "+ex);
               Debug.logStackTrace(ex);
            }

            System.out.println("Enter 'q' to quit or any other key to continue: ");
            String line = input.readLine();

            quit = line.equalsIgnoreCase("q");

         }

         client.cleanup();

      }
      catch(Exception ex){

         System.out.println("Error: "+ex.getMessage());
         Debug.logStackTrace(ex);

      }
   }
}
