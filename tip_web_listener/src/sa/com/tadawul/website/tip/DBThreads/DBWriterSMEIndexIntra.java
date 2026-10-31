package sa.com.tadawul.website.tip.DBThreads;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import sa.com.tadawul.website.tip.log.Log;
import sa.com.tadawul.website.tip.log.LogFactory;
import sa.com.tadawul.website.tip.queue.MessageDatabaseQueue;
import sa.com.tadawul.website.tip.shared.DBManagerSMEIndexIntra;
import sa.com.tadawul.website.tip.shared.TipConstants;
import sa.com.tadawul.website.tip.shared.Utility;
import sa.com.tadawul.website.tip.vo.TIP_BaseVO;
import sa.com.tadawul.website.tip.vo.TIP_SMEIndexVO;

public class DBWriterSMEIndexIntra extends DBWriter {

	private final static Log logger = LogFactory.getLog(DBWriterSMEIndexIntra.class);
	private DBManagerSMEIndexIntra dbUtilObj;
	static long currentUpdatedTime = 0;
	static long  lastUpdatedTime = 0;
	
	public DBWriterSMEIndexIntra(String threadName) throws Exception {	
		super( threadName);
		dbUtilObj = new DBManagerSMEIndexIntra(threadName);
	}

	@Override
	public void run() {
		boolean ret=true;
		long insertCounter = 0;
		long totalInsertedCounter = 0;

		BufferedWriter bufferedWriter = null;
		BufferedReader in = null;
		String fileName = "DBWriterSMEIndexIntra_" + TipConstants.TARGIN_FEED_REQ_DATE+ ".txt";
		int startCount = 0;
		TIP_BaseVO baseObj = null;
		boolean isCommit;
		
		long seqNum = 0;
		
		try {
			File file = new File(Utility.logFileCreation(fileName));
			if (file.exists()) {
				in = new BufferedReader(new FileReader(Utility.logFileCreation(fileName)));
				startCount = Integer.parseInt(in.readLine());
				in.close();
			}

			while (true) {
				isCommit=false;
				if (MessageDatabaseQueue.tipSMEIndexIntraQ.size() > 0) {
					baseObj = (TIP_BaseVO) MessageDatabaseQueue.tipSMEIndexIntraQ.getFrame();

					if (baseObj.sequence_no <= startCount &&  !baseObj.isBOD 	&&  !baseObj.isEOD ) {						
						logger.debug("Ignoring Message with sequence_no: " + baseObj.sequence_no + " startCount: " + startCount + " isBOD: " + baseObj.isBOD+ " isEOD: " + baseObj.isEOD);						
						MessageDatabaseQueue.tipSMEIndexIntraQ.releaseFrame();
						baseObj = null;
						continue;
					}
					
					if (baseObj.sequence_no > seqNum) {
						seqNum = baseObj.sequence_no;
					}
					//sequence_no = baseObj.sequence_no;
					
					if (baseObj.isBOD) {
						isCommit=true;
						logger.info("BOD for SME Index Intra ");
						ret = dbUtilObj.initializeDBforSMEIndexIntra();

						try { // to give time for other threads to stop
							Thread.sleep(5000);
						} catch (Exception e) {
						}

						if (!ret) {
							logger.error("BOD Operations for SMEIndex Intra Data in DB Failed");
							TipConstants.shutdownFlag = true;
						}
						MessageDatabaseQueue.tipSMEIndexIntraQ.releaseFrame();
						continue;
					}
				
					if (baseObj.isEOD) {
						isCommit=true;
					}
					
					try
					{
					ret = dbUtilObj.insertSMEIndexIntraData((TIP_SMEIndexVO) baseObj,isCommit);
				
					}catch(Exception e)
					{
						logger.error("Exception in " + threadname + " thread:" + e,e);
					}
					if (!ret) {
						logger.error("Insert SMEIndex Intra Data to DB Failed: Seq# "+ baseObj.sequence_no+ " uncommitted msgs " + insertCounter);

					}
					MessageDatabaseQueue.tipSMEIndexIntraQ.releaseFrame();
					insertCounter++;

				} else {
					currentUpdatedTime =  System.currentTimeMillis();
					if (insertCounter > 0 && (currentUpdatedTime-lastUpdatedTime)>TipConstants.WRITER_THREADS_DB_UPDATE_INTERVAL_NODATA_RECEIVED) {
						dbUtilObj.commitTrans(true);	
						lastUpdatedTime = currentUpdatedTime;
						insertCounter=0;
					}
					if (TipConstants.shutdownFlag) {
						logger.info("Shutdown received in Thread:DBWriter." + threadname);
						//GUIHelper.SMEindexIntraConnection = TipConstants.CONN_STATUS_STOPPED;
						
						break;
					}
					Thread.sleep(100);
				}				
				currentUpdatedTime =  System.currentTimeMillis();
				if (insertCounter > 0 &&  (currentUpdatedTime-lastUpdatedTime)>TipConstants.WRITER_THREADS_DB_FILE_UPDATE_INTERVAL) {
					dbUtilObj.commitTrans(true);				
					bufferedWriter = new BufferedWriter(new FileWriter(Utility.logFileCreation(fileName), false));
					bufferedWriter.write(String.valueOf(seqNum));
					bufferedWriter.close();

					totalInsertedCounter += insertCounter;	
					logger.info("Seq# " + seqNum + "  insertCounter " + insertCounter+ " totalInsertedCounter " + totalInsertedCounter+ 	" Remaining Msgs : " + MessageDatabaseQueue.tipSMEIndexIntraQ.size());
					insertCounter = 0;		
					lastUpdatedTime = currentUpdatedTime;
				}
				baseObj = null;
			}
		} catch (InterruptedException ex) {
			logger.error("InterruptedException in " + threadname + " thread:"+ ex);
			ex.printStackTrace();
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
					logger.info("Seq# " + seqNum + " insertCounter " + insertCounter + " totalInsertedCounter " + totalInsertedCounter + " Remaining Msgs : " + MessageDatabaseQueue.tipSMEIndexIntraQ.size()+ " Ending");
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
