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

	static OrderBooks messageType[];
	static int messageOrdinal;

	private OrderBooks(int value, String messageTypeStr) {
		this.value = value;
		this.messageTypeStr = messageTypeStr;
	}

	public static int getMessageValue(String message) {

		messageType = OrderBooks.values();

		for (OrderBooks msg : messageType) {
			if (msg.messageTypeStr.equals(message)) {
				messageOrdinal = msg.value;
				return messageOrdinal;
			}
		}
		return TipConstants.NODATAFOUND;
	}
    public static OrderBooks getMessageType(String message) {

        messageType = OrderBooks.values();

        for (OrderBooks msg : messageType) {
            if (msg.messageTypeStr.equals(message)) {
                messageOrdinal = msg.value;
                return msg;
            }
        }
        return OrderBooks.NOT_VALID;
    }
}
