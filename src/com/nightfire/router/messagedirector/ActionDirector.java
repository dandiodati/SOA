/**
 * The purpose of this director is to find out the name of the SPI to route
 * the message based on the action node. If it can not find a server then it 
 * throws an exception.
 *
 * @author Abhijit Talukdar
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is
 * considered to be confidential and proprietary to NeuStar.
 *
 * @see com.nightfire.spi.common.HeaderNodeNames
 * @see com.nightfire.common.ProcessingException
 * @see com.nightfire.framework.message.MessageException
 * @see com.nightfire.framework.message.parser.xml.XMLMessageParser
 * @see com.nightfire.framework.util.Debug
 * @see com.nightfire.router.ServerObject
 * @see com.nightfire.router.RouterSupervisor
 * @see com.nightfire.idl.RequestHandler
 * @see com.nightfire.framework.util.CustomerContext
 * @see com.nightfire.router.exceptions.UnKnownSPIException
 * @see org.w3c.dom.Document
 */

/**

	Revision History
	---------------
	
	Rev#	Modified By			Date			Reason
	----- ----------- ---------- --------------------------
	1.		Abhijit			01/24/2005			Created	
	2.		Abhijit			01/26/2005			Review comments incorporated

*/

package com.nightfire.router.messagedirector;

import com.nightfire.spi.common.HeaderNodeNames;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;
import com.nightfire.router.ServerObject;
import com.nightfire.router.RouterSupervisor;
import com.nightfire.idl.RequestHandler;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.router.exceptions.UnKnownSPIException;
import org.w3c.dom.Document;


public class ActionDirector extends MessageDirectorBase
{
  
  /**
   * sets up properties,aliases,etc. so child classes NEED to 
   * call super.intialize().
   * @param key The property key
   * @param type The property type
   * @throws ProcessingException if intialization fails
   */
  public void initialize(String key, String type) throws ProcessingException
  {
     super.initialize(key,type);
  }

  /**
   * This method determines if the current MessageDirector subclass can route 
   * this current message.
   *
   * @param header the xml header for this request comming in.
   * @param message the message comming in (will be xml in all current cases)
   * @return  boolean true if This MessageDirector can route this message or 
   * false if it can not.
   * @throws MessageException if the header is bad
   */

  public boolean canRoute(Document header, Object message) 
										throws MessageException
  {
        XMLMessageParser parser = new XMLMessageParser(header);

        if ( !parser.exists( HeaderNodeNames.ACTION_NODE ) )
		{

			return false;

		}
		else if ( parser.getValue( HeaderNodeNames.ACTION_NODE )
						.equals( HeaderNodeNames.SUBMIT ) )
		{

			return false;

		}

        return true;

   }


	/**
	* The method which forms the name of the SPI to route the message to.
	* This Director routes any header in the form of :
	*
	* <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
	* <!DOCTYPE header SYSTEM "file:./header.dtd">
	* 
	* <Header>                                    
	*    <Action value="validate"/>               
	*    <Request value="lsr_order"/>             
	*    <Subrequest value="loop"/>               
	*    <Supplier="SBC"/>                        
	*    <InterfaceVersion value="LSOG5"/>        
	*    <CustomerIdentifier value="ACME"/>     
	* </Header>                                 
	*
	* OR
	*
	* <Header>                                    
	*    <Request value="lsr_order"/>             
	*    <Subrequest value="loop"/>               
	*    <Supplier="SBC"/>                        
	*    <InterfaceVersion value="LSOG5"/>        
	*    <CustomerIdentifier value="ACME"/>     
	* </Header> 
	*
	* AlGORITHM:
	*
	* Form the COS Name by the following procedure:
	*
	* An alias lookup is done for the Action value. The real name defined for
	* the alias is the name of Server.
	*
	* The real name will be used to find the server.
	*
	* @param header the xml header for this request comming in.
	* @param message the message comming in (will be xml in all current cases)
	* @return RequestHandler - the RequestHandler representing the server.
	* @throws ProcessingException if processing fails
	* @throws MessageException if the message is bad.
	*
	*/
	protected RequestHandler getHandler(Document header, Object message) 
								throws ProcessingException, MessageException
	{
		XMLMessageParser parser = new XMLMessageParser(header);

        String actionName = parser.getValue( HeaderNodeNames.ACTION_NODE );

        // Create the inputName using the Action_Node and Request_Node from the Request header.
        // for e.g. validate.asr_order
        String inputName = actionName + PERIOD + parser.getValue( HeaderNodeNames.REQUEST_NODE );

        // Try to locate the realName i.e. Alias for this inputName.
        String cosName = getRealName (inputName);
        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "ActionDirector.geHandler: Looked up alias for key [" + inputName + "]. The alias found is [" + cosName + "]");
        }

        // If no Alias is available, getRealName() would return the inputString as is.
        // In this case, try to get the Alias using Action_Node only.
        if ( inputName.equals ( cosName) )
        {
            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                    {
                        Debug.log(Debug.MSG_STATUS, "ActionDirector.geHandler: Could not find valid alias with key [" + inputName + "]");
                    }

            cosName = getRealName ( actionName );

            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                    {
                        Debug.log(Debug.MSG_STATUS, "ActionDirector.geHandler: Looked up alias for action [" + actionName + "]. The alias found is [" + cosName + "]");
                    }
        }

		String errBuf = null;

		ServerObject obj = null;				

		try {

			obj = RouterSupervisor.getAvailableServers()
										.getServerObject(cosName);

        } catch (UnKnownSPIException uke) {

            errBuf = uke.getMessage() + "\n";            

        }

		if (obj == null)
		{
			throw new ProcessingException("ActionDirector: Could not locate"
							+ " server, the following servers were tried :\n" 
							+ errBuf );

		}
		Debug.log(Debug.MSG_STATUS, "ActionDirector: Found server [" 
										+ obj.cosName +"]");

		return obj.requestHandler;
	}

}
