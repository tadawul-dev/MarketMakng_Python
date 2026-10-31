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
 * Name          :     FileName : DataReader.java
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 15 - April  2018           Mohanram                 Initial Version
 *----------------------------------------------------------------------------------------------
 */

package sa.com.tadawul.website.tip.dataanalyser;

import sa.com.tadawul.website.tip.log.Log;
import sa.com.tadawul.website.tip.log.LogFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sa.com.tadawul.website.tip.messages.MessageType;
import sa.com.tadawul.website.tip.queue.FrameHolder;
import sa.com.tadawul.website.tip.queue.MessageQueue;
import sa.com.tadawul.website.tip.shared.LoadInitialData;
import sa.com.tadawul.website.tip.shared.TIPDataObjects;
import sa.com.tadawul.website.tip.shared.TipConstants;
import sa.com.tadawul.website.tip.shared.Utility;
import sa.com.tadawul.website.tip.vo.TIP_MarketVO;


public class DataReader implements Runnable {

	boolean isLowVolume = false;
	long startTime = 0;
	long endTime = 0;
	long timedef = 100;
	String msg = "";
	String str = "";
	String splitString[];
	String replycode;
	public static long seqno = 1;	
	private final static Log logger = LogFactory.getLog(DataReader.class);
	int noOfIndexUpdate=0;
	int noOfTurnOverlistUpdate=0;
	int noOfTradeUpdate=0;
	int noOfTradeStatisticsUpdate=0;
	int noOfOrderBook=0;
	int noOfOrderBookStatus=0;
	int noOfIndicativeOpening=0;
	int noOfOrderBookSummeries=0;
	int noOfLowVolumeMsgs=0;
	int totalNoofMesssagesReceived=0;
	 long lInterval = 500L;

	public DataReader() {	
	}

	/**
	 * The code to segregate the different message queue is written in this run
	 * method. All the messages coming from the TIP is pushed in the main
	 * message queue in the ServiceRequest method of AnalyserReader class. Those
	 * messages is segregated in this method based on volume of the data. Seven
	 * enum is created which is of high volume data type. Based on the enum
	 * switch statement calls the corresponding message and push the message in
	 * particular queue. Rest 20 messages will be set through default case of
	 * switch in separate queue.
	 * 
	 * @exception Exception
	 *                If there is any exception during processing of
	 *                messages.
	 * 
	 */	
	@Override
	public void run() {

		logger.info("DataReader Thread started. " );
			
		LoadInitialData loadinitData = new LoadInitialData();
		loadinitData.loadHashMap();				
		initializeMarketArrays();
		
		//This should be after the HashMap Load completed
		startDataParserThreads();	
		
		FrameHolder oFrameHolder = null;		
		
		while( !TipConstants.shutdownFlag)
			{					
			try
			{
				//oFrameHolder = null;
				startTime = System.currentTimeMillis();
		//	logger.info("remaining Messages : g_oInBuffer size " +MessageQueue.g_oBuffer.size() );
					//if (MessageQueue.g_oBuffer != null && MessageQueue.g_oBuffer.size() > 0) {		
				while(MessageQueue.g_oBuffer != null && MessageQueue.g_oBuffer.size() > 0) 
				{
					if (oFrameHolder == null)
                        oFrameHolder = MessageQueue.g_oBuffer.getFrame();                      
					
					startTime = System.currentTimeMillis();
					//logger.info("startTime  :"+startTime +" Frame time :"+oFrameHolder.getTime());
					//To handle the website display delay
					if(startTime >=  oFrameHolder.getTime())
					{	
							//logger.info("Message received for :"+oFrameHolder.getG_frameNo() +" After Display timeout. Hence proceeding for the processing");
							msg = oFrameHolder.getBuffer();
							splitString = msg.split(TipConstants.SPLITMESSAGE, 2);
							if (splitString[0] != null) {
								totalNoofMesssagesReceived++;					            
								switch (MessageType.getMessageValue(splitString[0])) {
								case 1:
									noOfTurnOverlistUpdate++;
									MessageQueue.turnoverListUpdateQueue.push(splitString[1]);
									break;									
								case 2:
									noOfIndexUpdate++;
									MessageQueue.indexUpdateQueue.push(splitString[1]);		
									break;
								case 3:								
									noOfTradeUpdate++;
									MessageQueue.tRdAndTStQueue.push(msg);
									break;
								case 4:
									noOfTradeStatisticsUpdate++;
									MessageQueue.tRdAndTStQueue.push(msg);
									break;
								case 5:
									noOfOrderBook++;									
									MessageQueue.orderBookQueue.push(splitString[1]);
									break;
								case 6:
									noOfOrderBookStatus++;
									MessageQueue.orderBookStateQueue.push(splitString[1]);
									break;
								case 7:
									noOfIndicativeOpening++;
									MessageQueue.indicativeOpeningQueue.push(splitString[1]);
									break;
								case 8:
									noOfOrderBookSummeries++;
									MessageQueue.tRdAndTStQueue.push(msg);
									break;
								default:
									noOfLowVolumeMsgs++;
									MessageQueue.lowVlmMsgQueue.push(msg);									
									break;
								}
							}					
				
					oFrameHolder = null;			
					}//End for If condition
					else
					{
						//logger.info("Message received for :"+oFrameHolder.getG_frameNo() +" within the website display time wait. Hence waiting for the Display time");					
						 lInterval = oFrameHolder.getTime() - System.currentTimeMillis();
						// logger.info("Wait interval time :"+lInterval);
                         Utility.sleepAnalyser(lInterval);
					}
					}//End for inner while
				//	logger.info("remaining Messages : DataParserBuffer ");
				//	logger.info("DataReader  Statistics : No of IndexUpdates : "+noOfIndexUpdate +" No of TLUs : "+noOfTurnOverlistUpdate +" No of Trades : "+noOfTradeUpdate +" No of TSts : "+noOfTradeStatisticsUpdate +" OrderBood : "+noOfOrderBook +" OrderBookStatus : "+noOfOrderBookStatus+ " No of Indicative Openings : "+noOfIndicativeOpening +" No of LowVolumemessages :"+noOfLowVolumeMsgs +"  Total Messages read :"+totalNoofMesssagesReceived +"  Total Messages Parsed :"+TIPDataObjects.totalMessagesParsed);
				//	logger.debug("remaining Messages : DataParserBuffer :"+g_oInBuffer.size() +" TLu"+messageQueue.turnoverListUpdateQueue.size() +"  INd :"+messageQueue.indexUpdateQueue.size() +" TRd :"+messageQueue.tradeQueue.size() +" TSt :"+messageQueue.tradeStatisticsQueue.size()+" OBk:"+messageQueue.orderBookQueue.size() +" OKs :"+messageQueue.orderBookStateQueue.size()+" INo:"+messageQueue.indicativeOpeningQueue.size() +" LowVoulme:"+messageQueue.lowVlmMsgQueue.size());
					//Utility.sleepAnalyser(TipConstants.DATA_PARSER_SLEEP_INTERVAL);
				//logger.info("DataReader  Statistics : No of IndexUpdates : "+noOfIndexUpdate +" No of TLUs : "+noOfTurnOverlistUpdate +" No of Trades : "+noOfTradeUpdate +" No of TSts : "+noOfTradeStatisticsUpdate +" OrderBood : "+noOfOrderBook +" OrderBookStatus : "+noOfOrderBookStatus+ " No of Indicative Openings : "+noOfIndicativeOpening +" No of LowVolumemessages :"+noOfLowVolumeMsgs +"  Total Messages read :"+totalNoofMesssagesReceived +" Total OrderBookSummyMessage Received :"+noOfOrderBookSummeries +" Total Messages Parsed :"+TIPDataObjects.totalMessagesParsed);
					
				
				} catch (Exception e1) {		
					e1.printStackTrace();
					logger.error("Exception in run method of DataReader class. " + e1.getMessage());
				}
				}

		
	}

	private void startDataParserThreads()
    {
    	// Create pool of threads
    	ExecutorService executor = Executors.newFixedThreadPool(7);
    	    	
    	 //Insert the low Volume message in DB
    	   Runnable dataParserForLowVolm = new DataParserLowVolm();
           executor.execute(dataParserForLowVolm);
           
         Runnable dataParserIndexUpdates = new DataParserIndexUpdates();
           executor.execute(dataParserIndexUpdates);           
       
           Runnable dataParserTurnOverListUpdates = new DataParserTurnOverListUpdates();
           executor.execute(dataParserTurnOverListUpdates);         
                     
           Runnable dataParserTradesAndTradeStatisticsUpdate = new DataParserTrdAndTSt();
           executor.execute(dataParserTradesAndTradeStatisticsUpdate);
           
           Runnable dataParserOrderBook = new DataParserOrderBook();
           executor.execute(dataParserOrderBook);
           
           Runnable dataParserOrderBookState = new DataParserOrderBookState();
           executor.execute(dataParserOrderBookState);
      
          Runnable dataParserIndicativeOpenings = new DataParserIndicativeOpenings();
          executor.execute(dataParserIndicativeOpenings);
    }
	
	private void initializeMarketArrays()
    {
		TIPDataObjects.tipMarket[0] = new TIP_MarketVO();
		TIPDataObjects. tipMarket[0].MarketId = TipConstants.SHARE_MARKET_ID;
		TIPDataObjects. tipMarket[0].Pk_exchange_code = TipConstants.SHARE_MARKET_CODE;
     
		TIPDataObjects.tipMarket[1] = new TIP_MarketVO();
		TIPDataObjects.tipMarket[1].MarketId = TipConstants.BOND_MARKET_ID;
		TIPDataObjects.tipMarket[1].Pk_exchange_code = TipConstants.BOND_MARKET_CODE;
     //Added for SME
		TIPDataObjects.tipMarket[2] = new TIP_MarketVO();
		TIPDataObjects.tipMarket[2].MarketId = TipConstants.SME_MARKET_ID;
		TIPDataObjects. tipMarket[2].Pk_exchange_code = TipConstants.SME_MARKET_CODE;
    }
	
}


