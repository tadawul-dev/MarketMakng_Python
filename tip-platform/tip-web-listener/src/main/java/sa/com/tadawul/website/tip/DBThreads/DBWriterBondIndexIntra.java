package sa.com.tadawul.website.tip.DBThreads;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import sa.com.tadawul.website.tip.queue.MessageDatabaseQueue;
import sa.com.tadawul.website.tip.shared.DBManagerBondIndexIntra;
import sa.com.tadawul.website.tip.shared.TipConstants;
import sa.com.tadawul.website.tip.shared.Utility;
import sa.com.tadawul.website.tip.vo.TIP_BaseVO;
import sa.com.tadawul.website.tip.vo.TIP_BondIndexVO;

public class DBWriterBondIndexIntra extends DBWriter {

	private static final Logger logger = LogManager.getLogger(DBWriterBondIndexIntra.class);

	DBManagerBondIndexIntra dbUtilObj;
	static long currentUpdatedTime = 0;
	static long  lastUpdatedTime = 0;
	
	public DBWriterBondIndexIntra(String threadName) throws Exception {	
		super( threadName);
		dbUtilObj = new DBManagerBondIndexIntra(threadName);
	}

	@Override
	public void run() {
		boolean ret=true;
		long insertCounter = 0;
		long totalInsertedCounter = 0;

		BufferedWriter bufferedWriter = null;
		BufferedReader in = null;
		String fileName = "DBWriterBondIndexIntra_" + TipConstants.TARGIN_FEED_REQ_DATE+ ".txt";
		int startCount = 0;
		TIP_BaseVO baseObj = null;
		long seqNum = 0;
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
				if (MessageDatabaseQueue.tipBondIndexIntraQ.size() > 0) {
					baseObj = (TIP_BaseVO) MessageDatabaseQueue.tipBondIndexIntraQ.getFrame();

					if (baseObj.sequence_no <= startCount &&  !baseObj.isBOD 	&&  !baseObj.isEOD) {						
						if (logger.isDebugEnabled()) logger.debug("Ignoring Message with sequence_no: {} startCount: {} isBOD: {} isEOD: {}", baseObj.sequence_no, startCount, baseObj.isBOD, baseObj.isEOD);
						MessageDatabaseQueue.tipBondIndexIntraQ.releaseFrame();
						baseObj = null;
						continue;
					}
					
					if (baseObj.sequence_no > seqNum) {
						seqNum = baseObj.sequence_no;
					}
									
					if (baseObj.isBOD) {
						isCommit=true;
						logger.info("BOD for Index Intra ");
						ret = dbUtilObj.initializeDBforBondIndexIntra();
						if (!ret) {
							logger.error("BOD Operations for Bond Index Intra Data in DB Failed");
							TipConstants.shutdownFlag = true;
						}
						MessageDatabaseQueue.tipBondIndexIntraQ.releaseFrame();
						continue;
						
					}
					if (baseObj.isEOD) {
						isCommit=true;
					}
					try
					{
					ret = dbUtilObj.insertBondIndexIntraData((TIP_BondIndexVO) baseObj,isCommit);
					}catch(Exception e)
					{
						logger.error("Exception in " + threadname + " thread:" + e,e);
					}
					if (!ret) {
						logger.error("Insert Bond Index Intra Data to DB Failed: Seq# "+ baseObj.sequence_no+ " uncommitted msgs " + insertCounter);
					}
					MessageDatabaseQueue.tipBondIndexIntraQ.releaseFrame();
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
						//GUIHelper.bondIndexIntraConnection = TipConstants.CONN_STATUS_STOPPED;
						
						break;
					}
					MessageDatabaseQueue.tipBondIndexIntraQ.awaitFrame(TipConstants.DB_QUEUE_IDLE_WAIT_MS); // was: Thread.sleep(100) polling
				}
				
				currentUpdatedTime =  System.currentTimeMillis();
				if (insertCounter > 0	&&  (currentUpdatedTime-lastUpdatedTime)>TipConstants.WRITER_THREADS_DB_FILE_UPDATE_INTERVAL) {
					dbUtilObj.commitTrans(true);					
					bufferedWriter = new BufferedWriter(new FileWriter(Utility.logFileCreation(fileName), false));
					bufferedWriter.write(String.valueOf(seqNum));
					bufferedWriter.close();
					totalInsertedCounter += insertCounter;	

					logger.info("Seq# {} insertCounter {} totalInsertedCounter {} Remaining Msgs : {}", seqNum, insertCounter, totalInsertedCounter, MessageDatabaseQueue.tipBondIndexIntraQ.size());
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
					bufferedWriter = new BufferedWriter(new FileWriter(	Utility.logFileCreation(fileName), false));
					bufferedWriter.write(String.valueOf(seqNum));
					bufferedWriter.close();

					totalInsertedCounter += insertCounter;				

					logger.info("Seq# {} insertCounter {} totalInsertedCounter {} Remaining Msgs : {} Ending", seqNum, insertCounter, totalInsertedCounter, MessageDatabaseQueue.tipBondIndexIntraQ.size());
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
