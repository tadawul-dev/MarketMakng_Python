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
import sa.com.tadawul.website.tip.vo.TIP_BondIndexVO;

/**
 * @description This class is used to get Database Connection and all the
 *              prepared Statements.
 *
 */
public class DBManagerBondIndex extends DBManager{


	private final static Log logger = LogFactory.getLog(DBManagerBondIndex.class);
	private PreparedStatement psInitializeDBforBondIndexShareMarket10;
	private PreparedStatement psInitializeDBforBondIndexShareMarket11;
	private PreparedStatement psUpdBondIndex;
	private PreparedStatement psEODShareMarketDelHsFiIndex;
	private PreparedStatement psEODShareMarketInsFiIndex;
	static long  lastUpdatedTime = 0;
	static long currentUpdatedTime = 0;
	public int totalBondIndexCount = 0;


	public DBManagerBondIndex(String threadName) throws Exception {
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

			logger.error("Error in DB2Manager in openConnection  method  while openConnection " + e);
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

				sbPrepareQueries.append("UPDATE EFP.FI_INDEX TE_ID SET (TE_ID.PREVIOUS_INDEX_PRICE ,TE_ID.NET_CHANGE)"
								+ "=(SELECT TE_IS.INDEX_PRICE, TE_IS.INDEX_PRICE - TE_IS.PREVIOUS_INDEX_PRICE FROM EFP.FI_INDEX TE_IS"
								+ " WHERE TE_ID.FK_TE_SYMBOL = TE_IS.FK_TE_SYMBOL  )");
				psInitializeDBforBondIndexShareMarket10 = g_oConnection	.prepareStatement(sbPrepareQueries.toString());

				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("UPDATE EFP.FI_INDEX FI_INDEX SET VOLUME_TRADED = 0, TURNOVER = 0, NO_OF_TRADES = 0, NET_CHANGE = 0, PERCENT_CHANGE = 0, "
								+ "HIGH_PRICE = 0, LOW_PRICE = 0, OPEN_PRICE = 0, TIMESTAMP = systimestamp WHERE  FI_INDEX.FK_TE_SYMBOL IN  ( SELECT SECTOR_SYMBOL FROM WEB.RF_SECTOR_SUKUK RF_SECTOR_SUKUK WHERE RF_SECTOR_SUKUK.SUKUK_TYPE NOT IN ('I') )");
				psInitializeDBforBondIndexShareMarket11 = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("UPDATE EFP.FI_INDEX SET VOLUME_TRADED = ?,TURNOVER = ?, INDEX_PRICE = ?,"
								+ "NET_CHANGE = ?, PERCENT_CHANGE = ?,HIGH_PRICE = ?, LOW_PRICE = ?, OPEN_PRICE = ?,PREVIOUS_INDEX_PRICE = ?,"
								+ "TIMESTAMP = ?, NO_OF_TRADES = ? WHERE FK_TE_SYMBOL =?");

				psUpdBondIndex = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				sbPrepareQueries.delete(0, sbPrepareQueries.length());

				sbPrepareQueries.append("DELETE from WEB.HS_FI_INDEX where trunc(pk_hs_te_index) = trunc(sysdate) AND HS_FI_INDEX.SYMBOL IN  ( SELECT SECTOR_SYMBOL FROM WEB.RF_SECTOR_SUKUK RF_SECTOR_SUKUK WHERE RF_SECTOR_SUKUK.SUKUK_TYPE NOT IN ('I') ) ");
				psEODShareMarketDelHsFiIndex = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				sbPrepareQueries.delete(0, sbPrepareQueries.length());
				sbPrepareQueries.append("INSERT INTO WEB.HS_FI_INDEX (PK_HS_TE_INDEX,SYMBOL, HIGH_PRICE, LOW_PRICE,"
								+ "OPEN_PRICE, PREVIOUS_INDEX_PRICE, NET_CHANGE,NO_OF_TRADES, PERCENT_CHANGE, TURNOVER,"
								+ "VOLUME_TRADED, INDEX_PRICE) SELECT systimestamp,FK_TE_SYMBOL, HIGH_PRICE, LOW_PRICE, OPEN_PRICE, "
								+ "PREVIOUS_INDEX_PRICE, NET_CHANGE, FI_INDEX.NO_OF_TRADES, PERCENT_CHANGE, FI_INDEX.TURNOVER, FI_INDEX.VOLUME_TRADED, "
								+ "INDEX_PRICE "
								+ "FROM EFP.FI_INDEX FI_INDEX WHERE FI_INDEX.FK_TE_SYMBOL IN  ( SELECT SECTOR_SYMBOL FROM WEB.RF_SECTOR_SUKUK RF_SECTOR_SUKUK WHERE RF_SECTOR_SUKUK.SUKUK_TYPE NOT IN ('I') )");
				psEODShareMarketInsFiIndex = g_oConnection.prepareStatement(sbPrepareQueries.toString());

				logger.debug("Exit DB2Manager prepareStatements function");
			
			
			} catch (Exception e) {
				logger.error("Exception while preparing query ["+ sbPrepareQueries.toString() + "] is " + e);
				throw e;
			}
	}


	// TODO Variable names has to be changed
	public boolean updateBondIndexData(TIP_BondIndexVO objIndex,boolean isCommit) {

		 logger.debug("Inside updateBondIndexData!!!!!!!TIP_BondIndexVO"+objIndex);

			try {

				psUpdBondIndex.setLong(1, objIndex.volume_trade);
				psUpdBondIndex.setDouble(2, objIndex.turnover);
				psUpdBondIndex.setDouble(3, objIndex.index_price);
				psUpdBondIndex.setDouble(4, objIndex.net_change);
				psUpdBondIndex.setDouble(5, objIndex.percent_change);
				psUpdBondIndex.setDouble(6, objIndex.high_price);
				psUpdBondIndex.setDouble(7, objIndex.low_price);
				psUpdBondIndex.setDouble(8, objIndex.open_price);
				psUpdBondIndex.setDouble(9, objIndex.previous_index_price);
				psUpdBondIndex.setTimestamp(10, objIndex.trans_time);
				psUpdBondIndex.setLong(11, objIndex.no_of_trades);
				psUpdBondIndex.setString(12, objIndex.Fk_te_symbol);

				psUpdBondIndex.addBatch();
				
				currentUpdatedTime =  System.currentTimeMillis();
				
				if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.UPDATER_THREADS_DB_UPDATE_INTERVAL  )
				{
				//if (objIndex.isEndOfBatch)
					psUpdBondIndex.executeBatch();
					logger.debug("DBManagerBondIndex Batch Updated");
					lastUpdatedTime = currentUpdatedTime;
					
				}
				totalBondIndexCount++;				
				logger.debug("DBManagerBondIndex: updateBondIndexData :"+totalBondIndexCount);
				
				return true ;
			} catch (Exception e) {
				logger.error("Exception in updateBondIndexData: " + e, e);
			}


		if (objIndex != null)
			objIndex = null;
		
		return false;
	}

	// TODO Variable names has to be changed
	public boolean initializeDBforBondIndex() {

		
			try {
				logger.debug("inside  DB2Manager initializeDBforBondIndex function");
				logger.debug("initializeDBforBondIndex --> dtFeedReqDate = "
						+ dtFeedReqDate);
				logger.debug("inside initializeDBforBondIndex --> dtBondLastUpdateTime = "
						+ LoadInitialData.dtBondLastUpdateTime);
				if (!LoadInitialData.dtBondLastUpdateTime.equals(dtFeedReqDate)) {
					psInitializeDBforBondIndexShareMarket10.executeUpdate();
					logger.info("initializeDBforBondIndex: EFP.FI_INDEX --> Previous index price updated");

					psInitializeDBforBondIndexShareMarket11.executeUpdate();
					commitTrans(true);
					logger.info("initializeDBforBondIndex: EFP.FI_INDEX table columns initialized to zero");
				}
				 return true;
			} catch (SQLException e) {
				logger.error("Exception in initializeDBforBondIndex : " + e	+ getStackTrace(e), e);

			}
		logger.debug("Exit  DB2Manager initializeDBforBondIndex function");
		return false;
	}

	// TODO Variable names has to changed
	public boolean EODforBondIndex() {

			try {
				logger.debug("inside  DB2Manager EODforBondIndex function");
				// the below two lines are commented since TIP already provides
				// this data correctly, will be enabled if required
				// psEODShareMarketUpdTeIndex.executeUpdate();
				// psEODShareMarketUpdTeIndexVolumetraded.executeUpdate();
				psEODShareMarketDelHsFiIndex.executeUpdate();
				psEODShareMarketInsFiIndex.executeUpdate();
				logger.info("EODforBondIndex: Done");
				return true;

			} catch (SQLException e) {
				logger.error("Exception in EODforBondIndex: " + e, e);
				
			}
		

		logger.debug("Exit  DB2Manager EODforBondIndex function");
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
			//if (objIndex.isEndOfBatch)
				psUpdBondIndex.executeBatch();
				logger.debug("DBManagerBondIndex Batch Updated");
				lastUpdatedTime = currentUpdatedTime;
				
			}
			g_oConnection.commit();

		} catch (BatchUpdateException be2) {
			logger.error("BatchUpdateException in commitTrans() while doing psUpdBondIndex "+ be2);
			try {
				psUpdBondIndex.clearBatch();
			} catch (SQLException e1) {
				logger.error("Exception in psUpdBondIndex in clearBatch(): " + e1, e1);
			}
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

	

			if (psInitializeDBforBondIndexShareMarket10 != null){
				psInitializeDBforBondIndexShareMarket10.close();
			}
			
			if (psInitializeDBforBondIndexShareMarket11 != null){
				psInitializeDBforBondIndexShareMarket11.close();
			}
			
			if (psUpdBondIndex != null){
				psUpdBondIndex.close();
			}
			
			if (psEODShareMarketDelHsFiIndex != null){
				psEODShareMarketDelHsFiIndex.close();
			}
			
			if (psEODShareMarketInsFiIndex != null){
				psEODShareMarketInsFiIndex.close();
			}

			if (g_oConnection != null){
				g_oConnection.close();
			}
			
			logger.debug("Exit  DB2Manager closeConnection function");
		} catch (SQLException ex) {
			logger.error("SQL Exception in closeConnection() " + ex, ex);
		}
	}


}
