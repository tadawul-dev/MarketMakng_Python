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
import sa.com.tadawul.website.tip.vo.TIP_IndexVO;

/**
 * @description This class is used to get Database Connection and all the
 *              prepared Statements.
 *
 */
public class DBManagerIndex extends DBManager {

	private final static Log logger = LogFactory.getLog(DBManagerIndex.class);

	
	private PreparedStatement psInitializeDBforShareMarket10;
	
	private PreparedStatement psInitializeDBforShareMarket11;
	private PreparedStatement psUpdIndex;

	private PreparedStatement psEODShareMarketDelHsTeIndex;

	private PreparedStatement psEODShareMarketDelHsTeIndex_Adj;

	private PreparedStatement psEODShareMarketInsTeIndex;

	private PreparedStatement psEODShareMarketInsTeIndex_Adj;
	
	static long  lastUpdatedTime = 0;
	static long currentUpdatedTime = 0;
	private int totalIndexCount = 0;

	public DBManagerIndex(String threadName) throws Exception {
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
				g_oConnection = DriverManager.getConnection(TipConstants.DB_URL, TipConstants.DB_USER,	TipConstants.DB_PASS);
				g_bClassLoaded = true;
			}

	
			logger.debug("Exit DB2Manager openConnection function");
		} catch (Exception e) {

			logger.error("Error in DB2Manager in openConnection  method  while openConnection "	+ e);
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

										
					sbPrepareQueries
						.append("UPDATE EFP.TE_INDEX TE_ID SET (TE_ID.PREVIOUS_INDEX_PRICE ,TE_ID.NET_CHANGE)"
								+ "=(SELECT TE_IS.INDEX_PRICE, TE_IS.INDEX_PRICE - TE_IS.PREVIOUS_INDEX_PRICE FROM EFP.TE_INDEX TE_IS"
								+ " WHERE TE_ID.FK_TE_SYMBOL = TE_IS.FK_TE_SYMBOL ) WHERE TE_ID.FK_TE_SYMBOL NOT IN ('TT50CITR', 'TASITR') ");
								
				psInitializeDBforShareMarket10 = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries
						.append("UPDATE EFP.TE_INDEX SET VOLUME_TRADED = 0, TURNOVER = 0, NO_OF_TRADES = 0, NET_CHANGE = 0, PERCENT_CHANGE = 0, "
								+ "HIGH_PRICE = 0, LOW_PRICE = 0, OPEN_PRICE = 0, TIMESTAMP = systimestamp, NO_OF_COMPANIES_TRADED=0 WHERE FK_TE_SYMBOL NOT IN ('TT50CITR','TASITR')");
				psInitializeDBforShareMarket11 = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries
						.append("UPDATE EFP.TE_INDEX SET VOLUME_TRADED = ?,TURNOVER = ?, INDEX_PRICE = ?,"
								+ "NET_CHANGE = ?, PERCENT_CHANGE = ?,HIGH_PRICE = ?, LOW_PRICE = ?, OPEN_PRICE = ?,PREVIOUS_INDEX_PRICE = ?,"
								+ "TIMESTAMP = ?, NO_OF_TRADES = ? , NO_OF_COMPANIES_TRADED =? WHERE FK_TE_SYMBOL =? AND FK_TE_SYMBOL NOT IN ('TT50CITR','TASITR') ");

				psUpdIndex = g_oConnection.prepareStatement(sbPrepareQueries
						.toString());

				sbPrepareQueries.delete(0, sbPrepareQueries.length());

				sbPrepareQueries
						.append("DELETE from WEB.HS_TE_INDEX where trunc(pk_hs_te_index) = trunc(sysdate) and  SYMBOL NOT IN ('TT50CITR','TASITR')");
				psEODShareMarketDelHsTeIndex = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				// Changed for SR6 for VWAP
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries
						.append("DELETE from WEB.HS_TE_INDEX_ADJUSTED where trunc(pk_hs_te_index) = trunc(sysdate) and SYMBOL NOT IN ('TT50CITR','TASITR') ");
				psEODShareMarketDelHsTeIndex_Adj = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries
						.append("INSERT INTO WEB.HS_TE_INDEX (PK_HS_TE_INDEX,SYMBOL, HIGH_PRICE, LOW_PRICE,"
								+ "OPEN_PRICE, PREVIOUS_INDEX_PRICE, NET_CHANGE,NO_OF_TRADES, PERCENT_CHANGE, TURNOVER,"
								+ "VOLUME_TRADED, INDEX_PRICE, NO_OF_COMPANIES_TRADED) SELECT distinct systimestamp ,FK_TE_SYMBOL, HIGH_PRICE, LOW_PRICE, OPEN_PRICE, "
								+ "PREVIOUS_INDEX_PRICE,"
								+ "NET_CHANGE, TE_INDEX.NO_OF_TRADES, PERCENT_CHANGE, TE_INDEX.TURNOVER, TE_INDEX.VOLUME_TRADED, "
								+ "INDEX_PRICE , NO_OF_COMPANIES_TRADED " + "FROM EFP.TE_INDEX TE_INDEX WHERE FK_TE_SYMBOL NOT IN ('TT50CITR','TASITR')");
				psEODShareMarketInsTeIndex = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				// Added for SR6 for VWAP changes
				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries
						.append("INSERT INTO WEB.HS_TE_INDEX_adjusted (PK_HS_TE_INDEX,SYMBOL, HIGH_PRICE, LOW_PRICE,"
								+ "OPEN_PRICE, PREVIOUS_INDEX_PRICE, NET_CHANGE,NO_OF_TRADES, PERCENT_CHANGE, TURNOVER,"
								+ "VOLUME_TRADED, INDEX_PRICE, NO_OF_COMPANIES_TRADED) SELECT distinct systimestamp,FK_TE_SYMBOL, HIGH_PRICE, LOW_PRICE, OPEN_PRICE, "
								+ "PREVIOUS_INDEX_PRICE,"
								+ "NET_CHANGE, TE_INDEX.NO_OF_TRADES, PERCENT_CHANGE, TE_INDEX.TURNOVER, TE_INDEX.VOLUME_TRADED, "
								+ "INDEX_PRICE , NO_OF_COMPANIES_TRADED " + "FROM EFP.TE_INDEX TE_INDEX  WHERE FK_TE_SYMBOL NOT IN ('TT50CITR','TASITR') ");
				psEODShareMarketInsTeIndex_Adj = g_oConnection.prepareStatement(sbPrepareQueries.toString());
				
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
				psUpdIndex.executeBatch();			
				lastUpdatedTime = currentUpdatedTime;				
			}			
			g_oConnection.commit();

		} catch (BatchUpdateException be2) {
			logger.error("BatchUpdateException in commitTrans() while doing IIndexUpdate "+ be2);
			try {
				psUpdIndex.clearBatch();
			} catch (SQLException e1) {
				logger.error("Exception in psUpdIndex in clearBatch(): " + e1, e1);
			}
		}catch(SQLException e)
		{
			logger.error("SQLException in commitTrans() while doing IIndexUpdate "+ e);
		}
			
			

		

	}

	/**
	 * This method will close the existing DB Connection
	 */
	public void closeConnection() {
		try {
			logger.debug("inside  DB2Manager closeConnection function");
			
			if (psInitializeDBforShareMarket10 != null){
				psInitializeDBforShareMarket10.close();
			}
			if (psInitializeDBforShareMarket11 != null){
				psInitializeDBforShareMarket11.close();
			}
			if (psUpdIndex != null){
				psUpdIndex.close();
			}
			if (psEODShareMarketDelHsTeIndex != null){
				psEODShareMarketDelHsTeIndex.close();
			}
			if (psEODShareMarketDelHsTeIndex_Adj != null){
				psEODShareMarketDelHsTeIndex_Adj.close();
			}
			if (psEODShareMarketInsTeIndex != null){
				psEODShareMarketInsTeIndex.close();
			}
			if (psEODShareMarketInsTeIndex_Adj != null){
				psEODShareMarketInsTeIndex_Adj.close();
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
	public boolean initializeDBforIndex() {


			try {
				logger.debug("inside  DB2Manager initializeDBforIndex function");
				logger.debug("initializeDBforIndex --> dtFeedReqDate = "
						+ dtFeedReqDate);
				logger.debug("inside initializeDBforIndex --> dtShareIndexLastUpdateTime = "
						+ LoadInitialData.dtShareIndexLastUpdateTime);
				if (!LoadInitialData.dtShareIndexLastUpdateTime.equals(dtFeedReqDate)) {
					psInitializeDBforShareMarket10.executeUpdate();
					logger.info("initializeDBforIndex: EFP.TE_INDEX --> Previous index price updated");

					psInitializeDBforShareMarket11.executeUpdate();
					commitTrans(true);
					logger.info("initializeDBforIndex: EFP.TE_INDEX table columns initialized to zero");
				}
				return true;

			} catch (SQLException e) {
				logger.error("Exception in initializeDBforIndex: " + e, e);
				
			}
		
		logger.debug("Exit  DB2Manager initializeDBforIndex function");
		return false;
	}

	/**
	 * This method will insert record into the table WEB.HS_FI_STOCK_INTRA
	 * 
	 * @param objIndex
	 *            contains attributes of WEB.HS_FI_STOCK_INTRA table
	 * @return boolean
	 */
	public boolean updateIndexData(TIP_IndexVO objIndex,boolean isCommit) {

			try {

				psUpdIndex.setLong(1, objIndex.volume_trade);
				psUpdIndex.setDouble(2, objIndex.turnover);
				psUpdIndex.setDouble(3, objIndex.index_price);
				psUpdIndex.setDouble(4, objIndex.net_change);
				psUpdIndex.setDouble(5, objIndex.percent_change);
				psUpdIndex.setDouble(6, objIndex.high_price);
				psUpdIndex.setDouble(7, objIndex.low_price);
				psUpdIndex.setDouble(8, objIndex.open_price);
				psUpdIndex.setDouble(9, objIndex.previous_index_price);
				psUpdIndex.setTimestamp(10, objIndex.trans_time);
				psUpdIndex.setLong(11, objIndex.no_of_trades);
					
				psUpdIndex.setInt(12, objIndex.no_of_symbols_traded);
				psUpdIndex.setString(13, objIndex.Fk_te_symbol);

				psUpdIndex.addBatch();
				
				currentUpdatedTime =  System.currentTimeMillis();
				
						
				if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.UPDATER_THREADS_DB_UPDATE_INTERVAL)
				{
				//if (objIndex.isEndOfBatch)
					psUpdIndex.executeBatch();
					//logger.info("DBManagerIndex Batch Updated");
					lastUpdatedTime = currentUpdatedTime;
					
				}

				totalIndexCount++;		
				
				logger.debug("DBManagerIndex: totalIndex Update Count :"+totalIndexCount);
				
				return true;

			} catch (Exception e) {
				logger.error("Exception in updateIndexData: " + e, e);
		
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
	public boolean EODforIndex() {

			try {
				logger.debug("inside  DB2Manager EODforIndex function");
				// the below two lines are commented since TIP already provides
				// this data correctly, will be enabled if required
				// psEODShareMarketUpdTeIndex.executeUpdate();
				psEODShareMarketDelHsTeIndex.executeUpdate();
				psEODShareMarketInsTeIndex.executeUpdate();
				logger.info("EODforIndex: Done");
				return true;

			} catch (SQLException e) {
				logger.error("Exception in EODforIndex: " + e, e);

			}


		logger.debug("Exit  DB2Manager EODforIndex function");
		return false;
	}

	public boolean EODforIndex_Adj() {

			try {
				logger.debug("inside  DB2Manager EODforIndex_Adj function");
				// the below two lines are commented since TIP already provides
				// this data correctly, will be enabled if required
				// psEODShareMarketUpdTeIndex.executeUpdate();
				psEODShareMarketDelHsTeIndex_Adj.executeUpdate();
				psEODShareMarketInsTeIndex_Adj.executeUpdate();
				logger.info("EODforIndex_Adj: Done");
				return true;

			} catch (SQLException e) {
				logger.error("Exception in EODforIndex_Adj: " + e, e);

			}
		

		logger.debug("Exit  DB2Manager EODforIndex function");
		return false;
	}

	

}
