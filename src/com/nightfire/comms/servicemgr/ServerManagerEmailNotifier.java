package com.nightfire.comms.servicemgr;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.nightfire.framework.email.EmailInterface;
import com.nightfire.framework.message.parser.MessageParserException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.repository.NotFoundException;
import com.nightfire.framework.repository.RepositoryException;
import com.nightfire.framework.repository.RepositoryManager;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.ObjectFactory;

/**
 * Helper class for ServiceManager to send email notifications
 * about processed commands. 
 * 
 * Methods in this class would not throw any exception; it will only log it.
 * Since fail to send an email would not break any functionality. 
 * @author hpirosha
 */
public class ServerManagerEmailNotifier 
{
    /**
     * private constructor
     */
    private ServerManagerEmailNotifier()
    {
    }

    private static ServerManagerEmailNotifier singleton;
   
    public static ServerManagerEmailNotifier getInstance()
    {
        if(singleton!=null)
            return singleton;
        
        synchronized (ServerManagerEmailNotifier.class) 
        {
            if(singleton!=null)
                return singleton;
            
            singleton = new ServerManagerEmailNotifier();
            singleton.init();
        }
        
        return singleton;
    }
    
    
    // constants representing xml element names 
    private final String EMAIL_CONFIG_CATEGORY = "servicemgr";
    private final String EMAIL_CONFIG_META = "email-notification-config";
    private final String EMAIL_NOTIFICATION_CFG_ELEM = "email-notification-config";
    private final String SENDER_EMAIL_CFG_ELEM = "sender-email-addr";
    private final String RECIEPIENT_EMAIL_CFG_ELEM = "recipient-email-addr";
    private final String SMPT_EMAIL_CFG_ELEM = "smtp-server-addr";
    private final String EMAIL_CLIENT_CFG_ELEM = "email-client-class-nm";
    private final String MAIL_SUBJECT_CFG_ELEM = "mail-subject";
    private final String ENABLED_ATTR = "enabled";
    private List<String> recipientAddrLst = new ArrayList<String>();
   
    private String senderEmailAddr, recipientEmailAddr, smtpSvrAddr, emailClientClassNm, mailSubject;
    private boolean enabled = false;
    private XMLMessageParser parser;
    
    /**
     * initialize email notifier with its configuration present in 
     * repository.
     */
    private void init()
    {
        // read the configurations from the repository.
        String xmlDescription;
        try 
        {
            xmlDescription = RepositoryManager.getInstance().getMetaData( EMAIL_CONFIG_CATEGORY, EMAIL_CONFIG_META );
            parser = new XMLMessageParser( xmlDescription );
        }
        catch (NotFoundException e) 
        {
            Debug.warning("Could not find repository for "+EMAIL_CONFIG_CATEGORY+" "+EMAIL_CONFIG_META);
            return;
        }
        catch (RepositoryException e) 
        {
            Debug.warning("A repository exception occured while reading "+EMAIL_CONFIG_CATEGORY+" "+EMAIL_CONFIG_META);
            Debug.warning(Debug.getStackTrace(e));
            return;
        }
        catch (MessageParserException e) 
        {
            Debug.warning("Could not parse configuration "+EMAIL_CONFIG_CATEGORY+" "+EMAIL_CONFIG_META);
            return;
        }

        Document document = parser.getDocument();
        NodeList list = document.getElementsByTagName(EMAIL_NOTIFICATION_CFG_ELEM);

        if(list == null || list.getLength()==0)
        {
            Debug.warning("No Server Configured in "+ EMAIL_CONFIG_CATEGORY+"/"+EMAIL_CONFIG_META);
        }
        else
        {
            // for all the server initialize the server properties 
            Element cfgElement = ((Element)list.item(0));
            if(cfgElement!=null)
            {
                enabled = Boolean.valueOf(cfgElement.getAttribute(ENABLED_ATTR));
                senderEmailAddr = cfgElement.getElementsByTagName(SENDER_EMAIL_CFG_ELEM).item(0).getFirstChild().getNodeValue();
                recipientEmailAddr = cfgElement.getElementsByTagName(RECIEPIENT_EMAIL_CFG_ELEM).item(0).getFirstChild().getNodeValue();
                smtpSvrAddr = cfgElement.getElementsByTagName(SMPT_EMAIL_CFG_ELEM).item(0).getFirstChild().getNodeValue();
                emailClientClassNm = cfgElement.getElementsByTagName(EMAIL_CLIENT_CFG_ELEM).item(0).getFirstChild().getNodeValue();
                mailSubject = cfgElement.getElementsByTagName(MAIL_SUBJECT_CFG_ELEM).item(0).getFirstChild().getNodeValue();
                
                StringTokenizer  strTok = new StringTokenizer(recipientEmailAddr,",");
                while(strTok.hasMoreElements())
                {
                    String mailAddr = (String)strTok.nextElement();
                    recipientAddrLst.add( mailAddr );
                }

            }
        }
    }
    
    
    /**
     * Send an email containing supplied message.
     * 
     * @param command
     */
    public void sendNotification(String message)
    {
        if(enabled)
        {
            try 
            {
                EmailInterface emailInterfaceObj = ( EmailInterface ) ObjectFactory.create ( emailClientClassNm );
                emailInterfaceObj.initServer ( smtpSvrAddr );
                emailInterfaceObj.setSender ( senderEmailAddr );
    
                for(String mailAddr :recipientAddrLst)
                    emailInterfaceObj.addRecipient ( mailAddr );
                
                emailInterfaceObj.setSubject ( mailSubject );
                
                emailInterfaceObj.setMessageContent (message);
                emailInterfaceObj.sendEmail ();    
                
                if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                    Debug.log(Debug.NORMAL_STATUS, "[ServiceMgrEmailNotifier:]Email sent ..");
            }
            catch(Exception e)
            {
                Debug.warning("ServiceMgrEmailNotifier : Unable to sent email "+Debug.getStackTrace(e));
            }
        }
    }
    
}
