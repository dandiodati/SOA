package com.nightfire.framework.util;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Appender;
import org.apache.log4j.spi.RootLogger;
import com.nightfire.framework.debug.DebugLevel;

import java.util.Properties;
import java.util.Enumeration;

/**
 * Wrapper Class over Log4j. This class is used in supplement to Debug
 * for intializing and delegating log messages to Log4j.
 */
public class Log4jWrapper
{

    /**
     * Static intializer block to intialize Log4j Logging for this class.
     */
    static
    {
       Properties prop = new Properties();
       prop.put("log4j.logger.com.nightfire.framework.util.Log4jWrapper","FATAL,console");
       prop.put("log4j.appender.console","org.apache.log4j.ConsoleAppender");
       prop.put("log4j.appender.console.layout","org.apache.log4j.PatternLayout");
       PropertyConfigurator.configure(prop);
    }

    /**
     * Since Debug is a static class therefore we are using only one Logger (named Log4jWrapper)
     * for the entire application. Contrary to this each class is required to get its own logger
     * using org.apache.log4j.Logger#getLogger method.
     */
	private static final Logger logger = Logger.getLogger(Log4jWrapper.class.getName());

    /**
     * Constant factor used to convert property MAX_DEBUG_WRITES to MB. In order to convert the
     * property into formidable size in Megabytes this factor is used.
     */
    private static final int LOG_SIZE_FACTOR = 7;

    /**
     * Return <code>true</code> if the supplied log level is enabled
     * @param level
     * @return
     */
    public static boolean isEnabledFor(int level)
    {
        Level log4jLevel = DebugLevel.convertNFLevel(level);
        return logger.isEnabledFor(log4jLevel);
    }

    /**
     * Log message for the supplied level.
     * @param level
     * @param message
     */
    public static void log(Level level,String message)
	{
        if(logger.isEnabledFor(level))
            logger.log(level,appendCustomerId(message));
	}

	/**
     *
     * @param message
     */
    public static void debug(String message)
	{
       logger.debug(appendCustomerId(message));
	}

	/**
     *
     * @param message
     */
    public static void info(String message)
	{
        logger.info(appendCustomerId(message));
	}

	/**
     *
     * @param message
     */
    public static void warn(String message)
	{
        logger.warn(appendCustomerId(message));
	}

	/**
     *
     * @param message
     */
    public static void error(String message)
	{
        logger.error(appendCustomerId(message));
	}

	/**
     *
     * @param message
     */
    public static void fatal(String message)
	{
        logger.fatal(appendCustomerId(message));
	}

    /**
     * Set the log4j level for this logger
     * @param level
     */
    public static void setLevel(Level level)
    {
        logger.setLevel(level);
    }

    /**
     * Method to Configure Log4j. If the root logger is already intialized i.e.
     * its logging level is set then only logger for Log4jWrapper is configured.
     *
     * @param level Log4j Level to set
     * @param fileNm String log file name
     * @param fileSize String log file size in bytes
     * @param backUpIdx String number of log files to back up
     */
    public static void configure(Level level,String fileNm,String fileSize,
                                 String backUpIdx)
    {
        Properties log4jProps = new Properties();

        if(Logger.getRootLogger()!=null)
        {
            RootLogger root = (RootLogger)Logger.getRootLogger();
            if(root.getChainedLevel()==null)
            {
                log4jProps.put("log4j.rootLogger",level.toString()+"#"+DebugLevel.class.getName()+",NF");
            }
            else
            {
                log4jProps.put("log4j.logger.com.nightfire.framework.util.Log4jWrapper",
                        level.toString()+"#"+DebugLevel.class.getName()+",NF");
            }
        }
        else
        {
            log4jProps.put("log4j.rootLogger",level.toString()+"#"+DebugLevel.class.getName()+",NF");
        }

        // if no file name is specified or file name is equal to "console"
        // then logging is done to System.out using ConsoleAppender.
        if(StringUtils.hasValue(fileNm) && !"console".equals(fileNm))
        {
            log4jProps.put("log4j.appender.NF","org.apache.log4j.RollingFileAppender");
            log4jProps.put("log4j.appender.NF.File",fileNm);

            if(!StringUtils.hasValue(fileSize))
                    fileSize = "10000"; // default value
            // divide by a constant factor to convert MAX_DEBUG_WRITES into MB's.
            int fileSizeInBytes = Integer.parseInt(fileSize)/LOG_SIZE_FACTOR * 1000;
            log4jProps.put("log4j.appender.NF.MaxFileSize",String.valueOf(fileSizeInBytes));

            if(!StringUtils.hasValue(backUpIdx))
                backUpIdx = "3";
            log4jProps.put("log4j.appender.NF.MaxBackupIndex",backUpIdx);
            logger.removeAppender("console");
        }
        else
            log4jProps.put("log4j.appender.NF","org.apache.log4j.ConsoleAppender");

        log4jProps.put("log4j.appender.NF.layout","org.apache.log4j.PatternLayout");
        log4jProps.put("log4j.appender.NF.layout.ConversionPattern","[%d{ABSOLUTE}][%-5p][%t] %m\n");

        PropertyConfigurator.configure(log4jProps);

        logger.info("-------------------- All Logging Appenders ------------------");
        Enumeration eNum = logger.getAllAppenders();
        while(eNum.hasMoreElements())
        {
            Appender appender = (Appender)eNum.nextElement();
            logger.info("Appender of Log4jWrapper [Class]"+appender.getClass().getName()+" [Name]"+appender.getName());
        }

        RootLogger rootLogger = (RootLogger)Logger.getRootLogger();
        eNum = rootLogger.getAllAppenders();
        while(eNum.hasMoreElements())
        {
            Appender appender = (Appender)eNum.nextElement();
            logger.info("Appender of Rootlogger [Class]"+appender.getClass().getName()+" [Name]"+appender.getName());
        }
        logger.info("--------------------------------------------------------------");

    }

    private static String appendCustomerId(String logMsg)
    {
        if(!cidLogging || logMsg==null)
            return logMsg;

        StringBuilder sb = new StringBuilder(logMsg.length()+15);
        sb.append( '[' );
        try
        {
            CustomerContext cc = CustomerContext.getInstance( );
            sb.append( cc.getCustomerID() );
            sb.append( ":" );
            sb.append( cc.getUserID() );
            if (StringUtils.hasValue ( cc.getSubDomainId (), true))
            {
                sb.append( ":" );
                sb.append( cc.getSubDomainId () );
            }
            if(StringUtils.hasValue(cc.getMessageId()))
            {
                sb.append( ":" );
                sb.append( cc.getMessageId() );
            }
            if(StringUtils.hasValue(cc.getUniqueIdentifier()))
            {
                sb.append( ":" );
                sb.append( cc.getUniqueIdentifier() );
            }
        }
        catch ( Exception e )
        {
            sb.append( e.toString() );
        }
        sb.append( "] " );
        sb.append(logMsg);

        return sb.toString();
    }

    private static boolean cidLogging = true;

    /**
     * Disable customer id logging
     */
    public static void disableCidLogging()
    {
        cidLogging = false;
    }
}
