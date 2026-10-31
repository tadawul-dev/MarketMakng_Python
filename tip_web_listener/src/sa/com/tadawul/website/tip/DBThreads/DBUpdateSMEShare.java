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
import java.util.Calendar;
import java.util.TimeZone;

import sa.com.tadawul.website.tip.log.Log;
import sa.com.tadawul.website.tip.log.LogFactory;
import sa.com.tadawul.website.tip.queue.MessageDatabaseQueue;
import sa.com.tadawul.website.tip.shared.DBManagerSMEShare;
import sa.com.tadawul.website.tip.shared.TIPDataObjects;
import sa.com.tadawul.website.tip.shared.TipConstants;
import sa.com.tadawul.website.tip.shared.Utility;
import sa.com.tadawul.website.tip.vo.TIP_BaseVO;
import sa.com.tadawul.website.tip.vo.TIP_SMEShareVO;
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

public class DBUpdateSMEShare extends DBUpdateThread {

	private final static Log logger = LogFactory.getLog(DBUpdateSMEShare.class);
	private DBManagerSMEShare dbUtilObj ;
	static long currentUpdatedTime = 0;
	static long  lastUpdatedTime = 0;
	
	public DBUpdateSMEShare( String threadName)	throws Exception {		
		super( threadName);
		dbUtilObj = new DBManagerSMEShare(threadName);
	}

	/**
	 * This method is called on starting the DBUpdateThread
	 *
	 */
	public void run() {
		boolean ret;
		long insertCounter = 0;
		long totalInsertedCounter = 0;
		BufferedWriter bufferedWriter = null;
		BufferedReader in = null;
		String fileName = "DBUpdateSMEShare_" + TipConstants.TARGIN_FEED_REQ_DATE+ ".txt";
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
				isCommit=false;
				// logger.info("Thread Name :DBUpdateSMEShare_ buffer  :"+g_oBuffer.tipSMEshareQ.size() );
				if (MessageDatabaseQueue.tipSMEshareQ.size() > 0) {
					baseObj = (TIP_BaseVO) MessageDatabaseQueue.tipSMEshareQ.getFrame();
					//logger.info("Thread Name :DBUpdateSMEShare_ buffer  :"+MessageDatabaseQueue.tipSMEshareQ.size() );					
					if (baseObj.sequence_no <= startCount && !baseObj.isBOD	&& !baseObj.isEOD) {
						logger.debug("Ignoring Message with sequence_no: "	+ baseObj.sequence_no + " startCount: "	+ startCount + " isBOD: " + baseObj.isBOD+ " isEOD: " + baseObj.isEOD);
						MessageDatabaseQueue.tipSMEshareQ.releaseFrame();
						baseObj = null;
						continue;
					}
					if (baseObj.sequence_no > seqNum) {
						seqNum = baseObj.sequence_no;					
					}
					
					//Changes for SR2. Extented NegDeals
					// Allow initialization only if the time is 9:30 AM or later, preventing pre-open status before this time
					Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
					int hour = calendar.get(Calendar.HOUR_OF_DAY);
					int minute = calendar.get(Calendar.MINUTE);

					boolean shouldInitialize = (hour == 9 && minute >= 30);

              	   //Added for SME Share 
                   // if (baseObj.isBOD) {
					 if (baseObj.isBODPreOpen && shouldInitialize) {
                    	isCommit=true;
                       logger.info("BOD for SME Share");
                       
                       ret = dbUtilObj.initializeDBforSMEShare();
                       if (!ret) {
                           logger.error("BOD Operations for SME Share Data in DB Failed");
                           TipConstants.shutdownFlag = true;
                       }
                       //Added for Share RI
                       logger.info("BOD for SME ShareRI");
                       ret = dbUtilObj.initializeDBforSMEShareRI();
                       if (!ret) {
                           logger.error("BOD Operations for SMEShareRI Data in DB Failed");
                           TipConstants.shutdownFlag = true;
                       }
                       
                   
                   }
                   if (baseObj.isEOD) {
                	   isCommit=true;
                	   asyncSMEShareEOD();
 
                   }
                   try
                   {
                  	 
                //logger.info("Updating SMEShareData    1::::"+((TIP_SMEShareVO) baseObj).fk_te_symbol);                              
                   boolean isShareRI=((TIP_SMEShareVO)baseObj).isShareRI;                           

                   if(!isShareRI)
                   {
                	 //Normal TIP Updates
	                	if(!TipConstants.IS_NEGDEALS_EARLY_START)
						{
	                		 logger.debug("Updating SMEShareData::::"+((TIP_SMEShareVO) baseObj).fk_te_symbol);
	                           	ret = dbUtilObj.updateSMEShareData((TIP_SMEShareVO) baseObj); 
						}
					
	                	//Incase for the NegDeals Early start
	                	if(TipConstants.IS_NEGDEALS_EARLY_START &&	TIPDataObjects.isAfterPreOpen  && Utility.shouldUpdatedShares())
		                    {
	                		 logger.debug("Updating SMEShareData::::"+((TIP_SMEShareVO) baseObj).fk_te_symbol);
	                           	ret = dbUtilObj.updateSMEShareData((TIP_SMEShareVO) baseObj); 
		                    }
                	   
                	   
                  	  
                   	
                   }
                   //Added for Share RI
                   else
                   {
                   	 //logger.info("updateShareRIData Called ");
                   	 ret = dbUtilObj.updateSMEShareRIData((TIP_SMEShareVO) baseObj);
                   	
                   }
               	

                   dbUtilObj.updateBestBidOffer((TIP_SMEShareVO) baseObj);
                  
                   	//logger.info("updateShareData isEndOfBatch Called :"+baseObj.isEndOfBatch);
                   	ret = dbUtilObj.SMEShareUpdate_batchExecute(isCommit);                            	
   				
                   
                   if (!ret) {
                       logger.error(" Update/Insert SMEShare Data to DB Failed");
                   }
                   }catch(Exception e)
                   {
                   	logger.error("***********Exception occured in DB Update Thread *************"+e.getMessage());
                   	e.printStackTrace();
                   }
              

                   MessageDatabaseQueue.tipSMEshareQ.releaseFrame();
					insertCounter++;					
				} else {
					
					currentUpdatedTime =  System.currentTimeMillis();
					if ( (currentUpdatedTime-lastUpdatedTime)>TipConstants.UPDATER_THREADS_DB_UPDATE_INTERVAL_NODATA_RECEIVED) {
						dbUtilObj.commitTrans(true);	
						lastUpdatedTime = currentUpdatedTime;
						insertCounter=0;
					}
					
					if (TipConstants.shutdownFlag) {
						logger.info("Shutdown received in Thread:DBUpdateThread." + threadname);
					//	GUIHelper.SMEshareConnection = TipConstants.CONN_STATUS_STOPPED;
					
						break;
					}
					Thread.sleep(100);
				}

				currentUpdatedTime =  System.currentTimeMillis();
				if ( insertCounter > 0 && (currentUpdatedTime-lastUpdatedTime)>TipConstants.UPDATER_THREADS_DB_FILE_UPDATE_INTERVAL) {
					dbUtilObj.commitTrans(true);					
					bufferedWriter = new BufferedWriter(new FileWriter(	Utility.logFileCreation(fileName), false));
					bufferedWriter.write(String.valueOf(seqNum));
					bufferedWriter.close();
					totalInsertedCounter += insertCounter;
					logger.info("Seq# " + seqNum + " updateCounter " + insertCounter	+ " totalUpdatedCounter " + totalInsertedCounter + " Remaining Msgs : " + MessageDatabaseQueue.tipSMEshareQ.size());
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
					bufferedWriter = new BufferedWriter(new FileWriter(	Utility.logFileCreation(fileName), false));
					bufferedWriter.write(String.valueOf(seqNum));
					bufferedWriter.close();

					totalInsertedCounter += insertCounter;

					logger.info("Seq# " + seqNum + " updateCounter " + insertCounter+ " totalUpdatedCounter " + totalInsertedCounter	+ " Remaining Msgs : " + MessageDatabaseQueue.tipSMEshareQ.size()+ " Ending");
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
	
	public void SMEShareEOD()
	{
		boolean ret=true;
		boolean isCommit;
		  while (true)
			 {
			//	 if(MessageDatabaseQueue.tipSMEshareQ.size() > 0)
		    if(! DBQueueUpdater.isDBQueueAllEmpty())
				 {
					 logger.info("EOD for SME Share : Pending DB Queue Messages in  tipSMEshareQ remaining messages :"+MessageDatabaseQueue.tipSMEshareQ.size()+" Quence Wait for EOD Operation");
					 try {
						Thread.sleep(TipConstants.SLEEPWAIT_FOR_EOD_BEFORE_PROCESSING);
					} catch (InterruptedException e) {
						logger.error("Exception in " + threadname + " thread:" + e, e);
					}
					 
					 dbUtilObj.SMEShareUpdate_batchExecute(true);  
					 
				 }else
				 {
					//This is needed to makesure all the databaseside are completed. 
					 try {
							Thread.sleep(TipConstants.SLEEPWAIT_FOR_EOD_BEFORE_PROCESSING_ACTUAL);
						} catch (InterruptedException e) {
							logger.error("Exception in " + threadname + " thread:" + e, e);
						}
					 isCommit =  true;
					 dbUtilObj.SMEShareUpdate_batchExecute(isCommit);  
						logger.info("EOD for SME Share. No messages are pending the queue. Hence process for EOD for Share");
						 ret = dbUtilObj.EODforSMEShare();
                    	  dbUtilObj.EODforSMEShare_Adj();
                    if (!ret) {
                        logger.error("EOD Operations for SME Share Data in DB Failed");
                        TipConstants.shutdownFlag = true;
                    }
                   // else
                    //Added for Share RI
                    logger.info("EOD for SME ShareRI");
                    ret = dbUtilObj.EODforSMEShareRI();
                    if (!ret) {
                        logger.error("EOD Operations for SMEShare RI Data in DB Failed");
                        TipConstants.shutdownFlag = true;
                    }
					break; 
				
				 }
			 }		
		 
	}
	
	public void asyncSMEShareEOD(){ 
	    Runnable task = new Runnable() {

	        @Override 
	        public void run() { 
	            try { 
	            	SMEShareEOD();
	            } catch (Exception ex) { 
	            	logger.error("Exception in " + threadname + " thread:" + ex, ex);
	            } 
	        } 
	    }; 
	    new Thread(task, "asyncSMEShareEOD").start(); 
	}
	
}
