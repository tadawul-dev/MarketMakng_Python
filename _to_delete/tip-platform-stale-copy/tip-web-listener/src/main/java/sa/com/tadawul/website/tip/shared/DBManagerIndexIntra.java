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
 * Name          :     FileName : DB2Manager.java
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 10/April/2009            Oracle                   Initial Version
 * 1.1                                                        14/November/2009         Mohanram,Oracle          Added functionalities to process the Equity Rights Share.
 * 1.2                                                        02/February/2010         Mohanram,Oracle          Added functionalities to process the Exchange Traded Funds.              
 *----------------------------------------------------------------------------------------------
 */
package sa.com.tadawul.website.tip.shared;

import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.sql.BatchUpdateException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import oracle.jdbc.pool.OracleDataSource;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import sa.com.tadawul.website.tip.vo.TIP_IndexVO;

/**
 * @description This class is used to get Database Connection and all the
 *              prepared Statements.
 *
 */
public class DBManagerIndexIntra extends DBManager {

	private static final Logger logger = LogManager.getLogger(DBManagerIndexIntra.class);
	private PreparedStatement psDeleteIndexIntrabyInterval;
	private PreparedStatement psDeleteIndexIntraComplete;
	private PreparedStatement psInsIndexIntra;
	
	static long  lastUpdatedTime = 0;
	static long currentUpdatedTime = 0;
	private int totalIndexIntraCount = 0;
	
	public DBManagerIndexIntra(String threadName) throws Exception {
		super(threadName);
		try {
			prepareStatements();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// openConnection() is inherited from DBManager (was duplicated in every manager)

	/**
	 * This method will Initialize all the Prepared Statements respective to the
	 * available Threads
	 *
	 */
	public void prepareStatements() throws Exception {

		logger.debug("inside DB2Manager prepareStatements function");
		StringBuilder sbPrepareQueries = new StringBuilder();
		try {

			sbPrepareQueries.append("DELETE FROM WEB.HS_TE_INDEX_INTRA where TRANS_TIME < to_date( ? , 'YYYY-MM-DD HH24:mi:SS') and  FK_TE_SYMBOL NOT IN('TT50CITR','TASITR') ");
			psDeleteIndexIntrabyInterval = g_oConnection.prepareStatement(sbPrepareQueries.toString());

			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("DELETE FROM WEB.HS_TE_INDEX_INTRA WHERE FK_TE_SYMBOL NOT IN('TT50CITR','TASITR')");
			psDeleteIndexIntraComplete = g_oConnection.prepareStatement(sbPrepareQueries.toString());

			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("INSERT INTO WEB.HS_TE_INDEX_INTRA(FK_TE_SYMBOL,DESCRIPTION_EN,"
							+ "DESCRIPTION_AR,INDEX_PRICE,VOLUME_TRADED,TURNOVER,NET_CHANGE,OPEN_PRICE,"
							+ "HIGH_PRICE,LOW_PRICE,TRANS_TIME,PREVIOUS_INDEX_PRICE,PERCENT_CHANGE,NO_OF_TRADES)"
							+ " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

			psInsIndexIntra = g_oConnection.prepareStatement(sbPrepareQueries	.toString());
		} catch (Exception e) {
			logger.error("Exception while preparing query ["+ sbPrepareQueries.toString() + "] is " + e);

			throw e;
		}

	}

	/**
	 * This method will do the commit.
	 * 
	 * @param null
	 */
	public void commitTrans(boolean isCommit) {
		try {

			try {
				
				currentUpdatedTime =  System.currentTimeMillis();
				if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.WRITER_THREADS_DB_UPDATE_INTERVAL )
				{
				//if (objIndex.isEndOfBatch)
					psInsIndexIntra.executeBatch();
					logger.debug("DBManagerIndexIntra Batch Updated");
					lastUpdatedTime = currentUpdatedTime;
					
				}
				
			} catch (BatchUpdateException be1) {
				logger.error("BatchUpdateException in commitTrans() while doing InsIndexIntra "	+ be1);				
					try {
						psInsIndexIntra.clearBatch();
					} catch (SQLException e1) {
						logger.error("Exception in psInsIndexIntra in clearBatch(): " + e1, e1);
					}					
				}
			
			commitIfNeeded();

		} catch (SQLException ex) {
			logger.error("SQL Exception in commitTrans() " + ex, ex);
		} finally {
			// System.gc() removed: forced full GC every commit caused stop-the-world pauses
		}

	}

	
	public void commitIntraTrans() {
		try {			
			
			commitIfNeeded();

		} catch (SQLException ex) {
			logger.error("SQL Exception in commitTrans() " + ex, ex);
		} finally {

			// System.gc() removed: forced full GC every commit caused stop-the-world pauses
		}

	}
	
	/**
	 * This method will close the existing DB Connection
	 */
	public void closeConnection() {
		try {
			logger.debug("inside  DB2Manager closeConnection function");

			if (psDeleteIndexIntrabyInterval != null){
				psDeleteIndexIntrabyInterval.close();
			}
			
			if (psDeleteIndexIntraComplete != null){
				psDeleteIndexIntraComplete.close();
			}
			
			if (psInsIndexIntra != null){
				psInsIndexIntra.close();
			}
			
			if (g_oConnection != null){
				g_oConnection.close();
			}
			logger.debug("Exit  DB2Manager closeConnection function");
		} catch (SQLException ex) {
			logger.error("SQL Exception in closeConnection() " + ex, ex);
		}
	}

	/**
	 * This method will initialize IndexIntra table at BOD
	 * 
	 * @return boolean
	 */
	public boolean initializeDBforIndexIntra() {

		try {
			logger.debug("inside  DB2Manager initializeDBforIndexIntra function");

			if (!LoadInitialData.dtShareIndexLastUpdateTime.equals(dtFeedReqDate)) {
				int numRowsDeleted = 0;
				/*int totMinutes = (TipConstants.DELETE_END_HOUR - TipConstants.DELETE_START_HOUR) * 60;
				int numRowsDeleted = 0, dltHr = TipConstants.DELETE_START_HOUR, dltMin = 0;
				String beginTime = "";
				String dateTime = LoadInitialData.strLastUpdDateForShareIndex + " ";

				logger.debug("initializeDBforIndexIntra: dtShareIndexLastUpdateTime "	+ LoadInitialData.dtShareIndexLastUpdateTime	+ " DELETE_START_HOUR "+ TipConstants.DELETE_START_HOUR	+ " DELETE_END_HOUR "
						+ TipConstants.DELETE_END_HOUR+ " DELETE_INTERVAL_MIN "	+ TipConstants.DELETE_INTERVAL_MIN);
				if (logger.isDebugEnabled()) logger.debug("initializeDBforIndexIntra: totalMinutes {}", totMinutes);

				for (int i = 0; i <= totMinutes; i += TipConstants.DELETE_INTERVAL_MIN) {
					if (dltMin >= 10)
						beginTime = dltHr + ":" + dltMin + ":00";
					else
						beginTime = dltHr + ":0" + dltMin + ":00";

					psDeleteIndexIntrabyInterval.setString(1, dateTime	+ beginTime);

					numRowsDeleted = numRowsDeleted	+ psDeleteIndexIntrabyInterval.executeUpdate();
					commitTrans();
					dltMin += TipConstants.DELETE_INTERVAL_MIN;
					if (dltMin >= 60) {
						dltMin -= 60;
						dltHr++;
					}
					if (logger.isDebugEnabled()) logger.debug("Inside for loop --> initializeDBforIndexIntra: deleted upto dateTime+beginTime {}{} rows deleted {}", dateTime, beginTime, numRowsDeleted);
				}*/
				numRowsDeleted = numRowsDeleted	+ psDeleteIndexIntraComplete.executeUpdate();
				commitIntraTrans();
				if (logger.isDebugEnabled()) logger.debug("Outside for loop --> initializeDBforIndexIntra: general delete  rows deleted {}", numRowsDeleted);
				logger.info("initializeDBforIndexIntra: Records deleted from WEB.HS_TE_INDEX_INTRA");
			}
			return true;

		} catch (SQLException e) {
			logger.error("Exception in initializeDBforIndexIntra: " + e, e);

		}

		logger.debug("Exit  DB2Manager initializeDBforIndexIntra function");
		return false;
	}

	/**
	 * This method will insert recored in to the table WEB.HS_TE_INDEX_INTRA
	 * 
	 * @param objIndex
	 *            contains attributes of WEB.HS_TE_INDEX_INTRA table
	 * @return boolean
	 */
	public boolean insertIndexIntraData(TIP_IndexVO objIndex,boolean isCommit) {

		String description_ar = objIndex.Description_AR;

		try {
			psInsIndexIntra.setString(1, objIndex.Fk_te_symbol);
			psInsIndexIntra.setString(2, objIndex.Description_EN);
			/**
			 * the next line is temporary until get response from Tadawul
			 */
			if (null != description_ar && description_ar.length() > 0) {
				// description_ar= ;
				try {
					// logger.info("************* encode **********:{}", description_ar);
					description_ar = reencodeArabic(description_ar);
					// logger.info("************* Afrer encode **********:{}", description_ar);
				} catch (CharacterCodingException e) {
					logger.error("Exception in insertIndexIntraData in cov: "
							+ e, e);
				}

			} else
				description_ar = objIndex.Description_EN;
			// psInsIndexIntra.setString(3, objIndex.Description_AR !=null
			// && objIndex.Description_AR.length()>0 ?
			// objIndex.Description_AR : objIndex.Description_EN);
			psInsIndexIntra.setString(3, description_ar);
			psInsIndexIntra.setDouble(4, objIndex.index_price);
			psInsIndexIntra.setLong(5, objIndex.volume_trade);
			psInsIndexIntra.setDouble(6, objIndex.turnover);
			psInsIndexIntra.setDouble(7, objIndex.net_change);
			psInsIndexIntra.setDouble(8, objIndex.open_price);
			psInsIndexIntra.setDouble(9, objIndex.high_price);
			psInsIndexIntra.setDouble(10, objIndex.low_price);
			psInsIndexIntra.setTimestamp(11, objIndex.trans_time);
			psInsIndexIntra.setDouble(12, objIndex.previous_index_price);
			psInsIndexIntra.setDouble(13, objIndex.percent_change);
			psInsIndexIntra.setLong(14, objIndex.no_of_trades);
			
			psInsIndexIntra.addBatch();
			
			currentUpdatedTime =  System.currentTimeMillis();
			if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.WRITER_THREADS_DB_UPDATE_INTERVAL )
			{
			//if (objIndex.isEndOfBatch)
				psInsIndexIntra.executeBatch();
				logger.debug("DBManagerIndexIntra Batch Updated");
				lastUpdatedTime = currentUpdatedTime;
				
			}

			totalIndexIntraCount++;
			

			if (logger.isDebugEnabled()) logger.debug("DBManagerIndexIntra: updateIndexIntraData :{}", totalIndexIntraCount);
			return true;

			
		} catch (BatchUpdateException be2) {
			logger.error("BatchUpdateException in commitTrans() while doing psInsIndexIntra "+ be2);
			try {
				psInsIndexIntra.clearBatch();
			} catch (SQLException e1) {
				logger.error("Exception in psInsIndexIntra in clearBatch(): " + e1, e1);
			}
			/*while (be2.getNextException() != null)
				logger.error("BatchUpdateException in commitTrans() while doing InsBondIntra "+ be2.getNextException());*/
		}catch (SQLException e) {			
			logger.error("Exception in insertIndexIntraData: " + e, e);
		}

		if (objIndex != null)
			objIndex = null;
		return false;
	}

}
