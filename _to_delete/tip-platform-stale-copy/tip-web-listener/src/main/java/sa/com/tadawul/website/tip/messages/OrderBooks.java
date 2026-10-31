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
 * FileName : OrderBooks.java
 *
 * Created Date : 19 April 2018
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 19 April 2018           Mohanram                  Initial Version
 *----------------------------------------------------------------------------------------------
*/

package sa.com.tadawul.website.tip.messages;

import sa.com.tadawul.website.tip.shared.TipConstants;

/**
 * This Enum is used in the switch statement to identify message type.
 * It is used to identify message type. It will take string 
 * argument and return corresponding number.  
 *
 */

public enum OrderBooks {
	
	   IDCODE (1, "i"),	 
	   SOURCEID (2, "Si"),	   
	   TIMEEXEC (3, "Tx"),
	   ORDERBOOKFLUSH (4, "Of"),
	   BIDLEVELDELETED (5, "LDb"),
	   ASKLEVELDELETED (6, "LDs"),	   
	   BIDPRICEATLEVEL (7, "b"),  	  
	   BIDVOLUMEATLEVEL (8, "Vb"),	   
	   ASKPRICEATLEVEL (9, "a"),	  
	   ASKVOLUMEATLEVEL (10, "Va"),
	   SEQUENCENO(11,"n"),
	   BIDYIELDATLEVEL(12,"Yb"),
	   ASKYIELDATLEVEL(13,"Ya"),
	   BID_ORDER_AT_LEVEL(14,"Nb"),
       ASK_ORDER_AT_LEVEL(15,"Na"),
       NOT_VALID(-1,"");

	private int value;
	private String messageTypeStr;

	private OrderBooks(int value, String messageTypeStr) {
		this.value = value;
		this.messageTypeStr = messageTypeStr;
	}

	private static final java.util.Map<String, Integer> LOOKUP = buildLookup();

	private static java.util.Map<String, Integer> buildLookup() {
		// first declaration wins, as with the old linear scan
		return java.util.Arrays.stream(values())
				.collect(java.util.stream.Collectors.toUnmodifiableMap(e -> e.messageTypeStr, e -> e.value, (first, later) -> first));
	}

	/** O(1), allocation-free and thread-safe (the old version cloned values() and wrote shared statics on every call). */
	public static int getMessageValue(String message) {
		if (message == null) {
			return TipConstants.NODATAFOUND;
		}
		Integer v = LOOKUP.get(message);
		return v != null ? v : TipConstants.NODATAFOUND;
	}
	private static final java.util.Map<String, OrderBooks> TYPE_LOOKUP = buildTypeLookup();

	private static java.util.Map<String, OrderBooks> buildTypeLookup() {
		return java.util.Arrays.stream(values())
				.collect(java.util.stream.Collectors.toUnmodifiableMap(e -> e.messageTypeStr, e -> e, (first, later) -> first));
	}

	/** O(1) lookup; NOT_VALID when unknown (same contract as the old linear scan). */
	public static OrderBooks getMessageType(String message) {
		if (message == null) {
			return OrderBooks.NOT_VALID;
		}
		return TYPE_LOOKUP.getOrDefault(message, OrderBooks.NOT_VALID);
	}
}
