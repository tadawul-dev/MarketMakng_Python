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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.sql.Connection;

import sa.com.tadawul.website.tip.log.Log;
import sa.com.tadawul.website.tip.log.LogFactory;

/**
 * @description This class is used to get Database Connection and all the
 *              prepared Statements.
 *
 */
public abstract class DBManager {

	/**
	 * Common Variables -- should be moved to Interface
	 */
	public Connection g_oConnection;
	public boolean g_bClassLoaded;	
	public static String dtFeedReqDate;
	public static String dtShareIndexLastUpdateTime;
	public static String dtBondLastUpdateTime;
	public static String dtSMEShareIndexLastUpdateTime;
	public String threadname;
	private int retryCount = 0;
	private final int DB_CONNECTION_MAX_RETRY_COUNT = TipConstants.DB_CONNECTION_MAX_RETRY_COUNT;
	boolean dbflag = false;
	public static String strLastUpdDateForSMEShareIndex;
	public static String strLastUpdDateForBond;
	public static String strLastUpdDateForShareIndex;
	
	/**
	 * 
	 */
	
	private final static Log logger = LogFactory.getLog(DBManager.class);
	
	
	// Added for SME Project
	static Charset encoderCharset = null;
	static Charset decoderCharset = null;
	static CharsetDecoder decoder = null;
	static CharsetEncoder encoder = null;
	static ByteBuffer bbuf = null;
	static CharBuffer cbuf = null;

	static {
		try {
			// encoderCharset = Charset.forName("Cp1252");
			// decoderCharset = Charset.forName("Windows-1256");
			// Coding for parsing Arabic Data
			encoderCharset = Charset.forName(TipConstants.SOURCE_ENCODING);
			decoderCharset = Charset.forName(TipConstants.DEST_ENCODING);
			decoder = decoderCharset.newDecoder();
			encoder = encoderCharset.newEncoder();

		} catch (Exception e) {
			logger.info("Inside static block of DataParserForLowVolm Java File : Error : "
					+ e.getMessage());
		}
	}

	public DBManager(String threadName) throws Exception {
		threadname = threadName;
		logger.debug("DB2Manager Constructor --> DB_FEED_REQ_DATE = "
				+ TipConstants.DB_FEED_REQ_DATE.trim());
		dtFeedReqDate = TipConstants.DB_FEED_REQ_DATE.trim();
		logger.debug("DB2Manager Constructor --> dtFeedReqDate = "
				+ dtFeedReqDate);

		logger.info("DB2Manager Constructor for Thread-->  = " + threadName);
		openConnection();
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
	public abstract void openConnection() throws Exception;

	
	/**
	 * This method will Initialize all the Prepared Statements respective to the
	 * available Threads
	 *
	 */
	public abstract void prepareStatements() throws Exception;
	
	/**
	 * This method will reconnect to DB if disconnected until max tries reached
	 * 
	 * @param null
	 * @return dbflag (boolean)
	 */
	public boolean isConnectionOpen() {
		dbflag = false;
		try {
			for (retryCount = 0; retryCount < DB_CONNECTION_MAX_RETRY_COUNT;) {
				if (g_oConnection != null && !g_oConnection.isClosed()) {
					dbflag = true;
					break;
				} else {
					if (retryCount >= DB_CONNECTION_MAX_RETRY_COUNT) {
						logger.error("Error DB reconnect Exceeded");
						break;
					}
					openConnection();
					retryCount++;
				}
			}

			return dbflag;
		} catch (Exception e) {
			return false;
		}
	}

	
	/**
	 * This method will do the commit.
	 * 
	 * @param null
	 */
	public abstract void commitTrans(boolean iscommit);

	/**
	 * This method will close the existing DB Connection
	 */
	public abstract void closeConnection();


	public String getStackTrace(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		return sw.toString();
	}

}
