package com.nightfire.comms.email;

//////////////////////////////// JDK packages ////////////////////////////////
import java.util.*;
import java.io.*;
//////////////////////////////// NightFire packages //////////////////////////
import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import javax.mail.*;

public class GetEmailClient
{

    private static String indentStr = "                                               ";
    private static int level = 0;
    static boolean debug = false;
    static boolean showStructure = false;
    static String url = null;
    static String mbox = "INBOX";
    private Session session = null;
    public Folder folder = null;
    public Store store = null;
    private final static String ERROR_FOLDER = "Error";
    private final static String NO_ATTACHMENT_FOLDER = "Other";


   /**
     * Constructor
     */
    public GetEmailClient()
    { }

    public void logon(String protocol, String host, int port, String user, String password)
      throws ProcessingException {

      try {

          if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log (Debug.MSG_STATUS, "GetEmailClient: Connecting to Mail Server: " + host );

          // Get a Properties object
	        Properties props = System.getProperties();

          // Get a Session object
	        session = Session.getDefaultInstance(props, null);
	        session.setDebug(debug);

          // Get a Store object
	        //Store store = null;

	        if (url != null) {
		        URLName urln = new URLName(url);
		        store = session.getStore(urln);
		        store.connect();
	        }

          else {
		        if (protocol != null)
		            store = session.getStore(protocol);
		        else
		            store = session.getStore();

		        // Connect
            if (host != null || user != null || password != null)
		            store.connect(host, port, user, password);
		        else
		          store.connect();
          }
      }

      catch (Exception ex) {
         Debug.log (Debug.ALL_ERRORS,"ERROR: Unable to logon to Mail Server: " + host);
         throw new ProcessingException("ERROR: Unable to logon to Mail Server: " + host);
       }

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log (Debug.MSG_STATUS,"SUCCESSFULLY LOGGED ON TO MAIL SERVER: " + host);
    }


    public Hashtable getMail() throws ProcessingException {
        Hashtable messageList = new Hashtable();
        Vector msgVector = new Vector();

        try {
        // Open the Folder
          folder = store.getDefaultFolder();
	        if (folder == null) {
	            Debug.log (Debug.ALL_ERRORS, "No default folder");
	            throw new ProcessingException("ERROR: GetMailClient Unable to retrieve default folder.");
          }

          folder = folder.getFolder(mbox);
	        if (folder == null) {
	          Debug.log (Debug.ALL_ERRORS,"Invalid folder");
	          throw new ProcessingException("ERROR: GetMailClient Unable to retrieve folder" + mbox + ".");
	        }

          //make sure we open folder with READ_WRITE permissions so we can update message FLAG to DELETE
          
          folder.open(Folder.READ_WRITE);

	        int totalMessages = folder.getMessageCount();

          //if no messages log out and return null
	        if (totalMessages == 0) {
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log (Debug.MSG_STATUS,"Empty folder");
		        ///logout();
		        return null;
	        }

		      //else get the messages
		      javax.mail.Message[] msgs = folder.getMessages();

		      // Use a suitable FetchProfile
		      FetchProfile fp = new FetchProfile();
		      fp.add(FetchProfile.Item.ENVELOPE);
		      fp.add(FetchProfile.Item.FLAGS);
		      fp.add("X-Mailer");
		      folder.fetch(msgs, fp);


		      for (int i = 0; i < msgs.length; i++) {

            // Check if DELETED flag is set on this message if so skip it.
            if ( msgs[i].isSet(Flags.Flag.DELETED)) continue;

            //otherwise get the message
		          if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                  {
                    Debug.log (Debug.MSG_STATUS,"--------------------------");
		            Debug.log (Debug.MSG_STATUS,"MESSAGE #" + (i + 1) + ":");
                  }

            try {
              Vector mp = new Vector();
              Object messageObject = dumpPart(msgs[i], mp);

                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log(Debug.MSG_STATUS, msgs[i].toString() );

              //if the current message did not return an attachment, move to Other Folder and mark for delete
              if (messageObject == null) {
                javax.mail.Message[] temp = {msgs[i]};
                moveMessage(temp, NO_ATTACHMENT_FOLDER);
                msgs[i].setFlag(Flags.Flag.DELETED, true);
              }
              else
              {
                //check the type of Object the messgeObject is
                //if not a Vector wrap it in one:

                if (messageObject instanceof Vector)  {

                    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log(Debug.MSG_STATUS, "Adding Vector of attachments to message list.");
                  messageList.put(msgs[i], messageObject);
                }
                else {
                    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log(Debug.MSG_STATUS, "Wrapping the raw message in a Vector.");
                  msgVector.add(messageObject);
                  messageList.put(msgs[i], msgVector);
                  }
                }
              }

            catch (Exception e) {
              Debug.log(this, Debug.ALL_ERRORS, "WARNING: Error occured processing message moving to ERROR folder.");
              try {
                msgs[i].setFlag(Flags.Flag.DELETED, true);
                javax.mail.Message[] moveMsgs = {msgs[i]};
                moveMessage(moveMsgs, ERROR_FOLDER); }
              catch (Exception exp) { Debug.log(Debug.ALL_WARNINGS, "WARNING: Unable to move message [" + msgs[i] + " to ERROR_FOLDER"); }
              continue;
		        }
	      }
    }

    catch (Exception ex) {
	    Debug.log (Debug.ALL_ERRORS,"UNEXPECTED ERROR: GetEmailClient " + ex.getMessage());
      throw new ProcessingException("ERROR: GetEmailClient unexpected error.");
	  }

	  return messageList;
  }


  public static boolean isForwarded(Part p) {

    if (p == null) return false;

    try {
    if (p.isMimeType("message/rfc822") ) return true;
    }
    catch (Exception e) {e.printStackTrace(); }

    return false;

  }


  /* Recursive decent through the Message Object
   */

  public static Object dumpPart(Part p, Vector messageParts) throws Exception {

  if (p == null) return null;

	if (p instanceof javax.mail.Message)
	    dumpEnvelope((javax.mail.Message)p);


      if(Debug.isLevelEnabled(Debug.MSG_STATUS))
          Debug.log(Debug.MSG_STATUS, pr("CONTENT-TYPE: " + p.getContentType()) );
	/*
	 * Using isMimeType to determine the content type avoids
	 * fetching the actual content data until we need it.
	 */
	if (p.isMimeType("text/plain")) {
	    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log( Debug.MSG_STATUS,pr("This is plain text") );
            Debug.log( Debug.MSG_STATUS,pr("---------------------------") );
        }
      Object content = p.getContent();
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log( Debug.MSG_STATUS, pr("Message: " + (String) content) );
      messageParts.addElement(content);

	}
	else if (p.isMimeType("text/xml")) 
        {
            // the content will not be a string, so we want 
            // to get the bytes from it, convert them to a string
            // use the string.
	    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log( Debug.MSG_STATUS,pr("This is XML text") );
            Debug.log( Debug.MSG_STATUS,pr("---------------------------") );
        }

            // for content of type IMAPInputStream
            InputStream content = p.getInputStream();
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            int byteRead = 0;
            while ((byteRead = content.read()) != -1)
            {
                 byteStream.write(byteRead);
            }
            byte[] bytes = byteStream.toByteArray();
            byteStream.flush();
            byteStream.close();
            String xmlString = new String(bytes);

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log( Debug.MSG_STATUS, pr("Message: " + xmlString) );
            Debug.log( Debug.MSG_STATUS, pr("Message: " + content.getClass().getName()) );
            Debug.log( Debug.MSG_STATUS, pr("Message: " + content) );
        }
            messageParts.addElement(xmlString);
	}
      else if (p.isMimeType("multipart/mixed")) {
          if(Debug.isLevelEnabled(Debug.MSG_STATUS))
          {
              Debug.log( Debug.MSG_STATUS, pr("This is a Multipart Mixed") );
              Debug.log( Debug.MSG_STATUS, pr("---------------------------") );
          }
	    Multipart mp = (Multipart)p.getContent();
	    level++;
	    int count = mp.getCount();
	    for (int i = 0; i < count; i++)  {
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log( Debug.MSG_STATUS, "Retrieving message body(" + i + ")...." );
          Object o = dumpPart(mp.getBodyPart(i), messageParts);

          //only add the content if not null
          if ( o != null && o instanceof byte[] ) {
            messageParts.addElement(o);
              if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log( Debug.MSG_STATUS, "Added message body(" + i + ")...." );
          }
	}
      }
      else if (p.isMimeType("multipart/*")) {
          if(Debug.isLevelEnabled(Debug.MSG_STATUS))
          {
            Debug.log( Debug.MSG_STATUS, pr("This is a Multipart") );
            Debug.log( Debug.MSG_STATUS, pr("---------------------------") );
          }
	    Multipart mp = (Multipart)p.getContent();
	    level++;
	    int count = mp.getCount();
	    for (int i = 0; i < count; i++)  {
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log( Debug.MSG_STATUS, "Retrieving message body(" + i + ")...." );
          Object o = dumpPart(mp.getBodyPart(i), messageParts);

          //only add the content if not null
          if ( o != null && o instanceof byte[] ) {
            messageParts.addElement(o);
              if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                  Debug.log( Debug.MSG_STATUS, "Added message body(" + i + ")...." );
          }
      }

      level--;
      return messageParts;

  } else if (p.isMimeType("application/*") )  {
      if(Debug.isLevelEnabled(Debug.MSG_STATUS))
      {
          Debug.log( Debug.MSG_STATUS, pr("This is from an MS Client!") );
          Debug.log( Debug.MSG_STATUS, pr("---------------------------") );
      }
      InputStream is = p.getInputStream();
      ByteArrayOutputStream b = new ByteArrayOutputStream();
      int c;
      while ((c = is.read()) != -1) b.write(c);
      byte[] bytes = b.toByteArray();
      bytes = b.toByteArray();
      b.flush();
      b.close();
      return bytes;

	} else if (p.isMimeType("message/rfc822")) {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log( Debug.MSG_STATUS, pr("This is a Nested Message") );
            Debug.log( Debug.MSG_STATUS, pr("---------------------------") );
        }
	    level++;
      dumpPart((Part)p.getContent(), messageParts);
	    //return (Part)p.getContent();
	    level--;

	} else if (!showStructure) {
	    /*
	     * If we actually want to see the data, and it's not a
	     * MIME type we know, fetch it and check its Java type.
	     */
       if(Debug.isLevelEnabled(Debug.MSG_STATUS))
       {
            Debug.log( Debug.MSG_STATUS, pr("The MIMEType: " + p.getFileName() ) );
           Debug.log( Debug.MSG_STATUS, pr("---------------------------") );
       }

       Object msgObject = p.getContent();

       if ( (p.isMimeType("APPLICATION/MSWORD") || p.isMimeType("APPLICATION/rtf") ) && msgObject instanceof InputStream) {
           if(Debug.isLevelEnabled(Debug.MSG_STATUS))
               Debug.log( Debug.MSG_STATUS, "Found attachment of MIMETYPE: APPLICATION/MSWORD." );
		    InputStream is = (InputStream)msgObject;
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            int c;
            while ((c = is.read()) != -1) b.write(c);
            byte[] bytes = b.toByteArray();
            bytes = b.toByteArray();
           if(Debug.isLevelEnabled(Debug.MSG_STATUS))
               Debug.log ( Debug.MSG_STATUS, "Retrieved [ " + b.size() + " ] bytes");
            b.flush();
            b.close();
            return bytes;
       }
       return msgObject;
       
  }   else {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log( Debug.MSG_STATUS,pr("This is an unknown type") );
            Debug.log( Debug.MSG_STATUS,pr("---------------------------") );
        }
      return null;
	}
  return  messageParts;

}

  public static void dumpEnvelope(javax.mail.Message m) throws Exception {
      if(Debug.isLevelEnabled(Debug.MSG_STATUS))
      {
          Debug.log( Debug.MSG_STATUS, pr("This is the message envelope") );
          Debug.log( Debug.MSG_STATUS, pr("---------------------------") );
      }
	  Address[] a;

	  // FROM
	  if ((a = m.getFrom()) != null) {
	    for (int j = 0; j < a.length; j++)
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log( Debug.MSG_STATUS, pr("FROM: " + a[j].toString()) );
    }

	  // TO
	  if ((a = m.getRecipients(javax.mail.Message.RecipientType.TO)) != null) {
	    for (int j = 0; j < a.length; j++)
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log( Debug.MSG_STATUS, pr("TO: " + a[j].toString()) );
	    }

    // SUBJECT
      if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        Debug.log( Debug.MSG_STATUS, pr("SUBJECT: " + m.getSubject()) );

	  // DATE
	  Date d = m.getSentDate();
      if(Debug.isLevelEnabled(Debug.MSG_STATUS))
          Debug.log( Debug.MSG_STATUS, pr("SendDate: " +
	    (d != null ? d.toString() : "UNKNOWN")) );

	  // FLAGS
	  Flags flags = m.getFlags();
	  StringBuffer sb = new StringBuffer();
	  Flags.Flag[] sf = flags.getSystemFlags(); // get the system flags

	  boolean first = true;
	  for (int i = 0; i < sf.length; i++) {
	    String s;
	    Flags.Flag f = sf[i];
	    if (f == Flags.Flag.ANSWERED)
		        s = "\\Answered";
	      else if (f == Flags.Flag.DELETED)
		        s = "\\Deleted";
	      else if (f == Flags.Flag.DRAFT)
            s = "\\Draft";
	      else if (f == Flags.Flag.FLAGGED)
		        s = "\\Flagged";
	      else if (f == Flags.Flag.RECENT)
		        s = "\\Recent";
	    else if (f == Flags.Flag.SEEN)
		        s = "\\Seen";
	    else
		continue;	// skip it

    if (first)
		  first = false;
    else
		  sb.append(' ');
    sb.append(s);
	}

	String[] uf = flags.getUserFlags(); // get the user flag strings
	for (int i = 0; i < uf.length; i++) {
	    if (first)
		first = false;
	    else
		sb.append(' ');
	    sb.append(uf[i]);
	}
	pr("FLAGS: " + sb.toString());

	// X-MAILER
	String[] hdrs = m.getHeader("X-Mailer");
      if(Debug.isLevelEnabled(Debug.MSG_STATUS))
      {
        if (hdrs != null)
            Debug.log( Debug.MSG_STATUS, pr("X-Mailer: " + hdrs[0]) );
        else
            Debug.log( Debug.MSG_STATUS, pr("X-Mailer NOT available") );
      }
    }

  public void logout() throws javax.mail.MessagingException {

      javax.mail.Message expungedMessages[] = folder.expunge();
      
      if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
        Debug.log(Debug.OBJECT_LIFECYCLE, "Closing folder...");
      folder.close(true);
      
      if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
        Debug.log(Debug.OBJECT_LIFECYCLE, "Closing default store...");
      store.close();

  }

  public void moveMessage(javax.mail.Message[] msg, String dest) throws ProcessingException {

    Folder destFolder = null;

      if(Debug.isLevelEnabled(Debug.MSG_DATA))
        Debug.log(Debug.MSG_DATA, "Moving message(s) [" +  msg.length + "] to folder: " + dest);
    try {

        //destFolder = folder.getFolder(dest);
        destFolder = store.getFolder(dest);

        if ( destFolder == null || !destFolder.exists() )  {
            if(Debug.isLevelEnabled(Debug.MSG_DATA))
                Debug.log(Debug.MSG_DATA, "Creating folder: " + dest);
            destFolder.create(Folder.HOLDS_MESSAGES);
        }
        
        destFolder.open(Folder.READ_WRITE);
        folder.copyMessages(msg, destFolder);
        destFolder.close(true);
     }

    catch (Exception e) {
      throw new ProcessingException("ERROR GetMailClient: Unable to move messge to folder: " +  dest + ".");
    }

  }


  /**
   * Print a, possibly indented, string.
  */
  public static String pr(String s) {
	  if (showStructure)
	    return indentStr.substring(0, level * 2);
    else
      return s;
  }

public static void main(String[] args) {

  System.out.println("hi there!");
  Debug.enableAll();
  
  String protocol = "imap";
  String host = "192.168.10.6";
  int port = -1;
  String user = "dsl_order_pacbell";
  String pwd = "pacbell33x";
  int SLEEPTIME = 30;
  Hashtable messageList = null;
  Object curAttachment = null;
  String fileName = null;
  String targetDir = "c:/emailtest";
  String filePrefix = "jhl_";
  javax.mail.Message[] moveMsgs = new javax.mail.Message[1];

  GetEmailClient emc = new GetEmailClient();
  javax.mail.Message currentMsg = null;
    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        Debug.log(Debug.MSG_STATUS, "hi there!!!");

  try {
      if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        Debug.log(Debug.MSG_STATUS, "logging on....");
    emc.logon(protocol, host, port, user, pwd);
      if(Debug.isLevelEnabled(Debug.MSG_STATUS))
          Debug.log(Debug.MSG_STATUS, "getting mail....");
    messageList = emc.getMail();
  }

  catch (Exception e) { e.printStackTrace(); System.exit(-1);  }
  if (messageList != null) {
  for (Enumeration enumerator = messageList.keys(); enumerator.hasMoreElements(); ) {
          currentMsg = (javax.mail.Message) enumerator.nextElement();
          moveMsgs[0] = currentMsg;

      if(Debug.isLevelEnabled(Debug.MSG_DATA))
        Debug.log(emc, Debug.MSG_DATA, "Current Message: " + currentMsg);

          Vector attachments = (Vector) messageList.get(currentMsg);
      if(Debug.isLevelEnabled(Debug.MSG_DATA))
          Debug.log(emc, Debug.MSG_DATA, "\t number of attachments: " + attachments.size() );

          for (int k = 0; k < attachments.size(); k++ ) {

            //sleep for couple of seconds so don't use same timestamp on file name
            //is there a better way?
            try {
              Thread.currentThread().sleep(SLEEPTIME);
            }
            catch(InterruptedException e) {}

            curAttachment = (Object) attachments.elementAt(k);

            if ( curAttachment == null) {
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log( Debug.MSG_STATUS, "This message contained no attachments." );
              return;
            }

             try {
                  if ( curAttachment instanceof String ) {
                    String message = (String) curAttachment;
                    fileName = targetDir + File.separator + filePrefix + "_" + DateUtils.getDateToMsecAsString();
                    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log( Debug.MSG_STATUS, "Attempting to output to: " + targetDir );
                    FileUtils.writeFile(fileName, message );
                  }

                  else {
                    byte[] message = (byte[]) curAttachment;
                    fileName = targetDir + File.separator + filePrefix + "_" + DateUtils.getDateToMsecAsString();
                    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log( Debug.MSG_STATUS, "Attempting to output to: " + targetDir );
                    FileUtils.writeBinaryFile(fileName, message );
                  }
             }
                catch (FrameworkException fe) {
                  fe.printStackTrace();
                  if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                      Debug.log(Debug.MSG_STATUS, "WARNING: IOExeption, writing email message:\n" + fe.getMessage() );
                  }

               if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                  Debug.log(Debug.MSG_STATUS, "Processed attachment.");
                try { emc.moveMessage(moveMsgs, "Test"); }
                catch (Exception e) { e.printStackTrace(); }
            } //end attachment loop

            
      try { currentMsg.setFlag(Flags.Flag.DELETED,true); }
      catch (Exception e) { e.printStackTrace(); continue; }

      } //end message loop
    }
    try {  emc.logout(); }
    catch (Exception e) { e.printStackTrace(); }
  }

} //End of SMTPEmailClient


