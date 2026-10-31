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

import sa.com.tadawul.website.tip.log.Log;
import sa.com.tadawul.website.tip.log.LogFactory;
import sa.com.tadawul.website.tip.vo.TIP_ShareVO;
/**
 * @description This class is used to get Database Connection and all the
 *              prepared Statements.
 *
 */
public class DBManagerShareIntra extends DBManager {

	private final static Log logger = LogFactory.getLog(DBManagerShareIntra.class);
	private PreparedStatement psDeleteStockIntrabyInterval;
	private PreparedStatement psDeleteStockIntraComplete;
	private PreparedStatement psInsShareIntra;
	private PreparedStatement psDeleteStockETFIntrabyInterval;
	private PreparedStatement psDeleteStockETFIntraComplete;
	private PreparedStatement psInsShareETFIntra;
	private PreparedStatement psDeleteStockRIIntrabyInterval;
	private PreparedStatement psDeleteStockRIIntraComplete;
	private PreparedStatement psInsShareRIIntra;
	private PreparedStatement psInsSpecialShareIntra;
	
	//Added for PTTP Project CRQ22398
	private PreparedStatement psInsOTCShareIntra;

	private int totalShareIntraCount = 0;
	private int totalSpecialShareIntraCount = 0;
	private int totalShareETFIntraCount = 0;
	private int totalShareRIIntraCount = 0;
	
	//Added for PTTP Project CRQ22398
	private int totalOTCShareIntraCount = 0;
	
	static long  lastUpdatedTime = 0;
	static long currentUpdatedTime = 0;
	
	
	public DBManagerShareIntra(String threadName) throws Exception {
		super(threadName);
		try {
			prepareStatements();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method will establish connection with Database. It can use either
	 * DataSource or JDBC connectivity
	 * 
	 * @param null
	 *
	 */
	public void openConnection() throws Exception {
		try {
			logger.debug("inside DB2Manager openConnection function");
	
			if (TipConstants.USE_DATA_SOURCE == 1) {
				OracleDataSource odc = new OracleDataSource();
				odc.setURL(TipConstants.DB_URL);
				odc.setUser(TipConstants.DB_USER);
				odc.setPassword(TipConstants.DB_PASS);
				odc.setDriverType("oci6");
				odc.setNetworkProtocol("ipc");
				g_oConnection = odc.getConnection();

				g_bClassLoaded = true;
			} else {
				// DB2 JDBC CONNECTION
				Class.forName(TipConstants.JDBC_DRIVER).newInstance();
				g_oConnection = DriverManager.getConnection(TipConstants.DB_URL, TipConstants.DB_USER,TipConstants.DB_PASS);
				g_bClassLoaded = true;
			}

			logger.debug("Exit DB2Manager openConnection function");
		} catch (Exception e) {

				logger.error("Error in DB2Manager in openConnection  method  while openConnection "+ e);
			throw e;
		}
	}

	/**
	 * This method will Initialize all the Prepared Statements respective to the
	 * available Threads
	 *
	 */
	public void prepareStatements() throws Exception {

		logger.debug("inside DB2Manager prepareStatements function");
		StringBuffer sbPrepareQueries = new StringBuffer();

		try {

			sbPrepareQueries.append("DELETE FROM WEB.HS_TE_STOCK_INTRA where TRANS_TIME < to_date(? , 'YYYY-MM-DD HH24:mi:SS') ");
			psDeleteStockIntrabyInterval = g_oConnection	.prepareStatement(sbPrepareQueries.toString());

			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("DELETE FROM WEB.HS_TE_STOCK_INTRA");
			psDeleteStockIntraComplete = g_oConnection	.prepareStatement(sbPrepareQueries.toString());

			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("INSERT INTO WEB.HS_TE_STOCK_INTRA(FK_TE_SYMBOL,TRANSACTION_DATE,LAST_TRADE_PRICE,"
							+ "LAST_TRADE_QUANTITY,LAST_UPDATE_TIME,ASK_PRICE,ASK_QUANTITY,BID_PRICE,BID_QUANTITY,NET_CHANGE,HIGH_PRICE,"
							+ "HIGH_52_WEEKS_PRICE,LOW_PRICE,LOW_52_WEEKS_PRICE,MARKET_CAP,NO_OF_TRADES,PBR,PER,PERCENT_CHANGE,"
							+ "PREVIOUS_CLOSE_PRICE,SEQUENCE_NO,TODAYS_CLOSE_PRICE,TODAYS_OPEN,TURNOVER,VOLUME_TRADED,YIELD,TRANS_TIME"
							+ ", HIGH_52_WEEKS_DATE,LOW_52_WEEKS_DATE"
							+ ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

			psInsShareIntra = g_oConnection.prepareStatement(sbPrepareQueries.toString());

			// Added fpr Share ETF
			if (sbPrepareQueries.length() > 0)
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("DELETE FROM WEB.HS_ETF_STOCK_INTRA where TRANS_TIME < to_date(? , 'YYYY-MM-DD HH24:mi:SS') ");
			psDeleteStockETFIntrabyInterval = g_oConnection.prepareStatement(sbPrepareQueries.toString());

			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("DELETE FROM WEB.HS_ETF_STOCK_INTRA");
			psDeleteStockETFIntraComplete = g_oConnection.prepareStatement(sbPrepareQueries.toString());

			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("INSERT INTO WEB.HS_ETF_STOCK_INTRA(FK_ETF_SYMBOL,TRANSACTION_DATE,LAST_TRADE_PRICE,"
							+ "LAST_TRADE_QUANTITY,LAST_UPDATE_TIME,ASK_PRICE,ASK_QUANTITY,BID_PRICE,BID_QUANTITY,NET_CHANGE,HIGH_PRICE,"
							+ "HIGH_52_WEEKS_PRICE,LOW_PRICE,LOW_52_WEEKS_PRICE,MARKET_CAP,NO_OF_TRADES,PBR,PER,PERCENT_CHANGE,"
							+ "PREVIOUS_CLOSE_PRICE,SEQUENCE_NO,TODAYS_CLOSE_PRICE,TODAYS_OPEN,TURNOVER,VOLUME_TRADED,YIELD,TRANS_TIME"
							+ ", HIGH_52_WEEKS_DATE,LOW_52_WEEKS_DATE"
							+ ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

			psInsShareETFIntra = g_oConnection.prepareStatement(sbPrepareQueries.toString());

			// Added fpr Share RI
			if (sbPrepareQueries.length() > 0)
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("DELETE FROM WEB.HS_RI_STOCK_INTRA where TRANS_TIME < to_date(? , 'YYYY-MM-DD HH24:mi:SS') ");
			psDeleteStockRIIntrabyInterval = g_oConnection.prepareStatement(sbPrepareQueries.toString());

			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("DELETE FROM WEB.HS_RI_STOCK_INTRA");
			psDeleteStockRIIntraComplete = g_oConnection.prepareStatement(sbPrepareQueries.toString());

			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("INSERT INTO WEB.HS_RI_STOCK_INTRA(FK_RI_SYMBOL,TRANSACTION_DATE,LAST_TRADE_PRICE,"
							+ "LAST_TRADE_QUANTITY,LAST_UPDATE_TIME,ASK_PRICE,ASK_QUANTITY,BID_PRICE,BID_QUANTITY,NET_CHANGE,HIGH_PRICE,"
							+ "HIGH_52_WEEKS_PRICE,LOW_PRICE,LOW_52_WEEKS_PRICE,MARKET_CAP,NO_OF_TRADES,PBR,PER,PERCENT_CHANGE,"
							+ "PREVIOUS_CLOSE_PRICE,SEQUENCE_NO,TODAYS_CLOSE_PRICE,TODAYS_OPEN,TURNOVER,VOLUME_TRADED,YIELD,TRANS_TIME"
							+ ", HIGH_52_WEEKS_DATE,LOW_52_WEEKS_DATE"
							+ ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

			psInsShareRIIntra = g_oConnection.prepareStatement(sbPrepareQueries	.toString());

			if (sbPrepareQueries.length() > 0)
				sbPrepareQueries.delete(0, sbPrepareQueries.length());

			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("INSERT INTO WEB.HS_STE_STOCK_INTRA(FK_STE_SYMBOL,TRANSACTION_DATE,LAST_TRADE_PRICE,"
							+ "LAST_TRADE_QUANTITY,LAST_UPDATE_TIME,ASK_PRICE,ASK_QUANTITY,BID_PRICE,BID_QUANTITY,NET_CHANGE,HIGH_PRICE,"
							+ "HIGH_52_WEEKS_PRICE,LOW_PRICE,LOW_52_WEEKS_PRICE,MARKET_CAP,NO_OF_TRADES,PBR,PER,PERCENT_CHANGE,"
							+ "PREVIOUS_CLOSE_PRICE,SEQUENCE_NO,TODAYS_CLOSE_PRICE,TODAYS_OPEN,TURNOVER,VOLUME_TRADED,YIELD,TRANS_TIME"
							+ ", HIGH_52_WEEKS_DATE,LOW_52_WEEKS_DATE"
							+ ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

			psInsSpecialShareIntra = g_oConnection.prepareStatement(sbPrepareQueries.toString());
			
			
			if (sbPrepareQueries.length() > 0)
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("INSERT INTO  WEB.OTC_COMPANIES_DATA  ( SYMBOL,  VOLUME_TRADED, TRADED_VALUE, LAST_PRICE, LAST_PRICE_UPDATED_DATE ) VALUES (?,?,?,?,?)");
			psInsOTCShareIntra = g_oConnection.prepareStatement(sbPrepareQueries.toString());
			
			
			logger.debug("Exit DB2Manager prepareStatements function");

		} catch (Exception e) {
			logger.error("Exception while preparing query ["
					+ sbPrepareQueries.toString() + "] is " + e);

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
					psInsShareIntra.executeBatch();
			} catch (BatchUpdateException be) {
				logger.error("BatchUpdateException in commitTrans() while doing InsShareIntra "	+ be);
				try {
					psInsShareIntra.clearBatch();
				} catch (SQLException e1) {
					logger.error("Exception in psInsShareIntra in clearBatch(): " + e1, e1);
				}
				/*
				while (be.getNextException() != null)
					logger.error("BatchUpdateException in commitTrans() while doing InsShareIntra "	+ be.getNextException());*/
			}			
			// Added for Share ETF
			try {				
					psInsShareETFIntra.executeBatch();				
			} catch (BatchUpdateException be) {
				logger.error("BatchUpdateException in commitTrans() while doing InsShareETFIntra "+ be);
				try {
					psInsShareETFIntra.clearBatch();
				} catch (SQLException e1) {
					logger.error("Exception in psInsShareETFIntra in clearBatch(): " + e1, e1);
				}
				/*while (be.getNextException() != null)
					logger.error("BatchUpdateException in commitTrans() while doing InsShareETFIntra "+ be.getNextException());*/
			}

			try {			
					psInsShareRIIntra.executeBatch();					
				
			} catch (BatchUpdateException be) {
				logger.error("BatchUpdateException in commitTrans() while doing InsShareRIIntra "	+ be);
				try {
					psInsShareRIIntra.clearBatch();
				} catch (SQLException e1) {
					logger.error("Exception in psInsShareRIIntra in clearBatch(): " + e1, e1);
				}
				/*while (be.getNextException() != null)
					logger.error("BatchUpdateException in commitTrans() while doing InsShareRIIntra "	+ be.getNextException());*/
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
			
			g_oConnection.commit();

		} catch (SQLException ex) {
			logger.error("SQL Exception in commitTrans() " + ex, ex);
		} finally {

		//	GUIHelper.shareIntraFrames = totalShareIntraCount;
		//	GUIHelper.shareIntraTime = new Timestamp(System.currentTimeMillis());
			System.gc();
		}

	}

	
	public void commitIntraTrans() {
		try {			
			
			g_oConnection.commit();

		} catch (SQLException ex) {
			logger.error("SQL Exception in commitTrans() " + ex, ex);
		} finally {

			System.gc();
		}

	}
	
	/**
	 * This method will close the existing DB Connection
	 */
	public void closeConnection() {
		try {
			logger.debug("inside  DB2Manager closeConnection function");
			
			if (psDeleteStockIntrabyInterval != null){
				psDeleteStockIntrabyInterval.close();
			}
			
			if (psDeleteStockIntraComplete != null){
				psDeleteStockIntraComplete.close();
			}
			
			if (psInsShareIntra != null){
				psInsShareIntra.close();
			}
			
			if (psDeleteStockETFIntrabyInterval != null){
				psDeleteStockETFIntrabyInterval.close();
			}
			
			if (psDeleteStockETFIntraComplete != null){
				psDeleteStockETFIntraComplete.close();
			}
			
			if (psInsShareETFIntra != null){
				psInsShareETFIntra.close();
			}
			
			if (psDeleteStockRIIntrabyInterval != null){
				psDeleteStockRIIntrabyInterval.close();
			}
			
			if (psDeleteStockRIIntraComplete != null){
				psDeleteStockRIIntraComplete.close();
			}
			
			if (psInsShareRIIntra != null){
				psInsShareRIIntra.close();
			}
			
			if (psInsSpecialShareIntra != null){
				psInsSpecialShareIntra.close();
			}
			
			if (psInsOTCShareIntra != null){
				psInsOTCShareIntra.close();
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
	 * WEB.HS_TE_STOCK_INTRA,WEB.HS_TE_INDEX_INTRA EFP.TE_MARKET,EFP.TE_INDEX
	 * ,EFP.TE_STOCK for Share Market
	 * 
	 * @param null
	 * @return boolean
	 */
	public boolean initializeDBforShareIntra() {

		try {
			logger.debug("inside DB2Manager initializeDBforShareIntra function");
			
			//logger.info(" LoadInitialData.dtShareIndexLastUpdateTime :"+LoadInitialData.dtShareIndexLastUpdateTime +" dtFeedReqDate :"+dtFeedReqDate);

			if (!LoadInitialData.dtShareIndexLastUpdateTime.equals(dtFeedReqDate)) {
				int numRowsDeleted = 0;
				/*int totMinutes = (TipConstants.DELETE_END_HOUR - TipConstants.DELETE_START_HOUR) * 60;
				int numRowsDeleted = 0, dltHr = TipConstants.DELETE_START_HOUR, dltMin = 0;
				String beginTime = "";
				String dateTime = LoadInitialData.strLastUpdDateForShareIndex.split("\\s+")[0]	+ " ";
				logger.debug("initializeDBforShareIntra: dtShareIndexLastUpdateTime "+ LoadInitialData.dtShareIndexLastUpdateTime	+ " DELETE_START_HOUR "	+ TipConstants.DELETE_START_HOUR
						+ " DELETE_END_HOUR "+ TipConstants.DELETE_END_HOUR	+ " DELETE_INTERVAL_MIN "	+ TipConstants.DELETE_INTERVAL_MIN);
				logger.debug("initializeDBforShareIntra: totalMinutes "	+ totMinutes);
				for (int i = 0; i <= totMinutes; i += TipConstants.DELETE_INTERVAL_MIN) {
					if (dltMin >= 10)
						beginTime = dltHr + ":" + dltMin + ":00";
					else
						beginTime = dltHr + ":0" + dltMin + ":00";

					psDeleteStockIntrabyInterval.setString(1, dateTime	+ beginTime);
					numRowsDeleted = numRowsDeleted+ psDeleteStockIntrabyInterval.executeUpdate();
					commitTrans();
					dltMin += TipConstants.DELETE_INTERVAL_MIN;
					if (dltMin >= 60) {
						dltMin -= 60;
						dltHr++;
					}
					logger.debug("Inside for loop --> initializeDBforShareIntra: deleted upto dateTime+beginTime "	+ dateTime	+ beginTime	+ " rows deleted "	+ numRowsDeleted);
				}*/
				numRowsDeleted =  psDeleteStockIntraComplete.executeUpdate();
				commitIntraTrans();
				logger.debug("initializeDBforShareIntra --> initializeDBforShareIntra: general delete  rows deleted "	+ numRowsDeleted);
				logger.info("initializeDBforShareIntra: Records deleted from WEB.HS_TE_STOCK_INTRA");
			}

			return true;

		} catch (SQLException e) {
			logger.error("Exception in initializeDBforShareIntra " + e, e);

		}

		logger.debug("Exit  DB2Manager initializeDBforShareIntra function");
		return false;
	}

	/**
	 * This method will initialize all the columns in the table
	 * WEB.HS_ETF_STOCK_INTRA,EFP.TE_STOCK for Share Market
	 * 
	 * @param null
	 * @return boolean
	 */
	public boolean initializeDBforShareETFIntra() {

		try {
			logger.debug("inside DB2Manager initializeDBforShareETFIntra function");

			/**
			 * To be anlysed for the variable dtShareIndexLastUpdateTime
			 */
			if (!LoadInitialData.dtShareIndexLastUpdateTime.equals(dtFeedReqDate)) {
				int numRowsDeleted = 0;
				/*int totMinutes = (TipConstants.DELETE_END_HOUR - TipConstants.DELETE_START_HOUR) * 60;
				int numRowsDeleted = 0, dltHr = TipConstants.DELETE_START_HOUR, dltMin = 0;
				String beginTime = "";
				String dateTime = LoadInitialData.strLastUpdDateForShareIndex + " ";
				logger.debug("initializeDBforShareRIIntra: dtShareIndexLastUpdateTime " + LoadInitialData.dtShareIndexLastUpdateTime	+ " DELETE_START_HOUR "+ TipConstants.DELETE_START_HOUR
						+ " DELETE_END_HOUR "+ TipConstants.DELETE_END_HOUR	+ " DELETE_INTERVAL_MIN "	+ TipConstants.DELETE_INTERVAL_MIN);
				logger.debug("initializeDBforShareETFIntra: totalMinutes "	+ totMinutes);
				for (int i = 0; i <= totMinutes; i += TipConstants.DELETE_INTERVAL_MIN) {
					if (dltMin >= 10)
						beginTime = dltHr + ":" + dltMin + ":00";
					else
						beginTime = dltHr + ":0" + dltMin + ":00";

					psDeleteStockETFIntrabyInterval.setString(1, dateTime	+ beginTime);

					numRowsDeleted = numRowsDeleted+ psDeleteStockETFIntrabyInterval.executeUpdate();
					commitTrans();
					dltMin += TipConstants.DELETE_INTERVAL_MIN;
					if (dltMin >= 60) {
						dltMin -= 60;
						dltHr++;
					}
					logger.debug("Inside for loop --> initializeDBforShareETFIntra: deleted upto dateTime+beginTime "	+ dateTime+ beginTime+ " rows deleted "+ numRowsDeleted);
				}*/
				numRowsDeleted = numRowsDeleted+ psDeleteStockETFIntraComplete.executeUpdate();
				commitIntraTrans();
				logger.debug("Outside for loop --> initializeDBforShareETFIntra: general delete rows deleted "+ numRowsDeleted);
				logger.info("initializeDBforShareETFIntra: Records deleted from WEB.HS_ETF_STOCK_INTRA");
			}

			return true;

		} catch (SQLException e) {
			logger.error("Exception in initializeDBforShareETFIntra " + e, e);

		}

		logger.debug("Exit  DB2Manager initializeDBforShareETFIntra function");
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
	public boolean initializeDBforShareRIIntra() {

		try {
			logger.debug("inside DB2Manager initializeDBforShareRIIntra function");

			/**
			 * To be anlysed for the variable dtShareIndexLastUpdateTime
			 */
			if (!LoadInitialData.dtShareIndexLastUpdateTime.equals(dtFeedReqDate)) {
				int numRowsDeleted = 0;
				/*int totMinutes = (TipConstants.DELETE_END_HOUR - TipConstants.DELETE_START_HOUR) * 60;
				int numRowsDeleted = 0, dltHr = TipConstants.DELETE_START_HOUR, dltMin = 0;
				String beginTime = "";
				String dateTime = LoadInitialData.strLastUpdDateForShareIndex + " ";
				logger.debug("initializeDBforShareRIIntra: dtShareIndexLastUpdateTime "	+ LoadInitialData.dtShareIndexLastUpdateTime	+ " DELETE_START_HOUR "	+ TipConstants.DELETE_START_HOUR
						+ " DELETE_END_HOUR "	+ TipConstants.DELETE_END_HOUR	+ " DELETE_INTERVAL_MIN "	+ TipConstants.DELETE_INTERVAL_MIN);
				logger.debug("initializeDBforShareRIIntra: totalMinutes " + totMinutes);
				for (int i = 0; i <= totMinutes; i += TipConstants.DELETE_INTERVAL_MIN) {
					if (dltMin >= 10)
						beginTime = dltHr + ":" + dltMin + ":00";
					else
						beginTime = dltHr + ":0" + dltMin + ":00";

					psDeleteStockRIIntrabyInterval.setString(1, dateTime+ beginTime);

					numRowsDeleted = numRowsDeleted+ psDeleteStockRIIntrabyInterval.executeUpdate();
					commitTrans();
					dltMin += TipConstants.DELETE_INTERVAL_MIN;
					if (dltMin >= 60) {
						dltMin -= 60;
						dltHr++;
					}
					logger.debug("Inside for loop --> initializeDBforShareRIIntra: deleted upto dateTime+beginTime "	+ dateTime	+ beginTime	+ " rows deleted "	+ numRowsDeleted);
				}*/
				numRowsDeleted = numRowsDeleted+ psDeleteStockRIIntraComplete.executeUpdate();
				commitIntraTrans();
				logger.debug("Outside for loop --> initializeDBforShareRIIntra: general delete  rows deleted "+ numRowsDeleted);
				logger.info("initializeDBforShareRIIntra: Records deleted from WEB.HS_RI_STOCK_INTRA");
			}

			return true;

		} catch (SQLException e) {
			logger.error("Exception in initializeDBforShareRIIntra " + e, e);

		}

		logger.debug("Exit  DB2Manager initializeDBforShareRIIntra function");
		return false;
	}

	/**
	 * This method will insert recored in to the table WEB.HS_TE_STOCK_INTRA
	 * 
	 * @param objShare
	 *            contains attributes of WEB.HS_TE_STOCK_INTRA table
	 * @return boolean
	 */
	public boolean insertShareIntraData(TIP_ShareVO objShare,boolean isCommit) {

		try {

			psInsShareIntra.setString(1, objShare.fk_te_symbol);
			psInsShareIntra.setTimestamp(2, objShare.transaction_date);
			psInsShareIntra.setDouble(3, objShare.last_trade_price);
			psInsShareIntra.setInt(4, objShare.last_trade_quantity);
			psInsShareIntra.setTimestamp(5, objShare.last_update_time);
			psInsShareIntra.setDouble(6, objShare.ask_price);
			psInsShareIntra.setLong(7, objShare.ask_quantity);
			psInsShareIntra.setDouble(8, objShare.bid_price);
			psInsShareIntra.setLong(9, objShare.bid_quantity);
			psInsShareIntra.setDouble(10, objShare.net_change);
			psInsShareIntra.setDouble(11, objShare.high_price);
			psInsShareIntra.setDouble(12, objShare.high_52_weeks_price);
			psInsShareIntra.setDouble(13, objShare.low_price);
			psInsShareIntra.setDouble(14, objShare.low_52_weeks_price);
			psInsShareIntra.setDouble(15, objShare.market_cap);
			psInsShareIntra.setLong(16, objShare.no_of_trades);
			psInsShareIntra.setDouble(17, objShare.Pbr);
			psInsShareIntra.setDouble(18, objShare.Per);
			psInsShareIntra.setDouble(19, objShare.percent_change);
			psInsShareIntra.setDouble(20, objShare.Previous_close_price);
			psInsShareIntra.setLong(21, objShare.sequence_no);
			psInsShareIntra.setDouble(22, objShare.todays_close_price);
			psInsShareIntra.setDouble(23, objShare.Todays_open);
			psInsShareIntra.setDouble(24, objShare.turnover);
			psInsShareIntra.setDouble(25, objShare.volume_traded);
			psInsShareIntra.setDouble(26, objShare.yield);
			psInsShareIntra.setTimestamp(27, objShare.transaction_date);

			psInsShareIntra.setDate(28, objShare.high_52_weeks_date);
			psInsShareIntra.setDate(29, objShare.low_52_weeks_date);
			
	/*		logger.info("**** insertShareIntraData*** objShare.fk_te_symbol:"+objShare.fk_te_symbol +" objShare.transaction_date :"+objShare.transaction_date +"objShare.last_trade_price :"+objShare.last_trade_price);
			logger.info("**** insertShareIntraData*** objShare.last_trade_quantity:"+objShare.last_trade_quantity +" objShare.last_update_time :"+objShare.last_update_time +"objShare.ask_price :"+objShare.ask_price);
			logger.info("**** insertShareIntraData*** objShare.ask_quantity:"+objShare.ask_quantity +" objShare.bid_price :"+objShare.bid_price +"objShare.bid_quantity :"+objShare.bid_quantity);
			logger.info("**** insertShareIntraData*** objShare.net_change:"+objShare.net_change +" objShare.high_price :"+objShare.high_price +"objShare.high_52_weeks_price :"+objShare.high_52_weeks_price);
			logger.info("**** insertShareIntraData*** objShare.low_price:"+objShare.low_price +" objShare.low_52_weeks_price :"+objShare.low_52_weeks_price +"objShare.no_of_trades :"+objShare.no_of_trades);
			logger.info("**** insertShareIntraData*** objShare.net_change:"+objShare.net_change +" objShare.high_price :"+objShare.high_price +"objShare.high_52_weeks_price :"+objShare.high_52_weeks_price);
			logger.info("**** insertShareIntraData*** objShare.Pbr:"+objShare.Pbr +" objShare.Per :"+objShare.Per +"objShare.percent_change :"+objShare.percent_change+" objShare.Previous_close_price :"+objShare.Previous_close_price +"objShare.sequence_no :"+objShare.sequence_no);
			logger.info("**** insertShareIntraData*** objShare.todays_close_price:"+objShare.todays_close_price +" objShare.Todays_open :"+objShare.Todays_open +"objShare.turnover :"+objShare.turnover+" objShare.volume_traded :"+objShare.volume_traded +"objShare.yield :"+objShare.yield);
*/
			
			psInsShareIntra.addBatch();
			
			currentUpdatedTime =  System.currentTimeMillis();
			
			if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.WRITER_THREADS_DB_UPDATE_INTERVAL )
			{
			//if (objBond.isEndOfBatch){
				psInsShareIntra.executeBatch();
				lastUpdatedTime = currentUpdatedTime;
			}
			
			totalShareIntraCount++;								
			logger.debug("DBManagerShareIntra: psInsShareIntra :"+totalShareIntraCount);
			
	
			return true;

		}catch (BatchUpdateException be2) {
			logger.error("BatchUpdateException in commitTrans() while doing psInsShareIntra "+ be2);
			try {
				psInsShareIntra.clearBatch();
			} catch (SQLException e1) {
				logger.error("Exception in psInsShareIntra in clearBatch(): " + e1, e1);
			}			
		} catch (SQLException e) {			
			logger.error("Exception in insertShareIntraData: " + e, e);

		}

		if (objShare != null)
			objShare = null;
		return false;
	}

	/**
	 * This method will insert recored in to the table WEB.HS_TE_STOCK_INTRA
	 * 
	 * @param objShare
	 *            contains attributes of WEB.HS_TE_STOCK_INTRA table
	 * @return boolean
	 */
	public boolean insertSpecialShareIntraData(TIP_ShareVO objShare,boolean isCommit) {

		try {

			logger.info("insertSpecialShareIntraData :"+ objShare.fk_te_symbol);
			psInsSpecialShareIntra.setString(1, objShare.fk_te_symbol);
			psInsSpecialShareIntra.setTimestamp(2, objShare.transaction_date);
			psInsSpecialShareIntra.setDouble(3, objShare.last_trade_price);
			psInsSpecialShareIntra.setInt(4, objShare.last_trade_quantity);
			psInsSpecialShareIntra.setTimestamp(5, objShare.last_update_time);
			psInsSpecialShareIntra.setDouble(6, objShare.ask_price);
			psInsSpecialShareIntra.setLong(7, objShare.ask_quantity);
			psInsSpecialShareIntra.setDouble(8, objShare.bid_price);
			psInsSpecialShareIntra.setLong(9, objShare.bid_quantity);
			psInsSpecialShareIntra.setDouble(10, objShare.net_change);
			psInsSpecialShareIntra.setDouble(11, objShare.high_price);
			psInsSpecialShareIntra.setDouble(12, objShare.high_52_weeks_price);
			psInsSpecialShareIntra.setDouble(13, objShare.low_price);
			psInsSpecialShareIntra.setDouble(14, objShare.low_52_weeks_price);
			psInsSpecialShareIntra.setDouble(15, objShare.market_cap);
			psInsSpecialShareIntra.setLong(16, objShare.no_of_trades);
			psInsSpecialShareIntra.setDouble(17, objShare.Pbr);
			psInsSpecialShareIntra.setDouble(18, objShare.Per);
			psInsSpecialShareIntra.setDouble(19, objShare.percent_change);
			psInsSpecialShareIntra.setDouble(20, objShare.Previous_close_price);
			psInsSpecialShareIntra.setLong(21, objShare.sequence_no);
			psInsSpecialShareIntra.setDouble(22, objShare.todays_close_price);
			psInsSpecialShareIntra.setDouble(23, objShare.Todays_open);
			psInsSpecialShareIntra.setDouble(24, objShare.turnover);
			psInsSpecialShareIntra.setDouble(25, objShare.volume_traded);
			psInsSpecialShareIntra.setDouble(26, objShare.yield);
			psInsSpecialShareIntra.setTimestamp(27, objShare.transaction_date);

			psInsSpecialShareIntra.setDate(28, objShare.high_52_weeks_date);
			psInsSpecialShareIntra.setDate(29, objShare.low_52_weeks_date);

			psInsSpecialShareIntra.addBatch();
			
			//psInsSpecialShareIntra.execute();
			
			currentUpdatedTime =  System.currentTimeMillis();
			
			if( isCommit ||(currentUpdatedTime-lastUpdatedTime)>TipConstants.WRITER_THREADS_DB_UPDATE_INTERVAL )
			{
			//if (objBond.isEndOfBatch){
				logger.info("insertSpecialShareIntraData commit completed :"+ objShare.fk_te_symbol);
				psInsSpecialShareIntra.executeBatch();
				lastUpdatedTime = currentUpdatedTime;
				logger.info("insertSpecialShareIntraData commit completed :"+ objShare.fk_te_symbol);
			}
			
			totalSpecialShareIntraCount++;								
			logger.debug("DBManagerShareIntra: psInsSpecialShareIntra :"+totalSpecialShareIntraCount);
		
			return true;

		} catch (BatchUpdateException be2) {
			logger.error("BatchUpdateException in commitTrans() while doing psInsSpecialShareIntra "+ be2);
			try {
				psInsSpecialShareIntra.clearBatch();
			} catch (SQLException e1) {
				logger.error("Exception in psInsSpecialShareIntra in clearBatch(): " + e1, e1);
			}			
		}catch (SQLException e) {			
			logger.error("Exception in insertShareIntraData: " + e, e);
		}

		if (objShare != null)
			objShare = null;
		return false;
	}

	/**
	 * This method will insert recored in to the table WEB.HS_TE_STOCK_INTRA
	 * 
	 * @param objShare
	 *            contains attributes of WEB.HS_TE_STOCK_INTRA table
	 * @return boolean
	 */
	// Added for ShareRI
	public boolean insertShareETFIntraData(TIP_ShareVO objShare,boolean isCommit) {

		try {

			psInsShareETFIntra.setString(1, objShare.fk_te_symbol);
			psInsShareETFIntra.setTimestamp(2, objShare.transaction_date);
			psInsShareETFIntra.setDouble(3, objShare.last_trade_price);
			psInsShareETFIntra.setInt(4, objShare.last_trade_quantity);
			psInsShareETFIntra.setTimestamp(5, objShare.last_update_time);
			psInsShareETFIntra.setDouble(6, objShare.ask_price);
			psInsShareETFIntra.setLong(7, objShare.ask_quantity);
			psInsShareETFIntra.setDouble(8, objShare.bid_price);
			psInsShareETFIntra.setLong(9, objShare.bid_quantity);
			psInsShareETFIntra.setDouble(10, objShare.net_change);
			psInsShareETFIntra.setDouble(11, objShare.high_price);
			psInsShareETFIntra.setDouble(12, objShare.high_52_weeks_price);
			psInsShareETFIntra.setDouble(13, objShare.low_price);
			psInsShareETFIntra.setDouble(14, objShare.low_52_weeks_price);
			psInsShareETFIntra.setDouble(15, objShare.market_cap);
			psInsShareETFIntra.setLong(16, objShare.no_of_trades);
			psInsShareETFIntra.setDouble(17, objShare.Pbr);
			psInsShareETFIntra.setDouble(18, objShare.Per);
			psInsShareETFIntra.setDouble(19, objShare.percent_change);
			psInsShareETFIntra.setDouble(20, objShare.Previous_close_price);
			psInsShareETFIntra.setLong(21, objShare.sequence_no);
			psInsShareETFIntra.setDouble(22, objShare.todays_close_price);
			psInsShareETFIntra.setDouble(23, objShare.Todays_open);
			psInsShareETFIntra.setDouble(24, objShare.turnover);
			psInsShareETFIntra.setDouble(25, objShare.volume_traded);
			psInsShareETFIntra.setDouble(26, objShare.yield);
			psInsShareETFIntra.setTimestamp(27, objShare.transaction_date);

			psInsShareETFIntra.setDate(28, objShare.high_52_weeks_date);
			psInsShareETFIntra.setDate(29, objShare.low_52_weeks_date);			
			psInsShareETFIntra.addBatch();			
			currentUpdatedTime =  System.currentTimeMillis();
			
			if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.WRITER_THREADS_DB_UPDATE_INTERVAL )
			{
			//if (objBond.isEndOfBatch){
				psInsShareETFIntra.executeBatch();
				lastUpdatedTime = currentUpdatedTime;
			}
			
			totalShareETFIntraCount++;								
			logger.debug("DBManagerShareIntra: psInsShareETFIntra :"+totalShareETFIntraCount);
			
	
			return true;

		} catch (BatchUpdateException be2) {
			logger.error("BatchUpdateException in commitTrans() while doing psInsShareETFIntra "+ be2);
			try {
				psInsShareETFIntra.clearBatch();
			} catch (SQLException e1) {
				logger.error("Exception in psInsShareETFIntra in clearBatch(): " + e1, e1);
			}			
		} catch (SQLException e) {			
			logger.error("Exception in insertShareETFIntraData: " + e, e);

		}

		if (objShare != null)
			objShare = null;
		return false;
	}

	/**
	 * This method will insert recored in to the table WEB.HS_TE_STOCK_INTRA
	 * 
	 * @param objShare
	 *            contains attributes of WEB.HS_TE_STOCK_INTRA table
	 * @return boolean
	 */
	// Added for ShareRI
	public boolean insertShareRIIntraData(TIP_ShareVO objShare,boolean isCommit) {

		try {

			psInsShareRIIntra.setString(1, objShare.fk_te_symbol);
			psInsShareRIIntra.setTimestamp(2, objShare.transaction_date);
			psInsShareRIIntra.setDouble(3, objShare.last_trade_price);
			psInsShareRIIntra.setInt(4, objShare.last_trade_quantity);
			psInsShareRIIntra.setTimestamp(5, objShare.last_update_time);
			psInsShareRIIntra.setDouble(6, objShare.ask_price);
			psInsShareRIIntra.setLong(7, objShare.ask_quantity);
			psInsShareRIIntra.setDouble(8, objShare.bid_price);
			psInsShareRIIntra.setLong(9, objShare.bid_quantity);
			psInsShareRIIntra.setDouble(10, objShare.net_change);
			psInsShareRIIntra.setDouble(11, objShare.high_price);
			psInsShareRIIntra.setDouble(12, objShare.high_52_weeks_price);
			psInsShareRIIntra.setDouble(13, objShare.low_price);
			psInsShareRIIntra.setDouble(14, objShare.low_52_weeks_price);
			psInsShareRIIntra.setDouble(15, objShare.market_cap);
			psInsShareRIIntra.setLong(16, objShare.no_of_trades);
			psInsShareRIIntra.setDouble(17, objShare.Pbr);
			psInsShareRIIntra.setDouble(18, objShare.Per);
			psInsShareRIIntra.setDouble(19, objShare.percent_change);
			psInsShareRIIntra.setDouble(20, objShare.Previous_close_price);
			psInsShareRIIntra.setLong(21, objShare.sequence_no);
			psInsShareRIIntra.setDouble(22, objShare.todays_close_price);
			psInsShareRIIntra.setDouble(23, objShare.Todays_open);
			psInsShareRIIntra.setDouble(24, objShare.turnover);
			psInsShareRIIntra.setDouble(25, objShare.volume_traded);
			psInsShareRIIntra.setDouble(26, objShare.yield);
			psInsShareRIIntra.setTimestamp(27, objShare.transaction_date);

			psInsShareRIIntra.setDate(28, objShare.high_52_weeks_date);
			psInsShareRIIntra.setDate(29, objShare.low_52_weeks_date);

			psInsShareRIIntra.addBatch();			
			currentUpdatedTime =  System.currentTimeMillis();
			
			if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.WRITER_THREADS_DB_UPDATE_INTERVAL )
			{
			//if (objBond.isEndOfBatch){
				psInsShareRIIntra.executeBatch();
				lastUpdatedTime = currentUpdatedTime;
			}
			
			totalShareRIIntraCount++;								
			logger.debug("DBManagerShareIntra: psInsShareRIIntra :"+totalShareRIIntraCount);
			
	
			return true;

		}catch (BatchUpdateException be2) {
			logger.error("BatchUpdateException in commitTrans() while doing psInsShareRIIntra "+ be2);
			try {
				psInsShareRIIntra.clearBatch();
			} catch (SQLException e1) {
				logger.error("Exception in psInsShareRIIntra in clearBatch(): " + e1, e1);
			}			
		} 
		catch (SQLException e) {			
			logger.error("Exception in insertShareRIIntraData: " + e, e);
		}

		if (objShare != null)
			objShare = null;

		return false;
	}

	
	
	/**
	 * This method will insert recored in to the table WEB.OTC_COMPANIES_DATA
	 * 
	 * @param objShare
	 *            contains attributes of WEB.OTC_COMPANIES_DATA table
	 * @return boolean
	 */
	 //Added for PTTP Project CRQ22398
	public boolean insertOTCShareIntraData(TIP_ShareVO objShare,boolean isCommit) {

		try {
			
			logger.debug("Entry() - insertOTCShareIntraData() - Symbol :"+objShare.fk_te_symbol);
			psInsOTCShareIntra.setString(1, objShare.fk_te_symbol);
			psInsOTCShareIntra.setLong(2, objShare.volume_traded);
			psInsOTCShareIntra.setDouble(3, objShare.turnover);
			psInsOTCShareIntra.setDouble(4, objShare.last_trade_price);
			psInsOTCShareIntra.setTimestamp(5, objShare.last_update_time);
						
			psInsOTCShareIntra.execute();
			
			totalOTCShareIntraCount++;								
			logger.debug("DBManagerShareIntra: insertOTCShareIntraData :"+totalOTCShareIntraCount);
				
			logger.debug("Exit() - insertOTCShareIntraData() - Symbol :"+objShare.fk_te_symbol);
			
			return true;

		}
		catch (SQLException e) {			
			logger.error("Exception in insertOTCShareIntraData: " + e, e);
		}

		if (objShare != null)
			objShare = null;

		return false;
	}
	
	
}
