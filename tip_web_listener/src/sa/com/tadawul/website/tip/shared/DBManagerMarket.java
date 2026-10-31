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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import oracle.jdbc.pool.OracleDataSource;

import sa.com.tadawul.website.tip.log.Log;
import sa.com.tadawul.website.tip.log.LogFactory;
import sa.com.tadawul.website.tip.vo.BestBidOffVO;
import sa.com.tadawul.website.tip.vo.TIP_BaseVO;
import sa.com.tadawul.website.tip.vo.TIP_MarketVO;
/**
 * @description This class is used to get Database Connection and all the
 *              prepared Statements.
 *
 */
public class DBManagerMarket extends DBManager {

	private final static Log logger = LogFactory.getLog(DBManagerMarket.class);
	private PreparedStatement psUpdMarketDetail;
	private PreparedStatement psInitMarketDetail;
	private int totalMarketCount = 0;
	private PreparedStatement psBestBidBestOffer;
	private PreparedStatement psresetMarketOrderBook;
	static long  lastUpdatedTime = 0;
	static long currentUpdatedTime = 0;
	public static final int ORDER_BOOK_DEPTH = 10;
	
	public DBManagerMarket(String threadName) throws Exception {
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
			sbPrepareQueries.append("UPDATE EFP.TE_MARKET SET LASTTRADEDDATE = ?, MARKET_STATUS = ?, NO_OF_TRADES = ?, NO_OF_SYMBOLS_TRADED = ?, TURNOVER = ?, "
							+ "VOLUME_TRADED = ?, NO_OF_DOWNS = ?, NO_OF_NO_CHANGES = ?, NO_OF_UPS = ? WHERE PK_EXCHANGE_CODE = ?");

			psUpdMarketDetail = g_oConnection.prepareStatement(sbPrepareQueries	.toString());
			
			/*sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("UPDATE EFP.TE_MARKET SET LASTTRADEDDATE = ?, MARKET_STATUS = ?, NO_OF_TRADES = 0, NO_OF_SYMBOLS_TRADED = 0, TURNOVER = 0, "
							+ "VOLUME_TRADED = 0, NO_OF_DOWNS = 0, NO_OF_NO_CHANGES = 0, NO_OF_UPS = 0 WHERE PK_EXCHANGE_CODE = ?");
			psInitMarketDetail = g_oConnection.prepareStatement(sbPrepareQueries.toString());
			*/
			sbPrepareQueries.delete(0, sbPrepareQueries.length());
			sbPrepareQueries.append("UPDATE EFP.TE_MARKET SET LASTTRADEDDATE = ?, MARKET_STATUS = ?  WHERE PK_EXCHANGE_CODE = ?");
			psInitMarketDetail = g_oConnection.prepareStatement(sbPrepareQueries.toString());
			
			
			logger.debug("Exit DB2Manager prepareStatements function");

		} catch (Exception e) {
			logger.error("Exception while preparing query ["+ sbPrepareQueries.toString() + "] is " + e);
			throw e;
		}
		
		makePresetPrepareStatemntforBBBO();
		
		makebestBidOfferPreapredStatement();
		logger.debug("Exit DB2Manager prepareStatements function");

	}


	protected void initilizeOrderBook(String marketType) {
		try  {
			makePresetPrepareStatemntforBBBO();
			psresetMarketOrderBook.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));     // TRANSACTION_DATE                      
		    psBestBidBestOffer.setString(2, marketType);     // MARKET_TYPE      
		    int affected = psBestBidBestOffer.executeUpdate();
		    System.out.println("Rows affected fro initilizeOrderBook : " + affected+" , for marketType : "+marketType);
		} catch(Exception e) {
			
		}
	}
	
	private void makePresetPrepareStatemntforBBBO() {
		if(null == psresetMarketOrderBook) {
		String resetSql = "UPDATE BEST_BIDS_BEST_ASKS SET " +
                "TRANSACTION_DATE = ?, BID1_PRICE = 0, BID2_PRICE = 0, BID3_PRICE = 0, BID4_PRICE = 0, BID5_PRICE = 0, " +
                "ASK1_PRICE = 0, ASK2_PRICE = 0, ASK3_PRICE = 0, ASK4_PRICE = 0, ASK5_PRICE = 0 " +
                "WHERE MARKET_TYPE = ?";
		
		try {

			psresetMarketOrderBook = g_oConnection.prepareStatement(resetSql);
		} catch(Exception e) {
			logger.error("Exception while preparing query ["+ resetSql.toString() + "] is " + e);
			e.printStackTrace();
			}
		}

	}
	
	String bestBidOfferSql = 
		    "MERGE INTO WEB.BEST_BIDS_BEST_ASKS BBA " +
		    "USING ( " +
		    "  SELECT " +
		    "    ? AS FK_TE_SYMBOL, " +
		    "    ? AS TRANSACTION_DATE, " +
		    generatePlaceholders("BID", ORDER_BOOK_DEPTH) +
		    generatePlaceholders("ASK", ORDER_BOOK_DEPTH) +
		    "    ? AS MARKET_TYPE " +
		    "  FROM DUAL " +
		    ") SRC_BBA " +
		    "ON (BBA.FK_TE_SYMBOL = SRC_BBA.FK_TE_SYMBOL) " +
		    "WHEN MATCHED THEN " +
		    "  UPDATE SET " +
		    "    BBA.TRANSACTION_DATE = SRC_BBA.TRANSACTION_DATE, " +
		    generateUpdateStatements("BID", ORDER_BOOK_DEPTH) +
		    generateUpdateStatements("ASK", ORDER_BOOK_DEPTH) +
		    "    BBA.MARKET_TYPE = SRC_BBA.MARKET_TYPE " +
		    "WHEN NOT MATCHED THEN " +
		    "  INSERT ( " +
		    "    FK_TE_SYMBOL, TRANSACTION_DATE, " +
		    generateInsertColumns("BID", ORDER_BOOK_DEPTH) +
		    generateInsertColumns("ASK", ORDER_BOOK_DEPTH) +
		    "    MARKET_TYPE " +
		    "  ) VALUES ( " +
		    "    SRC_BBA.FK_TE_SYMBOL, SRC_BBA.TRANSACTION_DATE, " +
		    generateInsertValues("BID", ORDER_BOOK_DEPTH) +
		    generateInsertValues("ASK", ORDER_BOOK_DEPTH) +
		    "    SRC_BBA.MARKET_TYPE " +
		    "  )";

	private String generatePlaceholders(String prefix, int count) {
	    StringBuilder sb = new StringBuilder();
	    for (int i = 1; i <= count; i++) {
	        sb.append("    ? AS ").append(prefix).append(i).append("_QTY, \n");
	        sb.append("    ? AS ").append(prefix).append(i).append("_PRICE, \n");
	    }
	    return sb.toString();
	}

	private String generateUpdateStatements(String prefix, int count) {
	    StringBuilder sb = new StringBuilder();
	    for (int i = 1; i <= count; i++) {
	        sb.append("    BBA.").append(prefix).append(i).append("_QTY = SRC_BBA.").append(prefix).append(i).append("_QTY, \n");
	        sb.append("    BBA.").append(prefix).append(i).append("_PRICE = SRC_BBA.").append(prefix).append(i).append("_PRICE, \n");
	    }
	    return sb.toString();
	}

	private String generateInsertColumns(String prefix, int count) {
	    StringBuilder sb = new StringBuilder();
	    for (int i = 1; i <= count; i++) {
	        sb.append(prefix).append(i).append("_QTY, ").append(prefix).append(i).append("_PRICE, ");
	    }
	    return sb.toString();
	}

	private String generateInsertValues(String prefix, int count) {
	    StringBuilder sb = new StringBuilder();
	    for (int i = 1; i <= count; i++) {
	        sb.append("SRC_BBA.").append(prefix).append(i).append("_QTY, ");
	        sb.append("SRC_BBA.").append(prefix).append(i).append("_PRICE, ");
	    }
	    return sb.toString();
	}

	protected void updateBestBidOffer(String symbol, Timestamp lastUpdated, BestBidOffVO[] bestBids, BestBidOffVO[] bestOffers, String marketType) {
	    try {
	    	if (bestBids.length < ORDER_BOOK_DEPTH || bestOffers.length < ORDER_BOOK_DEPTH) {
	    	    throw new IllegalArgumentException("Expected at least "+ORDER_BOOK_DEPTH+" bids and "+ORDER_BOOK_DEPTH+" asks");
	    	}

	        makebestBidOfferPreapredStatement();

	        Arrays.sort(bestBids, (b1, b2) -> Double.compare(b2.price, b1.price));
	       
	        List<BestBidOffVO> filteredSortedList = Arrays.stream(bestOffers)
	        	    .filter(b -> b.price != 0.0)                 
	        	    .sorted(Comparator.comparingDouble(b -> b.price))  
	        	    .collect(Collectors.toList());            
	        BestBidOffVO[] filteredSortedOffers = TIP_BaseVO.getInitailBestBidsOffers();

	        for (int bOPos = 0 ; bOPos < filteredSortedList.size(); bOPos++) {
	            filteredSortedOffers[bOPos] = filteredSortedList.get(bOPos);
	        }

	        
	        psBestBidBestOffer.setString(1, symbol);           // FK_TE_SYMBOL
	        psBestBidBestOffer.setTimestamp(2, lastUpdated);   // TRANSACTION_DATE

	        int paramIndex = 3;

	        // Set BID1_QTY, BID1_PRICE ... BID20_QTY, BID20_PRICE [based on ORDER_BOOK_DEPTH]
	        for (int i = 0; i < ORDER_BOOK_DEPTH && i < bestBids.length; i++) {
	            psBestBidBestOffer.setLong(paramIndex++, bestBids[i].quantity);
	            psBestBidBestOffer.setDouble(paramIndex++, bestBids[i].price);
	        }

	        // Set ASK1_QTY, ASK1_PRICE ... ASK20_QTY, ASK20_PRICE [based on ORDER_BOOK_DEPTH]
	        for (int i = 0; i < ORDER_BOOK_DEPTH && i < filteredSortedOffers.length; i++) {
	            psBestBidBestOffer.setLong(paramIndex++, filteredSortedOffers[i].quantity);
	            psBestBidBestOffer.setDouble(paramIndex++, filteredSortedOffers[i].price);
	        }

	        // Set MARKET_TYPE
	        psBestBidBestOffer.setString(paramIndex++, marketType);

	        psBestBidBestOffer.addBatch();

	        currentUpdatedTime = System.currentTimeMillis();

	        if ((currentUpdatedTime - lastUpdatedTime) > TipConstants.UPDATER_THREADS_DB_UPDATE_INTERVAL) {
	            psBestBidBestOffer.executeBatch();
	            lastUpdatedTime = currentUpdatedTime;
	        }

	    } catch (Exception e) {
	        logger.error("OBk BBBO Exception during updateBestBidOffer DB update ", e);
	        e.printStackTrace();
	    }
	}

	private void makebestBidOfferPreapredStatement() {
		if(null == psBestBidBestOffer ) {
			try {
				logger.info("bestBidOfferSql  SQL :"+bestBidOfferSql.toString());
				psBestBidBestOffer = g_oConnection.prepareStatement(bestBidOfferSql.toString());
			} catch (Exception e) {
				logger.error("Exception while preparing query [" + bestBidOfferSql.toString() + "] is " + e);
				e.printStackTrace();
			}
	    }
    }
	
	
	/**
	 * This method will do the commit.
	 * 
	 * @param null
	 */
	public void commitTrans(boolean isCommit) {
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

			if (psUpdMarketDetail != null){
				psUpdMarketDetail.close();
			}
			if (psInitMarketDetail != null){
				psInitMarketDetail.close();
			}
			if (psBestBidBestOffer != null){
				psBestBidBestOffer.close();
			}
			if (psresetMarketOrderBook != null){
				psresetMarketOrderBook.close();
			}
			
			if (g_oConnection != null)
				g_oConnection.close();
			logger.debug("Exit  DB2Manager closeConnection function");
		} catch (SQLException ex) {
			logger.error("SQL Exception in closeConnection() " + ex, ex);
		}
	}

	/**
	 * This method will updates the market details into the table EFP.TE_MARKET
	 * 
	 * @param objMarket
	 *            contains attributes of EFP.TE_MARKET table
	 * @return boolean
	 */
	public boolean updateMarketInfoData(TIP_MarketVO objMarket) {

			try {


				psUpdMarketDetail.setTimestamp(1, objMarket.lasttradeddate);
				psUpdMarketDetail.setInt(2, objMarket.market_status);
				psUpdMarketDetail.setInt(3, objMarket.no_of_trades);
				psUpdMarketDetail.setDouble(4, objMarket.no_of_symbols_traded);
				psUpdMarketDetail.setDouble(5, objMarket.turnover);
				psUpdMarketDetail.setLong(6, objMarket.volume_traded);
				psUpdMarketDetail.setInt(7, objMarket.no_of_downs);
				psUpdMarketDetail.setInt(8, objMarket.no_of_no_changes);
				psUpdMarketDetail.setInt(9, objMarket.no_of_ups);
				psUpdMarketDetail.setString(10, objMarket.Pk_exchange_code);
				
				//if(objMarket.no_of_trades > 0)
					psUpdMarketDetail.executeUpdate();
				
				totalMarketCount++;
				
				logger.debug("DBManagerMarket: Total Market Updates :"+totalMarketCount);
				
				return true;
				
			} catch (SQLException e) {
				logger.error("Exception in updateMarketInfoData: " + e, e);
				
			}

		if (objMarket != null)
			objMarket = null;
		return false;
	}
	
	
	/**
	 * This method will updates the market details into the table EFP.TE_MARKET
	 * 
	 * @param objMarket
	 *            contains attributes of EFP.TE_MARKET table
	 * @return boolean
	 */
	public boolean initMarketStateData(TIP_MarketVO objMarket) {

			logger.info("Entry initMarketStateData  for Market :"+objMarket.Pk_exchange_code);
			
			try {


				psInitMarketDetail.setTimestamp(1, objMarket.lasttradeddate);
				psInitMarketDetail.setInt(2, TipConstants.TIP_DB_STATE_PREOPENED);
				psInitMarketDetail.setString(3, objMarket.Pk_exchange_code);
				
				psInitMarketDetail.executeUpdate();
				totalMarketCount++;
				
				
				logger.debug("DBManagerMarket: Total Market Updates :"+totalMarketCount);
				
				logger.info("Exit initMarketInfoData  for Market :"+objMarket.Pk_exchange_code);
				
				return true;
				
			} catch (SQLException e) {
				logger.error("Exception in updateMarketInfoData: " + e, e);
				
			}

		if (objMarket != null)
			objMarket = null;
		return false;
	}
	
	
	public boolean initMarketInfoData(TIP_MarketVO objMarket) {

		try {


			psUpdMarketDetail.setTimestamp(1, objMarket.lasttradeddate);
			psUpdMarketDetail.setInt(2, objMarket.market_status);
			psUpdMarketDetail.setInt(3, objMarket.no_of_trades);
			psUpdMarketDetail.setDouble(4, objMarket.no_of_symbols_traded);
			psUpdMarketDetail.setDouble(5, objMarket.turnover);
			psUpdMarketDetail.setLong(6, objMarket.volume_traded);
			psUpdMarketDetail.setInt(7, objMarket.no_of_downs);
			psUpdMarketDetail.setInt(8, objMarket.no_of_no_changes);
			psUpdMarketDetail.setInt(9, objMarket.no_of_ups);
			psUpdMarketDetail.setString(10, objMarket.Pk_exchange_code);			
		
				psUpdMarketDetail.executeUpdate();
			
			totalMarketCount++;
			
			logger.debug("DBManagerMarket: Total Market Updates :"+totalMarketCount);
			
			return true;
			
		} catch (SQLException e) {
			logger.error("Exception in updateMarketInfoData: " + e, e);
			
		}

	if (objMarket != null)
		objMarket = null;
	return false;
}
	
	
}
