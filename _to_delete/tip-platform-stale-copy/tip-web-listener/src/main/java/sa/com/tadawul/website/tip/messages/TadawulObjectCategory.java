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
 * FileName : BasicDataShare.java
 *
 * Created Date : 19 April 2018
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 19 April 2018           Mohanram                  Initial Version
 *----------------------------------------------------------------------------------------------
*/

package sa.com.tadawul.website.tip.messages;

import sa.com.tadawul.website.tip.shared.TIPDataObjects;
import sa.com.tadawul.website.tip.shared.TipConstants;

/**
 * This Enum is used in the switch statement to identify message type.
 * It is used to identify message type. It will take string 
 * argument and return corresponding number.  
 */

public enum TadawulObjectCategory {
	
SHARE (1),
SMESHARE(2),
BOND (3),
SHAREINDEX (4),
SMESHAEINDEX (5),
BONDINDEX (6),
OTCSHARE(7),
OTCBOND(8);
	


	static TadawulObjectCategory messageType[];
	static int messageOrdinal;

	private TadawulObjectCategory(int value) {		
		
	}

	public static TadawulObjectCategory getIndexMessageCategoty(String symbol_id) {

		messageType = TadawulObjectCategory.values();		
		
		 if( TIPDataObjects.tipBondIndex_Symbol_ArrayIndexMap.get(symbol_id) !=null)
    	 {
    		return BONDINDEX;
    	 }else if(TIPDataObjects.tipIndex_Symbol_ArrayIndexMap.get(symbol_id) !=null)
    	 {
    		 return SHAREINDEX;
    	 }	else if(TIPDataObjects.tipSMEIndex_Symbol_ArrayIndexMap.get(symbol_id) !=null)
    	 {
    		 return SMESHAEINDEX;
    	 }    	 
		 return null;
	}
	
	
	public static TadawulObjectCategory getShareMessageCategoty(String symbol_id) {

		messageType = TadawulObjectCategory.values();
		
		 if(TIPDataObjects.tipOTCShare_Symbol_ArrayIndexMap.get(symbol_id) !=null)
    	 {
    		 return OTCSHARE;
    	 }else
		 if(TIPDataObjects.tipShare_Symbol_ArrayIndexMap.get(symbol_id) !=null)
    	 {
    		 return SHARE;
    	 }
    	 else if(TIPDataObjects.tipSMEShare_Symbol_ArrayIndexMap.get(symbol_id) !=null)
    	 {
    		 return SMESHARE;
    	 }		
    	 else if(TIPDataObjects.tipOTCBond_Symbol_ArrayIndexMap.get(symbol_id) !=null)
    	 {
    		 return OTCBOND;
    	 }else if(TIPDataObjects.tipBond_Symbol_ArrayIndexMap.get(symbol_id) !=null)
    	 {
    		 return BOND;
    	 }
		 return null;
	}
	
	
	public static TadawulObjectCategory getShareMessageCategotyByIDCode(int idCode) {

		messageType = TadawulObjectCategory.values();
		
		if(idCode == (int)TipConstants.SHARE_MARKET_LIST_ID){
			 return SHARE;
        }
        
        if(idCode == TipConstants.BOND_MARKET_LIST_ID){
        	 return BOND;
        }
        if(idCode == TipConstants.SME_MARKET_LIST_ID){
        	 return SMESHARE;
        }       
		
		 return null;
	}
}
