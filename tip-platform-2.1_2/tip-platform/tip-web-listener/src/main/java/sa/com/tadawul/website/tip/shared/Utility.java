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
 * Name          :     FileName : Utility.java
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 10/April/2009            Oracle                   Initial Version
 * 1.1				   14/November/2009         Mohanram,Oracle          Added functionalities to process the Equity Rights Share
 * 1.2				   02/February/2010         Mohanram,Oracle          Added functionalities to process the Exchange Traded Funds.	
 * 2.0                 31/October/2026          Modernization            Java 25: lock-free time formatting, regex-free
 *                                                                        tag splitting, java.time for Riyadh clock.
 *----------------------------------------------------------------------------------------------
 */
package sa.com.tadawul.website.tip.shared;

import java.time.LocalTime;
import java.time.ZoneId;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Shared helpers. All methods are stateless and thread-safe.
 */
public final class Utility {

    private static final Logger logger = LogManager.getLogger(Utility.class);

    /** Exchange time zone. Cached: {@code TimeZone.getTimeZone(..)} is synchronized and allocates. */
    public static final ZoneId RIYADH = ZoneId.of("Asia/Riyadh");

    private Utility() {
    }

    /** Absolute path of a file inside LOG_FILE_DIRECTORY. */
    public static String logFileCreation(String logFileName) {
        return TipConstants.LOG_FILE_DIRECTORY + logFileName;
    }

    /**
     * Splits a TIP tag/value field at its first lower-case ASCII letter.
     * <p>
     * Exact, drop-in equivalent of {@code field.split("[a-z]", 2)} (the old
     * {@code TipConstants.SPLITVALUE} regex) that the parsers ran on every field of every message.
     * {@code String.split} with a character class cannot use the JDK single-char fast path, so it
     * compiled a new {@code Pattern} and built a {@code Matcher} per call. This is a plain scan.
     * <ul>
     *   <li>"Tp12.5" -&gt; ["T", "12.5"]</li>
     *   <li>"Si1010" -&gt; ["S", "1010"]</li>
     *   <li>"n42"    -&gt; ["", "42"]</li>
     *   <li>"ABC"    -&gt; ["ABC"]</li>
     * </ul>
     */
    public static String[] splitTagValue(String field) {
        final int len = field.length();
        for (int i = 0; i < len; i++) {
            char c = field.charAt(i);
            if (c >= 'a' && c <= 'z') {
                return new String[] { field.substring(0, i), field.substring(i + 1) };
            }
        }
        return new String[] { field };
    }

    /**
     * Builds "yyyy-MM-dd HH:mm:ss" from DB_FEED_REQ_DATE and a TIP "HHmmss" time.
     * No longer {@code synchronized}: it was a global lock shared by every parser thread.
     */
    public static String appendTxWithToday(String txTime) {
        return TipConstants.DB_FEED_REQ_DATE + formatTime(txTime);
    }

    /** "HHmmss" -&gt; "HH:mm:ss". Throws StringIndexOutOfBoundsException for short input, as before. */
    public static String formatTime(String txTime) {
        return txTime.substring(0, 2) + ':' + txTime.substring(2, 4) + ':' + txTime.substring(4, 6);
    }

    /** "yyyyMMdd" -&gt; "yyyy-MM-dd". */
    public static String formatDate(String feedDate) {
        return feedDate.substring(0, 4) + '-' + feedDate.substring(4, 6) + '-' + feedDate.substring(6, 8);
    }

    public static String GetLevel(String values) {
        return values.substring(0, values.indexOf(':'));
    }

    public static String GetValue(String values) {
        return values.substring(values.indexOf(':') + 1);
    }

    /** Sleeps, preserving the thread's interrupt status (the old version swallowed it). */
    public static void sleepAnalyser(long lInterval) {
        if (lInterval <= 0) {
            return;
        }
        try {
            Thread.sleep(lInterval);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Current wall-clock time at the exchange. */
    public static LocalTime riyadhTime() {
        return LocalTime.now(RIYADH);
    }

    /**
     * Changes for SR2 (extended NegDeals): shares may be initialised only from 09:30 Riyadh time,
     * preventing a pre-open status before then.
     */
    public static boolean shouldUpdatedShares() {
        LocalTime now = riyadhTime();
        int hour = now.getHour();
        int minute = now.getMinute();
        boolean shouldUpdateShares = (hour == 9 && minute >= 30) || hour > 9;
        if (logger.isDebugEnabled()) {
            if (logger.isDebugEnabled()) logger.debug("Time Hour :{} Minute: {}   shouldUpdatedShares status :{}", hour, minute, shouldUpdateShares);
        }
        return shouldUpdateShares;
    }
}
