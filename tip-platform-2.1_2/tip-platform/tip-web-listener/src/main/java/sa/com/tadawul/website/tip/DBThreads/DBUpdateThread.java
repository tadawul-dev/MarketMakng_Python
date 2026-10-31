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
 * Name          :     FileName :  DBUpdateThread.java
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 10/April/2009            Oracle                   Initial Version
 * 1.1				   14/November/2009         Mohanram,Oracle          Added functionalities to process the Equity Rights Share
 * 1.2				   02/February/2010         Mohanram,Oracle          Added functionalities to process the Exchange Traded Funds.
 *----------------------------------------------------------------------------------------------
 */
package sa.com.tadawul.website.tip.DBThreads;

import sa.com.tadawul.website.tip.queue.MessageDatabaseQueue;

/**
 * @description The DBUpdateThread creates a single instance each for Index, Share, Bond and Market.
 *              This class handles the BOD/EOD logic for index, share, bond. It retrieves the specific VO
 *              from queue and updates the respective tables. The thread will continue running until there
 *              are messages in queue, after which if the shutdownFlag is set to true will stop running.
 *              The DBUpdateThread will also write the last sequence number committed to database
 *              to a separate log file.
 *
 */

public class DBUpdateThread implements Runnable {

	/** Thread name; the worker thread is created and named by DBQueueUpdater via TipThreads. */
	protected String threadname;
	protected long seqNum;

	public DBUpdateThread(MessageDatabaseQueue buffer, String ThreadName) throws Exception {
		this(ThreadName);
	}

	public DBUpdateThread(String ThreadName) throws Exception {
		threadname = ThreadName;
	}

	public String getName() {
		return threadname;
	}

	@Override
	public void run() {
		// overridden by every updater
	}
}
