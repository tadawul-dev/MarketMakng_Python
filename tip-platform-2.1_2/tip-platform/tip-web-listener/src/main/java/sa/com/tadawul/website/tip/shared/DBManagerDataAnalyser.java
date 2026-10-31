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
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import oracle.jdbc.pool.OracleDataSource;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import sa.com.tadawul.website.tip.vo.TIP_BondIndexVO;
import sa.com.tadawul.website.tip.vo.TIP_BondVO;
import sa.com.tadawul.website.tip.vo.TIP_IndexVO;
import sa.com.tadawul.website.tip.vo.TIP_SMEIndexVO;
import sa.com.tadawul.website.tip.vo.TIP_SMEShareVO;
import sa.com.tadawul.website.tip.vo.TIP_ShareVO;

/**
 * @description This class is used to get Database Connection and all the
 *              prepared Statements.
 *
 */
public class DBManagerDataAnalyser extends DBManager {

	private static final Logger logger = LogManager.getLogger(DBManagerDataAnalyser.class);
	private PreparedStatement psInsShareSymbol;
	private PreparedStatement psInsBondSymbol;
	private PreparedStatement psInsOTCBondSymbol;
	private PreparedStatement psInsSMESymbol;
	private PreparedStatement psInsIndex;
	private PreparedStatement psInsBondIndex;
	private PreparedStatement psInsSMEIndex;
	private PreparedStatement psInsShare;
	private PreparedStatement psInsShareETF;
	private PreparedStatement psInsShareRI;
	private PreparedStatement psUpdateShare;
	private PreparedStatement psUpdateSMEShare;
	private PreparedStatement psUpdateETFShare;
	private PreparedStatement psUpdateRIShare;
	private PreparedStatement psUpdateRISMEShare;
	
	private PreparedStatement psInsBond;
	private PreparedStatement psInsOTCBond;
	private PreparedStatement psupdBond;
	private PreparedStatement psInsSMEShare;
	private PreparedStatement psInsSMEShareRI;
	private PreparedStatement psupdateMarketType;
	//Problem of frequent bond missing in FI_STOCK
	private PreparedStatement psExistsBond;
	private PreparedStatement  psExistsOTCBond;
	private PreparedStatement psupdOTCBond;
	
	private PreparedStatement psupdBondType;
	private PreparedStatement psupdBondSubType;
	private PreparedStatement psInsIndexComponents;
    private PreparedStatement psDelIndexComponents;
    private PreparedStatement psSelIndexComponents;
    private PreparedStatement psDelIndexComponentsTable;

	public DBManagerDataAnalyser(String threadName) throws Exception {
		super(threadName);
		prepareStatements();
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

				sbPrepareQueries
						.append("INSERT INTO EFP.TE_SYMBOL(PK_TE_SYMBOL,SHORT_DESC) VALUES(?,?)");
				psInsShareSymbol = g_oConnection
						.prepareStatement(sbPrepareQueries.toString());

				// Insert Symbol Data into FI_Symbol table
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("INSERT INTO EFP.FI_SYMBOL(PK_FI_SYMBOL,SHORT_DESC) VALUES(?,?)");
				psInsBondSymbol = g_oConnection.prepareStatement(sbPrepareQueries.toString());
				
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("INSERT INTO EFP.FI_SYMBOL_UNLISTED(PK_FI_SYMBOL,SHORT_DESC) VALUES(?,?)");
				psInsOTCBondSymbol = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				// Insert Symbol Data into TE_SME_TABLE table
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("INSERT INTO EFP.TE_SME_SYMBOL(PK_TE_SYMBOL,SHORT_DESC) VALUES(?,?)");
				psInsSMESymbol = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("INSERT INTO EFP.TE_INDEX(DESCRIPTION_EN,DESCRIPTION_AR,INDEX_PRICE,VOLUME_TRADED,TURNOVER,"
								+ "NO_OF_TRADES,NET_CHANGE,PERCENT_CHANGE,HIGH_PRICE,LOW_PRICE,OPEN_PRICE,PREVIOUS_INDEX_PRICE,FK_TE_SYMBOL,"
								+ "TIMESTAMP) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

				psInsIndex = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("INSERT INTO EFP.FI_INDEX(DESCRIPTION_EN,DESCRIPTION_AR,INDEX_PRICE,VOLUME_TRADED,TURNOVER,"
								+ "NO_OF_TRADES,NET_CHANGE,PERCENT_CHANGE,HIGH_PRICE,LOW_PRICE,OPEN_PRICE,PREVIOUS_INDEX_PRICE,FK_TE_SYMBOL,"
								+ "TIMESTAMP) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

				psInsBondIndex = g_oConnection	.prepareStatement(sbPrepareQueries.toString());

				// Added for SME
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries
						.append("INSERT INTO EFP.TE_SME_INDEX(DESCRIPTION_EN,DESCRIPTION_AR,INDEX_PRICE,VOLUME_TRADED,TURNOVER,"
								+ "NO_OF_TRADES,NET_CHANGE,PERCENT_CHANGE,HIGH_PRICE,LOW_PRICE,OPEN_PRICE,PREVIOUS_INDEX_PRICE,FK_TE_SYMBOL,"
								+ "TIMESTAMP) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

				psInsSMEIndex = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				/* End */

				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				// Added for Indicative Opening
				sbPrepareQueries
						.append("INSERT INTO EFP.TE_STOCK (FK_PK_EXCHANGE_CODE, FK_TE_SYMBOL, MARKET_STATUS, TRANSACTION_DATE, "
								+ "LAST_TRADE_PRICE, LAST_TRADE_QUANTITY, LAST_UPDATE_TIME, ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY, "
								+ "NET_CHANGE, HIGH_PRICE, HIGH_52_WEEKS_PRICE, LOW_PRICE, LOW_52_WEEKS_PRICE, MARKET_CAP, NO_OF_TRADES, PBR, "
								+ "PER, PERCENT_CHANGE, PREVIOUS_CLOSE_PRICE, SEQUENCE_NO, TODAYS_CLOSE_PRICE, TODAYS_OPEN, TURNOVER, "
								+ "VOLUME_TRADED, YIELD "
								+ ", HIGH_52_WEEKS_DATE, LOW_52_WEEKS_DATE, INDICATIVEPRICE, INDICATIVEVOLUME, LISTING_DATE "
								+ ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
				
				psInsShare = g_oConnection.prepareStatement(sbPrepareQueries
						.toString());

				
				sbPrepareQueries.delete(0, sbPrepareQueries.length());				
				sbPrepareQueries
						.append("UPDATE EFP.TE_STOCK  SET LISTING_DATE =?   WHERE FK_TE_SYMBOL = ?");				
				psUpdateShare = g_oConnection.prepareStatement(sbPrepareQueries
						.toString());
				
				sbPrepareQueries.delete(0, sbPrepareQueries.length());				
				sbPrepareQueries
						.append("UPDATE EFP.TE_SME_STOCK  SET LISTING_DATE =?   WHERE FK_TE_SYMBOL = ?");				
				psUpdateSMEShare = g_oConnection.prepareStatement(sbPrepareQueries
						.toString());
				
				sbPrepareQueries.delete(0, sbPrepareQueries.length());				
				sbPrepareQueries
						.append("UPDATE EFP.ETF_STOCK  SET LISTING_DATE =?   WHERE FK_ETF_SYMBOL = ?");				
				psUpdateETFShare = g_oConnection.prepareStatement(sbPrepareQueries
						.toString());
				
				sbPrepareQueries.delete(0, sbPrepareQueries.length());				
				sbPrepareQueries
						.append("UPDATE EFP.RI_STOCK  SET LISTING_DATE =?   WHERE FK_RI_SYMBOL = ?");				
				psUpdateRIShare = g_oConnection.prepareStatement(sbPrepareQueries
						.toString());
				
				sbPrepareQueries.delete(0, sbPrepareQueries.length());				
				sbPrepareQueries
						.append("UPDATE EFP.RI_SME_STOCK  SET LISTING_DATE =?   WHERE FK_RI_SYMBOL = ?");				
				psUpdateRISMEShare = g_oConnection.prepareStatement(sbPrepareQueries
						.toString());
				
				
				// Added for Share ETF
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries
						.append("INSERT INTO EFP.ETF_STOCK (FK_PK_EXCHANGE_CODE, FK_ETF_SYMBOL, MARKET_STATUS, TRANSACTION_DATE, "
								+ "LAST_TRADE_PRICE, LAST_TRADE_QUANTITY, LAST_UPDATE_TIME, ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY, "
								+ "NET_CHANGE, HIGH_PRICE, HIGH_52_WEEKS_PRICE, LOW_PRICE, LOW_52_WEEKS_PRICE, MARKET_CAP, NO_OF_TRADES, PBR, "
								+ "PER, PERCENT_CHANGE, PREVIOUS_CLOSE_PRICE, SEQUENCE_NO, TODAYS_CLOSE_PRICE, TODAYS_OPEN, TURNOVER, "
								+ "VOLUME_TRADED, YIELD "
								+ ", HIGH_52_WEEKS_DATE, LOW_52_WEEKS_DATE, LISTING_DATE "
								+ ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

				psInsShareETF = g_oConnection.prepareStatement(sbPrepareQueries	.toString());

				// Added for Rights Issue Changes for ReferencePrice Upper
				// Limit and Reference Price Lower Limit

				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries
						.append("INSERT INTO EFP.RI_STOCK (FK_PK_EXCHANGE_CODE, FK_RI_SYMBOL, MARKET_STATUS, TRANSACTION_DATE, "
								+ "LAST_TRADE_PRICE, LAST_TRADE_QUANTITY, LAST_UPDATE_TIME, ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY, "
								+ "NET_CHANGE, HIGH_PRICE, HIGH_52_WEEKS_PRICE, LOW_PRICE, LOW_52_WEEKS_PRICE, MARKET_CAP, NO_OF_TRADES, PBR, "
								+ "PER, PERCENT_CHANGE, PREVIOUS_CLOSE_PRICE, SEQUENCE_NO, TODAYS_CLOSE_PRICE, TODAYS_OPEN, TURNOVER, "
								+ "VOLUME_TRADED, YIELD "
								+ ", HIGH_52_WEEKS_DATE, LOW_52_WEEKS_DATE, REFERENCEPRICE_UPPERLIMIT, REFERENCEPRICE_LOWERLIMIT, LISTING_DATE "
								+ " ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

				psInsShareRI = g_oConnection.prepareStatement(sbPrepareQueries
						.toString());

				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries
						.append("INSERT INTO EFP.FI_STOCK (FK_PK_EXCHANGE_CODE, FK_FI_SYMBOL,TRANSACTION_DATE, "
								+ "LAST_TRADE_PRICE, LAST_TRADE_QUANTITY, LAST_UPDATE_TIME, ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY, "
								+ "NET_CHANGE, HIGH_PRICE, HIGH_52_WEEKS_PRICE, LOW_PRICE, LOW_52_WEEKS_PRICE, NO_OF_TRADES, "
								+ "PERCENT_CHANGE, PREVIOUS_CLOSE_PRICE, TODAYS_CLOSE_PRICE, TODAYS_OPEN_PRICE, TURNOVER, VOLUME_TRADED"
								+ ", HIGH_52_WEEKS_DATE, LOW_52_WEEKS_DATE,ISIN , TRADE_YIELD,  BEST_BID_YIELD,  BEST_OFFER_YIELD, OUTSTANDING_AMOUNT, ISSUE_CURRENCY "
								+ ",  PAR_VALUE, COUPON_FREQUENCY, DAYCOUNT_METHOD, COUPON_RATE, MATURITY_DATE, RATE_CALCTYPE , LISTING_DATE , SETTLEMENT_DATE,  ISSUE_DATE, ISSUE_AMOUNT  "						
				                + ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

				psInsBond = g_oConnection.prepareStatement(sbPrepareQueries.toString());
				
				//OTC Unlisted Bond
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries
						.append("INSERT INTO EFP.FI_STOCK_UNLISTED (FK_PK_EXCHANGE_CODE, FK_FI_SYMBOL,TRANSACTION_DATE, "
								+ "LAST_TRADE_PRICE, LAST_TRADE_QUANTITY, LAST_UPDATE_TIME, ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY, "
								+ "NET_CHANGE, HIGH_PRICE, HIGH_52_WEEKS_PRICE, LOW_PRICE, LOW_52_WEEKS_PRICE, NO_OF_TRADES, "
								+ "PERCENT_CHANGE, PREVIOUS_CLOSE_PRICE, TODAYS_CLOSE_PRICE, TODAYS_OPEN_PRICE, TURNOVER, VOLUME_TRADED"
								+ ", HIGH_52_WEEKS_DATE, LOW_52_WEEKS_DATE,ISIN , TRADE_YIELD,  BEST_BID_YIELD,  BEST_OFFER_YIELD, OUTSTANDING_AMOUNT, ISSUE_CURRENCY "
								+ ",  PAR_VALUE, COUPON_FREQUENCY, DAYCOUNT_METHOD, COUPON_RATE, MATURITY_DATE, RATE_CALCTYPE , LISTING_DATE , SETTLEMENT_DATE,  ISSUE_DATE, ISSUE_AMOUNT  "						
				                + ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

				psInsOTCBond = g_oConnection.prepareStatement(sbPrepareQueries.toString());
				
				//Added to include the ISIN
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("UPDATE  EFP.FI_STOCK  SET ISIN=? , OUTSTANDING_AMOUNT = ? , ISSUE_CURRENCY =? ,  PAR_VALUE =? , COUPON_FREQUENCY =? , DAYCOUNT_METHOD =? , COUPON_RATE =? , "
						+ " MATURITY_DATE =?,  RATE_CALCTYPE=?, LISTING_DATE =?,  SETTLEMENT_DATE =?, ISSUE_DATE =?,  ISSUE_AMOUNT =? WHERE FK_FI_SYMBOL =? ");

				psupdBond = g_oConnection.prepareStatement(sbPrepareQueries.toString());
				
				//Added to include the ISIN
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("UPDATE  EFP.FI_STOCK_UNLISTED  SET ISIN=? , OUTSTANDING_AMOUNT = ? , ISSUE_CURRENCY =? ,  PAR_VALUE =? , COUPON_FREQUENCY =? , DAYCOUNT_METHOD =? , COUPON_RATE =? , "
						+ " MATURITY_DATE =?,  RATE_CALCTYPE=?, LISTING_DATE =?,  SETTLEMENT_DATE =?, ISSUE_DATE =?,  ISSUE_AMOUNT =? WHERE FK_FI_SYMBOL =? ");

				psupdOTCBond = g_oConnection.prepareStatement(sbPrepareQueries.toString());
				
				//Added to include the ISIN
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("UPDATE  WEB.FI_COMPANY  SET BOND_LISTING_TYPE=? WHERE PK_RF_COMPANY =? ");

				psupdBondType = g_oConnection.prepareStatement(sbPrepareQueries.toString());
				
				//Added to include the ISIN
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("UPDATE  WEB.FI_COMPANY  SET  GOVTBOND_SUBTYPE=? WHERE PK_RF_COMPANY =? ");

				psupdBondSubType = g_oConnection.prepareStatement(sbPrepareQueries.toString());


				// Added for SME changes
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries
						.append("INSERT INTO EFP.TE_SME_STOCK (FK_PK_EXCHANGE_CODE, FK_TE_SYMBOL, MARKET_STATUS, TRANSACTION_DATE, "
								+ "LAST_TRADE_PRICE, LAST_TRADE_QUANTITY, LAST_UPDATE_TIME, ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY, "
								+ "NET_CHANGE, HIGH_PRICE, HIGH_52_WEEKS_PRICE, LOW_PRICE, LOW_52_WEEKS_PRICE, MARKET_CAP, NO_OF_TRADES, PBR, "
								+ "PER, PERCENT_CHANGE, PREVIOUS_CLOSE_PRICE, SEQUENCE_NO, TODAYS_CLOSE_PRICE, TODAYS_OPEN, TURNOVER, "
								+ "VOLUME_TRADED, YIELD "
								+ ", HIGH_52_WEEKS_DATE, LOW_52_WEEKS_DATE, INDICATIVEPRICE, INDICATIVEVOLUME, LISTING_DATE "
								+ ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
				psInsSMEShare = g_oConnection.prepareStatement(sbPrepareQueries
						.toString());

				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries
						.append("INSERT INTO EFP.RI_SME_STOCK (FK_PK_EXCHANGE_CODE, FK_RI_SYMBOL, MARKET_STATUS, TRANSACTION_DATE, "
								+ "LAST_TRADE_PRICE, LAST_TRADE_QUANTITY, LAST_UPDATE_TIME, ASK_PRICE, ASK_QUANTITY, BID_PRICE, BID_QUANTITY, "
								+ "NET_CHANGE, HIGH_PRICE, HIGH_52_WEEKS_PRICE, LOW_PRICE, LOW_52_WEEKS_PRICE, MARKET_CAP, NO_OF_TRADES, PBR, "
								+ "PER, PERCENT_CHANGE, PREVIOUS_CLOSE_PRICE, SEQUENCE_NO, TODAYS_CLOSE_PRICE, TODAYS_OPEN, TURNOVER, "
								+ "VOLUME_TRADED, YIELD "
								+ ", HIGH_52_WEEKS_DATE, LOW_52_WEEKS_DATE, REFERENCEPRICE_UPPERLIMIT, REFERENCEPRICE_LOWERLIMIT, LISTING_DATE "
								+ " ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

				psInsSMEShareRI = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				sbPrepareQueries.delete(0, sbPrepareQueries.length());

				sbPrepareQueries
						.append("UPDATE SACP.RF_COMPANY SET MARKET_TYPE=? WHERE PK_RF_COMPANY =? ");
				psupdateMarketType = g_oConnection	.prepareStatement(sbPrepareQueries.toString());
				
				
				//Problem of frequent bond missing in F_STOCK			
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("SELECT  count(*) COUNT FROM  EFP.FI_STOCK  WHERE   FK_FI_SYMBOL=?  ");
				psExistsBond = g_oConnection.prepareStatement(sbPrepareQueries.toString());
				
				//Problem of frequent bond missing in F_STOCK			
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("SELECT  count(*) COUNT FROM  EFP.FI_STOCK_UNLISTED  WHERE   FK_FI_SYMBOL=?  ");
				psExistsOTCBond = g_oConnection.prepareStatement(sbPrepareQueries.toString());
				
				
				 sbPrepareQueries.delete(0, sbPrepareQueries.length());
                 sbPrepareQueries.append("INSERT INTO WEB.INDEX_COMPONENTS (INDEX_NAME, SYMBOL) VALUES(?,?)");
                 psInsIndexComponents = g_oConnection.prepareStatement(sbPrepareQueries.toString());
                 
                 sbPrepareQueries.delete(0, sbPrepareQueries.length());
                 sbPrepareQueries.append("DELETE FROM WEB.INDEX_COMPONENTS WHERE INDEX_NAME=? AND SYMBOL = ? ");
                 psDelIndexComponents = g_oConnection.prepareStatement(sbPrepareQueries.toString());
                 
                 sbPrepareQueries.delete(0, sbPrepareQueries.length());
                 sbPrepareQueries.append("SELECT count(*) COUNT FROM WEB.INDEX_COMPONENTS WHERE INDEX_NAME=? AND SYMBOL = ? ");
                 psSelIndexComponents = g_oConnection.prepareStatement(sbPrepareQueries.toString());
                 
                 sbPrepareQueries.delete(0, sbPrepareQueries.length());
                 sbPrepareQueries.append("DELETE FROM WEB.INDEX_COMPONENTS  ");
                 psDelIndexComponentsTable = g_oConnection.prepareStatement(sbPrepareQueries.toString());

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

			if (psInsShareSymbol != null)
				psInsShareSymbol.close();

			if (psInsBondSymbol != null)
				psInsBondSymbol.close();
			
			if (psInsOTCBondSymbol != null)
				psInsOTCBondSymbol.close();
			
			if (psInsSMESymbol != null)
				psInsSMESymbol.close();
			
			if (psInsIndex != null)
				psInsIndex.close();
			
			if (psInsBondIndex != null)
				psInsBondIndex.close();
			
			if (psInsSMEIndex != null)
				psInsSMEIndex.close();
			
			if (psInsShare != null)
				psInsShare.close();
			
			if (psInsShareETF != null)
				psInsShareETF.close();
			
			if (psInsShareRI != null)
				psInsShareRI.close();
			
			if (psInsBond != null)
				psInsBond.close();
			
			if (psInsOTCBond != null)
				psInsOTCBond.close();
			
			if (psupdBond != null)
				psupdBond.close();
			
			if (psupdOTCBond != null)
				psupdOTCBond.close();
			
			if (psupdBondType != null)
				psupdBondType.close();
			
			if (psupdBondSubType != null)
				psupdBondSubType.close();
			
			if (psInsSMEShare != null)
				psInsSMEShare.close();
			
			if (psInsSMEShareRI != null)
				psInsSMEShareRI.close();

			if (psupdateMarketType != null)
				psupdateMarketType.close();
			
			if (g_oConnection != null)
				g_oConnection.close();
			
			if (psExistsBond != null)
				psExistsBond.close();
			
			if (psExistsOTCBond != null)
				psExistsOTCBond.close();
			
			if (psUpdateShare != null)
				psUpdateShare.close();
			
			if (psUpdateSMEShare != null)
				psUpdateSMEShare.close();
			
			if (psUpdateETFShare != null)
				psUpdateETFShare.close();
			
			if (psUpdateRIShare != null)
				psUpdateRIShare.close();
			
			if (psUpdateRISMEShare != null)
				psUpdateRISMEShare.close();
			
					
		    if (psInsIndexComponents != null)
		    	psInsIndexComponents.close();
		    
		    if (psDelIndexComponents != null)
		    	psDelIndexComponents.close();
		    
		    if (psSelIndexComponents != null)
		    	psSelIndexComponents.close();
		    
		    if (psDelIndexComponentsTable != null)
		    	psDelIndexComponentsTable.close();
			
			
			logger.debug("Exit  DB2Manager closeConnection function");
		} catch (SQLException ex) {
			logger.error("SQL Exception in closeConnection() " + ex, ex);
		}
	}

	/**
	 * This method will insert record into the table EFP.FI_STOCK
	 *
	 * @param objBond
	 *            contains attributes of EFP.FI_STOCK table
	 * @return boolean
	 */
	public boolean insertBondData(TIP_BondVO objBond) {

			try {

				psInsBond.setString(1, TipConstants.BOND_MARKET_CODE);
				psInsBond.setString(2, objBond.fk_fi_symbol);
				psInsBond.setTimestamp(3, objBond.transaction_date);
				psInsBond.setDouble(4, objBond.last_trade_price);
				psInsBond.setInt(5, objBond.last_trade_quantity);
				psInsBond.setTimestamp(6, objBond.last_update_time);
				psInsBond.setDouble(7, objBond.ask_price);
				psInsBond.setLong(8, objBond.ask_quantity);
				psInsBond.setDouble(9, objBond.bid_price);
				psInsBond.setLong(10, objBond.bid_quantity);
				psInsBond.setDouble(11, objBond.net_change);
				psInsBond.setDouble(12, objBond.high_price);
				psInsBond.setDouble(13, objBond.high_52_weeks_price);
				psInsBond.setDouble(14, objBond.low_price);
				psInsBond.setDouble(15, objBond.low_52_weeks_price);
				psInsBond.setLong(16, objBond.no_of_trades);
				psInsBond.setDouble(17, objBond.percent_change);
				psInsBond.setDouble(18, objBond.Previous_close_price);
				psInsBond.setDouble(19, objBond.todays_close_price);
				psInsBond.setDouble(20, objBond.Todays_open_price);
				psInsBond.setDouble(21, objBond.turnover);
				psInsBond.setDouble(22, objBond.volume_traded);
				psInsBond.setDate(23, objBond.high_52_weeks_date);
				psInsBond.setDate(24, objBond.low_52_weeks_date);
				//Added for Bonds Yield calculations projects CRQ21157	
				psInsBond.setString(25, objBond.isin);
				psInsBond.setDouble(26, objBond.traded_yield);
				psInsBond.setDouble(27, objBond.bestBid_yield);
				psInsBond.setDouble(28, objBond.bestOffer_yield);

				// Added for Debt Market EnhancementsCRQ21357
				psInsBond.setDouble(29, objBond.outstandingAmount);
				psInsBond.setString(30, objBond.issueCurrency);
				psInsBond.setDouble(31, objBond.parValue);
				psInsBond.setDouble(32, objBond.couponFrequency);
				psInsBond.setInt(33, objBond.dayCountMethod);
				psInsBond.setDouble(34, objBond.couponRate);
				psInsBond.setDate(35, objBond.maturityDate);
				psInsBond.setInt(36, objBond.rateCalcType);
				
				//Added fpr OTC PTTP Project
				psInsBond.setDate(37, objBond.listingDate);
				psInsBond.setDate(38, objBond.settlementDate);
				psInsBond.setDate(39, objBond.issueDate);
				psInsBond.setDouble(40, objBond.issueAmount);
			
				
				psInsBond.executeUpdate();

				return true;

			} catch (SQLException e) {
				logger.error("Exception in insertBondData: " + e, e);
				
			}catch(Exception e)
			{
				logger.error("Exception in insertBondData: " + e, e);
			}

		if (objBond != null)
			objBond = null;
		return false;
	}
	
	
	
	/**
	 * This method will insert record into the table EFP.FI_STOCK
	 *
	 * @param objBond
	 *            contains attributes of EFP.FI_STOCK table
	 * @return boolean
	 */
	public boolean insertOTCBondData(TIP_BondVO objBond) {

			try {

				psInsOTCBond.setString(1, TipConstants.BOND_MARKET_CODE);
				psInsOTCBond.setString(2, objBond.fk_fi_symbol);
				psInsOTCBond.setTimestamp(3, objBond.transaction_date);
				psInsOTCBond.setDouble(4, objBond.last_trade_price);
				psInsOTCBond.setInt(5, objBond.last_trade_quantity);
				psInsOTCBond.setTimestamp(6, objBond.last_update_time);
				psInsOTCBond.setDouble(7, objBond.ask_price);
				psInsOTCBond.setLong(8, objBond.ask_quantity);
				psInsOTCBond.setDouble(9, objBond.bid_price);
				psInsOTCBond.setLong(10, objBond.bid_quantity);
				psInsOTCBond.setDouble(11, objBond.net_change);
				psInsOTCBond.setDouble(12, objBond.high_price);
				psInsOTCBond.setDouble(13, objBond.high_52_weeks_price);
				psInsOTCBond.setDouble(14, objBond.low_price);
				psInsOTCBond.setDouble(15, objBond.low_52_weeks_price);
				psInsOTCBond.setLong(16, objBond.no_of_trades);
				psInsOTCBond.setDouble(17, objBond.percent_change);
				psInsOTCBond.setDouble(18, objBond.Previous_close_price);
				psInsOTCBond.setDouble(19, objBond.todays_close_price);
				psInsOTCBond.setDouble(20, objBond.Todays_open_price);
				psInsOTCBond.setDouble(21, objBond.turnover);
				psInsOTCBond.setDouble(22, objBond.volume_traded);
				psInsOTCBond.setDate(23, objBond.high_52_weeks_date);
				psInsOTCBond.setDate(24, objBond.low_52_weeks_date);
				//Added for Bonds Yield calculations projects CRQ21157	
				psInsOTCBond.setString(25, objBond.isin);
				psInsOTCBond.setDouble(26, objBond.traded_yield);
				psInsOTCBond.setDouble(27, objBond.bestBid_yield);
				psInsOTCBond.setDouble(28, objBond.bestOffer_yield);

				// Added for Debt Market EnhancementsCRQ21357
				psInsOTCBond.setDouble(29, objBond.outstandingAmount);
				psInsOTCBond.setString(30, objBond.issueCurrency);
				psInsOTCBond.setDouble(31, objBond.parValue);
				psInsOTCBond.setDouble(32, objBond.couponFrequency);
				psInsOTCBond.setInt(33, objBond.dayCountMethod);
				psInsOTCBond.setDouble(34, objBond.couponRate);
				psInsOTCBond.setDate(35, objBond.maturityDate);
				psInsOTCBond.setInt(36, objBond.rateCalcType);
				
				//Added fpr OTC PTTP Project
				psInsOTCBond.setDate(37, objBond.listingDate);
				psInsOTCBond.setDate(38, objBond.settlementDate);
				psInsOTCBond.setDate(39, objBond.issueDate);
				psInsOTCBond.setDouble(40, objBond.issueAmount);
			
				
				psInsOTCBond.executeUpdate();

				return true;

			} catch (SQLException e) {
				logger.error("Exception in insertOTCBondData: " + e, e);
				
			}catch(Exception e)
			{
				logger.error("Exception in insertOTCBondData: " + e, e);
			}

		if (objBond != null)
			objBond = null;
		return false;
	}
	
	
	
	/**
	 * This method will insert record into the table EFP.FI_STOCK
	 *
	 * @param objBond
	 *            contains attributes of EFP.FI_STOCK table
	 * @return boolean
	 */
	public boolean updateBondBasicData(TIP_BondVO objBond) {

			try {

				logger.info("updateBondBasicData is called :{} with ISIN code :{}", objBond.fk_fi_symbol, objBond.isin);
				psupdBond.setString(1, objBond.isin);
				psupdBond.setDouble(2, objBond.outstandingAmount);
				psupdBond.setString(3, objBond.issueCurrency);
				psupdBond.setDouble(4, objBond.parValue);
				psupdBond.setDouble(5, objBond.couponFrequency);
				psupdBond.setInt(6, objBond.dayCountMethod);
				psupdBond.setDouble(7, objBond.couponRate);
				psupdBond.setDate(8, objBond.maturityDate);
				psupdBond.setInt(9, objBond.rateCalcType);
				
				
				//Added fpr OTC PTTP Project
				psupdBond.setDate(10, objBond.listingDate);
				psupdBond.setDate(11, objBond.settlementDate);
				psupdBond.setDate(12, objBond.issueDate);
				psupdBond.setDouble(13, objBond.issueAmount);		
				
				psupdBond.setString(14, objBond.fk_fi_symbol);
				
				psupdBond.executeUpdate();
				
				return true;

			} catch (SQLException e) {
				logger.error("Exception in insertBondData: " + e, e);
				
			}
		return false;
	}
	
	
	
	
	/**
	 * This method will insert record into the table EFP.FI_STOCK
	 *
	 * @param objBond
	 *            contains attributes of EFP.FI_STOCK table
	 * @return boolean
	 */
	public boolean updateOTCBondBasicData(TIP_BondVO objBond) {

			try {

				logger.info("updateOTCBondBasicData is called :{} with ISIN code :{}", objBond.fk_fi_symbol, objBond.isin);
				psupdOTCBond.setString(1, objBond.isin);
				psupdOTCBond.setDouble(2, objBond.outstandingAmount);
				psupdOTCBond.setString(3, objBond.issueCurrency);
				psupdOTCBond.setDouble(4, objBond.parValue);
				psupdOTCBond.setDouble(5, objBond.couponFrequency);
				psupdOTCBond.setInt(6, objBond.dayCountMethod);
				psupdOTCBond.setDouble(7, objBond.couponRate);
				psupdOTCBond.setDate(8, objBond.maturityDate);
				psupdOTCBond.setInt(9, objBond.rateCalcType);
				
				
				//Added fpr OTC PTTP Project
				psupdOTCBond.setDate(10, objBond.listingDate);
				psupdOTCBond.setDate(11, objBond.settlementDate);
				psupdOTCBond.setDate(12, objBond.issueDate);
				psupdOTCBond.setDouble(13, objBond.issueAmount);		
				
				psupdOTCBond.setString(14, objBond.fk_fi_symbol);
				
				psupdOTCBond.executeUpdate();
				
				return true;

			} catch (SQLException e) {
				logger.error("Exception in updateOTCBondBasicData: " + e, e);
				
			}
		return false;
	}
	
	
	public boolean updateBondType(String bondid, String bondType) {

		try {

			logger.info("updateBondType is called :{} with BondType :{}", bondid, bondType);
			psupdBondType.setString(1, bondType);
			psupdBondType.setString(2, bondid);
			
			
			psupdBondType.executeUpdate();
			
			return true;

		} catch (SQLException e) {
			logger.error("Exception in updateBondType: " + e, e);
			
		}
	return false;
}

	public boolean updateBondSubType(String bondid, int bondSubType) {

		try {
			String strBondSubType= TipConstants.BOND_SUBTYPE_FIXED;
			logger.info("updateBondSubType is called :{} with BondSubType :{}", bondid, bondSubType);
			if(bondSubType == 1) strBondSubType =TipConstants.BOND_SUBTYPE_FIXED;
			if(bondSubType == 4 ) strBondSubType = TipConstants.BOND_SUBTYPE_FLOATING;
			psupdBondSubType.setString(1, strBondSubType);
			psupdBondSubType.setString(2, bondid);
			
			
			psupdBondSubType.executeUpdate();
			
			return true;

		} catch (SQLException e) {
			logger.error("Exception in updateBondSubType: " + e, e);
			
		}
	return false;
}
	/**
	 * This method will insert recored in to the table EFP.TE_INDEX
	 * 
	 * @param objIndex
	 *            contains attributes of EFP.TE_INDEX table
	 * @return boolean
	 */
	public boolean insertIndexData(TIP_IndexVO objIndex) {


		String description_ar = objIndex.Description_AR;

			try {

				psInsIndex.setString(1, objIndex.Description_EN);

				if (null != description_ar && description_ar.length() > 0) {
					try {

						description_ar = reencodeArabic(description_ar);

					} catch (CharacterCodingException e) {
						logger.error(	"Exception in insertIndexData in conversion: "+ e, e);
					}

				} else
					description_ar = objIndex.Description_EN;
				psInsIndex.setString(2, description_ar);
				// psInsIndex.setString(2, objIndex.Description_AR);
				psInsIndex.setDouble(3, objIndex.index_price);
				psInsIndex.setLong(4, objIndex.volume_trade);
				psInsIndex.setDouble(5, objIndex.turnover);
				psInsIndex.setLong(6, objIndex.no_of_trades);
				psInsIndex.setDouble(7, objIndex.net_change);
				psInsIndex.setDouble(8, objIndex.percent_change);
				psInsIndex.setDouble(9, objIndex.high_price);
				psInsIndex.setDouble(10, objIndex.low_price);
				psInsIndex.setDouble(11, objIndex.open_price);
				psInsIndex.setDouble(12, objIndex.previous_index_price);
				psInsIndex.setString(13, objIndex.Fk_te_symbol);
				psInsIndex.setTimestamp(14, objIndex.trans_time);
				psInsIndex.executeUpdate();
				return true;
			} catch (SQLException e) {
				logger.error("Exception in insertIndexData: " + e, e);

			}


		if (objIndex != null)
			objIndex = null;
		return false;
	}

	/**
	 * This method will insert recored in to the table EFP.TE_INDEX
	 * 
	 * @param objIndex
	 *            contains attributes of EFP.TE_INDEX table
	 * @return boolean
	 */
	// Added for SME
	public boolean insertSMEIndexData(TIP_SMEIndexVO objIndex) {


		String description_ar = objIndex.Description_AR;

			try {

				psInsSMEIndex.setString(1, objIndex.Description_EN);
				if (null != description_ar && description_ar.length() > 0) {
					// description_ar= ;
					try {
						description_ar = reencodeArabic(description_ar);
					} catch (CharacterCodingException e) {
						logger.error("Exception in insertIndexData in conversion: "	+ e, e);
					}

				} else
					description_ar = objIndex.Description_EN;
				psInsSMEIndex.setString(2, description_ar);
				// psInsSMEIndex.setString(2, objIndex.Description_AR);
				psInsSMEIndex.setDouble(3, objIndex.index_price);
				psInsSMEIndex.setLong(4, objIndex.volume_trade);
				psInsSMEIndex.setDouble(5, objIndex.turnover);
				psInsSMEIndex.setLong(6, objIndex.no_of_trades);
				psInsSMEIndex.setDouble(7, objIndex.net_change);
				psInsSMEIndex.setDouble(8, objIndex.percent_change);
				psInsSMEIndex.setDouble(9, objIndex.high_price);
				psInsSMEIndex.setDouble(10, objIndex.low_price);
				psInsSMEIndex.setDouble(11, objIndex.open_price);
				psInsSMEIndex.setDouble(12, objIndex.previous_index_price);
				psInsSMEIndex.setString(13, objIndex.Fk_te_symbol);
				psInsSMEIndex.setTimestamp(14, objIndex.trans_time);
				psInsSMEIndex.executeUpdate();

				return true;
			} catch (SQLException e) {
				logger.error("Exception in insertSMEIndexData: " + e, e);
	
			}


		if (objIndex != null)
			objIndex = null;
		return false;
	}

	/**
	 * This method will insert recored in to the table EFP.TE_STOCK
	 * 
	 * @param objShare
	 *            contains attributes of EFP.TE_STOCK table
	 * @return boolean
	 */
	public boolean insertShareData(TIP_ShareVO objShare) {

			try {

				psInsShare.setString(1, TipConstants.SHARE_MARKET_CODE);
				psInsShare.setString(2, objShare.fk_te_symbol);
				psInsShare.setInt(3, objShare.market_status);
				psInsShare.setTimestamp(4, objShare.transaction_date);
				psInsShare.setDouble(5, objShare.last_trade_price);
				psInsShare.setInt(6, objShare.last_trade_quantity);
				psInsShare.setTimestamp(7, objShare.last_update_time);
				psInsShare.setDouble(8, objShare.ask_price);
				psInsShare.setLong(9, objShare.ask_quantity);
				psInsShare.setDouble(10, objShare.bid_price);
				psInsShare.setLong(11, objShare.bid_quantity);
				psInsShare.setDouble(12, objShare.net_change);
				psInsShare.setDouble(13, objShare.high_price);
				psInsShare.setDouble(14, objShare.high_52_weeks_price);
				psInsShare.setDouble(15, objShare.low_price);
				psInsShare.setDouble(16, objShare.low_52_weeks_price);
				psInsShare.setDouble(17, objShare.market_cap);
				psInsShare.setLong(18, objShare.no_of_trades);
				psInsShare.setDouble(19, objShare.Pbr);
				psInsShare.setDouble(20, objShare.Per);
				psInsShare.setDouble(21, objShare.percent_change);
				psInsShare.setDouble(22, objShare.Previous_close_price);
				psInsShare.setLong(23, objShare.sequence_no);
				psInsShare.setDouble(24, objShare.todays_close_price);
				psInsShare.setDouble(25, objShare.Todays_open);
				psInsShare.setDouble(26, objShare.turnover);
				psInsShare.setDouble(27, objShare.volume_traded);
				psInsShare.setDouble(28, objShare.yield);
				psInsShare.setDate(29, objShare.high_52_weeks_date);
				psInsShare.setDate(30, objShare.low_52_weeks_date);
				// Added for Indicative Opening changes
				psInsShare.setDouble(31, objShare.indicative_Open_price);
				psInsShare.setDouble(32, objShare.indicative_Open_volume);
				psInsShare.setDate(33, objShare.listingDate);

				psInsShare.executeUpdate();
				return true;
				
			} catch (SQLException e) {
				logger.error("Exception in insertShareData: " + e, e);
	
			}


		if (objShare != null)
			objShare = null;
		return false;
	}

	
	public boolean updateShareDataListingDate(TIP_ShareVO objShare) {

		try {

			psUpdateShare.setDate(1, objShare.listingDate);
			psUpdateShare.setString(2, objShare.fk_te_symbol);	
			psUpdateShare.executeUpdate();
			return true;
			
		} catch (SQLException e) {
			logger.error("Exception in updateShareDataListingDate: " + e, e);

		}


	if (objShare != null)
		objShare = null;
	return false;
}

	
	
	/**
	 * This method will insert recored in to the table EFP.TE_STOCK
	 * 
	 * @param objShare
	 *            contains attributes of EFP.TE_STOCK table
	 * @return boolean
	 */
	// Added for SME
	public boolean insertSMEShareData(TIP_SMEShareVO objShare) {

			try {

				psInsSMEShare.setString(1, TipConstants.SME_MARKET_CODE);
				psInsSMEShare.setString(2, objShare.fk_te_symbol);
				psInsSMEShare.setInt(3, objShare.market_status);
				psInsSMEShare.setTimestamp(4, objShare.transaction_date);
				psInsSMEShare.setDouble(5, objShare.last_trade_price);
				psInsSMEShare.setInt(6, objShare.last_trade_quantity);
				psInsSMEShare.setTimestamp(7, objShare.last_update_time);
				psInsSMEShare.setDouble(8, objShare.ask_price);
				psInsSMEShare.setLong(9, objShare.ask_quantity);
				psInsSMEShare.setDouble(10, objShare.bid_price);
				psInsSMEShare.setLong(11, objShare.bid_quantity);
				psInsSMEShare.setDouble(12, objShare.net_change);
				psInsSMEShare.setDouble(13, objShare.high_price);
				psInsSMEShare.setDouble(14, objShare.high_52_weeks_price);
				psInsSMEShare.setDouble(15, objShare.low_price);
				psInsSMEShare.setDouble(16, objShare.low_52_weeks_price);
				psInsSMEShare.setDouble(17, objShare.market_cap);
				psInsSMEShare.setLong(18, objShare.no_of_trades);
				psInsSMEShare.setDouble(19, objShare.Pbr);
				psInsSMEShare.setDouble(20, objShare.Per);
				psInsSMEShare.setDouble(21, objShare.percent_change);
				psInsSMEShare.setDouble(22, objShare.Previous_close_price);
				psInsSMEShare.setLong(23, objShare.sequence_no);
				psInsSMEShare.setDouble(24, objShare.todays_close_price);
				psInsSMEShare.setDouble(25, objShare.Todays_open);
				psInsSMEShare.setDouble(26, objShare.turnover);
				psInsSMEShare.setDouble(27, objShare.volume_traded);
				psInsSMEShare.setDouble(28, objShare.yield);
				psInsSMEShare.setDate(29, objShare.high_52_weeks_date);
				psInsSMEShare.setDate(30, objShare.low_52_weeks_date);
				// Added for Indicative Opening changes
				psInsSMEShare.setDouble(31, objShare.indicative_Open_price);
				psInsSMEShare.setDouble(32, objShare.indicative_Open_volume);
				psInsSMEShare.setDate(33, objShare.listingDate);

				psInsSMEShare.executeUpdate();

				return true;
				} catch (SQLException e) {
				logger.error("Exception in insertShareData: " + e, e);
	
			}


		if (objShare != null)
			objShare = null;
		return false;
	}
	
	public boolean updateSMEShareDataListingDate(TIP_SMEShareVO objShare) {

		try {

			psUpdateSMEShare.setDate(1, objShare.listingDate);
			psUpdateSMEShare.setString(2, objShare.fk_te_symbol);	
			psUpdateSMEShare.executeUpdate();
			return true;
			
		} catch (SQLException e) {
			logger.error("Exception in updateSMEShareDataListingDate: " + e, e);

		}


	if (objShare != null)
		objShare = null;
	return false;
}

	/**
	 * This method will insert recored in to the table EFP.TE_STOCK
	 * 
	 * @param objShare
	 *            contains attributes of EFP.TE_STOCK table
	 * @return boolean
	 */
	public boolean insertShareETFData(TIP_ShareVO objShare) {

			try {

				psInsShareETF.setString(1, TipConstants.SHARE_MARKET_CODE);
				psInsShareETF.setString(2, objShare.fk_te_symbol);
				psInsShareETF.setInt(3, objShare.market_status);
				psInsShareETF.setTimestamp(4, objShare.transaction_date);
				psInsShareETF.setDouble(5, objShare.last_trade_price);
				psInsShareETF.setInt(6, objShare.last_trade_quantity);
				psInsShareETF.setTimestamp(7, objShare.last_update_time);
				psInsShareETF.setDouble(8, objShare.ask_price);
				psInsShareETF.setLong(9, objShare.ask_quantity);
				psInsShareETF.setDouble(10, objShare.bid_price);
				psInsShareETF.setLong(11, objShare.bid_quantity);
				psInsShareETF.setDouble(12, objShare.net_change);
				psInsShareETF.setDouble(13, objShare.high_price);
				psInsShareETF.setDouble(14, objShare.high_52_weeks_price);
				psInsShareETF.setDouble(15, objShare.low_price);
				psInsShareETF.setDouble(16, objShare.low_52_weeks_price);
				psInsShareETF.setDouble(17, objShare.market_cap);
				psInsShareETF.setLong(18, objShare.no_of_trades);
				psInsShareETF.setDouble(19, objShare.Pbr);
				psInsShareETF.setDouble(20, objShare.Per);
				psInsShareETF.setDouble(21, objShare.percent_change);
				psInsShareETF.setDouble(22, objShare.Previous_close_price);
				psInsShareETF.setLong(23, objShare.sequence_no);
				psInsShareETF.setDouble(24, objShare.todays_close_price);
				psInsShareETF.setDouble(25, objShare.Todays_open);
				psInsShareETF.setDouble(26, objShare.turnover);
				psInsShareETF.setDouble(27, objShare.volume_traded);
				psInsShareETF.setDouble(28, objShare.yield);
				psInsShareETF.setDate(29, objShare.high_52_weeks_date);
				psInsShareETF.setDate(30, objShare.low_52_weeks_date);
				psInsShareETF.setDate(31, objShare.listingDate);

				psInsShareETF.executeUpdate();
				
				return true;

			} catch (SQLException e) {
				logger.error("Exception in insertShareETFData: " + e, e);

			}


		if (objShare != null)
			objShare = null;
		return false;
	}
	
	public boolean updateETFShareDataListingDate(TIP_ShareVO objShare) {

		try {

			psUpdateETFShare.setDate(1, objShare.listingDate);
			psUpdateETFShare.setString(2, objShare.fk_te_symbol);	
			psUpdateETFShare.executeUpdate();
			return true;
			
		} catch (SQLException e) {
			logger.error("Exception in updateETFShareDataListingDate: " + e, e);

		}

	if (objShare != null)
		objShare = null;
	return false;
}

	/**
	 * This method will insert recored in to the table EFP.TE_STOCK
	 * 
	 * @param objShare
	 *            contains attributes of EFP.TE_STOCK table
	 * @return boolean
	 */
	public boolean insertShareRIData(TIP_ShareVO objShare) {


	//	logger.info("insertShareRIData is called ");
			try {
				psInsShareRI.setString(1, TipConstants.SHARE_MARKET_CODE);
				psInsShareRI.setString(2, objShare.fk_te_symbol);
				psInsShareRI.setInt(3, objShare.market_status);
				psInsShareRI.setTimestamp(4, objShare.transaction_date);
				psInsShareRI.setDouble(5, objShare.last_trade_price);
				psInsShareRI.setInt(6, objShare.last_trade_quantity);
				psInsShareRI.setTimestamp(7, objShare.last_update_time);
				psInsShareRI.setDouble(8, objShare.ask_price);
				psInsShareRI.setLong(9, objShare.ask_quantity);
				psInsShareRI.setDouble(10, objShare.bid_price);
				psInsShareRI.setLong(11, objShare.bid_quantity);
				psInsShareRI.setDouble(12, objShare.net_change);
				psInsShareRI.setDouble(13, objShare.high_price);
				psInsShareRI.setDouble(14, objShare.high_52_weeks_price);
				psInsShareRI.setDouble(15, objShare.low_price);
				psInsShareRI.setDouble(16, objShare.low_52_weeks_price);
				psInsShareRI.setDouble(17, objShare.market_cap);
				psInsShareRI.setLong(18, objShare.no_of_trades);
				psInsShareRI.setDouble(19, objShare.Pbr);
				psInsShareRI.setDouble(20, objShare.Per);
				psInsShareRI.setDouble(21, objShare.percent_change);
				psInsShareRI.setDouble(22, objShare.Previous_close_price);
				psInsShareRI.setLong(23, objShare.sequence_no);
				psInsShareRI.setDouble(24, objShare.todays_close_price);
				psInsShareRI.setDouble(25, objShare.Todays_open);
				psInsShareRI.setDouble(26, objShare.turnover);
				psInsShareRI.setDouble(27, objShare.volume_traded);
				psInsShareRI.setDouble(28, objShare.yield);
				psInsShareRI.setDate(29, objShare.high_52_weeks_date);
				psInsShareRI.setDate(30, objShare.low_52_weeks_date);

				// Added for Rights Issue Changes for Referenc PriceUpper Limit
				// and Reference Price Lower Limit
				psInsShareRI.setInt(31, objShare.reference_price_upperlimit);
				psInsShareRI.setInt(32, objShare.reference_price_lowerlimit);
				psInsShareRI.setDate(33, objShare.listingDate);

				psInsShareRI.executeUpdate();
				return true;
		
			} catch (SQLException e) {
				logger.error("Exception in insertShareRIData: " + e, e);

			}


		if (objShare != null)
			objShare = null;
		return false;
	}
	
	public boolean updateRIShareDataListingDate(TIP_ShareVO objShare) {

		try {

			psUpdateRIShare.setDate(1, objShare.listingDate);
			psUpdateRIShare.setString(2, objShare.fk_te_symbol);	
			psUpdateRIShare.executeUpdate();
			return true;
			
		} catch (SQLException e) {
			logger.error("Exception in updateRIShareDataListingDate: " + e, e);

		}

	if (objShare != null)
		objShare = null;
	return false;
}

	/**
	 * This method will insert recored in to the table EFP.TE_STOCK
	 * 
	 * @param objShare
	 *            contains attributes of EFP.TE_STOCK table
	 * @return boolean
	 */
	// Added for SME
	public boolean insertSMEShareRIData(TIP_SMEShareVO objShare) {

			try {
				psInsSMEShareRI.setString(1, TipConstants.SME_MARKET_CODE);
				psInsSMEShareRI.setString(2, objShare.fk_te_symbol);
				psInsSMEShareRI.setInt(3, objShare.market_status);
				psInsSMEShareRI.setTimestamp(4, objShare.transaction_date);
				psInsSMEShareRI.setDouble(5, objShare.last_trade_price);
				psInsSMEShareRI.setInt(6, objShare.last_trade_quantity);
				psInsSMEShareRI.setTimestamp(7, objShare.last_update_time);
				psInsSMEShareRI.setDouble(8, objShare.ask_price);
				psInsSMEShareRI.setLong(9, objShare.ask_quantity);
				psInsSMEShareRI.setDouble(10, objShare.bid_price);
				psInsSMEShareRI.setLong(11, objShare.bid_quantity);
				psInsSMEShareRI.setDouble(12, objShare.net_change);
				psInsSMEShareRI.setDouble(13, objShare.high_price);
				psInsSMEShareRI.setDouble(14, objShare.high_52_weeks_price);
				psInsSMEShareRI.setDouble(15, objShare.low_price);
				psInsSMEShareRI.setDouble(16, objShare.low_52_weeks_price);
				psInsSMEShareRI.setDouble(17, objShare.market_cap);
				psInsSMEShareRI.setLong(18, objShare.no_of_trades);
				psInsSMEShareRI.setDouble(19, objShare.Pbr);
				psInsSMEShareRI.setDouble(20, objShare.Per);
				psInsSMEShareRI.setDouble(21, objShare.percent_change);
				psInsSMEShareRI.setDouble(22, objShare.Previous_close_price);
				psInsSMEShareRI.setLong(23, objShare.sequence_no);
				psInsSMEShareRI.setDouble(24, objShare.todays_close_price);
				psInsSMEShareRI.setDouble(25, objShare.Todays_open);
				psInsSMEShareRI.setDouble(26, objShare.turnover);
				psInsSMEShareRI.setDouble(27, objShare.volume_traded);
				psInsSMEShareRI.setDouble(28, objShare.yield);
				psInsSMEShareRI.setDate(29, objShare.high_52_weeks_date);
				psInsSMEShareRI.setDate(30, objShare.low_52_weeks_date);

				// Added for Rights Issue Changes for Referenc PriceUpper Limit
				// and Reference Price Lower Limit
				psInsSMEShareRI.setInt(31, objShare.reference_price_upperlimit);
				psInsSMEShareRI.setInt(32, objShare.reference_price_lowerlimit);
				psInsSMEShareRI.setDate(33, objShare.listingDate);

				psInsSMEShareRI.executeUpdate();
				return true;
			
			} catch (SQLException e) {
				logger.error("Exception in insertSMEShareRIData: " + e, e);

			}

		if (objShare != null)
			objShare = null;
		return false;
	}
	
	public boolean updateRISMEShareDataListingDate(TIP_SMEShareVO objShare) {

		try {

			psUpdateRISMEShare.setDate(1, objShare.listingDate);
			psUpdateRISMEShare.setString(2, objShare.fk_te_symbol);	
			psUpdateRISMEShare.executeUpdate();
			return true;
			
		} catch (SQLException e) {
			logger.error("Exception in updateRISMEShareDataListingDate: " + e, e);

		}

	if (objShare != null)
		objShare = null;
	return false;
}

	/**
	 * This method will insert Share and Bond Symbol data in to the table
	 * EFP.TE_SYMBOL and EFP.FI_SYMBOL
	 * 
	 * @param sym
	 *            ,type
	 * @return boolean
	 */
	public boolean insertSymbolData(String sym, String type) {

			try {
				logger.debug("inside  DB2Manager insertSymbolData function");
				if (type.equals(TipConstants.SHARE)) {
					psInsShareSymbol.setString(1, sym);
					psInsShareSymbol.setString(2, sym);
					psInsShareSymbol.executeUpdate();
				} else if (type.equals(TipConstants.BOND)) {
					psInsBondSymbol.setString(1, sym);
					psInsBondSymbol.setString(2, sym);
					psInsBondSymbol.executeUpdate();
				} else if (type.equals(TipConstants.SME)) {
					psInsSMESymbol.setString(1, sym);
					psInsSMESymbol.setString(2, sym);
					psInsSMESymbol.executeUpdate();
				}else if (type.equals(TipConstants.BOND_OTC)) {
					psInsOTCBondSymbol.setString(1, sym);
					psInsOTCBondSymbol.setString(2, sym);
					psInsOTCBondSymbol.executeUpdate();
				}
				return true;
			
			} catch (SQLException e) {
				logger.error("Exception in insertSymbolData: " + e, e);

			}


		return false;
	}

	/**
	 * Added for SME Project This method will update the Market_Type in
	 * SACP.TE_SYMBOL Table
	 * 
	 * @param sym
	 *            ,type
	 * @return boolean
	 */
	public boolean updateMarketType(String sym, String type) {


			try {

				psupdateMarketType.setString(1, type);
				psupdateMarketType.setString(2, sym);
				psupdateMarketType.executeUpdate();

				return true;

			} catch (SQLException e) {
				logger.error("Exception in updateMarketType: " + e, e);

			}


		return false;
	}

	/**
	 * This method will insert recored in to the table EFP.FI_INDEX
	 * 
	 * @param objIndex
	 *            contains attributes of EFP.FI_INDEX table
	 * @return boolean
	 */
	// TODO
	public boolean insertBondIndexData(TIP_BondIndexVO indexVO) {

		// logger.debug("Inside insertBondIndexData!!!!TIP_BondIndexVO{}", indexVO);
		
		String description_ar = indexVO.Description_AR;

			try {

				psInsBondIndex.setString(1, indexVO.Description_EN);
				if (null != description_ar && description_ar.length() > 0) {
					// description_ar= ;
					try {
						description_ar = reencodeArabic(description_ar);
					} catch (CharacterCodingException e) {
						logger.error("Exception in insertBondIndexData in coversion: "	+ e, e);
					}

				} else
					description_ar = indexVO.Description_EN;
				psInsBondIndex.setString(2, description_ar);
				// psInsBondIndex.setString(2, indexVO.Description_AR);
				psInsBondIndex.setDouble(3, indexVO.index_price);
				psInsBondIndex.setLong(4, indexVO.volume_trade);
				psInsBondIndex.setDouble(5, indexVO.turnover);
				psInsBondIndex.setLong(6, indexVO.no_of_trades);
				psInsBondIndex.setDouble(7, indexVO.net_change);
				psInsBondIndex.setDouble(8, indexVO.percent_change);
				psInsBondIndex.setDouble(9, indexVO.high_price);
				psInsBondIndex.setDouble(10, indexVO.low_price);
				psInsBondIndex.setDouble(11, indexVO.open_price);
				psInsBondIndex.setDouble(12, indexVO.previous_index_price);
				psInsBondIndex.setString(13, indexVO.Fk_te_symbol);
				psInsBondIndex.setTimestamp(14, indexVO.trans_time);
				psInsBondIndex.executeUpdate();
				// logger.debug("Inside insertBondIndexData!!!!DataInserted");
				return true;
				
			} catch (SQLException e) {
				logger.error("Exception in insertBondIndexData: " + e, e);

			}


		if (indexVO != null)
			indexVO = null;
		return false;
	}
	
	
	
	
	/**
	 * This method will check if bond exists in  table EFP.FI_STOCK
	 *
	 * @param objBond
	 *            contains attributes of EFP.FI_STOCK table
	 * @return boolean
	 */
	public boolean bondExistInFIStock(String bondSymbol) {

			try {
				
				boolean isBondExistsinFISTOCK = false;
				int count=0;

				logger.info("bondExistInFIStock is called :{}", bondSymbol);
				psExistsBond.setString(1, bondSymbol);				
				psExistsBond.executeUpdate();
				
				ResultSet rs = psExistsBond.executeQuery();
				
				if(rs.next())
				{
					count =  rs.getInt("COUNT");
				}
				
				if (count >0) isBondExistsinFISTOCK=true;
				
				logger.info("bondExistInFIStock  for sysmbol :{} isBondExistsinFISTOCK :{}", bondSymbol, isBondExistsinFISTOCK);
				
				return isBondExistsinFISTOCK;

			} catch (SQLException e) {
				logger.error("Exception in bondExistInFIStock: " + e, e);
				
			}
		return false;
	}

	
	
	/**
	 * This method will check if bond exists in  table EFP.FI_STOCK
	 *
	 * @param objBond
	 *            contains attributes of EFP.FI_STOCK table
	 * @return boolean
	 */
	public boolean bondExistInFIStockUnlisted(String bondSymbol) {

			try {
				
				boolean isBondExistsinFISTOCK = false;
				int count=0;

				logger.info("bondExistInFIStockUnlisted is called :{}", bondSymbol);
				psExistsOTCBond.setString(1, bondSymbol);				
				psExistsOTCBond.executeUpdate();
				
				ResultSet rs = psExistsOTCBond.executeQuery();
				
				if(rs.next())
				{
					count =  rs.getInt("COUNT");
				}
				
				if (count >0) isBondExistsinFISTOCK=true;
				
				logger.info("bondExistInFIStock  for sysmbol :{} isBondExistsinFISTOCK :{}", bondSymbol, isBondExistsinFISTOCK);
				
				return isBondExistsinFISTOCK;

			} catch (SQLException e) {
				logger.error("Exception in bondExistInFIStockUnlisted: " + e, e);
				
			}
		return false;
	}
	
	
	
	
	  /**
     * This  method will insert recored in to the table WEB.Index_Components
     * @param objShare contains attributes of  WEB.Index_Components
     * @return boolean
     */
     public boolean insertIndexComponents(String symbol, String index) {
                    boolean succeeded = false;
                      try {
                                                    
                             psInsIndexComponents.setString(1, index);
                             psInsIndexComponents.setString(2, symbol);
                                                    
                             psInsIndexComponents.executeUpdate();
                             succeeded = true;
                          } catch (SQLException e) { 
                        	  logger.error("Exception in insertIndexComponents: " + e, e);
                           }
                     return succeeded;
     }

     
     /**
      * This  method will insert recored in to the table WEB.Index_Components
      * @param objShare contains attributes of  WEB.Index_Components
      * @return boolean
      */
      public boolean deleteIndexComponents(String symbol, String index) {                     
    	  boolean succeeded = false;
               try {
                                                     
            	   psDelIndexComponents.setString(1, index);
                   psDelIndexComponents.setString(2, symbol);
                                                     
                   psDelIndexComponents.executeUpdate();
                   succeeded = true;
                                                     
                   } catch (SQLException e) {
                	   logger.error("Exception in deleteIndexComponents: " + e, e);                                  
                   }
                   
                      return succeeded;
      }
      
      
      
      /**
       * This  method will insert recored in to the table WEB.Index_Components
       * @param objShare contains attributes of  WEB.Index_Components
       * @return boolean
       */
       public boolean deleteIndexComponentsTable() {                     
     	  boolean succeeded = false;
                try {
                                                      
             	                                                      
                    psDelIndexComponentsTable.executeUpdate();
                    succeeded = true;
                    logger.info("Index Components Tables Deleted successfully");
                                                      
                    } catch (SQLException e) {
                 	   logger.error("Exception in deleteIndexComponentsTable: " + e, e);                                  
                    }
                    
                       return succeeded;
       }
       
      /**
       * This  method will insert recored in to the table WEB.Index_Components
       * @param objShare contains attributes of  WEB.Index_Components
       * @return boolean
       */
       public boolean isIndexComponentsAdded(String symbol, String index) {
                      
                    
                       boolean isIndexAlreadyAdded=false;
                       int count=0;
                    
                                       try {
                                                      
                                    	   psSelIndexComponents.setString(1, index);
                                    	   psSelIndexComponents.setString(2, symbol);
                                                      
                                    	   ResultSet rs = psSelIndexComponents.executeQuery();
                                    	   if(rs.next())
                                    	   {
                                    		   count =  rs.getInt("COUNT");
                                    	   }
                                                   
                                    	   isIndexAlreadyAdded=(count>0)?true:false;
                                    		 
                                    	  return isIndexAlreadyAdded;
                                    	  
                                       } catch (SQLException e) {
                                                       logger.error("Exception in isIndexComponentsAdded: "+e, e);
                                                      
                                       }
                      
                      

                       return isIndexAlreadyAdded;
       }

}
