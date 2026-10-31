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
 * Name          :     FileName : MessageList.java
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 15 - May 2016           Mohanram                  Initial Version
 *----------------------------------------------------------------------------------------------
 */
package sa.com.tadawul.website.tip.queue;

/**
*  This class is used keep different queue of messages.
*  The queue of messages is passed from DataReader to corresponding data Writer 
*  with the help of object of this class.
*  
* 
*/

public class MessageDatabaseQueue {

		public volatile static Buffer tipIndexIntraQ = new Buffer();
		public volatile static Buffer tipSMEIndexIntraQ = new Buffer();
		public volatile static Buffer tipBondIndexIntraQ = new Buffer();
		public volatile static  Buffer tipShareIntraQ = new Buffer();
		public volatile static  Buffer tipShareSpecialIntraQ = new Buffer();
		public volatile static Buffer tipSMEShareIntraQ = new Buffer();
		public volatile static  Buffer tipSMEShareSpecialIntraQ = new Buffer();
		public volatile static Buffer tipBondIntraQ = new Buffer();	 
		public volatile static  Buffer tipBondSpecialIntraQ = new Buffer();
		
		public volatile static Buffer tipindexQ = new Buffer();
	    public volatile static Buffer tipSMEindexQ = new Buffer();
	    public volatile static Buffer tipBondindexQ = new Buffer();
	    public volatile static Buffer tipshareQ = new Buffer();
	    public volatile static Buffer tipSMEshareQ = new Buffer();
	    public volatile static Buffer tipbondQ = new Buffer();
	    public volatile static Buffer tipmarketQ = new Buffer();
	    
	    //Added for PTTP Project CRQ22398	    
	    public volatile static Buffer tipOTCShareIntraQ = new Buffer();
	    
	    //Added for PTTP Project CRQ22398	    
	    public volatile static Buffer tipOTCBondIntraQ = new Buffer();
	    public volatile static Buffer tipOTCbondQ = new Buffer();
	    
}
