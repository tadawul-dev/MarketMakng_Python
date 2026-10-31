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
 * Name          :     FileName : AnalyserReader.java
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 10/April/2009            Oracle                   Initial Version
 * 1.1				   14/November/2009         Mohanram,Oracle          Added functionalities to process the Equity Rights Share
 * 1.2				   02/February/2010         Mohanram,Oracle          Added functionalities to process the Exchange Traded Funds.	

 *----------------------------------------------------------------------------------------------
 */
package sa.com.tadawul.applications.tip.isi.efp.analyser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import sa.com.tadawul.website.tip.queue.FrameHolder;
import sa.com.tadawul.website.tip.queue.MessageDatabaseQueue;
import sa.com.tadawul.website.tip.queue.MessageQueue;
import sa.com.tadawul.website.tip.log.Log;
import sa.com.tadawul.website.tip.log.LogFactory;
import sa.com.tadawul.website.tip.DBThreads.DBQueueUpdater;
import sa.com.tadawul.website.tip.dataanalyser.DataReader;
import sa.com.tadawul.website.tip.shared.Settings;
import sa.com.tadawul.website.tip.shared.TIPDataObjects;
import sa.com.tadawul.website.tip.vo.TIP_MsgVO;
import sa.com.tadawul.website.tip.shared.TipConstants;
import sa.com.tadawul.website.tip.shared.Utility;

/**
 * @description This reader class is responsible for connecting to primary/secondary TARGIN server,
 *              Logon request, sending a streaming request, reading the messages from the TARGIN
 *              feed and writing them to the queue.
  */

public class AnalyserReader {
    public String userName = null;
    public String passWord = null;
    public String priHostName = null;
    public int priPortNum = 0;
    public String secHostName = null;
    public int secPortNum = 0;
    public int date = 0;
    public String hostName = null;
    public int portNum = 0;
    public long startSeq=1;
    public long endSeq = 0;
    private FileOutputStream fos;
    private MessageDatabaseQueue MessagedatabaseQueue = new MessageDatabaseQueue();  
    int iMessageCount = 0;
    
    //Native C Call Methods
    public native static int CNativeInitialize();
    public native static int CNativeConnect(String hostName,int portNum,int compression);
    public native static String CNativeGetNextMessage();
    public native static String CNativeGetErrorMessage();
    public native static String CNativeLogon(String userName,String password,String protocolVersion);
    public native static int CNativeStreamingRequest(int reqType, int scope, int date,long startSeq, long endSeq);
    public native static int CNativeLogoff();
    public native static int CNativeGetSignalReceived();
    public native static int CNativeDisconnect();
    static boolean logoffSent = false;    
    private final static Log logger = LogFactory.getLog(AnalyserReader.class);
    
    Timestamp timeObj;
   
    private TIP_MsgVO msg = new TIP_MsgVO();
    String[] tags_values = null;
    int n = 0;
    int i = 0;
    static int count = 0;    
    
    static {
        if(Settings.g_oProperties == null)
            Settings.Load();
        System.load(TipConstants.XMP_LIBRARY);
       // System.loadLibrary("Tipclient.dll");
        logger.debug("Successfully loaded xmp shared library from "+ TipConstants.XMP_LIBRARY);
        
    }
    
    public AnalyserReader() {
    	
    }
    
    /**
     * This  method will establish connection with TARGIN and sends Logon,Streaming Request
     * To TARGIN and reads the data from the feed and stores into the buffer
     * @param null
     * @return boolean
     *
     */
    private boolean ServiceRequest() {
        logger.debug("inside ServiceRequest funciton in AnalyserReader");
        String buffer;
        long readerEndTime =0;       
        FrameHolder oHolder = null;
        long readerStartTime = 0;        
        int ret = 0;
        
        try {
            // Connect to Primary TIP
            while (!TipConstants.shutdownFlag) {
                hostName=priHostName;
                portNum=priPortNum;
                // Connect to TIP
                if (!connectTIP()) {
                    logger.error("Unable to connect with Primary...");
                    if (secHostName == null || secPortNum <=0) {
                        logger.error("No secondary IP to connect...");
                        TipConstants.shutdownFlag = true;
                        continue;
                    }
                    hostName=secHostName;
                    portNum=secPortNum;
                    if (!connectTIP()) {
                        logger.error("Unable to connect with Secondary...");
                        TipConstants.shutdownFlag = true;
                        continue;
                    }
                }
                
                // Send Logon to TIP
                if (!LogonToTIP()) {
                    logger.error("Unable to Logon...");
                    TipConstants.shutdownFlag = true;
                    ret = CNativeDisconnect();
                    if (ret == TipConstants.FALSE)
                        logger.fatal("Call to CNativeDisconnect failed: " + CNativeGetErrorMessage());
                    continue;
                }
                // Send Streaming Request to TIP
                if (!SendStreamingRequest()) {
                    logger.error("Unable to Send Streaming Request...");
                    HandleLogoff();
                    continue;
                }
                
                readerStartTime = System.currentTimeMillis();              
                while ((buffer = CNativeGetNextMessage()) != null ) {
                	// Adding code to check the end of line char:
                	//Java 7 provided System.getProperty("line.separator") to check and insert line separators regardless its OS representation in the running JVM;
                	if ((buffer.charAt(buffer.length() - 1) != TipConstants.TIP_ENDOFMSG) || !((buffer.contains(TipConstants.TIP_LINE_SEPARATOR)))){
                    	buffer = buffer.concat(TipConstants.TIP_LINE_SEPARATOR);
                    }
                    try {
                        if (TipConstants.LOG_FEED == 1) {
                            fos.write(buffer.getBytes());
                            fos.flush();
                        }
                    } catch (Exception e) {
                        logger.error("File Write Error = "+e);
                    }
                    iMessageCount++;
                    readerEndTime = System.currentTimeMillis();
                    oHolder = new FrameHolder(System.currentTimeMillis() + TipConstants.WEBSITE_DISPLAY_DELAY_TIME, buffer,iMessageCount,readerStartTime,readerEndTime);
                    MessageQueue.g_oBuffer.setFrame(oHolder);
                   // logger.info("Buffer size in Reader:" +g_oBuffer.size());
                    buffer = null;
                    oHolder = null;                 
                    readerStartTime = System.currentTimeMillis();                   
                
                }
                // Check if Signal has been received
                if (CNativeGetSignalReceived() != 0 || TipConstants.shutdownFlag) {
                    logger.info("Signal received is "+ CNativeGetSignalReceived() + " shutdownFlag = " + TipConstants.shutdownFlag);
                    TipConstants.shutdownFlag = true;
                    HandleLogoff();
                }
                if (!TipConstants.shutdownFlag) {
                    logger.error("Call to CNativeGetNextMessage failed: " + CNativeGetErrorMessage());
                }
                readerEndTime = System.currentTimeMillis();               
            }
        } catch (Exception e) {
            logger.error("Exception occurred = "+e.toString(),e);
        } finally {
           // GUIHelper.framesReceived = iMessageCount;
            logger.debug("In finally Block of ServiceRequest");
            try {
                if (TipConstants.LOG_FEED == 1)
                    fos.close();
            } catch (Exception e) {
                logger.error("Log File Close Error: "+e);
            }
          //  TipConstants.shutdownFlag=true;
        }
        logger.debug("Exit ServiceRequest funciton in AnalyserReader");
        return true;
    }
    
    /**
     * The main method will get the connection arguments from the efp.ini.
     * It also checks if the system parameter LISTENER_GUI_VISIBLE is set to 1.
     * The GUI is displayed only if LISTENER_GUI_VISIBLE is set to 1.
     */
    public static void main(String args[]) {
        AnalyserReader obj = new AnalyserReader();
        obj.userName = TipConstants.TARGIN_USER_NAME;
        obj.passWord = TipConstants.TARGIN_PASSWORD;
        obj.priHostName = TipConstants.TARGIN_PRIMARY_HOST;
        obj.priPortNum = TipConstants.TARGIN_PRIMARY_PORT;
        obj.secHostName = TipConstants.TARGIN_SECONDARY_HOST;
        obj.secPortNum = TipConstants.TARGIN_SECONDARY_PORT;
        obj.date = (int) TipConstants.TARGIN_FEED_REQ_DATE;
      
        int ret = 0;
        try{   	
        ret = CNativeInitialize();
        } catch(UnsatisfiedLinkError e){
        	 logger.debug("Call to CNativeInitialize() interrupted "+e.getMessage());
        	 e.printStackTrace();
        	 logger.error("Connection Error", e);
        	 System.exit(1);
        }
   
        if(ret == 0) {
            logger.fatal("Call to CNativeInitialize failed: " + CNativeGetErrorMessage());
            System.exit(1);
        }
        obj.startAnalyzerThreads();
        try { // to give time for other threads to stop
            Thread.sleep(5000);
        } catch (Exception e) {
        	logger.error("Thread is interrupted : " + e.getMessage());
        }
        obj.startDBThreads();
        try { // to give time for other threads to stop
            Thread.sleep(5000);
        } catch (Exception e) {
        	logger.error("Thread is interrupted : " + e.getMessage());
        }
        obj.ServiceRequest();
        try { // to give time for other threads to stop
            Thread.sleep(5000);
        } catch (Exception e) {
        	logger.error("Thread is interrupted : " + e.getMessage());
        }
        System.exit(0);
        
    }
   
    /**
     * This  method will reconnect to TARGIN if disconnected until max tries reached
     * @param null
     * @return boolean
     */
    private boolean connectTIP() {
        logger.debug("inside connectTIP funciton in AnalyserReader");
        int ret = TipConstants.TRUE;
        int numberOfAttempts = 0;
        // Establish connection with TIP server
        for (numberOfAttempts=0;numberOfAttempts<TipConstants.TARGIN_CONNECT_MAX_RETRY_COUNT;numberOfAttempts++) {
            // Connecting...
         
            logger.info("Connect Attempt " + numberOfAttempts + " to " + hostName + ":" + portNum);
         
            try{
            ret = CNativeConnect(hostName,portNum,TipConstants.TIP_COMPRESSION_DISABLED);
            }catch(Exception e){
          
            	 logger.error("Call to CNativeConnect failed: " + e.getMessage());
            }
            if (ret == TipConstants.FALSE) {
                logger.error("Call to CNativeConnect failed: " + CNativeGetErrorMessage());
            } else {
                // Connected
            
                logger.info("Connected to "+hostName+":"+portNum);
           
                return true;
            }
        }
        logger.debug("Exit connectTIP funciton in AnalyserReader");
     
        return false;
        
    }
    
    /**
     * This  method will send LogonRequest to TARGIN if Logon failed
     *  until max tries reached
     * @param null
     * @return boolean
     */
    private boolean LogonToTIP() {
        String logonresponse;
        int numberOfAttempts = 0;
        logger.debug("inside LogonToTIP funciton in AnalyserReader");
        for (numberOfAttempts=0;numberOfAttempts<TipConstants.TARGIN_CONNECT_MAX_RETRY_COUNT;numberOfAttempts++) {
            logger.info("Logon Attempt " + numberOfAttempts);
            
           logonresponse = CNativeLogon(userName,passWord,null);
     
            if (logonresponse == null) {
                logger.error("Call to CNativeLogon failed: " + CNativeGetErrorMessage());
                continue;
            }
            if (ProcessLogonLogoff(logonresponse,logonresponse.length())) {
                logger.info("ProcessMessage returned true");
                // Logged On
             
                return true;
            }
        }
        logger.debug("Exit LogonToTIP funciton in AnalyserReader");
        return false;
        
      
    }
    
    /**
     * This  method will send StreamingRequest to TARGIN
     * @param null
     * @return boolean
     */
    private boolean SendStreamingRequest() {
        logger.debug("inside SendStreamingRequest funciton in AnalyserReader");    
        long readerStartTime =0;
        long readerEndTime=0; 
        String buffer = null;
        TIPDataObjects.replayMode = false;
        FrameHolder oHolder = null;
        
        String logFileName = "javalog.txt";
        BufferedReader bufReader = null;
        logFileName = "tip_" + date + "_r" + TipConstants.TIP_REAL_TIME_REQUEST + "_s" + TipConstants.TIP_SCOPE_ALL + ".txt";
        if (TipConstants.LOG_FEED == 1 && TipConstants.FILE_BASED_RECOVERY == 1) {
            try {
                bufReader = new BufferedReader(new InputStreamReader(new FileInputStream(Utility.logFileCreation(logFileName))));
                // Replay from the old file
                TIPDataObjects.replayMode=true;
                logger.info("Replay mode started");
                // Beginning Replay                
                readerStartTime = System.currentTimeMillis();               
                while(true) {
                    buffer = bufReader.readLine();
                    if (buffer == null) {
                        break;
                    }
                    //buffer=buffer.concat("\n");
                    buffer = buffer.concat(TipConstants.TIP_LINE_SEPARATOR);
                    iMessageCount++;
                    readerEndTime = System.currentTimeMillis();
                    //oDataAnalyser.ProcessMessage(buffer, buffer.length(),replayMode,iMessageCount,readerStartTime,readerEndTime,readerEndTime,false);
                    oHolder = new FrameHolder(System.currentTimeMillis(), buffer,iMessageCount,readerStartTime,readerEndTime);
                    MessageQueue.g_oBuffer.setFrame(oHolder);                   
                 
                    buffer = null;                  
                    readerStartTime = System.currentTimeMillis();
                }
                bufReader.close();
            } catch (FileNotFoundException ex) {
                logger.info("No file found for replay...Sending streaming request from seq 1");
            } catch (IOException ex) {
                logger.error("IOException occured while reading log file for reloading " + ex);
                try {
                    if (bufReader!= null)
                        bufReader.close();
                } catch (IOException ex1) {
                    logger.error("Exception occured while closing the log file " + ex);
                }
                return false;
            }
            if (TIPDataObjects.replayMode) {
               startSeq = TIPDataObjects.last_Sequencer_Analyzer;
                endSeq = 0;
                TIPDataObjects.replayMode=false;
            }
            logger.info("Replay mode ended");
            // Replay Ended
           // GUIHelper.readerConnection = TipConstants.CONN_STATUS_ENDREPLAY;
            
        }
        logger.info("Before calling StreamingRequest: reqType "+ TipConstants.TIP_REAL_TIME_REQUEST
                + " scope " + TipConstants.TIP_SCOPE_ALL + " Date " + date + " startSeq " + startSeq + " endSeq " + endSeq);
        
        int ret=CNativeStreamingRequest(TipConstants.TIP_REAL_TIME_REQUEST,TipConstants.TIP_SCOPE_ALL,date,startSeq,endSeq);
        if (ret == TipConstants.FALSE) {
            logger.error("Call to CNativeStreamingRequest failed: " + CNativeGetErrorMessage());
            return false;
        }
        if (TipConstants.LOG_FEED == 1) {
            try {
                fos = new java.io.FileOutputStream(Utility.logFileCreation(logFileName),true);
            } catch (Exception e) {
                logger.error("File Output Stream Error");
            }
        }
        logger.debug("Exit SendStreamingRequest funciton in AnalyserReader");
        // Established
       // GUIHelper.readerConnection = TipConstants.CONN_STATUS_ESTABLISHED;
        return true;
    }
    
    /**
     * This  method will sends Logoff Request to TARGIN
     * @param null
     * @return boolean
     */
    public boolean HandleLogoff() {
        logger.debug("inside HandleLogoff funciton in AnalyserReader");
        String buffer;
        int ret = 0;
            
        ret = CNativeLogoff();
        if (ret == TipConstants.FALSE) {
            logger.error("Call to CNativeLogoff failed: " + CNativeGetErrorMessage());
            return false;
        }
        
        logoffSent = true;
        while ((buffer = CNativeGetNextMessage()) != null) {
            if (ProcessLogonLogoff(buffer,buffer.length())) {
                logger.info("Logoff Acknowledged by TIP Server.. ");
                ret = CNativeDisconnect();
                
                if (ret == TipConstants.FALSE)
                    logger.error("Call to CNativeDisconnect failed: " + CNativeGetErrorMessage());
                return true;
            }
        }
        logger.info("Logoff Not Acknowledged by TIP Server.. ");
        ret = CNativeDisconnect();
        if (ret == TipConstants.FALSE)
            logger.error("Call to CNativeDisconnect failed: " + CNativeGetErrorMessage());
        logger.debug("Exit HandleLogoff funciton in AnalyserReader");
        return false;
    }
    
    /**
     * This  method will process the Logon and Logoff request
     * @param buffer
     * @param len
     * @return boolean
     */
    private boolean ProcessLogonLogoff(String buffer, int len) {
        logger.debug("inside ProcessLogonLogoff funciton in AnalyserReader");
        TIP_MsgVO msg;
        
                /*
                 * Parse message and return a struct with all tags and values.
                 */
        if ((msg = ParseMsg(buffer, len)) == null) {
            logger.error("Message format error");
            return false;
        }
        
                /*
                 * Process general reply. General reply in streaming mode is
                 * only sent as a response to Logoff. The general reply is
                 * the last message sent; the server will disconnect the
                 * session now.
                 */
        
        if (msg.msgType.equals(TipConstants.TIP_MSGTYPE_GENERALREPLY)) {
            String replycode = FindTag(msg, TipConstants.fTag_ReplyCode);
            if (replycode == null) {
                logger.error("Invalid General reply: no reply code found");
                msg = null;
                return false;
            }
            logger.info("General reply: " + Integer.parseInt(replycode) + ":"
                    + FindTag(msg, TipConstants.fTag_RequestType) + ":" + TipConstants.TIP_MSGTYPE_LOGON + ":" + FindTag(msg, TipConstants.fTag_ReplyText));
            if (FindTag(msg, TipConstants.fTag_RequestType).equals(TipConstants.TIP_MSGTYPE_LOGON) && Integer.parseInt(replycode) == 0 ) {
                msg = null;
                return true;
            }
            if (FindTag(msg, TipConstants.fTag_RequestType).equals(TipConstants.TIP_MSGTYPE_LOGOFF) && Integer.parseInt(replycode) == 0 ) {
                msg = null;
                return true;
            }
        }
        logger.info("returning false from ProcessLogonLogoff, Msg Type received is " + msg.msgType);
        logger.debug("Exit ProcessLogonLogoff funciton in AnalyserReader");
        msg = null;
        return false;
    }
    /**
     * This  method will parse(TAG/Value pair) the message
     * @param buffer,len
     */
    public TIP_MsgVO ParseMsg(String buffer, int len) {
        
        InitializeTIPMsg_VO(msg);
        tags_values = buffer.split(";");
        if (msg == null) {
            logger.error("ParseMsg: Parameter TIP_Msg is null");
            return null;
        }
        
        msg.inputmsg = buffer;
        msg.length = len;
        /*
         * Verify that line termination is correct.
         */
        if (msg.inputmsg.charAt(msg.inputmsg.length() - 1) != TipConstants.TIP_ENDOFMSG) {
            logger.error("Message not correctly terminated" + msg.length
                    + msg.inputmsg);
            return null;
        }
        msg.msgType = tags_values[0];
        msg.numtags = 0;
        for (n = 1; n < tags_values.length; n++) {
            for (i = 0; i < tags_values[n].length(); i++) {
                if (Character.isLowerCase(tags_values[n].charAt(i))) {
                    msg.tags[msg.numtags] = tags_values[n].substring(0, ++i);
                    msg.values[msg.numtags] = tags_values[n].substring(i,
                            tags_values[n].length());
                    msg.numtags++;
                    break;
                }
            }
        }
        
        buffer = null;
        tags_values = null;
        return msg;
    }
    private void InitializeTIPMsg_VO(TIP_MsgVO tmpTipMsg){
        tmpTipMsg.inputmsg=null;
        tmpTipMsg.length=0;
        tmpTipMsg.msgType =null;
        tmpTipMsg.numtags=0;
        for (int i=0; i < 256; i++) {
            tmpTipMsg.tags[i]=null;
            tmpTipMsg.values[i]=null;
            
        }
    }
    
    /**
     * This  method will find the values of the corresponding Tag
     * @param ptags,ftag
     */
    public String FindTag(TIP_MsgVO ptags, String ftag) {
        
        for (count = 0; count < ptags.numtags; ++count) {
            if (ptags.tags[count].equals(ftag)) {
                return ptags.values[count];
            }
        }
        
        return null;
    }
    
    
    private void startAnalyzerThreads()
    {
    	// Create pool of threads
    	ExecutorService executor = Executors.newFixedThreadPool(1);    	
    	// separate the message queue based on data volume    	
    	Runnable dataReader = new DataReader();
    	executor.execute(dataReader);

    
    }
    
    
    private void startDBThreads()
    {
    	// Create pool of threads
    	ExecutorService executor = Executors.newFixedThreadPool(1);
    	
    	// separate the message queue based on data volume
    	
    	Runnable dbQueueUpdater = new DBQueueUpdater();
    	executor.execute(dbQueueUpdater);

    
    }
    
}