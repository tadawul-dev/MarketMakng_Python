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
import sa.com.tadawul.website.tip.shared.DBManagerIndex;
import sa.com.tadawul.website.tip.shared.TipConstants;
import sa.com.tadawul.website.tip.shared.Utility;
import sa.com.tadawul.website.tip.vo.TIP_BaseVO;
import sa.com.tadawul.website.tip.vo.TIP_IndexVO;

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

public class DBUpdateIndex extends DBUpdateThread {

	private final static Log logger = LogFactory.getLog(DBUpdateIndex.class);
	
	DBManagerIndex dbUtilObj;
	String threadName;
	static long currentUpdatedTime = 0;
	static long  lastUpdatedTime = 0;
	
	public DBUpdateIndex( String threadName) 	throws Exception {	
		super( threadName);
		this.threadName=threadName;
		dbUtilObj= new DBManagerIndex(this.threadName);
		
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
		String fileName = "DBUpdateIndex_" + TipConstants.TARGIN_FEED_REQ_DATE	+ ".txt";
		int startCount = 0;
		TIP_BaseVO baseObj = null;
		boolean isCommit;


		try {
			File file = new File(Utility.logFileCreation(fileName));
			if (file.exists()) {
				in = new BufferedReader(new FileReader(Utility.logFileCreation(fileName)));
				startCount = Integer.parseInt(in.readLine());
				in.close();
			//	logger.info("******** DBUpdateIndex_ ************ :"+startCount);
			}

			while (true) {
				isCommit=false;
				// logger.info("Thread Name : DBUpdateIndex,  buffer  size:"+g_oBuffer.tipindexQ.size() );
				
				
				if (MessageDatabaseQueue.tipindexQ.size() > 0) {
					baseObj = (TIP_BaseVO) MessageDatabaseQueue.tipindexQ.getFrame();
					// logger.info("Thread Name : DBUpdateIndex,  buffer  size:"+MessageDatabaseQueue.tipindexQ.size());			
					if (baseObj.sequence_no <= startCount && !baseObj.isBOD		&& !baseObj.isEOD) {
						logger.info("Ignoring Message with sequence_no: "+ baseObj.sequence_no + " startCount: "+ startCount + " isBOD: " + baseObj.isBOD	+ " isEOD: " + baseObj.isEOD);
						MessageDatabaseQueue.tipindexQ.releaseFrame();
						baseObj = null;
						continue;
					}
					if (baseObj.sequence_no > seqNum) {
						seqNum = baseObj.sequence_no;						
					}
					
				//	if (baseObj.isBOD) {
						if (baseObj.isBODOpen) {
						logger.info("BOD for Index");
						isCommit=true;
						ret = dbUtilObj.initializeDBforIndex();
						
						if (!ret) {
							logger.error("BOD Operations for Index Data in DB Failed");
							TipConstants.shutdownFlag = true;
						}
					}
					if (baseObj.isEOD) {
						isCommit=true;
						asyncShareIndexEOD();

					}
					try
					{
						ret = dbUtilObj.updateIndexData((TIP_IndexVO) baseObj,isCommit);
						}catch(Exception e)
						{
							logger.error("Exception in " + threadname + " thread:" + e,e);
						}
					if (!ret) {
						logger.error("Update/Insert Index Data to DB Failed");
					}

					MessageDatabaseQueue.tipindexQ.releaseFrame();
					insertCounter++;					
				
				} else {
					currentUpdatedTime =  System.currentTimeMillis();
					if ( (currentUpdatedTime-lastUpdatedTime)>TipConstants.UPDATER_THREADS_DB_UPDATE_INTERVAL_NODATA_RECEIVED) {
						dbUtilObj.commitTrans(true);	
						lastUpdatedTime = currentUpdatedTime;
						insertCounter=0;
					}
					if (TipConstants.shutdownFlag) {
						logger.info("Shutdown received in Thread:DBUpdateThread."+ this.threadname);					
						break;
					}
					Thread.sleep(100);
				}
				
				currentUpdatedTime =  System.currentTimeMillis();
				if (insertCounter > 0 && (currentUpdatedTime-lastUpdatedTime)>TipConstants.UPDATER_THREADS_DB_FILE_UPDATE_INTERVAL) {
					dbUtilObj.commitTrans(true);	
							bufferedWriter = new BufferedWriter(new FileWriter(	Utility.logFileCreation(fileName), false));
							bufferedWriter.write(String.valueOf(seqNum));
							bufferedWriter.close();		
					totalInsertedCounter += insertCounter;
					logger.info("Seq# " + seqNum + " updateCounter " + insertCounter	+ " totalUpdatedCounter " + totalInsertedCounter	+ " Remaining Msgs : " + MessageDatabaseQueue.tipindexQ.size());
					insertCounter = 0;
					lastUpdatedTime = currentUpdatedTime;
				}
				baseObj = null;
			}
		} catch (Exception ex) {
			logger.error("Exception in " + this.threadname + " thread:" + ex, ex);
			ex.printStackTrace();
		} finally {
			try {
				if (insertCounter > 0) {
					dbUtilObj.commitTrans(false);					
						bufferedWriter = new BufferedWriter(new FileWriter(	Utility.logFileCreation(fileName), false));
						bufferedWriter.write(String.valueOf(seqNum));
						bufferedWriter.close();				
					totalInsertedCounter += insertCounter;
					logger.info("Seq# " + seqNum +" updateCounter " + insertCounter	+ " totalUpdatedCounter " + totalInsertedCounter		+ " Remaining Msgs : " + MessageDatabaseQueue.tipindexQ.size()	+ " Ending");
					insertCounter = 0;
				}
			} catch (Exception e) {
				logger.error("Exception in finally of " + this.threadname	+ " thread:" + e);
			} finally {
				if (dbUtilObj != null)
					dbUtilObj.closeConnection();
			}
		}
	}
	
	public void ShareIndexEOD()
	{
		boolean ret=true;
		boolean isCommit;
		 while (true)
		 {
			// if(MessageDatabaseQueue.tipindexQ.size() > 0 || MessageDatabaseQueue.tipshareQ.size() > 0)
			if(! DBQueueUpdater.isDBQueueAllEmpty())
			 {
				 logger.info("EOD for Share Index : Pending DB Queue Messages in  tipindexQ remaining messages :"+MessageDatabaseQueue.tipindexQ.size()+" Hence Wait for EOD Operation");
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
				    isCommit=true;
				    dbUtilObj.commitTrans(isCommit);				    
				    
				    logger.info("EOD for Share Index. No messages are pending the queue. Hence process for EOD for Share Index");
					ret = dbUtilObj.EODforIndex();
					dbUtilObj.EODforIndex_Adj();
					if (!ret) {
						logger.error("EOD Operations for Index Data in DB Failed");
						TipConstants.shutdownFlag = true;
					}	
				break; 
			 }
		 }	
	}
	
	public void asyncShareIndexEOD(){ 
	    Runnable task = new Runnable() {

	        @Override 
	        public void run() { 
	            try { 
	            	ShareIndexEOD();
	            } catch (Exception ex) { 
	            	logger.error("Exception in " + threadname + " thread:" + ex, ex);
	            } 
	        } 
	    }; 
	    new Thread(task, "asyncShareIndexEOD").start(); 
	}
	
}
