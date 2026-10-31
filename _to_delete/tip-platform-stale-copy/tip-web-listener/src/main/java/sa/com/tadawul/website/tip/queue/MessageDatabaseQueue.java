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

public final class MessageDatabaseQueue {

		private MessageDatabaseQueue() {
		}


		public static final Buffer tipIndexIntraQ = new Buffer("tipIndexIntraQ");
		public static final Buffer tipSMEIndexIntraQ = new Buffer("tipSMEIndexIntraQ");
		public static final Buffer tipBondIndexIntraQ = new Buffer("tipBondIndexIntraQ");
		public static final Buffer tipShareIntraQ = new Buffer("tipShareIntraQ");
		public static final Buffer tipShareSpecialIntraQ = new Buffer("tipShareSpecialIntraQ");
		public static final Buffer tipSMEShareIntraQ = new Buffer("tipSMEShareIntraQ");
		public static final Buffer tipSMEShareSpecialIntraQ = new Buffer("tipSMEShareSpecialIntraQ");
		public static final Buffer tipBondIntraQ = new Buffer("tipBondIntraQ");	 
		public static final Buffer tipBondSpecialIntraQ = new Buffer("tipBondSpecialIntraQ");
		
		public static final Buffer tipindexQ = new Buffer("tipindexQ");
	    public static final Buffer tipSMEindexQ = new Buffer("tipSMEindexQ");
	    public static final Buffer tipBondindexQ = new Buffer("tipBondindexQ");
	    public static final Buffer tipshareQ = new Buffer("tipshareQ");
	    public static final Buffer tipSMEshareQ = new Buffer("tipSMEshareQ");
	    public static final Buffer tipbondQ = new Buffer("tipbondQ");
	    public static final Buffer tipmarketQ = new Buffer("tipmarketQ");
	    
	    //Added for PTTP Project CRQ22398	    
	    public static final Buffer tipOTCShareIntraQ = new Buffer("tipOTCShareIntraQ");
	    
	    //Added for PTTP Project CRQ22398	    
	    public static final Buffer tipOTCBondIntraQ = new Buffer("tipOTCBondIntraQ");
	    public static final Buffer tipOTCbondQ = new Buffer("tipOTCbondQ");
	    

	    /** Every DB buffer, for monitoring and shutdown draining. */
	    public static java.util.List<Buffer> all() {
	        return java.util.List.of(tipIndexIntraQ, tipSMEIndexIntraQ, tipBondIndexIntraQ, tipShareIntraQ, tipShareSpecialIntraQ, tipSMEShareIntraQ, tipSMEShareSpecialIntraQ, tipBondIntraQ, tipBondSpecialIntraQ, tipindexQ, tipSMEindexQ, tipBondindexQ, tipshareQ, tipSMEshareQ, tipbondQ, tipmarketQ, tipOTCShareIntraQ, tipOTCBondIntraQ, tipOTCbondQ);
	    }

	    /** Value objects waiting in all DB buffers. */
	    public static int totalPending() {
	        return all().stream().mapToInt(Buffer::size).sum();
	    }
}
