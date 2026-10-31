package sa.com.tadawul.website.tip.DBThreads;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import sa.com.tadawul.website.tip.log.Log;
import sa.com.tadawul.website.tip.log.LogFactory;
import sa.com.tadawul.website.tip.queue.MessageDatabaseQueue;
import sa.com.tadawul.website.tip.shared.DBManagerSMEShareIntra;
import sa.com.tadawul.website.tip.shared.TipConstants;
import sa.com.tadawul.website.tip.shared.Utility;
import sa.com.tadawul.website.tip.vo.TIP_BaseVO;
import sa.com.tadawul.website.tip.vo.TIP_SMEShareVO;

public class DBWriterSMEShareIntra extends DBWriter {

	private final static Log logger = LogFactory.getLog(DBWriterSMEShareIntra.class);

	private DBManagerSMEShareIntra dbUtilObj;
	static long currentUpdatedTime = 0;
	static long  lastUpdatedTime = 0;
	
	public DBWriterSMEShareIntra( String ThreadName) throws Exception {	
		super( ThreadName);
		this.threadname=ThreadName;
		dbUtilObj = new DBManagerSMEShareIntra(this.threadname);
	}

	@Override
	public void run() {
		boolean ret;
		long insertCounter = 0;
		long totalInsertedCounter = 0;

		BufferedWriter bufferedWriter = null;
		BufferedReader in = null;
		String fileName ="DBWriterSMEShareIntra_" + TipConstants.TARGIN_FEED_REQ_DATE	+ ".txt";
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
				if (MessageDatabaseQueue.tipSMEShareIntraQ.size() > 0) {
					baseObj = (TIP_BaseVO) MessageDatabaseQueue.tipSMEShareIntraQ.getFrame();

					if (baseObj.sequence_no <= startCount &&  !baseObj.isBOD 	&&  !baseObj.isEOD) {						
						logger.debug("Ignoring Message with sequence_no: " + baseObj.sequence_no + " startCount: " + startCount + " isBOD: " + baseObj.isBOD+ " isEOD: " + baseObj.isEOD);						
						MessageDatabaseQueue.tipSMEShareIntraQ.releaseFrame();
						baseObj = null;
						baseObj = null;
					}
					
					if (null != baseObj && baseObj.sequence_no > seqNum) {
						seqNum = baseObj.sequence_no;
					}
					
					if (baseObj.isBOD) {
						isCommit=true;
						logger.info("BOD for SME Share Intra ");
						ret = dbUtilObj.initializeDBforSMEShareIntra();
						if (!ret) {
							logger.error("BOD Operations for SMEShare Intra Data in DB Failed");
							TipConstants.shutdownFlag = true;
						}

						// Added for Share RI
						logger.info("BOD for SMEShareRI Intra ");
						ret = dbUtilObj.initializeDBforSMEShareRIIntra();
						if (!ret) {
							logger.error("BOD Operations for SMEShare RI Intra Data in DB Failed");
							TipConstants.shutdownFlag = true;
						}		
						MessageDatabaseQueue.tipSMEShareIntraQ.releaseFrame();
						continue;
					}

					if (baseObj.isEOD) {
						isCommit=true;
					}
					
					try {

						boolean isShareRI = ((TIP_SMEShareVO) baseObj).isShareRI;
						boolean isSpecialShare = ((TIP_SMEShareVO) baseObj).isSpecialShare;
						if (!isShareRI) {
							if (isSpecialShare) {
								ret = dbUtilObj.insertSpecialSMEShareIntraData((TIP_SMEShareVO) baseObj,isCommit);
							} else {							
								ret = dbUtilObj.insertSMEShareIntraData((TIP_SMEShareVO) baseObj,isCommit);
							}
						}
						else {
							ret = dbUtilObj.insertSMEShareRIIntraData((TIP_SMEShareVO) baseObj,isCommit);
						}
						if (!ret) {
							logger.error("Insert SME Share Intra Data to DB Failed: Seq# "	+ baseObj.sequence_no+ " uncommitted msgs " + insertCounter);
						}
					} catch (Exception e) {
						logger.error("************Exception Occured in DBWriter  :"+ e.getMessage());
					}

					MessageDatabaseQueue.tipSMEShareIntraQ.releaseFrame();
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
					//	GUIHelper.SMEshareIntraConnection = TipConstants.CONN_STATUS_STOPPED;
						break;
					}
					Thread.sleep(100);
				}			
				currentUpdatedTime =  System.currentTimeMillis();
				if (insertCounter > 0	&&  (currentUpdatedTime-lastUpdatedTime)>TipConstants.WRITER_THREADS_DB_FILE_UPDATE_INTERVAL) {
					dbUtilObj.commitTrans(true);					
					bufferedWriter = new BufferedWriter(new FileWriter(Utility.logFileCreation(fileName), false));
					bufferedWriter.write(String.valueOf(seqNum));
					bufferedWriter.close();
					totalInsertedCounter += insertCounter;
					logger.info("Seq# " + seqNum +  " insertCounter " + insertCounter+ " totalInsertedCounter " + totalInsertedCounter+" Remaining Msgs : " + MessageDatabaseQueue.tipSMEShareIntraQ.size());
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
					logger.info("Seq# " + seqNum +  " insertCounter " + insertCounter + " totalInsertedCounter " + totalInsertedCounter + " Remaining Msgs : " + MessageDatabaseQueue.tipSMEShareIntraQ.size()+ " Ending");
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
