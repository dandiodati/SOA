package com.nightfire.comms.connectors;

/**
 * Interface , to be implemented by the class interested in listening to
 * the message delivered by the PushConsumerCallBackImpl class.
 */
public interface MessageListener
{
    public void messageReceived(String message);
}