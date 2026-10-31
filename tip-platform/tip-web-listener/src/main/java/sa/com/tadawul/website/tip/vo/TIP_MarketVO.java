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
 * Name          :     FileName : TIP_MarketVO.java
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 10/April/2009            Oracle                   Initial Version
 * 1.1				   14/November/2009         Mohanram,Oracle          Added functionalities to process the Equity Rights Share
 * 1.2				   02/February/2010         Mohanram,Oracle          Added functionalities to process the Exchange Traded Funds.	
 *----------------------------------------------------------------------------------------------
 */
package sa.com.tadawul.website.tip.vo;

import java.sql.Timestamp;

import sa.com.tadawul.website.tip.shared.TipConstants;
import sa.com.tadawul.website.tip.vo.TIP_BaseVO;

/**
 * @description This class contains attributes to get and set Market related information.
 * @ version 1.0
 */
public class TIP_MarketVO extends TIP_BaseVO{
    
    /**MarketId*/
    public long MarketId;
    /**Exchange Code*/
    public String Pk_exchange_code;
    /**Description*/
    public String description;
    /**isdefault*/
    public int isdefault;
    /**Market Status - If the market is suspended, this status will show
     * suspended but current state field will reflect the state properly*/
    public int market_status;
    /**Current State*/
    public volatile int current_state;
    /**No of Trades*/
    public int no_of_trades;
    /**No of Symbol Traded*/
    public int no_of_symbols_traded;
    /**Turn over*/
    public double turnover;
    /**Volume Traded*/
    public long volume_traded;
    /**No of Downs*/
    public int no_of_downs;
    /**No of changes*/
    public int no_of_no_changes;
    /**No of ups*/
    public int no_of_ups;
    /**Lasttradeddate*/
    public Timestamp lasttradeddate;
    
    /**
     * This constructor is called to initialize all Market related attributes
     */
    public  TIP_MarketVO(){
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
        
        MarketId = 0;
        Pk_exchange_code = "";
        description = "";
        isdefault = 0;
        market_status = TipConstants.TIP_DB_STATE_CLOSED;
        current_state = TipConstants.TIP_DB_STATE_CLOSED;
        no_of_trades = 0;
        no_of_symbols_traded = 0;
        turnover = 0;
        volume_traded = 0;
        no_of_downs = 0;
        no_of_no_changes = 0;
        no_of_ups = 0;
        lasttradeddate = new Timestamp(System.currentTimeMillis());
    }
    /**
     * This constructor is called to set all Market related values into attributes.
     * @param tmpTipMarket contains market related attributes
     */
    public  TIP_MarketVO(TIP_MarketVO tmpTipMarket){
        frameNo = tmpTipMarket.frameNo;
        analyserStartTime = tmpTipMarket.analyserStartTime;
        analyserEndTime = tmpTipMarket.analyserEndTime;
        readerStartTime = tmpTipMarket.readerStartTime;
        readerEndTime = tmpTipMarket.readerEndTime;
        sequence_no = tmpTipMarket.sequence_no;
        isBOD = tmpTipMarket.isBOD;
        isEOD = tmpTipMarket.isEOD;
        isUpdated = tmpTipMarket.isUpdated;
        isEndOfBatch = tmpTipMarket.isEndOfBatch;
        
        MarketId = tmpTipMarket.MarketId;
        Pk_exchange_code = tmpTipMarket.Pk_exchange_code;
        description = tmpTipMarket.description;
        isdefault = tmpTipMarket.isdefault;
        market_status = tmpTipMarket.market_status;
        current_state = tmpTipMarket.current_state;
        no_of_trades = tmpTipMarket.no_of_trades;
        no_of_symbols_traded = tmpTipMarket.no_of_symbols_traded;
        turnover = tmpTipMarket.turnover;
        volume_traded = tmpTipMarket.volume_traded;
        no_of_downs = tmpTipMarket.no_of_downs;
        no_of_no_changes = tmpTipMarket.no_of_no_changes;
        no_of_ups = tmpTipMarket.no_of_ups;
        lasttradeddate = tmpTipMarket.lasttradeddate;
    }
    
}
