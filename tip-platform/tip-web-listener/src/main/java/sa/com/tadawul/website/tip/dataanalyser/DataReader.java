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
 * Name          :     FileName : DataReader.java
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 15 - April  2018           Mohanram                 Initial Version
 *----------------------------------------------------------------------------------------------
 */

package sa.com.tadawul.website.tip.dataanalyser;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import sa.com.tadawul.website.tip.messages.MessageType;
import sa.com.tadawul.website.tip.queue.FrameHolder;
import sa.com.tadawul.website.tip.queue.MessageQueue;
import sa.com.tadawul.website.tip.shared.LoadInitialData;
import sa.com.tadawul.website.tip.shared.TIPDataObjects;
import sa.com.tadawul.website.tip.shared.TipConstants;
import sa.com.tadawul.website.tip.shared.TipThreads;
import sa.com.tadawul.website.tip.shared.Utility;
import sa.com.tadawul.website.tip.vo.TIP_MarketVO;

/**
 * Dispatcher: takes raw frames from the reader and routes each one to the queue of the parser
 * thread responsible for its message type.
 * <p>
 * Modernization notes:
 * <ul>
 *   <li>The legacy loop had no sleep when the buffer was empty and spun one core at 100% CPU for
 *       the whole session. It now blocks on the buffer and wakes as soon as a frame arrives.</li>
 *   <li>The message type is cut out with {@code indexOf} instead of {@code split(";", 2)}
 *       (no ArrayList/array allocation per message).</li>
 *   <li>Parser threads are named platform threads instead of an unnamed fixed pool.</li>
 * </ul>
 */
public class DataReader implements Runnable {

	private static final Logger logger = LogManager.getLogger(DataReader.class);

	public static long seqno = 1;

	private long noOfIndexUpdate;
	private long noOfTurnOverlistUpdate;
	private long noOfTradeUpdate;
	private long noOfTradeStatisticsUpdate;
	private long noOfOrderBook;
	private long noOfOrderBookStatus;
	private long noOfIndicativeOpening;
	private long noOfOrderBookSummeries;
	private long noOfLowVolumeMsgs;
	private long totalNoofMesssagesReceived;

	public DataReader() {
	}

	/**
	 * Segregates the TIP messages pushed by AnalyserReader into per-type queues. The eight
	 * high-volume types get dedicated queues/threads; every other type goes to the low-volume
	 * queue. Frames are released only once WEBSITE_DISPLAY_DELAY_TIME has elapsed.
	 */
	@Override
	public void run() {
		logger.info("DataReader Thread started. ");
		LoadInitialData loadinitData = new LoadInitialData();
		loadinitData.loadHashMap();
		initializeMarketArrays();
		// This must run after the HashMap load completed
		startDataParserThreads();

		FrameHolder frame = null;
		while (!TipConstants.shutdownFlag) {
			try {
				if (frame == null) {
					frame = MessageQueue.g_oBuffer.poll(TipConstants.READER_QUEUE_IDLE_WAIT_MS);
					if (frame == null) {
						continue; // idle: re-check shutdown flag
					}
				}
				// Website display delay: hold the frame until its release time.
				long wait = frame.time() - System.currentTimeMillis();
				if (wait > 0) {
					Utility.sleepAnalyser(wait);
					continue;
				}
				dispatch(frame.buffer());
			} catch (Exception e) {
				logger.error("Exception in run method of DataReader class. " + e.getMessage(), e);
			}
			frame = null;
		}
		logger.info("DataReader stopped. Total messages read: {}", totalNoofMesssagesReceived);
	}

	/** Routes one raw message. Behaviour identical to the legacy switch on split(";", 2). */
	private void dispatch(String msg) {
		final int sep = msg.indexOf(';');
		final String type = sep < 0 ? msg : msg.substring(0, sep);
		final String body = sep < 0 ? null : msg.substring(sep + 1);
		totalNoofMesssagesReceived++;

		switch (MessageType.getMessageValue(type)) {
			case 1 -> {
				noOfTurnOverlistUpdate++;
				MessageQueue.turnoverListUpdateQueue.push(requireBody(type, body, msg));
			}
			case 2 -> {
				noOfIndexUpdate++;
				MessageQueue.indexUpdateQueue.push(requireBody(type, body, msg));
			}
			case 3 -> {
				noOfTradeUpdate++;
				MessageQueue.tRdAndTStQueue.push(msg);
			}
			case 4 -> {
				noOfTradeStatisticsUpdate++;
				MessageQueue.tRdAndTStQueue.push(msg);
			}
			case 5 -> {
				noOfOrderBook++;
				MessageQueue.orderBookQueue.push(requireBody(type, body, msg));
			}
			case 6 -> {
				noOfOrderBookStatus++;
				MessageQueue.orderBookStateQueue.push(requireBody(type, body, msg));
			}
			case 7 -> {
				noOfIndicativeOpening++;
				MessageQueue.indicativeOpeningQueue.push(requireBody(type, body, msg));
			}
			case 8 -> {
				noOfOrderBookSummeries++;
				MessageQueue.tRdAndTStQueue.push(msg);
			}
			default -> {
				noOfLowVolumeMsgs++;
				MessageQueue.lowVlmMsgQueue.push(msg);
			}
		}
	}

	/** The legacy code threw ArrayIndexOutOfBounds for a typed message without ';' and dropped it. */
	private static String requireBody(String type, String body, String msg) {
		if (body == null) {
			throw new IllegalArgumentException("Malformed " + type + " message without fields, dropped: " + msg);
		}
		return body;
	}

	private void startDataParserThreads() {
		TipThreads.startWorker("Parser-LowVolume", new DataParserLowVolm());
		TipThreads.startWorker("Parser-IndexUpdates", new DataParserIndexUpdates());
		TipThreads.startWorker("Parser-TurnOverList", new DataParserTurnOverListUpdates());
		TipThreads.startWorker("Parser-TradesAndTSt", new DataParserTrdAndTSt());
		TipThreads.startWorker("Parser-OrderBook", new DataParserOrderBook());
		TipThreads.startWorker("Parser-OrderBookState", new DataParserOrderBookState());
		TipThreads.startWorker("Parser-IndicativeOpening", new DataParserIndicativeOpenings());
	}

	private void initializeMarketArrays()
    {
		TIPDataObjects.tipMarket[0] = new TIP_MarketVO();
		TIPDataObjects. tipMarket[0].MarketId = TipConstants.SHARE_MARKET_ID;
		TIPDataObjects. tipMarket[0].Pk_exchange_code = TipConstants.SHARE_MARKET_CODE;
     
		TIPDataObjects.tipMarket[1] = new TIP_MarketVO();
		TIPDataObjects.tipMarket[1].MarketId = TipConstants.BOND_MARKET_ID;
		TIPDataObjects.tipMarket[1].Pk_exchange_code = TipConstants.BOND_MARKET_CODE;
     //Added for SME
		TIPDataObjects.tipMarket[2] = new TIP_MarketVO();
		TIPDataObjects.tipMarket[2].MarketId = TipConstants.SME_MARKET_ID;
		TIPDataObjects. tipMarket[2].Pk_exchange_code = TipConstants.SME_MARKET_CODE;
    }
	
}


