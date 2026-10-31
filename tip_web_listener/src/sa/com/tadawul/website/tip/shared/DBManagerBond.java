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
import sa.com.tadawul.website.tip.vo.TIP_BondVO;


/**
 * @description This class is used to get Database Connection and all the
 *              prepared Statements.
 *
 */
public class DBManagerBond extends DBManagerMarket{

	private final static Log logger = LogFactory.getLog(DBManagerBond.class);


	int totalBondCount = 0;
	int totalOTCBondCount = 0;
	static long  lastUpdatedTime = 0;
	static long currentUpdatedTime = 0;
	

	// DB Initialization for Bond Market
	PreparedStatement psInitializeDBforBondMarket2;
	PreparedStatement psUpdBond;

	// Endofday for Bond Market
	PreparedStatement psEODBondMarketDelHsFiStock;
	PreparedStatement psEODBondMarketInsHsFIStock;
	PreparedStatement psEODBondMarketDelHsFIMarket;
	PreparedStatement psEODBondMarketInsHsFIMarket;
	
	
	// DB Initialization for unlistedBond Market
	PreparedStatement psInitializeDBforOTCBondMarket2;
	PreparedStatement psUpdOTCBond;
	PreparedStatement psEODOTCBondMarketDelHsFiStock;
	PreparedStatement psEODOTCBondMarketInsHsFIStock;


	public DBManagerBond(String threadName) throws Exception {
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

				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries
						.append("UPDATE EFP.FI_STOCK SET BID_PRICE = 0, BID_QUANTITY = 0, ASK_PRICE = 0, ASK_QUANTITY = 0, LAST_TRADE_PRICE = 0, "
								+ "LAST_TRADE_QUANTITY = 0, NET_CHANGE = 0, HIGH_PRICE = 0, LOW_PRICE = 0, LAST_UPDATE_TIME = systimestamp, PERCENT_CHANGE = 0, "
								+ "TURNOVER = 0, VOLUME_TRADED = 0, TODAYS_OPEN_PRICE = 0, NO_OF_TRADES = 0, TODAYS_CLOSE_PRICE = 0,TRADE_YIELD=0, BEST_BID_YIELD=0,BEST_OFFER_YIELD=0 ");
				
				psInitializeDBforBondMarket2 = g_oConnection	.prepareStatement(sbPrepareQueries.toString());
				
				
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries
				.append("UPDATE EFP.FI_STOCK_UNLISTED SET BID_PRICE = 0, BID_QUANTITY = 0, ASK_PRICE = 0, ASK_QUANTITY = 0, LAST_TRADE_PRICE = 0, "
						+ "LAST_TRADE_QUANTITY = 0, NET_CHANGE = 0, HIGH_PRICE = 0, LOW_PRICE = 0, LAST_UPDATE_TIME = systimestamp, PERCENT_CHANGE = 0, "
						+ "TURNOVER = 0, VOLUME_TRADED = 0, TODAYS_OPEN_PRICE = 0, NO_OF_TRADES = 0, TODAYS_CLOSE_PRICE = 0,TRADE_YIELD=0, BEST_BID_YIELD=0,BEST_OFFER_YIELD=0 ");
		
				psInitializeDBforOTCBondMarket2 = g_oConnection	.prepareStatement(sbPrepareQueries.toString());

				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("UPDATE EFP.FI_STOCK SET TRANSACTION_DATE = ?, LAST_TRADE_PRICE = ?, LAST_TRADE_QUANTITY = ?,"
								+ " LAST_UPDATE_TIME = ?, ASK_PRICE = ?, ASK_QUANTITY = ?, BID_PRICE = ?, BID_QUANTITY = ?, NET_CHANGE = ?, "
								+ "HIGH_PRICE = ?, HIGH_52_WEEKS_PRICE = ?, LOW_PRICE = ?, LOW_52_WEEKS_PRICE = ?, "
								+ "NO_OF_TRADES = ?, PERCENT_CHANGE = ?, PREVIOUS_CLOSE_PRICE = ?, "
								+ "TODAYS_CLOSE_PRICE = ?, TODAYS_OPEN_PRICE = ?, TURNOVER = ?, VOLUME_TRADED = ?,TRADE_YIELD=?, BEST_BID_YIELD=?,BEST_OFFER_YIELD=? "
								+ " WHERE FK_FI_SYMBOL = ?");

				psUpdBond = g_oConnection.prepareStatement(sbPrepareQueries.toString());
				
				//OTC Bond Update
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("UPDATE EFP.FI_STOCK_UNLISTED SET TRANSACTION_DATE = ?, LAST_TRADE_PRICE = ?, LAST_TRADE_QUANTITY = ?,"
								+ " LAST_UPDATE_TIME = ?, ASK_PRICE = ?, ASK_QUANTITY = ?, BID_PRICE = ?, BID_QUANTITY = ?, NET_CHANGE = ?, "
								+ "HIGH_PRICE = ?, HIGH_52_WEEKS_PRICE = ?, LOW_PRICE = ?, LOW_52_WEEKS_PRICE = ?, "
								+ "NO_OF_TRADES = ?, PERCENT_CHANGE = ?, PREVIOUS_CLOSE_PRICE = ?, "
								+ "TODAYS_CLOSE_PRICE = ?, TODAYS_OPEN_PRICE = ?, TURNOVER = ?, VOLUME_TRADED = ?,TRADE_YIELD=?, BEST_BID_YIELD=?,BEST_OFFER_YIELD=? "
								+ " WHERE FK_FI_SYMBOL = ?");

				psUpdOTCBond = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				// EOD For Bond Market
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("DELETE from WEB.HS_FI_STOCK where trunc(TRANSACTION_DATE) = trunc(sysdate)");
				psEODBondMarketDelHsFiStock = g_oConnection.prepareStatement(sbPrepareQueries.toString());
				
				//Changes for PTTP Project OTC
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("DELETE from WEB.HS_FI_STOCK_UNLISTED where trunc(TRANSACTION_DATE) = trunc(sysdate)");
				psEODOTCBondMarketDelHsFiStock = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries
						.append("INSERT INTO WEB.HS_FI_STOCK (FK_FI_SYMBOL,TRANSACTION_DATE,LAST_TRADE_PRICE,LAST_TRADE_QUANTITY, LAST_UPDATE_TIME, ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY,NO_OF_TRADES,NET_CHANGE,PERCENT_CHANGE,HIGH_PRICE,  LOW_PRICE,HIGH_52_WEEKS_PRICE,LOW_52_WEEKS_PRICE, PREVIOUS_CLOSE_PRICE,TODAYS_CLOSE_PRICE,TODAYS_OPEN_PRICE,TURNOVER,VOLUME_TRADED ,HIGH_52_WEEKS_DATE,LOW_52_WEEKS_DATE ,ISIN,TRADE_YIELD, BEST_BID_YIELD,BEST_OFFER_YIELD, OUTSTANDING_AMOUNT, ISSUE_CURRENCY "
								+ ",  PAR_VALUE, COUPON_FREQUENCY, DAYCOUNT_METHOD, COUPON_RATE, MATURITY_DATE, RATE_CALCTYPE) SELECT FK_FI_SYMBOL, systimestamp, LAST_TRADE_PRICE, LAST_TRADE_QUANTITY, LAST_UPDATE_TIME, ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY,NO_OF_TRADES,NET_CHANGE,PERCENT_CHANGE,HIGH_PRICE,  LOW_PRICE,HIGH_52_WEEKS_PRICE,LOW_52_WEEKS_PRICE, PREVIOUS_CLOSE_PRICE,TODAYS_CLOSE_PRICE,TODAYS_OPEN_PRICE,TURNOVER,VOLUME_TRADED ,HIGH_52_WEEKS_DATE,LOW_52_WEEKS_DATE, ISIN,TRADE_YIELD, BEST_BID_YIELD,BEST_OFFER_YIELD ,OUTSTANDING_AMOUNT, ISSUE_CURRENCY "
								+ ",  PAR_VALUE, COUPON_FREQUENCY, DAYCOUNT_METHOD, COUPON_RATE, MATURITY_DATE, RATE_CALCTYPE FROM EFP.FI_STOCK WHERE FK_FI_SYMBOL IN (SELECT to_CHAR(PK_RF_COMPANY) FROM WEB.FI_COMPANY WHERE BOND_LISTING_TYPE ='L' and active =1)");
				psEODBondMarketInsHsFIStock = g_oConnection
						.prepareStatement(sbPrepareQueries.toString());
				
				
				//Changes for PTTP Project OTC
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries
						.append("INSERT INTO WEB.HS_FI_STOCK_UNLISTED (FK_FI_SYMBOL,TRANSACTION_DATE,LAST_TRADE_PRICE,LAST_TRADE_QUANTITY, LAST_UPDATE_TIME, ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY,NO_OF_TRADES,NET_CHANGE,PERCENT_CHANGE,HIGH_PRICE,  LOW_PRICE,HIGH_52_WEEKS_PRICE,LOW_52_WEEKS_PRICE, PREVIOUS_CLOSE_PRICE,TODAYS_CLOSE_PRICE,TODAYS_OPEN_PRICE,TURNOVER,VOLUME_TRADED ,HIGH_52_WEEKS_DATE,LOW_52_WEEKS_DATE ,ISIN,TRADE_YIELD, BEST_BID_YIELD,BEST_OFFER_YIELD, OUTSTANDING_AMOUNT, ISSUE_CURRENCY "
								+ ",  PAR_VALUE, COUPON_FREQUENCY, DAYCOUNT_METHOD, COUPON_RATE, MATURITY_DATE, RATE_CALCTYPE) SELECT FK_FI_SYMBOL, systimestamp, LAST_TRADE_PRICE, LAST_TRADE_QUANTITY, LAST_UPDATE_TIME, ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY,NO_OF_TRADES,NET_CHANGE,PERCENT_CHANGE,HIGH_PRICE,  LOW_PRICE,HIGH_52_WEEKS_PRICE,LOW_52_WEEKS_PRICE, PREVIOUS_CLOSE_PRICE,TODAYS_CLOSE_PRICE,TODAYS_OPEN_PRICE,TURNOVER,VOLUME_TRADED ,HIGH_52_WEEKS_DATE,LOW_52_WEEKS_DATE, ISIN,TRADE_YIELD, BEST_BID_YIELD,BEST_OFFER_YIELD ,OUTSTANDING_AMOUNT, ISSUE_CURRENCY "
								+ ",  PAR_VALUE, COUPON_FREQUENCY, DAYCOUNT_METHOD, COUPON_RATE, MATURITY_DATE, RATE_CALCTYPE FROM EFP.FI_STOCK_UNLISTED WHERE FK_FI_SYMBOL IN (SELECT to_CHAR(PK_RF_COMPANY) FROM WEB.FI_COMPANY WHERE BOND_LISTING_TYPE ='U' and active =1)");
				psEODOTCBondMarketInsHsFIStock = g_oConnection
						.prepareStatement(sbPrepareQueries.toString());
				

				// Insert into HS_FI_MARKET
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries
						.append("DELETE from WEB.HS_FI_MARKET where trunc(TRANSACTION_DATE) = trunc(sysdate)");
				psEODBondMarketDelHsFIMarket = g_oConnection	.prepareStatement(sbPrepareQueries.toString());

				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries
						.append("INSERT INTO WEB.HS_FI_MARKET(TRANSACTION_DATE,NO_OF_TRADES,TURNOVER,VOLUME_TRADED) "
								+ "SELECT systimestamp, NO_OF_TRADES,TURNOVER,VOLUME_TRADED from EFP.TE_MARKET WHERE PK_EXCHANGE_CODE = '"
								+ TipConstants.BOND_MARKET_CODE + "'");
				psEODBondMarketInsHsFIMarket = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				/*
				 * Fixed Income Phase II Project
				 * 
				 * Ends
				 */

				logger.debug("Exit DB2Manager prepareStatements function");
			
			
			} catch (Exception e) {
				logger.error("Exception while preparing query ["
						+ sbPrepareQueries.toString() + "] is " + e);
				throw e;
			}

	}




	/**
	 * This method will initialize Bond table at BOD
	 * 
	 * @return boolean
	 * @throws SQLException 
	 */
	public boolean initializeDBforBond() {
			
			try {
				logger.debug("inside  DB2Manager initializeDBforBond function");
				logger.debug("initializeDBforBondIntra --> dtFeedReqDate = "	+ dtFeedReqDate);
				logger.debug("inside initializeDBforBondIntra --> dtBondLastUpdateTime = "+LoadInitialData. dtBondLastUpdateTime);
				if (!LoadInitialData.dtBondLastUpdateTime.equals(dtFeedReqDate)) {
					initilizeOrderBook("FI");
					psInitializeDBforBondMarket2.executeUpdate();
					commitTrans(true);
					logger.info("initializeDBforBond: EFP.FI_STOCK table columns initialized to zero");
				}
				return true;
				
			} catch (SQLException e) {
				logger.error("Exception in initializeDBforBond : " + e	+ getStackTrace(e), e);
			}
			
			logger.debug("Exit  DB2Manager initializeDBforBond function");
			
		return false;
	}

	/**
	 * This method will initialize Bond table at BOD
	 * 
	 * @return boolean
	 * @throws SQLException 
	 */
	public boolean initializeDBforOTCBond() {
			
			try {
				logger.debug("inside  DB2Manager initializeDBforOTCBond function");
				logger.debug("initializeDBforOTCBond --> dtFeedReqDate = "	+ dtFeedReqDate);
				logger.debug("inside initializeDBforOTCBond --> dtBondLastUpdateTime = "+LoadInitialData. dtBondLastUpdateTime);
				if (!LoadInitialData.dtBondLastUpdateTime.equals(dtFeedReqDate)) {
					psInitializeDBforOTCBondMarket2.executeUpdate();
					commitTrans(true);
					logger.info("initializeDBforOTCBond: EFP.FI_STOCK table columns initialized to zero");
				}
				return true;
				
			} catch (SQLException e) {
				logger.error("Exception in initializeDBforOTCBond : " + e	+ getStackTrace(e), e);
			}
			
			logger.debug("Exit  DB2Manager initializeDBforOTCBond function");
			
		return false;
	}
	
	/**
	 * This method will updates the table EFP.FI_STOCK
	 * 
	 * @param objBond
	 *            contains attributes of EFP.FI_STOCK table
	 * @return boolean
	 * @throws SQLException 
	 */
	public boolean updateBondData(TIP_BondVO objBond,boolean isCommit) throws SQLException {

			try {

				psUpdBond.setTimestamp(1, objBond.transaction_date);
				psUpdBond.setDouble(2, objBond.last_trade_price);
				psUpdBond.setInt(3, objBond.last_trade_quantity);
				psUpdBond.setTimestamp(4, objBond.last_update_time);
				psUpdBond.setDouble(5, objBond.ask_price);
				psUpdBond.setLong(6, objBond.ask_quantity);
				psUpdBond.setDouble(7, objBond.bid_price);
				psUpdBond.setLong(8, objBond.bid_quantity);
				psUpdBond.setDouble(9, objBond.net_change);
				psUpdBond.setDouble(10, objBond.high_price);
				psUpdBond.setDouble(11, objBond.high_52_weeks_price);
				psUpdBond.setDouble(12, objBond.low_price);
				psUpdBond.setDouble(13, objBond.low_52_weeks_price);
				psUpdBond.setLong(14, objBond.no_of_trades);
				psUpdBond.setDouble(15, objBond.percent_change);
				psUpdBond.setDouble(16, objBond.Previous_close_price);
				psUpdBond.setDouble(17, objBond.todays_close_price);
				psUpdBond.setDouble(18, objBond.Todays_open_price);
				psUpdBond.setDouble(19, objBond.turnover);
				psUpdBond.setDouble(20, objBond.volume_traded);
				
				//Added for Bonds Yield calculations projects CRQ21157	
				psUpdBond.setDouble(21, objBond.traded_yield);
				psUpdBond.setDouble(22, objBond.bestBid_yield);
				psUpdBond.setDouble(23, objBond.bestOffer_yield);
				
				psUpdBond.setString(24, objBond.fk_fi_symbol); 
				psUpdBond.addBatch();
				
				currentUpdatedTime =  System.currentTimeMillis();
				
				if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.UPDATER_THREADS_DB_UPDATE_INTERVAL  )
				{
				//if (objBond.isEndOfBatch){
					psUpdBond.executeBatch();
					lastUpdatedTime = currentUpdatedTime;
				}
				
				totalBondCount++;								
				logger.debug("DBManagerBondIndex: updateBondData :"+totalBondCount);
				
				return true;
				
			} catch (SQLException e) {
				logger.error("Exception in updateBondData: " + e, e);
				
			}
			
			if (objBond != null){
				objBond = null;
			
			}

		return false;
	}

	public void updateBestBidOffer(TIP_BondVO objShare) {
		if ("OBk".equals(objShare.messageType)) {
			logger.info("OBk BBBO BOND updateBestBidOffer  " + objShare);
			updateBestBidOffer(objShare.fk_fi_symbol, objShare.last_update_time, objShare.bestBids, objShare.bestOffers,
					"FI");
		} else {
			logger.info("OBk BBBO MAIN updateBestBidOffer Message seems not OBK " + objShare.messageType);
		}
	}

	/**
	 * This method will updates the table EFP.FI_STOCK
	 * 
	 * @param objBond
	 *            contains attributes of EFP.FI_STOCK table
	 * @return boolean
	 * @throws SQLException 
	 */
	public boolean updateOTCBondData(TIP_BondVO objBond,boolean isCommit) throws SQLException {

			try {
				
				logger.debug("updateOTCBondData for symbol :"+objBond.fk_fi_symbol);
				
				psUpdOTCBond.setTimestamp(1, objBond.transaction_date);
				psUpdOTCBond.setDouble(2, objBond.last_trade_price);
				psUpdOTCBond.setInt(3, objBond.last_trade_quantity);
				psUpdOTCBond.setTimestamp(4, objBond.last_update_time);
				psUpdOTCBond.setDouble(5, objBond.ask_price);
				psUpdOTCBond.setLong(6, objBond.ask_quantity);
				psUpdOTCBond.setDouble(7, objBond.bid_price);
				psUpdOTCBond.setLong(8, objBond.bid_quantity);
				psUpdOTCBond.setDouble(9, objBond.net_change);
				psUpdOTCBond.setDouble(10, objBond.high_price);
				psUpdOTCBond.setDouble(11, objBond.high_52_weeks_price);
				psUpdOTCBond.setDouble(12, objBond.low_price);
				psUpdOTCBond.setDouble(13, objBond.low_52_weeks_price);
				psUpdOTCBond.setLong(14, objBond.no_of_trades);
				psUpdOTCBond.setDouble(15, objBond.percent_change);
				psUpdOTCBond.setDouble(16, objBond.Previous_close_price);
				psUpdOTCBond.setDouble(17, objBond.todays_close_price);
				psUpdOTCBond.setDouble(18, objBond.Todays_open_price);
				psUpdOTCBond.setDouble(19, objBond.turnover);
				psUpdOTCBond.setDouble(20, objBond.volume_traded);
				
				//Added for Bonds Yield calculations projects CRQ21157	
				psUpdOTCBond.setDouble(21, objBond.traded_yield);
				psUpdOTCBond.setDouble(22, objBond.bestBid_yield);
				psUpdOTCBond.setDouble(23, objBond.bestOffer_yield);
				
				psUpdOTCBond.setString(24, objBond.fk_fi_symbol); 
				psUpdOTCBond.addBatch();
				
				currentUpdatedTime =  System.currentTimeMillis();
				
				if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.UPDATER_THREADS_DB_UPDATE_INTERVAL  )
				{
				//if (objBond.isEndOfBatch){
					psUpdOTCBond.executeBatch();
					lastUpdatedTime = currentUpdatedTime;
				}
				
				totalOTCBondCount++;								
				logger.debug("updateOTCBondData: updateOTCBondData :"+totalOTCBondCount);
				
				return true;
				
			} catch (SQLException e) {
				logger.error("Exception in updateOTCBondData: " + e, e);
				
			}
			
			if (objBond != null){
				objBond = null;
			
			}

		return false;
	}




	/**
	 * This method will invoke the EOD process for Bond Market
	 * 
	 * @param objMarket
	 *            contains attributes of EFP.TE_MARKET table
	 * @return boolean
	 */
	public boolean EODforBond() {

			try {
				logger.debug("inside  DB2Manager EODforBond function");
				psEODBondMarketDelHsFiStock.executeUpdate();
				psEODBondMarketInsHsFIStock.executeUpdate();
				psEODBondMarketDelHsFIMarket.executeUpdate();
				psEODBondMarketInsHsFIMarket.executeUpdate();
				
				//Added for PTTP Project
				psEODOTCBondMarketDelHsFiStock.executeUpdate();
				psEODOTCBondMarketInsHsFIStock.executeUpdate();

				logger.info("EODforBond: Done");
				return true;
				
			} catch (SQLException e) {
				logger.error("Exception in EODforBond: " + e, e);
		
			}
	

		logger.debug("Exit  DB2Manager EODforBond function");
		return false;
	}

	/**
	 * This method will do the commit.
	 * 
	 * @param null
	 */
	public void commitTrans(boolean isCommit) {
		try {
		
			currentUpdatedTime =  System.currentTimeMillis();
			
			if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.UPDATER_THREADS_DB_UPDATE_INTERVAL )
			{	
				psUpdBond.executeBatch();			
				lastUpdatedTime = currentUpdatedTime;				
			}	
			g_oConnection.commit();

		} catch (BatchUpdateException be2) {
			logger.error("BatchUpdateException in commitTrans() while doing psUpdBond "+ be2);
			try {
				psUpdBond.clearBatch();
			} catch (SQLException e1) {
				logger.error("Exception in psUpdBond in clearBatch(): " + e1, e1);
			}
		}catch(SQLException e)
		{
			logger.error("SQLException in commitTrans() while doing psUpdBond "+ e);
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

			if (psEODBondMarketInsHsFIStock != null) 	psEODBondMarketInsHsFIStock.close();
			if (psEODBondMarketDelHsFIMarket != null) psEODBondMarketDelHsFIMarket.close();
			if (psEODBondMarketInsHsFIMarket != null) 	psEODBondMarketInsHsFIMarket.close();

			if (psInitializeDBforBondMarket2 != null) psInitializeDBforBondMarket2.close();
			
			if (psInitializeDBforOTCBondMarket2 != null) psInitializeDBforOTCBondMarket2.close();
			
			if (psUpdOTCBond != null) 	psUpdOTCBond.close();
			
			if (psEODOTCBondMarketDelHsFiStock != null)	psEODOTCBondMarketDelHsFiStock.close();
			if (psEODOTCBondMarketInsHsFIStock != null) 	psEODOTCBondMarketInsHsFIStock.close();
			

			if (g_oConnection != null)
				g_oConnection.close();
			logger.debug("Exit  DB2Manager closeConnection function");
		} catch (SQLException ex) {
			logger.error("SQL Exception in closeConnection() " + ex, ex);			//GUIHelper.bondConnection = TipConstants.CONN_STATUS_NOTCONNECTED;

		}
	}


}
