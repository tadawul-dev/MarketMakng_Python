/**
 * #---------------------------------------------------------------------------------------------
 * #   This source is part of the Tadawul Web Listener, Saudi Stock Exchange
 * #   Copyright (c) 2009 Saudi Stock Exchange
 * #
 * #
 * #   All rights reserved.  No part of this source or any copyrights subsisting or arising out of these materials may be reproduced, distributed, amended, modified, adapted, stored in a
 * #   retrieval system, adopted or transmitted in any form or by any means,
 * #   electronic, mechanical, photographic, graphic, optic recording or otherwise,
 * #   translated in any language or computer language, without the prior written
 * #   permission of Saudi Stock Exchange
 * #
 * # ---------------------------------------------------------------------------------------------
 * ---------------------------------------------------------------------------------------------
 * Name          :     FileName : Utility.java
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 10/April/2009            Oracle                   Initial Version
 * 1.1				   14/November/2009         Mohanram,Oracle          Added functionalities to process the Equity Rights Share
 * 1.2				   02/February/2010         Mohanram,Oracle          Added functionalities to process the Exchange Traded Funds.	
 *----------------------------------------------------------------------------------------------
 */
package sa.com.tadawul.website.tip.shared;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import sa.com.tadawul.website.tip.dataanalyser.DataParserTurnOverListUpdates;
import sa.com.tadawul.website.tip.log.Log;
import sa.com.tadawul.website.tip.log.LogFactory;

/**
 * @description This class contains the logger related and Common
 * Methods
 *
 */

public class Utility implements Serializable {
    
    // ------------------------------------------------------------- Attributes
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final static Log logger = LogFactory.getLog(Utility.class);

	/** The fully qualified name of the Utility class. */
   // private static final String FQCN = Utility.class.getName();
    
    /** Log to this logger */
    //private transient Logger logger = null;
    
    /** Logger name */
    //private String name = null;
    
    //private static Priority traceLevel;
    
    private static StringBuffer sbFormattedTime;
    private static String tmpStr;
    
    // ------------------------------------------------------------
    // Static Initializer.
    //
    // Note that this must come after the static variable declarations
    // otherwise initialiser expressions associated with those variables
    // will override any settings done here.
    //
    // Verify that log4j is available, and that it is version 1.2.
    // If an ExceptionInInitializerError is generated, then LogFactoryImpl
    // will treat that as meaning that the appropriate underlying logging
    // library is just not present - if discovery is in progress then
    // discovery will continue.
    // ------------------------------------------------------------
    
    static {
       /* if (!Priority.class.isAssignableFrom(Level.class)) {
            // nope, this is log4j 1.3, so force an ExceptionInInitializerError
            throw new InstantiationError("Log4J 1.2 not available");
        }
        
        // Releases of log4j1.2 >= 1.2.12 have Priority.TRACE available, earlier
        // versions do not. If TRACE is not available, then we have to map
        // calls to Log.trace(...) onto the DEBUG level.
        
        try {
            traceLevel = (Priority) Level.class.getDeclaredField("TRACE").get(null);
        } catch(Exception ex) {
            // ok, trace not available
            traceLevel = Priority.DEBUG;
        } */
    }
    
    
    // ------------------------------------------------------------ Constructor
    
    public Utility() {
    }
    
    
    /**
     * Base constructor.
     */
    public Utility(String name) {
      //  this.name = name;
       // this.logger = getLogger();
    }
    
    /** For use with a log4j factory.
     */
    /*public Utility(Logger logger ) {
        this.name = logger.getName();
      //  this.logger=logger;
    }*/
    
    
    // ---------------------------------------------------------
    // Implementation
    //
    // Note that in the methods below the Priority class is used to define
    // levels even though the Level class is supported in 1.2. This is done
    // so that at compile time the call definitely resolves to a call to
    // a method that takes a Priority rather than one that takes a Level.
    //
    // The Category class (and hence its subclass Logger) in version 1.2 only
    // has methods that take Priority objects. The Category class (and hence
    // Logger class) in version 1.3 has methods that take both Priority and
    // Level objects. This means that if we use Level here, and compile
    // against log4j 1.3 then calls would be bound to the versions of
    // methods taking Level objects and then would fail to run against
    // version 1.2 of log4j.
    // ---------------------------------------------------------
    
    
    /**
     * Logs a message with <code>org.apache.log4j.Priority.TRACE</code>.
     * When using a log4j version that does not support the <code>TRACE</code>
     * level, the message will be logged at the <code>DEBUG</code> level.
     *
     * @param message to log
     * @see org.apache.commons.logging.Log#trace(Object)
     */
  /*  public void trace(Object message) {
        
        getLogger().log(FQCN, traceLevel, message, null );
    }
    
    
    *//**
     * Logs a message with <code>org.apache.log4j.Priority.TRACE</code>.
     * When using a log4j version that does not support the <code>TRACE</code>
     * level, the message will be logged at the <code>DEBUG</code> level.
     *
     * @param message to log
     * @param t log this cause
     * @see org.apache.commons.logging.Log#trace(Object, Throwable)
     *//*
    public void trace(Object message, Throwable t) {
        getLogger().log(FQCN, traceLevel, message, t );
    }
    
    
    *//**
     * Logs a message with <code>org.apache.log4j.Priority.DEBUG</code>.
     *
     * @param message to log
     * @see org.apache.commons.logging.Log#debug(Object)
     *//*
    public void debug(Object message) {
        getLogger().log(FQCN, Priority.DEBUG, message, null );
    }
    
    *//**
     * Logs a message with <code>org.apache.log4j.Priority.DEBUG</code>.
     *
     * @param message to log
     * @param t log this cause
     * @see org.apache.commons.logging.Log#debug(Object, Throwable)
     *//*
    public void debug(Object message, Throwable t) {
        getLogger().log(FQCN, Priority.DEBUG, message, t );
    }
    
    
    *//**
     * Logs a message with <code>org.apache.log4j.Priority.INFO</code>.
     *
     * @param message to log
     * @see org.apache.commons.logging.Log#info(Object)
     *//*
    public void info(Object message) {
        getLogger().log(FQCN, Priority.INFO, message, null );
    }
    
    
    *//**
     * Logs a message with <code>org.apache.log4j.Priority.INFO</code>.
     *
     * @param message to log
     * @param t log this cause
     * @see org.apache.commons.logging.Log#info(Object, Throwable)
     *//*
    public void info(Object message, Throwable t) {
        getLogger().log(FQCN, Priority.INFO, message, t );
    }
    
    
    *//**
     * Logs a message with <code>org.apache.log4j.Priority.WARN</code>.
     *
     * @param message to log
     * @see org.apache.commons.logging.Log#warn(Object)
     *//*
    public void warn(Object message) {
        getLogger().log(FQCN, Priority.WARN, message, null );
    }
    
    
    *//**
     * Logs a message with <code>org.apache.log4j.Priority.WARN</code>.
     *
     * @param message to log
     * @param t log this cause
     * @see org.apache.commons.logging.Log#warn(Object, Throwable)
     *//*
    public void warn(Object message, Throwable t) {
        getLogger().log(FQCN, Priority.WARN, message, t );
    }
    
    
    *//**
     * Logs a message with <code>org.apache.log4j.Priority.ERROR</code>.
     *
     * @param message to log
     * @see org.apache.commons.logging.Log#error(Object)
     *//*
    public void error(Object message) {
        getLogger().log(FQCN, Priority.ERROR, "*************** ERROR MESSAGE STARTS *************", null );
        getLogger().log(FQCN, Priority.ERROR, message, null );
        getLogger().log(FQCN, Priority.ERROR, "*************** ERROR MESSAGE ENDS *************", null );
    }
    
    
    *//**
     * Logs a message with <code>org.apache.log4j.Priority.ERROR</code>.
     *
     * @param message to log
     * @param t log this cause
     * @see org.apache.commons.logging.Log#error(Object, Throwable)
     *//*
    public void error(Object message, Throwable t) {
        getLogger().log(FQCN, Priority.ERROR, "*************** ERROR MESSAGE STARTS *************", t );
        getLogger().log(FQCN, Priority.ERROR, message, t );
        getLogger().log(FQCN, Priority.ERROR, "*************** ERROR MESSAGE ENDS *************", t );
    }
    
    
    *//**
     * Logs a message with <code>org.apache.log4j.Priority.FATAL</code>.
     *
     * @param message to log
     * @see org.apache.commons.logging.Log#fatal(Object)
     *//*
    public void fatal(Object message) {
        getLogger().log(FQCN, Priority.FATAL, "$$$$$$$$$$$$$$$ FATAL MESSAGE STARTS $$$$$$$$$$$$$$$", null );
        getLogger().log(FQCN, Priority.FATAL, message, null );
        getLogger().log(FQCN, Priority.FATAL, "$$$$$$$$$$$$$$$ FATAL MESSAGE ENDS $$$$$$$$$$$$$$$", null );
    }
    
    
    *//**
     * Logs a message with <code>org.apache.log4j.Priority.FATAL</code>.
     *
     * @param message to log
     * @param t log this cause
     * @see org.apache.commons.logging.Log#fatal(Object, Throwable)
     *//*
    public void fatal(Object message, Throwable t) {
        getLogger().log(FQCN, Priority.FATAL, "$$$$$$$$$$$$$$$ FATAL MESSAGE STARTS $$$$$$$$$$$$$$$", t );
        getLogger().log(FQCN, Priority.FATAL, message, t );
        getLogger().log(FQCN, Priority.FATAL, "$$$$$$$$$$$$$$$ FATAL MESSAGE ENDS $$$$$$$$$$$$$$$", t );
    }
    
    
    *//**
     * Return the native Logger instance we are using.
     *//*
    public Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(name);
        }
        return (this.logger);
    }
    
    
    *//**
     * Check whether the Log4j Logger used is enabled for <code>DEBUG</code> priority.
     *//*
    public boolean isDebugEnabled() {
        return getLogger().isDebugEnabled();
    }
    
    
    *//**
     * Check whether the Log4j Logger used is enabled for <code>ERROR</code> priority.
     *//*
    public boolean isErrorEnabled() {
        return getLogger().isEnabledFor(Priority.ERROR);
    }
    
    
    *//**
     * Check whether the Log4j Logger used is enabled for <code>FATAL</code> priority.
     *//*
    public boolean isFatalEnabled() {
        return getLogger().isEnabledFor(Priority.FATAL);
    }
    
    
    *//**
     * Check whether the Log4j Logger used is enabled for <code>INFO</code> priority.
     *//*
    public boolean isInfoEnabled() {
        return getLogger().isInfoEnabled();
    }
    
    
    *//**
     * Check whether the Log4j Logger used is enabled for <code>TRACE</code> priority.
     * When using a log4j version that does not support the TRACE level, this call
     * will report whether <code>DEBUG</code> is enabled or not.
     *//*
    public boolean isTraceEnabled() {
        return getLogger().isEnabledFor(traceLevel);
    }
    
    *//**
     * Check whether the Log4j Logger used is enabled for <code>WARN</code> priority.
     *//*
    public boolean isWarnEnabled() {
        return getLogger().isEnabledFor(Priority.WARN);
    }
    */
    public static DateFormat dfCurrentDateWoTime = new SimpleDateFormat("yyyyMMdd");
    private static Calendar cal = Calendar.getInstance();
    //private static Date today = cal.getTime();
    
    /**
     * 	This method is called to move the all the log files into specified directory
     * @param logFileName Log File name
     * @return String path of the LogfileDirectory
     */
    public static String logFileCreation(String logFileName) {
        return TipConstants.LOG_FILE_DIRECTORY + logFileName;
    }
    
   /* public static long getYesterdayStartDateinTimeMillis(){
        // Time dltStartTime = new Time(TipConstants.DELETE_START_HOUR,0,0);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        cal.set(Calendar.HOUR, TipConstants.DELETE_START_HOUR);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTimeInMillis();
    }
    
        	public static Time getDeleteStartTime(){
                Time dltStartTime = new Time(TipConstants.DELETE_START_HOUR,0,0);
                dltStartTime.
                return dltStartTime;
        }
         
    public static long getYesterdayEndDateinTimeMillis(){
        cal.add(Calendar.DAY_OF_MONTH, -1);
        cal.set(Calendar.HOUR, TipConstants.DELETE_END_HOUR);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTimeInMillis();
    }*/
    
    /**
     * 	This method is called to appends current time with given date
     * @param Date
     * @return date appeend with current time
     */
    public static synchronized  String appendTxWithToday(String TxTime) {
        //String dateWithoutTime = dfCurrentDateWoTime.format(today);
    	//logger.info( " TipConstants.DB_FEED_REQ_DATE :"+TipConstants.DB_FEED_REQ_DATE);
    	//logger.info( " formatTime(TxTime) :"+formatTime(TxTime));
        return  TipConstants.DB_FEED_REQ_DATE + formatTime(TxTime);
    }
    
    /**
     * 	This method is called to appends format the time
     * @param TxTime
     * @return formated Time with HH:MM:SS
     */
    public static synchronized String formatTime(String TxTime) {
        // TxTime = HHMMSS
        //sbFormattedTime = null;
        sbFormattedTime = new StringBuffer();
        /** Getting Hour */
        tmpStr = TxTime.substring(0, 2);
        sbFormattedTime.append(tmpStr + ":");
        //tmpStr = null;
        /** Getting Minute */
        tmpStr = TxTime.substring(2, 4);
        sbFormattedTime.append(tmpStr + ":");
        //tmpStr = null;
        /** Getting Seconds */
        tmpStr = TxTime.substring(4, 6);
        sbFormattedTime.append(tmpStr);
        //tmpStr = null;
        tmpStr = sbFormattedTime.toString();
        return tmpStr;
    }
    
    /**
     * 	This method is called to format the date
     * @param feedDate
     * @return formated Date with yyyy_MM_DD
     */
    public static String formatDate(String feedDate) {
        // feedDate = YYYYMMDD
        StringBuffer sbFormattedDate = new StringBuffer();
        /** Getting Year */
        String Year = feedDate.substring(0, 4);
        sbFormattedDate.append(Year + "-");
        
        /** Getting Month */
        String mon = feedDate.substring(4, 6);
        sbFormattedDate.append(mon + "-");
        
        /** Getting Day */
        String day = feedDate.substring(6, 8);
        sbFormattedDate.append(day);
        
        return sbFormattedDate.toString();
    }
    /**
     * 	This method is called to return the Level of the Tip Field
     * @param Values
     * @return String Level of the Tip Field before :
     */
    public static String GetLevel(String Values) {
        return Values.substring(0, Values.indexOf(':'));
    }
    /**
     * 	This method is called to return the Level of the Tip Field
     * @param Values
     * @return String Level of the Tip Field after :
     */
    public static String GetValue(String Values) {
        return Values.substring(Values.indexOf(':') + 1);
    }
    
    /**
     * This method is called to make the thread go to sleep for specific
     * time interval.
     *
     * @param lInterval
     */
    public static void sleepAnalyser(long lInterval) {
        try {
            Thread.sleep(lInterval);
        } catch (Exception exception) {
        }
    }
    
  //Changes for SR2. Extented NegDeals
    public static boolean shouldUpdatedShares()
    {
    	boolean shouldUpdateShares = false;
    	// Allow initialization only if the time is 9:30 AM or later, preventing pre-open status before this time
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);

		shouldUpdateShares = ((hour == 9 && minute >= 30) || (hour > 9 ));
		logger.info("Time Hour :"+hour + " Minute: "+minute +"   shouldUpdatedShares status :"+shouldUpdateShares);
		 return shouldUpdateShares;
    }
}
