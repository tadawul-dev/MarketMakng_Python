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
import java.sql.Timestamp;

import oracle.jdbc.pool.OracleDataSource;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import sa.com.tadawul.website.tip.vo.BestBidOffVO;
import sa.com.tadawul.website.tip.vo.TIP_BondVO;
import sa.com.tadawul.website.tip.vo.TIP_SMEShareVO;
import sa.com.tadawul.website.tip.vo.TIP_ShareVO;


/**
 * @description This class is used to get Database Connection and all the
 *              prepared Statements.
 *
 */
public class DBManagerShare extends DBManagerMarket{


	private static final Logger logger = LogManager.getLogger(DBManagerShare.class);
	private PreparedStatement psInitializeDBforShareMarket12;
	private PreparedStatement psInitializeDBforShareRIMarket12;
	private PreparedStatement psInitializeDBforShareETFMarket;
	private PreparedStatement psUpdShare;

	private PreparedStatement psUpdShareRI;
	private PreparedStatement psUpdShareETF;
	private PreparedStatement psEODShareMarketUpdTeETFStock;
	private PreparedStatement psEODShareMarketDelHsTeStock;
	private PreparedStatement psEODShareMarketDelHsTeStock_Adj;
	private PreparedStatement psEODShareMarketDelHsTeRIStock;
	private PreparedStatement psEODShareMarketDelHsETFStock;
	private PreparedStatement psEODShareMarketInsTeStock;
	private PreparedStatement psEODShareMarketInsTeStock_Adj;
	private PreparedStatement psEODShareMarketInsTeRIStock;
	private PreparedStatement psEODShareMarketInsETFStock;
	private PreparedStatement psEODShareMarketDelHsETFMarket;
	private PreparedStatement psEODShareMarketInsHsETFMarket;
	@SuppressWarnings("unused")
	private int totalShareCount = 0;
	
	static long  lastUpdatedTime = 0;
	static long currentUpdatedTime = 0;
	
	public DBManagerShare(String threadName) throws Exception {
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

				sbPrepareQueries.append("UPDATE EFP.TE_STOCK SET BID_PRICE = 0, BID_QUANTITY = 0, ASK_PRICE = 0, ASK_QUANTITY = 0, LAST_TRADE_PRICE = 0, "
								+ "LAST_TRADE_QUANTITY = 0, NET_CHANGE = 0, HIGH_PRICE = 0, LOW_PRICE = 0, LAST_UPDATE_TIME = systimestamp, PERCENT_CHANGE = 0, "
								+ "SEQUENCE_NO = 0, TURNOVER = 0, VOLUME_TRADED = 0, TODAYS_OPEN = 0, PBR = 0, PER = 0, YIELD = 0, NO_OF_TRADES = 0, MARKET_STATUS = 0, TODAYS_CLOSE_PRICE = 0");
				psInitializeDBforShareMarket12 = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				// Added for Rights Issue for Reference Price Lower limit
				// and Reference price upper limit
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				 sbPrepareQueries.append("UPDATE EFP.RI_STOCK SET BID_PRICE = 0, BID_QUANTITY = 0, ASK_PRICE = 0, ASK_QUANTITY = 0, LAST_TRADE_PRICE = 0, "
								+ "LAST_TRADE_QUANTITY = 0, NET_CHANGE = 0, HIGH_PRICE = 0, LOW_PRICE = 0, LAST_UPDATE_TIME = systimestamp , PERCENT_CHANGE = 0, "
								+ "SEQUENCE_NO = 0, TURNOVER = 0, VOLUME_TRADED = 0, TODAYS_OPEN = 0, PBR = 0, PER = 0, YIELD = 0, NO_OF_TRADES = 0, MARKET_STATUS = 0, TODAYS_CLOSE_PRICE = 0 ");
				psInitializeDBforShareRIMarket12 = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				// Added for Share ETF
			
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("UPDATE EFP.ETF_STOCK SET BID_PRICE = 0, BID_QUANTITY = 0, ASK_PRICE = 0, ASK_QUANTITY = 0, LAST_TRADE_PRICE = 0, "
								+ "LAST_TRADE_QUANTITY = 0, NET_CHANGE = 0, HIGH_PRICE = 0, LOW_PRICE = 0, LAST_UPDATE_TIME = systimestamp, PERCENT_CHANGE = 0, "
								+ "SEQUENCE_NO = 0, TURNOVER = 0, VOLUME_TRADED = 0, TODAYS_OPEN = 0, PBR = 0, PER = 0, YIELD = 0, NO_OF_TRADES = 0, MARKET_STATUS = 0, TODAYS_CLOSE_PRICE = 0");
				psInitializeDBforShareETFMarket = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				// Added the Indicative Opening details
				sbPrepareQueries.append("UPDATE EFP.TE_STOCK SET TRANSACTION_DATE = ?, LAST_TRADE_PRICE = ?, LAST_TRADE_QUANTITY = ?,"
								+ " LAST_UPDATE_TIME = ?, ASK_PRICE = ?, ASK_QUANTITY = ?, BID_PRICE = ?, BID_QUANTITY = ?, NET_CHANGE = ?, "
								+ "HIGH_PRICE = ?, LOW_PRICE = ?, MARKET_CAP = ?, "
								+ "NO_OF_TRADES = ?, PBR = ?, PER = ?, PERCENT_CHANGE = ?, PREVIOUS_CLOSE_PRICE = ?, SEQUENCE_NO = ?, "
								+ "TODAYS_CLOSE_PRICE = ?, TODAYS_OPEN = ?, TURNOVER = ?, VOLUME_TRADED = ?, YIELD = ?, MARKET_STATUS = ?,  "
								+ " INDICATIVEPRICE = ?, INDICATIVEVOLUME = ? WHERE FK_TE_SYMBOL = ?");

				psUpdShare = g_oConnection.prepareStatement(sbPrepareQueries.toString());
				
			

				// Added for Rights Issues changes for reference
				// PriceUpperlimit and Reference price lower limit
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("UPDATE EFP.RI_STOCK SET TRANSACTION_DATE = ?, LAST_TRADE_PRICE = ?, LAST_TRADE_QUANTITY = ?,"
								+ " LAST_UPDATE_TIME = ?, ASK_PRICE = ?, ASK_QUANTITY = ?, BID_PRICE = ?, BID_QUANTITY = ?, NET_CHANGE = ?, "
								+ "HIGH_PRICE = ?, LOW_PRICE = ?, MARKET_CAP = ?, "
								+ "NO_OF_TRADES = ?, PBR = ?, PER = ?, PERCENT_CHANGE = ?, PREVIOUS_CLOSE_PRICE = ?, SEQUENCE_NO = ?, "
								+ "TODAYS_CLOSE_PRICE = ?, TODAYS_OPEN = ?, TURNOVER = ?, VOLUME_TRADED = ?, YIELD = ?, MARKET_STATUS = ? , "
								+ "  REFERENCEPRICE_UPPERLIMIT = ?, REFERENCEPRICE_LOWERLIMIT = ? WHERE FK_RI_SYMBOL = ?");
				psUpdShareRI = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				// Added for Share ETF
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("UPDATE EFP.ETF_STOCK SET TRANSACTION_DATE = ?, LAST_TRADE_PRICE = ?, LAST_TRADE_QUANTITY = ?,"
								+ " LAST_UPDATE_TIME = ?, ASK_PRICE = ?, ASK_QUANTITY = ?, BID_PRICE = ?, BID_QUANTITY = ?, NET_CHANGE = ?, "
								+ "HIGH_PRICE = ?, LOW_PRICE = ?, MARKET_CAP = ?, "
								+ "NO_OF_TRADES = ?, PBR = ?, PER = ?, PERCENT_CHANGE = ?, PREVIOUS_CLOSE_PRICE = ?, SEQUENCE_NO = ?, "
								+ "TODAYS_CLOSE_PRICE = ?, TODAYS_OPEN = ?, TURNOVER = ?, VOLUME_TRADED = ?, YIELD = ?, MARKET_STATUS = ? "
								+ " WHERE FK_ETF_SYMBOL = ?");

				psUpdShareETF = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				// Added for ShareETF
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("UPDATE EFP.ETF_STOCK SET LAST_TRADE_PRICE=TODAYS_CLOSE_PRICE");
				psEODShareMarketUpdTeETFStock = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				// EOD For Share Market
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("DELETE from WEB.HS_TE_STOCK where trunc(pk_hs_te_stock) = trunc(sysdate) ");
				psEODShareMarketDelHsTeStock = g_oConnection	.prepareStatement(sbPrepareQueries.toString());

				// EOD For Share Market Adjusted
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("DELETE from WEB.HS_TE_STOCK_ADJUSTED where trunc(pk_hs_te_stock) = trunc(sysdate) ");
				psEODShareMarketDelHsTeStock_Adj = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				// Added for ShareRI
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("DELETE from WEB.HS_RI_STOCK where trunc(pk_hs_ri_stock) = trunc(sysdate) ");
				psEODShareMarketDelHsTeRIStock = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				// Added for ShareETF
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("DELETE from WEB.HS_ETF_STOCK where trunc(pk_hs_ETF_stock) = trunc(sysdate) ");
				psEODShareMarketDelHsETFStock = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				// For SR6 VWAP Changes
				// Added for Indicative Opening changes
				sbPrepareQueries.append("INSERT INTO WEB.HS_TE_STOCK ( FK_TE_SYMBOL, TRANSACTION_DATE, LAST_TRADE_PRICE, LAST_TRADE_QUANTITY, "
								+ " LAST_UPDATE_TIME, ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY, NET_CHANGE, HIGH_PRICE, HIGH_52_WEEKS_PRICE, LOW_PRICE, "
								+ " LOW_52_WEEKS_PRICE, MARKET_CAP, NO_OF_TRADES, PBR, PER, PERCENT_CHANGE, PREVIOUS_CLOSE_PRICE, SEQUENCE_NO, TODAYS_CLOSE_PRICE, "
								+ " TODAYS_OPEN, TURNOVER, VOLUME_TRADED, YIELD, PK_HS_TE_STOCK , HIGH_52_WEEKS_DATE, LOW_52_WEEKS_DATE,INDICATIVEPRICE, INDICATIVEVOLUME ) SELECT distinct FK_TE_SYMBOL, "
								+ " systimestamp, LAST_TRADE_PRICE, LAST_TRADE_QUANTITY, LAST_UPDATE_TIME, ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY, "
								+ " (TODAYS_CLOSE_PRICE - PREVIOUS_CLOSE_PRICE ) AS NET_CHANGE, HIGH_PRICE, HIGH_52_WEEKS_PRICE, LOW_PRICE, LOW_52_WEEKS_PRICE, "
								+ " MARKET_CAP, NO_OF_TRADES, PBR, PER, ROUND(100 * to_number( CASE WHEN PREVIOUS_CLOSE_PRICE IS NULL OR PREVIOUS_CLOSE_PRICE =0 OR "
								+ " TODAYS_CLOSE_PRICE IS NULL OR TODAYS_CLOSE_PRICE = 0 THEN 0.00 ELSE to_number((TODAYS_CLOSE_PRICE - PREVIOUS_CLOSE_PRICE )) / "
								+ " to_number( PREVIOUS_CLOSE_PRICE) END),2) AS PERCENT_CHANGE, PREVIOUS_CLOSE_PRICE, SEQUENCE_NO, TODAYS_CLOSE_PRICE, TODAYS_OPEN, "
								+ " TURNOVER, VOLUME_TRADED, YIELD, systimestamp , HIGH_52_WEEKS_DATE, LOW_52_WEEKS_DATE, INDICATIVEPRICE, INDICATIVEVOLUME  FROM EFP.TE_STOCK WHERE  TO_CHAR(LISTING_DATE,'yyyyMMdd')!='39990101' AND  fk_te_symbol IN "
								+ " ( SELECT to_CHAR(pk_rf_company) FROM sacp.rf_company WHERE active =1 )");


				psEODShareMarketInsTeStock = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				// Added for SR6 VWAP
				// Added for Indicative Opening changes
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("INSERT INTO WEB.HS_TE_STOCK_ADJUSTED (FK_TE_SYMBOL, TRANSACTION_DATE, LAST_TRADE_PRICE, LAST_TRADE_QUANTITY, "
								+ " LAST_UPDATE_TIME, ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY, NET_CHANGE, HIGH_PRICE,  HIGH_52_WEEKS_PRICE, LOW_PRICE, LOW_52_WEEKS_PRICE, "
								+ " MARKET_CAP, NO_OF_TRADES, PBR, PER,  PERCENT_CHANGE, PREVIOUS_CLOSE_PRICE, SEQUENCE_NO, TODAYS_CLOSE_PRICE, TODAYS_OPEN, TURNOVER, VOLUME_TRADED, "
								+ " YIELD, PK_HS_TE_STOCK , HIGH_52_WEEKS_DATE, LOW_52_WEEKS_DATE, INDICATIVEPRICE, INDICATIVEVOLUME ) SELECT distinct FK_TE_SYMBOL, systimestamp, LAST_TRADE_PRICE, LAST_TRADE_QUANTITY, LAST_UPDATE_TIME,  "
								+ " ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY, (TODAYS_CLOSE_PRICE - PREVIOUS_CLOSE_PRICE ) AS  NET_CHANGE, HIGH_PRICE, HIGH_52_WEEKS_PRICE,  "
								+ " LOW_PRICE, LOW_52_WEEKS_PRICE, MARKET_CAP, NO_OF_TRADES, PBR, PER, Round(100 * to_number(CASE WHEN PREVIOUS_CLOSE_PRICE IS NULL OR PREVIOUS_CLOSE_PRICE=0 "
								+ " OR TODAYS_CLOSE_PRICE IS NULL OR TODAYS_CLOSE_PRICE = 0 THEN 0.00 ELSE to_number((TODAYS_CLOSE_PRICE - PREVIOUS_CLOSE_PRICE ))/to_number(PREVIOUS_CLOSE_PRICE) END),2)"
								+ " AS PERCENT_CHANGE, PREVIOUS_CLOSE_PRICE,  SEQUENCE_NO, TODAYS_CLOSE_PRICE, TODAYS_OPEN, TURNOVER, VOLUME_TRADED, YIELD, systimestamp  , HIGH_52_WEEKS_DATE, "
								+ " LOW_52_WEEKS_DATE,INDICATIVEPRICE, INDICATIVEVOLUME  FROM EFP.TE_STOCK WHERE TO_CHAR(LISTING_DATE,'yyyyMMdd')!='39990101' AND fk_te_symbol in (select to_char(pk_rf_company) from sacp.rf_company where active =1)");
			

				psEODShareMarketInsTeStock_Adj = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				// Added for Share RI
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				// Added for the RI Changes for reference price upper limit
				// and reference price lower limit
				// Changed for VWAP close Price
				sbPrepareQueries.append("INSERT INTO WEB.HS_RI_STOCK (FK_RI_SYMBOL, TRANSACTION_DATE, LAST_TRADE_PRICE, LAST_TRADE_QUANTITY, LAST_UPDATE_TIME, ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY, NET_CHANGE, HIGH_PRICE, "
						+ " HIGH_52_WEEKS_PRICE, LOW_PRICE, LOW_52_WEEKS_PRICE, MARKET_CAP, NO_OF_TRADES, PBR, PER,  PERCENT_CHANGE, PREVIOUS_CLOSE_PRICE, SEQUENCE_NO, TODAYS_CLOSE_PRICE, TODAYS_OPEN, TURNOVER, VOLUME_TRADED, YIELD, "
						+ " PK_HS_RI_STOCK , HIGH_52_WEEKS_DATE, LOW_52_WEEKS_DATE , REFERENCEPRICE_UPPERLIMIT, REFERENCEPRICE_LOWERLIMIT  ) SELECT FK_RI_SYMBOL, systimestamp, LAST_TRADE_PRICE, LAST_TRADE_QUANTITY, LAST_UPDATE_TIME,  "
						+ " ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY, (TODAYS_CLOSE_PRICE - PREVIOUS_CLOSE_PRICE ) AS  NET_CHANGE, HIGH_PRICE, HIGH_52_WEEKS_PRICE,  LOW_PRICE, LOW_52_WEEKS_PRICE, MARKET_CAP, NO_OF_TRADES, PBR, "
						+ " PER, Round(100 * to_number(CASE WHEN PREVIOUS_CLOSE_PRICE IS NULL OR PREVIOUS_CLOSE_PRICE=0   OR TODAYS_CLOSE_PRICE IS NULL OR TODAYS_CLOSE_PRICE = 0 THEN 0.00 ELSE to_number((TODAYS_CLOSE_PRICE - PREVIOUS_CLOSE_PRICE ))/to_number(PREVIOUS_CLOSE_PRICE) END),2) AS PERCENT_CHANGE, "
						+ " PREVIOUS_CLOSE_PRICE,  SEQUENCE_NO, TODAYS_CLOSE_PRICE, TODAYS_OPEN, TURNOVER, VOLUME_TRADED, YIELD, systimestamp  , HIGH_52_WEEKS_DATE, LOW_52_WEEKS_DATE , REFERENCEPRICE_UPPERLIMIT, REFERENCEPRICE_LOWERLIMIT   FROM EFP.RI_STOCK WHERE TO_CHAR(LISTING_DATE,'yyyyMMdd')!='39990101' AND fk_ri_symbol in (select to_char(pk_rf_company) from web.ri_company where active =1)");
				psEODShareMarketInsTeRIStock = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				// Added for Share ETF
				sbPrepareQueries.delete(0, sbPrepareQueries.length());

				sbPrepareQueries.append("INSERT INTO WEB.HS_ETF_STOCK (FK_ETF_SYMBOL, TRANSACTION_DATE, LAST_TRADE_PRICE, LAST_TRADE_QUANTITY, LAST_UPDATE_TIME, ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY, "
						+ " NET_CHANGE, HIGH_PRICE,  HIGH_52_WEEKS_PRICE, LOW_PRICE, LOW_52_WEEKS_PRICE, MARKET_CAP, NO_OF_TRADES, PBR, PER,  PERCENT_CHANGE, PREVIOUS_CLOSE_PRICE, SEQUENCE_NO, TODAYS_CLOSE_PRICE, "
						+ " TODAYS_OPEN, TURNOVER, VOLUME_TRADED, YIELD, PK_HS_ETF_STOCK , HIGH_52_WEEKS_DATE, LOW_52_WEEKS_DATE ) SELECT FK_ETF_SYMBOL, systimestamp, LAST_TRADE_PRICE, LAST_TRADE_QUANTITY, LAST_UPDATE_TIME,  "
						+ " ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY, NET_CHANGE, HIGH_PRICE, HIGH_52_WEEKS_PRICE,  LOW_PRICE, LOW_52_WEEKS_PRICE, MARKET_CAP, NO_OF_TRADES, PBR, PER, PERCENT_CHANGE, PREVIOUS_CLOSE_PRICE, "
						+ "  SEQUENCE_NO, TODAYS_CLOSE_PRICE, TODAYS_OPEN, TURNOVER, VOLUME_TRADED, YIELD, systimestamp  , HIGH_52_WEEKS_DATE, LOW_52_WEEKS_DATE  FROM EFP.ETF_STOCK  WHERE TO_CHAR(LISTING_DATE,'yyyyMMdd')!='39990101' ");
				psEODShareMarketInsETFStock = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				// Added for Share ETFpsEODShareMarketInsHsETFMarket
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("DELETE from WEB.HS_ETF_MARKET where trunc(TRANSACTION_DATE) = trunc(sysdate)");
				psEODShareMarketDelHsETFMarket = g_oConnection	.prepareStatement(sbPrepareQueries.toString());

				// Added for Share ETF
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("INSERT INTO WEB.HS_ETF_MARKET(TRANSACTION_DATE,NO_OF_TRADES,TURNOVER,VOLUME_TRADED) "
								+ "SELECT systimestamp,sum(NO_OF_TRADES),sum(TURNOVER),sum(VOLUME_TRADED) from EFP.ETF_STOCK ");
				psEODShareMarketInsHsETFMarket = g_oConnection.prepareStatement(sbPrepareQueries.toString());


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
			
			
			if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.UPDATER_THREADS_DB_UPDATE_INTERVAL)
			{
			
						try {
							logger.debug("ShareUpd in ShareUpdate_batchExecute Executed");
							psUpdShare.executeBatch();
						}catch (BatchUpdateException be2) {
							logger.error("BatchUpdateException in commitTrans() while doing psUpdShare "+ be2);
							try {
								psUpdShare.clearBatch();
							} catch (SQLException e1) {
								logger.error("Exception in psUpdShare in clearBatch(): " + e1, e1);
							} 
						}catch (SQLException ex) {							
							logger.error(	"Exception in psUpdShare in ShareUpdate_batchExecute: "	+ ex, ex);
						}
						try {
							logger.debug("ShareUpdRI in ShareUpdate_batchExecute Executed");
							psUpdShareRI.executeBatch();
						}catch (BatchUpdateException be2) {
							logger.error("BatchUpdateException in commitTrans() while doing psUpdShareRI "+ be2);
							try {
								psUpdShareRI.clearBatch();
							} catch (SQLException e1) {
								logger.error("Exception in psUpdShareRI in clearBatch(): " + e1, e1);
							} 
						}
						catch (SQLException ex) {						
							logger.error(	"Exception in psUpdShareRI in ShareUpdate_batchExecute: "	+ ex, ex);
						}
				
						try {
							logger.debug("ShareUpdETF in ShareUpdate_batchExecute Executed");
							psUpdShareETF.executeBatch();
						} catch (BatchUpdateException be2) {
							logger.error("BatchUpdateException in commitTrans() while doing psUpdShareETF "+ be2);
							try {
								psUpdShareETF.clearBatch();
							} catch (SQLException e1) {
								logger.error("Exception in psUpdShareETF in clearBatch(): " + e1, e1);
							} 
						}
						catch (SQLException ex) {							
							logger.error("Exception in psUpdShareETF in ShareUpdate_batchExecute: "	+ ex, ex);
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


			if (psInitializeDBforShareMarket12 != null){
				psInitializeDBforShareMarket12.close();
			}
			if (psInitializeDBforShareRIMarket12 != null){
				psInitializeDBforShareRIMarket12.close();
			}
			if (psInitializeDBforShareETFMarket != null){
				psInitializeDBforShareETFMarket.close();
			}
			if (psUpdShare != null){
				psUpdShare.close();
			}
			if (psUpdShareRI != null){
				psUpdShareRI.close();
			}
			if (psUpdShareETF != null){
				psUpdShareETF.close();
			}
			if (psEODShareMarketUpdTeETFStock != null){
				psEODShareMarketUpdTeETFStock.close();
			}
			if (psEODShareMarketDelHsTeStock != null){
				psEODShareMarketDelHsTeStock.close();
			}
			if (psEODShareMarketDelHsTeStock_Adj != null){
				psEODShareMarketDelHsTeStock_Adj.close();
			}
			if (psEODShareMarketDelHsTeRIStock != null){
				psEODShareMarketDelHsTeRIStock.close();
			}

			if (psEODShareMarketDelHsETFStock != null){
				psEODShareMarketDelHsETFStock.close();
			}
			
			if (psEODShareMarketInsTeStock != null){
				psEODShareMarketInsTeStock.close();
			}
			
			if (psEODShareMarketInsTeStock_Adj != null){
				psEODShareMarketInsTeStock_Adj.close();
			}
			
			if (psEODShareMarketInsTeRIStock != null){
				psEODShareMarketInsTeRIStock.close();
			}
			
			if (psEODShareMarketInsETFStock != null){
				psEODShareMarketInsETFStock.close();
			}
			
			if (psEODShareMarketDelHsETFMarket != null){
				psEODShareMarketDelHsETFMarket.close();
			}
			
			if (psEODShareMarketInsHsETFMarket != null){
				psEODShareMarketInsHsETFMarket.close();
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
	public boolean initializeDBforShare() {

			// logger.debug("**************bod FOR sHARE IS cALLED***********");
			try {
				logger.debug("inside  DB2Manager initializeDBforShare function");
				logger.debug("initializeDBforShare --> dtFeedReqDate = "
						+ dtFeedReqDate);
				logger.debug("inside initializeDBforShare --> dtShareIndexLastUpdateTime = "
						+ LoadInitialData.dtShareIndexLastUpdateTime);
				if (!LoadInitialData.dtShareIndexLastUpdateTime.equals(dtFeedReqDate)) {
					psInitializeDBforShareMarket12.executeUpdate();
					initilizeOrderBook("M");
					commitTrans(true);
					logger.info("initializeDBforShare: EFP.TE_STOCK table columns initialized to zero");
				}
				return true;
		
			} catch (SQLException e) {
				logger.error("Exception in initializeDBforShare: " + e, e);

			}
		

		logger.debug("Exit  DB2Manager initializeDBforShare function");
		return false;
	}
	
	/**
	 * This method will initialize Share RI table at BOD
	 * 
	 * @return boolean
	 */
	public boolean initializeDBforShareRI() {

			
			try {
				// logger.debug("*************bod FOR sHARE RI IS cALLED***********");
				logger.debug("inside  DB2Manager initializeDBforShareRI function");
				logger.debug("initializeDBforShare RI--> dtFeedReqDate = "
						+ dtFeedReqDate);
				logger.debug("inside initializeDBforShare RI--> dtShareIndexLastUpdateTime = "
						+ LoadInitialData.dtShareIndexLastUpdateTime);
				/*
				 * To be anlyzed whether we need another variable for
				 * dtShareIndexLastUpdateTime
				 */
				logger.debug(dtShareIndexLastUpdateTime + "***********"
						+ dtFeedReqDate);
				if (!LoadInitialData.dtShareIndexLastUpdateTime.equals(dtFeedReqDate)) {
					// logger.debug("Inside if");
					psInitializeDBforShareRIMarket12.executeUpdate();
					commitTrans(true);
					logger.info("initializeDBforShareRI: EFP.RI_STOCK table columns initialized to zero");
				}
				// logger.debug("Down IF");
				
				return true;
			} catch (SQLException e) {
				logger.error("Exception in initializeDBforShareRI: " + e, e);
			}

		logger.debug("Exit  DB2Manager initializeDBforShareRI function");
		return false;
	}
	
	// Added for Share ETF
	/**
	 * This method will initialize Share ETF table at BOD
	 * 
	 * @return boolean
	 */
	public boolean initializeDBforShareETF() {

			try {
				logger.debug("*************bod FOR sHARE ETF IS cALLED***********");
				logger.debug("inside  DB2Manager initializeDBforShareETF function");
				if (logger.isDebugEnabled()) logger.debug("initializeDBforShare ETF--> dtFeedReqDate = {}", dtFeedReqDate);
				if (logger.isDebugEnabled()) logger.debug("inside initializeDBforShare ETF--> dtShareIndexLastUpdateTime = {}", LoadInitialData.dtShareIndexLastUpdateTime);
				/*
				 * To be anlyzed whether we need another variable for
				 * dtShareIndexLastUpdateTime
				 */
				if (logger.isDebugEnabled()) logger.debug(dtShareIndexLastUpdateTime + "***********"+ dtFeedReqDate);
				if (!LoadInitialData.dtShareIndexLastUpdateTime.equals(dtFeedReqDate)) {
					logger.debug("Inside if");
					psInitializeDBforShareETFMarket.executeUpdate();
					commitTrans(true);
					logger.info("initializeDBforShareETF: EFP.ETF_STOCK table columns initialized to zero");
				}
								
				return true;
				
			} catch (SQLException e) {
				logger.error("Exception in initializeDBforShareETF: " + e, e);

			}


		logger.debug("Exit  DB2Manager initializeDBforShareETF function");
		return false;
	}
	
	/**
	 * This method will updates the table EFP.TE_STOCK SET
	 * 
	 * @param objShare
	 *            contains attributes of EFP.TE_STOCK SET table
	 * @return boolean
	 */
	public boolean updateShareData(TIP_ShareVO objShare) {
		 logger.info(" updateShareData has been started ");

			try {

				psUpdShare.setTimestamp(1, objShare.transaction_date);
				psUpdShare.setDouble(2, objShare.last_trade_price);
				psUpdShare.setInt(3, objShare.last_trade_quantity);
				psUpdShare.setTimestamp(4, objShare.last_update_time);
				psUpdShare.setDouble(5, objShare.ask_price);
				psUpdShare.setLong(6, objShare.ask_quantity);
				psUpdShare.setDouble(7, objShare.bid_price);
				psUpdShare.setDouble(8, objShare.bid_quantity);
				psUpdShare.setDouble(9, objShare.net_change);
				psUpdShare.setDouble(10, objShare.high_price);
				psUpdShare.setDouble(11, objShare.low_price);
				psUpdShare.setDouble(12, objShare.market_cap);
				psUpdShare.setLong(13, objShare.no_of_trades);
				psUpdShare.setDouble(14, objShare.Pbr);
				psUpdShare.setDouble(15, objShare.Per);
				psUpdShare.setDouble(16, objShare.percent_change);
				psUpdShare.setDouble(17, objShare.Previous_close_price);
				psUpdShare.setLong(18, objShare.sequence_no);
				psUpdShare.setDouble(19, objShare.todays_close_price);
				psUpdShare.setDouble(20, objShare.Todays_open);
				psUpdShare.setDouble(21, objShare.turnover);
				psUpdShare.setDouble(22, objShare.volume_traded);
				psUpdShare.setDouble(23, objShare.yield);
				psUpdShare.setInt(24, objShare.market_status);
				// Added for indicative Opening change
				psUpdShare.setDouble(25, objShare.indicative_Open_price);
				psUpdShare.setDouble(26, objShare.indicative_Open_volume);
				psUpdShare.setString(27, objShare.fk_te_symbol); 
				
				totalShareCount++;
				
				psUpdShare.addBatch();

				return true;
				
			} catch (SQLException e) {
				logger.error("Exception in updateShareData: " + e, e);
			}


		if (objShare != null)
			objShare = null;
		
		return false;
	}
	
	
	
	public void updateBestBidOffer(TIP_ShareVO objShare) {
		if ("OBk".equals(objShare.messageType)) {
			logger.info("OBk BBBO MAIN updateBestBidOffer MainMarket {}", objShare);
			updateBestBidOffer(objShare.fk_te_symbol, objShare.last_update_time, objShare.bestBids, objShare.bestOffers,
					"M");
		} else {
			logger.info("OBk BBBO MAIN updateBestBidOffer Message seems not OBK {}", objShare.messageType);
		}
	}


	public boolean ShareUpdate_batchExecute(boolean isCommit) {

		boolean isFailed = false;

		currentUpdatedTime =  System.currentTimeMillis();		
		
	
		if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.UPDATER_THREADS_DB_UPDATE_INTERVAL )
		{
		
					try {
						logger.debug("ShareUpd in ShareUpdate_batchExecute Executed");
						psUpdShare.executeBatch();
					} catch (SQLException ex) {
						isFailed = true;
						logger.error(	"Exception in psUpdShare in ShareUpdate_batchExecute: "	+ ex, ex);
					}
					try {
						logger.debug("ShareUpdRI in ShareUpdate_batchExecute Executed");
						psUpdShareRI.executeBatch();
					} catch (SQLException ex) {
						isFailed = true;
						logger.error(	"Exception in psUpdShareRI in ShareUpdate_batchExecute: "	+ ex, ex);
					}
			
					try {
						logger.debug("ShareUpdETF in ShareUpdate_batchExecute Executed");
						psUpdShareETF.executeBatch();
					} catch (SQLException ex) {
						isFailed = true;
						logger.error("Exception in psUpdShareETF in ShareUpdate_batchExecute: "	+ ex, ex);
					}					
					lastUpdatedTime = currentUpdatedTime;
		}
		// End
		if (isFailed){
			return false;
		}
		return true;
	}
	
	/**
	 * This method will updates the table EFP.TE_RISTOCK SET
	 * 
	 * @param objShare
	 *            contains attributes of EFP.TE_RISTOCK SET table
	 * @return boolean
	 */
	public boolean updateShareRIData(TIP_ShareVO objShare) {


			try {

				psUpdShareRI.setTimestamp(1, objShare.transaction_date);
				psUpdShareRI.setDouble(2, objShare.last_trade_price);
				psUpdShareRI.setInt(3, objShare.last_trade_quantity);
				psUpdShareRI.setTimestamp(4, objShare.last_update_time);
				psUpdShareRI.setDouble(5, objShare.ask_price);
				psUpdShareRI.setLong(6, objShare.ask_quantity);
				psUpdShareRI.setDouble(7, objShare.bid_price);
				psUpdShareRI.setDouble(8, objShare.bid_quantity);
				psUpdShareRI.setDouble(9, objShare.net_change);
				psUpdShareRI.setDouble(10, objShare.high_price);
				psUpdShareRI.setDouble(11, objShare.low_price);
				psUpdShareRI.setDouble(12, objShare.market_cap);
				psUpdShareRI.setLong(13, objShare.no_of_trades);
				psUpdShareRI.setDouble(14, objShare.Pbr);
				psUpdShareRI.setDouble(15, objShare.Per);
				psUpdShareRI.setDouble(16, objShare.percent_change);
				psUpdShareRI.setDouble(17, objShare.Previous_close_price);
				psUpdShareRI.setLong(18, objShare.sequence_no);
				psUpdShareRI.setDouble(19, objShare.todays_close_price);
				psUpdShareRI.setDouble(20, objShare.Todays_open);
				psUpdShareRI.setDouble(21, objShare.turnover);
				psUpdShareRI.setDouble(22, objShare.volume_traded);
				psUpdShareRI.setDouble(23, objShare.yield);
				psUpdShareRI.setInt(24, objShare.market_status);
				// Added for Rights Issue for reference price upper limt and
				// reference price lower limit
				psUpdShareRI.setInt(25, objShare.reference_price_upperlimit);
				psUpdShareRI.setInt(26, objShare.reference_price_lowerlimit);

				psUpdShareRI.setString(27, objShare.fk_te_symbol); 

				psUpdShareRI.addBatch();
				

				totalShareCount++;
				return true;
			} catch (SQLException e) {
				logger.error("Exception in updateShareRIData: " + e, e);

			}


		if (objShare != null)
			objShare = null;
		
		return false;
	}
	
	/**
	 * This method will updates the table EFP.ETF_STOCK SET
	 * 
	 * @param objShare
	 *            contains attributes of EFP.ETF_STOCK SET table
	 * @return boolean
	 */
	public boolean updateShareETFData(TIP_ShareVO objShare) {

			try {

				psUpdShareETF.setTimestamp(1, objShare.transaction_date);
				psUpdShareETF.setDouble(2, objShare.last_trade_price);
				psUpdShareETF.setInt(3, objShare.last_trade_quantity);
				psUpdShareETF.setTimestamp(4, objShare.last_update_time);
				psUpdShareETF.setDouble(5, objShare.ask_price);
				psUpdShareETF.setLong(6, objShare.ask_quantity);
				psUpdShareETF.setDouble(7, objShare.bid_price);
				psUpdShareETF.setDouble(8, objShare.bid_quantity);
				psUpdShareETF.setDouble(9, objShare.net_change);
				psUpdShareETF.setDouble(10, objShare.high_price);
				psUpdShareETF.setDouble(11, objShare.low_price);
				psUpdShareETF.setDouble(12, objShare.market_cap);
				psUpdShareETF.setLong(13, objShare.no_of_trades);
				psUpdShareETF.setDouble(14, objShare.Pbr);
				psUpdShareETF.setDouble(15, objShare.Per);
				psUpdShareETF.setDouble(16, objShare.percent_change);
				psUpdShareETF.setDouble(17, objShare.Previous_close_price);
				psUpdShareETF.setLong(18, objShare.sequence_no);
				psUpdShareETF.setDouble(19, objShare.todays_close_price);
				psUpdShareETF.setDouble(20, objShare.Todays_open);
				psUpdShareETF.setDouble(21, objShare.turnover);
				psUpdShareETF.setDouble(22, objShare.volume_traded);
				psUpdShareETF.setDouble(23, objShare.yield);
				psUpdShareETF.setInt(24, objShare.market_status);
				psUpdShareETF.setString(25, objShare.fk_te_symbol); 

				psUpdShareETF.addBatch();
				totalShareCount++;
				return true;
			} catch (SQLException e) {
				logger.error("Exception in updateShareETFData: " + e, e);
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
	public boolean EODforShare() {


			try {
				logger.debug("inside  DB2Manager EODforShare function");
				// Changed for VWAP Price changes
				// psEODShareMarketUpdTeStock.executeUpdate();
				psEODShareMarketDelHsTeStock.executeUpdate();
				psEODShareMarketInsTeStock.executeUpdate();
				logger.info("EODforShare: Done");

				return true;
			} catch (SQLException e) {
				logger.error("Exception in EODforShare: " + e, e);
	
			}

		logger.debug("Exit  DB2Manager EODforShare function");
		return false;
	}
	
	/**
	 * This method will invoke the EOD process for Share Market
	 * 
	 * @param objMarket
	 *            contains attributes of EFP.TE_MARKET table
	 * @return boolean
	 */
	public boolean EODforShare_Adj() {

			try {
				logger.debug("inside  DB2Manager EODforShare_Adj function");
				// Changed for VWAP Price changes
				// psEODShareMarketUpdTeStock.executeUpdate();
				psEODShareMarketDelHsTeStock_Adj.executeUpdate();
				psEODShareMarketInsTeStock_Adj.executeUpdate();
				logger.info("EODforShare_Adj: Done");

				return true;
			} catch (SQLException e) {
				logger.error("Exception in EODforShare_Adj: " + e, e);
			}

		logger.debug("Exit  DB2Manager EODforShare_Adj function");
		return false;
	}

	/**
	 * This method will invoke the EOD process for Share RI
	 * 
	 * @param objMarket
	 *            contains attributes of EFP.TE_MARKET table
	 * @return boolean
	 */
	public boolean EODforShareRI() {

			try {
				logger.debug("inside  DB2Manager EODforShareRI function");
				// psEODShareMarketUpdTeStock.executeUpdate();
				// Changed for VWAP Price changes
				// psEODShareMarketUpdTeRIStock.executeUpdate();
				// psEODShareMarketDelHsTeStock.executeUpdate();
				psEODShareMarketDelHsTeRIStock.executeUpdate();
				// psEODShareMarketInsTeStock.executeUpdate();
				psEODShareMarketInsTeRIStock.executeUpdate();

				logger.info("EODforShareRI: Done");

				return true;
			} catch (SQLException e) {
				logger.error("Exception in EODforShare RI: " + e, e);
	
			}

		logger.debug("Exit  DB2Manager EODforShare RI function");
		return false;
	}

	/**
	 * This method will invoke the EOD process for Share ETF
	 * 
	 * @param objMarket
	 *            contains attributes of EFP.TE_MARKET table
	 * @return boolean
	 */
	public boolean EODforShareETF() {


			try {
				logger.debug("inside  DB2Manager EODforShareETF function");
				// To copy the Close Price Value
				psEODShareMarketUpdTeETFStock.executeUpdate();
				// To Delete the ETF History Table
				psEODShareMarketDelHsETFStock.executeUpdate();
				// To move the ETF_STOCK to HS_ETF_STOCK
				psEODShareMarketInsETFStock.executeUpdate();

				psEODShareMarketDelHsETFMarket.executeUpdate();

				psEODShareMarketInsHsETFMarket.executeUpdate();

				logger.info("EODforShareETF: Done");
				return true;
			} catch (SQLException e) {
				logger.error("Exception in EODforShare ETF: " + e, e);

			}
			
		logger.debug("Exit  DB2Manager EODforShare ETF function");
		return false;
	}

}
