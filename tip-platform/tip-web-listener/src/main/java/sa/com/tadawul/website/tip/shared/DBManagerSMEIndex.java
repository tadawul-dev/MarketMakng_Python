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
import sa.com.tadawul.website.tip.vo.TIP_SMEIndexVO;

/**
 * @description This class is used to get Database Connection and all the
 *              prepared Statements.
 *
 */
public class DBManagerSMEIndex extends DBManager {

	private static final Logger logger = LogManager.getLogger(DBManagerSMEIndex.class);
	private PreparedStatement psInitializeDBforSMEShareMarket10;
	private PreparedStatement psInitializeDBforSMEShareMarket11;
	private PreparedStatement psUpdSMEIndex;
	private PreparedStatement psEODSMEShareMarketDelHsTeIndex;
	private PreparedStatement psEODSMEShareMarketDelHsTeIndex_Adj;
	private PreparedStatement psEODSMEShareMarketInsTeIndex;
	private PreparedStatement psEODSMEShareMarketInsTeIndex_Adj;

	static long  lastUpdatedTime = 0;
	static long currentUpdatedTime = 0;
	
	public int totalSMEIndexCount = 0;
	
	public DBManagerSMEIndex(String threadName) throws Exception {
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

			sbPrepareQueries.append("UPDATE EFP.TE_SME_INDEX TE_ID SET (TE_ID.PREVIOUS_INDEX_PRICE ,TE_ID.NET_CHANGE)"
							+ "=(SELECT TE_IS.INDEX_PRICE, TE_IS.INDEX_PRICE - TE_IS.PREVIOUS_INDEX_PRICE FROM EFP.TE_SME_INDEX TE_IS"
							+ " WHERE TE_ID.FK_TE_SYMBOL = TE_IS.FK_TE_SYMBOL)");
			psInitializeDBforSMEShareMarket10 = g_oConnection.prepareStatement(sbPrepareQueries.toString());

			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("UPDATE EFP.TE_SME_INDEX SET VOLUME_TRADED = 0, TURNOVER = 0, NO_OF_TRADES = 0, NET_CHANGE = 0, PERCENT_CHANGE = 0, "
							+ "HIGH_PRICE = 0, LOW_PRICE = 0, OPEN_PRICE = 0, TIMESTAMP = systimestamp , NO_OF_COMPANIES_TRADED=0 ");
			psInitializeDBforSMEShareMarket11 = g_oConnection.prepareStatement(sbPrepareQueries.toString());

			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("UPDATE EFP.TE_SME_INDEX SET VOLUME_TRADED = ?,TURNOVER = ?, INDEX_PRICE = ?,"
							+ "NET_CHANGE = ?, PERCENT_CHANGE = ?,HIGH_PRICE = ?, LOW_PRICE = ?, OPEN_PRICE = ?,PREVIOUS_INDEX_PRICE = ?,"
							+ "TIMESTAMP = ?, NO_OF_TRADES = ?  , NO_OF_COMPANIES_TRADED =? WHERE FK_TE_SYMBOL =?");

			psUpdSMEIndex = g_oConnection.prepareStatement(sbPrepareQueries.toString());


			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("DELETE from WEB.HS_TE_SME_INDEX where trunc(pk_hs_te_index) = trunc(sysdate) ");
			psEODSMEShareMarketDelHsTeIndex = g_oConnection.prepareStatement(sbPrepareQueries.toString());

			// Changed for SR6 for VWAP
			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("DELETE from WEB.HS_TE_SME_INDEX_ADJUSTED where trunc(pk_hs_te_index) = trunc(sysdate) ");
			psEODSMEShareMarketDelHsTeIndex_Adj = g_oConnection.prepareStatement(sbPrepareQueries.toString());

			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("INSERT INTO WEB.HS_TE_SME_INDEX (PK_HS_TE_INDEX,SYMBOL, HIGH_PRICE, LOW_PRICE,"
							+ "OPEN_PRICE, PREVIOUS_INDEX_PRICE, NET_CHANGE,NO_OF_TRADES, PERCENT_CHANGE, TURNOVER,"
							+ "VOLUME_TRADED, INDEX_PRICE, NO_OF_COMPANIES_TRADED) SELECT distinct systimestamp ,FK_TE_SYMBOL, HIGH_PRICE, LOW_PRICE, OPEN_PRICE, "
							+ "PREVIOUS_INDEX_PRICE,"
							+ "NET_CHANGE, TE_INDEX.NO_OF_TRADES, PERCENT_CHANGE, TE_INDEX.TURNOVER, TE_INDEX.VOLUME_TRADED, "
							+ "INDEX_PRICE, NO_OF_COMPANIES_TRADED " + "FROM EFP.TE_SME_INDEX TE_INDEX");
			psEODSMEShareMarketInsTeIndex = g_oConnection.prepareStatement(sbPrepareQueries.toString());

			// Added for SR6 for VWAP changes
			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("INSERT INTO WEB.HS_TE_SME_INDEX_adjusted (PK_HS_TE_INDEX,SYMBOL, HIGH_PRICE, LOW_PRICE,"
							+ "OPEN_PRICE, PREVIOUS_INDEX_PRICE, NET_CHANGE,NO_OF_TRADES, PERCENT_CHANGE, TURNOVER,"
							+ "VOLUME_TRADED, INDEX_PRICE, NO_OF_COMPANIES_TRADED) SELECT distinct systimestamp,FK_TE_SYMBOL, HIGH_PRICE, LOW_PRICE, OPEN_PRICE, "
							+ "PREVIOUS_INDEX_PRICE,"
							+ "NET_CHANGE, TE_INDEX.NO_OF_TRADES, PERCENT_CHANGE, TE_INDEX.TURNOVER, TE_INDEX.VOLUME_TRADED, "
							+ "INDEX_PRICE , NO_OF_COMPANIES_TRADED " + "FROM EFP.TE_SME_INDEX TE_INDEX");
			psEODSMEShareMarketInsTeIndex_Adj = g_oConnection.prepareStatement(sbPrepareQueries.toString());

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
			
			if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.UPDATER_THREADS_DB_UPDATE_INTERVAL )
			{			
				psUpdSMEIndex.executeBatch();
			logger.debug("DBManagerSMEIndex Batch Updated");
			lastUpdatedTime = currentUpdatedTime;			
			}

			commitIfNeeded();

		} catch (BatchUpdateException be2) {
			logger.error("BatchUpdateException in commitTrans() while doing psUpdSMEIndex "+ be2);
			try {
				psUpdSMEIndex.clearBatch();
			} catch (SQLException e1) {
				logger.error("Exception in psUpdSMEIndex in clearBatch(): " + e1, e1);
			}
		}catch (SQLException ex) {
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

			if (psInitializeDBforSMEShareMarket10 != null){
				psInitializeDBforSMEShareMarket10.close();
			}
			
			if (psInitializeDBforSMEShareMarket11 != null){
				psInitializeDBforSMEShareMarket11.close();
			}

			if (psUpdSMEIndex != null){
				psUpdSMEIndex.close();
			}
			
			if (psEODSMEShareMarketDelHsTeIndex != null){
				psEODSMEShareMarketDelHsTeIndex.close();
			}
			
			if (psEODSMEShareMarketDelHsTeIndex_Adj != null){
				psEODSMEShareMarketDelHsTeIndex_Adj.close();
			}
			
			if (psEODSMEShareMarketInsTeIndex != null){
				psEODSMEShareMarketInsTeIndex.close();
			}
			
			if (psEODSMEShareMarketInsTeIndex_Adj != null){
				psEODSMEShareMarketInsTeIndex_Adj.close();
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
	 * This method will initialize Index table at BOD
	 * 
	 * @return boolean
	 */
	// Added for SME
	public boolean initializeDBforSMEIndex() {


			try {
				logger.debug("inside  DB2Manager initializeDBforSMEIndex function");
				if (logger.isDebugEnabled()) logger.debug("initializeDBforSMEIndex --> dtFeedReqDate = {}", dtFeedReqDate);
				if (logger.isDebugEnabled()) logger.debug("inside initializeDBforSMEIndex --> dtSMEShareIndexLastUpdateTime = {}", LoadInitialData.dtSMEShareIndexLastUpdateTime);
				if (!LoadInitialData.dtSMEShareIndexLastUpdateTime.equals(dtFeedReqDate)) {
					psInitializeDBforSMEShareMarket10.executeUpdate();
					logger.info("initializeDBforSMEIndex: EFP.TE_SME_INDEX --> Previous index price updated");
					psInitializeDBforSMEShareMarket11.executeUpdate();
					commitTrans(true);
					logger.info("initializeDBforIndex: EFP.TE_SME_INDEX table columns initialized to zero");
				}

				return true;
			} catch (SQLException e) {
				logger.error("Exception in initializeDBforSMEIndex: " + e, e);

			}

		logger.debug("Exit  DB2Manager initializeDBforSMEIndex function");
		return false;
	}

	
	/**
	 * This method will insert record into the table WEB.HS_FI_STOCK_INTRA
	 * 
	 * @param objIndex
	 *            contains attributes of WEB.HS_FI_STOCK_INTRA table
	 * @return boolean
	 */
	// Added for SME
	public boolean updateSMEIndexData(TIP_SMEIndexVO objIndex,boolean isCommit) {

		try {

			psUpdSMEIndex.setLong(1, objIndex.volume_trade);
			psUpdSMEIndex.setDouble(2, objIndex.turnover);
			psUpdSMEIndex.setDouble(3, objIndex.index_price);
			psUpdSMEIndex.setDouble(4, objIndex.net_change);
			psUpdSMEIndex.setDouble(5, objIndex.percent_change);
			psUpdSMEIndex.setDouble(6, objIndex.high_price);
			psUpdSMEIndex.setDouble(7, objIndex.low_price);
			psUpdSMEIndex.setDouble(8, objIndex.open_price);
			psUpdSMEIndex.setDouble(9, objIndex.previous_index_price);
			psUpdSMEIndex.setTimestamp(10, objIndex.trans_time);
			psUpdSMEIndex.setLong(11, objIndex.no_of_trades);
			psUpdSMEIndex.setInt(12, objIndex.no_of_symbols_traded);
			psUpdSMEIndex.setString(13, objIndex.Fk_te_symbol);

			psUpdSMEIndex.addBatch();
			
			currentUpdatedTime =  System.currentTimeMillis();
			
			/*logger.info("DBManagerSMEIndex: objIndex.Fk_te_symbol :{}", objIndex.Fk_te_symbol);
			logger.info("DBManagerSMEIndex: objIndex.volume_trade :{}", objIndex.volume_trade);
			logger.info("DBManagerSMEIndex: LastTimestamp :{}", lastUpdatedTime);
			logger.info("DBManagerSMEIndex: CurrentTimestamp :{}", currentUpdatedTime);
			logger.info("DBManagerSMEIndex: TimeDifference :{}", (currentUpdatedTime-lastUpdatedTime));
			logger.info("DBManagerSMEIndex: isEndOfBatch :{}", ((currentUpdatedTime-lastUpdatedTime)>TipConstants.UPDATER_THREADS_DB_UPDATE_INTERVAL));
			logger.info("DBManagerSMEIndex: isCommit :{}", isCommit);
*/			
			if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.UPDATER_THREADS_DB_UPDATE_INTERVAL )
			{
			//if (objIndex.isEndOfBatch)
				psUpdSMEIndex.executeBatch();
			logger.debug("DBManagerSMEIndex Batch Updated");
			lastUpdatedTime = currentUpdatedTime;			
			}
			
			totalSMEIndexCount++;
			if (logger.isDebugEnabled()) logger.debug("DBManagerSMEIndex: updateIndexData :{}", totalSMEIndexCount);
			return true;
		} catch (Exception e) {
			logger.error("Exception in updateSMEIndexData: " + e, e);

		}

		if (objIndex != null)
			objIndex = null;
		return false;
	}
	
	/**
	 * This method will handle the EOD logic for Index
	 * 
	 * @return boolean
	 */
	// Added for SME
	public boolean EODforSMEIndex() {


			try {
				logger.debug("inside  DB2Manager EODforSMEIndex function");

				psEODSMEShareMarketDelHsTeIndex.executeUpdate();
				psEODSMEShareMarketInsTeIndex.executeUpdate();
				logger.info("EODforSMEIndex: Done");


				return true; 
			} catch (SQLException e) {
				logger.error("Exception in EODforSMEIndex: " + e, e);
	
			}
		

		logger.debug("Exit  DB2Manager EODforSMEIndex function");
		return false;
	}
	
	public boolean EODforSMEIndex_Adj() {

			try {
				logger.debug("inside  DB2Manager EODforSMEIndex_Adj function");

				psEODSMEShareMarketDelHsTeIndex_Adj.executeUpdate();
				psEODSMEShareMarketInsTeIndex_Adj.executeUpdate();
				logger.info("EODforSMEIndex_Adj: Done");

				return true;
			} catch (SQLException e) {
				logger.error("Exception in EODforSMEIndex_Adj: " + e, e);
			
			}


		logger.debug("Exit  DB2Manager EODforSMEIndex function");
		return false;
	}
	
	
}
