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

import sa.com.tadawul.website.tip.shared.TipThreads;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import sa.com.tadawul.website.tip.queue.MessageDatabaseQueue;
import sa.com.tadawul.website.tip.shared.DBManagerBond;
import sa.com.tadawul.website.tip.shared.TipConstants;
import sa.com.tadawul.website.tip.shared.Utility;
import sa.com.tadawul.website.tip.vo.TIP_BaseVO;
import sa.com.tadawul.website.tip.vo.TIP_BondVO;
import sa.com.tadawul.website.tip.vo.TIP_ShareVO;


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

public class DBUpdateBond extends DBUpdateThread {

	private static final Logger logger = LogManager.getLogger(DBUpdateBond.class);
	DBManagerBond dbUtilObj;
	static long currentUpdatedTime = 0;
	static long  lastUpdatedTime = 0;
	 
	public DBUpdateBond(String threadName) throws Exception {	
		super( threadName);
		dbUtilObj = new DBManagerBond(threadName);
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
		String fileName = "DBUpdateBond_" + TipConstants.TARGIN_FEED_REQ_DATE+ ".txt";
		int startCount = 0;
		TIP_BaseVO baseObj = null;
		boolean isCommit;

		try {
			File file = new File(Utility.logFileCreation(fileName));
			if (file.exists()) {
				in = new BufferedReader(new FileReader(Utility.logFileCreation(fileName)));
				startCount = Integer.parseInt(in.readLine());
				in.close();
			}

			while (true) {
				isCommit = false;
				// logger.info("Thread Name :DBUpdateBond, buffer  :{}", MessageDatabaseQueue.tipbondQ.size());
				if (MessageDatabaseQueue.tipbondQ.size() > 0) {
					baseObj = (TIP_BaseVO) MessageDatabaseQueue.tipbondQ.getFrame();
					 //logger.info("Thread Name :DBUpdateBond buffer  :{}", MessageDatabaseQueue.tipbondQ.size());
					if (baseObj.sequence_no <= startCount && !baseObj.isBOD 	&& !baseObj.isEOD) {
						if (logger.isDebugEnabled()) logger.debug("Ignoring Message with sequence_no: {} startCount: {} isBOD: {} isEOD: {}", baseObj.sequence_no, startCount, baseObj.isBOD, baseObj.isEOD);
						MessageDatabaseQueue.tipbondQ.releaseFrame();
						baseObj = null;
						continue;
					}
					if (baseObj.sequence_no > seqNum) {
						seqNum = baseObj.sequence_no;	
					}			

				//	if (baseObj.isBOD) {
					if (baseObj.isBODPreOpen) {
						isCommit =  true;
						logger.info("BOD for Bond");
						ret = dbUtilObj.initializeDBforBond();
						logger.info("BOD for OTC Bond");
						ret = dbUtilObj.initializeDBforOTCBond();
						if (!ret) {
							logger.error("BOD Operations for Bond Data in DB Failed");
							TipConstants.shutdownFlag = true;
						}

					}
					if (baseObj.isEOD) {
						isCommit=true;
						asyncBondEOD();
					}
						try
						{
						   ret = dbUtilObj.updateBondData((TIP_BondVO) baseObj,isCommit);
						   dbUtilObj.updateBestBidOffer((TIP_BondVO) baseObj);
						}catch(Exception e)
						{
							logger.error("Exception in " + threadname + " thread:" + e,e);
						}
					if (!ret) {
						logger.error("Update/Insert Bond Data to DB Failed");
					}
					MessageDatabaseQueue.tipbondQ.releaseFrame();
					insertCounter++;					
				} else {
					currentUpdatedTime =  System.currentTimeMillis();
					if ( (currentUpdatedTime-lastUpdatedTime)>TipConstants.UPDATER_THREADS_DB_UPDATE_INTERVAL_NODATA_RECEIVED) {
						dbUtilObj.commitTrans(true);	
						lastUpdatedTime = currentUpdatedTime;
						insertCounter=0;
					}
					if (TipConstants.shutdownFlag) {
						logger.info("Shutdown received in Thread:DBUpdateThread.{}", threadname);
						break;
					}
					MessageDatabaseQueue.tipbondQ.awaitFrame(TipConstants.DB_QUEUE_IDLE_WAIT_MS); // was: Thread.sleep(100) polling
				}

				currentUpdatedTime =  System.currentTimeMillis();
				if (insertCounter > 0 && (currentUpdatedTime-lastUpdatedTime)>TipConstants.UPDATER_THREADS_DB_FILE_UPDATE_INTERVAL) {
					dbUtilObj.commitTrans(true);					
					bufferedWriter = new BufferedWriter(new FileWriter(Utility.logFileCreation(fileName), false));
					bufferedWriter.write(String.valueOf(seqNum));
					bufferedWriter.close();
					totalInsertedCounter += insertCounter;
					logger.info("Seq# {} updateCounter {} totalUpdatedCounter {} Remaining Msgs : {}", seqNum, insertCounter, totalInsertedCounter, MessageDatabaseQueue.tipbondQ.size());
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
					logger.info("Seq# {} updateCounter {} totalUpdatedCounter {} Remaining Msgs : {} Ending", seqNum, insertCounter, totalInsertedCounter, MessageDatabaseQueue.tipbondQ.size());
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
	
	public void BondEOD()
	{
		boolean ret=true;
		boolean isCommit;
		 while (true)
		 {
			// if(MessageDatabaseQueue.tipbondQ.size() > 0)
	      if(! DBQueueUpdater.isDBQueueAllEmpty())
			 {
				 logger.info("EOD for Bond : Pending DB Queue Messages in  tipbondQ, Quence Wait for Terminate");
				 try {
					Thread.sleep(TipConstants.SLEEPWAIT_FOR_EOD_BEFORE_PROCESSING);
				} catch (InterruptedException e) {
					logger.error("Exception in " + threadname + " thread:" + e, e);
				
				}
				 
				 dbUtilObj.commitTrans(true);
				 
			 }else
			 {
				//This is needed to makesure all the databaseside are completed. 
				 try {
						Thread.sleep(TipConstants.SLEEPWAIT_FOR_EOD_BEFORE_PROCESSING_ACTUAL);
					} catch (InterruptedException e) {
						logger.error("Exception in " + threadname + " thread:" + e, e);
					}
				 
				 	isCommit =  true;
				 	dbUtilObj.commitTrans(isCommit);	
					logger.info("EOD for Bond . No messages are pending the queue. Hence process for EOD for Bond ");
					ret = dbUtilObj.EODforBond();
					if (!ret) {
						logger.error("EOD Operations for Bond Data in DB Failed");
						TipConstants.shutdownFlag = true;
					}
				break; 
			
			 }
		 }
	}
	
	public void asyncBondEOD(){ 
	    Runnable task = new Runnable() {

	        @Override 
	        public void run() { 
	            try { 
	            	BondEOD();
	            } catch (Exception ex) { 
	            	logger.error("Exception in " + threadname + " thread:" + ex, ex);
	                //handle error which cannot be thrown back 
	            } 
	        } 
	    }; 
	    TipThreads.startVirtual("asyncBondEOD", task); 
	}
	
}



