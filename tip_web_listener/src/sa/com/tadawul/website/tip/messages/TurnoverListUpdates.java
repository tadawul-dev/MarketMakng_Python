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
 * FileName : TurnoverListUpdates.java
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

public enum TurnoverListUpdates {
	
	IDCODE (1, "i"),
	SOURCEID (2, "Si"),
   TIMEEXEC (3, "Tx"),
   NUMBEROFTRADES (4, "ANt"),
   TRADEDQUANTITY (5, "Tq"),
   ACCUMULATEDTURNOVER (6, "At"),
   TOTALTRADED (7, "TOt"),
   MINUSPAID (8, "Pm"),
   UNCHANGEDPAID (9, "Pu"),
   PLUSPAID (10, "Pp") ,
   SEQUENCENO(11,"n");
	
	
	private int value;
	private String messageTypeStr;

	static TurnoverListUpdates messageType[];
	static int messageOrdinal;

	private TurnoverListUpdates(int value, String messageTypeStr) {
		this.value = value;
		this.messageTypeStr = messageTypeStr;
	}

	public static int getMessageValue(String message) {

		messageType = TurnoverListUpdates.values();

		for (TurnoverListUpdates msg : messageType) {
			if (msg.messageTypeStr.equals(message)) {
				messageOrdinal = msg.value;
				return messageOrdinal;
			}
		}
		return TipConstants.NODATAFOUND;
	}
	
}
