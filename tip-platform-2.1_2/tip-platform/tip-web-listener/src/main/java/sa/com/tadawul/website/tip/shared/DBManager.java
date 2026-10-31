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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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
	
	private static final Logger logger = LogManager.getLogger(DBManager.class);
	
	
	// Added for SME Project: Arabic re-encoding (Cp1252 bytes -> Windows-1256 text).
	// CharsetEncoder/CharsetDecoder are NOT thread-safe. They used to be static and shared by the
	// IndexIntra, SMEIndexIntra and BondIndexIntra writer threads, which could corrupt Arabic
	// descriptions or throw under load. Each DBManager is owned by exactly one thread, so instance
	// fields give every thread its own coder while keeping the subclass code unchanged.
	static final Charset encoderCharset = charsetOrNull(TipConstants.SOURCE_ENCODING);
	static final Charset decoderCharset = charsetOrNull(TipConstants.DEST_ENCODING);
	final CharsetDecoder decoder = decoderCharset == null ? null : decoderCharset.newDecoder();
	final CharsetEncoder encoder = encoderCharset == null ? null : encoderCharset.newEncoder();

	/**
	 * Legacy Arabic fix-up: the JNI library delivered Windows-1256 bytes as if they were ISO-8859-1
	 * characters, so the text is turned back into bytes with Cp1252 and decoded as Windows-1256.
	 * Applied only while the feed is decoded in JNI-compatible mode (FEED_CHARSET=JNI, the default);
	 * with a real feed charset the text is already correct and is returned unchanged.
	 */
	protected String reencodeArabic(String text) throws java.nio.charset.CharacterCodingException {
		if (!TipConstants.ARABIC_REENCODE || text == null || encoder == null || decoder == null) {
			return text;
		}
		ByteBuffer bytes = encoder.encode(CharBuffer.wrap(text));
		return decoder.decode(bytes).toString();
	}

	private static Charset charsetOrNull(String name) {
		try {
			return Charset.forName(name);
		} catch (Exception e) {
			logger.error("Charset " + name + " is not available in this JVM (is the jdk.charsets module present?): " + e.getMessage());
			return null;
		}
	}

	/**
	 * Opens the connection and stores it in {@link #g_oConnection}.
	 * <p>
	 * Prepared statements are NOT created here any more. The old constructor called the
	 * overridable {@code prepareStatements()} and then every subclass constructor called it a
	 * second time, so each manager leaked a full set of open cursors on the server. Subclass
	 * constructors now call {@code prepareStatements()} exactly once.
	 */
	public DBManager(String threadName) throws Exception {
		threadname = threadName;
		dtFeedReqDate = TipConstants.DB_FEED_REQ_DATE.trim();
		if (logger.isDebugEnabled()) {
			if (logger.isDebugEnabled()) logger.debug("DBManager constructor --> dtFeedReqDate = {}", dtFeedReqDate);
		}
		logger.info("DBManager constructor for thread --> {}", threadName);
		openConnection();
	}

	/**
	 * Establishes the connection with the database (DataSource or JDBC, see {@link DBUtil}).
	 * Was duplicated verbatim in all 14 subclasses.
	 */
	public void openConnection() throws Exception {
		try {
			g_oConnection = DBUtil.openConnection();
			g_bClassLoaded = true;
		} catch (Exception e) {
			logger.error("Error in DBManager.openConnection for thread " + threadname + ": " + e, e);
			throw e;
		}
	}

	/**
	 * Commits only when the connection is in manual-commit mode.
	 * <p>
	 * The connections run with the JDBC default auto-commit=true, so every {@code executeBatch}
	 * is already committed. With the default {@code oracle.jdbc.autoCommitSpecCompliant=true},
	 * ojdbc throws "Could not commit with auto-commit set on" from an explicit commit(), which the
	 * old code caught and logged with a full stack trace on every flush (each thread, every 10 s).
	 * Transaction semantics are unchanged.
	 */
	protected void commitIfNeeded() throws java.sql.SQLException {
		if (g_oConnection != null && !g_oConnection.getAutoCommit()) {
			g_oConnection.commit();
		}
	}

	
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
