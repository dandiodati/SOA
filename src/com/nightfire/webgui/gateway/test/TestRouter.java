package com.nightfire.webgui.gateway.test;

import java.io.File;
import java.util.Properties;

import org.omg.CORBA.ORB;


import com.nightfire.framework.corba.CorbaPortabilityLayer;
import com.nightfire.framework.corba.ObjectLocator;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.debug.DebugConfigurator;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FileUtils;
import com.nightfire.idl.RequestHandler;
import com.nightfire.idl.RequestHandlerHelper;

public class TestRouter {

    public static void main(String[] args) throws Exception
    {
        Properties props = new Properties();
        props.put(Debug.LOG_FILE_NAME_PROP, "D:/prg/nightfire/router/test.log");
        props.put(Debug.DEBUG_LOG_LEVELS_PROP, "ALL");
        DebugConfigurator.configure(props,"testApp");
        Debug.configureFromProperties(props);

        System.setProperty("NF_REPOSITORY_ROOT", "d:/GW/repository");
        DBInterface.initialize("jdbc:oracle:thin:@impetus-132:1521:ORCL132","suninstall", "suninstall");

        Properties orbProperties = new Properties();
        orbProperties.put(CorbaPortabilityLayer.ORB_AGENT_ADDR_PROP, "192.168.101.3");
        orbProperties.put(CorbaPortabilityLayer.ORB_AGENT_PORT_PROP, "14000");

        CorbaPortabilityLayer cpl = new CorbaPortabilityLayer(null, orbProperties);
        ORB orb = cpl.getORB();

        ObjectLocator objLoc = new ObjectLocator(orb);
        org.omg.CORBA.Object object = objLoc.find("Nightfire.Router");
        RequestHandler handler = RequestHandlerHelper.narrow(object);

        String header = FileUtils.readFile(args[0]+File.separator+args[1]+"_header.xml");
        String body = FileUtils.readFile(args[0]+File.separator+args[1]+"_body.xml");

        handler.processAsync(header, body);
    }
}
