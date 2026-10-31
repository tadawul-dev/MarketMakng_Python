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
 * FileName : BasicDataBond.java
 *
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
  */

public enum BasicDataBond {

	IDCODE (1,"i"),
	SOURCEID (2,"Si"),
	 MARKETID (3,"m"),
	SYMBOL (4,"SYm"),
	NAME (5,"NAm"),
	SEQUENCENO(6,"n"),
	ISIN(7,"ISn"),
	OUTSTANDINGAMOUNT(8,"AOs"),
	ISSUANCECURRENCY(9,"CUi"),
	//NOMINALVALUE(10,"NMv"),
	ROUNDLOTVALUE(10,"LOt"),
	COUPONFREQUENCY(11,"CFq"),
	DAYCOUNTMETHOD(12,"DCm"),
	COUPONRATE(13,"RCp"),
	MATURITYDATE(14,"DMa"),
	RATECALCTYPE(15,"RCt"),
	LISTINGDATE(16,"LDa"),
	ISSUEAMOUNT(17,"AIs"),
	ISSUEDATE(18,"DIs"),
	SETTLENTDATE(19,"STd"),
	UPDATECODE(20,"Uc");
	
	
	private int value;
	private String messageTypeStr;
	
	static BasicDataBond messageType [];
	static int messageOrdinal;
	private BasicDataBond(int value, String messageTypeStr){
		this.value = value;
		this.messageTypeStr = messageTypeStr;
	}
	
	public static int getMessageValue(String message){
		
		messageType = BasicDataBond.values();
		
		for(BasicDataBond msg : messageType){
			if(msg.messageTypeStr.equals(message)) {
				messageOrdinal = msg.value;
				return messageOrdinal;
			}
		}
		return TipConstants.NODATAFOUND;
	}
}
