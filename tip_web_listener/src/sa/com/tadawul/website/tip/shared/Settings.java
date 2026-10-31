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
 * Name          :     FileName : Settings.java
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 10/April/2009            Oracle                   Initial Version
 * 1.1				   14/November/2009         Mohanram,Oracle          Added functionalities to process the Equity Rights Share
 * 1.2				   02/February/2010         Mohanram,Oracle          Added functionalities to process the Exchange Traded Funds.	
 *----------------------------------------------------------------------------------------------
 */
package sa.com.tadawul.website.tip.shared;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import sa.com.tadawul.website.tip.log.Log;
import sa.com.tadawul.website.tip.log.LogFactory;

/**
 * @description This class contains all the  Common
 * Validations
 *
 */
public class Settings {
    
    public static Properties g_oProperties;
    static boolean g_bIsNewDay = false;
    public static final int SOCKET_BUFFER_SIZE = 40000;
    private final static Log logger = LogFactory.getLog(Settings.class);
    static DateFormat dfCurrentDateWoTime = new SimpleDateFormat("yyyyMMdd");
    static Date today = Calendar.getInstance().getTime();
    static String iniFileName = null;
    
    public Settings() {
    }
    
    /**
     * This method is called to load the ini file and
     *  Get all the values from ini file and setting into TipConstants
     */
    public static void Load() {
        logger.debug("inside Load method in Settings");
       // Calendar calendar = null;
        try {
            g_oProperties = new Properties();
            File file = null;
            iniFileName = System.getProperty("LISTENER_INI_FILE");
            logger.info("INI fileName: "+iniFileName);
            if (iniFileName == null ) {
                logger.fatal("LISTENER_INI_FILE environment variable not defined...Exiting");
                System.exit(1);
            }
            file = new File(iniFileName);
            logger.debug("Loading Initialization parameters from " + file.getCanonicalPath());
            InputStream oIn = new FileInputStream(iniFileName);
            g_oProperties.load(oIn);
            oIn.close();
            
            
            // Getting the values from ini file and setting into TipConstants
            TipConstants.SHARE_MARKET_CODE = getString("SHARE_MARKET_CODE");           
            TipConstants.SHARE_MARKET_ID = getLong("SHARE_MARKET_ID");
            TipConstants.SHARE_MARKET_LIST_ID = getLong("SHARE_MARKET_LIST_ID");
            
            TipConstants.BOND_MARKET_CODE = getString("BOND_MARKET_CODE");         
            TipConstants.BOND_MARKET_ID = getLong("BOND_MARKET_ID");
            TipConstants.BOND_MARKET_LIST_ID = getLong("BOND_MARKET_LIST_ID");
            
            TipConstants.SME_MARKET_CODE = getString("SME_MARKET_CODE");          
            TipConstants.SME_MARKET_ID = getLong("SME_MARKET_ID");
            TipConstants.SME_MARKET_LIST_ID = getLong("SME_MARKET_LIST_ID");
            
            //Added for PTTP Project CRQ22398
            TipConstants.OTC_MARKET_CODE = getString("OTC_MARKET_CODE");          
            TipConstants.OTC_MARKET_ID = getLong("OTC_MARKET_ID");
            
          //Added for PTTP Project CRQ22398
            TipConstants.BOND_OTC_MARKET_CODE = getString("BOND_OTC_MARKET_CODE");          
            TipConstants.BOND_OTC_MARKET_ID = getLong("BOND_OTC_MARKET_ID");
            
            TipConstants.SHARED_LIST_MAPPING = getString("SHARED_LIST_MAPPING");
            
            TipConstants.TARGIN_USER_NAME = getString("TARGIN_USER_NAME");
            TipConstants.TARGIN_PASSWORD = getString("TARGIN_PASSWORD");
            TipConstants.TARGIN_PRIMARY_HOST = getString("TARGIN_PRIMARY_HOST");
            TipConstants.TARGIN_PRIMARY_PORT =  getInt("TARGIN_PRIMARY_PORT");
            TipConstants.TARGIN_SECONDARY_HOST = getString("TARGIN_SECONDARY_HOST");
            if (TipConstants.TARGIN_SECONDARY_HOST.equals(""))
                TipConstants.TARGIN_SECONDARY_HOST = null;
            TipConstants.TARGIN_SECONDARY_PORT = getInt("TARGIN_SECONDARY_PORT");
            TipConstants.TARGIN_FEED_REQ_DATE = getInt("TARGIN_FEED_REQ_DATE");
            TipConstants.TARGIN_CONNECT_MAX_RETRY_COUNT = getInt("TARGIN_CONNECT_MAX_RETRY_COUNT");
            
            TipConstants.TIP_MAX_NO_MARKET = getInt("TIP_MAX_NO_MARKET");
            TipConstants.TIP_MAX_NO_INDICES = getInt("TIP_MAX_NO_INDICES");   
            TipConstants.TIP_MAX_NO_SME_INDICES = getInt("TIP_MAX_NO_SME_INDICES"); 
            TipConstants.TIP_MAX_NO_BOND_INDICES = getInt("TIP_MAX_NO_BOND_INDICES"); 
            TipConstants.TIP_MAX_NO_SHARES = getInt("TIP_MAX_NO_SHARES");
            TipConstants.TIP_MAX_NO_SME_SHARES = getInt("TIP_MAX_NO_SME_SHARES");
            TipConstants.TIP_MAX_NO_BONDS = getInt("TIP_MAX_NO_BONDS");
            
            //Added for PTTP Project CRQ22398
            TipConstants.TIP_MAX_NO_OTC_SHARES = getInt("TIP_MAX_NO_OTC_SHARES");
            
            TipConstants.TIP_MAX_DELAY_TIME = getInt("TIP_MAX_DELAY_TIME");
          
            
            TipConstants.USE_DATA_SOURCE = getInt("USE_DATA_SOURCE");
            TipConstants.DB_CONNECTION_MAX_RETRY_COUNT = getInt("DB_CONNECTION_MAX_RETRY_COUNT");
            TipConstants.JDBC_DRIVER = getString("JDBC_DRIVER");
            TipConstants.DB_URL = getString("DB_URL");
            TipConstants.DB_USER = getString("DB_USER");
            TipConstants.DB_PASS = getString("DB_PASS");
            TipConstants.DB_SERVERNAME = getString("DB_SERVERNAME");
            TipConstants.DB_PORT = getInt("DB_PORT");
            
            TipConstants.WEBSITE_DISPLAY_DELAY_TIME = getLong("WEBSITE_DISPLAY_DELAY_TIME");
            TipConstants.LOG_FEED = getInt("LOG_FEED");
            TipConstants.FILE_BASED_RECOVERY = getInt("FILE_BASED_RECOVERY");
            TipConstants.XMP_LIBRARY = getString("XMP_LIBRARY");
            logger.info("XMP_LIBRARY : " + TipConstants.XMP_LIBRARY);
            TipConstants.LOG_FILE_DIRECTORY = getString("LOG_FILE_DIRECTORY");
            logger.info("Log file directory is " + TipConstants.LOG_FILE_DIRECTORY.equals(""));
            
            //Neg Deals timing changes
            TipConstants.IS_NEGDEALS_EARLY_START_VAL = getInt("IS_NEGDEALS_EARLY_START_VAL");
         
            
            //Mandatory field validation
            if(TipConstants.TARGIN_USER_NAME.equals("")){
                logger.fatal("Mandatory field validation failed -TARGIN_USER_NAME should not be empty");
                System.exit(1);
            }
            if(TipConstants.TARGIN_PASSWORD.equals("")){
                logger.fatal("Mandatory field validation failed -TARGIN_PASSWORD in ini file should not be empty");
                System.exit(1);
            }if(TipConstants.TARGIN_PRIMARY_HOST.equals("")){
                logger.fatal("Mandatory field validation failed -TARGIN_PRIMARY_HOST in ini file should not be empty");
                System.exit(1);
            }
            if(TipConstants.TARGIN_PRIMARY_PORT <= 0){
                logger.fatal("Mandatory field validation failed -TARGIN_PRIMARY_PORT in ini file should not be empty");
                System.exit(1);
            }
            if(TipConstants.DB_URL.equals("")){
                logger.fatal("Mandatory field validation failed -DB_URL in ini file should not be empty");
                System.exit(1);
            }
            if(TipConstants.DB_USER.equals("")){
                logger.fatal("Mandatory field validation failed -DB_USER in ini file should not be empty");
                System.exit(1);
            }
            if(TipConstants.DB_PASS.equals("")){
                logger.fatal("Mandatory field validation failed -DB_PASS in ini file should not be empty");
                System.exit(1);
            }
            if(TipConstants.DB_SERVERNAME.equals("")){
                logger.fatal("Mandatory field validation failed -DB_SERVERNAME in ini file should not be empty");
                System.exit(1);
            }
            if(TipConstants.DB_PORT <=0){
                logger.fatal("Mandatory field validation failed -DB_PORT in ini file should not be empty");
                System.exit(1);
            }
            if(TipConstants.XMP_LIBRARY.equals("")){
                logger.fatal("Mandatory field validation failed -XMP_LIBRARY in ini file should not be empty");
                System.exit(1);
            }
            if(TipConstants.SHARE_MARKET_CODE.equals("")){
                logger.fatal("Mandatory field validation failed -SHARE_MARKET_CODE in ini file should not be empty");
                System.exit(1);
            }
          
            if(TipConstants.SHARE_MARKET_ID <= 0){
                logger.fatal("Mandatory field validation failed -SHARE_MARKET_ID in ini file should not be empty");
                System.exit(1);
            }
            if(TipConstants.SHARE_MARKET_LIST_ID <= 0){
                logger.fatal("Mandatory field validation failed -SHARE_MARKET_LIST_ID in ini file should not be empty");
                System.exit(1);
            }
            if(TipConstants.BOND_MARKET_CODE.equals("")){
                logger.fatal("Mandatory field validation failed -BOND_MARKET_CODE in ini file should not be empty");
                System.exit(1);
            }
         
            if(TipConstants.BOND_MARKET_ID <= 0){
                logger.fatal("Mandatory field validation failed -BOND_MARKET_ID in ini file should not be empty");
                System.exit(1);
            }
            if(TipConstants.BOND_MARKET_LIST_ID<=0){
                logger.fatal("Mandatory field validation failed -BOND_MARKET_LIST_ID in ini file should not be empty");
                System.exit(1);
            }
            //setting  Default value
            if(TipConstants.DB_CONNECTION_MAX_RETRY_COUNT <=0 )
                TipConstants.DB_CONNECTION_MAX_RETRY_COUNT =1;
            if(TipConstants.TARGIN_CONNECT_MAX_RETRY_COUNT <=0)
                TipConstants.TARGIN_CONNECT_MAX_RETRY_COUNT=1;
            
            if(TipConstants.TIP_MAX_NO_MARKET <= 0)
                TipConstants.TIP_MAX_NO_MARKET =2;
            if(TipConstants.TIP_MAX_NO_INDICES <= 0)
                TipConstants.TIP_MAX_NO_INDICES =50;
            if(TipConstants.TIP_MAX_NO_SME_INDICES <= 0)
                TipConstants.TIP_MAX_NO_SME_INDICES =20;
            if(TipConstants.TIP_MAX_NO_BOND_INDICES <= 0)
                TipConstants.TIP_MAX_NO_BOND_INDICES =10;
            if(TipConstants.TIP_MAX_NO_SHARES <= 0)
                TipConstants.TIP_MAX_NO_SHARES=200;
            if(TipConstants.TIP_MAX_NO_SME_SHARES <= 0)
                TipConstants.TIP_MAX_NO_SME_SHARES=50;
            if(TipConstants.TIP_MAX_NO_BONDS <= 0)
                TipConstants.TIP_MAX_NO_BONDS=100;
            
          //Added for PTTP Project CRQ22398
            if(TipConstants.TIP_MAX_NO_OTC_SHARES <= 0)
                TipConstants.TIP_MAX_NO_OTC_SHARES=50;
            
          //Added for PTTP Project CRQ22398
            if(TipConstants.TIP_MAX_NO_BOND_OTC_SHARES <= 0)
                TipConstants.TIP_MAX_NO_BOND_OTC_SHARES=100;
            
            if(TipConstants.TIP_MAX_DELAY_TIME <= 0)
                TipConstants.TIP_MAX_DELAY_TIME = 5;
            TipConstants.TIP_MAX_DELAY_TIME = 1000 * TipConstants.TIP_MAX_DELAY_TIME;
           
          
            
            if(TipConstants.WEBSITE_DISPLAY_DELAY_TIME <= 0)
                TipConstants.WEBSITE_DISPLAY_DELAY_TIME=0;
            TipConstants.WEBSITE_DISPLAY_DELAY_TIME = 1000 * TipConstants.WEBSITE_DISPLAY_DELAY_TIME;
            
            //TARGIN_FEED_REQ_DATE
            if(TipConstants.TARGIN_FEED_REQ_DATE <= 0)
            {
                TipConstants.TARGIN_FEED_REQ_DATE =Integer.parseInt(dfCurrentDateWoTime.format(today));
                TipConstants.IS_PAST_OR_FUTURE_DATE =false;
            }else
            {
            	TipConstants.IS_PAST_OR_FUTURE_DATE =true;
            }
            
           //Added for NegDeals  
            if(TipConstants.IS_NEGDEALS_EARLY_START_VAL == 1)
            {
            	TipConstants.IS_NEGDEALS_EARLY_START = true;
            }else
            {
            	TipConstants.IS_NEGDEALS_EARLY_START = false;
            }
            
            TipConstants.DB_FEED_REQ_DATE = Utility.formatDate(String.valueOf(TipConstants.TARGIN_FEED_REQ_DATE)) + " ";          
            logger.info("DB_FEED_REQ_DATE is '" + TipConstants.DB_FEED_REQ_DATE + "'");
            
        
            logger.info("finished loading........................................");
        } catch(Exception e) {
            logger.error("Error in Load()" +e.toString());
            try {
                Thread.sleep(5000L);
            } catch(InterruptedException e1) {
                logger.error("Interrupted Exception in Thread.sleep()" +e1.toString());
            }
            System.exit(0);
        }
        logger.debug("Exit Load method in Settings");
    }
    
    /**
     * This method to get the String values from the  ini file constant file.
     * @param sID
     * @return sIP values of the corresponding constants.
     */
    public static String getString(String sID) {
        String sIP = g_oProperties.getProperty(sID);
        if(sIP == null)
            return "";
        else
            return sIP;
    }
    
    /**
     * This method to set the String values to the constants present in the ini file constant file.
     * @param sID,sValue
     */
    public static void setString(String sID, String sValue) {
        g_oProperties.setProperty(sID, sValue);
    }
    
    /**
     * This method to get the int values from the  ini file constant file.
     * @param sID
     * @return sValue values of the corresponding constants.
     */
    public static int getInt(String sID) {
        String sValue = g_oProperties.getProperty(sID);
        if(sValue == null)
            return -1;
        try {
            return Integer.parseInt(sValue);
        } catch(Exception e) {
            return 0;
        }
    }
    
    /**
     * This method to get the Long values from the  ini file constant file.
     * @param sID
     * @return sValue values of the corresponding constants.
     */
    public static long getLong(String sID) {
        String sValue = g_oProperties.getProperty(sID);
        if(sValue == null)
            return 0L;
        try {
            return Long.parseLong(sValue);
        } catch(Exception e) {
            return 0L;
        }
    }
    
    /**
     * This method will write data to efp.ini file
     */
    static void save() {
        OutputStream oOut = null;
        try {
            oOut = new FileOutputStream(iniFileName);
            g_oProperties.store(oOut, "TIP Web Listener Application Configurations. Do not change manually");
            oOut.close();
        } catch(Exception e) {
            logger.error("Error  while writing the property list to OutputStream" +e.toString());
        }
    }
    
    /**
     * This method will write data to efp.ini file
     */
    public static void saveSettings() throws Exception {
        OutputStream oOut = new FileOutputStream(iniFileName);
        g_oProperties.store(oOut, "TIP Web Listener Application Configurations. Do not change manually");
        oOut.close();
    }
}