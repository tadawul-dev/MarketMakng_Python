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
import sa.com.tadawul.website.tip.vo.TIP_SMEIndexVO;

/**
 * @description This class is used to get Database Connection and all the
 *              prepared Statements.
 *
 */
public class DBManagerSMEIndexIntra extends DBManager {

	private final static Log logger = LogFactory.getLog(DBManagerSMEIndexIntra.class);
	private PreparedStatement psDeleteSMEIndexIntrabyInterval;
	private PreparedStatement psDeleteSMEIndexIntraComplete;
	private PreparedStatement psInsSMEIndexIntra;

	static long  lastUpdatedTime = 0;
	static long currentUpdatedTime = 0;
	private int totalSMEIndexIntraCount = 0;
	
	public DBManagerSMEIndexIntra(String threadName) throws Exception {
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

			sbPrepareQueries.append("DELETE FROM WEB.HS_TE_SME_INDEX_INTRA where TRANS_TIME < to_date( ? , 'YYYY-MM-DD HH24:mi:SS') ");
			psDeleteSMEIndexIntrabyInterval = g_oConnection.prepareStatement(sbPrepareQueries.toString());

			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("DELETE FROM WEB.HS_TE_SME_INDEX_INTRA");
			psDeleteSMEIndexIntraComplete = g_oConnection.prepareStatement(sbPrepareQueries.toString());

			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("INSERT INTO WEB.HS_TE_SME_INDEX_INTRA(FK_TE_SYMBOL,DESCRIPTION_EN,"
							+ "DESCRIPTION_AR,INDEX_PRICE,VOLUME_TRADED,TURNOVER,NET_CHANGE,OPEN_PRICE,"
							+ "HIGH_PRICE,LOW_PRICE,TRANS_TIME,PREVIOUS_INDEX_PRICE,PERCENT_CHANGE,NO_OF_TRADES)"
							+ " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

			psInsSMEIndexIntra = g_oConnection.prepareStatement(sbPrepareQueries.toString());
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

			
			
			
			// Added for SME
			try {
				
				currentUpdatedTime =  System.currentTimeMillis();
				if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.WRITER_THREADS_DB_UPDATE_INTERVAL  )
				{
				//if (objIndex.isEndOfBatch)
					psInsSMEIndexIntra.executeBatch();
					logger.info("DBManagerSMEIndexIntra Batch Updated");
					lastUpdatedTime = currentUpdatedTime;
					
				}
				

			} catch (BatchUpdateException be1) {
				logger.error("BatchUpdateException in commitTrans() while doing InsSMEIndexIntra "+ be1);
				try {
					psInsSMEIndexIntra.clearBatch();
				} catch (SQLException e1) {
					logger.error("Exception in psInsSMEIndexIntra in clearBatch(): " + e1, e1);
				}
				/*while (be1.getNextException() != null)
					logger.error("BatchUpdateException in commitTrans() while doing InsSMEIndexIntra "+ be1.getNextException());*/
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
	
			if (psDeleteSMEIndexIntrabyInterval != null){
				psDeleteSMEIndexIntrabyInterval.close();
			}
			
			if (psDeleteSMEIndexIntraComplete != null){
				psDeleteSMEIndexIntraComplete.close();
			}
			
			if (psInsSMEIndexIntra != null){
				psInsSMEIndexIntra.close();
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
	 * This method will initialize IndexIntra table at BOD
	 * 
	 * @return boolean
	 */
	// Added for SME
	public boolean initializeDBforSMEIndexIntra() {

		try {
			logger.debug("inside  DB2Manager initializeDBforSMEIndexIntra function");

			if (!LoadInitialData.dtSMEShareIndexLastUpdateTime.equals(dtFeedReqDate)) {
				int numRowsDeleted = 0;
				/*int totMinutes = (TipConstants.DELETE_END_HOUR - TipConstants.DELETE_START_HOUR) * 60;
				int numRowsDeleted = 0, dltHr = TipConstants.DELETE_START_HOUR, dltMin = 0;
				String beginTime = "";
				String dateTime = LoadInitialData.strLastUpdDateForSMEShareIndex + " ";
				logger.debug("initializeDBforSMEIndexIntra: dtSMEShareIndexLastUpdateTime "	+ LoadInitialData.dtSMEShareIndexLastUpdateTime	+ " DELETE_START_HOUR "+ TipConstants.DELETE_START_HOUR
						+ " DELETE_END_HOUR "+ TipConstants.DELETE_END_HOUR	+ " DELETE_INTERVAL_MIN "	+ TipConstants.DELETE_INTERVAL_MIN);
				logger.debug("initializeDBforSMEIndexIntra: totalMinutes "+ totMinutes);
				for (int i = 0; i <= totMinutes; i += TipConstants.DELETE_INTERVAL_MIN) {
					if (dltMin >= 10)
						beginTime = dltHr + ":" + dltMin + ":00";
					else
						beginTime = dltHr + ":0" + dltMin + ":00";

					psDeleteSMEIndexIntrabyInterval.setString(1, dateTime+ beginTime);
					numRowsDeleted = numRowsDeleted	+ psDeleteSMEIndexIntrabyInterval.executeUpdate();
					commitTrans();
					dltMin += TipConstants.DELETE_INTERVAL_MIN;
					if (dltMin >= 60) {
						dltMin -= 60;
						dltHr++;
					}
					logger.debug("Inside for loop --> initializeDBforSMEIndexIntra: deleted upto dateTime+beginTime "	+ dateTime	+ beginTime		+ " rows deleted "		+ numRowsDeleted);
				}*/
				numRowsDeleted = numRowsDeleted	+ psDeleteSMEIndexIntraComplete.executeUpdate();
				commitIntraTrans();
				logger.debug("Outside for loop --> initializeDBforSMEIndexIntra: general delete  rows deleted "	+ numRowsDeleted);
				logger.info("initializeDBforIndexIntra: Records deleted from WEB.HS_TE_SME_INDEX_INTRA");
			}
			return true;
		} catch (SQLException e) {
			logger.error("Exception in initializeDBforSMEIndexIntra: " + e, e);
		}
		logger.debug("Exit  DB2Manager initializeDBforSMEIndexIntra function");
		return true;
	}

	/**
	 * This method will insert recored in to the table WEB.HS_TE_SME_INDEX_INTRA
	 * 
	 * @param objIndex
	 *            contains attributes of WEB.HS_TE_SME_INDEX_INTRA table
	 * @return boolean
	 */
	// Added for SME
	public boolean insertSMEIndexIntraData(TIP_SMEIndexVO objIndex,boolean isCommit) {

		String description_ar = objIndex.Description_AR;

		try {
			psInsSMEIndexIntra.setString(1, objIndex.Fk_te_symbol);
			psInsSMEIndexIntra.setString(2, objIndex.Description_EN);
			/**
			 * the next line is temporary until get response from Tadawul
			 */
			if (null != description_ar && description_ar.length() > 0) {
				// description_ar= ;
				try {
					bbuf = encoder.encode(CharBuffer.wrap(description_ar));
					cbuf = decoder.decode(bbuf);
					description_ar = cbuf.toString();
				} catch (CharacterCodingException e) {
					logger.error("Exception in insertIndexIntraData in cov: "
							+ e, e);
				}

			} else
				description_ar = objIndex.Description_EN;
			psInsSMEIndexIntra.setString(3, description_ar);
	
			psInsSMEIndexIntra.setDouble(4, objIndex.index_price);
			psInsSMEIndexIntra.setLong(5, objIndex.volume_trade);
			psInsSMEIndexIntra.setDouble(6, objIndex.turnover);
			psInsSMEIndexIntra.setDouble(7, objIndex.net_change);
			psInsSMEIndexIntra.setDouble(8, objIndex.open_price);
			psInsSMEIndexIntra.setDouble(9, objIndex.high_price);
			psInsSMEIndexIntra.setDouble(10, objIndex.low_price);
			psInsSMEIndexIntra.setTimestamp(11, objIndex.trans_time);
			psInsSMEIndexIntra.setDouble(12, objIndex.previous_index_price);
			psInsSMEIndexIntra.setDouble(13, objIndex.percent_change);
			psInsSMEIndexIntra.setLong(14, objIndex.no_of_trades);

			psInsSMEIndexIntra.addBatch();
			
			currentUpdatedTime =  System.currentTimeMillis();
			
			if( isCommit || (currentUpdatedTime-lastUpdatedTime)>TipConstants.UPDATER_THREADS_DB_UPDATE_INTERVAL  )
			{
			//if (objIndex.isEndOfBatch)
				psInsSMEIndexIntra.executeBatch();
				logger.debug("DBManagerSMEIndexIntra Batch Updated");
				lastUpdatedTime = currentUpdatedTime;
				
			}

			totalSMEIndexIntraCount++;
			
			
			logger.debug("DBManagerSMEIndexIntra: updateSMEIndexIntraData :"+totalSMEIndexIntraCount);
		
			return true;

		}catch (BatchUpdateException be2) {
			logger.error("BatchUpdateException in commitTrans() while doing psInsSMEIndexIntra "+ be2);
			try {
				psInsSMEIndexIntra.clearBatch();
			} catch (SQLException e1) {
				logger.error("Exception in psInsSMEIndexIntra in clearBatch(): " + e1, e1);
			}			
		} 
		catch (SQLException e) {
			logger.error("Exception in insertSMEIndexIntraData: " + e, e);			

		}

		if (objIndex != null)
			objIndex = null;
		return false;
	}

}
