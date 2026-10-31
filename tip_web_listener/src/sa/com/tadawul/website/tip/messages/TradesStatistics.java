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
 * FileName : TradesStatistics.java
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

public enum TradesStatistics {

  IDCODE (1, "i"),
  SOURCEID (2, "Si"),
  DIFFLASTPRICE (3, "Pd"),
  NUMBEROFTRADES (4, "ANt"),
  DIFFLASTPRICEPERCENT (5, "PLp"),
  MARKETCAP (6, "CAp"),
  BOOKRATIO (7, "Br"),
  PRICEEARNINGSRATIO (8, "Pr"),
  ACCUMULATEDVOLUME (9, "Aq"),
  REPORTEDVOLUME (10, "Rq"),
  ACCUMULATEDTURNOVER (11, "At"),
  REPORTEDTURNOVER (12, "Rt"),
  LASTYIELD (13, "Yl"),
  HIGHPRICE (14, "Ph"),
  LOWPRICE (15, "LOp"),
  FIRSTPRICE (16, "Pf"),
  SEQUENCENO(17,"n");
  

	/* SEQUENCENUMBER (1, "n"),
	    IDCODE (2, "i"),
	    SOURCEID (3, "Si"),
	    SOURCESYSTEM (4, "s"),
	  FIRSTPRICE (5, "Pf"),
	  LASTPRICE (6, "Pl"),
	  HIGHPRICE (7, "Ph"),
	  LOWPRICE (8, "LOp"),
	  AVERAGEPRICE (9, "Pa"),
	  DIFFLASTPRICE (10, "Pd"),
	  FIRSTYIELD (11, "Yf"),
	  LASTYIELD (12, "Yl"),
	  HIGHYIELD (13, "Yh"),
	  LOWYIELD (14, "Yo"),
	  AVERAGEYIELD (15, "Yv"),
	  DIFFLASTYIELD (16, "Yd"),
	  NUMBEROFTRADES (17, "ANt"),
	  ACCUMULATEDVOLUME (18, "Aq"),
	  REPORTEDVOLUME (19, "Rq"),
	  ACCUMULATEDTURNOVER (20, "At"),
	  REPORTEDTURNOVER (21, "Rt"),
	  LASTTRADEREPORTPRICE (22, "LTRp"),
	  LASTTRADEREPORTQUANTITY (23, "LTRq"),
	  DIFFLASTPRICEPERCENT (24, "PLp"),
	  BOOKRATIO (25, "Br"),
	  MARKETCAP (26, "CAp"),
	  PRICEEARNINGSRATIO (27, "Pr"),
	  AVERAGEVOLUME (28, "Av"),
	  LASTTRADEVOLUME (29, "LTv"),
	  TWAP (30, "TWAp");*/
	
	private int value;
	private String messageTypeStr;

	static TradesStatistics messageType[];
	static int messageOrdinal;

	private TradesStatistics(int value, String messageTypeStr) {
		this.value = value;
		this.messageTypeStr = messageTypeStr;
	}

	public static int getMessageValue(String message) {

		messageType = TradesStatistics.values();

		for (TradesStatistics msg : messageType) {
			if (msg.messageTypeStr.equals(message)) {
				messageOrdinal = msg.value;
				return messageOrdinal;
			}
		}
		return TipConstants.NODATAFOUND;
	}
	
}
