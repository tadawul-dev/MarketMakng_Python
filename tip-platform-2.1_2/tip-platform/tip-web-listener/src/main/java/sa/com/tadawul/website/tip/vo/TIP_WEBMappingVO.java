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
 * Name          :     FileName : TIP_IndexVO.java
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 10/April/2009            Oracle                   Initial Version
 * 1.1				   14/November/2009         Mohanram,Oracle          Added functionalities to process the Equity Rights Share
 * 1.2				   02/February/2010         Mohanram,Oracle          Added functionalities to process the Exchange Traded Funds.	
 *----------------------------------------------------------------------------------------------
 */
package sa.com.tadawul.website.tip.vo;

import sa.com.tadawul.website.tip.vo.TIP_BaseVO;

/**
 *@description This class contains attributes to get and set values in to Index Tables.
 * @ version 1.0
 */

public class TIP_WEBMappingVO extends TIP_BaseVO{
    

	/** Index Id*/
	 public String MarketId;
	 
	 public int Website_SectorID;
    /** ListId*/
    public String BDLi;
    public String BDLiId;
    
    public String BDIn;
    
    public String BDInId;
    
    public String Index_category;
    
   
    
    /**
     * This constructor is called to initialize  Index Table related attributes
     */
    public  TIP_WEBMappingVO(){
        frameNo = 0;
        analyserStartTime = 0;
        analyserEndTime = 0;
        readerStartTime = 0;
        readerEndTime = 0;
        sequence_no = 0;
        isBOD = false;
        isEOD = false;
        isUpdated = false;
        isEndOfBatch = false;
        
        MarketId = "";
        Website_SectorID = 0;
        BDLi = "";
        BDLiId = "";
        BDIn = "";
        BDInId = "";
        Index_category="";
    }
    /**
     * This constructor is called to set values related to Index Table.
     * @param tmpTipIndex contains index Table related attributes
     */
    public  TIP_WEBMappingVO(TIP_WEBMappingVO tmpTipIndex){
        frameNo = tmpTipIndex.frameNo;
        analyserStartTime = tmpTipIndex.analyserStartTime;
        analyserEndTime = tmpTipIndex.analyserEndTime;
        readerStartTime = tmpTipIndex.readerStartTime;
        readerEndTime = tmpTipIndex.readerEndTime;
        sequence_no = tmpTipIndex.sequence_no;
        isBOD = tmpTipIndex.isBOD;
        isEOD = tmpTipIndex.isEOD;
        isUpdated = tmpTipIndex.isUpdated;
        isEndOfBatch = tmpTipIndex.isEndOfBatch;
        
        MarketId = tmpTipIndex.MarketId;
        Website_SectorID = tmpTipIndex.Website_SectorID;
        BDLi = tmpTipIndex.BDLi;
        BDLiId = tmpTipIndex.BDLiId;
        BDIn = tmpTipIndex.BDIn;
        BDInId = tmpTipIndex.BDInId;
        Index_category = tmpTipIndex.Index_category;
      
    }
    
    
    public String getMarketId() {
		return MarketId;
	}
	public void setMarketId(String marketId) {
		MarketId = marketId;
	}
	public int getWebsite_SectorID() {
		return Website_SectorID;
	}
	public void setWebsite_SectorID(int website_SectorID) {
		Website_SectorID = website_SectorID;
	}
	public String getBDLi() {
		if(null!=BDLi)
			BDLi = BDLi.trim();
		return BDLi;
	}
	public void setBDLi(String bDLi) {
		BDLi = bDLi;
	}
	public String getBDLiId() {
		if(null!=BDLiId)
			BDLiId = BDLiId.trim();
		return BDLiId;
	}
	public void setBDLiId(String bDLiId) {
		BDLiId = bDLiId;
	}
	public String getBDIn() {
		return BDIn;
	}
	public void setBDIn(String bDIn) {
		BDIn = bDIn;
	}
	public String getBDInId() {
		if(null!=BDInId)
			BDInId = BDInId.trim();
		return BDInId;
	}
	public void setBDInId(String bDInId) {
		BDInId = bDInId;
	}
	public String getIndex_category() {
		return Index_category;
	}
	public void setIndex_category(String index_category) {
		Index_category = index_category;
	}
    
}
