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
 * Name          :     FileName: DBWriter.java
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 10/April/2009            Oracle                   Initial Version
 * 1.1				   14/November/2009         Mohanram,Oracle          Added functionalities to process the Equity Rights Share
 * 1.2				   02/February/2010         Mohanram,Oracle          Added functionalities to process the Exchange Traded Funds.
 *----------------------------------------------------------------------------------------------
 */
package sa.com.tadawul.website.tip.DBThreads;


/**
 * @description The DBWriter Thread creates a single instance each for
 *              IndexIntra, ShareIntra, BondIntra. This class handles the BOD
 *              logic for indexintra, shareintra, bondintra. It retrieves the
 *              specific VO from queue and inserts into respective tables. The
 *              thread will continue running until there are messages in queue,
 *              after which if the shutdownFlag is set to true will stop
 *              running. The DBWriter Thread will also write the last sequence
 *              number committed to database to a separate log file.
 *
 */

public class DBWriter extends Thread {

	//MessageDatabaseQueue g_oBuffer;

	String threadname;

	
/*	public DBWriter(MessageDatabaseQueue buffer, String ThreadName)	throws Exception {
	       //g_oBuffer = buffer;
	        threadname = ThreadName;
	        setName(threadname);

	}*/

	
	public DBWriter(String ThreadName)throws Exception {
	      // g_oBuffer = buffer;
	        threadname = ThreadName;
	        setName(threadname);

	}

}
