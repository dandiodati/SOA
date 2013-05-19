package com.nightfire.comms.connectors;

import java.util.*;
import com.nightfire.framework.corba.*;

/**
 * This class implements PushConsumerCallBack interface.
 * Whenever an event arrives in the Event service, processEvent
 * method will be called.
 * This class also supports event listener protocol to loosly bind itself
 * from other classes interested in the the message received in processEvent.
 */
public class PushConsumerCallBackImpl implements PushConsumerCallBack
{
   public PushConsumerCallBackImpl()
   {
   }

   /**
    * listenerList vector contains the list of the object of type MessageListener.
    */
   private Vector listenerList = new Vector();

   /**
    * This method is called whenever event arrived in Event service.
    * @param message - message string.
    */
   public void processEvent(String message)
   {
      fireMessageEvent(message);
   }

   /**
    *  Adds object of MessageListener type to listenerList.
    */
   public void addListener(MessageListener obj)
   {
      synchronized(listenerList){

        for(int i = 0; i < listenerList.size() ; i++)
        {
          if(listenerList.elementAt(i).equals(obj))
          {
            return;
          }
        }
        listenerList.addElement(obj);

      }
   }

   /**
    *  removes MessageListener object from listenerList.
    */
   public void removeListener(MessageListener obj)
   {
      listenerList.removeElement(obj);
   }

   /**
    *  calls messageReceived methods for all objects in listenerList.
    */
   public void fireMessageEvent(String message)
   {
      synchronized(listenerList){

        for (int i = 0; i < listenerList.size(); i++)
        {
           ((MessageListener)listenerList.elementAt(i)).messageReceived(message);
        }

      }
   }
}
