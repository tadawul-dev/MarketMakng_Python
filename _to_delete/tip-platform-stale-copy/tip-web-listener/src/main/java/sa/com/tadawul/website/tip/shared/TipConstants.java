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
 * Name          :     FileName : TipConstants.java
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 10/April/2009            Oracle                   Initial Version
 * 1.1				   14/November/2009         Mohanram,Oracle          Added functionalities to process the Equity Rights Share
 * 1.2				   02/February/2010         Mohanram,Oracle          Added functionalities to process the Exchange Traded Funds.
 * 2.1				   02/Aug/2016				Mohanram, Tadawul		 Added for Theoritical Open Price changes	
 *----------------------------------------------------------------------------------------------
 */
package sa.com.tadawul.website.tip.shared;


/**
 * This class contains all constants required for TIP application.
 * @version 1.0
 */
public class TipConstants {
    // TIP Parser/Generator constants
    public static final char TIP_ENDOFMSG = '\n';
    public static final char TIP_DELIMITER = ';';
    public static final char TIP_ESCAPE = '\\';
    public static final String TIP_LINE_SEPARATOR = System.lineSeparator();
    
    
    // Tip Message tag constants
    public static final String TIP_MSGTYPE_ENDOFSTREAM = "EOs";
    public static final String TIP_MSGTYPE_GENERALREPLY = "GEn";
    public static final String TIP_MSGTYPE_HEARTBEAT = "HBt";
    public static final String TIP_MSGTYPE_REALTIME = "RTr";
    public static final String TIP_MSGTYPE_BASICDATAMARKET = "BDm";
    public static final String TIP_MSGTYPE_BASICDATALIST = "BDLi";
    public static final String TIP_MSGTYPE_BASICDATAINDEX = "BDIn";
    public static final String TIP_MSGTYPE_INDEXSUMMARY = "INs";
    public static final String TIP_MSGTYPE_BASICDATASHARE = "BDSh";
//  Added for Share RI
    public static final String TIP_MSGTYPE_BASICDATARIGHT = "BDRi";
    public static final String TIP_MSGTYPE_ORDERBOOKSUMMARY = "OBs";
    public static final String TIP_MSGTYPE_TRADESTATISTICS = "TSt";
    public static final String TIP_MSGTYPE_TURNOVERLISTUPDATE = "TLu";
    public static final String TIP_MSGTYPE_INDEXUPDATE = "INd";
    public static final String TIP_MSGTYPE_ORDERBOOK = "OBk";
    public static final String TIP_MSGTYPE_ORDERBOOKSTATE = "OKs";
    public static final String TIP_MSGTYPE_TRADE = "TRd";
    public static final String TIP_MSGTYPE_MARKETSTATE = "STa";
    public static final String TIP_MSGTYPE_BASICDATABOND="BDBo";
    public static final String TIP_MSGTYPE_LOGON = "LGn";
    public static final String TIP_MSGTYPE_LOGOFF = "LGf";
    public static final String TIP_MSGTYPE_TRADEHALT = "TRh";
    public static final String TIP_MSGTYPE_ORDERBOOKREFERENCEPRICE = "OBRf";
   
    //Added for Theortical Opening price changes
    public static final String TIP_MSGTYPE_INDICATIVE_OPENING = "INo";
    
    //Added for SME Project
    public static final String TIP_MSGTYPE_BASICDATAINDEXMEMBER = "BDIm";
    
    // Tip Field tag constants
    public static final String fTag_ReplyCode = "Rc";
    public static final String fTag_ReplyText = "Rx";
    public static final String fTag_SequenceNumber = "n";
    public static final String fTag_RequestType = "Qt";
    public static final String fTag_Symbol                  ="SYm";
    public static final String fTag_Si                ="Si";
    public static final String fTag_ListType                ="LSt";
    public static final String fTag_TurnoverCalculationEnabled ="TCe";
    public static final String fTag_IdCode                  ="i";
    public static final String fTag_Name                    ="NAm";
    public static final String fTag_NameLocal               ="NAl";
    public static final String fTag_Date                    ="Dt";
    public static final String fTag_TimeExec                ="Tx";
    public static final String fTag_last_trade_quantity	="Tq";
    public static final String fTag_TradedPrice             ="Tp";
    public static final String fTag_LastPaidDate            ="LPd";
    public static final String fTag_CurrentAdjustedValue    ="CAv";
    public static final String fTag_InitialValue            ="Vi";
    public static final String fTag_HighValue               ="Vh";
    public static final String fTag_LowValue                ="Vl";
    public static final String fTag_DiffDayPer              ="Dd";
    public static final String fTag_DiffDayNom              ="DDn";
    public static final String fTag_TradedQuantity          ="Tq";
    public static final String fTag_TotalTraded             ="TOt";
    public static final String fTag_MinusPaid               ="Pm";
    public static final String fTag_UnchangedPaid           ="Pu";
    public static final String fTag_PlusPaid                ="Pp";
    public static final String fTag_CurrentValue            ="Vc";
    public static final String fTag_MarketId                ="m";
    public static final String fTag_TotalIssue              ="TIs";
    public static final String fTag_SharesListed            ="LNo";
    public static final String fTag_NominalValue            ="NMv";
    public static final String fTag_ShareCapital            ="SHc";
    public static final String fTag_AbbreviatedName         ="SNm";
    public static final String fTag_FirstPrice              ="Pf";
    public static final String fTag_LastPrice               ="Pl";
    public static final String fTag_HighPrice               ="Ph";
    public static final String fTag_LowPrice                ="LOp";
    public static final String fTag_HighPrice52Week         ="HPw";
    public static final String fTag_LowPrice52Week          ="LPw";
    public static final String fTag_OrderbookStateCode      ="OSt";
    public static final String fTag_AdjustedClosingPrice    ="ACp";
    public static final String fTag_HighPriceYearDate       ="HPYd";
    public static final String fTag_LowPriceYearDate        ="LPYd";
    public static final String fTag_AskVolumeAtLevel        ="Va";
    public static final String fTag_AskPriceAtLevel         ="a";
    public static final String fTag_BidVolumeAtLevel        ="Vb";
    public static final String fTag_BidPriceAtLevel         ="b";
    public static final String fTag_OrderBookFlush          ="Of";
    public static final String fTag_AskLevelDeleted         ="LDs";
    public static final String fTag_BidLevelDeleted         ="LDb";
    public static final String fTag_AccumulatedVolume       ="Aq";
    public static final String fTag_AccumulatedTurnover     ="At";
    public static final String fTag_DiffLastPricePercent    ="PLp";
    public static final String fTag_DiffLastPrice           ="Pd";
    public static final String fTag_NumberOfTrades          ="ANt";
    public static final String fTag_LastTradeVolume         ="LTv";
    public static final String fTag_MarketCap               ="CAp";
    public static final String fTag_MarketCapital           ="MCa";
    public static final String fTag_BookRatio               ="Br";
    public static final String fTag_PriceEarningsRatio      ="Pr";
    public static final String fTag_AverageYield            ="Yv";
    public static final String fTag_LastYield               ="Yl";
    public static final String fTag_MarketStateCode         ="MSt";
    public static final String fTag_BidPriceAtLevel1        ="BPr";
    public static final String fTag_AskPriceAtLevel1        ="APl";
    public static final String fTag_BidVolumeAtLevel1       ="VBl";
    public static final String fTag_AskVolumeAtLevel1       ="VAl";
    public static final String fTag_ObjectType              ="OBt";
    public static final String fTag_SuspResume              ="SRe";
    public static final String fTag_UpdateCode              ="Uc";
    public static final String fTag_ReferencePrice          ="RPr";
    public static final String fTag_TradeUpdatesLastPaid    ="Ul";
    public static final String fTag_OfficialClosingPrice	="POf";
    
    //Added for Rights Issue Reference Price Upper Limit and Reference Price Lower Limit
    public static final String fTag_ReferencePrice_UpperLimit    ="RPu";
    public static final String fTag_ReferencePrice_LowerLimit    ="RPl";

    //TradeType
    public static final String fTag_Tradetype				="Tt";
    
    //Added for XStream Changes
    public static final String fTag_ReportedVolume				="Rq";
    public static final String fTag_ReportedTurnover			="Rt";
    
    //Added for Theoritical Open Price changes
    public static final String fTag_IndicativePrice    ="TOp";
    public static final String fTag_IndicativeVolume    ="TOv";
    
    //Added for SME Project
    public static final String fTag_IdOrderBook    ="IDo";
    public static final String fTag_SecurityGroupId    ="CDi";
    
    public static final int NODATAFOUND = -1;
    public static final String SPLITMESSAGE = ";";
    public static final String SPLITVALUE = "[a-z]";    
    
    public static String msgTypes[] = new String[] { TIP_MSGTYPE_BASICDATALIST,
    TIP_MSGTYPE_BASICDATAINDEX, TIP_MSGTYPE_INDEXSUMMARY,
    TIP_MSGTYPE_BASICDATASHARE,
//  Added for Share RI
    TIP_MSGTYPE_BASICDATARIGHT,
    TIP_MSGTYPE_ORDERBOOKSUMMARY,
    TIP_MSGTYPE_TRADESTATISTICS, TIP_MSGTYPE_TURNOVERLISTUPDATE,
    TIP_MSGTYPE_INDEXUPDATE, TIP_MSGTYPE_ORDERBOOK,
    TIP_MSGTYPE_ORDERBOOKSTATE, TIP_MSGTYPE_TRADE,
    TIP_MSGTYPE_BASICDATABOND, TIP_MSGTYPE_TRADEHALT, TIP_MSGTYPE_ORDERBOOKREFERENCEPRICE,
    //Added for Theortical Opening price changes
    TIP_MSGTYPE_INDICATIVE_OPENING,TIP_MSGTYPE_BASICDATAINDEXMEMBER
   //Added for XStream changes
   // TIP_MSGTYPE_MARKETSTATE
    };
    
    public static final double twoDecimalRoundOff = 100.0;
    public static final double threeDecimalRoundOff = 1000.0;
    //Added for Yield calculations projects CRQ21157
    public static final double fourDecimalRoundOff = 10000.0;
    
    // Logic Specific Constants
    public static final boolean INTRA_TO_DB = true;
    public static final String fos_Index_FILE ="TIP_Index.txt";
    public static final String fos_share_FILE = "TIP_Share.txt";
    public static final String fos_Bond_FILE = "TIP_Bond.txt";
    /** Global stop signal. volatile: it is written by one thread and polled by ~30 others
     *  (without volatile the JIT may hoist the read out of a hot loop and never see the change). */
    public static volatile boolean shutdownFlag = false;
    public static boolean DB_AUTO_COMMIT_FLAG = false;
    public static final int ERROR_LIST_SIZE = 25;
    
    public static int TARGIN_CONNECT_MAX_RETRY_COUNT;
    public static final int TIP_COMPRESSION_DISABLED = 0;
    public static final int TIP_REAL_TIME_REQUEST = 1;
    public static final int TIP_BASIC_DATA_REQUEST = 2;
    public static final int TIP_SCOPE_ALL = 3;
    public static final int TRUE = 1;
    public static final int FALSE = 0;
    
    // Market States as in OBs OrderbookStateCode as in TIP Message
    public static final int TIP_STATE_CLOSED = 1;
    public static final int TIP_STATE_CALL_INTERACTION = 4;
    public static final int TIP_STATE_CONTINUOUS_TRADING = 6;
    public static final int TIP_STATE_POST_TRADING = 8;
  //Added for Closing Auction changes
    public static final int TIP_STATE_CLOSING_AUCTION = 10;
  //Added for SR3 changes
    public static final int TIP_STATE_TRADEATLAST = 33;
  //Added for SR2 NegDeals Only
    public static final int TIP_STATE_NEGDEALS_ONLY = 41;
    public static final int TIP_STATE_NEGDEALS_NOTPLUS0 = 42;
    
    // Market States as in Sta Message as in TIP Message
    public static final int TIP_MARKET_STATE_CLOSED = 1;
    public static final int TIP_MARKET_STATE_PRE_TRADING = 2;
    public static final int TIP_MARKET_STATE_TRADING = 3;
    public static final int TIP_MARKET_STATE_POST_TRADING = 5;
    //Added for Closing Auction changes
    public static final int TIP_MARKET_STATE_CLOSING_AUCTION = 6;
    //Added for SR3 changes
    public static final int TIP_MARKET_STATE_TRADEATLAST = 9;
    
    //Added for SR2 NegDeals Only
    public static final int TIP_MARKET_STATE_NEGDEALS_ONLY = 13;
    public static final int TIP_MARKET_STATE_NEGDEALS_NOTPLUS0 = 14;
    
    // Market States as in Database
    public static final int TIP_DB_STATE_CLOSED = 3;
    public static final int TIP_DB_STATE_PREOPENED = 1;
    public static final int TIP_DB_STATE_OPENED = 2;
    public static final int TIP_DB_STATE_PRECLOSED = 4;
    public static final int TIP_DB_STATE_SUSPENDED = 0;
  //Added for Closing Auction changes
    public static final int TIP_DB_STATE_CLOSING_AUCTION = 6;
  //Added for SR3 changes
    public static final int TIP_DB_STATE_TRADEATLAST = 9;
    
   //Added for SR2 NegDearlsOnly
    public static final int TIP_DB_STATE_NEGDEALSONLY = 10;
    
    public static final int TIP_SHARE_BOD = -1;
    public static final int TIP_SHARE_EOD = -2;
    public static final int TIP_BOND_BOD = -3;
    public static final int TIP_BOND_EOD = -4;
    
    //efp.ini Key Values
    public static String SHARE_MARKET_CODE;   
    public static long SHARE_MARKET_ID;
    public static long SHARE_MARKET_LIST_ID;
    
    public static String BOND_MARKET_CODE;  
    public static long BOND_MARKET_ID;
    public static long BOND_MARKET_LIST_ID;
    
    //Added for SME
    public static String SME_MARKET_CODE;   
    public static long SME_MARKET_ID;
    public static long SME_MARKET_LIST_ID;
    
   //Added for PTTP Project CRQ22398
    public static String OTC_MARKET_CODE;   
    public static long OTC_MARKET_ID;
    
    //Added for PTTP Project CRQ22398
    public static String BOND_OTC_MARKET_CODE;   
    public static long BOND_OTC_MARKET_ID;
    
    public static String SHARED_LIST_MAPPING;
    
    public static String TARGIN_USER_NAME;
    public static String TARGIN_PASSWORD;
    public static String TARGIN_PRIMARY_HOST;
    public static int TARGIN_PRIMARY_PORT;
    public static String TARGIN_SECONDARY_HOST;
    public static int TARGIN_SECONDARY_PORT;
    public static long TARGIN_FEED_REQ_DATE;
    public static String DB_FEED_REQ_DATE;
    public static boolean IS_PAST_OR_FUTURE_DATE=false;
    
    public static int TIP_MAX_NO_MARKET;
    public static int TIP_MAX_NO_INDICES;
    public static int TIP_MAX_NO_SME_INDICES;
    public static int TIP_MAX_NO_BOND_INDICES;
    public static int TIP_MAX_NO_SHARES;
    public static int TIP_MAX_NO_SME_SHARES;
    public static int TIP_MAX_NO_BONDS;
    //Added for PTTP Project CRQ22398
    public static int TIP_MAX_NO_OTC_SHARES;
    
    //Added for PTTP Project CRQ22398
    public static int TIP_MAX_NO_BOND_OTC_SHARES;
    
    public static int TIP_MAX_DELAY_TIME;

    
    public static int USE_DATA_SOURCE;
    public static int DB_CONNECTION_MAX_RETRY_COUNT;
    public static String JDBC_DRIVER;
    public static String DB_URL;
    public static String LOG_FILE_DIRECTORY;
    public static String DB_USER;
    public static String DB_PASS;
    public static String DB_SERVERNAME;
    public static int DB_PORT;
    
    public static long WEBSITE_DISPLAY_DELAY_TIME;
    public static int LOG_FEED;
    public static int FILE_BASED_RECOVERY;
    public static String XMP_LIBRARY;
    
  
    
    //Values are in milli seconds
    public static int DATA_ANALYZER_SLEEP_INTERVAL_TRD=1; //1 milli seconds
    public static int DATA_ANALYZER_SLEEP_INTERVAL_IND=2; //2 milli seconds
    public static int DATA_ANALYZER_SLEEP_INTERVAL_TLU=2; //2 milli seconds
    //This should be less than TRD to process for Negotiated deals
    public static int DATA_ANALYZER_SLEEP_INTERVAL_TST=2; //2 milli seconds
    public static int DATA_ANALYZER_SLEEP_INTERVAL_OBK=2; //2 milli seconds
    public static int DATA_ANALYZER_SLEEP_INTERVAL_OKS=2; //2 milli seconds
    public static int DATA_ANALYZER_SLEEP_INTERVAL_INO=2; //2 milli seconds
        
    public static int DATA_ANALYZER_SLEEP_INTERVAL_LOWVOLUME=1; //5 milli seconds
    //public static int DATA_PARSER_SLEEP_INTERVAL=1;
    public static int DATA_PARSER_SLEEP_INTERVAL_DBQUEUE_UPDATER=2;//2 milli seconds (conflation tick of DBQueueUpdater)

    // ---- Blocking-queue wait bounds (replace the legacy 1-2 ms / 100 ms sleep-polling) ----
    // Consumers now wake up immediately when data arrives; these values only bound how long an
    // idle consumer sleeps before re-checking housekeeping (periodic commits, shutdown flag).
    /** Max idle wait of a parser thread on its message queue (ms). */
    public static final long PARSER_QUEUE_IDLE_WAIT_MS = 500L;
    /** Max idle wait of a DB writer/updater thread on its buffer (ms). Same cadence as the old sleep(100). */
    public static final long DB_QUEUE_IDLE_WAIT_MS = 100L;
    /** Max idle wait of the DataReader dispatcher on the raw frame buffer (ms). */
    public static final long READER_QUEUE_IDLE_WAIT_MS = 500L;

    // ---- Feed log (efp.ini, optional) ----
    /** FEED_LOG_FLUSH_INTERVAL_MS: 0 = flush after every message (legacy behaviour). */
    public static long FEED_LOG_FLUSH_INTERVAL_MS = 200L;

    // ---- Pure-Java TIP client (efp.ini, optional; replaces libtipclient.so) ----
    /** FEED_CHARSET: "JNI" (default) = decode exactly like the old JNI library; or a charset name, e.g. windows-1256. */
    public static String FEED_CHARSET = "JNI";
    /** Legacy Cp1252 -> Windows-1256 re-encoding of Arabic text in the DB layer. On only in JNI mode. */
    public static volatile boolean ARABIC_REENCODE = true;
    /** TIP_COMPRESSION: 1 = ask the server for LZ1 compression (the JNI call always passed 0). */
    public static boolean TIP_COMPRESSION = false;
    /** TIP_CONNECT_TIMEOUT_SECS: TCP connect timeout per address. */
    public static int TIP_CONNECT_TIMEOUT_SECS = 30;
    /** TIP_RECV_TIMEOUT_SECS: reconnect if nothing (not even a heartbeat) arrives for this long. 0 = wait forever (legacy). */
    public static int TIP_RECV_TIMEOUT_SECS = 0;
    /** TIP_QUEUE_ALERT_MS: server-side slow-consumer alert threshold sent with each request. */
    public static int TIP_QUEUE_ALERT_MS = 500;
    /** TARGIN_PROTOCOL_VERSION: optional VEr sent at logon (the JNI call sent none). */
    public static String TARGIN_PROTOCOL_VERSION = null;
    /** SHUTDOWN_DRAIN_SECS: on SIGTERM/SIGINT, max time to wait for the DB queues to drain. */
    public static int SHUTDOWN_DRAIN_SECS = 20;
    
    
    //public static int UPDATER_THREADS_DB_UPDATE_INTERVAL=30000;// 30 seconds
    //public static int WRITER_THREADS_DB_UPDATE_INTERVAL=30000;// 30 seconds
    
    //UPDATED for Mobile index updates for 5 seconds or 10 seconds
    public static int UPDATER_THREADS_DB_UPDATE_INTERVAL=5000;// 5 seconds
    public static int UPDATER_THREADS_DB_UPDATE_BBO_INTERVAL=5000;// 5 seconds
    public static int WRITER_THREADS_DB_UPDATE_INTERVAL=10000;// 10 seconds
    
   // public static int UPDATER_THREADS_DB_FILE_UPDATE_INTERVAL=30000;// 30 seconds
   // public static int WRITER_THREADS_DB_FILE_UPDATE_INTERVAL=30000;// 30 seconds
    
    //UPDATED for Mobile index updates for  10 seconds
    public static int UPDATER_THREADS_DB_FILE_UPDATE_INTERVAL=10000;// 10 seconds
    public static int WRITER_THREADS_DB_FILE_UPDATE_INTERVAL=10000;// 10 seconds
    
    public static int UPDATER_THREADS_DB_UPDATE_INTERVAL_NODATA_RECEIVED=300000;// 5 minutes
    public static int WRITER_THREADS_DB_UPDATE_INTERVAL_NODATA_RECEIVED=300000;// 5 minutes
    
    public static int SLEEPINTERVALFOROBK_OKS_INOFORFIRSTMESSAGE=5000;
    
    public static int SLEEPWAIT_FOR_EOS_BEFORE_SHUTDOWN=60000;//1 minutes
    public static int SLEEPWAIT_FOR_EOS_BEFORE_SHUTDOWN_ACTUAL=600000;//10 minutes
    public static int SLEEPWAIT_FOR_EOD_BEFORE_PROCESSING=1000;//1 second
    public static int SLEEPWAIT_FOR_EOD_BEFORE_PROCESSING_ACTUAL=180000;//3 Minute
    
    //public static int SLEEPWAIT_FOR_OBS_BEFORE_PROCESSING=180000;//3 minute
    
    //Added for SME Project
    public static final String SOURCE_ENCODING = "Cp1252";
   // public static final String SOURCE_ENCODING = "UTF-8";
    public static final String DEST_ENCODING = "Windows-1256";
   // public static final String DEST_ENCODING = "Cp1256";
    
    
    public static final String THREAD_NAME_BOND_INDEXINTRA = "BondIndexIntra";
    public static final String THREAD_NAME_BONDINDEXUPDATER = "BondIndexUpdater";
    
    public static final String THREAD_NAME_SHAREINTRA = "ShareIntra";
    public static final String THREAD_NAME_SMESHAREINTRA = "SMEShareIntra";
    public static final String THREAD_NAME_INDEXINTRA = "IndexIntra";
    public static final String THREAD_NAME_SMEINDEXINTRA = "SMEIndexIntra";
    public static final String THREAD_NAME_BONDINTRA = "BondIntra";
    public static final String THREAD_NAME_INDEXUPDATER = "IndexUpdater";
    public static final String THREAD_NAME_SMEINDEXUPDATER = "SMEIndexUpdater";
    public static final String THREAD_NAME_SHAREUPDATER = "ShareUpdater";
    public static final String THREAD_NAME_SMESHAREUPDATER = "SMEShareUpdater";
    public static final String THREAD_NAME_BONDUPDATER = "BondUpdater";
    public static final String THREAD_NAME_MARKETUPDATER = "MarketUpdater";
    public static final String THREAD_NAME_DATA_ANALYSER = "DataAnalyser";
    public static final String SHARE = "Share";
    public static final String BOND = "Bond";
    public static final String BOND_OTC = "BondOTC";
    //Added for SME
    public static final String SME = "SME";
    
    public static final String CONN_STATUS_CONNECTING = "Connecting...";
    public static final String CONN_STATUS_CONNECTED = "Connected";
    public static final String CONN_STATUS_NOTCONNECTED = "Not Connected";
    public static final String CONN_STATUS_LOGGEDON = "Logged On";
    public static final String CONN_STATUS_BEGINREPLAY = "Beginning Replay";
    public static final String CONN_STATUS_ENDREPLAY = "Replay Ended";
    public static final String CONN_STATUS_ESTABLISHED = "Established";
    public static final String CONN_STATUS_STOPPED = "Stopped";
    
    /** yyyyMMdd parser. Was a shared SimpleDateFormat (not thread-safe); now one instance per thread,
     *  with identical (lenient) parsing semantics. */
    public static final ThreadSafeDateFormat dateFormatter = new ThreadSafeDateFormat("yyyyMMdd");
    
    
    public static final int UPDATE_CODE_INSERT=1;
    public static final int UPDATE_CODE_UPDATE=2; 
    public static final int UPDATE_CODE_DELETE=3; 
    public static final int UPDATE_CODE_UNCHANGED=4;
    
    
    public static final String BOND_TYPE_LISTED="L" ;
    public static final String BOND_TYPE_UNLISTED="U" ;
    
    
    public static final String BOND_SUBTYPE_FIXED="Fixed" ;
    public static final String BOND_SUBTYPE_FLOATING="Floating" ;
    
    public static boolean IS_NEGDEALS_EARLY_START=false;
    public static int IS_NEGDEALS_EARLY_START_VAL = 0;
    
}