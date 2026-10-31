package sa.com.tadawul.website.tip.DBThreads;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import sa.com.tadawul.website.tip.queue.MessageDatabaseQueue;
import sa.com.tadawul.website.tip.shared.DBManagerBondIntra;
import sa.com.tadawul.website.tip.shared.TipConstants;
import sa.com.tadawul.website.tip.shared.Utility;
import sa.com.tadawul.website.tip.vo.TIP_BaseVO;
import sa.com.tadawul.website.tip.vo.TIP_BondVO;

public class DBWriterBondSpecialIntra extends DBWriter {

	private static final Logger logger = LogManager.getLogger(DBWriterBondSpecialIntra.class);
	private DBManagerBondIntra dbUtilObj;
	static long currentUpdatedTime = 0;
	static long  lastUpdatedTime = 0;
	
	public DBWriterBondSpecialIntra(String ThreadName) throws Exception {	
		super( ThreadName);
		this.threadname=ThreadName;
		dbUtilObj = new DBManagerBondIntra(this.threadname);
	}

	@Override
	public void run() {
		boolean ret=true;
		long insertCounter = 0;
		long totalInsertedCounter = 0;		

		BufferedWriter bufferedWriter = null;
		BufferedReader in = null;
		String fileName = "DBWriterBondSpecialIntra_" + TipConstants.TARGIN_FEED_REQ_DATE+ ".txt";
		int startCount = 0;
		TIP_BaseVO baseObj = null;				
		long seqNum = 0;
		
		
		try {
			File file = new File(Utility.logFileCreation(fileName));
			if (file.exists()) {
				in = new BufferedReader(new FileReader(Utility.logFileCreation(fileName)));
				startCount = Integer.parseInt(in.readLine());
				in.close();
			}

			while (true) {				
				if (MessageDatabaseQueue.tipBondSpecialIntraQ.size() > 0) {
					baseObj = (TIP_BaseVO) MessageDatabaseQueue.tipBondSpecialIntraQ.getFrame();
					if (baseObj.sequence_no <= startCount &&  !baseObj.isBOD 	&&  !baseObj.isEOD ) {						
						if (logger.isDebugEnabled()) logger.debug("Ignoring Message with sequence_no: {} startCount: {} isBOD: {} isEOD: {}", baseObj.sequence_no, startCount, baseObj.isBOD, baseObj.isEOD);
						MessageDatabaseQueue.tipBondSpecialIntraQ.releaseFrame();
						baseObj = null;
						continue;
					}
					
					if (baseObj.sequence_no > seqNum) {
						seqNum = baseObj.sequence_no;
					}
					
					try {						
									 logger.info("insertBondspecial Trade Called :{}", ((TIP_BondVO) baseObj).fk_fi_symbol);
									ret = dbUtilObj.insertSpecialFIShareIntraData((TIP_BondVO) baseObj,true);
								
								
						if (!ret) {
							logger.error("Insert Special Bond Intra Data to DB Failed: Seq# "	+ baseObj.sequence_no	+ " uncommitted msgs " + insertCounter);
						}
					} catch (Exception e) {
						logger.error("************Exception Occured in DBWriter  :"	+ e.getMessage());
					}				

					MessageDatabaseQueue.tipBondSpecialIntraQ.releaseFrame();
					insertCounter++;

				} else {
					currentUpdatedTime =  System.currentTimeMillis();
					if (insertCounter > 0 && (currentUpdatedTime-lastUpdatedTime)>TipConstants.WRITER_THREADS_DB_UPDATE_INTERVAL_NODATA_RECEIVED) {
						dbUtilObj.commitTrans(true);	
						lastUpdatedTime = currentUpdatedTime;
						insertCounter=0;
					}
					if (TipConstants.shutdownFlag) {
						logger.info("Shutdown received in Thread:DBWriter.{}", threadname);
					//	GUIHelper.shareIntraConnection = TipConstants.CONN_STATUS_STOPPED;
						break;
					}
					MessageDatabaseQueue.tipBondSpecialIntraQ.awaitFrame(TipConstants.DB_QUEUE_IDLE_WAIT_MS); // was: Thread.sleep(100) polling
				}			
				currentUpdatedTime =  System.currentTimeMillis();
				if (insertCounter > 0	&&  (currentUpdatedTime-lastUpdatedTime)>TipConstants.WRITER_THREADS_DB_FILE_UPDATE_INTERVAL) {
					dbUtilObj.commitTrans(true);
					bufferedWriter = new BufferedWriter(new FileWriter(Utility.logFileCreation(fileName), false));
					bufferedWriter.write(String.valueOf(seqNum));
					bufferedWriter.close();

					totalInsertedCounter += insertCounter;					
					logger.info("Seq# {} insertCounter {} totalInsertedCounter {} Remaining Msgs : {}", seqNum, insertCounter, totalInsertedCounter, MessageDatabaseQueue.tipBondSpecialIntraQ.size());
					insertCounter = 0;		
					lastUpdatedTime = currentUpdatedTime;
				}
				baseObj = null;
			}
		} catch (Exception ex1) {
			logger.error("Exception in " + threadname + " thread:" + ex1, ex1);
			ex1.printStackTrace();
		} finally {
			try {
				if (insertCounter > 0) {
					dbUtilObj.commitTrans(false);					
					bufferedWriter = new BufferedWriter(new FileWriter(Utility.logFileCreation(fileName), false));
					bufferedWriter.write(String.valueOf(seqNum));
					bufferedWriter.close();
					totalInsertedCounter += insertCounter;					
					logger.info("Seq# {} insertCounter {} totalInsertedCounter {} Remaining Msgs : {} Ending", seqNum, insertCounter, totalInsertedCounter, MessageDatabaseQueue.tipBondSpecialIntraQ.size());
				}
			} catch (Exception e) {
				logger.error("Exception in finally of " + threadname+ " thread:" + e);
			} finally {
				if (dbUtilObj != null)
					dbUtilObj.closeConnection();
			}
		}
	}

}
