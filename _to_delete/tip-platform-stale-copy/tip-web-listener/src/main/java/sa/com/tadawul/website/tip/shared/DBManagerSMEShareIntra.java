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

import java.sql.BatchUpdateException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import oracle.jdbc.pool.OracleDataSource;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import sa.com.tadawul.website.tip.vo.TIP_SMEShareVO;

/**
 * @description This class is used to get Database Connection and all the
 *              prepared Statements.
 *
 */
public class DBManagerSMEShareIntra extends DBManager {

	private static final Logger logger = LogManager.getLogger(DBManagerSMEShareIntra.class);
	private PreparedStatement psDeleteSMEStockIntrabyInterval;
	private PreparedStatement psDeleteSMEStockIntraComplete;
	private PreparedStatement psInsSMEShareIntra;
	private PreparedStatement psInsSpecialSMEShareIntra;
	private PreparedStatement psDeleteStockSMERIIntrabyInterval;
	private PreparedStatement psDeleteStockSMERIIntraComplete;
	private PreparedStatement psInsSMEShareRIIntra;
	private PreparedStatement psInsSpecialShareIntra;
	
	static long  lastUpdatedTime = 0;
	static long currentUpdatedTime = 0;
	private int totalSMEShareIntraCount = 0;
	private int totalSMESpecialShareIntraCount = 0;
	private int totalSMERIShareIntraCount = 0;
	
	
	public DBManagerSMEShareIntra(String threadName) throws Exception {
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

			sbPrepareQueries.append("DELETE FROM WEB.HS_TE_SME_STOCK_INTRA where TRANS_TIME < to_date(? , 'YYYY-MM-DD HH24:mi:SS') ");
			psDeleteSMEStockIntrabyInterval = g_oConnection.prepareStatement(sbPrepareQueries.toString());

			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("DELETE FROM WEB.HS_TE_SME_STOCK_INTRA");
			psDeleteSMEStockIntraComplete = g_oConnection.prepareStatement(sbPrepareQueries.toString());

			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("INSERT INTO WEB.HS_TE_SME_STOCK_INTRA(FK_TE_SYMBOL,TRANSACTION_DATE,LAST_TRADE_PRICE,"
							+ "LAST_TRADE_QUANTITY,LAST_UPDATE_TIME,ASK_PRICE,ASK_QUANTITY,BID_PRICE,BID_QUANTITY,NET_CHANGE,HIGH_PRICE,"
							+ "HIGH_52_WEEKS_PRICE,LOW_PRICE,LOW_52_WEEKS_PRICE,MARKET_CAP,NO_OF_TRADES,PBR,PER,PERCENT_CHANGE,"
							+ "PREVIOUS_CLOSE_PRICE,SEQUENCE_NO,TODAYS_CLOSE_PRICE,TODAYS_OPEN,TURNOVER,VOLUME_TRADED,YIELD,TRANS_TIME"
							+ ", HIGH_52_WEEKS_DATE,LOW_52_WEEKS_DATE"
							+ ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

			psInsSMEShareIntra = g_oConnection.prepareStatement(sbPrepareQueries.toString());

			// SME special stock
			if (sbPrepareQueries.length() > 0)
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("INSERT INTO WEB.HS_STE_SME_STOCK_INTRA(FK_STE_SYMBOL,TRANSACTION_DATE,LAST_TRADE_PRICE,"
							+ "LAST_TRADE_QUANTITY,LAST_UPDATE_TIME,ASK_PRICE,ASK_QUANTITY,BID_PRICE,BID_QUANTITY,NET_CHANGE,HIGH_PRICE,"
							+ "HIGH_52_WEEKS_PRICE,LOW_PRICE,LOW_52_WEEKS_PRICE,MARKET_CAP,NO_OF_TRADES,PBR,PER,PERCENT_CHANGE,"
							+ "PREVIOUS_CLOSE_PRICE,SEQUENCE_NO,TODAYS_CLOSE_PRICE,TODAYS_OPEN,TURNOVER,VOLUME_TRADED,YIELD,TRANS_TIME"
							+ ", HIGH_52_WEEKS_DATE,LOW_52_WEEKS_DATE"
							+ ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

			psInsSpecialSMEShareIntra = g_oConnection.prepareStatement(sbPrepareQueries.toString());

			// SME RI stock
			if (sbPrepareQueries.length() > 0)
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("DELETE FROM WEB.HS_RI_SME_STOCK_INTRA where TRANS_TIME < to_date(? , 'YYYY-MM-DD HH24:mi:SS') ");
			psDeleteStockSMERIIntrabyInterval = g_oConnection.prepareStatement(sbPrepareQueries.toString());

			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("DELETE FROM WEB.HS_RI_SME_STOCK_INTRA");
			psDeleteStockSMERIIntraComplete = g_oConnection.prepareStatement(sbPrepareQueries.toString());

			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("INSERT INTO WEB.HS_RI_SME_STOCK_INTRA(FK_RI_SYMBOL,TRANSACTION_DATE,LAST_TRADE_PRICE,"
							+ "LAST_TRADE_QUANTITY,LAST_UPDATE_TIME,ASK_PRICE,ASK_QUANTITY,BID_PRICE,BID_QUANTITY,NET_CHANGE,HIGH_PRICE,"
							+ "HIGH_52_WEEKS_PRICE,LOW_PRICE,LOW_52_WEEKS_PRICE,MARKET_CAP,NO_OF_TRADES,PBR,PER,PERCENT_CHANGE,"
							+ "PREVIOUS_CLOSE_PRICE,SEQUENCE_NO,TODAYS_CLOSE_PRICE,TODAYS_OPEN,TURNOVER,VOLUME_TRADED,YIELD,TRANS_TIME"
							+ ", HIGH_52_WEEKS_DATE,LOW_52_WEEKS_DATE"
							+ ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

			psInsSMEShareRIIntra = g_oConnection.prepareStatement(sbPrepareQueries.toString());
			
			
			if (sbPrepareQueries.length() > 0)
				sbPrepareQueries.delete(0, sbPrepareQueries.length());

			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("INSERT INTO WEB.HS_STE_SME_STOCK_INTRA(FK_STE_SYMBOL,TRANSACTION_DATE,LAST_TRADE_PRICE,"
							+ "LAST_TRADE_QUANTITY,LAST_UPDATE_TIME,ASK_PRICE,ASK_QUANTITY,BID_PRICE,BID_QUANTITY,NET_CHANGE,HIGH_PRICE,"
							+ "HIGH_52_WEEKS_PRICE,LOW_PRICE,LOW_52_WEEKS_PRICE,MARKET_CAP,NO_OF_TRADES,PBR,PER,PERCENT_CHANGE,"
							+ "PREVIOUS_CLOSE_PRICE,SEQUENCE_NO,TODAYS_CLOSE_PRICE,TODAYS_OPEN,TURNOVER,VOLUME_TRADED,YIELD,TRANS_TIME"
							+ ", HIGH_52_WEEKS_DATE,LOW_52_WEEKS_DATE"
							+ ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

			psInsSpecialShareIntra = g_oConnection.prepareStatement(sbPrepareQueries.toString());
			
			logger.debug("Exit DB2Manager prepareStatements function");

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

			// Added for SME
			try {			
					psInsSMEShareIntra.executeBatch();				
			} catch (BatchUpdateException be) {
				logger.error("BatchUpdateException in commitTrans() while doing InsSMEShareIntra "+ be);
				
				try {
					psInsSMEShareIntra.clearBatch();
				} catch (SQLException e1) {
					logger.error("Exception in psInsSMEShareIntra in clearBatch(): " + e1, e1);
				}
				/*while (be.getNextException() != null)
					logger.error("BatchUpdateException in commitTrans() while doing InsSMEShareIntra "	+ be.getNextException());*/
			}

			// Added for SMEShare RI
			try {				
					psInsSMEShareRIIntra.executeBatch();					
			} catch (BatchUpdateException be) {
				logger.error("BatchUpdateException in commitTrans() while doing InsSMEShareRIIntra "	+ be);
				try {
					psInsSMEShareRIIntra.clearBatch();
				} catch (SQLException e1) {
					logger.error("Exception in psInsSMEShareRIIntra in clearBatch(): " + e1, e1);
				}
				/*while (be.getNextException() != null)
					logger.error("BatchUpdateException in commitTrans() while doing InsSMEShareRIIntra "	+ be.getNextException());*/
			}
			
			
			try {			
				psInsSpecialShareIntra.executeBatch();					
			
		} catch (BatchUpdateException be) {
			logger.error("BatchUpdateException in commitTrans() while doing psInsSpecialShareIntra "	+ be);
			try {
				psInsSpecialShareIntra.clearBatch();
			} catch (SQLException e1) {
				logger.error("Exception in psInsSpecialShareIntra in clearBatch(): " + e1, e1);
			}
			/*while (be.getNextException() != null)
				logger.error("BatchUpdateException in commitTrans() while doing psInsSpecialShareIntra "	+ be.getNextException());*/
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

			if (psDeleteSMEStockIntrabyInterval != null){
				psDeleteSMEStockIntrabyInterval.close();
			}
			
			if (psDeleteSMEStockIntraComplete != null){
				psDeleteSMEStockIntraComplete.close();
			}
			
			if (psInsSMEShareIntra != null){
				psInsSMEShareIntra.close();
			}
			
			if (psInsSpecialSMEShareIntra != null){
				psInsSpecialSMEShareIntra.close();
			}
			
			if (psDeleteStockSMERIIntrabyInterval != null){
				psDeleteStockSMERIIntrabyInterval.close();
			}
			
			if (psDeleteStockSMERIIntraComplete != null){
				psDeleteStockSMERIIntraComplete.close();
			}

			if (psInsSMEShareRIIntra != null){
				psInsSMEShareRIIntra.close();
			}
			
			if (psInsSpecialShareIntra != null){
				psInsSpecialShareIntra.close();
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
	 * This method will initialize all the columns in the table
	 * WEB.HS_TE_SME_STOCK_INTRA,WEB.HS_TE_SME_INDEX_INTRA
	 * EFP.TE_MARKET,EFP.TE_INDEX ,EFP.TE_STOCK for Share Market
	 * 
	 * @param null
	 * @return boolean
	 */
	// Added for SME
	public boolean initializeDBforSMEShareIntra() {

		try {
			logger.debug("inside DB2Manager initializeDBforSMEShareIntra function");

			if (!LoadInitialData.dtSMEShareIndexLastUpdateTime.equals(dtFeedReqDate)) {
				int numRowsDeleted = 0;
				/*int totMinutes = (TipConstants.DELETE_END_HOUR - TipConstants.DELETE_START_HOUR) * 60;
				int numRowsDeleted = 0, dltHr = TipConstants.DELETE_START_HOUR, dltMin = 0;
				String beginTime = "";
				String dateTime = LoadInitialData.strLastUpdDateForSMEShareIndex.split("\\s+")[0]
						+ " ";
				logger.debug("initializeDBforSMEShareIntra: dtSMEShareIndexLastUpdateTime "	+ LoadInitialData.dtSMEShareIndexLastUpdateTime	+ " DELETE_START_HOUR "+ TipConstants.DELETE_START_HOUR
						+ " DELETE_END_HOUR "	+ TipConstants.DELETE_END_HOUR	+ " DELETE_INTERVAL_MIN "	+ TipConstants.DELETE_INTERVAL_MIN);
				if (logger.isDebugEnabled()) logger.debug("initializeDBforSMEShareIntra: totalMinutes {}", totMinutes);
				for (int i = 0; i <= totMinutes; i += TipConstants.DELETE_INTERVAL_MIN) {
					if (dltMin >= 10)
						beginTime = dltHr + ":" + dltMin + ":00";
					else
						beginTime = dltHr + ":0" + dltMin + ":00";

					psDeleteSMEStockIntrabyInterval.setString(1, dateTime+ beginTime);

					numRowsDeleted = numRowsDeleted	+ psDeleteSMEStockIntrabyInterval.executeUpdate();
					commitTrans();
					dltMin += TipConstants.DELETE_INTERVAL_MIN;
					if (dltMin >= 60) {
						dltMin -= 60;
						dltHr++;
					}
					if (logger.isDebugEnabled()) logger.debug("Inside for loop --> initializeDBforSMEShareIntra: deleted upto dateTime+beginTime {}{} rows deleted {}", dateTime, beginTime, numRowsDeleted);
				}*/
				numRowsDeleted = numRowsDeleted	+ psDeleteSMEStockIntraComplete.executeUpdate();
				commitIntraTrans();
				if (logger.isDebugEnabled()) logger.debug("Outside for loop --> initializeDBforSMEShareIntra: general delete  rows deleted {}", numRowsDeleted);
				logger.info("initializeDBforSMEShareIntra: Records deleted from WEB.HS_TE_SME_STOCK_INTRA");
			}

			return true;

		} catch (SQLException e) {
			logger.error("Exception in initializeDBforSMEShareIntra " + e, e);

		}

		logger.debug("Exit  DB2Manager initializeDBforSMEShareIntra function");
		return false;
	}

	/**
	 * This method will initialize all the columns in the table
	 * WEB.HS_TE_STOCK_INTRA,WEB.HS_TE_INDEX_INTRA EFP.TE_MARKET,EFP.TE_INDEX
	 * ,EFP.TE_STOCK for Share Market
	 * 
	 * @param null
	 * @return boolean
	 */
	// Added for SME
	public boolean initializeDBforSMEShareRIIntra() {

		try {
			logger.debug("inside DB2Manager initializeDBforSMEShareRIIntra function");

			/**
			 * To be anlysed for the variable dtShareIndexLastUpdateTime
			 */
			if (!LoadInitialData.dtSMEShareIndexLastUpdateTime.equals(dtFeedReqDate)) {
				int numRowsDeleted = 0;
				/*int totMinutes = (TipConstants.DELETE_END_HOUR - TipConstants.DELETE_START_HOUR) * 60;
				int numRowsDeleted = 0, dltHr = TipConstants.DELETE_START_HOUR, dltMin = 0;
				String beginTime = "";
				String dateTime = LoadInitialData.strLastUpdDateForSMEShareIndex + " ";
				logger.debug("initializeDBforSMEShareRIIntra: dtSMEShareIndexLastUpdateTime "	+ LoadInitialData.dtSMEShareIndexLastUpdateTime	+ " DELETE_START_HOUR "+ TipConstants.DELETE_START_HOUR
						+ " DELETE_END_HOUR "	+ TipConstants.DELETE_END_HOUR	+ " DELETE_INTERVAL_MIN "	+ TipConstants.DELETE_INTERVAL_MIN);
				if (logger.isDebugEnabled()) logger.debug("initializeDBforSMEShareRIIntra: totalMinutes {}", totMinutes);
				for (int i = 0; i <= totMinutes; i += TipConstants.DELETE_INTERVAL_MIN) {
					if (dltMin >= 10)
						beginTime = dltHr + ":" + dltMin + ":00";
					else
						beginTime = dltHr + ":0" + dltMin + ":00";

					psDeleteStockSMERIIntrabyInterval.setString(1, dateTime	+ beginTime);

					numRowsDeleted = numRowsDeleted	+ psDeleteStockSMERIIntrabyInterval.executeUpdate();
					commitTrans();
					dltMin += TipConstants.DELETE_INTERVAL_MIN;
					if (dltMin >= 60) {
						dltMin -= 60;
						dltHr++;
					}
					if (logger.isDebugEnabled()) logger.debug("Inside for loop --> initializeDBforSMEShareRIIntra: deleted upto dateTime+beginTime {}{} rows deleted {}", dateTime, beginTime, numRowsDeleted);
				}*/
				numRowsDeleted = numRowsDeleted	+ psDeleteStockSMERIIntraComplete.executeUpdate();
				commitIntraTrans();
				if (logger.isDebugEnabled()) logger.debug("Outside for loop --> initializeDBforSMEShareRIIntra: general delete  rows deleted {}", numRowsDeleted);
				logger.info("initializeDBforSMEShareRIIntra: Records deleted from WEB.HS_RI_STE_STOCK_INTRA");
			}

			return true;
		} catch (SQLException e) {
			logger.error("Exception in initializeDBforShareSMERIIntra " + e, e);

		}

		logger.debug("Exit  DB2Manager initializeDBforSMEShareRIIntra function");
		return false;
	}

	/**
	 * This method will insert recored in to the table WEB.HS_TE_SME_STOCK_INTRA
	 * 
	 * @param objShare
	 *            contains attributes of WEB.HS_TE_SME_STOCK_INTRA table
	 * @return boolean
	 */
	// Added for SME
	public boolean insertSMEShareIntraData(TIP_SMEShareVO objShare,boolean isCommit) {

		try {

			psInsSMEShareIntra.setString(1, objShare.fk_te_symbol);
			psInsSMEShareIntra.setTimestamp(2, objShare.transaction_date);
			psInsSMEShareIntra.setDouble(3, objShare.last_trade_price);
			psInsSMEShareIntra.setInt(4, objShare.last_trade_quantity);
			psInsSMEShareIntra.setTimestamp(5, objShare.last_update_time);
			psInsSMEShareIntra.setDouble(6, objShare.ask_price);
			psInsSMEShareIntra.setLong(7, objShare.ask_quantity);
			psInsSMEShareIntra.setDouble(8, objShare.bid_price);
			psInsSMEShareIntra.setLong(9, objShare.bid_quantity);
			psInsSMEShareIntra.setDouble(10, objShare.net_change);
			psInsSMEShareIntra.setDouble(11, objShare.high_price);
			psInsSMEShareIntra.setDouble(12, objShare.high_52_weeks_price);
			psInsSMEShareIntra.setDouble(13, objShare.low_price);
			psInsSMEShareIntra.setDouble(14, objShare.low_52_weeks_price);
			psInsSMEShareIntra.setDouble(15, objShare.market_cap);
			psInsSMEShareIntra.setLong(16, objShare.no_of_trades);
			psInsSMEShareIntra.setDouble(17, objShare.Pbr);
			psInsSMEShareIntra.setDouble(18, objShare.Per);
			psInsSMEShareIntra.setDouble(19, objShare.percent_change);
			psInsSMEShareIntra.setDouble(20, objShare.Previous_close_price);
			psInsSMEShareIntra.setLong(21, objShare.sequence_no);
			psInsSMEShareIntra.setDouble(22, objShare.todays_close_price);
			psInsSMEShareIntra.setDouble(23, objShare.Todays_open);
			psInsSMEShareIntra.setDouble(24, objShare.turnover);
			psInsSMEShareIntra.setDouble(25, objShare.volume_traded);
			psInsSMEShareIntra.setDouble(26, objShare.yield);
			psInsSMEShareIntra.setTimestamp(27, objShare.transaction_date);

			psInsSMEShareIntra.setDate(28, objShare.high_52_weeks_date);
			psInsSMEShareIntra.setDate(29, objShare.low_52_weeks_date);
			
			psInsSMEShareIntra.addBatch();
			
			currentUpdatedTime =  System.currentTimeMillis();
			if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.WRITER_THREADS_DB_UPDATE_INTERVAL  )
			{
			//if (objIndex.isEndOfBatch)
				psInsSMEShareIntra.executeBatch();
				logger.debug("DBManagerSMEShareIntra Batch Updated");
				lastUpdatedTime = currentUpdatedTime;				
			}
			totalSMEShareIntraCount++;

				if (logger.isDebugEnabled()) logger.debug("DBManagerSMEShareIntra: insertSMEShareIntraData :{}", totalSMEShareIntraCount);
			return true;

		}catch (BatchUpdateException be) {
			logger.error("BatchUpdateException in commitTrans() while doing InsSMEShareRIIntra "	+ be);
			try {
				psInsSMEShareRIIntra.clearBatch();
			} catch (SQLException e1) {
				logger.error("Exception in psInsSMEShareRIIntra in clearBatch(): " + e1, e1);
			}			
		} 
		catch (SQLException e) {
			try {
				psInsSMEShareIntra.clearBatch();
			} catch (SQLException e1) {
				logger.error("Exception in psInsSMEShareIntra in clearBatch(): " + e, e);
			}
			logger.error("Exception in insertSMEShareIntraData: " + e, e);

		}

		if (objShare != null)
			objShare = null;
		return false;
	}

	public boolean insertSpecialSMEShareIntraData(TIP_SMEShareVO objShare,boolean isCommit) {

		try {

			psInsSpecialSMEShareIntra.setString(1, objShare.fk_te_symbol);
			psInsSpecialSMEShareIntra.setTimestamp(2, objShare.transaction_date);
			psInsSpecialSMEShareIntra.setDouble(3, objShare.last_trade_price);
			psInsSpecialSMEShareIntra.setInt(4, objShare.last_trade_quantity);
			psInsSpecialSMEShareIntra.setTimestamp(5, objShare.last_update_time);
			psInsSpecialSMEShareIntra.setDouble(6, objShare.ask_price);
			psInsSpecialSMEShareIntra.setLong(7, objShare.ask_quantity);
			psInsSpecialSMEShareIntra.setDouble(8, objShare.bid_price);
			psInsSpecialSMEShareIntra.setLong(9, objShare.bid_quantity);
			psInsSpecialSMEShareIntra.setDouble(10, objShare.net_change);
			psInsSpecialSMEShareIntra.setDouble(11, objShare.high_price);
			psInsSpecialSMEShareIntra.setDouble(12,objShare.high_52_weeks_price);
			psInsSpecialSMEShareIntra.setDouble(13, objShare.low_price);
			psInsSpecialSMEShareIntra.setDouble(14, objShare.low_52_weeks_price);
			psInsSpecialSMEShareIntra.setDouble(15, objShare.market_cap);
			psInsSpecialSMEShareIntra.setLong(16, objShare.no_of_trades);
			psInsSpecialSMEShareIntra.setDouble(17, objShare.Pbr);
			psInsSpecialSMEShareIntra.setDouble(18, objShare.Per);
			psInsSpecialSMEShareIntra.setDouble(19, objShare.percent_change);
			psInsSpecialSMEShareIntra.setDouble(20,objShare.Previous_close_price);
			psInsSpecialSMEShareIntra.setLong(21, objShare.sequence_no);
			psInsSpecialSMEShareIntra.setDouble(22, objShare.todays_close_price);
			psInsSpecialSMEShareIntra.setDouble(23, objShare.Todays_open);
			psInsSpecialSMEShareIntra.setDouble(24, objShare.turnover);
			psInsSpecialSMEShareIntra.setDouble(25, objShare.volume_traded);
			psInsSpecialSMEShareIntra.setDouble(26, objShare.yield);
			psInsSpecialSMEShareIntra.setTimestamp(27,objShare.transaction_date);
			psInsSpecialSMEShareIntra.setDate(28, objShare.high_52_weeks_date);
			psInsSpecialSMEShareIntra.setDate(29, objShare.low_52_weeks_date);

			psInsSpecialSMEShareIntra.addBatch();			
			currentUpdatedTime =  System.currentTimeMillis();
			if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.WRITER_THREADS_DB_UPDATE_INTERVAL  )
			{
			//if (objIndex.isEndOfBatch)
				psInsSpecialSMEShareIntra.executeBatch();
				logger.debug("DBManagerSMEShareIntra Batch Updated");
				lastUpdatedTime = currentUpdatedTime;				
			}
			totalSMESpecialShareIntraCount++;
			
	
			if (logger.isDebugEnabled()) logger.debug("DBManagerSMEShareIntra: insertSpecialSMEShareIntraData :{}", totalSMESpecialShareIntraCount);
			return true;

		} catch (BatchUpdateException be) {
			logger.error("BatchUpdateException in commitTrans() while doing psInsSpecialSMEShareIntra "	+ be);
			try {
				psInsSpecialSMEShareIntra.clearBatch();
			} catch (SQLException e1) {
				logger.error("Exception in psInsSpecialSMEShareIntra in clearBatch(): " + e1, e1);
			}			
		} catch (SQLException e) {
			try {
				psInsSpecialSMEShareIntra.clearBatch();
			} catch (SQLException e1) {
				logger.error("Exception in psInsSpecialSMEShareIntra in clearBatch(): " + e, e);
			}
			logger.error("Exception in insertShareIntraData: " + e, e);

		}

		if (objShare != null)
			objShare = null;
		return false;
	}

	/**
	 * This method will insert recored in to the table
	 * WEB.HS_TE_SME_RI_STOCK_INTRA
	 * 
	 * @param objShare
	 *            contains attributes of WEB.HS_TE_SME_RI_STOCK_INTRA table
	 * @return boolean
	 */
	// Added for SME
	public boolean insertSMEShareRIIntraData(TIP_SMEShareVO objShare,boolean isCommit) {

		try {

			psInsSMEShareRIIntra.setString(1, objShare.fk_te_symbol);
			psInsSMEShareRIIntra.setTimestamp(2, objShare.transaction_date);
			psInsSMEShareRIIntra.setDouble(3, objShare.last_trade_price);
			psInsSMEShareRIIntra.setInt(4, objShare.last_trade_quantity);
			psInsSMEShareRIIntra.setTimestamp(5, objShare.last_update_time);
			psInsSMEShareRIIntra.setDouble(6, objShare.ask_price);
			psInsSMEShareRIIntra.setLong(7, objShare.ask_quantity);
			psInsSMEShareRIIntra.setDouble(8, objShare.bid_price);
			psInsSMEShareRIIntra.setLong(9, objShare.bid_quantity);
			psInsSMEShareRIIntra.setDouble(10, objShare.net_change);
			psInsSMEShareRIIntra.setDouble(11, objShare.high_price);
			psInsSMEShareRIIntra.setDouble(12, objShare.high_52_weeks_price);
			psInsSMEShareRIIntra.setDouble(13, objShare.low_price);
			psInsSMEShareRIIntra.setDouble(14, objShare.low_52_weeks_price);
			psInsSMEShareRIIntra.setDouble(15, objShare.market_cap);
			psInsSMEShareRIIntra.setLong(16, objShare.no_of_trades);
			psInsSMEShareRIIntra.setDouble(17, objShare.Pbr);
			psInsSMEShareRIIntra.setDouble(18, objShare.Per);
			psInsSMEShareRIIntra.setDouble(19, objShare.percent_change);
			psInsSMEShareRIIntra.setDouble(20, objShare.Previous_close_price);
			psInsSMEShareRIIntra.setLong(21, objShare.sequence_no);
			psInsSMEShareRIIntra.setDouble(22, objShare.todays_close_price);
			psInsSMEShareRIIntra.setDouble(23, objShare.Todays_open);
			psInsSMEShareRIIntra.setDouble(24, objShare.turnover);
			psInsSMEShareRIIntra.setDouble(25, objShare.volume_traded);
			psInsSMEShareRIIntra.setDouble(26, objShare.yield);
			psInsSMEShareRIIntra.setTimestamp(27, objShare.transaction_date);

			psInsSMEShareRIIntra.setDate(28, objShare.high_52_weeks_date);
			psInsSMEShareRIIntra.setDate(29, objShare.low_52_weeks_date);

			psInsSMEShareRIIntra.addBatch();			
			currentUpdatedTime =  System.currentTimeMillis();
			
			if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.WRITER_THREADS_DB_UPDATE_INTERVAL  )
			{
			//if (objIndex.isEndOfBatch)
				psInsSMEShareRIIntra.executeBatch();
				logger.debug("DBManagerSMEShareIntra Batch Updated");
				lastUpdatedTime = currentUpdatedTime;				
			}
			totalSMERIShareIntraCount++;
			if (logger.isDebugEnabled()) logger.debug("DBManagerSMEShareIntra: insertSMEShareRIIntraData :{}", totalSMERIShareIntraCount);
			return true;

		} catch (BatchUpdateException be) {
			logger.error("BatchUpdateException in commitTrans() while doing psInsSMEShareRIIntra "	+ be);
			try {
				psInsSMEShareRIIntra.clearBatch();
			} catch (SQLException e1) {
				logger.error("Exception in psInsSMEShareRIIntra in clearBatch(): " + e1, e1);
			}			
		}catch (SQLException e) {
			
			try {
				psInsSMEShareRIIntra.clearBatch();
			} catch (SQLException e1) {
				logger.error("Exception in psInsSMEShareRIIntra in clearBatch(): " + e, e);
			}
			
			logger.error("Exception in insertSMEShareRIIntraData: " + e, e);

		}

		if (objShare != null)
			objShare = null;
		return false;
	}
}
