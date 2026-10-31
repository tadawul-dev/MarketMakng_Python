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
import sa.com.tadawul.website.tip.vo.TIP_BondVO;


/**
 * @description This class is used to get Database Connection and all the
 *              prepared Statements.
 *
 */
public class DBManagerBondIntra extends DBManager {

	private static final Logger logger = LogManager.getLogger(DBManagerBondIntra.class);
	private PreparedStatement psDeleteBondIntrabyInterval;
	private PreparedStatement psDeleteBondIntraComplete;
	private PreparedStatement psInsBondIntra;
	
	//Added for Sukuk & Bonds Negotiated deals
	private PreparedStatement psInsSpecialBondIntra;
	
	//Added for PTTP Project
	private PreparedStatement psDeleteOTCBondIntraComplete;
	private PreparedStatement psInsOTCBondIntra;

	static long  lastUpdatedTime = 0;
	static long currentUpdatedTime = 0;
	private int totalBondIntraCount = 0;
	private int totalFISpecialShareIntraCount = 0;
	
	public DBManagerBondIntra(String threadName) throws Exception {
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

			sbPrepareQueries.append("DELETE FROM WEB.HS_FI_STOCK_INTRA where to_date(to_char(TRANSACTION_DATE,'YYYY-MM-DD HH24:mi:SS'),'YYYY-MM-DD HH24:mi:SS') <  to_date(?,'YYYY-MM-DD HH24:mi:SS') ");
			psDeleteBondIntrabyInterval = g_oConnection.prepareStatement(sbPrepareQueries.toString());

			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("DELETE FROM WEB.HS_FI_STOCK_INTRA");
			psDeleteBondIntraComplete = g_oConnection.prepareStatement(sbPrepareQueries.toString());
			
			//Added for PTTP
			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("DELETE FROM WEB.HS_FI_STOCK_INTRA_UNLISTED");
			psDeleteOTCBondIntraComplete = g_oConnection.prepareStatement(sbPrepareQueries.toString());

			//Added for Bonds Yield calculations projects CRQ21157	
			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("INSERT INTO WEB.HS_FI_STOCK_INTRA(FK_FI_SYMBOL,TRANSACTION_DATE,LAST_TRADE_PRICE,"
							+ "LAST_TRADE_QUANTITY,LAST_UPDATE_TIME,ASK_PRICE,ASK_QUANTITY,BID_PRICE,BID_QUANTITY,NET_CHANGE,HIGH_PRICE,"
							+ "HIGH_52_WEEKS_PRICE,LOW_PRICE,LOW_52_WEEKS_PRICE,NO_OF_TRADES,PERCENT_CHANGE,"
							+ "PREVIOUS_CLOSE_PRICE,TODAYS_CLOSE_PRICE,TODAYS_OPEN_PRICE,TURNOVER,VOLUME_TRADED"
							+ ",HIGH_52_WEEKS_DATE,LOW_52_WEEKS_DATE, TRADE_YIELD, BEST_BID_YIELD,BEST_OFFER_YIELD "
							+ ") VALUES"
							+ "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

			psInsBondIntra = g_oConnection.prepareStatement(sbPrepareQueries	.toString());
			
			
			//Added for PTTPproject	
			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("INSERT INTO WEB.HS_FI_STOCK_INTRA_UNLISTED(FK_FI_SYMBOL,TRANSACTION_DATE,LAST_TRADE_PRICE,"
							+ "LAST_TRADE_QUANTITY,LAST_UPDATE_TIME,ASK_PRICE,ASK_QUANTITY,BID_PRICE,BID_QUANTITY,NET_CHANGE,HIGH_PRICE,"
							+ "HIGH_52_WEEKS_PRICE,LOW_PRICE,LOW_52_WEEKS_PRICE,NO_OF_TRADES,PERCENT_CHANGE,"
							+ "PREVIOUS_CLOSE_PRICE,TODAYS_CLOSE_PRICE,TODAYS_OPEN_PRICE,TURNOVER,VOLUME_TRADED"
							+ ",HIGH_52_WEEKS_DATE,LOW_52_WEEKS_DATE, TRADE_YIELD, BEST_BID_YIELD,BEST_OFFER_YIELD "
							+ ") VALUES"
							+ "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

			psInsOTCBondIntra = g_oConnection.prepareStatement(sbPrepareQueries	.toString());
			
			// Sukuk & Bonds special stock
			if (sbPrepareQueries.length() > 0)
				sbPrepareQueries.delete(0, sbPrepareQueries.length());				
				sbPrepareQueries.append("INSERT INTO  WEB.HS_STE_FI_INTRA(FK_FI_SYMBOL ,TRANSACTION_DATE,LAST_TRADE_PRICE,"
										+ "LAST_TRADE_QUANTITY,LAST_UPDATE_TIME,ASK_PRICE,ASK_QUANTITY,BID_PRICE,BID_QUANTITY,NET_CHANGE,HIGH_PRICE,"
										+ "HIGH_52_WEEKS_PRICE,LOW_PRICE,LOW_52_WEEKS_PRICE,NO_OF_TRADES,PERCENT_CHANGE,"
										+ "PREVIOUS_CLOSE_PRICE,SEQUENCE_NO,TODAYS_CLOSE_PRICE,TODAYS_OPEN,TURNOVER,VOLUME_TRADED,YIELD,TRANS_TIME"
										+ ", HIGH_52_WEEKS_DATE,LOW_52_WEEKS_DATE, ISIN, TRADE_YIELD, BEST_BID_YIELD, BEST_OFFER_YIELD"
										+ ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

				psInsSpecialBondIntra = g_oConnection.prepareStatement(sbPrepareQueries.toString());
			
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
			try {
				
				currentUpdatedTime =  System.currentTimeMillis();
				if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.WRITER_THREADS_DB_UPDATE_INTERVAL )
				{	
					psInsBondIntra.executeBatch();
					logger.info("DBManagerBondIntra Batch Updated");
					
					psInsOTCBondIntra.executeBatch();
					logger.info("DBManagerBondIntra Batch Updated");
					
					psInsSpecialBondIntra.executeBatch();
					logger.info("DBManagerBondIntra Batch for SpecialTrades Updated");
					
					lastUpdatedTime = currentUpdatedTime;					
				}
				

			} catch (BatchUpdateException be2) {
				logger.error("BatchUpdateException in commitTrans() while doing InsBondIntra "+ be2);
				try {
					psInsBondIntra.clearBatch();
				} catch (SQLException e1) {
					logger.error("Exception in psInsBondIntra in clearBatch(): " + e1, e1);
				}
				/*while (be2.getNextException() != null)
					logger.error("BatchUpdateException in commitTrans() while doing InsBondIntra "+ be2.getNextException());*/
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

			if (psDeleteBondIntrabyInterval != null){
				psDeleteBondIntrabyInterval.close();
			}
			
			if (psDeleteBondIntraComplete != null){
				psDeleteBondIntraComplete.close();
			}
			
			if (psDeleteOTCBondIntraComplete != null){
				psDeleteOTCBondIntraComplete.close();
			}
			if (psInsBondIntra != null){
				psInsBondIntra.close();
			}
			
			if (psInsOTCBondIntra != null){
				psInsOTCBondIntra.close();
			}
			
			if (psInsSpecialBondIntra != null){
				psInsSpecialBondIntra.close();
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
	 * WEB.HS_FI_STOCK_INTRA, EFP.TE_MARKET, EFP.FI_STOCK for Bond Market
	 * 
	 * @param null
	 * @return boolean
	 */
	public boolean initializeDBforBondIntra() {

		try {
			logger.debug("inside  DB2Manager initializeDBforBondIntra function");
			if (!LoadInitialData.dtBondLastUpdateTime.equals(dtFeedReqDate)) {
				int numRowsDeleted = 0;
				
				
				numRowsDeleted = numRowsDeleted	+ psDeleteBondIntraComplete.executeUpdate();
				commitIntraTrans();
				if (logger.isDebugEnabled()) logger.debug("Outside for loop --> initializeDBforBondIntra: deleted upto dateTime+beginTime rows deleted {}", numRowsDeleted);
				logger.info("initializeDBforBondIntra: Records deleted from WEB.HS_FI_STOCK_INTRA");
			}
			return true;

		} catch (SQLException e) {

			logger.error("Exception in initializeDBforBondIntra  Version: " + e
					+ getStackTrace(e));

		}

		return false;
	}

	
	/**
	 * This method will initialize all the columns in the table
	 * WEB.HS_FI_STOCK_INTRA, EFP.TE_MARKET, EFP.FI_STOCK for Bond Market
	 * 
	 * @param null
	 * @return boolean
	 */
	public boolean initializeDBforOTCBondIntra() {

		try {
			logger.debug("inside  DB2Manager initializeDBforOTCBondIntra function");
			if (!LoadInitialData.dtBondLastUpdateTime.equals(dtFeedReqDate)) {
				int numRowsDeleted = 0;
				
				
				numRowsDeleted = numRowsDeleted	+ psDeleteOTCBondIntraComplete.executeUpdate();
				commitIntraTrans();
				if (logger.isDebugEnabled()) logger.debug("Outside for loop --> initializeDBforOTCBondIntra: deleted upto dateTime+beginTime rows deleted {}", numRowsDeleted);
				logger.info("initializeDBforOTCBondIntra: Records deleted from WEB.HS_FI_STOCK_INTRA_UNLISTED");
			}
			return true;

		} catch (SQLException e) {

			logger.error("Exception in initializeDBforBondIntra  Version: " + e
					+ getStackTrace(e));

		}

		return false;
	}
	
	
	/**
	 * This method will insert record into the table WEB.HS_FI_STOCK_INTRA
	 * 
	 * @param objBond
	 *            contains attributes of WEB.HS_FI_STOCK_INTRA table
	 * @return boolean
	 */
	public boolean insertBondIntraData(TIP_BondVO objBond,boolean isCommit) {

		try {

			psInsBondIntra.setString(1, objBond.fk_fi_symbol);
			psInsBondIntra.setTimestamp(2, objBond.transaction_date);
			psInsBondIntra.setDouble(3, objBond.last_trade_price);
			psInsBondIntra.setInt(4, objBond.last_trade_quantity);
			psInsBondIntra.setTimestamp(5, objBond.last_update_time);
			psInsBondIntra.setDouble(6, objBond.ask_price);
			psInsBondIntra.setLong(7, objBond.ask_quantity);
			psInsBondIntra.setDouble(8, objBond.bid_price);
			psInsBondIntra.setLong(9, objBond.bid_quantity);
			psInsBondIntra.setDouble(10, objBond.net_change);
			psInsBondIntra.setDouble(11, objBond.high_price);
			psInsBondIntra.setDouble(12, objBond.high_52_weeks_price);
			psInsBondIntra.setDouble(13, objBond.low_price);
			psInsBondIntra.setDouble(14, objBond.low_52_weeks_price);
			psInsBondIntra.setLong(15, objBond.no_of_trades);
			psInsBondIntra.setDouble(16, objBond.percent_change);
			psInsBondIntra.setDouble(17, objBond.Previous_close_price);
			psInsBondIntra.setDouble(18, objBond.todays_close_price);
			psInsBondIntra.setDouble(19, objBond.Todays_open_price);
			psInsBondIntra.setDouble(20, objBond.turnover);
			psInsBondIntra.setDouble(21, objBond.volume_traded);

			psInsBondIntra.setDate(22, objBond.high_52_weeks_date);
			psInsBondIntra.setDate(23, objBond.low_52_weeks_date);
			
			//Added for Bonds Yield calculations projects CRQ21157	
			psInsBondIntra.setDouble(24, objBond.traded_yield);
			psInsBondIntra.setDouble(25, objBond.bestBid_yield);
			psInsBondIntra.setDouble(26, objBond.bestOffer_yield);
			
			psInsBondIntra.addBatch();
			
			currentUpdatedTime =  System.currentTimeMillis();
			if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.WRITER_THREADS_DB_UPDATE_INTERVAL )
			{

				psInsBondIntra.executeBatch();
				logger.debug("DBManagerBondIntra Batch Updated");
				lastUpdatedTime = currentUpdatedTime;				
			}
			totalBondIntraCount++;

			if (logger.isDebugEnabled()) logger.debug("DBManagerBondIntra: updateIndexIntraData :{}", totalBondIntraCount);
			return true;

		}catch(BatchUpdateException e) 
		{
			logger.error("Exception in psInsBondIntra in BatchUpdateException(): " + e, e);
			try {
				psInsBondIntra.clearBatch();
			} catch (SQLException e1) {
				logger.error("Exception in psInsBondIntra in clearBatch(): " + e1, e1);
			}
		} 
		catch (SQLException e) {
			logger.error("Exception in insertBondIntraData: " + e, e);

		}

		if (objBond != null)
			objBond = null;
		return false;
	}

	
	  //Added for Sukuk & Bonds Negotiated deals
	public boolean insertSpecialFIShareIntraData(TIP_BondVO objBond,boolean isCommit) {

		try {

			if (logger.isDebugEnabled()) logger.debug("insertSpecialFIShareIntraData called with bond_id :{} objBond.net_change :{}", objBond.fk_fi_symbol, objBond.net_change);
			psInsSpecialBondIntra.setString(1, objBond.fk_fi_symbol);
			psInsSpecialBondIntra.setTimestamp(2, objBond.transaction_date);
			psInsSpecialBondIntra.setDouble(3, objBond.last_trade_price);
			psInsSpecialBondIntra.setInt(4, objBond.last_trade_quantity);
			psInsSpecialBondIntra.setTimestamp(5, objBond.last_update_time);
			psInsSpecialBondIntra.setDouble(6, objBond.ask_price);
			psInsSpecialBondIntra.setLong(7, objBond.ask_quantity);
			psInsSpecialBondIntra.setDouble(8, objBond.bid_price);
			psInsSpecialBondIntra.setLong(9, objBond.bid_quantity);
			psInsSpecialBondIntra.setDouble(10, objBond.net_change);
			psInsSpecialBondIntra.setDouble(11, objBond.high_price);
			psInsSpecialBondIntra.setDouble(12,objBond.high_52_weeks_price);
			psInsSpecialBondIntra.setDouble(13, objBond.low_price);
			psInsSpecialBondIntra.setDouble(14, objBond.low_52_weeks_price);			
			psInsSpecialBondIntra.setLong(15, objBond.no_of_trades);		
			psInsSpecialBondIntra.setDouble(16, objBond.percent_change);
			psInsSpecialBondIntra.setDouble(17,objBond.Previous_close_price);
			psInsSpecialBondIntra.setLong(18, objBond.sequence_no);
			psInsSpecialBondIntra.setDouble(19, objBond.todays_close_price);
			psInsSpecialBondIntra.setDouble(20, objBond.Todays_open_price);
			psInsSpecialBondIntra.setDouble(21, objBond.turnover);
			psInsSpecialBondIntra.setDouble(22, objBond.volume_traded);
			psInsSpecialBondIntra.setDouble(23, objBond.traded_yield);
			psInsSpecialBondIntra.setTimestamp(24,objBond.transaction_date);
			psInsSpecialBondIntra.setDate(25, objBond.high_52_weeks_date);
			psInsSpecialBondIntra.setDate(26, objBond.low_52_weeks_date);
			psInsSpecialBondIntra.setString(27, objBond.isin);
			psInsSpecialBondIntra.setDouble(28, objBond.traded_yield);
			psInsSpecialBondIntra.setDouble(29, objBond.bestBid_yield);
			psInsSpecialBondIntra.setDouble(30, objBond.bestOffer_yield);
			
			
			
			psInsSpecialBondIntra.addBatch();			
			currentUpdatedTime =  System.currentTimeMillis();
			if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.WRITER_THREADS_DB_UPDATE_INTERVAL  )
			{
			//if (objIndex.isEndOfBatch)
				psInsSpecialBondIntra.executeBatch();
				logger.debug("DBManagerFIShareIntra Batch Updated");
				lastUpdatedTime = currentUpdatedTime;				
			}
			totalFISpecialShareIntraCount++;			
	
			if (logger.isDebugEnabled()) logger.debug("DBManagerFIShareIntra: insertSpecialFIShareIntraData :{}", totalFISpecialShareIntraCount);
			return true;

		} catch (BatchUpdateException be) {
			logger.error("BatchUpdateException in commitTrans() while doing insertSpecialFIShareIntraData "	+ be);
			try {
				psInsSpecialBondIntra.clearBatch();
			} catch (SQLException e1) {
				logger.error("Exception in insertSpecialFIShareIntraData in clearBatch(): " + e1, e1);
			}			
		} catch (SQLException e) {
			try {
				psInsSpecialBondIntra.clearBatch();
			} catch (SQLException e1) {
				logger.error("Exception in insertSpecialFIShareIntraData in clearBatch(): " + e, e);
			}
			logger.error("Exception in insertSpecialFIShareIntraData: " + e, e);

		}

		if (objBond != null)
			objBond = null;
		return false;
	}
	
	
	
	/**
	 * This method will insert record into the table WEB.HS_FI_STOCK_INTRA
	 * 
	 * @param objBond
	 *            contains attributes of WEB.HS_FI_STOCK_INTRA table
	 * @return boolean
	 */
	public boolean insertOTCBondIntraData(TIP_BondVO objBond,boolean isCommit) {

		try {
			
			if (logger.isDebugEnabled()) logger.debug("insertOTCBondIntraData is called :{}", objBond.fk_fi_symbol);
			
			psInsOTCBondIntra.setString(1, objBond.fk_fi_symbol);
			psInsOTCBondIntra.setTimestamp(2, objBond.transaction_date);
			psInsOTCBondIntra.setDouble(3, objBond.last_trade_price);
			psInsOTCBondIntra.setInt(4, objBond.last_trade_quantity);
			psInsOTCBondIntra.setTimestamp(5, objBond.last_update_time);
			psInsOTCBondIntra.setDouble(6, objBond.ask_price);
			psInsOTCBondIntra.setLong(7, objBond.ask_quantity);
			psInsOTCBondIntra.setDouble(8, objBond.bid_price);
			psInsOTCBondIntra.setLong(9, objBond.bid_quantity);
			psInsOTCBondIntra.setDouble(10, objBond.net_change);
			psInsOTCBondIntra.setDouble(11, objBond.high_price);
			psInsOTCBondIntra.setDouble(12, objBond.high_52_weeks_price);
			psInsOTCBondIntra.setDouble(13, objBond.low_price);
			psInsOTCBondIntra.setDouble(14, objBond.low_52_weeks_price);
			psInsOTCBondIntra.setLong(15, objBond.no_of_trades);
			psInsOTCBondIntra.setDouble(16, objBond.percent_change);
			psInsOTCBondIntra.setDouble(17, objBond.Previous_close_price);
			psInsOTCBondIntra.setDouble(18, objBond.todays_close_price);
			psInsOTCBondIntra.setDouble(19, objBond.Todays_open_price);
			psInsOTCBondIntra.setDouble(20, objBond.turnover);
			psInsOTCBondIntra.setDouble(21, objBond.volume_traded);

			psInsOTCBondIntra.setDate(22, objBond.high_52_weeks_date);
			psInsOTCBondIntra.setDate(23, objBond.low_52_weeks_date);
			
			//Added for Bonds Yield calculations projects CRQ21157	
			psInsOTCBondIntra.setDouble(24, objBond.traded_yield);
			psInsOTCBondIntra.setDouble(25, objBond.bestBid_yield);
			psInsOTCBondIntra.setDouble(26, objBond.bestOffer_yield);
			
			psInsOTCBondIntra.addBatch();
			
			currentUpdatedTime =  System.currentTimeMillis();
			if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.WRITER_THREADS_DB_UPDATE_INTERVAL )
			{

				psInsOTCBondIntra.executeBatch();
				logger.debug("insertOTCBondIntraData Batch Updated");
				lastUpdatedTime = currentUpdatedTime;				
			}
			totalBondIntraCount++;

			if (logger.isDebugEnabled()) logger.debug("insertOTCBondIntraData: updateIndexIntraData :{}", totalBondIntraCount);
			return true;

		}catch(BatchUpdateException e) 
		{
			logger.error("Exception in psInsOTCBondIntra in BatchUpdateException(): " + e, e);
			try {
				psInsBondIntra.clearBatch();
			} catch (SQLException e1) {
				logger.error("Exception in psInsOTCBondIntra in clearBatch(): " + e1, e1);
			}
		} 
		catch (SQLException e) {
			logger.error("Exception in psInsOTCBondIntra: " + e, e);

		}

		if (objBond != null)
			objBond = null;
		return false;
	}
}
