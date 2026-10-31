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
 * Name          :     FileName : AnalyserReader.java
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 10/April/2009            Oracle                   Initial Version
 * 1.1				   14/November/2009         Mohanram,Oracle          Added functionalities to process the Equity Rights Share
 * 1.2				   02/February/2010         Mohanram,Oracle          Added functionalities to process the Exchange Traded Funds.
 * 2.0                 31/October/2026          Modernization            Java 25: buffered feed log with explicit charset,
 *                                                                        named worker threads, no regex/contains on hot path.
 * 2.1                 31/October/2026          Modernization            libtipclient.so/JNI replaced by the pure-Java
 *                                                                        TIP client (tip-client module); Log4j 2 API.
 *----------------------------------------------------------------------------------------------
 */
package sa.com.tadawul.applications.tip.isi.efp.analyser;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sa.com.tadawul.website.tip.DBThreads.DBQueueUpdater;
import sa.com.tadawul.website.tip.dataanalyser.DataReader;
import sa.com.tadawul.website.tip.queue.MessageDatabaseQueue;
import sa.com.tadawul.website.tip.queue.MessageQueue;
import sa.com.tadawul.website.tip.shared.Settings;
import sa.com.tadawul.website.tip.shared.TIPDataObjects;
import sa.com.tadawul.website.tip.shared.TipConstants;
import sa.com.tadawul.website.tip.shared.TipThreads;
import sa.com.tadawul.website.tip.shared.Utility;
import tip.xm.XmService;

/**
 * Entry point of the TIP Web Listener.
 * <p>
 * Starts the dispatcher and parser threads, the DB threads, then reads the TIP feed on the main
 * thread with {@link TipFeedReader} (pure Java - the JNI library {@code libtipclient.so} and
 * XMP_LIBRARY are no longer used).
 * <p>
 * Shutdown: SIGTERM / SIGINT / SIGHUP run a shutdown hook that logs off from TIP, lets the DB
 * queues drain for up to SHUTDOWN_DRAIN_SECS and gives the writers time to commit.
 */
public final class AnalyserReader {

    private static final Logger logger = LogManager.getLogger(AnalyserReader.class);

    static {
        if (Settings.g_oProperties == null)
            Settings.Load();
    }

    private static volatile boolean shutdownHookRunning;

    private AnalyserReader() {
    }

    public static void main(String[] args) {
        logger.info("Starting TIP Web Listener 2.1 on Java {} ({})", Runtime.version(), System.getProperty("java.vm.name"));

        FeedTextDecoder decoder = FeedTextDecoder.of(TipConstants.FEED_CHARSET);
        TipConstants.ARABIC_REENCODE = decoder.needsLegacyArabicReencode();
        logger.info("Feed text decoding: {} (legacy Arabic re-encoding {})", decoder.name(), TipConstants.ARABIC_REENCODE ? "on" : "off");

        TipFeedReader reader = new TipFeedReader(configFromSettings(decoder),
                MessageQueue.g_oBuffer::setFrame,
                () -> TIPDataObjects.last_Sequencer_Analyzer);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(reader), "tip-shutdown"));

        TipThreads.startWorker("DataReader", new DataReader());
        pause(5000); // let the dispatcher load reference data and start the parsers
        TipThreads.startWorker("DBQueueUpdater", new DBQueueUpdater());
        pause(5000); // let the DB threads open their connections

        reader.run();

        if (!shutdownHookRunning) {
            pause(5000); // give the other threads time to drain and stop
            System.exit(0);
        }
    }

    /** Builds the reader configuration from efp.ini (TipConstants). */
    static TipFeedReader.Config configFromSettings(FeedTextDecoder decoder) {
        List<XmService.Address> addresses = new ArrayList<>(2);
        addresses.add(new XmService.Address(TipConstants.TARGIN_PRIMARY_HOST.trim(), TipConstants.TARGIN_PRIMARY_PORT));
        if (TipConstants.TARGIN_SECONDARY_HOST != null && !TipConstants.TARGIN_SECONDARY_HOST.isBlank()
                && TipConstants.TARGIN_SECONDARY_PORT > 0) {
            addresses.add(new XmService.Address(TipConstants.TARGIN_SECONDARY_HOST.trim(), TipConstants.TARGIN_SECONDARY_PORT));
        }
        int date = (int) TipConstants.TARGIN_FEED_REQ_DATE;
        String feedLog = Utility.logFileCreation("tip_" + date + "_r" + TipConstants.TIP_REAL_TIME_REQUEST
                + "_s" + TipConstants.TIP_SCOPE_ALL + ".txt");
        return new TipFeedReader.Config(
                TipConstants.TARGIN_USER_NAME,
                TipConstants.TARGIN_PASSWORD,
                TipConstants.TARGIN_PROTOCOL_VERSION,
                addresses,
                date,
                TipConstants.TARGIN_CONNECT_MAX_RETRY_COUNT,
                TipConstants.TIP_COMPRESSION,
                Duration.ofSeconds(TipConstants.TIP_CONNECT_TIMEOUT_SECS),
                TipConstants.TIP_RECV_TIMEOUT_SECS > 0 ? Duration.ofSeconds(TipConstants.TIP_RECV_TIMEOUT_SECS) : null,
                TipConstants.TIP_QUEUE_ALERT_MS,
                decoder,
                TipConstants.LOG_FEED == 1,
                TipConstants.FILE_BASED_RECOVERY == 1,
                feedLog,
                TipConstants.FEED_LOG_FLUSH_INTERVAL_MS,
                TipConstants.WEBSITE_DISPLAY_DELAY_TIME,
                Duration.ofSeconds(1));
    }

    /** Shutdown hook: log off, let the queues drain, let the DB writers commit. */
    private static void shutdown(TipFeedReader reader) {
        shutdownHookRunning = true;
        logger.info("Shutdown signal received - logging off from TIP");
        reader.requestStop(Duration.ofSeconds(15));
        TipConstants.shutdownFlag = true;

        long deadline = System.nanoTime() + Duration.ofSeconds(TipConstants.SHUTDOWN_DRAIN_SECS).toNanos();
        int pending;
        while ((pending = pendingMessages()) > 0 && System.nanoTime() < deadline) {
            Utility.sleepAnalyser(200);
        }
        if (pending > 0) {
            logger.warn("Shutting down with {} messages still queued", pending);
        }
        Utility.sleepAnalyser(1500); // DB threads commit and write their sequence files when idle
        logger.info("TIP Web Listener stopped");
        LogManager.shutdown();
    }

    static int pendingMessages() {
        return MessageQueue.totalPending() + MessageDatabaseQueue.totalPending();
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Thread is interrupted : {}", e.getMessage());
        }
    }
}
