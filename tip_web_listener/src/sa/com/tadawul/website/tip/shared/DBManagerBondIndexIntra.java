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
public class DBManagerBondIndexIntra extends DBManager {

	private final static Log logger = LogFactory.getLog(DBManagerBondIndexIntra.class);
	private PreparedStatement psDeleteBondIndexIntrabyInterval;
	private PreparedStatement psDeleteBondIndexIntraComplete;
	private PreparedStatement psInsBondIndexIntra;	
	static String strLastUpdDateForShareIndex;
	
	static long  lastUpdatedTime = 0;
	static long currentUpdatedTime = 0;
	private int totalBondIndexIntraCount = 0;
	
	public DBManagerBondIndexIntra(String threadName) throws Exception {
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
			sbPrepareQueries
					.append("DELETE FROM WEB.HS_FI_INDEX_INTRA where TRANS_TIME < to_date(? , 'YYYY-MM-DD HH24:mi:SS')");
			psDeleteBondIndexIntrabyInterval = g_oConnection
					.prepareStatement(sbPrepareQueries.toString());

			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("DELETE FROM WEB.HS_FI_INDEX_INTRA");
			psDeleteBondIndexIntraComplete = g_oConnection
					.prepareStatement(sbPrepareQueries.toString());

			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries
					.append("INSERT INTO WEB.HS_FI_INDEX_INTRA(FK_TE_SYMBOL,DESCRIPTION_EN,"
							+ "DESCRIPTION_AR,INDEX_PRICE,VOLUME_TRADED,TURNOVER,NET_CHANGE,OPEN_PRICE,"
							+ "HIGH_PRICE,LOW_PRICE,TRANS_TIME,PREVIOUS_INDEX_PRICE,PERCENT_CHANGE,NO_OF_TRADES)"
							+ " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

			psInsBondIndexIntra = g_oConnection
					.prepareStatement(sbPrepareQueries.toString());
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
			if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.WRITER_THREADS_DB_UPDATE_INTERVAL)
			{
			//if (objIndex.isEndOfBatch)
				psInsBondIndexIntra.executeBatch();
				logger.debug("DBManageBondrIndexIntra Batch Updated");
				lastUpdatedTime = currentUpdatedTime;
				
			}
			
			g_oConnection.commit();

		} catch (SQLException ex) {
			logger.error("SQL Exception in commitTrans() " + ex, ex);
		} finally {

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
	
			if (psDeleteBondIndexIntrabyInterval != null){
				psDeleteBondIndexIntrabyInterval.close();
			}

			if (psDeleteBondIndexIntraComplete != null){
				psDeleteBondIndexIntraComplete.close();
			}
			
			if (psInsBondIndexIntra != null){
				psInsBondIndexIntra.close();
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
	 * This method will insert recored in to the table WEB.HS_TE_INDEX_INTRA
	 * 
	 * @param indexVO
	 *            contains attributes of WEB.HS_TE_INDEX_INTRA table
	 * @return boolean
	 */
	// TODO Variables has to be changed
	public boolean insertBondIndexIntraData(TIP_BondIndexVO indexVO,boolean isCommit) {
		// logger.debug("Inside insertBondIndexIntraData!!!!TIP_BondIndexVO"+indexVO);

		String description_ar = indexVO.Description_AR;

			try {

				psInsBondIndexIntra.setString(1, indexVO.Fk_te_symbol);
				psInsBondIndexIntra.setString(2, indexVO.Description_EN);
				// psInsBondIndexIntra.setString(3, indexVO.Description_AR);
				if (null != description_ar && description_ar.length() > 0) {
					// description_ar= ;
					try {
						bbuf = encoder.encode(CharBuffer.wrap(description_ar));
						cbuf = decoder.decode(bbuf);
						description_ar = cbuf.toString();
					} catch (CharacterCodingException e) {
						logger.error(	"Exception in insertBondIndexIntraData in cov: "+ e, e);
					}

				} else
					description_ar = indexVO.Description_EN;
				// psInsBondIndexIntra.setString(3, indexVO.Description_EN);
				psInsBondIndexIntra.setString(3, description_ar);
				psInsBondIndexIntra.setDouble(4, indexVO.index_price);
				psInsBondIndexIntra.setLong(5, indexVO.volume_trade);
				psInsBondIndexIntra.setDouble(6, indexVO.turnover);
				psInsBondIndexIntra.setDouble(7, indexVO.net_change);
				psInsBondIndexIntra.setDouble(8, indexVO.open_price);
				psInsBondIndexIntra.setDouble(9, indexVO.high_price);
				psInsBondIndexIntra.setDouble(10, indexVO.low_price);
				psInsBondIndexIntra.setTimestamp(11, indexVO.trans_time);
				psInsBondIndexIntra.setDouble(12, indexVO.previous_index_price);
				psInsBondIndexIntra.setDouble(13, indexVO.percent_change);
				psInsBondIndexIntra.setLong(14, indexVO.no_of_trades);

				
				psInsBondIndexIntra.addBatch();
				
				currentUpdatedTime =  System.currentTimeMillis();
				if(isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.WRITER_THREADS_DB_UPDATE_INTERVAL )
				{
				//if (objIndex.isEndOfBatch)
					psInsBondIndexIntra.executeBatch();
					logger.debug("DBManageBondrIndexIntra Batch Updated");
					lastUpdatedTime = currentUpdatedTime;
					
				}

				totalBondIndexIntraCount++;
	
				logger.debug("DBManagerBondIndexIntra: updateBondIndexIntraData :"+totalBondIndexIntraCount);
				return true;
			}catch(BatchUpdateException e) 
			{
				logger.error("Exception in psInsBondIndexIntra in BatchUpdateException(): " + e, e);
				try {
					psInsBondIndexIntra.clearBatch();
				} catch (SQLException e1) {
					logger.error("Exception in psInsBondIndexIntra in clearBatch(): " + e1, e1);
				}
			}
			catch (SQLException e) {				
				logger.error("Exception in insertBondIndexIntraData: " + e, e);

			}


		if (indexVO != null)
			indexVO = null;
		return false;
	}

	/**
	 * This method will initialize BondIndexIntra table at BOD
	 * 
	 * @return boolean
	 */
	// TODO --Have to change variables
	public boolean initializeDBforBondIndexIntra() {

		// logger.debug("Inside initializeDBforBondIndexIntra");

			try {
				logger.debug("inside  DB2Manager initializeDBforBondIndexIntra function");

				if (!LoadInitialData.dtBondLastUpdateTime.equals(dtFeedReqDate)) {
					logger.debug("Inside initializeDBforBondIndexIntra*****Inside iF");
					int numRowsDeleted = 0;
					/*int totMinutes = (TipConstants.DELETE_END_HOUR - TipConstants.DELETE_START_HOUR) * 60;
					int numRowsDeleted = 0, dltHr = TipConstants.DELETE_START_HOUR, dltMin = 0;
					String beginTime = "";
					String dateTime = LoadInitialData.strLastUpdDateForShareIndex + " ";
					logger.debug("initializeDBforIndexIntra: dtShareIndexLastUpdateTime "	+ LoadInitialData.dtBondLastUpdateTime	+ " DELETE_START_HOUR "+ TipConstants.DELETE_START_HOUR
							+ " DELETE_END_HOUR "+ TipConstants.DELETE_END_HOUR	+ " DELETE_INTERVAL_MIN "	+ TipConstants.DELETE_INTERVAL_MIN);
					logger.debug("initializeDBforIndexIntra: totalMinutes "	+ totMinutes);
					for (int i = 0; i <= totMinutes; i += TipConstants.DELETE_INTERVAL_MIN) {
						if (dltMin >= 10)
							beginTime = dltHr + ":" + dltMin + ":00";
						else
							beginTime = dltHr + ":0" + dltMin + ":00";
						logger.debug("initializeDBforIndexIntra: dateTime+beginTime="	+ dateTime + beginTime);
						psDeleteBondIndexIntrabyInterval.setString(1, dateTime	+ beginTime);
						numRowsDeleted = numRowsDeleted	+ psDeleteBondIndexIntrabyInterval.executeUpdate();
						logger.debug("Inside initializeDBforBondIndexIntra*****psDeleteBondIndexIntrabyInterval Inside For Eexuted Deleted Rows::"	+ numRowsDeleted);
						commitTrans();
						dltMin += TipConstants.DELETE_INTERVAL_MIN;
						if (dltMin >= 60) {
							dltMin -= 60;
							dltHr++;
						}
						logger.debug("Inside for loop --> initializeDBforBondIndexIntra: deleted upto dateTime+beginTime "	+ dateTime	+ beginTime	+ " rows deleted "	+ numRowsDeleted);
					}*/
					numRowsDeleted = numRowsDeleted+ psDeleteBondIndexIntraComplete.executeUpdate();
					commitIntraTrans();
					logger.debug("Outside for loop --> initializeDBforBondIndexIntra: general delete  rows deleted "+ numRowsDeleted);
					logger.info("initializeDBforBondIndexIntra: Records deleted from WEB.HS_TE_INDEX_INTRA");
				}

				return true;
			} catch (SQLException e) {
				logger.error("Exception in initializeDBforBondIndexIntra : "	+ e + getStackTrace(e), e);
			}


		logger.debug("Exit  DB2Manager initializeDBforBondIndexIntra function");
		return false;
	}

}
