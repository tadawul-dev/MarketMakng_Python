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
 * Name          :     FileName :  DBUpdateThread.java
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 10/April/2009            Oracle                   Initial Version
 * 1.1				   14/November/2009         Mohanram,Oracle          Added functionalities to process the Equity Rights Share
 * 1.2				   02/February/2010         Mohanram,Oracle          Added functionalities to process the Exchange Traded Funds.
 *----------------------------------------------------------------------------------------------
 */
package sa.com.tadawul.website.tip.DBThreads;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import sa.com.tadawul.website.tip.log.Log;
import sa.com.tadawul.website.tip.log.LogFactory;
import sa.com.tadawul.website.tip.queue.MessageDatabaseQueue;
import sa.com.tadawul.website.tip.shared.DBManagerMarket;
import sa.com.tadawul.website.tip.shared.TipConstants;
import sa.com.tadawul.website.tip.shared.Utility;
import sa.com.tadawul.website.tip.vo.TIP_BaseVO;
import sa.com.tadawul.website.tip.vo.TIP_MarketVO;

/**
 * @description The DBUpdateThread creates a single instance each for Index,
 *              Share, Bond and Market. This class handles the BOD/EOD logic for
 *              index, share, bond. It retrieves the specific VO from queue and
 *              updates the respective tables. The thread will continue running
 *              until there are messages in queue, after which if the
 *              shutdownFlag is set to true will stop running. The
 *              DBUpdateThread will also write the last sequence number
 *              committed to database to a separate log file.
 *
 */

public class DBUpdateMarket extends DBUpdateThread {

	private final static Log logger = LogFactory.getLog(DBUpdateMarket.class);
	DBManagerMarket dbUtilObj;
	static long currentUpdatedTime = 0;
	static long  lastUpdatedTime = 0;
	
	public DBUpdateMarket( String threadName) throws Exception {
		super(threadName);
		dbUtilObj= new DBManagerMarket(threadName);
		
	}

	/**
	 * This method is called on starting the DBUpdateThread
	 *
	 */
	public void run() {
		boolean ret=true;
		long insertCounter = 0;
		long totalInsertedCounter = 0;
		BufferedWriter bufferedWriter = null;
		BufferedReader in = null;
		String fileName = threadname + "_" + TipConstants.TARGIN_FEED_REQ_DATE+ ".txt";
		int startCount = 0;
		TIP_BaseVO baseObj = null;
	

		try {
			File file = new File(Utility.logFileCreation(fileName));
			if (file.exists()) {
				in = new BufferedReader(new FileReader(Utility.logFileCreation(fileName)));
				startCount = Integer.parseInt(in.readLine());
				in.close();
			}

			while (true) {
				// logger.info("Thread Name :DBUpdateMarket, buffer  :"+g_oBuffer.tipmarketQ.size());
				if (MessageDatabaseQueue.tipmarketQ.size() > 0) {
					baseObj = (TIP_BaseVO) MessageDatabaseQueue.tipmarketQ.getFrame();
					 logger.info("Thread Name :DBUpdateMarket, buffer  :"+MessageDatabaseQueue.tipmarketQ.size() +" baseObj is BOD :"+baseObj.isBOD +"  baseObj.isBODPreOpen :"+baseObj.isBODPreOpen);
					 
				      //	if (baseObj.sequence_no <= startCount && !baseObj.isBOD		&& !baseObj.isEOD) {
					if (baseObj.sequence_no <= startCount ) {
						//logger.debug("Ignoring Message with sequence_no: "+ baseObj.sequence_no + " startCount: "+ startCount + " isBOD: " + baseObj.isBOD	+ " isEOD: " + baseObj.isEOD);
						MessageDatabaseQueue.tipmarketQ.releaseFrame();
						baseObj = null;
						continue;
					}
					if (baseObj.sequence_no > seqNum) {
						seqNum = baseObj.sequence_no;						
					}

					if ((baseObj.isBOD || baseObj.isEOD) && insertCounter > 0) {
						dbUtilObj.commitTrans(true);
						totalInsertedCounter += insertCounter;
						logger.info("Seq# " + seqNum +" updateCounter " + insertCounter	+ " totalUpdatedCounter "		+ totalInsertedCounter + " Remaining Msgs : "	+ MessageDatabaseQueue.tipmarketQ.size() + " BOD/EOD");
						insertCounter = 0;
					}
					
					logger.info(" baseObj.isBODPreOpen :"+baseObj.isBODPreOpen);
					
					if (baseObj.isBODPreOpen) {					
						logger.info("BOD PreOpen for Market");
						//ret = dbUtilObj.updateMarketInfoData((TIP_MarketVO) baseObj);
						ret = dbUtilObj.initMarketStateData((TIP_MarketVO) baseObj);
						if (!ret) {
							logger.error("BOD isBODOpen Operations for  Data in DB Failed");
							TipConstants.shutdownFlag = true;
						}
					}else
					if (baseObj.isBODOpen) {
						logger.info("BOD Open for Market");
						//ret = dbUtilObj.updateMarketInfoData((TIP_MarketVO) baseObj);
						ret = dbUtilObj.initMarketInfoData((TIP_MarketVO) baseObj);
						if (!ret) {
							logger.error("BOD Operations for  Data in DB Failed");
							TipConstants.shutdownFlag = true;
						}
					}//else
				//	{
						try
						{
						ret = dbUtilObj.updateMarketInfoData((TIP_MarketVO) baseObj);
						}catch(Exception e)
						{
							logger.error("Exception in " + threadname + " thread:" + e,e);
						}
						if (!ret) {
							logger.error("Update/Insert Market Data to DB Failed");
						}
						MessageDatabaseQueue.tipmarketQ.releaseFrame();
						insertCounter++;
				//	}
					
				} else {
					if (TipConstants.shutdownFlag) {
						logger.info("Shutdown received in Thread:DBUpdateThread." + threadname);					
						break;
					}
					Thread.sleep(100);
				}

				currentUpdatedTime =  System.currentTimeMillis();
				if (insertCounter > 0 &&  (currentUpdatedTime-lastUpdatedTime)>TipConstants.UPDATER_THREADS_DB_FILE_UPDATE_INTERVAL) {
					dbUtilObj.commitTrans(true);					
					bufferedWriter = new BufferedWriter(new FileWriter(	Utility.logFileCreation(fileName), false));
					bufferedWriter.write(String.valueOf(seqNum));
					bufferedWriter.close();
					totalInsertedCounter += insertCounter;
					logger.info("Seq# " + seqNum +  " updateCounter " + insertCounter	+ " totalUpdatedCounter " + totalInsertedCounter	+ " Remaining Msgs : " + MessageDatabaseQueue.tipmarketQ.size());
					insertCounter = 0;
					lastUpdatedTime = currentUpdatedTime;
				}
				baseObj = null;
			}
		} catch (Exception ex) {
			logger.error("Exception in " + threadname + " thread:" + ex, ex);
			ex.printStackTrace();
		} finally {
			try {
				if (insertCounter > 0) {
					dbUtilObj.commitTrans(false);				
					bufferedWriter = new BufferedWriter(new FileWriter(Utility.logFileCreation(fileName), false));
					bufferedWriter.write(String.valueOf(seqNum));
					bufferedWriter.close();
					totalInsertedCounter += insertCounter;
					logger.info("Seq# " + seqNum +" updateCounter " + insertCounter		+ " totalUpdatedCounter " + totalInsertedCounter		+ " Remaining Msgs : " + MessageDatabaseQueue.tipmarketQ.size()+ " Ending");
					insertCounter = 0;
				}
			} catch (Exception e) {
				logger.error("Exception in finally of " + threadname	+ " thread:" + e);
			} finally {
				if (dbUtilObj != null)
					dbUtilObj.closeConnection();
			}
		}
	}
}
