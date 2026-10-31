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
 * FileName : OrderBookSummary.java
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

public enum OrderBookSummary {
	
	IDCODE (1,"i"),
	SOURCEID (2,"Si"),	
	Date (3, "Dt"),
	EXECUTIONDATETIME (4,"Tx"),
	ORDERBOOKSTATECODE (5,"OSt"),
	ADJUSTEDCLOSINGPRICE (6,"ACp"),
	LASTPRICE (7,"Pl"),
	FIRSTPRICE (8,"Pf"),
	MARKETCAPITAL (9,"MCa"),
	LASTPAIDDATE (10,"LPd"),
	OFFICIALCLOSINGPRICE (11,"POf"),
	LASTTRADEVOLUME (12,"LTv"),
	HIGHPRICE (13,"Ph"),
	LOWPRICE (14,"LOp"),
	HIGHPRICE52WEEK (15,"HPw"),
	LOWPRICE52WEEK (16,"LPw"),
	SEQUENCENO(17,"n"),
	BIDYIELDATLEVEL1(18,"BYl"),
	ASKYIELDATLEVEL1(19,"AYa"),
	BIDPRICEATLEVEL1(20,"BPr"),
	ASKPRICEATLEVEL1(21,"APl"),
	BIDVOLUMEATLEVEL1(22,"VBl"),
	ASKVOLUMEATLEVEL1(23,"VAl"),
	DIFFLASTPRICE (24, "Pd"),
	NUMBEROFTRADES (25, "ANt"),
	DIFFLASTPRICEPERCENT (26, "PLp"),
	ACCUMULATEDVOLUME (27, "Aq"),
	REPORTEDVOLUME (28, "Rq"),
	ACCUMULATEDTURNOVER (29, "At"),
	REPORTEDTURNOVER (30, "Rt"),;
	/*
	AVERAGEPRICE (13,"Pa"),
	DIFFLASTPRICE (14,"Pd"),
	BIDYIELDDIFF (15,"BYd"),
	BIDYIELDATLEVEL1 (16,"BYl"),
	ASKYIELDATLEVEL1 (17,"AYa"),
	FIRSTYIELD (18,"Yf"),
	LASTYIELD (19,"Yl"),
	HIGHYIELD (20,"Yh"),
	LOWYIELD (21,"Yo"),
	AVERAGEYIELD (22,"Yv"),
	DIFFLASTYIELD (23,"Yd"),
	NUMBEROFTRADES (24,"ANt"),
	ACCUMULATEDVOLUME (25,"Aq"),
	REPORTEDVOLUME (26,"Rq"),
	ACCUMULATEDTURNOVER (27,"At"),
	REPORTEDTURNOVER (28,"Rt"),
	HIGHPRICEMONTH (29,"HPm"),
	HIGHYIELDMONTH (30,"HYm"),
	HIGHPRICEMONTHDATE (31,"HPMd"),
	LOWPRICEMONTH (32,"LPm"),
	LOWYIELDMONTH (33,"LYm"),
	LOWPRICEMONTHDATE (34,"LPMd"),
	HIGHPRICEYEAR (35,"HPy"),
	HIGHYIELDYEAR (36,"HYy"),
	HIGHPRICEYEARDATE (37,"HPYd"),
	LOWPRICEYEAR (38,"LPy"),
	LOWYIELDYEAR (39,"LYy"),
	LOWPRICEYEARDATE (40,"LPYd"),
	HIGHPRICELIFE (41,"HPl"),
	HIGHYIELDLIFE (42,"HYl"),
	HIGHPRICELIFEDATE (43,"HPLd"),
	LOWPRICELIFE (44,"LPl"),
	LOWYIELDLIFE (45,"LYl"),
	LOWPRICELIFEDATE (46,"LPLd"),
	LASTTRADEDDATE (47,"LTd"),
	LASTPAIDDATE (48,"LPd"),
	MARKETCAPITAL (49,"MCa"),
	ADJUSTEDCLOSINGPRICE (50,"ACp"),
	LASTTRADEVOLUME (51,"LTv"),
	HIGHPRICE52WEEK (52,"HPw"),
	LOWPRICE52WEEK (53,"LPw"),
	ASKVOLUMEATLEVEL1 (54,"VAl"),
	BIDVOLUMEATLEVEL1 (55,"VBl"),
	OFFICIALCLOSINGPRICE (56,"POf"),
	TWAP (57,"TWAp"),
	
	SEQUENCENUMBER (59, "n");*/

	
	 /* SEQUENCENUMBER (1, "n"),
	    IDCODE (2, "i"),
	    SOURCEID (3, "Si"),
	    SOURCESYSTEM (4, "s"),
	    TIMEEXEC (5, "Tx"),
	   Date (6, "Dt"),
	  ORDERBOOKSTATECODE (7, "OSt"),
		  ISOFFICIALCLOSINGPRICE (8, "ISOc"),
		  ISOFFICIALCLOSINGTURNOVER (9, "ISOt"),
		  BIDPRICEDIFF (10, "d"),
		  BIDPRICEATLEVEL1 (11, "BPr"),
		  ASKPRICEATLEVEL1 (12, "APl"),
		  FIRSTPRICE (13, "Pf"),
		  LASTPRICE (14, "Pl"),
		  HIGHPRICE (15, "Ph"),
		  LOWPRICE (16, "LOp"),
		  AVERAGEPRICE (17, "Pa"),
		  DIFFLASTPRICE (18, "Pd"),
		  BIDYIELDDIFF (19, "BYd"),
		  BIDYIELDATLEVEL1 (20, "BYl"),
		  ASKYIELDATLEVEL1 (21, "AYa"),
		  FIRSTYIELD (22, "Yf"),
		  LASTYIELD (23, "Yl"),
		  HIGHYIELD (24, "Yh"),
		  LOWYIELD (25, "Yo"),
	      AVERAGEYIELD (26, "Yv"),
		  DIFFLASTYIELD (27, "Yd"),
		  NUMBEROFTRADES (28, "ANt"),
		  ACCUMULATEDVOLUME (29, "Aq"),
		  REPORTEDVOLUME (30, "Rq"),
		  ACCUMULATEDTURNOVER (31, "At"),
		  REPORTEDTURNOVER (32, "Rt"),
		  HIGHPRICEMONTH (33, "HPm"),
		  HIGHYIELDMONTH (34, "HYm"),
		  HIGHPRICEMONTHDATE (35, "HPMd"),
		  LOWPRICEMONTH (36, "LPm"),
		  LOWYIELDMONTH (37, "LYm"),
		  LOWPRICEMONTHDATE (38, "LPMd"),
		  HIGHPRICEYEAR (39, "HPy"),
		  HIGHYIELDYEAR (40, "HYy"),
		  HIGHPRICEYEARDATE (41, "HPYd"),
		  LOWPRICEYEAR (42, "LPy"),
		  LOWYIELDYEAR (43, "LYy"),
		  LOWPRICEYEARDATE (44, "LPYd"),
		  HIGHPRICELIFE (45, "HPl"),
		  HIGHYIELDLIFE (46, "HYl"),
		  HIGHPRICELIFEDATE (47, "HPLd"),
		  LOWPRICELIFE (48, "LPl"),
		  LOWYIELDLIFE (49, "LYl"),
		  LOWPRICELIFEDATE (50, "LPLd"),
		  LASTTRADEDDATE (51, "LTd"),
		  LASTPAIDDATE (52, "LPd"),
		  MARKETCAPITAL (53, "MCa"),
		  ADJUSTEDCLOSINGPRICE (54, "ACp"),
		  LASTTRADEVOLUME (55, "LTv"),
		  HIGHPRICE52WEEK (56, "HPw"),
		  LOWPRICE52WEEK (57, "LPw"),
		  ASKVOLUMEATLEVEL1 (58, "VAl"),
		  BIDVOLUMEATLEVEL1 (59, "VBl"),
		  OFFICIALCLOSINGPRICE (60, "POf"),
		  TWAP (61, "TWAp");*/
	
	private int value;
	private String messageTypeStr;

	static OrderBookSummary messageType[];
	static int messageOrdinal;

	private OrderBookSummary(int value, String messageTypeStr) {
		this.value = value;
		this.messageTypeStr = messageTypeStr;
	}

	public static int getMessageValue(String message) {

		messageType = OrderBookSummary.values();

		for (OrderBookSummary msg : messageType) {
			if (msg.messageTypeStr.equals(message)) {
				messageOrdinal = msg.value;
				return messageOrdinal;
			}
		}
		return TipConstants.NODATAFOUND;
	}
	
}
