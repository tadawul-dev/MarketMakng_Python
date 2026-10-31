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

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import oracle.jdbc.pool.OracleDataSource;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import sa.com.tadawul.website.tip.vo.TIP_SMEShareVO;
import sa.com.tadawul.website.tip.vo.TIP_ShareVO;


/**
 * @description This class is used to get Database Connection and all the
 *              prepared Statements.
 *
 */
public class DBManagerSMEShare extends DBManagerMarket{


	private static final Logger logger = LogManager.getLogger(DBManagerSMEShare.class);
	private PreparedStatement psInitializeDBforSMEShareMarket12;
	private PreparedStatement psEODSMEShareMarketInsTeRIStock;
	private PreparedStatement psEODSMEShareMarketInsTeStock_Adj;
	private PreparedStatement psEODSMEShareMarketInsTeStock;
	private PreparedStatement psEODSMEShareMarketDelHsTeRIStock;
	private PreparedStatement psEODSMEShareMarketDelHsTeStock_Adj;
	private PreparedStatement psEODSMEShareMarketDelHsTeStock;
	private PreparedStatement psUpdSMEShareRI;
	private PreparedStatement psUpdSMEShare;
	private PreparedStatement psInitializeDBforSMEShareRIMarket12;
	static long  lastUpdatedTime = 0;
	static long currentUpdatedTime = 0;
	
	public DBManagerSMEShare(String threadName) throws Exception {
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
				
				// Added for SME
				if (sbPrepareQueries.length() > 0)
					sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("UPDATE EFP.TE_SME_STOCK SET BID_PRICE = 0, BID_QUANTITY = 0, ASK_PRICE = 0, ASK_QUANTITY = 0, LAST_TRADE_PRICE = 0, "
								+ "LAST_TRADE_QUANTITY = 0, NET_CHANGE = 0, HIGH_PRICE = 0, LOW_PRICE = 0, LAST_UPDATE_TIME = systimestamp, PERCENT_CHANGE = 0, "
								+ "SEQUENCE_NO = 0, TURNOVER = 0, VOLUME_TRADED = 0, TODAYS_OPEN = 0, PBR = 0, PER = 0, YIELD = 0, NO_OF_TRADES = 0, MARKET_STATUS = 0, TODAYS_CLOSE_PRICE = 0");
				psInitializeDBforSMEShareMarket12 = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				// Added for SME
				if (sbPrepareQueries.length() > 0)
					sbPrepareQueries.delete(0, sbPrepareQueries.length());
				
				sbPrepareQueries.append("UPDATE EFP.RI_SME_STOCK SET BID_PRICE = 0, BID_QUANTITY = 0, ASK_PRICE = 0, ASK_QUANTITY = 0, LAST_TRADE_PRICE = 0, "
								+ "LAST_TRADE_QUANTITY = 0, NET_CHANGE = 0, HIGH_PRICE = 0, LOW_PRICE = 0, LAST_UPDATE_TIME = systimestamp , PERCENT_CHANGE = 0, "
								+ "SEQUENCE_NO = 0, TURNOVER = 0, VOLUME_TRADED = 0, TODAYS_OPEN = 0, PBR = 0, PER = 0, YIELD = 0, NO_OF_TRADES = 0, MARKET_STATUS = 0, TODAYS_CLOSE_PRICE = 0 ");
				psInitializeDBforSMEShareRIMarket12 = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				// Added for SME
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				// Added the Indicative Opening details
				sbPrepareQueries.append("UPDATE EFP.TE_SME_STOCK SET TRANSACTION_DATE = ?, LAST_TRADE_PRICE = ?, LAST_TRADE_QUANTITY = ?,"
								+ " LAST_UPDATE_TIME = ?, ASK_PRICE = ?, ASK_QUANTITY = ?, BID_PRICE = ?, BID_QUANTITY = ?, NET_CHANGE = ?, "
								+ "HIGH_PRICE = ?, LOW_PRICE = ?, MARKET_CAP = ?, "
								+ "NO_OF_TRADES = ?, PBR = ?, PER = ?, PERCENT_CHANGE = ?, PREVIOUS_CLOSE_PRICE = ?, SEQUENCE_NO = ?, "
								+ "TODAYS_CLOSE_PRICE = ?, TODAYS_OPEN = ?, TURNOVER = ?, VOLUME_TRADED = ?, YIELD = ?, MARKET_STATUS = ?,  "
								+ " INDICATIVEPRICE = ?, INDICATIVEVOLUME = ? WHERE FK_TE_SYMBOL = ?");

				psUpdSMEShare = g_oConnection
						.prepareStatement(sbPrepareQueries.toString());
				// Added for SME
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries
						.append("UPDATE EFP.RI_SME_STOCK SET TRANSACTION_DATE = ?, LAST_TRADE_PRICE = ?, LAST_TRADE_QUANTITY = ?,"
								+ " LAST_UPDATE_TIME = ?, ASK_PRICE = ?, ASK_QUANTITY = ?, BID_PRICE = ?, BID_QUANTITY = ?, NET_CHANGE = ?, "
								+ "HIGH_PRICE = ?, LOW_PRICE = ?, MARKET_CAP = ?, "
								+ "NO_OF_TRADES = ?, PBR = ?, PER = ?, PERCENT_CHANGE = ?, PREVIOUS_CLOSE_PRICE = ?, SEQUENCE_NO = ?, "
								+ "TODAYS_CLOSE_PRICE = ?, TODAYS_OPEN = ?, TURNOVER = ?, VOLUME_TRADED = ?, YIELD = ?, MARKET_STATUS = ? , "
								+ "  REFERENCEPRICE_UPPERLIMIT = ?, REFERENCEPRICE_LOWERLIMIT = ? WHERE FK_RI_SYMBOL = ?");

				psUpdSMEShareRI = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				// Added for SME
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("DELETE from WEB.HS_TE_SME_STOCK where trunc(pk_hs_te_stock) = trunc(sysdate) ");
				psEODSMEShareMarketDelHsTeStock = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				// Added for SME
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("DELETE from WEB.HS_TE_SME_STOCK_ADJUSTED where trunc(pk_hs_te_stock) = trunc(sysdate) ");
				psEODSMEShareMarketDelHsTeStock_Adj = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				// Added for SME
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("DELETE from WEB.HS_RI_SME_STOCK where trunc(pk_hs_ri_stock) = trunc(sysdate) ");
				psEODSMEShareMarketDelHsTeRIStock = g_oConnection.prepareStatement(sbPrepareQueries.toString());

			
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("INSERT INTO WEB.HS_TE_SME_STOCK (FK_TE_SYMBOL, TRANSACTION_DATE, LAST_TRADE_PRICE, LAST_TRADE_QUANTITY, "
								+ " LAST_UPDATE_TIME, ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY, NET_CHANGE, HIGH_PRICE,  HIGH_52_WEEKS_PRICE, LOW_PRICE, LOW_52_WEEKS_PRICE, "
								+ " MARKET_CAP, NO_OF_TRADES, PBR, PER,  PERCENT_CHANGE, PREVIOUS_CLOSE_PRICE, SEQUENCE_NO, TODAYS_CLOSE_PRICE, TODAYS_OPEN, TURNOVER, VOLUME_TRADED, "
								+ " YIELD, PK_HS_TE_STOCK , HIGH_52_WEEKS_DATE, LOW_52_WEEKS_DATE, INDICATIVEPRICE, INDICATIVEVOLUME ) SELECT distinct FK_TE_SYMBOL, systimestamp, LAST_TRADE_PRICE, LAST_TRADE_QUANTITY, LAST_UPDATE_TIME,  "
								+ " ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY, (TODAYS_CLOSE_PRICE - PREVIOUS_CLOSE_PRICE ) AS  NET_CHANGE, HIGH_PRICE, HIGH_52_WEEKS_PRICE,  "
								+ " LOW_PRICE, LOW_52_WEEKS_PRICE, MARKET_CAP, NO_OF_TRADES, PBR, PER, Round(100 * to_number(CASE WHEN PREVIOUS_CLOSE_PRICE IS NULL OR PREVIOUS_CLOSE_PRICE=0 "
								+ " OR TODAYS_CLOSE_PRICE IS NULL OR TODAYS_CLOSE_PRICE = 0 THEN 0.00 ELSE to_number((TODAYS_CLOSE_PRICE - PREVIOUS_CLOSE_PRICE ))/to_number(PREVIOUS_CLOSE_PRICE) END),2) "
								+ " AS PERCENT_CHANGE, PREVIOUS_CLOSE_PRICE,  SEQUENCE_NO, TODAYS_CLOSE_PRICE, TODAYS_OPEN, TURNOVER, VOLUME_TRADED, YIELD, systimestamp  , HIGH_52_WEEKS_DATE, "
								+ " LOW_52_WEEKS_DATE,INDICATIVEPRICE, INDICATIVEVOLUME  FROM EFP.TE_SME_STOCK WHERE TO_CHAR(LISTING_DATE,'yyyyMMdd')!='39990101' AND fk_te_symbol in (select to_char(pk_rf_company) from sacp.rf_company where active =1)");

				
				psEODSMEShareMarketInsTeStock = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				// TODO Need to verify
				// Added for SME
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries
						.append("INSERT INTO WEB.HS_TE_SME_STOCK_ADJUSTED (FK_TE_SYMBOL, TRANSACTION_DATE, LAST_TRADE_PRICE, LAST_TRADE_QUANTITY, "
								+ " LAST_UPDATE_TIME, ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY, NET_CHANGE, HIGH_PRICE,  HIGH_52_WEEKS_PRICE, LOW_PRICE, LOW_52_WEEKS_PRICE, "
								+ " MARKET_CAP, NO_OF_TRADES, PBR, PER,  PERCENT_CHANGE, PREVIOUS_CLOSE_PRICE, SEQUENCE_NO, TODAYS_CLOSE_PRICE, TODAYS_OPEN, TURNOVER, VOLUME_TRADED, "
								+ " YIELD, PK_HS_TE_STOCK , HIGH_52_WEEKS_DATE, LOW_52_WEEKS_DATE, INDICATIVEPRICE, INDICATIVEVOLUME ) SELECT distinct FK_TE_SYMBOL, systimestamp, LAST_TRADE_PRICE, LAST_TRADE_QUANTITY, LAST_UPDATE_TIME,  "
								+ " ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY, (TODAYS_CLOSE_PRICE - PREVIOUS_CLOSE_PRICE ) AS  NET_CHANGE, HIGH_PRICE, HIGH_52_WEEKS_PRICE,  "
								+ " LOW_PRICE, LOW_52_WEEKS_PRICE, MARKET_CAP, NO_OF_TRADES, PBR, PER, Round(100 * to_number(CASE WHEN PREVIOUS_CLOSE_PRICE IS NULL OR PREVIOUS_CLOSE_PRICE=0 "
								+ " OR TODAYS_CLOSE_PRICE IS NULL OR TODAYS_CLOSE_PRICE = 0 THEN 0.00 ELSE to_number((TODAYS_CLOSE_PRICE - PREVIOUS_CLOSE_PRICE ))/to_number(PREVIOUS_CLOSE_PRICE) END),2)"
								+ " AS PERCENT_CHANGE, PREVIOUS_CLOSE_PRICE,  SEQUENCE_NO, TODAYS_CLOSE_PRICE, TODAYS_OPEN, TURNOVER, VOLUME_TRADED, YIELD, systimestamp  , HIGH_52_WEEKS_DATE, "
								+ " LOW_52_WEEKS_DATE,INDICATIVEPRICE, INDICATIVEVOLUME  FROM EFP.TE_SME_STOCK WHERE TO_CHAR(LISTING_DATE,'yyyyMMdd')!='39990101' AND fk_te_symbol in (select to_char(pk_rf_company) from sacp.rf_company where active =1)");

				psEODSMEShareMarketInsTeStock_Adj = g_oConnection.prepareStatement(sbPrepareQueries.toString());
				// Added for SME
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("INSERT INTO WEB.HS_RI_SME_STOCK (FK_RI_SYMBOL, TRANSACTION_DATE, LAST_TRADE_PRICE, LAST_TRADE_QUANTITY, LAST_UPDATE_TIME, ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY, NET_CHANGE, HIGH_PRICE, "
						+ "  HIGH_52_WEEKS_PRICE, LOW_PRICE, LOW_52_WEEKS_PRICE, MARKET_CAP, NO_OF_TRADES, PBR, PER,  PERCENT_CHANGE, PREVIOUS_CLOSE_PRICE, SEQUENCE_NO, TODAYS_CLOSE_PRICE, TODAYS_OPEN, TURNOVER, VOLUME_TRADED, YIELD, "
						+ " PK_HS_RI_STOCK , HIGH_52_WEEKS_DATE, LOW_52_WEEKS_DATE , REFERENCEPRICE_UPPERLIMIT, REFERENCEPRICE_LOWERLIMIT  ) SELECT FK_RI_SYMBOL, systimestamp, LAST_TRADE_PRICE, LAST_TRADE_QUANTITY, LAST_UPDATE_TIME,  "
						+ " ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY, (TODAYS_CLOSE_PRICE - PREVIOUS_CLOSE_PRICE ) AS  NET_CHANGE, HIGH_PRICE, HIGH_52_WEEKS_PRICE,  LOW_PRICE, LOW_52_WEEKS_PRICE, MARKET_CAP, NO_OF_TRADES, PBR, "
						+ " PER, Round(100 * to_number(CASE WHEN PREVIOUS_CLOSE_PRICE IS NULL OR PREVIOUS_CLOSE_PRICE=0   OR TODAYS_CLOSE_PRICE IS NULL OR TODAYS_CLOSE_PRICE = 0 THEN 0.00 ELSE to_number((TODAYS_CLOSE_PRICE - PREVIOUS_CLOSE_PRICE ))/to_number(PREVIOUS_CLOSE_PRICE) END),2) AS PERCENT_CHANGE,"
						+ " PREVIOUS_CLOSE_PRICE,  SEQUENCE_NO, TODAYS_CLOSE_PRICE, TODAYS_OPEN, TURNOVER, VOLUME_TRADED, YIELD, systimestamp  , HIGH_52_WEEKS_DATE, LOW_52_WEEKS_DATE , REFERENCEPRICE_UPPERLIMIT, REFERENCEPRICE_LOWERLIMIT   FROM EFP.RI_SME_STOCK WHERE TO_CHAR(LISTING_DATE,'yyyyMMdd')!='39990101'  AND fk_ri_symbol in (select to_char(pk_rf_company) from web.ri_sme_company where active =1)");
				psEODSMEShareMarketInsTeRIStock = g_oConnection.prepareStatement(sbPrepareQueries.toString());
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
		
			
currentUpdatedTime =  System.currentTimeMillis();		
			
			
//Added for SME
		if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.UPDATER_THREADS_DB_UPDATE_INTERVAL  )
		{
				try {
					logger.debug("SMEShareUpd in ShareUpdate_batchExecute Executed");
					psUpdSMEShare.executeBatch();
				} catch (SQLException ex) {
				
					logger.error(	"Exception in psUpdSMEShare in ShareUpdate_batchExecute: "+ ex, ex);
				}
				try {
					logger.debug("SMEShareUpdRI in ShareUpdate_batchExecute Executed");
					psUpdSMEShareRI.executeBatch();
				} catch (SQLException ex) {
				
					logger.error(	"Exception in psUpdSMEShareRI in ShareUpdate_batchExecute: "+ ex, ex);
				}
				lastUpdatedTime = currentUpdatedTime;
		}
			
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
			
			if (psInitializeDBforSMEShareMarket12 != null){
				psInitializeDBforSMEShareMarket12.close();
			}
			
			if (psEODSMEShareMarketInsTeRIStock != null){
				psEODSMEShareMarketInsTeRIStock.close();
			}
			
			if (psEODSMEShareMarketInsTeStock_Adj != null){
				psEODSMEShareMarketInsTeStock_Adj.close();
			}
			
			if (psEODSMEShareMarketInsTeStock != null){
				psEODSMEShareMarketInsTeStock.close();
			}
			
			if (psEODSMEShareMarketDelHsTeRIStock != null){
				psEODSMEShareMarketDelHsTeRIStock.close();
			}
			
			if (psEODSMEShareMarketDelHsTeStock_Adj != null){
				psEODSMEShareMarketDelHsTeStock_Adj.close();
			}
			
			if (psEODSMEShareMarketDelHsTeStock != null){
				psEODSMEShareMarketDelHsTeStock.close();
			}
			
			if (psUpdSMEShareRI != null){
				psUpdSMEShareRI.close();
			}
			
			if (psUpdSMEShare != null){
				psUpdSMEShare.close();
			}
			
			if (psInitializeDBforSMEShareRIMarket12 != null){
				psInitializeDBforSMEShareRIMarket12.close();
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
	 * This method will initialize Share table at BOD
	 * 
	 * @return boolean
	 */
	// Added for SME
	public boolean initializeDBforSMEShare() {

			// logger.debug("**************bod FOR sHARE IS cALLED***********");
			try {
				logger.debug("inside  DB2Manager initializeDBforSMEShare function");
				logger.debug("initializeDBforSMEShare --> dtFeedReqDate = "
						+ dtFeedReqDate);
				logger.debug("inside initializeDBforSMEShare --> dtSMEShareIndexLastUpdateTime = "
						+ LoadInitialData.dtSMEShareIndexLastUpdateTime);
				if (!LoadInitialData.dtSMEShareIndexLastUpdateTime.equals(dtFeedReqDate)) {
					psInitializeDBforSMEShareMarket12.executeUpdate();
					initilizeOrderBook("S");
					commitTrans(true);
					logger.info("initializeDBforShare: EFP.TE_SME_STOCK table columns initialized to zero");
				}

				return true; 
			} catch (SQLException e) {
				logger.error("Exception in initializeDBforSMEShare: " + e, e);
			}
		

		logger.debug("Exit  DB2Manager initializeDBforSMEShare function");
		return false;
	}

	// Added for SME
	public boolean initializeDBforSMEShareRI() {

			try {
				logger.debug("inside  DB2Manager initializeDBforSMEShareRI function");
				logger.debug("initializeDBforSMEShare RI--> dtFeedReqDate = "
						+ dtFeedReqDate);
				logger.debug("inside initializeDBforSMEShare RI--> dtSMEShareIndexLastUpdateTime = "
						+ LoadInitialData.dtSMEShareIndexLastUpdateTime);

				if (logger.isDebugEnabled()) logger.debug(dtSMEShareIndexLastUpdateTime + "***********"				+ dtFeedReqDate);
				if (!LoadInitialData.dtSMEShareIndexLastUpdateTime.equals(dtFeedReqDate)) {
					// logger.debug("Inside if");
					psInitializeDBforSMEShareRIMarket12.executeUpdate();
					commitTrans(true);
					logger.info("initializeDBforSMEShareRI: EFP.RI_SME_STOCK table columns initialized to zero");
				}
				// logger.debug("Down IF");

				return true;
			} catch (SQLException e) {
				logger.error("Exception in initializeDBforSMEShareRI: " + e, e);
		
			}


		logger.debug("Exit  DB2Manager initializeDBforSMEShareRI function");
		return false;
	}

	/**
	 * This method will updates the table EFP.TE_SME_STOCK SET
	 * 
	 * @param objShare
	 *            contains attributes of EFP.TE_SME_STOCK SET table
	 * @return boolean
	 */
	// Added for SME
	public boolean updateSMEShareData(TIP_SMEShareVO objShare) {

			try {

				psUpdSMEShare.setTimestamp(1, objShare.transaction_date);
				psUpdSMEShare.setDouble(2, objShare.last_trade_price);
				psUpdSMEShare.setInt(3, objShare.last_trade_quantity);
				psUpdSMEShare.setTimestamp(4, objShare.last_update_time);
				psUpdSMEShare.setDouble(5, objShare.ask_price);
				psUpdSMEShare.setLong(6, objShare.ask_quantity);
				psUpdSMEShare.setDouble(7, objShare.bid_price);
				psUpdSMEShare.setDouble(8, objShare.bid_quantity);
				psUpdSMEShare.setDouble(9, objShare.net_change);
				psUpdSMEShare.setDouble(10, objShare.high_price);
				psUpdSMEShare.setDouble(11, objShare.low_price);
				psUpdSMEShare.setDouble(12, objShare.market_cap);
				psUpdSMEShare.setLong(13, objShare.no_of_trades);
				psUpdSMEShare.setDouble(14, objShare.Pbr);
				psUpdSMEShare.setDouble(15, objShare.Per);
				psUpdSMEShare.setDouble(16, objShare.percent_change);
				psUpdSMEShare.setDouble(17, objShare.Previous_close_price);
				psUpdSMEShare.setLong(18, objShare.sequence_no);
				psUpdSMEShare.setDouble(19, objShare.todays_close_price);
				psUpdSMEShare.setDouble(20, objShare.Todays_open);
				psUpdSMEShare.setDouble(21, objShare.turnover);
				psUpdSMEShare.setDouble(22, objShare.volume_traded);
				psUpdSMEShare.setDouble(23, objShare.yield);
				psUpdSMEShare.setInt(24, objShare.market_status);
				// Added for indicative Opening change
				psUpdSMEShare.setDouble(25, objShare.indicative_Open_price);
				psUpdSMEShare.setDouble(26, objShare.indicative_Open_volume);
				psUpdSMEShare.setString(27, objShare.fk_te_symbol); 

				psUpdSMEShare.addBatch();
	

				return true;
			} catch (SQLException e) {
				logger.error("Exception in updateSMEShareData: " + e, e);

			}


		if (objShare != null)
			objShare = null;
		return false;
	}

	// Added for SME
	public boolean SMEShareUpdate_batchExecute(boolean isCommit) {

		boolean isFailed = false;

		currentUpdatedTime =  System.currentTimeMillis();		
		

		// Added for SME
		if(  isCommit ||(currentUpdatedTime-lastUpdatedTime)>TipConstants.UPDATER_THREADS_DB_UPDATE_INTERVAL )
		{
				try {
					logger.debug("SMEShareUpd in ShareUpdate_batchExecute Executed");
					psUpdSMEShare.executeBatch();
				} catch (SQLException ex) {
					isFailed = true;
					logger.error(	"Exception in psUpdSMEShare in ShareUpdate_batchExecute: "+ ex, ex);
				}
				try {
					logger.debug("SMEShareUpdRI in ShareUpdate_batchExecute Executed");
					psUpdSMEShareRI.executeBatch();
				} catch (SQLException ex) {
					isFailed = true;
					logger.error(	"Exception in psUpdSMEShareRI in ShareUpdate_batchExecute: "+ ex, ex);
				}
				lastUpdatedTime = currentUpdatedTime;
		}
		// Added for SME END

		// End
		if (isFailed)
			return false;
		return true;
	}

	/**
	 * This method will updates the table EFP.TE_RISTOCK SET
	 * 
	 * @param objShare
	 *            contains attributes of EFP.TE_RISTOCK SET table
	 * @return boolean
	 */
	// Added for SME
	public boolean updateSMEShareRIData(TIP_SMEShareVO objShare) {

			try {

				psUpdSMEShareRI.setTimestamp(1, objShare.transaction_date);
				psUpdSMEShareRI.setDouble(2, objShare.last_trade_price);
				psUpdSMEShareRI.setInt(3, objShare.last_trade_quantity);
				psUpdSMEShareRI.setTimestamp(4, objShare.last_update_time);
				psUpdSMEShareRI.setDouble(5, objShare.ask_price);
				psUpdSMEShareRI.setLong(6, objShare.ask_quantity);
				psUpdSMEShareRI.setDouble(7, objShare.bid_price);
				psUpdSMEShareRI.setDouble(8, objShare.bid_quantity);
				psUpdSMEShareRI.setDouble(9, objShare.net_change);
				psUpdSMEShareRI.setDouble(10, objShare.high_price);
				psUpdSMEShareRI.setDouble(11, objShare.low_price);
				psUpdSMEShareRI.setDouble(12, objShare.market_cap);
				psUpdSMEShareRI.setLong(13, objShare.no_of_trades);
				psUpdSMEShareRI.setDouble(14, objShare.Pbr);
				psUpdSMEShareRI.setDouble(15, objShare.Per);
				psUpdSMEShareRI.setDouble(16, objShare.percent_change);
				psUpdSMEShareRI.setDouble(17, objShare.Previous_close_price);
				psUpdSMEShareRI.setLong(18, objShare.sequence_no);
				psUpdSMEShareRI.setDouble(19, objShare.todays_close_price);
				psUpdSMEShareRI.setDouble(20, objShare.Todays_open);
				psUpdSMEShareRI.setDouble(21, objShare.turnover);
				psUpdSMEShareRI.setDouble(22, objShare.volume_traded);
				psUpdSMEShareRI.setDouble(23, objShare.yield);
				psUpdSMEShareRI.setInt(24, objShare.market_status);
				// Added for Rights Issue for reference price upper limt and
				// reference price lower limit
				psUpdSMEShareRI.setInt(25, objShare.reference_price_upperlimit);
				psUpdSMEShareRI.setInt(26, objShare.reference_price_lowerlimit);

				psUpdSMEShareRI.setString(27, objShare.fk_te_symbol); 

				psUpdSMEShareRI.addBatch();
	

				return true;
			} catch (SQLException e) {
				logger.error("Exception in updateSMEShareRIData: " + e, e);

			}


		if (objShare != null)
			objShare = null;
		return false;
	}
	
	/**
	 * This method will invoke the EOD process for Share Market
	 * 
	 * @param objMarket
	 *            contains attributes of EFP.TE_MARKET table
	 * @return boolean
	 */
	// Added for SME
	public boolean EODforSMEShare() {

			try {
				logger.debug("inside  DB2Manager EODforSMEShare function");
				// Changed for VWAP Price changes
				// psEODShareMarketUpdTeStock.executeUpdate();
				
				
				
				
				
				
				
				psEODSMEShareMarketDelHsTeStock.executeUpdate();
				psEODSMEShareMarketInsTeStock.executeUpdate();
				logger.info("EODforSMEShare: Done");

				return true;
			} catch (SQLException e) {
				logger.error("Exception in EODforSMEShare: " + e, e);

			}
	

		logger.debug("Exit  DB2Manager EODforSMEShare function");
		return false;
	}

	/**
	 * This method will invoke the EOD process for Share Market
	 * 
	 * @param objMarket
	 *            contains attributes of EFP.TE_MARKET table
	 * @return boolean
	 */
	// Added for SME
	public boolean EODforSMEShare_Adj() {


			try {
				logger.debug("inside  DB2Manager EODforSMEShare_Adj function");
				// Changed for VWAP Price changes
				// psEODShareMarketUpdTeStock.executeUpdate();
				
				
				
				
				psEODSMEShareMarketDelHsTeStock_Adj.executeUpdate();
				psEODSMEShareMarketInsTeStock_Adj.executeUpdate();
				logger.info("EODforSMEShare_Adj: Done");

				return true;
			} catch (SQLException e) {
				logger.error("Exception in EODforSMEShare_Adj: " + e, e);
	
			}


		logger.debug("Exit  DB2Manager EODforSMEShare_Adj function");
		return false;
	}

	/**
	 * This method will invoke the EOD process for Share RI
	 * 
	 * @param objMarket
	 *            contains attributes of EFP.TE_MARKET table
	 * @return boolean
	 */
	// Added for SME
	public boolean EODforSMEShareRI() {


			try {
				logger.debug("inside  DB2Manager EODforSMEShareRI function");

				psEODSMEShareMarketDelHsTeRIStock.executeUpdate();
				psEODSMEShareMarketInsTeRIStock.executeUpdate();

				logger.info("EODforSMEShareRI: Done");

				return true;
			} catch (SQLException e) {
				logger.error("Exception in EODforSMEShare RI: " + e, e);
	
			}

		logger.debug("Exit  DB2Manager EODforSMEShare RI function");
		return false;
	}

	public void updateBestBidOffer(TIP_SMEShareVO objShare) {
		if ("OBk".equals(objShare.messageType)) {
			logger.info("OBk BBBO SME updateBestBidOffer in NOMU Market {}", objShare);
			updateBestBidOffer(objShare.fk_te_symbol, objShare.last_update_time, objShare.bestBids, objShare.bestOffers,
					"S");
		} else {
			logger.info("OBk BBBO MAIN updateBestBidOffer Message seems not OBK {}", objShare.messageType);
		}

	}

}
